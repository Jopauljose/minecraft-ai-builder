package com.aiextractor.models;

import org.bukkit.Location;
import org.bukkit.World;

/**
 * Represents a 3D bounding box in the world
 */
public class BoundingBox {
    
    private int minX, minY, minZ;
    private int maxX, maxY, maxZ;
    private World world;
    
    public BoundingBox(World world, int x1, int y1, int z1, int x2, int y2, int z2) {
        this.world = world;
        this.minX = Math.min(x1, x2);
        this.minY = Math.min(y1, y2);
        this.minZ = Math.min(z1, z2);
        this.maxX = Math.max(x1, x2);
        this.maxY = Math.max(y1, y2);
        this.maxZ = Math.max(z1, z2);
    }
    
    public BoundingBox(Location loc1, Location loc2) {
        if (!loc1.getWorld().equals(loc2.getWorld())) {
            throw new IllegalArgumentException("Locations must be in the same world");
        }
        this.world = loc1.getWorld();
        this.minX = Math.min(loc1.getBlockX(), loc2.getBlockX());
        this.minY = Math.min(loc1.getBlockY(), loc2.getBlockY());
        this.minZ = Math.min(loc1.getBlockZ(), loc2.getBlockZ());
        this.maxX = Math.max(loc1.getBlockX(), loc2.getBlockX());
        this.maxY = Math.max(loc1.getBlockY(), loc2.getBlockY());
        this.maxZ = Math.max(loc1.getBlockZ(), loc2.getBlockZ());
    }
    
    /**
     * Create a bounding box from a single point (will be expanded later)
     */
    public BoundingBox(Location loc) {
        this.world = loc.getWorld();
        this.minX = loc.getBlockX();
        this.minY = loc.getBlockY();
        this.minZ = loc.getBlockZ();
        this.maxX = loc.getBlockX();
        this.maxY = loc.getBlockY();
        this.maxZ = loc.getBlockZ();
    }
    
    /**
     * Expand the bounding box to include a new point
     */
    public void expand(int x, int y, int z) {
        minX = Math.min(minX, x);
        minY = Math.min(minY, y);
        minZ = Math.min(minZ, z);
        maxX = Math.max(maxX, x);
        maxY = Math.max(maxY, y);
        maxZ = Math.max(maxZ, z);
    }
    
    /**
     * Expand the bounding box to include a location
     */
    public void expand(Location loc) {
        expand(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
    }
    
    /**
     * Add padding to all sides of the bounding box
     */
    public void addPadding(int padding) {
        minX -= padding;
        minY -= padding;
        minZ -= padding;
        maxX += padding;
        maxY += padding;
        maxZ += padding;
    }
    
    /**
     * Check if a point is inside this bounding box
     */
    public boolean contains(int x, int y, int z) {
        return x >= minX && x <= maxX &&
               y >= minY && y <= maxY &&
               z >= minZ && z <= maxZ;
    }
    
    /**
     * Check if a location is inside this bounding box
     */
    public boolean contains(Location loc) {
        return loc.getWorld().equals(world) && 
               contains(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
    }
    
    // Dimension getters
    public int getSizeX() {
        return maxX - minX + 1;
    }
    
    public int getSizeY() {
        return maxY - minY + 1;
    }
    
    public int getSizeZ() {
        return maxZ - minZ + 1;
    }
    
    public int getVolume() {
        return getSizeX() * getSizeY() * getSizeZ();
    }
    
    // Position getters
    public int getMinX() {
        return minX;
    }
    
    public int getMinY() {
        return minY;
    }
    
    public int getMinZ() {
        return minZ;
    }
    
    public int getMaxX() {
        return maxX;
    }
    
    public int getMaxY() {
        return maxY;
    }
    
    public int getMaxZ() {
        return maxZ;
    }
    
    public World getWorld() {
        return world;
    }
    
    public Location getMinLocation() {
        return new Location(world, minX, minY, minZ);
    }
    
    public Location getMaxLocation() {
        return new Location(world, maxX, maxY, maxZ);
    }
    
    public Location getCenter() {
        return new Location(world, 
            (minX + maxX) / 2.0, 
            (minY + maxY) / 2.0, 
            (minZ + maxZ) / 2.0);
    }
    
    @Override
    public String toString() {
        return String.format("BoundingBox[(%d,%d,%d) to (%d,%d,%d), size=%dx%dx%d]",
            minX, minY, minZ, maxX, maxY, maxZ, getSizeX(), getSizeY(), getSizeZ());
    }
}
