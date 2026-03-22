package com.github.happyuky7.separeWorldItems.managers;

import com.github.happyuky7.separeWorldItems.SepareWorldItems;
import com.github.happyuky7.separeWorldItems.storage.PlayerDataScope;
import com.github.happyuky7.separeWorldItems.storage.PlayerDataStore;
import com.github.happyuky7.separeWorldItems.storage.StorageType;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
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

        File snapshotDir = null;
        boolean includeRemoteSnapshot = shouldIncludeRemoteSnapshot(plugin, false);
        if (includeRemoteSnapshot) {
            snapshotDir = createRemoteSnapshotExport(plugin, null, "auto");
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
        } finally {
            if (snapshotDir != null) {
                deleteRecursively(snapshotDir.toPath());
            }
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
    public static void zipDirectory(File folder, String basePath, ZipOutputStream zos) throws IOException {
        for (File file : Objects.requireNonNull(folder.listFiles())) {
            if (file.isDirectory()) {
                String name = file.getName();
                if ("backups".equalsIgnoreCase(name) || "forceBackups".equalsIgnoreCase(name)) {
                    continue;
                }
            }
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

    public static String forebackupname = "";

    /**
     * Static method to create a force backup in the "forceBackups" folder.
     * This method does not require an instance of BackupManager.
     *
     * @param plugin the plugin instance
     * @param sourceFolder the folder to back up
     */
    public static void createForceBackup(JavaPlugin plugin, File sourceFolder) {
        // Get the forceBackups directory
        File forceBackupFolder = new File(plugin.getDataFolder(), "forceBackups");
        if (!forceBackupFolder.exists()) {
            if (!forceBackupFolder.mkdirs()) {
                plugin.getLogger().warning("Failed to create forceBackups directory.");
                return;
            }
        }

        // Create the backup using the BackupManager's createBackup method
        File snapshotDir = null;
        boolean includeRemoteSnapshot = shouldIncludeRemoteSnapshot(plugin, true);
        if (includeRemoteSnapshot) {
            snapshotDir = createRemoteSnapshotExport(plugin, null, "force");
        }

        String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
        File backupFile = new File(forceBackupFolder, sourceFolder.getName() + "_forceBackup_" + timestamp + ".zip");

        try (FileOutputStream fos = new FileOutputStream(backupFile);
             ZipOutputStream zos = new ZipOutputStream(fos)) {
            zipDirectory(sourceFolder, sourceFolder.getName(), zos);
            plugin.getLogger().info("Force backup created: " + backupFile.getName());
            forebackupname = backupFile.getName();
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to create force backup for: " + sourceFolder.getName(), e);
        } finally {
            if (snapshotDir != null) {
                deleteRecursively(snapshotDir.toPath());
            }
        }
    }

    /**
     * Creates a one-time backup zip in the given destination folder.
     * Intended for migrations / upgrade safety backups.
     */
    public static @Nullable File createOneTimeBackup(JavaPlugin plugin, File sourceFolder, File destinationFolder, String fileNamePrefix, boolean includeRemoteSnapshot) {
        return createOneTimeBackup(plugin, sourceFolder, destinationFolder, fileNamePrefix, includeRemoteSnapshot, null, null);
    }

    public static @Nullable File createOneTimeBackup(JavaPlugin plugin,
                                                    File sourceFolder,
                                                    File destinationFolder,
                                                    String fileNamePrefix,
                                                    boolean includeRemoteSnapshot,
                                                    @Nullable PlayerDataStore snapshotStore,
                                                    @Nullable String snapshotTag) {
        if (!sourceFolder.exists() || !sourceFolder.isDirectory()) {
            return null;
        }

        if (!destinationFolder.exists() && !destinationFolder.mkdirs()) {
            plugin.getLogger().warning("Failed to create backup destination folder: " + destinationFolder.getAbsolutePath());
            return null;
        }

        File snapshotDir = null;
        if (includeRemoteSnapshot) {
            snapshotDir = createRemoteSnapshotExport(plugin, snapshotStore, snapshotTag);
        }

        String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
        File backupFile = new File(destinationFolder, fileNamePrefix + "_" + timestamp + ".zip");

        try (FileOutputStream fos = new FileOutputStream(backupFile);
             ZipOutputStream zos = new ZipOutputStream(fos)) {
            zipDirectory(sourceFolder, sourceFolder.getName(), zos);
            return backupFile;
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to create one-time backup for: " + sourceFolder.getName(), e);
            return null;
        } finally {
            if (snapshotDir != null) {
                deleteRecursively(snapshotDir.toPath());
            }
        }
    }

    /**
     * Starts the automatic backup task.
     *
     * @param sourceFolder the folder to back up
     */
    public void startAutoBackup(File sourceFolder) {
        if (SepareWorldItems.getInstance().isFolia()) {
            plugin.getServer().getGlobalRegionScheduler().runAtFixedRate(plugin, (task) -> createBackup(sourceFolder), 1L,
                    backupInterval / 50);
        } else {
            plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, () -> createBackup(sourceFolder), 0L,
                    backupInterval / 50);
        }
    }

    private static boolean shouldIncludeRemoteSnapshot(JavaPlugin plugin, boolean force) {
        try {
            StorageType type = StorageType.fromConfig(plugin.getConfig().getString("storage.type", "YAML"));
            if (type == StorageType.YAML || type == StorageType.SQLITE) {
                return false;
            }
            if (force) {
                return true;
            }
            return plugin.getConfig().getBoolean("settings.backups.remote-snapshot.enabled", false);
        } catch (Throwable ignored) {
            return false;
        }
    }

    /**
     * Creates a local YAML export folder containing the remote backend data.
     * The folder is created inside the plugin data folder so it will be included in the zip.
     */
    private static @Nullable File createRemoteSnapshotExport(JavaPlugin plugin, @Nullable PlayerDataStore store, @Nullable String snapshotTag) {
        if (!(plugin instanceof SepareWorldItems swi)) {
            return null;
        }

        PlayerDataStore effectiveStore = store != null ? store : swi.getPlayerDataStore();
        if (effectiveStore == null) {
            return null;
        }

        String tag = (snapshotTag == null || snapshotTag.isBlank()) ? "current" : sanitizeFileComponent(snapshotTag);
        File exportRoot = new File(swi.getDataFolder(), "_remoteSnapshotExport_" + tag);
        // ensure clean
        if (exportRoot.exists()) {
            deleteRecursively(exportRoot.toPath());
        }
        if (!exportRoot.mkdirs()) {
            return null;
        }

        try {
            exportScopeToYaml(swi, effectiveStore, exportRoot, PlayerDataScope.WORLD_GROUP);
            exportScopeToYaml(swi, effectiveStore, exportRoot, PlayerDataScope.WORLDGUARD_GROUP);
            return exportRoot;
        } catch (UnsupportedOperationException uoe) {
            swi.getLogger().warning("[Backups] Remote snapshot not supported by current backend: " + uoe.getMessage());
            deleteRecursively(exportRoot.toPath());
            return null;
        } catch (Throwable t) {
            swi.getLogger().warning("[Backups] Remote snapshot export failed: " + t.getMessage());
            deleteRecursively(exportRoot.toPath());
            return null;
        }
    }

    private static void exportScopeToYaml(SepareWorldItems plugin, PlayerDataStore store, File exportRoot, PlayerDataScope scope) {
        store.forEachEntry(scope, (groupName, playerUuid, playerName) -> {
            try {
                String safeName = sanitizeFileComponent(playerName != null && !playerName.isBlank() ? playerName : playerUuid.toString());

                File file = switch (scope) {
                    case WORLD_GROUP -> new File(exportRoot, "groups" + File.separator + groupName + File.separator + safeName + "-" + playerUuid + ".yml");
                    case WORLDGUARD_GROUP -> new File(exportRoot, "worldguard" + File.separator + "groups" + File.separator + groupName + File.separator + safeName + "-" + playerUuid + ".yml");
                };

                File parent = file.getParentFile();
                if (parent != null && !parent.exists()) {
                    parent.mkdirs();
                }

                var cfg = store.load(scope, groupName, playerUuid);
                cfg.save(file);
            } catch (Throwable ignored) {
                // best-effort snapshot
            }
        });
    }

    private static String sanitizeFileComponent(String input) {
        if (input == null || input.isBlank()) {
            return "unknown";
        }
        // avoid weird filesystem characters
        return input.replaceAll("[^a-zA-Z0-9_\\-\\.]", "_");
    }

    private static void deleteRecursively(Path root) {
        if (root == null) {
            return;
        }
        try {
            if (!Files.exists(root)) {
                return;
            }
            // delete children first
            try (var walk = Files.walk(root)) {
                walk.sorted(Comparator.reverseOrder()).forEach(p -> {
                    try {
                        Files.deleteIfExists(p);
                    } catch (IOException ignored) {
                    }
                });
            }
        } catch (IOException ignored) {
        }
    }
}

