package org.pepsoft.worldpainter.layers.plants;

import org.pepsoft.minecraft.Material;

import javax.vecmath.Point3i;

/**
 * A simple one-block plant with no growth stages.
 */
class SimplePlant extends Plant {
    SimplePlant(String name, Material material, Category category) {
        super(name, material, category, "block/" + material.simpleName + ".png");
    }

    SimplePlant(String name, Material material, Category category, String iconName) {
        super(name, material, category, iconName);
    }

    @Override
    public Point3i getDimensions() {
        return DIMENSIONS;
    }

    private static final Point3i DIMENSIONS = new Point3i(1, 1, 1);
}