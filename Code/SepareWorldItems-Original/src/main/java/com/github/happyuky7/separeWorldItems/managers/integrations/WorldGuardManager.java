package com.github.happyuky7.separeWorldItems.managers.integrations;

import com.github.happyuky7.separeWorldItems.SepareWorldItems;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
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
                && SepareWorldItems.getInstance().getServer().getPluginManager().isPluginEnabled("WorldGuard");
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
            RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
            RegionQuery query = container.createQuery();
            ApplicableRegionSet regionSet = query.getApplicableRegions(BukkitAdapter.adapt(location));

            // Get max priority region
            ProtectedRegion highestPriorityRegion = null;
            int highestPriority = Integer.MIN_VALUE;
            
            for (ProtectedRegion protectedRegion : regionSet) {
                // Ignore the implicit global region so "no region" correctly falls back to world-group
                if (protectedRegion == null || "__global__".equalsIgnoreCase(protectedRegion.getId())) {
                    continue;
                }
                int priority = protectedRegion.getPriority();
                if (priority > highestPriority) {
                    highestPriority = priority;
                    highestPriorityRegion = protectedRegion;
                } else if (priority == highestPriority && highestPriorityRegion != null) {
                    // Stable tie-breaker: pick lexicographically smallest id when priorities match.
                    String idA = protectedRegion.getId();
                    String idB = highestPriorityRegion.getId();
                    if (idA != null && idB != null && idA.compareToIgnoreCase(idB) < 0) {
                        highestPriorityRegion = protectedRegion;
                    }
                } else if (priority == highestPriority && highestPriorityRegion == null) {
                    highestPriorityRegion = protectedRegion;
                }
            }

            return highestPriorityRegion != null ? highestPriorityRegion.getId() : null;
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
            RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
            RegionQuery query = container.createQuery();
            ApplicableRegionSet regionSet = query.getApplicableRegions(BukkitAdapter.adapt(location));

            ProtectedRegion highest = null;
            int highestPriority = Integer.MIN_VALUE;
            String worldName = location.getWorld().getName();

            for (ProtectedRegion region : regionSet) {
                if (region == null) {
                    continue;
                }
                String id = region.getId();
                if (id == null || id.isEmpty() || "__global__".equalsIgnoreCase(id)) {
                    continue;
                }

                String group = getConfiguredRegionGroup(worldName, id);
                if (!isConfiguredGroup(group)) {
                    continue;
                }

                int priority = region.getPriority();
                if (priority > highestPriority) {
                    highestPriority = priority;
                    highest = region;
                } else if (priority == highestPriority && highest != null) {
                    String idA = id;
                    String idB = highest.getId();
                    if (idA != null && idB != null && idA.compareToIgnoreCase(idB) < 0) {
                        highest = region;
                    }
                } else if (priority == highestPriority && highest == null) {
                    highest = region;
                }
            }

            return highest != null ? highest.getId() : null;
        } catch (Exception e) {
            SepareWorldItems.getInstance().getLogger().warning("Error getting configured region at location: " + e.getMessage());
            return null;
        }
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
