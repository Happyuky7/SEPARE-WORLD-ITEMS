package com.github.happyuky7.separeWorldItems.data;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class PotionEffectsData {

    public static void save(Player player, FileConfiguration config) {
        int index = 0;
        for (PotionEffect effect : player.getActivePotionEffects()) {
            config.set("potion_effect." + index + ".type", effect.getType().getName());
            config.set("potion_effect." + index + ".duration", effect.getDuration());
            config.set("potion_effect." + index + ".amplifier", effect.getAmplifier());
            index++;
        }
    }

    public static void load(Player player, FileConfiguration config) {
        if (config.contains("potion_effect")) {
            for (String key : config.getConfigurationSection("potion_effect").getKeys(false)) {
                PotionEffectType type = PotionEffectType.getByName(config.getString("potion_effect." + key + ".type"));
                int duration = config.getInt("potion_effect." + key + ".duration");
                int amplifier = config.getInt("potion_effect." + key + ".amplifier");
                player.addPotionEffect(new PotionEffect(type, duration, amplifier));
            }
        }
    }

}
