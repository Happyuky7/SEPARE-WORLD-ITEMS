package com.github.happyuky7.swi.common.api;

import com.github.happyuky7.swi.api.context.GroupContext;
import com.github.happyuky7.swi.api.data.DataNode;
import com.github.happyuky7.swi.api.data.PlayerDataService;
import com.github.happyuky7.swi.api.data.PlayerSnapshot;
import com.github.happyuky7.swi.common.storage.PlayerDataStore;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

/**
 * {@link PlayerDataService} reading from a common {@link PlayerDataStore}.
 */
public final class StorePlayerDataService implements PlayerDataService {

    private final Supplier<PlayerDataStore> store;
    private final Executor async;

    public StorePlayerDataService(Supplier<PlayerDataStore> store, Executor async) {
        this.store = store;
        this.async = async;
    }

    @Override
    public @NotNull CompletableFuture<Optional<PlayerSnapshot>> load(@NotNull UUID player, @NotNull GroupContext context) {
        return CompletableFuture.supplyAsync(() -> {
            PlayerDataStore st = store.get();
            if (!st.exists(context, player)) {
                return Optional.<PlayerSnapshot>empty();
            }
            DataNode data = st.load(context, player);
            return Optional.of(new PlayerSnapshot(player, null, context, data));
        }, async);
    }

    @Override
    public @NotNull CompletableFuture<Boolean> exists(@NotNull UUID player, @NotNull GroupContext context) {
        return CompletableFuture.supplyAsync(() -> store.get().exists(context, player), async);
    }

    @Override
    public @NotNull CompletableFuture<Optional<UUID>> resolveUuid(@NotNull String playerName, @NotNull GroupContext context) {
        return CompletableFuture.supplyAsync(() -> Optional.ofNullable(store.get().resolveUuidByName(context, playerName)), async);
    }
}
