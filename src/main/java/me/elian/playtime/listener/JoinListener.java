package me.elian.playtime.listener;
import me.elian.playtime.manager.DataManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

public class JoinListener implements Listener {

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        Player player = e.getPlayer();

        UUID id = player.getUniqueId();
        String name = player.getName();

        DataManager.getInstance().playerJoin(id, name);
    }


    @EventHandler
    public void onPlayerLogout(PlayerQuitEvent event) {
        DataManager.getInstance().playerLeave(event.getPlayer().getUniqueId());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onPlayerLogout(PlayerKickEvent event) {
        DataManager.getInstance().playerLeave(event.getPlayer().getUniqueId());
    }

}
