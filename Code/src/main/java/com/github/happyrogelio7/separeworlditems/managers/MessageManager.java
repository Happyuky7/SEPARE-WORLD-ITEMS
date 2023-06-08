package com.github.happyrogelio7.separeworlditems.managers;

import com.github.happyrogelio7.separeworlditems.SepareWorldItems;
import com.github.happyrogelio7.separeworlditems.utils.MessageColors;


/*
 * Code by: HappyRogelio7
 * Github: https://github.com/happyrogelio7/
 * License: Custom
 * Link: https://github.com/HappyRogelio7/SEPARE-WORLD-ITEMS
 */

public class MessageManager {


    // Get Message form file language.
    public static String getMessage(String path) {

        if (SepareWorldItems.getInstance().getLangs().getString(path) == null) {
            return MessageColors.getMsgColor("&cError: &7The path &b" + path + " &7does not exist in the file: &b"
                    + SepareWorldItems.getInstance().getConfig().getString("experimental.lang") + ".yml&7.");
        }

        return MessageColors.getMsgColor(SepareWorldItems.getInstance().getLangs().getString(path));


    }


}
