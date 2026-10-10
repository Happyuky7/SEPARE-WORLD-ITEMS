package com.github.happyuky7.swi.mc;

import com.github.happyuky7.swi.api.SepareWorldItems;
import com.github.happyuky7.swi.api.section.DataSection;
import com.github.happyuky7.swi.common.platform.SwiLogger;

import java.util.function.Supplier;

/**
 * Registers optional integrations with other mods. Compiled against the newest API available, so a mod
 * update that changes its API only disables that integration (with a warning) instead of crashing the server.
 */
public final class Integrations {

    private Integrations() {
    }

    public static void registerSection(SwiLogger logger, String modName, Supplier<DataSection<?>> section) {
        try {
            DataSection<?> s = section.get();
            SepareWorldItems.get().sections().register(s);
            logger.info("[Integrations] " + modName + " detected: its slots are saved per group (toggle: settings.options.saves." + s.id() + ")");
        } catch (LinkageError | RuntimeException e) {
            logger.warn("[Integrations] " + modName + " is installed but this version is not supported (" + e + "). Its items are NOT separated per group.");
        }
    }

    public static void run(SwiLogger logger, String modName, Runnable action) {
        try {
            action.run();
            logger.info("[Integrations] " + modName + " integration enabled");
        } catch (LinkageError | RuntimeException e) {
            logger.warn("[Integrations] " + modName + " is installed but this version is not supported (" + e + ")");
        }
    }
}
