package com.github.happyuky7.separeWorldItems.managers;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Shared, in-memory tracking of a player's current data context.
 *
 * <p>This is used to keep WorldChangeEvent and MoveEvent consistent and to avoid
 * saving to the wrong group during rapid transitions.</p>
 */
public final class PlayerContextState {

    private PlayerContextState() {
    }

    public static final class Context {
        public final boolean worldGuardRegion;
        public final String groupName;

        public Context(boolean worldGuardRegion, String groupName) {
            this.worldGuardRegion = worldGuardRegion;
            this.groupName = groupName;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Context context)) return false;
            return worldGuardRegion == context.worldGuardRegion && Objects.equals(groupName, context.groupName);
        }

        @Override
        public int hashCode() {
            return Objects.hash(worldGuardRegion, groupName);
        }

        @Override
        public String toString() {
            return (worldGuardRegion ? "WG" : "WORLD") + ":" + groupName;
        }
    }

    private static final ConcurrentMap<UUID, Context> LAST_CONTEXT_BY_PLAYER = new ConcurrentHashMap<>();

    public static Context get(UUID uuid) {
        return uuid == null ? null : LAST_CONTEXT_BY_PLAYER.get(uuid);
    }

    public static void set(UUID uuid, Context context) {
        if (uuid == null || context == null) {
            return;
        }
        LAST_CONTEXT_BY_PLAYER.put(uuid, context);
    }

    public static Context putIfAbsent(UUID uuid, Context context) {
        if (uuid == null || context == null) {
            return null;
        }
        return LAST_CONTEXT_BY_PLAYER.putIfAbsent(uuid, context);
    }
}
