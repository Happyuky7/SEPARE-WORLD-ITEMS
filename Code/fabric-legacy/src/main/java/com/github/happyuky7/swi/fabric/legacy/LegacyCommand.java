package com.github.happyuky7.swi.fabric.legacy;

import com.github.happyuky7.swi.api.context.GroupContext;
import com.github.happyuky7.swi.common.backup.BackupService;
import com.github.happyuky7.swi.common.i18n.Messages;
import net.minecraft.command.AbstractCommand;
import net.minecraft.command.CommandException;
import net.minecraft.command.CommandSource;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.LiteralText;

import java.io.File;
import java.util.Arrays;
import java.util.List;

/**
 * /separeworlditems (alias /swi): info, reload, backup, group [player]. 1.12.2 predates Brigadier.
 */
final class LegacyCommand extends AbstractCommand {

    private final SwiLegacyFabric mod;

    LegacyCommand(SwiLegacyFabric mod) {
        this.mod = mod;
    }

    @Override
    public String getCommandName() {
        return "separeworlditems";
    }

    @Override
    public List<String> getAliases() {
        return Arrays.asList("swi");
    }

    @Override
    public String getUsageTranslationKey(CommandSource source) {
        return "/swi [reload|backup|group [player]]";
    }

    /** Everyone may run info and "group" on themselves; admin sub-commands are checked in execute. */
    @Override
    public int getPermissionLevel() {
        return 0;
    }

    @Override
    public boolean method_3278(MinecraftServer server, CommandSource source) { // isAccessible
        return true;
    }

    @Override
    public void method_3279(MinecraftServer server, CommandSource source, String[] args) throws CommandException { // execute
        boolean admin = source.canUseCommand(2, getCommandName());
        Messages m = mod.messages();
        if (args.length == 0) {
            for (String line : m.lines("mod.info", "version", mod.platform().modVersion(), "loader", "Legacy Fabric",
                    "minecraft", "1.12.2", "storage", mod.settings().storageType(), "groups", String.join(", ", mod.settings().groups()))) {
                send(source, line);
            }
            return;
        }
        if (args[0].equalsIgnoreCase("reload")) {
            if (!admin) {
                send(source, m.get("no-permission"));
                return;
            }
            List<String> problems = mod.reload();
            m = mod.messages();
            send(source, problems.isEmpty() ? m.get("mod.reload") : m.get("mod.reload-warnings", "count", problems.size()));
            for (String p : problems) send(source, m.get("mod.reload-warning-line", "problem", p));
            return;
        }
        if (args[0].equalsIgnoreCase("backup")) {
            if (!admin) {
                send(source, m.get("no-permission"));
                return;
            }
            backup(server, source);
            return;
        }
        if (args[0].equalsIgnoreCase("group")) {
            ServerPlayerEntity target;
            if (args.length > 1) {
                if (!admin) {
                    send(source, m.get("no-permission"));
                    return;
                }
                target = server.getPlayerManager().getPlayer(args[1]);
                if (target == null) {
                    send(source, m.get("mod.player-offline", "player", args[1]));
                    return;
                }
            } else {
                target = getAsPlayer(source);
            }
            GroupContext ctx = mod.engine().tracker().get(target.getUuid());
            String name = target.getGameProfile().getName();
            send(source, ctx != null ? m.get("mod.group", "player", name, "group", ctx.group()) : m.get("mod.group-untracked", "player", name));
            return;
        }
        send(source, getUsageTranslationKey(source));
    }

    /** Zips the data folder off the server thread; the result is reported back on the server thread. */
    private void backup(MinecraftServer server, CommandSource source) {
        BackupService backups = mod.backups();
        if (backups == null) {
            return;
        }
        send(source, mod.messages().get("backups.force-backup.start"));
        Thread t = new Thread(() -> {
            List<String> lines;
            try {
                File zip = backups.createBackup();
                lines = mod.messages().lines("backups.force-backup.completed", "backup_path", zip.getAbsolutePath());
            } catch (Exception e) {
                mod.platform().logger().error("[Backups] Manual backup failed", e);
                lines = mod.messages().lines("backups.force-backup.error");
            }
            List<String> result = lines;
            server.submit(() -> {
                for (String line : result) send(source, line);
            });
        }, "SepareWorldItems-Backup");
        t.setDaemon(true);
        t.start();
    }

    /** Sends a message with {@code &} color codes. */
    private static void send(CommandSource source, String text) {
        source.sendMessage(new LiteralText(Messages.colorize(text)));
    }
}
