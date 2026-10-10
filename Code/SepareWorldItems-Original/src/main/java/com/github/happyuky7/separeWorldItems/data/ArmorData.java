package com.github.happyuky7.separeWorldItems.data;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

public class ArmorData {

    public static void save(Player player, FileConfiguration config) {
        config.set("armor_contents.helmet", player.getInventory().getHelmet());
        config.set("armor_contents.chestplate", player.getInventory().getChestplate());
        config.set("armor_contents.leggings", player.getInventory().getLeggings());
        config.set("armor_contents.boots", player.getInventory().getBoots());
    }

    public static void load(Player player, FileConfiguration config) {
        if (config.contains("armor_contents")) {
            player.getInventory().setHelmet(config.getItemStack("armor_contents.helmet"));
            player.getInventory().setChestplate(config.getItemStack("armor_contents.chestplate"));
            player.getInventory().setLeggings(config.getItemStack("armor_contents.leggings"));
            player.getInventory().setBoots(config.getItemStack("armor_contents.boots"));
        }
    }
}
