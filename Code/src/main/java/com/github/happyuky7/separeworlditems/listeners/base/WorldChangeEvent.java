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
 * Event listener for handling player world change events and managing player
 * data specific to different worlds.
 */
public class WorldChangeEvent implements Listener {

    private final SepareWorldItems plugin;

    /**
     * Constructor for WorldChangeEvent.
     *
     * @param plugin The main plugin instance.
     */
    public WorldChangeEvent(SepareWorldItems plugin) {
        this.plugin = plugin;
    }

    /**
     * Event handler for PlayerChangedWorldEvent. Manages inventory and player state
     * when a player changes worlds.
     *
     * @param event The PlayerChangedWorldEvent.
     */
    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();

        // Check if the teleportation flag is set
       if (TeleportationManager.isTeleporting(player.getUniqueId())) {
        TeleportationManager.setTeleporting(player.getUniqueId(), false); // Reset flag after check
        player.getServer().broadcastMessage("§7[Debug] Teleport flag cleared for player " + player.getName() + " after world change.");
        return; // Prevent further onWorldChange execution
    }

        // Continue with the world change handling
        String fromWorld = event.getFrom().getName();
        String toWorld = player.getWorld().getName();

        player.getServer().broadcastMessage("§7[Debug] Player " + player.getName() + " is changing worlds from " + fromWorld + " to " + toWorld);

        FileConfiguration config = plugin.getConfig();

        if (config.contains("worlds." + fromWorld) && config.contains("worlds." + toWorld)) {
            String fromGroup = config.getString("worlds." + fromWorld);
            String toGroup = config.getString("worlds." + toWorld);

            if (!fromGroup.equals(toGroup)) {
                player.getServer().broadcastMessage("§7[Debug] Worlds are in different groups. Saving and loading player data.");
                savePlayerData(player, fromGroup);
                loadPlayerData(player, toGroup);
            } else {
                player.getServer().broadcastMessage("§7[Debug] Worlds are in the same group. No need to reload data.");
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
}
