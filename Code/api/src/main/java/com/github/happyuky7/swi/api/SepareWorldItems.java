package com.github.happyuky7.swi.api;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Static entry point to the SepareWorldItems API.
 *
 * <p>Works the same on every platform (Bukkit/Spigot/Paper/Folia plugin and Fabric mod):
 * <pre>{@code
 * SepareWorldItemsApi api = SepareWorldItems.get();
 * api.events().subscribe(GroupSwitchPostEvent.class, e -> ...);
 * }</pre>
 *
 * <p>The implementation registers itself while the plugin/mod is starting, so call {@link #get()}
 * from your own enable/initialize logic (declare SepareWorldItems as a dependency so it loads first),
 * or use {@link #getIfLoaded()} when the dependency is optional.</p>
 */
public final class SepareWorldItems {

    private static volatile SepareWorldItemsApi instance;

    private SepareWorldItems() {
    }

    /**
     * @return the running API implementation
     * @throws IllegalStateException if SepareWorldItems is not loaded (yet)
     */
    public static @NotNull SepareWorldItemsApi get() {
        SepareWorldItemsApi api = instance;
        if (api == null) {
            throw new IllegalStateException("SepareWorldItems is not loaded. "
                    + "Add it as a dependency (plugin.yml depend / fabric.mod.json depends) and call the API after it is enabled.");
        }
        return api;
    }

    /**
     * @return the running API implementation, or null when SepareWorldItems is not installed/enabled
     */
    public static @Nullable SepareWorldItemsApi getIfLoaded() {
        return instance;
    }

    public static boolean isLoaded() {
        return instance != null;
    }

    @ApiStatus.Internal
    public static void register(@NotNull SepareWorldItemsApi api) {
        if (instance != null && instance != api) {
            throw new IllegalStateException("A SepareWorldItems API implementation is already registered: " + instance.platform());
        }
        instance = api;
    }

    @ApiStatus.Internal
    public static void unregister() {
        instance = null;
    }
}
