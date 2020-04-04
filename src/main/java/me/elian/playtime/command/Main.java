package me.elian.playtime.command;

import me.elian.playtime.object.Command;
import me.elian.playtime.object.TimeType;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class Main extends Command {

    @Override
    public void onCommand(CommandSender sender, String label, String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player)) {
                sendMessage(sender, "player_only");
                return;
            }

            Player p = (Player) sender;

            int seconds = getData().getOnlineTime(p.getUniqueId(), TimeType.ALL_TIME);
            sendMessage(p, "playtime_self", formatTime(seconds));
        } else {
            OfflinePlayer p = Bukkit.getOfflinePlayer(args[0]);

            final UUID targetUUID = p.getUniqueId();
            final Player senderPlayer = (sender instanceof Player) ? ((Player) sender) : null;

            // Check if sender is the target
            if (senderPlayer != null && targetUUID.equals(senderPlayer.getUniqueId())) {
                Player player = (Player) sender;
                int seconds = getData().getOnlineTime(player.getUniqueId(), TimeType.ALL_TIME);
                sendMessage(player, "playtime_self", formatTime(seconds));
                return;
            }

            // Check if player is online. If so get online time.
            if (p.isOnline()) {
                int seconds = getData().getOnlineTime(p.getUniqueId(), TimeType.ALL_TIME);
                sendMessage(sender, "playtime_other", formatTime(seconds), p.getName());
                return;
            }

            final String targetName = p.getName();
            sendMessage(sender, "playtime_fetch");

            runTask(true, () -> {
                final int seconds = getData().getOfflineTime(targetUUID, TimeType.ALL_TIME);

                // We can send messages async with relatively safety.
                if (seconds == -1) {
                    sendMessage(senderPlayer, "player_never_played");
                } else {
                    sendMessage(senderPlayer, "playtime_other", formatTime(seconds), targetName);
                }
            });
        }
    }
}
