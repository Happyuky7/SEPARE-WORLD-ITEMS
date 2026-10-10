//#if MC >= 1.21
package com.github.happyuky7.swi.mc.integration.regions;

import com.github.happyuky7.swi.common.config.SwiSettings;
import com.github.happyuky7.swi.common.yaml.YamlCodec;
import com.github.happyuky7.swi.mc.SwiCore;
import com.github.happyuky7.swi.mc.VanillaSelfTest;
import com.github.happyuky7.swi.mc.compat.Mc;
import com.github.happyuky7.swi.mc.region.RegionSource;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

/**
 * Self-test hook for region mods: creates a YAWP region / Flan claim through their own APIs and checks
 * which group the player's position resolves to (inside each area and outside).
 */
public final class RegionSelfTest implements VanillaSelfTest.Hook {

    private final SwiCore core;
    private final boolean yawp;
    private final boolean flan;

    public RegionSelfTest(SwiCore core, boolean yawp, boolean flan) {
        this.core = core;
        this.yawp = yawp;
        this.flan = flan;
    }

    @Override
    public String name() {
        return "regions" + (yawp ? " YAWP" : "") + (flan ? " Flan" : "");
    }

    @Override
    public void prepare(ServerPlayer player, List<String> failures) {
        SwiSettings settings = new SwiSettings(YamlCodec.parse(
                "regions:\n  enabled: true\n  groups: [yawpgroup, flangroup]\n"
                        + "  yawp:\n    \"*\":\n      swi_yawp: yawpgroup\n"
                        + "  flan:\n    any-claim: flangroup\n"));
        double x = player.getX(), y = player.getY(), z = player.getZ();
        try {
            if (yawp) {
                if (!YawpTestArea.create(player, new BlockPos(100, -64, 100), new BlockPos(110, 320, 110))) {
                    failures.add("YAWP: could not add the test region");
                } else {
                    expect(failures, player, settings, 105.5, 70, 105.5, "yawpgroup");
                }
            }
            if (flan) {
                boolean created = FlanTestArea.create(player, new BlockPos(200, 64, 200), new BlockPos(210, 64, 210));
                if (!created) {
                    failures.add("Flan: could not create the test claim");
                } else {
                    expect(failures, player, settings, 205.5, 70, 205.5, "flangroup");
                }
            }
            expect(failures, player, settings, 0.5, 70, 0.5, null);
        } finally {
            player.setPos(x, y, z);
        }
    }

    @Override
    public void verifyCleared(ServerPlayer player, List<String> failures) {
        // region resolution is fully checked in prepare()
    }

    @Override
    public void verifyRestored(ServerPlayer player, List<String> failures) {
    }

    private void expect(List<String> failures, ServerPlayer player, SwiSettings settings, double x, double y, double z, String group) {
        player.setPos(x, y, z);
        RegionSource.Match m = core.regions().match(player, settings, core.platform().logger());
        String got = m != null ? m.group() : null;
        if (group == null ? got != null : !group.equals(got)) {
            failures.add("regions: at " + (int) x + "," + (int) z + " expected group " + group + " got " + got);
        } else {
            core.platform().logger().info("[SelfTest] region at " + (int) x + "," + (int) z + " -> " + got + (m != null ? " (" + m.areaId() + ")" : ""));
        }
    }

    private static final class YawpTestArea {
        static boolean create(ServerPlayer player, BlockPos min, BlockPos max) {
            de.z0rdak.yawp.core.region.IMarkableRegion region = new de.z0rdak.yawp.api.core.region.CuboidRegionBuilder("swi_yawp")
                    .setArea(new de.z0rdak.yawp.core.area.CuboidArea(min, max))
                    .inDim(Mc.level(player).dimension())
                    .withPriority(5)
                    .build();
            de.z0rdak.yawp.api.core.RegionManager manager = de.z0rdak.yawp.api.core.RegionManager.get();
            // YAWP 0.6.3+ only knows levels it tracks; older versions have no such step (test code only: reflection).
            try {
                java.lang.reflect.Method has = manager.getClass().getMethod("hasLevelData", net.minecraft.resources.ResourceKey.class);
                if (!(Boolean) has.invoke(manager, Mc.level(player).dimension())) {
                    manager.getClass().getMethod("trackLevel", net.minecraft.resources.ResourceKey.class).invoke(manager, Mc.level(player).dimension());
                }
            } catch (ReflectiveOperationException ignored) {
                // not needed on this YAWP version
            }
            return manager.getDimRegionApi(Mc.level(player).dimension()).map(api -> api.addLocalRegion(region)).orElse(false);
        }
    }

    private static final class FlanTestArea {
        static boolean create(ServerPlayer player, BlockPos a, BlockPos b) {
            return io.github.flemmli97.flan.claim.ClaimStorage.get(Mc.level(player)).createClaim(a, b, player);
        }
    }
}
//#endif
