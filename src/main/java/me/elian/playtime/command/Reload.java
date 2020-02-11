package me.elian.playtime.command;

import me.elian.playtime.PlaytimePro;
import me.elian.playtime.manager.ConfigManager;
import me.elian.playtime.manager.DataManager;
import me.elian.playtime.manager.Messages;
import me.elian.playtime.object.Command;
import org.bukkit.command.CommandSender;

public class Reload extends Command {

    @Override
    public void onCommand(CommandSender sender, String label, String[] args) {
        sendMessage(sender, "reload_start");

        PlaytimePro.getInstance().stopRunnables();
        getData().saveStorageToDatabase();

        Messages.getInstance().reload();
        ConfigManager.reload();

        if (!DataManager.getInstance().registerDatabase()) {
            sendMessage(sender, "reload_failed");
            return;
        }

        PlaytimePro.getInstance().registerRunnables();
        sendMessage(sender, "reload_finished");
    }
}
