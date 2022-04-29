/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.layers.trees;

import org.pepsoft.minecraft.Material;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.Platform;
import org.pepsoft.worldpainter.exporting.Fixup;
import org.pepsoft.worldpainter.exporting.MinecraftWorld;
import org.pepsoft.worldpainter.layers.SwampLand;
import org.pepsoft.worldpainter.layers.TreeLayer;
import org.pepsoft.worldpainter.layers.exporters.TreesExporter;

import java.awt.*;
import java.util.List;
import java.util.Random;

import static org.pepsoft.minecraft.Constants.MC_WATER;
import static org.pepsoft.minecraft.Material.AIR;

/**
 *
 * @author pepijn
 */
public class SwampLandExporter extends TreesExporter<TreeLayer> {
    public SwampLandExporter(SwampLand layer) {
        super(layer);
    }

    @Override
    public List<Fixup> addFeatures(Dimension dimension, Rectangle area, Rectangle exportedArea, MinecraftWorld world, Platform platform) {
        List<Fixup> fixups = super.render(dimension, area, exportedArea, world, platform);
        
        // Render lily pads
        TreeLayerSettings<TreeLayer> settings = (TreeLayerSettings<TreeLayer>) getSettings();
        int minimumLevel = settings.getMinimumLevel();
        int maxZ = dimension.getMaxHeight() - 1;
        for (int chunkX = area.x; chunkX < area.x + area.width; chunkX += 16) {
            for (int chunkY = area.y; chunkY < area.y + area.height; chunkY += 16) {
                // Set the seed and randomizer according to the chunk
                // coordinates to make sure the chunk is always rendered the
                // same, no matter how often it is rendererd
                long seed = dimension.getSeed() + (chunkX >> 4) * 65537 + (chunkY >> 4) * 4099 + layer.hashCode() + 1;
                Random random = new Random(seed);
                for (int x = chunkX; x < chunkX + 16; x++) {
                    for (int y = chunkY; y < chunkY + 16; y++) {
                        int terrainLevel = dimension.getIntHeightAt(x, y);
                        if (terrainLevel == Integer.MIN_VALUE) {
                            // height == Integer.MIN_VALUE means there is no tile there
                            continue;
                        }
                        int waterLevel = dimension.getWaterLevelAt(x, y);
                        if ((waterLevel > terrainLevel) && (waterLevel < maxZ)) {
                            int strength = Math.max(minimumLevel, dimension.getLayerValueAt(layer, x, y));
                            if ((strength > 0) && (random.nextInt(3840) <= (strength * strength))) {
                                Material material = world.getMaterialAt(x, y, waterLevel);
                                Material materialAbove = world.getMaterialAt(x, y, waterLevel + 1);
                                if (material.isNamed(MC_WATER) && (materialAbove == AIR)) {
                                    world.setMaterialAt(x, y, waterLevel + 1, Material.LILY_PAD);
                                }
                            }
                        }
                    }
                }
            }
        }
        return fixups;
    }
}