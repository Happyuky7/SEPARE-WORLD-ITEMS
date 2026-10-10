package com.github.happyuky7.swi.common.event;

import com.github.happyuky7.swi.api.event.EventBus;
import com.github.happyuky7.swi.api.event.EventPriority;
import com.github.happyuky7.swi.api.event.Subscription;
import com.github.happyuky7.swi.api.event.SwiEvent;
import com.github.happyuky7.swi.common.platform.SwiLogger;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public final class SimpleEventBus implements EventBus {

    private final SwiLogger logger;
    private final List<Registration<?>> registrations = new CopyOnWriteArrayList<>();

    public SimpleEventBus(SwiLogger logger) {
        this.logger = logger;
    }

    @Override
    public @NotNull <E extends SwiEvent> Subscription subscribe(@NotNull Class<E> type, @NotNull Consumer<? super E> listener) {
        return subscribe(type, EventPriority.NORMAL, listener);
    }

    @Override
    public @NotNull <E extends SwiEvent> Subscription subscribe(@NotNull Class<E> type, @NotNull EventPriority priority, @NotNull Consumer<? super E> listener) {
        Registration<E> reg = new Registration<>(Objects.requireNonNull(type), Objects.requireNonNull(priority), Objects.requireNonNull(listener));
        synchronized (registrations) {
            registrations.add(reg);
            // Stable sort: same priority keeps subscription order.
            registrations.sort(Comparator.comparingInt(r -> r.priority.ordinal()));
        }
        return reg;
    }

    @Override
    public @NotNull <E extends SwiEvent> E post(@NotNull E event) {
        for (Registration<?> reg : registrations) {
            if (reg.type.isInstance(event)) {
                try {
                    reg.dispatch(event);
                } catch (Throwable t) {
                    logger.error("[API] Listener " + reg.listener.getClass().getName() + " failed on " + event.getClass().getSimpleName(), t);
                }
            }
        }
        return event;
    }

    private final class Registration<E extends SwiEvent> implements Subscription {
        private final Class<E> type;
        private final EventPriority priority;
        private final Consumer<? super E> listener;

        private Registration(Class<E> type, EventPriority priority, Consumer<? super E> listener) {
            this.type = type;
            this.priority = priority;
            this.listener = listener;
        }

        private void dispatch(Object event) {
            listener.accept(type.cast(event));
        }

        @Override
        public void unsubscribe() {
            registrations.remove(this);
        }

        @Override
        public boolean isActive() {
            return registrations.contains(this);
        }
    }
}
