package com.github.happyuky7.separeWorldItems.storage.migration;

import com.github.happyuky7.separeWorldItems.SepareWorldItems;
import com.github.happyuky7.separeWorldItems.storage.PlayerDataScope;
import com.github.happyuky7.separeWorldItems.storage.PlayerDataStore;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Migrates player data between any two {@link PlayerDataStore} backends.
 *
 * <p>Migration preserves scope and group names and copies the YAML payload.</p>
 */
public final class StoreToStoreMigration {

    private StoreToStoreMigration() {
    }

    public static void migrate(@NotNull SepareWorldItems plugin,
                               @NotNull PlayerDataStore source,
                               @NotNull PlayerDataStore target,
                               boolean overwriteTarget) {
        migrateScope(plugin, source, target, PlayerDataScope.WORLD_GROUP, overwriteTarget);
        migrateScope(plugin, source, target, PlayerDataScope.WORLDGUARD_GROUP, overwriteTarget);
    }

    private static void migrateScope(SepareWorldItems plugin,
                                     PlayerDataStore source,
                                     PlayerDataStore target,
                                     PlayerDataScope scope,
                                     boolean overwriteTarget) {

        AtomicLong migrated = new AtomicLong(0);
        AtomicLong skipped = new AtomicLong(0);
        AtomicLong failed = new AtomicLong(0);

        try {
            source.forEachEntry(scope, (groupName, playerUuid, playerName) -> {
                try {
                    if (!overwriteTarget && target.exists(scope, groupName, playerUuid)) {
                        skipped.incrementAndGet();
                        return;
                    }

                    YamlConfiguration cfg = source.load(scope, groupName, playerUuid);
                    if (cfg == null) {
                        cfg = new YamlConfiguration();
                    }

                    String name = normalizeName(playerName, playerUuid);
                    target.save(scope, groupName, playerUuid, name, cfg);
                    migrated.incrementAndGet();
                } catch (Throwable t) {
                    failed.incrementAndGet();
                }
            });
        } catch (UnsupportedOperationException uoe) {
            plugin.getLogger().warning("[Storage] Source backend cannot enumerate entries for migration: " + uoe.getMessage());
            return;
        }

        plugin.getLogger().info("[Storage] Migration scope=" + scope.name() + " migrated=" + migrated.get() + " skipped=" + skipped.get() + " failed=" + failed.get());
    }

    private static String normalizeName(String playerName, UUID uuid) {
        if (playerName != null && !playerName.isBlank()) {
            return playerName;
        }
        return uuid.toString();
    }
}
