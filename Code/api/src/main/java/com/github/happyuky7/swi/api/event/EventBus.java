package com.github.happyuky7.swi.api.event;

import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

/**
 * Platform-independent event bus. Listeners are called on the server (main/region) thread,
 * in {@link EventPriority} order; a listener throwing an exception is logged and skipped.
 *
 * <pre>{@code
 * Subscription sub = SepareWorldItems.get().events().subscribe(GroupSwitchPreEvent.class, e -> {
 *     if (e.to().group().equals("minigames") && isInCombat(e.playerUuid())) e.setCancelled(true);
 * });
 * // later: sub.unsubscribe();
 * }</pre>
 *
 * Subscribing to a supertype (e.g. {@link PlayerEvent}) receives every subtype.
 */
public interface EventBus {

    @NotNull <E extends SwiEvent> Subscription subscribe(@NotNull Class<E> type, @NotNull Consumer<? super E> listener);

    @NotNull <E extends SwiEvent> Subscription subscribe(@NotNull Class<E> type, @NotNull EventPriority priority, @NotNull Consumer<? super E> listener);

    /**
     * Dispatches an event to its listeners.
     *
     * @return the same event, so callers can read {@link Cancellable#isCancelled()}
     */
    @NotNull <E extends SwiEvent> E post(@NotNull E event);
}
