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
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.Orientable;
import org.bukkit.block.data.Rotatable;
import org.bukkit.block.data.type.Stairs;
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

        // Get player facing direction for structure orientation
        BlockFace playerFacing = getPlayerFacing(player);

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

                    // Parse block ID and properties (e.g., "minecraft:oak_stairs[facing=north,half=bottom]")
                    BlockParseResult parseResult = parseBlockId(blockId);
                    Material material = parseResult.material;
                    
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

                    // Save original state for undo (including block data)
                    undoEntry.addBlockState(new BlockState(block.getLocation(), block.getType(), block.getBlockData()));

                    // Place the block
                    block.setType(material, false);
                    
                    // Apply block orientation/properties
                    applyBlockData(block, parseResult.properties, playerFacing, x, z, layerData);
                    
                    blocksPlaced++;
                }
            }
        }

        // Save undo entry
        undoBuffer.saveEntry(player, undoEntry);

        plugin.getLogger().info("Built structure with " + blocksPlaced + " blocks for " + player.getName());
    }

    /**
     * Get the cardinal direction the player is facing
     */
    private BlockFace getPlayerFacing(Player player) {
        float yaw = player.getLocation().getYaw();
        // Normalize yaw to 0-360
        yaw = (yaw % 360 + 360) % 360;
        
        if (yaw >= 315 || yaw < 45) {
            return BlockFace.SOUTH;
        } else if (yaw >= 45 && yaw < 135) {
            return BlockFace.WEST;
        } else if (yaw >= 135 && yaw < 225) {
            return BlockFace.NORTH;
        } else {
            return BlockFace.EAST;
        }
    }

    /**
     * Apply block data properties like facing direction, orientation, etc.
     */
    private void applyBlockData(Block block, Map<String, String> properties, BlockFace playerFacing, 
                                 int x, int z, String[][] layerData) {
        BlockData blockData = block.getBlockData();
        
        // Handle directional blocks (stairs, ladders, furnaces, etc.)
        if (blockData instanceof Directional) {
            Directional directional = (Directional) blockData;
            
            if (properties.containsKey("facing")) {
                try {
                    BlockFace face = BlockFace.valueOf(properties.get("facing").toUpperCase());
                    if (directional.getFaces().contains(face)) {
                        directional.setFacing(face);
                    }
                } catch (IllegalArgumentException ignored) {}
            } else {
                // Auto-orient based on position in structure
                BlockFace autoFace = determineAutoFacing(x, z, layerData, block.getType());
                if (autoFace != null && directional.getFaces().contains(autoFace)) {
                    directional.setFacing(autoFace);
                }
            }
        }
        
        // Handle orientable blocks (logs, pillars)
        if (blockData instanceof Orientable) {
            Orientable orientable = (Orientable) blockData;
            
            if (properties.containsKey("axis")) {
                try {
                    org.bukkit.Axis axis = org.bukkit.Axis.valueOf(properties.get("axis").toUpperCase());
                    if (orientable.getAxes().contains(axis)) {
                        orientable.setAxis(axis);
                    }
                } catch (IllegalArgumentException ignored) {}
            }
        }
        
        // Handle stairs specifically
        if (blockData instanceof Stairs) {
            Stairs stairs = (Stairs) blockData;
            
            if (properties.containsKey("half")) {
                stairs.setHalf(properties.get("half").equalsIgnoreCase("top") ? 
                    Stairs.Half.TOP : Stairs.Half.BOTTOM);
            }
            
            if (properties.containsKey("shape")) {
                try {
                    Stairs.Shape shape = Stairs.Shape.valueOf(properties.get("shape").toUpperCase());
                    stairs.setShape(shape);
                } catch (IllegalArgumentException ignored) {}
            }
        }
        
        // Handle rotatable blocks (banners, skulls)
        if (blockData instanceof Rotatable) {
            Rotatable rotatable = (Rotatable) blockData;
            
            if (properties.containsKey("rotation")) {
                try {
                    int rotation = Integer.parseInt(properties.get("rotation"));
                    BlockFace face = rotationToFace(rotation);
                    rotatable.setRotation(face);
                } catch (NumberFormatException ignored) {}
            }
        }
        
        block.setBlockData(blockData, false);
    }

    /**
     * Determine automatic facing for directional blocks based on position
     */
    private BlockFace determineAutoFacing(int x, int z, String[][] layerData, Material material) {
        // For ladders, face away from adjacent solid block
        if (material.name().contains("LADDER")) {
            // Check adjacent positions for solid blocks
            int maxX = layerData[0].length - 1;
            int maxZ = layerData.length - 1;
            
            if (x == 0) return BlockFace.EAST;
            if (x == maxX) return BlockFace.WEST;
            if (z == 0) return BlockFace.SOUTH;
            if (z == maxZ) return BlockFace.NORTH;
        }
        
        // For doors at edges, face outward
        if (material.name().contains("DOOR")) {
            int maxX = layerData[0].length - 1;
            int maxZ = layerData.length - 1;
            
            if (z == 0) return BlockFace.SOUTH;
            if (z == maxZ) return BlockFace.NORTH;
            if (x == 0) return BlockFace.EAST;
            if (x == maxX) return BlockFace.WEST;
        }
        
        return null;
    }

    /**
     * Convert rotation value (0-15) to BlockFace
     */
    private BlockFace rotationToFace(int rotation) {
        switch (rotation) {
            case 0: return BlockFace.SOUTH;
            case 1: return BlockFace.SOUTH_SOUTH_WEST;
            case 2: return BlockFace.SOUTH_WEST;
            case 3: return BlockFace.WEST_SOUTH_WEST;
            case 4: return BlockFace.WEST;
            case 5: return BlockFace.WEST_NORTH_WEST;
            case 6: return BlockFace.NORTH_WEST;
            case 7: return BlockFace.NORTH_NORTH_WEST;
            case 8: return BlockFace.NORTH;
            case 9: return BlockFace.NORTH_NORTH_EAST;
            case 10: return BlockFace.NORTH_EAST;
            case 11: return BlockFace.EAST_NORTH_EAST;
            case 12: return BlockFace.EAST;
            case 13: return BlockFace.EAST_SOUTH_EAST;
            case 14: return BlockFace.SOUTH_EAST;
            case 15: return BlockFace.SOUTH_SOUTH_EAST;
            default: return BlockFace.SOUTH;
        }
    }

    /**
     * Parse a block ID that may include properties
     * e.g., "minecraft:oak_stairs[facing=north,half=bottom]" or "minecraft:oak_log[axis=y]"
     */
    private BlockParseResult parseBlockId(String blockId) {
        BlockParseResult result = new BlockParseResult();
        result.properties = new java.util.HashMap<>();
        
        if (blockId == null) {
            result.material = null;
            return result;
        }

        String materialPart = blockId;
        
        // Check for properties in brackets
        int bracketStart = blockId.indexOf('[');
        if (bracketStart != -1) {
            materialPart = blockId.substring(0, bracketStart);
            int bracketEnd = blockId.indexOf(']');
            if (bracketEnd != -1) {
                String propsStr = blockId.substring(bracketStart + 1, bracketEnd);
                for (String prop : propsStr.split(",")) {
                    String[] kv = prop.split("=");
                    if (kv.length == 2) {
                        result.properties.put(kv[0].trim().toLowerCase(), kv[1].trim().toLowerCase());
                    }
                }
            }
        }

        // Remove "minecraft:" prefix if present
        if (materialPart.startsWith("minecraft:")) {
            materialPart = materialPart.substring("minecraft:".length());
        }

        // Convert to uppercase for Bukkit Material enum
        materialPart = materialPart.toUpperCase();

        try {
            result.material = Material.valueOf(materialPart);
        } catch (IllegalArgumentException e) {
            result.material = null;
        }
        
        return result;
    }

    private static class BlockParseResult {
        Material material;
        Map<String, String> properties;
    }
}