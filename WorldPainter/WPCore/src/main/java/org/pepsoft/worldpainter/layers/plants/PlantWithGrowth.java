package org.pepsoft.worldpainter.layers.plants;

import org.pepsoft.minecraft.Material;
import org.pepsoft.worldpainter.Platform;

/**
 * A plant with growth stages.
 */
abstract class PlantWithGrowth extends SimplePlant {
    /**
     * Create a new plant with growth states. The growth value is one-based.
     */
    PlantWithGrowth(String name, Material material, String iconName, int maxGrowth, Category... category) {
        super(name, material, iconName, category);
        this.maxGrowth = maxGrowth;
    }

    /**
     * Create a new plant with growth states. The growth value is one-based.
     */
    PlantWithGrowth(String name, Material material, String[] iconNames, int maxGrowth, Category... category) {
        super(name, material, iconNames, category);
        this.maxGrowth = maxGrowth;
    }

    @Override
    public final int getMaxGrowth() {
        return maxGrowth;
    }

    @Override
    public abstract Plant realise(int growth, Platform platform);

    protected final int maxGrowth;
}