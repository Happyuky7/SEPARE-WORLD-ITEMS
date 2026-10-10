package com.github.happyuky7.separeWorldItems.managers.integrations.regions;

import com.sk89q.worldguard.bukkit.WGBukkit;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.Location;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * WorldGuard 6.1 / 6.2 (Minecraft 1.8.8 - 1.12.2). Compiled against worldguard-legacy 6.2 in its own
 * source set and created by reflection, so the class is only touched on servers that run WorldGuard 6.
 */
public final class WorldGuard6Provider implements RegionProvider {

    @Override
    public String name() {
        return "WorldGuard 6";
    }

    @Override
    public List<RegionHit> regionsAt(Location location) {
        RegionManager manager = WGBukkit.getRegionManager(location.getWorld());
        if (manager == null) {
            return Collections.emptyList();
        }
        ApplicableRegionSet set = manager.getApplicableRegions(location);
        List<RegionHit> hits = new ArrayList<>();
        for (ProtectedRegion region : set) {
            if (region != null && region.getId() != null && !"__global__".equalsIgnoreCase(region.getId())) {
                hits.add(new RegionHit(region.getId(), region.getPriority()));
            }
        }
        return hits;
    }
}
