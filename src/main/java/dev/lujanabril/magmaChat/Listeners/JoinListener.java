package dev.lujanabril.magmaChat.Listeners;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.imageio.ImageIO;
import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.profile.PlayerTextures;

public class JoinListener implements Listener {
    private final Plugin plugin;
    private Map<UUID, List<String>> playerFaceCache;
    private final MiniMessage miniMessage;
    private final Map<String, String> headInfo;
    private final UUID danovaUUID = UUID.fromString("c39f65d7-b4d6-4cc5-8452-aaf1aed69380");
    private final File skinsFolder;
    private boolean enableWelcomeHead;
    private String newPlayerMessage;
    private boolean enableNewPlayerMessage;

    public JoinListener(Plugin plugin) {
        this.plugin = plugin;
        this.playerFaceCache = new HashMap();
        this.miniMessage = MiniMessage.miniMessage();
        this.skinsFolder = new File(plugin.getDataFolder(), "skins");
        if (!this.skinsFolder.exists()) {
            this.skinsFolder.mkdirs();
        }

        this.newPlayerMessage = plugin.getConfig().getString("welcome.new-player-message", "<#00FF00><b>¡Bienvenido %player% a MagmaMC!</b> <white>¡Es tu primera vez aquí! Usa <#FFD700>/spawn<white> para comenzar.");
        this.headInfo = new HashMap();
        this.headInfo.put("1", plugin.getConfig().getString("welcome.head-lines.line1", ""));
        this.headInfo.put("2", plugin.getConfig().getString("welcome.head-lines.line2", "<white>Bienvenido de vuelta a <#FF2A2A><b>SURVIVAL OP<reset>!"));
        this.headInfo.put("3", plugin.getConfig().getString("welcome.head-lines.line3", ""));
        this.headInfo.put("4", plugin.getConfig().getString("welcome.head-lines.line4", "<#FF2A2A><b>|</b> <white>Jugando: <#FF5F5F>%server_online%"));
        this.headInfo.put("5", plugin.getConfig().getString("welcome.head-lines.line5", "<#FF2A2A><b>|</b> <white>Tienda: <#FF5F5F>tienda.magmamc.us"));
        this.headInfo.put("6", plugin.getConfig().getString("welcome.head-lines.line6", "<#FF2A2A><b>|</b> <white>Discord: <#FF5F5F>discord.magmamc.us"));
        this.headInfo.put("7", plugin.getConfig().getString("welcome.head-lines.line7", "<#FF2A2A><b>|</b> <white>Usa <#FF5F5F>/vote40<white> para recibir recompensas!"));
        this.headInfo.put("8", plugin.getConfig().getString("welcome.head-lines.line8", ""));

        this.enableWelcomeHead = plugin.getConfig().getBoolean("welcome.enable-welcome-head", true);
        this.enableNewPlayerMessage = plugin.getConfig().getBoolean("welcome.enable-new-player-message", true);
    }

