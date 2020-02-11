package me.elian.playtime.command;

import me.elian.playtime.PlaytimePro;
import me.elian.playtime.object.Command;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

public class Purge extends Command {

    @Override
    public void onCommand(CommandSender sender, String label, String[] args) {
        if (args.length == 0) {
            sendMessage(sender, "purge_info");
            return;
        }

        int time;

        if (!isInt(args[0]) || (time = Integer.parseInt(args[0])) < 1) {
            sendMessage(sender, "top_list_invalid_time");
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(PlaytimePro.getInstance(), () -> {
            sendMessage(sender, "purge_start");

            int purged = getData().purge(time);
            sendMessage(sender, "purge_finish", purged);
        });
    }
}
