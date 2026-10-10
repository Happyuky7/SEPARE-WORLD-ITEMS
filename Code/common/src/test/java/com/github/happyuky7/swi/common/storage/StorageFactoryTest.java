package com.github.happyuky7.swi.common.storage;

import com.github.happyuky7.swi.api.context.GroupContext;
import com.github.happyuky7.swi.api.data.DataNode;
import com.github.happyuky7.swi.common.config.SwiSettings;
import com.github.happyuky7.swi.common.platform.SwiLogger;
import com.github.happyuky7.swi.common.yaml.YamlCodec;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StorageFactoryTest {

    private static final SwiLogger LOG = new SwiLogger() {
        public void info(String m) { }
        public void warn(String m) { }
        public void error(String m, Throwable t) { }
        public void debug(String m) { }
    };

    @Test
    void sqliteCreatesMissingDataFolder(@TempDir File dir) {
        File dataFolder = new File(dir, "world/separeworlditems");
        SwiSettings settings = new SwiSettings(YamlCodec.parse("storage:\n  type: SQLITE\n"));
        PlayerDataStore store = StorageFactory.create(settings, dataFolder, LOG);
        try {
            UUID uuid = UUID.randomUUID();
            DataNode data = DataNode.create();
            data.set("exp-level", 5);
            store.save(GroupContext.world("default"), uuid, "Alex", data);
            assertEquals(5, store.load(GroupContext.world("default"), uuid).getInt("exp-level", 0));
            assertTrue(new File(dataFolder, "separeworlditems.sqlite").isFile());
        } finally {
            store.close();
        }
    }

    @Test
    void buildsMysqlUrlFromHostSettings() {
        DataNode cfg = YamlCodec.parse("storage:\n  sql:\n    host: db.local\n    port: 3307\n    database: swi\n    parameters: useSSL=false\n");
        assertEquals("jdbc:mariadb://db.local:3307/swi?useSSL=false", StorageFactory.sqlUrl(StorageType.MARIADB, cfg));
    }
}
