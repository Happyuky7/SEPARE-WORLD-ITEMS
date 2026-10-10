package com.github.happyuky7.separeWorldItems.bridge;

import com.github.happyuky7.separeWorldItems.SepareWorldItems;
import com.github.happyuky7.separeWorldItems.managers.PlayerContextState;
import com.github.happyuky7.separeWorldItems.storage.PlayerDataScope;
import com.github.happyuky7.separeWorldItems.utils.PluginSchedulers;
import com.github.happyuky7.swi.api.Platform;
import com.github.happyuky7.swi.api.context.ContextType;
import com.github.happyuky7.swi.api.context.GroupContext;
import com.github.happyuky7.swi.api.context.GroupService;
import com.github.happyuky7.swi.api.data.DataNode;
import com.github.happyuky7.swi.api.data.PlayerDataService;
import com.github.happyuky7.swi.api.data.PlayerSnapshot;
import com.github.happyuky7.swi.api.event.GroupSwitchPostEvent;
import com.github.happyuky7.swi.api.event.GroupSwitchPreEvent;
import com.github.happyuky7.swi.api.event.PlayerDataLoadEvent;
import com.github.happyuky7.swi.api.event.PlayerDataSaveEvent;
import com.github.happyuky7.swi.api.section.DataSection;
import com.github.happyuky7.swi.common.api.CommonApi;
import com.github.happyuky7.swi.common.event.SimpleEventBus;
import com.github.happyuky7.swi.common.platform.SwiLogger;
import com.github.happyuky7.swi.common.section.SimpleSectionRegistry;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

/**
 * Connects the plugin to the platform-independent API ({@code com.github.happyuky7.swi.api}).
 */
public final class ApiBridge {

    /** Built-in save toggles; custom sections cannot use these ids. */
    public static final List<String> BUILTIN_TOGGLES = Arrays.asList(
            "gamemode", "flying", "fly-speed", "exp", "enderchest", "inventory", "potion-effects", "food-level", "off-hand", "health");

    private static SimpleEventBus events;
    private static SimpleSectionRegistry sections;

    private ApiBridge() {
    }

    public static void enable(SepareWorldItems plugin) {
        SwiLogger logger = new SwiLogger() {
            public void info(String m) { plugin.getLogger().info(m); }
            public void warn(String m) { plugin.getLogger().warning(m); }
            public void error(String m, Throwable t) { plugin.getLogger().log(Level.SEVERE, m, t); }
            public void debug(String m) { if (plugin.getConfig().getBoolean("settings.debug")) plugin.getLogger().info("[debug] " + m); }
        };
        events = new SimpleEventBus(logger);
        sections = new SimpleSectionRegistry(BUILTIN_TOGGLES);
        String mc = Bukkit.getBukkitVersion().split("-")[0];
        com.github.happyuky7.swi.api.SepareWorldItems.register(new CommonApi(Platform.BUKKIT, mc, plugin.getDescription().getVersion(),
                new Groups(plugin), new Data(plugin), events, sections));
    }

    public static void disable() {
        com.github.happyuky7.swi.api.SepareWorldItems.unregister();
        events = null;
        sections = null;
    }

    private static boolean active() {
        return events != null;
    }

    public static GroupContext context(PlayerDataScope scope, String group) {
        return GroupContext.of(scope == PlayerDataScope.WORLDGUARD_GROUP ? ContextType.REGION : ContextType.WORLD, group);
    }

    // ---- switch events ----

    /**
     * @return true when an API listener cancelled the switch
     */
    public static boolean firePre(Player player, PlayerDataScope fromScope, String fromGroup, PlayerDataScope toScope, String toGroup) {
        if (!active()) return false;
        return events.post(new GroupSwitchPreEvent(player, player.getUniqueId(), player.getName(),
                context(fromScope, fromGroup), context(toScope, toGroup), null)).isCancelled();
    }

    public static void firePost(Player player, PlayerDataScope fromScope, String fromGroup, PlayerDataScope toScope, String toGroup) {
        if (!active()) return;
        events.post(new GroupSwitchPostEvent(player, player.getUniqueId(), player.getName(),
                context(fromScope, fromGroup), context(toScope, toGroup), null));
    }

    // ---- custom sections + data events ----

    public static void afterSave(SepareWorldItems plugin, Player player, PlayerDataScope scope, String group, YamlConfiguration config) {
        if (!active()) return;
        for (DataSection<?> s : sections.all()) {
            if (s.playerType().isInstance(player) && enabled(plugin, group, s)) {
                config.set("custom." + s.id(), null);
                ConfigurationSection out = config.createSection("custom." + s.id());
                try {
                    saveSection(s, player, new BukkitDataNode(out));
                } catch (RuntimeException e) {
                    plugin.getLogger().log(Level.SEVERE, "[API] Section '" + s.id() + "' failed to save for " + player.getName(), e);
                }
            }
        }
        events.post(new PlayerDataSaveEvent(player, player.getUniqueId(), player.getName(), context(scope, group), new BukkitDataNode(config)));
    }

    public static void beforeLoad(Player player, PlayerDataScope scope, String group, YamlConfiguration config) {
        if (!active()) return;
        events.post(new PlayerDataLoadEvent(player, player.getUniqueId(), player.getName(), context(scope, group), new BukkitDataNode(config)));
    }

