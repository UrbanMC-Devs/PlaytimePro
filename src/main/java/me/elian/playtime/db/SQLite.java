package me.elian.playtime.db;

import me.elian.playtime.PlaytimePro;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Level;

public class SQLite extends SQLDatabase {

    private PlaytimePro plugin;
    private final File file;

    private Connection con;

    public SQLite(PlaytimePro plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "database.db");

        createFile();
        createTables();
    }

    private void createFile() {
        if (!file.getParentFile().isDirectory())
            file.getParentFile().mkdir();

        if (!file.isFile()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not create database file.", e);
            }
        }
    }

    @Override
    public Connection getConnection() {
        try {
            if (con != null && !con.isClosed())
                return con;

            Class.forName("org.sqlite.JDBC");
            con = DriverManager.getConnection("jdbc:sqlite:" + file);

            return con;
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, "SQLite exception on initialize", ex);
        } catch (ClassNotFoundException ex) {
            plugin.getLogger().log(Level.SEVERE, "You need the SQLite JBDC library. Google it. Put it in /lib folder.");
        }

        return null;
    }

    @Override
    public PlaytimePro getPlugin() {
        return plugin;
    }
}
