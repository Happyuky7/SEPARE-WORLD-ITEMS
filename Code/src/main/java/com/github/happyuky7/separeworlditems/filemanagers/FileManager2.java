package com.github.happyuky7.separeworlditems.filemanagers;

/*
 * Code by: Happyuky7
 * Github: https://github.com/Happyuky7
 * License: Custom
 * Link: https://github.com/Happyuky7/SEPARE-WORLD-ITEMS
 */

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public class FileManager2 {

    public static FileConfiguration getYaml(File f) {
        return (FileConfiguration) YamlConfiguration.loadConfiguration(f);
    }

    public static void saveConfiguraton(File f, FileConfiguration fc) {
        try {
            fc.save(f);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
