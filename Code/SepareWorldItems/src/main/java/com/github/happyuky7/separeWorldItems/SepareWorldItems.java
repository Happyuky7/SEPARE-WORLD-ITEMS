package com.github.happyuky7.separeWorldItems;

import com.github.happyuky7.separeWorldItems.commands.SepareWorldItemsCMD;
import com.github.happyuky7.separeWorldItems.files.FileManager;
import com.github.happyuky7.separeWorldItems.listeners.MoveEvent;
import com.github.happyuky7.separeWorldItems.listeners.WorldChangeEvent;
import com.github.happyuky7.separeWorldItems.managers.BackupManager;
import com.github.happyuky7.separeWorldItems.managers.InventoryChangeLogs;
import com.github.happyuky7.separeWorldItems.utils.ConvertTime;
import com.github.happyuky7.separeWorldItems.utils.DownloadTranslations;
import com.github.happyuky7.separeWorldItems.utils.MessageColors;
import com.github.happyuky7.separeWorldItems.utils.ConfigValidator;
import com.github.happyuky7.separeWorldItems.utils.LegacyUpgradeBackup;
import com.github.happyuky7.separeWorldItems.storage.PlayerDataStore;
import com.github.happyuky7.separeWorldItems.storage.StorageManager;
import com.github.happyuky7.separeWorldItems.storage.StorageType;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Arrays;

public final class SepareWorldItems extends JavaPlugin {

    private static SepareWorldItems instance;

    public static SepareWorldItems getInstance() {
        return instance;
    }

    public FileManager config;
    public FileManager langs;

    private BackupManager backupManager;

    private PlayerDataStore playerDataStore;

