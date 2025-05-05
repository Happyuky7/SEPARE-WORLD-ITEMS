package com.github.happyuky7.separeWorldItems.data;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

public class FlyingData {

    public static void save(Player player, FileConfiguration config) {
        config.set("allow_flight", player.getAllowFlight());
        config.set("flying", player.isFlying());
    }

    public static void load(Player player, FileConfiguration config) {
        if (config.contains("allow_flight")) {
            player.setAllowFlight(config.getBoolean("allow_flight"));
        }
        if (config.contains("flying")) {
            player.setFlying(config.getBoolean("flying"));
        }
    }
}
