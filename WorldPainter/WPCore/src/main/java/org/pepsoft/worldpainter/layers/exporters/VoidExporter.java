/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.layers.exporters;

import org.pepsoft.util.MathUtils;
import org.pepsoft.util.PerlinNoise;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.Platform;
import org.pepsoft.worldpainter.exporting.AbstractLayerExporter;
import org.pepsoft.worldpainter.exporting.Fixup;
import org.pepsoft.worldpainter.exporting.MinecraftWorld;
import org.pepsoft.worldpainter.exporting.SecondPassLayerExporter;
import org.pepsoft.worldpainter.layers.Void;

import java.awt.*;
import java.util.List;

import static org.pepsoft.minecraft.Constants.MC_LAVA;
import static org.pepsoft.minecraft.Constants.MC_WATER;
import static org.pepsoft.minecraft.Material.*;
import static org.pepsoft.worldpainter.Constants.SMALL_BLOBS;

/**
 * This exporter does the second half of the void processing. The first half
 * has been performed in the first pass by hardcoded code which has left any
 * columns marked as Void completely empty.
 * 
 * <p>This plugin does some decoration around the void areas.
 * 
 * @author pepijn
 */
public class VoidExporter extends AbstractLayerExporter<org.pepsoft.worldpainter.layers.Void> implements SecondPassLayerExporter {
    public VoidExporter() {
        super(Void.INSTANCE);
    }
    
    @Override
    public List<Fixup> render(Dimension dimension, Rectangle area, Rectangle exportedArea, MinecraftWorld minecraftWorld, Platform platform) {
        if (noise.getSeed() != (dimension.getSeed() + SEED_OFFSET)) {
            noise.setSeed(dimension.getSeed() + SEED_OFFSET);
        }
        for (int x = area.x; x < area.x + area.width; x++) {
            for (int y = area.y; y < area.y + area.height; y++) {
                if (dimension.getBitLayerValueAt(Void.INSTANCE, x, y)
                        && (dimension.getDistanceToEdge(Void.INSTANCE, x, y, 2) < 2)) {
                    // We're on the edge of the Void
                    processEdgeColumn(dimension, x, y, minecraftWorld);
                }
            }
        }
        return null;
    }

    private void processEdgeColumn(final Dimension dimension, final int x, final int y, final MinecraftWorld minecraftWorld) {
        final int maxHeight = minecraftWorld.getMaxHeight();
        // Taper the world edges slightly inward
        final int r = 3;
        for (int dx = -r; dx <= r; dx++) {
            for (int dy = -r; dy <= r; dy++) {
                if ((dx != 0) || (dy != 0)) {
                    final int x2 = x + dx, y2 = y + dy;
                    if (dimension.getBitLayerValueAt(Void.INSTANCE, x2, y2)) {
                        continue;
                    }
                    final float distance = MathUtils.getDistance(dx, dy);
                    final float height = dimension.getHeightAt(x2, y2);
                    final int depth = (int) (height / Math.pow(2, distance + noise.getPerlinNoise(x2 / SMALL_BLOBS, y2 / SMALL_BLOBS)) + 0.5f);
                    for (int z = 0; z < depth; z++) {
                        minecraftWorld.setMaterialAt(x2, y2, z, AIR);
                    }
                }
            }
        }
        // Check for water surrounding the column; pre-render the falling water
        // column to avoid long pauses in Minecraft when the chunks are loaded
        // (but not for ceiling dimensions)
        if (dimension.getDim() >= 0) {
            for (int z = maxHeight - 1; z >= 0; z--) {
                if ((minecraftWorld.getMaterialAt(x, y, z).isNamed(MC_WATER))
                        || (minecraftWorld.getMaterialAt(x, y, z).isNamed(MC_LAVA))) {
                    // A previous iteration already placed fluid here
                    break;
                } else if (isWaterAndNotVoid(dimension, minecraftWorld, x - 1, y, z)
                        || isWaterAndNotVoid(dimension, minecraftWorld, x, y - 1, z)
                        || isWaterAndNotVoid(dimension, minecraftWorld, x + 1, y, z)
                        || isWaterAndNotVoid(dimension, minecraftWorld, x, y + 1, z)) {
                    minecraftWorld.setMaterialAt(x, y, z, WATER.withProperty(LEVEL, 1));
                    for (z--; z >= 0; z--) {
                        minecraftWorld.setMaterialAt( x, y, z, WATER.withProperty(LEVEL, 9));
                    }
                    break;
                } else if (isLavaAndNotVoid(dimension, minecraftWorld, x - 1, y, z)
                        || isLavaAndNotVoid(dimension, minecraftWorld, x, y - 1, z)
                        || isLavaAndNotVoid(dimension, minecraftWorld, x + 1, y, z)
                        || isLavaAndNotVoid(dimension, minecraftWorld, x, y + 1, z)) {
                    minecraftWorld.setMaterialAt(x, y, z, LAVA.withProperty(LEVEL, 2));
                    for (z--; z >= 0; z--) {
                        minecraftWorld.setMaterialAt(x, y, z, LAVA.withProperty(LEVEL, 10));
                    }
                    break;
                }
            }
        }
    }

    private boolean isWaterAndNotVoid(Dimension dimension, MinecraftWorld minecraftWorld, int x, int y, int z) {
        return (! dimension.getBitLayerValueAt(Void.INSTANCE, x, y)) && (minecraftWorld.getMaterialAt(x, y, z).isNamed(MC_WATER));
    }

    private boolean isLavaAndNotVoid(Dimension dimension, MinecraftWorld minecraftWorld, int x, int y, int z) {
        return (! dimension.getBitLayerValueAt(Void.INSTANCE, x, y)) && (minecraftWorld.getMaterialAt(x, y, z).isNamed(MC_LAVA));
    }

    private final PerlinNoise noise = new PerlinNoise(0);
    
    private static final long SEED_OFFSET = 142644289;
}