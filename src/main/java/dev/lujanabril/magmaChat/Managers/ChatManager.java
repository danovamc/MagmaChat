package dev.lujanabril.magmaChat.Managers;

import dev.lujanabril.magmaChat.Main;
import dev.lujanabril.magmaChat.Listeners.JoinListener;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

public class ChatManager {
    private final Main plugin;
    private Set<String> slurWords;
    private final Map<UUID, Long> lastMessageTime;
    private final Map<UUID, Long> lastCommandTime;
    private final Map<UUID, String> lastMessage;
    private final Map<UUID, UUID> replyTarget;
    private final Map<UUID, Boolean> commandSpamBypassCache = new ConcurrentHashMap<>();

    // Nuevo mapa para conteo de violaciones de spam
    private final Map<UUID, Integer> spamViolations = new ConcurrentHashMap<>();

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

    // Variables para Similarity Check
    private boolean similarityEnabled;
    private int similarityThreshold;
    private int similarityMinLength;
    private int similarityExpiration;
    private String similarityMessage;
    private boolean punishmentsEnabled;

    public ChatManager(Main plugin) {
        this.plugin = plugin;
        this.slurWords = new HashSet<>();
        this.lastMessageTime = new ConcurrentHashMap<>();
        this.lastCommandTime = new ConcurrentHashMap<>();
        this.lastMessage = new ConcurrentHashMap<>();
        this.replyTarget = new ConcurrentHashMap<>();
        this.ipPattern = Pattern.compile("\\b(?:https?://)?(?:www\\.)?[a-zA-Z0-9-]+\\.[a-zA-Z0-9-.]+(:[0-9]{1,5})?(?:/[^\\s]*)?\\b|\\b(?:[0-9]{1,3}\\.){3}[0-9]{1,3}(?::[0-9]{1,5})?\\b");
        this.colorCodePattern = Pattern.compile("&[0-9a-fk-or]", Pattern.CASE_INSENSITIVE);
        this.loadConfiguration();
        this.ggWaveManager = new GGWaveManager(plugin);
        this.joinListener = new JoinListener(plugin);
    }

    private void loadConfiguration() {
        FileConfiguration config = this.plugin.getConfig();
        this.slurWords = new HashSet<>(config.getStringList("chat-filter.slurs"));
        this.spamCooldown = config.getDouble("anti-spam.cooldown", 3.0);
        this.antiSpamEnabled = config.getBoolean("anti-spam.enabled", true);
        this.commandCooldown = config.getDouble("anti-spam.command-cooldown", 1.0);
        this.antiCommandSpamEnabled = config.getBoolean("anti-spam.command-spam-enabled", true);
        this.antiServerIpEnabled = config.getBoolean("chat-filter.block-server-ips", true);
        this.antiFloodEnabled = config.getBoolean("chat-filter.anti-flood.enabled", true);
        this.antiCapsEnabled = config.getBoolean("chat-filter.anti-caps.enabled", true);
        this.maxCapsCount = config.getInt("chat-filter.anti-caps.max-caps", 12);
        this.maxRepeatedChars = config.getInt("chat-filter.anti-flood.max-repeated-chars", 7);

        // Similarity Check Config
        this.similarityEnabled = config.getBoolean("chat-filter.similarity-check.enabled", true);
        this.similarityThreshold = config.getInt("chat-filter.similarity-check.threshold", 80);
        this.similarityMinLength = config.getInt("chat-filter.similarity-check.min-length", 5);
        this.similarityExpiration = config.getInt("chat-filter.similarity-check.expiration", 60);
        this.similarityMessage = config.getString("chat-filter.similarity-check.message", "<red>¡No repitas mensajes!</red>");
        this.punishmentsEnabled = config.getBoolean("chat-filter.similarity-check.punishments.enabled", false);

        if (!config.contains("chat-filter.anti-flood.enabled")) {
            config.set("chat-filter.anti-flood.enabled", true);
            config.set("chat-filter.anti-flood.max-repeated-chars", 7);
            config.set("chat-filter.anti-caps.enabled", true);
            config.set("chat-filter.anti-caps.max-caps", 12);
            this.plugin.saveConfig();
        }
    }

    public void cleanupPlayer(UUID playerUUID) {
        this.lastMessage.remove(playerUUID);
        this.lastMessageTime.remove(playerUUID);
        this.lastCommandTime.remove(playerUUID);
        this.commandSpamBypassCache.remove(playerUUID);
        this.replyTarget.remove(playerUUID);
        this.spamViolations.remove(playerUUID);
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
            Long lastTime = this.lastMessageTime.get(playerUUID);
            if (lastTime != null && (double)(currentTime - lastTime) < this.spamCooldown * 1000.0) {
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
            Boolean hasBypass = this.commandSpamBypassCache.get(playerUUID);
            if (hasBypass == null) {
                hasBypass = player.hasPermission("magmachat.bypass.commandspam");
                this.commandSpamBypassCache.put(playerUUID, hasBypass);
            }

            if (hasBypass) {
                return false;
            } else {
                long currentTime = System.currentTimeMillis();
                long lastTime = this.lastCommandTime.getOrDefault(playerUUID, 0L);
                if ((double)(currentTime - lastTime) < this.commandCooldown * 1000.0) {
                    return true;
                } else {
                    this.lastCommandTime.put(playerUUID, currentTime);
                    return false;
                }
            }
        }
    }

    // --- NUEVA LÓGICA DE SIMILITUD INTELIGENTE ---

