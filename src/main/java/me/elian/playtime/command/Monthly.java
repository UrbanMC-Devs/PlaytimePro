package me.elian.playtime.command;

import me.elian.playtime.object.Command;
import me.elian.playtime.object.TimeType;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class Monthly extends Command {

    @Override
    public void onCommand(CommandSender sender, String label, String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player)) {
                sendMessage(sender, "player_only");
                return;
            }

            Player p = (Player) sender;

            int seconds = getData().getTime(p.getUniqueId(), TimeType.MONTHLY);
            sendMessage(p, "monthly_self", formatTime(seconds));
        } else {
            OfflinePlayer p = Bukkit.getOfflinePlayer(args[0]);

            int seconds = getData().getTime(p.getUniqueId(), TimeType.MONTHLY);

            if (seconds == 0) {
                sendMessage(sender, "player_never_played_month");
            } else {
                if (sender instanceof Player && ((Player) sender).getUniqueId() == p.getUniqueId()) {
                    sendMessage(sender, "monthly_self", formatTime(seconds));
                } else {
                    sendMessage(sender, "monthly_other", formatTime(seconds), p.getName());
                }
            }
        }
    }
}
