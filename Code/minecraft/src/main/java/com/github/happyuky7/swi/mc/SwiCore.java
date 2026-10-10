package com.github.happyuky7.swi.mc;

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
import com.github.happyuky7.swi.common.switching.SwitchEngine;
import com.github.happyuky7.swi.mc.compat.Mc;
import com.github.happyuky7.swi.mc.section.VanillaSections;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Loader-independent heart of the Fabric, NeoForge and Forge mods. Each loader only forwards its events
 * (server lifecycle, join/quit, dimension change, respawn, command registration) to this class.
 */
public final class SwiCore {

    /** Bundled copies of the repository's langs/ folder. */
    static final String LANGS_RESOURCE = "separeworlditems/langs/";

    private final String platformId;
    private final String loaderName;
    private final ConfigFile configFile;
    private final File langsDir;
    private final VanillaPlatform platform;
    private final SwitchEngine<ServerPlayer> engine;

    private volatile SwiSettings settings;
    private volatile PlayerDataStore store;
    private volatile Messages messages;
    private volatile BackupService backups;
    private volatile java.util.function.Predicate<CommandSourceStack> adminCheck = SwiCommands::vanillaAdmin;
    private final com.github.happyuky7.swi.mc.region.ModRegions regions = new com.github.happyuky7.swi.mc.region.ModRegions();
    private final java.util.Map<java.util.UUID, Pending> pendingRegion = new java.util.concurrent.ConcurrentHashMap<>();
    private int tickCounter;

    /** A region/world context seen at the last check, waiting for confirmation. */
    private static final class Pending {
        final GroupContext context;
        int seen = 1;

        Pending(GroupContext context) {
            this.context = context;
        }
    }

    /**
     * @param platformId  "fabric", "neoforge" or "forge" (written into records)
     * @param loaderName  shown by /swi, e.g. "NeoForge"
     * @param configDir   the loader's config directory; config lives in {@code <configDir>/separeworlditems/config.yml}
     * @param modVersion  version of the installed mod
     */
    public SwiCore(Platform apiPlatform, String platformId, String loaderName, File configDir, Supplier<String> modVersion) {
        this.platformId = platformId;
        this.loaderName = loaderName;
        this.configFile = new ConfigFile(new File(new File(configDir, "separeworlditems"), "config.yml"),
                "separeworlditems/config.yml", SwiCore.class.getClassLoader());
        this.langsDir = new File(new File(configDir, "separeworlditems"), "langs");
        this.platform = new VanillaPlatform(apiPlatform, modVersion, () -> settings != null && settings.debug());
        loadSettings();
        loadMessages();

        SimpleEventBus events = new SimpleEventBus(platform.logger());
        SimpleSectionRegistry sections = new SimpleSectionRegistry(VanillaSections.TOGGLES);
        this.engine = new SwitchEngine<>(platform, new VanillaPlayers(platform::server), this::settings, this::store,
                events, sections, VanillaSections.create(this::settings), VanillaSections.stamper(platformId));
        engine.setRecordValidator(SwiCore::rejectForeignItems);

        // Registered while the mod initializes so other mods can add sections/listeners from their own init.
        SepareWorldItems.register(new CommonApi(apiPlatform, Mc.minecraftVersion(), platform.modVersion(),
                new SettingsGroupService(this::settings, engine.tracker()),
                new StorePlayerDataService(this::store, platform.asyncExecutor()), events, sections));
    }

    public String platformId() {
        return platformId;
    }

    public String loaderName() {
        return loaderName;
    }

    public SwiSettings settings() {
        return settings;
    }

    public VanillaPlatform platform() {
        return platform;
    }

    /** Messages of the configured language ({@code settings.langs.lang}). */
    public Messages messages() {
        return messages;
    }

    /** Null while the server is not running. */
    public BackupService backups() {
        return backups;
    }

    public SwitchEngine<ServerPlayer> engine() {
        return engine;
    }

