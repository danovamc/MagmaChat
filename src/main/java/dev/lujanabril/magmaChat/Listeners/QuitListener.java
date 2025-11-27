package dev.lujanabril.magmaChat.Listeners;

import dev.lujanabril.magmaChat.Main;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class QuitListener implements Listener {
    private final Main plugin;

    public QuitListener(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Limpiar datos del jugador para liberar memoria
        if (this.plugin.getChatManager() != null) {
            this.plugin.getChatManager().cleanupPlayer(event.getPlayer().getUniqueId());
        }
    }
}