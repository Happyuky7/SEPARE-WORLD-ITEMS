package com.github.happyuky7.separeWorldItems.utils;

import org.bukkit.Bukkit;

public class MCVersionChecker {

    /**
     * Small semantic version (major.minor.patch) parsed from Bukkit.
     */
    public static final class SemVer implements Comparable<SemVer> {
        private final int major;
        private final int minor;
        private final int patch;

        public SemVer(int major, int minor, int patch) {
            this.major = major;
            this.minor = minor;
            this.patch = patch;
        }

        public int major() {
            return major;
        }

        public int minor() {
            return minor;
        }

        public int patch() {
            return patch;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof SemVer)) return false;
            SemVer that = (SemVer) o;
            return major == that.major
                    && minor == that.minor
                    && patch == that.patch;
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(major, minor, patch);
        }

        @Override
        public String toString() {
            return "SemVer[" + "major=" + major + ", " + "minor=" + minor + ", " + "patch=" + patch + "]";
        }

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
        // Paper provides this in modern versions (Spigot API doesn't expose it).
        String mc = null;
        try {
            java.lang.reflect.Method m = Bukkit.class.getMethod("getMinecraftVersion");
            Object v = m.invoke(null);
            if (v instanceof String) {
                mc = (String) v;
            }
        } catch (Throwable ignored) {
            // ignore
        }

        if (mc == null || mc.trim().isEmpty()) {
            try {
                mc = Bukkit.getBukkitVersion();
            } catch (Throwable ignored) {
                // ignore
            }
        }

        if (mc == null || mc.trim().isEmpty()) {
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
