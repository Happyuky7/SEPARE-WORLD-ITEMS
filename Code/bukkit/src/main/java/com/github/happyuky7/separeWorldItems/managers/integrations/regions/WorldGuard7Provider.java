package com.github.happyuky7.separeWorldItems.managers.integrations.regions;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.Location;

import java.util.ArrayList;
import java.util.List;

/**
 * WorldGuard 7.x (Minecraft 1.13 - 26.x). Only loaded when {@code com.sk89q.worldguard.WorldGuard} exists.
 */
public final class WorldGuard7Provider implements RegionProvider {

    @Override
    public String name() {
        return "WorldGuard 7";
    }

    @Override
    public List<RegionHit> regionsAt(Location location) {
        ApplicableRegionSet set = WorldGuard.getInstance().getPlatform().getRegionContainer().createQuery()
                .getApplicableRegions(BukkitAdapter.adapt(location));
        List<RegionHit> hits = new ArrayList<>();
        for (ProtectedRegion region : set) {
            if (region != null && region.getId() != null && !"__global__".equalsIgnoreCase(region.getId())) {
                hits.add(new RegionHit(region.getId(), region.getPriority()));
            }
        }
        return hits;
    }
}
