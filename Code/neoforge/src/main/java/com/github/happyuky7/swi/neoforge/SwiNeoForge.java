package com.github.happyuky7.swi.neoforge;

import com.github.happyuky7.swi.api.Platform;
import com.github.happyuky7.swi.mc.Integrations;
import com.github.happyuky7.swi.mc.SwiCore;
import com.github.happyuky7.swi.mc.VanillaSelfTest;
import com.github.happyuky7.swi.neoforge.integration.NeoForgeIntegrations;
import com.github.happyuky7.swi.neoforge.integration.NeoForgePermissions;
import com.github.happyuky7.swi.mc.compat.Mc;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * NeoForge entry point: forwards NeoForge events to {@link SwiCore}. Runs on dedicated and integrated servers.
 */
@Mod("separeworlditems")
public final class SwiNeoForge {

    private static SwiCore core;

    public static SwiCore core() {
        return core;
    }

    public SwiNeoForge(IEventBus modBus, ModContainer container) {
        core = new SwiCore(Platform.NEOFORGE, "neoforge", "NeoForge", FMLPaths.CONFIGDIR.get().toFile(),
                () -> container.getModInfo().getVersion().toString());

        IEventBus bus = NeoForge.EVENT_BUS;
        NeoForgePermissions.install(core, bus);
        NeoForgeIntegrations.register(core);
        bus.addListener(ServerStartingEvent.class, e -> core.serverStarting(e.getServer()));
        bus.addListener(ServerStartedEvent.class, e -> core.serverStarted(e.getServer(), s -> FakePlayerFactory.getMinecraft(s.overworld())));
        bus.addListener(ServerStoppedEvent.class, e -> core.serverStopped());
        bus.addListener(ServerTickEvent.Post.class, e -> core.onServerTick(e.getServer()));

        bus.addListener(PlayerEvent.PlayerLoggedInEvent.class, e -> {
            if (e.getEntity() instanceof ServerPlayer player) core.onJoin(player);
        });
        bus.addListener(PlayerEvent.PlayerLoggedOutEvent.class, e -> {
            if (e.getEntity() instanceof ServerPlayer player) core.onQuit(player);
        });
        bus.addListener(PlayerEvent.PlayerChangedDimensionEvent.class, e -> {
            if (e.getEntity() instanceof ServerPlayer player) core.onLevelChange(player, Mc.level(player));
        });
        // Dying/leaving the End recreates the player entity, possibly in another dimension.
        bus.addListener(PlayerEvent.PlayerRespawnEvent.class, e -> {
            if (e.getEntity() instanceof ServerPlayer player) core.onRespawn(player);
        });

        bus.addListener(RegisterCommandsEvent.class, e -> core.registerCommands(e.getDispatcher()));
    }
}
