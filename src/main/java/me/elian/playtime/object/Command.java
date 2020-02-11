package me.elian.playtime.object;

import me.elian.playtime.PlaytimePro;
import me.elian.playtime.manager.DataManager;
import me.elian.playtime.manager.Messages;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public abstract class Command implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, org.bukkit.command.Command command, String label,
                             String[] args) {
        onCommand(sender, label, args);
        return true;
    }

    abstract public void onCommand(CommandSender sender, String label, String[] args);

    protected DataManager getData() {
        return DataManager.getInstance();
    }

    protected void sendMessage(CommandSender sender, String property, Object... args) {
        String message = Messages.getString(property, args);

        if (sender instanceof Player) {
            sender.sendMessage(message);
        } else {
            sender.sendMessage(ChatColor.stripColor(message));
        }
    }

    // Must be called sync
    protected void sendMessage(UUID sender, String property, Object... args) {
        String message = Messages.getString(property, args);

        if (sender == null) {
            Bukkit.getConsoleSender().sendMessage(ChatColor.stripColor(message));
        }
        else {
            Player p = Bukkit.getPlayer(sender);

            if (p != null)
                p.sendMessage(message);
        }
    }

    protected void runTask(boolean async, Runnable run) {
        if (async)
            Bukkit.getScheduler().runTaskAsynchronously(PlaytimePro.getInstance(), run);
        else
            Bukkit.getScheduler().runTask(PlaytimePro.getInstance(), run);
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

    protected boolean isInt(String number) {
        try {
            Integer.parseInt(number);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
