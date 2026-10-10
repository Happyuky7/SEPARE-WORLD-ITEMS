package com.github.happyuky7.swi.api.event;

/**
 * Handle returned by {@link EventBus#subscribe}. Call {@link #unsubscribe()} when your plugin/mod disables.
 */
public interface Subscription {

    void unsubscribe();

    boolean isActive();
}
