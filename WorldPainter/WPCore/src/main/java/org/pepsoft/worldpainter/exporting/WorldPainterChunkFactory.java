/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.worldpainter.exporting;

import org.pepsoft.minecraft.ChunkFactory;
import org.pepsoft.minecraft.ChunkImpl2;
import org.pepsoft.minecraft.Material;
import org.pepsoft.util.PerlinNoise;
import org.pepsoft.worldpainter.*;
import org.pepsoft.worldpainter.layers.*;
import org.pepsoft.worldpainter.plugins.BlockBasedPlatformProvider;
import org.pepsoft.worldpainter.plugins.PlatformManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Random;
import java.util.Set;

import static org.pepsoft.minecraft.Constants.*;
import static org.pepsoft.worldpainter.Constants.*;
import static org.pepsoft.worldpainter.Platform.Capability.BIOMES;
import static org.pepsoft.worldpainter.biomeschemes.Minecraft1_7Biomes.*;

/**
 *
 * @author pepijn
 */
public class WorldPainterChunkFactory implements ChunkFactory {
    public WorldPainterChunkFactory(Dimension dimension, Map<Layer, LayerExporter> exporters, Platform platform, int maxHeight) {
        this.dimension = dimension;
        this.exporters = exporters;
        this.platform  = platform;
        platformProvider = (BlockBasedPlatformProvider) PlatformManager.getInstance().getPlatformProvider(platform);
        this.maxHeight = maxHeight;
        minimumLayers = dimension.getMinimumLayers();
    }

    @Override
    public int getMaxHeight() {
        return maxHeight;
    }

    @Override
    public ChunkCreationResult createChunk(int chunkX, int chunkZ) {
        Tile tile = dimension.getTile(chunkX >> 3, chunkZ >> 3);
        if (tile != null) {
            return createChunk(tile, chunkX, chunkZ);
        } else {
            return null;
        }
    }
    
