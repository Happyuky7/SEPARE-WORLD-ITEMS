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
        return storageType == StorageType.WORLDGUARD_GROUP ? PlayerDataScope.WORLDGUARD_GROUP : PlayerDataScope.WORLD_GROUP;
    }

    private static File getGroupDir(StorageType storageType, String groupName) {
        File dataFolder = SepareWorldItems.getInstance().getDataFolder();
        return storageType == StorageType.WORLDGUARD_GROUP
            ? new File(dataFolder, "worldguard" + File.separator + "groups" + File.separator + groupName)
            : new File(dataFolder, "groups" + File.separator + groupName);
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
    public static final class PotionEffectSnapshot {
        private final String typeName;
        private final PotionEffectType type;
        private final int duration;
        private final int amplifier;

        public PotionEffectSnapshot(@Nullable String typeName, @Nullable PotionEffectType type, int duration, int amplifier) {
            this.typeName = typeName;
            this.type = type;
            this.duration = duration;
            this.amplifier = amplifier;
        }

        public @Nullable String typeName() {
            return typeName;
        }

        public @Nullable PotionEffectType type() {
            return type;
        }

        public int duration() {
            return duration;
        }

        public int amplifier() {
            return amplifier;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof PotionEffectSnapshot)) return false;
            PotionEffectSnapshot that = (PotionEffectSnapshot) o;
            return java.util.Objects.equals(typeName, that.typeName)
                    && java.util.Objects.equals(type, that.type)
                    && duration == that.duration
                    && amplifier == that.amplifier;
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(typeName, type, duration, amplifier);
        }

        @Override
        public String toString() {
            return "PotionEffectSnapshot[" + "typeName=" + typeName + ", " + "type=" + type + ", " + "duration=" + duration + ", " + "amplifier=" + amplifier + "]";
        }

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
    public static final class PlayerDataSnapshot {
        private final StorageType storageType;
        private final String groupName;
        private final UUID playerUuid;
        private final File file;
        private final ItemStack[] inventory;
        private final ItemStack[] enderChest;
        private final ItemStack offHand;
        private final ItemStack helmet;
        private final ItemStack chestplate;
        private final ItemStack leggings;
        private final ItemStack boots;
        private final String gamemode;
        private final Boolean allowFlight;
        private final Boolean flying;
        private final Float flySpeed;
        private final Float exp;
        private final Integer expLevel;
        private final Integer hunger;
        private final Double health;
        private final Double maxHealth;
        private final Double auraSkillsHealth;
        private final Double auraSkillsMana;
        private final List<PotionEffectSnapshot> potionEffects;

        public PlayerDataSnapshot(@NotNull StorageType storageType, @NotNull String groupName, @NotNull UUID playerUuid, @NotNull File file, @NotNull ItemStack[] inventory, @NotNull ItemStack[] enderChest, @Nullable ItemStack offHand, @Nullable ItemStack helmet, @Nullable ItemStack chestplate, @Nullable ItemStack leggings, @Nullable ItemStack boots, @Nullable String gamemode, @Nullable Boolean allowFlight, @Nullable Boolean flying, @Nullable Float flySpeed, @Nullable Float exp, @Nullable Integer expLevel, @Nullable Integer hunger, @Nullable Double health, @Nullable Double maxHealth, @Nullable Double auraSkillsHealth, @Nullable Double auraSkillsMana, @NotNull List<PotionEffectSnapshot> potionEffects) {
            this.storageType = storageType;
            this.groupName = groupName;
            this.playerUuid = playerUuid;
            this.file = file;
            this.inventory = inventory;
            this.enderChest = enderChest;
            this.offHand = offHand;
            this.helmet = helmet;
            this.chestplate = chestplate;
            this.leggings = leggings;
            this.boots = boots;
            this.gamemode = gamemode;
            this.allowFlight = allowFlight;
            this.flying = flying;
            this.flySpeed = flySpeed;
            this.exp = exp;
            this.expLevel = expLevel;
            this.hunger = hunger;
            this.health = health;
            this.maxHealth = maxHealth;
            this.auraSkillsHealth = auraSkillsHealth;
            this.auraSkillsMana = auraSkillsMana;
            this.potionEffects = potionEffects;
        }

        public @NotNull StorageType storageType() {
            return storageType;
        }

        public @NotNull String groupName() {
            return groupName;
        }

        public @NotNull UUID playerUuid() {
            return playerUuid;
        }

        public @NotNull File file() {
            return file;
        }

        public @NotNull ItemStack[] inventory() {
            return inventory;
        }

        public @NotNull ItemStack[] enderChest() {
            return enderChest;
        }

        public @Nullable ItemStack offHand() {
            return offHand;
        }

        public @Nullable ItemStack helmet() {
            return helmet;
        }

        public @Nullable ItemStack chestplate() {
            return chestplate;
        }

        public @Nullable ItemStack leggings() {
            return leggings;
        }

        public @Nullable ItemStack boots() {
            return boots;
        }

        public @Nullable String gamemode() {
            return gamemode;
        }

        public @Nullable Boolean allowFlight() {
            return allowFlight;
        }

        public @Nullable Boolean flying() {
            return flying;
        }

        public @Nullable Float flySpeed() {
            return flySpeed;
        }

        public @Nullable Float exp() {
            return exp;
        }

        public @Nullable Integer expLevel() {
            return expLevel;
        }

        public @Nullable Integer hunger() {
            return hunger;
        }

        public @Nullable Double health() {
            return health;
        }

        public @Nullable Double maxHealth() {
            return maxHealth;
        }

        public @Nullable Double auraSkillsHealth() {
            return auraSkillsHealth;
        }

        public @Nullable Double auraSkillsMana() {
            return auraSkillsMana;
        }

        public @NotNull List<PotionEffectSnapshot> potionEffects() {
            return potionEffects;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof PlayerDataSnapshot)) return false;
            PlayerDataSnapshot that = (PlayerDataSnapshot) o;
            return java.util.Objects.equals(storageType, that.storageType)
                    && java.util.Objects.equals(groupName, that.groupName)
                    && java.util.Objects.equals(playerUuid, that.playerUuid)
                    && java.util.Objects.equals(file, that.file)
                    && java.util.Objects.equals(inventory, that.inventory)
                    && java.util.Objects.equals(enderChest, that.enderChest)
                    && java.util.Objects.equals(offHand, that.offHand)
                    && java.util.Objects.equals(helmet, that.helmet)
                    && java.util.Objects.equals(chestplate, that.chestplate)
                    && java.util.Objects.equals(leggings, that.leggings)
                    && java.util.Objects.equals(boots, that.boots)
                    && java.util.Objects.equals(gamemode, that.gamemode)
                    && java.util.Objects.equals(allowFlight, that.allowFlight)
                    && java.util.Objects.equals(flying, that.flying)
                    && java.util.Objects.equals(flySpeed, that.flySpeed)
                    && java.util.Objects.equals(exp, that.exp)
                    && java.util.Objects.equals(expLevel, that.expLevel)
                    && java.util.Objects.equals(hunger, that.hunger)
                    && java.util.Objects.equals(health, that.health)
                    && java.util.Objects.equals(maxHealth, that.maxHealth)
                    && java.util.Objects.equals(auraSkillsHealth, that.auraSkillsHealth)
                    && java.util.Objects.equals(auraSkillsMana, that.auraSkillsMana)
                    && java.util.Objects.equals(potionEffects, that.potionEffects);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(storageType, groupName, playerUuid, file, inventory, enderChest, offHand, helmet, chestplate, leggings, boots, gamemode, allowFlight, flying, flySpeed, exp, expLevel, hunger, health, maxHealth, auraSkillsHealth, auraSkillsMana, potionEffects);
        }

        @Override
        public String toString() {
            return "PlayerDataSnapshot[" + "storageType=" + storageType + ", " + "groupName=" + groupName + ", " + "playerUuid=" + playerUuid + ", " + "file=" + file + ", " + "inventory=" + inventory + ", " + "enderChest=" + enderChest + ", " + "offHand=" + offHand + ", " + "helmet=" + helmet + ", " + "chestplate=" + chestplate + ", " + "leggings=" + leggings + ", " + "boots=" + boots + ", " + "gamemode=" + gamemode + ", " + "allowFlight=" + allowFlight + ", " + "flying=" + flying + ", " + "flySpeed=" + flySpeed + ", " + "exp=" + exp + ", " + "expLevel=" + expLevel + ", " + "hunger=" + hunger + ", " + "health=" + health + ", " + "maxHealth=" + maxHealth + ", " + "auraSkillsHealth=" + auraSkillsHealth + ", " + "auraSkillsMana=" + auraSkillsMana + ", " + "potionEffects=" + potionEffects + "]";
        }
    }
}
