package me.elian.playtime.object;

import me.elian.playtime.manager.DataManager;
import me.elian.playtime.manager.Messages;
import me.elian.playtime.runnable.DatabaseSaver;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public abstract class Command implements CommandExecutor {

    private DataManager data = DataManager.getInstance();

    @Override
    public boolean onCommand(CommandSender sender, org.bukkit.command.Command command, String label,
                             String[] args) {
        onCommand(sender, label, args);
        return true;
    }

    abstract public void onCommand(CommandSender sender, String label, String[] args);

    protected DataManager getData() {
        return data;
    }

    protected void sendMessage(CommandSender sender, String property, Object... args) {
        String message = Messages.getString(property, args);

        if (sender instanceof Player) {
            sender.sendMessage(message);
        } else {
            sender.sendMessage(ChatColor.stripColor(message));
        }
    }

    protected String formatTime(int seconds) {
        long hours = seconds / 3600;
        int minutes = ((seconds % 3600) / 60);
        int secs = (seconds % 60);

        String hoursFormat = Messages.getString("hours"), minutesFormat = Messages.getString("minutes"),
                secondsFormat = Messages.getString("seconds");

        String delimiter = Messages.getString("time_separator");

        String formattedTime = "";

        if (hours > 0) {
            formattedTime += hours;
            formattedTime += hoursFormat;

            if (minutes > 0)
                formattedTime += delimiter;
        }

        if (minutes > 0) {
            formattedTime += minutes;
            formattedTime += minutesFormat;

            if (seconds > 0)
                formattedTime += delimiter;
        }

        if (seconds > 0) {
            formattedTime += secs;
            formattedTime += secondsFormat;
        }

        return formattedTime;
    }
}
