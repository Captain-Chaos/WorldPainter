/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.layers.exporters;

import org.pepsoft.minecraft.Chunk;
import org.pepsoft.util.PerlinNoise;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.Tile;
import org.pepsoft.worldpainter.exporting.AbstractLayerExporter;
import org.pepsoft.worldpainter.exporting.FirstPassLayerExporter;
import org.pepsoft.worldpainter.layers.Resources;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static org.pepsoft.minecraft.Constants.*;
import static org.pepsoft.worldpainter.Constants.*;

/**
 *
 * @author pepijn
 */
public class ResourcesExporter extends AbstractLayerExporter<Resources> implements FirstPassLayerExporter {
    public ResourcesExporter() {
        super(Resources.INSTANCE);
    }
    
    @Override
    public void setSettings(ExporterSettings settings) {
        super.setSettings(settings);
        ResourcesExporterSettings resourcesSettings = (ResourcesExporterSettings) getSettings();
        if (resourcesSettings != null) {
            noiseGenerators = new PerlinNoise[256];
            seedOffsets = new long[256];
            minLevels = new int[256];
            maxLevels = new int[256];
            chances = new float[256][16];
            activeOreCount = 0;
            for (int blockType: resourcesSettings.getBlockTypes()) {
                if (resourcesSettings.getChance(blockType) == 0) {
                    continue;
                }
                activeOreCount++;
                noiseGenerators[blockType] = new PerlinNoise(0);
                seedOffsets[blockType] = resourcesSettings.getSeedOffset(blockType);
                minLevels[blockType] = resourcesSettings.getMinLevel(blockType);
                maxLevels[blockType] = resourcesSettings.getMaxLevel(blockType);
                chances[blockType] = new float[16];
                for (int i = 0; i < 16; i++) {
                    chances[blockType][i] = PerlinNoise.getLevelForPromillage(Math.min(resourcesSettings.getChance(blockType) * i / 8f, 1000f));
                }
            }
        }
    }
    
    @Override
    public void render(Dimension dimension, Tile tile, Chunk chunk) {
        ResourcesExporterSettings settings = (ResourcesExporterSettings) getSettings();
        if (settings == null) {
            settings = new ResourcesExporterSettings(dimension.getMaxHeight());
            setSettings(settings);
        }
        
        final int minimumLevel = settings.getMinimumLevel();
        final int xOffset = (chunk.getxPos() & 7) << 4;
        final int zOffset = (chunk.getzPos() & 7) << 4;
        final long seed = dimension.getSeed();
        final int[] oreTypes = new int[activeOreCount];
        final int maxY = dimension.getMaxHeight() - 1;
        final boolean coverSteepTerrain = dimension.isCoverSteepTerrain();
        int i = 0;
        for (int oreType: settings.getBlockTypes()) {
            if (settings.getChance(oreType) == 0) {
                continue;
            }
            oreTypes[i++] = oreType;
        }
        if ((currentSeed == 0) || (currentSeed != seed)) {
            for (int blockType: oreTypes) {
                if (noiseGenerators[blockType].getSeed() != (seed + seedOffsets[blockType])) {
                    noiseGenerators[blockType].setSeed(seed + seedOffsets[blockType]);
                }
            }
        }
//        int[] counts = new int[256];
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                final int localX = xOffset + x, localY = zOffset + z;
                final int worldX = tile.getX() * TILE_SIZE + localX, worldY = tile.getY() * TILE_SIZE + localY;
                if (tile.getBitLayerValue(org.pepsoft.worldpainter.layers.Void.INSTANCE, localX, localY)) {
                    continue;
                }
                final int resourcesValue = Math.max(minimumLevel, tile.getLayerValue(Resources.INSTANCE, localX, localY));
                if (resourcesValue > 0) {
                    final int terrainheight = tile.getIntHeight(localX, localY);
                    final int topLayerDepth = dimension.getTopLayerDepth(worldX, worldY, terrainheight);
                    int subsurfaceMaxHeight = terrainheight - topLayerDepth;
                    if (coverSteepTerrain) {
                        subsurfaceMaxHeight = Math.min(subsurfaceMaxHeight,
                                Math.min(Math.min(dimension.getIntHeightAt(worldX - 1, worldY, Integer.MAX_VALUE),
                                        dimension.getIntHeightAt(worldX + 1, worldY, Integer.MAX_VALUE)),
                                        Math.min(dimension.getIntHeightAt(worldX, worldY - 1, Integer.MAX_VALUE),
                                                dimension.getIntHeightAt(worldX, worldY + 1, Integer.MAX_VALUE))));
                    }
                    final double dx = worldX / TINY_BLOBS, dy = worldY / TINY_BLOBS;
                    final double dirtX = worldX / SMALL_BLOBS, dirtY = worldY / SMALL_BLOBS;
                    // Capping to maxY really shouldn't be necessary, but we've
                    // had several reports from the wild of this going higher
                    // than maxHeight, so there must be some obscure way in
                    // which the terrainHeight can be raised too high
                    for (int y = Math.min(subsurfaceMaxHeight, maxY); y > 0; y--) {
                        final double dz = y / TINY_BLOBS;
                        final double dirtZ = y / SMALL_BLOBS;
                        for (int oreType: oreTypes) {
                            final float chance = chances[oreType][resourcesValue];
                            if ((chance <= 0.5f)
                                    && (y >= minLevels[oreType])
                                    && (y <= maxLevels[oreType])
                                    && (((oreType == BLK_DIRT) || (oreType == BLK_GRAVEL))
                                        ? (noiseGenerators[oreType].getPerlinNoise(dirtX, dirtY, dirtZ) >= chance)
                                        : (noiseGenerators[oreType].getPerlinNoise(dx, dy, dz) >= chance))) {
//                                counts[oreType]++;
                                chunk.setBlockType(x, y, z, oreType);
                                chunk.setDataValue(x, y, z, 0);
                                break;
                            }
                        }
                    }
                }
            }
        }
