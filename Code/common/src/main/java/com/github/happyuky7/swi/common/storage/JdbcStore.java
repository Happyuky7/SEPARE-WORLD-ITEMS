package com.github.happyuky7.swi.common.storage;

import com.github.happyuky7.swi.api.context.ContextType;
import com.github.happyuky7.swi.api.context.GroupContext;
import com.github.happyuky7.swi.api.data.DataNode;
import com.github.happyuky7.swi.common.platform.SwiLogger;
import com.github.happyuky7.swi.common.yaml.YamlCodec;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Locale;
import java.util.UUID;

/**
 * SQLite / MySQL / MariaDB store. Uses the same tables as the Bukkit plugin ({@code swi_player_data},
 * {@code swi_change_logs}), with the record kept as YAML text, so both can share a database.
 *
 * <p>Plain JDBC without a pool: one connection per operation for MySQL/MariaDB, one shared
 * (synchronized) connection for SQLite, which does not like concurrent writers.</p>
 */
public final class JdbcStore implements PlayerDataStore {

    private final StorageType type;
    private final String url;
    private final String user;
    private final String password;
    private final SwiLogger logger;
    private final boolean sqlite;
    private Connection shared;

    public JdbcStore(StorageType type, String url, String user, String password, SwiLogger logger) {
        this.type = type;
        this.url = adaptUrl(url);
        this.user = user;
        this.password = password;
        this.logger = logger;
        this.sqlite = type == StorageType.SQLITE;
        loadDriver();
        ensureSchema();
    }

    @Override
    public StorageType type() {
        return type;
    }

    @Override
    public boolean exists(GroupContext context, UUID uuid) {
        return query("SELECT 1 FROM swi_player_data WHERE scope=? AND group_name=? AND player_uuid=?", ps -> {
            bindKey(ps, context, uuid);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        });
    }

