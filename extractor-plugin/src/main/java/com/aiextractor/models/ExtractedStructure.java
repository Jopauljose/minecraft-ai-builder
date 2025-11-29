package com.aiextractor.models;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents an extracted structure with all its block data
 */
public class ExtractedStructure {
    
    private String name;
    private int[] size; // [x, y, z]
    private List<BlockEntry> blocks;
    private StructureMetadata metadata;
    
    public ExtractedStructure(String name, int sizeX, int sizeY, int sizeZ) {
        this.name = name;
        this.size = new int[]{sizeX, sizeY, sizeZ};
        this.blocks = new ArrayList<>();
        this.metadata = new StructureMetadata();
    }
    
    public void addBlock(int x, int y, int z, String blockId) {
        blocks.add(new BlockEntry(x, y, z, blockId));
    }
    
    public void addBlock(int x, int y, int z, String blockId, String blockData) {
        blocks.add(new BlockEntry(x, y, z, blockId, blockData));
    }
    
    // Getters and setters
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public int[] getSize() {
        return size;
    }
    
    public int getSizeX() {
        return size[0];
    }
    
    public int getSizeY() {
        return size[1];
    }
    
    public int getSizeZ() {
        return size[2];
    }
    
    public List<BlockEntry> getBlocks() {
        return blocks;
    }
    
    public StructureMetadata getMetadata() {
        return metadata;
    }
    
    public void setMetadata(StructureMetadata metadata) {
        this.metadata = metadata;
    }
    
    public int getBlockCount() {
        return blocks.size();
    }
    
    /**
     * Represents a single block entry in the structure
     */
    public static class BlockEntry {
        private int x;
        private int y;
        private int z;
        private String block;
        // Complete BlockData string for exact reconstruction
        // e.g., "minecraft:oak_door[facing=north,half=lower,hinge=left,open=false,powered=false]"
        private String data;
        
        public BlockEntry(int x, int y, int z, String block) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.block = block;
            this.data = null;
        }
        
        public BlockEntry(int x, int y, int z, String block, String data) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.block = block;
            this.data = data;
        }
        
        public int getX() {
            return x;
        }
        
        public int getY() {
            return y;
        }
        
        public int getZ() {
            return z;
        }
        
        public String getBlock() {
            return block;
        }
        
        public String getData() {
            return data;
        }
    }
    
    /**
     * Metadata about the extracted structure
     */
    public static class StructureMetadata {
        private String sourceWorld;
        private String captureMode;
        private String captureTime;
        private String capturedBy;
        private int originalX;
        private int originalY;
        private int originalZ;
        
        public StructureMetadata() {
            this.captureTime = java.time.Instant.now().toString();
        }
        
        public String getSourceWorld() {
            return sourceWorld;
        }
        
        public void setSourceWorld(String sourceWorld) {
            this.sourceWorld = sourceWorld;
        }
        
        public String getCaptureMode() {
            return captureMode;
        }
        
        public void setCaptureMode(String captureMode) {
            this.captureMode = captureMode;
        }
        
        public String getCaptureTime() {
            return captureTime;
        }
        
        public void setCaptureTime(String captureTime) {
            this.captureTime = captureTime;
        }
        
        public String getCapturedBy() {
            return capturedBy;
        }
        
        public void setCapturedBy(String capturedBy) {
            this.capturedBy = capturedBy;
        }
        
        public int getOriginalX() {
            return originalX;
        }
        
        public void setOriginalX(int originalX) {
            this.originalX = originalX;
        }
        
        public int getOriginalY() {
            return originalY;
        }
        
        public void setOriginalY(int originalY) {
            this.originalY = originalY;
        }
        
        public int getOriginalZ() {
            return originalZ;
        }
        
        public void setOriginalZ(int originalZ) {
            this.originalZ = originalZ;
        }
    }
}
