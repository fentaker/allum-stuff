package net.allumgo.stuff.commands;

import net.allumgo.stuff.AllumStuffPlugin;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class AllumStuffCommand implements CommandExecutor {

    private final AllumStuffPlugin plugin;

    public AllumStuffCommand(AllumStuffPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            plugin.reloadAll();
            sender.sendMessage(ChatColor.GREEN + "AllumStuff: конфигурация перезагружена.");
            return true;
        }
        sender.sendMessage(ChatColor.YELLOW + "Использование: /allumstuff reload");
        return true;
    }
}
