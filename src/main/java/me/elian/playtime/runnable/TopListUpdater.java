package me.elian.playtime.runnable;

import me.elian.playtime.manager.TopListManager;
import org.bukkit.scheduler.BukkitRunnable;

public class TopListUpdater extends BukkitRunnable {

    @Override
    public void run() {
        TopListManager.getInstance().updateTopListSorted();
    }
}
