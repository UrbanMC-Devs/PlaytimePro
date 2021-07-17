package me.elian.playtime.command;

import me.elian.playtime.object.Command;
import me.elian.playtime.object.TimeType;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class Season extends Command {

    @Override
    public void onCommand(CommandSender sender, String label, String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player)) {
                sendMessage(sender, "player_only");
                return;
            }

            Player p = (Player) sender;

            int seconds = getData().getOnlineTime(p.getUniqueId(), TimeType.SEASON);
            sendMessage(p, "season_self", formatTime(seconds));
        } else {
            OfflinePlayer p = Bukkit.getOfflinePlayer(args[0]);

            final UUID targetUUID = p.getUniqueId();
            final Player senderPlayer = (sender instanceof Player) ? ((Player) sender) : null;

            // Check if sender is the target
            if (senderPlayer != null && targetUUID.equals(senderPlayer.getUniqueId())) {
                Player player = (Player) sender;
                int seconds = getData().getOnlineTime(player.getUniqueId(), TimeType.SEASON);
                sendMessage(player, "season_self", formatTime(seconds));
                return;
            }

            if (p.isOnline()) {
                int seconds = getData().getOnlineTime(p.getUniqueId(), TimeType.SEASON);
                sendMessage(sender, "season_other", formatTime(seconds), p.getName());
                return;
            }

            final String targetName = p.getName();
            sendMessage(sender, "playtime_fetch");

            runTask(true, () -> {
                final int seconds = getData().getOfflineTime(targetUUID, TimeType.SEASON);

                if (seconds == -1) {
                    sendMessage(senderPlayer, "player_never_played_season");
                } else {
                    sendMessage(senderPlayer, "season_other", formatTime(seconds), targetName);
                }
            });
        }
    }
}
