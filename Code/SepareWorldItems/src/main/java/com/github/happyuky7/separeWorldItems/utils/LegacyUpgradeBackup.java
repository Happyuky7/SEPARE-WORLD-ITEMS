package com.github.happyuky7.separeWorldItems.utils;

import com.github.happyuky7.separeWorldItems.SepareWorldItems;
import com.github.happyuky7.separeWorldItems.managers.BackupManager;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.io.File;

/**
 * Creates a one-time safety backup for very old installs before config/storage checks may stop the plugin.
 */
public final class LegacyUpgradeBackup {

    private static final PluginVersion THRESHOLD_DEV_104 = PluginVersion.parseOrZero("2.0.0-DEV-104");

    private LegacyUpgradeBackup() {
    }

    public static void maybeCreateLegacyBackup(@NotNull SepareWorldItems plugin) {
        try {
            File dataFolder = plugin.getDataFolder();
            if (!dataFolder.exists()) {
                return;
            }

            File stateFile = new File(dataFolder, "upgrade-state.yml");
            YamlConfiguration state = YamlConfiguration.loadConfiguration(stateFile);

            String current = plugin.getDescription().getVersion();
            String lastRun = state.getString("last-run-version", null);
            boolean legacyBackupDone = state.getBoolean("legacy-backup.done", false);

            // Try to infer previous version from config-version when state is missing.
            if (lastRun == null) {
                try {
                    lastRun = plugin.getConfig().getString("config-version", null);
                } catch (Throwable ignored) {
                }
            }

            boolean hasExistingData = new File(dataFolder, "groups").exists() || new File(dataFolder, "worldguard").exists();

            boolean shouldBackup = false;
            if (!legacyBackupDone) {
                if (lastRun != null) {
                    PluginVersion last = PluginVersion.parseOrZero(lastRun);
                    shouldBackup = last.compareTo(THRESHOLD_DEV_104) <= 0;
                } else {
                    // Unknown previous version; if there is existing data, do a one-time safety backup.
                    shouldBackup = hasExistingData;
                }
            }

            if (shouldBackup) {
                File outDir = new File(dataFolder, "backups" + File.separator + "legacyUpgrades");
                String fromSafe = sanitizeVersion(lastRun);
                String toSafe = sanitizeVersion(current);
                String prefix = "legacy_from_" + fromSafe + "_to_" + toSafe;
                File backup = BackupManager.createOneTimeBackup(plugin, dataFolder, outDir, prefix, false);
                if (backup != null) {
                    plugin.getLogger().warning("[Backups] Legacy upgrade backup created: " + backup.getName());
                    state.set("legacy-backup.done", true);
                    state.set("legacy-backup.from", lastRun);
                    state.set("legacy-backup.to", current);
                }
            }

            state.set("last-run-version", current);
            try {
                state.save(stateFile);
            } catch (Throwable ignored) {
            }

        } catch (Throwable ignored) {
        }
    }

    private static String sanitizeVersion(String v) {
        if (v == null || v.isBlank()) {
            return "unknown";
        }
        return v.trim().replaceAll("[^a-zA-Z0-9_\\-\\.]", "_");
    }
}
