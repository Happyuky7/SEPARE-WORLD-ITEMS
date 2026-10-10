package com.github.happyuky7.swi.common.storage;

import com.github.happyuky7.swi.api.context.ContextType;
import com.github.happyuky7.swi.api.context.GroupContext;
import com.github.happyuky7.swi.api.data.DataNode;
import com.github.happyuky7.swi.common.platform.SwiLogger;
import com.github.happyuky7.swi.common.yaml.YamlCodec;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * One YAML file per player and group, same layout as the Bukkit plugin:
 * {@code groups/<group>/<name>-<uuid>.yml} and {@code worldguard/groups/<group>/<name>-<uuid>.yml} for regions.
 */
public final class YamlFileStore implements PlayerDataStore {

    private static final Pattern FILE_PATTERN = Pattern.compile("^(.+)-([0-9a-fA-F\\-]{36})\\.yml$");

    private final File dataFolder;
    private final SwiLogger logger;

    public YamlFileStore(File dataFolder, SwiLogger logger) {
        this.dataFolder = dataFolder;
        this.logger = logger;
    }

    @Override
    public StorageType type() {
        return StorageType.YAML;
    }

    @Override
    public boolean exists(GroupContext context, UUID uuid) {
        return findFile(context, uuid) != null;
    }

    @Override
    public DataNode load(GroupContext context, UUID uuid) {
        File file = findFile(context, uuid);
        if (file == null) {
            return DataNode.create();
        }
        try {
            return YamlCodec.read(file);
        } catch (Exception e) {
            logger.error("[Storage:YAML] Cannot read " + file + ", using empty data", e);
            return DataNode.create();
        }
    }

    @Override
    public void save(GroupContext context, UUID uuid, String playerName, DataNode data) {
        File target = new File(groupDir(context), playerName + "-" + uuid + ".yml");
        try {
            YamlCodec.write(target, data);
        } catch (IOException e) {
            throw new StorageException("Cannot write " + target, e);
        }
        // A renamed player leaves an old <oldName>-<uuid>.yml behind: remove it so lookups stay unambiguous.
        File[] stale = groupDir(context).listFiles((dir, name) -> name.endsWith("-" + uuid + ".yml") && !name.equals(target.getName()));
        if (stale != null) {
            for (File f : stale) {
                if (!f.delete()) {
                    logger.warn("[Storage:YAML] Could not delete stale file " + f);
                }
            }
        }
    }

    @Override
    public UUID resolveUuidByName(GroupContext context, String playerName) {
        File[] files = groupDir(context).listFiles((dir, name) -> name.toLowerCase(Locale.ROOT).startsWith(playerName.toLowerCase(Locale.ROOT) + "-"));
        File newest = newest(files);
        if (newest == null) {
            return null;
        }
        Matcher m = FILE_PATTERN.matcher(newest.getName());
        return m.matches() && m.group(1).equalsIgnoreCase(playerName) ? parseUuid(m.group(2)) : null;
    }

    @Override
    public void forEachEntry(ContextType type, EntryConsumer consumer) {
        File root = rootDir(type);
        File[] groupDirs = root.listFiles(File::isDirectory);
        if (groupDirs == null) {
            return;
        }
        for (File groupDir : groupDirs) {
            File[] files = groupDir.listFiles((dir, name) -> name.toLowerCase(Locale.ROOT).endsWith(".yml"));
            if (files == null) {
                continue;
            }
            Map<UUID, File> newestPerUuid = new HashMap<>();
            Map<UUID, String> namePerUuid = new HashMap<>();
            for (File file : files) {
                Matcher m = FILE_PATTERN.matcher(file.getName());
                UUID uuid = m.matches() ? parseUuid(m.group(2)) : null;
                if (uuid == null) {
                    continue;
                }
                File current = newestPerUuid.get(uuid);
                if (current == null || file.lastModified() > current.lastModified()) {
                    newestPerUuid.put(uuid, file);
                    namePerUuid.put(uuid, m.group(1));
                }
            }
            for (UUID uuid : newestPerUuid.keySet()) {
                consumer.accept(groupDir.getName(), uuid, namePerUuid.get(uuid));
            }
        }
    }

    private File findFile(GroupContext context, UUID uuid) {
        String suffix = "-" + uuid + ".yml";
        return newest(groupDir(context).listFiles((dir, name) -> name.endsWith(suffix)));
    }

    private static File newest(File[] files) {
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

    private File rootDir(ContextType type) {
        return type == ContextType.REGION
                ? new File(dataFolder, "worldguard" + File.separator + "groups")
                : new File(dataFolder, "groups");
    }

    private File groupDir(GroupContext context) {
        return new File(rootDir(context.type()), context.group());
    }

    private static UUID parseUuid(String s) {
        try {
            return UUID.fromString(s);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
