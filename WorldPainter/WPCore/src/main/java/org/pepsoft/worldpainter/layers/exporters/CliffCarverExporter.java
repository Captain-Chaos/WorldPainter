package org.pepsoft.worldpainter.layers.exporters;

import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.Platform;
import org.pepsoft.worldpainter.Terrain;
import org.pepsoft.worldpainter.exporting.AbstractLayerExporter;
import org.pepsoft.worldpainter.exporting.Fixup;
import org.pepsoft.worldpainter.exporting.MinecraftWorld;
import org.pepsoft.worldpainter.exporting.SecondPassLayerExporter;
import org.pepsoft.worldpainter.layers.CliffCarver;

import java.awt.*;
import java.util.List;

import static org.pepsoft.minecraft.Material.AIR;
import static org.pepsoft.worldpainter.util.GeometryUtil.getFilledCircleCoordinates;

public class CliffCarverExporter extends AbstractLayerExporter<CliffCarver> implements SecondPassLayerExporter {
    public CliffCarverExporter(Dimension dimension, Platform platform, ExporterSettings settings, CliffCarver layer) {
        super(dimension, platform, settings, layer);
    }

    public int getMinCliffHeight() {
        return minCliffHeight;
    }

    public void setMinCliffHeight(int minCliffHeight) {
        this.minCliffHeight = minCliffHeight;
    }

    @Override
    public List<Fixup> carve(Rectangle area, Rectangle exportedArea, MinecraftWorld world) {
        for (int x = area.x; x < area.x + area.width; x++) {
            for (int y = area.y; y < area.y + area.height; y++) {
                if (dimension.getBitLayerValueAt(layer, x, y)) {
                    carveColumn(world, x, y);
                }
            }
        }
        return null;
    }

    private void carveColumn(MinecraftWorld world, int x, int y) {
        final float terrainHeight = dimension.getHeightAt(x, y);
        final Terrain terrain = dimension.getTerrainAt(x, y);
        final long seed = dimension.getSeed();
        final float cliffTop = Math.max(
                Math.max(dimension.getHeightAt(x - 1, y), dimension.getHeightAt(x, y - 1)),
                Math.max(dimension.getHeightAt(x + 1, y), dimension.getHeightAt(x, y + 1)));
        final float cliffHeight = cliffTop - terrainHeight;
        boolean replaceTerrain = true;
        float previousCarveDepth = 0.0f;
        if (cliffHeight > minCliffHeight) {
            for (int z = Math.round(terrainHeight) + 1; z < Math.round(cliffTop); z++) {
                final float carveDepth = getCarveDepth(x, y, terrainHeight, z - terrainHeight, cliffHeight);
                if (replaceTerrain && (carveDepth <= previousCarveDepth)) {
                    replaceTerrain = false;
                }
                previousCarveDepth = carveDepth;
                for (Point p: getFilledCircleCoordinates(carveDepth)) {
                    if ((p.x == 0) && (p.y == 0)) {
                        continue;
                    }
                    final int worldX = x + p.x, worldY = y + p.y;
                    world.setMaterialAt(worldX, worldY, z, AIR);
                    final int zBelow = z - 1;
                    if (replaceTerrain && (world.getMaterialAt(worldX, worldY, zBelow).solid)) {
                        world.setMaterialAt(worldX, worldY, zBelow, terrain.getMaterial(platform, seed, worldX, worldY, zBelow, zBelow));
                        System.out.println(world.getMaterialAt(worldX, worldY, zBelow));
                    }
                }
            }
        }
    }

    private float getCarveDepth(int x, int y, float terrainHeight, float dz, float cliffHeight) {
        // TODO this is just a simple demo algorithm
        return Math.min(Math.min(dz, cliffHeight - dz), 16.0f);
    }

    private int minCliffHeight = 2;
}
