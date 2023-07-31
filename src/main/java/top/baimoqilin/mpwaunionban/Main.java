package top.baimoqilin.mpwaunionban;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.*;

public class Main extends JavaPlugin implements CommandExecutor {
    private Connection connection;
    private String host, port, database, username, password, fromServer;
    private int banCheckInterval, int_isOnline;
    private long storedVersion = -1L;

    @Override
    public void onEnable() {
        // Load configuration
        loadConfiguration();

        // Connect to MySQL database
        connectToDatabase();

        // Schedule ban check task
        getServer().getScheduler().runTaskTimerAsynchronously(this, this::checkForUpdatesAndBannedPlayers, 0L, banCheckInterval);

        getCommand("uban").setExecutor(this);
        getCommand("uban-reload").setExecutor(this);
    }

    @Override
    public void onDisable() {
        // Disconnect from the database
        disconnectFromDatabase();
    }

    private void loadConfiguration() {
        saveDefaultConfig();
        getConfig().options().copyDefaults(true);

        // Read configuration values
        host = getConfig().getString("database.host");
        port = getConfig().getString("database.port");
        database = getConfig().getString("database.database");
        username = getConfig().getString("database.username");
        password = getConfig().getString("database.password");
        banCheckInterval = getConfig().getInt("banCheckInterval") * 20; // Convert to ticks (20 ticks = 1 second)
        fromServer = getConfig().getString("from");
    }

    private void connectToDatabase() {
        try {
            connection = DriverManager.getConnection("jdbc:mysql://" + host + ":" + port + "/" +
                    database + "?useSSL=false", username, password);
            getLogger().info("Connected to the MySQL database.");
        } catch (SQLException e) {
            getLogger().severe("Failed to connect to the MySQL database: " + e.getMessage());
        }
    }

