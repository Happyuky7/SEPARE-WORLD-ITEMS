package com.github.happyuky7.separeWorldItems.managers;

import com.github.happyuky7.separeWorldItems.SepareWorldItems;
import com.github.happyuky7.separeWorldItems.utils.MessageColors;

import java.util.List;

public class MessagesManager {

    // Get the message from the language file
    public static String getMessage(String path) {
        String message = SepareWorldItems.getInstance().getLangs().getString(path);

        if (message == null) {
            return logErrorPath(path);
        }

        return MessageColors.getMsgColor(
                message
                        .replace("%prefix%", SepareWorldItems.getInstance().getLangs().getString("prefix"))
                        .replace("%version%", SepareWorldItems.getInstance().getDescription().getVersion())
                        .replace("%author%", SepareWorldItems.getInstance().getDescription().getAuthors().get(0))

        );
    }

    // Get the message list from the language file
    public static String getMessageList(String path) {
        List<String> messages = SepareWorldItems.getInstance().getLangs().getStringList(path);

        if (messages == null) {
            return logErrorPath(path);
        }

        // Corregir aquí el reemplazo para cada mensaje
        for (int i = 0; i < messages.size(); i++) {
            String message = messages.get(i);
            messages.set(i, MessageColors.getMsgColor(
                    message
                            .replace("%prefix%", SepareWorldItems.getInstance().getLangs().getString("prefix"))
                            .replace("%version%", SepareWorldItems.getInstance().getDescription().getVersion())
                            .replace("%author%", SepareWorldItems.getInstance().getDescription().getAuthors().get(0))
            ));
        }

        return String.join("\n", messages);
    }

    // Get the message from the language file
    public static String logErrorPath(String path) {
        System.out.println("[SepareWorldItems] [ERROR] [MSG] " + path + " is not found in the language file.");
        return MessageColors.getMsgColor("&8[&aSepareWorldItems&8] &c[ERROR] &7(&f" + path + "&7) &cis not found in the language file.");
    }

}
