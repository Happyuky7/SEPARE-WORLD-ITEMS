package com.github.happyuky7.separeWorldItems.utils;

import com.github.happyuky7.separeWorldItems.SepareWorldItems;

/**
 * Resolves save/load toggles.
 *
 * <p>Supports global settings under {@code settings.options.saves.*}
 * and optional per-group overrides under {@code settings.options.saves-per-group.<group>.*}.</p>
 */
public final class SaveToggles {

    private SaveToggles() {
    }

    public static boolean isEnabled(SepareWorldItems plugin, String groupName, String key) {
        if (groupName != null && !groupName.isEmpty()) {
            String overridePath = "settings.options.saves-per-group." + groupName + "." + key;
            try {
                if (plugin.getConfig().contains(overridePath)) {
                    return plugin.getConfig().getBoolean(overridePath);
                }
            } catch (Throwable ignored) {
            }
        }
        return plugin.getConfig().getBoolean("settings.options.saves." + key);
    }
}
