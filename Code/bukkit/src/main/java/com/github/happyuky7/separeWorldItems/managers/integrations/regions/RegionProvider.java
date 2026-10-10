package com.github.happyuky7.separeWorldItems.managers.integrations.regions;

import org.bukkit.Location;

import java.util.List;

/**
 * Lists the protection regions at a location, independent of the WorldGuard major version.
 * Implementations must ignore WorldGuard's implicit {@code __global__} region.
 */
public interface RegionProvider {

    /**
     * @return name shown in logs, e.g. "WorldGuard 7"
     */
    String name();

    List<RegionHit> regionsAt(Location location);

    final class RegionHit {
        private final String id;
        private final int priority;

        public RegionHit(String id, int priority) {
            this.id = id;
            this.priority = priority;
        }

        public String id() {
            return id;
        }

        public int priority() {
            return priority;
        }
    }
}
