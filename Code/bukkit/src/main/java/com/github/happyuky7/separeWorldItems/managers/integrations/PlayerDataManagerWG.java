package com.github.happyuky7.separeWorldItems.managers.integrations;

import com.github.happyuky7.separeWorldItems.SepareWorldItems;
import com.github.happyuky7.separeWorldItems.data.*;
import com.github.happyuky7.separeWorldItems.data.integrations.AuraSkillsManaData;
import com.github.happyuky7.separeWorldItems.storage.PlayerDataScope;
import com.github.happyuky7.separeWorldItems.utils.MCVersionChecker;
import org.bukkit.GameMode;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

public class PlayerDataManagerWG {

    // Save player data to a file
    public static void save(Player player, String groupName) {

        YamlConfiguration config = SepareWorldItems.getInstance().getPlayerDataStore()
            .load(PlayerDataScope.WORLDGUARD_GROUP, groupName, player.getUniqueId());

        com.github.happyuky7.separeWorldItems.data.PlayerDataSections.save(
                SepareWorldItems.getInstance(),
                player,
                PlayerDataScope.WORLDGUARD_GROUP,
                groupName,
                config
        );

        SepareWorldItems.getInstance().getPlayerDataStore()
                .save(PlayerDataScope.WORLDGUARD_GROUP, groupName, player.getUniqueId(), player.getName(), config);

        com.github.happyuky7.separeWorldItems.managers.InventoryChangeLogs.onSaved(
            SepareWorldItems.getInstance(),
            SepareWorldItems.getInstance().getPlayerDataStore(),
            PlayerDataScope.WORLDGUARD_GROUP,
            groupName,
            player,
            config
        );

    }


    // Load player data from a file
    public static void load(Player player, String groupName) {

        YamlConfiguration config = SepareWorldItems.getInstance().getPlayerDataStore()
            .load(PlayerDataScope.WORLDGUARD_GROUP, groupName, player.getUniqueId());

        com.github.happyuky7.separeWorldItems.data.PlayerDataSections.load(
                SepareWorldItems.getInstance(),
                player,
                PlayerDataScope.WORLDGUARD_GROUP,
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
            .load(PlayerDataScope.WORLDGUARD_GROUP, groupName, player.getUniqueId());

        if (SepareWorldItems.getInstance().getConfig().getBoolean("settings.options.saves.gamemode")) {
            GamemodeData.load(player, config);
        }

        if (SepareWorldItems.getInstance().getConfig().getBoolean("settings.options.saves.flying")) {
            try {
                FlyingData.load(player, config);
                System.out.println("Flying data loaded successfully.");
            } catch (Exception e) {
                System.out.println("Error loading flying data: " + e.getMessage());
            }
        }

        if (SepareWorldItems.getInstance().getConfig().getBoolean("settings.options.saves.fly-speed")) {
            FlySpeedData.load(player, config);
        }

        if (SepareWorldItems.getInstance().getConfig().getBoolean("settings.options.saves.exp")) {
            ExpData.reload(player, config);
        }

        if (SepareWorldItems.getInstance().getConfig().getBoolean("settings.options.saves.enderchest")) {
            EnderChestData.load(player, config);
        }

        if (SepareWorldItems.getInstance().getConfig().getBoolean("settings.options.saves.inventory")) {
            InventoryData.load(player, config);
        }

        if (SepareWorldItems.getInstance().getConfig().getBoolean("settings.options.saves.potion-effects")) {
            PotionEffectsData.load(player, config);
        }

        if (SepareWorldItems.getInstance().getConfig().getBoolean("settings.options.saves.food-level")) {
            FoodLevelData.load(player, config);
        }

        if (MCVersionChecker.isOffHandSupported()) {
            if (SepareWorldItems.getInstance().getConfig().getBoolean("settings.options.saves.off-hand")) {
                OffHandItemData.load(player, config);
            }
        }

        if (SepareWorldItems.getInstance().getConfig().getBoolean("settings.options.saves.health")) {
            HealthData.load(player, config, SepareWorldItems.getInstance().getConfig().getString("settings.health-options.type"));
        }

        if (SepareWorldItems.getInstance().getConfig().getBoolean("integrations.auraskills.enabled")) {

            if (SepareWorldItems.getInstance().getConfig().getBoolean("auraskills.save-mana")) {
                AuraSkillsManaData.load(player, config);
            }
        }

    }

}
