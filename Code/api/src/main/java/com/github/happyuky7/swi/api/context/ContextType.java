package com.github.happyuky7.swi.api.context;

/**
 * Where a group comes from.
 */
public enum ContextType {
    /** Group assigned to a world/dimension ({@code worlds:} in the config). */
    WORLD,
    /** Group assigned to a region (WorldGuard regions on Bukkit). */
    REGION
}
