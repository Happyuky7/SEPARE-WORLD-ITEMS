package com.github.happyuky7.separeworlditems.data.savers;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

/**
 * Utility class for saving player experience data to a configuration file.
 */
public class ExperienceSaver {

    /**
     * Saves the player's experience and level to the configuration file.
     *
     * @param player The player whose experience and level are being saved.
     * @param config The configuration file where the data is saved.
     */
    public static void save(Player player, FileConfiguration config) {
        config.set("exp", player.getExp());
        config.set("exp-level", player.getLevel());
    }
}