//        System.out.println("Tile " + tile.getX() + "," + tile.getY());
//        for (i = 0; i < 256; i++) {
//            if (counts[i] > 0) {
//                System.out.printf("Exported %6d of ore type %3d%n", counts[i], i);
//            }
//        }
//        System.out.println();
    }
    
//  TODO: resource frequenties onderzoeken met Statistics tool!

    private PerlinNoise[] noiseGenerators;
    private long[] seedOffsets;
    private int[] minLevels, maxLevels;
    private float[][] chances;
    private int activeOreCount;
    private long currentSeed;
    
    public static class ResourcesExporterSettings implements ExporterSettings {
        public ResourcesExporterSettings(int maxHeight) {
            this(maxHeight, false);
        }
        
        public ResourcesExporterSettings(int maxHeight, boolean nether) {
            minLevels.put(BLK_GOLD_ORE,         0);
            minLevels.put(BLK_IRON_ORE,         0);
            minLevels.put(BLK_COAL,             0);
            minLevels.put(BLK_LAPIS_LAZULI_ORE, 0);
            minLevels.put(BLK_DIAMOND_ORE,      0);
            minLevels.put(BLK_REDSTONE_ORE,     0);
            minLevels.put(BLK_WATER,            0);
            minLevels.put(BLK_LAVA,             0);
            minLevels.put(BLK_DIRT,             0);
            minLevels.put(BLK_GRAVEL,           0);
            minLevels.put(BLK_EMERALD_ORE,      0);
            minLevels.put(BLK_QUARTZ_ORE,       0);
            
            maxLevels.put(BLK_GOLD_ORE,         31);
            maxLevels.put(BLK_IRON_ORE,         63);
            maxLevels.put(BLK_COAL,             maxHeight - 1);
            maxLevels.put(BLK_LAPIS_LAZULI_ORE, 31);
            maxLevels.put(BLK_DIAMOND_ORE,      15);
            maxLevels.put(BLK_REDSTONE_ORE,     15);
            maxLevels.put(BLK_WATER,            maxHeight - 1);
            maxLevels.put(BLK_LAVA,             15);
            maxLevels.put(BLK_DIRT,             maxHeight - 1);
            maxLevels.put(BLK_GRAVEL,           maxHeight - 1);
            maxLevels.put(BLK_EMERALD_ORE,      31);
            maxLevels.put(BLK_QUARTZ_ORE,       maxHeight - 1);
            
            if (nether) {
                chances.put(BLK_GOLD_ORE,         0);
                chances.put(BLK_IRON_ORE,         0);
                chances.put(BLK_COAL,             0);
                chances.put(BLK_LAPIS_LAZULI_ORE, 0);
                chances.put(BLK_DIAMOND_ORE,      0);
                chances.put(BLK_REDSTONE_ORE,     0);
                chances.put(BLK_WATER,            0);
                chances.put(BLK_LAVA,             0);
                chances.put(BLK_DIRT,             0);
                chances.put(BLK_GRAVEL,           0);
                chances.put(BLK_EMERALD_ORE,      0);
                if (maxHeight != DEFAULT_MAX_HEIGHT_2) {
                    chances.put(BLK_QUARTZ_ORE,       0);
                } else {
                    chances.put(BLK_QUARTZ_ORE,       6);
                }
            } else {
                chances.put(BLK_GOLD_ORE,          1);
                chances.put(BLK_IRON_ORE,          6);
                chances.put(BLK_COAL,             10);
                chances.put(BLK_LAPIS_LAZULI_ORE,  1);
                chances.put(BLK_DIAMOND_ORE,       1);
                chances.put(BLK_REDSTONE_ORE,      8);
                chances.put(BLK_WATER,             1);
                chances.put(BLK_LAVA,              2);
                chances.put(BLK_DIRT,             57);
                chances.put(BLK_GRAVEL,           28);
                if (maxHeight != DEFAULT_MAX_HEIGHT_2) {
                    chances.put(BLK_EMERALD_ORE,   0);
                } else {
                    chances.put(BLK_EMERALD_ORE,   1);
                }
                chances.put(BLK_QUARTZ_ORE,        0);
            }
            
            Random random = new Random();
            for (int blockType: maxLevels.keySet()) {
                seedOffsets.put(blockType, random.nextLong());
            }
        }
        
        private ResourcesExporterSettings(int minimumLevel, Map<Integer, Integer> minLevels, Map<Integer, Integer> maxLevels, Map<Integer, Integer> chances, Map<Integer, Long> seedOffsets) {
            this.minimumLevel = minimumLevel;
            this.minLevels.putAll(minLevels);
            this.maxLevels.putAll(maxLevels);
            this.chances.putAll(chances);
            this.seedOffsets.putAll(seedOffsets);
        }
        
        @Override
        public boolean isApplyEverywhere() {
            return minimumLevel > 0;
        }

        public int getMinimumLevel() {
            return minimumLevel;
        }

        public void setMinimumLevel(int minimumLevel) {
            this.minimumLevel = minimumLevel;
        }
        
        public Set<Integer> getBlockTypes() {
            return maxLevels.keySet();
        }
        
        public int getMinLevel(int blockType) {
            return minLevels.get(blockType);
        }
        
        public void setMinLevel(int blockType, int minLevel) {
            minLevels.put(blockType, minLevel);
        }
        
        public int getMaxLevel(int blockType) {
            return maxLevels.get(blockType);
        }
        
        public void setMaxLevel(int blockType, int maxLevel) {
            maxLevels.put(blockType, maxLevel);
        }
        
        public int getChance(int blockType) {
            return chances.get(blockType);
        }
        
        public void setChance(int blockType, int chance) {
            chances.put(blockType, chance);
        }

        public long getSeedOffset(int blockType) {
            return seedOffsets.get(blockType);
        }
        
        @Override
        public Resources getLayer() {
            return Resources.INSTANCE;
        }

        @Override
        public ResourcesExporterSettings clone() {
            return new ResourcesExporterSettings(minimumLevel, minLevels, maxLevels, chances, seedOffsets);
        }
        
        private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
            in.defaultReadObject();
            
            // Fix static water and lava
            if (! maxLevels.containsKey(BLK_WATER)) {
                logger.warn("Fixing water and lava settings");
                maxLevels.put(BLK_WATER, maxLevels.get(BLK_STATIONARY_WATER));
                chances.put(BLK_WATER, chances.get(BLK_STATIONARY_WATER));
                seedOffsets.put(BLK_WATER, seedOffsets.get(BLK_STATIONARY_WATER));
                maxLevels.put(BLK_LAVA, maxLevels.get(BLK_STATIONARY_LAVA));
                chances.put(BLK_LAVA, chances.get(BLK_STATIONARY_LAVA));
                seedOffsets.put(BLK_LAVA, seedOffsets.get(BLK_STATIONARY_LAVA));
                maxLevels.remove(BLK_STATIONARY_WATER);
                chances.remove(BLK_STATIONARY_WATER);
                seedOffsets.remove(BLK_STATIONARY_WATER);
                maxLevels.remove(BLK_STATIONARY_LAVA);
                chances.remove(BLK_STATIONARY_LAVA);
                seedOffsets.remove(BLK_STATIONARY_LAVA);
            }
            if (! maxLevels.containsKey(BLK_EMERALD_ORE)) {
                maxLevels.put(BLK_EMERALD_ORE, 31);
                chances.put(BLK_EMERALD_ORE, 0);
            }
            Random random = new Random();
            if (! seedOffsets.containsKey(BLK_EMERALD_ORE)) {
                seedOffsets.put(BLK_EMERALD_ORE, random.nextLong());
            }
            if (minLevels == null) {
                minLevels = new HashMap<>();
                for (int blockType: maxLevels.keySet()) {
                    minLevels.put(blockType, 0);
                }
            }
            if (! minLevels.containsKey(BLK_QUARTZ_ORE)) {
                minLevels.put(BLK_QUARTZ_ORE, 0);
                maxLevels.put(BLK_QUARTZ_ORE, 255);
                chances.put(BLK_QUARTZ_ORE, 0);
                seedOffsets.put(BLK_QUARTZ_ORE, random.nextLong());
            }
        }
        
        private int minimumLevel = 8;
        private final Map<Integer, Integer> maxLevels = new HashMap<>();
        private final Map<Integer, Integer> chances = new HashMap<>();
        private final Map<Integer, Long> seedOffsets = new HashMap<>();
        private Map<Integer, Integer> minLevels = new HashMap<>();

        private static final long serialVersionUID = 1L;
        private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ResourcesExporter.class);
    }
}