package com.github.happyuky7.separeWorldItems.data.integrations;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import com.github.happyuky7.separeWorldItems.integrations.IntegrationAuraSkills;

public class AuraSkillsManaData {
    

    public static void save(Player player, FileConfiguration config) {
        config.set("auraskills.mana", IntegrationAuraSkills.getMana(player));
    }

    public static void load(Player player, FileConfiguration config) {
        if (config.contains("auraskills.mana")) {
            IntegrationAuraSkills.setMana(player, config.getDouble("auraskills.mana"));
        }
    }

    public static void cleardataState(Player player) {
        IntegrationAuraSkills.setMana(player, (double) 20);
    }

}
