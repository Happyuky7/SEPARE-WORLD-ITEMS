package com.github.happyuky7.swi.mc;

import com.github.happyuky7.swi.api.context.GroupContext;
import com.github.happyuky7.swi.api.data.DataNode;
import com.github.happyuky7.swi.common.config.SwiSettings;
import com.github.happyuky7.swi.common.event.SimpleEventBus;
import com.github.happyuky7.swi.common.platform.SwiPlatform;
import com.github.happyuky7.swi.common.section.SimpleSectionRegistry;
import com.github.happyuky7.swi.common.storage.YamlFileStore;
import com.github.happyuky7.swi.common.switching.PlayerAdapter;
import com.github.happyuky7.swi.common.switching.SwitchEngine;
import com.github.happyuky7.swi.common.yaml.YamlCodec;
import com.github.happyuky7.swi.mc.compat.Effects;
import com.github.happyuky7.swi.mc.compat.ItemCodec;
import com.github.happyuky7.swi.mc.compat.Mc;
import com.github.happyuky7.swi.mc.section.VanillaSections;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * In-game integration test shared by every mod loader, run with {@code -Dsepareworlditems.selftest=true}:
 * switches a test player between two groups with the real sections and storage, checks everything came back,
 * logs {@code [SelfTest] PASSED/FAILED} and stops the server. Each loader only provides the test player.
 */
public final class VanillaSelfTest {

    public static final String PROPERTY = "separeworlditems.selftest";

    private VanillaSelfTest() {
    }

    /**
     * Extra checks contributed by mod integrations (Curios, Trinkets...).
     */
    public interface Hook {
        String name();

        /** Put integration items on the player before the first switch. */
        void prepare(ServerPlayer player, List<String> failures);

        /** Called in the other group: the integration items must be gone. */
        void verifyCleared(ServerPlayer player, List<String> failures);

        /** Called back in the first group: the integration items must be back. */
        void verifyRestored(ServerPlayer player, List<String> failures);
    }

    private static final List<Hook> HOOKS = new java.util.concurrent.CopyOnWriteArrayList<>();

    public static void addHook(Hook hook) {
        HOOKS.add(hook);
    }

    public static boolean enabled() {
        return Boolean.getBoolean(PROPERTY);
    }

    /**
     * Items of other mods to round-trip, from {@code -Dsepareworlditems.selftest.items=create:backtank,create:wrench}:
     * each one is put in the main inventory (slots 9+) and must come back identical.
     */
    private static final class ModItemsHook implements Hook {
        private final String[] ids;
        private final java.util.Map<Integer, String> expected = new java.util.LinkedHashMap<>();

        ModItemsHook(String[] ids) {
            this.ids = ids;
        }

        public String name() {
            return "mod items " + java.util.Arrays.toString(ids);
        }

        public void prepare(ServerPlayer p, List<String> failures) {
            int slot = 9;
            for (String id : ids) {
                ItemStack stack = Mc.stackOf(id.trim(), 1);
                if (stack == null) {
                    failures.add("mod item " + id + " is not registered");
                    continue;
                }
                Mc.inventory(p).setItem(slot, stack);
                expected.put(slot++, ItemCodec.encode(p, stack));
            }
        }

        public void verifyCleared(ServerPlayer p, List<String> failures) {
            for (Integer slot : expected.keySet()) {
                if (!Mc.inventory(p).getItem(slot).isEmpty()) failures.add("mod item in slot " + slot + " not cleared");
            }
        }

        public void verifyRestored(ServerPlayer p, List<String> failures) {
            for (java.util.Map.Entry<Integer, String> e : expected.entrySet()) {
                String now = ItemCodec.encode(p, Mc.inventory(p).getItem(e.getKey()));
                if (!e.getValue().equals(now)) failures.add("mod item changed: expected " + e.getValue() + " got " + now);
            }
        }
    }

    public static void run(MinecraftServer server, SwiPlatform platform, ServerPlayer testPlayer, String platformId) {
        List<String> failures = new ArrayList<>();
        try {
            execute(server, platform, testPlayer, platformId, failures);
        } catch (Throwable t) {
            platform.logger().error("[SelfTest] crashed", t);
            failures.add("crash: " + t);
        }
        if (failures.isEmpty()) {
            platform.logger().info("[SelfTest] PASSED on Minecraft " + Mc.minecraftVersion() + " (" + platformId + ")");
        } else {
            for (String f : failures) {
                platform.logger().warn("[SelfTest] FAIL " + f);
            }
            platform.logger().warn("[SelfTest] FAILED (" + failures.size() + ") on Minecraft " + Mc.minecraftVersion());
        }
        server.halt(false);
    }

