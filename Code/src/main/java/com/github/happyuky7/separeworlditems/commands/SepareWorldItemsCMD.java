package com.github.happyuky7.separeworlditems.commands;

/*
 * Code by: Happyuky7
 * GitHub: https://github.com/Happyuky7
 * License: Custom
 * Link: https://github.com/Happyuky7/SEPARE-WORLD-ITEMS
 */

import com.github.happyuky7.separeworlditems.SepareWorldItems;
import com.github.happyuky7.separeworlditems.managers.MessageManager;
import com.github.happyuky7.separeworlditems.utils.MessageColors;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Objects;

public class SepareWorldItemsCMD implements CommandExecutor {

    private final SepareWorldItems plugin;

    /**
     * Constructor for the command executor.
     *
     * @param plugin the main plugin instance
     */
    public SepareWorldItemsCMD(SepareWorldItems plugin) {
        this.plugin = plugin;
    }

    /**
     * Handles the execution of commands for SepareWorldItems.
     *
     * @param sender  the command sender (e.g., a player or console)
     * @param command the command being executed
     * @param label   the command label
     * @param args    the command arguments
     * @return true if the command was handled, false otherwise
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(MessageColors.getMsgColor("&cThis command can only be used by players."));
            return true;
        }

        Player player = (Player) sender;

        // Check for no arguments
        if (args.length == 0) {
            sendUnknownCommandMessage(player);
            return true;
        }

        // Process commands based on the first argument
        switch (args[0].toLowerCase()) {
            case "help":
                sendHelpMessage(player);
                break;
            case "info":
            case "version":
                sendPluginInfo(player);
                break;
            case "api":
                sendApiInfo(player);
                break;
            case "update":
                handleUpdateCommand(player);
                break;
            case "reload":
                handleReloadCommand(player);
                break;
            case "bypass":
                handleBypassCommand(player, args);
                break;
            default:
                sendUnknownCommandMessage(player);
                break;
        }

        return true;
    }

    /**
     * Sends a message for unknown commands.
     *
     * @param player the player to send the message to
     */
    private void sendUnknownCommandMessage(Player player) {
        player.sendMessage(MessageManager.getMessage("general.unknown-cmd-args")
                .replace("%prefix%", MessageManager.getMessage("general.prefix")));
    }

    /**
     * Sends the help message to the player.
     *
     * @param player the player to send the message to
     */
    private void sendHelpMessage(Player player) {
        player.sendMessage(MessageColors.getMsgColor("&r "));
        player.sendMessage(MessageColors.getMsgColor("&aSepareWorldItems &7| &3Commands:"));
        player.sendMessage(MessageColors.getMsgColor("&r "));
        player.sendMessage(MessageColors.getMsgColor("&7  <> Required &7&l|&r&7 [] Optional"));
        player.sendMessage(MessageColors.getMsgColor("&r "));
        player.sendMessage(MessageColors.getMsgColor("&f * &a/separeworlditems help &7| &fShow this help."));
        player.sendMessage(MessageColors.getMsgColor("&f * &a/separeworlditems info &7| &fShow plugin info."));
        player.sendMessage(MessageColors.getMsgColor("&f * &a/separeworlditems api &7| &fShow info about the API."));
        player.sendMessage(MessageColors.getMsgColor("&f * &a/separeworlditems update &7| &fUpdate the plugin info."));
        player.sendMessage(MessageColors
                .getMsgColor("&f * &a/separeworlditems reload &7| &fReload the plugin, config, and messages."));
        player.sendMessage(MessageColors.getMsgColor("&r "));
    }

    /**
     * Sends plugin information to the player.
     *
     * @param player the player to send the message to
     */
    private void sendPluginInfo(Player player) {
        player.sendMessage(MessageColors.getMsgColor("&r "));
        player.sendMessage(MessageColors.getMsgColor("&aSepareWorldItems &7| &3Info:"));
        player.sendMessage(MessageColors.getMsgColor("&r "));
        player.sendMessage(MessageColors.getMsgColor("&f * &9Version: &f" + plugin.version));
        player.sendMessage(MessageColors.getMsgColor("&f * &aCreated By: &fHappyuky7"));
        player.sendMessage(MessageColors.getMsgColor("&f * &dGitHub: &fhttps://github.com/Happyuky7"));
        player.sendMessage(MessageColors.getMsgColor("&f * &6Website: &fhttps://happyuky7.github.io/"));
        player.sendMessage(MessageColors.getMsgColor("&r "));
    }

    /**
     * Sends the API documentation link to the player.
     *
     * @param player the player to send the message to
     */
    private void sendApiInfo(Player player) {
        player.sendMessage(MessageColors.getMsgColor("&r "));
        player.sendMessage(MessageColors
                .getMsgColor("&8[&aSepareWorldItems&8]&r https://github.com/Happyuky7/SEPARE-WORLD-ITEMS/wiki/API"));
        player.sendMessage(MessageColors.getMsgColor("&r "));
    }

