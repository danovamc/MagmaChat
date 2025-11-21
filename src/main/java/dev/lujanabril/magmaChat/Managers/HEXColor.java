package dev.lujanabril.magmaChat.Managers;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.md_5.bungee.api.ChatColor;

public class HEXColor {
    private static final Pattern pattern = Pattern.compile("#[a-fA-F0-9]{6}");

    public static String message(String message) {
        for(Matcher matcher = pattern.matcher(message); matcher.find(); matcher = pattern.matcher(message)) {
            String color = message.substring(matcher.start(), matcher.end());
            message = message.replace(color, "" + String.valueOf(ChatColor.of(color)));
        }

        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
