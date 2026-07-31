package net.allumgo.stuff.commands;

import net.allumgo.stuff.restart.RestartManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class RSkipCommand implements CommandExecutor {

    private final RestartManager restartManager;

    public RSkipCommand(RestartManager restartManager) {
        this.restartManager = restartManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Эту команду может использовать только игрок.");
            return true;
        }
        restartManager.registerVote(player);
        return true;
    }
}
