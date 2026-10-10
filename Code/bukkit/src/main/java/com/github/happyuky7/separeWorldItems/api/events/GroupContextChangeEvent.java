package com.github.happyuky7.separeWorldItems.api.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Fired when SepareWorldItems switches a player's storage context.
 *
 * Context is either:
 * - WORLD: regular world-based groups (data folder: groups/<group>/...)
 * - WORLDGUARD_REGION: region-based groups (data folder: worldguard/groups/<group>/...)
 */
public final class GroupContextChangeEvent extends Event {

    public enum ContextType {
        WORLD,
        WORLDGUARD_REGION
    }

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final ContextType fromType;
    private final String fromGroup;
    private final ContextType toType;
    private final String toGroup;
    private final @Nullable String worldGuardRegionId;

    public GroupContextChangeEvent(
            @NotNull Player player,
            @NotNull ContextType fromType,
            @NotNull String fromGroup,
            @NotNull ContextType toType,
            @NotNull String toGroup,
            @Nullable String worldGuardRegionId
    ) {
        this.player = player;
        this.fromType = fromType;
        this.fromGroup = fromGroup;
        this.toType = toType;
        this.toGroup = toGroup;
        this.worldGuardRegionId = worldGuardRegionId;
    }

    @NotNull
    public Player getPlayer() {
        return player;
    }

    @NotNull
    public ContextType getFromType() {
        return fromType;
    }

    @NotNull
    public String getFromGroup() {
        return fromGroup;
    }

    @NotNull
    public ContextType getToType() {
        return toType;
    }

    @NotNull
    public String getToGroup() {
        return toGroup;
    }

    /**
     * Region id when the switch was evaluated inside a region; may be null.
     */
    @Nullable
    public String getWorldGuardRegionId() {
        return worldGuardRegionId;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static @NotNull HandlerList getHandlerList() {
        return HANDLERS;
    }
}
