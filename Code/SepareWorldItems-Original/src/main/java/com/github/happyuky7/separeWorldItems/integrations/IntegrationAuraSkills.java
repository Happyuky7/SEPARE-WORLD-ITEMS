package com.github.happyuky7.separeWorldItems.integrations;

import org.bukkit.entity.Player;

import dev.aurelium.auraskills.api.AuraSkillsApi;
import dev.aurelium.auraskills.api.skill.Skills;
import dev.aurelium.auraskills.api.user.SkillsUser;

public class IntegrationAuraSkills {

    public static boolean isAuraSkills() {
        try {
            Class.forName("dev.aurelium.auraskills.api.AuraSkillsApi");
            return AuraSkillsApi.get() != null;
        } catch (ClassNotFoundException | NoClassDefFoundError e) {
            return false;
        }
    }
    public static double getHealth(Player player) {
        AuraSkillsApi auraSkills = AuraSkillsApi.get();
        SkillsUser user = auraSkills.getUser(player.getUniqueId());
        
        // Get the health skill level
        int healthLevel = user.getSkillLevel(Skills.ENDURANCE);
        
        // Calculate health based on skill level (base health + bonus from skill)
        double baseHealth = 20.0; // Default Minecraft health
        double healthBonus = healthLevel * 2.0; // 2 HP per level (configurable)
        
        return baseHealth + healthBonus;
    }

    public static void setHealth(Player player, double health) {
        AuraSkillsApi auraSkills = AuraSkillsApi.get();
        SkillsUser user = auraSkills.getUser(player.getUniqueId());
        
        // Calculate the required endurance level to achieve the target health
        double baseHealth = 20.0;
        double healthBonus = health - baseHealth;
        int requiredLevel = Math.max(0, (int) Math.round(healthBonus / 2.0));
        
        // Set the endurance skill level
        user.setSkillLevel(Skills.ENDURANCE, requiredLevel);
    }

    public static double getMana(Player player) {
        AuraSkillsApi auraSkills = AuraSkillsApi.get();
        SkillsUser user = auraSkills.getUser(player.getUniqueId());
        
        // Get the mana from the user
        return user.getMana();
    }

    public static void setMana(Player player, double mana) {
        AuraSkillsApi auraSkills = AuraSkillsApi.get();
        SkillsUser user = auraSkills.getUser(player.getUniqueId());
        
        // Set the mana for the user
        user.setMana(mana);
    }

}
