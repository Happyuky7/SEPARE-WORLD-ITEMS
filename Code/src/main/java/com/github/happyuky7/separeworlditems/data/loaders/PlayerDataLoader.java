package com.github.happyuky7.separeworlditems.data.loaders;

import org.bukkit.GameMode;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class PlayerDataLoader {
    public static void loadAttributes(Player player, FileConfiguration config) {
        player.setGameMode(GameMode.valueOf(config.getString("gamemode", "SURVIVAL")));
        player.setFlying(config.getBoolean("flying", false));
        player.setHealth(config.getDouble("health", 20.0D));
        player.setFoodLevel(config.getInt("hunger", 20));
        ExperienceLoader.load(player, config);
    }

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

    public static void loadOffHandItem(Player player, FileConfiguration config) {
        if (config.contains("off_hand_item")) {
            player.getInventory().setItemInOffHand(config.getItemStack("off_hand_item"));
        }
    }
}
