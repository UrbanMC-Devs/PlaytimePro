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
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class DataManager {

    private static final DataManager instance = new DataManager();

    private SQLDatabase database;

    /* This map contains players along with the time (millis) that they joined
     * this is used to make calculations, using the old player time, to get the current time the player has
     * this is updated each time the database is saved so that we can remove players who are no longer online
     * and save some memory
     */
    private final Map<UUID, OnlineTime> playerJoins;

    public static DataManager getInstance() {
        return instance;
    }

    private DataManager() {
        playerJoins = new ConcurrentHashMap<>();
    }

    private Logger getLogger() {
        return PlaytimePro.getInstance().getLogger();
    }

    /**
     * Called on the main thread
     * @return true if database successfully loaded, false if not successfully loaded
     */
    public boolean registerDatabase() {
        String databaseType = ConfigManager.getDatabaseType();

        if (databaseType == null) {
            getLogger().severe("Invalid database type! Check your config. Plugin disabling.");
            return false;
        }

        if (databaseType.equalsIgnoreCase("sqlite")) {
            database = new SQLite();

            getLogger().info("Loaded SQLite Database");
        } else if (databaseType.equalsIgnoreCase("mysql")) {
            database = newMySQL();

            if (database == null)
                return false;

            getLogger().info("Loaded MySQL Database");
        } else {
            getLogger().severe("Invalid database type! Check your config. Plugin disabling.");
            return false;
        }

        if(!database.establishConnection()) {
            getLogger().severe("Could not establish a valid connection to the database! Disabling...");
            return false;
        }

        getLogger().info("Successfully established a connection to the database!");
        return true;
    }

    // Called Async/Sync
    public void saveStorageToDatabase() {
        long startTime = System.currentTimeMillis(); // Time saves

        DateManager.getInstance().updateTime();

        final Map<UUID, Integer> localPlaytimes = getRecentPlaytimes();

        database.updateTimes(localPlaytimes);

        filterOfflinePlayers(localPlaytimes.keySet());

        // Clean GC
        localPlaytimes.clear();

        // Display timing in debug
        PlaytimePro.debug("Updating playtime database took " + (System.currentTimeMillis() - startTime) + "ms to complete!");
    }

    public List<TopListItem> getSortedTimes(String table, int minimumTime, AtomicLong totalHours) {
        return database.getSortedTimes(table, minimumTime, totalHours);
    }

    private void filterOfflinePlayers(Set<UUID> storedPlayers) {
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            storedPlayers.remove(onlinePlayer.getUniqueId());
        }

        for (UUID offlinePlayer : storedPlayers) {
            playerJoins.remove(offlinePlayer);
        }
    }

    private Map<UUID, Integer> getRecentPlaytimes() {
        Map<UUID,Integer> localPlaytimeMap = new HashMap<>((playerJoins.size() * 4) / 3);

        long currentTime = System.currentTimeMillis();

        for (Map.Entry<UUID, OnlineTime> entry : playerJoins.entrySet()) {
            localPlaytimeMap.put(entry.getKey(), entry.getValue().returnAndReset(currentTime));
        }

        return localPlaytimeMap;
    }

    // Called Sync
    public void playerJoin(UUID id, String lastName) {
        OnlineTime onlineTime = playerJoins.get(id);

        if (onlineTime != null) {
            PlaytimePro.debug(lastName + " logged in. OnlineTime already exists.");
            onlineTime.login();
        }
        else {
            onlineTime =  new OnlineTime();
            playerJoins.put(id, onlineTime);
            PlaytimePro.debug(lastName + " logged in. Created new online time. Requesting playtime from DB...");
            fetchAllPlaytimes(id, lastName, onlineTime);
        }
    }

    // Only called if online time is not already cached
    private void fetchAllPlaytimes(final UUID player, final String lastName, final OnlineTime cache) {
        Bukkit.getScheduler().runTaskAsynchronously(PlaytimePro.getInstance(),
                () -> database.localFillTime(player, lastName, cache));
    }

    // Called Sync
    public void playerLeave(UUID id) {
        OnlineTime onlineTime = playerJoins.get(id);

        if (onlineTime != null) {
            PlaytimePro.debug("Marking logout for " + id);
            onlineTime.handleLogout();
        }
        else {
            PlaytimePro.debug("No cached time for logout of " + id);
        }
    }

    public void closeDatabase() {
        if (database == null)
            return;

        try {
            saveStorageToDatabase();
            database.closeConnection();
        } catch (Exception ignored) {
        }
    }

    // Must be called async. Returns error as -1
    public int getOfflineTime(UUID player, TimeType type) {
        try (Connection con = database.getConnection()) {
            if (con == null)
                return -1;

            Statement stmt = con.createStatement();
            return database.getTime(stmt, type, player);
        } catch (SQLException ex) {
            getLogger().log(Level.SEVERE, "SQL Exception!", ex);
            return -1;
        }
    }

    // Called Sync
    public int getOnlineTime(UUID player, TimeType type) {
        final OnlineTime ot = playerJoins.get(player);

        if (ot == null)
            return 0;

        int localPlaytime = (int) TimeUnit.MILLISECONDS.toSeconds(ot.getOnlinePlaytime());

        int playTime = 0;

        if (type == TimeType.ALL_TIME)
            playTime = ot.getAllTime();
        else if (type == TimeType.MONTHLY)
            playTime = ot.getMonthlyTime();
        else if (type == TimeType.WEEKLY)
            playTime = ot.getWeeklyTime();

        PlaytimePro.debug("Online time requested for " + player + " of type " + type.name() +
                ": " + (playTime + localPlaytime) + " seconds!");

        return playTime + localPlaytime;
    }

    // Called Async
    public void setTime(UUID player, TimeType type, int time) {
        database.setTime(player, type, time);
    }

    // Called Sync
    public void setTimeLocal(UUID player, TimeType type, int time) {
        OnlineTime ot = playerJoins.get(player);

        if (ot == null)
            return;

        if (type == TimeType.ALL_TIME) {
            int previousTime = ot.getAllTime();
            ot.addToAllTime(time - previousTime);
        }
        else if (type == TimeType.MONTHLY) {
            int previousTime = ot.getMonthlyTime();
            ot.addToWeeklyTime(time - previousTime);
        }
        else if (type == TimeType.WEEKLY) {
            int previousTime = ot.getWeeklyTime();
            ot.addToWeeklyTime(time - previousTime);
        }
    }

    // Called Async
    public void migrateOld() {
        PlaytimePro plugin = PlaytimePro.getInstance();

        File file = new File(plugin.getDataFolder(), "times.json");

        if (!file.exists()) {
            getLogger().warning("Could not migrate from old data. times.json file not found.");
            return;
        }

        try {
            Scanner scanner = new Scanner(file);

            getLogger().info(Messages.getString("migration_loading_old"));
            Map<String, Double> stringMap = new Gson().fromJson(scanner.nextLine(), Map.class);

            getLogger().info(Messages.getString("migration_mapping_players"));
            Map<UUID, Integer> times = stringMap.entrySet().stream()
                    .map(e -> new SimpleEntry<>(UUID.fromString(e.getKey()), e.getValue().intValue()))
                    .collect(Collectors.toMap(SimpleEntry::getKey, SimpleEntry::getValue));

            plugin.stopRunnables();

            getLogger().info(Messages.getString("migration_uploading_times"));
            database.updateTimesMigration(TimeType.ALL_TIME, times);

            getLogger().info(Messages.getString("migration_updating_names"));

            // Use Silverwolfg11's UUIDMap plugin (which is already used for TownyNameUpdater and XenLink) to get
            // the names from UUID from players who have already joined the server async.
            // We cannot use Bukkit.getOfflinePlayer because it is not thread safe!
            if (PlaytimePro.getInstance().getUUIDMapDependency() != null) {
                PlaytimePro.getInstance().getUUIDMapDependency().getNameFromUUID(times.keySet()).thenAcceptAsync(map -> {
                    if (!map.isEmpty())
                        database.updateNames(map);
                });
            }

            getLogger().info(Messages.getString("migration_completed"));

            plugin.registerRunnables();

            scanner.close();
        } catch (IOException e) {
            getLogger().log(Level.WARNING, "Could not migrate from old data.", e);
        }
    }

    // Called Async
    public void migrateToOther() {
        try {
            SQLDatabase other = database instanceof MySQL ? new SQLite() : newMySQL();

            if (other == null)
                return;

            if (other instanceof MySQL && !other.establishConnection())
                return;

            Map<UUID, Integer> allTime = new HashMap<>(),
                               monthlyTime = new HashMap<>(),
                               weeklyTime = new HashMap<>();

            // Fill the maps with the data
            database.fillTimesToMap(allTime, monthlyTime, weeklyTime);

            // Migrate data to other database
            other.updateTimesMigration(TimeType.ALL_TIME, allTime);
            other.updateTimesMigration(TimeType.MONTHLY, monthlyTime);
            other.updateTimesMigration(TimeType.WEEKLY, weeklyTime);


            getLogger().info("Migration to other database completed.");
        } catch (Exception e) {
            getLogger().log(Level.WARNING, "Could not migrate to new database");
        }
    }

    public int purge(int time) {
        return database.purge(time);
    }

    public void purgeTable(TimeType type) {
        database.purgeTable(type);
    }

    private MySQL newMySQL() {
        String host = ConfigManager.getMySQLHost();
        int port = ConfigManager.getMySQLPort();
        String databaseName = ConfigManager.getMySQLDatabase();
        String username = ConfigManager.getMySQLUsername();
        String password = ConfigManager.getMySQLPassword();

        if (host == null || port == 0 || databaseName == null || username == null || password == null) {
            getLogger().severe("Invalid MySQL database configuration! Check your config. Plugin disabling.");
            return null;
        }

        return new MySQL(host, port, databaseName, username, password);
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
