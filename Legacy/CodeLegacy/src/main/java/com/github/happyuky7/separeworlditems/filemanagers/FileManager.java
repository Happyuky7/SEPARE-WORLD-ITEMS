package com.github.happyuky7.separeworlditems.filemanagers;

/*
   Code by: Happyuky7
   Github: https://github.com/Happyuky7
   License: MIT
   Link: https://github.com/Happyuky7/FileManagerBukkit1
*/

/*
   FileManager Link: https://github.com/Happyuky7/FileManagerBukkit1
*/

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

/**
 * A custom file manager class to handle YAML configuration files.
 * Extends {@link YamlConfiguration} to provide additional utility methods for
 * configuration management.
 */
public class FileManager extends YamlConfiguration {

    private final String fileName;
    private final JavaPlugin plugin;
    private final File folder;

    /**
     * Constructs a FileManager with the specified plugin, file name, and folder.
     *
     * @param plugin   the plugin instance
     * @param fileName the name of the file (with or without the extension)
     * @param folder   the folder where the file will be stored
     */
    public FileManager(JavaPlugin plugin, String fileName, File folder) {
        this(plugin, fileName, ".yml", folder);
    }

    /**
     * Constructs a FileManager with a custom file extension.
     *
     * @param plugin        the plugin instance
     * @param fileName      the name of the file (without the extension)
     * @param fileExtension the file extension (e.g., ".yml")
     * @param folder        the folder where the file will be stored
     */
    public FileManager(JavaPlugin plugin, String fileName, String fileExtension, File folder) {
        this.folder = folder;
        this.plugin = plugin;
        this.fileName = fileName + (fileName.endsWith(fileExtension) ? "" : fileExtension);
        createFile();
    }

    /**
     * Constructs a FileManager using the default plugin data folder and ".yml"
     * extension.
     *
     * @param plugin   the plugin instance
     * @param fileName the name of the file
     */
    public FileManager(JavaPlugin plugin, String fileName) {
        this(plugin, fileName, ".yml");
    }

    /**
     * Constructs a FileManager using the default plugin data folder with a custom
     * file extension.
     *
     * @param plugin        the plugin instance
     * @param fileName      the name of the file (without the extension)
     * @param fileExtension the file extension
     */
    public FileManager(JavaPlugin plugin, String fileName, String fileExtension) {
        this(plugin, fileName, fileExtension, plugin.getDataFolder());
    }

    /**
     * Returns the plugin instance associated with this FileManager.
     *
     * @return the plugin instance
     */
    public JavaPlugin getPlugin() {
        return this.plugin;
    }

    /**
     * Retrieves a configuration value as the specified type.
     *
     * @param <T>   the type to cast the value to
     * @param clazz the class of the desired type
     * @param path  the path to the value in the configuration
     * @return the value cast to the specified type
     */
    public <T> T get(Class<T> clazz, String path) {
        Object value = get(path);
        return clazz.cast(value);
    }

    /**
     * Creates the configuration file if it doesn't exist.
     * If a resource file with the same name exists in the plugin jar, it will be
     * copied.
     */
    private void createFile() {
        File file = new File(this.folder, this.fileName);
        try {
            if (!file.exists()) {
                if (this.plugin.getResource(this.fileName) != null) {
                    this.plugin.saveResource(this.fileName, false);
                } else {
                    save(file);
                }
            }
            load(file);
        } catch (Exception ex) {
            logSevereError("An error occurred while creating the file.", ex);
        }
    }

    /**
     * Saves the current configuration to the file.
     */
    public void save() {
        File file = new File(this.folder, this.fileName);
        try {
            save(file);
        } catch (IOException ex) {
            logSevereError("An error occurred while saving the file.", ex);
        }
    }

    /**
     * Reloads the configuration file.
     */
    public void reload() {
        File file = new File(this.folder, this.fileName);
        try {
            load(file);
        } catch (IOException | org.bukkit.configuration.InvalidConfigurationException ex) {
            logSevereError("An error occurred while reloading the file.", ex);
        }
    }

    /**
     * Logs a severe error to the plugin logger.
     *
     * @param message the error message
     * @param ex      the exception that was thrown
     */
    private void logSevereError(String message, Exception ex) {
        this.plugin.getLogger().log(Level.SEVERE, message);
        this.plugin.getLogger().log(Level.SEVERE, "Error: " + ex.getMessage());
    }

    /* You can remove this if you are using versions higher than 1.16,
     soon I will implement a new improved FileManager stay tuned to the
     Github repository: https://github.com/Happyuky7/FileManagerBukkit1
     START | Use only for legacy colors i.e. below Minecraft version 1.16.X
    /**
     * Retrieves a string from the configuration and applies color codes.
     *
     * @param path the path to the string in the configuration
     * @return the colored string, or the path itself if not found
     *
    public String getColouredString(String path) {
        String value = getString(path);
        return ChatColor.translateAlternateColorCodes('&', value != null ? value : path);
    }

    /**
     * Retrieves a list of strings from the configuration and applies color codes to
     * each entry.
     *
     * @param path the path to the string list in the configuration
     * @return a list of colored strings
     *
    public List<String> getColouredStringList(String path) {
        List<String> originalList = getStringList(path);
        List<String> coloredList = new ArrayList<>();
        for (String line : originalList) {
            coloredList.add(ChatColor.translateAlternateColorCodes('&', line));
        }
        return coloredList;
    }*/
}
