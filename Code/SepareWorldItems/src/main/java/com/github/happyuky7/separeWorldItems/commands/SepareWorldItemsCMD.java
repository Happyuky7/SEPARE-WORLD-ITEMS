package com.github.happyuky7.separeWorldItems.commands;

import com.github.happyuky7.separeWorldItems.SepareWorldItems;
import com.github.happyuky7.separeWorldItems.managers.BackupManager;
import com.github.happyuky7.separeWorldItems.managers.MessagesManager;
import com.github.happyuky7.separeWorldItems.utils.MessageColors;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.List;

public class SepareWorldItemsCMD implements CommandExecutor, TabCompleter {


    @Override
    public boolean onCommand(CommandSender sender, Command command, String s, String[] args) {

        if (args.length == 0) {

            sender.sendMessage(MessageColors.getMsgColor(" "));
            sender.sendMessage(MessageColors.getMsgColor("&a separeWorldItems &7- &aVersion: &f"
                    + SepareWorldItems.getInstance().getDescription().getVersion()));
            sender.sendMessage(MessageColors.getMsgColor(" "));
            sender.sendMessage(MessageColors.getMsgColor("&a Developed by: &fHappyuky7"));
            sender.sendMessage(MessageColors.getMsgColor("&a Github: &fhttps://github.com/Happyuky7/"));
            sender.sendMessage(MessageColors.getMsgColor(" "));

            if (sender.hasPermission("separeWorldItems.admin")) {
                sender.sendMessage(MessageColors.getMsgColor(" "));
                sender.sendMessage(MessageColors.getMsgColor("&a SepareWorldItems &7- &aHelp"));
                sender.sendMessage(MessageColors.getMsgColor(" "));
                sender.sendMessage(MessageColors.getMsgColor("&a /separeworlditems &7- &fShow this message"));
                sender.sendMessage(MessageColors.getMsgColor("&a /separeworlditems reload &7- &fReload the plugin"));
                sender.sendMessage(MessageColors.getMsgColor("&a /separeworlditems backupforce &7- &fForce a backup"));
                sender.sendMessage(MessageColors.getMsgColor(" "));
                return true;
            }

            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("separeworlditems.admin")) {
                sender.sendMessage(MessagesManager.getMessage("no-permission"));
                return true;
            }

            SepareWorldItems.getInstance().getConfig().reload();
            SepareWorldItems.getInstance().getLangs().reload();

            sender.sendMessage(MessagesManager.getMessage("reload"));

        }

        if (args[0].equalsIgnoreCase("backupforce")) {
            if (!sender.hasPermission("separeworlditems.admin")) {
                sender.sendMessage(MessagesManager.getMessage("no-permission"));
                return true;
            }

            sender.sendMessage(MessagesManager.getMessage("backups.force-backup.start"));

            try {
                File sourcefolder = new File(SepareWorldItems.getInstance().getDataFolder(), "groups");
                File forceBackupFolder = new File(SepareWorldItems.getInstance().getDataFolder(), "forceBackups");

                BackupManager.createForceBackup(SepareWorldItems.getInstance(), sourcefolder);

                sender.sendMessage(MessagesManager.getMessage("backups.force-backup.end"));
                sender.sendMessage(MessagesManager.getMessageList("backups.force-backup.completed")
                        .replace("%backup_path%", BackupManager.forebackupname));

            } catch (Exception e) {
                e.printStackTrace();
                sender.sendMessage(MessagesManager.getMessageList("backups.force-backup.error"));
            }

            return true;

        }

        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(CommandSender sender, Command command, String s, String[] args) {

        if (!sender.hasPermission("separeworlditems.admin")) {
            return List.of();
        }

        if (args.length == 1) {
            return List.of("reload", "backupforce");
        }

        return List.of();
    }
}
