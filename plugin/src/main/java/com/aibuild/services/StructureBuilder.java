package com.aibuild.services;

import com.aibuild.AIBuildPlugin;
import com.aibuild.models.Structure;
import com.aibuild.models.UndoBuffer;
import com.aibuild.models.UndoBuffer.BlockState;
import com.aibuild.models.UndoBuffer.UndoEntry;
import com.aibuild.utils.BlockValidator;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.FaceAttachable;
import org.bukkit.block.data.Orientable;
import org.bukkit.block.data.Powerable;
import org.bukkit.block.data.Rotatable;
import org.bukkit.block.data.type.*;
import org.bukkit.entity.Player;

import java.util.*;

public class StructureBuilder {

    private final AIBuildPlugin plugin;
    private final UndoBuffer undoBuffer;
    
    // Blocks that need support - place in second pass
    private static final Set<String> PHYSICS_DEPENDENT = new HashSet<>(Arrays.asList(
        "TORCH", "WALL_TORCH", "SOUL_TORCH", "SOUL_WALL_TORCH", "REDSTONE_TORCH", "REDSTONE_WALL_TORCH",
        "LANTERN", "SOUL_LANTERN", "CHAIN",
        "LEVER", "TRIPWIRE_HOOK", "TRIPWIRE",
        "STONE_BUTTON", "OAK_BUTTON", "SPRUCE_BUTTON", "BIRCH_BUTTON", "JUNGLE_BUTTON",
        "ACACIA_BUTTON", "DARK_OAK_BUTTON", "MANGROVE_BUTTON", "CHERRY_BUTTON", "BAMBOO_BUTTON",
        "CRIMSON_BUTTON", "WARPED_BUTTON", "POLISHED_BLACKSTONE_BUTTON",
        "STONE_PRESSURE_PLATE", "OAK_PRESSURE_PLATE", "SPRUCE_PRESSURE_PLATE", "BIRCH_PRESSURE_PLATE",
        "JUNGLE_PRESSURE_PLATE", "ACACIA_PRESSURE_PLATE", "DARK_OAK_PRESSURE_PLATE",
        "LIGHT_WEIGHTED_PRESSURE_PLATE", "HEAVY_WEIGHTED_PRESSURE_PLATE",
        "RAIL", "POWERED_RAIL", "DETECTOR_RAIL", "ACTIVATOR_RAIL",
        "REDSTONE_WIRE", "REPEATER", "COMPARATOR",
        "LADDER", "VINE", "GLOW_LICHEN",
        "PAINTING", "ITEM_FRAME", "GLOW_ITEM_FRAME",
        "BELL", "SCAFFOLDING"
    ));

    public StructureBuilder(AIBuildPlugin plugin, UndoBuffer undoBuffer) {
        this.plugin = plugin;
        this.undoBuffer = undoBuffer;
    }

    /**
     * Build a structure at the given location with proper block ordering
     * Uses two-pass placement: solid blocks first, then physics-dependent blocks
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

        // Collect all blocks to place, organized by placement priority
        List<BlockPlacement> solidBlocks = new ArrayList<>();
        List<BlockPlacement> physicsBlocks = new ArrayList<>();
        List<BlockPlacement> upperBlocks = new ArrayList<>();  // Upper halves of doors, etc.

        // Iterate through each layer (Y level) - process from bottom to top
        List<Integer> sortedLayers = new ArrayList<>(layers.keySet());
        Collections.sort(sortedLayers);
        
        for (int layerIndex : sortedLayers) {
            String[][] layerData = layers.get(layerIndex);
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

                    // Parse block ID and properties
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

                    BlockPlacement placement = new BlockPlacement(
                        worldX, worldY, worldZ, x, z, layerIndex,
                        material, parseResult.properties, parseResult.fullBlockData, layerData
                    );
                    
                    // Categorize by placement priority
                    if (isUpperBlockPart(parseResult.properties)) {
                        upperBlocks.add(placement);
                    } else if (isPhysicsDependent(material)) {
                        physicsBlocks.add(placement);
                    } else {
                        solidBlocks.add(placement);
                    }
                }
            }
        }

        int blocksPlaced = 0;

        // Pass 1: Place solid blocks (bottom to top already sorted by layer)
        for (BlockPlacement p : solidBlocks) {
            blocksPlaced += placeBlock(world, p, undoEntry, playerFacing);
        }
        
        // Pass 2: Place upper parts of multi-block structures (doors top half, etc.)
        for (BlockPlacement p : upperBlocks) {
            blocksPlaced += placeBlock(world, p, undoEntry, playerFacing);
        }
        
        // Pass 3: Place physics-dependent blocks last
        for (BlockPlacement p : physicsBlocks) {
            blocksPlaced += placeBlock(world, p, undoEntry, playerFacing);
        }

        // Save undo entry
        undoBuffer.saveEntry(player, undoEntry);

        plugin.getLogger().info("Built structure with " + blocksPlaced + " blocks for " + player.getName());
    }
    
    /**
     * Place a single block with proper BlockData
     */
    private int placeBlock(World world, BlockPlacement p, UndoEntry undoEntry, BlockFace playerFacing) {
        Block block = world.getBlockAt(p.worldX, p.worldY, p.worldZ);
        
        // Save original state for undo
        undoEntry.addBlockState(new BlockState(block.getLocation(), block.getType(), block.getBlockData()));
        
        try {
            // If we have a complete BlockData string, use it directly
            if (p.fullBlockData != null && !p.fullBlockData.isEmpty()) {
                try {
                    BlockData blockData = Bukkit.createBlockData(p.fullBlockData);
                    block.setBlockData(blockData, false);
                    return 1;
                } catch (IllegalArgumentException e) {
                    // Fall through to property-based placement
                }
            }
            
            // Place the block type first
            block.setType(p.material, false);
            
            // Apply block orientation/properties
            applyBlockData(block, p.properties, playerFacing, p.relX, p.relZ, p.layerData);
            
            return 1;
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to place block at " + p.worldX + "," + p.worldY + "," + p.worldZ + ": " + e.getMessage());
            return 0;
        }
    }
    
