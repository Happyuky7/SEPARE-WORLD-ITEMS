package com.github.happyuky7.swi.mc;

import com.github.happyuky7.swi.api.context.GroupContext;
import com.github.happyuky7.swi.common.backup.BackupService;
import com.github.happyuky7.swi.common.i18n.Messages;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
//#if MC >= 1.19
import net.minecraft.network.chat.Component;
//#else
//$$ import net.minecraft.network.chat.TextComponent;
//#endif

import java.io.File;
import java.util.List;

/**
 * /separeworlditems (alias /swi): info, reload, backup, group [player]. Texts come from the language files.
 */
public final class SwiCommands {

    private SwiCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, SwiCore mod) {
        for (String name : new String[]{"separeworlditems", "swi"}) {
            LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal(name)
                    .executes(ctx -> info(ctx, mod))
                    .then(Commands.literal("reload")
                            .requires(mod::isAdmin)
                            .executes(ctx -> reload(ctx, mod)))
                    .then(Commands.literal("backup")
                            .requires(mod::isAdmin)
                            .executes(ctx -> backup(ctx, mod)))
                    .then(Commands.literal("group")
                            .executes(ctx -> group(ctx, mod, ctx.getSource().getPlayerOrException()))
                            .then(Commands.argument("player", StringArgumentType.word())
                                    .requires(mod::isAdmin)
                                    .executes(ctx -> {
                                        String target = StringArgumentType.getString(ctx, "player");
                                        ServerPlayer p = ctx.getSource().getServer().getPlayerList().getPlayerByName(target);
                                        if (p == null) {
                                            send(ctx, mod.messages().get("mod.player-offline", "player", target));
                                            return 0;
                                        }
                                        return group(ctx, mod, p);
                                    })));
            dispatcher.register(root);
        }
    }

    /**
     * Operator level 2 (game masters): the default when no permissions mod is installed.
     */
    public static boolean vanillaAdmin(CommandSourceStack source) {
        //#if MC >= 1.21.11
        return Commands.hasPermission(Commands.LEVEL_GAMEMASTERS).test(source);
        //#else
        //$$ return source.hasPermission(2);
        //#endif
    }

    private static int info(CommandContext<CommandSourceStack> ctx, SwiCore mod) {
        for (String line : mod.messages().lines("mod.info", "version", mod.platform().modVersion(), "loader", mod.loaderName(),
                "minecraft", mod.platform().minecraftVersion(), "storage", mod.settings().storageType(),
                "groups", String.join(", ", mod.settings().groups()))) {
            send(ctx, line);
        }
        return 1;
    }

    private static int reload(CommandContext<CommandSourceStack> ctx, SwiCore mod) {
        List<String> problems = mod.reload();
        Messages m = mod.messages();
        send(ctx, problems.isEmpty() ? m.get("mod.reload") : m.get("mod.reload-warnings", "count", problems.size()));
        for (String p : problems) {
            send(ctx, m.get("mod.reload-warning-line", "problem", p));
        }
        return 1;
    }

    /** Zips the data folder off the server thread; the result is reported back on the server thread. */
    private static int backup(CommandContext<CommandSourceStack> ctx, SwiCore mod) {
        BackupService backups = mod.backups();
        if (backups == null) {
            return 0;
        }
        CommandSourceStack source = ctx.getSource();
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
            source.getServer().execute(() -> {
                for (String line : result) {
                    send(source, line);
                }
            });
        }, "SepareWorldItems-Backup");
        t.setDaemon(true);
        t.start();
        return 1;
    }

    private static int group(CommandContext<CommandSourceStack> ctx, SwiCore mod, ServerPlayer player) {
        GroupContext current = mod.engine().tracker().get(player.getUUID());
        send(ctx, current != null
                ? mod.messages().get("mod.group", "player", player.getScoreboardName(), "group", current.group())
                : mod.messages().get("mod.group-untracked", "player", player.getScoreboardName()));
        return 1;
    }

    private static void send(CommandContext<CommandSourceStack> ctx, String text) {
        send(ctx.getSource(), text);
    }

    /** Sends a message with {@code &} color codes. */
    private static void send(CommandSourceStack source, String text) {
        String colored = Messages.colorize(text);
        //#if MC >= 1.20
        source.sendSuccess(() -> Component.literal(colored), false);
        //#elseif MC >= 1.19
        //$$ source.sendSuccess(Component.literal(colored), false);
        //#else
        //$$ source.sendSuccess(new TextComponent(colored), false);
        //#endif
    }
}
