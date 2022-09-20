package com.github.happyrogelio7.separeworlditems;

import com.github.happyrogelio7.separeworlditems.commands.SepareWorldItemsCMD;
import com.github.happyrogelio7.separeworlditems.filemanagers.FileManager;
import com.github.happyrogelio7.separeworlditems.listeners.WorldChangeEvent;
import com.github.happyrogelio7.separeworlditems.utils.MessageColors;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandExecutor;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.UUID;
import java.util.logging.Level;

public final class SepareWorldItems extends JavaPlugin {

    private FileManager config;
    private FileManager msgs;
    private FileManager bypasssave;

    public ArrayList<UUID> playerlist1 = new ArrayList<java.util.UUID>();

    PluginDescriptionFile pdffile = getDescription();
    public String version = this.pdffile.getVersion();

    @Override
    public void onEnable() {
        // Plugin startup logic

        Bukkit.getConsoleSender().sendMessage(MessageColors.getMsgColor("&7&m------------------------------------"));
        Bukkit.getConsoleSender().sendMessage(MessageColors.getMsgColor("&a On SepareWorldItems &b"+version));
        Bukkit.getConsoleSender().sendMessage(MessageColors.getMsgColor("&r "));
        Bukkit.getConsoleSender().sendMessage(MessageColors.getMsgColor("&a Thanks for using SepareWorldItems :D"));
        Bukkit.getConsoleSender().sendMessage(MessageColors.getMsgColor("&2 Created By: HappyRogelio7"));
        Bukkit.getConsoleSender().sendMessage(MessageColors.getMsgColor("&r "));
        Bukkit.getConsoleSender().sendMessage(MessageColors.getMsgColor("&a Version Server:&f "+Bukkit.getVersion()));
        Bukkit.getConsoleSender().sendMessage(MessageColors.getMsgColor("&r "));
        Bukkit.getConsoleSender().sendMessage(MessageColors.getMsgColor("&9&l Discord: &fhttps://discord.gg/3EebYUyeUX"));
        Bukkit.getConsoleSender().sendMessage(MessageColors.getMsgColor("&d&l GitHub: &fhttps://github.com/HappyRogelio7/SEPARE-WORLD-ITEMS"));
        Bukkit.getConsoleSender().sendMessage(MessageColors.getMsgColor("&7&m------------------------------------"));

        Bukkit.getConsoleSender().sendMessage(MessageColors.getMsgColor("&r "));
        Bukkit.getConsoleSender().sendMessage(MessageColors.getMsgColor("&3&m------------------------------------"));

        registerFileManager();

        Bukkit.getConsoleSender().sendMessage(MessageColors.getMsgColor("&f [Register]: &aLoad Files."));
        Bukkit.getConsoleSender().sendMessage(MessageColors.getMsgColor("&3&m------------------------------------"));

        registerCommands();

        Bukkit.getConsoleSender().sendMessage(MessageColors.getMsgColor("&f [Register]: &aLoad Commands."));
        Bukkit.getConsoleSender().sendMessage(MessageColors.getMsgColor("&3&m------------------------------------"));

        registerEvents();

        Bukkit.getConsoleSender().sendMessage(MessageColors.getMsgColor("&f [Register]: &aLoad Events."));
        Bukkit.getConsoleSender().sendMessage(MessageColors.getMsgColor("&3&m------------------------------------"));

        reloadConfig();

        Bukkit.getConsoleSender().sendMessage(MessageColors.getMsgColor("&f [Register]: &aReload Files."));
        Bukkit.getConsoleSender().sendMessage(MessageColors.getMsgColor("&3&m------------------------------------"));

        /*if (getConfig().getString("general.config").contains("1.2.19-DEV-112")) {
            Bukkit.getConsoleSender().sendMessage(MessageColors.getMsgColor("&3&m------------------------------------"));
            Bukkit.getConsoleSender().sendMessage(MessageColors.getMsgColor("&f [Error]: &cConfig Version ERROR."));
            getLogger().log(Level.SEVERE, "[Error]: &cConfig Version ERROR.");
            Bukkit.getConsoleSender().sendMessage(MessageColors.getMsgColor("&3&m------------------------------------"));
            //Bukkit.getPluginManager().disablePlugin(this);
            //onDisable();
        }

        if (getConfig().getString("general.java-version").contains("17")) {
            Bukkit.getConsoleSender().sendMessage(MessageColors.getMsgColor("&3&m------------------------------------"));
            Bukkit.getConsoleSender().sendMessage(MessageColors.getMsgColor("&f [Error]: &cConfig Version JAVA ERROR."));
            getLogger().log(Level.SEVERE, "[Error]: &cConfig Version JAVA ERROR.");
            getLogger().log(Level.SEVERE, "[Error]: &cJava 17");
            Bukkit.getConsoleSender().sendMessage(MessageColors.getMsgColor("&3&m------------------------------------"));
            //Bukkit.getPluginManager().disablePlugin(this);
            //onDisable();
        }*/

    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        Bukkit.getConsoleSender().sendMessage(MessageColors.getMsgColor("&7&m------------------------------------"));
        Bukkit.getConsoleSender().sendMessage(MessageColors.getMsgColor("&c OFF SepareWorldItems &b"+version));
        Bukkit.getConsoleSender().sendMessage(MessageColors.getMsgColor("&r "));
        Bukkit.getConsoleSender().sendMessage(MessageColors.getMsgColor("&a Thanks for using SepareWorldItems :D"));
        Bukkit.getConsoleSender().sendMessage(MessageColors.getMsgColor("&2 Created By: HappyRogelio7"));
        Bukkit.getConsoleSender().sendMessage(MessageColors.getMsgColor("&r "));
        Bukkit.getConsoleSender().sendMessage(MessageColors.getMsgColor("&a Version Server:&f "+Bukkit.getVersion()));
        Bukkit.getConsoleSender().sendMessage(MessageColors.getMsgColor("&r "));
        Bukkit.getConsoleSender().sendMessage(MessageColors.getMsgColor("&9&l Discord: &fhttps://discord.gg/3EebYUyeUX"));
        Bukkit.getConsoleSender().sendMessage(MessageColors.getMsgColor("&d&l GitHub: &fhttps://github.com/HappyRogelio7/SEPARE-WORLD-ITEMS"));
        Bukkit.getConsoleSender().sendMessage(MessageColors.getMsgColor("&7&m------------------------------------"));
    }

    public void registerCommands(){
        getCommand("separeworlditems").setExecutor((CommandExecutor)new SepareWorldItemsCMD(this));
    }

    public void registerEvents() {
        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents((Listener)new WorldChangeEvent(this), (Plugin)this);
    }

    public void registerFileManager() {
        this.config = new FileManager(this, "config");
        this.bypasssave = new FileManager(this, "bypass-save");
        this.msgs = new FileManager(this, "langs");
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
