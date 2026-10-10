package com.github.happyuky7.swi.fabric;

import com.github.happyuky7.swi.mc.VanillaSelfTest;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
//#if MC >= 1.19.4
import net.fabricmc.fabric.api.entity.FakePlayer;
//#else
//$$ import com.mojang.authlib.GameProfile;
//$$ import java.util.UUID;
//$$ import net.minecraft.network.Connection;
//$$ import net.minecraft.network.protocol.PacketFlow;
//$$ import net.minecraft.server.level.ServerPlayerGameMode;
//$$ import net.minecraft.server.network.ServerGamePacketListenerImpl;
//#endif

/**
 * Fabric side of {@link VanillaSelfTest}: provides the test player.
 */
final class SelfTest {

    private SelfTest() {
    }

    /**
     * A player entity that is not in the player list: Fabric's FakePlayer, or a hand-built one with a
     * channel-less connection (packets are only queued) on versions without FakePlayer.
     */
    static ServerPlayer testPlayer(MinecraftServer server) {
        //#if MC >= 1.19.4
        return FakePlayer.get(server.overworld());
        //#else
        //$$ GameProfile profile = new GameProfile(UUID.randomUUID(), "SelfTest");
        //#if MC >= 1.17
        //$$ ServerPlayer p = new ServerPlayer(server, server.overworld(), profile);
        //#else
        //$$ ServerPlayer p = new ServerPlayer(server, server.overworld(), profile, new ServerPlayerGameMode(server.overworld()));
        //#endif
        //$$ new ServerGamePacketListenerImpl(server, new Connection(PacketFlow.SERVERBOUND), p);
        //$$ return p;
        //#endif
    }

}
