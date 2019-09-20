package me.elian.playtime;

import me.elian.playtime.command.*;
import me.elian.playtime.listener.HeadListener;
import me.elian.playtime.listener.JoinListener;
import me.elian.playtime.manager.ConfigManager;
import me.elian.playtime.manager.DataManager;
import me.elian.playtime.runnable.DatabaseSaver;
import me.elian.playtime.runnable.HeadUpdater;
import me.elian.playtime.runnable.NullNameUpdater;
import me.elian.playtime.runnable.TopListUpdater;
import me.elian.playtime.util.UUIDMapDependency;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.concurrent.atomic.AtomicBoolean;

public class PlaytimePro extends JavaPlugin {

    private BukkitRunnable topListUpdater, headUpdater, databaseSaver;
    private static PlaytimePro instance;
    private static UUIDMapDependency uuidMap;

    @Override
    public void onEnable() {
        if (!DataManager.getInstance().registerDatabase(this)) {
            setEnabled(false);
            return;
        }

        instance = this;

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
        FileConfiguration config = ConfigManager.getConfig();

        int topListTime = config.getInt("update-top-list");

        if (topListTime == 0)
            topListTime = 1;

        topListUpdater = new TopListUpdater();
        topListUpdater.runTaskTimerAsynchronously(this, 0, 20 * topListTime);

        int headUpdaterTime = config.getInt("update-heads");

        if (headUpdaterTime == 0)
            headUpdaterTime = 1;

        headUpdater = new HeadUpdater();
        headUpdater.runTaskTimer(this, 200, 20 * headUpdaterTime);

        int saveTime = config.getInt("save-database");

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
    }

    private void registerListeners() {
        PluginManager pm = getServer().getPluginManager();

        pm.registerEvents(new HeadListener(), this);
        pm.registerEvents(new JoinListener(this), this);
    }

    @SuppressWarnings ("ConstantConditions")
    private void registerCommands() {
        getCommand("playtime").setExecutor(new Main());
        getCommand("playtimetop").setExecutor(new Top());
        getCommand("playtimemonthly").setExecutor(new Monthly());
        getCommand("playtimemonthlytop").setExecutor(new MonthlyTop());
        getCommand("playtimeweekly").setExecutor(new Weekly());
        getCommand("playtimeweeklytop").setExecutor(new WeeklyTop());
        getCommand("playtimemigrate").setExecutor(new Migrate(this));
        getCommand("playtimepurge").setExecutor(new Purge(this));
        getCommand("playtimereload").setExecutor(new Reload(this));
    }

    private void registerSoftDependencies() {
        if (Bukkit.getPluginManager().getPlugin("UUIDMap") != null)
            uuidMap = new UUIDMapDependency();
    }

    public static void executeSync(Runnable run) {
        Bukkit.getScheduler().runTask(instance, run);
    }

    public static void checkForNullName() {
        NullNameUpdater.runTask(instance);
    }

    public static UUIDMapDependency getUUIDMapDependency() {
        return uuidMap;
    }
}
