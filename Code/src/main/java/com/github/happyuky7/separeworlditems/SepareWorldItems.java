package com.github.happyuky7.separeworlditems;

/*
 * Code by: Happyuky7
 * Github: https://github.com/Happyuky7
 * License: Custom
 * Link: https://github.com/Happyuky7/SEPARE-WORLD-ITEMS
 */

import com.github.happyuky7.separeworlditems.commands.SepareWorldItemsCMD;
import com.github.happyuky7.separeworlditems.filemanagers.FileManager;
import com.github.happyuky7.separeworlditems.listeners.WorldChangeEvent;
import com.github.happyuky7.separeworlditems.utils.MessageColors;
import org.bukkit.Bukkit;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

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
    private FileManager msgs;
    private FileManager bypasssave;
    private FileManager langs;

    // List of bypass players.
    public ArrayList<UUID> playerlist1 = new ArrayList<>();

    // Plugin info.
    PluginDescriptionFile pdffile = getDescription();
    public String version = this.pdffile.getVersion();

    @Override
    public void onEnable() {
        // Plugin startup logic
        instance = this;

        // Log plugin details to console
        logPluginDetails();

        // Load files.
        registerFileManager();
        Bukkit.getConsoleSender().sendMessage(MessageColors.getMsgColor("&f [Register]: &aLoad Files."));

        // Load language files.
        langs = new FileManager(this, "langs/" + getConfig().getString("experimental.lang"));
        Bukkit.getConsoleSender().sendMessage(MessageColors.getMsgColor("&f [Register]: &aLoad Langs Files."));

        // Register commands and events.
        registerCommands();
        Bukkit.getConsoleSender().sendMessage(MessageColors.getMsgColor("&f [Register]: &aLoad Commands."));

        registerEvents();
        Bukkit.getConsoleSender().sendMessage(MessageColors.getMsgColor("&f [Register]: &aLoad Events."));

        // Reload config files.
        reloadConfig();
        Bukkit.getConsoleSender().sendMessage(MessageColors.getMsgColor("&f [Register]: &aReload Files."));

        // Verify config version.
        verifyConfigVersion();
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        logPluginShutdownDetails();
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
     * Verifies the configuration version and disables the plugin if the version
     * does not match the required version.
     */
    private void verifyConfigVersion() {
        if (!getConfig().getString("general.config").equals("1.2.21")) {
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
     * Registers the commands for the plugin.
     */
    public void registerCommands() {
        getCommand("separeworlditems").setExecutor(new SepareWorldItemsCMD(this));
    }

    /**
     * Registers the events for the plugin.
     */
    public void registerEvents() {
        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(new WorldChangeEvent(this), this);
    }

    /**
     * Registers the file managers for the plugin.
     */
    public void registerFileManager() {
        config = new FileManager(this, "config");
        bypasssave = new FileManager(this, "bypass-save");
        msgs = new FileManager(this, "langs");
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
     * Gets the messages file manager.
     *
     * @return the messages file manager.
     */
    public FileManager getMsgs() {
        return msgs;
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
        this.getMsgs().reload();
        this.getBypassSave().reload();
    }
}
