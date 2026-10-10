package com.github.happyuky7.swi.api.event;

import com.github.happyuky7.swi.api.context.GroupContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Fired after a player's data was swapped to a new context (the new data is already applied).
 */
public final class GroupSwitchPostEvent extends PlayerEvent {

    private final GroupContext from;
    private final GroupContext to;
    private final String regionId;

    public GroupSwitchPostEvent(@NotNull Object player, @NotNull UUID uuid, @NotNull String name,
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

    public @Nullable String regionId() {
        return regionId;
    }
}
