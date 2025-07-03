package org.pepsoft.worldpainter.layers.tunnel;

import org.pepsoft.minecraft.Chunk;
import org.pepsoft.util.PerlinNoise;
import org.pepsoft.worldpainter.*;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.Dimension.Anchor;
import org.pepsoft.worldpainter.exporting.FirstPassLayerExporter;
import org.pepsoft.worldpainter.exporting.SecondPassLayerExporter;
import org.pepsoft.worldpainter.exporting.WorldPainterChunkFactory;
import org.pepsoft.worldpainter.heightMaps.AbstractHeightMap;
import org.pepsoft.worldpainter.layers.Biome;
import org.pepsoft.worldpainter.layers.FloodWithLava;
import org.pepsoft.worldpainter.layers.Layer;
import org.pepsoft.worldpainter.objects.WPObject;
import org.pepsoft.worldpainter.util.BiomeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;

import static java.awt.Color.BLACK;
import static java.lang.Math.round;
import static java.util.Collections.singleton;
import static org.pepsoft.minecraft.Constants.*;
import static org.pepsoft.worldpainter.Constants.*;
import static org.pepsoft.worldpainter.Dimension.Role.FLOATING_FLOOR;
import static org.pepsoft.worldpainter.biomeschemes.Minecraft1_7Biomes.*;
import static org.pepsoft.worldpainter.exporting.SecondPassLayerExporter.Stage.ADD_FEATURES;
import static org.pepsoft.worldpainter.exporting.WorldPainterChunkFactory.SUGAR_CANE_CHANCE;
import static org.pepsoft.worldpainter.exporting.WorldPainterChunkFactory.SUGAR_CANE_SEED_OFFSET;
import static org.pepsoft.worldpainter.layers.plants.Plants.SUGAR_CANE;
import static org.pepsoft.worldpainter.layers.tunnel.TunnelLayer.LayerMode.FLOATING;

public class FloatingLayerExporter extends AbstractTunnelLayerExporter implements FirstPassLayerExporter, SecondPassLayerExporter {
    public FloatingLayerExporter(Dimension outerDimension, Platform platform, TunnelLayer layer, TunnelLayerHelper helper) {
        super(outerDimension.getWorld().getDimension(new Anchor(outerDimension.getAnchor().dim, FLOATING_FLOOR, outerDimension.getAnchor().invert, layer.getFloorDimensionId())), platform, layer, helper);
        seed = dimension.getSeed();
        if (sugarCaneNoise.getSeed() != (seed + SUGAR_CANE_SEED_OFFSET)) {
            sugarCaneNoise.setSeed(seed + SUGAR_CANE_SEED_OFFSET);
        }
        wpChunkFactory = new WorldPainterChunkFactory(dimension, null, platform, outerDimension.getMaxHeight());
        maxZ = maxHeight - 1;
        minHeightField = new AbstractHeightMap() {
            @Override
            public Icon getIcon() {
                return null;
            }

            @Override
            public double[] getRange() {
                return new double[] {minHeight, maxZ};
            }

            @Override
            public double getHeight(int x, int y) {
                return (outerDimension.getBitLayerValueAt(layer, x, y))
                        ? helper.calculateBottomLevel(x, y, minHeight, maxZ, helper.calculateFloorLevel(x, y, outerDimension.getIntHeightAt(x, y), minHeight, maxZ), helper.getDistanceToWall(x, y))
                        : maxHeight;
            }
        };
        switch (dimension.getAnchor().dim) {
            case DIM_NORMAL:
                defaultBiome = BIOME_PLAINS;
                break;
            case DIM_NETHER:
                defaultBiome = BIOME_HELL;
                break;
            case DIM_END:
                defaultBiome = BIOME_SKY;
                break;
            default:
                throw new InternalError();
        }
    }

    // FirstPassLayerExporter