    /**
     * Handles the update command, providing the player with the update link.
     *
     * @param player the player executing the command
     */
    private void handleUpdateCommand(Player player) {
        if (!player.hasPermission("separeworlditems.cmd.update")) {
            player.sendMessage(MessageManager.getMessage("general.no-permission")
                    .replace("%prefix%", MessageManager.getMessage("general.prefix")));
            return;
        }

        player.sendMessage(MessageColors.getMsgColor("&r "));
        player.sendMessage(MessageColors.getMsgColor(
                "&8[&aSepareWorldItems&8]&r https://github.com/Happyuky7/SEPARE-WORLD-ITEMS/wiki#download"));
        player.sendMessage(MessageColors.getMsgColor("&r "));
    }

    /**
     * Handles the reload command, reloading config, messages and language.
     *
     * @param player the player executing the command
     */
    private void handleReloadCommand(Player player) {
        if (!player.hasPermission("separeworlditems.cmd.reload")) {
            player.sendMessage(MessageManager.getMessage("general.no-permission")
                    .replace("%prefix%", MessageManager.getMessage("general.prefix")));
            return;
        }

        plugin.getConfig().reload();
        plugin.getMsgs().reload();
        plugin.getLangs().reload();

        player.sendMessage(MessageManager.getMessage("general.reload")
                .replace("%prefix%", MessageManager.getMessage("general.prefix")));
    }

    /**
     * Handles the bypass command for a player. This command can enable or disable
     * the bypass mode for
     * inventory and armor management in specific worlds.
     *
     * @param player The player executing the command.
     * @param args   The arguments provided with the command.
     */
    private void handleBypassCommand(Player player, String[] args) {
        if (!plugin.getConfig().getBoolean("Options.bypass-world-options.use_bypass", false)) {
            player.sendMessage(MessageColors
                    .getMsgColor(plugin.getMsgs().getString("general.bypass.bypass-world-options.use_bypass")
                            .replace("%prefix%", plugin.getMsgs().getString("general.prefix"))));
            return;
        }

        if (args.length == 1) {
            sendBypassUsageMessage(player);
            return;
        }

        switch (args[1].toLowerCase()) {
            case "on":
                enableBypass(player);
                break;
            case "off":
                disableBypass(player);
                break;
            default:
                sendBypassUsageMessage(player);
                break;
        }
    }

    /**
     * Sends a usage message to the player indicating how to use the bypass command.
     *
     * @param player The player to send the message to.
     */
    private void sendBypassUsageMessage(Player player) {
        player.sendMessage(MessageColors.getMsgColor(plugin.getMsgs().getString("general.bypass.bypass-warning-alert")
                .replace("%prefix%", plugin.getMsgs().getString("general.prefix"))));
    }

    /**
     * Enables the bypass mode for a player's inventory and armor in the current
     * world.
     *
     * @param player The player enabling the bypass mode.
     */
    private void enableBypass(Player player) {
        plugin.getBypassSave().set("save-bypass." + player.getUniqueId() + ".name", player.getName());
        plugin.getBypassSave().set(
                "save-bypass." + player.getUniqueId() + ".worlds." + player.getWorld().getName() + ".inventory",
                player.getInventory().getContents());
        plugin.getBypassSave().set(
                "save-bypass." + player.getUniqueId() + ".worlds." + player.getWorld().getName() + ".armor",
                player.getInventory().getArmorContents());

        plugin.getBypassSave().save();
        plugin.getBypassSave().reload();
        plugin.playerlist1.add(player.getUniqueId());

        player.sendMessage(MessageColors.getMsgColor(plugin.getMsgs().getString("general.bypass.bypass-enabled")
                .replace("%prefix%", plugin.getMsgs().getString("general.prefix"))));
    }

    /**
     * Disables the bypass mode for a player's inventory and armor in the current
     * world.
     *
     * @param player The player disabling the bypass mode.
     */
    private void disableBypass(Player player) {
        String worldPath = "save-bypass." + player.getUniqueId() + ".worlds." + player.getWorld().getName();

        if (!Objects.equals(plugin.getBypassSave().getString(worldPath), player.getWorld().getName())) {
            player.sendMessage(MessageColors.getMsgColor(plugin.getMsgs().getString("general.bypass.no-data")
                    .replace("%prefix%", plugin.getMsgs().getString("general.prefix"))));
            return;
        }

        restorePlayerInventory(player, worldPath);

        plugin.getBypassSave().set(worldPath, null);
        plugin.playerlist1.remove(player.getUniqueId());

        player.sendMessage(MessageColors.getMsgColor(plugin.getMsgs().getString("general.bypass.bypass-disabled")
                .replace("%prefix%", plugin.getMsgs().getString("general.prefix"))));
    }

    /**
     * Restores the player's inventory and armor from saved data in the current
     * world.
     *
     * @param player    The player whose inventory and armor are being restored.
     * @param worldPath The path in the configuration file where the inventory and
     *                  armor data are stored.
     */
    private void restorePlayerInventory(Player player, String worldPath) {
        // Restore inventory items
        for (String key : plugin.getBypassSave().getConfigurationSection(worldPath + ".inventory").getKeys(false)) {
            player.getInventory().setItem(Integer.parseInt(key),
                    plugin.getBypassSave().getItemStack(worldPath + ".inventory." + key));
        }

        // Restore armor contents
        ItemStack[] armorContents = new ItemStack[4];
        for (int i = 0; i < armorContents.length; i++) {
            armorContents[i] = plugin.getBypassSave().getItemStack(worldPath + ".armor." + i);
        }
        player.getInventory().setArmorContents(armorContents);
    }

}
