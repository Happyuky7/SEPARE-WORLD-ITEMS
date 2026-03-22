package com.github.happyuky7.separeWorldItems.utils;

import org.bukkit.Bukkit;

public class MCVersionChecker {

    /**
     * Small semantic version (major.minor.patch) parsed from Bukkit.
     */
    public record SemVer(int major, int minor, int patch) implements Comparable<SemVer> {
        @Override
        public int compareTo(SemVer other) {
            if (major != other.major) return Integer.compare(major, other.major);
            if (minor != other.minor) return Integer.compare(minor, other.minor);
            return Integer.compare(patch, other.patch);
        }
    }

    /**
     * Best-effort server Minecraft version.
     * Prefers Bukkit.getMinecraftVersion() (e.g. "1.21.1") and falls back to parsing Bukkit.getBukkitVersion()/getVersion().
     */
    public static SemVer getServerMinecraftVersion() {
        // Paper/Spigot provides this in modern versions.
        String mc = null;
        try {
            mc = Bukkit.getMinecraftVersion();
        } catch (Throwable ignored) {
            // ignore
        }

        if (mc == null || mc.isBlank()) {
            try {
                mc = Bukkit.getBukkitVersion();
            } catch (Throwable ignored) {
                // ignore
            }
        }

        if (mc == null || mc.isBlank()) {
            mc = Bukkit.getVersion();
        }

        return parseSemVer(mc);
    }

    public static boolean isAtLeast(int major, int minor, int patch) {
        return getServerMinecraftVersion().compareTo(new SemVer(major, minor, patch)) >= 0;
    }

    public static boolean isAtLeast(int major, int minor) {
        return isAtLeast(major, minor, 0);
    }

    public static boolean isOffHandSupported() {
        // Offhand exists since 1.9
        return isAtLeast(1, 9);
    }

    public static boolean isVersion21OrAbove() {
        return isAtLeast(1, 21);
    }

    private static SemVer parseSemVer(String input) {
        if (input == null) {
            return new SemVer(0, 0, 0);
        }

        // Extract first occurrence of something like 1.21 or 1.21.1
        String normalized = input;
        int start = -1;
        for (int i = 0; i < normalized.length(); i++) {
            if (Character.isDigit(normalized.charAt(i))) {
                start = i;
                break;
            }
        }
        if (start == -1) {
            return new SemVer(0, 0, 0);
        }

        String tail = normalized.substring(start);
        StringBuilder ver = new StringBuilder();
        for (int i = 0; i < tail.length(); i++) {
            char c = tail.charAt(i);
            if (Character.isDigit(c) || c == '.') {
                ver.append(c);
            } else {
                break;
            }
        }

        String extracted = ver.toString();
        String[] parts = extracted.split("\\.");
        int major = parts.length > 0 ? safeParseInt(parts[0]) : 0;
        int minor = parts.length > 1 ? safeParseInt(parts[1]) : 0;
        int patch = parts.length > 2 ? safeParseInt(parts[2]) : 0;

        // Newer MC version scheme may omit the leading "1." (e.g. "26.1").
        // Normalize 26.1 -> 1.26.1 so legacy comparisons keep working.
        boolean startsWithLegacyPrefix = extracted.startsWith("1.");
        if (!startsWithLegacyPrefix && parts.length == 2 && major >= 20) {
            return new SemVer(1, major, minor);
        }

        return new SemVer(major, minor, patch);
    }

    private static int safeParseInt(String s) {
        try {
            return Integer.parseInt(s);
        } catch (Exception ignored) {
            return 0;
        }
    }

}
