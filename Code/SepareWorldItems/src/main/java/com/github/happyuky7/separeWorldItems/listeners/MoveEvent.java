package com.github.happyuky7.separeWorldItems.listeners;

import com.github.happyuky7.separeWorldItems.SepareWorldItems;
import com.github.happyuky7.separeWorldItems.managers.integrations.PlayerDataManagerWG;
import com.github.happyuky7.separeWorldItems.managers.integrations.WorldGuardManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

public class MoveEvent implements Listener {

    private static String lastRegion = "";

    @EventHandler
    public void onMove(PlayerMoveEvent event) {


        if (event.getFrom().getBlockX() == event.getTo().getBlockX() 
            && event.getFrom().getBlockY() == event.getTo().getBlockY() 
            && event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }

        Player player = event.getPlayer();
        String currentRegion = WorldGuardManager.getRegion(player);
        String currentGroupName = WorldGuardManager.getGroupName(player);

        if (currentRegion != null && !currentRegion.equals(lastRegion)) {
            
            if (currentGroupName != null && !currentGroupName.isEmpty()) {
                
                if (!lastRegion.isEmpty()) {
                    String previousGroupName = WorldGuardManager.getGroupNameByRegion(lastRegion);
                    if (previousGroupName != null && !previousGroupName.equals(currentGroupName)) {
                        PlayerDataManagerWG.save(player, previousGroupName);
                    }
                }

                PlayerDataManagerWG.load(player, currentGroupName);

                if (SepareWorldItems.getInstance().getConfig().getBoolean("settings.debug")) {
                    SepareWorldItems.getInstance().getLogger().info(
                        "Player " + player.getName() + " moved to region '" + currentRegion 
                        + "' with group '" + currentGroupName + "'"
                    );
                }
            }

            lastRegion = currentRegion;
        }
    }
}
