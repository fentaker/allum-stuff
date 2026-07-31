package net.allumgo.stuff.commands;

import net.allumgo.stuff.sleep.SleepManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class NSkipCommand implements CommandExecutor {

    private final SleepManager sleepManager;

    public NSkipCommand(SleepManager sleepManager) {
        this.sleepManager = sleepManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Эту команду может использовать только игрок.");
            return true;
        }
        boolean cancelled = sleepManager.cancelSkip(player.getWorld(), player.getName());
        if (!cancelled) {
            player.sendMessage(ChatColor.YELLOW + "Сейчас никто не пропускает ночь в этом мире.");
        }
        return true;
    }
}
