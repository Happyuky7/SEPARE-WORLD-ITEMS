package com.github.happyuky7.separeWorldItems.storage.backends;

import com.github.happyuky7.separeWorldItems.SepareWorldItems;
import com.github.happyuky7.separeWorldItems.storage.PlayerDataScope;
import com.github.happyuky7.separeWorldItems.storage.PlayerDataStore;
import com.github.happyuky7.separeWorldItems.storage.StorageType;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Locale;
import java.util.UUID;

public final class SqlPlayerDataStore implements PlayerDataStore {

    private final SepareWorldItems plugin;
    private final HikariDataSource dataSource;
    private final Dialect dialect;

    private enum Dialect {
        SQLITE,
        MYSQL
    }

    private SqlPlayerDataStore(SepareWorldItems plugin, HikariDataSource dataSource, Dialect dialect) {
        this.plugin = plugin;
        this.dataSource = dataSource;
        this.dialect = dialect;
    }

    public static SqlPlayerDataStore create(SepareWorldItems plugin, StorageType type) {
        String jdbcUrl;
        String username = plugin.getConfig().getString("storage.sql.username", "");
        String password = plugin.getConfig().getString("storage.sql.password", "");

        String driverClassName = null;

        if (type == StorageType.SQLITE) {
            String fileName = plugin.getConfig().getString("storage.sqlite.file", "separeworlditems.sqlite");
            File dbFile = new File(plugin.getDataFolder(), fileName);
            jdbcUrl = "jdbc:sqlite:" + dbFile.getAbsolutePath();
            username = "";
            password = "";
            driverClassName = "org.sqlite.JDBC";
        } else {
            // MYSQL or MARIADB
            jdbcUrl = plugin.getConfig().getString("storage.sql.jdbc-url", "");
            if (jdbcUrl == null || jdbcUrl.isEmpty()) {
                // Compatibility keys
                jdbcUrl = plugin.getConfig().getString("storage.mysql.jdbc-url", "");
                if (jdbcUrl == null || jdbcUrl.isEmpty()) {
                    jdbcUrl = plugin.getConfig().getString("storage.mariadb.jdbc-url", "");
                }
            }

            // Alternative config: host/port/database (+ optional parameters)
            if (jdbcUrl == null || jdbcUrl.isEmpty()) {
                String host = plugin.getConfig().getString("storage.sql.host", "");
                int port = plugin.getConfig().getInt("storage.sql.port", 3306);
                String database = plugin.getConfig().getString("storage.sql.database", "");
                String parameters = plugin.getConfig().getString("storage.sql.parameters", "");

                if (host != null && !host.isEmpty() && database != null && !database.isEmpty()) {
                    String prefix = (type == StorageType.MARIADB) ? "jdbc:mariadb://" : "jdbc:mysql://";
                    StringBuilder sb = new StringBuilder(prefix)
                        .append(host)
                        .append(":")
                        .append(Math.max(1, port))
                        .append("/")
                        .append(database);

                    if (parameters != null && !parameters.isEmpty()) {
                        String p = parameters.trim();
                        if (!p.startsWith("?") && !p.startsWith(";")) {
                            sb.append("?");
                        }
                        sb.append(p);
                    }
                    jdbcUrl = sb.toString();
                }
            }

            if (jdbcUrl != null) {
                String urlLower = jdbcUrl.toLowerCase(Locale.ROOT);
                if (urlLower.startsWith("jdbc:mariadb:")) {
                    driverClassName = "org.mariadb.jdbc.Driver";
                } else if (urlLower.startsWith("jdbc:mysql:")) {
                    driverClassName = "com.mysql.cj.jdbc.Driver";
                }
            }
        }

        if (jdbcUrl == null || jdbcUrl.isEmpty()) {
            throw new IllegalArgumentException("Missing storage.sql.jdbc-url (or storage.sqlite.file)");
        }

        HikariConfig cfg = new HikariConfig();
        cfg.setJdbcUrl(jdbcUrl);
        if (driverClassName != null && !driverClassName.isEmpty()) {
            try {
                Class.forName(driverClassName);
            } catch (ClassNotFoundException ignored) {
                // shaded jar might not include the selected driver
            }
            cfg.setDriverClassName(driverClassName);
        }
        if (username != null && !username.isEmpty()) {
            cfg.setUsername(username);
        }
        if (password != null && !password.isEmpty()) {
            cfg.setPassword(password);
        }

        int maxPool = plugin.getConfig().getInt("storage.sql.pool.maxPoolSize", 10);
        cfg.setMaximumPoolSize(Math.max(1, maxPool));
        cfg.setPoolName("SepareWorldItems-SQL");

        HikariDataSource ds = new HikariDataSource(cfg);

        Dialect dialect = detectDialect(ds);
        SqlPlayerDataStore store = new SqlPlayerDataStore(plugin, ds, dialect);
        store.ensureSchema();
        return store;
    }

