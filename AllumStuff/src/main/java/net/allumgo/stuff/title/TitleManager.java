package net.allumgo.stuff.title;

import net.allumgo.stuff.AllumStuffPlugin;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Хранит персональный тег (title) игрока, отображаемый перед ником в чате.
 * Данные сохраняются в plugins/AllumStuff/titles.yml и переживают рестарты.
 */
public class TitleManager {

    private final AllumStuffPlugin plugin;
    private final File file;
    private YamlConfiguration storage;
    private final Map<UUID, String> titles = new HashMap<>();

    private int maxLength;

    public TitleManager(AllumStuffPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "titles.yml");
        load();
        reloadFromConfig();
    }

    public void reloadFromConfig() {
        this.maxLength = plugin.getConfig().getInt("title.max-length", 24);
    }

    private void load() {
        if (!file.exists()) {
            try {
                file.getParentFile().mkdirs();
                file.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().warning("Не удалось создать titles.yml: " + e.getMessage());
            }
        }
        storage = YamlConfiguration.loadConfiguration(file);
        for (String key : storage.getKeys(false)) {
            try {
                titles.put(UUID.fromString(key), storage.getString(key));
            } catch (IllegalArgumentException ignored) {
                // Пропускаем повреждённые записи
            }
        }
    }

    private void save() {
        try {
            storage.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Не удалось сохранить titles.yml: " + e.getMessage());
        }
    }

    /**
     * Устанавливает тег игроку. Цветовые коды указываются через '&'.
     * Возвращает null при успехе, либо текст ошибки, если тег слишком длинный.
     */
    public String setTitle(UUID uuid, String rawInput) {
        String plain = ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', rawInput));
        if (plain.length() > maxLength) {
            return "Тег слишком длинный (максимум " + maxLength + " символов без учёта цветовых кодов).";
        }
        String colored = ChatColor.translateAlternateColorCodes('&', rawInput);
        titles.put(uuid, colored);
        storage.set(uuid.toString(), colored);
        save();
        return null;
    }

    public void clearTitle(UUID uuid) {
        titles.remove(uuid);
        storage.set(uuid.toString(), null);
        save();
    }

    /**
     * @return окрашенный тег игрока (с кодами §), либо null, если тег не установлен.
     */
    public String getTitle(UUID uuid) {
        return titles.get(uuid);
    }
}
