package com.aiextractor.detection;

import com.aiextractor.StructureExtractorPlugin;
import com.aiextractor.models.BoundingBox;
import com.aiextractor.models.ExtractedStructure;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;

import java.util.*;

/**
 * Detects and extracts structures from the world
 */
public class StructureDetector {

    private final StructureExtractorPlugin plugin;
    private final Set<Material> naturalBlocks;
    private final Set<Material> structureIndicatorBlocks;
    private final int maxFloodIterations;
    private final int searchRadius;

    public StructureDetector(StructureExtractorPlugin plugin) {
        this.plugin = plugin;
        this.naturalBlocks = loadMaterialSet("detection.natural-blocks");
        this.structureIndicatorBlocks = loadMaterialSet("detection.structure-indicator-blocks");
        this.maxFloodIterations = plugin.getConfig().getInt("detection.max-flood-iterations", 100000);
        this.searchRadius = plugin.getConfig().getInt("detection.search-radius", 64);
    }

    /**
     * Load a set of materials from config
     */
    private Set<Material> loadMaterialSet(String configPath) {
        Set<Material> materials = new HashSet<>();
        List<String> blockNames = plugin.getConfig().getStringList(configPath);
        
        for (String name : blockNames) {
            String materialName = name.replace("minecraft:", "").toUpperCase();
            try {
                materials.add(Material.valueOf(materialName));
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Unknown material in config: " + name);
            }
        }
        
        return materials;
    }

    /**
     * Check if a block is considered natural terrain
     */
    public boolean isNaturalBlock(Material material) {
        return naturalBlocks.contains(material);
    }

    /**
     * Check if a block indicates a structure
     */
    public boolean isStructureBlock(Material material) {
        return structureIndicatorBlocks.contains(material);
    }

