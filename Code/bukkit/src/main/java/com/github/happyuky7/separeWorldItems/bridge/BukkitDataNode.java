package com.github.happyuky7.separeWorldItems.bridge;

import com.github.happyuky7.swi.api.data.DataNode;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Live {@link DataNode} view over a Bukkit {@link ConfigurationSection}: API listeners edit the plugin's YAML directly.
 * Bukkit objects (ItemStack...) are exposed as their serialized maps, with the usual {@code "=="} type key.
 */
public final class BukkitDataNode implements DataNode {

    private final ConfigurationSection section;

    public BukkitDataNode(ConfigurationSection section) {
        this.section = section;
    }

    public ConfigurationSection section() {
        return section;
    }

    @Override
    public boolean contains(@NotNull String path) {
        return section.contains(path);
    }

    @Override
    public @Nullable Object get(@NotNull String path) {
        Object v = section.get(path);
        return v instanceof ConfigurationSection ? new BukkitDataNode((ConfigurationSection) v) : v;
    }

    @Override
    public void set(@NotNull String path, @Nullable Object value) {
        section.set(path, value instanceof DataNode ? ((DataNode) value).toMap() : value);
    }

    @Override
    public @NotNull Set<String> keys() {
        return Collections.unmodifiableSet(new LinkedHashSet<>(section.getKeys(false)));
    }

    @Override
    public @Nullable DataNode getNode(@NotNull String path) {
        ConfigurationSection child = section.getConfigurationSection(path);
        return child != null ? new BukkitDataNode(child) : null;
    }

    @Override
    public @NotNull DataNode getOrCreateNode(@NotNull String path) {
        ConfigurationSection child = section.getConfigurationSection(path);
        return new BukkitDataNode(child != null ? child : section.createSection(path));
    }

    @Override
    public @NotNull Map<String, Object> toMap() {
        return toPlainMap(section);
    }

    public static Map<String, Object> toPlainMap(ConfigurationSection section) {
        Map<String, Object> out = new LinkedHashMap<>();
        for (String key : section.getKeys(false)) {
            out.put(key, plain(section.get(key)));
        }
        return out;
    }

    private static Object plain(Object v) {
        if (v instanceof ConfigurationSection) {
            return toPlainMap((ConfigurationSection) v);
        }
        if (v instanceof ConfigurationSerializable) {
            Map<String, Object> out = new LinkedHashMap<>();
            out.put(ConfigurationSerialization.SERIALIZED_TYPE_KEY, ConfigurationSerialization.getAlias(((ConfigurationSerializable) v).getClass()));
            for (Map.Entry<String, Object> e : ((ConfigurationSerializable) v).serialize().entrySet()) {
                out.put(e.getKey(), plain(e.getValue()));
            }
            return out;
        }
        if (v instanceof Map) {
            Map<String, Object> out = new LinkedHashMap<>();
            for (Map.Entry<?, ?> e : ((Map<?, ?>) v).entrySet()) {
                out.put(String.valueOf(e.getKey()), plain(e.getValue()));
            }
            return out;
        }
        if (v instanceof List) {
            List<Object> out = new ArrayList<>();
            for (Object o : (List<?>) v) {
                out.add(plain(o));
            }
            return out;
        }
        return v;
    }
}
