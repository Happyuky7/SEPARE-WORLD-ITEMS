package com.github.happyuky7.separeworlditems.data.loaders;

import org.bukkit.GameMode;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Utility class for loading player data such as attributes, potion effects, and
 * off-hand items from a configuration file.
 */
public class PlayerDataLoader {

    /**
     * Loads the player's attributes such as gamemode, flying state, health, hunger,
     * and experience from the configuration file.
     *
     * @param player The player whose attributes are being loaded.
     * @param config The configuration file where the data is loaded from.
     */
    public static void loadAttributes(Player player, FileConfiguration config) {
        player.setGameMode(GameMode.valueOf(config.getString("gamemode", "SURVIVAL")));
        player.setFlying(config.getBoolean("flying", false));
        player.setHealth(config.getDouble("health", 20.0D));
        player.setFoodLevel(config.getInt("hunger", 20));
        ExperienceLoader.load(player, config);
    }

    /**
     * Loads the player's active potion effects from the configuration file.
     *
     * @param player The player whose potion effects are being loaded.
     * @param config The configuration file where the data is loaded from.
     */
    public static void loadPotionEffects(Player player, FileConfiguration config) {
        if (config.contains("potion_effect")) {
            for (String key : config.getConfigurationSection("potion_effect").getKeys(false)) {
                @SuppressWarnings("deprecation")
                PotionEffect effect = new PotionEffect(
                        PotionEffectType.getByName(config.getString("potion_effect." + key + ".type")),
                        config.getInt("potion_effect." + key + ".duration"),
                        config.getInt("potion_effect." + key + ".level"));
                player.addPotionEffect(effect);
            }
        }
    }

    /**
     * Loads the player's off-hand item from the configuration file.
     *
     * @param player The player whose off-hand item is being loaded.
     * @param config The configuration file where the data is loaded from.
     */
    public static void loadOffHandItem(Player player, FileConfiguration config) {
        if (config.contains("off_hand_item")) {
            player.getInventory().setItemInOffHand(config.getItemStack("off_hand_item"));
        }
    }
}
