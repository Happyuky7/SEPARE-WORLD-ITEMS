package com.github.happyuky7.separeWorldItems.integrations;

import com.viaversion.viaversion.api.Via;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * Optional ViaVersion support without a hard dependency.
 * Uses reflection so the plugin can run even if ViaVersion is not installed.
 */
public final class ViaVersionHook {

    private ViaVersionHook() {
    }

    public static boolean isEnabled() {
        try {
            return Bukkit.getPluginManager().isPluginEnabled("ViaVersion");
        } catch (Throwable ignored) {
            return false;
        }
    }

    /**
     * Returns the player's client protocol version (e.g. 765, 766...) when ViaVersion is installed.
     * Returns null when ViaVersion is not present or any reflection call fails.
     */
    public static Integer getPlayerProtocolVersion(Player player) {
        if (player == null || !isEnabled()) {
            return null;
        }

        try {
            return Via.getAPI().getPlayerVersion(player.getUniqueId());
        } catch (NoClassDefFoundError ignored) {
            // ViaVersion not present at runtime
            return null;
        } catch (Throwable ignored) {
            return null;
        }
    }
}
