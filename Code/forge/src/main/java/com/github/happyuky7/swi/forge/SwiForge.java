package com.github.happyuky7.swi.forge;

import com.github.happyuky7.swi.api.Platform;
import com.github.happyuky7.swi.mc.SwiCommands;
import com.github.happyuky7.swi.mc.SwiCore;
import com.github.happyuky7.swi.mc.TestPlayers;
import com.github.happyuky7.swi.mc.compat.Mc;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.server.permission.PermissionAPI;
import net.minecraftforge.server.permission.events.PermissionGatherEvent;
import net.minecraftforge.server.permission.nodes.PermissionNode;
import net.minecraftforge.server.permission.nodes.PermissionTypes;
//#if MC < 1.21.6
//$$ import net.minecraftforge.common.MinecraftForge;
//#endif

/**
 * MinecraftForge entry point: forwards Forge events to {@link SwiCore}. Runs on dedicated and integrated servers.
 * Forge 1.21.6+ uses EventBus 7 (one {@code BUS} per event); 1.21.1 uses the classic {@code MinecraftForge.EVENT_BUS}.
 */
@Mod("separeworlditems")
public final class SwiForge {

    /** {@code separeworlditems.admin} in Forge's PermissionAPI (LuckPerms...). Default: operator level 2. */
    public static final PermissionNode<Boolean> ADMIN = new PermissionNode<>("separeworlditems", "admin", PermissionTypes.BOOLEAN,
            (player, uuid, context) -> player != null && SwiCommands.vanillaAdmin(player.createCommandSourceStack()));

    private static SwiCore core;

    public static SwiCore core() {
        return core;
    }

    private static String modVersion() {
        //#if MC >= 26.1
        return ModList.getModContainerById("separeworlditems")
        //#else
        //$$ return ModList.get().getModContainerById("separeworlditems")
        //#endif
                .map(c -> c.getModInfo().getVersion().toString()).orElse("unknown");
    }

    public SwiForge(FMLJavaModLoadingContext context) {
        core = new SwiCore(Platform.FORGE, "forge", "Forge", FMLPaths.CONFIGDIR.get().toFile(), SwiForge::modVersion);
        core.setAdminCheck(source -> source.getEntity() instanceof ServerPlayer player
                ? PermissionAPI.getPermission(player, ADMIN)
                : SwiCommands.vanillaAdmin(source));

        //#if MC >= 1.21.6
        PermissionGatherEvent.Nodes.BUS.addListener(e -> e.addNodes(ADMIN));
        ServerStartingEvent.BUS.addListener(e -> core.serverStarting(e.getServer()));
        ServerStartedEvent.BUS.addListener(e -> core.serverStarted(e.getServer(), TestPlayers::create));
        ServerStoppedEvent.BUS.addListener(e -> core.serverStopped());
        TickEvent.ServerTickEvent.Post.BUS.addListener(e -> core.onServerTick(e.server()));
        PlayerEvent.PlayerLoggedInEvent.BUS.addListener(e -> {
            if (e.getEntity() instanceof ServerPlayer player) core.onJoin(player);
        });
        PlayerEvent.PlayerLoggedOutEvent.BUS.addListener(e -> {
            if (e.getEntity() instanceof ServerPlayer player) core.onQuit(player);
        });
        PlayerEvent.PlayerChangedDimensionEvent.BUS.addListener(e -> {
            if (e.getEntity() instanceof ServerPlayer player) core.onLevelChange(player, Mc.level(player));
        });
        PlayerEvent.PlayerRespawnEvent.BUS.addListener(e -> {
            if (e.getEntity() instanceof ServerPlayer player) core.onRespawn(player);
        });
        RegisterCommandsEvent.BUS.addListener(e -> core.registerCommands(e.getDispatcher()));
        //#else
        //$$ var bus = MinecraftForge.EVENT_BUS;
        //$$ bus.addListener((PermissionGatherEvent.Nodes e) -> e.addNodes(ADMIN));
        //$$ bus.addListener((ServerStartingEvent e) -> core.serverStarting(e.getServer()));
        //$$ bus.addListener((ServerStartedEvent e) -> core.serverStarted(e.getServer(), TestPlayers::create));
        //$$ bus.addListener((ServerStoppedEvent e) -> core.serverStopped());
        //$$ bus.addListener((TickEvent.ServerTickEvent e) -> {
        //$$     if (e.phase == TickEvent.Phase.END) core.onServerTick(e.getServer());
        //$$ });
        //$$ bus.addListener((PlayerEvent.PlayerLoggedInEvent e) -> {
        //$$     if (e.getEntity() instanceof ServerPlayer player) core.onJoin(player);
        //$$ });
        //$$ bus.addListener((PlayerEvent.PlayerLoggedOutEvent e) -> {
        //$$     if (e.getEntity() instanceof ServerPlayer player) core.onQuit(player);
        //$$ });
        //$$ bus.addListener((PlayerEvent.PlayerChangedDimensionEvent e) -> {
        //$$     if (e.getEntity() instanceof ServerPlayer player) core.onLevelChange(player, Mc.level(player));
        //$$ });
        //$$ bus.addListener((PlayerEvent.PlayerRespawnEvent e) -> {
        //$$     if (e.getEntity() instanceof ServerPlayer player) core.onRespawn(player);
        //$$ });
        //$$ bus.addListener((RegisterCommandsEvent e) -> core.registerCommands(e.getDispatcher()));
        //#endif
    }
}
