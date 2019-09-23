package me.elian.playtime.manager;

import org.bukkit.Bukkit;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class ConfigManager {

    private static String DATABASE_TYPE;
    private static String MYSQL_HOST, MYSQL_DATABASE, MYSQL_USERNAME, MYSQL_PASSWORD;
    private static int MYSQL_PORT,
            TOP_MIN_HOURS,
            TOP_MIN_FETCH_HOURS,
            TOP_INTERVAL,
            HEADS_INTERVAL,
            SAVE_INTERVAL;

    private final File file = new File("plugins/PlaytimePro", "config.yml");

    private ConfigManager() {
        createFile();

        Configuration config;

        try {
            config = YamlConfiguration.loadConfiguration(file);
        } catch (Exception e) {
            Bukkit.getLogger().info("[PlaytimePro] Error loading config.yml");
            e.printStackTrace();
            return;
        }

        DATABASE_TYPE = config.getString("database-type", "sqlite");

        MYSQL_HOST = config.getString("mysql-host", "host");
        MYSQL_PORT = config.getInt("mysql-port", 3306);
        MYSQL_DATABASE = config.getString("mysql-database", "playtime");
        MYSQL_USERNAME = config.getString("mysql-username", "user");
        MYSQL_PASSWORD = config.getString("mysql-password", "pass");

        TOP_MIN_HOURS = config.getInt("top-list-minimum-hours", 5);
        TOP_MIN_FETCH_HOURS = config.getInt("top-list-fetch-minimum-hours", 50);
        TOP_INTERVAL = config.getInt("update-top-list", 120);
        HEADS_INTERVAL = config.getInt("update-heads", 120);
        SAVE_INTERVAL = config.getInt("save-database", 120);
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

    public static String getDatabaseType() { return DATABASE_TYPE; }

    public static String getMySQLHost() { return MYSQL_HOST; }

    public static int getMySQLPort() { return MYSQL_PORT; }

    public static String getMySQLDatabase() { return MYSQL_DATABASE; }

    public static String getMySQLUsername() { return MYSQL_USERNAME; }

    public static String getMySQLPassword() { return MYSQL_PASSWORD; }

    public static int getTopMinimumHours() { return TOP_MIN_HOURS; }

    public static int getTopMinimumFetchHours() { return TOP_MIN_FETCH_HOURS; }

    public static int getTopUpdateInterval() { return TOP_INTERVAL; }

    public static int getHeadsUpdateInterval() { return HEADS_INTERVAL; }

    public static int getSaveInterval() { return SAVE_INTERVAL; }

    public static void reload() {
        new ConfigManager();
    }
}
