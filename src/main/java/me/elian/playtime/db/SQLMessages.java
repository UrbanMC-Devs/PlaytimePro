package me.elian.playtime.db;

import java.io.*;
import java.text.MessageFormat;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;

public class SQLMessages {

    private static SQLMessages instance = new SQLMessages();

    private ResourceBundle bundle;

    private SQLMessages() {
        loadBundle();
    }

    private void loadBundle() {
        try {
            InputStream input = getClass().getClassLoader().getResourceAsStream("sql.properties");
            Reader reader = new InputStreamReader(input, "UTF-8");

            bundle = new PropertyResourceBundle(reader);
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static String get(String key, Object... args) {
        try {
            return instance.format(instance.bundle.getString(key), args);
        } catch (Exception e) {
            return key;
        }
    }

    private String format(String message, Object... args) {
        if (args != null) {
            message = MessageFormat.format(message, args);
        }

        return message;
    }
}
