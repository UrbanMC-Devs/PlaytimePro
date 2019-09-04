package me.elian.playtime.runnable;

import me.elian.playtime.PlaytimePro;
import me.elian.playtime.manager.DataManager;
import me.elian.playtime.manager.TopListManager;
import org.bukkit.scheduler.BukkitRunnable;

public class TopListUpdater extends BukkitRunnable {

    private PlaytimePro plugin;

    public TopListUpdater(PlaytimePro plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        TopListManager.getInstance().updateTopList(plugin);
    }
}
