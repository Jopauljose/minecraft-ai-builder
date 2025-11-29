package com.aiextractor.builder;

import com.aiextractor.StructureExtractorPlugin;
import com.aiextractor.models.ExtractedStructure;
import com.aiextractor.models.ExtractedStructure.BlockEntry;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

/**
 * Builds structures from exported JSON files with precise block placement
 * Handles multi-part blocks (doors, beds) and physics-dependent blocks correctly
 */
public class StructureBuilder {

    private final StructureExtractorPlugin plugin;
    private final Gson gson;
    
    // Undo buffer - stores original blocks before building
    private final Map<UUID, List<BlockSnapshot>> undoBuffer = new HashMap<>();
    
    // Blocks that need a supporting block below/beside them
    private static final Set<String> PHYSICS_DEPENDENT_BLOCKS = new HashSet<>(Arrays.asList(
        "torch", "wall_torch", "soul_torch", "soul_wall_torch", "redstone_torch", "redstone_wall_torch",
        "lantern", "soul_lantern", "chain",
        "lever", "tripwire_hook", "tripwire",
        "button", "pressure_plate", "weighted_pressure_plate",
        "carpet", "moss_carpet",
        "rail", "powered_rail", "detector_rail", "activator_rail",
        "redstone_wire", "repeater", "comparator",
        "flower", "poppy", "dandelion", "blue_orchid", "allium", "azure_bluet",
        "tulip", "oxeye_daisy", "cornflower", "lily_of_the_valley", "wither_rose",
        "sunflower", "lilac", "rose_bush", "peony", "tall_grass", "large_fern",
        "grass", "fern", "dead_bush", "seagrass", "tall_seagrass",
        "wheat", "carrots", "potatoes", "beetroots", "melon_stem", "pumpkin_stem",
        "sweet_berry_bush", "nether_wart", "cocoa",
        "sign", "wall_sign", "hanging_sign", "wall_hanging_sign",
        "banner", "wall_banner",
        "head", "skull", "wall_head", "wall_skull",
        "painting", "item_frame", "glow_item_frame",
        "bell", "scaffolding",
        "candle", "candle_cake",
        "pointed_dripstone", "spore_blossom", "glow_lichen",
        "vine", "cave_vines", "weeping_vines", "twisting_vines",
        "snow"
    ));
    
    // Upper parts of multi-block structures (place after lower parts)
    private static final Set<String> UPPER_BLOCK_PARTS = new HashSet<>(Arrays.asList(
        "door", "tall_grass", "large_fern", "sunflower", "lilac", "rose_bush", "peony",
        "tall_seagrass", "small_dripleaf", "big_dripleaf_stem"
    ));

    public StructureBuilder(StructureExtractorPlugin plugin) {
        this.plugin = plugin;
        this.gson = new GsonBuilder().create();
    }

    /**
     * Load a structure from a JSON file
     */
    public ExtractedStructure loadStructure(File jsonFile) throws IOException {
        try (FileReader reader = new FileReader(jsonFile)) {
            return gson.fromJson(reader, ExtractedStructure.class);
        }
    }

    /**
     * Build a structure at the given location with proper block ordering
     * @return BuildResult with success status and block count
     */
    public BuildResult buildStructure(Player player, Location origin, ExtractedStructure structure) {
        World world = origin.getWorld();
        if (world == null) {
            return new BuildResult(false, 0, "Invalid world");
        }

        List<BlockEntry> blocks = structure.getBlocks();
        if (blocks == null || blocks.isEmpty()) {
            return new BuildResult(false, 0, "Structure has no blocks");
        }

        // Store original blocks for undo - collect all positions first
        List<BlockSnapshot> snapshots = new ArrayList<>();
        for (BlockEntry entry : blocks) {
            int targetX = origin.getBlockX() + entry.getX();
            int targetY = origin.getBlockY() + entry.getY();
            int targetZ = origin.getBlockZ() + entry.getZ();
            Block block = world.getBlockAt(targetX, targetY, targetZ);
            snapshots.add(new BlockSnapshot(block.getLocation(), block.getBlockData()));
        }
        
        // Sort blocks for proper placement order:
        // 1. Sort by Y-level (bottom to top)
        // 2. Non-physics-dependent blocks first
        // 3. Upper parts of multi-block structures last
        List<BlockEntry> sortedBlocks = new ArrayList<>(blocks);
        sortedBlocks.sort((a, b) -> {
            // Primary sort: Y level (bottom to top)
            int yCompare = Integer.compare(a.getY(), b.getY());
            if (yCompare != 0) return yCompare;
            
            // Secondary: physics-dependent blocks last
            boolean aPhysics = isPhysicsDependent(a);
            boolean bPhysics = isPhysicsDependent(b);
            if (aPhysics != bPhysics) return aPhysics ? 1 : -1;
            
            // Tertiary: upper block parts last
            boolean aUpper = isUpperBlockPart(a);
            boolean bUpper = isUpperBlockPart(b);
            if (aUpper != bUpper) return aUpper ? 1 : -1;
            
            return 0;
        });
        
        int successCount = 0;
        int failCount = 0;
        List<String> errors = new ArrayList<>();

        // First pass: place all non-air blocks
        for (BlockEntry entry : sortedBlocks) {
            int targetX = origin.getBlockX() + entry.getX();
            int targetY = origin.getBlockY() + entry.getY();
            int targetZ = origin.getBlockZ() + entry.getZ();

            Block block = world.getBlockAt(targetX, targetY, targetZ);

            try {
                // Use the complete BlockData string if available (new format)
                if (entry.getData() != null && !entry.getData().isEmpty()) {
                    // The data field now contains the complete BlockData string
                    // e.g., "minecraft:oak_door[facing=north,half=lower,hinge=left,open=false,powered=false]"
                    try {
                        BlockData blockData = Bukkit.createBlockData(entry.getData());
                        block.setBlockData(blockData, false);
                        successCount++;
                        continue;
                    } catch (IllegalArgumentException e) {
                        // Fall back to legacy handling if the format is different
                        plugin.getLogger().fine("Could not parse BlockData directly: " + entry.getData());
                    }
                }
                
                // Legacy fallback: parse material from block ID
                String blockId = entry.getBlock();
                if (blockId.startsWith("minecraft:")) {
                    blockId = blockId.substring(10);
                }
                
                Material material = Material.matchMaterial(blockId);
                if (material == null) {
                    material = Material.matchMaterial("minecraft:" + blockId);
                }
                
                if (material == null) {
                    failCount++;
                    if (errors.size() < 5) {
                        errors.add("Unknown material: " + entry.getBlock());
                    }
                    continue;
                }

                // Set the block type
                block.setType(material, false);
                
                // Try to apply legacy block data format (just properties in brackets)
                if (entry.getData() != null && !entry.getData().isEmpty() && entry.getData().startsWith("[")) {
                    try {
                        String fullBlockData = material.getKey().toString() + entry.getData();
                        BlockData data = Bukkit.createBlockData(fullBlockData);
                        block.setBlockData(data, false);
                    } catch (Exception e) {
                        plugin.getLogger().fine("Could not apply legacy block data: " + entry.getData());
                    }
                }
                
                successCount++;
                
            } catch (Exception e) {
                failCount++;
                if (errors.size() < 5) {
                    errors.add("Error at " + targetX + "," + targetY + "," + targetZ + ": " + e.getMessage());
                }
            }
        }

        // Store undo data
        undoBuffer.put(player.getUniqueId(), snapshots);

        String errorMsg = errors.isEmpty() ? null : String.join("; ", errors);
        if (failCount > 5) {
            errorMsg = (errorMsg != null ? errorMsg + "; " : "") + "... and " + (failCount - 5) + " more errors";
        }
        
        return new BuildResult(failCount == 0, successCount, errorMsg);
    }
    
