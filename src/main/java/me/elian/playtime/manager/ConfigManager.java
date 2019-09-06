package me.elian.playtime.manager;

import me.elian.playtime.PlaytimePro;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class ConfigManager {

    private static ConfigManager instance = new ConfigManager();

    private final File file = new File("plugins/PlaytimePro", "config.yml");

    private YamlConfiguration config;

    public static ConfigManager getInstance() {
        return instance;
    }

    private ConfigManager() {
        createFile();
        loadConfig();
    }

    private void createFile() {
        if (!file.getParentFile().isDirectory()) {
            file.getParentFile().mkdir();
        }

        if (!file.exists()) {
            try {

                InputStream input = getClass().getClassLoader().getResourceAsStream("config.yml");

                Files.copy(input, file.toPath(), StandardCopyOption.REPLACE_EXISTING); // Use Files.copy
            } catch (IOException e) {
                Bukkit.getLogger().info("[PlaytimePro] Error creating config.yml");
                e.printStackTrace();
            }
        }
    }

    public void loadConfig() {
        try {
            config = YamlConfiguration.loadConfiguration(file);
        } catch (Exception e) {
            Bukkit.getLogger().info("[PlaytimePro] Error loading config.yml");
            e.printStackTrace();
        }
    }

    public static FileConfiguration getConfig() {
        return instance.config;
    }

    public void reload() {
        createFile();
        loadConfig();
    }
}
