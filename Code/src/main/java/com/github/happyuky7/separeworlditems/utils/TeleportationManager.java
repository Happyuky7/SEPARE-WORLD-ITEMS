package com.github.happyuky7.separeworlditems.utils;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class TeleportationManager {
    private static final Set<UUID> teleportingPlayers = new HashSet<>();

    // Mark a player as teleporting
    public static void setTeleporting(UUID playerUUID, boolean isTeleporting) {
        if (isTeleporting) {
            teleportingPlayers.add(playerUUID);
        } else {
            teleportingPlayers.remove(playerUUID);
        }
    }

    // Check if a player is teleporting
    public static boolean isTeleporting(UUID playerUUID) {
        return teleportingPlayers.contains(playerUUID);
    }
}
