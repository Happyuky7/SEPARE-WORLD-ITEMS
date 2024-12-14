package com.github.happyuky7.separeworlditems.listeners.Integration.EssentialsX;

import com.github.happyuky7.separeworlditems.SepareWorldItems;
import com.github.happyuky7.separeworlditems.data.loaders.InventoryLoader;
import com.github.happyuky7.separeworlditems.data.loaders.PlayerDataLoader;
import com.github.happyuky7.separeworlditems.data.savers.InventorySaver;
import com.github.happyuky7.separeworlditems.data.savers.PlayerDataSaver;
import com.github.happyuky7.separeworlditems.filemanagers.FileManager2;
import com.github.happyuky7.separeworlditems.utils.TeleportationManager;

import net.ess3.api.events.UserTeleportHomeEvent;

import org.bukkit.GameMode;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.io.File;

/**
 * Event listener for handling player teleport events related to home teleports.
 * Integrates with EssentialsX plugin.
 */
public class HomeEvent implements Listener {

    private final SepareWorldItems plugin;

    /**
     * Constructor for UserTeleportHomeEvent.
     *
     * @param plugin The main plugin instance.
     */
    public HomeEvent(SepareWorldItems plugin) {
        this.plugin = plugin;
    }

    /**
     * Event handler for PlayerTeleportEvent. Handles the specific teleportation
     * behavior when a player teleports to their home.
     *
     * @param event The PlayerTeleportEvent.
     */
    @EventHandler
    public void onUserTeleportHome(UserTeleportHomeEvent event) {
        @SuppressWarnings("deprecation")
        Player player = event.getUser().getBase().getPlayer();
    
        // Check if the player is already teleporting to avoid double handling
        if (TeleportationManager.isTeleporting(player.getUniqueId())) {
            player.getServer().broadcastMessage("§7[Debug] Player " + player.getName() + " is already teleporting, skipping.");
            return; // Player is already in teleport process, so skip
        }
    
        String fromWorld = player.getWorld().getName(); // Current world
        String toWorld = event.getHomeLocation().getWorld().getName(); // Target world (home)
    
        // Check if the target world is configured
        FileConfiguration config = plugin.getConfig();
        if (config.contains("worlds." + fromWorld) && config.contains("worlds." + toWorld)) {
            String fromGroup = config.getString("worlds." + fromWorld);
            String toGroup = config.getString("worlds." + toWorld);
    
            // Only mark the player as teleporting if the groups are different
            if (!fromGroup.equals(toGroup)) {
                TeleportationManager.setTeleporting(player.getUniqueId(), true);
                player.getServer().broadcastMessage("§7[Debug] Player " + player.getName() + " is teleporting to home.");
    
                // Save current player data before proceeding
                savePlayerData(player, fromGroup);
    
                player.getServer().broadcastMessage("§7[Debug] Worlds are in different groups. Saving data and loading home data.");
                loadPlayerData(player, toGroup);
            } else {
                player.getServer().broadcastMessage("§7[Debug] Player " + player.getName() + " is in the same group, not setting teleport flag.");
                // Flag is not set, just reload data without changing the teleportation flag
                reloadAllPlayerData(player, fromGroup);
            }
        }
    }

    /**
     * Saves the player's current data to a group-specific configuration file.
     *
     * @param player    the player whose data is being saved
     * @param groupName the group name associated with the player's current world
     */
    private void savePlayerData(Player player, String groupName) {
        File file = new File(plugin.getDataFolder() + File.separator + "groups"
                + File.separator + groupName + File.separator + player.getName() + "-" + player.getUniqueId() + ".yml");
        FileConfiguration config = FileManager2.getYaml(file);

        InventorySaver.save(player, config);
        PlayerDataSaver.saveAttributes(player, config);
        PlayerDataSaver.savePotionEffects(player, config);
        PlayerDataSaver.saveOffHandItem(player, config);

        FileManager2.saveConfiguration(file, config);

        clearPlayerState(player);
    }

    /**
     * Loads the player's data from a group-specific configuration file.
     *
     * @param player    the player whose data is being loaded
     * @param groupName the group name associated with the player's target world
     */
    private void loadPlayerData(Player player, String groupName) {
        File file = new File(plugin.getDataFolder() + File.separator + "groups"
                + File.separator + groupName + File.separator + player.getName() + "-" + player.getUniqueId() + ".yml");
        FileConfiguration config = FileManager2.getYaml(file);

        InventoryLoader.load(player, config);
        PlayerDataLoader.loadAttributes(player, config);
        PlayerDataLoader.loadPotionEffects(player, config);
        PlayerDataLoader.loadOffHandItem(player, config);
    }

    /**
     * Clears the player's state by resetting their inventory, ender chest, flying
     * state, gamemode, health, hunger, experience, and level.
     *
     * @param player The player whose state is being cleared.
     */
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

    /**
     * Reloads the player's full data (inventory, attributes, potion effects, etc.)
     * from
     * the group-specific configuration file.
     *
     * @param player    The player whose full data is being reloaded.
     * @param groupName The group name associated with the player's data.
     */
    private void reloadAllPlayerData(Player player, String groupName) {
        File file = new File(plugin.getDataFolder() + File.separator + "groups"
                + File.separator + groupName + File.separator + player.getName() + "-" + player.getUniqueId() + ".yml");
        FileConfiguration config = FileManager2.getYaml(file);

        // Reload all player data (inventory, attributes, potion effects, off-hand,
        // etc.)
        InventoryLoader.load(player, config);
        PlayerDataLoader.loadAttributes(player, config);
        PlayerDataLoader.loadPotionEffects(player, config);
        PlayerDataLoader.loadOffHandItem(player, config);

        // Reload experience and level separately if needed
        reloadExperienceAndLevel(player, config);
    }

    /**
     * Reloads the player's experience and level from a group-specific configuration
     * file.
     *
     * @param player The player whose experience and level are being reloaded.
     * @param config The configuration file containing the player's data.
     */
    private void reloadExperienceAndLevel(Player player, FileConfiguration config) {
        // Check if experience and level data are present
        if (config.contains("exp") && config.contains("exp-level")) {
            float experience = (float) config.getDouble("exp", 0.0F);
            int level = config.getInt("exp-level", 0);

            // Reload experience and level
            player.setExp(experience);
            player.setLevel(level);
        }
    }
}
