package com.github.happyuky7.separeWorldItems.integrations;

import com.github.happyuky7.separeWorldItems.SepareWorldItems;
import org.bukkit.entity.Player;
import com.earth2me.essentials.Essentials;

public class IntegrationEssentialsX {

    public static boolean getPlayerVanishStatus(Player player) {

        Essentials ess = (Essentials) SepareWorldItems.getInstance().getServer().getPluginManager().getPlugin("Essentials");
        if (ess != null) {
            return ess.getUser(player).isVanished();
        }

        return false;
    }

    public static void setPlayerVanish(Player player, boolean status) {

        Essentials ess = (Essentials) SepareWorldItems.getInstance().getServer().getPluginManager().getPlugin("Essentials");
        if (ess != null) {
            ess.getUser(player).setVanished(status);
        }
    }

}