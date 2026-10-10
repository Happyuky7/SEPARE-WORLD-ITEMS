package com.github.happyuky7.swi.api;

/**
 * Platform the SepareWorldItems implementation runs on.
 */
public enum Platform {
    /** Bukkit, Spigot, Paper, Purpur, Folia and hybrids (Mohist, Arclight...). */
    BUKKIT,
    /** Fabric and Legacy Fabric. */
    FABRIC,
    /** NeoForge. */
    NEOFORGE,
    /** MinecraftForge. */
    FORGE;

    /**
     * @return the Java type of the platform player objects passed to
     * {@link com.github.happyuky7.swi.api.section.DataSection} and events:
     * {@code org.bukkit.entity.Player} on Bukkit, {@code net.minecraft.server.level.ServerPlayer} on
     * Fabric/NeoForge/Forge (named {@code net.minecraft.entity.player.ServerPlayerEntity} on Legacy Fabric 1.12.2).
     */
    public String playerClassName() {
        return this == BUKKIT ? "org.bukkit.entity.Player" : "net.minecraft.server.level.ServerPlayer";
    }
}
