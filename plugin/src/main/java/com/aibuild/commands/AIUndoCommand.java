package com.aibuild.commands;

import com.aibuild.AIBuildPlugin;
import com.aibuild.models.UndoBuffer;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class AIUndoCommand implements CommandExecutor {

    private final UndoBuffer undoBuffer;

    public AIUndoCommand(AIBuildPlugin plugin, UndoBuffer undoBuffer) {
        this.undoBuffer = undoBuffer;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Component.text("This command can only be executed by a player.", NamedTextColor.RED));
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("aiundo.use")) {
            player.sendMessage(Component.text("You don't have permission to use this command.", NamedTextColor.RED));
            return true;
        }

        if (undoBuffer.hasUndo(player)) {
            int blocksRestored = undoBuffer.undo(player);
            player.sendMessage(Component.text("Undo successful! " + blocksRestored + " blocks restored.", NamedTextColor.GREEN));
        } else {
            player.sendMessage(Component.text("Nothing to undo.", NamedTextColor.YELLOW));
        }

        return true;
    }
}