package dev.lujanabril.magmaChat.Managers;

import dev.lujanabril.magmaChat.Main;
import dev.lujanabril.magmaChat.Listeners.JoinListener;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

public class ChatManager {
    private final Main plugin;
    private Set<String> slurWords;
    private final Map<UUID, Long> lastMessageTime;
    private final Map<UUID, Long> lastCommandTime;
    private final Map<UUID, String> lastMessage;
    private final Map<UUID, UUID> replyTarget;
    private final Map<UUID, Boolean> commandSpamBypassCache = new ConcurrentHashMap();
    private double spamCooldown;
    private double commandCooldown;
    private boolean antiSpamEnabled;
    private boolean antiCommandSpamEnabled;
    private boolean antiServerIpEnabled;
    private boolean antiFloodEnabled;
    private boolean antiCapsEnabled;
    private int maxCapsCount;
    private int maxRepeatedChars;
    private final Pattern ipPattern;
    private final Pattern colorCodePattern;
    private final GGWaveManager ggWaveManager;
    private final JoinListener joinListener;

    public ChatManager(Main plugin) {
        this.plugin = plugin;
        this.slurWords = new HashSet();
        this.lastMessageTime = new ConcurrentHashMap();
        this.lastCommandTime = new ConcurrentHashMap();
        this.lastMessage = new ConcurrentHashMap();
        this.replyTarget = new ConcurrentHashMap();
        this.ipPattern = Pattern.compile("\\b(?:https?://)?(?:www\\.)?[a-zA-Z0-9-]+\\.[a-zA-Z0-9-.]+(:[0-9]{1,5})?(?:/[^\\s]*)?\\b|\\b(?:[0-9]{1,3}\\.){3}[0-9]{1,3}(?::[0-9]{1,5})?\\b");
        this.colorCodePattern = Pattern.compile("&[0-9a-fk-or]", 2);
        this.loadConfiguration();
        this.ggWaveManager = new GGWaveManager(plugin);
        this.joinListener = new JoinListener(plugin);
    }

    private void loadConfiguration() {
        FileConfiguration config = this.plugin.getConfig();
        this.slurWords = new HashSet(config.getStringList("chat-filter.slurs"));
        this.spamCooldown = config.getDouble("anti-spam.cooldown", (double)3.0F);
        this.antiSpamEnabled = config.getBoolean("anti-spam.enabled", true);
        this.commandCooldown = config.getDouble("anti-spam.command-cooldown", (double)1.0F);
        this.antiCommandSpamEnabled = config.getBoolean("anti-spam.command-spam-enabled", true);
        this.antiServerIpEnabled = config.getBoolean("chat-filter.block-server-ips", true);
        this.antiFloodEnabled = config.getBoolean("chat-filter.anti-flood.enabled", true);
        this.antiCapsEnabled = config.getBoolean("chat-filter.anti-caps.enabled", true);
        this.maxCapsCount = config.getInt("chat-filter.anti-caps.max-caps", 12);
        this.maxRepeatedChars = config.getInt("chat-filter.anti-flood.max-repeated-chars", 7);
        if (!config.contains("chat-filter.anti-flood.enabled")) {
            config.set("chat-filter.anti-flood.enabled", true);
            config.set("chat-filter.anti-flood.max-repeated-chars", 7);
            config.set("chat-filter.anti-caps.enabled", true);
            config.set("chat-filter.anti-caps.max-caps", 12);
            this.plugin.saveConfig();
        }

    }

    public boolean containsSlur(String message) {
        String lowercaseMessage = message.toLowerCase();
        return this.slurWords.stream().anyMatch((slur) -> lowercaseMessage.contains(slur.toLowerCase()));
    }

    public void clearPermissionCache(UUID playerUUID) {
        this.commandSpamBypassCache.remove(playerUUID);
    }

    public boolean containsServerIp(String message) {
        if (!this.antiServerIpEnabled) {
            return false;
        } else {
            Matcher matcher = this.ipPattern.matcher(message);
            return matcher.find();
        }
    }

    public boolean isSpam(Player player) {
        if (!this.antiSpamEnabled) {
            return false;
        } else if (player.hasPermission("magmachat.bypass.spam")) {
            return false;
        } else {
            UUID playerUUID = player.getUniqueId();
            long currentTime = System.currentTimeMillis();
            Long lastTime = (Long)this.lastMessageTime.get(playerUUID);
            if (lastTime != null && (double)(currentTime - lastTime) < this.spamCooldown * (double)1000.0F) {
                return true;
            } else {
                this.lastMessageTime.put(playerUUID, currentTime);
                return false;
            }
        }
    }

