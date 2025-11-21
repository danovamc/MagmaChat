package dev.lujanabril.magmaChat.Listeners;

import dev.lujanabril.magmaChat.Main;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

public class CommandListener implements Listener {
    private final Main plugin;
    private final LegacyComponentSerializer serializer;

    public CommandListener(Main plugin) {
        this.plugin = plugin;
        this.serializer = LegacyComponentSerializer.legacyAmpersand();
    }

    @EventHandler(
            priority = EventPriority.LOWEST
    )
    public void onCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        if (!player.hasPermission("magmachat.bypass.commandspam")) {
            if (this.plugin.getChatManager().isCommandSpam(player)) {
                event.setCancelled(true);
                double remainingTime = this.plugin.getChatManager().getRemainingCommandCooldown(player);
                String commandSpamMessage = this.plugin.getConfig().getString("messages.command-spam", "&cPlease wait {cooldown} seconds before using another command!");
                commandSpamMessage = commandSpamMessage.replace("{cooldown}", String.valueOf(remainingTime));
                player.sendMessage(this.serializer.deserialize(commandSpamMessage));
            }

        }
    }
}