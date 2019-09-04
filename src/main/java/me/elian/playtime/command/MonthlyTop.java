package me.elian.playtime.command;

import me.elian.playtime.manager.TopListManager;
import me.elian.playtime.object.Command;
import me.elian.playtime.object.TimeType;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class MonthlyTop extends Command {

    @Override
    public void onCommand(CommandSender sender, String label, String[] args) {
        int pageNumber = 1;

        if (args.length > 0 && isInt(args[0]))
            pageNumber = Integer.parseInt(args[0]);

        try {
            String page = TopListManager.getInstance().getTopListPage(TimeType.MONTHLY, pageNumber);

            if (sender instanceof Player)
                sender.sendMessage(page);
            else
                sender.sendMessage(ChatColor.stripColor(page));
        } catch (NullPointerException e) {
            sendMessage(sender, "top_list_not_loaded");
        }
    }

    private boolean isInt(String number) {
        try {
            Integer.parseInt(number);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