    @Override
    public DataNode load(GroupContext context, UUID uuid) {
        String yaml = query("SELECT yaml FROM swi_player_data WHERE scope=? AND group_name=? AND player_uuid=?", ps -> {
            bindKey(ps, context, uuid);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString(1) : null;
            }
        });
        try {
            return YamlCodec.parse(yaml);
        } catch (Exception e) {
            logger.error("[Storage:SQL] Corrupted record " + context + "/" + uuid + ", using empty data", e);
            return DataNode.create();
        }
    }

    @Override
    public void save(GroupContext context, UUID uuid, String playerName, DataNode data) {
        String sql = sqlite
                ? "INSERT INTO swi_player_data(scope, group_name, player_uuid, player_name, player_name_lower, yaml, updated_at) VALUES(?,?,?,?,?,?,?) "
                + "ON CONFLICT(scope, group_name, player_uuid) DO UPDATE SET player_name=excluded.player_name, "
                + "player_name_lower=excluded.player_name_lower, yaml=excluded.yaml, updated_at=excluded.updated_at"
                : "INSERT INTO swi_player_data(scope, group_name, player_uuid, player_name, player_name_lower, yaml, updated_at) VALUES(?,?,?,?,?,?,?) "
                + "ON DUPLICATE KEY UPDATE player_name=VALUES(player_name), player_name_lower=VALUES(player_name_lower), "
                + "yaml=VALUES(yaml), updated_at=VALUES(updated_at)";
        String yaml = YamlCodec.dump(data);
        query(sql, ps -> {
            bindKey(ps, context, uuid);
            ps.setString(4, playerName);
            ps.setString(5, playerName.toLowerCase(Locale.ROOT));
            ps.setString(6, yaml);
            ps.setLong(7, System.currentTimeMillis());
            return ps.executeUpdate();
        });
    }

    @Override
    public UUID resolveUuidByName(GroupContext context, String playerName) {
        String found = query("SELECT player_uuid FROM swi_player_data WHERE scope=? AND group_name=? AND player_name_lower=? "
                + "ORDER BY updated_at DESC LIMIT 1", ps -> {
            ps.setString(1, PlayerDataStore.legacyScopeName(context.type()));
            ps.setString(2, context.group());
            ps.setString(3, playerName.toLowerCase(Locale.ROOT));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString(1) : null;
            }
        });
        try {
            return found != null ? UUID.fromString(found) : null;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @Override
    public void forEachEntry(ContextType contextType, EntryConsumer consumer) {
        query("SELECT group_name, player_uuid, player_name FROM swi_player_data WHERE scope=?", ps -> {
            ps.setString(1, PlayerDataStore.legacyScopeName(contextType));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    try {
                        consumer.accept(rs.getString(1), UUID.fromString(rs.getString(2)), rs.getString(3));
                    } catch (IllegalArgumentException | NullPointerException ignored) {
                        // skip malformed rows
                    }
                }
            }
            return null;
        });
    }

    @Override
    public synchronized void close() {
        if (shared != null) {
            try {
                shared.close();
            } catch (SQLException ignored) {
            }
            shared = null;
        }
    }

    private interface SqlWork<T> {
        T run(PreparedStatement ps) throws SQLException;
    }

    private <T> T query(String sql, SqlWork<T> work) {
        try {
            if (sqlite) {
                synchronized (this) {
                    try (PreparedStatement ps = sharedConnection().prepareStatement(sql)) {
                        return work.run(ps);
                    }
                }
            }
            try (Connection c = open(); PreparedStatement ps = c.prepareStatement(sql)) {
                return work.run(ps);
            }
        } catch (SQLException e) {
            throw new StorageException("[Storage:" + type + "] " + e.getMessage(), e);
        }
    }

    private void bindKey(PreparedStatement ps, GroupContext context, UUID uuid) throws SQLException {
        ps.setString(1, PlayerDataStore.legacyScopeName(context.type()));
        ps.setString(2, context.group());
        ps.setString(3, uuid.toString());
    }

    private Connection sharedConnection() throws SQLException {
        if (shared == null || shared.isClosed()) {
            shared = open();
        }
        return shared;
    }

    private Connection open() throws SQLException {
        return user == null || user.isEmpty()
                ? DriverManager.getConnection(url)
                : DriverManager.getConnection(url, user, password);
    }

    /**
     * The Fabric mod only bundles the MariaDB driver, which speaks the MySQL protocol but only accepts
     * {@code jdbc:mysql:} URLs with {@code permitMysqlScheme}. Keep the URL untouched when Connector/J exists.
     */
    private static String adaptUrl(String url) {
        if (!url.toLowerCase(Locale.ROOT).startsWith("jdbc:mysql:") || url.contains("permitMysqlScheme")
                || isPresent("com.mysql.cj.jdbc.Driver") || !isPresent("org.mariadb.jdbc.Driver")) {
            return url;
        }
        return url + (url.indexOf('?') >= 0 ? "&" : "?") + "permitMysqlScheme";
    }

    private static boolean isPresent(String className) {
        try {
            Class.forName(className, false, JdbcStore.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    private void loadDriver() {
        String driver = sqlite ? "org.sqlite.JDBC"
                : url.toLowerCase(Locale.ROOT).startsWith("jdbc:mariadb:") || url.contains("permitMysqlScheme")
                ? "org.mariadb.jdbc.Driver" : "com.mysql.cj.jdbc.Driver";
        try {
            Class.forName(driver);
        } catch (ClassNotFoundException e) {
            // DriverManager may still find another driver for the URL through ServiceLoader.
            logger.debug("[Storage:" + type + "] Driver class " + driver + " not found, relying on DriverManager");
        }
    }

    private void ensureSchema() {
        String[] ddl = sqlite ? new String[]{
                "CREATE TABLE IF NOT EXISTS swi_player_data (scope TEXT NOT NULL, group_name TEXT NOT NULL, player_uuid TEXT NOT NULL, "
                        + "player_name TEXT, player_name_lower TEXT, yaml TEXT, updated_at INTEGER, PRIMARY KEY (scope, group_name, player_uuid))",
                "CREATE INDEX IF NOT EXISTS idx_swi_name ON swi_player_data(scope, group_name, player_name_lower)",
                "CREATE TABLE IF NOT EXISTS swi_change_logs (id INTEGER PRIMARY KEY AUTOINCREMENT, created_at INTEGER NOT NULL, scope TEXT NOT NULL, "
                        + "group_name TEXT NOT NULL, player_uuid TEXT NOT NULL, player_name TEXT, yaml TEXT)",
                "CREATE INDEX IF NOT EXISTS idx_swi_change_logs_ts ON swi_change_logs(created_at)"
        } : new String[]{
                "CREATE TABLE IF NOT EXISTS swi_player_data (scope VARCHAR(32) NOT NULL, group_name VARCHAR(64) NOT NULL, "
                        + "player_uuid VARCHAR(36) NOT NULL, player_name VARCHAR(64), player_name_lower VARCHAR(64), yaml MEDIUMTEXT, "
                        + "updated_at BIGINT, PRIMARY KEY (scope, group_name, player_uuid), INDEX idx_swi_name (scope, group_name, player_name_lower))",
                "CREATE TABLE IF NOT EXISTS swi_change_logs (id BIGINT NOT NULL AUTO_INCREMENT, created_at BIGINT NOT NULL, scope VARCHAR(32) NOT NULL, "
                        + "group_name VARCHAR(64) NOT NULL, player_uuid VARCHAR(36) NOT NULL, player_name VARCHAR(64), yaml MEDIUMTEXT, "
                        + "PRIMARY KEY (id), INDEX idx_swi_change_logs_ts (created_at))"
        };
        try {
            if (sqlite) {
                synchronized (this) {
                    try (Statement st = sharedConnection().createStatement()) {
                        for (String s : ddl) st.executeUpdate(s);
                    }
                }
            } else {
                try (Connection c = open(); Statement st = c.createStatement()) {
                    for (String s : ddl) st.executeUpdate(s);
                }
            }
        } catch (SQLException e) {
            throw new StorageException("[Storage:" + type + "] Cannot create schema: " + e.getMessage(), e);
        }
    }
}
