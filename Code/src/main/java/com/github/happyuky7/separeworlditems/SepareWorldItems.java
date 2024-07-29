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
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.UUID;
import java.util.logging.Level;

public final class SepareWorldItems extends JavaPlugin {


    // Instance of plugin.

    private static SepareWorldItems instance;

    public static SepareWorldItems getInstance() {
        return instance;
    }

    // Files Manager.
    private FileManager config;
    private FileManager msgs;
    private FileManager bypasssave;

    private FileManager langs;

    // List of bypass players.
    public ArrayList<UUID> playerlist1 = new ArrayList<UUID>();

    // Plugin info.
    PluginDescriptionFile pdffile = getDescription();
    public String version = this.pdffile.getVersion();


    @Override
    public void onEnable() {
        // Plugin startup logic

        instance = this;

        Bukkit.getConsoleSender().sendMessage(MessageColors.getMsgColor("&7&m------------------------------------"));
        Bukkit.getConsoleSender().sendMessage(MessageColors.getMsgColor("&a On SepareWorldItems &b"+version));
        Bukkit.getConsoleSender().sendMessage(MessageColors.getMsgColor("&r "));
        Bukkit.getConsoleSender().sendMessage(MessageColors.getMsgColor("&a Thanks for using SepareWorldItems :D"));
        Bukkit.getConsoleSender().sendMessage(MessageColors.getMsgColor("&2 Created By: Happyuky7"));
        Bukkit.getConsoleSender().sendMessage(MessageColors.getMsgColor("&r "));
        Bukkit.getConsoleSender().sendMessage(MessageColors.getMsgColor("&a Version Server:&f "+Bukkit.getVersion()));
        Bukkit.getConsoleSender().sendMessage(MessageColors.getMsgColor("&r "));
        Bukkit.getConsoleSender().sendMessage(MessageColors.getMsgColor("&9&l Discord: &fhttps://discord.gg/3EebYUyeUX"));
        Bukkit.getConsoleSender().sendMessage(MessageColors.getMsgColor("&d&l GitHub: &fhttps://github.com/Happyuky7/SEPARE-WORLD-ITEMS"));
        Bukkit.getConsoleSender().sendMessage(MessageColors.getMsgColor("&7&m------------------------------------"));

        Bukkit.getConsoleSender().sendMessage(MessageColors.getMsgColor("&r "));
        Bukkit.getConsoleSender().sendMessage(MessageColors.getMsgColor("&3&m------------------------------------"));

        // Load files.
        registerFileManager();

        Bukkit.getConsoleSender().sendMessage(MessageColors.getMsgColor("&f [Register]: &aLoad Files."));
        Bukkit.getConsoleSender().sendMessage(MessageColors.getMsgColor("&3&m------------------------------------"));

        // Load langs files.

        langs = new FileManager(this, "langs/" + getConfig().getString("experimental.lang"));


        Bukkit.getConsoleSender().sendMessage(MessageColors.getMsgColor("&f [Register]: &aLoad Langs Files."));
        Bukkit.getConsoleSender().sendMessage(MessageColors.getMsgColor("&3&m------------------------------------"));

        // Register Commands.
        registerCommands();

        Bukkit.getConsoleSender().sendMessage(MessageColors.getMsgColor("&f [Register]: &aLoad Commands."));
        Bukkit.getConsoleSender().sendMessage(MessageColors.getMsgColor("&3&m------------------------------------"));

        // Register Events.
        registerEvents();

        Bukkit.getConsoleSender().sendMessage(MessageColors.getMsgColor("&f [Register]: &aLoad Events."));
        Bukkit.getConsoleSender().sendMessage(MessageColors.getMsgColor("&3&m------------------------------------"));

        // Reload Configs.
        reloadConfig();

        Bukkit.getConsoleSender().sendMessage(MessageColors.getMsgColor("&f [Register]: &aReload Files."));
        Bukkit.getConsoleSender().sendMessage(MessageColors.getMsgColor("&3&m------------------------------------"));

        // Verify config version.
        if (!getConfig().getString("general.config").equals("1.2.21")) {
            Bukkit.getConsoleSender().sendMessage(MessageColors.getMsgColor("&3&m------------------------------------"));
            Bukkit.getConsoleSender().sendMessage(MessageColors.getMsgColor("&f [Error]: &cConfig Version ERROR."));
            getLogger().log(Level.SEVERE, "[Error]: Config Version ERROR.");
            Bukkit.getConsoleSender().sendMessage(MessageColors.getMsgColor("&3&m------------------------------------"));
            Bukkit.getPluginManager().disablePlugin(this);
            onDisable();
        }

    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        Bukkit.getConsoleSender().sendMessage(MessageColors.getMsgColor("&7&m------------------------------------"));
        Bukkit.getConsoleSender().sendMessage(MessageColors.getMsgColor("&c OFF SepareWorldItems &b"+version));
        Bukkit.getConsoleSender().sendMessage(MessageColors.getMsgColor("&r "));
        Bukkit.getConsoleSender().sendMessage(MessageColors.getMsgColor("&a Thanks for using SepareWorldItems :D"));
        Bukkit.getConsoleSender().sendMessage(MessageColors.getMsgColor("&2 Created By: Happyuky7"));
        Bukkit.getConsoleSender().sendMessage(MessageColors.getMsgColor("&r "));
        Bukkit.getConsoleSender().sendMessage(MessageColors.getMsgColor("&a Version Server:&f "+Bukkit.getVersion()));
        Bukkit.getConsoleSender().sendMessage(MessageColors.getMsgColor("&r "));
        Bukkit.getConsoleSender().sendMessage(MessageColors.getMsgColor("&9&l Discord: &fhttps://discord.gg/3EebYUyeUX"));
        Bukkit.getConsoleSender().sendMessage(MessageColors.getMsgColor("&d&l GitHub: &fhttps://github.com/Happyuky7/SEPARE-WORLD-ITEMS"));
        Bukkit.getConsoleSender().sendMessage(MessageColors.getMsgColor("&7&m------------------------------------"));
    }

    public void registerCommands(){
        getCommand("separeworlditems").setExecutor(new SepareWorldItemsCMD(this));
    }

    public void registerEvents() {
        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents((Listener)new WorldChangeEvent(this), (Plugin)this);
    }

    public void registerFileManager() {
        config = new FileManager(this, "config");
        bypasssave = new FileManager(this, "bypass-save");
        msgs = new FileManager(this, "langs");
    }

    public FileManager getLangs() {
        return this.langs;
    }

    public FileManager getConfig() {
        return this.config;
    }

    public FileManager getMsgs() {
        return this.msgs;
    }

    public FileManager getBypassSave() {
        return this.bypasssave;
    }


    public void reloadConfig() {
        //reload config.yml
        this.getConfig().reload();
        //reload langs.yml
        this.getMsgs().reload();
        //reload bypass-save.yml
        this.getBypassSave().reload();
    }
}
