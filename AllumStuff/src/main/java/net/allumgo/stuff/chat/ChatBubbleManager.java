package net.allumgo.stuff.chat;

import net.allumgo.stuff.AllumStuffPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Показывает "пузырь" с текстом сообщения над головой игрока при отправке чата.
 * Реализовано через сущность TextDisplay (Paper 1.19.4+), которая следует за игроком
 * и исчезает через настраиваемое время. Работает и для Bedrock-зрителей через Geyser.
 */
public class ChatBubbleManager {

    private final AllumStuffPlugin plugin;
    private final Map<UUID, TextDisplay> activeBubbles = new HashMap<>();
    private final Map<UUID, BukkitTask> activeUpdaters = new HashMap<>();

    private boolean enabled;
    private long durationTicks;
    private long updateIntervalTicks;
    private int backgroundOpacity;

    public ChatBubbleManager(AllumStuffPlugin plugin) {
        this.plugin = plugin;
        reloadFromConfig();
    }

    public void reloadFromConfig() {
        this.enabled = plugin.getConfig().getBoolean("chat.bubble.enabled", true);
        this.durationTicks = plugin.getConfig().getLong("chat.bubble.duration-seconds", 4) * 20L;
        this.updateIntervalTicks = plugin.getConfig().getLong("chat.bubble.update-interval-ticks", 2);
        this.backgroundOpacity = plugin.getConfig().getInt("chat.bubble.background-opacity", 40);
    }

    public void showBubble(Player player, String plainMessage) {
        if (!enabled) {
            return;
        }
        removeBubble(player.getUniqueId());

        Location spawnLocation = player.getEyeLocation().add(0, 0.45, 0);
        TextDisplay display = player.getWorld().spawn(spawnLocation, TextDisplay.class, entity -> {
            entity.text(Component.text(plainMessage, NamedTextColor.WHITE));
            entity.setBillboard(Display.Billboard.CENTER);
            entity.setShadowed(false);
            entity.setSeeThrough(true);
            entity.setDefaultBackground(false);
            entity.setBackgroundColor(Color.fromARGB(backgroundOpacity, 0, 0, 0));
            entity.setPersistent(false);
            entity.setInvulnerable(true);
            entity.setGravity(false);
            entity.setLineWidth(200);
        });

        activeBubbles.put(player.getUniqueId(), display);

        BukkitTask updater = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (!player.isOnline() || display.isDead()) {
                removeBubble(player.getUniqueId());
                return;
            }
            display.teleport(player.getEyeLocation().add(0, 0.45, 0));
        }, updateIntervalTicks, updateIntervalTicks);

        activeUpdaters.put(player.getUniqueId(), updater);

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> removeBubble(player.getUniqueId()), durationTicks);
    }

    private void removeBubble(UUID uuid) {
        TextDisplay display = activeBubbles.remove(uuid);
        if (display != null && !display.isDead()) {
            display.remove();
        }
        BukkitTask updater = activeUpdaters.remove(uuid);
        if (updater != null) {
            updater.cancel();
        }
    }

    public void clearAll() {
        for (UUID uuid : activeBubbles.keySet().toArray(new UUID[0])) {
            removeBubble(uuid);
        }
    }
}
