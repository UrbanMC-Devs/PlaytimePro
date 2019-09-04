package me.elian.playtime.command;

import me.elian.playtime.PlaytimePro;
import me.elian.playtime.object.Command;
import org.bukkit.command.CommandSender;

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
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> getData().migrateToOther(plugin));
            sendMessage(sender, "migration_started");
        } else {
            sendMessage(sender, "migrate_info");
        }
    }
}
