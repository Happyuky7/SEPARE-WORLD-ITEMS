package com.github.happyuky7.separeWorldItems.api;

import com.github.happyuky7.separeWorldItems.managers.integrations.WorldGuardManager;
import org.bukkit.entity.Player;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Public API (BETA) for other plugins and for internal future features.
 */
public final class SepareWorldItemsAPI {

    private SepareWorldItemsAPI() {
    }

    /**
     * Resolves the effective group for this player.
     * If WorldGuard is enabled and the player is inside a configured region, returns the region group.
     * Otherwise returns the world group.
     */
    public static String getEffectiveGroup(Player player) {
        if (player == null) {
            return "default";
        }
        return WorldGuardManager.getGroupName(player);
    }

    /**
     * Returns the WorldGuard region id with highest priority at the player location, or null.
     * This ignores the implicit __global__ region.
     */
    public static String getCurrentWorldGuardRegion(Player player) {
        if (player == null) {
            return null;
        }
        return WorldGuardManager.getRegion(player);
    }

    /**
     * Fluent API entry point (BETA).
     *
     * <p>Example:
     * {@code SepareWorldItemsAPI.api().getGroup("default").getPlayer("Notch").getData()}
     */
    public static @NotNull Fluent api() {
        return new Fluent();
    }

    /**
     * Reads the saved player data from {@code <dataFolder>/groups/<groupName>/*-<uuid>.yml}.
     * Returns null if there is no file.
     */
    public static PlayerDataFilesAPI.PlayerDataSnapshot readWorldGroupSnapshot(UUID playerUuid, String groupName) {
        return PlayerDataFilesAPI.readSnapshot(PlayerDataFilesAPI.StorageType.WORLD_GROUP, groupName, playerUuid);
    }

    /**
     * Reads the saved player data from {@code <dataFolder>/worldguard/groups/<groupName>/*-<uuid>.yml}.
     * Returns null if there is no file.
     */
    public static PlayerDataFilesAPI.PlayerDataSnapshot readWorldGuardGroupSnapshot(UUID playerUuid, String groupName) {
        return PlayerDataFilesAPI.readSnapshot(PlayerDataFilesAPI.StorageType.WORLDGUARD_GROUP, groupName, playerUuid);
    }

    /**
     * Fluent query root.
     */
    public static final class Fluent {

        /**
         * World group storage (data folder: groups/<group>/...).
         */
        public @NotNull GroupRef getGroup(@NotNull String groupName) {
            return new GroupRef(PlayerDataFilesAPI.StorageType.WORLD_GROUP, groupName);
        }

        /**
         * WorldGuard group storage (data folder: worldguard/groups/<group>/...).
         */
        public @NotNull GroupRef getWorldGuardGroup(@NotNull String groupName) {
            return new GroupRef(PlayerDataFilesAPI.StorageType.WORLDGUARD_GROUP, groupName);
        }

        /**
         * Resolves the effective storage + group for an online player.
         * Example: {@code SepareWorldItemsAPI.api().getEffective(player).getData()}.
         */
        public @NotNull PlayerRef getEffective(@NotNull Player player) {
            String regionId = WorldGuardManager.getRegion(player);
            String regionGroup = regionId != null
                ? WorldGuardManager.getGroupNameByRegion(player.getWorld().getName(), regionId)
                : null;

            if (regionGroup != null && !regionGroup.isEmpty()) {
                return new PlayerRef(PlayerDataFilesAPI.StorageType.WORLDGUARD_GROUP, regionGroup, player.getUniqueId(), player.getName());
            }

            String worldGroup = WorldGuardManager.getWorldGroup(player);
            return new PlayerRef(PlayerDataFilesAPI.StorageType.WORLD_GROUP, worldGroup, player.getUniqueId(), player.getName());
        }
    }

    /**
     * Represents one group folder.
     */
    public static final class GroupRef {
        private final PlayerDataFilesAPI.StorageType storageType;
        private final String groupName;

        private GroupRef(PlayerDataFilesAPI.StorageType storageType, String groupName) {
            this.storageType = storageType;
            this.groupName = groupName;
        }

        public @NotNull PlayerRef getPlayer(@NotNull UUID uuid) {
            return new PlayerRef(storageType, groupName, uuid, null);
        }

        public @NotNull PlayerRef getPlayer(@NotNull String playerName) {
            return new PlayerRef(storageType, groupName, null, playerName);
        }
    }

    /**
     * Represents one player inside one group folder.
     */
    public static final class PlayerRef {
        private final PlayerDataFilesAPI.StorageType storageType;
        private final String groupName;
        private final UUID uuid;
        private final String playerName;

