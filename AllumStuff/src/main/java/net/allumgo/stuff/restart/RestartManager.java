package net.allumgo.stuff.restart;

import net.allumgo.stuff.AllumStuffPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Управляет циклом автоматического рестарта сервера.
 * Работает как конечный автомат, проверяемый раз в секунду (heartbeat):
 *  - ожидание -> предупреждение+голосование (последние N минут) -> рестарт или отсрочка.
 */
public class RestartManager {

    private final AllumStuffPlugin plugin;
    private final LegacyComponentSerializer legacy = LegacyComponentSerializer.legacyAmpersand();

    private long intervalSeconds;
    private long warningSeconds;
    private long postponeSeconds;
    private String shellCommandBeforeStop;

    private String messageWarning;
    private String messageVoteRegistered;
    private String messageVoteAlready;
    private String messageVoteNoneActive;
    private String messagePostponed;
    private String messageFinalCountdown;

    private long nextRestartEpochSeconds;
    private boolean warningActive = false;
    private boolean restarting = false;
    private final Set<UUID> votesAgainst = new HashSet<>();

    private BukkitTask heartbeatTask;

    public RestartManager(AllumStuffPlugin plugin) {
        this.plugin = plugin;
        reloadFromConfig();
    }

    public void reloadFromConfig() {
        FileConfiguration cfg = plugin.getConfig();
        this.intervalSeconds = cfg.getLong("restart.interval-minutes", 180) * 60L;
        this.warningSeconds = cfg.getLong("restart.warning-minutes", 5) * 60L;
        this.postponeSeconds = cfg.getLong("restart.postpone-minutes", 60) * 60L;
        this.shellCommandBeforeStop = cfg.getString("restart.shell-command-before-stop", "");

        this.messageWarning = cfg.getString("restart.messages.warning", "&eРестарт через 5 минут.");
        this.messageVoteRegistered = cfg.getString("restart.messages.vote-registered", "&eГолос учтён.");
        this.messageVoteAlready = cfg.getString("restart.messages.vote-already", "&eТы уже голосовал(а).");
        this.messageVoteNoneActive = cfg.getString("restart.messages.vote-none-active", "&eНет активного голосования.");
        this.messagePostponed = cfg.getString("restart.messages.postponed", "&aРестарт отложен.");
        this.messageFinalCountdown = cfg.getString("restart.messages.final-countdown", "&cПерезапуск!");

        // Если таймер ещё не запускался - выставляем первую точку рестарта от текущего момента.
        if (nextRestartEpochSeconds == 0) {
            nextRestartEpochSeconds = nowSeconds() + intervalSeconds;
        }
    }

    public void start() {
        if (heartbeatTask != null) {
            return;
        }
        heartbeatTask = Bukkit.getScheduler().runTaskTimer(plugin, this::heartbeat, 20L, 20L);
    }

    public void stop() {
        if (heartbeatTask != null) {
            heartbeatTask.cancel();
            heartbeatTask = null;
        }
    }

    private long nowSeconds() {
        return System.currentTimeMillis() / 1000L;
    }

    private void heartbeat() {
        if (restarting) {
            return;
        }
        long remaining = nextRestartEpochSeconds - nowSeconds();

        if (!warningActive && remaining <= warningSeconds) {
            int online = Bukkit.getOnlinePlayers().size();
            if (online == 0) {
                // Некому мешать - перезапускаемся сразу, без голосования и предупреждений.
                executeRestart();
                return;
            }
            warningActive = true;
            votesAgainst.clear();
            broadcast(messageWarning);
        }

        if (remaining <= 0) {
            executeRestart();
        }
    }

    /**
     * Вызывается командой /rskip. Возвращает true, если голос был принят.
     */
    public VoteResult registerVote(Player player) {
        if (!warningActive || restarting) {
            sendMessage(player, messageVoteNoneActive);
            return VoteResult.NO_ACTIVE_VOTE;
        }
        if (!votesAgainst.add(player.getUniqueId())) {
            sendMessage(player, messageVoteAlready);
            return VoteResult.ALREADY_VOTED;
        }

        int online = Bukkit.getOnlinePlayers().size();
        int needed = (int) Math.ceil(online / 2.0);

        sendMessage(player, messageVoteRegistered
                .replace("%votes%", String.valueOf(votesAgainst.size()))
                .replace("%needed%", String.valueOf(needed)));

        if (votesAgainst.size() >= needed) {
            postpone();
        }
        return VoteResult.REGISTERED;
    }

    private void postpone() {
        warningActive = false;
        votesAgainst.clear();
        nextRestartEpochSeconds = nowSeconds() + postponeSeconds;
        broadcast(messagePostponed);
    }

    private void executeRestart() {
        if (restarting) {
            return;
        }
        restarting = true;
        broadcast(messageFinalCountdown);

        // Небольшая задержка, чтобы сообщение точно дошло до клиентов перед остановкой.
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (shellCommandBeforeStop != null && !shellCommandBeforeStop.isBlank()) {
                try {
                    new ProcessBuilder("sh", "-c", shellCommandBeforeStop).start();
                } catch (Exception e) {
                    plugin.getLogger().warning("Не удалось выполнить shell-command-before-stop: " + e.getMessage());
                }
            }
            Bukkit.shutdown();
        }, 60L); // 3 секунды
    }

    private void broadcast(String rawMessage) {
        Component component = legacy.deserialize(rawMessage);
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendMessage(component);
        }
        plugin.getLogger().info(net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(component));
    }

    private void sendMessage(Player player, String rawMessage) {
        player.sendMessage(legacy.deserialize(rawMessage));
    }

    public enum VoteResult {
        REGISTERED, ALREADY_VOTED, NO_ACTIVE_VOTE
    }
}
