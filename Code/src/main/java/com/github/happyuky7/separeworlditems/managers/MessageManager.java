package com.github.happyuky7.separeworlditems.managers;

import com.github.happyuky7.separeworlditems.SepareWorldItems;
import com.github.happyuky7.separeworlditems.utils.MessageColors;

/*
 * Code by: HappyRogelio7
 * GitHub: https://github.com/Happyuky7/
 * License: Custom
 * Link: https://github.com/Happyuky7/SEPARE-WORLD-ITEMS
 */

public class MessageManager {

    /**
     * Retrieves a message from the language file.
     *
     * @param path the path to the message in the language file
     * @return the formatted message, or an error message if the path doesn't exist
     */
    public static String getMessage(String path) {
        String message = SepareWorldItems.getInstance().getLangs().getString(path);

        if (message == null) {
            return buildErrorMessage(path);
        }

        return MessageColors.getMsgColor(message);
    }

    /**
     * Builds an error message when a path does not exist in the language file.
     *
     * @param path the path that doesn't exist
     * @return the formatted error message
     */
    private static String buildErrorMessage(String path) {
        String langFile = SepareWorldItems.getInstance().getConfig().getString("experimental.lang");
        return MessageColors.getMsgColor("&cError: &7The path &b" + path + " &7does not exist in the file: &b" + langFile + ".yml&7.");
    }
}
