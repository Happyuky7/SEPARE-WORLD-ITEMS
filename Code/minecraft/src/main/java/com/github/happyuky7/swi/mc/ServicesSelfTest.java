package com.github.happyuky7.swi.mc;

import com.github.happyuky7.swi.api.context.GroupContext;
import com.github.happyuky7.swi.api.data.DataNode;
import com.github.happyuky7.swi.common.backup.BackupService;
import com.github.happyuky7.swi.common.i18n.Messages;
import com.github.happyuky7.swi.common.storage.PlayerDataStore;
import net.minecraft.server.level.ServerPlayer;

import java.io.File;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipFile;

/**
 * Self test of the services around the switch engine: bundled language files and backups.
 */
final class ServicesSelfTest implements VanillaSelfTest.Hook {

    private final SwiCore core;
    private final File langsDir;

    ServicesSelfTest(SwiCore core, File langsDir) {
        this.core = core;
        this.langsDir = langsDir;
    }

    @Override
    public String name() {
        return "langs + backups";
    }

    @Override
    public void prepare(ServerPlayer player, List<String> failures) {
        String reload = core.messages().get("mod.reload");
        if ("mod.reload".equals(reload) || !Messages.colorize(reload).contains("§")) {
            failures.add("language " + core.messages().lang() + " has no mod.reload message: " + reload);
        }
        // Bundled Spanish file, read from the jar only (no download, empty folder).
        Messages es = Messages.load(new File(langsDir, "selftest-missing"), "es_ES", false,
                SwiCore.class.getClassLoader(), SwiCore.LANGS_RESOURCE, core.platform().logger());
        if (!es.get("mod.reload").contains("recargada")) {
            failures.add("bundled es_ES.yml not found: " + es.get("mod.reload"));
        }

        // The configured backend (MONGODB / REDIS / SQL drivers are jar-in-jar libraries: this proves they load).
        try {
            PlayerDataStore store = core.store();
            UUID uuid = UUID.randomUUID();
            GroupContext ctx = GroupContext.world("swi-selftest");
            DataNode data = DataNode.create();
            data.set("exp-level", 42);
            store.save(ctx, uuid, "SwiSelfTest", data);
            if (store.load(ctx, uuid).getInt("exp-level", 0) != 42) failures.add("storage " + store.type() + " round trip");
            core.platform().logger().info("[SelfTest] storage " + store.type() + " round trip done");
        } catch (Throwable t) {
            failures.add("storage " + core.settings().storageType() + " failed: " + t);
        }

        BackupService backups = core.backups();
        if (backups == null) {
            failures.add("backup service not started");
            return;
        }
        try {
            File zip = backups.createBackup();
            try (ZipFile z = new ZipFile(zip)) {
                if (z.getEntry("extra/config.yml") == null) failures.add("backup " + zip.getName() + " has no extra/config.yml");
            }
        } catch (Exception e) {
            failures.add("backup failed: " + e);
        }
    }

    @Override
    public void verifyCleared(ServerPlayer player, List<String> failures) {
    }

    @Override
    public void verifyRestored(ServerPlayer player, List<String> failures) {
    }
}
