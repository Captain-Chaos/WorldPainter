package org.pepsoft.worldpainter.layers.plants;

import org.pepsoft.minecraft.Material;
import org.pepsoft.worldpainter.Platform;

import javax.vecmath.Point3i;

import static org.pepsoft.minecraft.Constants.BLK_LARGE_FLOWERS;
import static org.pepsoft.minecraft.Constants.MC_HALF;
import static org.pepsoft.worldpainter.Platform.Capability.NAME_BASED;

/**
 * A simple double high plant without growth stages using vanilla Minecraft
 * upper block support.
 */
class DoubleHighPlant extends Plant {
    /**
     * Create a new double high plant.
     *
     * @param name          The name of the plant.
     * @param lowerMaterial The material of the lower block. The object will
     *                      automatically provide the correct matching upper
     *                      block for the type of plant and the platform.
     * @param category      The category of the plant.
     */
    DoubleHighPlant(String name, Material lowerMaterial, Category category) {
        super(name, lowerMaterial, "block/" + lowerMaterial.simpleName + "_top.png", category);
        platform = null;
    }

    /**
     * Create a new double high plant.
     *
     * @param name          The name of the plant.
     * @param lowerMaterial The material of the lower block. The object will automatically provide the correct matching
     *                      upper block for the type of plant and the platform.
     * @param iconName      The name of the icon of the plant.
     * @param category      The category of the plant.
     */
    DoubleHighPlant(String name, Material lowerMaterial, String iconName, Category category) {
        super(name, lowerMaterial, iconName, category);
        platform = null;
    }

    protected DoubleHighPlant(String name, Material lowerMaterial, Category category, Platform platform) {
        super(name, lowerMaterial, "block/" + lowerMaterial.simpleName + "_top.png", category);
        this.platform = platform;
    }

    /**
     * Copy constructor.
     */
    private DoubleHighPlant(String name, Material lowerMaterial, Category[] categories, String[] iconNames, Platform platform) {
        super(name, lowerMaterial, iconNames, categories);
        this.platform = platform;
    }

    @Override
    public Point3i getDimensions() {
        return DIMENSIONS;
    }

    @Override
    public Material getMaterial(int x, int y, int z) {
        if (z > 0) {
            if ((platform != null) && platform.capabilities.contains(NAME_BASED)) {
                return material.withProperty(MC_HALF, "upper");
            } else {
                return UPPER_DOUBLE_HIGH_PLANT;
            }
        } else {
            return material;
        }
    }

    @Override
    public DoubleHighPlant realise(int growth, Platform platform) {
        return new DoubleHighPlant(name, material, categories, getIconNames(), platform);
    }

    private final Platform platform;

    private static final Material UPPER_DOUBLE_HIGH_PLANT = Material.get(BLK_LARGE_FLOWERS, 8);
    private static final Point3i DIMENSIONS = new Point3i(1, 1, 2);
}