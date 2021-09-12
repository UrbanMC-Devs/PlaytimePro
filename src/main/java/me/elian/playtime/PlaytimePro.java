package me.elian.playtime;

import me.elian.playtime.command.*;
import me.elian.playtime.listener.HeadListener;
import me.elian.playtime.listener.JoinListener;
import me.elian.playtime.manager.ConfigManager;
import me.elian.playtime.manager.DataManager;
import me.elian.playtime.manager.DateManager;
import me.elian.playtime.runnable.DatabaseSaver;
import me.elian.playtime.runnable.HeadUpdater;
import me.elian.playtime.runnable.TopListUpdater;
import me.elian.playtime.util.UUIDMapDependency;
import org.bukkit.Bukkit;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public class PlaytimePro extends JavaPlugin {

    private BukkitRunnable topListUpdater, headUpdater, databaseSaver;
    private UUIDMapDependency uuidMap;

    private static PlaytimePro instance;
    public static PlaytimePro getInstance() {
        return instance;
    }

    private static boolean debug = false;

    @Override
    public void onEnable() {
        instance = this;

        ConfigManager.reload();

        if (!DataManager.getInstance().registerDatabase()) {
            setEnabled(false);
            return;
        }

        // Load date time
        DateManager.getInstance().loadFromTextFile();

        registerSoftDependencies();
        registerRunnables();
        registerListeners();
        registerCommands();
    }

    @Override
    public void onDisable() {
        stopRunnables();
        DataManager.getInstance().closeDatabase();
        instance = null;
    }

    public void registerRunnables() {
        int topListTime = ConfigManager.getTopUpdateInterval();

        if (topListTime == 0)
            topListTime = 1;

        topListUpdater = new TopListUpdater();
        topListUpdater.runTaskTimerAsynchronously(this, 0, 20 * topListTime);

        int headUpdaterTime = ConfigManager.getHeadsUpdateInterval();

        if (headUpdaterTime == 0)
            headUpdaterTime = 1;

        headUpdater = new HeadUpdater();
        headUpdater.runTaskTimer(this, 200, 20 * headUpdaterTime);

        int saveTime = ConfigManager.getSaveInterval();

        if (saveTime == 0)
            saveTime = 1;

        databaseSaver = new DatabaseSaver();
        databaseSaver.runTaskTimerAsynchronously(this, 20 * saveTime, 20 * saveTime);
    }

    public void stopRunnables() {
        if (topListUpdater != null && !topListUpdater.isCancelled()) {
            topListUpdater.cancel();
        }

        if (headUpdater != null && !headUpdater.isCancelled()) {
            headUpdater.cancel();
        }

        if (databaseSaver != null && !databaseSaver.isCancelled()) {
            databaseSaver.cancel();
        }
        // Just for good measure
        Bukkit.getScheduler().cancelTasks(this);
    }

    private void registerListeners() {
        PluginManager pm = getServer().getPluginManager();

        pm.registerEvents(new HeadListener(), this);
        pm.registerEvents(new JoinListener(), this);
    }

    @SuppressWarnings ("ConstantConditions")
    private void registerCommands() {
        getCommand("playtime").setExecutor(new Main());
        getCommand("playtimetop").setExecutor(new Top());
        getCommand("playtimemonthly").setExecutor(new Monthly());
        getCommand("playtimemonthlytop").setExecutor(new MonthlyTop());
        getCommand("playtimeweekly").setExecutor(new Weekly());
        getCommand("playtimeweeklytop").setExecutor(new WeeklyTop());
        getCommand("playtimeseason").setExecutor(new Season());
        getCommand("playtimeseasontop").setExecutor(new SeasonTop());
        getCommand("playtimemigrate").setExecutor(new Migrate());
        getCommand("playtimepurge").setExecutor(new Purge());
        getCommand("playtimereload").setExecutor(new Reload());
        getCommand("playtimedebug").setExecutor(new Debug());
        getCommand("playtimeset").setExecutor(new Set());
    }

    private void registerSoftDependencies() {
        if (Bukkit.getPluginManager().getPlugin("UUIDMap") != null)
            uuidMap = new UUIDMapDependency();
    }

    public UUIDMapDependency getUUIDMapDependency() {
        return uuidMap;
    }

    // Easy access debug methods
    public static void debug(String message) {
        if (debug)
            instance.getLogger().info("Debug: " + message);
    }

    // Returns the new state of the debug boolean.
    public static boolean toggleDebug() {
        return (debug ^= true);
    }
}
