package com.github.happyuky7.swi.api.data;

import com.github.happyuky7.swi.api.context.GroupContext;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Read access to the player data stored by SepareWorldItems, whatever the storage backend
 * (YAML files, SQLite, MySQL/MariaDB, MongoDB, Redis).
 *
 * <p>All methods do IO off the server thread and complete their future on an async thread.
 * Switch back to the server thread yourself before touching game objects.</p>
 *
 * <p>Note: for the context a player is currently in, the live state is the player itself;
 * the stored record is only updated when the player leaves that context.</p>
 */
public interface PlayerDataService {

    @NotNull CompletableFuture<Optional<PlayerSnapshot>> load(@NotNull UUID player, @NotNull GroupContext context);

    @NotNull CompletableFuture<Boolean> exists(@NotNull UUID player, @NotNull GroupContext context);

    /**
     * Finds the UUID of a player by the last name saved in that context (case-insensitive).
     */
    @NotNull CompletableFuture<Optional<UUID>> resolveUuid(@NotNull String playerName, @NotNull GroupContext context);
}
