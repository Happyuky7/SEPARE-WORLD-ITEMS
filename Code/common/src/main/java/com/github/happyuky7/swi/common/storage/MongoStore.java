package com.github.happyuky7.swi.common.storage;

import com.github.happyuky7.swi.api.context.ContextType;
import com.github.happyuky7.swi.api.context.GroupContext;
import com.github.happyuky7.swi.api.data.DataNode;
import com.github.happyuky7.swi.common.platform.SwiLogger;
import com.github.happyuky7.swi.common.yaml.YamlCodec;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Updates;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.Locale;
import java.util.UUID;

/**
 * MongoDB store. Same documents as the Bukkit plugin ({@code scope, group, uuid, name, nameLower, yaml, updatedAt}),
 * so both platforms can read each other's records.
 */
public final class MongoStore implements PlayerDataStore {

    private final MongoClient client;
    private final MongoCollection<Document> collection;
    private final SwiLogger logger;

    public MongoStore(String uri, String database, String collectionName, SwiLogger logger) {
        this.logger = logger;
        this.client = MongoClients.create(uri);
        this.collection = client.getDatabase(database).getCollection(collectionName);
        try {
            collection.createIndex(Indexes.ascending("scope", "group", "uuid"));
            collection.createIndex(Indexes.ascending("scope", "group", "nameLower"));
        } catch (RuntimeException e) {
            logger.warn("[Storage:MongoDB] Could not create indexes: " + e.getMessage());
        }
    }

    @Override
    public StorageType type() {
        return StorageType.MONGODB;
    }

    private static Bson key(GroupContext context, UUID uuid) {
        return Filters.and(
                Filters.eq("scope", PlayerDataStore.legacyScopeName(context.type())),
                Filters.eq("group", context.group()),
                Filters.eq("uuid", uuid.toString()));
    }

    @Override
    public boolean exists(GroupContext context, UUID uuid) {
        try {
            return collection.find(key(context, uuid)).projection(new Document("_id", 1)).first() != null;
        } catch (RuntimeException e) {
            throw new StorageException("[Storage:MongoDB] " + e.getMessage(), e);
        }
    }

    @Override
    public DataNode load(GroupContext context, UUID uuid) {
        Document doc;
        try {
            doc = collection.find(key(context, uuid)).first();
        } catch (RuntimeException e) {
            throw new StorageException("[Storage:MongoDB] " + e.getMessage(), e);
        }
        if (doc == null) {
            return DataNode.create();
        }
        try {
            return YamlCodec.parse(doc.getString("yaml"));
        } catch (RuntimeException e) {
            logger.error("[Storage:MongoDB] Corrupted record " + context + "/" + uuid + ", using empty data", e);
            return DataNode.create();
        }
    }

    @Override
    public void save(GroupContext context, UUID uuid, String playerName, DataNode data) {
        try {
            collection.updateOne(key(context, uuid), Updates.combine(
                    Updates.set("name", playerName),
                    Updates.set("nameLower", playerName.toLowerCase(Locale.ROOT)),
                    Updates.set("yaml", YamlCodec.dump(data)),
                    Updates.set("updatedAt", System.currentTimeMillis())
            ), new UpdateOptions().upsert(true));
        } catch (RuntimeException e) {
            throw new StorageException("[Storage:MongoDB] " + e.getMessage(), e);
        }
    }

    @Override
    public UUID resolveUuidByName(GroupContext context, String playerName) {
        try {
            Document doc = collection.find(Filters.and(
                    Filters.eq("scope", PlayerDataStore.legacyScopeName(context.type())),
                    Filters.eq("group", context.group()),
                    Filters.eq("nameLower", playerName.toLowerCase(Locale.ROOT))
            )).sort(Sorts.descending("updatedAt")).first();
            return doc != null && doc.getString("uuid") != null ? UUID.fromString(doc.getString("uuid")) : null;
        } catch (IllegalArgumentException e) {
            return null;
        } catch (RuntimeException e) {
            throw new StorageException("[Storage:MongoDB] " + e.getMessage(), e);
        }
    }

    @Override
    public void forEachEntry(ContextType type, EntryConsumer consumer) {
        for (Document doc : collection.find(Filters.eq("scope", PlayerDataStore.legacyScopeName(type)))
                .projection(new Document("group", 1).append("uuid", 1).append("name", 1).append("_id", 0))) {
            try {
                consumer.accept(doc.getString("group"), UUID.fromString(doc.getString("uuid")), doc.getString("name"));
            } catch (IllegalArgumentException | NullPointerException ignored) {
                // skip malformed documents
            }
        }
    }

    @Override
    public void close() {
        client.close();
    }
}
