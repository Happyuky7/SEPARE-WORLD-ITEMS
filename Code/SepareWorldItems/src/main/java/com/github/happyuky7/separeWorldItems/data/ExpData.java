package com.github.happyuky7.separeWorldItems.data;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

public class ExpData {

    public static void save(Player player, FileConfiguration config) {
        config.set("exp", player.getExp());
        config.set("level", player.getLevel());
    }

    public static void load(Player player, FileConfiguration config) {
        if (config.contains("exp")) {
            player.setExp((float) config.getDouble("exp"));
        }
        if (config.contains("level")) {
            player.setLevel(config.getInt("level"));
        }
    }

}