    public void loadConfiguration() {
        this.newPlayerMessage = this.plugin.getConfig().getString("welcome.new-player-message", "<#00FF00><b>¡Bienvenido %player% a MagmaMC!</b> <white>¡Es tu primera vez aquí! Usa <#FFD700>/spawn<white> para comenzar.");
        this.headInfo.clear();
        this.headInfo.put("1", this.plugin.getConfig().getString("welcome.head-lines.line1", ""));
        this.headInfo.put("2", this.plugin.getConfig().getString("welcome.head-lines.line2", "<white>Bienvenido de vuelta a <#FF2A2A><b>SURVIVAL OP<reset>!"));
        this.headInfo.put("3", this.plugin.getConfig().getString("welcome.head-lines.line3", ""));
        this.headInfo.put("4", this.plugin.getConfig().getString("welcome.head-lines.line4", "<#FF2A2A><b>|</b> <white>Jugando: <#FF5F5F>%server_online%"));
        this.headInfo.put("5", this.plugin.getConfig().getString("welcome.head-lines.line5", "<#FF2A2A><b>|</b> <white>Tienda: <#FF5F5F>tienda.magmamc.us"));
        this.headInfo.put("6", this.plugin.getConfig().getString("welcome.head-lines.line6", "<#FF2A2A><b>|</b> <white>Discord: <#FF5F5F>discord.magmamc.us"));
        this.headInfo.put("7", this.plugin.getConfig().getString("welcome.head-lines.line7", "<#FF2A2A><b>|</b> <white>Usa <#FF5F5F>/vote40<white> para recibir recompensas!"));
        this.headInfo.put("8", this.plugin.getConfig().getString("welcome.head-lines.line8", ""));
        this.enableWelcomeHead = this.plugin.getConfig().getBoolean("welcome.enable-welcome-head", true);
        this.enableNewPlayerMessage = this.plugin.getConfig().getBoolean("welcome.enable-new-player-message", true);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        event.setJoinMessage((String)null);
        if (!player.hasPlayedBefore()) {
            if (this.enableNewPlayerMessage) {
                this.sendNewPlayerMessage(player);
            }
        } else {
            this.plugin.getServer().getScheduler().runTaskAsynchronously(this.plugin, () -> {
                this.downloadAndSavePlayerSkin(player);
                List<String> faceArt = this.getFaceArtHex(player.getUniqueId());
                this.playerFaceCache.put(player.getUniqueId(), faceArt);
                this.plugin.getServer().getScheduler().runTask(this.plugin, () -> this.sendWelcomeMessage(player));
            });
        }
    }

    private void downloadAndSavePlayerSkin(Player player) {
        try {
            PlayerTextures textures = player.getPlayerProfile().getTextures();
            URL skinUrl = textures.getSkin();
            if (skinUrl != null) {
                File skinFile = new File(this.skinsFolder, player.getUniqueId().toString() + ".png");
                BufferedImage skinImage = ImageIO.read(skinUrl);
                if (skinImage != null) {
                    ImageIO.write(skinImage, "PNG", skinFile);
                }
            }
        } catch (Exception var6) {
        }

    }

    private void sendNewPlayerMessage(Player player) {
        String message = this.processPlaceholders(this.newPlayerMessage, player);
        String legacyMessage = this.convertToLegacy(message);
        this.plugin.getServer().broadcastMessage(legacyMessage);
    }

    private void sendWelcomeMessage(Player player) {

        if (!this.enableWelcomeHead) {
            return;
        }

        player.sendMessage("");
        List<String> faceArt = (List)this.playerFaceCache.get(player.getUniqueId());
        if (faceArt == null) {
            faceArt = this.getDanovaFaceArt();
        }

        Map<String, String> messageInfo = this.headInfo;
        String[] welcomeLines = new String[8];

        for(int i = 1; i <= 8; ++i) {
            String lineConfig = (String)messageInfo.get(String.valueOf(i));
            if (lineConfig != null) {
                lineConfig = this.processPlaceholders(lineConfig, player);
                welcomeLines[i - 1] = this.convertToLegacy(lineConfig);
            } else {
                welcomeLines[i - 1] = "";
            }
        }

        for(int i = 0; i < Math.min(faceArt.size(), 8); ++i) {
            String faceLine = (String)faceArt.get(i);
            String messageLine = i < welcomeLines.length ? welcomeLines[i] : "";
            player.sendMessage(faceLine + "  " + messageLine);
        }

        player.sendMessage("");
    }

    private List<String> getDanovaFaceArt() {
        try {
            return this.getFaceArtHexFromUUID(this.danovaUUID);
        } catch (Exception var4) {
            List<String> fallbackArt = new ArrayList();

            for(int i = 0; i < 8; ++i) {
                fallbackArt.add(String.valueOf(ChatColor.GRAY) + "████");
            }

            return fallbackArt;
        }
    }

