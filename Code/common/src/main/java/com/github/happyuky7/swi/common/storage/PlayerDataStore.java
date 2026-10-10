package com.github.happyuky7.swi.common.storage;

import com.github.happyuky7.swi.api.context.ContextType;
import com.github.happyuky7.swi.api.context.GroupContext;
import com.github.happyuky7.swi.api.data.DataNode;

import java.util.UUID;

/**
 * Platform-independent persistence of player records. Implementations may block (IO).
 */
public interface PlayerDataStore extends AutoCloseable {

    interface EntryConsumer {
        void accept(String group, UUID uuid, String lastKnownName);
    }

    StorageType type();

    boolean exists(GroupContext context, UUID uuid);

    /**
     * @return the record, or an empty node when there is none
     */
    DataNode load(GroupContext context, UUID uuid);

    void save(GroupContext context, UUID uuid, String playerName, DataNode data);

    /**
     * @return the UUID saved with that name (case-insensitive), or null
     */
    UUID resolveUuidByName(GroupContext context, String playerName);

    void forEachEntry(ContextType type, EntryConsumer consumer);

    @Override
    default void close() {
    }

    /**
     * Name used by the Bukkit plugin's SQL/Mongo/Redis records for a context type, kept for compatibility.
     */
    static String legacyScopeName(ContextType type) {
        return type == ContextType.REGION ? "WORLDGUARD_GROUP" : "WORLD_GROUP";
    }
}
