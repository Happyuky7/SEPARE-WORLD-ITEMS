package com.github.happyuky7.swi.neoforge.integration;

import com.github.happyuky7.swi.mc.SwiCommands;
import com.github.happyuky7.swi.mc.SwiCore;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.server.permission.PermissionAPI;
import net.neoforged.neoforge.server.permission.events.PermissionGatherEvent;
import net.neoforged.neoforge.server.permission.nodes.PermissionNode;
import net.neoforged.neoforge.server.permission.nodes.PermissionTypes;

/**
 * Registers {@code separeworlditems.admin} in NeoForge's PermissionAPI, which LuckPerms and other
 * permission mods implement. Default (no permission mod): operator level 2.
 */
public final class NeoForgePermissions {

    public static final PermissionNode<Boolean> ADMIN = new PermissionNode<>("separeworlditems", "admin", PermissionTypes.BOOLEAN,
            (player, uuid, context) -> player != null && SwiCommands.vanillaAdmin(player.createCommandSourceStack()));

    private NeoForgePermissions() {
    }

    public static void install(SwiCore core, IEventBus gameBus) {
        gameBus.addListener(PermissionGatherEvent.Nodes.class, e -> e.addNodes(ADMIN));
        core.setAdminCheck(source -> source.getEntity() instanceof ServerPlayer player
                ? PermissionAPI.getPermission(player, ADMIN)
                : SwiCommands.vanillaAdmin(source));
    }
}
