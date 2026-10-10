package com.github.happyuky7.separeWorldItems.managers.integrations.regions;

import java.util.logging.Logger;

/**
 * Picks the region provider matching the installed WorldGuard (7.x or 6.x).
 */
public final class RegionProviders {

    private static volatile RegionProvider cached;
    private static volatile boolean detected;

    private RegionProviders() {
    }

    /**
     * @return the provider, or null when no supported WorldGuard is installed
     */
    public static RegionProvider get(Logger logger) {
        if (!detected) {
            synchronized (RegionProviders.class) {
                if (!detected) {
                    cached = detect(logger);
                    detected = true;
                }
            }
        }
        return cached;
    }

    /** Forget the detection (e.g. after /swi reload or a plugin being enabled later). */
    public static void reset() {
        detected = false;
        cached = null;
    }

    private static RegionProvider detect(Logger logger) {
        try {
            if (present("com.sk89q.worldguard.WorldGuard")) {
                return log(logger, new WorldGuard7Provider());
            }
            if (present("com.sk89q.worldguard.bukkit.WGBukkit")) {
                return log(logger, (RegionProvider) Class.forName(RegionProviders.class.getPackage().getName() + ".WorldGuard6Provider")
                        .getConstructor().newInstance());
            }
        } catch (Throwable t) {
            logger.warning("[WorldGuard] Unsupported WorldGuard version: " + t);
        }
        return null;
    }

    private static RegionProvider log(Logger logger, RegionProvider provider) {
        logger.info("[WorldGuard] Using " + provider.name() + " region support.");
        return provider;
    }

    private static boolean present(String className) {
        try {
            Class.forName(className, false, RegionProviders.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
