package com.github.happyuky7.separeworlditems.data.loaders;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class InventoryLoader {
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
