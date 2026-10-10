package com.github.happyuky7.separeWorldItems.data;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

public class ExpData {

    public static void save(Player player, FileConfiguration config) {
        config.set("exp", player.getExp());
        config.set("exp-level", player.getLevel());
    }

    public static void load(Player player, FileConfiguration config) {
        if (config.contains("exp")) {
            player.setExp((float) config.getDouble("exp"));
        }
        if (config.contains("exp-level")) {
            player.setLevel(config.getInt("exp-level"));
        }
    }

    public static void reload(Player player, FileConfiguration config) {
        if (config.contains("exp") && config.contains("exp-level")) {
            float experience = (float) config.getDouble("exp", 0.0F);
            int level = config.getInt("exp-level", 0);
            player.setExp(experience);
            player.setLevel(level);
        }
    }

}
