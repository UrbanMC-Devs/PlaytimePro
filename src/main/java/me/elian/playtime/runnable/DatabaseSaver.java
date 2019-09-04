package me.elian.playtime.runnable;

import me.elian.playtime.manager.DataManager;
import org.bukkit.scheduler.BukkitRunnable;

public class DatabaseSaver extends BukkitRunnable {

    @Override
    public void run() {
        DataManager.getInstance().saveStorageToDatabase();
    }
}
