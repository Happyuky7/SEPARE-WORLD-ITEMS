package com.github.happyuky7.separeWorldItems.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;

import com.github.happyuky7.separeWorldItems.utils.MessageColors;

import org.bukkit.entity.Player;

public class JoinEvent {
    

    @EventHandler
    public void OnJoin(PlayerJoinEvent event) {

        Player player = event.getPlayer();

        if (player.isOp() || player.hasPermission("separeworlditems.notify")) {


            player.sendMessage(MessageColors.getMsgColor("&r "));
            player.sendMessage(MessageColors.getMsgColor("&r "));
            player.sendMessage(MessageColors.getMsgColor("&r "));
            player.sendMessage(MessageColors.getMsgColor("&r "));
            player.sendMessage(MessageColors.getMsgColor("&r "));
            player.sendMessage(MessageColors.getMsgColor("&r "));
            player.sendMessage(MessageColors.getMsgColor("&r "));

        }

    }

}
