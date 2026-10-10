package com.github.happyuky7.swi.api.event;

/**
 * Listener order: LOWEST runs first, MONITOR runs last and must not change the outcome.
 */
public enum EventPriority {
    LOWEST,
    LOW,
    NORMAL,
    HIGH,
    HIGHEST,
    MONITOR
}
