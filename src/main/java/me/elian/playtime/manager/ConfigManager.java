package me.elian.playtime.manager;

import me.elian.playtime.PlaytimePro;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.*;

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
                file.createNewFile();

                InputStream input = getClass().getClassLoader().getResourceAsStream("config.yml");
                OutputStream output = new FileOutputStream(file);

                PlaytimePro.copy(input, output);
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
