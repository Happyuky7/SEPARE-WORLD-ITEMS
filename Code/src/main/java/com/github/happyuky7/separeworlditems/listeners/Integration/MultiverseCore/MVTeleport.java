package com.github.happyuky7.separeworlditems.listeners.base;

import com.github.happyuky7.separeworlditems.SepareWorldItems;
import com.github.happyuky7.separeworlditems.data.loaders.InventoryLoader;
import com.github.happyuky7.separeworlditems.data.loaders.PlayerDataLoader;
import com.github.happyuky7.separeworlditems.data.savers.InventorySaver;
import com.github.happyuky7.separeworlditems.data.savers.PlayerDataSaver;
import com.github.happyuky7.separeworlditems.filemanagers.FileManagerData;
import com.github.happyuky7.separeworlditems.utils.TeleportationManager;
import org.bukkit.GameMode;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;

import java.io.File;

public class MVTeleport implements Listener {

    private final SepareWorldItems plugin;

    public MVTeleport(SepareWorldItems plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        String message = event.getMessage();
        String[] args = message.split(" ");

        if ((args[0].equalsIgnoreCase("/mvtp") || args[0].equalsIgnoreCase("/mv tp")) && args.length > 1) {
            Player player = event.getPlayer();
            String toWorld = args[1];

            FileConfiguration config = plugin.getConfig();

            String fromWorld = player.getWorld().getName();

            if (config.contains("worlds." + fromWorld) && config.contains("worlds." + toWorld)) {
                String fromGroup = config.getString("worlds." + fromWorld);
                String toGroup = config.getString("worlds." + toWorld);

                savePlayerData(player, fromGroup);

                if (!fromGroup.equals(toGroup)) {
                    loadPlayerData(player, toGroup);
                } else {
                    reloadAllPlayerData(player, fromGroup);
                }

                event.setCancelled(false);
            }
        }
    }

    private void savePlayerData(Player player, String groupName) {
        File file = new File(plugin.getDataFolder() + File.separator + "groups"
                + File.separator + groupName + File.separator + player.getName() + "-" + player.getUniqueId() + ".yml");
        FileConfiguration config = FileManagerData.getYaml(file);

        InventorySaver.save(player, config);
        PlayerDataSaver.saveAttributes(player, config);
        PlayerDataSaver.savePotionEffects(player, config);
        PlayerDataSaver.saveOffHandItem(player, config);

        FileManagerData.saveConfiguration(file, config);
        clearPlayerState(player);
    }

    private void loadPlayerData(Player player, String groupName) {
        File file = new File(plugin.getDataFolder() + File.separator + "groups"
                + File.separator + groupName + File.separator + player.getName() + "-" + player.getUniqueId() + ".yml");
        FileConfiguration config = FileManagerData.getYaml(file);

        InventoryLoader.load(player, config);
        PlayerDataLoader.loadAttributes(player, config);
        PlayerDataLoader.loadPotionEffects(player, config);
        PlayerDataLoader.loadOffHandItem(player, config);
    }

    private void clearPlayerState(Player player) {
        player.getInventory().clear();
        player.getEnderChest().clear();
        player.setFlying(false);
        player.setGameMode(GameMode.SURVIVAL);
        player.setHealth(20.0D);
        player.setFoodLevel(20);
        player.setExp(0.0F);
        player.setLevel(0);
    }

    private void reloadAllPlayerData(Player player, String groupName) {
        File file = new File(plugin.getDataFolder() + File.separator + "groups"
                + File.separator + groupName + File.separator + player.getName() + "-" + player.getUniqueId() + ".yml");
        FileConfiguration config = FileManagerData.getYaml(file);

        InventoryLoader.load(player, config);
        PlayerDataLoader.loadAttributes(player, config);
        PlayerDataLoader.loadPotionEffects(player, config);
        PlayerDataLoader.loadOffHandItem(player, config);

        reloadExperienceAndLevel(player, config);
    }

    private void reloadExperienceAndLevel(Player player, FileConfiguration config) {
        if (config.contains("exp") && config.contains("exp-level")) {
            float experience = (float) config.getDouble("exp", 0.0F);
            int level = config.getInt("exp-level", 0);

            player.setExp(experience);
            player.setLevel(level);
        }
    }
}
