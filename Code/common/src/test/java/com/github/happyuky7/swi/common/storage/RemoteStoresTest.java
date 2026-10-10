package com.github.happyuky7.swi.common.storage;

import com.github.happyuky7.swi.api.context.ContextType;
import com.github.happyuky7.swi.api.context.GroupContext;
import com.github.happyuky7.swi.api.data.DataNode;
import com.github.happyuky7.swi.common.platform.SwiLogger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Runs only against real servers, e.g.
 * {@code docker run -d -p 27017:27017 mongo} + {@code SWI_TEST_MONGO_URI=mongodb://localhost:27017} and
 * {@code docker run -d -p 6379:6379 redis} + {@code SWI_TEST_REDIS_URI=redis://localhost:6379}.
 */
class RemoteStoresTest {

    private static final SwiLogger LOG = new SwiLogger() {
        public void info(String m) { }
        public void warn(String m) { }
        public void error(String m, Throwable t) { }
        public void debug(String m) { }
    };

    @Test
    @EnabledIfEnvironmentVariable(named = "SWI_TEST_MONGO_URI", matches = ".+")
    void mongo() {
        String run = "swi_test_" + System.nanoTime();
        roundTrip(new MongoStore(System.getenv("SWI_TEST_MONGO_URI"), run, "player_data", LOG));
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "SWI_TEST_REDIS_URI", matches = ".+")
    void redis() {
        roundTrip(new RedisStore(System.getenv("SWI_TEST_REDIS_URI"), "switest" + System.nanoTime(), LOG));
    }

    private static void roundTrip(PlayerDataStore store) {
        try {
            UUID uuid = UUID.randomUUID();
            GroupContext world = GroupContext.world("default");
            GroupContext region = GroupContext.region("arena:pvp");
            assertFalse(store.exists(world, uuid));

            DataNode data = DataNode.create();
            data.set("exp-level", 7);
            store.save(world, uuid, "Alex", data);
            store.save(region, uuid, "Alex", data);

            assertTrue(store.exists(world, uuid));
            assertEquals(7, store.load(world, uuid).getInt("exp-level", 0));
            assertEquals(uuid, store.resolveUuidByName(world, "alex"));
            Map<String, UUID> regions = new HashMap<>();
            store.forEachEntry(ContextType.REGION, (group, id, name) -> regions.put(group, id));
            assertEquals(uuid, regions.get("arena:pvp"), "group names with ':' survive the key scan");
        } finally {
            store.close();
        }
    }
}
