package com.github.happyuky7.swi.mc.region;

import com.github.happyuky7.swi.api.data.DataNode;
import com.github.happyuky7.swi.common.config.SwiSettings;
import com.github.happyuky7.swi.common.platform.SwiLogger;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Region-based groups for mods (the mod-world equivalent of the plugin's {@code worldguard-regions}).
 *
 * <pre>
 * regions:
 *   enabled: true
 *   check-interval-ticks: 10
 *   groups: [spawngroup, claims]
 *   yawp:  { "*": { spawn: spawngroup } }
 *   flan:  { any-claim: claims }
 *   opac:  { parties: { "Builders": claims } }
 * </pre>
 *
 * Sources are asked in registration order; the first configured match wins (YAWP regions have priorities).
 */
public final class ModRegions {

    private final List<RegionSource> sources = new CopyOnWriteArrayList<>();

    public void register(RegionSource source, SwiLogger logger) {
        sources.add(source);
        logger.info("[Regions] " + source.id() + " areas can switch groups (configure regions." + source.id() + ")");
    }

    public boolean hasSources() {
        return !sources.isEmpty();
    }

    public static boolean enabled(SwiSettings settings) {
        return settings.raw().getBoolean("regions.enabled", false);
    }

    public static int intervalTicks(SwiSettings settings) {
        return Math.max(1, settings.raw().getInt("regions.check-interval-ticks", 10));
    }

    /**
     * @return the matching region, or null (player not in a configured area, regions disabled or no source installed)
     */
    public RegionSource.Match match(ServerPlayer player, SwiSettings settings, SwiLogger logger) {
        if (sources.isEmpty() || !enabled(settings)) {
            return null;
        }
        DataNode regions = settings.raw().getNode("regions");
        Set<String> allowed = new java.util.HashSet<>();
        List<?> groups = regions != null ? regions.getList("groups") : null;
        if (groups != null) {
            for (Object g : groups) allowed.add(String.valueOf(g));
        }
        for (RegionSource source : sources) {
            DataNode cfg = regions != null ? regions.getNode(source.id()) : null;
            if (cfg == null) {
                continue;
            }
            try {
                RegionSource.Match m = source.match(player, cfg);
                if (m != null && m.group() != null && !m.group().isEmpty()) {
                    if (allowed.contains(m.group())) {
                        return m;
                    }
                    logger.debug("[Regions] " + source.id() + " area " + m.areaId() + " maps to group '" + m.group()
                            + "' which is not listed under regions.groups");
                }
            } catch (LinkageError | RuntimeException e) {
                logger.warn("[Regions] " + source.id() + " lookup failed (" + e + "); disabling it");
                sources.remove(source);
            }
        }
        return null;
    }

    /**
     * Reads {@code map.<key>} trying several keys in order (e.g. a UUID then a name). Keys are compared case-insensitively.
     */
    public static String lookup(DataNode map, String... keys) {
        if (map == null) {
            return null;
        }
        for (String key : keys) {
            if (key == null) continue;
            for (String k : map.keys()) {
                if (k.equalsIgnoreCase(key)) {
                    return map.getString(k);
                }
            }
        }
        return null;
    }
}
