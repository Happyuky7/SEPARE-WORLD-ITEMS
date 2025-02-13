package com.github.happyuky7.separeWorldItems.listeners;

import com.github.happyuky7.separeWorldItems.SepareWorldItems;
import com.github.happyuky7.separeWorldItems.managers.PlayerDataManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;

public class WorldChangeEvent implements Listener {

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {

        Player player = event.getPlayer();

        String default_group = SepareWorldItems.getInstance().getConfig().getString("settings.options.default_group.default.group");

        String fromWorld = event.getFrom().getName();
        String toWorld = player.getWorld().getName();


        if (SepareWorldItems.getInstance().getConfig().getBoolean("settings.auto-configure-worlds", false)) {

            if (SepareWorldItems.getInstance().getConfig().getBoolean("settings.options.default_group.default.enabled", false)) {

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

        if (SepareWorldItems.getInstance().getConfig().contains("worlds." + fromWorld)
            && SepareWorldItems.getInstance().getConfig().contains("worlds." + toWorld)) {

            String fromGroup = SepareWorldItems.getInstance().getConfig().getString("worlds." + fromWorld);
            String toGroup = SepareWorldItems.getInstance().getConfig().getString("worlds." + toWorld);

            PlayerDataManager.save(player, fromGroup);

            if (!fromGroup.equals(toGroup)) {
                PlayerDataManager.load(player, toGroup);
            } else {
                //PlayerDataManager.load(player, fromGroup);
            }
        }
    }

}
