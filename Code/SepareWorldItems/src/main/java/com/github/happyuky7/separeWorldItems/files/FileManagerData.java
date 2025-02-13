package com.github.happyuky7.separeWorldItems.files;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FileManagerData {

    public static FileConfiguration getYaml(File file) {
        return YamlConfiguration.loadConfiguration(file);
    }

    public static void saveConfiguration(File file, FileConfiguration configuration) {
        try {
            configuration.save(file);
        } catch (IOException e) {
            Logger.getLogger(FileManagerData.class.getName()).log(Level.SEVERE,
                    "An error occurred while saving the configuration file: " + file.getName(), e);
        }
    }

}
