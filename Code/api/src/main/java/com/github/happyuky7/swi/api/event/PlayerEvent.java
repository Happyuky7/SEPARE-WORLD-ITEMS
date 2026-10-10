package com.github.happyuky7.swi.api.event;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.UUID;

/**
 * Base class of events about an online player.
 */
public abstract class PlayerEvent implements SwiEvent {

    private final Object player;
    private final UUID playerUuid;
    private final String playerName;

    protected PlayerEvent(@NotNull Object player, @NotNull UUID playerUuid, @NotNull String playerName) {
        this.player = Objects.requireNonNull(player, "player");
        this.playerUuid = Objects.requireNonNull(playerUuid, "playerUuid");
        this.playerName = Objects.requireNonNull(playerName, "playerName");
    }

    public @NotNull UUID playerUuid() {
        return playerUuid;
    }

    public @NotNull String playerName() {
        return playerName;
    }

    /**
     * @return the platform player object ({@code org.bukkit.entity.Player} or {@code ServerPlayer})
     */
    public @NotNull Object platformPlayer() {
        return player;
    }

    /**
     * Typed access to the platform player, e.g. {@code event.player(Player.class)}.
     *
     * @throws ClassCastException when the type does not match the running platform
     */
    public @NotNull <P> P player(@NotNull Class<P> type) {
        return type.cast(player);
    }
}
