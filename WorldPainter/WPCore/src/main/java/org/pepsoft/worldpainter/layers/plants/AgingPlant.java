package org.pepsoft.worldpainter.layers.plants;

import org.pepsoft.minecraft.Material;
import org.pepsoft.worldpainter.Platform;

import static org.pepsoft.minecraft.Material.AGE;

/**
 * A simple one-block plant with an {@code age} property.
 */
class AgingPlant extends SimplePlant {
    AgingPlant(String name, Material material, Category category, String iconName, int maxGrowth) {
        super(name, material, category, iconName);
        this.maxGrowth = maxGrowth;
    }

    @Override
    public int getMaxGrowth() {
        return maxGrowth;
    }

    @Override
    public AgingPlant realise(int growth, Platform platform) {
        return new AgingPlant(name, material.withProperty(AGE, growth - 1), category, iconName, maxGrowth);
    }

    protected final int maxGrowth;
}