    public static void afterLoad(SepareWorldItems plugin, Player player, String group, YamlConfiguration config) {
        if (!active()) return;
        for (DataSection<?> s : sections.all()) {
            if (s.playerType().isInstance(player) && enabled(plugin, group, s)) {
                ConfigurationSection in = config.getConfigurationSection("custom." + s.id());
                try {
                    loadSection(s, player, in != null ? new BukkitDataNode(in) : DataNode.create());
                } catch (RuntimeException e) {
                    plugin.getLogger().log(Level.SEVERE, "[API] Section '" + s.id() + "' failed to load for " + player.getName(), e);
                }
            }
        }
    }

    public static void clear(SepareWorldItems plugin, Player player) {
        if (!active()) return;
        for (DataSection<?> s : sections.all()) {
            if (s.playerType().isInstance(player)) {
                try {
                    clearSection(s, player);
                } catch (RuntimeException e) {
                    plugin.getLogger().log(Level.SEVERE, "[API] Section '" + s.id() + "' failed to clear for " + player.getName(), e);
                }
            }
        }
    }

    private static boolean enabled(SepareWorldItems plugin, String group, DataSection<?> s) {
        String override = "settings.options.saves-per-group." + group + "." + s.id();
        if (group != null && plugin.getConfig().contains(override)) {
            return plugin.getConfig().getBoolean(override);
        }
        String global = "settings.options.saves." + s.id();
        return plugin.getConfig().contains(global) ? plugin.getConfig().getBoolean(global) : s.enabledByDefault();
    }

    @SuppressWarnings("unchecked")
    private static <P> void saveSection(DataSection<P> s, Object player, DataNode node) {
        s.save((P) player, node);
    }

    @SuppressWarnings("unchecked")
    private static <P> void loadSection(DataSection<P> s, Object player, DataNode node) {
        s.load((P) player, node);
    }

    @SuppressWarnings("unchecked")
    private static <P> void clearSection(DataSection<P> s, Object player) {
        s.clear((P) player);
    }

    // ---- services ----

    private static final class Groups implements GroupService {
        private final SepareWorldItems plugin;

        Groups(SepareWorldItems plugin) {
            this.plugin = plugin;
        }

        public @NotNull Set<String> worldGroups() {
            return Collections.unmodifiableSet(new LinkedHashSet<>(plugin.getConfig().getStringList("groups")));
        }

        public @NotNull Set<String> regionGroups() {
            return Collections.unmodifiableSet(new LinkedHashSet<>(plugin.getConfig().getStringList("worldguard-regions.groups")));
        }

        public @NotNull Map<String, String> worldMappings() {
            Map<String, String> out = new LinkedHashMap<>();
            ConfigurationSection worlds = plugin.getConfig().getConfigurationSection("worlds");
            if (worlds != null) {
                for (String w : worlds.getKeys(false)) out.put(w, worlds.getString(w));
            }
            return Collections.unmodifiableMap(out);
        }

        public @Nullable String groupOfWorld(@NotNull String worldId) {
            return plugin.getConfig().getString("worlds." + worldId);
        }

        public @NotNull String defaultGroup() {
            return plugin.getConfig().getString("settings.options.default-group.group", "default");
        }

        public @Nullable GroupContext currentContext(@NotNull UUID player) {
            PlayerContextState.Context c = PlayerContextState.get(player);
            if (c == null) {
                return null;
            }
            return GroupContext.of(c.worldGuardRegion ? ContextType.REGION : ContextType.WORLD, c.groupName);
        }
    }

    private static final class Data implements PlayerDataService {
        private final SepareWorldItems plugin;

        Data(SepareWorldItems plugin) {
            this.plugin = plugin;
        }

        private static PlayerDataScope scope(GroupContext c) {
            return c.isRegion() ? PlayerDataScope.WORLDGUARD_GROUP : PlayerDataScope.WORLD_GROUP;
        }

        public @NotNull CompletableFuture<Optional<PlayerSnapshot>> load(@NotNull UUID player, @NotNull GroupContext context) {
            return PluginSchedulers.supplyAsync(plugin, () -> {
                if (!plugin.getPlayerDataStore().exists(scope(context), context.group(), player)) {
                    return Optional.empty();
                }
                YamlConfiguration cfg = plugin.getPlayerDataStore().load(scope(context), context.group(), player);
                return Optional.of(new PlayerSnapshot(player, null, context, DataNode.of(BukkitDataNode.toPlainMap(cfg))));
            });
        }

        public @NotNull CompletableFuture<Boolean> exists(@NotNull UUID player, @NotNull GroupContext context) {
            return PluginSchedulers.supplyAsync(plugin, () -> plugin.getPlayerDataStore().exists(scope(context), context.group(), player));
        }

        public @NotNull CompletableFuture<Optional<UUID>> resolveUuid(@NotNull String playerName, @NotNull GroupContext context) {
            return PluginSchedulers.supplyAsync(plugin,
                    () -> Optional.ofNullable(plugin.getPlayerDataStore().resolveUuidByName(scope(context), context.group(), playerName)));
        }
    }
}
