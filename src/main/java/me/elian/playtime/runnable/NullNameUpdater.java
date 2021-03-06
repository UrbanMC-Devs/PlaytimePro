package me.elian.playtime.runnable;

import me.elian.playtime.PlaytimePro;
import me.elian.playtime.manager.DataManager;
import org.bukkit.scheduler.BukkitRunnable;

public class NullNameUpdater extends BukkitRunnable {

    private static boolean running;

    private NullNameUpdater() {
        runTaskTimerAsynchronously(PlaytimePro.getInstance(), 40, 11 * 60 * 20);
        running = true;
    }

    @Override
    public void run() {
        if (!running) {
            cancel();
            return;
        }
        if (!DataManager.getInstance().updateNullNames()) {
            running = false;
            cancel();
        }
    }

    public static void runTask() {
        if (running) return;

        new NullNameUpdater();
    }
}
