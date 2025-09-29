package com.github.happyuky7.separeWorldItems.listeners;

import com.github.happyuky7.separeWorldItems.SepareWorldItems;
import com.github.happyuky7.separeWorldItems.managers.MessagesManager;
import com.github.happyuky7.separeWorldItems.managers.PlayerDataManager;
import com.github.happyuky7.separeWorldItems.utils.MessageColors;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import com.github.happyuky7.separeWorldItems.integrations.IntegrationEssentialsX;

public class WorldChangeEvent implements Listener {

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {

        Player player = event.getPlayer();

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

            PlayerDataManager.save(player, fromGroup);

            if (!fromGroup.equals(toGroup)) {
                PlayerDataManager.load(player, toGroup);
            } else {
                // PlayerDataManager.load(player, fromGroup);
                PlayerDataManager.reloadAllPlayerData(player, fromGroup);
            }
        }
    }

}