    @Override
    public void render(Tile surfaceTile, Chunk chunk) {
        final Tile floatingTile = dimension.getTile(surfaceTile.getX(), surfaceTile.getY());
        final FirstPassLayerExporter[] exporters = getFirstPassExportersForTile(floatingTile);
        final int xOffset = (chunk.getxPos() & 7) << 4;
        final int yOffset = (chunk.getzPos() & 7) << 4;
        final Random random = new Random(seed + xOffset * 3 + yOffset * 5);
        final BiomeUtils biomeUtils = new BiomeUtils(dimension);
        final boolean applyBiomesAboveGround = layer.isApplyBiomesAboveGround(), applyBiomesBelowGround = layer.isApplyBiomesBelowGround();
        final int undergroundBiome = (dimension.getUndergroundBiome() != null) ? dimension.getUndergroundBiome() : -1;
        final int surfaceBiomeHeight = applyBiomesAboveGround ? layer.getBiomeHeightAboveGround() : 0;
        final boolean applyBiomes = (chunk.is3DBiomesSupported() || chunk.isNamedBiomesSupported()) && (applyBiomesBelowGround || applyBiomesAboveGround);
        for (int x = 0; x < 16; x++) {
            final int localX = xOffset + x;
            final int worldX = (chunk.getxPos() << 4) + x;
            for (int y = 0; y < 16; y++) {
                final int localY = yOffset + y;
                if (! surfaceTile.getBitLayerValue(layer, localX, localY)) {
                    continue;
                }

                // Create terrain
                final int xInTile = xOffset | x;
                final int yInTile = yOffset | y;
                if (! floatingTile.getBitLayerValue(org.pepsoft.worldpainter.layers.Void.INSTANCE, xInTile, yInTile)) {
                    final int worldY = (chunk.getzPos() << 4) + y;
                    final int terrainHeight = floatingTile.getIntHeight(localX, localY);
                    final int floorLevel = helper.calculateFloorLevel(worldX, worldY, terrainHeight, minZ, maxZ);
                    final int bottomLevel = helper.calculateBottomLevel(worldX, worldY, minZ, maxZ, floorLevel, helper.getDistanceToWall(worldX, worldY));
                    final int intHeight = floatingTile.getIntHeight(xInTile, yInTile);
                    final int waterLevel = floatingTile.getWaterLevel(xInTile, yInTile);
                    final boolean underWater = waterLevel > intHeight;
                    final Terrain terrain = floatingTile.getTerrain(xInTile, yInTile);
                    final boolean floodWithLava;
                    if (underWater) {
                        floodWithLava = floatingTile.getBitLayerValue(FloodWithLava.INSTANCE, xInTile, yInTile);
//                        result.stats.waterArea++; TODO?
                    } else {
                        floodWithLava = false;
//                        result.stats.landArea++; TODO?
                    }
                    wpChunkFactory.applySubSurface(floatingTile, chunk, xInTile, yInTile, bottomLevel);
                    wpChunkFactory.applyTopLayer(floatingTile, chunk, xInTile, yInTile, bottomLevel, false);
                    if (! underWater) {
                        // Above the surface on dry land
                        WPObject object = null;
                        if (((terrain == Terrain.GRASS) || (terrain == Terrain.DESERT) || (terrain == Terrain.RED_DESERT) || (terrain == Terrain.BEACHES))
                                && ((sugarCaneNoise.getPerlinNoise(worldX / TINY_BLOBS, worldY / TINY_BLOBS, y / TINY_BLOBS) * sugarCaneNoise.getPerlinNoise(worldX / SMALL_BLOBS, worldY / SMALL_BLOBS, y / SMALL_BLOBS)) > SUGAR_CANE_CHANCE)
                                && (wpChunkFactory.isAdjacentWater(floatingTile, intHeight, xInTile - 1, yInTile)
                                || wpChunkFactory.isAdjacentWater(floatingTile, intHeight, xInTile + 1, yInTile)
                                || wpChunkFactory.isAdjacentWater(floatingTile, intHeight, xInTile, yInTile - 1)
                                || wpChunkFactory.isAdjacentWater(floatingTile, intHeight, xInTile, yInTile + 1))) {
                            final int blockTypeBelow = chunk.getBlockType(x, intHeight, y);
                            if ((random.nextInt(5) > 0) && ((blockTypeBelow == BLK_GRASS) || (blockTypeBelow == BLK_DIRT) || (blockTypeBelow == BLK_SAND) || (blockTypeBelow == BLK_SUGAR_CANE))) {
                                object = SUGAR_CANE.realise(random.nextInt(3) + 1, platform);
                            }
                        }
                        if (object == null) {
                            object = terrain.getSurfaceObject(platform, seed, worldX, worldY, 0);
                        }
                        if (object != null) {
                            wpChunkFactory.renderObject(chunk, object, x, intHeight + 1, y);
                        }
                    } else if (! floodWithLava) {
                        final WPObject object = terrain.getSurfaceObject(platform, seed, worldX, worldY, waterLevel - intHeight);
                        if (object != null) {
                            wpChunkFactory.renderObject(chunk, object, x, intHeight + 1, y);
                        }
                    }

                    if (applyBiomes) {
                        // TODO this is extremely inefficient, since we are doing this 64 times too many, but it is by
                        //  far the simplest way to ensure the whole extent of the underground is covered
                        applyBiomes(applyBiomesBelowGround, undergroundBiome, surfaceBiomeHeight, chunk, x, y, bottomLevel, biomeUtils);
                    }
                }
            }
        }

        // Apply first pass layers
        for (FirstPassLayerExporter exporter: exporters) {
            exporter.render(floatingTile, chunk, minHeightField);
        }
    }

