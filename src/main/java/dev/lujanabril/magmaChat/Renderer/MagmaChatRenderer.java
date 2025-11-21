package dev.lujanabril.magmaChat.Renderer;

import dev.lujanabril.magmaChat.Main;
import io.papermc.paper.chat.ChatRenderer;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.cacheddata.CachedMetaData;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.jetbrains.annotations.NotNull;

public class MagmaChatRenderer implements ChatRenderer {
    private final LuckPerms luckPerms = LuckPermsProvider.get();
    private final Main plugin;
    private final boolean hasPapi;
    private static final Pattern hexPattern = Pattern.compile("(?i)(?:&#([A-F0-9]{6})|#([A-F0-9]{6}))");
    private static final Pattern legacyPattern = Pattern.compile("&[0-9a-fk-orx]");

    public MagmaChatRenderer(Main plugin) {
        this.plugin = plugin;
        PluginManager pluginManager = plugin.getServer().getPluginManager();
        this.hasPapi = pluginManager.getPlugin("PlaceholderAPI") != null;
    }

    private String processColors(String message) {
        Matcher matcher = hexPattern.matcher(message);
        StringBuffer buffer = new StringBuffer();

        while(matcher.find()) {
            String color = matcher.group(1) != null ? "#" + matcher.group(1) : "#" + matcher.group(2);
            matcher.appendReplacement(buffer, ChatColor.of(color).toString());
        }

        matcher.appendTail(buffer);
        return ChatColor.translateAlternateColorCodes('&', buffer.toString());
    }

    private String stripColors(String message) {
        message = hexPattern.matcher(message).replaceAll("");
        return legacyPattern.matcher(message).replaceAll("");
    }

    private Component createHoverText(Player source) {
        CachedMetaData metaData = this.luckPerms.getPlayerAdapter(Player.class).getMetaData(source);
        String prefix = metaData.getPrefix() != null ? metaData.getPrefix() : "";
        String suffix = metaData.getSuffix() != null ? metaData.getSuffix() : "";
        List<String> hoverLines = this.plugin.getConfig().getStringList("hover-format");
        if (hoverLines.isEmpty()) {
            return Component.empty();
        } else {
            String formattedPrefix = prefix.equals("&7") ? prefix : prefix + " ";
            List<Component> componentLines = new ArrayList();

            for(String line : hoverLines) {
                line = line.replace("{prefix}", formattedPrefix).replace("{suffix}", suffix).replace("{displayname}", PlainTextComponentSerializer.plainText().serialize(source.displayName())).replace("{name}", source.getName()).replace("{world}", source.getWorld().getName());
                line = this.processColors(line);
                if (this.hasPapi) {
                    line = PlaceholderAPI.setPlaceholders(source, line);
                }

                componentLines.add(LegacyComponentSerializer.legacySection().deserialize(line));
            }

            return Component.join(JoinConfiguration.newlines(), componentLines);
        }
    }

    public @NotNull Component render(@NotNull Player source, @NotNull Component sourceDisplayName, @NotNull Component message, @NotNull Audience viewer) {
        boolean hasPermission = source.hasPermission("magmachat.colors");
        CachedMetaData metaData = this.luckPerms.getPlayerAdapter(Player.class).getMetaData(source);
        String group = (String)Objects.requireNonNull(metaData.getPrimaryGroup(), "Primary group cannot be null");
        String plainMessage = PlainTextComponentSerializer.plainText().serialize(message);
        if (hasPermission) {
            plainMessage = this.processColors(plainMessage);
        } else {
            plainMessage = this.stripColors(plainMessage);
        }

        String format = this.plugin.getConfig().getString("group-formats." + group);
        if (format == null) {
            format = this.plugin.getConfig().getString("chat-format", "&f{name}: {message}");
        }

        String prefix = metaData.getPrefix() != null ? metaData.getPrefix() : "";
        String suffix = metaData.getSuffix() != null ? metaData.getSuffix() : "";
        if (!suffix.isEmpty()) {
            suffix = " " + suffix + " ";
        } else {
            suffix = " " + suffix;
        }

        if (prefix.equals("&7")) {
            prefix = prefix;
        } else {
            prefix = prefix + " ";
        }

        format = format.replace("{prefix}", prefix).replace("{suffix}", suffix).replace("{prefixes}", String.join(" ", metaData.getPrefixes().values())).replace("{suffixes}", String.join(" ", metaData.getSuffixes().values())).replace("{world}", source.getWorld().getName()).replace("{name}", source.getName()).replace("{displayname}", PlainTextComponentSerializer.plainText().serialize(source.displayName())).replace("{username-color}", metaData.getMetaValue("username-color") != null ? metaData.getMetaValue("username-color") : "").replace("{message-color}", metaData.getMetaValue("message-color") != null ? metaData.getMetaValue("message-color") : "");
        format = this.processColors(format);
        if (this.hasPapi) {
            format = PlaceholderAPI.setPlaceholders(source, format);
        }

        if (!format.contains("{message}")) {
            Component formatComponent = ((TextComponent)LegacyComponentSerializer.legacySection().deserialize(format).hoverEvent(HoverEvent.showText(this.createHoverText(source)))).clickEvent(ClickEvent.suggestCommand("/msg " + source.getName() + " "));
            Component messageComponent = LegacyComponentSerializer.legacySection().deserialize(plainMessage);
            return formatComponent.append(messageComponent);
        } else {
            int messageIndex = format.indexOf("{message}");
            String beforeMessage = format.substring(0, messageIndex);
            String afterMessage = format.substring(messageIndex + 9);
            Component resultComponent = Component.empty();
            if (!beforeMessage.isEmpty()) {
                Component beforeComponent = ((TextComponent)LegacyComponentSerializer.legacySection().deserialize(beforeMessage).hoverEvent(HoverEvent.showText(this.createHoverText(source)))).clickEvent(ClickEvent.suggestCommand("/msg " + source.getName() + " "));
                resultComponent = resultComponent.append(beforeComponent);
            }

            Component messageComponent = LegacyComponentSerializer.legacySection().deserialize(plainMessage);
            resultComponent = resultComponent.append(messageComponent);
            if (!afterMessage.isEmpty()) {
                Component afterComponent = ((TextComponent)LegacyComponentSerializer.legacySection().deserialize(afterMessage).hoverEvent(HoverEvent.showText(this.createHoverText(source)))).clickEvent(ClickEvent.suggestCommand("/msg " + source.getName() + " "));
                resultComponent = resultComponent.append(afterComponent);
            }

            return resultComponent;
        }
    }
}
