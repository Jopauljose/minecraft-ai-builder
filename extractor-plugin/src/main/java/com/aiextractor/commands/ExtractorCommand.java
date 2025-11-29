package com.aiextractor.commands;

import com.aiextractor.StructureExtractorPlugin;
import com.aiextractor.export.StructureExporter;
import com.aiextractor.export.StructureExporter.ExportStats;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ExtractorCommand implements CommandExecutor, TabCompleter {

    private final StructureExtractorPlugin plugin;
    private final StructureExporter exporter;

    public ExtractorCommand(StructureExtractorPlugin plugin) {
        this.plugin = plugin;
        this.exporter = plugin.getStructureExporter();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("extractor.admin")) {
            sender.sendMessage(Component.text("You don't have permission to use this command.", NamedTextColor.RED));
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "help":
                sendHelp(sender);
                break;
            case "stats":
                sendStats(sender);
                break;
            case "config":
                sendConfig(sender);
                break;
            case "reload":
                reloadConfig(sender);
                break;
            default:
                sendHelp(sender);
        }

        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(Component.text("========== Structure Extractor Help ==========", NamedTextColor.GOLD));
        sender.sendMessage(Component.empty());
        sender.sendMessage(Component.text("Scanning Commands:", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("  /scan start [world]", NamedTextColor.WHITE).append(Component.text(" - Start auto-scanning a world", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("  /scan stop", NamedTextColor.WHITE).append(Component.text(" - Stop the current scan", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("  /scan status", NamedTextColor.WHITE).append(Component.text(" - Check scan progress", NamedTextColor.GRAY)));
        sender.sendMessage(Component.empty());
        sender.sendMessage(Component.text("Capture Commands:", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("  /capture auto [name]", NamedTextColor.WHITE).append(Component.text(" - Auto-detect nearest structure", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("  /capture block [name]", NamedTextColor.WHITE).append(Component.text(" - Detect from looked-at block", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("  /capture selection [name]", NamedTextColor.WHITE).append(Component.text(" - Export selected region", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("  /capture pos1", NamedTextColor.WHITE).append(Component.text(" - Set selection corner 1", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("  /capture pos2", NamedTextColor.WHITE).append(Component.text(" - Set selection corner 2", NamedTextColor.GRAY)));
        sender.sendMessage(Component.empty());
        sender.sendMessage(Component.text("Admin Commands:", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("  /extractor stats", NamedTextColor.WHITE).append(Component.text(" - Show export statistics", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("  /extractor config", NamedTextColor.WHITE).append(Component.text(" - Show current config", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("  /extractor reload", NamedTextColor.WHITE).append(Component.text(" - Reload configuration", NamedTextColor.GRAY)));
        sender.sendMessage(Component.empty());
        sender.sendMessage(Component.text("Exported structures are saved to:", NamedTextColor.GRAY));
        sender.sendMessage(Component.text("  " + plugin.getExportDirectory().getAbsolutePath(), NamedTextColor.WHITE));
        sender.sendMessage(Component.text("==============================================", NamedTextColor.GOLD));
    }

    private void sendStats(CommandSender sender) {
        ExportStats stats = exporter.getStats();
        
        sender.sendMessage(Component.text("=== Export Statistics ===", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("Total structures exported: ", NamedTextColor.GRAY).append(Component.text(String.valueOf(stats.getTotalFiles()), NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("Total file size: ", NamedTextColor.GRAY).append(Component.text(stats.getTotalSizeFormatted(), NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("Worlds with exports: ", NamedTextColor.GRAY).append(Component.text(String.valueOf(stats.getWorldCount()), NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("Export directory: ", NamedTextColor.GRAY).append(Component.text(plugin.getExportDirectory().getAbsolutePath(), NamedTextColor.WHITE)));
    }

    private void sendConfig(CommandSender sender) {
        sender.sendMessage(Component.text("=== Current Configuration ===", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("Pretty JSON: ", NamedTextColor.GRAY).append(Component.text(String.valueOf(plugin.getConfig().getBoolean("export.pretty-json")), NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("Include air blocks: ", NamedTextColor.GRAY).append(Component.text(String.valueOf(plugin.getConfig().getBoolean("export.include-air")), NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("Chunks per tick: ", NamedTextColor.GRAY).append(Component.text(String.valueOf(plugin.getConfig().getInt("scanner.chunks-per-tick")), NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("Min structure size: ", NamedTextColor.GRAY).append(Component.text(plugin.getConfig().getInt("scanner.min-structure-size") + " blocks", NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("Max structure size: ", NamedTextColor.GRAY).append(Component.text(plugin.getConfig().getInt("scanner.max-structure-size") + " blocks", NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("Max dimensions: ", NamedTextColor.GRAY).append(Component.text(
            plugin.getConfig().getInt("scanner.max-dimensions.x") + "x" +
            plugin.getConfig().getInt("scanner.max-dimensions.y") + "x" +
            plugin.getConfig().getInt("scanner.max-dimensions.z"), NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("Flood fill max iterations: ", NamedTextColor.GRAY).append(Component.text(String.valueOf(plugin.getConfig().getInt("detection.max-flood-iterations")), NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("Search radius: ", NamedTextColor.GRAY).append(Component.text(plugin.getConfig().getInt("detection.search-radius") + " blocks", NamedTextColor.WHITE)));
    }

    private void reloadConfig(CommandSender sender) {
        plugin.reloadConfig();
        sender.sendMessage(Component.text("Configuration reloaded!", NamedTextColor.GREEN));
        sender.sendMessage(Component.text("Note: Some settings require a server restart to take full effect.", NamedTextColor.YELLOW));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            List<String> subCommands = Arrays.asList("help", "stats", "config", "reload");
            for (String sub : subCommands) {
                if (sub.startsWith(args[0].toLowerCase())) {
                    completions.add(sub);
                }
            }
        }

        return completions;
    }
}
