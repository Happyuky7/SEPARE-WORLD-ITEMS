package com.github.happyuky7.separeworlditems.listeners.Integration.EssentialsX;

import com.github.happyuky7.separeworlditems.SepareWorldItems;
import com.github.happyuky7.separeworlditems.data.loaders.InventoryLoader;
import com.github.happyuky7.separeworlditems.data.loaders.PlayerDataLoader;
import com.github.happyuky7.separeworlditems.data.savers.InventorySaver;
import com.github.happyuky7.separeworlditems.data.savers.PlayerDataSaver;
import com.github.happyuky7.separeworlditems.filemanagers.FileManagerData;
import com.github.happyuky7.separeworlditems.utils.TeleportationManager;

import net.ess3.api.events.UserTeleportHomeEvent;

import org.bukkit.GameMode;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.io.File;

/**
 * Event listener that handles player teleport events specifically related to
 * home teleports.
 * Integrates with the EssentialsX plugin to manage player data during
 * teleportation.
 */
public class HomeEvent implements Listener {

    private final SepareWorldItems plugin;

    /**
     * Constructor for initializing the HomeEvent listener with the main plugin
     * instance.
     *
     * @param plugin The main plugin instance.
     */
    public HomeEvent(SepareWorldItems plugin) {
        this.plugin = plugin;
    }

    /**
     * Handles the user teleportation event when a player teleports to their home.
     * It ensures that player data is saved and reloaded appropriately for the
     * target world.
     *
     * @param event The event triggered when a player teleports to their home.
     */
    @EventHandler
    public void onUserTeleportHome(UserTeleportHomeEvent event) {
        @SuppressWarnings("deprecation")
        Player player = event.getUser().getBase().getPlayer();

        // Skip handling if the player is already in a teleportation process
        /*if (TeleportationManager.isTeleporting(player.getUniqueId())) {
            return; // Player is already teleporting, skip further handling
        }*/

        if (TeleportationManager.isTeleporting(player.getUniqueId())) {
            TeleportationManager.setTeleporting(player.getUniqueId(), false); // Reset the teleportation flag
            return; // Skip further processing
        }

        String fromWorld = player.getWorld().getName(); // Current world
        String toWorld = event.getHomeLocation().getWorld().getName(); // Target world (home)

        // Check if both the source and destination worlds are configured in the plugin
        FileConfiguration config = plugin.getConfig();
        if (config.contains("worlds." + fromWorld) && config.contains("worlds." + toWorld)) {
            String fromGroup = config.getString("worlds." + fromWorld);
            String toGroup = config.getString("worlds." + toWorld);

            // Mark the player as teleporting if the worlds differ
            if (!fromWorld.equals(toWorld)) {
                TeleportationManager.setTeleporting(player.getUniqueId(), true);
            }

            // Save player data for the source world
            savePlayerData(player, fromGroup);

            // Load or reload player data based on the group of the target world
            if (!fromGroup.equals(toGroup)) {
                loadPlayerData(player, toGroup);
            } else {
                reloadAllPlayerData(player, fromGroup);
            }
        }
    }

    /**
     * Saves the player's current data (inventory, attributes, potion effects, etc.)
     * to a group-specific configuration file before teleportation.
     *
     * @param player    The player whose data is being saved.
     * @param groupName The group name associated with the player's current world.
     */
    private void savePlayerData(Player player, String groupName) {
        File file = new File(plugin.getDataFolder() + File.separator + "groups"
                + File.separator + groupName + File.separator + player.getName() + "-" + player.getUniqueId() + ".yml");
        FileConfiguration config = FileManagerData.getYaml(file);

        // Save various player data into the configuration file
        InventorySaver.save(player, config);
        PlayerDataSaver.saveAttributes(player, config);
        PlayerDataSaver.savePotionEffects(player, config);
        PlayerDataSaver.saveOffHandItem(player, config);

        // Save the updated configuration to disk
        FileManagerData.saveConfiguration(file, config);

        // Clear the player's state in preparation for the teleportation
        clearPlayerState(player);
    }

    /**
     * Loads the player's data from a group-specific configuration file after
     * teleportation.
     *
     * @param player    The player whose data is being loaded.
     * @param groupName The group name associated with the target world.
     */
    private void loadPlayerData(Player player, String groupName) {
        File file = new File(plugin.getDataFolder() + File.separator + "groups"
                + File.separator + groupName + File.separator + player.getName() + "-" + player.getUniqueId() + ".yml");
        FileConfiguration config = FileManagerData.getYaml(file);

        // Load player data (inventory, attributes, potion effects, etc.)
        InventoryLoader.load(player, config);
        PlayerDataLoader.loadAttributes(player, config);
        PlayerDataLoader.loadPotionEffects(player, config);
        PlayerDataLoader.loadOffHandItem(player, config);
    }

    /**
     * Clears the player's state by resetting inventory, ender chest, game mode,
     * health,
     * food level, experience, and other attributes to ensure a clean state when the
     * player transitions between worlds.
     *
     * @param player The player whose state is being cleared.
     */
    private void clearPlayerState(Player player) {
        // Clear the player's inventory and ender chest
        player.getInventory().clear();
        player.getEnderChest().clear();

        // Reset the player's state to a default, clean state
        player.setFlying(false);
        player.setGameMode(GameMode.SURVIVAL);
        player.setHealth(20.0D); // Full health
        player.setFoodLevel(20); // Full hunger
        player.setExp(0.0F); // No experience
        player.setLevel(0); // No levels
    }

    /**
     * Reloads all of the player's data (inventory, attributes, potion effects,
     * etc.)
     * from a group-specific configuration file.
     *
     * @param player    The player whose data is being reloaded.
     * @param groupName The group name associated with the player's world.
     */
    private void reloadAllPlayerData(Player player, String groupName) {
        File file = new File(plugin.getDataFolder() + File.separator + "groups"
                + File.separator + groupName + File.separator + player.getName() + "-" + player.getUniqueId() + ".yml");
        FileConfiguration config = FileManagerData.getYaml(file);

        // Reload the player's data (inventory, attributes, potion effects, off-hand,
        // etc.)
        InventoryLoader.load(player, config);
        PlayerDataLoader.loadAttributes(player, config);
        PlayerDataLoader.loadPotionEffects(player, config);
        PlayerDataLoader.loadOffHandItem(player, config);

        // Reload the player's experience and level if necessary
        reloadExperienceAndLevel(player, config);
    }

    /**
     * Reloads the player's experience and level from a group-specific configuration
     * file to ensure they match the state from the previous world or group.
     *
     * @param player The player whose experience and level are being reloaded.
     * @param config The configuration file containing the player's data.
     */
    private void reloadExperienceAndLevel(Player player, FileConfiguration config) {
        // Check if experience and level data are present in the configuration
        if (config.contains("exp") && config.contains("exp-level")) {
            // Retrieve and set the player's experience and level
            float experience = (float) config.getDouble("exp", 0.0F);
            int level = config.getInt("exp-level", 0);

            // Set the player's experience and level from the loaded data
            player.setExp(experience);
            player.setLevel(level);
        }
    }
}
