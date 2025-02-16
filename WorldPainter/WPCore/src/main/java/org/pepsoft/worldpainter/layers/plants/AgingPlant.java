package org.pepsoft.worldpainter.layers.plants;

import org.pepsoft.minecraft.Material;
import org.pepsoft.worldpainter.Platform;

import static org.pepsoft.minecraft.Material.AGE;

/**
 * A simple one-block plant with an {@code age} property.
 */
class AgingPlant extends PlantWithGrowth {
    AgingPlant(String name, Material material, String iconName, int maxGrowth, Category... category) {
        super(name, material, iconName, maxGrowth, category);
    }

    @Override
    public Plant realise(int growth, Platform platform) {
        return new SimplePlant(name, material.withProperty(AGE, growth - 1), getIconNames(), categories);
    }
}