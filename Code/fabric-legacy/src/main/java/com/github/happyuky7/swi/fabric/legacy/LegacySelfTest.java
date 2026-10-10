package com.github.happyuky7.swi.fabric.legacy;

import com.github.happyuky7.swi.api.context.GroupContext;
import com.github.happyuky7.swi.api.data.DataNode;
import com.github.happyuky7.swi.common.config.SwiSettings;
import com.github.happyuky7.swi.common.event.SimpleEventBus;
import com.github.happyuky7.swi.common.section.SimpleSectionRegistry;
import com.github.happyuky7.swi.common.storage.YamlFileStore;
import com.github.happyuky7.swi.common.switching.PlayerAdapter;
import com.github.happyuky7.swi.common.switching.SwitchEngine;
import com.github.happyuky7.swi.common.yaml.YamlCodec;
import com.mojang.authlib.GameProfile;
import net.minecraft.block.Blocks;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.NetworkSide;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerInteractionManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Same in-game test as the modern mod, run with {@code -Dsepareworlditems.selftest=true}.
 */
final class LegacySelfTest {

    static final String PROPERTY = "separeworlditems.selftest";

    private LegacySelfTest() {
    }

    /** Bundled language files and a backup zip. */
    private static void checkServices(SwiLegacyFabric mod, List<String> failures) throws Exception {
        String reload = mod.messages().get("mod.reload");
        if ("mod.reload".equals(reload)) failures.add("language " + mod.messages().lang() + " has no mod.reload message");
        // Configured backend: proves the jar-in-jar drivers load.
        UUID uuid = UUID.randomUUID();
        DataNode data = DataNode.create();
        data.set("exp-level", 42);
        mod.store().save(GroupContext.world("swi-selftest"), uuid, "SwiSelfTest", data);
        if (mod.store().load(GroupContext.world("swi-selftest"), uuid).getInt("exp-level", 0) != 42) {
            failures.add("storage " + mod.store().type() + " round trip");
        }
        mod.platform().logger().info("[SelfTest] storage " + mod.store().type() + " round trip done");
        if (mod.backups() == null) {
            failures.add("backup service not started");
            return;
        }
        File zip = mod.backups().createBackup();
        try (java.util.zip.ZipFile z = new java.util.zip.ZipFile(zip)) {
            if (z.getEntry("extra/config.yml") == null) failures.add("backup " + zip.getName() + " has no extra/config.yml");
        }
    }

    static void run(MinecraftServer server, LegacyPlatform platform, SwiLegacyFabric mod) {
        List<String> failures = new ArrayList<>();
        try {
            execute(server, platform, failures);
            checkServices(mod, failures);
        } catch (Throwable t) {
            platform.logger().error("[SelfTest] crashed", t);
            failures.add("crash: " + t);
        }
        if (failures.isEmpty()) {
            platform.logger().info("[SelfTest] PASSED on Minecraft 1.12.2");
        } else {
            for (String f : failures) platform.logger().warn("[SelfTest] FAIL " + f);
            platform.logger().warn("[SelfTest] FAILED (" + failures.size() + ") on Minecraft 1.12.2");
        }
        server.stopRunning();
    }

    private static void execute(MinecraftServer server, LegacyPlatform platform, List<String> failures) throws Exception {
        ServerWorld world = server.worlds[0];
        ServerPlayerEntity p = new ServerPlayerEntity(server, world, new GameProfile(UUID.randomUUID(), "SelfTest"),
                new ServerPlayerInteractionManager(world));
        new ServerPlayNetworkHandler(server, new ClientConnection(NetworkSide.SERVERBOUND), p);
        PlayerInventory inv = p.inventory;

        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        sword.setDamage(42);
        inv.setInvStack(0, sword);
        inv.setInvStack(5, new ItemStack(Blocks.LOG, 32));
        inv.setInvStack(39, new ItemStack(Items.DIAMOND_HELMET));
        inv.setInvStack(40, new ItemStack(Items.SHIELD));
        p.getEnderChestInventory().setInvStack(3, new ItemStack(Items.GOLDEN_APPLE, 5));
        p.experienceLevel = 7;
        p.getHungerManager().setFoodLevel(11);
        p.addStatusEffect(new StatusEffectInstance(StatusEffect.REGISTRY.get(new Identifier("minecraft:speed")), 1200, 1));

        String[] expected = {
                LegacySections.encode(inv.getInvStack(0)), LegacySections.encode(inv.getInvStack(5)),
                LegacySections.encode(inv.getInvStack(39)), LegacySections.encode(inv.getInvStack(40)),
                LegacySections.encode(p.getEnderChestInventory().getInvStack(3))
        };

        File folder = new File(server.getRunDirectory(), "swi-selftest-" + System.currentTimeMillis());
        YamlFileStore store = new YamlFileStore(folder, platform.logger());
        SwiSettings settings = new SwiSettings(YamlCodec.parse("groups: [a, b]\n"));
        PlayerAdapter<ServerPlayerEntity> alwaysOnline = new PlayerAdapter<ServerPlayerEntity>() {
            public Class<ServerPlayerEntity> playerType() { return ServerPlayerEntity.class; }
            public UUID uuid(ServerPlayerEntity player) { return player.getUuid(); }
            public String name(ServerPlayerEntity player) { return "SelfTest"; }
            public boolean isOnline(ServerPlayerEntity player) { return true; }
        };
        SwitchEngine<ServerPlayerEntity> engine = new SwitchEngine<>(platform, alwaysOnline, () -> settings, () -> store,
                new SimpleEventBus(platform.logger()), new SimpleSectionRegistry(LegacySections.TOGGLES),
                LegacySections.create(() -> settings), LegacySections::stamp);

        engine.onJoin(p, GroupContext.world("a"));
        engine.switchTo(p, GroupContext.world("b"), null).get(10, TimeUnit.SECONDS);
        check(failures, inv.getInvStack(0).isEmpty() && inv.getInvStack(39).isEmpty(), "inventory empty in group b");
        check(failures, p.experienceLevel == 0, "xp reset in group b");
        check(failures, p.getStatusEffectInstances().isEmpty(), "effects cleared in group b");

        DataNode saved = store.load(GroupContext.world("a"), p.getUuid());
        platform.logger().info("[SelfTest] stored record:\n" + YamlCodec.dump(saved));

        inv.setInvStack(1, new ItemStack(Blocks.DIRT, 3));
        engine.switchTo(p, GroupContext.world("a"), null).get(10, TimeUnit.SECONDS);
        String[] actual = {
                LegacySections.encode(inv.getInvStack(0)), LegacySections.encode(inv.getInvStack(5)),
                LegacySections.encode(inv.getInvStack(39)), LegacySections.encode(inv.getInvStack(40)),
                LegacySections.encode(p.getEnderChestInventory().getInvStack(3))
        };
        String[] names = {"slot 0 (damaged sword)", "slot 5 (32 logs)", "helmet", "off-hand", "ender chest slot 3"};
        for (int i = 0; i < expected.length; i++) {
            check(failures, expected[i] != null && expected[i].equals(actual[i]), names[i] + ": expected " + expected[i] + " got " + actual[i]);
        }
        check(failures, inv.getInvStack(1).isEmpty(), "dirt from group b did not leak into group a");
        check(failures, p.experienceLevel == 7, "xp level restored (" + p.experienceLevel + ")");
        check(failures, p.getHungerManager().getFoodLevel() == 11, "food restored");
        check(failures, p.getStatusEffectInstances().size() == 1, "speed effect restored");
    }

    private static void check(List<String> failures, boolean ok, String what) {
        if (!ok) failures.add(what);
    }
}
