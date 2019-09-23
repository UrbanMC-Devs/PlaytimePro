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
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.AbstractMap.SimpleEntry;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class DataManager {

    private static DataManager instance = new DataManager();

    private SQLDatabase database;

    private Map<UUID, Integer> timesAllTime;
    private Map<UUID, Integer> timesMonthly;
    private Map<UUID, Integer> timesWeekly;

    private AtomicInteger localUpdateIterations = new AtomicInteger(0);

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
        String databaseType = ConfigManager.getDatabaseType();

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
    public void  updateLocalStorage() {
        try {
            Connection con = database.getConnection();
            Statement statement = con.createStatement();

            // Help GC
            timesAllTime.clear();
            timesMonthly.clear();
            timesWeekly.clear();

            database.getTimes(statement, TimeType.ALL_TIME, timesAllTime);
            database.getTimes(statement, TimeType.MONTHLY, timesMonthly);
            database.getTimes(statement, TimeType.WEEKLY, timesWeekly);

            statement.close();
        } catch (SQLException e) {
            Bukkit.getLogger().log(Level.SEVERE, "[PlaytimePro] Error updating local times", e);
        }
    }

    // Called Async/Sync
    public void saveStorageToDatabase() {
        long startTime = System.currentTimeMillis(); // Save timing

        database.updateTimes(TimeType.ALL_TIME, playerJoins, timesAllTime);
        database.updateTimes(TimeType.MONTHLY, playerJoins, timesMonthly);
        database.updateTimes(TimeType.WEEKLY, playerJoins, timesWeekly);

        updatePlayerJoins();

        // This atomic integer is used to mark the number of iterations
        // Once there have been 3 iterations of the save, the time maps will update and fill the data from the DB
        if (localUpdateIterations.get() == 3) {
            updateLocalStorage();
            localUpdateIterations.set(0);
        }

        localUpdateIterations.addAndGet(1);

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

            plugin.getLogger().info(Messages.getString("migration_updating_names"));

            // Use Silverwolfg11's UUIDMap plugin (which is already used for TownyNameUpdater and XenLink) to get
            // the names from UUID from players who have already joined the server async.
            // We cannot use Bukkit.getOfflinePlayer because it is not thread safe!
            if (PlaytimePro.getUUIDMapDependency() != null) {
                PlaytimePro.getUUIDMapDependency().getNameFromUUID(times.keySet()).thenAcceptAsync(map -> {
                    if (!map.isEmpty())
                        database.updateNames(map);
                });
            }

            plugin.getLogger().info(Messages.getString("migration_completed"));

            updateLocalStorage();
            plugin.registerRunnables();

            scanner.close();
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Could not migrate from old data.", e);
        }
    }

    // Called Async
    public void migrateToOther(PlaytimePro plugin) {
        try {
            SQLDatabase other = database instanceof MySQL ? new SQLite(plugin) : newMySQL(plugin);

            if (other == null)
                return;

            if (other instanceof MySQL && !((MySQL) other).canConnect())
                return;

            other.updateTimes(TimeType.ALL_TIME, playerJoins, timesAllTime);
            other.updateTimes(TimeType.MONTHLY, playerJoins, timesMonthly);
            other.updateTimes(TimeType.WEEKLY, playerJoins, timesWeekly);


            plugin.getLogger().info("Migration to other database completed.");
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Could not migrate to new database");
        }
    }

    public int purge(int time) {
        return database.purge(time);
    }

    private MySQL newMySQL(PlaytimePro plugin) {
        String host = ConfigManager.getMySQLHost();
        int port = ConfigManager.getMySQLPort();
        String databaseName = ConfigManager.getMySQLDatabase();
        String username = ConfigManager.getMySQLUsername();
        String password = ConfigManager.getMySQLPassword();

        if (host == null || port == 0 || databaseName == null || username == null || password == null) {
            plugin.getLogger().severe("Invalid MySQL database configuration! Check your config. Plugin disabling.");
            return null;
        }

        return new MySQL(plugin, host, port, databaseName, username, password);
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

    public boolean updateNullNames() {
        return database.updateNullNames();
    }
}
