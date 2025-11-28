package com.aibuild.commands;

import com.aibuild.AIBuildPlugin;
import com.aibuild.models.UndoBuffer;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class AIUndoCommand implements CommandExecutor {

    private final AIBuildPlugin plugin;
    private final UndoBuffer undoBuffer;

    public AIUndoCommand(AIBuildPlugin plugin, UndoBuffer undoBuffer) {
        this.plugin = plugin;
        this.undoBuffer = undoBuffer;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be executed by a player.");
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("aiundo.use")) {
            player.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
        }

        if (undoBuffer.hasUndo(player)) {
            int blocksRestored = undoBuffer.undo(player);
            player.sendMessage(ChatColor.GREEN + "Undo successful! " + blocksRestored + " blocks restored.");
        } else {
            player.sendMessage(ChatColor.YELLOW + "Nothing to undo.");
        }

        return true;
    }
}