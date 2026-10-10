package com.github.happyuky7.separeWorldItems.api;

import com.github.happyuky7.separeWorldItems.SepareWorldItems;
import com.github.happyuky7.separeWorldItems.storage.PlayerDataScope;
import com.github.happyuky7.separeWorldItems.storage.PlayerDataStore;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Locale;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * BETA API for reading player data files from disk.
 *
 * <p>Data is stored by this plugin under:
 * <ul>
 *     <li>{@code <dataFolder>/groups/<groupName>/<playerName>-<uuid>.yml}</li>
 *     <li>{@code <dataFolder>/worldguard/groups/<groupName>/<playerName>-<uuid>.yml}</li>
 * </ul>
 *
 * <p><b>Warning:</b> This reads from disk; avoid calling frequently on the main thread.
 */
public final class PlayerDataFilesAPI {

    public enum StorageType {
        WORLD_GROUP,
        WORLDGUARD_GROUP
    }

    private PlayerDataFilesAPI() {
    }

    private static final long NAME_INDEX_TTL_MILLIS = 30_000L;
    private static final ConcurrentMap<String, NameIndex> NAME_INDEX_BY_DIR = new ConcurrentHashMap<>();

    private static final class NameIndex {
        private final long builtAt;
        private final ConcurrentMap<String, UUID> uuidByLowerName;

        private NameIndex(long builtAt, ConcurrentMap<String, UUID> uuidByLowerName) {
            this.builtAt = builtAt;
            this.uuidByLowerName = uuidByLowerName;
        }
    }

    /**
     * Reads a snapshot of the saved data for a player UUID inside a given group.
     * Returns {@code null} when the file doesn't exist.
     */
    public static @Nullable PlayerDataSnapshot readSnapshot(
            @NotNull StorageType storageType,
            @NotNull String groupName,
            @NotNull UUID playerUuid
    ) {
        Objects.requireNonNull(storageType, "storageType");
        Objects.requireNonNull(groupName, "groupName");
        Objects.requireNonNull(playerUuid, "playerUuid");

        PlayerDataStore store = SepareWorldItems.getInstance().getPlayerDataStore();
        PlayerDataScope scope = toScope(storageType);
        if (!store.exists(scope, groupName, playerUuid)) {
            return null;
        }

        File file = null;
        if (store.isFileBased()) {
            file = findPlayerFile(storageType, groupName, playerUuid);
        }

        FileConfiguration config = store.load(scope, groupName, playerUuid);
        return fromConfig(storageType, groupName, playerUuid, file, config);
    }

    /**
     * Resolves the UUID by scanning the group folder for files like {@code <playerName>-<uuid>.yml}.
     * Returns null when no file is found.
     */
    public static @Nullable UUID resolveUuidByName(
            @NotNull StorageType storageType,
            @NotNull String groupName,
            @NotNull String playerName
    ) {
        Objects.requireNonNull(storageType, "storageType");
        Objects.requireNonNull(groupName, "groupName");
        Objects.requireNonNull(playerName, "playerName");

        PlayerDataStore store = SepareWorldItems.getInstance().getPlayerDataStore();
        PlayerDataScope scope = toScope(storageType);

        if (!store.isFileBased()) {
            return store.resolveUuidByName(scope, groupName, playerName);
        }

        File groupDir = getGroupDir(storageType, groupName);
        if (!groupDir.exists() || !groupDir.isDirectory()) {
            return null;
        }

        String lower = playerName.toLowerCase(Locale.ROOT);
        String indexKey = storageType + ":" + groupDir.getAbsolutePath();
        long now = System.currentTimeMillis();
        NameIndex cached = NAME_INDEX_BY_DIR.get(indexKey);
        if (cached == null || (now - cached.builtAt) > NAME_INDEX_TTL_MILLIS) {
            cached = rebuildNameIndex(groupDir);
            NAME_INDEX_BY_DIR.put(indexKey, cached);
        }

        return cached.uuidByLowerName.get(lower);
    }

    private static NameIndex rebuildNameIndex(File groupDir) {
        long now = System.currentTimeMillis();
        ConcurrentMap<String, UUID> map = new ConcurrentHashMap<>();

        File[] files = groupDir.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null || files.length == 0) {
            return new NameIndex(now, map);
        }

