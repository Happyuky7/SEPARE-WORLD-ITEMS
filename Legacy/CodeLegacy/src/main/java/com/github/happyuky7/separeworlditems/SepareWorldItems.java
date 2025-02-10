package com.github.happyuky7.separeworlditems;

/*
 * Code by: Happyuky7
 * Github: https://github.com/Happyuky7
 * License: Custom
 * Link: https://github.com/Happyuky7/SEPARE-WORLD-ITEMS
 */

import com.github.happyuky7.separeworlditems.commands.SWITeleportMoHist;
import com.github.happyuky7.separeworlditems.commands.SepareWorldItemsCMD;
import com.github.happyuky7.separeworlditems.filemanagers.FileManager;
import com.github.happyuky7.separeworlditems.listeners.Integration.EssentialsX.HomeEvent;
import com.github.happyuky7.separeworlditems.listeners.base.MVTeleport;
import com.github.happyuky7.separeworlditems.listeners.base.WorldChangeEvent;
import com.github.happyuky7.separeworlditems.utils.ConvertTime;
import com.github.happyuky7.separeworlditems.utils.DownloadTranslations;
import com.github.happyuky7.separeworlditems.managers.BackupManager;
import com.github.happyuky7.separeworlditems.utils.MessageColors;
import org.bukkit.Bukkit;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Main class for the SepareWorldItems plugin. This class handles plugin startup
 * and shutdown,
 * command registration, and event registration.
 */
public final class SepareWorldItems extends JavaPlugin {

    // Instance of plugin.
    private static SepareWorldItems instance;

    /**
     * Gets the instance of the SepareWorldItems plugin.
     *
     * @return the instance of the plugin.
     */
    public static SepareWorldItems getInstance() {
        return instance;
    }

    // Files Manager.
    private FileManager config;
    //private FileManager msgs;
    private FileManager bypasssave;
    private FileManager langs;
    private FileManager langenUS;
    private BackupManager backupManager;

    // List of bypass players.
    public ArrayList<UUID> playerlist1 = new ArrayList<>();

    // Plugin info.
    PluginDescriptionFile pdffile = getDescription();
    public String version = this.pdffile.getVersion();

