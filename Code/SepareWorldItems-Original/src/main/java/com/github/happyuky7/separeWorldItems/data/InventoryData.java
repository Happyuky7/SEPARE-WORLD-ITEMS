package com.github.happyuky7.separeWorldItems.data;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class InventoryData {

    public static void save(Player player, FileConfiguration config) {
        int index = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            config.set("inventory." + index++, item);
        }
    }

    public static void load(Player player, FileConfiguration config) {
        if (config.contains("inventory")) {
            for (String key : config.getConfigurationSection("inventory").getKeys(false)) {
                player.getInventory().setItem(Integer.parseInt(key), config.getItemStack("inventory." + key));
            }
        }
    }



}