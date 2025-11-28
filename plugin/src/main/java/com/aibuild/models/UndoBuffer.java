package com.aibuild.models;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.UUID;

/**
 * Per-player undo buffer that stores block states before structure placement
 */
public class UndoBuffer {
    
    // Map of player UUID to their undo stack
    private final Map<UUID, Stack<UndoEntry>> playerUndoStacks;
    private static final int MAX_UNDO_HISTORY = 10;

    public UndoBuffer() {
        this.playerUndoStacks = new HashMap<>();
    }
    
    /**
     * Represents a single undo operation containing multiple block changes
     */
    public static class BlockState {
        private final Location location;
        private final Material material;
        private final byte data;
        
        public BlockState(Location location, Material material) {
            this.location = location.clone();
            this.material = material;
            this.data = 0;
        }
        
        public Location getLocation() {
            return location;
        }
        
        public Material getMaterial() {
            return material;
        }
    }
    
    public static class UndoEntry {
        private final List<BlockState> blockStates;
        private final long timestamp;
        
        public UndoEntry() {
            this.blockStates = new ArrayList<>();
            this.timestamp = System.currentTimeMillis();
        }
        
        public void addBlockState(BlockState state) {
            blockStates.add(state);
        }
        
        public List<BlockState> getBlockStates() {
            return blockStates;
        }
        
        public long getTimestamp() {
            return timestamp;
        }
    }

    /**
     * Start recording block states for an undo operation
     * @param player The player performing the action
     * @return A new UndoEntry to record block states
     */
    public UndoEntry startRecording(Player player) {
        return new UndoEntry();
    }
    
    /**
     * Save an undo entry for a player
     * @param player The player
     * @param entry The undo entry to save
     */
    public void saveEntry(Player player, UndoEntry entry) {
        UUID playerId = player.getUniqueId();
        playerUndoStacks.computeIfAbsent(playerId, k -> new Stack<>());
        
        Stack<UndoEntry> stack = playerUndoStacks.get(playerId);
        
        // Limit undo history
        while (stack.size() >= MAX_UNDO_HISTORY) {
            stack.remove(0);
        }
        
        stack.push(entry);
    }

    /**
     * Check if a player has any undo actions available
     * @param player The player to check
     * @return true if there are undo actions available
     */
    public boolean hasUndo(Player player) {
        Stack<UndoEntry> stack = playerUndoStacks.get(player.getUniqueId());
        return stack != null && !stack.isEmpty();
    }

    /**
     * Undo the last action for a player
     * @param player The player
     * @return Number of blocks restored
     */
    public int undo(Player player) {
        Stack<UndoEntry> stack = playerUndoStacks.get(player.getUniqueId());
        if (stack == null || stack.isEmpty()) {
            return 0;
        }
        
        UndoEntry entry = stack.pop();
        int count = 0;
        
        for (BlockState state : entry.getBlockStates()) {
            Location loc = state.getLocation();
            World world = loc.getWorld();
            if (world != null) {
                Block block = world.getBlockAt(loc);
                block.setType(state.getMaterial(), false);
                count++;
            }
        }
        
        return count;
    }
    
    /**
     * Clear all undo history for a player
     * @param player The player
     */
    public void clearHistory(Player player) {
        playerUndoStacks.remove(player.getUniqueId());
    }
    
    /**
     * Get the number of undo actions available for a player
     * @param player The player
     * @return Number of undo actions
     */
    public int getUndoCount(Player player) {
        Stack<UndoEntry> stack = playerUndoStacks.get(player.getUniqueId());
        return stack != null ? stack.size() : 0;
    }
}