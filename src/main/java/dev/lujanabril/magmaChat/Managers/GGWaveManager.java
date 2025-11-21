package dev.lujanabril.magmaChat.Managers;

import dev.lujanabril.magmaChat.Main;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

public class GGWaveManager {
    private final Main plugin;
    private boolean ggWaveActive = false;
    private BukkitTask ggWaveTask = null;
    private final Pattern ggPattern = Pattern.compile("(?i)\\bGG\\b");
    private final Random random = new Random();

    public GGWaveManager(Main plugin) {
        this.plugin = plugin;
    }

    public boolean startGGWave(CommandSender sender) {
        if (this.ggWaveActive) {
            List<String> alreadyActiveMessages = this.plugin.getConfig().getStringList("messages.ggwave-already-active");
            if (alreadyActiveMessages.isEmpty()) {
                alreadyActiveMessages.add("<red>Una ola GG ya está activa. Espera a que termine.</red>");
            }

            for(String message : alreadyActiveMessages) {
                sender.sendMessage(MiniMessage.miniMessage().deserialize(message));
            }

            return false;
        } else {
            this.ggWaveActive = true;
            List<String> startMessages = this.plugin.getConfig().getStringList("messages.ggwave-started");
            if (startMessages.isEmpty()) {
                startMessages.add("<green>¡Una ola GG ha comenzado! Todos los mensajes con 'GG' serán coloreados por 30 segundos!</green>");
            }

            for(String message : startMessages) {
                Bukkit.getServer().broadcast(MiniMessage.miniMessage().deserialize(message));
            }

            this.ggWaveTask = (new BukkitRunnable() {
                public void run() {
                    GGWaveManager.this.stopGGWave();
                    List<String> endMessages = GGWaveManager.this.plugin.getConfig().getStringList("messages.ggwave-ended");
                    if (endMessages.isEmpty()) {
                        endMessages.add("<gold>La ola GG ha terminado. ¡Gracias por participar!</gold>");
                    }

                    for(String message : endMessages) {
                        Bukkit.getServer().broadcast(MiniMessage.miniMessage().deserialize(message));
                    }

                }
            }).runTaskLaterAsynchronously(this.plugin, 600L);
            return true;
        }
    }

    public boolean stopGGWave() {
        if (!this.ggWaveActive) {
            return false;
        } else {
            this.ggWaveActive = false;
            if (this.ggWaveTask != null && !this.ggWaveTask.isCancelled()) {
                this.ggWaveTask.cancel();
                this.ggWaveTask = null;
            }

            return true;
        }
    }

    public boolean isGGWaveActive() {
        return this.ggWaveActive;
    }

    public String processGGWave(String message) {
        if (!this.ggWaveActive) {
            return message;
        } else {
            StringBuffer result = new StringBuffer();
            Matcher matcher = this.ggPattern.matcher(message);

            while(matcher.find()) {
                Object[] var10001 = new Object[]{this.random.nextInt(16777215)};
                String randomColor = "#" + String.format("%06X", var10001);
                matcher.appendReplacement(result, randomColor + "&lGG&r");
            }

            matcher.appendTail(result);
            return HEXColor.message(result.toString());
        }
    }
}
