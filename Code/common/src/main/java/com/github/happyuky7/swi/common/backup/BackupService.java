package com.github.happyuky7.swi.common.backup;

import com.github.happyuky7.swi.api.context.ContextType;
import com.github.happyuky7.swi.api.context.GroupContext;
import com.github.happyuky7.swi.common.config.SwiSettings;
import com.github.happyuky7.swi.common.platform.SwiLogger;
import com.github.happyuky7.swi.common.storage.PlayerDataStore;
import com.github.happyuky7.swi.common.yaml.YamlCodec;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Zip backups of the data folder, same settings as the plugin ({@code settings.backups.*}):
 * {@code enabled}, {@code interval} + {@code interval-type} (m / h / d), {@code max-backups} and
 * {@code remote-snapshot.enabled} (export database records as YAML inside the zip).
 * Backups go to {@code <dataFolder>/backups/SepareWorldItems_<date>.zip}.
 */
public final class BackupService {

    private final File dataFolder;
    private final SwiLogger logger;
    private final Supplier<SwiSettings> settings;
    private final Supplier<PlayerDataStore> store;
    private final File[] extraFiles;
    private ScheduledExecutorService scheduler;
    private ScheduledFuture<?> task;

    /**
     * @param extraFiles files outside the data folder to include under {@code extra/} (e.g. the mod's config.yml)
     */
    public BackupService(File dataFolder, SwiLogger logger, Supplier<SwiSettings> settings, Supplier<PlayerDataStore> store,
                         File... extraFiles) {
        this.dataFolder = dataFolder;
        this.logger = logger;
        this.settings = settings;
        this.store = store;
        this.extraFiles = extraFiles;
    }

    public File backupFolder() {
        return new File(dataFolder, "backups");
    }

    /** (Re)schedules automatic backups from the current settings. */
    public synchronized void start() {
        stop();
        SwiSettings s = settings.get();
        if (!s.raw().getBoolean("settings.backups.enabled", true)) {
            logger.info("[Backups] Automatic backups are disabled");
            return;
        }
        long periodMs = intervalMillis(s);
        if (periodMs <= 0) {
            logger.warn("[Backups] Invalid settings.backups.interval / interval-type (use m, h or d): automatic backups disabled");
            return;
        }
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "SepareWorldItems-Backups");
            t.setDaemon(true);
            return t;
        });
        // Count from the newest backup, so servers restarted more often than the interval still get backups.
        long delayMs = Math.max(TimeUnit.MINUTES.toMillis(1), periodMs - (System.currentTimeMillis() - newestBackupTime()));
        task = scheduler.scheduleAtFixedRate(() -> {
            try {
                createBackup();
            } catch (Exception e) {
                logger.error("[Backups] Automatic backup failed", e);
            }
        }, Math.min(delayMs, periodMs), periodMs, TimeUnit.MILLISECONDS);
        logger.info("[Backups] Automatic backup every " + s.raw().getInt("settings.backups.interval", 1)
                + s.raw().getString("settings.backups.interval-type", "d") + ", keeping " + maxBackups(s));
    }

    public synchronized void stop() {
        if (task != null) task.cancel(false);
        if (scheduler != null) scheduler.shutdown();
        task = null;
        scheduler = null;
    }

    static long intervalMillis(SwiSettings s) {
        long interval = s.raw().getLong("settings.backups.interval", 1);
        String unit = s.raw().getString("settings.backups.interval-type", "d").trim();
        if (interval <= 0) return -1;
        switch (unit) {
            case "m": return TimeUnit.MINUTES.toMillis(interval);
            case "h": return TimeUnit.HOURS.toMillis(interval);
            case "d": return TimeUnit.DAYS.toMillis(interval);
            default: return -1;
        }
    }

    private static int maxBackups(SwiSettings s) {
        return Math.max(1, s.raw().getInt("settings.backups.max-backups", 25));
    }

    /**
     * Creates a backup now (blocking; call it off the server thread).
     *
     * @return the zip file
     */
    public synchronized File createBackup() throws IOException {
        File folder = backupFolder();
        if (!folder.isDirectory() && !folder.mkdirs()) {
            throw new IOException("Cannot create " + folder);
        }
        String name = "SepareWorldItems_" + new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.ROOT).format(new Date()) + ".zip";
        File zip = new File(folder, name);
        Path root = dataFolder.toPath();
        Path backups = folder.toPath();
        try (OutputStream out = Files.newOutputStream(zip.toPath()); ZipOutputStream zos = new ZipOutputStream(out);
             Stream<Path> files = Files.walk(root)) {
            for (Path p : (Iterable<Path>) files::iterator) {
                if (Files.isDirectory(p) || p.startsWith(backups) || p.getFileName().toString().endsWith(".tmp")) {
                    continue;
                }
                zos.putNextEntry(new ZipEntry(root.relativize(p).toString().replace(File.separatorChar, '/')));
                Files.copy(p, zos);
                zos.closeEntry();
            }
            for (File extra : extraFiles) {
                if (extra.isFile()) {
                    zos.putNextEntry(new ZipEntry("extra/" + extra.getName()));
                    Files.copy(extra.toPath(), zos);
                    zos.closeEntry();
                }
            }
            SwiSettings s = settings.get();
            PlayerDataStore st = store.get();
            if (st != null && st.type().isRemote() && s.raw().getBoolean("settings.backups.remote-snapshot.enabled", false)) {
                exportRemote(st, zos);
            }
        }
        prune();
        logger.info("[Backups] Backup created: " + zip.getName());
        return zip;
    }

    /** Writes every database record as {@code remote-snapshot/<scope>/<group>/<name>-<uuid>.yml}. */
    private void exportRemote(PlayerDataStore st, ZipOutputStream zos) throws IOException {
        int[] count = {0};
        IOException[] failure = {null};
        for (ContextType type : ContextType.values()) {
            String dir = type == ContextType.REGION ? "worldguard/groups" : "groups";
            st.forEachEntry(type, (group, uuid, name) -> {
                if (failure[0] != null) return;
                try {
                    GroupContext ctx = GroupContext.of(type, group);
                    zos.putNextEntry(new ZipEntry("remote-snapshot/" + dir + "/" + group + "/" + (name != null ? name : uuid) + "-" + uuid + ".yml"));
                    zos.write(YamlCodec.dump(st.load(ctx, uuid)).getBytes(StandardCharsets.UTF_8));
                    zos.closeEntry();
                    count[0]++;
                } catch (IOException e) {
                    failure[0] = e;
                }
            });
        }
        if (failure[0] != null) throw failure[0];
        logger.info("[Backups] Remote snapshot: " + count[0] + " records exported");
    }

    /** Last-modified time of the newest backup, 0 when there is none. */
    private long newestBackupTime() {
        long newest = 0;
        File[] zips = listBackups();
        if (zips != null) {
            for (File f : zips) newest = Math.max(newest, f.lastModified());
        }
        return newest;
    }

    private File[] listBackups() {
        return backupFolder().listFiles((d, n) -> n.startsWith("SepareWorldItems_") && n.endsWith(".zip"));
    }

    private void prune() {
        File[] zips = listBackups();
        if (zips == null) return;
        int max = maxBackups(settings.get());
        if (zips.length <= max) return;
        Arrays.sort(zips, Comparator.comparing(File::getName));
        for (int i = 0; i < zips.length - max; i++) {
            if (!zips[i].delete()) logger.warn("[Backups] Could not delete old backup " + zips[i].getName());
        }
    }
}
