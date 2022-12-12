package com.github.happyrogelio7.separeworlditems.managers;

import com.github.happyrogelio7.separeworlditems.SepareWorldItems;
import com.github.happyrogelio7.separeworlditems.filemanagers.FileManager2;
import com.github.happyrogelio7.separeworlditems.utils.MessageColors;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;


/*
 * Code by: HappyRogelio7
 * Github: https://github.com/happyrogelio7/
 * License: Custom
 * Link: https://github.com/HappyRogelio7/SEPARE-WORLD-ITEMS
 */

public class MessageManager {


    // Get Message form file language.
    public static String getMessage(String path) {

        String lang = SepareWorldItems.getInstance().getConfig().getString("general.lang");

        return MessageColors.getMsgColor(MessageManager.getFileLang(lang.toLowerCase(), path));
    }

    // Get File Language.
    public static String getFileLang(String lang, String path) {

        File f = new File(SepareWorldItems.getInstance().getDataFolder(), "langs/" + lang.toLowerCase() + ".yml");
        FileConfiguration fc = FileManager2.getYaml(f);

        return fc.getString(path);
    }


}
