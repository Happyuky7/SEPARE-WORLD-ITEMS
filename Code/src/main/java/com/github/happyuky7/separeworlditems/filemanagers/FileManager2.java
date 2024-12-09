package com.github.happyuky7.separeworlditems.filemanagers;

/*
 * Code by: Happyuky7
 * GitHub: https://github.com/Happyuky7
 * License: Custom
 * Link: https://github.com/Happyuky7/SEPARE-WORLD-ITEMS
 */

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A utility class for managing YAML configuration files.
 * Provides methods to load and save configurations to/from files.
 */
public class FileManager2 {

    /**
     * Loads a YAML configuration file into a {@link FileConfiguration}.
     *
     * @param file the file to load
     * @return the loaded {@link FileConfiguration} instance
     */
    public static FileConfiguration getYaml(File file) {
        return YamlConfiguration.loadConfiguration(file);
    }

    /**
     * Saves a {@link FileConfiguration} to a file.
     *
     * @param file          the file to save the configuration to
     * @param configuration the {@link FileConfiguration} to save
     */
    public static void saveConfiguration(File file, FileConfiguration configuration) {
        try {
            configuration.save(file);
        } catch (IOException e) {
            Logger.getLogger(FileManager2.class.getName()).log(Level.SEVERE,
                    "An error occurred while saving the configuration file: " + file.getName(), e);
        }
    }
}
