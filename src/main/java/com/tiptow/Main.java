package com.tiptow;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class Main extends JavaPlugin implements Listener {
    private FileConfiguration config;
    private Set<UUID> teleportedPlayers = new HashSet<>();

    @Override
    public void onEnable() {
        // Register events
        Bukkit.getServer().getPluginManager().registerEvents(this, this);

        // Load or create configuration file
        loadConfig();

        // Initialize teams
        initializeTeams();
    }

    @Override
    public void onDisable() {
        // Save config
        saveConfig();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String playerName = player.getName();

        if (!teleportedPlayers.contains(player.getUniqueId())) {
            // Check if player is listed in config and assign them to their team
            for (String teamName : config.getConfigurationSection("").getKeys(false)) {
                if (config.contains(teamName + ".Members") && config.getList(teamName + ".Members").contains(playerName)) {
                    assignPlayerToTeam(player, teamName);
                    break;
                }
            }

            // Add player to teleported set
            teleportedPlayers.add(player.getUniqueId());
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (label.equalsIgnoreCase("reloadteams")) {
            if (sender.hasPermission("yourplugin.reload")) {
                reloadConfig();
                loadConfig();
                sender.sendMessage(ChatColor.GREEN + "Configuration reloaded.");
                return true;
            } else {
                sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
                return false;
            }
        }
        return false;
    }

    private void loadConfig() {
        // Load or create configuration file
        File configFile = new File(getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            getConfig().options().copyDefaults(true);
            saveDefaultConfig();
        }
        config = YamlConfiguration.loadConfiguration(configFile);
    }

    private void initializeTeams() {
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();

        // Create or get teams
        for (String teamName : config.getConfigurationSection("").getKeys(false)) {
            Team team = scoreboard.getTeam(teamName);
            if (team == null) {
                team = scoreboard.registerNewTeam(teamName);
                team.setPrefix(ChatColor.RED + "[" + teamName + "] ");
            }
        }
    }

    private void assignPlayerToTeam(Player player, String teamName) {
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        Team team = scoreboard.getTeam(teamName);
        if (team != null) {
            // Remove player from old team if exists
            for (Team t : scoreboard.getTeams()) {
                if (t.hasEntry(player.getName())) {
                    t.removeEntry(player.getName());
                    break;
                }
            }
            team.addEntry(player.getName());
            teleportPlayerToDimension(player, teamName);
        }
    }

    private void teleportPlayerToDimension(Player player, String teamName) {
        World world;
        double x, y, z;

        // Get dimension and coordinates based on team
        switch (teamName) {
            case "Nether":
                world = Bukkit.getWorld("world_nether");
                x = config.getDouble("Nether.Coordinates.X");
                y = config.getDouble("Nether.Coordinates.Y");
                z = config.getDouble("Nether.Coordinates.Z");
                break;
            case "End":
                world = Bukkit.getWorld("world_the_end");
                x = config.getDouble("End.Coordinates.X");
                y = config.getDouble("End.Coordinates.Y");
                z = config.getDouble("End.Coordinates.Z");
                break;
            case "Overworld":
            default:
                world = Bukkit.getWorld("world");
                x = config.getDouble("Overworld.Coordinates.X");
                y = config.getDouble("Overworld.Coordinates.Y");
                z = config.getDouble("Overworld.Coordinates.Z");
                break;
        }

        if (world != null) {
            Location location = new Location(world, x, y, z);
            player.teleport(location);
            player.setBedSpawnLocation(location, true);
        }
    }
}
