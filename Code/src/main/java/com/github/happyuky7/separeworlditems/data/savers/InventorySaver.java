package com.github.happyuky7.separeworlditems.data.savers;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Utility class for saving player inventory data to a configuration file.
 */
public class InventorySaver {

    /**
     * Saves the player's inventory, ender chest, and armor contents to the
     * configuration file.
     *
     * @param player The player whose inventory is being saved.
     * @param config The configuration file where the data is saved.
     */
    public static void save(Player player, FileConfiguration config) {
        // Save inventory contents
        int index = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            config.set("inventory." + index++, item);
        }

        // Save ender chest contents if enabled
        if (config.getBoolean("Options.ender-chest", true)) {
            index = 0;
            for (ItemStack item : player.getEnderChest().getContents()) {
                config.set("ender_chest." + index++, item);
            }
        }

        // Save armor contents
        config.set("armor_contents.helmet", player.getInventory().getHelmet());
        config.set("armor_contents.chestplate", player.getInventory().getChestplate());
        config.set("armor_contents.leggings", player.getInventory().getLeggings());
        config.set("armor_contents.boots", player.getInventory().getBoots());
    }
}
