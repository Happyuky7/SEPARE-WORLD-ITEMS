package com.github.happyuky7.separeWorldItems.storage.migration;

import com.github.happyuky7.separeWorldItems.SepareWorldItems;
import com.github.happyuky7.separeWorldItems.storage.PlayerDataScope;
import com.github.happyuky7.separeWorldItems.storage.PlayerDataStore;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.Objects;
import java.util.UUID;

/**
 * One-way import: reads existing YAML files and writes them into the selected store.
 */
public final class YamlToStoreMigration {

    private YamlToStoreMigration() {
    }

    public static void migrate(SepareWorldItems plugin, PlayerDataStore targetStore) {
        migrateScope(plugin, targetStore, PlayerDataScope.WORLD_GROUP, new File(plugin.getDataFolder(), "groups"));
        migrateScope(plugin, targetStore, PlayerDataScope.WORLDGUARD_GROUP, new File(plugin.getDataFolder(), "worldguard" + File.separator + "groups"));
    }

    private static void migrateScope(SepareWorldItems plugin, PlayerDataStore targetStore, PlayerDataScope scope, File rootDir) {
        if (!rootDir.exists() || !rootDir.isDirectory()) {
            return;
        }

        File[] groupDirs = rootDir.listFiles(File::isDirectory);
        if (groupDirs == null) {
            return;
        }

        for (File groupDir : groupDirs) {
            String groupName = groupDir.getName();
            for (File file : Objects.requireNonNull(groupDir.listFiles((dir, name) -> name.endsWith(".yml")))) {
                String base = file.getName().substring(0, file.getName().length() - 4);
                int lastDash = base.lastIndexOf('-');
                if (lastDash <= 0 || lastDash >= base.length() - 1) {
                    continue;
                }

                String playerName = base.substring(0, lastDash);
                String uuidStr = base.substring(lastDash + 1);
                UUID uuid;
                try {
                    uuid = UUID.fromString(uuidStr);
                } catch (IllegalArgumentException ignored) {
                    continue;
                }

                YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
                try {
                    targetStore.save(scope, groupName, uuid, playerName, config);
                } catch (Throwable t) {
                    plugin.getLogger().warning("[Storage] Failed migrating " + scope + "/" + groupName + "/" + file.getName() + ": " + t.getMessage());
                }
            }
        }
    }
}
