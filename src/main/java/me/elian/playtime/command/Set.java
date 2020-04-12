package me.elian.playtime.command;

import me.elian.playtime.manager.Messages;
import me.elian.playtime.object.Command;
import me.elian.playtime.object.TimeType;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;

public class Set extends Command {

    @Override
    public void onCommand(CommandSender sender, String label, String[] args) {
        if (args.length != 3) {
            sender.sendMessage(Messages.getString("cmd.set.usage"));
            return;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);

        TimeType timeType;

        switch (args[1].toLowerCase()) {
            case "all":
                timeType = TimeType.ALL_TIME;
                break;
            case "monthly":
                timeType = TimeType.MONTHLY;
                break;
            case "weekly":
                timeType = TimeType.WEEKLY;
                break;
            default:
                sender.sendMessage(Messages.getString("cmd.set.invalid-type"));
                return;
        }

        int time = parseTime(args[2]);

        if (time == 0) {
            sender.sendMessage(Messages.getString("cmd.set.invalid-time"));
            return;
        }

        getData().setTimeLocal(target.getUniqueId(), timeType, time);
        runTask(true, () -> getData().setTime(target.getUniqueId(), timeType, time));
        sender.sendMessage(Messages.getString("cmd.set.time-set", args[0], time));
    }

    private int parseTime(String arg) {
        int time = 0;

        int lastCharIndex = 0;
        int currentIndex = -1;

        for (char c : arg.toCharArray()) {
            currentIndex++; // Sets to current index

            double multiplier; // In seconds
            switch (c) {
                case 'y': // Year
                    multiplier = 365 * 24 * 60 * 60;
                    break;
                case 'w': // Week
                    multiplier = 7 * 24 * 60 * 60;
                    break;
                case 'd': // Day
                    multiplier = 24 * 60 * 60;
                    break;
                case 'h': // Hour
                    multiplier = 60 * 60;
                    break;
                case 'm': // Minute
                    multiplier = 60;
                    break;
                case 's': // Second
                    multiplier = 1;
                    break;
                default:
                    continue;
            }

            String tempString = arg.substring(lastCharIndex, currentIndex);

            lastCharIndex = currentIndex + 1; // Should point to next number

            if (tempString.isEmpty()) continue;

            try {
                time += Double.parseDouble(tempString) * multiplier;
            } catch (NumberFormatException ignore) {
            }
        }

        // Handle inputting just seconds
        if (time == 0) {
            try {
                time += Double.parseDouble(arg);
            } catch (NumberFormatException ignore) {
            }
        }

        return time;
    }
}
