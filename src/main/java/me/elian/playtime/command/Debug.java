package me.elian.playtime.command;

import me.elian.playtime.PlaytimePro;
import me.elian.playtime.object.Command;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public class Debug extends Command {

    @Override
    public void onCommand(CommandSender sender, String label, String[] args) {
        if(PlaytimePro.toggleDebug())
            sender.sendMessage(ChatColor.GREEN + "Playtime Pro debug mode enabled. All debug output will be shown in console!");
        else
            sender.sendMessage(ChatColor.RED + "Playtime Pro debug mode disabled!");
    }
}
