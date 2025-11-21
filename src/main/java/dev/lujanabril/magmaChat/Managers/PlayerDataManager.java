//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package dev.lujanabril.magmaChat.Managers;

import dev.lujanabril.magmaChat.Main;
import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.entity.Player;

public class PlayerDataManager {
    private final Main plugin;
    private Connection connection;
    private final Map<UUID, Set<UUID>> playerIgnoreList = new ConcurrentHashMap();
    private final Set<UUID> messagingDisabled = Collections.newSetFromMap(new ConcurrentHashMap());
    private final Set<UUID> chatVisibilityDisabled = Collections.newSetFromMap(new ConcurrentHashMap());
    private final Map<UUID, UUID> lastMessageSender = new ConcurrentHashMap();

    public PlayerDataManager(Main plugin) {
        this.plugin = plugin;
        this.initializeDatabase();
        this.loadData();
    }

    private void initializeDatabase() {
        File dataFolder = new File(this.plugin.getDataFolder(), "database");
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }

        String dbFile = (new File(dataFolder, "player_data.db")).getAbsolutePath();
        String url = "jdbc:sqlite:" + dbFile;

        try {
            Class.forName("org.sqlite.JDBC");
            this.connection = DriverManager.getConnection(url);

            try (Statement statement = this.connection.createStatement()) {
                statement.execute("CREATE TABLE IF NOT EXISTS ignored_players (player_uuid TEXT NOT NULL, ignored_uuid TEXT NOT NULL, PRIMARY KEY (player_uuid, ignored_uuid))");
                statement.execute("CREATE TABLE IF NOT EXISTS message_toggle (player_uuid TEXT PRIMARY KEY, enabled INTEGER NOT NULL)");
                statement.execute("CREATE TABLE IF NOT EXISTS chat_visibility (player_uuid TEXT PRIMARY KEY, enabled INTEGER NOT NULL)");
            }
        } catch (ClassNotFoundException | SQLException e) {
            this.plugin.getLogger().severe("Failed to initialize database: " + ((Exception)e).getMessage());
            ((Exception)e).printStackTrace();
        }

    }

    private void loadData() {
        this.playerIgnoreList.clear();
        this.messagingDisabled.clear();
        this.chatVisibilityDisabled.clear();

        try {
            try (PreparedStatement ps = this.connection.prepareStatement("SELECT player_uuid, ignored_uuid FROM ignored_players")) {
                ResultSet rs = ps.executeQuery();

                while(rs.next()) {
                    UUID playerUuid = UUID.fromString(rs.getString("player_uuid"));
                    UUID ignoredUuid = UUID.fromString(rs.getString("ignored_uuid"));
                    ((Set)this.playerIgnoreList.computeIfAbsent(playerUuid, (k) -> new HashSet())).add(ignoredUuid);
                }
            }

            try (PreparedStatement ps = this.connection.prepareStatement("SELECT player_uuid FROM message_toggle WHERE enabled = 0")) {
                ResultSet rs = ps.executeQuery();

                while(rs.next()) {
                    UUID playerUuid = UUID.fromString(rs.getString("player_uuid"));
                    this.messagingDisabled.add(playerUuid);
                }
            }

            try (PreparedStatement ps = this.connection.prepareStatement("SELECT player_uuid FROM chat_visibility WHERE enabled = 0")) {
                ResultSet rs = ps.executeQuery();

                while(rs.next()) {
                    UUID playerUuid = UUID.fromString(rs.getString("player_uuid"));
                    this.chatVisibilityDisabled.add(playerUuid);
                }
            }
        } catch (SQLException e) {
            this.plugin.getLogger().severe("Failed to load player data: " + e.getMessage());
            e.printStackTrace();
        }

    }

    public void setLastMessageSender(Player receiver, Player sender) {
        this.lastMessageSender.put(receiver.getUniqueId(), sender.getUniqueId());
    }

    public UUID getLastMessageSender(Player player) {
        return (UUID)this.lastMessageSender.get(player.getUniqueId());
    }

    public boolean isIgnoring(Player player, Player target) {
        Set<UUID> ignored = (Set)this.playerIgnoreList.get(player.getUniqueId());
        return ignored != null && ignored.contains(target.getUniqueId());
    }

    public boolean addIgnore(Player player, Player target) {
        UUID playerUuid = player.getUniqueId();
        UUID targetUuid = target.getUniqueId();

        try (PreparedStatement ps = this.connection.prepareStatement("INSERT OR IGNORE INTO ignored_players (player_uuid, ignored_uuid) VALUES (?, ?)")) {
            ps.setString(1, playerUuid.toString());
            ps.setString(2, targetUuid.toString());
            int affectedRows = ps.executeUpdate();
            if (affectedRows > 0) {
                ((Set)this.playerIgnoreList.computeIfAbsent(playerUuid, (k) -> new HashSet())).add(targetUuid);
                return true;
            } else {
                return false;
            }
        } catch (SQLException e) {
            this.plugin.getLogger().severe("Failed to add player to ignore list: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public boolean removeIgnore(Player player, Player target) {
        UUID playerUuid = player.getUniqueId();
        UUID targetUuid = target.getUniqueId();

        try (PreparedStatement ps = this.connection.prepareStatement("DELETE FROM ignored_players WHERE player_uuid = ? AND ignored_uuid = ?")) {
            ps.setString(1, playerUuid.toString());
            ps.setString(2, targetUuid.toString());
            int affectedRows = ps.executeUpdate();
            if (affectedRows > 0) {
                Set<UUID> ignored = (Set)this.playerIgnoreList.get(playerUuid);
                if (ignored != null) {
                    ignored.remove(targetUuid);
                }

                return true;
            } else {
                return false;
            }
        } catch (SQLException e) {
            this.plugin.getLogger().severe("Failed to remove player from ignore list: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public Set<UUID> getIgnoredPlayers(Player player) {
        return (Set)this.playerIgnoreList.getOrDefault(player.getUniqueId(), new HashSet());
    }

    public boolean isMessagingDisabled(Player player) {
        return this.messagingDisabled.contains(player.getUniqueId());
    }

    public boolean toggleMessaging(Player player) {
        UUID playerUuid = player.getUniqueId();
        boolean currentlyEnabled = !this.messagingDisabled.contains(playerUuid);
        boolean newState = !currentlyEnabled;

        try (PreparedStatement ps = this.connection.prepareStatement("INSERT OR REPLACE INTO message_toggle (player_uuid, enabled) VALUES (?, ?)")) {
            ps.setString(1, playerUuid.toString());
            ps.setInt(2, newState ? 1 : 0);
            ps.executeUpdate();
            if (newState) {
                this.messagingDisabled.remove(playerUuid);
            } else {
                this.messagingDisabled.add(playerUuid);
            }

            return newState;
        } catch (SQLException e) {
            this.plugin.getLogger().severe("Failed to toggle messaging: " + e.getMessage());
            e.printStackTrace();
            return currentlyEnabled;
        }
    }

    public boolean isChatVisibilityEnabled(Player player) {
        return !this.chatVisibilityDisabled.contains(player.getUniqueId());
    }

    public boolean toggleChatVisibility(Player player) {
        UUID playerUuid = player.getUniqueId();
        boolean currentlyEnabled = !this.chatVisibilityDisabled.contains(playerUuid);
        boolean newState = !currentlyEnabled;

        try (PreparedStatement ps = this.connection.prepareStatement("INSERT OR REPLACE INTO chat_visibility (player_uuid, enabled) VALUES (?, ?)")) {
            ps.setString(1, playerUuid.toString());
            ps.setInt(2, newState ? 1 : 0);
            ps.executeUpdate();
            if (newState) {
                this.chatVisibilityDisabled.remove(playerUuid);
            } else {
                this.chatVisibilityDisabled.add(playerUuid);
            }

            return newState;
        } catch (SQLException e) {
            this.plugin.getLogger().severe("Failed to toggle chat visibility: " + e.getMessage());
            e.printStackTrace();
            return currentlyEnabled;
        }
    }

    public void close() {
        try {
            if (this.connection != null && !this.connection.isClosed()) {
                this.connection.close();
            }
        } catch (SQLException e) {
            this.plugin.getLogger().severe("Failed to close database connection: " + e.getMessage());
            e.printStackTrace();
        }

    }
}
