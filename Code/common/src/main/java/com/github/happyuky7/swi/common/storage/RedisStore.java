package com.github.happyuky7.swi.common.storage;

import com.github.happyuky7.swi.api.context.ContextType;
import com.github.happyuky7.swi.api.context.GroupContext;
import com.github.happyuky7.swi.api.data.DataNode;
import com.github.happyuky7.swi.common.platform.SwiLogger;
import com.github.happyuky7.swi.common.yaml.YamlCodec;
import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.params.ScanParams;
import redis.clients.jedis.resps.ScanResult;

import java.util.Locale;
import java.util.UUID;

/**
 * Redis store. Same keys as the Bukkit plugin, so both platforms can read each other's records:
 * <pre>
 * &lt;prefix&gt;:data:&lt;SCOPE&gt;:&lt;group&gt;:&lt;uuid&gt;      record (YAML)
 * &lt;prefix&gt;:name:&lt;SCOPE&gt;:&lt;group&gt;:&lt;lowername&gt; uuid
 * &lt;prefix&gt;:uuids:&lt;SCOPE&gt;:&lt;group&gt;            set of uuids
 * &lt;prefix&gt;:uuidname:&lt;SCOPE&gt;:&lt;group&gt;:&lt;uuid&gt;  last name
 * </pre>
 */
public final class RedisStore implements PlayerDataStore {

    private final JedisPooled jedis;
    private final String prefix;
    private final SwiLogger logger;

    public RedisStore(String uri, String prefix, SwiLogger logger) {
        this.jedis = new JedisPooled(uri);
        this.prefix = prefix;
        this.logger = logger;
    }

    @Override
    public StorageType type() {
        return StorageType.REDIS;
    }

    private String scope(GroupContext context) {
        return PlayerDataStore.legacyScopeName(context.type());
    }

    private String dataKey(GroupContext c, UUID uuid) {
        return prefix + ":data:" + scope(c) + ":" + c.group() + ":" + uuid;
    }

    @Override
    public boolean exists(GroupContext context, UUID uuid) {
        try {
            return jedis.exists(dataKey(context, uuid));
        } catch (RuntimeException e) {
            throw new StorageException("[Storage:Redis] " + e.getMessage(), e);
        }
    }

    @Override
    public DataNode load(GroupContext context, UUID uuid) {
        String yaml;
        try {
            yaml = jedis.get(dataKey(context, uuid));
        } catch (RuntimeException e) {
            throw new StorageException("[Storage:Redis] " + e.getMessage(), e);
        }
        try {
            return YamlCodec.parse(yaml);
        } catch (RuntimeException e) {
            logger.error("[Storage:Redis] Corrupted record " + context + "/" + uuid + ", using empty data", e);
            return DataNode.create();
        }
    }

    @Override
    public void save(GroupContext context, UUID uuid, String playerName, DataNode data) {
        String s = scope(context);
        try {
            jedis.set(dataKey(context, uuid), YamlCodec.dump(data));
            jedis.sadd(prefix + ":uuids:" + s + ":" + context.group(), uuid.toString());
            jedis.set(prefix + ":uuidname:" + s + ":" + context.group() + ":" + uuid, playerName);
            jedis.set(prefix + ":name:" + s + ":" + context.group() + ":" + playerName.toLowerCase(Locale.ROOT), uuid.toString());
        } catch (RuntimeException e) {
            throw new StorageException("[Storage:Redis] " + e.getMessage(), e);
        }
    }

    @Override
    public UUID resolveUuidByName(GroupContext context, String playerName) {
        try {
            String uuid = jedis.get(prefix + ":name:" + scope(context) + ":" + context.group() + ":" + playerName.toLowerCase(Locale.ROOT));
            return uuid != null ? UUID.fromString(uuid) : null;
        } catch (IllegalArgumentException e) {
            return null;
        } catch (RuntimeException e) {
            throw new StorageException("[Storage:Redis] " + e.getMessage(), e);
        }
    }

    @Override
    public void forEachEntry(ContextType type, EntryConsumer consumer) {
        String scope = PlayerDataStore.legacyScopeName(type);
        String cursor = ScanParams.SCAN_POINTER_START;
        ScanParams params = new ScanParams().match(prefix + ":data:" + scope + ":*").count(500);
        int skip = (prefix + ":data:" + scope + ":").length();
        do {
            ScanResult<String> result = jedis.scan(cursor, params);
            cursor = result.getCursor();
            for (String key : result.getResult()) {
                // <group>:<uuid>, the uuid is the last 36 characters (groups may contain ':')
                String rest = key.substring(skip);
                if (rest.length() < 38) continue;
                String group = rest.substring(0, rest.length() - 37);
                try {
                    UUID uuid = UUID.fromString(rest.substring(rest.length() - 36));
                    consumer.accept(group, uuid, jedis.get(prefix + ":uuidname:" + scope + ":" + group + ":" + uuid));
                } catch (IllegalArgumentException ignored) {
                    // not a record key
                }
            }
        } while (!ScanParams.SCAN_POINTER_START.equals(cursor));
    }

    @Override
    public void close() {
        jedis.close();
    }
}
