package org.pepsoft.worldpainter.layers.plants;

import org.pepsoft.minecraft.Material;
import org.pepsoft.worldpainter.Platform;

import javax.vecmath.Point3i;

import static org.pepsoft.minecraft.Constants.BLK_LARGE_FLOWERS;
import static org.pepsoft.worldpainter.DefaultPlugin.JAVA_ANVIL_1_13;

/**
 * A simple double high plant without growth stages using vanilla Minecraft
 * upper block support.
 */
final class DoubleHighPlant extends Plant {
    /**
     * Create a new double high plant.
     *
     * @param name          The name of the plant.
     * @param lowerMaterial The material of the lower block. The object will
     *                      automatically provide the correct matching upper
     *                      block for the type of plant and the platform.
     * @param category      The category of the plant.
     * @param iconName      The name of the icon of the plant.
     */
    DoubleHighPlant(String name, Material lowerMaterial, Category category, String iconName) {
        super(name, lowerMaterial, category, iconName);
        platform = null;
    }

    private DoubleHighPlant(String name, Material lowerMaterial, Category category, String iconName, Platform platform) {
        super(name, lowerMaterial, category, iconName);
        this.platform = platform;
    }

    @Override
    public Point3i getDimensions() {
        return DIMENSIONS;
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

    @Override
    public DoubleHighPlant realise(int growth, Platform platform) {
        return new DoubleHighPlant(name, material, category, iconName, platform);
    }

    protected final Platform platform;

    @SuppressWarnings("deprecation") // Legacy support
    private static final Material UPPER_DOUBLE_HIGH_PLANT = Material.get(BLK_LARGE_FLOWERS, 8);
    private static final Point3i DIMENSIONS = new Point3i(1, 1, 2);
}