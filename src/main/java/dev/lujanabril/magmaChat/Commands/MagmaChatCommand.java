package dev.lujanabril.magmaChat.Commands;

import dev.lujanabril.magmaChat.Main;
import dev.lujanabril.magmaChat.Managers.GGWaveManager;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;

public class MagmaChatCommand implements CommandExecutor, TabCompleter {
    private final Main plugin;

    public MagmaChatCommand(Main plugin) {
        this.plugin = plugin;
    }

    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        switch (args[0].toLowerCase()) {
            case "reload":
                if (!sender.hasPermission("magmachat.reload")) {
                    sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>You don't have permission to use this command!</red>"));
                    return true;
                }

                this.plugin.getChatManager().reloadConfig();
                sender.sendMessage(MiniMessage.miniMessage().deserialize("<green>MagmaChat configuration reloaded!</green>"));
                return true;
            case "ggwave":
                if (!sender.hasPermission("magmachat.ggwave")) {
                    sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>No tienes permiso para usar este comando!</red>"));
                    return true;
                } else if (args.length < 2) {
                    sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Uso: /magmachat ggwave <start|stop></red>"));
                    return true;
                } else {
                    GGWaveManager ggWaveManager = this.plugin.getGGWaveManager();
                    if (args[1].equalsIgnoreCase("start")) {
                        if (ggWaveManager.startGGWave(sender)) {
                            sender.sendMessage(MiniMessage.miniMessage().deserialize("<green>¡Ola GG iniciada correctamente!</green>"));
                        }

                        return true;
                    } else {
                        if (args[1].equalsIgnoreCase("stop")) {
                            if (ggWaveManager.stopGGWave()) {
                                sender.sendMessage(MiniMessage.miniMessage().deserialize("<green>¡Ola GG detenida correctamente!</green>"));
                            } else {
                                sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>No hay ninguna ola GG activa actualmente.</red>"));
                            }

                            return true;
                        }

                        sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Subcomando desconocido. Usa: /magmachat ggwave <start|stop></red>"));
                        return true;
                    }
                }
            default:
                sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Unknown subcommand. Available commands: reload, ggwave</red>"));
                return true;
        }
    }

    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
        if (args.length == 1) {
            List<String> subcommands = new ArrayList();
            if (sender.hasPermission("magmachat.reload")) {
                subcommands.add("reload");
            }

            if (sender.hasPermission("magmachat.ggwave")) {
                subcommands.add("ggwave");
            }

            return this.filterCompletions(subcommands, args[0]);
        } else {
            return (List<String>)(args.length == 2 && args[0].equalsIgnoreCase("ggwave") && sender.hasPermission("magmachat.ggwave") ? this.filterCompletions(Arrays.asList("start", "stop"), args[1]) : new ArrayList());
        }
    }

    private List<String> filterCompletions(List<String> completions, String input) {
        return (List)completions.stream().filter((s) -> s.toLowerCase().startsWith(input.toLowerCase())).collect(Collectors.toList());
    }
}