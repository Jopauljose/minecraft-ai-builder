package com.aiextractor.commands;

import com.aiextractor.StructureExtractorPlugin;
import com.aiextractor.detection.StructureDetector;
import com.aiextractor.detection.StructureDetector.DetectionResult;
import com.aiextractor.export.StructureExporter;
import com.aiextractor.export.StructureExporter.ExportResult;
import com.aiextractor.models.BoundingBox;
import com.aiextractor.models.ExtractedStructure;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CaptureCommand implements CommandExecutor, TabCompleter {

    private final StructureExtractorPlugin plugin;
    private final StructureDetector detector;
    private final StructureExporter exporter;

    public CaptureCommand(StructureExtractorPlugin plugin) {
        this.plugin = plugin;
        this.detector = plugin.getStructureDetector();
        this.exporter = plugin.getStructureExporter();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Component.text("This command can only be used by players.", NamedTextColor.RED));
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("extractor.capture")) {
            player.sendMessage(Component.text("You don't have permission to use this command.", NamedTextColor.RED));
            return true;
        }

        if (args.length == 0) {
            sendUsage(player);
            return true;
        }

        String subCommand = args[0].toLowerCase();
        String name = args.length > 1 ? args[1] : null;

        switch (subCommand) {
            case "auto":
                return handleAuto(player, name);
            case "block":
                return handleBlock(player, name);
            case "selection":
                return handleSelection(player, name);
            case "pos1":
                return handlePos1(player);
            case "pos2":
                return handlePos2(player);
            default:
                sendUsage(player);
                return true;
        }
    }

    /**
     * Capture structure automatically from player's location
     */
    private boolean handleAuto(Player player, String name) {
        player.sendMessage(Component.text("Detecting structure near you...", NamedTextColor.YELLOW));

        Location loc = player.getLocation();
        DetectionResult result = detector.detectFromLocation(loc);

        if (!result.isSuccess()) {
            player.sendMessage(Component.text("Detection failed: " + result.getMessage(), NamedTextColor.RED));
            return true;
        }

        BoundingBox box = result.getBoundingBox();
        player.sendMessage(Component.text("Structure detected! ", NamedTextColor.GREEN)
            .append(Component.text("(" + result.getBlockCount() + " blocks, " + 
                box.getSizeX() + "x" + box.getSizeY() + "x" + box.getSizeZ() + ")", NamedTextColor.GRAY)));

        // Extract and export
        extractAndExport(player, box, name, "auto");
        return true;
    }

    /**
     * Capture structure from the block the player is looking at
     */
    private boolean handleBlock(Player player, String name) {
        Block targetBlock = player.getTargetBlockExact(100);
        
        if (targetBlock == null) {
            player.sendMessage(Component.text("No block in sight (max 100 blocks).", NamedTextColor.RED));
            return true;
        }

        player.sendMessage(Component.text("Detecting structure from " + 
            targetBlock.getType().name() + " at " + formatLocation(targetBlock.getLocation()) + "...", NamedTextColor.YELLOW));

        DetectionResult result = detector.detectFromLocation(targetBlock.getLocation());

        if (!result.isSuccess()) {
            player.sendMessage(Component.text("Detection failed: " + result.getMessage(), NamedTextColor.RED));
            return true;
        }

        BoundingBox box = result.getBoundingBox();
        player.sendMessage(Component.text("Structure detected! ", NamedTextColor.GREEN)
            .append(Component.text("(" + result.getBlockCount() + " blocks, " + 
                box.getSizeX() + "x" + box.getSizeY() + "x" + box.getSizeZ() + ")", NamedTextColor.GRAY)));

        extractAndExport(player, box, name, "block");
        return true;
    }

    // Store player selections for manual mode
    private static final java.util.Map<java.util.UUID, Location> pos1Map = new java.util.HashMap<>();
    private static final java.util.Map<java.util.UUID, Location> pos2Map = new java.util.HashMap<>();

    /**
     * Set position 1 for manual selection
     */
    private boolean handlePos1(Player player) {
        Location loc = player.getLocation().getBlock().getLocation();
        pos1Map.put(player.getUniqueId(), loc);
        player.sendMessage(Component.text("Position 1 set to " + formatLocation(loc), NamedTextColor.GREEN));
        
        if (pos2Map.containsKey(player.getUniqueId())) {
            Location pos2 = pos2Map.get(player.getUniqueId());
            BoundingBox box = new BoundingBox(loc, pos2);
            player.sendMessage(Component.text("Selection size: " + 
                box.getSizeX() + "x" + box.getSizeY() + "x" + box.getSizeZ(), NamedTextColor.GRAY));
        }
        
        return true;
    }

    /**
     * Set position 2 for manual selection
     */
    private boolean handlePos2(Player player) {
        Location loc = player.getLocation().getBlock().getLocation();
        pos2Map.put(player.getUniqueId(), loc);
        player.sendMessage(Component.text("Position 2 set to " + formatLocation(loc), NamedTextColor.GREEN));
        
        if (pos1Map.containsKey(player.getUniqueId())) {
            Location pos1 = pos1Map.get(player.getUniqueId());
            BoundingBox box = new BoundingBox(pos1, loc);
            player.sendMessage(Component.text("Selection size: " + 
                box.getSizeX() + "x" + box.getSizeY() + "x" + box.getSizeZ(), NamedTextColor.GRAY));
        }
        
        return true;
    }

    /**
     * Capture from manual selection or WorldEdit selection
     */
    private boolean handleSelection(Player player, String name) {
        Location pos1 = pos1Map.get(player.getUniqueId());
        Location pos2 = pos2Map.get(player.getUniqueId());

        // Try WorldEdit selection first if available
        BoundingBox box = getWorldEditSelection(player);
        
        if (box == null) {
            // Fall back to manual selection
            if (pos1 == null || pos2 == null) {
                player.sendMessage(Component.text("No selection defined!", NamedTextColor.RED));
                player.sendMessage(Component.text("Use /capture pos1 and /capture pos2 to define corners,", NamedTextColor.GRAY));
                player.sendMessage(Component.text("or use WorldEdit's //wand to select a region.", NamedTextColor.GRAY));
                return true;
            }
            box = new BoundingBox(pos1, pos2);
        }

        player.sendMessage(Component.text("Exporting selection: ", NamedTextColor.GREEN)
            .append(Component.text(box.getSizeX() + "x" + box.getSizeY() + "x" + box.getSizeZ(), NamedTextColor.GRAY)));

        extractAndExport(player, box, name, "selection");
        return true;
    }

    /**
     * Try to get WorldEdit selection
     */
    private BoundingBox getWorldEditSelection(Player player) {
        try {
            // Check if WorldEdit is available
            if (plugin.getServer().getPluginManager().getPlugin("WorldEdit") == null) {
                return null;
            }

            com.sk89q.worldedit.bukkit.WorldEditPlugin worldEdit = 
                (com.sk89q.worldedit.bukkit.WorldEditPlugin) plugin.getServer()
                    .getPluginManager().getPlugin("WorldEdit");
            
            com.sk89q.worldedit.regions.Region region = worldEdit.getSession(player)
                .getSelection(com.sk89q.worldedit.bukkit.BukkitAdapter.adapt(player.getWorld()));
            
            if (region == null) {
                return null;
            }

            com.sk89q.worldedit.math.BlockVector3 min = region.getMinimumPoint();
            com.sk89q.worldedit.math.BlockVector3 max = region.getMaximumPoint();

            return new BoundingBox(player.getWorld(),
                min.x(), min.y(), min.z(),
                max.x(), max.y(), max.z());

        } catch (Exception e) {
            // WorldEdit not available or no selection
            return null;
        }
    }

    /**
     * Extract structure and export to file
     */
    private void extractAndExport(Player player, BoundingBox box, String name, String mode) {
        player.sendMessage(Component.text("Extracting structure...", NamedTextColor.YELLOW));

        boolean includeAir = plugin.getConfig().getBoolean("export.include-air", false);
        ExtractedStructure structure = detector.extractStructure(box, name, includeAir);
        
        // Set metadata
        structure.getMetadata().setCaptureMode(mode);
        structure.getMetadata().setCapturedBy(player.getName());

        // Export asynchronously
        exporter.exportAsync(structure).thenAccept(result -> {
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (result.isSuccess()) {
                    player.sendMessage(Component.text("Structure exported successfully!", NamedTextColor.GREEN));
                    player.sendMessage(Component.text("File: " + result.getOutputFile().getName(), NamedTextColor.GRAY));
                    player.sendMessage(Component.text("Blocks: " + result.getBlockCount(), NamedTextColor.GRAY));
                } else {
                    player.sendMessage(Component.text("Export failed: " + result.getError(), NamedTextColor.RED));
                }
            });
        });
    }

    private String formatLocation(Location loc) {
        return String.format("(%d, %d, %d)", loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
    }

    private void sendUsage(Player player) {
        player.sendMessage(Component.text("=== Capture Commands ===", NamedTextColor.GOLD));
        player.sendMessage(Component.text("/capture auto [name]", NamedTextColor.YELLOW).append(Component.text(" - Detect & export nearest structure", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("/capture block [name]", NamedTextColor.YELLOW).append(Component.text(" - Detect from looked-at block", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("/capture selection [name]", NamedTextColor.YELLOW).append(Component.text(" - Export selected region", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("/capture pos1", NamedTextColor.YELLOW).append(Component.text(" - Set selection corner 1", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("/capture pos2", NamedTextColor.YELLOW).append(Component.text(" - Set selection corner 2", NamedTextColor.GRAY)));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            List<String> subCommands = Arrays.asList("auto", "block", "selection", "pos1", "pos2");
            for (String sub : subCommands) {
                if (sub.startsWith(args[0].toLowerCase())) {
                    completions.add(sub);
                }
            }
        }

        return completions;
    }
}
