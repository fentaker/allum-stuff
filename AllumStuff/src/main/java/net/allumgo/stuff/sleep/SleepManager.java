package net.allumgo.stuff.sleep;

import net.allumgo.stuff.AllumStuffPlugin;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.World;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Заменяет ванильный механизм пропуска ночи: пропуск начинается, как только заснул ОДИН
 * игрок, но с задержкой в несколько секунд и возможностью отмены через /nskip.
 * Ванильный процентный порог сна отключается через гейм-правило playersSleepingPercentage.
 */
public class SleepManager implements Listener {

    private final AllumStuffPlugin plugin;
    private final LegacyComponentSerializer legacy = LegacyComponentSerializer.legacyAmpersand();

    private long skipDelayTicks;
    private String messageStarted;
    private String messageCancelled;
    private String messageSkipped;

    private final Map<UUID, BukkitTask> pendingByWorld = new HashMap<>();

    public SleepManager(AllumStuffPlugin plugin) {
        this.plugin = plugin;
        reloadFromConfig();
    }

    public void reloadFromConfig() {
        long seconds = plugin.getConfig().getLong("sleep.skip-delay-seconds", 6);
        this.skipDelayTicks = seconds * 20L;
        this.messageStarted = plugin.getConfig().getString("sleep.messages.started", "&b%player% начал(а) спать.");
        this.messageCancelled = plugin.getConfig().getString("sleep.messages.cancelled", "&bПропуск ночи отменён.");
        this.messageSkipped = plugin.getConfig().getString("sleep.messages.skipped", "&bНочь пропущена.");

        // Отключаем ванильный авто-пропуск по проценту спящих - управляем этим сами.
        for (World world : Bukkit.getWorlds()) {
            world.setGameRule(GameRule.PLAYERS_SLEEPING_PERCENTAGE, 100);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBedEnter(PlayerBedEnterEvent event) {
        if (event.getBedEnterResult() != PlayerBedEnterEvent.BedEnterResult.OK) {
            return;
        }
        World world = event.getPlayer().getWorld();
        if (pendingByWorld.containsKey(world.getUID())) {
            // Пропуск ночи в этом мире уже запущен другим игроком - ничего не делаем.
            return;
        }

        broadcast(messageStarted.replace("%player%", event.getPlayer().getName()));

        BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            pendingByWorld.remove(world.getUID());
            skipNight(world);
        }, skipDelayTicks);

        pendingByWorld.put(world.getUID(), task);
    }

    /**
     * Вызывается командой /nskip. Возвращает true, если пропуск был отменён.
     */
    public boolean cancelSkip(World world, String cancellerName) {
        BukkitTask task = pendingByWorld.remove(world.getUID());
        if (task == null) {
            return false;
        }
        task.cancel();
        broadcast(messageCancelled.replace("%player%", cancellerName));
        return true;
    }

    private void skipNight(World world) {
        world.setTime(1000L);
        world.setStorm(false);
        world.setThundering(false);

        for (Player p : world.getPlayers()) {
            if (p.isSleeping()) {
                p.wakeup(false);
            }
        }
        broadcast(messageSkipped);
    }

    public void cancelAll() {
        for (BukkitTask task : pendingByWorld.values()) {
            task.cancel();
        }
        pendingByWorld.clear();
    }

    private void broadcast(String rawMessage) {
        var component = legacy.deserialize(rawMessage);
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendMessage(component);
        }
    }
}
