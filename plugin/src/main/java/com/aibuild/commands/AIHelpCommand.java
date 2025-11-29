package com.aibuild.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class AIHelpCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        sender.sendMessage(Component.text("========== AI Build Help ==========", NamedTextColor.GOLD));
        sender.sendMessage(Component.empty());
        sender.sendMessage(Component.text("/aibuild <prompt> [,width,depth,height]", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("  Generate an AI structure at your location.", NamedTextColor.GRAY));
        sender.sendMessage(Component.text("  Dimensions are optional (default: 16x16x16)", NamedTextColor.GRAY));
        sender.sendMessage(Component.empty());
        sender.sendMessage(Component.text("  Examples:", NamedTextColor.WHITE));
        sender.sendMessage(Component.text("    /aibuild a small wooden house", NamedTextColor.AQUA));
        sender.sendMessage(Component.text("      (Uses default 16x16x16 dimensions)", NamedTextColor.GRAY));
        sender.sendMessage(Component.empty());
        sender.sendMessage(Component.text("    /aibuild a desert house ,32,32,40", NamedTextColor.AQUA));
        sender.sendMessage(Component.text("      (Uses 32 width, 32 depth, 40 height)", NamedTextColor.GRAY));
        sender.sendMessage(Component.empty());
        sender.sendMessage(Component.text("    /aibuild medieval castle with towers ,48,48,64", NamedTextColor.AQUA));
        sender.sendMessage(Component.text("      (Uses 48 width, 48 depth, 64 height)", NamedTextColor.GRAY));
        sender.sendMessage(Component.empty());
        sender.sendMessage(Component.text("/aiundo", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("  Undo the last AI-generated structure.", NamedTextColor.GRAY));
        sender.sendMessage(Component.empty());
        sender.sendMessage(Component.text("/aihelp", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("  Show this help message.", NamedTextColor.GRAY));
        sender.sendMessage(Component.empty());
        sender.sendMessage(Component.text("====================================", NamedTextColor.GOLD));
        return true;
    }
}