    // SecondPassLayerExporter

    @Override
    public Set<Stage> getStages() {
        return singleton(ADD_FEATURES); // Implemented in superclass
    }

    public static BufferedImage generatePreview(Dimension backgroundDimension, TunnelLayer layer, int width, int height, int waterLevel, int minHeight, int baseHeight, int range) {
        if (layer.getLayerMode() != FLOATING) {
            throw new IllegalArgumentException("Layer must be in mode FLOATING");
        }
        // TODO - what?
        final int tunnelExtent = width - 34;
        final boolean floodWithLava = layer.isFloodWithLava();
        final PerlinNoise noise = new PerlinNoise(0);
        final BufferedImage preview = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        final TunnelLayerHelper helper = layer.getHelper(null);
        final boolean drawBackgroundDimension = (backgroundDimension != null) && (backgroundDimension.getTileFactory() instanceof HeightMapTileFactory);
        final int bgFluidLevel, bgFluidColour;
        final HeightMap bgHeightMap;
        if (drawBackgroundDimension) {
            HeightMapTileFactory tileFactory = (HeightMapTileFactory) backgroundDimension.getTileFactory();
            bgFluidLevel = tileFactory.getWaterHeight();
            bgHeightMap = tileFactory.getHeightMap();
            bgFluidColour = tileFactory.isFloodWithLava() ? 0xfef0be : 0xafafff;
        } else {
            bgFluidLevel = bgFluidColour = Integer.MIN_VALUE;
            bgHeightMap = null;
        }
        for (int x = 0; x < width; x++) {
            // Clear the sky and draw the background
            final int bgTerrainHeight = bgHeightMap != null ? (int) round(bgHeightMap.getHeight(x, 0)) : Integer.MIN_VALUE;
            for (int z = height - 1 + minHeight; z >= minHeight; z--) {
                if (z <= bgTerrainHeight) {
                    // Background terrain
                    preview.setRGB(x, height - 1 - z + minHeight, 0xafafaf);
                } else if (z <= bgFluidLevel) {
                    // Background water/lava
                    preview.setRGB(x, height - 1 - z + minHeight, bgFluidColour);
                } else {
                    // Sky
                    preview.setRGB(x, height - 1 - z + minHeight, 0xffffff);
                }
            }

            if (x <= tunnelExtent) {
                // Draw the floating dimension
                final int actualFloorLevel = baseHeight + round((noise.getPerlinNoise(x / LARGE_BLOBS) + 0.5f) * range);
                final float distanceToWall = tunnelExtent - x;
                final int actualBottomLevel = helper.calculateBottomLevel(x, 0, minHeight + 1, height - 1, actualFloorLevel, distanceToWall);
                if (actualBottomLevel <= actualFloorLevel) {
                    // TODO adjust for edge width and shape
                    for (int z = Math.max(actualFloorLevel, waterLevel); z >= actualBottomLevel; z--) {
                        if (z <= actualFloorLevel) {
                            preview.setRGB(x, height - 1 - z + minHeight, 0x000000);
                        } else {
                            if (floodWithLava) {
                                preview.setRGB(x, height - 1 - z + minHeight, 0xff8000);
                            } else {
                                preview.setRGB(x, height - 1 - z + minHeight, 0x0000ff);
                            }
                        }
                    }
                }
            }
        }
        // Add height markers
        final Graphics2D g2 = preview.createGraphics();
        try {
            g2.setColor(BLACK);
            g2.setFont(HEIGHT_MARKER_FONT);
            for (int y = (minHeight / 20) * 20; y < (height + minHeight); y += 20) {
                g2.drawLine(width - 10, height + minHeight - y, width - 1, height + minHeight - y);
                g2.drawString(Integer.toString(y), width - 30, height + minHeight - y + 4);
            }
        } finally {
            g2.dispose();
        }
        return preview;
    }

