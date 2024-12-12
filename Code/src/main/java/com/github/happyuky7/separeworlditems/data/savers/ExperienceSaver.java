package com.github.happyuky7.separeworlditems.data.savers;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

public class ExperienceSaver {
    public static void save(Player player, FileConfiguration config) {
        config.set("exp", player.getExp());
        config.set("exp-level", player.getLevel());
    }
}