    /**
     * Check if material is physics-dependent
     */
    private boolean isPhysicsDependent(Material material) {
        return PHYSICS_DEPENDENT.contains(material.name());
    }
    
    /**
     * Check if properties indicate upper part of multi-block structure
     */
    private boolean isUpperBlockPart(Map<String, String> properties) {
        if (properties == null) return false;
        String half = properties.get("half");
        if (half != null && (half.equalsIgnoreCase("upper") || half.equalsIgnoreCase("top"))) {
            return true;
        }
        String part = properties.get("part");
        if (part != null && part.equalsIgnoreCase("head")) {
            return true;
        }
        return false;
    }
    
    /**
     * Temporary storage for block placement info
     */
    private static class BlockPlacement {
        final int worldX, worldY, worldZ;
        final int relX, relZ, layer;
        final Material material;
        final Map<String, String> properties;
        final String fullBlockData;
        final String[][] layerData;
        
        BlockPlacement(int worldX, int worldY, int worldZ, int relX, int relZ, int layer,
                      Material material, Map<String, String> properties, String fullBlockData, String[][] layerData) {
            this.worldX = worldX;
            this.worldY = worldY;
            this.worldZ = worldZ;
            this.relX = relX;
            this.relZ = relZ;
            this.layer = layer;
            this.material = material;
            this.properties = properties;
            this.fullBlockData = fullBlockData;
            this.layerData = layerData;
        }
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
     * Handles all major block types: stairs, doors, beds, slabs, trapdoors, etc.
     */
    private void applyBlockData(Block block, Map<String, String> properties, BlockFace playerFacing, 
                                 int x, int z, String[][] layerData) {
        if (properties == null || properties.isEmpty()) {
            return;
        }
        
        BlockData blockData = block.getBlockData();
        
        // Handle directional blocks (stairs, ladders, furnaces, doors, etc.)
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
        
        // Handle stairs
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
            
            if (properties.containsKey("waterlogged")) {
                stairs.setWaterlogged(properties.get("waterlogged").equalsIgnoreCase("true"));
            }
        }
        
        // Handle doors
        if (blockData instanceof Door) {
            Door door = (Door) blockData;
            
            if (properties.containsKey("half")) {
                door.setHalf(properties.get("half").equalsIgnoreCase("upper") ? 
                    Bisected.Half.TOP : Bisected.Half.BOTTOM);
            }
            
            if (properties.containsKey("hinge")) {
                door.setHinge(properties.get("hinge").equalsIgnoreCase("right") ? 
                    Door.Hinge.RIGHT : Door.Hinge.LEFT);
            }
            
            if (properties.containsKey("open")) {
                door.setOpen(properties.get("open").equalsIgnoreCase("true"));
            }
            
            if (properties.containsKey("powered")) {
                door.setPowered(properties.get("powered").equalsIgnoreCase("true"));
            }
        }
        
