package net.allumgo.stuff.commands;

import net.allumgo.stuff.title.TitleManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class TitleCommand implements CommandExecutor {

    private final TitleManager titleManager;

    public TitleCommand(TitleManager titleManager) {
        this.titleManager = titleManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Эту команду может использовать только игрок.");
            return true;
        }

        if (args.length == 0) {
            player.sendMessage(ChatColor.YELLOW + "Использование: /title <текст с &цветами> или /title off");
            return true;
        }

        if (args.length == 1 && (args[0].equalsIgnoreCase("off") || args[0].equalsIgnoreCase("clear"))) {
            titleManager.clearTitle(player.getUniqueId());
            player.sendMessage(ChatColor.GREEN + "Тег снят.");
            return true;
        }

        String input = String.join(" ", args);
        String error = titleManager.setTitle(player.getUniqueId(), input);
        if (error != null) {
            player.sendMessage(ChatColor.RED + error);
        } else {
            player.sendMessage(ChatColor.GREEN + "Тег установлен: "
                    + ChatColor.translateAlternateColorCodes('&', input) + ChatColor.GREEN + " " + player.getName());
        }
        return true;
    }
}
