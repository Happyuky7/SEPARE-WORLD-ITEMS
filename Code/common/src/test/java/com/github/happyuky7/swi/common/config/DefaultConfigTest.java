package com.github.happyuky7.swi.common.config;

import com.github.happyuky7.swi.api.data.DataNode;
import com.github.happyuky7.swi.common.yaml.YamlCodec;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The default configs shipped with the plugin and the mods must parse, be version 3.0.0 and contain
 * every key the code reads (so editing comments can never drop a setting).
 */
class DefaultConfigTest {

    @Test
    void bundledModConfigIsValid() throws Exception {
        DataNode raw = YamlCodec.read(new File("../fabric/src/main/resources/separeworlditems/config.yml"));
        SwiSettings settings = new SwiSettings(raw);

        assertEquals("3.0.0", raw.getString("config-version"));
        assertTrue(settings.validate().isEmpty(), "config problems: " + settings.validate());
        assertEquals("default", settings.groupOfWorld("minecraft:overworld"));
        assertTrue(settings.isSaveEnabled("default", "curios", false));
        assertEquals("VANILLA", settings.healthType());
        assertFalse(raw.getBoolean("regions.enabled", true));
        assertEquals(10, raw.getInt("regions.check-interval-ticks", 0));
        for (String key : new String[]{"settings.debug", "settings.auto-configure-worlds", "settings.options.default-group.enabled",
                "settings.options.default-group.group", "settings.options.saves-per-group", "storage.type", "storage.sqlite.file",
                "storage.sql.jdbc-url", "storage.sql.host", "storage.sql.port", "storage.sql.database", "storage.sql.username",
                "storage.sql.password", "settings.langs.autoDownload", "settings.langs.lang", "settings.backups.enabled",
                "settings.backups.interval", "settings.backups.interval-type", "settings.backups.max-backups",
                "settings.backups.remote-snapshot.enabled", "storage.mongodb.uri", "storage.mongodb.database",
                "storage.mongodb.collection", "storage.redis.uri", "storage.redis.key-prefix", "regions.groups", "regions.yawp", "regions.flan.claims", "regions.flan.owners",
                "regions.flan.admin-claims", "regions.flan.any-claim", "regions.opac.owners", "regions.opac.parties",
                "regions.opac.any-claim", "groups", "worlds"}) {
            assertNotNull(raw.get(key), "missing key " + key);
        }
    }

    @Test
    void bundledPluginConfigHasEveryKeyTheCodeReads() throws Exception {
        DataNode raw = YamlCodec.read(new File("../bukkit/src/main/resources/config.yml"));

        assertEquals("3.0.0", raw.getString("config-version"));
        for (String key : new String[]{
                "settings.debug", "settings.langs.autoDownload", "settings.langs.lang", "settings.auto-configure-worlds",
                "settings.backups.enabled", "settings.backups.interval", "settings.backups.interval-type", "settings.backups.max-backups",
                "settings.backups.remote-snapshot.enabled",
                "settings.change-logs.enabled", "settings.change-logs.retention-days", "settings.change-logs.include-payload",
                "settings.change-logs.capture-all-data", "settings.change-logs.yaml.enabled", "settings.change-logs.sqlite.confirmed",
                "settings.options.default-group.enabled", "settings.options.default-group.group",
                "settings.options.saves.gamemode", "settings.options.saves.flying", "settings.options.saves.fly-speed",
                "settings.options.saves.exp", "settings.options.saves.enderchest", "settings.options.saves.inventory",
                "settings.options.saves.potion-effects", "settings.options.saves.food-level", "settings.options.saves.off-hand",
                "settings.options.saves.health", "settings.options.saves-per-group", "settings.health-options.type",
                "integrations.placeholderapi.enabled", "integrations.auraskills.enabled", "integrations.worldguard.enabled",
                "integrations.essentials.enabled",
                "storage.type", "storage.migrate-from-yaml", "storage.migration.enabled", "storage.migration.from",
                "storage.migration.overwrite", "storage.sqlite.file", "storage.sql.jdbc-url", "storage.sql.host", "storage.sql.port",
                "storage.sql.database", "storage.sql.parameters", "storage.sql.username", "storage.sql.password",
                "storage.sql.pool.maxPoolSize", "storage.mongodb.uri", "storage.mongodb.database", "storage.mongodb.collection",
                "storage.mongodb.logs-collection", "storage.redis.uri", "storage.redis.key-prefix",
                "groups", "worlds", "worldguard-regions.groups", "worldguard-regions.regions",
                "essentialsx.vanish-bypass", "auraskills.save-mana", "metrics.enabled"}) {
            assertNotNull(raw.get(key), "missing key " + key);
        }
        assertEquals("d", raw.getString("settings.backups.interval-type"));
        assertEquals("BUKKIT", raw.getString("settings.health-options.type"));
    }
}
