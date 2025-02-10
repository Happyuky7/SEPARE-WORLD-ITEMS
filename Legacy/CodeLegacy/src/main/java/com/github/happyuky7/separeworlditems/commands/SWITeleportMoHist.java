package com.github.happyuky7.separeworlditems.commands;

import com.github.happyuky7.separeworlditems.utils.MessageColors;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class SWITeleportMoHist implements CommandExecutor, TabCompleter {


    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {

        if (!(sender instanceof Player)) {
            sender.sendMessage(MessageColors.getMsgColor("&cThis command can only be executed by a player."));
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            player.sendMessage(MessageColors.getMsgColor("&cUsage: /swimohist teleport <world>"));
            return true;
        }

        if (args[0].equalsIgnoreCase("teleport")) {

            if (args.length == 1) {
                player.sendMessage(MessageColors.getMsgColor("&cUsage: /swimohist teleport <world>"));
                return true;
            }

            if (!player.hasPermission("swi.mohist.teleport")) {
                player.sendMessage(MessageColors.getMsgColor("&cYou do not have permission to use this command."));
                return true;
            }

            String worldName = args[1];

            if (player.getServer().getWorld(worldName) == null) {
                player.sendMessage(MessageColors.getMsgColor("&cThe world &6" + worldName + "&c does not exist."));
                return true;
            }

            if (!player.hasPermission("swi.mohist.teleport.world." + worldName)) {
                player.sendMessage(MessageColors.getMsgColor("&cYou do not have permission to teleport to the world &6" + worldName + "&c."));
                return true;
            }

            player.teleport(player.getServer().getWorld(worldName).getSpawnLocation());

            player.sendMessage(MessageColors.getMsgColor("&aTeleported to world &6" + worldName + "&a."));

            return true;
        }

        return true;

    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {

        if (args.length == 0) {
            return List.of("teleport");
        }

        if (args.length == 1) {
            List<String> WORLD_NAMES = new ArrayList<>();
            for (World world : Bukkit.getWorlds()) {
                WORLD_NAMES.add(world.getName());
            }
            return WORLD_NAMES;
        }

        return null;

    }
}
