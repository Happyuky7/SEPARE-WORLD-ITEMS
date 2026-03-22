package com.github.happyuky7.separeWorldItems.listeners;

import com.github.happyuky7.separeWorldItems.SepareWorldItems;
import com.github.happyuky7.separeWorldItems.managers.MessagesManager;
import com.github.happyuky7.separeWorldItems.managers.PlayerDataManager;
import com.github.happyuky7.separeWorldItems.managers.PlayerDataSwitching;
import com.github.happyuky7.separeWorldItems.managers.PlayerContextState;
import com.github.happyuky7.separeWorldItems.managers.PlayerSwitchQueue;
import com.github.happyuky7.separeWorldItems.utils.MessageColors;
import com.github.happyuky7.separeWorldItems.listeners.MoveEvent;
import com.github.happyuky7.separeWorldItems.managers.integrations.WorldGuardManager;
import com.github.happyuky7.separeWorldItems.storage.PlayerDataScope;
import com.github.happyuky7.separeWorldItems.utils.PluginSchedulers;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import com.github.happyuky7.separeWorldItems.integrations.IntegrationEssentialsX;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class WorldChangeEvent implements Listener {

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {

        Player player = event.getPlayer();

        // If WorldGuard switching already handled a teleport/portal, avoid a second save/load here.
        if (SepareWorldItems.getInstance().getConfig().getBoolean("integrations.worldguard.enabled")) {
            if (MoveEvent.wasSwitchedRecently(player.getUniqueId(), 1500L)) {
                return;
            }

            // If the player is inside a configured WG region in the destination world, let MoveEvent manage it.
            String regionGroup = WorldGuardManager.getConfiguredRegionGroupAt(player.getLocation());
            if (regionGroup != null) {
                return;
            }
        }

        String default_group = SepareWorldItems.getInstance().getConfig()
                .getString("settings.options.default-group.group");

        String fromWorld = event.getFrom().getName();
        String toWorld = player.getWorld().getName();

        if (SepareWorldItems.getInstance().getConfig().getBoolean("settings.auto-configure-worlds", false)) {

            if (SepareWorldItems.getInstance().getConfig().getBoolean("settings.options.default-group.enabled",
                    false)) {

                if (SepareWorldItems.getInstance().getConfig().getString("worlds." + fromWorld) == null
                        || SepareWorldItems.getInstance().getConfig().getString("worlds." + fromWorld).isEmpty()) {

                    SepareWorldItems.getInstance().getConfig().set("worlds." + fromWorld, default_group);
                    SepareWorldItems.getInstance().getConfig().save();
                }

                if (SepareWorldItems.getInstance().getConfig().getString("worlds." + toWorld) == null
                        || SepareWorldItems.getInstance().getConfig().getString("worlds." + toWorld).isEmpty()) {

                    SepareWorldItems.getInstance().getConfig().set("worlds." + toWorld, default_group);
                    SepareWorldItems.getInstance().getConfig().save();
                }

            }
        }

        if (SepareWorldItems.getInstance().getConfig().getBoolean("integrations.essentialsx.enabled")) {
            if (SepareWorldItems.getInstance().getConfig().getBoolean("essentialsx.vanish-bypass")) {
                if (IntegrationEssentialsX.getPlayerVanishStatus(player)) {

                    IntegrationEssentialsX.setPlayerVanish(player, false);

                    if (SepareWorldItems.getInstance().getConfig().getBoolean("settings.debug")) {
                        SepareWorldItems.getInstance().getLogger().info("Player " + player.getName()
                                + " is in vanish, temporarily disabling it for world change.");
                    }

                    player.sendMessage(MessageColors
                            .getMsgColor(MessagesManager.getMessage("integration.essentialsx.vanish-deactivated")));

                    if (SepareWorldItems.getInstance().isFolia()) {

                        SepareWorldItems.getInstance().getServer().getGlobalRegionScheduler().runDelayed(
                                SepareWorldItems.getInstance(), (task) -> {
                                    IntegrationEssentialsX.setPlayerVanish(player, true);
                                    player.sendMessage(MessageColors.getMsgColor(
                                            MessagesManager.getMessage("integration.essentialsx.vanish-reactivated")));
                                }, 1L);

                    } else {

                        SepareWorldItems.getInstance().getServer().getScheduler()
                                .runTaskLater(SepareWorldItems.getInstance(), () -> {
                                    IntegrationEssentialsX.setPlayerVanish(player, true);
                                    player.sendMessage(MessageColors.getMsgColor(
                                            MessagesManager.getMessage("integration.essentialsx.vanish-reactivated")));
                                }, 1L);

                    }
                }
            }
        }

        if (SepareWorldItems.getInstance().getConfig().contains("worlds." + fromWorld)
                && SepareWorldItems.getInstance().getConfig().contains("worlds." + toWorld)) {

            String fromGroup = SepareWorldItems.getInstance().getConfig().getString("worlds." + fromWorld);
            String toGroup = SepareWorldItems.getInstance().getConfig().getString("worlds." + toWorld);

            SepareWorldItems plugin = SepareWorldItems.getInstance();
            UUID uuid = player.getUniqueId();

            PlayerContextState.Context fromCtx = new PlayerContextState.Context(false, fromGroup);
            PlayerContextState.Context toCtx = new PlayerContextState.Context(false, toGroup);

            // Align shared tracking so queued operations know the correct "from".
            PlayerContextState.set(uuid, fromCtx);

            PlayerSwitchQueue.enqueue(uuid, () -> {
                if (!player.isOnline()) {
                    return CompletableFuture.completedFuture(null);
                }

                PlayerContextState.Context tracked = PlayerContextState.get(uuid);
                PlayerContextState.Context actualFrom = tracked != null ? tracked : fromCtx;

                // If we're already in the desired context, skip.
                if (actualFrom.equals(toCtx)) {
                    return CompletableFuture.completedFuture(null);
                }

                CompletableFuture<Void> switchF = PlayerDataSwitching.switchContext(
                    plugin,
                    player,
                    actualFrom.worldGuardRegion ? PlayerDataScope.WORLDGUARD_GROUP : PlayerDataScope.WORLD_GROUP,
                    actualFrom.groupName,
                    PlayerDataScope.WORLD_GROUP,
                    toGroup
                );

                if (fromGroup.equals(toGroup)) {
                    // Preserve reload semantics (EXP reload) for same-group transitions.
                    switchF = switchF.thenCompose(v -> PlayerDataSwitching.reloadContext(
                        plugin,
                        player,
                        PlayerDataScope.WORLD_GROUP,
                        fromGroup
                    ));
                }

                return switchF
                    .handle((v, t) -> t)
                    .thenCompose(t -> PluginSchedulers.runSyncFuture(plugin, () -> {
                        if (t != null) {
                            plugin.getLogger().warning("[Switch] Failed for " + player.getName() + ": " + t.getMessage());
                            return;
                        }

                        PlayerContextState.set(uuid, toCtx);
                        MoveEvent.markSwitched(uuid);
                    }));
            });
        }
    }

}
