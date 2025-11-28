package com.aibuild.services;

import com.aibuild.AIBuildPlugin;
import com.aibuild.models.Structure;
import com.aibuild.models.UndoBuffer;
import com.aibuild.models.UndoBuffer.BlockState;
import com.aibuild.models.UndoBuffer.UndoEntry;
import com.aibuild.utils.BlockValidator;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.Map;

public class StructureBuilder {

    private final AIBuildPlugin plugin;
    private final UndoBuffer undoBuffer;

    public StructureBuilder(AIBuildPlugin plugin, UndoBuffer undoBuffer) {
        this.plugin = plugin;
        this.undoBuffer = undoBuffer;
    }

    /**
     * Build a structure at the given location
     * @param player The player building the structure
     * @param startLocation The starting location (bottom-left corner)
     * @param structure The structure to build
     */
    public void buildStructure(Player player, Location startLocation, Structure structure) {
        World world = startLocation.getWorld();
        if (world == null) {
            throw new IllegalArgumentException("World cannot be null");
        }

        int[] size = structure.getSize();
        Map<String, String> palette = structure.getPalette();
        Map<Integer, String[][]> layers = structure.getLayers();

        // Start recording for undo
        UndoEntry undoEntry = undoBuffer.startRecording(player);

        int blocksPlaced = 0;

        // Iterate through each layer (Y level)
        for (Map.Entry<Integer, String[][]> layerEntry : layers.entrySet()) {
            int layerIndex = layerEntry.getKey();
            String[][] layerData = layerEntry.getValue();

            if (layerData == null) continue;

            // Iterate through Z (rows)
            for (int z = 0; z < layerData.length; z++) {
                String[] row = layerData[z];
                if (row == null) continue;

                // Iterate through X (columns)
                for (int x = 0; x < row.length; x++) {
                    String paletteKey = row[x];
                    if (paletteKey == null || paletteKey.isEmpty()) continue;

                    String blockId = palette.get(paletteKey);
                    if (blockId == null) {
                        plugin.getLogger().warning("Unknown palette key: " + paletteKey);
                        continue;
                    }

                    // Convert minecraft:block_id to Bukkit Material
                    Material material = blockIdToMaterial(blockId);
                    if (material == null) {
                        plugin.getLogger().warning("Unknown block ID: " + blockId);
                        continue;
                    }

                    // Validate block is allowed
                    if (!BlockValidator.isValidBlock(material)) {
                        plugin.getLogger().warning("Block not allowed: " + material.name());
                        continue;
                    }

                    // Calculate world position
                    int worldX = startLocation.getBlockX() + x;
                    int worldY = startLocation.getBlockY() + layerIndex;
                    int worldZ = startLocation.getBlockZ() + z;

                    Block block = world.getBlockAt(worldX, worldY, worldZ);

                    // Save original state for undo
                    undoEntry.addBlockState(new BlockState(block.getLocation(), block.getType()));

                    // Place the block
                    block.setType(material, false);
                    blocksPlaced++;
                }
            }
        }

        // Save undo entry
        undoBuffer.saveEntry(player, undoEntry);

        plugin.getLogger().info("Built structure with " + blocksPlaced + " blocks for " + player.getName());
    }

    /**
     * Convert a minecraft block ID to Bukkit Material
     * @param blockId The minecraft block ID (e.g., "minecraft:cobblestone")
     * @return The corresponding Bukkit Material, or null if not found
     */
    private Material blockIdToMaterial(String blockId) {
        if (blockId == null) return null;

        // Remove "minecraft:" prefix if present
        String materialName = blockId;
        if (materialName.startsWith("minecraft:")) {
            materialName = materialName.substring("minecraft:".length());
        }

        // Convert to uppercase for Bukkit Material enum
        materialName = materialName.toUpperCase();

        try {
            return Material.valueOf(materialName);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}