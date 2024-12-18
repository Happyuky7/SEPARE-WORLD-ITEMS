package com.github.happyuky7.separeworlditems.data.loaders;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

/**
 * Utility class for loading player experience data from a configuration file.
 */
public class ExperienceLoader {

    /**
     * Loads the player's experience and level from the configuration file.
     *
     * @param player The player whose experience and level are being loaded.
     * @param config The configuration file where the data is loaded from.
     */
    public static void load(Player player, FileConfiguration config) {
        player.setExp((float) config.getDouble("exp", 0.0F));
        player.setLevel(config.getInt("exp-level", 0));
    }
}
