package com.github.happyuky7.separeworlditems.utils;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Level;

/**
 * Manages backups of plugin configuration files.
 */
public class BackupManager {

    private final JavaPlugin plugin;
    private final File backupFolder;
    private final int maxBackups;
    private final long backupInterval;
    private final Queue<File> backupQueue = new LinkedList<>();

    /**
     * Constructs a BackupManager with the specified parameters.
     *
     * @param plugin         the plugin instance
     * @param backupFolder   the folder where backups will be stored
     * @param maxBackups     the maximum number of backups to retain
     * @param backupInterval the interval between backups (in milliseconds)
     */
    public BackupManager(JavaPlugin plugin, File backupFolder, int maxBackups, long backupInterval) {
        this.plugin = plugin;
        this.backupFolder = backupFolder;
        this.maxBackups = maxBackups;
        this.backupInterval = backupInterval;

        if (!backupFolder.exists()) {
            backupFolder.mkdirs();
        }

        // Initialize the backup queue with existing backups
        initializeBackupQueue();
    }

    /**
     * Initializes the backup queue with existing backups, maintaining the order
     * based on the last modified time.
     */
    private void initializeBackupQueue() {
        File[] backups = backupFolder.listFiles((dir, name) -> name.endsWith(".bak"));
        if (backups != null) {
            Arrays.sort(backups, Comparator.comparingLong(File::lastModified));
            for (File backup : backups) {
                backupQueue.add(backup);
            }

            // Remove extra backups if they exceed the maximum allowed
            while (backupQueue.size() > maxBackups) {
                File oldestBackup = backupQueue.poll();
                if (oldestBackup != null && oldestBackup.exists()) {
                    oldestBackup.delete();
                    plugin.getLogger().info("Deleted extra backup during initialization: " + oldestBackup.getName());
                }
            }
        }
    }

    /**
     * Creates a backup of the specified file.
     *
     * @param file the file to back up
     */
    public void createBackup(File file) {
        if (!file.exists()) {
            plugin.getLogger().warning("File does not exist: " + file.getName());
            return;
        }

        String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
        File backupFile = new File(backupFolder, file.getName() + "_" + timestamp + ".bak");

        try {
            Files.copy(file.toPath(), backupFile.toPath());
            plugin.getLogger().info("Backup created: " + backupFile.getName());
            backupQueue.add(backupFile);

            // Remove old backups if the limit is exceeded
            if (backupQueue.size() > maxBackups) {
                File oldestBackup = backupQueue.poll();
                if (oldestBackup != null && oldestBackup.exists()) {
                    oldestBackup.delete();
                    plugin.getLogger().info("Deleted old backup: " + oldestBackup.getName());
                }
            }
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to create backup for: " + file.getName(), e);
        }
    }

    /**
     * Starts the automatic backup task.
     *
     * @param file the file to back up
     */
    public void startAutoBackup(File file) {
        plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, () -> createBackup(file), 0L,
                backupInterval / 50);
    }
}
