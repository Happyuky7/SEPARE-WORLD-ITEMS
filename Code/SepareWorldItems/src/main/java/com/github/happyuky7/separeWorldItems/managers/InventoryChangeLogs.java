package com.github.happyuky7.separeWorldItems.managers;

import com.github.happyuky7.separeWorldItems.SepareWorldItems;
import com.github.happyuky7.separeWorldItems.data.PlayerDataSections;
import com.github.happyuky7.separeWorldItems.storage.PlayerDataScope;
import com.github.happyuky7.separeWorldItems.storage.PlayerDataStore;
import com.github.happyuky7.separeWorldItems.storage.StorageType;
import com.github.happyuky7.separeWorldItems.storage.backends.MongoPlayerDataStore;
import com.github.happyuky7.separeWorldItems.storage.backends.RedisPlayerDataStore;
import com.github.happyuky7.separeWorldItems.storage.backends.SqlPlayerDataStore;
import com.github.happyuky7.separeWorldItems.utils.PluginSchedulers;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Inventory/data change logs.
 *
 * <p>Designed to record every persisted player data snapshot (inventory + status, etc.)
 * when using DB backends. YAML logging is optional and only recommended for small servers.</p>
 */
public final class InventoryChangeLogs {

    private static final DateTimeFormatter DAY_FMT = DateTimeFormatter.ISO_LOCAL_DATE;
    private static volatile boolean sqliteWarned = false;

    private InventoryChangeLogs() {
    }

    public static void onSaved(
            SepareWorldItems plugin,
            PlayerDataStore store,
            PlayerDataScope scope,
            String groupName,
            Player player,
            YamlConfiguration persistedConfig
    ) {
        try {
            if (!isEnabled(plugin, store)) {
                return;
            }

            long now = System.currentTimeMillis();
            boolean includePayload = plugin.getConfig().getBoolean("settings.change-logs.include-payload", true);
            String yaml;
            if (!includePayload) {
                yaml = "";
            } else {
                boolean captureAll = plugin.getConfig().getBoolean("settings.change-logs.capture-all-data", true);
                YamlConfiguration payloadCfg = captureAll ? PlayerDataSections.captureAll(plugin, player) : persistedConfig;
                yaml = payloadCfg.saveToString();
            }

            UUID playerUuid = player.getUniqueId();
            String playerName = player.getName();

            StorageType type = StorageType.fromConfig(plugin.getConfig().getString("storage.type", "YAML"));
            boolean remoteBackend = (type != StorageType.YAML && type != StorageType.SQLITE);

            if (store instanceof SqlPlayerDataStore sqlStore) {
                if (remoteBackend) {
                    PluginSchedulers.runAsync(plugin, () -> sqlStore.appendChangeLog(scope, groupName, playerUuid, playerName, now, yaml));
                } else {
                    sqlStore.appendChangeLog(scope, groupName, playerUuid, playerName, now, yaml);
                }
                return;
            }
            if (store instanceof MongoPlayerDataStore mongoStore) {
                if (remoteBackend) {
                    int retention = getRetentionDays(plugin);
                    PluginSchedulers.runAsync(plugin, () -> mongoStore.appendChangeLog(scope, groupName, playerUuid, playerName, now, yaml, retention));
                } else {
                    mongoStore.appendChangeLog(scope, groupName, playerUuid, playerName, now, yaml, getRetentionDays(plugin));
                }
                return;
            }
            if (store instanceof RedisPlayerDataStore redisStore) {
                if (remoteBackend) {
                    int retention = getRetentionDays(plugin);
                    PluginSchedulers.runAsync(plugin, () -> redisStore.appendChangeLog(scope, groupName, playerUuid, playerName, now, yaml, retention));
                } else {
                    redisStore.appendChangeLog(scope, groupName, playerUuid, playerName, now, yaml, getRetentionDays(plugin));
                }
                return;
            }

            // YAML/file-based backend: optional JSONL logs on disk.
            if (store.isFileBased()) {
                if (plugin.getConfig().getBoolean("settings.change-logs.yaml.enabled", false)) {
                    appendYamlFileLog(plugin, scope, groupName, playerUuid, playerName, now, yaml);
                }
            }
        } catch (Throwable t) {
            plugin.getLogger().warning("[ChangeLogs] Failed to append: " + t.getMessage());
        }
    }


