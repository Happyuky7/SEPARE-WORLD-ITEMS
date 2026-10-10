package com.github.happyuky7.swi.api.event;

import com.github.happyuky7.swi.api.context.GroupContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Fired right before a player's data is swapped from one context to another.
 *
 * <p>Cancelling it keeps the player's current data (nothing is saved, cleared or loaded) and the
 * player keeps being tracked in the old context. The world change/teleport itself is not undone.</p>
 */
public final class GroupSwitchPreEvent extends PlayerEvent implements Cancellable {

    private final GroupContext from;
    private final GroupContext to;
    private final String regionId;
    private boolean cancelled;

    public GroupSwitchPreEvent(@NotNull Object player, @NotNull UUID uuid, @NotNull String name,
                               @NotNull GroupContext from, @NotNull GroupContext to, @Nullable String regionId) {
        super(player, uuid, name);
        this.from = from;
        this.to = to;
        this.regionId = regionId;
    }

    public @NotNull GroupContext from() {
        return from;
    }

    public @NotNull GroupContext to() {
        return to;
    }

    /**
     * @return the region id that caused the switch when {@link #to()} is a region context, else null
     */
    public @Nullable String regionId() {
        return regionId;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
}
