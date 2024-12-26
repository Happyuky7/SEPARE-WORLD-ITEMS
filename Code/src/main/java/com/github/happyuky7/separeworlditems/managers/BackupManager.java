package com.github.happyuky7.separeworlditems.managers;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

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
            try {
                backupFolder.mkdirs();
            } catch (SecurityException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to create backup folder: " + backupFolder.getName(), e);
            }
        }

        // Initialize the backup queue with existing backups
        initializeBackupQueue();
    }

    /**
     * Initializes the backup queue with existing backups, maintaining the order
     * based on the last modified time.
     */
    private void initializeBackupQueue() {
        File[] backups = backupFolder.listFiles((dir, name) -> name.endsWith(".zip"));
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
     * Creates a ZIP backup of the specified directory.
     *
     * @param sourceFolder the folder to back up
     */
    public void createBackup(File sourceFolder) {
        if (!sourceFolder.exists() || !sourceFolder.isDirectory()) {
            plugin.getLogger().warning("Specified folder does not exist or is not a directory: " + sourceFolder.getName());
            return;
        }

        String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
        File backupFile = new File(backupFolder, sourceFolder.getName() + "_" + timestamp + ".zip");

        try (FileOutputStream fos = new FileOutputStream(backupFile);
             ZipOutputStream zos = new ZipOutputStream(fos)) {
            zipDirectory(sourceFolder, sourceFolder.getName(), zos);
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
            plugin.getLogger().log(Level.SEVERE, "Failed to create backup for: " + sourceFolder.getName(), e);
        }
    }

    /**
     * Recursively zips a directory.
     *
     * @param folder the folder to zip
     * @param basePath the base path to preserve folder structure
     * @param zos the ZIP output stream
     * @throws IOException if an I/O error occurs
     */
    private void zipDirectory(File folder, String basePath, ZipOutputStream zos) throws IOException {
        for (File file : Objects.requireNonNull(folder.listFiles())) {
            String entryName = basePath + File.separator + file.getName();
            if (file.isDirectory()) {
                zipDirectory(file, entryName, zos);
            } else {
                try (FileInputStream fis = new FileInputStream(file)) {
                    ZipEntry entry = new ZipEntry(entryName);
                    zos.putNextEntry(entry);

                    byte[] buffer = new byte[1024];
                    int length;
                    while ((length = fis.read(buffer)) > 0) {
                        zos.write(buffer, 0, length);
                    }
                    zos.closeEntry();
                }
            }
        }
    }

    /**
     * Starts the automatic backup task.
     *
     * @param sourceFolder the folder to back up
     */
    public void startAutoBackup(File sourceFolder) {
        plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, () -> createBackup(sourceFolder), 0L,
                backupInterval / 50);
    }
}
