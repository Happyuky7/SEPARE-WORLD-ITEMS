package com.github.happyuky7.separeWorldItems.managers;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;

/**
 * Serializes per-player switch operations so rapid world/region transitions cannot overlap.
 */
public final class PlayerSwitchQueue {

    private PlayerSwitchQueue() {
    }

    private static final ConcurrentMap<UUID, CompletableFuture<Void>> TAIL_BY_PLAYER = new ConcurrentHashMap<>();

    public static CompletableFuture<Void> enqueue(UUID uuid, Supplier<CompletableFuture<Void>> action) {
        if (uuid == null || action == null) {
            return CompletableFuture.completedFuture(null);
        }

        for (;;) {
            CompletableFuture<Void> tail = TAIL_BY_PLAYER.get(uuid);
            CompletableFuture<Void> base = (tail != null) ? tail : CompletableFuture.completedFuture(null);

            CompletableFuture<Void> next = base
                    .exceptionally(ignored -> null)
                    .thenCompose(v -> {
                        try {
                            CompletableFuture<Void> f = action.get();
                            return (f != null) ? f : CompletableFuture.completedFuture(null);
                        } catch (Throwable t) {
                            CompletableFuture<Void> failed = new CompletableFuture<>();
                            failed.completeExceptionally(t);
                            return failed;
                        }
                    });

            // best-effort cleanup to prevent unbounded growth
            next.whenComplete((v, t) -> TAIL_BY_PLAYER.remove(uuid, next));

            if (tail == null) {
                if (TAIL_BY_PLAYER.putIfAbsent(uuid, next) == null) {
                    return next;
                }
            } else {
                if (TAIL_BY_PLAYER.replace(uuid, tail, next)) {
                    return next;
                }
            }
        }
    }
}
