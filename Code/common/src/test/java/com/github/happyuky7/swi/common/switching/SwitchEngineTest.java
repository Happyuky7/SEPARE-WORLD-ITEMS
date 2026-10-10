package com.github.happyuky7.swi.common.switching;

import com.github.happyuky7.swi.api.Platform;
import com.github.happyuky7.swi.api.context.GroupContext;
import com.github.happyuky7.swi.api.data.DataNode;
import com.github.happyuky7.swi.api.event.GroupSwitchPostEvent;
import com.github.happyuky7.swi.api.event.GroupSwitchPreEvent;
import com.github.happyuky7.swi.api.section.DataSection;
import com.github.happyuky7.swi.common.config.SwiSettings;
import com.github.happyuky7.swi.common.event.SimpleEventBus;
import com.github.happyuky7.swi.common.platform.SwiLogger;
import com.github.happyuky7.swi.common.platform.SwiPlatform;
import com.github.happyuky7.swi.common.section.SimpleSectionRegistry;
import com.github.happyuky7.swi.common.storage.JdbcStore;
import com.github.happyuky7.swi.common.storage.PlayerDataStore;
import com.github.happyuky7.swi.common.storage.StorageType;
import com.github.happyuky7.swi.common.storage.YamlFileStore;
import com.github.happyuky7.swi.common.yaml.YamlCodec;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SwitchEngineTest {

    /** Fake player: an "inventory" string and a level. */
    static final class FakePlayer {
        final UUID uuid = UUID.randomUUID();
        String inventory = "";
        int level;
        double mana;
    }

    static final SectionHandler<FakePlayer> INVENTORY = new SectionHandler<FakePlayer>() {
        public String toggleKey() { return "inventory"; }
        public void save(FakePlayer p, DataNode r) { r.set("inventory.0", p.inventory); }
        public void load(FakePlayer p, DataNode r) { p.inventory = r.getString("inventory.0", ""); }
        public void clear(FakePlayer p) { p.inventory = ""; }
    };

    static final SectionHandler<FakePlayer> EXP = new SectionHandler<FakePlayer>() {
        public String toggleKey() { return "exp"; }
        public void save(FakePlayer p, DataNode r) { r.set("exp-level", p.level); }
        public void load(FakePlayer p, DataNode r) { p.level = r.getInt("exp-level", 0); }
        public void clear(FakePlayer p) { p.level = 0; }
    };

    static final SwiLogger LOG = new SwiLogger() {
        public void info(String m) { }
        public void warn(String m) { System.out.println("WARN " + m); }
        public void error(String m, Throwable t) { System.out.println("ERROR " + m); t.printStackTrace(); }
        public void debug(String m) { }
    };

    private SimpleEventBus bus;
    private SimpleSectionRegistry registry;

    private SwitchEngine<FakePlayer> engine(File folder, PlayerDataStore store, String yaml) {
        Executor direct = Runnable::run;
        SwiPlatform platform = new SwiPlatform() {
            public Platform platform() { return Platform.FABRIC; }
            public String minecraftVersion() { return "test"; }
            public String modVersion() { return "test"; }
            public File dataFolder() { return folder; }
            public SwiLogger logger() { return LOG; }
            public Executor asyncExecutor() { return direct; }
            public Executor syncExecutor() { return direct; }
        };
        PlayerAdapter<FakePlayer> adapter = new PlayerAdapter<FakePlayer>() {
            public Class<FakePlayer> playerType() { return FakePlayer.class; }
            public UUID uuid(FakePlayer p) { return p.uuid; }
            public String name(FakePlayer p) { return "Steve"; }
            public boolean isOnline(FakePlayer p) { return true; }
        };
        SwiSettings settings = new SwiSettings(YamlCodec.parse(yaml));
        bus = new SimpleEventBus(LOG);
        registry = new SimpleSectionRegistry(java.util.Arrays.asList("inventory", "exp"));
        List<SectionHandler<FakePlayer>> builtins = new ArrayList<>();
        Collections.addAll(builtins, INVENTORY, EXP);
        return new SwitchEngine<>(platform, adapter, () -> settings, () -> store, bus, registry, builtins,
                r -> r.set("swi.format", "test"));
    }

    private static final String CONFIG = "settings:\n  options:\n    saves:\n      inventory: true\n      exp: true\n"
            + "    saves-per-group:\n      creative:\n        exp: false\n"
            + "groups: [survival, creative]\n";

    private static void await(java.util.concurrent.CompletableFuture<Void> f) throws Exception {
        f.get(5, TimeUnit.SECONDS);
    }

    @Test
    void separatesDataBetweenGroupsWithYaml(@TempDir File dir) throws Exception {
        roundTrip(dir, new YamlFileStore(dir, LOG));
        assertTrue(new File(dir, "groups/survival").isDirectory());
    }

    @Test
    void separatesDataBetweenGroupsWithSqlite(@TempDir File dir) throws Exception {
        roundTrip(dir, new JdbcStore(StorageType.SQLITE, "jdbc:sqlite:" + new File(dir, "t.sqlite").getAbsolutePath(), null, null, LOG));
    }

    private void roundTrip(File dir, PlayerDataStore store) throws Exception {
        SwitchEngine<FakePlayer> engine = engine(dir, store, CONFIG);
        FakePlayer p = new FakePlayer();
        GroupContext survival = GroupContext.world("survival");
        GroupContext creative = GroupContext.world("creative");

        engine.onJoin(p, survival);
        p.inventory = "diamond_sword";
        p.level = 30;

        await(engine.switchTo(p, creative, null));
        assertEquals("", p.inventory, "creative starts empty");
        assertEquals(0, p.level);

        p.inventory = "bedrock";
        p.level = 99; // exp disabled for creative: never stored for that group
        await(engine.switchTo(p, survival, null));
        assertEquals("diamond_sword", p.inventory);
        assertEquals(30, p.level);

        await(engine.switchTo(p, creative, null));
        assertEquals("bedrock", p.inventory);
        assertEquals(0, p.level, "exp is not separated for creative, so it is reset");

        DataNode stored = store.load(survival, p.uuid);
        assertEquals("test", stored.getString("swi.format"));
        assertEquals(p.uuid, store.resolveUuidByName(survival, "steve"));
        store.close();
    }

    @Test
    void cancelledPreEventKeepsState(@TempDir File dir) throws Exception {
        SwitchEngine<FakePlayer> engine = engine(dir, new YamlFileStore(dir, LOG), CONFIG);
        AtomicInteger posts = new AtomicInteger();
        bus.subscribe(GroupSwitchPreEvent.class, e -> e.setCancelled(true));
        bus.subscribe(GroupSwitchPostEvent.class, e -> posts.incrementAndGet());
        FakePlayer p = new FakePlayer();
        engine.onJoin(p, GroupContext.world("survival"));
        p.inventory = "apple";

        await(engine.switchTo(p, GroupContext.world("creative"), null));
        assertEquals("apple", p.inventory);
        assertEquals(GroupContext.world("survival"), engine.tracker().get(p.uuid));
        assertEquals(0, posts.get());
        assertFalse(new File(dir, "groups").exists());
    }

    @Test
    void customSectionsAreSavedUnderTheirOwnNode(@TempDir File dir) throws Exception {
        YamlFileStore store = new YamlFileStore(dir, LOG);
        SwitchEngine<FakePlayer> engine = engine(dir, store, CONFIG);
        registry.register(new DataSection<FakePlayer>() {
            public @NotNull String id() { return "magic:mana"; }
            public @NotNull Class<FakePlayer> playerType() { return FakePlayer.class; }
            public void save(@NotNull FakePlayer p, @NotNull DataNode out) { out.set("value", p.mana); }
            public void load(@NotNull FakePlayer p, @NotNull DataNode in) { p.mana = in.getDouble("value", 0); }
            public void clear(@NotNull FakePlayer p) { p.mana = 0; }
        });
        FakePlayer p = new FakePlayer();
        engine.onJoin(p, GroupContext.world("survival"));
        p.mana = 42.5;

        await(engine.switchTo(p, GroupContext.world("creative"), null));
        assertEquals(0, p.mana);
        assertEquals(42.5, store.load(GroupContext.world("survival"), p.uuid).getDouble("custom.magic:mana.value", -1));

        await(engine.switchTo(p, GroupContext.world("survival"), null));
        assertEquals(42.5, p.mana);
    }
}
