package com.github.happyuky7.separeWorldItems.utils;

import com.github.happyuky7.separeWorldItems.files.FileManager;
import com.github.happyuky7.separeWorldItems.storage.StorageType;
import org.bukkit.configuration.ConfigurationSection;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

public final class ConfigValidator {

    private ConfigValidator() {
    }

    public static void validate(FileManager config, Logger logger) {
        if (config == null || logger == null) {
            return;
        }

        // ---- storage ----
        try {
            StorageType storageType = StorageType.fromConfig(config.getString("storage.type", "YAML"));
            if (storageType != StorageType.YAML && storageType != StorageType.SQLITE) {
                logger.warning("[Config] Storage backend '" + storageType + "' is BETA. Make backups; unexpected bugs may occur.");

                if (config.getBoolean("integrations.worldguard.enabled", false)) {
                    logger.warning("[Config] WorldGuard regions + BETA DB storage is experimental; prefer YAML/SQLITE or disable regions if you see issues.");
                }
            }

            if (storageType == StorageType.MYSQL || storageType == StorageType.MARIADB) {
                String jdbcUrl = config.getString("storage.sql.jdbc-url", "");
                String host = config.getString("storage.sql.host", "");
                String database = config.getString("storage.sql.database", "");

                boolean hasJdbc = jdbcUrl != null && !jdbcUrl.isEmpty();
                boolean hasHostDb = host != null && !host.isEmpty() && database != null && !database.isEmpty();

                if (!hasJdbc && !hasHostDb) {
                    logger.warning("[Config] Missing SQL connection info. Set either storage.sql.jdbc-url OR storage.sql.host + storage.sql.database.");
                }

                if (hasJdbc && hasHostDb) {
                    logger.warning("[Config] Both storage.sql.jdbc-url and storage.sql.host/database are set. jdbc-url will be used.");
                }
            }
        } catch (Throwable ignored) {
        }

        // ---- groups ----
        List<String> groups = config.getStringList("groups");
        if (groups == null || groups.isEmpty()) {
            logger.warning("[Config] 'groups' list is empty. The plugin may fallback to 'default'.");
        }

        if (config.getBoolean("settings.options.default-group.enabled", false)) {
            String defaultGroup = config.getString("settings.options.default-group.group", "default");
            if (defaultGroup == null || defaultGroup.isEmpty()) {
                logger.warning("[Config] Default group is enabled but 'settings.options.default-group.group' is empty.");
            } else if (groups != null && !groups.isEmpty() && !groups.contains(defaultGroup)) {
                logger.warning("[Config] Default group '" + defaultGroup + "' is not present in 'groups'.");
            }
        }

        // ---- worlds ----
        ConfigurationSection worlds = config.getConfigurationSection("worlds");
        if (worlds != null) {
            for (String worldName : worlds.getKeys(false)) {
                String group = config.getString("worlds." + worldName);
                if (group == null || group.isEmpty()) {
                    continue;
                }
                if (groups != null && !groups.isEmpty() && !groups.contains(group)) {
                    logger.warning("[Config] World '" + worldName + "' references unknown group '" + group + "'.");
                }
            }
        }

        // ---- worldguard regions ----
        if (config.getBoolean("integrations.worldguard.enabled", false)) {
            List<String> regionGroups = config.getStringList("worldguard-regions.groups");
            if (regionGroups == null || regionGroups.isEmpty()) {
                logger.warning("[Config] WorldGuard is enabled but 'worldguard-regions.groups' is empty.");
            } else {
                Set<String> uniq = new HashSet<>(regionGroups);
                if (uniq.size() != regionGroups.size()) {
                    logger.warning("[Config] Duplicate entries found in 'worldguard-regions.groups'.");
                }
            }

            ConfigurationSection regionsSection = config.getConfigurationSection("worldguard-regions.regions");
            if (regionsSection == null) {
                logger.warning("[Config] WorldGuard is enabled but 'worldguard-regions.regions' section is missing.");
                return;
            }

            // Supports:
            // - flat: regions.<region> = <group>
            // - per-world: regions.<world>.<region> = <group>
            for (String key : regionsSection.getKeys(false)) {
                ConfigurationSection perWorld = regionsSection.getConfigurationSection(key);
                if (perWorld != null) {
                    // per-world mapping
                    for (String regionId : perWorld.getKeys(false)) {
                        String group = perWorld.getString(regionId);
                        validateRegionMapping(logger, regionGroups, key, regionId, group);
                    }
                } else {
                    // flat mapping
                    String group = regionsSection.getString(key);
                    validateRegionMapping(logger, regionGroups, null, key, group);
                }
            }
        }
    }

    private static void validateRegionMapping(Logger logger, List<String> regionGroups, String world, String regionId, String group) {
        if (regionId == null || regionId.isEmpty()) {
            return;
        }
        if (group == null || group.isEmpty()) {
            return;
        }
        if (regionGroups == null || regionGroups.isEmpty()) {
            return;
        }
        if (!regionGroups.contains(group)) {
            if (world != null) {
                logger.warning("[Config] WG region mapping '" + world + "." + regionId + "' points to group '" + group + "' which is not in 'worldguard-regions.groups'.");
            } else {
                logger.warning("[Config] WG region mapping '" + regionId + "' points to group '" + group + "' which is not in 'worldguard-regions.groups'.");
            }
        }
    }
}