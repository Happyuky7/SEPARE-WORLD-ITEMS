package com.github.happyuky7.swi.fabric.integration;

import com.github.happyuky7.swi.common.platform.SwiLogger;
import com.github.happyuky7.swi.mc.Integrations;
import com.github.happyuky7.swi.mc.SwiCore;
import com.github.happyuky7.swi.mc.section.SlotContainerCodec;
import net.fabricmc.loader.api.FabricLoader;

/**
 * Optional integrations with other Fabric mods, activated only when they are installed.
 */
public final class FabricIntegrations {

    private FabricIntegrations() {
    }

    public static void register(SwiCore core) {
        SwiLogger log = core.platform().logger();
        FabricLoader loader = FabricLoader.getInstance();
        if (loader.isModLoaded("fabric-permissions-api-v0")) {
            FabricPermissions.install(core);
        }
        java.util.function.Predicate<String> LOADED = loader::isModLoaded;
        //#if MC >= 1.21
        // Region/claim mods: areas can use their own group (config: regions.*).
        if (LOADED.test("yawp")) {
            Integrations.run(log, "YAWP", () -> core.regions().register(new com.github.happyuky7.swi.mc.integration.regions.YawpRegions(), log));
        }
        if (LOADED.test("flan")) {
            Integrations.run(log, "Flan", () -> core.regions().register(new com.github.happyuky7.swi.mc.integration.regions.FlanRegions(), log));
        }
        if (LOADED.test("openpartiesandclaims")) {
            Integrations.run(log, "Open Parties and Claims", () -> core.regions().register(new com.github.happyuky7.swi.mc.integration.regions.OpacRegions(), log));
        }
        if (com.github.happyuky7.swi.mc.VanillaSelfTest.enabled() && (LOADED.test("yawp") || LOADED.test("flan"))) {
            com.github.happyuky7.swi.mc.VanillaSelfTest.addHook(new com.github.happyuky7.swi.mc.integration.regions.RegionSelfTest(core, LOADED.test("yawp"), LOADED.test("flan")));
        }
        //#endif
        boolean accessories = loader.isModLoaded("accessories");
        //#if MC >= 1.21 && MC < 26.1
        if (accessories) {
            Integrations.registerSection(log, "Accessories", com.github.happyuky7.swi.mc.integration.AccessoriesSection::new);
            if (com.github.happyuky7.swi.mc.VanillaSelfTest.enabled()) {
                com.github.happyuky7.swi.mc.VanillaSelfTest.addHook(com.github.happyuky7.swi.mc.integration.AccessoriesSection.selfTestHook());
            }
        }
        //#endif
        //#if MC >= 1.18 && MC < 26.1
        if (loader.isModLoaded("trinkets")) {
            if (accessories) {
                // Accessories can emulate the Trinkets API on top of its own storage: saving both would duplicate items.
                log.info("[Integrations] Trinkets API provided alongside Accessories: slots are handled by the Accessories integration");
            } else {
                Integrations.registerSection(log, "Trinkets", TrinketsSection::new);
                if (com.github.happyuky7.swi.mc.VanillaSelfTest.enabled()) {
                    com.github.happyuky7.swi.mc.VanillaSelfTest.addHook(new com.github.happyuky7.swi.mc.SlotSelfTest("Trinkets", p ->
                            dev.emi.trinkets.api.TrinketsApi.getTrinketComponent(p)
                                    .flatMap(c -> c.getInventory().values().stream().flatMap(g -> g.values().stream())
                                            .filter(inv -> inv.getContainerSize() > 0).findFirst())
                                    .map(SlotContainerCodec::of).orElse(null)));
                }
            }
        }
        //#endif
    }
}
