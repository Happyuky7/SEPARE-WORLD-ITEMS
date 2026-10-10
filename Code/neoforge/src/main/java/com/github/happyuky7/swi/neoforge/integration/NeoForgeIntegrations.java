package com.github.happyuky7.swi.neoforge.integration;

import com.github.happyuky7.swi.common.platform.SwiLogger;
import com.github.happyuky7.swi.mc.Integrations;
import com.github.happyuky7.swi.mc.SwiCore;
import com.github.happyuky7.swi.mc.VanillaSelfTest;
import net.neoforged.fml.ModList;

/**
 * Optional integrations with other NeoForge mods, activated only when they are installed.
 */
public final class NeoForgeIntegrations {

    private NeoForgeIntegrations() {
    }

    public static void register(SwiCore core) {
        SwiLogger log = core.platform().logger();
        ModList mods = ModList.get();
        java.util.function.Predicate<String> LOADED = mods::isLoaded;
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
        boolean accessories = mods.isLoaded("accessories");
        //#if MC >= 1.21 && MC < 26.1
        if (accessories) {
            Integrations.registerSection(log, "Accessories", com.github.happyuky7.swi.mc.integration.AccessoriesSection::new);
            if (VanillaSelfTest.enabled()) {
                VanillaSelfTest.addHook(com.github.happyuky7.swi.mc.integration.AccessoriesSection.selfTestHook());
            }
        }
        //#endif
        if (mods.isLoaded("curios")) {
            if (accessories) {
                // Accessories can emulate the Curios API on top of its own storage: saving both would duplicate items.
                log.info("[Integrations] Curios API provided alongside Accessories: slots are handled by the Accessories integration");
            } else {
                Integrations.registerSection(log, "Curios", CuriosSection::new);
                if (VanillaSelfTest.enabled()) {
                    VanillaSelfTest.addHook(new CuriosSelfTest());
                }
            }
        }
    }
}
