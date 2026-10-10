package com.github.happyuky7.swi.api.data;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Platform-independent tree of saved values (the content of one player data record).
 *
 * <p>Paths use {@code .} as separator ({@code "armor_contents.helmet"}). Values are plain
 * YAML/JSON types: {@link String}, {@link Number}, {@link Boolean}, {@link List} and nested nodes.
 * On Bukkit, item stacks saved by the plugin keep the Bukkit serialization format; on Fabric
 * items are stored as SNBT strings (see {@link PlayerSnapshot#format()}).</p>
 */
public interface DataNode {

    /**
     * @return a new, empty, mutable node
     */
    static @NotNull DataNode create() {
        return new MapDataNode();
    }

    /**
     * @return a new mutable node containing a deep copy of the given map
     */
    static @NotNull DataNode of(@NotNull Map<?, ?> values) {
        return new MapDataNode(values);
    }

    boolean contains(@NotNull String path);

    /**
     * @return the raw value (nested nodes are returned as {@link DataNode}), or null
     */
    @Nullable Object get(@NotNull String path);

    void set(@NotNull String path, @Nullable Object value);

    default void remove(@NotNull String path) {
        set(path, null);
    }

    /**
     * @return the direct child keys of this node, in insertion order
     */
    @NotNull Set<String> keys();

    /**
     * @return the child node at the path, or null when absent or not a node
     */
    @Nullable DataNode getNode(@NotNull String path);

    /**
     * @return the child node at the path, creating it (and replacing any non-node value) when needed
     */
    @NotNull DataNode getOrCreateNode(@NotNull String path);

    default @Nullable String getString(@NotNull String path) {
        Object v = get(path);
        return v == null || v instanceof DataNode ? null : String.valueOf(v);
    }

    default @NotNull String getString(@NotNull String path, @NotNull String def) {
        String v = getString(path);
        return v != null ? v : def;
    }

    default int getInt(@NotNull String path, int def) {
        Number n = number(get(path));
        return n != null ? n.intValue() : def;
    }

    default long getLong(@NotNull String path, long def) {
        Number n = number(get(path));
        return n != null ? n.longValue() : def;
    }

    default double getDouble(@NotNull String path, double def) {
        Number n = number(get(path));
        return n != null ? n.doubleValue() : def;
    }

    default float getFloat(@NotNull String path, float def) {
        Number n = number(get(path));
        return n != null ? n.floatValue() : def;
    }

    default boolean getBoolean(@NotNull String path, boolean def) {
        Object v = get(path);
        if (v instanceof Boolean) return (Boolean) v;
        if (v instanceof String) return Boolean.parseBoolean((String) v);
        return def;
    }

    default @Nullable List<?> getList(@NotNull String path) {
        Object v = get(path);
        return v instanceof List ? (List<?>) v : null;
    }

    /**
     * @return a deep copy of this node as nested {@code Map<String, Object>} (ready for YAML/JSON)
     */
    @NotNull Map<String, Object> toMap();

    /**
     * @return a deep, independent copy of this node
     */
    default @NotNull DataNode copy() {
        return of(toMap());
    }

    static @Nullable Number number(@Nullable Object v) {
        if (v instanceof Number) return (Number) v;
        if (v instanceof String) {
            try {
                return Double.parseDouble((String) v);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }
}