        private PlayerRef(PlayerDataFilesAPI.StorageType storageType, String groupName, UUID uuid, String playerName) {
            this.storageType = storageType;
            this.groupName = groupName;
            this.uuid = uuid;
            this.playerName = playerName;
        }

        /**
         * Returns the full snapshot (inventory, armor, etc.) or null if file doesn't exist.
         */
        public PlayerDataFilesAPI.PlayerDataSnapshot getData() {
            if (uuid != null) {
                return PlayerDataFilesAPI.readSnapshot(storageType, groupName, uuid);
            }
            if (playerName != null) {
                return PlayerDataFilesAPI.readSnapshot(storageType, groupName, playerName);
            }
            return null;
        }

        /**
         * Returns the raw YAML config for this player in this group, or null.
         * Useful for any soft-dependency integrations that store extra keys.
         */
        public @Nullable FileConfiguration getRawConfig() {
            if (uuid != null) {
                return PlayerDataFilesAPI.readRawConfig(storageType, groupName, uuid);
            }
            if (playerName != null) {
                return PlayerDataFilesAPI.readRawConfig(storageType, groupName, playerName);
            }
            return null;
        }

        public ItemStack[] getInventory() {
            PlayerDataFilesAPI.PlayerDataSnapshot snapshot = getData();
            return snapshot != null ? snapshot.inventory() : null;
        }

        public ItemStack[] getEnderChest() {
            PlayerDataFilesAPI.PlayerDataSnapshot snapshot = getData();
            return snapshot != null ? snapshot.enderChest() : null;
        }

        public ItemStack getOffHand() {
            PlayerDataFilesAPI.PlayerDataSnapshot snapshot = getData();
            return snapshot != null ? snapshot.offHand() : null;
        }

        public ItemStack[] getArmor() {
            PlayerDataFilesAPI.PlayerDataSnapshot snapshot = getData();
            if (snapshot == null) {
                return null;
            }
            return new ItemStack[]{snapshot.helmet(), snapshot.chestplate(), snapshot.leggings(), snapshot.boots()};
        }

        // ---- Supported scalar fields ----

        public @Nullable String getGamemode() {
            PlayerDataFilesAPI.PlayerDataSnapshot snapshot = getData();
            return snapshot != null ? snapshot.gamemode() : null;
        }

        public @Nullable Boolean getAllowFlight() {
            PlayerDataFilesAPI.PlayerDataSnapshot snapshot = getData();
            return snapshot != null ? snapshot.allowFlight() : null;
        }

        public @Nullable Boolean getFlying() {
            PlayerDataFilesAPI.PlayerDataSnapshot snapshot = getData();
            return snapshot != null ? snapshot.flying() : null;
        }

        public @Nullable Float getFlySpeed() {
            PlayerDataFilesAPI.PlayerDataSnapshot snapshot = getData();
            return snapshot != null ? snapshot.flySpeed() : null;
        }

        public @Nullable Float getExp() {
            PlayerDataFilesAPI.PlayerDataSnapshot snapshot = getData();
            return snapshot != null ? snapshot.exp() : null;
        }

        public @Nullable Integer getExpLevel() {
            PlayerDataFilesAPI.PlayerDataSnapshot snapshot = getData();
            return snapshot != null ? snapshot.expLevel() : null;
        }

        public @Nullable Integer getHunger() {
            PlayerDataFilesAPI.PlayerDataSnapshot snapshot = getData();
            return snapshot != null ? snapshot.hunger() : null;
        }

        public @Nullable Double getHealth() {
            PlayerDataFilesAPI.PlayerDataSnapshot snapshot = getData();
            return snapshot != null ? snapshot.health() : null;
        }

        public @Nullable Double getMaxHealth() {
            PlayerDataFilesAPI.PlayerDataSnapshot snapshot = getData();
            return snapshot != null ? snapshot.maxHealth() : null;
        }

        // ---- Integrations (soft-dependencies) ----

        public @Nullable Double getAuraSkillsHealth() {
            PlayerDataFilesAPI.PlayerDataSnapshot snapshot = getData();
            return snapshot != null ? snapshot.auraSkillsHealth() : null;
        }

        public @Nullable Double getAuraSkillsMana() {
            PlayerDataFilesAPI.PlayerDataSnapshot snapshot = getData();
            return snapshot != null ? snapshot.auraSkillsMana() : null;
        }

        public @Nullable java.util.List<PlayerDataFilesAPI.PotionEffectSnapshot> getPotionEffects() {
            PlayerDataFilesAPI.PlayerDataSnapshot snapshot = getData();
            return snapshot != null ? snapshot.potionEffects() : null;
        }
    }
}
