package org.pepsoft.worldpainter.layers.plants;

import org.pepsoft.minecraft.Material;
import org.pepsoft.worldpainter.Platform;

import static org.pepsoft.minecraft.Material.AGE;

/**
 * A simple one-block plant with an {@code age} property.
 */
class AgingPlant extends SimplePlant {
    AgingPlant(String name, Material material, String iconName, int maxGrowth, Category... category) {
        super(name, material, iconName, category);
        this.maxGrowth = maxGrowth;
    }

    @Override
    public int getMaxGrowth() {
        return maxGrowth;
    }

    @Override
    public Plant realise(int growth, Platform platform) {
        return new SimplePlant(name, material.withProperty(AGE, growth - 1), iconName, categories);
    }

    protected final int maxGrowth;
}