    @Override
    public void onEnable() {
        // Plugin startup logic

        instance = this;

        config = new FileManager(this, "config");

        // Safety backup for very old installs (<= 2.0.0-DEV-104)
        LegacyUpgradeBackup.maybeCreateLegacyBackup(this);


        Bukkit.getConsoleSender().sendMessage(MessageColors.getMsgColor(" "));
        Bukkit.getConsoleSender().sendMessage(MessageColors.getMsgColor(" "));

        Bukkit.getConsoleSender().sendMessage(MessageColors.getMsgColor("&a SepareWorldItems &7- &aStarting up..."));

        Bukkit.getConsoleSender().sendMessage(MessageColors.getMsgColor(" "));
        Bukkit.getConsoleSender().sendMessage(MessageColors.getMsgColor(" "));




        // Validate configuration (warnings only)
        try {
            ConfigValidator.validate(getConfig(), getLogger());
        } catch (Throwable t) {
            getLogger().warning("[Config] Validation failed: " + t.getMessage());
        }

        // Initialize storage backend (YAML by default)
        try {
            playerDataStore = StorageManager.createAndMaybeMigrate(this);
        } catch (Throwable t) {
            getLogger().warning("[Storage] Failed to initialize storage, falling back to YAML files: " + t.getMessage());
            playerDataStore = new com.github.happyuky7.separeWorldItems.storage.backends.YamlFilePlayerDataStore(this);
        }

        // Change-log retention (best-effort). SQL + YAML purge; Mongo/Redis handled via TTL.
        InventoryChangeLogs.startRetentionTask(this, playerDataStore);
        Bukkit.getConsoleSender().sendMessage(MessageColors.getMsgColor("&a SepareWorldItems &7- &aEnabled!"));
        Bukkit.getConsoleSender().sendMessage(MessageColors.getMsgColor(" "));
        Bukkit.getConsoleSender().sendMessage(MessageColors.getMsgColor("&a Author: &f" + getDescription().getAuthors().get(0)));
        Bukkit.getConsoleSender().sendMessage(MessageColors.getMsgColor("&a Version: &f" + getDescription().getVersion()));
        Bukkit.getConsoleSender().sendMessage(MessageColors.getMsgColor("&a Github: &fhttps://github.com/Happyuky7/SepareWorldItems"));
        Bukkit.getConsoleSender().sendMessage(MessageColors.getMsgColor(" "));
        Bukkit.getConsoleSender().sendMessage(MessageColors.getMsgColor("&a Server Version: &f" + Bukkit.getVersion()));
        

        // Warning Version is early access
        Bukkit.getConsoleSender().sendMessage(MessageColors.getMsgColor("&e WARNING: &fThis version is in early access, "+
        "if you find any bugs please report them on the GitHub page."));
        Bukkit.getConsoleSender().sendMessage(MessageColors.getMsgColor("&e RECOMMENDATION: &fMake a manual backup of the plugin folder " + 
        "before replacing config.yml with the new version."));

        Bukkit.getConsoleSender().sendMessage(MessageColors.getMsgColor(" "));
        Bukkit.getConsoleSender().sendMessage(MessageColors.getMsgColor(" "));

        // Check server type
        if (isFolia()) {
            Bukkit.getConsoleSender().sendMessage(MessageColors.getMsgColor("&a Server Type: &fFolia"));
        } else {
            Bukkit.getConsoleSender().sendMessage(MessageColors.getMsgColor("&a Server Type: &fSpigot&7/&fPaper&7/&fBukkit&7/&fPurpur&7/&fOther"));
            Bukkit.getConsoleSender().sendMessage(MessageColors.getMsgColor("&a Server Type (Detected): &f" + Bukkit.getServer().getClass().getName()));
        }


        Bukkit.getConsoleSender().sendMessage(MessageColors.getMsgColor(" "));

        // Version config check
        if (!getConfig().getString("config-version").equalsIgnoreCase("2.0.0")) {

            Bukkit.getConsoleSender().sendMessage(MessageColors.getMsgColor("&c[SepareWorldItems] Your config is outdated! Please delete your config.yml and restart the server!"));
            Bukkit.getPluginManager().disablePlugin(this);

        }

        Bukkit.getConsoleSender().sendMessage(MessageColors.getMsgColor(" "));
        Bukkit.getConsoleSender().sendMessage(MessageColors.getMsgColor(" "));

        Bukkit.getConsoleSender().sendMessage(MessageColors.getMsgColor(
            "&a Storage Backend: &f" + getConfig().getString("storage.type", "YAML")
        ));

        // Explicit warnings for non-file backends
        try {
            StorageType storageType = StorageType.fromConfig(getConfig().getString("storage.type", "YAML"));
            if (storageType != StorageType.YAML && storageType != StorageType.SQLITE) {
                Bukkit.getConsoleSender().sendMessage(MessageColors.getMsgColor("&e[SepareWorldItems] WARNING: &fMYSQL/MARIADB/MONGODB/REDIS backends are still &eBETA&f."));
                Bukkit.getConsoleSender().sendMessage(MessageColors.getMsgColor("&e[SepareWorldItems] WARNING: &fThey may have bugs and could corrupt player data. Make backups."));

                if (getConfig().getBoolean("integrations.worldguard.enabled", false)) {
                    Bukkit.getConsoleSender().sendMessage(MessageColors.getMsgColor("&c[SepareWorldItems] IMPORTANT: &fUsing BETA DB backends with &cWorldGuard regions&f is experimental."));
                    Bukkit.getConsoleSender().sendMessage(MessageColors.getMsgColor("&c[SepareWorldItems] IMPORTANT: &fIf you only use world-groups (no regions), corruption is less likely but still possible."));
                }
            }
        } catch (Throwable ignored) {
        }
        Bukkit.getConsoleSender().sendMessage(MessageColors.getMsgColor(" "));
        Bukkit.getConsoleSender().sendMessage(MessageColors.getMsgColor(" "));





        // Check for WorldGuard
        if (Bukkit.getPluginManager().getPlugin("WorldGuard") != null) {
            Bukkit.getConsoleSender().sendMessage(MessageColors.getMsgColor("&a WorldGuard detected! Support available."));
            Bukkit.getConsoleSender().sendMessage(MessageColors.getMsgColor("&a Required ENABLED Integration in config.yml"));
        } else {
            Bukkit.getConsoleSender().sendMessage(MessageColors.getMsgColor("&c WorldGuard not detected! Skipping support..."));

            if (getConfig().getBoolean("integrations.worldguard.enabled")) {
                getConfig().set("integrations.worldguard.enabled", false);
            }
        }

        Bukkit.getConsoleSender().sendMessage(MessageColors.getMsgColor(" "));

        // Check for Essentials
        if (Bukkit.getPluginManager().getPlugin("Essentials") != null) {
            Bukkit.getConsoleSender().sendMessage(MessageColors.getMsgColor("&a Essentials detected! Support available."));
            Bukkit.getConsoleSender().sendMessage(MessageColors.getMsgColor("&a Required ENABLED Integration in config.yml"));
        } else {
            Bukkit.getConsoleSender().sendMessage(MessageColors.getMsgColor("&c Essentials not detected! Skipping support..."));

            if (getConfig().getBoolean("integrations.essentials.enabled")) {
                getConfig().set("integrations.essentials.enabled", false);
            }
        }

        Bukkit.getConsoleSender().sendMessage(MessageColors.getMsgColor(" "));

        // Check for AuraSkills
        if (Bukkit.getPluginManager().getPlugin("AuraSkills") != null) {
            Bukkit.getConsoleSender().sendMessage(MessageColors.getMsgColor("&a AuraSkills detected! Support available."));
            Bukkit.getConsoleSender().sendMessage(MessageColors.getMsgColor("&a Required ENABLED Integration in config.yml"));
        } else {
            Bukkit.getConsoleSender().sendMessage(MessageColors.getMsgColor("&c AuraSkills not detected! Skipping support..."));

            if (getConfig().getBoolean("integrations.auraskills.enabled")) {
                getConfig().set("integrations.auraskills.enabled", false);
            }
        }

        Bukkit.getConsoleSender().sendMessage(MessageColors.getMsgColor(" "));



        // Auto download languages
        if (getConfig().getBoolean("settings.langs.autoDownload")) {
            try {
                DownloadTranslations.downloadTranslations();
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("Error ID: lang-002");
            }
        }

        // Check if the language file exists
        if (!checkLangFileExists(getConfig().getString("settings.langs.lang"))) {
            System.out.println("Error ID: lang-001");
            Bukkit.getConsoleSender().sendMessage(MessageColors.getMsgColor("&c[SepareWorldItems] The language file with the ID '" + getConfig().getString("settings.langs.lang") + "' does not exist!"));
            Bukkit.getPluginManager().disablePlugin(this);
        } else {

            langs = new FileManager(this, "langs/" + getConfig().getString("settings.langs.lang"));

        }

        // Check if the language file version is lasted
        if (!checkLangVersion("2.0.0")) {
            Bukkit.getConsoleSender().sendMessage(MessageColors.getMsgColor("&c[SepareWorldItems] The language file with the ID '" + getConfig().getString("settings.langs.lang") + "' is outdated!")); 
            Bukkit.getConsoleSender().sendMessage(MessageColors.getMsgColor("&c[SepareWorldItems] REQUIERED UPDATE!"));
        }

        // Configure backups
        configureBackups(
                getConfig().getBoolean("settings.backups.enabled"),
                getConfig().getInt("settings.backups.interval"),
                getConfig().getString("settings.backups.interval-type"),
                getConfig().getInt("settings.backups.max-backups")
        );

        // Listeners
        // WorldChangeEvent is the default world-group switcher.
        // If WorldGuard is enabled, MoveEvent adds region-based switching on top (only when a configured region applies).
        getServer().getPluginManager().registerEvents(new WorldChangeEvent(), this);

        // Register WorldGuard integration if enabled
        if (getConfig().getBoolean("integrations.worldguard.enabled")) {
            getServer().getPluginManager().registerEvents(new MoveEvent(), this);
            getLogger().info("WorldGuard integration enabled - region-based inventory switching activated!");
        }
    

        // Commands
        getCommand("separeworlditems").setExecutor(new SepareWorldItemsCMD());


    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic

        Bukkit.getConsoleSender().sendMessage(MessageColors.getMsgColor(" "));

        try {
            if (playerDataStore != null) {
                playerDataStore.close();
            }
        } catch (Throwable ignored) {
        }
        Bukkit.getConsoleSender().sendMessage(MessageColors.getMsgColor("&c SepareWorldItems &7- &cDisabled"));
        Bukkit.getConsoleSender().sendMessage(MessageColors.getMsgColor(" "));
        Bukkit.getConsoleSender().sendMessage(MessageColors.getMsgColor("&a Author: &f" + getDescription().getAuthors().get(0)));
        Bukkit.getConsoleSender().sendMessage(MessageColors.getMsgColor("&a Version: &f" + getDescription().getVersion()));
        Bukkit.getConsoleSender().sendMessage(MessageColors.getMsgColor("&a Github: &fhttps://github.com/Happyuky7/SepareWorldItems"));
        Bukkit.getConsoleSender().sendMessage(MessageColors.getMsgColor(" "));

    }