    private static void execute(MinecraftServer server, SwiPlatform platform, ServerPlayer p, String platformId, List<String> failures) throws Exception {
        Inventory inv = Mc.inventory(p);

        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        sword.setDamageValue(42);
        inv.setItem(0, sword);
        inv.setItem(5, new ItemStack(Items.OAK_LOG, 32));
        inv.setItem(39, new ItemStack(Items.DIAMOND_HELMET));
        inv.setItem(40, new ItemStack(Items.SHIELD));
        p.getEnderChestInventory().setItem(3, new ItemStack(Items.GOLDEN_APPLE, 5));
        p.setExperienceLevels(7);
        p.getFoodData().setFoodLevel(11);
        p.addEffect(Effects.create("minecraft:speed", 1200, 1, false, true, true));

        String[] expected = {
                ItemCodec.encode(p, inv.getItem(0)), ItemCodec.encode(p, inv.getItem(5)),
                ItemCodec.encode(p, inv.getItem(39)), ItemCodec.encode(p, inv.getItem(40)),
                ItemCodec.encode(p, p.getEnderChestInventory().getItem(3))
        };
        check(failures, expected[0] != null && expected[0].contains("diamond_sword"), "sword SNBT: " + expected[0]);

        File folder = new File(server.getServerDirectory().toString(), "swi-selftest-" + System.currentTimeMillis());
        YamlFileStore store = new YamlFileStore(folder, platform.logger());
        SwiSettings settings = new SwiSettings(YamlCodec.parse("groups: [a, b]\n"));
        SimpleSectionRegistry sections = new SimpleSectionRegistry(VanillaSections.TOGGLES);
        // Include the sections registered by integrations (Curios, Trinkets...) through the public API.
        for (com.github.happyuky7.swi.api.section.DataSection<?> section : com.github.happyuky7.swi.api.SepareWorldItems.get().sections().all()) {
            sections.register(section);
        }
        String modItems = System.getProperty(PROPERTY + ".items");
        if (modItems != null && !modItems.trim().isEmpty()) {
            HOOKS.add(new ModItemsHook(modItems.split(",")));
        }
        for (Hook hook : HOOKS) {
            hook.prepare(p, failures);
        }
        PlayerAdapter<ServerPlayer> alwaysOnline = new PlayerAdapter<ServerPlayer>() {
            public Class<ServerPlayer> playerType() { return ServerPlayer.class; }
            public UUID uuid(ServerPlayer player) { return player.getUUID(); }
            public String name(ServerPlayer player) { return "SelfTest"; }
            public boolean isOnline(ServerPlayer player) { return true; }
        };
        SwitchEngine<ServerPlayer> engine = new SwitchEngine<>(platform, alwaysOnline, () -> settings, () -> store,
                new SimpleEventBus(platform.logger()), sections, VanillaSections.create(() -> settings), VanillaSections.stamper(platformId));

        engine.onJoin(p, GroupContext.world("a"));
        engine.switchTo(p, GroupContext.world("b"), null).get(10, TimeUnit.SECONDS);
        check(failures, inv.isEmpty(), "inventory empty in group b");
        check(failures, p.getEnderChestInventory().isEmpty(), "ender chest empty in group b");
        check(failures, p.experienceLevel == 0, "xp reset in group b (" + p.experienceLevel + ")");
        check(failures, p.getActiveEffects().isEmpty(), "effects cleared in group b");
        for (Hook hook : HOOKS) {
            hook.verifyCleared(p, failures);
        }

        DataNode saved = store.load(GroupContext.world("a"), p.getUUID());
        platform.logger().info("[SelfTest] stored record:\n" + YamlCodec.dump(saved));

        inv.setItem(1, new ItemStack(Items.DIRT, 3));
        engine.switchTo(p, GroupContext.world("a"), null).get(10, TimeUnit.SECONDS);
        String[] actual = {
                ItemCodec.encode(p, inv.getItem(0)), ItemCodec.encode(p, inv.getItem(5)),
                ItemCodec.encode(p, inv.getItem(39)), ItemCodec.encode(p, inv.getItem(40)),
                ItemCodec.encode(p, p.getEnderChestInventory().getItem(3))
        };
        String[] names = {"slot 0 (damaged sword)", "slot 5 (32 logs)", "helmet", "off-hand", "ender chest slot 3"};
        for (int i = 0; i < expected.length; i++) {
            check(failures, expected[i] != null && expected[i].equals(actual[i]), names[i] + ": expected " + expected[i] + " got " + actual[i]);
        }
        check(failures, inv.getItem(1).isEmpty(), "dirt from group b did not leak into group a");
        check(failures, p.experienceLevel == 7, "xp level restored (" + p.experienceLevel + ")");
        check(failures, p.getFoodData().getFoodLevel() == 11, "food restored (" + p.getFoodData().getFoodLevel() + ")");
        check(failures, p.getActiveEffects().size() == 1, "speed effect restored");
        for (Hook hook : HOOKS) {
            hook.verifyRestored(p, failures);
            platform.logger().info("[SelfTest] integration checked: " + hook.name());
        }

        engine.switchTo(p, GroupContext.world("b"), null).get(10, TimeUnit.SECONDS);
        check(failures, inv.getItem(1).getCount() == 3, "group b kept its dirt");
    }

    private static void check(List<String> failures, boolean ok, String what) {
        if (!ok) {
            failures.add(what);
        }
    }
}
