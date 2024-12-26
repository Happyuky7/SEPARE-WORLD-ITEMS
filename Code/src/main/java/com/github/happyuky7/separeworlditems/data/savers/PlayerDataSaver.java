package com.github.happyuky7.separeworlditems.data.savers;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;

/**
 * Utility class for saving player data such as attributes, potion effects, and
 * off-hand items to a configuration file.
 */
public class PlayerDataSaver {

    /**
     * Saves the player's attributes such as gamemode, flying state, health, hunger,
     * and experience to the configuration file.
     *
     * @param player The player whose attributes are being saved.
     * @param config The configuration file where the data is saved.
     */
    public static void saveAttributes(Player player, FileConfiguration config) {
        // Save gamemode
        if (config.getBoolean("options.gamemode", true)) {
            config.set("gamemode", player.getGameMode().toString());
        }

        // Save flying state
        if (config.getBoolean("options.flying", true)) {
            config.set("flying", player.isFlying());
        }

        // Save health and hunger
        config.set("health", config.getBoolean("Options.health-options.health-default-save", true)
                ? player.getHealth()
                : 20.0D);
        config.set("hunger", player.getFoodLevel());

        ExperienceSaver.save(player, config);
    }

    /**
     * Saves the player's active potion effects to the configuration file.
     *
     * @param player The player whose potion effects are being saved.
     * @param config The configuration file where the data is saved.
     */
    @SuppressWarnings("deprecation")
    public static void savePotionEffects(Player player, FileConfiguration config) {
        int index = 0;
        for (PotionEffect effect : player.getActivePotionEffects()) {
            config.set("potion_effect." + index + ".type", effect.getType().getName());
            config.set("potion_effect." + index + ".level", effect.getAmplifier());
            config.set("potion_effect." + index + ".duration", effect.getDuration());
            index++;
        }
    }

    /**
     * Saves the player's off-hand item to the configuration file.
     *
     * @param player The player whose off-hand item is being saved.
     * @param config The configuration file where the data is saved.
     */
    public static void saveOffHandItem(Player player, FileConfiguration config) {
        config.set("off_hand_item", player.getInventory().getItemInOffHand());
    }
}
