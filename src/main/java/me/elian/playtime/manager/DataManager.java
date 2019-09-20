package me.elian.playtime.manager;

import com.google.gson.Gson;
import me.elian.playtime.PlaytimePro;
import me.elian.playtime.db.MySQL;
import me.elian.playtime.db.SQLDatabase;
import me.elian.playtime.db.SQLite;
import me.elian.playtime.object.OnlineTime;
import me.elian.playtime.object.SignHead;
import me.elian.playtime.object.TimeType;
import me.elian.playtime.object.TopListItem;
import me.elian.playtime.util.NameUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.AbstractMap.SimpleEntry;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class DataManager {

    private static DataManager instance = new DataManager();

    private SQLDatabase database;

    private Map<UUID, Integer> timesAllTime;
    private Map<UUID, Integer> timesMonthly;
    private Map<UUID, Integer> timesWeekly;

    /* this map contains players along with the time (millis) that they joined
     * this is used to make calculations, using the old player time, to get the current time the player has
     * this is updated each time the database is saved so that we can remove players who are no longer online
     * and save some memory
     */
    private Map<UUID, OnlineTime> playerJoins;

    public static DataManager getInstance() {
        return instance;
    }

    private DataManager() {
        timesAllTime = new ConcurrentHashMap<>();
        timesMonthly = new ConcurrentHashMap<>();
        timesWeekly = new ConcurrentHashMap<>();

        playerJoins = new ConcurrentHashMap<>();
    }

    /**
     * Called on the main thread
     * @param plugin PlaytimePro instance
     * @return true if database successfully loaded, false if not successfully loaded
     */
    public boolean registerDatabase(PlaytimePro plugin) {
        FileConfiguration config = ConfigManager.getConfig();

        String databaseType = config.getString("database-type");

        if (databaseType == null) {
            plugin.getLogger().severe("Invalid database type! Check your config. Plugin disabling.");
            return false;
        }

        if (databaseType.equalsIgnoreCase("sqlite")) {
            database = new SQLite(plugin);
            updateLocalStorage();

            plugin.getLogger().info("Loaded SQLite Database");

            return true;
        } else if (databaseType.equalsIgnoreCase("mysql")) {
            database = newMySQL(plugin);

            if (database == null)
                return false;

            if (!((MySQL) database).canConnect())
                return false;

            updateLocalStorage();

            plugin.getLogger().info("Loaded MySQL Database");

            return true;
        } else {
            plugin.getLogger().severe("Invalid database type! Check your config. Plugin disabling.");
            return false;
        }
    }

    // Called Sync
    public void updateLocalStorage() {
        try {
            Connection con = database.getConnection();
            Statement statement = con.createStatement();

            timesAllTime = database.getTimes(statement, TimeType.ALL_TIME);
            timesMonthly = database.getTimes(statement, TimeType.MONTHLY);
            timesWeekly = database.getTimes(statement, TimeType.WEEKLY);

            statement.close();
        } catch (SQLException e) {
            Bukkit.getLogger().log(Level.SEVERE, "[PlaytimePro] Error updating local times", e);
        }
    }

    // Called Async when migrateOld or when TopListUpdater run
    // It will fetch the data but then update the timeMaps on the main thread
    public void syncUpdateLocalStorage() {
        // Temporary Maps
        final Map<UUID, Integer> timesAllMap, timesMonthlyMap,timesWeeklyMap;

        try {
            Connection con = database.getConnection();
            Statement statement = con.createStatement();

            timesAllMap = database.getTimes(statement, TimeType.ALL_TIME);
            timesMonthlyMap = database.getTimes(statement, TimeType.MONTHLY);
            timesWeeklyMap = database.getTimes(statement, TimeType.WEEKLY);

            statement.close();
        } catch (SQLException e) {
            Bukkit.getLogger().log(Level.SEVERE, "[PlaytimePro] Error updating local times", e);
            return;
        }

        // Possible Issue: Maps are not cleared so at this point, RAM is going to be crowded
        // Switch the actual maps with the temporary maps on the main thread
        // This deals with concurrency issues
        PlaytimePro.executeSync(() -> {
            timesAllTime = timesAllMap;
            timesMonthly = timesMonthlyMap;
            timesWeekly = timesWeeklyMap;
        });
    }

    // Called Sync
    public void saveStorageToDatabase() {
        database.updateTimes(TimeType.ALL_TIME, playerJoins, timesAllTime);
        database.updateTimes(TimeType.MONTHLY, playerJoins, timesMonthly);
        database.updateTimes(TimeType.WEEKLY, playerJoins, timesWeekly);

        updatePlayerJoins();
        updateLocalStorage();
    }

    public void asyncSaveStorageToDatabase() {
        long startTime = System.currentTimeMillis(); // Save timing
        // Temporary Maps

        database.updateTimes(TimeType.ALL_TIME, playerJoins, timesAllTime);
        database.updateTimes(TimeType.MONTHLY, playerJoins, timesMonthly);
        database.updateTimes(TimeType.WEEKLY, playerJoins, timesWeekly);

        updatePlayerJoins();
        syncUpdateLocalStorage();

        // Display timing in console
        Bukkit.getLogger().info("[PlaytimePro] Playtime saving took " + (System.currentTimeMillis() - startTime) + "ms to complete!");
    }

    public List<TopListItem> getSortedTimes(String table, int minimumTime, AtomicLong totalHours) {
        return database.getSortedTimes(table, minimumTime, totalHours);
    }

    private void updatePlayerJoins() {
        long currentTime = System.currentTimeMillis();

        playerJoins.clear();

        for (Player p : Bukkit.getOnlinePlayers()) {
            playerJoins.put(p.getUniqueId(), new OnlineTime(currentTime));
        }
    }

    // Called Sync
    public void playerJoin(UUID id) {
        if (playerJoins.containsKey(id)) {
            playerJoins.get(id).login();
            return;
        }

        playerJoins.put(id, new OnlineTime());

    }

    // Called Sync
    public void playerLeave(UUID id) {
        if (playerJoins.containsKey(id)) playerJoins.get(id).handleLogout();
    }

    public void closeDatabase() {
        if (database == null)
            return;

        try {
            saveStorageToDatabase();
            database.getConnection().close();
        } catch (Exception ignored) {
        }
    }

    Map<UUID, Integer> getTimesAllTime() {
        return timesAllTime;
    }

    Map<UUID, Integer> getTimesMonthly() {
        return timesMonthly;
    }

    Map<UUID, Integer> getTimesWeekly() {
        return timesWeekly;
    }

    // Called Sync
    public int getTime(UUID player, TimeType type) {
        switch (type) {
            case ALL_TIME:
                return calculateSeconds(player)
                        + timesAllTime.getOrDefault(player, 0);
            case MONTHLY:
                return calculateSeconds(player)
                        + timesMonthly.getOrDefault(player, 0);
            case WEEKLY:
                return calculateSeconds(player)
                        + timesWeekly.getOrDefault(player, 0);
            default:
                return 0;
        }
    }

    private int calculateSeconds(UUID player) {
        final OnlineTime ot = playerJoins.get(player);

        if (ot == null) return 0;

        return (int) TimeUnit.MILLISECONDS.toSeconds(ot.getUnstoredPlaytime());
    }

    // Called Async
    public void migrateOld(PlaytimePro plugin) {
        File file = new File(plugin.getDataFolder(), "times.json");

        if (!file.exists()) {
            plugin.getLogger().warning("Could not migrate from old data. times.json file not found.");
            return;
        }

        try {
            Scanner scanner = new Scanner(file);

            plugin.getLogger().info(Messages.getString("migration_loading_old"));
            Map<String, Double> stringMap = new Gson().fromJson(scanner.nextLine(), Map.class);

            plugin.getLogger().info(Messages.getString("migration_mapping_players"));
            Map<UUID, Integer> times = stringMap.entrySet().stream()
                    .map(e -> new SimpleEntry<>(UUID.fromString(e.getKey()), e.getValue().intValue()))
                    .collect(Collectors.toMap(SimpleEntry::getKey, SimpleEntry::getValue));

            plugin.stopRunnables();

            plugin.getLogger().info(Messages.getString("migration_uploading_times"));
            database.updateTimesMigration(TimeType.ALL_TIME, times);

            Map<UUID, String> names = new HashMap<>();

            boolean limit = false;

            plugin.getLogger().info(Messages.getString("migration_updating_names"));

            for (UUID id : times.keySet()) {
                OfflinePlayer op = Bukkit.getOfflinePlayer(id);
                String name = op.getName();

                if (name == null && !limit) {
                    name = NameUtil.getNameByUniqueId(id);

                    if (name.equals("_playtime_not_found_")) {
                        name = null;
                    } else if (name.equals("_playtime_limit_reached_")) {
                        limit = true;
                        name = null;
                    }
                }

                if (name != null) {
                    names.put(id, name);
                }
            }

            database.updateNames(names);

            plugin.getLogger().info(Messages.getString("migration_completed"));

            syncUpdateLocalStorage();
            plugin.registerRunnables();

            scanner.close();
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Could not migrate from old data.", e);
        }
    }

    // Called Async
    public void migrateToOther(PlaytimePro plugin, Map<UUID, Integer> allTimeMap, Map<UUID, Integer> monthlyMap, Map<UUID, Integer> weeklyMap) {
        try {
            SQLDatabase other = database instanceof MySQL ? new SQLite(plugin) : newMySQL(plugin);

            if (other == null)
                return;

            if (other instanceof MySQL && !((MySQL) other).canConnect())
                return;

            other.updateTimes(TimeType.ALL_TIME, playerJoins, allTimeMap);
            other.updateTimes(TimeType.MONTHLY, playerJoins, monthlyMap);
            other.updateTimes(TimeType.WEEKLY, playerJoins, weeklyMap);

            Map<UUID, String> names = database.getNames();
            other.updateNames(names);

            // Help GC
            allTimeMap.clear();
            monthlyMap.clear();
            weeklyMap.clear();

            plugin.getLogger().info("Migration to other database completed.");
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Could not migrate to new database");
        }
    }

    public int purge(int time) {
        return database.purge(time);
    }

    private MySQL newMySQL(PlaytimePro plugin) {
        FileConfiguration config = ConfigManager.getConfig();

        String host = config.getString("mysql-host");
        int port = config.getInt("mysql-port");
        String databaseName = config.getString("mysql-database");
        String username = config.getString("mysql-username");
        String password = config.getString("mysql-password");

        if (host == null || port == 0 || databaseName == null || username == null || password == null) {
            plugin.getLogger().severe("Invalid MySQL database configuration! Check your config. Plugin disabling.");
            return null;
        }

        return new MySQL(plugin, host, port, databaseName, username, password);
    }

    public String getLastName(UUID id) {
        return database.getLastName(id);
    }

    public void setLastName(UUID id, String name) {
        database.setLastName(id, name);
    }

    public List<SignHead> getHeads() {
        return database.getHeads();
    }

    public void addHead(SignHead head) {
        database.addHead(head);
    }

    public void removeHead(SignHead head) {
        database.removeHead(head);
    }

    public Map<UUID, Integer> getSnapshotMap(TimeType type) {

        if (type.equals(TimeType.ALL_TIME)) {
            return new HashMap<>(timesAllTime);
        } else if (type.equals(TimeType.MONTHLY)) {
            return new HashMap<>(timesMonthly);
        } else {
            return new HashMap<>(timesWeekly);
        }
    }
}
