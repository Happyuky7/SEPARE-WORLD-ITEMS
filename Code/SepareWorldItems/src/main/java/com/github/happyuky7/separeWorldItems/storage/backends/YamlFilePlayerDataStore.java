package com.github.happyuky7.separeWorldItems.storage.backends;

import com.github.happyuky7.separeWorldItems.SepareWorldItems;
import com.github.happyuky7.separeWorldItems.files.FileManagerData;
import com.github.happyuky7.separeWorldItems.storage.PlayerDataScope;
import com.github.happyuky7.separeWorldItems.storage.PlayerDataStore;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class YamlFilePlayerDataStore implements PlayerDataStore {

    private static final Pattern FILE_PATTERN = Pattern.compile("^(.+)-([0-9a-fA-F\\-]{36})\\.yml$");

    private final SepareWorldItems plugin;

    public YamlFilePlayerDataStore(SepareWorldItems plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean exists(@NotNull PlayerDataScope scope, @NotNull String groupName, @NotNull UUID playerUuid) {
        return findPlayerFile(scope, groupName, playerUuid) != null;
    }

    @Override
    public @NotNull YamlConfiguration load(@NotNull PlayerDataScope scope, @NotNull String groupName, @NotNull UUID playerUuid) {
        File file = findPlayerFile(scope, groupName, playerUuid);
        if (file == null) {
            return new YamlConfiguration();
        }

        return (YamlConfiguration) FileManagerData.getYaml(file);
    }

    @Override
    public void save(@NotNull PlayerDataScope scope, @NotNull String groupName, @NotNull UUID playerUuid, @NotNull String playerName, @NotNull YamlConfiguration config) {
        File file = getExpectedFile(scope, groupName, playerName, playerUuid);
        FileManagerData.saveConfiguration(file, config);
    }

    @Override
    public @Nullable UUID resolveUuidByName(@NotNull PlayerDataScope scope, @NotNull String groupName, @NotNull String playerName) {
        File groupDir = getGroupDir(scope, groupName);
        if (!groupDir.exists() || !groupDir.isDirectory()) {
            return null;
        }

        String prefix = playerName + "-";
        File[] files = groupDir.listFiles((dir, name) -> name.startsWith(prefix) && name.endsWith(".yml"));
        if (files == null || files.length == 0) {
            return null;
        }

        File newest = files[0];
        for (File f : files) {
            if (f.lastModified() > newest.lastModified()) {
                newest = f;
            }
        }

        return parseUuidFromFilename(newest.getName(), playerName);
    }

    @Override
    public boolean isFileBased() {
        return true;
    }

    @Override
    public void forEachEntry(@NotNull PlayerDataScope scope, @NotNull PlayerDataEntryConsumer consumer) {
        File root = switch (scope) {
            case WORLD_GROUP -> new File(plugin.getDataFolder(), "groups");
            case WORLDGUARD_GROUP -> new File(plugin.getDataFolder(), "worldguard" + File.separator + "groups");
        };

        if (!root.exists() || !root.isDirectory()) {
            return;
        }

        File[] groupDirs = root.listFiles(File::isDirectory);
        if (groupDirs == null) {
            return;
        }

        for (File groupDir : groupDirs) {
            String groupName = groupDir.getName();
            File[] files = groupDir.listFiles((dir, name) -> name.toLowerCase(Locale.ROOT).endsWith(".yml"));
            if (files == null || files.length == 0) {
                continue;
            }

            // keep newest file per UUID (player can have multiple files due to name changes)
            Map<UUID, File> newestPerUuid = new HashMap<>();
            Map<UUID, String> namePerUuid = new HashMap<>();

            for (File file : files) {
                Matcher m = FILE_PATTERN.matcher(file.getName());
                if (!m.matches()) {
                    continue;
                }
                String name = m.group(1);
                String uuidStr = m.group(2);
                UUID uuid;
                try {
                    uuid = UUID.fromString(uuidStr);
                } catch (IllegalArgumentException ignored) {
                    continue;
                }

                File current = newestPerUuid.get(uuid);
                if (current == null || file.lastModified() > current.lastModified()) {
                    newestPerUuid.put(uuid, file);
                    namePerUuid.put(uuid, name);
                }
            }

            for (Map.Entry<UUID, File> entry : newestPerUuid.entrySet()) {
                UUID uuid = entry.getKey();
                String name = namePerUuid.get(uuid);
                consumer.accept(groupName, uuid, name);
            }
        }
    }

    private File getExpectedFile(PlayerDataScope scope, String groupName, String playerName, UUID uuid) {
        return new File(getGroupDir(scope, groupName), playerName + "-" + uuid + ".yml");
    }

    private @Nullable File findPlayerFile(PlayerDataScope scope, String groupName, UUID uuid) {
        File groupDir = getGroupDir(scope, groupName);
        if (!groupDir.exists() || !groupDir.isDirectory()) {
            return null;
        }

        String suffix = "-" + uuid + ".yml";
        File[] files = groupDir.listFiles((dir, name) -> name.endsWith(suffix));
        if (files == null || files.length == 0) {
            return null;
        }

        File newest = files[0];
        for (File f : files) {
            if (f.lastModified() > newest.lastModified()) {
                newest = f;
            }
        }
        return newest;
    }

    private File getGroupDir(PlayerDataScope scope, String groupName) {
        File dataFolder = plugin.getDataFolder();
        return switch (scope) {
            case WORLD_GROUP -> new File(dataFolder, "groups" + File.separator + groupName);
            case WORLDGUARD_GROUP -> new File(dataFolder, "worldguard" + File.separator + "groups" + File.separator + groupName);
        };
    }

    private static @Nullable UUID parseUuidFromFilename(String filename, String expectedPlayerName) {
        if (filename == null) {
            return null;
        }

        String prefix = expectedPlayerName + "-";
        if (!filename.startsWith(prefix) || !filename.toLowerCase(Locale.ROOT).endsWith(".yml")) {
            return null;
        }

        String uuidStr = filename.substring(prefix.length(), filename.length() - 4);
        try {
            return UUID.fromString(uuidStr);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
