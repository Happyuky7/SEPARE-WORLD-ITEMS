package com.github.happyuky7.swi.common.switching;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;

/**
 * Serializes switch operations per player so rapid world changes can never overlap.
 */
public final class SwitchQueue {

    private final ConcurrentMap<UUID, CompletableFuture<Void>> tails = new ConcurrentHashMap<>();

    public CompletableFuture<Void> enqueue(UUID uuid, Supplier<CompletableFuture<Void>> action) {
        for (;;) {
            CompletableFuture<Void> tail = tails.get(uuid);
            CompletableFuture<Void> base = tail != null ? tail : CompletableFuture.completedFuture(null);
            CompletableFuture<Void> next = base.exceptionally(ignored -> null).thenCompose(v -> {
                try {
                    CompletableFuture<Void> f = action.get();
                    return f != null ? f : CompletableFuture.<Void>completedFuture(null);
                } catch (Throwable t) {
                    CompletableFuture<Void> failed = new CompletableFuture<>();
                    failed.completeExceptionally(t);
                    return failed;
                }
            });
            boolean installed = tail == null ? tails.putIfAbsent(uuid, next) == null : tails.replace(uuid, tail, next);
            if (installed) {
                next.whenComplete((v, t) -> tails.remove(uuid, next));
                return next;
            }
        }
    }
}
