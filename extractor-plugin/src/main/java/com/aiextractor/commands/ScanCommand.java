package com.aiextractor.commands;

import com.aiextractor.StructureExtractorPlugin;
import com.aiextractor.scanner.WorldScanner;
import com.aiextractor.scanner.WorldScanner.ScanStats;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ScanCommand implements CommandExecutor, TabCompleter {

    private final StructureExtractorPlugin plugin;
    private final WorldScanner scanner;

    public ScanCommand(StructureExtractorPlugin plugin) {
        this.plugin = plugin;
        this.scanner = plugin.getWorldScanner();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("extractor.scan")) {
            sender.sendMessage(Component.text("You don't have permission to use this command.", NamedTextColor.RED));
            return true;
        }

        if (args.length == 0) {
            sendUsage(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "start":
                return handleStart(sender, args);
            case "stop":
                return handleStop(sender);
            case "status":
                return handleStatus(sender);
            default:
                sendUsage(sender);
                return true;
        }
    }

    private boolean handleStart(CommandSender sender, String[] args) {
        if (scanner.isScanning()) {
            sender.sendMessage(Component.text("A scan is already in progress. Use /scan stop first.", NamedTextColor.RED));
            return true;
        }

        World world;
        Location startLocation = null;
        
        if (args.length > 1) {
            world = plugin.getServer().getWorld(args[1]);
            if (world == null) {
                sender.sendMessage(Component.text("World not found: " + args[1], NamedTextColor.RED));
                return true;
            }
        } else if (sender instanceof Player) {
            Player player = (Player) sender;
            world = player.getWorld();
            startLocation = player.getLocation();  // Scan around the player
        } else {
            sender.sendMessage(Component.text("Please specify a world name.", NamedTextColor.RED));
            return true;
        }

        if (scanner.startScan(world, startLocation)) {
            String locationInfo = startLocation != null ? 
                " around your location" : " around spawn";
            sender.sendMessage(Component.text("Started scanning world: ", NamedTextColor.GREEN)
                .append(Component.text(world.getName(), NamedTextColor.YELLOW))
                .append(Component.text(locationInfo, NamedTextColor.GRAY)));
            sender.sendMessage(Component.text("Use /scan status to check progress, /scan stop to cancel.", NamedTextColor.GRAY));
        } else {
            sender.sendMessage(Component.text("Failed to start scan.", NamedTextColor.RED));
        }

        return true;
    }

    private boolean handleStop(CommandSender sender) {
        if (!scanner.isScanning()) {
            sender.sendMessage(Component.text("No scan is currently running.", NamedTextColor.YELLOW));
            return true;
        }

        ScanStats stats = scanner.getStats();
        scanner.stopScan();
        
        sender.sendMessage(Component.text("Scan stopped.", NamedTextColor.GREEN));
        sender.sendMessage(Component.text("Results:", NamedTextColor.GRAY));
        sender.sendMessage(Component.text("  Chunks scanned: ", NamedTextColor.GRAY).append(Component.text(String.valueOf(stats.getChunksScanned()), NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("  Structures found: ", NamedTextColor.GRAY).append(Component.text(String.valueOf(stats.getStructuresFound()), NamedTextColor.WHITE)));

        return true;
    }

    private boolean handleStatus(CommandSender sender) {
        if (!scanner.isScanning()) {
            sender.sendMessage(Component.text("No scan is currently running.", NamedTextColor.YELLOW));
            return true;
        }

        ScanStats stats = scanner.getStats();
        
        sender.sendMessage(Component.text("=== Scan Status ===", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("World: ", NamedTextColor.GRAY).append(Component.text(stats.getWorldName(), NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("Chunks scanned: ", NamedTextColor.GRAY).append(Component.text(String.valueOf(stats.getChunksScanned()), NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("Chunks in queue: ", NamedTextColor.GRAY).append(Component.text(String.valueOf(stats.getChunksRemaining()), NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("Structures found: ", NamedTextColor.GRAY).append(Component.text(String.valueOf(stats.getStructuresFound()), NamedTextColor.GREEN)));

        return true;
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage(Component.text("=== Scan Commands ===", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/scan start [world]", NamedTextColor.YELLOW).append(Component.text(" - Start auto-scanning", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/scan stop", NamedTextColor.YELLOW).append(Component.text(" - Stop scanning", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/scan status", NamedTextColor.YELLOW).append(Component.text(" - Check scan progress", NamedTextColor.GRAY)));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            List<String> subCommands = Arrays.asList("start", "stop", "status");
            for (String sub : subCommands) {
                if (sub.startsWith(args[0].toLowerCase())) {
                    completions.add(sub);
                }
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("start")) {
            for (World world : plugin.getServer().getWorlds()) {
                if (world.getName().toLowerCase().startsWith(args[1].toLowerCase())) {
                    completions.add(world.getName());
                }
            }
        }

        return completions;
    }
}
