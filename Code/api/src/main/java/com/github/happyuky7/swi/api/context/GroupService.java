package com.github.happyuky7.swi.api.context;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Configured groups and the live context of online players.
 *
 * <p>World ids are platform names: Bukkit world names ({@code world_nether}) on Bukkit,
 * dimension ids ({@code minecraft:the_nether}) on Fabric.</p>
 */
public interface GroupService {

    /**
     * @return all configured world groups
     */
    @NotNull Set<String> worldGroups();

    /**
     * @return all configured region groups (empty when regions are not supported/enabled)
     */
    @NotNull Set<String> regionGroups();

    /**
     * @return world id -> group name, as configured
     */
    @NotNull Map<String, String> worldMappings();

    /**
     * @return the group of a world, or null when the world is not managed by SepareWorldItems
     */
    @Nullable String groupOfWorld(@NotNull String worldId);

    /**
     * @return the configured default group
     */
    @NotNull String defaultGroup();

    /**
     * @return the context whose data the online player is currently using, or null if unknown/offline
     */
    @Nullable GroupContext currentContext(@NotNull UUID player);
}
