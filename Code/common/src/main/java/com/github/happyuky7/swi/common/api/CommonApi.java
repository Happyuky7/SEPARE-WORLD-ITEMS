package com.github.happyuky7.swi.common.api;

import com.github.happyuky7.swi.api.Platform;
import com.github.happyuky7.swi.api.SepareWorldItemsApi;
import com.github.happyuky7.swi.api.context.GroupService;
import com.github.happyuky7.swi.api.data.PlayerDataService;
import com.github.happyuky7.swi.api.event.EventBus;
import com.github.happyuky7.swi.api.section.DataSectionRegistry;
import org.jetbrains.annotations.NotNull;

/**
 * {@link SepareWorldItemsApi} assembled from its services; each platform provides the pieces.
 */
public final class CommonApi implements SepareWorldItemsApi {

    private final Platform platform;
    private final String minecraftVersion;
    private final String version;
    private final GroupService groups;
    private final PlayerDataService playerData;
    private final EventBus events;
    private final DataSectionRegistry sections;

    public CommonApi(Platform platform, String minecraftVersion, String version, GroupService groups,
                     PlayerDataService playerData, EventBus events, DataSectionRegistry sections) {
        this.platform = platform;
        this.minecraftVersion = minecraftVersion;
        this.version = version;
        this.groups = groups;
        this.playerData = playerData;
        this.events = events;
        this.sections = sections;
    }

    @Override
    public @NotNull Platform platform() {
        return platform;
    }

    @Override
    public @NotNull String minecraftVersion() {
        return minecraftVersion;
    }

    @Override
    public @NotNull String version() {
        return version;
    }

    @Override
    public @NotNull GroupService groups() {
        return groups;
    }

    @Override
    public @NotNull PlayerDataService playerData() {
        return playerData;
    }

    @Override
    public @NotNull EventBus events() {
        return events;
    }

    @Override
    public @NotNull DataSectionRegistry sections() {
        return sections;
    }
}
