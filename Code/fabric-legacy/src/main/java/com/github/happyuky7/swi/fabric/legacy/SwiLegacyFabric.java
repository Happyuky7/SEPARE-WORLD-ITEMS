package com.github.happyuky7.swi.fabric.legacy;

import com.github.happyuky7.swi.api.Platform;
import com.github.happyuky7.swi.api.SepareWorldItems;
import com.github.happyuky7.swi.api.context.GroupContext;
import com.github.happyuky7.swi.api.data.DataNode;
import com.github.happyuky7.swi.api.data.PlayerSnapshot;
import com.github.happyuky7.swi.common.api.CommonApi;
import com.github.happyuky7.swi.common.backup.BackupService;
import com.github.happyuky7.swi.common.i18n.Messages;
import com.github.happyuky7.swi.common.api.SettingsGroupService;
import com.github.happyuky7.swi.common.api.StorePlayerDataService;
import com.github.happyuky7.swi.common.config.ConfigFile;
import com.github.happyuky7.swi.common.config.SwiSettings;
import com.github.happyuky7.swi.common.event.SimpleEventBus;
import com.github.happyuky7.swi.common.section.SimpleSectionRegistry;
import com.github.happyuky7.swi.common.storage.PlayerDataStore;
import com.github.happyuky7.swi.common.storage.StorageFactory;
import com.github.happyuky7.swi.common.storage.YamlFileStore;
import com.github.happyuky7.swi.common.switching.PlayerAdapter;
import com.github.happyuky7.swi.common.switching.SwitchEngine;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.legacyfabric.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.legacyfabric.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.legacyfabric.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.legacyfabric.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.legacyfabric.fabric.api.registry.CommandRegistrationCallback;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Minecraft 1.12.2 entry point (Legacy Fabric). Same behaviour, config and data layout as the modern Fabric mod.
 */
public final class SwiLegacyFabric implements ModInitializer {

    private final ConfigFile configFile = new ConfigFile(
            FabricLoader.getInstance().getConfigDir().resolve("separeworlditems").resolve("config.yml").toFile(),
            "separeworlditems/config.yml", SwiLegacyFabric.class.getClassLoader());
    private final File langsDir = FabricLoader.getInstance().getConfigDir().resolve("separeworlditems").resolve("langs").toFile();

    private volatile SwiSettings settings;
    private volatile PlayerDataStore store;
    private volatile Messages messages;
    private volatile BackupService backups;
    private LegacyPlatform platform;
    private SwitchEngine<ServerPlayerEntity> engine;