    public static void startRetentionTask(SepareWorldItems plugin, PlayerDataStore store) {
        try {
            long periodTicks = 20L * 60L * 60L * 6L; // 6h
            long initialDelayTicks = 20L * 60L; // 1m
            Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
                try {
                    if (!isEnabled(plugin, store)) {
                        return;
                    }

                    int retentionDays = getRetentionDays(plugin);
                    long cutoffMs = System.currentTimeMillis() - (retentionDays * 86400L * 1000L);

                    if (store instanceof SqlPlayerDataStore sqlStore) {
                        sqlStore.purgeChangeLogsOlderThan(cutoffMs);
                    }

                    // YAML/file-based purge (delete old daily files)
                    if (store.isFileBased() && plugin.getConfig().getBoolean("settings.change-logs.yaml.enabled", false)) {
                        purgeYamlLogs(plugin, retentionDays);
                    }
                } catch (Throwable t) {
                    plugin.getLogger().warning("[ChangeLogs] Retention task failed: " + t.getMessage());
                }
            }, initialDelayTicks, periodTicks);
        } catch (Throwable t) {
            plugin.getLogger().warning("[ChangeLogs] Could not schedule retention task: " + t.getMessage());
        }
    }

    private static boolean isEnabled(SepareWorldItems plugin, PlayerDataStore store) {
        boolean global = plugin.getConfig().getBoolean("settings.change-logs.enabled", true);
        if (!global) {
            return false;
        }

        if (store.isFileBased()) {
            return plugin.getConfig().getBoolean("settings.change-logs.yaml.enabled", false);
        }

        StorageType type = StorageType.fromConfig(plugin.getConfig().getString("storage.type", "YAML"));
        if (type == StorageType.SQLITE) {
            boolean confirmed = plugin.getConfig().getBoolean("settings.change-logs.sqlite.confirmed", false);
            if (!confirmed) {
                // Avoid spamming
                if (!sqliteWarned) {
                    sqliteWarned = true;
                    plugin.getLogger().warning("[ChangeLogs] SQLite change-logs require confirmation. Set settings.change-logs.sqlite.confirmed: true to enable.");
                }
                return false;
            }
        }

        return true;
    }

    private static int getRetentionDays(SepareWorldItems plugin) {
        int days = plugin.getConfig().getInt("settings.change-logs.retention-days", 30);
        return Math.max(30, days); // guarantee >= 30 days by default
    }

    private static void appendYamlFileLog(
            SepareWorldItems plugin,
            PlayerDataScope scope,
            String groupName,
            UUID playerUuid,
            String playerName,
            long timestampMs,
            String yaml
    ) throws Exception {
        File dir = new File(plugin.getDataFolder(), "change-logs");
        if (!dir.exists() && !dir.mkdirs()) {
            return;
        }

        LocalDate day = Instant.ofEpochMilli(timestampMs).atZone(ZoneId.systemDefault()).toLocalDate();
        File out = new File(dir, DAY_FMT.format(day) + ".log");

        JSONObject obj = new JSONObject();
        obj.put("ts", timestampMs);
        obj.put("scope", scope.name());
        obj.put("group", groupName);
        obj.put("uuid", playerUuid.toString());
        obj.put("name", playerName);
        obj.put("yaml", yaml);

        try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(out, true), StandardCharsets.UTF_8))) {
            bw.write(obj.toString());
            bw.newLine();
        }
    }

    private static void purgeYamlLogs(SepareWorldItems plugin, int retentionDays) {
        File dir = new File(plugin.getDataFolder(), "change-logs");
        if (!dir.isDirectory()) {
            return;
        }

        File[] files = dir.listFiles((d, name) -> name != null && name.endsWith(".log"));
        if (files == null) {
            return;
        }

        long cutoffMs = System.currentTimeMillis() - (retentionDays * 86400L * 1000L);
        for (File f : files) {
            try {
                // Expect ISO date filename, fallback to lastModified
                String name = f.getName();
                long ts = f.lastModified();
                if (name.length() >= 10) {
                    String datePart = name.substring(0, 10);
                    try {
                        LocalDate d = LocalDate.parse(datePart);
                        ts = d.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
                    } catch (Exception ignored) {
                    }
                }
                if (ts < cutoffMs) {
                    // best effort
                    //noinspection ResultOfMethodCallIgnored
                    f.delete();
                }
            } catch (Throwable ignored) {
            }
        }
    }
}
