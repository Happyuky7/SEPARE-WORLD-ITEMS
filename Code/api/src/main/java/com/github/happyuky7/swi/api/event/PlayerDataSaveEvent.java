package com.github.happyuky7.swi.api.event;

import com.github.happyuky7.swi.api.context.GroupContext;
import com.github.happyuky7.swi.api.data.DataNode;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Fired after the player's state was written into {@link #data()} and before it is persisted.
 * Listeners may add or change values; changes are saved.
 */
public final class PlayerDataSaveEvent extends PlayerEvent {

    private final GroupContext context;
    private final DataNode data;

    public PlayerDataSaveEvent(@NotNull Object player, @NotNull UUID uuid, @NotNull String name,
                               @NotNull GroupContext context, @NotNull DataNode data) {
        super(player, uuid, name);
        this.context = context;
        this.data = data;
    }

    public @NotNull GroupContext context() {
        return context;
    }

    /**
     * @return the live record about to be saved (mutable)
     */
    public @NotNull DataNode data() {
        return data;
    }
}
