package me.elian.playtime.listener;

import me.elian.playtime.PlaytimePro;
import me.elian.playtime.manager.DataManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.UUID;

public class JoinListener implements Listener {

    private PlaytimePro plugin;

    public JoinListener(PlaytimePro plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        Player player = e.getPlayer();

        UUID id = player.getUniqueId();
        String name = player.getName();

        DataManager.getInstance().playerJoin(id);

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin,
                () -> DataManager.getInstance().setLastName(id, name));
    }
}
