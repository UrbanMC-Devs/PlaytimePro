package me.elian.playtime.db;

import me.elian.playtime.PlaytimePro;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;

public class MySQL extends SQLDatabase {

    private PlaytimePro plugin;
    private String host, database, user, pass;
    private int port;

    private Connection con;

    public MySQL(PlaytimePro plugin, String host, int port, String database, String user, String pass) {
        this.plugin = plugin;
        this.host = host;
        this.database = database;
        this.user = user;
        this.pass = pass;
        this.port = port;

        createDatabase(database);
        createTables();
    }

    private void createDatabase(String databaseName) {
        try {
            Class.forName("com.mysql.jdbc.Driver");
            con = DriverManager.getConnection("jdbc:mysql://" + host + ":" + port + "/", user, pass);

            Statement statement = con.createStatement();

            String sql = "CREATE DATABASE IF NOT EXISTS " + databaseName;
            statement.execute(sql);

            statement.close();
            con.close();
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Could not connect to MySQL database!" + databaseName, e);
        }
    }

    public boolean canConnect() {
        return getConnection() != null;
    }

    @Override
    public Connection getConnection() {
        try {
            if (con != null && !con.isClosed())
                return con;

            Class.forName("com.mysql.jdbc.Driver");
            con = DriverManager.getConnection("jdbc:mysql://" + host + ":" + port + "/" + database, user, pass);

            return con;
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not connect to MySQL database!", e);
        } catch (ClassNotFoundException e) {
            plugin.getLogger().log(Level.SEVERE, "You need the MySQL JBDC library. Google it. Put it in /lib folder.");
        }

        return null;
    }

    @Override
    public PlaytimePro getPlugin() {
        return plugin;
    }
}