    @Override
    public void onInitialize() {
        platform = new LegacyPlatform(() -> settings != null && settings.debug());
        loadSettings();
        loadMessages();

        SimpleEventBus events = new SimpleEventBus(platform.logger());
        SimpleSectionRegistry sections = new SimpleSectionRegistry(LegacySections.TOGGLES);
        engine = new SwitchEngine<>(platform, players(platform), this::settings, this::store, events, sections,
                LegacySections.create(this::settings), LegacySections::stamp);
        engine.setRecordValidator(SwiLegacyFabric::rejectForeignItems);

        SepareWorldItems.register(new CommonApi(Platform.FABRIC, "1.12.2", platform.modVersion(),
                new SettingsGroupService(this::settings, engine.tracker()),
                new StorePlayerDataService(this::store, platform.asyncExecutor()), events, sections));

        // Worlds are not loaded yet at SERVER_STARTING on 1.12.2; nobody can join before SERVER_STARTED.
        ServerLifecycleEvents.SERVER_STARTED.register(this::start);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> stop());
        if (Boolean.getBoolean(LegacySelfTest.PROPERTY)) {
            ServerLifecycleEvents.SERVER_STARTED.register(server -> LegacySelfTest.run(server, platform, this));
        }

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> onJoin(handler.player));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> engine.onQuit(handler.player));
        ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register((player, origin, destination) -> onWorldChange(player, destination));
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, world, alive) -> onWorldChange(newPlayer, newPlayer.world));

        CommandRegistrationCallback.EVENT.register(registry -> registry.register(new LegacyCommand(this)));
    }

    SwiSettings settings() {
        return settings;
    }

    PlayerDataStore store() {
        PlayerDataStore s = store;
        if (s == null) {
            throw new IllegalStateException("SepareWorldItems storage is not available until the server starts");
        }
        return s;
    }

    SwitchEngine<ServerPlayerEntity> engine() {
        return engine;
    }

    LegacyPlatform platform() {
        return platform;
    }

    Messages messages() {
        return messages;
    }

    /** Null while the server is not running. */
    BackupService backups() {
        return backups;
    }

    List<String> reload() {
        List<String> problems = loadSettings();
        loadMessages();
        BackupService b = backups;
        if (b != null) {
            b.start();
        }
        if (store != null && store.type() != settings.storageType()) {
            problems.add("storage.type changed to " + settings.storageType() + ": restart the server to switch backend.");
        }
        return problems;
    }

    /**
     * @return dimension id like the modern versions use, e.g. {@code minecraft:the_nether}
     */
    static String dimensionId(World world) {
        return "minecraft:" + world.dimension.getDimensionType().getName();
    }

    private static PlayerAdapter<ServerPlayerEntity> players(LegacyPlatform platform) {
        return new PlayerAdapter<ServerPlayerEntity>() {
            public Class<ServerPlayerEntity> playerType() { return ServerPlayerEntity.class; }
            public UUID uuid(ServerPlayerEntity p) { return p.getUuid(); }
            public String name(ServerPlayerEntity p) { return p.getGameProfile().getName(); }
            public boolean isOnline(ServerPlayerEntity p) {
                MinecraftServer s = platform.server();
                return s != null && s.getPlayerManager().getPlayer(p.getUuid()) == p;
            }
        };
    }

    private List<String> loadSettings() {
        try {
            settings = new SwiSettings(configFile.load());
        } catch (IOException | RuntimeException e) {
            platform.logger().error("Cannot read " + configFile.file() + ", using defaults", e);
            if (settings == null) {
                settings = new SwiSettings(DataNode.create());
            }
        }
        List<String> problems = new ArrayList<>(settings.validate());
        for (String p : problems) {
            platform.logger().warn("[Config] " + p);
        }
        return problems;
    }

    /** Same as the modern mods: local/bundled file right away, newest GitHub file in the background. */
    private void loadMessages() {
        String lang = settings.raw().getString("settings.langs.lang", Messages.DEFAULT_LANG);
        ClassLoader loader = SwiLegacyFabric.class.getClassLoader();
        messages = Messages.load(langsDir, lang, false, loader, "separeworlditems/langs/", platform.logger());
        if (settings.raw().getBoolean("settings.langs.autoDownload", true)) {
            Thread t = new Thread(() -> messages = Messages.load(langsDir, lang, true, loader, "separeworlditems/langs/", platform.logger()),
                    "SepareWorldItems-Langs");
            t.setDaemon(true);
            t.start();
        }
    }

    private void start(MinecraftServer server) {
        File dataFolder = new File(server.worlds[0].getSaveHandler().getWorldFolder(), "separeworlditems");
        platform.start(server, dataFolder);
        try {
            store = StorageFactory.create(settings, dataFolder, platform.logger());
        } catch (RuntimeException e) {
            platform.logger().error("[Storage] Cannot start " + settings.storageType() + ", falling back to YAML files", e);
            store = new YamlFileStore(dataFolder, platform.logger());
        }
        backups = new BackupService(dataFolder, platform.logger(), this::settings, () -> store, configFile.file());
        backups.start();
        platform.logger().info("SepareWorldItems " + platform.modVersion() + " started (storage: " + store.type() + ", data: " + dataFolder + ")");
    }

    private void stop() {
        BackupService b = backups;
        backups = null;
        if (b != null) {
            b.stop();
        }
        platform.stop();
        PlayerDataStore s = store;
        store = null;
        if (s != null) {
            s.close();
        }
    }

    private void onJoin(ServerPlayerEntity player) {
        String group = settings.groupOfWorld(dimensionId(player.world));
        if (group == null && settings.defaultGroupEnabled()) {
            group = settings.defaultGroup();
        }
        if (group != null) {
            engine.onJoin(player, GroupContext.world(group));
        }
    }

    private void onWorldChange(ServerPlayerEntity player, World destination) {
        String group = settings.groupOfWorld(dimensionId(destination));
        if (group != null) {
            engine.switchTo(player, GroupContext.world(group), null);
        }
    }

    private static void rejectForeignItems(DataNode record) {
        String format = record.getString("swi.format");
        boolean hasItems = record.contains("inventory") || record.contains("ender-chest")
                || record.contains("armor_contents") || record.contains("off_hand_item");
        if (hasItems && !PlayerSnapshot.FORMAT_SNBT.equals(format)) {
            throw new IllegalStateException("Record contains items in '" + (format == null ? PlayerSnapshot.FORMAT_BUKKIT : format)
                    + "' format (written by the Bukkit plugin?). Refusing to switch so no items are lost.");
        }
    }
}