    @Override
    public boolean exists(@NotNull PlayerDataScope scope, @NotNull String groupName, @NotNull UUID playerUuid) {
        String sql = "SELECT 1 FROM swi_player_data WHERE scope=? AND group_name=? AND player_uuid=?";
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, scope.name());
            ps.setString(2, groupName);
            ps.setString(3, playerUuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public @NotNull YamlConfiguration load(@NotNull PlayerDataScope scope, @NotNull String groupName, @NotNull UUID playerUuid) {
        String sql = "SELECT yaml FROM swi_player_data WHERE scope=? AND group_name=? AND player_uuid=?";
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, scope.name());
            ps.setString(2, groupName);
            ps.setString(3, playerUuid.toString());

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return new YamlConfiguration();
                }
                String yaml = rs.getString(1);
                YamlConfiguration config = new YamlConfiguration();
                if (yaml != null && !yaml.isEmpty()) {
                    try {
                        config.loadFromString(yaml);
                    } catch (Exception e) {
                        plugin.getLogger().warning("[Storage:SQL] Failed to parse YAML for " + playerUuid + ": " + e.getMessage());
                    }
                }
                return config;
            }
        } catch (Exception e) {
            plugin.getLogger().warning("[Storage:SQL] Load failed: " + e.getMessage());
            return new YamlConfiguration();
        }
    }

    @Override
    public void save(@NotNull PlayerDataScope scope, @NotNull String groupName, @NotNull UUID playerUuid, @NotNull String playerName, @NotNull YamlConfiguration config) {
        String yaml = config.saveToString();
        long now = System.currentTimeMillis();
        String playerLower = playerName.toLowerCase(Locale.ROOT);

        String sql;
        if (dialect == Dialect.SQLITE) {
            sql = "INSERT INTO swi_player_data(scope, group_name, player_uuid, player_name, player_name_lower, yaml, updated_at) " +
                    "VALUES(?,?,?,?,?,?,?) " +
                    "ON CONFLICT(scope, group_name, player_uuid) DO UPDATE SET " +
                    "player_name=excluded.player_name, player_name_lower=excluded.player_name_lower, yaml=excluded.yaml, updated_at=excluded.updated_at";
        } else {
            sql = "INSERT INTO swi_player_data(scope, group_name, player_uuid, player_name, player_name_lower, yaml, updated_at) " +
                    "VALUES(?,?,?,?,?,?,?) " +
                    "ON DUPLICATE KEY UPDATE " +
                    "player_name=VALUES(player_name), player_name_lower=VALUES(player_name_lower), yaml=VALUES(yaml), updated_at=VALUES(updated_at)";
        }

        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, scope.name());
            ps.setString(2, groupName);
            ps.setString(3, playerUuid.toString());
            ps.setString(4, playerName);
            ps.setString(5, playerLower);
            ps.setString(6, yaml);
            ps.setLong(7, now);
            ps.executeUpdate();
        } catch (Exception e) {
            plugin.getLogger().warning("[Storage:SQL] Save failed: " + e.getMessage());
        }
    }

    @Override
    public @Nullable UUID resolveUuidByName(@NotNull PlayerDataScope scope, @NotNull String groupName, @NotNull String playerName) {
        String sql = "SELECT player_uuid FROM swi_player_data WHERE scope=? AND group_name=? AND player_name_lower=? ORDER BY updated_at DESC LIMIT 1";
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, scope.name());
            ps.setString(2, groupName);
            ps.setString(3, playerName.toLowerCase(Locale.ROOT));
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return UUID.fromString(rs.getString(1));
            }
        } catch (Exception e) {
            plugin.getLogger().warning("[Storage:SQL] resolveUuidByName failed: " + e.getMessage());
            return null;
        }
    }

    @Override
    public void forEachEntry(@NotNull PlayerDataScope scope, @NotNull PlayerDataEntryConsumer consumer) {
        String sql = "SELECT group_name, player_uuid, player_name FROM swi_player_data WHERE scope=?";
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, scope.name());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String group = rs.getString(1);
                    String uuidStr = rs.getString(2);
                    String name = rs.getString(3);

                    if (group == null || uuidStr == null) {
                        continue;
                    }
                    UUID uuid;
                    try {
                        uuid = UUID.fromString(uuidStr);
                    } catch (IllegalArgumentException ignored) {
                        continue;
                    }
                    consumer.accept(group, uuid, name);
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("[Storage:SQL] forEachEntry failed: " + e.getMessage());
        }
    }

    @Override
    public void close() {
        try {
            dataSource.close();
        } catch (Throwable ignored) {
        }
    }

    private void ensureSchema() {
        try (Connection c = dataSource.getConnection();
             Statement st = c.createStatement()) {

            if (dialect == Dialect.SQLITE) {
                st.executeUpdate(
                        "CREATE TABLE IF NOT EXISTS swi_player_data (" +
                                "scope TEXT NOT NULL," +
                                "group_name TEXT NOT NULL," +
                                "player_uuid TEXT NOT NULL," +
                                "player_name TEXT," +
                                "player_name_lower TEXT," +
                                "yaml TEXT," +
                                "updated_at INTEGER," +
                                "PRIMARY KEY (scope, group_name, player_uuid)" +
                                ")"
                );
                st.executeUpdate("CREATE INDEX IF NOT EXISTS idx_swi_name ON swi_player_data(scope, group_name, player_name_lower)");

                st.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS swi_change_logs (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "created_at INTEGER NOT NULL," +
                        "scope TEXT NOT NULL," +
                        "group_name TEXT NOT NULL," +
                        "player_uuid TEXT NOT NULL," +
                        "player_name TEXT," +
                        "yaml TEXT" +
                        ")"
                );
                st.executeUpdate("CREATE INDEX IF NOT EXISTS idx_swi_change_logs_ts ON swi_change_logs(created_at)");
            } else {
                st.executeUpdate(
                        "CREATE TABLE IF NOT EXISTS swi_player_data (" +
                                "scope VARCHAR(32) NOT NULL," +
                                "group_name VARCHAR(64) NOT NULL," +
                                "player_uuid VARCHAR(36) NOT NULL," +
                                "player_name VARCHAR(64)," +
                                "player_name_lower VARCHAR(64)," +
                                "yaml MEDIUMTEXT," +
                                "updated_at BIGINT," +
                                "PRIMARY KEY (scope, group_name, player_uuid)," +
                                "INDEX idx_swi_name (scope, group_name, player_name_lower)" +
                                ")"
                );

                st.executeUpdate(
                        "CREATE TABLE IF NOT EXISTS swi_change_logs (" +
                                "id BIGINT NOT NULL AUTO_INCREMENT," +
                                "created_at BIGINT NOT NULL," +
                                "scope VARCHAR(32) NOT NULL," +
                                "group_name VARCHAR(64) NOT NULL," +
                                "player_uuid VARCHAR(36) NOT NULL," +
                                "player_name VARCHAR(64)," +
                                "yaml MEDIUMTEXT," +
                                "PRIMARY KEY (id)," +
                                "INDEX idx_swi_change_logs_ts (created_at)" +
                                ")"
                );
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to init SQL schema: " + e.getMessage(), e);
        }
    }

    /**
     * Appends a snapshot log entry of the saved player data.
     * Called by {@code InventoryChangeLogs} on runtime saves (not migrations).
     */
    public void appendChangeLog(
            @NotNull PlayerDataScope scope,
            @NotNull String groupName,
            @NotNull UUID playerUuid,
            @NotNull String playerName,
            long createdAt,
            @NotNull String yaml
    ) {
        String sql = "INSERT INTO swi_change_logs(created_at, scope, group_name, player_uuid, player_name, yaml) VALUES(?,?,?,?,?,?)";
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, createdAt);
            ps.setString(2, scope.name());
            ps.setString(3, groupName);
            ps.setString(4, playerUuid.toString());
            ps.setString(5, playerName);
            ps.setString(6, yaml);
            ps.executeUpdate();
        } catch (Exception e) {
            plugin.getLogger().warning("[ChangeLogs:SQL] Append failed: " + e.getMessage());
        }
    }

    /**
     * Purges log entries older than cutoff.
     */
    public void purgeChangeLogsOlderThan(long cutoffMs) {
        String sql = "DELETE FROM swi_change_logs WHERE created_at < ?";
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, cutoffMs);
            ps.executeUpdate();
        } catch (Exception e) {
            plugin.getLogger().warning("[ChangeLogs:SQL] Purge failed: " + e.getMessage());
        }
    }

    private static Dialect detectDialect(HikariDataSource ds) {
        try (Connection c = ds.getConnection()) {
            DatabaseMetaData md = c.getMetaData();
            String product = md.getDatabaseProductName();
            if (product != null && product.toLowerCase(Locale.ROOT).contains("sqlite")) {
                return Dialect.SQLITE;
            }
        } catch (Exception ignored) {
        }
        return Dialect.MYSQL;
    }
}
