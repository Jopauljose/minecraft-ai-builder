package com.aiextractor.scanner;

import com.aiextractor.StructureExtractorPlugin;
import com.aiextractor.detection.StructureDetector;
import com.aiextractor.detection.StructureDetector.DetectionResult;
import com.aiextractor.export.StructureExporter;
import com.aiextractor.models.BoundingBox;
import com.aiextractor.models.ExtractedStructure;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Scans worlds for structures automatically
 */
public class WorldScanner {

    private final StructureExtractorPlugin plugin;
    private final StructureDetector detector;
    private final StructureExporter exporter;
    
    // Minimum distance between structure centers to be considered different structures
    private static final int MIN_STRUCTURE_DISTANCE = 5;
    // Overlap threshold - if more than this % of blocks overlap, skip the structure
    private static final double OVERLAP_THRESHOLD = 0.3;
    
    private BukkitTask scanTask;
    private final AtomicBoolean scanning = new AtomicBoolean(false);
    private final AtomicInteger structuresFound = new AtomicInteger(0);
    private final AtomicInteger chunksScanned = new AtomicInteger(0);
    
    private World currentWorld;
    private Queue<long[]> chunkQueue;
    private Set<Long> processedChunks;
    private Set<Long> processedBlocks;  // Track ALL processed block positions
    private List<int[]> structureCenters;  // Track centers of captured structures
    
    private int chunksPerTick;

    public WorldScanner(StructureExtractorPlugin plugin) {
        this.plugin = plugin;
        this.detector = plugin.getStructureDetector();
        this.exporter = plugin.getStructureExporter();
        this.chunksPerTick = plugin.getConfig().getInt("scanner.chunks-per-tick", 2);
    }

    /**
     * Start scanning a world from spawn location
     */
    public boolean startScan(World world) {
        return startScan(world, null);
    }
    
    /**
     * Start scanning a world from a specific location (or spawn if null)
     */
    public boolean startScan(World world, Location startLocation) {
        if (scanning.get()) {
            return false;
        }
        
        scanning.set(true);
        structuresFound.set(0);
        chunksScanned.set(0);
        
        currentWorld = world;
        chunkQueue = new LinkedList<>();
        processedChunks = new HashSet<>();
        processedBlocks = new HashSet<>();
        structureCenters = new ArrayList<>();
        
        // Start from provided location or spawn
        Location center = startLocation != null ? startLocation : world.getSpawnLocation();
        int startChunkX = center.getBlockX() >> 4;
        int startChunkZ = center.getBlockZ() >> 4;
        
        // Add a larger initial area around center (configurable radius)
        int scanRadius = plugin.getConfig().getInt("scanner.initial-radius", 8);
        for (int dx = -scanRadius; dx <= scanRadius; dx++) {
            for (int dz = -scanRadius; dz <= scanRadius; dz++) {
                addChunkToQueue(startChunkX + dx, startChunkZ + dz);
            }
        }
        
        plugin.getLogger().info("Queued " + chunkQueue.size() + " chunks for scanning around " + 
            center.getBlockX() + ", " + center.getBlockZ());
        
        // Start the scan task
        scanTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!scanning.get() || chunkQueue.isEmpty()) {
                    stopScan();
                    return;
                }
                
