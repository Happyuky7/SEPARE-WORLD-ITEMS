package com.github.happyuky7.separeWorldItems.managers.integrations;

import com.github.happyuky7.separeWorldItems.SepareWorldItems;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.List;

public class WorldGuardManager {

    // Check WorldGuard Integration is Enabled
    public static boolean isWorldGuardEnabled() {
        return SepareWorldItems.getInstance().getConfig().getBoolean("integrations.worldguard.enabled", false)
                && SepareWorldItems.getInstance().getServer().getPluginManager().isPluginEnabled("WorldGuard");
    }

    // Get player region (max region priority)
    public static String getRegion(Player player) {
        if (!isWorldGuardEnabled()) {
            return null;
        }

        try {
            Location location = player.getLocation();
            World world = BukkitAdapter.adapt(player.getWorld());
            RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
            RegionManager regionsManager = container.get(world);
            
            if (regionsManager == null) {
                return null;
            }

            com.sk89q.worldguard.protection.ApplicableRegionSet regionSet = regionsManager.getApplicableRegions(BukkitAdapter.asBlockVector(location));

            // Get max priority region
            ProtectedRegion highestPriorityRegion = null;
            int highestPriority = Integer.MIN_VALUE;
            
            for (ProtectedRegion protectedRegion : regionSet) {
                if (protectedRegion.getPriority() > highestPriority) {
                    highestPriority = protectedRegion.getPriority();
                    highestPriorityRegion = protectedRegion;
                }
            }

            return highestPriorityRegion != null ? highestPriorityRegion.getId() : null;
        } catch (Exception e) {
            SepareWorldItems.getInstance().getLogger().warning("Error getting region for player " + player.getName() + ": " + e.getMessage());
            return null;
        }
    }

    // Get Group Name  based in player region location.
    public static String getGroupName(Player player) {
        if (!isWorldGuardEnabled()) {
            return getWorldGroup(player);
        }

        String region = getRegion(player);
        if (region != null) {
            String regionGroup = SepareWorldItems.getInstance().getConfig()
                .getString("worldguard-regions.regions." + region);
            
            if (regionGroup != null && !regionGroup.isEmpty()) {
                // Verify that the group is in the list of available groups
                List<String> availableGroups = SepareWorldItems.getInstance().getConfig()
                    .getStringList("worldguard-regions.groups");
                
                if (availableGroups.contains(regionGroup)) {
                    return regionGroup;
                }
            }
        }

        // If no exits using World Group
        return getWorldGroup(player);
    }

    // Get group name form region (from configuration)
    public static String getGroupNameByRegion(String regionName) {
        if (regionName == null || regionName.isEmpty()) {
            return null;
        }

        String regionGroup = SepareWorldItems.getInstance().getConfig()
            .getString("worldguard-regions.regions." + regionName);
        
        if (regionGroup != null && !regionGroup.isEmpty()) {
            List<String> availableGroups = SepareWorldItems.getInstance().getConfig()
                .getStringList("worldguard-regions.groups");
            
            if (availableGroups.contains(regionGroup)) {
                return regionGroup;
            }
        }

        return null;
    }

    // Get World Group
    public static String getWorldGroup(Player player) {
        String worldName = player.getWorld().getName();
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
