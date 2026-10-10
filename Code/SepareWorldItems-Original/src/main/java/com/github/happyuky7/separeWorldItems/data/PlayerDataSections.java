package com.github.happyuky7.separeWorldItems.data;

import com.github.happyuky7.separeWorldItems.SepareWorldItems;
import com.github.happyuky7.separeWorldItems.data.integrations.AuraSkillsManaData;
import com.github.happyuky7.separeWorldItems.storage.PlayerDataScope;
import com.github.happyuky7.separeWorldItems.utils.MCVersionChecker;
import com.github.happyuky7.separeWorldItems.utils.SaveToggles;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

/**
 * Centralized save/load logic for player data sections.
 *
 * <p>This keeps the toggle logic in one place, and allows future per-group overrides
 * without duplicating checks in multiple managers.</p>
 */
public final class PlayerDataSections {

    private PlayerDataSections() {
    }

    public static void save(SepareWorldItems plugin, Player player, PlayerDataScope scope, String groupName, YamlConfiguration config) {
        if (SaveToggles.isEnabled(plugin, groupName, "gamemode")) {
            GamemodeData.save(player, config);
        }
        if (SaveToggles.isEnabled(plugin, groupName, "flying")) {
            FlyingData.save(player, config);
        }
        if (SaveToggles.isEnabled(plugin, groupName, "fly-speed")) {
            FlySpeedData.save(player, config);
        }
        if (SaveToggles.isEnabled(plugin, groupName, "exp")) {
            ExpData.save(player, config);
        }
        if (SaveToggles.isEnabled(plugin, groupName, "enderchest")) {
            EnderChestData.save(player, config);
        }
        if (SaveToggles.isEnabled(plugin, groupName, "inventory")) {
            InventoryData.save(player, config);
            ArmorData.save(player, config);
        }
        if (SaveToggles.isEnabled(plugin, groupName, "potion-effects")) {
            PotionEffectsData.save(player, config);
        }
        if (SaveToggles.isEnabled(plugin, groupName, "food-level")) {
            FoodLevelData.save(player, config);
        }
        if (MCVersionChecker.isOffHandSupported() && SaveToggles.isEnabled(plugin, groupName, "off-hand")) {
            OffHandItemData.save(player, config);
        }
        if (SaveToggles.isEnabled(plugin, groupName, "health")) {
            HealthData.save(player, config, plugin.getConfig().getString("settings.health-options.type"));
        }

        if (plugin.getConfig().getBoolean("integrations.auraskills.enabled")) {
            if (plugin.getConfig().getBoolean("auraskills.save-mana")) {
                AuraSkillsManaData.save(player, config);
            }
        }
    }

    public static void load(SepareWorldItems plugin, Player player, PlayerDataScope scope, String groupName, YamlConfiguration config) {
        if (SaveToggles.isEnabled(plugin, groupName, "gamemode")) {
            GamemodeData.load(player, config);
        }
        if (SaveToggles.isEnabled(plugin, groupName, "flying")) {
            FlyingData.load(player, config);
        }
        if (SaveToggles.isEnabled(plugin, groupName, "fly-speed")) {
            FlySpeedData.load(player, config);
        }
        if (SaveToggles.isEnabled(plugin, groupName, "exp")) {
            ExpData.load(player, config);
        }
        if (SaveToggles.isEnabled(plugin, groupName, "enderchest")) {
            EnderChestData.load(player, config);
        }
        if (SaveToggles.isEnabled(plugin, groupName, "inventory")) {
            InventoryData.load(player, config);
            ArmorData.load(player, config);
        }
        if (SaveToggles.isEnabled(plugin, groupName, "potion-effects")) {
            PotionEffectsData.load(player, config);
        }
        if (SaveToggles.isEnabled(plugin, groupName, "food-level")) {
            FoodLevelData.load(player, config);
        }
        if (MCVersionChecker.isOffHandSupported() && SaveToggles.isEnabled(plugin, groupName, "off-hand")) {
            OffHandItemData.load(player, config);
        }
        if (SaveToggles.isEnabled(plugin, groupName, "health")) {
            HealthData.load(player, config, plugin.getConfig().getString("settings.health-options.type"));
        }

        if (plugin.getConfig().getBoolean("integrations.auraskills.enabled")) {
            if (plugin.getConfig().getBoolean("auraskills.save-mana")) {
                AuraSkillsManaData.load(player, config);
            }
        }
    }

