package com.github.happyuky7.separeWorldItems.utils;

import com.github.happyuky7.separeWorldItems.SepareWorldItems;
import org.bukkit.Bukkit;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public final class PluginSchedulers {

    private PluginSchedulers() {
    }

    public static void runSync(SepareWorldItems plugin, Runnable runnable) {
        if (plugin.isFolia()) {
            try {
                plugin.getServer().getGlobalRegionScheduler().run(plugin, task -> runnable.run());
                return;
            } catch (Throwable ignored) {
            }
        }
        Bukkit.getScheduler().runTask(plugin, runnable);
    }

    public static void runSyncLater(SepareWorldItems plugin, Runnable runnable, long delayTicks) {
        if (plugin.isFolia()) {
            try {
                plugin.getServer().getGlobalRegionScheduler().runDelayed(plugin, task -> runnable.run(), delayTicks);
                return;
            } catch (Throwable ignored) {
            }
        }
        Bukkit.getScheduler().runTaskLater(plugin, runnable, delayTicks);
    }

    public static void runAsync(SepareWorldItems plugin, Runnable runnable) {
        if (plugin.isFolia()) {
            try {
                plugin.getServer().getAsyncScheduler().runNow(plugin, task -> runnable.run());
                return;
            } catch (Throwable ignored) {
            }
        }
        Bukkit.getScheduler().runTaskAsynchronously(plugin, runnable);
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
