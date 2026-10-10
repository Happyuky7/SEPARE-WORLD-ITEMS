package com.github.happyuky7.swi.api.data;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * {@link DataNode} backed by a {@link LinkedHashMap}. Not thread-safe.
 */
final class MapDataNode implements DataNode {

    private final Map<String, Object> values = new LinkedHashMap<>();

    MapDataNode() {
    }

    MapDataNode(Map<?, ?> source) {
        for (Map.Entry<?, ?> e : source.entrySet()) {
            if (e.getKey() != null) {
                values.put(String.valueOf(e.getKey()), importValue(e.getValue()));
            }
        }
    }

    @Override
    public boolean contains(@NotNull String path) {
        return get(path) != null;
    }

    @Override
    public @Nullable Object get(@NotNull String path) {
        int dot = path.indexOf('.');
        if (dot < 0) {
            return values.get(path);
        }
        Object child = values.get(path.substring(0, dot));
        return child instanceof DataNode ? ((DataNode) child).get(path.substring(dot + 1)) : null;
    }

    @Override
    public void set(@NotNull String path, @Nullable Object value) {
        int dot = path.indexOf('.');
        if (dot < 0) {
            if (value == null) {
                values.remove(path);
            } else {
                values.put(path, importValue(value));
            }
            return;
        }
        String head = path.substring(0, dot);
        if (value == null) {
            Object child = values.get(head);
            if (child instanceof DataNode) {
                ((DataNode) child).set(path.substring(dot + 1), null);
            }
            return;
        }
        getOrCreateNode(head).set(path.substring(dot + 1), value);
    }

    @Override
    public @NotNull Set<String> keys() {
        return Collections.unmodifiableSet(values.keySet());
    }

    @Override
    public @Nullable DataNode getNode(@NotNull String path) {
        Object v = get(path);
        return v instanceof DataNode ? (DataNode) v : null;
    }

    @Override
    public @NotNull DataNode getOrCreateNode(@NotNull String path) {
        int dot = path.indexOf('.');
        String head = dot < 0 ? path : path.substring(0, dot);
        Object child = values.get(head);
        if (!(child instanceof DataNode)) {
            child = new MapDataNode();
            values.put(head, child);
        }
        return dot < 0 ? (DataNode) child : ((DataNode) child).getOrCreateNode(path.substring(dot + 1));
    }

    @Override
    public @NotNull Map<String, Object> toMap() {
        Map<String, Object> out = new LinkedHashMap<>();
        for (Map.Entry<String, Object> e : values.entrySet()) {
            out.put(e.getKey(), exportValue(e.getValue()));
        }
        return out;
    }

    @Override
    public String toString() {
        return toMap().toString();
    }

    private static Object importValue(Object value) {
        if (value instanceof DataNode) {
            return new MapDataNode(((DataNode) value).toMap());
        }
        if (value instanceof Map) {
            return new MapDataNode((Map<?, ?>) value);
        }
        if (value instanceof List) {
            List<Object> copy = new ArrayList<>();
            for (Object o : (List<?>) value) {
                copy.add(importValue(o));
            }
            return copy;
        }
        return value;
    }

    private static Object exportValue(Object value) {
        if (value instanceof DataNode) {
            return ((DataNode) value).toMap();
        }
        if (value instanceof List) {
            List<Object> copy = new ArrayList<>();
            for (Object o : (List<?>) value) {
                copy.add(exportValue(o));
            }
            return copy;
        }
        return value;
    }
}
