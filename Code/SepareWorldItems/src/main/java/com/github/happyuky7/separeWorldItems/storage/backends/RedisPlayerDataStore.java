package com.github.happyuky7.separeWorldItems.storage.backends;

import com.github.happyuky7.separeWorldItems.SepareWorldItems;
import com.github.happyuky7.separeWorldItems.storage.PlayerDataScope;
import com.github.happyuky7.separeWorldItems.storage.PlayerDataStore;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.params.ScanParams;
import redis.clients.jedis.resps.ScanResult;

import java.util.Locale;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public final class RedisPlayerDataStore implements PlayerDataStore {

    private final SepareWorldItems plugin;
    private final JedisPooled jedis;
    private final String keyPrefix;

    private RedisPlayerDataStore(SepareWorldItems plugin, JedisPooled jedis, String keyPrefix) {
        this.plugin = plugin;
        this.jedis = jedis;
        this.keyPrefix = keyPrefix;
    }

    public static RedisPlayerDataStore create(SepareWorldItems plugin) {
        String uri = plugin.getConfig().getString("storage.redis.uri", "redis://localhost:6379");
        String prefix = plugin.getConfig().getString("storage.redis.key-prefix", "swi");
        JedisPooled jedis = new JedisPooled(uri);
        return new RedisPlayerDataStore(plugin, jedis, prefix);
    }

    @Override
    public boolean exists(@NotNull PlayerDataScope scope, @NotNull String groupName, @NotNull UUID playerUuid) {
        try {
            return jedis.exists(dataKey(scope, groupName, playerUuid));
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public @NotNull YamlConfiguration load(@NotNull PlayerDataScope scope, @NotNull String groupName, @NotNull UUID playerUuid) {
        try {
            String yaml = jedis.get(dataKey(scope, groupName, playerUuid));
            YamlConfiguration config = new YamlConfiguration();
            if (yaml != null && !yaml.isEmpty()) {
                try {
                    config.loadFromString(yaml);
                } catch (Exception e) {
                    plugin.getLogger().warning("[Storage:Redis] Failed to parse YAML for " + playerUuid + ": " + e.getMessage());
                }
            }
            return config;
        } catch (Exception e) {
            plugin.getLogger().warning("[Storage:Redis] Load failed: " + e.getMessage());
            return new YamlConfiguration();
        }
    }

    @Override
    public void save(@NotNull PlayerDataScope scope, @NotNull String groupName, @NotNull UUID playerUuid, @NotNull String playerName, @NotNull YamlConfiguration config) {
        try {
            String yaml = config.saveToString();
            String dataKey = dataKey(scope, groupName, playerUuid);
            jedis.set(dataKey, yaml);

            // UUID index + name (helps migrations / snapshots)
            jedis.sadd(uuidSetKey(scope, groupName), playerUuid.toString());
            jedis.set(uuidNameKey(scope, groupName, playerUuid), playerName);

            // Name index
            jedis.set(nameKey(scope, groupName, playerName.toLowerCase(Locale.ROOT)), playerUuid.toString());
        } catch (Exception e) {
            plugin.getLogger().warning("[Storage:Redis] Save failed: " + e.getMessage());
        }
    }

    @Override
    public @Nullable UUID resolveUuidByName(@NotNull PlayerDataScope scope, @NotNull String groupName, @NotNull String playerName) {
        try {
            String uuid = jedis.get(nameKey(scope, groupName, playerName.toLowerCase(Locale.ROOT)));
            return uuid != null ? UUID.fromString(uuid) : null;
        } catch (Exception e) {
            plugin.getLogger().warning("[Storage:Redis] resolveUuidByName failed: " + e.getMessage());
            return null;
        }
    }

    @Override
    public void forEachEntry(@NotNull PlayerDataScope scope, @NotNull PlayerDataEntryConsumer consumer) {
        // Prefer set-based index when available, fallback to SCAN of data keys.
        try {
            String cursor = ScanParams.SCAN_POINTER_START;
            ScanParams params = new ScanParams().match(keyPrefix + ":data:" + scope.name() + ":*").count(500);

            do {
                ScanResult<String> res = jedis.scan(cursor, params);
                cursor = res.getCursor();
                List<String> keys = res.getResult();
                if (keys == null || keys.isEmpty()) {
                    continue;
                }

                for (String key : keys) {
                    // prefix:data:SCOPE:group:uuid
                    String[] parts = key.split(":", 6);
                    if (parts.length < 6) {
                        continue;
                    }
                    String group = parts[4];
                    String uuidStr = parts[5];
                    UUID uuid;
                    try {
                        uuid = UUID.fromString(uuidStr);
                    } catch (IllegalArgumentException ignored) {
                        continue;
                    }

                    String name = null;
                    try {
                        name = jedis.get(uuidNameKey(scope, group, uuid));
                    } catch (Exception ignored) {
                    }
                    consumer.accept(group, uuid, name);
                }

            } while (!"0".equals(cursor));
        } catch (Throwable t) {
            plugin.getLogger().warning("[Storage:Redis] forEachEntry failed: " + t.getMessage());
        }
    }

    @Override
    public void close() {
        try {
            jedis.close();
        } catch (Throwable ignored) {
        }
    }

    /**
     * Appends a snapshot log entry of the saved player data.
     *
     * <p>Implementation uses a per-entry key with EXPIRE to enforce retention.
     * Note: Redis can still evict keys under memory pressure (best-effort).</p>
     */
    public void appendChangeLog(
            @NotNull PlayerDataScope scope,
            @NotNull String groupName,
            @NotNull UUID playerUuid,
            @NotNull String playerName,
            long createdAt,
            @NotNull String yaml,
            int retentionDays
    ) {
        try {
            int expireSeconds = Math.max(30, retentionDays) * 86400;
            int rnd = ThreadLocalRandom.current().nextInt(1_000_000);
            String key = keyPrefix + ":changelog:" + scope.name() + ":" + groupName + ":" + playerUuid + ":" + createdAt + ":" + rnd;
            // Store a compact JSON payload for easier inspection.
            String value = "{\"ts\":" + createdAt + ",\"scope\":\"" + scope.name() + "\",\"group\":\"" + escapeJson(groupName) + "\",\"uuid\":\"" + playerUuid + "\",\"name\":\"" + escapeJson(playerName) + "\",\"yaml\":" + JSONObjectEscaper.quote(yaml) + "}";
            jedis.setex(key, expireSeconds, value);
        } catch (Exception e) {
            plugin.getLogger().warning("[ChangeLogs:Redis] Append failed: " + e.getMessage());
        }
    }

    private String dataKey(PlayerDataScope scope, String group, UUID uuid) {
        return keyPrefix + ":data:" + scope.name() + ":" + group + ":" + uuid;
    }

    private String nameKey(PlayerDataScope scope, String group, String lowerName) {
        return keyPrefix + ":name:" + scope.name() + ":" + group + ":" + lowerName;
    }

    private String uuidSetKey(PlayerDataScope scope, String group) {
        return keyPrefix + ":uuids:" + scope.name() + ":" + group;
    }

    private String uuidNameKey(PlayerDataScope scope, String group, UUID uuid) {
        return keyPrefix + ":uuidname:" + scope.name() + ":" + group + ":" + uuid;
    }

    private static String escapeJson(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    /**
     * Minimal JSON string quoting for large YAML payloads.
     */
    private static final class JSONObjectEscaper {
        private JSONObjectEscaper() {
        }

        static String quote(String s) {
            if (s == null) {
                return "\"\"";
            }
            StringBuilder sb = new StringBuilder(s.length() + 16);
            sb.append('"');
            for (int i = 0; i < s.length(); i++) {
                char c = s.charAt(i);
                switch (c) {
                    case '\\' -> sb.append("\\\\");
                    case '"' -> sb.append("\\\"");
                    case '\n' -> sb.append("\\n");
                    case '\r' -> sb.append("\\r");
                    case '\t' -> sb.append("\\t");
                    default -> {
                        if (c < 0x20) {
                            sb.append(String.format("\\u%04x", (int) c));
                        } else {
                            sb.append(c);
                        }
                    }
                }
            }
            sb.append('"');
            return sb.toString();
        }
    }
}
