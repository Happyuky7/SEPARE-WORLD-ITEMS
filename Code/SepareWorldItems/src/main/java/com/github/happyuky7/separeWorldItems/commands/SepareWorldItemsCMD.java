package com.github.happyuky7.separeWorldItems.commands;

import com.github.happyuky7.separeWorldItems.SepareWorldItems;
import com.github.happyuky7.separeWorldItems.managers.BackupManager;
import com.github.happyuky7.separeWorldItems.managers.MessagesManager;
import com.github.happyuky7.separeWorldItems.storage.PlayerDataStore;
import com.github.happyuky7.separeWorldItems.storage.StorageManager;
import com.github.happyuky7.separeWorldItems.storage.StorageType;
import com.github.happyuky7.separeWorldItems.storage.migration.StoreToStoreMigration;
import com.github.happyuky7.separeWorldItems.utils.MessageColors;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class SepareWorldItemsCMD implements CommandExecutor, TabCompleter {

    private static final long CONFIRM_TTL_MILLIS = 30_000L;
    private static final long CONFIRM_TTL_SECONDS = CONFIRM_TTL_MILLIS / 1000L;


    @Override
    public boolean onCommand(CommandSender sender, Command command, String s, String[] args) {

        if (args.length == 0) {

            sender.sendMessage(MessageColors.getMsgColor(" "));
            sender.sendMessage(MessageColors.getMsgColor("&a separeWorldItems &7- &aVersion: &f"
                    + SepareWorldItems.getInstance().getDescription().getVersion()));
            sender.sendMessage(MessageColors.getMsgColor(" "));
            sender.sendMessage(MessageColors.getMsgColor("&a Developed by: &fHappyuky7"));
            sender.sendMessage(MessageColors.getMsgColor("&a Github: &fhttps://github.com/Happyuky7/"));
            sender.sendMessage(MessageColors.getMsgColor(" "));

            if (sender.hasPermission("separeWorldItems.admin")) {
                sender.sendMessage(MessageColors.getMsgColor(" "));
                sender.sendMessage(MessageColors.getMsgColor("&a SepareWorldItems &7- &aHelp"));
                sender.sendMessage(MessageColors.getMsgColor(" "));
                sender.sendMessage(MessageColors.getMsgColor("&a /separeworlditems &7- &fShow this message"));
                sender.sendMessage(MessageColors.getMsgColor("&a /separeworlditems reload &7- &fReload the plugin"));
                sender.sendMessage(MessageColors.getMsgColor("&a /separeworlditems backup &7- &fCreate a manual backup"));
                sender.sendMessage(MessageColors.getMsgColor("&a /separeworlditems backupforce &7- &fForce a backup"));
                sender.sendMessage(MessageColors.getMsgColor("&a /separeworlditems migrate <from> <to> [overwrite] &7- &fMigrate storage backend"));
                sender.sendMessage(MessageColors.getMsgColor("&a /separeworlditems confirm <code> &7- &fConfirm a critical command"));
                sender.sendMessage(MessageColors.getMsgColor("&a /separeworlditems cancel &7- &fCancel a pending confirmation"));
                sender.sendMessage(MessageColors.getMsgColor(" "));
                return true;
            }

            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("separeworlditems.admin")) {
                sender.sendMessage(MessagesManager.getMessage("no-permission"));
                return true;
            }

            SepareWorldItems.getInstance().getConfig().reload();
            SepareWorldItems.getInstance().getLangs().reload();

            sender.sendMessage(MessagesManager.getMessage("reload"));

        }

        if (args[0].equalsIgnoreCase("backup")) {
            if (!sender.hasPermission("separeworlditems.admin")) {
                sender.sendMessage(MessagesManager.getMessage("no-permission"));
                return true;
            }

            SepareWorldItems plugin = SepareWorldItems.getInstance();
            File dataFolder = plugin.getDataFolder();
            File outDir = new File(dataFolder, "backups" + File.separator + "manual");

            StorageType type = StorageManager.getConfiguredType(plugin);
            boolean includeSnapshot = (type != StorageType.YAML && type != StorageType.SQLITE)
                    && plugin.getConfig().getBoolean("settings.backups.remote-snapshot.enabled", false);

            File backup = BackupManager.createOneTimeBackup(plugin, dataFolder, outDir, "manual_backup", includeSnapshot);
            if (backup != null) {
                sender.sendMessage(MessageColors.getMsgColor("&a[SepareWorldItems] Backup created: &f" + backup.getName()));
            } else {
                sender.sendMessage(MessageColors.getMsgColor("&c[SepareWorldItems] Backup failed."));
            }

            return true;
        }

        if (args[0].equalsIgnoreCase("confirm")) {
            if (!sender.hasPermission("separeworlditems.admin")) {
                sender.sendMessage(MessagesManager.getMessage("no-permission"));
                return true;
            }

            if (args.length < 2) {
                sender.sendMessage(MessagesManager.getMessage("commands.confirm.usage"));
                return true;
            }

            CommandConfirmations.PendingAction action = CommandConfirmations.consumeIfTokenMatches(sender, args[1]);
            if (action == null) {
                sender.sendMessage(MessagesManager.getMessage("commands.confirm.no-pending"));
                return true;
            }

            // Re-dispatch original command using stored args
            String[] original = action.args();
            if (original.length == 0) {
                sender.sendMessage(MessagesManager.getMessage("commands.confirm.pending-invalid"));
                return true;
            }

            // Execute again, but with an implicit confirmation marker.
            String[] replay = new String[original.length + 1];
            System.arraycopy(original, 0, replay, 0, original.length);
            replay[replay.length - 1] = "--confirmed";
            return onCommand(sender, command, s, replay);
        }

        if (args[0].equalsIgnoreCase("cancel")) {
            if (!sender.hasPermission("separeworlditems.admin")) {
                sender.sendMessage(MessagesManager.getMessage("no-permission"));
                return true;
            }
            CommandConfirmations.cancel(sender);
            sender.sendMessage(MessagesManager.getMessage("commands.cancel.success"));
            return true;
        }

        if (args[0].equalsIgnoreCase("backupforce")) {
            if (!sender.hasPermission("separeworlditems.admin")) {
                sender.sendMessage(MessagesManager.getMessage("no-permission"));
                return true;
            }

            if (!isConfirmed(args)) {
                CommandConfirmations.PendingAction pending = CommandConfirmations.create(sender, CommandConfirmations.ActionType.BACKUP_FORCE, args, CONFIRM_TTL_MILLIS);
                sender.sendMessage(MessagesManager.getMessage("commands.critical.header"));
                sender.sendMessage(MessagesManager.getMessage("commands.critical.confirm-within")
                        .replace("%seconds%", String.valueOf(CONFIRM_TTL_SECONDS))
                        .replace("%code%", pending.token()));
                return true;
            }

            sender.sendMessage(MessagesManager.getMessage("backups.force-backup.start"));

            try {
                File sourcefolder = SepareWorldItems.getInstance().getDataFolder();
                BackupManager.createForceBackup(SepareWorldItems.getInstance(), sourcefolder);

                sender.sendMessage(MessagesManager.getMessage("backups.force-backup.end"));
                sender.sendMessage(MessagesManager.getMessageList("backups.force-backup.completed")
                        .replace("%backup_path%", BackupManager.forebackupname));

            } catch (Exception e) {
                e.printStackTrace();
                sender.sendMessage(MessagesManager.getMessageList("backups.force-backup.error"));
            }

            return true;

        }

        if (args[0].equalsIgnoreCase("migrate")) {
            if (!sender.hasPermission("separeworlditems.admin")) {
                sender.sendMessage(MessagesManager.getMessage("no-permission"));
                return true;
            }

            if (args.length < 3) {
                sender.sendMessage(MessageColors.getMsgColor("&cUsage: /separeworlditems migrate <from> <to> [overwrite]"));
                return true;
            }

            StorageType from = StorageType.fromConfig(args[1]);
            StorageType to = StorageType.fromConfig(args[2]);
            boolean overwrite = args.length >= 4 && Boolean.parseBoolean(args[3]);

            if (from == to) {
                sender.sendMessage(MessageColors.getMsgColor("&c[from] and [to] cannot be the same."));
                return true;
            }

            if (!isConfirmed(args)) {
                CommandConfirmations.PendingAction pending = CommandConfirmations.create(sender, CommandConfirmations.ActionType.MIGRATE, args, CONFIRM_TTL_MILLIS);
                sender.sendMessage(MessagesManager.getMessage("commands.critical.header"));
                sender.sendMessage(MessagesManager.getMessage("commands.critical.migration-summary")
                    .replace("%from%", from.name())
                    .replace("%to%", to.name())
                    .replace("%overwrite%", String.valueOf(overwrite)));
                sender.sendMessage(MessagesManager.getMessage("commands.critical.confirm-within")
                    .replace("%seconds%", String.valueOf(CONFIRM_TTL_SECONDS))
                    .replace("%code%", pending.token()));
                return true;
            }

            sender.sendMessage(MessageColors.getMsgColor("&e[SepareWorldItems] Starting migration " + from + " -> " + to + " (overwrite=" + overwrite + ")..."));

            SepareWorldItems plugin = SepareWorldItems.getInstance();
            Runnable job = () -> {
                PlayerDataStore source = null;
                PlayerDataStore target = null;
                try {
                    source = StorageManager.createStore(plugin, from);
                    target = StorageManager.createStore(plugin, to);

                    // Safety backup with prefixed name
                    try {
                        File outDir = new File(plugin.getDataFolder(), "backups" + File.separator + "migrations");
                        String prefix = "migration_manual_" + from.name() + "_to_" + to.name();
                        boolean includeSnapshot = (from != StorageType.YAML && from != StorageType.SQLITE);
                        BackupManager.createOneTimeBackup(plugin, plugin.getDataFolder(), outDir, prefix, includeSnapshot, source, prefix);
                    } catch (Throwable ignored) {
                    }

                    StoreToStoreMigration.migrate(plugin, source, target, overwrite);
                    runSync(plugin, () -> sender.sendMessage(MessageColors.getMsgColor("&a[SepareWorldItems] Migration completed. Now set storage.type: " + to + " and restart.")));
                } catch (Throwable t) {
                    runSync(plugin, () -> sender.sendMessage(MessageColors.getMsgColor("&c[SepareWorldItems] Migration failed: " + t.getMessage())));
                } finally {
                    try {
                        if (source != null) source.close();
                    } catch (Throwable ignored) {
                    }
                    try {
                        if (target != null) target.close();
                    } catch (Throwable ignored) {
                    }
                }
            };

            if (plugin.isFolia()) {
                try {
                    plugin.getServer().getAsyncScheduler().runNow(plugin, task -> job.run());
                } catch (Throwable t) {
                    Bukkit.getScheduler().runTaskAsynchronously(plugin, job);
                }
            } else {
                Bukkit.getScheduler().runTaskAsynchronously(plugin, job);
            }

            return true;
        }

        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(CommandSender sender, Command command, String s, String[] args) {

        if (!sender.hasPermission("separeworlditems.admin")) {
            return List.of();
        }

        if (args.length == 1) {
            return List.of("reload", "backup", "backupforce", "migrate", "confirm", "cancel");
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("migrate")) {
            return storageTypes();
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("migrate")) {
            return storageTypes();
        }

        return List.of();
    }

    private static List<String> storageTypes() {
        List<String> out = new ArrayList<>();
        for (StorageType t : StorageType.values()) {
            out.add(t.name());
        }
        return out;
    }

    private static void runSync(SepareWorldItems plugin, Runnable run) {
        if (plugin.isFolia()) {
            plugin.getServer().getGlobalRegionScheduler().run(plugin, task -> run.run());
        } else {
            Bukkit.getScheduler().runTask(plugin, run);
        }
    }

    private static boolean isConfirmed(String[] args) {
        if (args == null || args.length == 0) {
            return false;
        }
        String last = args[args.length - 1];
        return "--confirmed".equalsIgnoreCase(last) || "confirm".equalsIgnoreCase(last);
    }
}
