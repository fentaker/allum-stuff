package net.allumgo.stuff;

import net.allumgo.stuff.commands.AllumStuffCommand;
import net.allumgo.stuff.commands.NSkipCommand;
import net.allumgo.stuff.commands.RSkipCommand;
import net.allumgo.stuff.commands.TitleCommand;
import net.allumgo.stuff.restart.RestartManager;
import net.allumgo.stuff.chat.ChatBubbleManager;
import net.allumgo.stuff.chat.ChatManager;
import net.allumgo.stuff.sleep.SleepManager;
import net.allumgo.stuff.title.TitleManager;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Главный класс плагина AllumStuff.
 * Объединяет 4 независимых модуля: авторестарт, чат (локальный/глобальный + пузыри),
 * пропуск ночи и теги игроков перед ником.
 */
public final class AllumStuffPlugin extends JavaPlugin {

    private RestartManager restartManager;
    private ChatManager chatManager;
    private ChatBubbleManager chatBubbleManager;
    private SleepManager sleepManager;
    private TitleManager titleManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.titleManager = new TitleManager(this);
        this.chatBubbleManager = new ChatBubbleManager(this);
        this.chatManager = new ChatManager(this, titleManager, chatBubbleManager);
        this.sleepManager = new SleepManager(this);
        this.restartManager = new RestartManager(this);

        getServer().getPluginManager().registerEvents(chatManager, this);
        getServer().getPluginManager().registerEvents(sleepManager, this);

        restartManager.start();

        getCommand("rskip").setExecutor(new RSkipCommand(restartManager));
        getCommand("nskip").setExecutor(new NSkipCommand(sleepManager));
        getCommand("title").setExecutor(new TitleCommand(titleManager));
        getCommand("allumstuff").setExecutor(new AllumStuffCommand(this));

        getLogger().info("AllumStuff включён.");
    }

    @Override
    public void onDisable() {
        if (restartManager != null) {
            restartManager.stop();
        }
        if (sleepManager != null) {
            sleepManager.cancelAll();
        }
        if (chatBubbleManager != null) {
            chatBubbleManager.clearAll();
        }
        getLogger().info("AllumStuff выключен.");
    }

    /**
     * Перезагружает config.yml и применяет значения ко всем модулям.
     */
    public void reloadAll() {
        reloadConfig();
        restartManager.reloadFromConfig();
        chatManager.reloadFromConfig();
        chatBubbleManager.reloadFromConfig();
        sleepManager.reloadFromConfig();
        titleManager.reloadFromConfig();
    }

    public RestartManager getRestartManager() {
        return restartManager;
    }

    public ChatManager getChatManager() {
        return chatManager;
    }

    public SleepManager getSleepManager() {
        return sleepManager;
    }

    public TitleManager getTitleManager() {
        return titleManager;
    }
}
