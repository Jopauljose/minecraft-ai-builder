package com.aibuild.utils;

import org.bukkit.Material;

public class BlockValidator {
    
    /**
     * Check if a material is a valid placeable block.
     * Allows any block type - only rejects non-block materials.
     * @param material The material to check
     * @return true if it's a valid block, false otherwise
     */
    public static boolean isValidBlock(Material material) {
        if (material == null) {
            return false;
        }
        // Allow any block type - just check if it's actually a block
        return material.isBlock();
    }
}
