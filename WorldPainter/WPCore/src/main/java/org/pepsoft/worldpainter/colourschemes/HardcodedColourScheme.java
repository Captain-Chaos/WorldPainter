package org.pepsoft.worldpainter.colourschemes;

import org.pepsoft.minecraft.Material;
import org.pepsoft.worldpainter.ColourScheme;

public class HardcodedColourScheme implements ColourScheme {
    @Override
    public int getColour(int blockType) {
        return Material.get(blockType).colour;
    }

    @Override
    public int getColour(int blockType, int dataValue) {
        return Material.get(blockType, dataValue).colour;
    }

    @Override
    public int getColour(Material material) {
        return material.colour;
    }
}