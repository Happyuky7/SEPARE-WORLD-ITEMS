package com.github.happyuky7.swi.mc;

import com.github.happyuky7.swi.api.Platform;
import com.github.happyuky7.swi.common.platform.SwiLogger;
import com.github.happyuky7.swi.common.platform.SwiPlatform;
import com.github.happyuky7.swi.mc.compat.Mc;
import net.minecraft.server.MinecraftServer;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/**
 * {@link SwiPlatform} for any Minecraft mod loader: server thread executor, IO pool, logger.
 */
public final class VanillaPlatform implements SwiPlatform {

    private final Platform platform;
    private final Supplier<String> modVersion;
    private final SwiLogger logger;
    private volatile MinecraftServer server;
    private volatile File dataFolder;
    private volatile ExecutorService async;

    public VanillaPlatform(Platform platform, Supplier<String> modVersion, BooleanSupplier debug) {
        this.platform = platform;
        this.modVersion = modVersion;
        this.logger = new VanillaLogger(debug);
    }

    public void start(MinecraftServer server, File dataFolder) {
        this.server = server;
        this.dataFolder = dataFolder;
        AtomicInteger n = new AtomicInteger();
        this.async = Executors.newFixedThreadPool(2, r -> {
            Thread t = new Thread(r, "SepareWorldItems-IO-" + n.incrementAndGet());
            t.setDaemon(true);
            return t;
        });
    }

    public void stop() {
        ExecutorService pool = async;
        if (pool != null) {
            pool.shutdown();
            try {
                if (!pool.awaitTermination(15, TimeUnit.SECONDS)) {
                    logger.warn("Pending storage tasks did not finish in 15s");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        async = null;
        server = null;
    }

    public MinecraftServer server() {
        return server;
    }

    @Override
    public Platform platform() {
        return platform;
    }

    @Override
    public String minecraftVersion() {
        return Mc.minecraftVersion();
    }

    @Override
    public String modVersion() {
        return modVersion.get();
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
    public java.util.concurrent.Executor asyncExecutor() {
        return r -> {
            ExecutorService pool = async;
            if (pool == null || pool.isShutdown()) {
                r.run();
            } else {
                pool.execute(r);
            }
        };
    }

    @Override
    public java.util.concurrent.Executor syncExecutor() {
        return r -> {
            MinecraftServer s = server;
            if (s == null || s.isSameThread()) {
                r.run();
            } else {
                s.execute(r);
            }
        };
    }
}
