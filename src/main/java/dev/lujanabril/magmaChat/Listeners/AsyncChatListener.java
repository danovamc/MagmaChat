package dev.lujanabril.magmaChat.Listeners;

import dev.lujanabril.magmaChat.Main;
import dev.lujanabril.magmaChat.Managers.HEXColor;
import dev.lujanabril.magmaChat.Renderer.MagmaChatRenderer;
import io.papermc.paper.event.player.AsyncChatEvent;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class AsyncChatListener implements Listener {
    private final Main plugin;
    private final MagmaChatRenderer magmaChatRenderer;

    public AsyncChatListener(Main plugin) {
        this.plugin = plugin;
        this.magmaChatRenderer = new MagmaChatRenderer(plugin);
    }

    @EventHandler(
            priority = EventPriority.LOWEST
    )
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        if (this.plugin.getMessageCommands().isChatMuted() && !player.hasPermission("magmachat.bypass.mutechat")) {
            String chatMutedMessage = this.plugin.getConfig().getString("messages.chat-is-muted", "<red>El chat está silenciado actualmente.</red>");
            player.sendMessage(MiniMessage.miniMessage().deserialize(chatMutedMessage));
            event.setCancelled(true);
        } else {
            String rawMessage = "";
            if (event.message() instanceof TextComponent) {
                rawMessage = ((TextComponent)event.message()).content();
            } else {
                rawMessage = event.message().toString();
            }

            if (this.plugin.getChatManager().containsSlur(rawMessage)) {
                event.setCancelled(true);
                String slurMessage = this.plugin.getConfig().getString("messages.slur-detected", "&cYour message contains prohibited words!");
                player.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(slurMessage));
            } else if (this.plugin.getChatManager().containsServerIp(rawMessage) && !player.hasPermission("magmachat.bypass.ipfilter")) {
                event.setCancelled(true);
                String ipMessage = this.plugin.getConfig().getString("messages.server-ip-detected", "&cServer IP addresses are not allowed in chat!");
                player.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(ipMessage));
            } else if (this.plugin.getChatManager().isSpam(player)) {
                event.setCancelled(true);
                double remainingTime = this.plugin.getChatManager().getRemainingChatCooldown(player);
                String spamMessage = this.plugin.getConfig().getString("messages.chat-spam", "&cPlease wait {cooldown} seconds before sending another message!");
                spamMessage = spamMessage.replace("{cooldown}", String.valueOf(remainingTime));
                player.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(spamMessage));
            } else {
                String processedMessage = this.plugin.getChatManager().processMessage(rawMessage, player);
                if (this.plugin.getGGWaveManager() != null && this.plugin.getGGWaveManager().isGGWaveActive()) {
                    processedMessage = this.plugin.getGGWaveManager().processGGWave(processedMessage);
                }

                if (!processedMessage.equals(rawMessage)) {
                    Component messageComponent;
                    if (processedMessage.contains("#")) {
                        messageComponent = LegacyComponentSerializer.legacySection().deserialize(HEXColor.message(processedMessage));
                    } else {
                        messageComponent = Component.text(processedMessage);
                    }

                    event.message(messageComponent);
                }

                Set<Audience> filteredViewers = new HashSet();

                for(Audience viewer : event.viewers()) {
                    if (viewer instanceof Player) {
                        Player viewerPlayer = (Player)viewer;
                        if (this.plugin.getPlayerDataManager().isChatVisibilityEnabled(viewerPlayer)) {
                            filteredViewers.add(viewer);
                        }
                    } else {
                        filteredViewers.add(viewer);
                    }
                }

                event.viewers().clear();
                event.viewers().addAll(filteredViewers);
                if (this.plugin.getConfig().getBoolean("use-item-placeholder", false) && player.hasPermission("magmachat.itemplaceholder")) {
                    ItemStack item = player.getInventory().getItemInMainHand();
                    Component displayName;
                    if (item.getItemMeta() != null && item.getItemMeta().hasDisplayName()) {
                        displayName = item.getItemMeta().displayName();
                    } else {
                        if (item.getType() == Material.AIR) {
                            event.renderer(this.magmaChatRenderer);
                            return;
                        }

                        Locale playerLocale = player.locale();
                        displayName = this.getLocalizedItemName(item, playerLocale);
                    }

                    if (!item.getType().equals(Material.AIR) && displayName != null) {
                        Component formattedItem = ((TextComponent)((TextComponent)Component.text("[").color(NamedTextColor.DARK_GRAY)).append(displayName.color(TextColor.fromHexString("#FF4848")).hoverEvent(item))).append(Component.text("]").color(NamedTextColor.DARK_GRAY));
                        event.renderer((source, sourceDisplayName, message1, viewerx) -> this.magmaChatRenderer.render(source, sourceDisplayName, message1, viewerx).replaceText((TextReplacementConfig)TextReplacementConfig.builder().match(Pattern.compile("\\[item]", 2)).replacement(formattedItem).build()));
                    } else {
                        event.renderer(this.magmaChatRenderer);
                    }
                } else {
                    event.renderer(this.magmaChatRenderer);
                }
            }
        }
    }

    private Component getLocalizedItemName(ItemStack item, Locale locale) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            return Component.translatable(item.getType().translationKey());
        } else {
            String materialName = item.getType().toString().toLowerCase().replace("_", " ");
            String[] words = materialName.split(" ");
            StringBuilder formattedName = new StringBuilder();

            for(String word : words) {
                if (word.length() > 0) {
                    formattedName.append(word.substring(0, 1).toUpperCase()).append(word.substring(1)).append(" ");
                }
            }

            return Component.text(formattedName.toString().trim());
        }
    }
}