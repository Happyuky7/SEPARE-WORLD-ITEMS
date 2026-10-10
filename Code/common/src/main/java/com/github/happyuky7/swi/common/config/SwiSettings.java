package com.github.happyuky7.swi.common.config;

import com.github.happyuky7.swi.api.data.DataNode;
import com.github.happyuky7.swi.common.storage.StorageType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Parsed view of config.yml. Same keys as the Bukkit plugin's config, so one file layout works everywhere.
 * Immutable: reloading creates a new instance.
 */
public final class SwiSettings {

    private final DataNode raw;
    private final boolean debug;
    private final boolean autoConfigureWorlds;
    private final boolean defaultGroupEnabled;
    private final String defaultGroup;
    private final Set<String> groups;
    private final Map<String, String> worlds;
    private final String healthType;
    private final StorageType storageType;

    public SwiSettings(DataNode raw) {
        this.raw = raw.copy();
        this.debug = raw.getBoolean("settings.debug", false);
        this.autoConfigureWorlds = raw.getBoolean("settings.auto-configure-worlds", false);
        this.defaultGroupEnabled = raw.getBoolean("settings.options.default-group.enabled", true);
        this.defaultGroup = raw.getString("settings.options.default-group.group", "default");
        this.healthType = raw.getString("settings.health-options.type", "VANILLA");
        this.storageType = StorageType.fromConfig(raw.getString("storage.type"));

        Set<String> g = new LinkedHashSet<>();
        List<?> list = raw.getList("groups");
        if (list != null) {
            for (Object o : list) {
                if (o != null) g.add(String.valueOf(o));
            }
        }
        this.groups = Collections.unmodifiableSet(g);

        Map<String, String> w = new LinkedHashMap<>();
        DataNode worldsNode = raw.getNode("worlds");
        if (worldsNode != null) {
            for (String world : worldsNode.keys()) {
                String group = worldsNode.getString(world);
                if (group != null && !group.isEmpty()) {
                    w.put(world, group);
                }
            }
        }
        this.worlds = Collections.unmodifiableMap(w);
    }

    /**
     * @return a mutable copy of the whole config tree
     */
    public DataNode raw() {
        return raw.copy();
    }

    public boolean debug() {
        return debug;
    }

    public boolean autoConfigureWorlds() {
        return autoConfigureWorlds;
    }

    public boolean defaultGroupEnabled() {
        return defaultGroupEnabled;
    }

    public String defaultGroup() {
        return defaultGroup;
    }

    public Set<String> groups() {
        return groups;
    }

    public Map<String, String> worlds() {
        return worlds;
    }

    /**
     * VANILLA (BUKKIT is accepted as an alias), CUSTOM (do not touch health) or an integration id.
     */
    public String healthType() {
        return "BUKKIT".equalsIgnoreCase(healthType) ? "VANILLA" : healthType.toUpperCase(java.util.Locale.ROOT);
    }

    public StorageType storageType() {
        return storageType;
    }

    /**
     * @return group of that world, or null when the world is not listed under {@code worlds:}
     */
    public String groupOfWorld(String worldId) {
        return worlds.get(worldId);
    }

    /**
     * Save toggle: {@code settings.options.saves-per-group.<group>.<key>} overrides {@code settings.options.saves.<key>}.
     */
    public boolean isSaveEnabled(String group, String key, boolean def) {
        if (group != null && !group.isEmpty()) {
            Object override = raw.get("settings.options.saves-per-group." + group + "." + key);
            if (override instanceof Boolean) {
                return (Boolean) override;
            }
        }
        return raw.getBoolean("settings.options.saves." + key, def);
    }


    public List<String> validate() {
        List<String> problems = new ArrayList<>();
        if (defaultGroupEnabled && !groups.contains(defaultGroup)) {
            problems.add("Default group '" + defaultGroup + "' is not listed under 'groups'.");
        }
        for (Map.Entry<String, String> e : worlds.entrySet()) {
            if (!groups.contains(e.getValue())) {
                problems.add("World '" + e.getKey() + "' uses group '" + e.getValue() + "' which is not listed under 'groups'.");
            }
        }
        return problems;
    }
}