                // Process chunks this tick
                for (int i = 0; i < chunksPerTick && !chunkQueue.isEmpty(); i++) {
                    long[] coords = chunkQueue.poll();
                    if (coords != null) {
                        processChunk((int) coords[0], (int) coords[1]);
                    }
                }
            }
        }.runTaskTimer(plugin, 20L, 1L); // Start after 1 second, run every tick
        
        plugin.getLogger().info("Started scanning world: " + world.getName());
        return true;
    }

    /**
     * Add a chunk to the scan queue
     */
    private void addChunkToQueue(int chunkX, int chunkZ) {
        long packed = packChunkCoords(chunkX, chunkZ);
        if (!processedChunks.contains(packed)) {
            processedChunks.add(packed);
            chunkQueue.add(new long[]{chunkX, chunkZ});
        }
    }

    /**
     * Process a single chunk
     */
    private void processChunk(int chunkX, int chunkZ) {
        // Load the chunk if not already loaded (required to scan it)
        boolean wasLoaded = currentWorld.isChunkLoaded(chunkX, chunkZ);
        
        if (!wasLoaded) {
            // Check if chunk exists on disk - if not, skip it
            if (!currentWorld.isChunkGenerated(chunkX, chunkZ)) {
                return;
            }
            // Load the chunk temporarily
            currentWorld.loadChunk(chunkX, chunkZ, false);
        }
        
        chunksScanned.incrementAndGet();
        Chunk chunk = currentWorld.getChunkAt(chunkX, chunkZ);
        
        // Scan for structure indicator blocks
        int baseX = chunkX << 4;
        int baseZ = chunkZ << 4;
        int blocksChecked = 0;
        int indicatorsFound = 0;
        
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = currentWorld.getMinHeight(); y < currentWorld.getMaxHeight(); y++) {
                    Block block = chunk.getBlock(x, y, z);
                    blocksChecked++;
                    
                    if (detector.isStructureBlock(block.getType())) {
                        indicatorsFound++;
                        // Check if this block has already been processed as part of another structure
                        long locPacked = packBlockCoords(baseX + x, y, baseZ + z);
                        if (!processedBlocks.contains(locPacked)) {
                            tryExtractStructure(block);
                        }
                    }
                }
            }
        }
        
        // Log progress periodically
        if (chunksScanned.get() % 50 == 0) {
            plugin.getLogger().info("Scan progress: " + chunksScanned.get() + " chunks scanned, " + 
                structuresFound.get() + " structures found, " + chunkQueue.size() + " chunks remaining");
        }
        
        // Unload chunk if we loaded it
        if (!wasLoaded && plugin.getConfig().getBoolean("scanner.unload-chunks", true)) {
            currentWorld.unloadChunkRequest(chunkX, chunkZ);
        }
        
        // Add neighboring chunks to queue (spiral outward)
        addChunkToQueue(chunkX + 1, chunkZ);
        addChunkToQueue(chunkX - 1, chunkZ);
        addChunkToQueue(chunkX, chunkZ + 1);
        addChunkToQueue(chunkX, chunkZ - 1);
    }

    /**
     * Try to extract a structure starting from a block
     */
    private void tryExtractStructure(Block startBlock) {
        DetectionResult result = detector.detectFromLocation(startBlock.getLocation());
        
        if (!result.isSuccess()) {
            return;
        }
        
        BoundingBox box = result.getBoundingBox();
        
        // Calculate structure center
        int centerX = (box.getMinX() + box.getMaxX()) / 2;
        int centerY = (box.getMinY() + box.getMaxY()) / 2;
        int centerZ = (box.getMinZ() + box.getMaxZ()) / 2;
        
        // Check if this structure's center is too close to an existing one
        for (int[] existingCenter : structureCenters) {
            int dx = Math.abs(centerX - existingCenter[0]);
            int dy = Math.abs(centerY - existingCenter[1]);
            int dz = Math.abs(centerZ - existingCenter[2]);
            double distance = Math.sqrt(dx*dx + dy*dy + dz*dz);
            
            if (distance < MIN_STRUCTURE_DISTANCE) {
                // Too close to an existing structure, skip
                return;
            }
        }
        
        // Check overlap with already processed blocks
        int overlapCount = 0;
        int totalBlocks = 0;
        
        for (int x = box.getMinX(); x <= box.getMaxX(); x++) {
            for (int y = box.getMinY(); y <= box.getMaxY(); y++) {
                for (int z = box.getMinZ(); z <= box.getMaxZ(); z++) {
                    Block block = currentWorld.getBlockAt(x, y, z);
                    if (!block.getType().isAir()) {
                        totalBlocks++;
                        if (processedBlocks.contains(packBlockCoords(x, y, z))) {
                            overlapCount++;
                        }
                    }
                }
            }
        }
        
        // If too much overlap, this is likely a duplicate
        if (totalBlocks > 0 && (double) overlapCount / totalBlocks > OVERLAP_THRESHOLD) {
            plugin.getLogger().fine("Skipping duplicate structure at " + centerX + "," + centerY + "," + centerZ + 
                " (" + Math.round((double) overlapCount / totalBlocks * 100) + "% overlap)");
            return;
        }
        
        // Mark all blocks in this bounding box as processed
        markBoundingBoxProcessed(box);
        
        // Record this structure's center
        structureCenters.add(new int[]{centerX, centerY, centerZ});
        
        // Extract and export
        boolean includeAir = plugin.getConfig().getBoolean("export.include-air", false);
        ExtractedStructure structure = detector.extractStructure(box, null, includeAir);
        structure.getMetadata().setCaptureMode("auto-scan");
        
        exporter.exportAsync(structure).thenAccept(exportResult -> {
            if (exportResult.isSuccess()) {
                structuresFound.incrementAndGet();
                plugin.getLogger().info("Auto-exported structure: " + 
                    exportResult.getOutputFile().getName() + 
                    " (" + exportResult.getBlockCount() + " blocks)");
            }
        });
    }

    /**
     * Mark all blocks in a bounding box as processed
     */
    private void markBoundingBoxProcessed(BoundingBox box) {
        // Mark ALL non-air blocks in the bounding box as processed
        for (int x = box.getMinX(); x <= box.getMaxX(); x++) {
            for (int y = box.getMinY(); y <= box.getMaxY(); y++) {
                for (int z = box.getMinZ(); z <= box.getMaxZ(); z++) {
                    Block block = currentWorld.getBlockAt(x, y, z);
                    if (!block.getType().isAir()) {
                        processedBlocks.add(packBlockCoords(x, y, z));
                    }
                }
            }
        }
    }

    /**
     * Stop the current scan
     */
    public void stopScan() {
        if (scanTask != null) {
            scanTask.cancel();
            scanTask = null;
        }
        
        scanning.set(false);
        
        if (currentWorld != null) {
            plugin.getLogger().info("Stopped scanning world: " + currentWorld.getName());
            plugin.getLogger().info("Chunks scanned: " + chunksScanned.get() + 
                                   ", Structures found: " + structuresFound.get());
        }
        
        currentWorld = null;
        chunkQueue = null;
        processedChunks = null;
        processedBlocks = null;
        structureCenters = null;
    }

    /**
     * Check if currently scanning
     */
    public boolean isScanning() {
        return scanning.get();
    }

    /**
     * Get scan statistics
     */
    public ScanStats getStats() {
        return new ScanStats(
            chunksScanned.get(),
            structuresFound.get(),
            chunkQueue != null ? chunkQueue.size() : 0,
            currentWorld != null ? currentWorld.getName() : null
        );
    }

    // Coordinate packing utilities
    private static long packChunkCoords(int x, int z) {
        return ((long) x << 32) | (z & 0xFFFFFFFFL);
    }

    private static long packBlockCoords(int x, int y, int z) {
        return ((long)(x & 0x3FFFFFF) << 38) | ((long)(y & 0xFFF) << 26) | (z & 0x3FFFFFF);
    }

    /**
     * Scan statistics
     */
    public static class ScanStats {
        private final int chunksScanned;
        private final int structuresFound;
        private final int chunksRemaining;
        private final String worldName;

        public ScanStats(int chunksScanned, int structuresFound, int chunksRemaining, String worldName) {
            this.chunksScanned = chunksScanned;
            this.structuresFound = structuresFound;
            this.chunksRemaining = chunksRemaining;
            this.worldName = worldName;
        }

        public int getChunksScanned() {
            return chunksScanned;
        }

        public int getStructuresFound() {
            return structuresFound;
        }

        public int getChunksRemaining() {
            return chunksRemaining;
        }

        public String getWorldName() {
            return worldName;
        }
    }
}
