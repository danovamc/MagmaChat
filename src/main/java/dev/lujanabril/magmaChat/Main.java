package dev.lujanabril.magmaChat;

import dev.lujanabril.magmaChat.Commands.CommandSpyCommand;
import dev.lujanabril.magmaChat.Commands.MagmaChatCommand;
import dev.lujanabril.magmaChat.Commands.MessageCommands;
import dev.lujanabril.magmaChat.Commands.PingCommand;
import dev.lujanabril.magmaChat.Listeners.AsyncChatListener;
import dev.lujanabril.magmaChat.Listeners.CommandListener;
import dev.lujanabril.magmaChat.Listeners.JoinListener;
import dev.lujanabril.magmaChat.Listeners.QuitListener;
import dev.lujanabril.magmaChat.Managers.ChatManager;
import dev.lujanabril.magmaChat.Managers.CommandSpyManager;
import dev.lujanabril.magmaChat.Managers.GGWaveManager;
import dev.lujanabril.magmaChat.Managers.PlayerDataManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class Main extends JavaPlugin {
    private ChatManager chatManager;
    private CommandSpyManager commandSpyManager;
    private PlayerDataManager playerDataManager;
    private MessageCommands messageCommands;
    private GGWaveManager ggWaveManager;

    public void onEnable() {
        this.saveDefaultConfig();
        this.chatManager = new ChatManager(this);
        this.playerDataManager = new PlayerDataManager(this);
        this.registerCommands();
        this.registerListeners();
    }

    public void registerCommands() {
        this.commandSpyManager = new CommandSpyManager(this);
        this.messageCommands = new MessageCommands(this);
        this.ggWaveManager = new GGWaveManager(this);
        this.getCommand("commandspy").setExecutor(new CommandSpyCommand(this.commandSpyManager));
        this.getCommand("magmachat").setExecutor(new MagmaChatCommand(this));
        this.getCommand("msg").setExecutor(this.messageCommands);
        this.getCommand("reply").setExecutor(this.messageCommands);
        this.getCommand("broadcast").setExecutor(this.messageCommands);
        this.getCommand("slur").setExecutor(this.messageCommands);
        this.getCommand("mutechat").setExecutor(this.messageCommands);
        this.getCommand("ping").setExecutor(new PingCommand(this));
        this.getCommand("ignore").setExecutor(new MessageCommands(this));
        this.getCommand("msgtoggle").setExecutor(new MessageCommands(this));
        this.getCommand("togglechat").setExecutor(new MessageCommands(this));
    }

    public void registerListeners() {
        this.getServer().getPluginManager().registerEvents(new AsyncChatListener(this), this);
        this.getServer().getPluginManager().registerEvents(new CommandListener(this), this);
        this.getServer().getPluginManager().registerEvents(new JoinListener(this), this);
        this.getServer().getPluginManager().registerEvents(new QuitListener(this), this);
    }

    public void onDisable() {
        this.playerDataManager.close();
    }

    public ChatManager getChatManager() {
        return this.chatManager;
    }

    public GGWaveManager getGGWaveManager() {
        return this.ggWaveManager;
    }

    public MessageCommands getMessageCommands() {
        return this.messageCommands;
    }

    public PlayerDataManager getPlayerDataManager() {
        return this.playerDataManager;
    }
}