    /**
     * Check if a block is physics-dependent (needs support)
     */
    private boolean isPhysicsDependent(BlockEntry entry) {
        String blockName = entry.getBlock().toLowerCase();
        if (blockName.startsWith("minecraft:")) {
            blockName = blockName.substring(10);
        }
        
        for (String pattern : PHYSICS_DEPENDENT_BLOCKS) {
            if (blockName.contains(pattern)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Check if a block is the upper part of a multi-block structure
     */
    private boolean isUpperBlockPart(BlockEntry entry) {
        String blockName = entry.getBlock().toLowerCase();
        String data = entry.getData();
        
        // Check for upper half indicators in block data
        if (data != null) {
            String dataLower = data.toLowerCase();
            if (dataLower.contains("half=upper") || dataLower.contains("half=top")) {
                return true;
            }
            // Beds: part=head is the upper/second part
            if (dataLower.contains("part=head")) {
                return true;
            }
        }
        
        return false;
    }

    /**
     * Undo the last build for a player
     */
    public int undoLastBuild(Player player) {
        List<BlockSnapshot> snapshots = undoBuffer.remove(player.getUniqueId());
        if (snapshots == null || snapshots.isEmpty()) {
            return 0;
        }

        int restored = 0;
        for (BlockSnapshot snapshot : snapshots) {
            Block block = snapshot.location.getBlock();
            block.setBlockData(snapshot.blockData, false);
            restored++;
        }

        return restored;
    }

    /**
     * Check if player has undo data
     */
    public boolean hasUndo(Player player) {
        return undoBuffer.containsKey(player.getUniqueId());
    }

    /**
     * List all available structure files
     */
    public List<File> listStructureFiles() {
        List<File> files = new ArrayList<>();
        File exportDir = plugin.getExportDirectory();
        
        if (exportDir.exists()) {
            // Search in world subdirectories
            File[] worldDirs = exportDir.listFiles(File::isDirectory);
            if (worldDirs != null) {
                for (File worldDir : worldDirs) {
                    File[] jsonFiles = worldDir.listFiles((dir, name) -> name.endsWith(".json"));
                    if (jsonFiles != null) {
                        files.addAll(Arrays.asList(jsonFiles));
                    }
                }
            }
            
            // Also check root export directory
            File[] rootFiles = exportDir.listFiles((dir, name) -> name.endsWith(".json"));
            if (rootFiles != null) {
                files.addAll(Arrays.asList(rootFiles));
            }
        }
        
        return files;
    }

    /**
     * Find a structure file by name
     */
    public File findStructureFile(String name) {
        if (!name.endsWith(".json")) {
            name = name + ".json";
        }
        
        for (File file : listStructureFiles()) {
            if (file.getName().equalsIgnoreCase(name)) {
                return file;
            }
        }
        
        return null;
    }

    /**
     * Snapshot of a block's original state
     */
    private static class BlockSnapshot {
        final Location location;
        final BlockData blockData;

        BlockSnapshot(Location location, BlockData blockData) {
            this.location = location;
            this.blockData = blockData;
        }
    }

    /**
     * Result of a build operation
     */
    public static class BuildResult {
        private final boolean success;
        private final int blocksPlaced;
        private final String error;

        public BuildResult(boolean success, int blocksPlaced, String error) {
            this.success = success;
            this.blocksPlaced = blocksPlaced;
            this.error = error;
        }

        public boolean isSuccess() {
            return success;
        }

        public int getBlocksPlaced() {
            return blocksPlaced;
        }

        public String getError() {
            return error;
        }
    }
}
