package me.elian.playtime.command;

import me.elian.playtime.PlaytimePro;
import me.elian.playtime.object.Command;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

public class Migrate extends Command {

    @Override
    public void onCommand(CommandSender sender, String label, String[] args) {
        if (args.length == 0) {
            sendMessage(sender, "migrate_info");
            return;
        }

        String option = args[0];

        if (option.equalsIgnoreCase("old")) {
            Bukkit.getScheduler().runTaskAsynchronously(PlaytimePro.getInstance(),
                    () -> getData().migrateOld());
            sendMessage(sender, "migration_started");
        } else if (option.equalsIgnoreCase("other")) {
            Bukkit.getScheduler().runTaskAsynchronously(PlaytimePro.getInstance(),
                    () -> getData().migrateToOther());
            sendMessage(sender, "migration_started");
        } else {
            sendMessage(sender, "migrate_info");
        }
    }
}
