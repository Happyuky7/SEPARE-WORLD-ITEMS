package com.github.happyuky7.separeWorldItems.data;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import com.github.happyuky7.separeWorldItems.integrations.IntegrationAuraSkills;

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
            config.set("auraskills.health", IntegrationAuraSkills.getHealth(player));
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
            if (config.contains("auraskills.health")) {
                IntegrationAuraSkills.setHealth(player, config.getDouble("auraskills.health"));
            }
        }

    }

    public static void cleardataState(Player player, String type) {
        if (type.equalsIgnoreCase("BUKKIT")) {
            player.setHealth(20);
            player.setMaxHealth(20);
        }

        if (type.equalsIgnoreCase("CUSTOM")) {
            System.out.println(System.Logger.Level.INFO + "Not loading health data for custom type (bypassing)");
        }

        if (type.equalsIgnoreCase("AURA_SKILLS")) {
           
            IntegrationAuraSkills.setHealth(player, (double) 20);
            
        }
    }

}
