package com.github.happyuky7.swi.common.platform;

import com.github.happyuky7.swi.api.Platform;

import java.io.File;
import java.util.concurrent.Executor;

/**
 * What common code needs from the loader/server it runs on.
 */
public interface SwiPlatform {

    Platform platform();

    String minecraftVersion();

    String modVersion();

    /**
     * Folder that holds config.yml, groups/, backups/...
     */
    File dataFolder();

    SwiLogger logger();

    /**
     * Runs tasks off the server thread (IO).
     */
    Executor asyncExecutor();

    /**
     * Runs tasks on the server thread. Must run immediately when already on it.
     */
    Executor syncExecutor();
}
