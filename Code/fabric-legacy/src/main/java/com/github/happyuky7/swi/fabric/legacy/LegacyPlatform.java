package com.github.happyuky7.swi.fabric.legacy;

import com.github.happyuky7.swi.api.Platform;
import com.github.happyuky7.swi.common.platform.SwiLogger;
import com.github.happyuky7.swi.common.platform.SwiPlatform;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;

final class LegacyPlatform implements SwiPlatform {

    private static final Logger LOG = LogManager.getLogger("SepareWorldItems");

    private final SwiLogger logger;
    private volatile MinecraftServer server;
    private volatile File dataFolder;
    private volatile ExecutorService async;

    LegacyPlatform(BooleanSupplier debug) {
        this.logger = new SwiLogger() {
            public void info(String m) { LOG.info(m); }
            public void warn(String m) { LOG.warn(m); }
            public void error(String m, Throwable t) { LOG.error(m, t); }
            public void debug(String m) { if (debug.getAsBoolean()) LOG.info("[debug] " + m); }
        };
    }

    void start(MinecraftServer server, File dataFolder) {
        this.server = server;
        this.dataFolder = dataFolder;
        AtomicInteger n = new AtomicInteger();
        this.async = Executors.newFixedThreadPool(2, r -> {
            Thread t = new Thread(r, "SepareWorldItems-IO-" + n.incrementAndGet());
            t.setDaemon(true);
            return t;
        });
    }

    void stop() {
        ExecutorService pool = async;
        if (pool != null) {
            pool.shutdown();
            try {
                pool.awaitTermination(15, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        async = null;
        server = null;
    }

    MinecraftServer server() {
        return server;
    }

    @Override
    public Platform platform() {
        return Platform.FABRIC;
    }

    @Override
    public String minecraftVersion() {
        return "1.12.2";
    }

    @Override
    public String modVersion() {
        return FabricLoader.getInstance().getModContainer("separeworlditems")
                .map(c -> c.getMetadata().getVersion().getFriendlyString()).orElse("unknown");
    }

    @Override
    public File dataFolder() {
        File f = dataFolder;
        if (f == null) {
            throw new IllegalStateException("The server is not running");
        }
        return f;
    }

    @Override
    public SwiLogger logger() {
        return logger;
    }

    @Override
    public Executor asyncExecutor() {
        return r -> {
            ExecutorService pool = async;
            if (pool == null || pool.isShutdown()) r.run();
            else pool.execute(r);
        };
    }

    @Override
    public Executor syncExecutor() {
        return r -> {
            MinecraftServer s = server;
            if (s == null || s.isOnThread()) r.run();
            else s.submit(r);
        };
    }
}
