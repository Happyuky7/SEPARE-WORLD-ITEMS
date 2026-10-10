package com.github.happyuky7.separeWorldItems.data;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

public class FoodLevelData {

    public static void save(Player player, FileConfiguration config) {
        config.set("hunger", player.getFoodLevel());
    }

    public static void load(Player player, FileConfiguration config) {
        if (config.contains("hunger")) {
            player.setFoodLevel(config.getInt("hunger"));
        }
    }

}