    /**
     * Checks if server is Folia.
     */
    public boolean isFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public FileManager getConfig() {
        return config;
    }

    public FileManager getLangs() {
        return langs;
    }

    public PlayerDataStore getPlayerDataStore() {
        return playerDataStore;
    }



    /**
     * Configures and starts the backup process for the user data folder.
     */
    private void configureBackups(Boolean backupsEnabled, Integer backupInterval, String intervaltype, Integer maxBackups) {

        // Static configuration for backups
        // Backup the whole plugin folder so it includes:
        // - groups/
        // - worldguard/groups/
        // - sqlite file (if used)
        // Excludes backups/ folders to avoid recursion.
        File userDataFolder = getDataFolder(); // Folder containing plugin data
        File backupFolder = new File(getDataFolder(), "backups"); // Folder for storing backups
        //int maxBackups = 5; // Maximum number of backups to retain
        //long backupInterval = 86400000L; // Backup interval in milliseconds (1 day)

        long backupIntervallong;

        try {

            // Check interval type
            if (!Arrays.asList("m", "h", "d").contains(intervaltype)) {
                Bukkit.getConsoleSender().sendMessage(MessageColors.getMsgColor("&c[SepareWorldItems] Invalid backup interval type! Please use 'm' for minutes, 'h' for hours, or 'd' for days."));
                throw new IllegalArgumentException("Invalid interval type");
            }

            backupIntervallong = ConvertTime.convertTimeToMilliseconds(backupInterval, intervaltype); // Backup interval in milliseconds (1 day)

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Error ID: backup-001");
            Bukkit.getConsoleSender().sendMessage(MessageColors.getMsgColor("&c[SepareWorldItems] The backup interval type is incorrect! Please use 'm' for minutes, 'h' for hours, or 'd' for days."));
            return;
        }

        // Ensure that the user data folder exists
        if (!userDataFolder.exists()) {
            userDataFolder.mkdirs(); // Create the folder if it does not exist
        }

        // Initialize and start the backup system if backups are enabled
        if (backupsEnabled) {
            backupManager = new BackupManager(this, backupFolder, maxBackups, backupIntervallong);
            backupManager.startAutoBackup(userDataFolder); // Creates backups of the entire folder
            getLogger().info("Backups are enabled and running for the user data folder.");
        } else {
            getLogger().info("Backups are disabled.");
        }
    }



    /**
     * Checks if the language file exists in the langs folder.
     *
     * @param langId the language file ID.
     * @return true if the file exists, false otherwise.
     */
    public static boolean checkLangFileExists(String langId) {

        File langsDir = new File(instance.getDataFolder(), "langs");
        if (!langsDir.exists()) {
            langsDir.mkdirs();
        }

        File langFile = new File(langsDir, langId + ".yml");
        return langFile.exists();
    }

    public static boolean checkLangVersion(String versionlangid) {

        String versionlang = getInstance().getLangs().getString("lang-version");

        if (versionlang == null) {
            return false;
        }

        return versionlang.equals(versionlangid);

    }

}
