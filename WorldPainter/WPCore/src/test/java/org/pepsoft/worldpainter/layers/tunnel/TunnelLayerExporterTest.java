package org.pepsoft.worldpainter.layers.tunnel;

import org.junit.Assert;
import org.junit.Test;
import org.pepsoft.minecraft.Material;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.NoiseSettings;
import org.pepsoft.worldpainter.exporting.MinecraftWorld;

import java.awt.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.pepsoft.minecraft.Material.*;
import static org.pepsoft.worldpainter.Constants.TILE_SIZE;
import static org.pepsoft.worldpainter.TestData.*;
import static org.pepsoft.worldpainter.layers.tunnel.TunnelLayer.LayerMode.CAVE;
import static org.pepsoft.worldpainter.layers.tunnel.TunnelLayer.Mode.FIXED_HEIGHT;

public class TunnelLayerExporterTest {
    @Test
    public void testTunnelLayerExporter() {
        final Rectangle area = new Rectangle(0, 0, TILE_SIZE, TILE_SIZE);
        final Dimension dimension = createDimension(area, 100);
        final TunnelLayer layer = createTestLayer();
        for (int x = 0; x < TILE_SIZE; x++) {
            for (int y = 0; y < TILE_SIZE; y++) {
                dimension.setBitLayerValueAt(layer, x, y, true);
            }
        }

        try (final MinecraftWorld minecraftWorld = createMinecraftWorld(area, 100, GRASS_BLOCK)) {
            final TunnelLayerExporter exporter = new TunnelLayerExporter(dimension, PLATFORM, layer, layer.getHelper(dimension));
            exporter.carve(area, area, minecraftWorld);
            for (int x = 0; x < TILE_SIZE; x++) {
                for (int y = 0; y < TILE_SIZE; y++) {
                    for (int z = MIN_HEIGHT; z < MAX_HEIGHT; z++) {
                        final Material material = minecraftWorld.getMaterialAt(x, y, z);
                        if (z == MIN_HEIGHT) {
                            assertEquals(x + "," + y + "," + z, BEDROCK, material);
                        } else if (z < 0) {
                            assertEquals(x + "," + y + "," + z, DEEPSLATE_Y, material);
                        } else if (z <= 20) {
                            assertEquals(x + "," + y + "," + z, STONE, material);
                        } else if (z <= 30) {
                            assertTrue(x + "," + y + "," + z + ": " + material, material == STONE || material.air);
                        } else if (z <= 70) {
                            assertEquals(x + "," + y + "," + z, CAVE_AIR, material);
                        } else if (z < 80) {
                            assertTrue(x + "," + y + "," + z + ": " + material, material == STONE || material.air);
                        } else if (z < 97) {
                            assertEquals(x + "," + y + "," + z, STONE, material);
                        } else if (z < 100) {
                            assertEquals(x + "," + y + "," + z, DIRT, material);
                        } else if (z == 100) {
                            assertEquals(x + "," + y + "," + z, GRASS_BLOCK, material);
                        } else {
                            assertEquals(x + "," + y + "," + z, AIR, material);
                        }
                    }
                }
            }
        }
    }

    @Test
    public void testTunnelDimensions() {
        final Rectangle area = new Rectangle(0, 0, TILE_SIZE, TILE_SIZE);
        final Dimension dimension = createDimension(area, 100);
        final TunnelLayer layer = createTestLayer();
        for (int x = 0; x < TILE_SIZE; x++) {
            for (int y = 0; y < TILE_SIZE; y++) {
                dimension.setBitLayerValueAt(layer, x, y, true);
            }
        }

        try (final MinecraftWorld minecraftWorld = createMinecraftWorld(area, 100, GRASS_BLOCK)) {
            final TunnelLayerHelper helper = layer.getHelper(dimension);
            final TunnelFloorDimension tunnelFloorDimension = new TunnelFloorDimension(dimension, layer, helper);
            final TunnelRoofDimension tunnelRoofDimension = new TunnelRoofDimension(dimension, layer, helper);
            final int reflectionPoint = MAX_HEIGHT + MIN_HEIGHT - 1;
            final TunnelLayerExporter exporter = new TunnelLayerExporter(dimension, PLATFORM, layer, helper);
            exporter.carve(area, area, minecraftWorld);
            for (int x = 0; x < TILE_SIZE; x++) {
                for (int y = 0; y < TILE_SIZE; y++) {
                    int detectedFloorLevel = Integer.MIN_VALUE, detectedRoofLevel = Integer.MIN_VALUE;
                    for (int z = MIN_HEIGHT; z <= 100; z++) {
                        final Material material = minecraftWorld.getMaterialAt(x, y, z);
                        if ((detectedFloorLevel == Integer.MIN_VALUE) && (material.air)) {
                            detectedFloorLevel = z - 1;
                        } else if ((detectedFloorLevel != Integer.MIN_VALUE) && ((! material.air))) {
                            detectedRoofLevel = z;
                            break;
                        }
                    }
                    Assert.assertTrue(x + "," + y, detectedFloorLevel != Integer.MIN_VALUE && detectedRoofLevel != Integer.MIN_VALUE);
                    assertEquals(x + "," + y, detectedFloorLevel, tunnelFloorDimension.getIntHeightAt(x, y));
                    assertEquals(x + "," + y, reflectionPoint - detectedRoofLevel, tunnelRoofDimension.getIntHeightAt(x, y));
                }
            }
        }
    }

    private TunnelLayer createTestLayer() {
        final TunnelLayer layer = new TunnelLayer("Test", CAVE, null, PLATFORM);
        layer.setFloorMode(FIXED_HEIGHT);
        layer.setFloorLevel(25);
        layer.setFloorWallDepth(0);
        layer.setFloorNoise(new NoiseSettings(SEED, 5, 1, 1.0f));
        layer.setRoofMode(FIXED_HEIGHT);
        layer.setRoofLevel(75);
        layer.setRoofWallDepth(0);
        layer.setRoofNoise(new NoiseSettings(SEED + 1, 5, 1, 1.0f));
        return layer;
    }
}