package com.github.happyuky7.swi.common.backup;

import com.github.happyuky7.swi.common.config.SwiSettings;
import com.github.happyuky7.swi.common.platform.SwiLogger;
import com.github.happyuky7.swi.common.yaml.YamlCodec;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class BackupServiceTest {

    private static final SwiLogger LOG = new SwiLogger() {
        public void info(String m) { }
        public void warn(String m) { }
        public void error(String m, Throwable t) { }
        public void debug(String m) { }
    };

    @Test
    void zipsDataFolderAndPrunesOldBackups(@TempDir File dir) throws Exception {
        File data = new File(dir, "separeworlditems");
        File record = new File(data, "groups/default/Alex-0000.yml");
        record.getParentFile().mkdirs();
        Files.write(record.toPath(), "exp-level: 5\n".getBytes(StandardCharsets.UTF_8));
        File config = new File(dir, "config.yml");
        Files.write(config.toPath(), "config-version: 3.0.0\n".getBytes(StandardCharsets.UTF_8));

        SwiSettings settings = new SwiSettings(YamlCodec.parse("settings:\n  backups:\n    max-backups: 2\n"));
        BackupService service = new BackupService(data, LOG, () -> settings, () -> null, config);

        File first = service.createBackup();
        try (ZipFile zip = new ZipFile(first)) {
            assertNotNull(zip.getEntry("groups/default/Alex-0000.yml"));
            assertNotNull(zip.getEntry("extra/config.yml"));
            assertNull(zip.getEntry("backups/" + first.getName()));
        }
        // Names have a one-second resolution: fake older backups instead of sleeping.
        new File(service.backupFolder(), "SepareWorldItems_2000-01-01_00-00-00.zip").createNewFile();
        new File(service.backupFolder(), "SepareWorldItems_2000-01-02_00-00-00.zip").createNewFile();
        service.createBackup();
        File[] left = service.backupFolder().listFiles((d, n) -> n.endsWith(".zip"));
        assertEquals(2, left.length);
    }

    @Test
    void intervals() {
        assertEquals(30 * 60_000L, BackupService.intervalMillis(new SwiSettings(YamlCodec.parse(
                "settings:\n  backups:\n    interval: 30\n    interval-type: m\n"))));
        assertEquals(-1, BackupService.intervalMillis(new SwiSettings(YamlCodec.parse(
                "settings:\n  backups:\n    interval: 1\n    interval-type: w\n"))));
    }
}
