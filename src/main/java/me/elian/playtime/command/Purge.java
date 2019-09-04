package me.elian.playtime.command;

import me.elian.playtime.PlaytimePro;
import me.elian.playtime.object.Command;
import org.bukkit.command.CommandSender;

public class Purge extends Command {

    private PlaytimePro plugin;

    public Purge(PlaytimePro plugin) {
        this.plugin = plugin;
    }

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

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            sendMessage(sender, "purge_start");

            int purged = getData().purge(time);
            sendMessage(sender, "purge_finish", purged);
        });
    }

    public boolean isInt(String number) {
        try {
            Integer.parseInt(number);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