        // Handle trapdoors
        if (blockData instanceof TrapDoor) {
            TrapDoor trapDoor = (TrapDoor) blockData;
            
            if (properties.containsKey("half")) {
                trapDoor.setHalf(properties.get("half").equalsIgnoreCase("top") ? 
                    Bisected.Half.TOP : Bisected.Half.BOTTOM);
            }
            
            if (properties.containsKey("open")) {
                trapDoor.setOpen(properties.get("open").equalsIgnoreCase("true"));
            }
            
            if (properties.containsKey("powered")) {
                trapDoor.setPowered(properties.get("powered").equalsIgnoreCase("true"));
            }
            
            if (properties.containsKey("waterlogged")) {
                trapDoor.setWaterlogged(properties.get("waterlogged").equalsIgnoreCase("true"));
            }
        }
        
        // Handle slabs
        if (blockData instanceof Slab) {
            Slab slab = (Slab) blockData;
            
            if (properties.containsKey("type")) {
                try {
                    Slab.Type type = Slab.Type.valueOf(properties.get("type").toUpperCase());
                    slab.setType(type);
                } catch (IllegalArgumentException ignored) {}
            }
            
            if (properties.containsKey("waterlogged")) {
                slab.setWaterlogged(properties.get("waterlogged").equalsIgnoreCase("true"));
            }
        }
        
        // Handle beds
        if (blockData instanceof Bed) {
            Bed bed = (Bed) blockData;
            
            if (properties.containsKey("part")) {
                bed.setPart(properties.get("part").equalsIgnoreCase("head") ? 
                    Bed.Part.HEAD : Bed.Part.FOOT);
            }
            
            if (properties.containsKey("occupied")) {
                bed.setOccupied(properties.get("occupied").equalsIgnoreCase("true"));
            }
        }
        
        // Handle fence gates
        if (blockData instanceof Gate) {
            Gate gate = (Gate) blockData;
            
            if (properties.containsKey("open")) {
                gate.setOpen(properties.get("open").equalsIgnoreCase("true"));
            }
            
            if (properties.containsKey("in_wall")) {
                gate.setInWall(properties.get("in_wall").equalsIgnoreCase("true"));
            }
        }
        
        // Handle lanterns and chains
        if (blockData instanceof Lantern) {
            Lantern lantern = (Lantern) blockData;
            
            if (properties.containsKey("hanging")) {
                lantern.setHanging(properties.get("hanging").equalsIgnoreCase("true"));
            }
            
            if (properties.containsKey("waterlogged")) {
                lantern.setWaterlogged(properties.get("waterlogged").equalsIgnoreCase("true"));
            }
        }
        
        // Handle chains
        if (blockData instanceof Chain) {
            Chain chain = (Chain) blockData;
            
            if (properties.containsKey("axis")) {
                try {
                    org.bukkit.Axis axis = org.bukkit.Axis.valueOf(properties.get("axis").toUpperCase());
                    chain.setAxis(axis);
                } catch (IllegalArgumentException ignored) {}
            }
            
            if (properties.containsKey("waterlogged")) {
                chain.setWaterlogged(properties.get("waterlogged").equalsIgnoreCase("true"));
            }
        }
        
        // Handle rotatable blocks (banners, skulls, signs)
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
        
        // Handle wall-mounted signs
        if (blockData instanceof WallSign) {
            WallSign sign = (WallSign) blockData;
            
            if (properties.containsKey("waterlogged")) {
                sign.setWaterlogged(properties.get("waterlogged").equalsIgnoreCase("true"));
            }
        }
        
        // Handle levers and buttons (FaceAttachable)
        if (blockData instanceof FaceAttachable) {
            FaceAttachable attachable = (FaceAttachable) blockData;
            
            if (properties.containsKey("face")) {
                try {
                    FaceAttachable.AttachedFace face = FaceAttachable.AttachedFace.valueOf(properties.get("face").toUpperCase());
                    attachable.setAttachedFace(face);
                } catch (IllegalArgumentException ignored) {}
            }
        }
        
        if (blockData instanceof Powerable) {
            Powerable powerable = (Powerable) blockData;
            
            if (properties.containsKey("powered")) {
                powerable.setPowered(properties.get("powered").equalsIgnoreCase("true"));
            }
        }
        
        // Handle chests (for double chests)
        if (blockData instanceof Chest) {
            Chest chest = (Chest) blockData;
            
            if (properties.containsKey("type")) {
                try {
                    Chest.Type type = Chest.Type.valueOf(properties.get("type").toUpperCase());
                    chest.setType(type);
                } catch (IllegalArgumentException ignored) {}
            }
            
            if (properties.containsKey("waterlogged")) {
                chest.setWaterlogged(properties.get("waterlogged").equalsIgnoreCase("true"));
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
        
        // Store the full block data string for direct BlockData creation
        result.fullBlockData = blockId;

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
        String fullBlockData;  // Complete block data string for direct BlockData creation
    }
}