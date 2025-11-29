package com.aiextractor.commands;

import com.aiextractor.StructureExtractorPlugin;
import com.aiextractor.builder.StructureBuilder;
import com.aiextractor.builder.StructureBuilder.BuildResult;
import com.aiextractor.models.ExtractedStructure;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Command to build structures from exported JSON files
 * Usage: /build <filename> - Build structure at player location
 *        /build list - List available structure files
 *        /build info <filename> - Show structure info
 *        /build undo - Undo last built structure
 */
public class BuildCommand implements CommandExecutor, TabCompleter {

    private final StructureExtractorPlugin plugin;
    private final StructureBuilder builder;

    public BuildCommand(StructureExtractorPlugin plugin) {
        this.plugin = plugin;
        this.builder = plugin.getStructureBuilder();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Component.text("This command can only be used by players.", NamedTextColor.RED));
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("extractor.build")) {
            player.sendMessage(Component.text("You don't have permission to use this command.", NamedTextColor.RED));
            return true;
        }

        if (args.length == 0) {
            sendUsage(player);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "list":
                return handleList(player);
            case "info":
                if (args.length < 2) {
                    player.sendMessage(Component.text("Usage: /build info <filename>", NamedTextColor.RED));
                    return true;
                }
                return handleInfo(player, args[1]);
            case "undo":
                return handleUndo(player);
            case "help":
                sendUsage(player);
                return true;
            default:
                // Assume it's a filename
                return handleBuild(player, args[0]);
        }
    }

    /**
     * Build a structure at player's location
     */
    private boolean handleBuild(Player player, String filename) {
        File structureFile = builder.findStructureFile(filename);
        
        if (structureFile == null) {
            player.sendMessage(Component.text("Structure file not found: " + filename, NamedTextColor.RED));
            player.sendMessage(Component.text("Use /build list to see available structures.", NamedTextColor.GRAY));
            return true;
        }

        try {
            ExtractedStructure structure = builder.loadStructure(structureFile);
            
            player.sendMessage(Component.text("Building structure: ", NamedTextColor.YELLOW)
                .append(Component.text(structure.getName(), NamedTextColor.WHITE)));
            player.sendMessage(Component.text("Size: " + structure.getSizeX() + "x" + 
                structure.getSizeY() + "x" + structure.getSizeZ() + 
                " (" + structure.getBlockCount() + " blocks)", NamedTextColor.GRAY));

            Location origin = player.getLocation().getBlock().getLocation();
            BuildResult result = builder.buildStructure(player, origin, structure);

            if (result.isSuccess()) {
                player.sendMessage(Component.text("Structure built successfully! ", NamedTextColor.GREEN)
                    .append(Component.text(result.getBlocksPlaced() + " blocks placed.", NamedTextColor.GRAY)));
                player.sendMessage(Component.text("Use /build undo to remove it.", NamedTextColor.GRAY));
            } else {
                player.sendMessage(Component.text("Structure built with some errors. ", NamedTextColor.YELLOW)
                    .append(Component.text(result.getBlocksPlaced() + " blocks placed.", NamedTextColor.GRAY)));
                if (result.getError() != null) {
                    player.sendMessage(Component.text("Errors: " + result.getError(), NamedTextColor.RED));
                }
            }

        } catch (Exception e) {
            player.sendMessage(Component.text("Failed to load structure: " + e.getMessage(), NamedTextColor.RED));
            plugin.getLogger().severe("Error loading structure: " + e.getMessage());
        }

        return true;
    }

    /**
     * List available structure files
     */
    private boolean handleList(Player player) {
        List<File> files = builder.listStructureFiles();
        
        if (files.isEmpty()) {
            player.sendMessage(Component.text("No structure files found.", NamedTextColor.YELLOW));
            player.sendMessage(Component.text("Use /capture to export structures first.", NamedTextColor.GRAY));
            return true;
        }

        player.sendMessage(Component.text("=== Available Structures (" + files.size() + ") ===", NamedTextColor.GOLD));
        
        int count = 0;
        for (File file : files) {
            if (count >= 20) {
                player.sendMessage(Component.text("... and " + (files.size() - 20) + " more", NamedTextColor.GRAY));
                break;
            }
            
            String worldName = file.getParentFile().getName();
            String displayName = file.getName().replace(".json", "");
            
            player.sendMessage(Component.text("  " + displayName, NamedTextColor.WHITE)
                .append(Component.text(" (" + worldName + ")", NamedTextColor.GRAY)));
            count++;
        }

        player.sendMessage(Component.text("Use /build <name> to build a structure.", NamedTextColor.GRAY));
        return true;
    }

    /**
     * Show info about a structure file
     */
    private boolean handleInfo(Player player, String filename) {
        File structureFile = builder.findStructureFile(filename);
        
        if (structureFile == null) {
            player.sendMessage(Component.text("Structure file not found: " + filename, NamedTextColor.RED));
            return true;
        }

        try {
            ExtractedStructure structure = builder.loadStructure(structureFile);
            
            player.sendMessage(Component.text("=== Structure Info ===", NamedTextColor.GOLD));
            player.sendMessage(Component.text("Name: ", NamedTextColor.GRAY)
                .append(Component.text(structure.getName(), NamedTextColor.WHITE)));
            player.sendMessage(Component.text("Size: ", NamedTextColor.GRAY)
                .append(Component.text(structure.getSizeX() + " x " + structure.getSizeY() + " x " + structure.getSizeZ(), NamedTextColor.WHITE)));
            player.sendMessage(Component.text("Blocks: ", NamedTextColor.GRAY)
                .append(Component.text(String.valueOf(structure.getBlockCount()), NamedTextColor.WHITE)));
            player.sendMessage(Component.text("File: ", NamedTextColor.GRAY)
                .append(Component.text(structureFile.getName(), NamedTextColor.WHITE)));
            
            if (structure.getMetadata() != null) {
                if (structure.getMetadata().getSourceWorld() != null) {
                    player.sendMessage(Component.text("Source World: ", NamedTextColor.GRAY)
                        .append(Component.text(structure.getMetadata().getSourceWorld(), NamedTextColor.WHITE)));
                }
                if (structure.getMetadata().getCaptureMode() != null) {
                    player.sendMessage(Component.text("Capture Mode: ", NamedTextColor.GRAY)
                        .append(Component.text(structure.getMetadata().getCaptureMode(), NamedTextColor.WHITE)));
                }
                if (structure.getMetadata().getCapturedBy() != null) {
                    player.sendMessage(Component.text("Captured By: ", NamedTextColor.GRAY)
                        .append(Component.text(structure.getMetadata().getCapturedBy(), NamedTextColor.WHITE)));
                }
            }

        } catch (Exception e) {
            player.sendMessage(Component.text("Failed to read structure: " + e.getMessage(), NamedTextColor.RED));
        }

        return true;
    }

    /**
     * Undo the last built structure
     */
    private boolean handleUndo(Player player) {
        if (!builder.hasUndo(player)) {
            player.sendMessage(Component.text("Nothing to undo.", NamedTextColor.YELLOW));
            return true;
        }

        int restored = builder.undoLastBuild(player);
        player.sendMessage(Component.text("Undo successful! ", NamedTextColor.GREEN)
            .append(Component.text(restored + " blocks restored.", NamedTextColor.GRAY)));

        return true;
    }

    private void sendUsage(Player player) {
        player.sendMessage(Component.text("=== Build Commands ===", NamedTextColor.GOLD));
        player.sendMessage(Component.text("/build <filename>", NamedTextColor.YELLOW)
            .append(Component.text(" - Build structure at your location", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("/build list", NamedTextColor.YELLOW)
            .append(Component.text(" - List available structure files", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("/build info <filename>", NamedTextColor.YELLOW)
            .append(Component.text(" - Show structure info", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("/build undo", NamedTextColor.YELLOW)
            .append(Component.text(" - Undo last built structure", NamedTextColor.GRAY)));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            List<String> options = new ArrayList<>();
            options.add("list");
            options.add("info");
            options.add("undo");
            options.add("help");
            
            // Add structure filenames
            for (File file : builder.listStructureFiles()) {
                options.add(file.getName().replace(".json", ""));
            }
            
            String prefix = args[0].toLowerCase();
            completions = options.stream()
                .filter(s -> s.toLowerCase().startsWith(prefix))
                .collect(Collectors.toList());
                
        } else if (args.length == 2 && args[0].equalsIgnoreCase("info")) {
            String prefix = args[1].toLowerCase();
            completions = builder.listStructureFiles().stream()
                .map(f -> f.getName().replace(".json", ""))
                .filter(s -> s.toLowerCase().startsWith(prefix))
                .collect(Collectors.toList());
        }

        return completions;
    }
}