    public static void reload(SepareWorldItems plugin, Player player, PlayerDataScope scope, String groupName, YamlConfiguration config) {
        // Same as load, but uses ExpData.reload for EXP.
        if (SaveToggles.isEnabled(plugin, groupName, "gamemode")) {
            GamemodeData.load(player, config);
        }
        if (SaveToggles.isEnabled(plugin, groupName, "flying")) {
            FlyingData.load(player, config);
        }
        if (SaveToggles.isEnabled(plugin, groupName, "fly-speed")) {
            FlySpeedData.load(player, config);
        }
        if (SaveToggles.isEnabled(plugin, groupName, "exp")) {
            ExpData.reload(player, config);
        }
        if (SaveToggles.isEnabled(plugin, groupName, "enderchest")) {
            EnderChestData.load(player, config);
        }
        if (SaveToggles.isEnabled(plugin, groupName, "inventory")) {
            InventoryData.load(player, config);
            ArmorData.load(player, config);
        }
        if (SaveToggles.isEnabled(plugin, groupName, "potion-effects")) {
            PotionEffectsData.load(player, config);
        }
        if (SaveToggles.isEnabled(plugin, groupName, "food-level")) {
            FoodLevelData.load(player, config);
        }
        if (MCVersionChecker.isOffHandSupported() && SaveToggles.isEnabled(plugin, groupName, "off-hand")) {
            OffHandItemData.load(player, config);
        }
        if (SaveToggles.isEnabled(plugin, groupName, "health")) {
            HealthData.load(player, config, plugin.getConfig().getString("settings.health-options.type"));
        }

        if (plugin.getConfig().getBoolean("integrations.auraskills.enabled")) {
            if (plugin.getConfig().getBoolean("auraskills.save-mana")) {
                AuraSkillsManaData.load(player, config);
            }
        }
    }

    /**
     * Captures a full snapshot of supported data regardless of save toggles.
     * Used for change-logs when capture-all-data is enabled.
     */
    public static YamlConfiguration captureAll(SepareWorldItems plugin, Player player) {
        YamlConfiguration cfg = new YamlConfiguration();
        try {
            GamemodeData.save(player, cfg);
        } catch (Throwable ignored) {
        }
        try {
            FlyingData.save(player, cfg);
        } catch (Throwable ignored) {
        }
        try {
            FlySpeedData.save(player, cfg);
        } catch (Throwable ignored) {
        }
        try {
            ExpData.save(player, cfg);
        } catch (Throwable ignored) {
        }
        try {
            EnderChestData.save(player, cfg);
        } catch (Throwable ignored) {
        }
        try {
            InventoryData.save(player, cfg);
            ArmorData.save(player, cfg);
        } catch (Throwable ignored) {
        }
        try {
            PotionEffectsData.save(player, cfg);
        } catch (Throwable ignored) {
        }
        try {
            FoodLevelData.save(player, cfg);
        } catch (Throwable ignored) {
        }
        try {
            OffHandItemData.save(player, cfg);
        } catch (Throwable ignored) {
        }
        try {
            HealthData.save(player, cfg, plugin.getConfig().getString("settings.health-options.type"));
        } catch (Throwable ignored) {
        }
        try {
            if (plugin.getConfig().getBoolean("integrations.auraskills.enabled") && plugin.getConfig().getBoolean("auraskills.save-mana")) {
                AuraSkillsManaData.save(player, cfg);
            }
        } catch (Throwable ignored) {
        }
        return cfg;
    }
}
