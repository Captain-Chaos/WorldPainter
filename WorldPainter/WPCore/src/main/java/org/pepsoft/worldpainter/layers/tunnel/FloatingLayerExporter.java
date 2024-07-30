package org.pepsoft.worldpainter.layers.tunnel;

import org.pepsoft.minecraft.Chunk;
import org.pepsoft.util.PerlinNoise;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.Dimension.Anchor;
import org.pepsoft.worldpainter.*;
import org.pepsoft.worldpainter.exporting.FirstPassLayerExporter;
import org.pepsoft.worldpainter.exporting.SecondPassLayerExporter;
import org.pepsoft.worldpainter.exporting.WorldPainterChunkFactory;
import org.pepsoft.worldpainter.layers.FloodWithLava;
import org.pepsoft.worldpainter.layers.Layer;
import org.pepsoft.worldpainter.objects.WPObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;

import static java.awt.Color.ORANGE;
import static java.lang.Math.round;
import static java.util.Collections.singleton;
import static org.pepsoft.minecraft.Constants.*;
import static org.pepsoft.worldpainter.Constants.SMALL_BLOBS;
import static org.pepsoft.worldpainter.Constants.TINY_BLOBS;
import static org.pepsoft.worldpainter.Dimension.Role.FLOATING_FLOOR;
import static org.pepsoft.worldpainter.exporting.SecondPassLayerExporter.Stage.ADD_FEATURES;
import static org.pepsoft.worldpainter.exporting.WorldPainterChunkFactory.SUGAR_CANE_CHANCE;
import static org.pepsoft.worldpainter.exporting.WorldPainterChunkFactory.SUGAR_CANE_SEED_OFFSET;
import static org.pepsoft.worldpainter.layers.plants.Plants.SUGAR_CANE;
import static org.pepsoft.worldpainter.layers.tunnel.TunnelLayer.LayerMode.FLOATING;
import static org.pepsoft.worldpainter.util.PreviewUtils.paintHeightMarks;

public class FloatingLayerExporter extends AbstractTunnelLayerExporter implements FirstPassLayerExporter, SecondPassLayerExporter {
    public FloatingLayerExporter(Dimension dimension, Platform platform, TunnelLayer layer, TunnelLayerHelper helper) {
        super(dimension, platform, layer, helper);
        floorDimension = dimension.getWorld().getDimension(new Anchor(dimension.getAnchor().dim, FLOATING_FLOOR, dimension.getAnchor().invert, layer.getFloorDimensionId()));
        seed = dimension.getSeed();
        if (sugarCaneNoise.getSeed() != (seed + SUGAR_CANE_SEED_OFFSET)) {
            sugarCaneNoise.setSeed(seed + SUGAR_CANE_SEED_OFFSET);
        }
        wpChunkFactory = new WorldPainterChunkFactory(floorDimension, null, platform, dimension.getMaxHeight());
    }

    // FirstPassLayerExporter

    @Override
    public void render(Tile surfaceTile, Chunk chunk) {
        final Tile floatingTile = floorDimension.getTile(surfaceTile.getX(), surfaceTile.getY());
        final FirstPassLayerExporter[] exporters = getFirstPassExportersForTile(floatingTile);
        final int xOffset = (chunk.getxPos() & 7) << 4;
        final int yOffset = (chunk.getzPos() & 7) << 4;
        final Random random = new Random(seed + xOffset * 3 + yOffset * 5);
        for (int x = 0; x < 16; x++) {
            final int localX = xOffset + x;
            final int worldX = (chunk.getxPos() << 4) + x;
            for (int y = 0; y < 16; y++) {
                final int localY = yOffset + y;
                if (! surfaceTile.getBitLayerValue(layer, localX, localY)) {
                    continue;
                }
                final int worldY = (chunk.getzPos() << 4) + y;
                final int terrainHeight = floatingTile.getIntHeight(localX, localY);
                final int floorLevel = helper.calculateFloorLevel(worldX, worldY, terrainHeight, minZ, maxZ);
                final int bottomLevel = helper.calculateBottomLevel(worldX, worldY, minZ, maxZ, floorLevel, helper.getDistanceToWall(worldX, worldY));

                // Create terrain
                final int xInTile = xOffset | x;
                final int yInTile = yOffset | y;

                final int intHeight = floatingTile.getIntHeight(xInTile, yInTile);
                final int waterLevel = floatingTile.getWaterLevel(xInTile, yInTile);
                final boolean underWater = waterLevel > intHeight;
                final boolean _void = floatingTile.getBitLayerValue(org.pepsoft.worldpainter.layers.Void.INSTANCE, xInTile, yInTile);
                if (! _void) {
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
                }

                // Apply first pass layers
                for (FirstPassLayerExporter exporter: exporters) {
                    exporter.render(floatingTile, chunk);
                }

                // TODO add 3D biome support
            }
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
        // TODO
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
            bgFluidColour = tileFactory.isFloodWithLava() ? ORANGE.getRGB() : 0x7f7fff;
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
                    preview.setRGB(x, height - 1 - z + minHeight, 0x7f7f7f);
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
                final int actualFloorLevel = baseHeight + round(noise.getPerlinNoise(x) * range);
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
        paintHeightMarks(preview, minHeight);
        return preview;
    }

    private FirstPassLayerExporter[] getFirstPassExportersForTile(Tile tile) {
        return exporterCache.computeIfAbsent(new Point(tile.getX(), tile.getY()), k -> {
            final SortedSet<Layer> floorLayers = new TreeSet<>(tile.getLayers());
            floorLayers.addAll(floorDimension.getMinimumLayers());
            return floorLayers.stream()
                    .filter(layer -> (layer.getExporterType() != null) && FirstPassLayerExporter.class.isAssignableFrom(layer.getExporterType()))
                    .map(layer -> (FirstPassLayerExporter) layer.getExporter(floorDimension, platform, floorDimension.getLayerSettings(layer)))
                    .toArray(FirstPassLayerExporter[]::new);
        });
    }

    private final Dimension floorDimension;
    private final long seed;
    private final WorldPainterChunkFactory wpChunkFactory;
    private final Map<Point, FirstPassLayerExporter[]> exporterCache = new HashMap<>();
    private final PerlinNoise sugarCaneNoise = new PerlinNoise(0);

    private static final Logger logger = LoggerFactory.getLogger(FloatingLayerExporter.class);
}