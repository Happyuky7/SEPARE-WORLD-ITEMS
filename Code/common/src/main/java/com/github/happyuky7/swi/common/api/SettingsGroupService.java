package com.github.happyuky7.swi.common.api;

import com.github.happyuky7.swi.api.context.GroupContext;
import com.github.happyuky7.swi.api.context.GroupService;
import com.github.happyuky7.swi.common.config.SwiSettings;
import com.github.happyuky7.swi.common.switching.ContextTracker;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * {@link GroupService} backed by {@link SwiSettings} (world groups only; no regions).
 */
public final class SettingsGroupService implements GroupService {

    private final Supplier<SwiSettings> settings;
    private final ContextTracker tracker;

    public SettingsGroupService(Supplier<SwiSettings> settings, ContextTracker tracker) {
        this.settings = settings;
        this.tracker = tracker;
    }

    @Override
    public @NotNull Set<String> worldGroups() {
        return settings.get().groups();
    }

    @Override
    public @NotNull Set<String> regionGroups() {
        return Collections.emptySet();
    }

    @Override
    public @NotNull Map<String, String> worldMappings() {
        return settings.get().worlds();
    }

    @Override
    public @Nullable String groupOfWorld(@NotNull String worldId) {
        return settings.get().groupOfWorld(worldId);
    }

    @Override
    public @NotNull String defaultGroup() {
        return settings.get().defaultGroup();
    }

    @Override
    public @Nullable GroupContext currentContext(@NotNull UUID player) {
        return tracker.get(player);
    }
}