    private void disconnectFromDatabase() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                getLogger().info("Disconnected from the MySQL database.");
            }
        } catch (SQLException e) {
            getLogger().severe("Failed to disconnect from the MySQL database: " + e.getMessage());
        }
    }

    private int getRows() {
        try (PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM UnionBan")) {
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                return resultSet.getInt(1);
            }
        } catch (SQLException e) {
            getLogger().severe("Error retrieving row count: " + e.getMessage());
        }
        return 0;
    }

    private void addRow(String ID, String IP, int Reason, String Reason_Text, boolean isOnline, String From) {
        if (isOnline) {
            int_isOnline = 1;
        } else {
            int_isOnline = 0;
        }

        // Increase the version by one in the database
        long newVersion = getVersionFromDatabase() + 1;
        updateVersionInDatabase(newVersion);

        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO UnionBan (ID, IP, Reason, Reason_Text, isOnline, `From`) VALUES (?, ?, ?, ?, ?, ?)")) {
            statement.setString(1, ID);
            statement.setString(2, IP);
            statement.setInt(3, Reason);
            statement.setString(4, Reason_Text);
            statement.setInt(5, int_isOnline);
            statement.setString(6, From);
            statement.executeUpdate();
            getLogger().info("Added a new row to the UnionBan table.");
        } catch (SQLException e) {
            getLogger().severe("Error adding a row to the UnionBan table: " + e.getMessage());
        }
    }

    private void checkForUpdatesAndBannedPlayers() {
        if (storedVersion == -1L) {
            storedVersion = getVersionFromDatabase();
            return;
        }

        long currentVersion = getVersionFromDatabase();
        if (currentVersion > storedVersion) {
            // New version detected, execute checkForBannedPlayers
            checkForBannedPlayers();
            updateStoredVersion(currentVersion);
        }
    }

    private long getVersionFromDatabase() {
        try (PreparedStatement statement = connection.prepareStatement("SELECT Version FROM info LIMIT 1")) {
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                return resultSet.getLong("Version");
            }
        } catch (SQLException e) {
            getLogger().severe("Error retrieving version from info table: " + e.getMessage());
        }
        return -1L;
    }

    private void updateStoredVersion(long newVersion) {
        storedVersion = newVersion;

        try (PreparedStatement statement = connection.prepareStatement("UPDATE info SET Version = ?")) {
            statement.setLong(1, newVersion);
            statement.executeUpdate();
            getLogger().info("Updated stored version in the info table.");
        } catch (SQLException e) {
            getLogger().severe("Error updating stored version in the info table: " + e.getMessage());
        }
    }

    private void updateVersionInDatabase(long newVersion) {
        try (PreparedStatement statement = connection.prepareStatement("UPDATE info SET Version = ?")) {
            statement.setLong(1, newVersion);
            statement.executeUpdate();
            getLogger().info("Updated version in the info table.");
        } catch (SQLException e) {
            getLogger().severe("Error updating version in the info table: " + e.getMessage());
        }
    }

    private void checkForBannedPlayers() {
        try (PreparedStatement statement = connection.prepareStatement("SELECT * FROM UnionBan")) {
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                String playerID = resultSet.getString("ID");
                String playerIP = resultSet.getString("IP");
                Player player = getPlayerByID(playerID);

                if (!isPlayerBanned(player)) {
                    // Player is not banned, determine whether to ban by IP or ID
                    if (playerIP.equalsIgnoreCase("UNKNOWN")) {
                        // Ban by ID
                        banPlayer(this, playerID);
                    } else {
                        // Ban by IP
                        banPlayer(this, playerIP);
                    }
                }
            }
        } catch (SQLException e) {
            getLogger().severe("Error checking for banned players: " + e.getMessage());
        }
    }

    private Player getPlayerByID(String playerID) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getUniqueId().toString().equals(playerID)) {
                return player;
            }
        }
        return null;
    }

    private void banPlayer(Plugin plugin, String banTarget) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (banTarget.contains(".")) {
                // Ban by player ip
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "ban-ip " + banTarget);
            } else {
                // Ban by player ID
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "ban " + banTarget);
            }
        });
    }

    private boolean isPlayerBanned(Player player) {
        if (player == null) {
            return false;
        }

        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT * FROM UnionBan WHERE ID=? OR IP=?")) {
            statement.setString(1, player.getUniqueId().toString());
            statement.setString(2, player.getAddress().getAddress().getHostAddress());
            ResultSet resultSet = statement.executeQuery();
            return resultSet.next();
        } catch (SQLException e) {
            getLogger().severe("Error checking ban status for player " + player.getName() + ": " + e.getMessage());
        }
        return false;
    }

    private int getReason(String reason) {
        switch (reason.toLowerCase()) {
            case "hacking":
                return 0;
            case "stealing":
                return 1;
            case "destroying":
                return 2;
            case "other":
                return 3;
            default:
                return -1;
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("uban")) {
            if (!sender.hasPermission("unionban.uban")) {
                sender.sendMessage("You don't have permission to use this command.");
                return true;
            }

            if (args.length < 4) {
                sender.sendMessage("Usage: /uban <playerID> <Reason(hacking, stealing, destroying, other)> <Reason_Text> <isOnline>");
                return true;
            }

            String playerID = args[0];
            int reason = getReason(args[1]);
            if (reason == -1) {
                sender.sendMessage("Invalid reason. Available reasons: hacking, stealing, destroying, other.");
                return true;
            }
            String reasonText = args[2];
            boolean isOnline = Boolean.parseBoolean(args[3]);

            Player target = Bukkit.getPlayer(playerID);
            if (target != null && target.isOnline()) {
                // The target player is online
                addRow(target.getUniqueId().toString(), target.getAddress().getAddress().getHostAddress(),
                        reason, reasonText, isOnline, fromServer);
                sender.sendMessage("Successfully added a new row to the UnionBan table.");
            } else {
                // The target player is not online
                addRow(playerID, "UNKNOWN", reason, reasonText, isOnline, fromServer);
                sender.sendMessage("Successfully added a new row to the UnionBan table.");
            }
            return true;
        } else if (command.getName().equalsIgnoreCase("uban-reload")) {
            if (!sender.hasPermission("unionban.reload")) {
                sender.sendMessage("You don't have permission to use this command.");
                return true;
            }

            // Reload configuration
            reloadConfig();
            loadConfiguration();
            sender.sendMessage("Configuration reloaded!");

            return true;
        }
        return false;
    }
}
