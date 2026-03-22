package com.github.happyuky7.separeWorldItems.managers;

import com.github.happyuky7.separeWorldItems.SepareWorldItems;
import com.github.happyuky7.separeWorldItems.data.*;
import com.github.happyuky7.separeWorldItems.data.integrations.AuraSkillsManaData;
import com.github.happyuky7.separeWorldItems.storage.PlayerDataScope;
import com.github.happyuky7.separeWorldItems.utils.MCVersionChecker;
import org.bukkit.GameMode;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

public class PlayerDataManager {

    // Save player data to a file
    public static void save(Player player, String groupName) {

        YamlConfiguration config = SepareWorldItems.getInstance().getPlayerDataStore()
            .load(PlayerDataScope.WORLD_GROUP, groupName, player.getUniqueId());

        // Apply section save rules (global + optional per-group overrides)
        com.github.happyuky7.separeWorldItems.data.PlayerDataSections.save(
                SepareWorldItems.getInstance(),
                player,
                PlayerDataScope.WORLD_GROUP,
                groupName,
                config
        );

        SepareWorldItems.getInstance().getPlayerDataStore()
            .save(PlayerDataScope.WORLD_GROUP, groupName, player.getUniqueId(), player.getName(), config);

        InventoryChangeLogs.onSaved(
            SepareWorldItems.getInstance(),
            SepareWorldItems.getInstance().getPlayerDataStore(),
            PlayerDataScope.WORLD_GROUP,
            groupName,
            player,
            config
        );

    }


    // Load player data from a file
    public static void load(Player player, String groupName) {

        YamlConfiguration config = SepareWorldItems.getInstance().getPlayerDataStore()
            .load(PlayerDataScope.WORLD_GROUP, groupName, player.getUniqueId());

        com.github.happyuky7.separeWorldItems.data.PlayerDataSections.load(
                SepareWorldItems.getInstance(),
                player,
                PlayerDataScope.WORLD_GROUP,
                groupName,
                config
        );


    }


    // Clear player state
    public static void cleardataState(Player player) {

        player.setExp(0);
        player.setLevel(0);

        player.setFoodLevel(20);

        HealthData.cleardataState(player, SepareWorldItems.getInstance().getConfig().getString("settings.health-options.type"));
    
        player.setAllowFlight(false);
        player.setFlying(false);
        player.setFlySpeed(0.1f);

        player.setGameMode(GameMode.SURVIVAL);

        player.getInventory().clear();
        player.getEnderChest().clear();
        player.getInventory().setArmorContents(null);

        player.getActivePotionEffects().forEach(potionEffect -> player.removePotionEffect(potionEffect.getType()));

        if (SepareWorldItems.getInstance().getConfig().getBoolean("integrations.auraskills.enabled")) {

            if (SepareWorldItems.getInstance().getConfig().getBoolean("auraskills.save-mana")) {
                AuraSkillsManaData.cleardataState(player);
            }
        }


    }


    // Reload all player data
    public static void reloadAllPlayerData(Player player, String groupName) {

        YamlConfiguration config = SepareWorldItems.getInstance().getPlayerDataStore()
            .load(PlayerDataScope.WORLD_GROUP, groupName, player.getUniqueId());

        com.github.happyuky7.separeWorldItems.data.PlayerDataSections.reload(
                SepareWorldItems.getInstance(),
                player,
                PlayerDataScope.WORLD_GROUP,
                groupName,
                config
        );

    }

}
