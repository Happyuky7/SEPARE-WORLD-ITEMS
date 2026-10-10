package com.github.happyuky7.swi.common.switching;

import com.github.happyuky7.swi.api.context.GroupContext;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Which context each online player's live state belongs to.
 */
public final class ContextTracker {

    private final ConcurrentMap<UUID, GroupContext> contexts = new ConcurrentHashMap<>();

    public GroupContext get(UUID uuid) {
        return contexts.get(uuid);
    }

    public void set(UUID uuid, GroupContext context) {
        contexts.put(uuid, context);
    }

    public GroupContext putIfAbsent(UUID uuid, GroupContext context) {
        return contexts.putIfAbsent(uuid, context);
    }

    public void remove(UUID uuid) {
        contexts.remove(uuid);
    }
}
