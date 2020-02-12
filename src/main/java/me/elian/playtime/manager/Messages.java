package me.elian.playtime.manager;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.MessageFormat;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;

public class Messages {

    private static final Messages instance = new Messages();

    private final File FILE = new File("plugins/PlaytimePro", "messages.properties");

    private ResourceBundle bundle;

    private Messages() {
        createFile();
        loadBundle();
    }

    public static Messages getInstance() {
        return instance;
    }

    public static String getString(String key, Object... args) {
        return instance.getStringFromBundle(key, args);
    }

    private void createFile() {
        if (!FILE.getParentFile().isDirectory()) {
            FILE.getParentFile().mkdir();
        }

        if (!FILE.exists()) {
            try {
                InputStream input = getClass().getClassLoader().getResourceAsStream("messages.properties");

                Files.copy(input, FILE.toPath(), StandardCopyOption.REPLACE_EXISTING); // Use file copy method
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void loadBundle() {
        try {
            InputStream input = new FileInputStream(FILE);
            Reader reader = new InputStreamReader(input, StandardCharsets.UTF_8);

            bundle = new PropertyResourceBundle(reader);
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String getStringFromBundle(String key, Object... args) {
        try {
            return format(bundle.getString(key), true, args);
        } catch (Exception e) {
            Bukkit.getLogger().severe("[ezEconomy] Missing message in message.properties! Message key: " + key);
            return key;
        }
    }

    private String format(String message, boolean color, Object... args) {
        try {
            message = message.replace("{prefix}", bundle.getString("prefix"));
        } catch (Exception ignored) {
        }

        if (args != null) {
            message = MessageFormat.format(message, args);
        }

        if (color) {
            message = ChatColor.translateAlternateColorCodes('&', message);
        }

        return message;
    }

    public void reload() {
        createFile();
        loadBundle();
    }
}
