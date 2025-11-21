package dev.lujanabril.magmaChat.Commands;

import dev.lujanabril.magmaChat.Main;
import java.util.Set;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.metadata.MetadataValue;
import org.jetbrains.annotations.NotNull;

public class MessageCommands implements CommandExecutor {
    private final Main plugin;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private final LegacyComponentSerializer legacySerializer = LegacyComponentSerializer.legacyAmpersand();
    private boolean chatMuted = false;

    public MessageCommands(Main plugin) {
        this.plugin = plugin;
    }

    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        switch (command.getName().toLowerCase()) {
            case "msg" -> {
                return this.handleMessage(sender, args);
            }
            case "reply" -> {
                return this.handleReply(sender, args);
            }
            case "broadcast" -> {
                return this.handleBroadcast(sender, args);
            }
            case "slur" -> {
                return this.handleSlurCommand(sender, args);
            }
            case "ignore" -> {
                return this.handleIgnoreCommand(sender, args);
            }
            case "msgtoggle" -> {
                return this.handleMsgToggleCommand(sender, args);
            }
            case "mutechat" -> {
                return this.handleMuteChatCommand(sender, args);
            }
            case "togglechat" -> {
                return this.handleToggleChatCommand(sender, args);
            }
            default -> {
                return false;
            }
        }
    }

    private void playSound(Player player, String soundConfigPath) {
        if (this.plugin.getConfig().getBoolean(soundConfigPath + ".enabled", true)) {
            String soundName = this.plugin.getConfig().getString(soundConfigPath + ".sound", "entity.experience_orb.pickup");
            float volume = (float)this.plugin.getConfig().getDouble(soundConfigPath + ".volume", (double)1.0F);
            float pitch = (float)this.plugin.getConfig().getDouble(soundConfigPath + ".pitch", (double)1.0F);
            player.playSound(player.getLocation(), soundName, volume, pitch);
        }

    }

    private boolean isVanished(Player player) {
        if (player.hasMetadata("vanished")) {
            for(MetadataValue meta : player.getMetadata("vanished")) {
                if (meta.asBoolean()) {
                    return true;
                }
            }
        }

        if (player.hasMetadata("essentials.vanished")) {
            for(MetadataValue meta : player.getMetadata("essentials.vanished")) {
                if (meta.asBoolean()) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean handleMessage(CommandSender sender, String[] args) {
        if (!(sender instanceof Player playerSender)) {
            sender.sendMessage(Component.text("Only players can use this command!").color(NamedTextColor.RED));
            return true;
        } else if (args.length < 2) {
            String msgUsage = this.plugin.getConfig().getString("messages.msg-usage", "<red>Uso correcto: /msg <jugador> <mensaje>");
            sender.sendMessage(this.miniMessage.deserialize(msgUsage));
            return true;
        } else {
            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                String playerOffline = this.plugin.getConfig().getString("messages.player-offline");
                sender.sendMessage(this.miniMessage.deserialize(playerOffline));
                return true;
            } else if (this.isVanished(target) && !playerSender.hasPermission("magmachat.seevanish")) {
                String playerOffline = this.plugin.getConfig().getString("messages.player-offline");
                sender.sendMessage(this.miniMessage.deserialize(playerOffline));
                return true;
            } else if (this.plugin.getPlayerDataManager().isIgnoring(playerSender, target)) {
                String ignoringMessage = this.plugin.getConfig().getString("messages.ignoring-player", "<red>Estás ignorando a este jugador. Designóralo con /ignore remove <jugador>.");
                sender.sendMessage(this.miniMessage.deserialize(ignoringMessage));
                return true;
            } else if (this.plugin.getPlayerDataManager().isIgnoring(target, playerSender)) {
                String beingIgnoredMessage = this.plugin.getConfig().getString("messages.being-ignored", "<red>Este jugador no está recibiendo tus mensajes.");
                sender.sendMessage(this.miniMessage.deserialize(beingIgnoredMessage));
                return true;
            } else if (this.plugin.getPlayerDataManager().isMessagingDisabled(target)) {
                String messagesDisabledMessage = this.plugin.getConfig().getString("messages.messages-disabled", "<red>Este jugador tiene los mensajes privados desactivados.");
                sender.sendMessage(this.miniMessage.deserialize(messagesDisabledMessage));
                return true;
            } else {
                StringBuilder message = new StringBuilder();

                for(int i = 1; i < args.length; ++i) {
                    message.append(args[i]).append(" ");
                }

                String outgoingFormat = this.plugin.getConfig().getString("messages.msg-format-outgoing", "<gray>To <player>: <message>");
                String incomingFormat = this.plugin.getConfig().getString("messages.msg-format-incoming", "<gray>From <player>: <message>");
                Component outgoingMessage = this.miniMessage.deserialize(outgoingFormat.replace("<player>", target.getName()).replace("<message>", message.toString().trim()));
                Component incomingMessage = this.miniMessage.deserialize(incomingFormat.replace("<player>", sender.getName()).replace("<message>", message.toString().trim()));
                target.sendMessage(incomingMessage);
                sender.sendMessage(outgoingMessage);
                this.plugin.getChatManager().setReplyTarget(playerSender, target);
                this.plugin.getPlayerDataManager().setLastMessageSender(target, playerSender);
                this.playSound(target, "messages.msg-sound");
                return true;
            }
        }
    }

    private boolean handleReply(CommandSender sender, String[] args) {
        if (!(sender instanceof Player playerSender)) {
            sender.sendMessage(Component.text("¡Solo los jugadores pueden usar este comando!").color(NamedTextColor.RED));
            return true;
        } else if (args.length < 1) {
            sender.sendMessage(Component.text("Uso correcto: /r <mensaje>").color(NamedTextColor.RED));
            return true;
        } else {
            UUID targetUUID = this.plugin.getPlayerDataManager().getLastMessageSender(playerSender);
            if (targetUUID == null) {
                targetUUID = this.plugin.getChatManager().getReplyTarget(playerSender);
            }

            if (targetUUID == null) {
                String noReplyTarget = this.plugin.getConfig().getString("messages.no-reply-target", "<red>¡No tienes a nadie a quien responder!");
                sender.sendMessage(this.miniMessage.deserialize(noReplyTarget));
                return true;
            } else {
                Player target = Bukkit.getPlayer(targetUUID);
                if (target == null) {
                    String playerOffline = this.plugin.getConfig().getString("messages.player-offline", "<red>¡El jugador está desconectado!");
                    sender.sendMessage(this.miniMessage.deserialize(playerOffline));
                    return true;
                } else if (this.isVanished(target) && !playerSender.hasPermission("magmachat.seevanish")) {
                    String playerOffline = this.plugin.getConfig().getString("messages.player-offline", "<red>¡El jugador está desconectado!");
                    sender.sendMessage(this.miniMessage.deserialize(playerOffline));
                    return true;
                } else {
                    String[] msgArgs = new String[args.length + 1];
                    msgArgs[0] = target.getName();
                    System.arraycopy(args, 0, msgArgs, 1, args.length);
                    return this.handleMessage(sender, msgArgs);
                }
            }
        }
    }

    private boolean handleBroadcast(CommandSender sender, String[] args) {
        if (!sender.hasPermission("magmachat.broadcast")) {
            sender.sendMessage(Component.text("You don't have permission to use this command!").color(NamedTextColor.RED));
            return true;
        } else if (args.length < 1) {
            sender.sendMessage(Component.text("Usage: /broadcast <message>").color(NamedTextColor.RED));
            return true;
        } else {
            String message = String.join(" ", args);
            String broadcastFormat = this.plugin.getConfig().getString("messages.broadcast-format", "<red>[Broadcast] <white><message>");
            String linePrefix = this.plugin.getConfig().getString("messages.broadcast-line-prefix", "<red>[Broadcast] <white>");
            String[] lines = message.split("/n");

            for(int i = 0; i < lines.length; ++i) {
                String line = lines[i].trim();
                if (i == 0) {
                    Component coloredMessage = this.legacySerializer.deserialize(line);
                    String broadcastWithoutMessage = broadcastFormat.replace("<message>", "");
                    Component prefixComponent = this.miniMessage.deserialize(broadcastWithoutMessage);
                    Component broadcast = prefixComponent.append(coloredMessage);
                    Bukkit.broadcast(broadcast);
                } else {
                    Component prefixComponent = this.miniMessage.deserialize(linePrefix);
                    Component coloredMessage = this.legacySerializer.deserialize(line);
                    Component broadcast = prefixComponent.append(coloredMessage);
                    Bukkit.broadcast(broadcast);
                }
            }

            for(Player player : Bukkit.getOnlinePlayers()) {
                this.playSound(player, "messages.broadcast-sound");
            }

            return true;
        }
    }

    private boolean handleSlurCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("magmachat.slurs.manage")) {
            sender.sendMessage(Component.text("You don't have permission to use this command!").color(NamedTextColor.RED));
            return true;
        } else if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /slur <add|remove> <word>").color(NamedTextColor.RED));
            return true;
        } else {
            String action = args[0].toLowerCase();
            String word = args[1].toLowerCase();
            switch (action) {
                case "add":
                    this.plugin.getChatManager().addSlur(word);
                    sender.sendMessage(Component.text("Added word to filter.").color(NamedTextColor.GREEN));
                    break;
                case "remove":
                    this.plugin.getChatManager().removeSlur(word);
                    sender.sendMessage(Component.text("Removed word from filter.").color(NamedTextColor.GREEN));
                    break;
                default:
                    sender.sendMessage(Component.text("Invalid action! Use 'add' or 'remove'").color(NamedTextColor.RED));
            }

            return true;
        }
    }

    private boolean handleIgnoreCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player playerSender)) {
            sender.sendMessage(Component.text("¡Solo los jugadores pueden usar este comando!").color(NamedTextColor.RED));
            return true;
        } else if (args.length < 1) {
            Set<UUID> ignoredPlayers = this.plugin.getPlayerDataManager().getIgnoredPlayers(playerSender);
            String header = this.plugin.getConfig().getString("messages.ignore-list-header", "<yellow>Jugadores que estás ignorando:</yellow>");
            sender.sendMessage(this.miniMessage.deserialize(header));
            if (ignoredPlayers.isEmpty()) {
                String emptyList = this.plugin.getConfig().getString("messages.ignore-list-empty", "<gray>No estás ignorando a ningún jugador.</gray>");
                sender.sendMessage(this.miniMessage.deserialize(emptyList));
            } else {
                for(UUID uuid : ignoredPlayers) {
                    String playerName = Bukkit.getOfflinePlayer(uuid).getName();
                    if (playerName != null) {
                        sender.sendMessage(Component.text(" - " + playerName).color(NamedTextColor.GRAY));
                    }
                }
            }

            String usage = this.plugin.getConfig().getString("messages.ignore-usage", "<gray>Uso: /ignore <add|remove> <jugador></gray>");
            sender.sendMessage(this.miniMessage.deserialize(usage));
            return true;
        } else {
            String action = args[0].toLowerCase();
            if (args.length < 2 && !action.equals("list")) {
                String usage = this.plugin.getConfig().getString("messages.ignore-usage", "<gray>Uso: /ignore <add|remove> <jugador></gray>");
                sender.sendMessage(this.miniMessage.deserialize(usage));
                return true;
            } else if (!action.equals("list")) {
                String targetName = args[1];
                Player target = Bukkit.getPlayer(targetName);
                if (target == null) {
                    String playerNotFound = this.plugin.getConfig().getString("messages.player-not-found", "<red>¡Jugador no encontrado!</red>");
                    sender.sendMessage(this.miniMessage.deserialize(playerNotFound));
                    return true;
                } else if (target.equals(playerSender)) {
                    String cannotIgnoreSelf = this.plugin.getConfig().getString("messages.cannot-ignore-self", "<red>¡No puedes ignorarte a ti mismo!</red>");
                    sender.sendMessage(this.miniMessage.deserialize(cannotIgnoreSelf));
                    return true;
                } else {
                    switch (action) {
                        case "add":
                            if (this.plugin.getPlayerDataManager().addIgnore(playerSender, target)) {
                                String playerIgnored = this.plugin.getConfig().getString("messages.player-ignored", "<green>Ahora estás ignorando a <player>.</green>");
                                sender.sendMessage(this.miniMessage.deserialize(playerIgnored.replace("<player>", target.getName())));
                            } else {
                                String alreadyIgnoring = this.plugin.getConfig().getString("messages.already-ignoring", "<yellow>Ya estás ignorando a <player>.</yellow>");
                                sender.sendMessage(this.miniMessage.deserialize(alreadyIgnoring.replace("<player>", target.getName())));
                            }
                            break;
                        case "remove":
                            if (this.plugin.getPlayerDataManager().removeIgnore(playerSender, target)) {
                                String playerUnignored = this.plugin.getConfig().getString("messages.player-unignored", "<green>Ya no estás ignorando a <player>.</green>");
                                sender.sendMessage(this.miniMessage.deserialize(playerUnignored.replace("<player>", target.getName())));
                            } else {
                                String notIgnoring = this.plugin.getConfig().getString("messages.not-ignoring", "<yellow>No estás ignorando a <player>.</yellow>");
                                sender.sendMessage(this.miniMessage.deserialize(notIgnoring.replace("<player>", target.getName())));
                            }
                            break;
                        default:
                            String invalidAction = this.plugin.getConfig().getString("messages.invalid-ignore-action", "<red>¡Acción inválida! Usa 'add', 'remove' o 'list'</red>");
                            sender.sendMessage(this.miniMessage.deserialize(invalidAction));
                    }

                    return true;
                }
            } else {
                Set<UUID> ignoredPlayers = this.plugin.getPlayerDataManager().getIgnoredPlayers(playerSender);
                String header = this.plugin.getConfig().getString("messages.ignore-list-header", "<yellow>Jugadores que estás ignorando:</yellow>");
                sender.sendMessage(this.miniMessage.deserialize(header));
                if (ignoredPlayers.isEmpty()) {
                    String emptyList = this.plugin.getConfig().getString("messages.ignore-list-empty", "<gray>No estás ignorando a ningún jugador.</gray>");
                    sender.sendMessage(this.miniMessage.deserialize(emptyList));
                } else {
                    for(UUID uuid : ignoredPlayers) {
                        String playerName = Bukkit.getOfflinePlayer(uuid).getName();
                        if (playerName != null) {
                            sender.sendMessage(Component.text(" - " + playerName).color(NamedTextColor.GRAY));
                        }
                    }
                }

                return true;
            }
        }
    }

    private boolean handleMsgToggleCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player playerSender)) {
            sender.sendMessage(Component.text("¡Solo los jugadores pueden usar este comando!").color(NamedTextColor.RED));
            return true;
        } else {
            boolean newState = this.plugin.getPlayerDataManager().toggleMessaging(playerSender);
            String message;
            if (newState) {
                message = this.plugin.getConfig().getString("messages.msg-toggle-enabled", "<green>Ahora estás recibiendo mensajes privados.</green>");
            } else {
                message = this.plugin.getConfig().getString("messages.msg-toggle-disabled", "<yellow>Ya no estás recibiendo mensajes privados.</yellow>");
            }

            sender.sendMessage(this.miniMessage.deserialize(message));
            return true;
        }
    }

    private boolean handleMuteChatCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("magmachat.mutechat")) {
            sender.sendMessage(Component.text("¡No tienes permiso para usar este comando!").color(NamedTextColor.RED));
            return true;
        } else {
            this.chatMuted = !this.chatMuted;
            String muteMessage = this.plugin.getConfig().getString("messages.chat-muted", "<red>¡El chat ha sido silenciado por <sender>!</red>");
            String unmuteMessage = this.plugin.getConfig().getString("messages.chat-unmuted", "<green>¡El chat ha sido reactivado por <sender>!</green>");
            String senderName = sender instanceof Player ? sender.getName() : "Consola";
            String broadcastMessage = this.chatMuted ? muteMessage.replace("<sender>", senderName) : unmuteMessage.replace("<sender>", senderName);
            Component message = this.miniMessage.deserialize(broadcastMessage);
            Bukkit.broadcast(message);

            for(Player player : Bukkit.getOnlinePlayers()) {
                this.playSound(player, this.chatMuted ? "messages.chat-muted-sound" : "messages.chat-unmuted-sound");
            }

            return true;
        }
    }

    private boolean handleToggleChatCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player playerSender)) {
            sender.sendMessage(Component.text("¡Solo los jugadores pueden usar este comando!").color(NamedTextColor.RED));
            return true;
        } else {
            boolean newState = this.plugin.getPlayerDataManager().toggleChatVisibility(playerSender);
            String message;
            if (newState) {
                message = this.plugin.getConfig().getString("messages.chat-toggle-enabled", "<green>Ahora puedes ver los mensajes del chat.</green>");
            } else {
                message = this.plugin.getConfig().getString("messages.chat-toggle-disabled", "<yellow>Ya no verás mensajes del chat público.</yellow>");
            }

            sender.sendMessage(this.miniMessage.deserialize(message));
            return true;
        }
    }

    public boolean isChatMuted() {
        return this.chatMuted;
    }
}