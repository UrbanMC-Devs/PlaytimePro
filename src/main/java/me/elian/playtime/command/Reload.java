package me.elian.playtime.command;

import me.elian.playtime.PlaytimePro;
import me.elian.playtime.manager.ConfigManager;
import me.elian.playtime.manager.DataManager;
import me.elian.playtime.manager.Messages;
import me.elian.playtime.object.Command;
import org.bukkit.command.CommandSender;

public class Reload extends Command {

    private PlaytimePro plugin;

    public Reload(PlaytimePro plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onCommand(CommandSender sender, String label, String[] args) {
        sendMessage(sender, "reload_start");

        plugin.stopRunnables();
        getData().saveStorageToDatabase();

        Messages.getInstance().reload();
        ConfigManager.getInstance().reload();

        if (!DataManager.getInstance().registerDatabase(plugin)) {
            sendMessage(sender, "reload_failed");
            return;
        }

        plugin.registerRunnables();
        sendMessage(sender, "reload_finished");
    }
}
