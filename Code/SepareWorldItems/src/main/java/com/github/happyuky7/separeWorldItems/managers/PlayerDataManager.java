package com.github.happyuky7.separeWorldItems.managers;

import com.github.happyuky7.separeWorldItems.SepareWorldItems;
import com.github.happyuky7.separeWorldItems.data.*;
import com.github.happyuky7.separeWorldItems.files.FileManagerData;
import com.github.happyuky7.separeWorldItems.utils.MCVersionChecker;
import org.bukkit.GameMode;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.io.File;

public class PlayerDataManager {

    // Save player data to a file
    public static void save(Player player, String groupName) {

        File file = new File(SepareWorldItems.getInstance().getDataFolder() + File.separator
                + "groups" + File.separator + groupName + File.separator
                + player.getName() + "-" + player.getUniqueId() + ".yml");

        FileConfiguration config = FileManagerData.getYaml(file);

        if (SepareWorldItems.getInstance().getConfig().getBoolean("settings.options.saves.gamemode")) {
            GamemodeData.save(player, config);
        }

        if (SepareWorldItems.getInstance().getConfig().getBoolean("settings.options.saves.flying")) {
            FlyingData.save(player, config);
        }

        if (SepareWorldItems.getInstance().getConfig().getBoolean("settings.options.saves.fly-speed")) {
            FlySpeedData.save(player, config);
        }

        if (SepareWorldItems.getInstance().getConfig().getBoolean("settings.options.saves.exp")) {
            ExpData.save(player, config);
        }

        if (SepareWorldItems.getInstance().getConfig().getBoolean("settings.options.saves.enderchest")) {
            EnderChestData.save(player, config);
        }

        if (SepareWorldItems.getInstance().getConfig().getBoolean("settings.options.saves.inventory")) {
            InventoryData.save(player, config);
        }

        if (SepareWorldItems.getInstance().getConfig().getBoolean("settings.options.saves.potion-effects")) {
            PotionEffectsData.save(player, config);
        }

        if (SepareWorldItems.getInstance().getConfig().getBoolean("settings.options.saves.food-level")) {
            FoodLevelData.save(player, config);
        }

        if (MCVersionChecker.isOffHandSupported()) {
            if (SepareWorldItems.getInstance().getConfig().getBoolean("settings.options.saves.off-hand")) {
                OffHandItemData.save(player, config);
            }
        }

        if (SepareWorldItems.getInstance().getConfig().getBoolean("settings.options.saves.health")) {
            HealthData.save(player, config, SepareWorldItems.getInstance().getConfig().getString("settings.health-options.type"));
        }

        FileManagerData.saveConfiguration(file, config);

        cleardataState(player);

    }


    // Load player data from a file
    public static void load(Player player, String groupName) {

        File file = new File(SepareWorldItems.getInstance().getDataFolder() + File.separator
                + "groups" + File.separator + groupName + File.separator
                + player.getName() + "-" + player.getUniqueId() + ".yml");

        FileConfiguration config = FileManagerData.getYaml(file);

        if (SepareWorldItems.getInstance().getConfig().getBoolean("settings.options.saves.gamemode")) {
            GamemodeData.load(player, config);
        }

        if (SepareWorldItems.getInstance().getConfig().getBoolean("settings.options.saves.flying")) {
            FlyingData.load(player, config);
        }

        if (SepareWorldItems.getInstance().getConfig().getBoolean("settings.options.saves.fly-speed")) {
            FlySpeedData.load(player, config);
        }

        if (SepareWorldItems.getInstance().getConfig().getBoolean("settings.options.saves.exp")) {
            ExpData.load(player, config);
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

    }


    // Clear player state
    public static void cleardataState(Player player) {

        player.setExp(0);
        player.setLevel(0);

        player.setFoodLevel(20);
        player.setHealth(20);

        player.setAllowFlight(false);
        player.setFlying(false);
        player.setFlySpeed(0.1f);

        player.setGameMode(GameMode.SURVIVAL);

        player.getInventory().clear();
        player.getEnderChest().clear();
        player.getInventory().setArmorContents(null);

        player.getActivePotionEffects().forEach(potionEffect -> player.removePotionEffect(potionEffect.getType()));

    }


    // Reload all player data
    public static void reloadAllPlayerData(Player player, String groupName) {

        File file = new File(SepareWorldItems.getInstance().getDataFolder() + File.separator
                + "groups" + File.separator + groupName + File.separator
                + player.getName() + "-" + player.getUniqueId() + ".yml");

        FileConfiguration config = FileManagerData.getYaml(file);

        if (SepareWorldItems.getInstance().getConfig().getBoolean("settings.options.saves.gamemode")) {
            GamemodeData.load(player, config);
        }

        if (SepareWorldItems.getInstance().getConfig().getBoolean("settings.options.saves.flying")) {
            FlyingData.load(player, config);
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

    }

}
