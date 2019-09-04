package me.elian.playtime.runnable;

import me.elian.playtime.manager.DataManager;
import org.bukkit.scheduler.BukkitRunnable;

public class LocalTimeUpdater extends BukkitRunnable {

    @Override
    public void run() {
        DataManager.getInstance().increaseLocalTimes();
    }
}