    public ChunkCreationResult createChunk(Tile tile, int chunkX, int chunkZ) {
        if (tile.getBitLayerValue(ReadOnly.INSTANCE, (chunkX << 4) & TILE_SIZE_MASK, (chunkZ << 4) & TILE_SIZE_MASK)
                || tile.getBitLayerValue(NotPresent.INSTANCE, (chunkX << 4) & TILE_SIZE_MASK, (chunkZ << 4) & TILE_SIZE_MASK)) {
            // Chunk is marked as read-only or not present; don't export or
            // merge it
            return null;
        }
        final long seed = dimension.getSeed();
        if (sugarCaneNoise.getSeed() != (seed + SUGAR_CANE_SEED_OFFSET)) {
            sugarCaneNoise.setSeed(seed + SUGAR_CANE_SEED_OFFSET);
        }
        final Terrain subsurfaceMaterial = dimension.getSubsurfaceMaterial();
        final boolean dark = dimension.isDarkLevel();
        final boolean bedrock = ! dimension.isBottomless();
        final boolean coverSteepTerrain = dimension.isCoverSteepTerrain();
        final boolean topLayersRelativeToTerrain = dimension.getTopLayerAnchor() == Dimension.LayerAnchor.TERRAIN;
        final boolean subSurfaceLayersRelativeToTerrain =
            subsurfaceMaterial.isCustom()
            && (Terrain.getCustomMaterial(subsurfaceMaterial.getCustomTerrainIndex()).getMode() == MixedMaterial.Mode.LAYERED)
            && (dimension.getSubsurfaceLayerAnchor() == Dimension.LayerAnchor.TERRAIN);
        final int subSurfacePatternHeight = subSurfaceLayersRelativeToTerrain ? Terrain.getCustomMaterial(subsurfaceMaterial.getCustomTerrainIndex()).getPatternHeight() : -1;

        final int tileX = tile.getX();
        final int tileY = tile.getY();
        final int xOffsetInTile = (chunkX & 7) << 4;
        final int yOffsetInTile = (chunkZ & 7) << 4;
        final Random random = new Random(seed + xOffsetInTile * 3 + yOffsetInTile * 5);
        final boolean populate = dimension.isPopulate() || tile.getBitLayerValue(Populate.INSTANCE, xOffsetInTile, yOffsetInTile);
        final ChunkCreationResult result = new ChunkCreationResult();
        result.chunk = platformProvider.createChunk(platform, chunkX, chunkZ, maxHeight);
        final int maxY = maxHeight - 1;
        final boolean biomesSupported = platform.capabilities.contains(BIOMES);
        final boolean copyBiomes = biomesSupported && (dimension.getDim() == DIM_NORMAL);
        final int defaultBiome;
        switch (dimension.getDim()) {
            case DIM_NORMAL:
                defaultBiome = BIOME_PLAINS;
                break;
            case DIM_NETHER:
                defaultBiome = BIOME_HELL;
                break;
            case DIM_END:
                defaultBiome = BIOME_SKY;
                break;
            case DIM_NORMAL_CEILING:
            case DIM_NETHER_CEILING:
            case DIM_END_CEILING:
                // Biome is ignored for ceilings
                defaultBiome = 0;
                break;
            default:
                throw new InternalError();
        }
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                final int xInTile = xOffsetInTile + x;
                final int yInTile = yOffsetInTile + z;
                final int worldX = tileX * TILE_SIZE + xInTile;
                final int worldY = tileY * TILE_SIZE + yInTile;

                if (copyBiomes) {
                    int biome = dimension.getLayerValueAt(Biome.INSTANCE, worldX, worldY);
                    if (biome == 255) {
                        biome = dimension.getAutoBiome(tile, xInTile, yInTile);
                        if ((biome < 0) || (biome > 255)) {
                            biome = defaultBiome;
                        }
                    }
                    result.chunk.setBiome(x, z, biome);
                } else if (biomesSupported && (result.chunk instanceof ChunkImpl2)) {
                    result.chunk.setBiome(x, z, defaultBiome);
                }

                final float height = tile.getHeight(xInTile, yInTile);
                final int intHeight = (int) (height + 0.5f);
                final int waterLevel = tile.getWaterLevel(xInTile, yInTile);
                final boolean _void = tile.getBitLayerValue(org.pepsoft.worldpainter.layers.Void.INSTANCE, xInTile, yInTile);
                if (! _void) {
                    final Terrain terrain = tile.getTerrain(xInTile, yInTile);
                    final int topLayerDepth = dimension.getTopLayerDepth(worldX, worldY, intHeight);
                    final boolean floodWithLava;
                    final boolean underWater = waterLevel > intHeight;
                    final int subSurfaceLayerOffset = subSurfaceLayersRelativeToTerrain ? -(intHeight - subSurfacePatternHeight + 1) : 0;
                    final int topLayerLayerOffset;
                    if (topLayersRelativeToTerrain
                            && terrain.isCustom()
                            && (Terrain.getCustomMaterial(terrain.getCustomTerrainIndex()).getMode() == MixedMaterial.Mode.LAYERED)) {
                        topLayerLayerOffset = -(intHeight - Terrain.getCustomMaterial(terrain.getCustomTerrainIndex()).getPatternHeight() + 1);
                    } else {
                        topLayerLayerOffset = 0;
                    }
                    if (underWater) {
                        floodWithLava = tile.getBitLayerValue(FloodWithLava.INSTANCE, xInTile, yInTile);
                        result.stats.waterArea++;
                    } else {
                        floodWithLava = false;
                        result.stats.landArea++;
                    }
                    if (bedrock) {
                        result.chunk.setBlockType(x, 0, z, BLK_BEDROCK);
                    }
                    int subsurfaceMaxHeight = intHeight - topLayerDepth;
                    if (coverSteepTerrain) {
                        subsurfaceMaxHeight = Math.min(subsurfaceMaxHeight,
                            Math.min(Math.min(dimension.getIntHeightAt(worldX - 1, worldY, Integer.MAX_VALUE),
                            dimension.getIntHeightAt(worldX + 1, worldY, Integer.MAX_VALUE)),
                            Math.min(dimension.getIntHeightAt(worldX, worldY - 1, Integer.MAX_VALUE),
                            dimension.getIntHeightAt(worldX, worldY + 1, Integer.MAX_VALUE))));
                    }
                    int columnRenderHeight = Math.min(Math.max(intHeight + Math.max(terrain.getToppingHeight(), 3), waterLevel), maxY);
                    for (int y = bedrock ? 1 : 0; y <= columnRenderHeight; y++) {
                        if (y <= subsurfaceMaxHeight) {
                            // Sub surface
                            result.chunk.setMaterial(x, y, z, subsurfaceMaterial.getMaterial(seed, worldX, worldY, y + subSurfaceLayerOffset, intHeight + subSurfaceLayerOffset));
                        } else if (y < intHeight) {
                            // Top/terrain layer, but not surface block
                            result.chunk.setMaterial(x, y, z, terrain.getMaterial(seed, worldX, worldY, y + topLayerLayerOffset, intHeight + topLayerLayerOffset));
                        } else if (y == intHeight) {
                            // Surface block
                            final Material material;
                            if (topLayerLayerOffset != 0) {
                                material = terrain.getMaterial(seed, worldX, worldY, intHeight + topLayerLayerOffset, intHeight + topLayerLayerOffset);
                            } else {
                                // Use floating point height here to make sure
                                // undulations caused by layer variation settings/
                                // blobs, etc. look continuous on the surface
                                material = terrain.getMaterial(seed, worldX, worldY, height + topLayerLayerOffset, intHeight + topLayerLayerOffset);
                            }
                            final int blockType = material.blockType;
                            if (((blockType == BLK_WOODEN_SLAB) || (blockType == BLK_SLAB) || (blockType == BLK_RED_SANDSTONE_SLAB)) && (! underWater) && (height > intHeight)) {
                                result.chunk.setMaterial(x, y, z, Material.get(blockType - 1, material.data));
                            } else {
                                result.chunk.setMaterial(x, y, z, material);
                            }
                        } else if (y <= waterLevel) {
                            // Above the surface but below the water/lava level
                            if (floodWithLava) {
                                result.chunk.setBlockType(x, y, z, BLK_STATIONARY_LAVA);
                            } else {
                                result.chunk.setBlockType(x, y, z, BLK_STATIONARY_WATER);
                            }
                        } else if (! underWater) {
                            // Above the surface on dry land
                            if ((y > 0) && ((y - intHeight) <= 3) && ((terrain == Terrain.GRASS) || (terrain == Terrain.DESERT) || (terrain == Terrain.RED_DESERT) || (terrain == Terrain.BEACHES))
                                    && ((sugarCaneNoise.getPerlinNoise(worldX / TINY_BLOBS, worldY / TINY_BLOBS, z / TINY_BLOBS) * sugarCaneNoise.getPerlinNoise(worldX / SMALL_BLOBS, worldY / SMALL_BLOBS, z / SMALL_BLOBS)) > SUGAR_CANE_CHANCE)
                                    && (isAdjacentWater(tile, intHeight, xInTile - 1, yInTile)
                                        || isAdjacentWater(tile, intHeight, xInTile + 1, yInTile)
                                        || isAdjacentWater(tile, intHeight, xInTile, yInTile - 1)
                                        || isAdjacentWater(tile, intHeight, xInTile, yInTile + 1))) {
                                int blockTypeBelow = result.chunk.getBlockType(x, y - 1, z);
                                if ((random.nextInt(5) > 0) && ((blockTypeBelow == BLK_GRASS) || (blockTypeBelow == BLK_DIRT) || (blockTypeBelow == BLK_SAND) || (blockTypeBelow == BLK_SUGAR_CANE))) {
                                    result.chunk.setMaterial(x, y, z, Material.SUGAR_CANE);
                                } else {
                                    result.chunk.setMaterial(x, y, z, terrain.getMaterial(seed, worldX, worldY, y, intHeight));
                                }
                            } else {
                                result.chunk.setMaterial(x, y, z, terrain.getMaterial(seed, worldX, worldY, y, intHeight));
                            }
                        }
                    }
                }
                if (dark) {
                    result.chunk.setBlockType(x, maxY, z, BLK_BEDROCK);
                    result.chunk.setHeight(x, z, maxY);
                } else if (_void) {
                    result.chunk.setHeight(x, z, 0);
                } else if (waterLevel > intHeight) {
                    result.chunk.setHeight(x, z, (waterLevel < maxY) ? (waterLevel + 1): maxY);
                } else {
                    result.chunk.setHeight(x, z, (intHeight < maxY) ? (intHeight + 1): maxY);
                }
            }
        }
        if (! populate) {
            result.chunk.setTerrainPopulated(true);
        }
        for (Layer layer: tile.getLayers(minimumLayers)) {
            LayerExporter layerExporter = exporters.get(layer);
            if (layerExporter instanceof FirstPassLayerExporter) {
                if (logger.isTraceEnabled()) {
                    logger.trace("Exporting layer {} for chunk {},{}", layer, chunkX, chunkZ);
                }
                ((FirstPassLayerExporter) layerExporter).render(dimension, tile, result.chunk);
            }
        }
        result.stats.surfaceArea = 256;
        return result;
    }
    
    private boolean isAdjacentWater(Tile tile, int height, int x, int y) {
        if ((x < 0) || (x >= TILE_SIZE) || (y < 0) || (y >= TILE_SIZE)) {
            return false;
        }
        return (tile.getWaterLevel(x, y) == height)
                && (! tile.getBitLayerValue(FloodWithLava.INSTANCE, x, y))
                && (! tile.getBitLayerValue(Frost.INSTANCE, x, y))
                && (tile.getIntHeight(x, y) < height);
    }

    private final Platform platform;
    private final BlockBasedPlatformProvider platformProvider;
    private final int maxHeight;
    private final Dimension dimension;
    private final Set<Layer> minimumLayers;
    private final PerlinNoise sugarCaneNoise = new PerlinNoise(0);
    private final Map<Layer, LayerExporter> exporters;

    private static final long SUGAR_CANE_SEED_OFFSET = 127411424;
    private static final float SUGAR_CANE_CHANCE = PerlinNoise.getLevelForPromillage(325);
    private static final Logger logger = LoggerFactory.getLogger(WorldPainterChunkFactory.class);
}