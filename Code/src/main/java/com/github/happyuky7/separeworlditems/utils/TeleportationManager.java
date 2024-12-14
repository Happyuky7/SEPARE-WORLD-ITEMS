package com.github.happyuky7.separeworlditems.utils;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Utility class that manages the state of players during teleportation events.
 * It keeps track of players who are in the process of teleporting, preventing
 * conflicts and ensuring smooth teleportation operations, such as saving and loading
 * player data across different worlds.
 * <p>
 * This class is crucial in scenarios where teleportation events are triggered,
 * and the system must ensure that a player does not get caught in multiple teleportation
 * events simultaneously. For example, when a player teleports between worlds, their data
 * must be saved before teleporting and reloaded afterward. The {@code TeleportationManager}
 * prevents the data from being overwritten or handled multiple times while the player is already teleporting.
 * </p>
 */
public class TeleportationManager {
    // Set to store UUIDs of players who are currently teleporting
    private static final Set<UUID> teleportingPlayers = new HashSet<>();

    /**
     * Marks a player as being in the process of teleporting or not.
     * <p>
     * This method is called before teleportation begins to mark the player as
     * being in a teleporting state. It ensures that any actions that require
     * the player to be out of teleportation (e.g., saving/loading player data)
     * will not interfere with the teleportation process.
     * </p>
     * <p>
     * Example usage:
     * <pre>
     * {@code TeleportationManager.setTeleporting(playerUUID, true); // Mark as teleporting}
     * {@code TeleportationManager.setTeleporting(playerUUID, false); // Mark as not teleporting}
     * </pre>
     * </p>
     *
     * @param playerUUID The unique identifier of the player.
     * @param isTeleporting A boolean indicating whether the player is teleporting.
     */
    public static void setTeleporting(UUID playerUUID, boolean isTeleporting) {
        if (isTeleporting) {
            teleportingPlayers.add(playerUUID);  // Add the player to the teleporting set
        } else {
            teleportingPlayers.remove(playerUUID);  // Remove the player from the teleporting set
        }
    }

    /**
     * Checks whether a player is currently in the process of teleporting.
     * <p>
     * This method is used to verify if a player is already teleporting, preventing
     * multiple teleportation processes from being triggered at once. This is particularly
     * important when there are multiple triggers for teleportation, such as home teleport
     * or world change.
     * </p>
     * <p>
     * Example usage:
     * <pre>
     * {@code if (TeleportationManager.isTeleporting(playerUUID)) { }
     *     // Skip processing since the player is already teleporting
     * }
     * </pre>
     * </p>
     *
     * @param playerUUID The unique identifier of the player.
     * @return {@code true} if the player is currently teleporting, {@code false} otherwise.
     */
    public static boolean isTeleporting(UUID playerUUID) {
        return teleportingPlayers.contains(playerUUID);  // Return true if player is in teleporting set
    }
}
