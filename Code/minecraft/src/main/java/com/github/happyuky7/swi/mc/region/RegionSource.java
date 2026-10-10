package com.github.happyuky7.swi.mc.region;

import com.github.happyuky7.swi.api.data.DataNode;
import net.minecraft.server.level.ServerPlayer;

/**
 * A protection/claim mod that can decide the group of the area a player stands in (YAWP, Flan, Open Parties and Claims).
 * Each implementation follows how that mod stores its areas and reads its own section of {@code regions:} in the config.
 */
public interface RegionSource {

    /**
     * @return config key and log name, e.g. "yawp"
     */
    String id();

    /**
     * @param config this source's section, {@code regions.<id>} (never null, may be empty)
     * @return the configured group for the player's position, or null when no configured area applies
     */
    Match match(ServerPlayer player, DataNode config);

    /**
     * A configured area the player is in.
     */
    final class Match {
        private final String group;
        private final String areaId;
        private final int priority;

        public Match(String group, String areaId, int priority) {
            this.group = group;
            this.areaId = areaId;
            this.priority = priority;
        }

        public String group() {
            return group;
        }

        /** Human readable id of the area (region name, claim id...), used in logs and events. */
        public String areaId() {
            return areaId;
        }

        public int priority() {
            return priority;
        }
    }
}
