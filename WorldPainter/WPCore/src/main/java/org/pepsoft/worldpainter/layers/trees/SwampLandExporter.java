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
import org.pepsoft.worldpainter.layers.exporters.ExporterSettings;
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
    public SwampLandExporter(Dimension dimension, Platform platform, ExporterSettings settings, SwampLand layer) {
        super(dimension, platform, settings, layer);
    }

    @Override
    public List<Fixup> addFeatures(Rectangle area, Rectangle exportedArea, MinecraftWorld world) {
        final List<Fixup> fixups = super.addFeatures(area, exportedArea, world);
        
        // Render lily pads
        final TreeLayerSettings<? extends TreeLayer> settings = (TreeLayerSettings<? extends TreeLayer>) super.settings;
        final int minimumLevel = settings.getMinimumLevel();
        for (int chunkX = area.x; chunkX < area.x + area.width; chunkX += 16) {
            for (int chunkY = area.y; chunkY < area.y + area.height; chunkY += 16) {
                // Set the seed and randomizer according to the chunk
                // coordinates to make sure the chunk is always rendered the
                // same, no matter how often it is rendererd
                final long seed = dimension.getSeed() + (chunkX >> 4) * 65537L + (chunkY >> 4) * 4099L + layer.hashCode() + 1;
                final Random random = new Random(seed);
                for (int x = chunkX; x < chunkX + 16; x++) {
                    for (int y = chunkY; y < chunkY + 16; y++) {
                        final int terrainLevel = dimension.getIntHeightAt(x, y);
                        if (terrainLevel == Integer.MIN_VALUE) {
                            // height == Integer.MIN_VALUE means there is no tile there
                            continue;
                        }
                        final int waterLevel = dimension.getWaterLevelAt(x, y);
                        if ((waterLevel > terrainLevel) && (waterLevel < maxZ)) {
                            final int strength = Math.max(minimumLevel, dimension.getLayerValueAt(layer, x, y));
                            if ((strength > 0) && (random.nextInt(3840) <= (strength * strength))) {
                                final Material material = world.getMaterialAt(x, y, waterLevel);
                                final Material materialAbove = world.getMaterialAt(x, y, waterLevel + 1);
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