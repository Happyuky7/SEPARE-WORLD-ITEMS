package com.github.happyuky7.separeWorldItems.utils;

import com.github.happyuky7.separeWorldItems.SepareWorldItems;
import org.bukkit.Bukkit;

import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class PluginSchedulers {

    private PluginSchedulers() {
    }

    public static void runSync(SepareWorldItems plugin, Runnable runnable) {
        if (plugin.isFolia() && tryFoliaGlobalRun(plugin, runnable)) {
            return;
        }
        Bukkit.getScheduler().runTask(plugin, runnable);
    }

    public static void runSyncLater(SepareWorldItems plugin, Runnable runnable, long delayTicks) {
        if (plugin.isFolia() && tryFoliaGlobalRunDelayed(plugin, runnable, delayTicks)) {
            return;
        }
        Bukkit.getScheduler().runTaskLater(plugin, runnable, delayTicks);
    }

    public static void runAsync(SepareWorldItems plugin, Runnable runnable) {
        if (plugin.isFolia() && tryFoliaAsyncRunNow(plugin, runnable)) {
            return;
        }
        Bukkit.getScheduler().runTaskAsynchronously(plugin, runnable);
    }

    public static boolean tryFoliaGlobalRunAtFixedRate(SepareWorldItems plugin, Runnable runnable, long initialDelayTicks, long periodTicks) {
        if (!plugin.isFolia()) {
            return false;
        }
        try {
            Object scheduler = getScheduler(plugin, "getGlobalRegionScheduler");
            Method m = scheduler.getClass().getMethod("runAtFixedRate", org.bukkit.plugin.Plugin.class, Consumer.class, long.class, long.class);
            m.invoke(scheduler, plugin, (Consumer<Object>) task -> runnable.run(), initialDelayTicks, periodTicks);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean tryFoliaGlobalRun(SepareWorldItems plugin, Runnable runnable) {
        try {
            Object scheduler = getScheduler(plugin, "getGlobalRegionScheduler");
            Method m = scheduler.getClass().getMethod("run", org.bukkit.plugin.Plugin.class, Consumer.class);
            m.invoke(scheduler, plugin, (Consumer<Object>) task -> runnable.run());
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean tryFoliaGlobalRunDelayed(SepareWorldItems plugin, Runnable runnable, long delayTicks) {
        try {
            Object scheduler = getScheduler(plugin, "getGlobalRegionScheduler");
            Method m = scheduler.getClass().getMethod("runDelayed", org.bukkit.plugin.Plugin.class, Consumer.class, long.class);
            m.invoke(scheduler, plugin, (Consumer<Object>) task -> runnable.run(), delayTicks);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean tryFoliaAsyncRunNow(SepareWorldItems plugin, Runnable runnable) {
        try {
            Object scheduler = getScheduler(plugin, "getAsyncScheduler");
            Method m = scheduler.getClass().getMethod("runNow", org.bukkit.plugin.Plugin.class, Consumer.class);
            m.invoke(scheduler, plugin, (Consumer<Object>) task -> runnable.run());
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static Object getScheduler(SepareWorldItems plugin, String methodName) throws Exception {
        Object server = plugin.getServer();
        Method m = server.getClass().getMethod(methodName);
        return m.invoke(server);
    }

    public static CompletableFuture<Void> runAsyncFuture(SepareWorldItems plugin, Runnable runnable) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        runAsync(plugin, () -> {
            try {
                runnable.run();
                future.complete(null);
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        });
        return future;
    }

    public static <T> CompletableFuture<T> supplyAsync(SepareWorldItems plugin, Supplier<T> supplier) {
        CompletableFuture<T> future = new CompletableFuture<>();
        runAsync(plugin, () -> {
            try {
                future.complete(supplier.get());
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        });
        return future;
    }

    public static CompletableFuture<Void> runSyncFuture(SepareWorldItems plugin, Runnable runnable) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        runSync(plugin, () -> {
            try {
                runnable.run();
                future.complete(null);
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        });
        return future;
    }

    public static <T> CompletableFuture<T> supplySync(SepareWorldItems plugin, Supplier<T> supplier) {
        CompletableFuture<T> future = new CompletableFuture<>();
        runSync(plugin, () -> {
            try {
                future.complete(supplier.get());
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        });
        return future;
    }
}