    public boolean isCommandSpam(Player player) {
        if (!this.antiCommandSpamEnabled) {
            return false;
        } else {
            UUID playerUUID = player.getUniqueId();
            Boolean hasBypass = (Boolean)this.commandSpamBypassCache.get(playerUUID);
            if (hasBypass == null) {
                hasBypass = player.hasPermission("magmachat.bypass.commandspam");
                this.commandSpamBypassCache.put(playerUUID, hasBypass);
            }

            if (hasBypass) {
                return false;
            } else {
                long currentTime = System.currentTimeMillis();
                long lastTime = (Long)this.lastCommandTime.getOrDefault(playerUUID, 0L);
                if ((double)(currentTime - lastTime) < this.commandCooldown * (double)1000.0F) {
                    return true;
                } else {
                    this.lastCommandTime.put(playerUUID, currentTime);
                    return false;
                }
            }
        }
    }

    public double getRemainingChatCooldown(Player player) {
        UUID playerUUID = player.getUniqueId();
        long currentTime = System.currentTimeMillis();
        Long lastTime = (Long)this.lastMessageTime.get(playerUUID);
        if (lastTime == null) {
            return (double)0.0F;
        } else {
            long elapsedTime = currentTime - lastTime;
            long cooldownMillis = (long)(this.spamCooldown * (double)1000.0F);
            long remainingMillis = cooldownMillis - elapsedTime;
            return remainingMillis <= 0L ? (double)0.0F : (double)Math.round((double)remainingMillis / (double)100.0F) / (double)10.0F;
        }
    }

    public double getRemainingCommandCooldown(Player player) {
        UUID playerUUID = player.getUniqueId();
        long currentTime = System.currentTimeMillis();
        Long lastTime = (Long)this.lastCommandTime.get(playerUUID);
        if (lastTime == null) {
            return (double)0.0F;
        } else {
            long elapsedTime = currentTime - lastTime;
            long cooldownMillis = (long)(this.commandCooldown * (double)1000.0F);
            long remainingMillis = cooldownMillis - elapsedTime;
            return remainingMillis <= 0L ? (double)0.0F : (double)Math.round((double)remainingMillis / (double)100.0F) / (double)10.0F;
        }
    }

    public void setReplyTarget(Player sender, Player target) {
        this.replyTarget.put(sender.getUniqueId(), target.getUniqueId());
    }

    public UUID getReplyTarget(Player player) {
        return (UUID)this.replyTarget.get(player.getUniqueId());
    }

    public void addSlur(String word) {
        this.slurWords.add(word.toLowerCase());
        this.saveSlurs();
    }

    public void removeSlur(String word) {
        this.slurWords.remove(word.toLowerCase());
        this.saveSlurs();
    }

    private void saveSlurs() {
        this.plugin.getConfig().set("chat-filter.slurs", new ArrayList(this.slurWords));
        this.plugin.saveConfig();
    }

    public String processMessage(String message, Player player) {
        if (player.hasPermission("magmachat.bypass.chatfilter")) {
            return message;
        } else {
            String contentForChecking = this.stripColorCodes(message);
            if (this.antiCapsEnabled && !player.hasPermission("magmachat.bypass.anticaps")) {
                message = this.processAntiCaps(message, contentForChecking);
            }

            if (this.antiFloodEnabled && !player.hasPermission("magmachat.bypass.antiflood")) {
                message = this.processAntiFlood(message);
            }

            return message;
        }
    }

    private String stripColorCodes(String input) {
        return this.colorCodePattern.matcher(input).replaceAll("");
    }

    private String processAntiFlood(String message) {
        StringBuilder result = new StringBuilder();
        int count = 1;
        char last = 0;

        for(int i = 0; i < message.length(); ++i) {
            char current = message.charAt(i);
            if (current == '&' && i + 1 < message.length() && "0123456789abcdefklmnorABCDEFKLMNOR".indexOf(message.charAt(i + 1)) >= 0) {
                result.append(current);
                result.append(message.charAt(i + 1));
                ++i;
            } else if (current == last) {
                ++count;
                if (count <= this.maxRepeatedChars) {
                    result.append(current);
                }
            } else {
                count = 1;
                result.append(current);
                last = current;
            }
        }

        return result.toString();
    }

    private String processAntiCaps(String message, String contentForChecking) {
        int capsCount = 0;

        for(char c : contentForChecking.toCharArray()) {
            if (Character.isUpperCase(c)) {
                ++capsCount;
            }
        }

        if (capsCount <= this.maxCapsCount) {
            return message;
        } else {
            StringBuilder result = new StringBuilder();

            for(int i = 0; i < message.length(); ++i) {
                char current = message.charAt(i);
                if (current == '&' && i + 1 < message.length() && "0123456789abcdefklmnorABCDEFKLMNOR".indexOf(message.charAt(i + 1)) >= 0) {
                    result.append(current);
                    result.append(message.charAt(i + 1));
                    ++i;
                } else {
                    result.append(Character.toLowerCase(current));
                }
            }

            return result.toString();
        }
    }

    public double getSpamCooldown() {
        return this.spamCooldown;
    }

    public double getCommandCooldown() {
        return this.commandCooldown;
    }

    public void reloadConfig() {
        this.plugin.reloadConfig();
        this.loadConfiguration();
    }
}