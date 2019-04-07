package org.pepsoft.worldpainter.layers.plants;

import org.pepsoft.minecraft.Material;

import static org.pepsoft.minecraft.Constants.BLK_LARGE_FLOWERS;
import static org.pepsoft.worldpainter.DefaultPlugin.JAVA_ANVIL_1_13;

/**
 * A simple double high plant without growth stages using vanilla Minecraft
 * upper block support.
 */
class DoubleHighPlant extends Plant {
    DoubleHighPlant(String name, Material material, Category category, String iconName) {
        super(name, material, 2, 0, category, iconName);
    }

    @Override
    public Material getMaterial(int x, int y, int z) {
        if (z > 0) {
            if (platform == JAVA_ANVIL_1_13) {
                return material.withProperty("half", "upper");
            } else {
                return UPPER_DOUBLE_HIGH_PLANT;
            }
        } else {
            return material;
        }
    }

    @SuppressWarnings("deprecation") // Legacy support
    private static final Material UPPER_DOUBLE_HIGH_PLANT = Material.get(BLK_LARGE_FLOWERS, 8);
}