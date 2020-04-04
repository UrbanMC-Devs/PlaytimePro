package me.elian.playtime.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.pool.HikariPool;
import me.elian.playtime.PlaytimePro;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;

public class MySQL extends SQLDatabase {

    private final String database;

    private HikariDataSource hikariDS;

    public MySQL(String host, int port, String database, String user, String pass) {
        this.database = database;

        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setPoolName("PlaytimeProPool");
        hikariConfig.setUsername(user);
        hikariConfig.setPassword(pass);
        hikariConfig.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database);

        try {
            hikariDS = new HikariDataSource(hikariConfig);
        } catch (HikariPool.PoolInitializationException exception) {
            PlaytimePro.getInstance().getLogger().severe("Error connecting to SQL Database. Please make sure everything is configured properly.");
            hikariDS = null;
        }
    }

    @Override
    public boolean establishConnection() {
        return hikariDS != null && createDatabase(database) && createTables();
    }

    @Override
    public void closeConnection() {
        if (hikariDS != null)
            hikariDS.close();
    }

    private boolean createDatabase(String databaseName) {
        try (Connection con = getConnection()) {
            Statement statement = con.createStatement();

            String sql = "CREATE DATABASE IF NOT EXISTS " + databaseName;
            statement.execute(sql);

            statement.close();
        } catch (Exception e) {
            PlaytimePro.getInstance().getLogger().log(Level.SEVERE, "Could not connect to MySQL database!" + databaseName, e);
            return false;
        }

        return true;
    }

    public boolean canConnect() {
        return getConnection() != null;
    }

    @Override
    public Connection getConnection() {
        try {
            return hikariDS.getConnection();
        } catch (SQLException e) {
            PlaytimePro.getInstance().getLogger().log(Level.SEVERE, "Could not connect to MySQL database!", e);
        }

        return null;
    }

    @Override
    public String getSimpleName() {
        return "mysql";
    }
}
