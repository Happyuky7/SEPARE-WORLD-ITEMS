package com.github.happyuky7.separeworlditems.data.loaders;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

/**
 * Utility class for loading player inventory data from a configuration file.
 */
public class InventoryLoader {

    /**
     * Loads the player's inventory, ender chest, and armor contents from the
     * configuration file.
     *
     * @param player The player whose inventory is being loaded.
     * @param config The configuration file where the data is loaded from.
     */
    public static void load(Player player, FileConfiguration config) {
        if (config.contains("inventory")) {
            for (String key : config.getConfigurationSection("inventory").getKeys(false)) {
                player.getInventory().setItem(Integer.parseInt(key), config.getItemStack("inventory." + key));
            }
        }

        if (config.getBoolean("Options.ender-chest", true) && config.contains("ender_chest")) {
            for (String key : config.getConfigurationSection("ender_chest").getKeys(false)) {
                player.getEnderChest().setItem(Integer.parseInt(key), config.getItemStack("ender_chest." + key));
            }
        }
    }
}
