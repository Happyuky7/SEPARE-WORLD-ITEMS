package com.github.happyuky7.separeWorldItems.managers.integrations;

import com.github.happyuky7.separeWorldItems.SepareWorldItems;
import com.github.happyuky7.separeWorldItems.managers.integrations.regions.RegionProvider;
import com.github.happyuky7.separeWorldItems.managers.integrations.regions.RegionProvider.RegionHit;
import com.github.happyuky7.separeWorldItems.managers.integrations.regions.RegionProviders;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.List;

public class WorldGuardManager {

    private static boolean isConfiguredGroup(String group) {
        if (group == null || group.isEmpty()) {
            return false;
        }
        List<String> availableGroups = SepareWorldItems.getInstance().getConfig()
            .getStringList("worldguard-regions.groups");
        return availableGroups.contains(group);
    }

    // Check WorldGuard Integration is Enabled
    public static boolean isWorldGuardEnabled() {
        return SepareWorldItems.getInstance().getConfig().getBoolean("integrations.worldguard.enabled", false)
                && SepareWorldItems.getInstance().getServer().getPluginManager().isPluginEnabled("WorldGuard")
                && provider() != null;
    }

    /**
     * WorldGuard 7 (1.13+) or WorldGuard 6 (1.8.8 - 1.12.2), whichever is installed.
     */
    private static RegionProvider provider() {
        return RegionProviders.get(SepareWorldItems.getInstance().getLogger());
    }

    // Get player region (max region priority)
    public static String getRegion(Player player) {
        return player != null ? getRegionAt(player.getLocation()) : null;
    }

    // Get region at location (max region priority)
    public static String getRegionAt(Location location) {
        if (!isWorldGuardEnabled()) {
            return null;
        }

        try {
            // The provider already ignores the implicit __global__ region, so "no region" falls back to the world group.
            RegionHit highest = null;
            for (RegionHit hit : provider().regionsAt(location)) {
                highest = better(hit, highest);
            }
            return highest != null ? highest.id() : null;
        } catch (Exception e) {
            SepareWorldItems.getInstance().getLogger().warning("Error getting region at location: " + e.getMessage());
            return null;
        }
    }

    /**
     * Returns the highest-priority region at this location that is ALSO configured in
     * worldguard-regions.regions (legacy or per-world mapping) and whose group exists in
     * worldguard-regions.groups.
     *
     * This prevents unconfigured regions from overriding configured ones when priorities overlap.
     */
    public static String getConfiguredRegionAt(Location location) {
        if (!isWorldGuardEnabled() || location == null || location.getWorld() == null) {
            return null;
        }

        try {
            RegionHit highest = null;
            String worldName = location.getWorld().getName();
            for (RegionHit hit : provider().regionsAt(location)) {
                if (hit.id().isEmpty() || !isConfiguredGroup(getConfiguredRegionGroup(worldName, hit.id()))) {
                    continue;
                }
                highest = better(hit, highest);
            }
            return highest != null ? highest.id() : null;
        } catch (Exception e) {
            SepareWorldItems.getInstance().getLogger().warning("Error getting configured region at location: " + e.getMessage());
            return null;
        }
    }

    /**
     * Highest priority wins; equal priorities pick the lexicographically smallest id (stable tie-breaker).
     */
    private static RegionHit better(RegionHit candidate, RegionHit current) {
        if (current == null || candidate.priority() > current.priority()) {
            return candidate;
        }
        if (candidate.priority() == current.priority() && candidate.id().compareToIgnoreCase(current.id()) < 0) {
            return candidate;
        }
        return current;
    }

    /**
     * Returns the configured region group at a location, or null if none.
     */
    public static String getConfiguredRegionGroupAt(Location location) {
        if (!isWorldGuardEnabled() || location == null || location.getWorld() == null) {
            return null;
        }

        String regionId = getConfiguredRegionAt(location);
        if (regionId == null || regionId.isEmpty()) {
            return null;
        }

        String group = getConfiguredRegionGroup(location.getWorld().getName(), regionId);
        return isConfiguredGroup(group) ? group : null;
    }

    // Get Group Name  based in player region location.
    public static String getGroupName(Player player) {
        if (!isWorldGuardEnabled()) {
            return getWorldGroup(player);
        }

        String regionGroup = getConfiguredRegionGroupAt(player.getLocation());
        if (regionGroup != null && !regionGroup.isEmpty()) {
            return regionGroup;
        }

        // If no exits using World Group
        return getWorldGroup(player);
    }

    // Get group name form region (from configuration)
    public static String getGroupNameByRegion(String regionName) {
        return getGroupNameByRegion(null, regionName);
    }

    /**
     * Gets group name for a region.
     * Supports both legacy flat mapping:
     *   worldguard-regions.regions.<region> = <group>
     * And per-world mapping:
     *   worldguard-regions.regions.<world>.<region> = <group>
     */
    public static String getGroupNameByRegion(String worldName, String regionName) {
        if (regionName == null || regionName.isEmpty()) {
            return null;
        }

        String regionGroup = getConfiguredRegionGroup(worldName, regionName);
        
        if (isConfiguredGroup(regionGroup)) {
            return regionGroup;
        }

        return null;
    }

    private static String getConfiguredRegionGroup(String worldName, String regionName) {
        // Prefer per-world config if present
        if (worldName != null && !worldName.isEmpty()) {
            String perWorld = SepareWorldItems.getInstance().getConfig()
                .getString("worldguard-regions.regions." + worldName + "." + regionName);
            if (perWorld != null && !perWorld.isEmpty()) {
                return perWorld;
            }
        }

        // Legacy global region mapping
        return SepareWorldItems.getInstance().getConfig()
            .getString("worldguard-regions.regions." + regionName);
    }

    // Get World Group
    public static String getWorldGroup(Player player) {
        return player != null ? getWorldGroup(player.getWorld().getName()) : "default";
    }

    // Get World Group by world name
    public static String getWorldGroup(String worldName) {
        String worldGroup = SepareWorldItems.getInstance().getConfig().getString("worlds." + worldName);
        
        if (worldGroup != null && !worldGroup.isEmpty()) {
            return worldGroup;
        }

        // Fallback World Group
        if (SepareWorldItems.getInstance().getConfig().getBoolean("settings.options.default-group.enabled", false)) {
            return SepareWorldItems.getInstance().getConfig().getString("settings.options.default-group.group", "default");
        }

        return "default";
    }
}
