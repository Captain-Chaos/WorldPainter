/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.worldpainter.exporting;

import org.pepsoft.minecraft.ChunkFactory;
import org.pepsoft.util.PerlinNoise;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.Dimension.Border;
import org.pepsoft.worldpainter.Platform;
import org.pepsoft.worldpainter.Terrain;
import org.pepsoft.worldpainter.Tile;
import org.pepsoft.worldpainter.layers.Layer;
import org.pepsoft.worldpainter.plugins.PlatformManager;
import org.pepsoft.worldpainter.util.BiomeUtils;

import java.util.Map;
import java.util.Set;

import static org.pepsoft.minecraft.Material.*;
import static org.pepsoft.worldpainter.Constants.*;
import static org.pepsoft.worldpainter.DefaultPlugin.*;
import static org.pepsoft.worldpainter.Terrain.BEACHES;
import static org.pepsoft.worldpainter.biomeschemes.Minecraft1_17Biomes.*;

/**
 *
 * @author pepijn
 */
public class BorderChunkFactory {
    public static ChunkFactory.ChunkCreationResult create(int chunkX, int chunkZ, Dimension dimension, Platform platform, Map<Layer, LayerExporter> exporters) {
        final int maxHeight = dimension.getMaxHeight();
        final Border border = dimension.getBorder();
        final int borderLevel = dimension.getBorderLevel();
        final boolean dark = dimension.isDarkLevel();
        final boolean bottomless = dimension.isBottomless();
        final Terrain subsurfaceMaterial = dimension.getSubsurfaceMaterial();
        final PerlinNoise noiseGenerator;
        if (noiseGenerators.get() == null) {
            noiseGenerator = new PerlinNoise(0);
            noiseGenerators.set(noiseGenerator);
        } else {
            noiseGenerator = noiseGenerators.get();
        }
        final long seed = dimension.getSeed();
        if (noiseGenerator.getSeed() != seed) {
            noiseGenerator.setSeed(seed);
        }
        final int floor = Math.max(borderLevel - 20, 0);
        final int variation = Math.min(15, (borderLevel - floor) / 2);
        final BiomeUtils biomeUtils = new BiomeUtils();

        final ChunkFactory.ChunkCreationResult result = new ChunkFactory.ChunkCreationResult();
        result.chunk = PlatformManager.getInstance().createChunk(platform, chunkX, chunkZ, maxHeight);
        final int maxY = maxHeight - 1;
        final int biome;
        switch (dimension.getDim()) {
            case DIM_NETHER:
            case DIM_NETHER_CEILING:
                biome = BIOME_HELL;
                break;
            case DIM_END:
            case DIM_END_CEILING:
                biome = BIOME_THE_END;
                break;
            default:
                switch (border) {
                    case VOID:
                        biome = ((platform == JAVA_ANVIL_1_15) || (platform == JAVA_ANVIL_1_17) || (platform == JAVA_ANVIL_1_18) /* TODO make dynamic */) ? BIOME_THE_VOID : BIOME_PLAINS;
                        break;
                    case WATER:
                        biome = BIOME_OCEAN;
                        break;
                    default:
                        biome = BIOME_PLAINS;
                        break;
                }
                break;
        }
        if (platform.supportsBiomes()) {
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    biomeUtils.set2DBiome(result.chunk, x, z, biome);
                }
            }
        }
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                if (border != Border.VOID) {
                    final int worldX = (chunkX << 4) | x, worldZ = (chunkZ << 4) | z;
                    final int floorLevel = (int) (floor + (noiseGenerator.getPerlinNoise(worldX / MEDIUM_BLOBS, worldZ / MEDIUM_BLOBS) + 0.5f) * variation + 0.5f);
                    final int surfaceLayerLevel = floorLevel - dimension.getTopLayerDepth(worldX, worldZ, floorLevel);
                    for (int y = 0; y <= maxY; y++) {
                        if ((y == 0) && (! bottomless)) {
                            result.chunk.setMaterial(x, y, z, BEDROCK);
                        } else if (y <= surfaceLayerLevel) {
                            result.chunk.setMaterial(x, y, z, subsurfaceMaterial.getMaterial(platform, seed, worldX, worldZ, y, floorLevel));
                        } else if (y <= floorLevel) {
                            result.chunk.setMaterial(x, y, z, BEACHES.getMaterial(platform, seed, worldX, worldZ, y, floorLevel));
                        } else if (y <= borderLevel) {
                            switch(border) {
                                case WATER:
                                    result.chunk.setMaterial(x, y, z, STATIONARY_WATER);
                                    break;
                                case LAVA:
                                    result.chunk.setMaterial(x, y, z, STATIONARY_LAVA);
                                    break;
                                default:
                                    // Do nothing
                            }
                        }
                    }
                }
                if (dark) {
                    result.chunk.setMaterial(x, maxY, z, BEDROCK);
                    result.chunk.setHeight(x, z, maxY);
                } else if (border == Border.VOID) {
                    result.chunk.setHeight(x, z, 0);
                } else {
                    result.chunk.setHeight(x, z, (borderLevel < maxY) ? (borderLevel + 1) : maxY);
                }
            }
        }

        if (border != Border.VOID) {
            // Apply layers set to be applied everywhere, if any
            final Set<Layer> minimumLayers = dimension.getMinimumLayers();
            if (!minimumLayers.isEmpty()) {
                Tile virtualTile = new Tile(chunkX >> 3, chunkZ >> 3, dimension.getMinHeight(), dimension.getMaxHeight()) {
                    @Override
                    public synchronized float getHeight(int x, int y) {
                        return floor + (noiseGenerator.getPerlinNoise(((getX() << TILE_SIZE_BITS) | x) / MEDIUM_BLOBS, ((getY() << TILE_SIZE_BITS) | y) / MEDIUM_BLOBS) + 0.5f) * variation;
                    }

                    @Override
                    public synchronized int getWaterLevel(int x, int y) {
                        return borderLevel;
                    }

                    private static final long serialVersionUID = 1L;
                };
                for (Layer layer: minimumLayers) {
                    LayerExporter layerExporter = exporters.get(layer);
                    if (layerExporter instanceof FirstPassLayerExporter) {
                        ((FirstPassLayerExporter) layerExporter).render(dimension, virtualTile, result.chunk, platform);
                    }
                }
            }
        }

        result.chunk.setTerrainPopulated(true);
        result.stats.surfaceArea = 256;
        if ((border == Border.WATER) || (border == Border.LAVA)) {
            result.stats.waterArea = 256;
        }
        return result;
    }

    private static final ThreadLocal<PerlinNoise> noiseGenerators = new ThreadLocal<>();
}