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
    private final int defaultWidth;
    private final int defaultDepth;
    private final int defaultHeight;

    public AIBuildCommand(AIBuildPlugin plugin, BackendClient backendClient, StructureBuilder structureBuilder, 
                          int defaultWidth, int defaultDepth, int defaultHeight) {
        this.plugin = plugin;
        this.backendClient = backendClient;
        this.structureBuilder = structureBuilder;
        this.defaultWidth = defaultWidth;
        this.defaultDepth = defaultDepth;
        this.defaultHeight = defaultHeight;
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
            player.sendMessage(ChatColor.RED + "Usage: /aibuild <prompt> [,width,depth,height]");
            player.sendMessage(ChatColor.GRAY + "Example: /aibuild a desert house ,32,32,40");
            player.sendMessage(ChatColor.GRAY + "Use /aihelp for more info.");
            return true;
        }

        // Join args and parse for dimensions
        String fullInput = String.join(" ", args);
        String prompt;
        int width = defaultWidth;
        int depth = defaultDepth;
        int height = defaultHeight;

        // Check if dimensions are specified (format: prompt ,width,depth,height)
        if (fullInput.contains(",")) {
            String[] parts = fullInput.split(",");
            prompt = parts[0].trim();
            
            try {
                if (parts.length >= 2 && !parts[1].trim().isEmpty()) {
                    width = Integer.parseInt(parts[1].trim());
                }
                if (parts.length >= 3 && !parts[2].trim().isEmpty()) {
                    depth = Integer.parseInt(parts[2].trim());
                }
                if (parts.length >= 4 && !parts[3].trim().isEmpty()) {
                    height = Integer.parseInt(parts[3].trim());
                }
            } catch (NumberFormatException e) {
                player.sendMessage(ChatColor.RED + "Invalid dimensions! Use numbers only.");
                player.sendMessage(ChatColor.GRAY + "Example: /aibuild a desert house ,32,32,40");
                return true;
            }

            // Validate dimensions
            if (width < 1 || width > 64 || depth < 1 || depth > 64 || height < 1 || height > 128) {
                player.sendMessage(ChatColor.RED + "Invalid dimensions! Width/Depth: 1-64, Height: 1-128");
                return true;
            }
        } else {
            prompt = fullInput;
        }

        Location targetLocation = player.getLocation().getBlock().getLocation();
        final int finalWidth = width;
        final int finalDepth = depth;
        final int finalHeight = height;
        
        player.sendMessage(ChatColor.YELLOW + "Generating structure: " + ChatColor.WHITE + prompt);
        player.sendMessage(ChatColor.GRAY + "Dimensions: " + finalWidth + "x" + finalDepth + "x" + finalHeight);
        player.sendMessage(ChatColor.GRAY + "Please wait...");

        // Run backend request asynchronously
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    JsonObject response = backendClient.generateStructure(prompt, finalWidth, finalDepth, finalHeight);
                    
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