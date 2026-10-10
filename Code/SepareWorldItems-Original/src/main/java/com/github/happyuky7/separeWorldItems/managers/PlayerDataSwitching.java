package com.github.happyuky7.separeWorldItems.managers;

import com.github.happyuky7.separeWorldItems.SepareWorldItems;
import com.github.happyuky7.separeWorldItems.data.PlayerDataSections;
import com.github.happyuky7.separeWorldItems.storage.PlayerDataScope;
import com.github.happyuky7.separeWorldItems.storage.PlayerDataStore;
import com.github.happyuky7.separeWorldItems.storage.StorageType;
import com.github.happyuky7.separeWorldItems.utils.PluginSchedulers;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class PlayerDataSwitching {

    private PlayerDataSwitching() {
    }

    public static boolean isRemoteBackend(SepareWorldItems plugin) {
        StorageType type = StorageType.fromConfig(plugin.getConfig().getString("storage.type", "YAML"));
        return type != StorageType.YAML && type != StorageType.SQLITE;
    }

    /**
     * Switches player data context from (fromScope, fromGroup) -> (toScope, toGroup).
     *
     * <p>For remote backends (MYSQL/MARIADB/MONGODB/REDIS), DB IO runs async; Bukkit state application runs sync.
     * For YAML/SQLITE, runs synchronously and returns an already-completed future.</p>
     */
    public static CompletableFuture<Void> switchContext(
            SepareWorldItems plugin,
            Player player,
            PlayerDataScope fromScope,
            String fromGroup,
            PlayerDataScope toScope,
            String toGroup
    ) {
        PlayerDataStore store = plugin.getPlayerDataStore();
        UUID uuid = player.getUniqueId();
        String name = player.getName();

        boolean remote = isRemoteBackend(plugin);
        if (!remote) {
            // Synchronous path (fast for YAML/SQLITE)
            YamlConfiguration fromConfig = store.load(fromScope, fromGroup, uuid);
            PlayerDataSections.save(plugin, player, fromScope, fromGroup, fromConfig);
            store.save(fromScope, fromGroup, uuid, name, fromConfig);
            InventoryChangeLogs.onSaved(plugin, store, fromScope, fromGroup, player, fromConfig);

            PlayerDataManager.cleardataState(player);

            YamlConfiguration toConfig = store.load(toScope, toGroup, uuid);
            PlayerDataSections.load(plugin, player, toScope, toGroup, toConfig);
            return CompletableFuture.completedFuture(null);
        }

        // Remote backend path (async IO + sync apply)
        CompletableFuture<YamlConfiguration> fromLoadF = PluginSchedulers.supplyAsync(plugin, () -> store.load(fromScope, fromGroup, uuid));
        CompletableFuture<YamlConfiguration> toLoadF = PluginSchedulers.supplyAsync(plugin, () -> store.load(toScope, toGroup, uuid));

        // Wait for BOTH configs, then do: save snapshot -> clear -> apply destination (sync)
        CompletableFuture<YamlConfiguration> applyF = fromLoadF.thenCombine(toLoadF, (fromCfg, toCfg) -> new YamlConfiguration[]{fromCfg, toCfg})
            .thenCompose(pair -> PluginSchedulers.supplySync(plugin, () -> {
                YamlConfiguration fromCfg = pair[0];
                YamlConfiguration toCfg = pair[1];

                PlayerDataSections.save(plugin, player, fromScope, fromGroup, fromCfg);
                PlayerDataManager.cleardataState(player);
                PlayerDataSections.load(plugin, player, toScope, toGroup, toCfg);
                return fromCfg;
            }));

        // Persist snapshot (async) then append logs (sync; logs implementation will offload DB writes)
        CompletableFuture<Void> saveF = applyF
            .thenCompose(fromCfg -> PluginSchedulers.runAsyncFuture(plugin, () -> store.save(fromScope, fromGroup, uuid, name, fromCfg))
                .thenCompose(v -> PluginSchedulers.runSyncFuture(plugin, () -> InventoryChangeLogs.onSaved(plugin, store, fromScope, fromGroup, player, fromCfg))
                )
            );

        return saveF;
    }

    /**
     * Reloads data for the same scope+group (uses PlayerDataSections.reload).
     * For remote backends, DB IO runs async.
     */
    public static CompletableFuture<Void> reloadContext(
            SepareWorldItems plugin,
            Player player,
            PlayerDataScope scope,
            String groupName
    ) {
        PlayerDataStore store = plugin.getPlayerDataStore();
        UUID uuid = player.getUniqueId();

        boolean remote = isRemoteBackend(plugin);
        if (!remote) {
            YamlConfiguration cfg = store.load(scope, groupName, uuid);
            PlayerDataSections.reload(plugin, player, scope, groupName, cfg);
            return CompletableFuture.completedFuture(null);
        }

        return PluginSchedulers.supplyAsync(plugin, () -> store.load(scope, groupName, uuid))
                .thenCompose(cfg -> PluginSchedulers.runSyncFuture(plugin, () -> PlayerDataSections.reload(plugin, player, scope, groupName, cfg)));
    }
}
