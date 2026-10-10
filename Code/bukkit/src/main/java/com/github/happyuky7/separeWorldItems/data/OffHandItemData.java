package com.github.happyuky7.separeWorldItems.data;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

public class OffHandItemData {

    public static void save(Player player, FileConfiguration config) {
        config.set("off_hand_item", player.getInventory().getItemInOffHand());
    }

    public static void load(Player player, FileConfiguration config) {
        if (config.contains("off_hand_item")) {
            player.getInventory().setItemInOffHand(config.getItemStack("off_hand_item"));
        }
    }

}