    /** Region/claim mods (YAWP, Flan, Open Parties and Claims) register themselves here. */
    public com.github.happyuky7.swi.mc.region.ModRegions regions() {
        return regions;
    }

    public PlayerDataStore store() {
        PlayerDataStore s = store;
        if (s == null) {
            throw new IllegalStateException("SepareWorldItems storage is not available until the server starts");
        }
        return s;
    }

    // ---- lifecycle ----

    /** Call when the server is starting (worlds may not be loaded yet). */
    public void serverStarting(MinecraftServer server) {
        File dataFolder = server.getWorldPath(LevelResource.ROOT).resolve("separeworlditems").toAbsolutePath().normalize().toFile();
        platform.start(server, dataFolder);
        try {
            store = StorageFactory.create(settings, dataFolder, platform.logger());
        } catch (RuntimeException e) {
            platform.logger().error("[Storage] Cannot start " + settings.storageType() + ", falling back to YAML files", e);
            store = new YamlFileStore(dataFolder, platform.logger());
        }
        backups = new BackupService(dataFolder, platform.logger(), this::settings, this::store, configFile.file());
        backups.start();
        platform.logger().info("SepareWorldItems " + platform.modVersion() + " started on " + loaderName
                + " (storage: " + store.type() + ", data: " + dataFolder + ")");
    }

    /**
     * Call when the server finished starting.
     *
     * @param selfTestPlayer creates the test player for {@link VanillaSelfTest} (only used when the self test is enabled)
     */
    public void serverStarted(MinecraftServer server, Function<MinecraftServer, ServerPlayer> selfTestPlayer) {
        autoConfigureWorlds(server);
        if (VanillaSelfTest.enabled()) {
            VanillaSelfTest.addHook(new ServicesSelfTest(this, langsDir));
            VanillaSelfTest.run(server, platform, selfTestPlayer.apply(server), platformId);
        }
    }

    public void serverStopped() {
        BackupService b = backups;
        backups = null;
        if (b != null) {
            b.stop();
        }
        platform.stop();
        PlayerDataStore s = store;
        store = null;
        if (s != null) {
            try {
                s.close();
            } catch (RuntimeException e) {
                platform.logger().error("[Storage] Error while closing", e);
            }
        }
    }

    // ---- players ----

    public void onJoin(ServerPlayer player) {
        com.github.happyuky7.swi.mc.region.RegionSource.Match region = regions.match(player, settings, platform.logger());
        GroupContext ctx = region != null ? GroupContext.region(region.group()) : contextOf(Mc.level(player));
        if (ctx != null) {
            engine.onJoin(player, ctx);
        }
    }

    public void onQuit(ServerPlayer player) {
        pendingRegion.remove(player.getUUID());
        engine.onQuit(player);
    }

    /** Call after the player arrived in another dimension (teleport, portal). */
    public void onLevelChange(ServerPlayer player, ServerLevel destination) {
        com.github.happyuky7.swi.mc.region.RegionSource.Match region = regions.match(player, settings, platform.logger());
        if (region != null) {
            engine.switchTo(player, GroupContext.region(region.group()), region.areaId());
            return;
        }
        String group = settings.groupOfWorld(Mc.dimensionId(destination));
        if (group == null) {
            // Unlisted dimension: the player keeps the data of the last managed group.
            return;
        }
        engine.switchTo(player, GroupContext.world(group), null);
    }

