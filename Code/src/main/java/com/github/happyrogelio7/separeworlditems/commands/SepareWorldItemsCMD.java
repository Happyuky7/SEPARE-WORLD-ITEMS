package com.github.happyrogelio7.separeworlditems.commands;

/*
 * Code by: HappyRogelio7
 * Github: https://github.com/happyrogelio7
 * License: Custom
 * Link: https://github.com/HappyRogelio7/SEPARE-WORLD-ITEMS
 */

import com.github.happyrogelio7.separeworlditems.SepareWorldItems;
import com.github.happyrogelio7.separeworlditems.utils.MessageColors;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Objects;

public class SepareWorldItemsCMD implements CommandExecutor {

    private final SepareWorldItems plugin;

    public SepareWorldItemsCMD(SepareWorldItems plugin) {
        this.plugin = plugin;
    }


    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player)){
            sender.sendMessage(MessageColors.getMsgColor("&c This command can only be used by players."));
            return true;
        }

        Player p = (Player) sender;

        if (args.length == 0){
            p.sendMessage(MessageColors.getMsgColor(plugin.getMsgs().getString("general.unknown-cmd-args")
                    .replaceAll(MessageColors.getMsgColor("%prefix%"), plugin.getMsgs().getString("general.prefix"))));
            return true;
        }

        if (args.length > 0){

            if (args[0].equalsIgnoreCase("help")){

                //command help
                p.sendMessage(MessageColors.getMsgColor("&r "));
                p.sendMessage(MessageColors.getMsgColor("&a SepareWorldItems &7| &3Commands:"));
                p.sendMessage(MessageColors.getMsgColor("&r "));
                p.sendMessage(MessageColors.getMsgColor("&7  <> Required &7&l|&r&7 [] Optional"));
                p.sendMessage(MessageColors.getMsgColor("&r "));
                p.sendMessage(MessageColors.getMsgColor("&f * &a/separeworlditems help"));
                p.sendMessage(MessageColors.getMsgColor("&f * &a/separeworlditems info"));
                p.sendMessage(MessageColors.getMsgColor("&f * &a/separeworlditems api"));
                p.sendMessage(MessageColors.getMsgColor("&f * &a/separeworlditems update"));
                p.sendMessage(MessageColors.getMsgColor("&f * &a/separeworlditems reload"));
                p.sendMessage(MessageColors.getMsgColor("&r "));
                return true;

            } else if (args[0].equalsIgnoreCase("info") || args[0].equalsIgnoreCase("version")){

                //info or version plugin.
                p.sendMessage(MessageColors.getMsgColor("&r "));
                p.sendMessage(MessageColors.getMsgColor("&a SepareWorldItems &7| &3Info:"));
                p.sendMessage(MessageColors.getMsgColor("&r "));
                p.sendMessage(MessageColors.getMsgColor("&f * &9version: &f" + plugin.version));
                p.sendMessage(MessageColors.getMsgColor("&f * &aCreated: &fHappyRogelio7"));
                p.sendMessage(MessageColors.getMsgColor("&f * &dGitHub: &fhttps://github.com/HappyRogelio7"));
                p.sendMessage(MessageColors.getMsgColor("&f * &6Website: &fhttps://happyrogelio7.github.io/index.html"));
                p.sendMessage(MessageColors.getMsgColor("&r "));
                return true;

            } else if (args[0].equalsIgnoreCase("api")){

                if (!p.hasPermission("separeworlditems.cmd.api")) {
                    p.sendMessage(MessageColors.getMsgColor(plugin.getMsgs().getString("general.no-permission")
                            .replaceAll(MessageColors.getMsgColor("%prefix%"), plugin.getMsgs().getString("general.prefix"))));
                    return true;
                }

                //API link using, add, etc.
                p.sendMessage(MessageColors.getMsgColor("&r "));
                p.sendMessage(MessageColors.getMsgColor("&8[&aSepareWorldItems&8]&r https://github.com/HappyRogelio7/SEPARE-WORLD-ITEMS/wiki/API"));
                p.sendMessage(MessageColors.getMsgColor("&r "));

                return true;

            } else if (args[0].equalsIgnoreCase("update")){

                if (!p.hasPermission("separeworlditems.cmd.update")) {
                    p.sendMessage(MessageColors.getMsgColor(plugin.getMsgs().getString("general.no-permission")
                            .replaceAll(MessageColors.getMsgColor("%prefix%"), plugin.getMsgs().getString("general.prefix"))));
                    return true;
                }

                //update link msg cmd
                p.sendMessage(MessageColors.getMsgColor("&r "));
                p.sendMessage(MessageColors.getMsgColor("&8[&aSepareWorldItems&8]&r https://github.com/HappyRogelio7/SEPARE-WORLD-ITEMS/wiki#download"));
                p.sendMessage(MessageColors.getMsgColor("&r "));
                return true;

            } else if (args[0].equalsIgnoreCase("reload")){

                if (!p.hasPermission("separeworlditems.cmd.reload")) {
                    p.sendMessage(MessageColors.getMsgColor(plugin.getMsgs().getString("general.no-permission")
                            .replaceAll(MessageColors.getMsgColor("%prefix%"), plugin.getMsgs().getString("general.prefix"))));
                    return true;
                }

                //reload config.yml
                plugin.getConfig().reload();
                //reload langs.yml
                plugin.getMsgs().reload();

                p.sendMessage(MessageColors.getMsgColor(plugin.getMsgs().getString("general.reload")
                        .replaceAll(MessageColors.getMsgColor("%prefix%"), plugin.getMsgs().getString("general.prefix"))));

                return true;

            }/* else if (args[0].equalsIgnoreCase("bypass1")){

                if (!p.hasPermission("separeworlditems.cmd.bypass1")) {
                    p.sendMessage(MessageColors.getMsgColor(plugin.getMsgs().getString("general.no-permission")
                            .replaceAll(MessageColors.getMsgColor("%prefix%"), plugin.getMsgs().getString("general.prefix"))));
                    return true;
                }

                p.sendMessage(MessageColors.getMsgColor(plugin.getMsgs().getString("general.bypass.bypass-warning-alert")
                        .replaceAll(MessageColors.getMsgColor("%prefix%"), plugin.getMsgs().getString("general.prefix"))));

                return true;

            }*/ else if (args[0].equalsIgnoreCase("bypass")){

                if (!p.hasPermission("separeworlditems.cmd.bypass")) {
                    p.sendMessage(MessageColors.getMsgColor(plugin.getMsgs().getString("general.no-permission")
                            .replaceAll(MessageColors.getMsgColor("%prefix%"), plugin.getMsgs().getString("general.prefix"))));
                    return true;
                }

                if (!plugin.getConfig().getBoolean("Options.bypass-world-options.use_bypass", false)) {
                    p.sendMessage(MessageColors.getMsgColor(plugin.getMsgs().getString("general.bypass.bypass-world-options.use_bypass")
                            .replaceAll(MessageColors.getMsgColor("%prefix%"), plugin.getMsgs().getString("general.prefix"))));
                    return true;

                }

                if (args.length == 1){
                    p.sendMessage(MessageColors.getMsgColor(plugin.getMsgs().getString("general.unknown-cmd-args")
                            .replaceAll(MessageColors.getMsgColor("%prefix%"), plugin.getMsgs().getString("general.prefix"))));

                    p.sendMessage(MessageColors.getMsgColor(plugin.getMsgs().getString("general.bypass.bypass-warning-alert")
                            .replaceAll(MessageColors.getMsgColor("%prefix%"), plugin.getMsgs().getString("general.prefix"))));

                    return true;
                }

                    if (args[1].equalsIgnoreCase("on")) {

                        p.sendMessage(MessageColors.getMsgColor(plugin.getMsgs().getString("general.bypass.bypass-warning-alert")
                                .replaceAll(MessageColors.getMsgColor("%prefix%"), plugin.getMsgs().getString("general.prefix"))));

                        plugin.getBypassSave().set("save-bypass." + p.getUniqueId() + ".name", p.getName());

                        System.out.println("worlds-1");
                        System.out.println("save-bypass." + p.getUniqueId() + ".worlds." + p.getWorld().getName());

                        plugin.getBypassSave().set("save-bypass." + p.getUniqueId() + ".worlds." + p.getWorld().getName() + ".inventory", p.getInventory().getContents());
                        plugin.getBypassSave().set("save-bypass." + p.getUniqueId() + ".worlds." + p.getWorld().getName() + ".armor", p.getInventory().getArmorContents());

                        plugin.getBypassSave().save();
                        plugin.getBypassSave().reload();

                        plugin.playerlist1.add(p.getUniqueId());

                        p.sendMessage(MessageColors.getMsgColor(plugin.getMsgs().getString("general.bypass.bypass-enabled")
                                .replaceAll(MessageColors.getMsgColor("%prefix%"), plugin.getMsgs().getString("general.prefix"))));

                        return true;

                    } else if (args[1].equalsIgnoreCase("off")) {

                        plugin.getBypassSave().getString("save-bypass." + p.getUniqueId() + ".name");
                        if (Objects.equals(plugin.getBypassSave().getString("save-bypass." + p.getUniqueId() + ".worlds." + p.getWorld().getName()), p.getWorld().getName())) {

                            String toConfig = plugin.getBypassSave().getString("save-bypass." + p.getUniqueId() + ".worlds.");

                            if (toConfig.contains(p.getWorld().getName() + ".inventory"))

                                for (String s : plugin.getBypassSave().getConfigurationSection("save-bypass." + p.getUniqueId() + ".worlds." + p.getWorld().getName() + ".inventory").getKeys(false)) {

                                    p.getInventory().setItem(Integer.parseInt(s), plugin.getBypassSave().getItemStack("inventory." + s));
                                }

                            for (String s : plugin.getBypassSave().getConfigurationSection("save-bypass." + p.getUniqueId() + ".worlds." + p.getWorld().getName() + ".armor").getKeys(false)) {

                                p.getInventory().setItem(Integer.parseInt(s), plugin.getBypassSave().getItemStack("armor." + s));
                            }

                            plugin.playerlist1.remove(p.getUniqueId());
                        }

                        plugin.getBypassSave().set("save-bypass." + p.getUniqueId() + ".worlds." + p.getWorld().getName(), null);
                        plugin.playerlist1.remove(p.getUniqueId());

                        p.sendMessage(MessageColors.getMsgColor(plugin.getMsgs().getString("general.bypass.bypass-disabled")
                                .replaceAll(MessageColors.getMsgColor("%prefix%"), plugin.getMsgs().getString("general.prefix"))));

                        return true;

                } else {
                    p.sendMessage(MessageColors.getMsgColor(plugin.getMsgs().getString("general.unknown-cmd-args")
                            .replaceAll(MessageColors.getMsgColor("%prefix%"), plugin.getMsgs().getString("general.prefix"))));
                    return true;
                }


            }

        }

        return true;
    }
}
