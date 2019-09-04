package me.elian.playtime.command;

import me.elian.playtime.object.Command;
import me.elian.playtime.object.TimeType;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class Weekly extends Command {

    @Override
    public void onCommand(CommandSender sender, String label, String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player)) {
                sendMessage(sender, "player_only");
                return;
            }

            Player p = (Player) sender;

            int seconds = getData().getTime(p.getUniqueId(), TimeType.WEEKLY);
            sendMessage(p, "weekly_self", formatTime(seconds));
        } else {
            OfflinePlayer p = Bukkit.getOfflinePlayer(args[0]);

            int seconds = getData().getTime(p.getUniqueId(), TimeType.WEEKLY);

            if (seconds == 0) {
                sendMessage(sender, "player_never_played_week");
            } else {
                if (sender instanceof Player && ((Player) sender).getUniqueId() == p.getUniqueId()) {
                    sendMessage(sender, "weekly_self", formatTime(seconds));
                } else {
                    sendMessage(sender, "weekly_other", formatTime(seconds), p.getName());
                }
            }
        }
    }
}
