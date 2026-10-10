package com.github.happyuky7.swi.mc;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * Builds a player entity for the self test on loaders without a fake player API (Forge 1.21+).
 * It is not in the player list and its connection has no channel: packets are only queued.
 */
public final class TestPlayers {

    private TestPlayers() {
    }

    public static ServerPlayer create(MinecraftServer server) {
        //#if MC >= 1.20.2
        com.mojang.authlib.GameProfile profile = new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "SelfTest");
        ServerPlayer player = new ServerPlayer(server, server.overworld(), profile, net.minecraft.server.level.ClientInformation.createDefault());
        new net.minecraft.server.network.ServerGamePacketListenerImpl(server,
                new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND), player,
                net.minecraft.server.network.CommonListenerCookie.createInitial(profile, false));
        return player;
        //#else
        //$$ throw new UnsupportedOperationException("TestPlayers needs Minecraft 1.20.2+");
        //#endif
    }
}
