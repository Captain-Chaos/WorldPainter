package org.pepsoft.worldpainter.layers.plants;

import org.pepsoft.minecraft.Material;

import javax.vecmath.Point3i;

/**
 * A simple one-block plant with no growth stages.
 */
class SimplePlant extends Plant {
    SimplePlant(String name, Material material, Category... category) {
        super(name, material, "block/" + material.simpleName + ".png", category);
    }

    SimplePlant(String name, Material material, String iconName, Category... category) {
        super(name, material, iconName, category);
    }

    SimplePlant(String name, Material material, String[] iconNames, Category... category) {
        super(name, material, iconNames, category);
    }

    @Override
    public Point3i getDimensions() {
        return DIMENSIONS;
    }

    private static final Point3i DIMENSIONS = new Point3i(1, 1, 1);
}