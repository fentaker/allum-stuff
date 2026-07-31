package net.allumgo.stuff.chat;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.allumgo.stuff.AllumStuffPlugin;
import net.allumgo.stuff.title.TitleManager;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

/**
 * Разделяет чат на локальный (по умолчанию, видно в радиусе) и глобальный (префикс "!").
 * Добавляет цветной тег [L]/[G] в самое начало сообщения и вызывает эффект "пузыря" над головой.
 */
public class ChatManager implements Listener {

    private final AllumStuffPlugin plugin;
    private final TitleManager titleManager;
    private final ChatBubbleManager bubbleManager;

    private final LegacyComponentSerializer legacy = LegacyComponentSerializer.legacyAmpersand();
    private final PlainTextComponentSerializer plainText = PlainTextComponentSerializer.plainText();

    private double localRadius;
    private Component globalTag;
    private Component localTag;

    public ChatManager(AllumStuffPlugin plugin, TitleManager titleManager, ChatBubbleManager bubbleManager) {
        this.plugin = plugin;
        this.titleManager = titleManager;
        this.bubbleManager = bubbleManager;
        reloadFromConfig();
    }

    public void reloadFromConfig() {
        this.localRadius = plugin.getConfig().getDouble("chat.local-radius", 150);
        this.globalTag = legacy.deserialize(plugin.getConfig().getString("chat.tags.global", "&a[G]"));
        this.localTag = legacy.deserialize(plugin.getConfig().getString("chat.tags.local", "&e[L]"));
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onChat(AsyncChatEvent event) {
        Player sender = event.getPlayer();
        String rawMessage = plainText.serialize(event.originalMessage());

        boolean isGlobal = rawMessage.startsWith("!");
        String finalText = isGlobal ? rawMessage.substring(1).stripLeading() : rawMessage;

        if (finalText.isBlank()) {
            event.setCancelled(true);
            return;
        }

        Component chatTag = isGlobal ? globalTag : localTag;
        Component titleComponent = buildTitleComponent(sender);
        Component messageComponent = Component.text(finalText, NamedTextColor.WHITE);

        // Собственный рендер сообщения: [тег] [title] Ник: сообщение
        event.renderer((source, sourceDisplayName, message, viewer) -> Component.text()
                .append(chatTag)
                .append(Component.text(" "))
                .append(titleComponent)
                .append(sourceDisplayName)
                .append(Component.text(": ", NamedTextColor.GRAY))
                .append(messageComponent)
                .build());

        event.message(messageComponent);

        if (!isGlobal) {
            event.viewers().removeIf(audience -> {
                if (audience instanceof Player viewer) {
                    if (viewer.getUniqueId().equals(sender.getUniqueId())) {
                        return false; // отправитель всегда видит своё сообщение
                    }
                    if (!viewer.getWorld().equals(sender.getWorld())) {
                        return true;
                    }
                    return viewer.getLocation().distance(sender.getLocation()) > localRadius;
                }
                return false; // консоль и прочие не-игровые адресаты не фильтруются
            });
        }

        bubbleManager.showBubble(sender, finalText);
    }

    private Component buildTitleComponent(Player player) {
        String rawTitle = titleManager.getTitle(player.getUniqueId());
        if (rawTitle == null || rawTitle.isBlank()) {
            return Component.empty();
        }
        return legacy.deserialize(rawTitle).append(Component.text(" "));
    }
}
