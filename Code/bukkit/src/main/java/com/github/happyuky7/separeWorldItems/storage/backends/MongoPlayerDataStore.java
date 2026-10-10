package com.github.happyuky7.separeWorldItems.storage.backends;

import com.github.happyuky7.separeWorldItems.SepareWorldItems;
import com.github.happyuky7.separeWorldItems.storage.PlayerDataScope;
import com.github.happyuky7.separeWorldItems.storage.PlayerDataStore;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.model.Updates;
import com.mongodb.client.model.UpdateOptions;
import org.bson.Document;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public final class MongoPlayerDataStore implements PlayerDataStore {

    private final SepareWorldItems plugin;
    private final MongoClient client;
    private final MongoCollection<Document> collection;
    private final MongoCollection<Document> changeLogs;

    private MongoPlayerDataStore(SepareWorldItems plugin, MongoClient client, MongoCollection<Document> collection, MongoCollection<Document> changeLogs) {
        this.plugin = plugin;
        this.client = client;
        this.collection = collection;
        this.changeLogs = changeLogs;
    }

    public static MongoPlayerDataStore create(SepareWorldItems plugin) {
        String uri = plugin.getConfig().getString("storage.mongodb.uri", "mongodb://localhost:27017");
        String dbName = plugin.getConfig().getString("storage.mongodb.database", "separeworlditems");
        String collectionName = plugin.getConfig().getString("storage.mongodb.collection", "player_data");
        String logsCollectionName = plugin.getConfig().getString("storage.mongodb.logs-collection", "change_logs");

        MongoClient client = MongoClients.create(uri);
        MongoDatabase db = client.getDatabase(dbName);
        MongoCollection<Document> coll = db.getCollection(collectionName);
        MongoCollection<Document> logs = db.getCollection(logsCollectionName);

        // Best-effort TTL index for retention using `expireAt`.
        try {
            logs.createIndex(Indexes.ascending("expireAt"), new IndexOptions().expireAfter(0L, TimeUnit.SECONDS));
        } catch (Throwable t) {
            plugin.getLogger().warning("[ChangeLogs:Mongo] TTL index creation failed: " + t.getMessage());
        }

        return new MongoPlayerDataStore(plugin, client, coll, logs);
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
            @NotNull String yaml,
            int retentionDays
    ) {
        try {
            long expireAtMs = createdAt + (retentionDays * 86400L * 1000L);
            Document doc = new Document("createdAt", createdAt)
                    .append("scope", scope.name())
                    .append("group", groupName)
                    .append("uuid", playerUuid.toString())
                    .append("name", playerName)
                    .append("yaml", yaml)
                    .append("expireAt", new Date(expireAtMs));
            changeLogs.insertOne(doc);
        } catch (Exception e) {
            plugin.getLogger().warning("[ChangeLogs:Mongo] Append failed: " + e.getMessage());
        }
    }

    @Override
    public boolean exists(@NotNull PlayerDataScope scope, @NotNull String groupName, @NotNull UUID playerUuid) {
        try {
            Document doc = collection.find(Filters.and(
                    Filters.eq("scope", scope.name()),
                    Filters.eq("group", groupName),
                    Filters.eq("uuid", playerUuid.toString())
            )).projection(new Document("_id", 1)).first();
            return doc != null;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public @NotNull YamlConfiguration load(@NotNull PlayerDataScope scope, @NotNull String groupName, @NotNull UUID playerUuid) {
        try {
            Document doc = collection.find(Filters.and(
                    Filters.eq("scope", scope.name()),
                    Filters.eq("group", groupName),
                    Filters.eq("uuid", playerUuid.toString())
            )).first();

            if (doc == null) {
                return new YamlConfiguration();
            }

            String yaml = doc.getString("yaml");
            YamlConfiguration config = new YamlConfiguration();
            if (yaml != null && !yaml.isEmpty()) {
                try {
                    config.loadFromString(yaml);
                } catch (Exception e) {
                    plugin.getLogger().warning("[Storage:Mongo] Failed to parse YAML for " + playerUuid + ": " + e.getMessage());
                }
            }
            return config;
        } catch (Exception e) {
            plugin.getLogger().warning("[Storage:Mongo] Load failed: " + e.getMessage());
            return new YamlConfiguration();
        }
    }

    @Override
    public void save(@NotNull PlayerDataScope scope, @NotNull String groupName, @NotNull UUID playerUuid, @NotNull String playerName, @NotNull YamlConfiguration config) {
        try {
            String yaml = config.saveToString();
            long now = System.currentTimeMillis();
            String lower = playerName.toLowerCase(Locale.ROOT);

            collection.updateOne(
                    Filters.and(
                            Filters.eq("scope", scope.name()),
                            Filters.eq("group", groupName),
                            Filters.eq("uuid", playerUuid.toString())
                    ),
                    Updates.combine(
                            Updates.set("name", playerName),
                            Updates.set("nameLower", lower),
                            Updates.set("yaml", yaml),
                            Updates.set("updatedAt", now)
                    ),
                    new UpdateOptions().upsert(true)
            );
        } catch (Exception e) {
            plugin.getLogger().warning("[Storage:Mongo] Save failed: " + e.getMessage());
        }
    }

    @Override
    public @Nullable UUID resolveUuidByName(@NotNull PlayerDataScope scope, @NotNull String groupName, @NotNull String playerName) {
        try {
            String lower = playerName.toLowerCase(Locale.ROOT);
            Document doc = collection.find(Filters.and(
                    Filters.eq("scope", scope.name()),
                    Filters.eq("group", groupName),
                    Filters.eq("nameLower", lower)
            )).sort(Sorts.descending("updatedAt")).first();

            if (doc == null) {
                return null;
            }
            String uuid = doc.getString("uuid");
            return uuid != null ? UUID.fromString(uuid) : null;
        } catch (Exception e) {
            plugin.getLogger().warning("[Storage:Mongo] resolveUuidByName failed: " + e.getMessage());
            return null;
        }
    }

    @Override
    public void forEachEntry(@NotNull PlayerDataScope scope, @NotNull PlayerDataEntryConsumer consumer) {
        try {
            for (Document doc : collection.find(Filters.eq("scope", scope.name()))
                    .projection(new Document("group", 1)
                            .append("uuid", 1)
                            .append("name", 1)
                            .append("_id", 0))) {

                String group = doc.getString("group");
                String uuidStr = doc.getString("uuid");
                String name = doc.getString("name");
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
        } catch (Exception e) {
            plugin.getLogger().warning("[Storage:Mongo] forEachEntry failed: " + e.getMessage());
        }
    }

    @Override
    public void close() {
        try {
            client.close();
        } catch (Throwable ignored) {
        }
    }
}
