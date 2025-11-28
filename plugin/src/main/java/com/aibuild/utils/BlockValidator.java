package com.aibuild.utils;

import org.bukkit.Material;

import java.util.Set;

public class BlockValidator {
    private static final Set<Material> ALLOWED_BLOCKS = Set.of(
        Material.COBBLESTONE,
        Material.OAK_PLANKS,
        Material.OAK_FENCE,
        Material.LADDER,
        Material.GLASS,
        Material.STONE_BRICKS,
        Material.BRICK,
        Material.AIR
    );

    public static boolean isValidBlock(Material block) {
        return ALLOWED_BLOCKS.contains(block);
    }

    public static boolean areValidBlocks(Material[][] layer) {
        for (Material[] row : layer) {
            for (Material block : row) {
                if (!isValidBlock(block)) {
                    return false;
                }
            }
        }
        return true;
    }
}