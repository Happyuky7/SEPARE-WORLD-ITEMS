package com.github.happyuky7.separeWorldItems.data;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

public class FlySpeedData {

    public static void save(Player player, FileConfiguration config) {
        config.set("fly_speed", player.getFlySpeed());
    }

    public static void load(Player player, FileConfiguration config) {
        if (config.contains("fly_speed")) {
            player.setFlySpeed((float) config.getDouble("fly_speed"));
        }
    }

}