    private String processPlaceholders(String message, Player player) {
        message = message.replace("%player%", player.getName());
        message = message.replace("%online%", String.valueOf(this.plugin.getServer().getOnlinePlayers().size()));
        if (this.plugin.getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            message = PlaceholderAPI.setPlaceholders(player, message);
        }

        return message;
    }

    private String convertToLegacy(String input) {
        if (input.contains("<") && input.contains(">")) {
            Component component = this.miniMessage.deserialize(input);
            return LegacyComponentSerializer.legacySection().serialize(component);
        } else {
            return ChatColor.translateAlternateColorCodes('&', input);
        }
    }

    private List<String> getFaceArtHex(UUID uuid) {
        try {
            File skinFile = new File(this.skinsFolder, uuid.toString() + ".png");
            return skinFile.exists() ? this.getFaceArtFromLocalSkin(skinFile) : this.getFaceArtHexFromUUID(uuid);
        } catch (IOException var3) {
            return this.getDanovaFaceArt();
        }
    }

    private List<String> getFaceArtFromLocalSkin(File skinFile) throws IOException {
        List<String> asciiArt = new ArrayList();
        BufferedImage originalImage = ImageIO.read(skinFile);
        if (originalImage == null) {
            throw new IOException("No se pudo cargar la imagen de skin");
        } else {
            BufferedImage faceImage;
            if (originalImage.getWidth() == 64 && originalImage.getHeight() == 64) {
                faceImage = originalImage.getSubimage(8, 8, 8, 8);

                try {
                    BufferedImage overlayImage = originalImage.getSubimage(40, 8, 8, 8);

                    for(int y = 0; y < 8; ++y) {
                        for(int x = 0; x < 8; ++x) {
                            Color overlayColor = new Color(overlayImage.getRGB(x, y), true);
                            if (overlayColor.getAlpha() > 0) {
                                faceImage.setRGB(x, y, overlayImage.getRGB(x, y));
                            }
                        }
                    }
                } catch (Exception var10) {
                }
            } else {
                BufferedImage resized = new BufferedImage(8, 8, 2);
                resized.getGraphics().drawImage(originalImage, 0, 0, 8, 8, (ImageObserver)null);
                faceImage = resized;
            }

            for(int y = 0; y < 8; ++y) {
                StringBuilder line = new StringBuilder();

                for(int x = 0; x < 8; ++x) {
                    Color color = new Color(faceImage.getRGB(x, y), true);
                    if (color.getAlpha() < 128) {
                        line.append(ChatColor.DARK_GRAY).append("▒");
                    } else {
                        String hexColor = String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
                        line.append(ChatColor.of(hexColor)).append("█");
                    }
                }

                asciiArt.add(line.toString());
            }

            return asciiArt;
        }
    }

    private List<String> getFaceArtHexFromUUID(UUID uuid) throws IOException {
        List<String> asciiArt = new ArrayList();
        URL url = new URL("https://crafatar.com/avatars/" + String.valueOf(uuid) + "?size=16&overlay");
        BufferedImage image = ImageIO.read(url);
        if (image == null) {
            throw new IOException("No se pudo cargar la imagen del jugador");
        } else {
            int size = 8;

            for(int y = 0; y < size; ++y) {
                StringBuilder line = new StringBuilder();

                for(int x = 0; x < size; ++x) {
                    int pixelX = x * image.getWidth() / size;
                    int pixelY = y * image.getHeight() / size;
                    Color color = new Color(image.getRGB(pixelX, pixelY), true);
                    if (color.getAlpha() < 128) {
                        line.append(ChatColor.DARK_GRAY).append("▒");
                    } else {
                        String hexColor = String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
                        line.append(ChatColor.of(hexColor)).append("█");
                    }
                }

                asciiArt.add(line.toString());
            }

            return asciiArt;
        }
    }
}