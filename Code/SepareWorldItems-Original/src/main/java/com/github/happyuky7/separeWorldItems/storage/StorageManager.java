package com.github.happyuky7.separeWorldItems.storage;

import com.github.happyuky7.separeWorldItems.SepareWorldItems;
import com.github.happyuky7.separeWorldItems.storage.backends.MongoPlayerDataStore;
import com.github.happyuky7.separeWorldItems.storage.backends.RedisPlayerDataStore;
import com.github.happyuky7.separeWorldItems.storage.backends.SqlPlayerDataStore;
import com.github.happyuky7.separeWorldItems.storage.backends.YamlFilePlayerDataStore;
import com.github.happyuky7.separeWorldItems.storage.migration.StoreToStoreMigration;
import com.github.happyuky7.separeWorldItems.storage.migration.YamlToStoreMigration;
import com.github.happyuky7.separeWorldItems.managers.BackupManager;

import java.io.File;
import java.util.logging.Logger;

public final class StorageManager {

    private StorageManager() {
    }

    public static PlayerDataStore createAndMaybeMigrate(SepareWorldItems plugin) {
        Logger logger = plugin.getLogger();
        StorageType type = getConfiguredType(plugin);

        PlayerDataStore store = createStore(plugin, type);

        logger.info("[Storage] Using backend: " + type);

        // New migration format (any -> any)
        boolean migrationEnabled = plugin.getConfig().getBoolean("storage.migration.enabled", false);
        if (migrationEnabled) {
            StorageType from = StorageType.fromConfig(plugin.getConfig().getString("storage.migration.from", ""));
            boolean overwrite = plugin.getConfig().getBoolean("storage.migration.overwrite", false);

            if (from == type) {
                logger.warning("[Storage] Migration skipped: source and target are the same (" + type + ")");
            } else {
                try {
                    logger.warning("[Storage] Migration enabled: " + from + " -> " + type + " (overwrite=" + overwrite + ")");
                    PlayerDataStore source = createStore(plugin, from);
                    try {
                        // One-time safety backup before migration
                        try {
                            File outDir = new File(plugin.getDataFolder(), "backups" + File.separator + "migrations");
                            String prefix = "migration_" + from.name() + "_to_" + type.name();
                            boolean includeSnapshot = (from != StorageType.YAML && from != StorageType.SQLITE);
                            BackupManager.createOneTimeBackup(plugin, plugin.getDataFolder(), outDir, prefix, includeSnapshot, source, prefix);
                        } catch (Throwable t) {
                            logger.warning("[Backups] Migration backup failed: " + t.getMessage());
                        }

                        StoreToStoreMigration.migrate(plugin, source, store, overwrite);
                        logger.info("[Storage] Migration completed.");
                    } finally {
                        try {
                            source.close();
                        } catch (Throwable ignored) {
                        }
                    }
                } catch (Throwable t) {
                    logger.warning("[Storage] Migration failed: " + t.getMessage());
                }
            }
        } else {
            // Legacy migration flag (YAML -> selected backend)
            boolean migrate = plugin.getConfig().getBoolean("storage.migrate-from-yaml", false);
            if (migrate && type != StorageType.YAML) {
                try {
                    logger.warning("[Storage] Migration enabled (legacy): importing existing YAML files into " + type + "...");
                    // Use store-to-store migration when possible; fall back to file migration helper.
                    try {
                        PlayerDataStore yamlSource = new YamlFilePlayerDataStore(plugin);
                        StoreToStoreMigration.migrate(plugin, yamlSource, store, false);
                    } catch (Throwable ignored) {
                        YamlToStoreMigration.migrate(plugin, store);
                    }
                    logger.info("[Storage] Migration completed.");
                } catch (Throwable t) {
                    logger.warning("[Storage] Migration failed: " + t.getMessage());
                }
            }
        }

        return store;
    }

    public static StorageType getConfiguredType(SepareWorldItems plugin) {
        return StorageType.fromConfig(plugin.getConfig().getString("storage.type", "YAML"));
    }

    public static PlayerDataStore createStore(SepareWorldItems plugin, StorageType type) {
        return switch (type) {
            case SQLITE, MYSQL, MARIADB -> SqlPlayerDataStore.create(plugin, type);
            case MONGODB -> MongoPlayerDataStore.create(plugin);
            case REDIS -> RedisPlayerDataStore.create(plugin);
            case YAML -> new YamlFilePlayerDataStore(plugin);
        };
    }
}
