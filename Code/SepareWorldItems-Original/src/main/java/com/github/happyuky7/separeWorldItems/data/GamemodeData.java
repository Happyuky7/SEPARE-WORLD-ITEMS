package com.github.happyuky7.separeWorldItems.data;

import org.bukkit.GameMode;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

public class GamemodeData {

    public static void save(Player player, FileConfiguration config) {
        config.set("gamemode", player.getGameMode().toString());
    }

    public static void load(Player player, FileConfiguration config) {
        if (config.contains("gamemode")) {
            player.setGameMode(GameMode.valueOf(config.getString("gamemode")));
        }
    }

}
