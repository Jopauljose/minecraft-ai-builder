package com.aibuild.commands;

import com.aibuild.AIBuildPlugin;
import com.aibuild.models.Structure;
import com.aibuild.services.BackendClient;
import com.aibuild.services.StructureBuilder;
import com.aibuild.utils.JsonParser;
import com.google.gson.JsonObject;

import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.ChatColor;
import org.bukkit.scheduler.BukkitRunnable;

public class AIBuildCommand implements CommandExecutor {

    private final AIBuildPlugin plugin;
    private final BackendClient backendClient;
    private final StructureBuilder structureBuilder;
    private final int maxDim;

    public AIBuildCommand(AIBuildPlugin plugin, BackendClient backendClient, StructureBuilder structureBuilder, int maxDim) {
        this.plugin = plugin;
        this.backendClient = backendClient;
        this.structureBuilder = structureBuilder;
        this.maxDim = maxDim;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be executed by a player.");
            return true;
        }

        Player player = (Player) sender;
        
        if (!player.hasPermission("aibuild.use")) {
            player.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
        }

        if (args.length == 0) {
            player.sendMessage(ChatColor.RED + "Usage: /aibuild <prompt>");
            player.sendMessage(ChatColor.GRAY + "Example: /aibuild small wooden house with a chimney");
            return true;
        }

        String prompt = String.join(" ", args);
        Location targetLocation = player.getLocation().getBlock().getLocation();
        
        player.sendMessage(ChatColor.YELLOW + "Generating structure: " + ChatColor.WHITE + prompt);
        player.sendMessage(ChatColor.GRAY + "Please wait...");

        // Run backend request asynchronously
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    JsonObject response = backendClient.generateStructure(prompt, maxDim);
                    
                    if (response == null) {
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                player.sendMessage(ChatColor.RED + "Failed to generate structure. Backend might be unavailable.");
                            }
                        }.runTask(plugin);
                        return;
                    }
                    
                    Structure structure = JsonParser.parseStructure(response);
                    
                    // Build synchronously on the main thread
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            try {
                                structureBuilder.buildStructure(player, targetLocation, structure);
                                player.sendMessage(ChatColor.GREEN + "Structure built successfully!");
                                player.sendMessage(ChatColor.GRAY + "Use /aiundo to undo.");
                            } catch (Exception e) {
                                player.sendMessage(ChatColor.RED + "Error building structure: " + e.getMessage());
                                plugin.getLogger().severe("Build error: " + e.getMessage());
                            }
                        }
                    }.runTask(plugin);
                    
                } catch (Exception e) {
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            player.sendMessage(ChatColor.RED + "Error: " + e.getMessage());
                        }
                    }.runTask(plugin);
                    plugin.getLogger().severe("Generation error: " + e.getMessage());
                }
            }
        }.runTaskAsynchronously(plugin);

        return true;
    }
}