    /**
     * Check if a block is part of a structure (not natural and not air)
     * This is used during flood fill to determine what to include
     */
    public boolean isStructurePart(Material material) {
        // Air is never a structure part
        if (material == Material.AIR || material == Material.CAVE_AIR || material == Material.VOID_AIR) {
            return false;
        }
        
        // Natural blocks are never structure parts
        if (isNaturalBlock(material)) {
            return false;
        }
        
        // Exclude natural vegetation that might not be in the natural-blocks list
        String name = material.name();
        if (name.contains("LEAVES") || name.contains("LOG") || name.contains("WOOD") ||
            name.contains("SAPLING") || name.contains("MUSHROOM") && !name.contains("BLOCK") ||
            name.contains("FLOWER") || name.contains("AZALEA") ||
            name.contains("DRIPLEAF") || name.contains("SPORE") ||
            name.contains("MOSS") && !name.contains("MOSSY") ||  // moss but not mossy_cobblestone
            name.contains("VINE") || name.contains("LICHEN") ||
            name.contains("CORAL") && !name.contains("BLOCK") ||
            name.contains("KELP") || name.contains("SEAGRASS") ||
            name.contains("SWEET_BERRY") || name.contains("SUGAR_CANE") ||
            name.contains("BAMBOO") && !name.contains("PLANKS") && !name.contains("BLOCK") ||
            name.contains("CACTUS") || name.contains("MELON") && !name.contains("STEM") ||
            name.contains("PUMPKIN") && !name.contains("CARVED") && !name.contains("JACK")) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Check if a block is a "man-made" structure block (placed by builders, not nature)
     * This is stricter than isStructurePart - used to determine if we should START detection
     */
    public boolean isManMadeBlock(Material material) {
        // Structure indicator blocks are definitely man-made
        if (isStructureBlock(material)) {
            return true;
        }
        
        String name = material.name();
        
        // Common building materials
        if (name.contains("PLANKS") || name.contains("BRICKS") || 
            name.contains("STAIRS") || name.contains("SLAB") ||
            name.contains("FENCE") || name.contains("WALL") ||
            name.contains("DOOR") || name.contains("TRAPDOOR") ||
            name.contains("GLASS") || name.contains("CARPET") ||
            name.contains("WOOL") || name.contains("CONCRETE") ||
            name.contains("TERRACOTTA") || name.contains("GLAZED") ||
            name.contains("BED") || name.contains("BANNER") ||
            name.contains("SIGN") || name.contains("BUTTON") ||
            name.contains("LEVER") || name.contains("PRESSURE_PLATE") ||
            name.contains("TORCH") || name.contains("LANTERN") ||
            name.contains("CAMPFIRE") || name.contains("CHAIN") ||
            name.contains("IRON_BARS") || name.contains("ANVIL") ||
            name.contains("CAULDRON") || name.contains("BREWING") ||
            name.contains("ENCHANTING") || name.contains("BARREL") ||
            name.contains("COMPOSTER") || name.contains("SMOKER") ||
            name.contains("BLAST_FURNACE") || name.contains("CARTOGRAPHY") ||
            name.contains("FLETCHING") || name.contains("GRINDSTONE") ||
            name.contains("LECTERN") || name.contains("LOOM") ||
            name.contains("SMITHING") || name.contains("STONECUTTER") ||
            name.contains("BELL") || name.contains("LODESTONE")) {
            return true;
        }
        
        // Processed stone variants
        if (name.contains("POLISHED") || name.contains("CUT_") || 
            name.contains("SMOOTH_") || name.contains("CHISELED") ||
            name.contains("CARVED") || name.contains("PILLAR")) {
            return true;
        }
        
        return false;
    }

    /**
     * Detect structure from a starting location using flood fill
     */
    public DetectionResult detectFromLocation(Location startLocation) {
        World world = startLocation.getWorld();
        if (world == null) {
            return new DetectionResult(false, "Invalid world");
        }

        Block startBlock = startLocation.getBlock();
        
        // If starting block is natural, search nearby for structure blocks
        if (isNaturalBlock(startBlock.getType())) {
            startBlock = findNearestStructureBlock(startLocation);
            if (startBlock == null) {
                return new DetectionResult(false, "No structure found nearby");
            }
        }

        return floodFillDetect(startBlock);
    }

    /**
     * Find the nearest structure block within search radius
     */
    private Block findNearestStructureBlock(Location center) {
        World world = center.getWorld();
        int cx = center.getBlockX();
        int cy = center.getBlockY();
        int cz = center.getBlockZ();

        // Search in expanding spheres
        for (int radius = 1; radius <= searchRadius; radius++) {
            for (int x = -radius; x <= radius; x++) {
                for (int y = -radius; y <= radius; y++) {
                    for (int z = -radius; z <= radius; z++) {
                        // Only check blocks on the surface of this radius sphere
                        if (Math.abs(x) != radius && Math.abs(y) != radius && Math.abs(z) != radius) {
                            continue;
                        }
                        
                        Block block = world.getBlockAt(cx + x, cy + y, cz + z);
                        if (isStructureBlock(block.getType())) {
                            return block;
                        }
                    }
                }
            }
        }
        
        return null;
    }

    /**
     * Flood fill to detect all connected structure blocks
     * Only expands through man-made/structure blocks, not through natural terrain
     */
    private DetectionResult floodFillDetect(Block startBlock) {
        World world = startBlock.getWorld();
        Set<Long> visited = new HashSet<>();
        Set<Long> structureBlocks = new HashSet<>();
        Queue<int[]> queue = new LinkedList<>();
        
        int startX = startBlock.getX();
        int startY = startBlock.getY();
        int startZ = startBlock.getZ();
        
        queue.add(new int[]{startX, startY, startZ});
        visited.add(packCoords(startX, startY, startZ));
        
        // Initialize bounding box
        int minX = startX, maxX = startX;
        int minY = startY, maxY = startY;
        int minZ = startZ, maxZ = startZ;
        
        int iterations = 0;
        
        while (!queue.isEmpty() && iterations < maxFloodIterations) {
            iterations++;
            int[] coords = queue.poll();
            int x = coords[0];
            int y = coords[1];
            int z = coords[2];
            
            Block block = world.getBlockAt(x, y, z);
            Material material = block.getType();
            
            // Skip natural blocks entirely - don't expand through them
            if (isNaturalBlock(material)) {
                continue;
            }
            
            // Skip air unless it's enclosed
            if (material == Material.AIR || material == Material.CAVE_AIR || material == Material.VOID_AIR) {
                // Only expand through air if it's enclosed by structure blocks
                if (!isEnclosedAir(world, x, y, z, visited)) {
                    continue;
                }
            }
            
            // If it's a structure part (man-made block), add it
            if (isStructurePart(material)) {
                structureBlocks.add(packCoords(x, y, z));
                
                // Update bounding box
                minX = Math.min(minX, x);
                maxX = Math.max(maxX, x);
                minY = Math.min(minY, y);
                maxY = Math.max(maxY, y);
                minZ = Math.min(minZ, z);
                maxZ = Math.max(maxZ, z);
            }
            
            // Determine if we should expand from this block
            // Only expand from structure parts or enclosed air
            boolean shouldExpand = isStructurePart(material) || 
                (material.isAir() && isEnclosedAir(world, x, y, z, visited));
            
            if (shouldExpand) {
                // Add neighbors to queue
                for (int[] offset : NEIGHBOR_OFFSETS) {
                    int nx = x + offset[0];
                    int ny = y + offset[1];
                    int nz = z + offset[2];
                    
                    long packed = packCoords(nx, ny, nz);
                    if (!visited.contains(packed)) {
                        visited.add(packed);
                        queue.add(new int[]{nx, ny, nz});
                    }
                }
            }
        }
        
        if (structureBlocks.isEmpty()) {
            return new DetectionResult(false, "No structure blocks found");
        }
        
        // Check minimum size
        int minSize = plugin.getConfig().getInt("scanner.min-structure-size", 20);
        if (structureBlocks.size() < minSize) {
            return new DetectionResult(false, "Structure too small (" + structureBlocks.size() + " blocks)");
        }
        
        // Check maximum size
        int maxSize = plugin.getConfig().getInt("scanner.max-structure-size", 50000);
        if (structureBlocks.size() > maxSize) {
            return new DetectionResult(false, "Structure too large (" + structureBlocks.size() + " blocks)");
        }
        
        BoundingBox boundingBox = new BoundingBox(world, minX, minY, minZ, maxX, maxY, maxZ);
        
        // Check dimension limits
        int maxDimX = plugin.getConfig().getInt("scanner.max-dimensions.x", 128);
        int maxDimY = plugin.getConfig().getInt("scanner.max-dimensions.y", 128);
        int maxDimZ = plugin.getConfig().getInt("scanner.max-dimensions.z", 128);
        
        if (boundingBox.getSizeX() > maxDimX || boundingBox.getSizeY() > maxDimY || boundingBox.getSizeZ() > maxDimZ) {
            return new DetectionResult(false, "Structure dimensions exceed limits");
        }
        
        return new DetectionResult(true, boundingBox, structureBlocks.size());
    }

    /**
     * Check if an air block is enclosed by structure blocks (interior air)
     */
    private boolean isEnclosedAir(World world, int x, int y, int z, Set<Long> visited) {
        int structureNeighbors = 0;
        
        for (int[] offset : NEIGHBOR_OFFSETS) {
            Block neighbor = world.getBlockAt(x + offset[0], y + offset[1], z + offset[2]);
            if (isStructurePart(neighbor.getType())) {
                structureNeighbors++;
            }
        }
        
        // Consider air as enclosed if it has at least 2 structure block neighbors
        return structureNeighbors >= 2;
    }

    /**
     * Extract structure data from a bounding box
     */
    public ExtractedStructure extractStructure(BoundingBox boundingBox, String name, boolean includeAir) {
        World world = boundingBox.getWorld();
        
        ExtractedStructure structure = new ExtractedStructure(
            name,
            boundingBox.getSizeX(),
            boundingBox.getSizeY(),
            boundingBox.getSizeZ()
        );
        
        for (int y = boundingBox.getMinY(); y <= boundingBox.getMaxY(); y++) {
            for (int z = boundingBox.getMinZ(); z <= boundingBox.getMaxZ(); z++) {
                for (int x = boundingBox.getMinX(); x <= boundingBox.getMaxX(); x++) {
                    Block block = world.getBlockAt(x, y, z);
                    Material material = block.getType();
                    
                    // Skip air unless requested
                    if (!includeAir && (material == Material.AIR || 
                                        material == Material.CAVE_AIR || 
                                        material == Material.VOID_AIR)) {
                        continue;
                    }
                    
                    // Calculate relative coordinates
                    int relX = x - boundingBox.getMinX();
                    int relY = y - boundingBox.getMinY();
                    int relZ = z - boundingBox.getMinZ();
                    
                    // Get complete BlockData string (includes block type and all properties)
                    // e.g., "minecraft:oak_door[facing=north,half=lower,hinge=left,open=false,powered=false]"
                    BlockData blockData = block.getBlockData();
                    String fullBlockData = blockData.getAsString();
                    
                    // Store with the full block data string for exact reconstruction
                    // blockId field contains just the material name for compatibility
                    // data field contains the COMPLETE BlockData string for precise placement
                    String blockId = "minecraft:" + material.name().toLowerCase();
                    structure.addBlock(relX, relY, relZ, blockId, fullBlockData);
                }
            }
        }
        
        // Set metadata
        ExtractedStructure.StructureMetadata metadata = structure.getMetadata();
        metadata.setSourceWorld(world.getName());
        metadata.setOriginalX(boundingBox.getMinX());
        metadata.setOriginalY(boundingBox.getMinY());
        metadata.setOriginalZ(boundingBox.getMinZ());
        
        return structure;
    }

    /**
     * Extract from a selection (manual mode)
     */
    public ExtractedStructure extractFromSelection(Location pos1, Location pos2, String name) {
        BoundingBox box = new BoundingBox(pos1, pos2);
        boolean includeAir = plugin.getConfig().getBoolean("export.include-air", false);
        return extractStructure(box, name, includeAir);
    }

    // Pack 3D coordinates into a single long for efficient set storage
    private static long packCoords(int x, int y, int z) {
        return ((long)(x & 0x3FFFFFF) << 38) | ((long)(y & 0xFFF) << 26) | (z & 0x3FFFFFF);
    }

    // 6-directional neighbor offsets
    private static final int[][] NEIGHBOR_OFFSETS = {
        {1, 0, 0}, {-1, 0, 0},
        {0, 1, 0}, {0, -1, 0},
        {0, 0, 1}, {0, 0, -1}
    };

    /**
     * Result of structure detection
     */
    public static class DetectionResult {
        private final boolean success;
        private final String message;
        private final BoundingBox boundingBox;
        private final int blockCount;

        public DetectionResult(boolean success, String message) {
            this.success = success;
            this.message = message;
            this.boundingBox = null;
            this.blockCount = 0;
        }

        public DetectionResult(boolean success, BoundingBox boundingBox, int blockCount) {
            this.success = success;
            this.message = "Structure detected";
            this.boundingBox = boundingBox;
            this.blockCount = blockCount;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }

        public BoundingBox getBoundingBox() {
            return boundingBox;
        }

        public int getBlockCount() {
            return blockCount;
        }
    }
}
