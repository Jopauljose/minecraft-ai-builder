package com.aibuild.models;

import java.util.Map;

public class Structure {
    private final int[] size;
    private final Map<String, String> palette;
    private final Map<Integer, String[][]> layers;
    private String name;

    public Structure(int[] size, Map<String, String> palette, Map<Integer, String[][]> layers) {
        this.size = size;
        this.palette = palette;
        this.layers = layers;
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

    public Map<String, String> getPalette() {
        return palette;
    }

    public Map<Integer, String[][]> getLayers() {
        return layers;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    /**
     * Get the block ID at a specific position
     * @param x X coordinate
     * @param y Y coordinate (layer index)
     * @param z Z coordinate
     * @return The minecraft block ID or null if not found
     */
    public String getBlockAt(int x, int y, int z) {
        String[][] layer = layers.get(y);
        if (layer == null) return null;
        if (z < 0 || z >= layer.length) return null;
        if (x < 0 || x >= layer[z].length) return null;
        
        String paletteKey = layer[z][x];
        return palette.get(paletteKey);
    }
}