        for (File f : files) {
            String name = f.getName();
            if (!name.endsWith(".yml")) {
                continue;
            }
            String base = name.substring(0, name.length() - 4);
            int lastDash = base.lastIndexOf('-');
            if (lastDash <= 0 || lastDash >= base.length() - 1) {
                continue;
            }

            String playerName = base.substring(0, lastDash);
            String uuidStr = base.substring(lastDash + 1);
            try {
                UUID uuid = UUID.fromString(uuidStr);
                map.put(playerName.toLowerCase(Locale.ROOT), uuid);
            } catch (IllegalArgumentException ignored) {
                // ignore
            }
        }

        return new NameIndex(now, map);
    }

    /**
     * Reads a snapshot using a player name by scanning for {@code <playerName>-<uuid>.yml}.
     * Returns null if there is no file.
     */
    public static @Nullable PlayerDataSnapshot readSnapshot(
            @NotNull StorageType storageType,
            @NotNull String groupName,
            @NotNull String playerName
    ) {
        UUID uuid = resolveUuidByName(storageType, groupName, playerName);
        if (uuid == null) {
            return null;
        }
        return readSnapshot(storageType, groupName, uuid);
    }

    /**
     * Returns the raw YAML config for a player's data file, or {@code null} if missing.
     */
    public static @Nullable FileConfiguration readRawConfig(
            @NotNull StorageType storageType,
            @NotNull String groupName,
            @NotNull UUID playerUuid
    ) {
        PlayerDataStore store = SepareWorldItems.getInstance().getPlayerDataStore();
        PlayerDataScope scope = toScope(storageType);
        if (!store.exists(scope, groupName, playerUuid)) {
            return null;
        }
        return store.load(scope, groupName, playerUuid);
    }

    /**
     * Returns the raw YAML config by playerName, or {@code null} if missing.
     */
    public static @Nullable FileConfiguration readRawConfig(
            @NotNull StorageType storageType,
            @NotNull String groupName,
            @NotNull String playerName
    ) {
        UUID uuid = resolveUuidByName(storageType, groupName, playerName);
        if (uuid == null) {
            return null;
        }
        return readRawConfig(storageType, groupName, uuid);
    }

    private static @Nullable File findPlayerFile(StorageType storageType, String groupName, UUID uuid) {
        File groupDir = getGroupDir(storageType, groupName);
        if (!groupDir.exists() || !groupDir.isDirectory()) {
            return null;
        }

        String suffix = "-" + uuid + ".yml";
        File[] files = groupDir.listFiles((dir, name) -> name.endsWith(suffix));
        if (files == null || files.length == 0) {
            return null;
        }

        // If multiple names match (name changes), prefer the newest.
        File newest = files[0];
        for (File f : files) {
            if (f.lastModified() > newest.lastModified()) {
                newest = f;
            }
        }
        return newest;
    }

    private static PlayerDataScope toScope(StorageType storageType) {
        return switch (storageType) {
            case WORLD_GROUP -> PlayerDataScope.WORLD_GROUP;
            case WORLDGUARD_GROUP -> PlayerDataScope.WORLDGUARD_GROUP;
        };
    }

    private static File getGroupDir(StorageType storageType, String groupName) {
        File dataFolder = SepareWorldItems.getInstance().getDataFolder();
        return switch (storageType) {
            case WORLD_GROUP -> new File(dataFolder, "groups" + File.separator + groupName);
            case WORLDGUARD_GROUP -> new File(dataFolder, "worldguard" + File.separator + "groups" + File.separator + groupName);
        };
    }

    private static @Nullable UUID parseUuidFromFilename(String filename, String expectedPlayerName) {
        // filename: <playerName>-<uuid>.yml
        if (filename == null) {
            return null;
        }

        String prefix = expectedPlayerName + "-";
        if (!filename.startsWith(prefix) || !filename.endsWith(".yml")) {
            return null;
        }

        String uuidStr = filename.substring(prefix.length(), filename.length() - 4);
        try {
            return UUID.fromString(uuidStr);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static PlayerDataSnapshot fromConfig(
            StorageType storageType,
            String groupName,
            UUID uuid,
            File file,
            FileConfiguration config
    ) {
        ItemStack[] inventory = readItemSectionAsArray(config, "inventory");
        ItemStack[] enderChest = readItemSectionAsArray(config, "ender-chest");

        ItemStack offHand = config.getItemStack("off_hand_item");

        ItemStack helmet = config.getItemStack("armor_contents.helmet");
        ItemStack chestplate = config.getItemStack("armor_contents.chestplate");
        ItemStack leggings = config.getItemStack("armor_contents.leggings");
        ItemStack boots = config.getItemStack("armor_contents.boots");

        String gamemode = config.getString("gamemode");

        Boolean allowFlight = config.contains("allow_flight") ? config.getBoolean("allow_flight") : null;
        Boolean flying = config.contains("flying") ? config.getBoolean("flying") : null;
        Float flySpeed = config.contains("fly_speed") ? (float) config.getDouble("fly_speed") : null;

        Float exp = config.contains("exp") ? (float) config.getDouble("exp") : null;
        Integer expLevel = config.contains("exp-level") ? config.getInt("exp-level") : null;

        Integer hunger = config.contains("hunger") ? config.getInt("hunger") : null;

        Double health = config.contains("health") ? config.getDouble("health") : null;
        Double maxHealth = config.contains("max-health") ? config.getDouble("max-health") : null;
        Double auraSkillsHealth = config.contains("auraskills.health") ? config.getDouble("auraskills.health") : null;
        Double auraSkillsMana = config.contains("auraskills.mana") ? config.getDouble("auraskills.mana") : null;

        List<PotionEffectSnapshot> potionEffects = readPotionEffects(config);

        return new PlayerDataSnapshot(
                storageType,
                groupName,
                uuid,
                file,
                inventory,
                enderChest,
                offHand,
                helmet,
                chestplate,
                leggings,
                boots,
                gamemode,
                allowFlight,
                flying,
                flySpeed,
                exp,
                expLevel,
                hunger,
                health,
                maxHealth,
                auraSkillsHealth,
            auraSkillsMana,
                potionEffects
        );
    }

    private static ItemStack[] readItemSectionAsArray(FileConfiguration config, String path) {
        if (!config.contains(path) || config.getConfigurationSection(path) == null) {
            return new ItemStack[0];
        }

        int maxIndex = -1;
        for (String key : Objects.requireNonNull(config.getConfigurationSection(path)).getKeys(false)) {
            try {
                int idx = Integer.parseInt(key);
                if (idx > maxIndex) {
                    maxIndex = idx;
                }
            } catch (NumberFormatException ignored) {
                // ignore
            }
        }

        if (maxIndex < 0) {
            return new ItemStack[0];
        }

        ItemStack[] out = new ItemStack[maxIndex + 1];
        for (int i = 0; i <= maxIndex; i++) {
            out[i] = config.getItemStack(path + "." + i);
        }
        return out;
    }

    private static List<PotionEffectSnapshot> readPotionEffects(FileConfiguration config) {
        if (!config.contains("potion_effect") || config.getConfigurationSection("potion_effect") == null) {
            return Collections.emptyList();
        }

        List<PotionEffectSnapshot> list = new ArrayList<>();
        for (String key : Objects.requireNonNull(config.getConfigurationSection("potion_effect")).getKeys(false)) {
            String typeName = config.getString("potion_effect." + key + ".type");
            int duration = config.getInt("potion_effect." + key + ".duration");
            int amplifier = config.getInt("potion_effect." + key + ".amplifier");

            PotionEffectType type = typeName != null ? PotionEffectType.getByName(typeName) : null;
            list.add(new PotionEffectSnapshot(typeName, type, duration, amplifier));
        }
        return Collections.unmodifiableList(list);
    }

    /**
     * Simple snapshot entry for potion effects read from YAML.
     */
    public record PotionEffectSnapshot(
            @Nullable String typeName,
            @Nullable PotionEffectType type,
            int duration,
            int amplifier
    ) {
        public @Nullable PotionEffect toBukkitEffect() {
            if (type == null) {
                return null;
            }
            return new PotionEffect(type, duration, amplifier);
        }
    }

    /**
     * Snapshot of the player's saved file.
     */
    public record PlayerDataSnapshot(
            @NotNull StorageType storageType,
            @NotNull String groupName,
            @NotNull UUID playerUuid,
            @NotNull File file,
            @NotNull ItemStack[] inventory,
            @NotNull ItemStack[] enderChest,
            @Nullable ItemStack offHand,
            @Nullable ItemStack helmet,
            @Nullable ItemStack chestplate,
            @Nullable ItemStack leggings,
            @Nullable ItemStack boots,
            @Nullable String gamemode,
            @Nullable Boolean allowFlight,
            @Nullable Boolean flying,
            @Nullable Float flySpeed,
            @Nullable Float exp,
            @Nullable Integer expLevel,
            @Nullable Integer hunger,
            @Nullable Double health,
            @Nullable Double maxHealth,
            @Nullable Double auraSkillsHealth,
            @Nullable Double auraSkillsMana,
            @NotNull List<PotionEffectSnapshot> potionEffects
    ) {
    }
}
