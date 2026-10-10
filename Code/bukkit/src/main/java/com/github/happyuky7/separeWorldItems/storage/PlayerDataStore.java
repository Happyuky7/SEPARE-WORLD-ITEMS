package com.github.happyuky7.separeWorldItems.storage;

import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Backend abstraction for player data persistence.
 *
 * <p>Important: Implementations must be safe to call from the server thread,
 * but may do IO. Callers should prefer async for heavy operations.</p>
 */
public interface PlayerDataStore extends AutoCloseable {

    @FunctionalInterface
    interface PlayerDataEntryConsumer {
        /**
         * @param groupName group name
         * @param playerUuid player UUID
         * @param playerName last known player name when available (nullable)
         */
        void accept(@NotNull String groupName, @NotNull UUID playerUuid, @Nullable String playerName);
    }

    /**
     * Returns true when the player has a saved record in this store for this scope+group.
     */
    boolean exists(@NotNull PlayerDataScope scope, @NotNull String groupName, @NotNull UUID playerUuid);

    @NotNull YamlConfiguration load(@NotNull PlayerDataScope scope, @NotNull String groupName, @NotNull UUID playerUuid);

    void save(
            @NotNull PlayerDataScope scope,
            @NotNull String groupName,
            @NotNull UUID playerUuid,
            @NotNull String playerName,
            @NotNull YamlConfiguration config
    );

    /**
     * Resolves a UUID by scanning/indexing where applicable.
     * Returns null when not found.
     */
    @Nullable UUID resolveUuidByName(@NotNull PlayerDataScope scope, @NotNull String groupName, @NotNull String playerName);

    /**
     * Iterates all known entries in this store for the given scope.
     *
     * <p>This is primarily used for backups (remote snapshot export) and migrations.
     * Implementations may do IO and should be used from async contexts when large.</p>
     *
     * @throws UnsupportedOperationException when the backend cannot enumerate entries.
     */
    default void forEachEntry(@NotNull PlayerDataScope scope, @NotNull PlayerDataEntryConsumer consumer) {
        throw new UnsupportedOperationException("This storage backend does not support enumeration");
    }

    /**
     * Returns true if this store is based on YAML files on disk.
     */
    default boolean isFileBased() {
        return false;
    }

    @Override
    default void close() {
        // optional
    }
}
