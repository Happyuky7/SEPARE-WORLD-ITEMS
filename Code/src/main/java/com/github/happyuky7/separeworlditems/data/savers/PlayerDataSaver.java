package com.github.happyuky7.separeworlditems.data.savers;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;

public class PlayerDataSaver {
    public static void saveAttributes(Player player, FileConfiguration config) {
        // Save gamemode
        if (config.getBoolean("Options.gamemode", true)) {
            config.set("gamemode", player.getGameMode().toString());
        }

        // Save flying state
        if (config.getBoolean("Options.flying", true)) {
            config.set("flying", player.isFlying());
        }

        // Save health and hunger
        config.set("health", config.getBoolean("Options.health-options.health-default-save", true)
                ? player.getHealth()
                : 20.0D);
        config.set("hunger", player.getFoodLevel());

        ExperienceSaver.save(player, config);
    }

    public static void savePotionEffects(Player player, FileConfiguration config) {
        int index = 0;
        for (PotionEffect effect : player.getActivePotionEffects()) {
            config.set("potion_effect." + index + ".type", effect.getType().getName());
            config.set("potion_effect." + index + ".level", effect.getAmplifier());
            config.set("potion_effect." + index + ".duration", effect.getDuration());
            index++;
        }
    }

    public static void saveOffHandItem(Player player, FileConfiguration config) {
        config.set("off_hand_item", player.getInventory().getItemInOffHand());
    }
}