    /**
     * Call at the end of every server tick. When region groups are enabled, checks every
     * {@code regions.check-interval-ticks} whether players entered or left a configured area; a new
     * context must be seen twice in a row before switching, so walking along a border does not flicker.
     */
    public void onServerTick(MinecraftServer server) {
        if (!regions.hasSources() || !com.github.happyuky7.swi.mc.region.ModRegions.enabled(settings)) {
            return;
        }
        if (++tickCounter < com.github.happyuky7.swi.mc.region.ModRegions.intervalTicks(settings)) {
            return;
        }
        tickCounter = 0;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            com.github.happyuky7.swi.mc.region.RegionSource.Match region = regions.match(player, settings, platform.logger());
            GroupContext wanted = region != null ? GroupContext.region(region.group()) : worldContextOf(Mc.level(player));
            java.util.UUID uuid = player.getUUID();
            if (wanted == null || wanted.equals(engine.tracker().get(uuid))) {
                pendingRegion.remove(uuid);
                continue;
            }
            Pending pending = pendingRegion.get(uuid);
            if (pending == null || !pending.context.equals(wanted)) {
                pendingRegion.put(uuid, new Pending(wanted));
                continue;
            }
            pendingRegion.remove(uuid);
            engine.switchTo(player, wanted, region != null ? region.areaId() : null);
        }
    }

    /** World group of a level, or null when the level is not listed (no fallback to the default group). */
    private GroupContext worldContextOf(ServerLevel level) {
        String group = settings.groupOfWorld(Mc.dimensionId(level));
        return group != null ? GroupContext.world(group) : null;
    }

    /** Call after a respawn (death, leaving the End): the player entity is new and may be in another dimension. */
    public void onRespawn(ServerPlayer newPlayer) {
        onLevelChange(newPlayer, Mc.level(newPlayer));
    }

    /**
     * Replaces the operator-level check used for admin commands, e.g. with a LuckPerms-aware permission API.
     * The permission node is {@code separeworlditems.admin}.
     */
    public void setAdminCheck(java.util.function.Predicate<CommandSourceStack> check) {
        this.adminCheck = check;
    }

    public boolean isAdmin(CommandSourceStack source) {
        return adminCheck.test(source);
    }

    public void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        SwiCommands.register(dispatcher, this);
    }

    /**
     * Re-reads config.yml. The storage backend itself only changes on restart.
     *
     * @return problems found in the new config (empty when valid)
     */
    public List<String> reload() {
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

    /** Context for a player's live state in a level; unlisted levels fall back to the default group. */
    public GroupContext contextOf(ServerLevel level) {
        String group = settings.groupOfWorld(Mc.dimensionId(level));
        if (group == null && settings.defaultGroupEnabled()) {
            group = settings.defaultGroup();
        }
        return group != null ? GroupContext.world(group) : null;
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

    /**
     * Loads the language from disk/jar right away; when {@code settings.langs.autoDownload} is on, the newest
     * file is downloaded from GitHub in the background and replaces it.
     */
    private void loadMessages() {
        String lang = settings.raw().getString("settings.langs.lang", Messages.DEFAULT_LANG);
        ClassLoader loader = SwiCore.class.getClassLoader();
        messages = Messages.load(langsDir, lang, false, loader, LANGS_RESOURCE, platform.logger());
        if (settings.raw().getBoolean("settings.langs.autoDownload", true)) {
            Thread t = new Thread(() -> messages = Messages.load(langsDir, lang, true, loader, LANGS_RESOURCE, platform.logger()),
                    "SepareWorldItems-Langs");
            t.setDaemon(true);
            t.start();
        }
    }

    private void autoConfigureWorlds(MinecraftServer server) {
        if (!settings.autoConfigureWorlds() || !settings.defaultGroupEnabled()) {
            return;
        }
        DataNode raw = settings.raw();
        boolean changed = false;
        for (ServerLevel level : server.getAllLevels()) {
            String id = Mc.dimensionId(level);
            if (settings.groupOfWorld(id) == null) {
                raw.getOrCreateNode("worlds").set(id, settings.defaultGroup());
                platform.logger().info("[Config] Added dimension " + id + " to group " + settings.defaultGroup());
                changed = true;
            }
        }
        if (changed) {
            try {
                configFile.save(raw);
                settings = new SwiSettings(raw);
            } catch (IOException e) {
                platform.logger().error("[Config] Cannot save auto-configured worlds", e);
            }
        }
    }

    /**
     * Records written by the Bukkit plugin keep Bukkit item maps, which the mods cannot read: refuse
     * the switch instead of silently dropping the player's items.
     */
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
