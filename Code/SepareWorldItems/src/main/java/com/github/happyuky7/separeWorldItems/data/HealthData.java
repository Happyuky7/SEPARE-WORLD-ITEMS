package com.github.happyuky7.separeWorldItems.data;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

public class HealthData {

    public static void save(Player player, FileConfiguration config, String type) {

        if (type.equalsIgnoreCase("BUKKIT")) {
            config.set("health", player.getHealth());
            config.set("max-health", player.getMaxHealth());
        }

        if (type.equalsIgnoreCase("CUSTOM")) {
            System.out.println(System.Logger.Level.INFO + "Not saving health data for custom type (bypassing)");
        }

        if (type.equalsIgnoreCase("AURA_SKILLS")) {
            System.out.println(System.Logger.Level.ERROR + "NOT IMPLEMENTED IN THIS VERSION");
        }

    }

    public static void load(Player player, FileConfiguration config, String type) {

        if (type.equalsIgnoreCase("BUKKIT")) {
            if (config.contains("health")) {
                player.setHealth(config.getDouble("health"));
            }
            if (config.contains("max-health")) {
                player.setMaxHealth(config.getDouble("max-health"));
            }
        }

        if (type.equalsIgnoreCase("CUSTOM")) {
            System.out.println(System.Logger.Level.INFO + "Not loading health data for custom type (bypassing)");
        }

        if (type.equalsIgnoreCase("AURA_SKILLS")) {
            System.out.println(System.Logger.Level.ERROR + "NOT IMPLEMENTED IN THIS VERSION");
        }

    }

}
