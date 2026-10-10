package com.github.happyuky7.swi.common.storage;

import com.github.happyuky7.swi.api.data.DataNode;
import com.github.happyuky7.swi.common.config.SwiSettings;
import com.github.happyuky7.swi.common.platform.SwiLogger;

import java.io.File;

/**
 * Creates the configured store from the {@code storage:} section (same keys as the Bukkit plugin).
 */
public final class StorageFactory {

    private StorageFactory() {
    }

    public static PlayerDataStore create(SwiSettings settings, File dataFolder, SwiLogger logger) {
        StorageType type = settings.storageType();
        DataNode cfg = settings.raw();
        switch (type) {
            case YAML:
                return new YamlFileStore(dataFolder, logger);
            case SQLITE: {
                File db = new File(dataFolder, cfg.getString("storage.sqlite.file", "separeworlditems.sqlite"));
                File parent = db.getAbsoluteFile().getParentFile();
                if (parent != null && !parent.isDirectory() && !parent.mkdirs()) {
                    throw new StorageException("Cannot create folder " + parent, null);
                }
                return new JdbcStore(type, "jdbc:sqlite:" + db.getAbsolutePath(), null, null, logger);
            }
            case MYSQL:
            case MARIADB:
                return new JdbcStore(type, sqlUrl(type, cfg),
                        cfg.getString("storage.sql.username", ""), cfg.getString("storage.sql.password", ""), logger);
            case MONGODB:
                try {
                    return new MongoStore(cfg.getString("storage.mongodb.uri", "mongodb://localhost:27017"),
                            cfg.getString("storage.mongodb.database", "separeworlditems"),
                            cfg.getString("storage.mongodb.collection", "player_data"), logger);
                } catch (LinkageError e) {
                    throw new StorageException("MongoDB driver not available in this build", null);
                }
            case REDIS:
                try {
                    return new RedisStore(cfg.getString("storage.redis.uri", "redis://localhost:6379"),
                            cfg.getString("storage.redis.key-prefix", "swi"), logger);
                } catch (LinkageError e) {
                    throw new StorageException("Redis driver not available in this build", null);
                }
            default:
                throw new UnsupportedOperationException("Storage type " + type + " is not available on this platform yet");
        }
    }

    static String sqlUrl(StorageType type, DataNode cfg) {
        String url = cfg.getString("storage.sql.jdbc-url", "");
        if (!url.isEmpty()) {
            return url;
        }
        String host = cfg.getString("storage.sql.host", "");
        String database = cfg.getString("storage.sql.database", "");
        if (host.isEmpty() || database.isEmpty()) {
            throw new IllegalArgumentException("Set storage.sql.jdbc-url or storage.sql.host + storage.sql.database");
        }
        StringBuilder sb = new StringBuilder(type == StorageType.MARIADB ? "jdbc:mariadb://" : "jdbc:mysql://")
                .append(host).append(':').append(Math.max(1, cfg.getInt("storage.sql.port", 3306))).append('/').append(database);
        String params = cfg.getString("storage.sql.parameters", "").trim();
        if (!params.isEmpty()) {
            if (!params.startsWith("?")) sb.append('?');
            sb.append(params);
        }
        return sb.toString();
    }
}
