package com.github.happyuky7.swi.api.event;

import com.github.happyuky7.swi.api.context.GroupContext;
import com.github.happyuky7.swi.api.data.DataNode;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Fired after a record was read from storage and before it is applied to the player.
 * Listeners may change {@link #data()} to alter what gets applied.
 */
public final class PlayerDataLoadEvent extends PlayerEvent {

    private final GroupContext context;
    private final DataNode data;

    public PlayerDataLoadEvent(@NotNull Object player, @NotNull UUID uuid, @NotNull String name,
                               @NotNull GroupContext context, @NotNull DataNode data) {
        super(player, uuid, name);
        this.context = context;
        this.data = data;
    }

    public @NotNull GroupContext context() {
        return context;
    }

    /**
     * @return the record about to be applied (mutable); empty when the player has no data in this context yet
     */
    public @NotNull DataNode data() {
        return data;
    }
}
