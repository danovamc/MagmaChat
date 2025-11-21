package dev.lujanabril.magmaChat.Commands;

import dev.lujanabril.magmaChat.Main;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class PingCommand implements CommandExecutor {
    private final Main plugin;

    public PingCommand(Main plugin) {
        this.plugin = plugin;
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (sender instanceof Player) {
                Player player = (Player)sender;
                int ping = player.getPing();
                String rawMessage = this.plugin.getConfig().getString("messages.ping-self", "&aYour ping is &e%ping%ms&a.");
                rawMessage = rawMessage.replace("%ping%", String.valueOf(ping));
                Component message = LegacyComponentSerializer.legacyAmpersand().deserialize(rawMessage);
                player.sendMessage(message);
            } else {
                String rawMessage = this.plugin.getConfig().getString("messages.console-error", "&cOnly players can check their own ping.");
                Component message = LegacyComponentSerializer.legacyAmpersand().deserialize(rawMessage);
                sender.sendMessage(message);
            }

            return true;
        } else {
            String targetName = args[0];
            Player target = Bukkit.getPlayer(targetName);
            if (target == null) {
                String rawMessage = this.plugin.getConfig().getString("messages.player-not-found", "&cPlayer &e%player%&c not found.");
                rawMessage = rawMessage.replace("%player%", targetName);
                Component message = LegacyComponentSerializer.legacyAmpersand().deserialize(rawMessage);
                sender.sendMessage(message);
                return true;
            } else {
                int ping = target.getPing();
                String rawMessage = this.plugin.getConfig().getString("messages.ping-other", "&a%player%'s ping is &e%ping%ms&a.");
                rawMessage = rawMessage.replace("%player%", target.getName()).replace("%ping%", String.valueOf(ping));
                Component message = LegacyComponentSerializer.legacyAmpersand().deserialize(rawMessage);
                sender.sendMessage(message);
                return true;
            }
        }
    }
}