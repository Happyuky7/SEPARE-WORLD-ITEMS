package com.github.happyuky7.swi.fabric;

import com.github.happyuky7.swi.api.Platform;
import com.github.happyuky7.swi.fabric.integration.FabricIntegrations;
import com.github.happyuky7.swi.mc.SwiCore;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
//#if MC >= 26.1
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityLevelChangeEvents;
//#else
//$$ import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
//#endif
//#if MC >= 1.19
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
//#else
//$$ import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
//#endif

/**
 * Fabric entry point: forwards Fabric API events to {@link SwiCore}. Runs on dedicated and integrated servers.
 */
public final class SwiFabric implements ModInitializer {

    private static SwiCore core;

    public static SwiCore core() {
        return core;
    }

    @Override
    public void onInitialize() {
        core = new SwiCore(Platform.FABRIC, "fabric", "Fabric",
                FabricLoader.getInstance().getConfigDir().toFile(),
                () -> FabricLoader.getInstance().getModContainer("separeworlditems")
                        .map(c -> c.getMetadata().getVersion().getFriendlyString()).orElse("unknown"));

        FabricIntegrations.register(core);

        ServerLifecycleEvents.SERVER_STARTING.register(core::serverStarting);
        ServerLifecycleEvents.SERVER_STARTED.register(server -> core.serverStarted(server, SelfTest::testPlayer));
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> core.serverStopped());
        ServerTickEvents.END_SERVER_TICK.register(core::onServerTick);

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> core.onJoin(handler.player));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> core.onQuit(handler.player));

        //#if MC >= 26.1
        ServerEntityLevelChangeEvents.AFTER_PLAYER_CHANGE_LEVEL.register((player, origin, destination) -> core.onLevelChange(player, destination));
        //#else
        //$$ ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register((player, origin, destination) -> core.onLevelChange(player, destination));
        //#endif
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> core.onRespawn(newPlayer));

        //#if MC >= 1.19
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> core.registerCommands(dispatcher));
        //#else
        //$$ CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> core.registerCommands(dispatcher));
        //#endif
    }
}
