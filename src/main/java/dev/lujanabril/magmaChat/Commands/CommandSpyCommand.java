package dev.lujanabril.magmaChat.Commands;

import dev.lujanabril.magmaChat.Managers.CommandSpyManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CommandSpyCommand implements CommandExecutor {
    private final CommandSpyManager manager;

    public CommandSpyCommand(CommandSpyManager manager) {
        this.manager = manager;
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player player) {
            if (!player.hasPermission("magmachat.commandspy")) {
                String noPermMessage = this.manager.processColors(this.manager.getPlugin().getConfig().getString("commandspy.messages.no-permission", "&cYou don't have permission to use this command."));
                player.sendMessage(LegacyComponentSerializer.legacy('§').deserialize(noPermMessage));
                return true;
            } else {
                if (this.manager.isSpy(player)) {
                    this.manager.disableSpy(player);
                } else {
                    this.manager.enableSpy(player);
                }

                return true;
            }
        } else {
            sender.sendMessage(Component.text("This command can only be used by players.", NamedTextColor.RED));
            return true;
        }
    }
}
