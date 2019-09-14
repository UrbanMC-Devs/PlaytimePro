package me.elian.playtime.command;

import me.elian.playtime.PlaytimePro;
import me.elian.playtime.object.Command;
import me.elian.playtime.object.TimeType;
import org.bukkit.command.CommandSender;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Migrate extends Command {

    private PlaytimePro plugin;

    public Migrate(PlaytimePro plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onCommand(CommandSender sender, String label, String[] args) {
        if (args.length == 0) {
            sendMessage(sender, "migrate_info");
            return;
        }

        String option = args[0];

        if (option.equalsIgnoreCase("old")) {
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> getData().migrateOld(plugin));
            sendMessage(sender, "migration_started");
        } else if (option.equalsIgnoreCase("other")) {
            Map<UUID, Integer> allTimeClone = getData().getSnapshotMap(TimeType.ALL_TIME),
                    monthlyClone = getData().getSnapshotMap(TimeType.MONTHLY),
                    weeklyClone = getData().getSnapshotMap(TimeType.WEEKLY);

            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> getData().migrateToOther(plugin, allTimeClone, monthlyClone, weeklyClone));
            sendMessage(sender, "migration_started");
        } else {
            sendMessage(sender, "migrate_info");
        }
    }
}
