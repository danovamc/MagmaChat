package dev.lujanabril.magmaChat.Managers;

import dev.lujanabril.magmaChat.Main;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;

public class CommandSpyManager implements Listener {
    private final Main plugin;
    private final Set<UUID> spyingPlayers;
    private final Set<String> whitelistedCommands;
    private final String spyFormat;

    public CommandSpyManager(Main plugin) {
        this.plugin = plugin;
        this.spyingPlayers = new HashSet();
        this.whitelistedCommands = new HashSet();
        this.spyFormat = plugin.getConfig().getString("commandspy.format", "&8[&cSPY&8] &7{player}: &f{command}");
        this.loadWhitelistedCommands();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    private void loadWhitelistedCommands() {
        this.whitelistedCommands.clear();
        if (this.plugin.getConfig().contains("commandspy.whitelisted-commands")) {
            this.whitelistedCommands.addAll(this.plugin.getConfig().getStringList("commandspy.whitelisted-commands"));
        }

    }

    public boolean enableSpy(Player player) {
        if (this.spyingPlayers.add(player.getUniqueId())) {
            String enableMessage = this.processColors(this.plugin.getConfig().getString("commandspy.messages.enabled", "&aCommandSpy has been activated."));
            player.sendMessage(LegacyComponentSerializer.legacy('§').deserialize(enableMessage));
            return true;
        } else {
            return false;
        }
    }

    public boolean disableSpy(Player player) {
        if (this.spyingPlayers.remove(player.getUniqueId())) {
            String disableMessage = this.processColors(this.plugin.getConfig().getString("commandspy.messages.disabled", "&cCommandSpy has been deactivated."));
            player.sendMessage(LegacyComponentSerializer.legacy('§').deserialize(disableMessage));
            return true;
        } else {
            return false;
        }
    }

    public boolean isSpy(Player player) {
        return this.spyingPlayers.contains(player.getUniqueId());
    }

    public String processColors(String message) {
        if (message.contains("#")) {
            for(int i = 0; i < message.length() - 6; ++i) {
                if (message.charAt(i) == '#') {
                    String hex = message.substring(i, i + 7);
                    if (hex.matches("#[a-fA-F0-9]{6}")) {
                        message = message.replace(hex, "" + String.valueOf(ChatColor.of(hex)));
                    }
                }
            }
        }

        return ChatColor.translateAlternateColorCodes('&', message);
    }

    private void sendSpyMessage(Player source, String command) {
        boolean isWhitelisted = false;
        String commandRoot = command.split(" ")[0].toLowerCase();
        if (this.whitelistedCommands.contains(commandRoot)) {
            isWhitelisted = true;
        }

        if (isWhitelisted) {
            String formattedMessage = this.spyFormat.replace("{player}", source.getName()).replace("{displayname}", source.getDisplayName()).replace("{command}", command);
            formattedMessage = this.processColors(formattedMessage);
            Component component = LegacyComponentSerializer.legacy('§').deserialize(formattedMessage);

            for(UUID uuid : this.spyingPlayers) {
                Player player = Bukkit.getPlayer(uuid);
                if (player != null && !player.equals(source) && player.hasPermission("magmachat.commandspy")) {
                    player.sendMessage(component);
                }
            }

        }
    }

    @EventHandler(
            priority = EventPriority.MONITOR,
            ignoreCancelled = true
    )
    public void onCommand(PlayerCommandPreprocessEvent event) {
        if (!event.getPlayer().hasPermission("magmachat.commandspy.bypass")) {
            this.sendSpyMessage(event.getPlayer(), event.getMessage());
        }

    }

    @EventHandler(
            priority = EventPriority.MONITOR
    )
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission("magmachat.commandspy.auto")) {
            this.enableSpy(player);
        }

    }

    public Main getPlugin() {
        return this.plugin;
    }
}
