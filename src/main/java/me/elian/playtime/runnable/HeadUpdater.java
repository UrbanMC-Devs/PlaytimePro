package me.elian.playtime.runnable;

import me.elian.playtime.manager.DataManager;
import me.elian.playtime.object.SignHead;
import me.elian.playtime.util.HeadUtil;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;

public class HeadUpdater extends BukkitRunnable {

    @Override
    public void run() {
        List<SignHead> heads = DataManager.getInstance().getHeads();

        for (SignHead head : heads) {
            HeadUtil.updateHead(head);
        }
    }
}