    /**
     * Elimina los nombres de jugadores conectados del mensaje para evitar
     * que "Hola Juan" y "Hola Pedro" cuenten como mensajes diferentes.
     */
    private String removePlayerNames(String message) {
        String result = message;
        for (Player p : Bukkit.getOnlinePlayers()) {
            String name = p.getName();
            // Reemplaza el nombre ignorando mayúsculas/minúsculas
            result = result.replaceAll("(?i)" + Pattern.quote(name), "");
        }
        // Limpia espacios dobles que puedan haber quedado
        return result.replaceAll("\\s+", " ").trim();
    }

    public boolean isSimilarSpam(Player player, String currentMessage) {
        if (!this.similarityEnabled) return false;
        if (player.hasPermission("magmachat.bypass.similarity")) return false;

        // 1. Limpiamos el mensaje actual (quitamos nombres)
        String cleanCurrent = removePlayerNames(currentMessage);

        // Si después de limpiar el mensaje es muy corto (ej: solo dijo un nombre), lo ignoramos
        if (cleanCurrent.length() < this.similarityMinLength) return false;

        UUID uuid = player.getUniqueId();
        String lastMsgRaw = this.lastMessage.get(uuid); // Obtenemos el último mensaje RAW

        Long lastTime = this.lastMessageTime.get(uuid);
        long currentTime = System.currentTimeMillis();

        // Chequeo de expiración
        if (lastMsgRaw == null || lastTime == null || (currentTime - lastTime) > (this.similarityExpiration * 1000L)) {
            this.lastMessage.put(uuid, currentMessage);
            return false;
        }

        // 2. Limpiamos el mensaje anterior también
        String cleanLast = removePlayerNames(lastMsgRaw);

        // Comparamos las versiones "limpias"
        double similarity = calculateSimilarity(cleanCurrent, cleanLast);

        if (similarity >= this.similarityThreshold) {
            handleSpamPunishment(player);
            return true;
        }

        this.lastMessage.put(uuid, currentMessage);
        return false;
    }

    // ----------------------------------------------

    public String getSimilarityMessage() {
        return this.similarityMessage;
    }

    private void handleSpamPunishment(Player player) {
        if (!this.punishmentsEnabled) return;

        UUID uuid = player.getUniqueId();
        int violations = this.spamViolations.getOrDefault(uuid, 0) + 1;
        this.spamViolations.put(uuid, violations);

        FileConfiguration config = this.plugin.getConfig();
        List<String> commands = config.getStringList("chat-filter.similarity-check.punishments.actions." + violations);

        if (commands != null && !commands.isEmpty()) {
            Bukkit.getScheduler().runTask(this.plugin, () -> {
                for (String cmd : commands) {
                    String finalCommand = cmd.replace("%player%", player.getName());
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCommand);
                }
            });
        }
    }

    private double calculateSimilarity(String s1, String s2) {
        String longer = s1, shorter = s2;
        if (s1.length() < s2.length()) {
            longer = s2; shorter = s1;
        }
        int longerLength = longer.length();
        if (longerLength == 0) return 100.0;

        int editDistance = levenshteinDistance(longer, shorter);
        return (longerLength - editDistance) / (double) longerLength * 100.0;
    }

    private int levenshteinDistance(String s1, String s2) {
        s1 = s1.toLowerCase();
        s2 = s2.toLowerCase();
        int[] costs = new int[s2.length() + 1];
        for (int i = 0; i <= s1.length(); i++) {
            int lastValue = i;
            for (int j = 0; j <= s2.length(); j++) {
                if (i == 0)
                    costs[j] = j;
                else {
                    if (j > 0) {
                        int newValue = costs[j - 1];
                        if (s1.charAt(i - 1) != s2.charAt(j - 1))
                            newValue = Math.min(Math.min(newValue, lastValue), costs[j]) + 1;
                        costs[j - 1] = lastValue;
                        lastValue = newValue;
                    }
                }
            }
            if (i > 0) costs[s2.length()] = lastValue;
        }
        return costs[s2.length()];
    }

    public double getRemainingChatCooldown(Player player) {
        UUID playerUUID = player.getUniqueId();
        long currentTime = System.currentTimeMillis();
        Long lastTime = this.lastMessageTime.get(playerUUID);
        if (lastTime == null) {
            return 0.0;
        } else {
            long elapsedTime = currentTime - lastTime;
            long cooldownMillis = (long)(this.spamCooldown * 1000.0);
            long remainingMillis = cooldownMillis - elapsedTime;
            return remainingMillis <= 0L ? 0.0 : Math.round((double)remainingMillis / 100.0) / 10.0;
        }
    }

    public double getRemainingCommandCooldown(Player player) {
        UUID playerUUID = player.getUniqueId();
        long currentTime = System.currentTimeMillis();
        Long lastTime = this.lastCommandTime.get(playerUUID);
        if (lastTime == null) {
            return 0.0;
        } else {
            long elapsedTime = currentTime - lastTime;
            long cooldownMillis = (long)(this.commandCooldown * 1000.0);
            long remainingMillis = cooldownMillis - elapsedTime;
            return remainingMillis <= 0L ? 0.0 : Math.round((double)remainingMillis / 100.0) / 10.0;
        }
    }

    public void setReplyTarget(Player sender, Player target) {
        this.replyTarget.put(sender.getUniqueId(), target.getUniqueId());
    }

    public UUID getReplyTarget(Player player) {
        return this.replyTarget.get(player.getUniqueId());
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
        this.plugin.getConfig().set("chat-filter.slurs", new ArrayList<>(this.slurWords));
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