package com.github.happyuky7.separeworlditems.data.loaders;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

public class ExperienceLoader {
    public static void load(Player player, FileConfiguration config) {
        player.setExp((float) config.getDouble("exp", 0.0F));
        player.setLevel(config.getInt("exp-level", 0));
    }
}