    private FirstPassLayerExporter[] getFirstPassExportersForTile(Tile tile) {
        return exporterCache.computeIfAbsent(new Point(tile.getX(), tile.getY()), k -> {
            final SortedSet<Layer> floorLayers = new TreeSet<>(tile.getLayers());
            floorLayers.addAll(dimension.getMinimumLayers());
            return floorLayers.stream()
                    .filter(layer -> (layer.getExporterType() != null) && FirstPassLayerExporter.class.isAssignableFrom(layer.getExporterType()))
                    .map(layer -> (FirstPassLayerExporter) layer.getExporter(dimension, platform, dimension.getLayerSettings(layer)))
                    .toArray(FirstPassLayerExporter[]::new);
        });
    }

    private void applyBiomes(boolean applyUndergroundBiomes, int undergroundBiome, int surfaceBiomeHeight, Chunk chunk, int x, int z, int bottomLevel, BiomeUtils biomeUtils) {
        final int height = dimension.getIntHeightAt(x, z);
        final int topLayerStart = height - dimension.getTopLayerDepth(x, z, height);
        int surfaceBiome = dimension.getLayerValueAt(Biome.INSTANCE, x, z);
        if (surfaceBiome == 255) {
            surfaceBiome = dimension.getAutoBiome(x, z, defaultBiome);
        }
        if (applyUndergroundBiomes) {
            if (undergroundBiome == -1) {
                undergroundBiome = surfaceBiome;
            }
            for (int y = bottomLevel; y < topLayerStart; y++) {
                biomeUtils.set3DBiome(chunk, x >> 2, y >> 2, z >> 2, undergroundBiome);
            }
        }
        if (surfaceBiomeHeight > 0) {
            final int maxY = Math.min(height + surfaceBiomeHeight, maxHeight);
            for (int y = topLayerStart; y < maxY; y++) {
                biomeUtils.set3DBiome(chunk, x >> 2, y >> 2, z >> 2, surfaceBiome);
            }
        }
    }

    private final long seed;
    private final WorldPainterChunkFactory wpChunkFactory;
    private final Map<Point, FirstPassLayerExporter[]> exporterCache = new HashMap<>();
    private final PerlinNoise sugarCaneNoise = new PerlinNoise(0);
    private final HeightMap minHeightField;
    private final int maxZ, defaultBiome;

    private static final Logger logger = LoggerFactory.getLogger(FloatingLayerExporter.class);
}