package com.github.happyuky7.swi.mc;

import com.github.happyuky7.swi.common.switching.PlayerAdapter;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;
import java.util.function.Supplier;

public final class VanillaPlayers implements PlayerAdapter<ServerPlayer> {

    private final Supplier<MinecraftServer> server;

    public VanillaPlayers(Supplier<MinecraftServer> server) {
        this.server = server;
    }

    @Override
    public Class<ServerPlayer> playerType() {
        return ServerPlayer.class;
    }

    @Override
    public UUID uuid(ServerPlayer player) {
        return player.getUUID();
    }

    @Override
    public String name(ServerPlayer player) {
        return player.getScoreboardName();
    }

    /**
     * True only for the current entity of a connected player (respawning replaces the entity).
     */
    @Override
    public boolean isOnline(ServerPlayer player) {
        MinecraftServer s = server.get();
        return s != null && s.getPlayerList().getPlayer(player.getUUID()) == player;
    }
}
