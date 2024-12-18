package com.github.happyuky7.separeworlditems.listeners.base;

import com.github.happyuky7.separeworlditems.SepareWorldItems;
import com.github.happyuky7.separeworlditems.data.loaders.InventoryLoader;
import com.github.happyuky7.separeworlditems.data.loaders.PlayerDataLoader;
import com.github.happyuky7.separeworlditems.data.savers.InventorySaver;
import com.github.happyuky7.separeworlditems.data.savers.PlayerDataSaver;
import com.github.happyuky7.separeworlditems.filemanagers.FileManager2;
import com.github.happyuky7.separeworlditems.utils.TeleportationManager;

import org.bukkit.GameMode;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;

import java.io.File;

/**
 * Event listener for handling player world change events.
 * Manages player data and inventory based on the world the player changes to.
 * Supports saving and loading data between different world groups.
 */
public class WorldChangeEvent implements Listener {

    private final SepareWorldItems plugin;

    /**
     * Constructs a new WorldChangeEvent listener.
     *
     * @param plugin The main plugin instance.
     */
    public WorldChangeEvent(SepareWorldItems plugin) {
        this.plugin = plugin;
    }

    /**
     * Handles the PlayerChangedWorldEvent. This method is triggered whenever a
     * player
     * changes worlds. It manages the player's data, such as inventory, attributes,
     * and other world-specific states.
     *
     * @param event The PlayerChangedWorldEvent that triggered this handler.
     */
    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();

        // Prevent further handling if the player is in the teleportation process
        // trigger by EssentialsX.
        if (TeleportationManager.isTeleporting(player.getUniqueId())) {
            TeleportationManager.setTeleporting(player.getUniqueId(), false); // Reset the teleportation flag
            return; // Skip further processing
        }

        // Retrieve the world names the player is transitioning between
        String fromWorld = event.getFrom().getName(); // Previous world
        String toWorld = player.getWorld().getName(); // Target world

        FileConfiguration config = plugin.getConfig();

        // Check if the worlds are configured in the plugin's settings
        if (config.contains("worlds." + fromWorld) && config.contains("worlds." + toWorld)) {
            // Retrieve the world groups the player is switching between
            String fromGroup = config.getString("worlds." + fromWorld);
            String toGroup = config.getString("worlds." + toWorld);

            // Save the player's data from the previous world before switching
            savePlayerData(player, fromGroup);

            // If worlds belong to different groups, load data for the new world
            if (!fromGroup.equals(toGroup)) {
                loadPlayerData(player, toGroup);
            } else {
                // If worlds belong to the same group, reload the player's data without changes
                reloadAllPlayerData(player, fromGroup);
            }
        }
    }

    /**
     * Saves the player's data (inventory, attributes, potion effects, etc.) to a
     * group-specific configuration file based on the player's current world group.
     *
     * @param player    The player whose data is being saved.
     * @param groupName The group name associated with the player's current world.
     */
    private void savePlayerData(Player player, String groupName) {
        File file = new File(plugin.getDataFolder() + File.separator + "groups"
                + File.separator + groupName + File.separator + player.getName() + "-" + player.getUniqueId() + ".yml");
        FileConfiguration config = FileManager2.getYaml(file);

        // Save various player data (inventory, attributes, potion effects, etc.)
        InventorySaver.save(player, config);
        PlayerDataSaver.saveAttributes(player, config);
        PlayerDataSaver.savePotionEffects(player, config);
        PlayerDataSaver.saveOffHandItem(player, config);

        // Persist the changes in the configuration file
        FileManager2.saveConfiguration(file, config);

        // Clear the player's state to prevent unwanted data persistence (e.g.,
        // inventory)
        clearPlayerState(player);
    }

    /**
     * Loads the player's data (inventory, attributes, potion effects, etc.) from a
     * group-specific configuration file based on the player's target world group.
     *
     * @param player    The player whose data is being loaded.
     * @param groupName The group name associated with the player's target world.
     */
    private void loadPlayerData(Player player, String groupName) {
        File file = new File(plugin.getDataFolder() + File.separator + "groups"
                + File.separator + groupName + File.separator + player.getName() + "-" + player.getUniqueId() + ".yml");
        FileConfiguration config = FileManager2.getYaml(file);

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
        FileConfiguration config = FileManager2.getYaml(file);

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
