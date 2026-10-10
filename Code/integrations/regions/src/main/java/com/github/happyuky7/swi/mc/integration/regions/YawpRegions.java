//#if MC >= 1.21
package com.github.happyuky7.swi.mc.integration.regions;

import com.github.happyuky7.swi.api.data.DataNode;
import com.github.happyuky7.swi.mc.compat.Mc;
import com.github.happyuky7.swi.mc.region.ModRegions;
import com.github.happyuky7.swi.mc.region.RegionSource;
import de.z0rdak.yawp.api.core.RegionManager;
import de.z0rdak.yawp.core.region.IMarkableRegion;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

/**
 * YAWP (Yet Another World Protector): named regions with priorities, like WorldGuard.
 *
 * <pre>
 * regions:
 *   yawp:
 *     "minecraft:overworld":   # per dimension...
 *       spawn: spawngroup
 *     "*":                     # ...or any dimension
 *       arena: pvp
 * </pre>
 */
public final class YawpRegions implements RegionSource {

    @Override
    public String id() {
        return "yawp";
    }

    @Override
    public Match match(ServerPlayer player, DataNode config) {
        // The per-dimension API type was renamed between YAWP versions (IDimensionRegionApi -> ILevelRegionApi),
        // so it is never named here; getRegionsAt exists in both.
        Optional<java.util.List<IMarkableRegion>> regions = RegionManager.get().getDimRegionApi(Mc.level(player).dimension())
                .map(api -> api.getRegionsAt(player.blockPosition()));
        if (!regions.isPresent()) {
            return null;
        }
        DataNode perDim = config.getNode(Mc.dimensionId(Mc.level(player)));
        DataNode anyDim = config.getNode("*");
        Match best = null;
        for (IMarkableRegion region : regions.get()) {
            if (!region.isActive()) {
                continue;
            }
            String group = ModRegions.lookup(perDim, region.getName());
            if (group == null) {
                group = ModRegions.lookup(anyDim, region.getName());
            }
            if (group != null && (best == null || region.getPriority() > best.priority())) {
                best = new Match(group, "yawp:" + region.getName(), region.getPriority());
            }
        }
        return best;
    }
}
//#endif
