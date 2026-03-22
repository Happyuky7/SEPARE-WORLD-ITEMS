package com.github.happyuky7.separeWorldItems.utils;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Small parser/comparator for plugin versions like:
 * - 1.0.0
 * - 2.0.0-DEV-104
 * - 2.0.0-dev-105
 */
public final class PluginVersion implements Comparable<PluginVersion> {

    private static final Pattern PATTERN = Pattern.compile(
            "^(\\d+)\\.(\\d+)\\.(\\d+)(?:[-_]?([A-Za-z]+)[-_]?(\\d+))?.*$"
    );

    public final int major;
    public final int minor;
    public final int patch;
    public final @Nullable String preTag;
    public final int preNum;

    private PluginVersion(int major, int minor, int patch, @Nullable String preTag, int preNum) {
        this.major = major;
        this.minor = minor;
        this.patch = patch;
        this.preTag = preTag;
        this.preNum = preNum;
    }

    public static @Nullable PluginVersion tryParse(@Nullable String text) {
        if (text == null) {
            return null;
        }
        String s = text.trim();
        if (s.isEmpty()) {
            return null;
        }

        Matcher m = PATTERN.matcher(s);
        if (!m.matches()) {
            return null;
        }

        try {
            int major = Integer.parseInt(m.group(1));
            int minor = Integer.parseInt(m.group(2));
            int patch = Integer.parseInt(m.group(3));
            String tag = m.group(4);
            String numStr = m.group(5);

            if (tag == null || tag.isBlank()) {
                return new PluginVersion(major, minor, patch, null, 0);
            }

            int num = 0;
            if (numStr != null && !numStr.isBlank()) {
                try {
                    num = Integer.parseInt(numStr);
                } catch (NumberFormatException ignored) {
                    num = 0;
                }
            }

            return new PluginVersion(major, minor, patch, tag.trim().toUpperCase(Locale.ROOT), num);
        } catch (Exception ignored) {
            return null;
        }
    }

    public static @NotNull PluginVersion parseOrZero(@Nullable String text) {
        PluginVersion v = tryParse(text);
        return v != null ? v : new PluginVersion(0, 0, 0, null, 0);
    }

    @Override
    public int compareTo(@NotNull PluginVersion other) {
        if (major != other.major) return Integer.compare(major, other.major);
        if (minor != other.minor) return Integer.compare(minor, other.minor);
        if (patch != other.patch) return Integer.compare(patch, other.patch);

        // Stable (no preTag) is greater than pre-release (DEV)
        boolean thisStable = (preTag == null);
        boolean otherStable = (other.preTag == null);
        if (thisStable != otherStable) {
            return thisStable ? 1 : -1;
        }
        if (thisStable) {
            return 0;
        }

        // Same tag preferred
        int tagCmp = String.valueOf(preTag).compareTo(String.valueOf(other.preTag));
        if (tagCmp != 0) {
            return tagCmp;
        }
        return Integer.compare(preNum, other.preNum);
    }

    @Override
    public String toString() {
        if (preTag == null) {
            return major + "." + minor + "." + patch;
        }
        return major + "." + minor + "." + patch + "-" + preTag + "-" + preNum;
    }
}