    @Override
    public void onEnable() {
        // Plugin startup logic
        instance = this;

        // Reload config files.
        // requires this to be here in order to validate the other options in code.
        config = new FileManager(this, "config");

        // Verify config version.
        verifyConfigVersion();

        // Configure backups
        configureBackups(
                getConfig().getBoolean("experimental.backups.enable"),
                getConfig().getInt("experimental.backups.interval"),
                getConfig().getInt("experimental.backups.max-backups")
                );

        // Auto download lang.
        if (getConfig().getBoolean("settings.langs.auto-download")) {
            DownloadTranslations.downloadTranslations();
            Bukkit.getConsoleSender().sendMessage(MessageColors.getMsgColor("&f [Register]: &aAuto Download Lang Enabled."));
        }
        // Check if lang file exists and create it if not.
        langenUS = new FileManager(this, "langs/en_US");


        // Log plugin details to console
        logPluginDetails();

        // Load files.
        registerFileManager();
        Bukkit.getConsoleSender().sendMessage(MessageColors.getMsgColor("&f [Register]: &aLoad Files."));

        // Load language files.
        // Validate if the lang file exists. (BETA)
        if (!checkLangFileExists(getConfig().getString("settings.langs.lang"))) {
            Bukkit.getConsoleSender().sendMessage(MessageColors.getMsgColor("&f [Register]: &cLang File not found."));
            Bukkit.getConsoleSender().sendMessage(MessageColors.getMsgColor("&f [Register]: &cDisable Plugin."));
            Bukkit.getPluginManager().disablePlugin(this);
        } else {
            langs = new FileManager(this, "langs/" + getConfig().getString("settings.langs.lang"));
            Bukkit.getConsoleSender().sendMessage(MessageColors.getMsgColor("&f [Register]: &aLoad Lang Files."));
        }


        // Register commands and events.
        registerCommands();
        Bukkit.getConsoleSender().sendMessage(MessageColors.getMsgColor("&f [Register]: &aLoad Commands."));

        registerEvents();
        Bukkit.getConsoleSender().sendMessage(MessageColors.getMsgColor("&f [Register]: &aLoad Events."));


        // UPDATE REQUIRED NEW VERSION 2.0.0 (ADDING THIS MESSAGE IN 1.2.27)
        /*Bukkit.getConsoleSender().sendMessage(MessageColors.getMsgColor("&8[&aSepareWorldItems&8] &cREQUIRED UPDATE! &8| &7Please update to the new version 2.0.0"));
        Bukkit.getConsoleSender().sendMessage(MessageColors.getMsgColor("&8[&aSepareWorldItems&8] &cDownload Link: &7https://github.com/Happyuky7/SEPARE-WORLD-ITEMS/releases"));
        Bukkit.getlogger().log(Level.SEVERE, "[Error]: REQUIRED UPDATE! Please update to the new version 2.0.0");
        Bukkit.getlogger().log(Level.SEVERE, "[Error]: REQUIRED UPDATE! Please update to the new version 2.0.0");
        Bukkit.getlogger().log(Level.SEVERE, "[Error]: REQUIRED UPDATE! Please update to the new version 2.0.0");
        Bukkit.getlogger().log(Level.SEVERE, "[Error]: REQUIRED UPDATE! Please update to the new version 2.0.0");
        Bukkit.getlogger().log(Level.SEVERE, "[Error]: REQUIRED UPDATE! Please update to the new version 2.0.0");
        Bukkit.getlogger().log(Level.SEVERE, "[Error]: REQUIRED UPDATE! Please update to the new version 2.0.0");
        Bukkit.getlogger().log(Level.SEVERE, "[Error]: REQUIRED UPDATE! Please update to the new version 2.0.0");
        Bukkit.getlogger().log(Level.SEVERE, "[Error]: REQUIRED UPDATE! Please update to the new version 2.0.0");
        Bukkit.getlogger().log(Level.SEVERE, "[Error]: REQUIRED UPDATE! Please update to the new version 2.0.0");
        Bukkit.getlogger().log(Level.SEVERE, "[Error]: REQUIRED UPDATE! Please update to the new version 2.0.0");
        Bukkit.getlogger().log(Level.SEVERE, "[Error]: REQUIRED UPDATE! Please update to the new version 2.0.0");
        Bukkit.getlogger().log(Level.SEVERE, "[Error]: REQUIRED UPDATE! Please update to the new version 2.0.0");
        Bukkit.getlogger().log(Level.SEVERE, "[Error]: Download Link: https://github.com/Happyuky7/SEPARE-WORLD-ITEMS/releases");
        Bukkit.getlogger().log(Level.SEVERE, "[Error]: Download Link: https://github.com/Happyuky7/SEPARE-WORLD-ITEMS/releases");
        Bukkit.getlogger().log(Level.SEVERE, "[Error]: Download Link: https://github.com/Happyuky7/SEPARE-WORLD-ITEMS/releases");
        Bukkit.getlogger().log(Level.SEVERE, "[Error]: Download Link: https://github.com/Happyuky7/SEPARE-WORLD-ITEMS/releases");
        Bukkit.getlogger().log(Level.SEVERE, "[Error]: Download Link: https://github.com/Happyuky7/SEPARE-WORLD-ITEMS/releases");*/

    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        logPluginShutdownDetails();
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

    /**
     * Verifies the configuration version and disables the plugin if the version
     * does not match the required version.
     */
    private void verifyConfigVersion() {
        if (!getConfig().getString("config-version").equalsIgnoreCase("1.2.26")) {
            Bukkit.getConsoleSender()
                    .sendMessage(MessageColors.getMsgColor("&3&m------------------------------------"));
            Bukkit.getConsoleSender().sendMessage(MessageColors.getMsgColor("&f [Error]: &cConfig Version ERROR."));
            getLogger().log(Level.SEVERE, "[Error]: Config Version ERROR.");
            Bukkit.getConsoleSender()
                    .sendMessage(MessageColors.getMsgColor("&3&m------------------------------------"));
            Bukkit.getPluginManager().disablePlugin(this);
            onDisable();
        }
    }

    /**
     * Configures and starts the backup process for the user data folder.
     */
    private void configureBackups(Boolean backupsEnabled, Integer backupInterval, Integer maxBackups) {

        // Static configuration for backups
        File userDataFolder = new File(getDataFolder(), "groups"); // Folder containing user data
        File backupFolder = new File(getDataFolder(), "backups"); // Folder for storing backups
        //int maxBackups = 5; // Maximum number of backups to retain
        //long backupInterval = 86400000L; // Backup interval in milliseconds (1 day)
        long backupIntervallong = ConvertTime.convertTimeToMilliseconds(backupInterval, "d"); // Backup interval in milliseconds (1 day)

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
     * Logs plugin details to the console on startup.
     */
    private void logPluginDetails() {
        Bukkit.getConsoleSender().sendMessage(MessageColors.getMsgColor("&7&m------------------------------------"));
        Bukkit.getConsoleSender().sendMessage(MessageColors.getMsgColor("&a On SepareWorldItems &b" + version));
        Bukkit.getConsoleSender().sendMessage(MessageColors.getMsgColor("&r "));
        Bukkit.getConsoleSender().sendMessage(MessageColors.getMsgColor("&a Thanks for using SepareWorldItems :D"));
        Bukkit.getConsoleSender().sendMessage(MessageColors.getMsgColor("&2 Created By: Happyuky7"));
        Bukkit.getConsoleSender().sendMessage(MessageColors.getMsgColor("&r "));
        Bukkit.getConsoleSender().sendMessage(MessageColors.getMsgColor("&a Version Server:&f " + Bukkit.getVersion()));
        Bukkit.getConsoleSender().sendMessage(MessageColors.getMsgColor("&r "));
        Bukkit.getConsoleSender()
                .sendMessage(MessageColors.getMsgColor("&9&l Discord: &fhttps://discord.gg/3EebYUyeUX"));
        Bukkit.getConsoleSender().sendMessage(
                MessageColors.getMsgColor("&d&l GitHub: &fhttps://github.com/Happyuky7/SEPARE-WORLD-ITEMS"));
        Bukkit.getConsoleSender().sendMessage(MessageColors.getMsgColor("&7&m------------------------------------"));
        Bukkit.getConsoleSender().sendMessage(MessageColors.getMsgColor("&r "));
        Bukkit.getConsoleSender().sendMessage(MessageColors.getMsgColor("&3&m------------------------------------"));
    }

    /**
     * Logs plugin details to the console on shutdown.
     */
    private void logPluginShutdownDetails() {
        Bukkit.getConsoleSender().sendMessage(MessageColors.getMsgColor("&7&m------------------------------------"));
        Bukkit.getConsoleSender().sendMessage(MessageColors.getMsgColor("&c OFF SepareWorldItems &b" + version));
        Bukkit.getConsoleSender().sendMessage(MessageColors.getMsgColor("&r "));
        Bukkit.getConsoleSender().sendMessage(MessageColors.getMsgColor("&a Thanks for using SepareWorldItems :D"));
        Bukkit.getConsoleSender().sendMessage(MessageColors.getMsgColor("&2 Created By: Happyuky7"));
        Bukkit.getConsoleSender().sendMessage(MessageColors.getMsgColor("&r "));
        Bukkit.getConsoleSender().sendMessage(MessageColors.getMsgColor("&a Version Server:&f " + Bukkit.getVersion()));
        Bukkit.getConsoleSender().sendMessage(MessageColors.getMsgColor("&r "));
        Bukkit.getConsoleSender()
                .sendMessage(MessageColors.getMsgColor("&9&l Discord: &fhttps://discord.gg/3EebYUyeUX"));
        Bukkit.getConsoleSender().sendMessage(
                MessageColors.getMsgColor("&d&l GitHub: &fhttps://github.com/Happyuky7/SEPARE-WORLD-ITEMS"));
        Bukkit.getConsoleSender().sendMessage(MessageColors.getMsgColor("&7&m------------------------------------"));
    }

    /**
     * Registers the commands for the plugin.
     */
    public void registerCommands() {
        getCommand("separeworlditems").setExecutor(new SepareWorldItemsCMD(this));

        if (getConfig().getBoolean("experimental.mohist.enable")) {
            getCommand("swimohist").setExecutor(new SWITeleportMoHist());
        }

    }

    /**
     * Registers the events for the plugin.
     */
    public void registerEvents() {
        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(new WorldChangeEvent(this), this);
        pm.registerEvents(new HomeEvent(this), this);
        pm.registerEvents(new MVTeleport(this), this);
    }

    /**
     * Registers the file managers for the plugin.
     */
    public void registerFileManager() {
        bypasssave = new FileManager(this, "bypass-save");
    }

    /**
     * Gets the language file manager.
     *
     * @return the language file manager.
     */
    public FileManager getLangs() {
        return langs;
    }

    /**
     * Gets the configuration file manager.
     *
     * @return the configuration file manager.
     */
    public FileManager getConfig() {
        return config;
    }

    /**
     * Gets the bypass save file manager.
     *
     * @return the bypass save file manager.
     */
    public FileManager getBypassSave() {
        return bypasssave;
    }

    /**
     * Reloads the configuration, messages, and bypass save files.
     */
    public void reloadConfig() {
        this.getConfig().reload();
        this.getBypassSave().reload();
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
}
