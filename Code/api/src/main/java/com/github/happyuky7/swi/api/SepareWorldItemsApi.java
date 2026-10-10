package com.github.happyuky7.swi.api;

import com.github.happyuky7.swi.api.context.GroupService;
import com.github.happyuky7.swi.api.data.PlayerDataService;
import com.github.happyuky7.swi.api.event.EventBus;
import com.github.happyuky7.swi.api.section.DataSectionRegistry;
import org.jetbrains.annotations.NotNull;

/**
 * The SepareWorldItems API. Obtain it with {@link SepareWorldItems#get()}.
 */
public interface SepareWorldItemsApi {

    /**
     * API revision. Increased when methods are added, so addons can check for features at runtime.
     */
    int API_VERSION = 1;

    /**
     * @return the platform running SepareWorldItems
     */
    @NotNull Platform platform();

    /**
     * @return the Minecraft version of the server, e.g. {@code 1.21.11} or {@code 26.3}
     */
    @NotNull String minecraftVersion();

    /**
     * @return the SepareWorldItems version, e.g. {@code 2.1.0}
     */
    @NotNull String version();

    /**
     * Groups, worlds and the context each online player currently uses.
     */
    @NotNull GroupService groups();

    /**
     * Read access to saved player data of any group.
     */
    @NotNull PlayerDataService playerData();

    /**
     * Platform-independent events (group switches, data save/load).
     */
    @NotNull EventBus events();

    /**
     * Register extra data (mana, skills, backpacks...) that must be saved and restored per group.
     */
    @NotNull DataSectionRegistry sections();
}
