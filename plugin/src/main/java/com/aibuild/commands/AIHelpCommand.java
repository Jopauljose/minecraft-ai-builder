package com.aibuild.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class AIHelpCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        sender.sendMessage(ChatColor.GOLD + "========== AI Build Help ==========");
        sender.sendMessage("");
        sender.sendMessage(ChatColor.YELLOW + "/aibuild <prompt> [,width,depth,height]");
        sender.sendMessage(ChatColor.GRAY + "  Generate an AI structure at your location.");
        sender.sendMessage(ChatColor.GRAY + "  Dimensions are optional (default: 16x16x16)");
        sender.sendMessage("");
        sender.sendMessage(ChatColor.WHITE + "  Examples:");
        sender.sendMessage(ChatColor.AQUA + "    /aibuild a small wooden house");
        sender.sendMessage(ChatColor.GRAY + "      (Uses default 16x16x16 dimensions)");
        sender.sendMessage("");
        sender.sendMessage(ChatColor.AQUA + "    /aibuild a desert house ,32,32,40");
        sender.sendMessage(ChatColor.GRAY + "      (Uses 32 width, 32 depth, 40 height)");
        sender.sendMessage("");
        sender.sendMessage(ChatColor.AQUA + "    /aibuild medieval castle with towers ,48,48,64");
        sender.sendMessage(ChatColor.GRAY + "      (Uses 48 width, 48 depth, 64 height)");
        sender.sendMessage("");
        sender.sendMessage(ChatColor.YELLOW + "/aiundo");
        sender.sendMessage(ChatColor.GRAY + "  Undo the last AI-generated structure.");
        sender.sendMessage("");
        sender.sendMessage(ChatColor.YELLOW + "/aihelp");
        sender.sendMessage(ChatColor.GRAY + "  Show this help message.");
        sender.sendMessage("");
        sender.sendMessage(ChatColor.GOLD + "====================================");
        return true;
    }
}
