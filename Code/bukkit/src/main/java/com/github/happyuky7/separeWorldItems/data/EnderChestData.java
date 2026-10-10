package com.github.happyuky7.separeWorldItems.data;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class EnderChestData {

    public static void save(Player player, FileConfiguration config) {
        int index = 0;
        for (ItemStack item : player.getEnderChest().getContents()) {
            config.set("ender-chest." + index++, item);
        }
    }

    public static void load(Player player, FileConfiguration config) {
        if (config.contains("ender-chest")) {
            for (String key : config.getConfigurationSection("ender-chest").getKeys(false)) {
                player.getEnderChest().setItem(Integer.parseInt(key), config.getItemStack("ender-chest." + key));
            }
        }
    }

}
