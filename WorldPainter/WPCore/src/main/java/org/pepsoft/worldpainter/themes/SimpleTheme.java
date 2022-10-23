/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.themes;

import com.google.common.collect.ImmutableMap;
import org.pepsoft.util.PerlinNoise;
import org.pepsoft.worldpainter.HeightTransform;
import org.pepsoft.worldpainter.Terrain;
import org.pepsoft.worldpainter.Tile;
import org.pepsoft.worldpainter.layers.Frost;
import org.pepsoft.worldpainter.layers.Layer;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.*;

import static org.pepsoft.util.MathUtils.clamp;
import static org.pepsoft.worldpainter.Constants.SMALL_BLOBS;
import static org.pepsoft.worldpainter.Constants.TINY_BLOBS;

/**
 *
 * @author SchmitzP
 */
public class SimpleTheme implements Theme, Cloneable {
    @Deprecated
    public SimpleTheme(long seed, int waterHeight, Terrain[] terrainRangesTable, int minHeight, int maxHeight, boolean randomise, boolean beaches) {
        setSeed(seed);
        setWaterHeight(waterHeight);
        this.minHeight = minHeight;
        this.maxHeight = terrainRangesTable.length;
        this.terrainRangesTable = terrainRangesTable;
        fixTerrainRangesTable();
        setMinMaxHeight(minHeight, maxHeight, HeightTransform.IDENTITY);
        setRandomise(randomise);
        setBeaches(beaches);
    }
    
    public SimpleTheme(long seed, int waterHeight, SortedMap<Integer, Terrain> terrainRanges, Map<Filter, Layer> layerMap, int minHeight, int maxHeight, boolean randomise, boolean beaches) {
        setSeed(seed);
        setWaterHeight(waterHeight);
        this.minHeight = minHeight;
        this.maxHeight = maxHeight;
        this.terrainRangesTable = new Terrain[maxHeight - minHeight];
        setTerrainRanges(terrainRanges);
        setLayerMap(layerMap);
        setRandomise(randomise);
        setBeaches(beaches);
    }

    @Override
    public void apply(Tile tile, int x, int y) {
        int height = tile.getIntHeight(x, y);
        Terrain terrain = getTerrain(x, y, clamp(minHeight, height, maxHeight - 1));
        // Sanity checks because of NPE's observed in the wild from this method
        if (terrain == null) {
            throw new NullPointerException("apply(" + tile + ", " + x + ", " + y + ": getTerrain() returned null for " + this);
        }
        if (tile.getTerrain(x, y) != terrain) {
            tile.setTerrain(x, y, terrain);
        }
        if (layerCache != null) {
            for (int i = 0; i < layerCache.length; i++) {
                int level = layerLevelCache[i][height - minHeight];
                if (level != tile.getLayerValue(layerCache[i], x, y)) {
                    tile.setLayerValue(layerCache[i], x, y, level);
                }
            }
        }
        if (bitLayerCache != null) {
            for (int i = 0; i < bitLayerCache.length; i++) {
                int level = bitLayerLevelCache[i][height - minHeight];
                boolean set = (level > 0) && ((level == 15) || (random.nextInt(15) < level));
                if (set != tile.getBitLayerValue(bitLayerCache[i], x, y)) {
                    tile.setBitLayerValue(bitLayerCache[i], x, y, set);
                }
            }
        }
    }

    @Override
    public final long getSeed() {
        return seed;
    }

    @Override
    public final void setSeed(long seed) {
        this.seed = seed;
        perlinNoise = new PerlinNoise(seed);
    }

    public final SortedMap<Integer, Terrain> getTerrainRanges() {
        return terrainRanges;
    }

    public final void setTerrainRanges(SortedMap<Integer, Terrain> terrainRanges) {
        if (terrainRanges == null) {
            throw new NullPointerException("terrainRanges");
        } else if (terrainRanges.isEmpty()) {
            throw new IllegalArgumentException("terrainRanges may not be empty");
        } else {
            // This has been observed to happen in the wild: TODO find out why and fix the underlying cause
            for (Map.Entry<Integer, Terrain> entry: terrainRanges.entrySet()) {
                if (entry.getKey() == null || entry.getValue() == null) {
                    throw new IllegalArgumentException("terrainRanges may not contain null values: " + terrainRanges);
                }
            }
        }
        // Make sure the ranges actually start from the lowest level
        final int lowestLevel = terrainRanges.firstKey();
        if (lowestLevel >= minHeight) {
            Terrain lowestTerrain = terrainRanges.get(lowestLevel);
            terrainRanges.remove(lowestLevel);
            terrainRanges.put(minHeight - 1, lowestTerrain);
        }
        this.terrainRanges = terrainRanges;
        for (int i = minHeight; i < maxHeight; i++) {
            terrainRangesTable[i - minHeight] = terrainRanges.get(terrainRanges.headMap(i).lastKey());
        }
    }

    public final boolean isRandomise() {
        return randomise;
    }

    public final void setRandomise(boolean randomise) {
        this.randomise = randomise;
    }

    @Override
    public final int getWaterHeight() {
        return waterHeight;
    }

    @Override
    public final void setWaterHeight(int waterHeight) {
        this.waterHeight = waterHeight;
    }

    public final boolean isBeaches() {
        return beaches;
    }

    public final void setBeaches(boolean beaches) {
        this.beaches = beaches;
    }

    @Override
    public int getMinHeight() {
        return minHeight;
    }

    @Override
    public final int getMaxHeight() {
        return maxHeight;
    }

    @Override
    public final void setMinMaxHeight(int minHeight, int maxHeight, HeightTransform transform) {
        if ((minHeight != this.minHeight) || (maxHeight != this.maxHeight) || (! transform.isIdentity())) {
            final int oldMinHeight = this.minHeight, oldMaxHeight = this.maxHeight;
            this.minHeight = minHeight;
            this.maxHeight = maxHeight;
            waterHeight = clamp(minHeight, transform.transformHeight(waterHeight), maxHeight - 1);
            Terrain[] oldTerrainRangesTable = terrainRangesTable;
            terrainRangesTable = new Terrain[maxHeight - minHeight];
            if (terrainRanges != null) {
                SortedMap<Integer, Terrain> oldTerrainRanges = this.terrainRanges;
                terrainRanges = new TreeMap<>();
                for (Map.Entry<Integer, Terrain> oldEntry: oldTerrainRanges.entrySet()) {
                    terrainRanges.put(extendOrClamp(oldMinHeight, minHeight, oldEntry.getKey(), transform, maxHeight - 1, oldMaxHeight - 1), oldEntry.getValue());
                }
                // Make sure the ranges actually start from the lowest level
                final int lowestLevel = terrainRanges.firstKey();
                if (lowestLevel >= minHeight) {
                    Terrain lowestTerrain = terrainRanges.get(lowestLevel);
                    terrainRanges.remove(lowestLevel);
                    terrainRanges.put(minHeight - 1, lowestTerrain);
                }
                for (int i = minHeight; i < maxHeight; i++) {
                    terrainRangesTable[i - minHeight] = terrainRanges.get(terrainRanges.headMap(i).lastKey());
                }
            } else {
                // No terrain ranges map set; this is probably because it is
                // an old map. All we can do is extend the last entry
                // TODO: this won't work correctly if minHeight changed; do we still need to worry about that?
                System.arraycopy(oldTerrainRangesTable, 0, terrainRangesTable, 0, Math.min(oldTerrainRangesTable.length, terrainRangesTable.length));
                if (terrainRangesTable.length > oldTerrainRangesTable.length) {
                    for (int i = oldTerrainRangesTable.length; i < terrainRangesTable.length; i++) {
                        terrainRangesTable[i] = oldTerrainRangesTable[oldTerrainRangesTable.length - 1];
                    }
                }
            }
            if (layerMap != null) {
                final Map<Filter, Layer> newLayerMap = new HashMap<>();
                for (Map.Entry<Filter, Layer> entry: layerMap.entrySet()) {
                    Filter filter = entry.getKey();
                    if (filter instanceof HeightFilter) {
                        final HeightFilter heightFilter = (HeightFilter) filter;
                        filter = new HeightFilter(minHeight, maxHeight,
                                extendOrClamp(oldMinHeight, minHeight, heightFilter.getStartHeight(), transform, maxHeight, oldMaxHeight),
                                extendOrClamp(oldMinHeight, minHeight, heightFilter.getStopHeight(), transform, maxHeight, oldMaxHeight),
                                heightFilter.isFeather());
                    }
                    newLayerMap.put(filter, entry.getValue());
                }
                layerMap = newLayerMap;
            }
            initCaches();
        }
    }

    public final Map<Filter, Layer> getLayerMap() {
        return layerMap;
    }

    public final void setLayerMap(Map<Filter, Layer> layerMap) {
        this.layerMap = layerMap;
        initCaches();
    }

    public final Map<Layer, Integer> getDiscreteValues() {
        return discreteValues;
    }

    public final void setDiscreteValues(Map<Layer, Integer> discreteValues) {
        this.discreteValues = discreteValues;
        initCaches();
    }

    @Override
    public Theme clone() {
        try {
            SimpleTheme clone = (SimpleTheme) super.clone();
            if (terrainRanges != null) {
                clone.terrainRanges = new TreeMap<>(terrainRanges);
            }
            clone.terrainRangesTable = terrainRangesTable.clone();
            if (layerMap != null) {
                clone.setLayerMap(new HashMap<>(layerMap));
            }
            clone.perlinNoise = (PerlinNoise) perlinNoise.clone();
            return clone;
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String toString() {
        return "SimpleTheme{" +
                "seed=" + seed +
                ", waterHeight=" + waterHeight +
                ", minHeight=" + minHeight +
                ", maxHeight=" + maxHeight +
                ", terrainRanges=" + terrainRanges +
                ", randomise=" + randomise +
                ", beaches=" + beaches +
                ", layerMap=" + layerMap +
                ", discreteValues=" + discreteValues +
                '}';
    }

    protected Terrain getTerrain(int x, int y, int height) {
        if (beaches && (height >= (waterHeight - 2)) && (height <= (waterHeight + 1))) {
            return Terrain.BEACHES;
        } else {
            if (isRandomise()) {
                height += perlinNoise.getPerlinNoise(x / SMALL_BLOBS, y / SMALL_BLOBS, height / SMALL_BLOBS) * 5;
                height += perlinNoise.getPerlinNoise(x / TINY_BLOBS, y / TINY_BLOBS, height / TINY_BLOBS) * 5;
                return terrainRangesTable[clamp(minHeight, height, maxHeight - 1) - minHeight];
            } else {
                return terrainRangesTable[height - minHeight];
            }
        }
    }

    protected int extendOrClamp(int oldMin, int min, int value, HeightTransform transform, int max, int oldMax) {
        if (value < oldMin) {
            return min - 1;
        } else if (value == oldMin) {
            return min;
        } else if (value == oldMax) {
            return max;
        } else if (value > oldMax) {
            return max + 1;
        } else {
            return clamp(min, transform.transformHeight(value), max);
        }
    }

    private void initCaches() {
        if (layerMap != null) {
            List<Layer> layers = new ArrayList<>(layerMap.size());
            List<Layer> bitLayers = new ArrayList<>(layerMap.size());
            List<int[]> layerLevels = new ArrayList<>(layerMap.size());
            List<int[]> bitLayerLevels = new ArrayList<>(layerMap.size());
            layerLevelCache = new int[layerMap.size()][maxHeight - minHeight];
            for (Map.Entry<Filter, Layer> entry: layerMap.entrySet()) {
                Layer layer = entry.getValue();
                Filter filter = entry.getKey();
                int[] levels = new int[maxHeight - minHeight];
                for (int z = minHeight; z < maxHeight; z++) {
                    final int filterLevel = filter.getLevel(0, 0, z, 15);
                    if ((discreteValues != null) && discreteValues.containsKey(layer)) {
                        levels[z - minHeight] = (filterLevel >= 0.5) ? discreteValues.get(layer) : layer.getDefaultValue();
                    } else {
                        levels[z - minHeight] = filterLevel;
                    }
                }
                switch (layer.getDataSize()) {
                    case BIT:
                    case BIT_PER_CHUNK:
                        bitLayers.add(layer);
                        bitLayerLevels.add(levels);
                        break;
                    case NIBBLE:
                        layers.add(layer);
                        layerLevels.add(levels);
                        break;
                    case BYTE:
                        if (! layer.discrete) {
                            throw new IllegalArgumentException("Layer with unsupported data size " + layer.getDataSize() + " encountered");
                        }
                        layers.add(layer);
                        layerLevels.add(levels);
                        break;
                    default:
                        throw new IllegalArgumentException("Layer with unsupported data size " + layer.getDataSize() + " encountered");
                }
            }
            if (! layers.isEmpty()) {
                layerCache = layers.toArray(new Layer[layers.size()]);
                layerLevelCache = layerLevels.toArray(new int[layerLevels.size()][]);
            } else {
                layerCache = null;
                layerLevelCache = null;
            }
            if (! bitLayers.isEmpty()) {
                bitLayerCache = bitLayers.toArray(new Layer[bitLayers.size()]);
                bitLayerLevelCache = bitLayerLevels.toArray(new int[bitLayerLevels.size()][]);
            } else {
                bitLayerCache = null;
                bitLayerLevelCache = null;
            }
        } else {
            layerCache = null;
            bitLayerCache = null;
            layerLevelCache = null;
            bitLayerLevelCache = null;
        }
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        fixTerrainRangesTable();
        perlinNoise = new PerlinNoise(seed);
        initCaches();
    }
    
    /**
     * This ensures there are no nulls in the terrain ranges table. There already shouldn't be, but we've had reports
     * from the wild about it happening, so as a workaround fix it here. TODO: find out how there can be holes in the terrain ranges table.
     */
    private void fixTerrainRangesTable() {
        for (int i = 0; i < terrainRangesTable.length; i++) {
            if (terrainRangesTable[i] == null) {
                // Least problematic default seems to be bare grass
                terrainRangesTable[i] = Terrain.BARE_GRASS;
            }
        }
    }

    public static SimpleTheme createSingleTerrain(Terrain terrain, int minHeight, int maxHeight, int waterHeight) {
        return new SimpleTheme(0, waterHeight, new TreeMap<>(ImmutableMap.of(minHeight - 1, terrain)), null, minHeight, maxHeight, false, false);
    }

    public static SimpleTheme createDefault(Terrain topTerrain, int minHeight, int maxHeight, int waterHeight) {
        return createDefault(topTerrain, minHeight, maxHeight, waterHeight, false, true);
    }
    
    public static SimpleTheme createDefault(Terrain topTerrain, int minHeight, int maxHeight, int waterHeight, boolean randomise, boolean beaches) {
        if (topTerrain == null) {
            throw new NullPointerException("topTerrain");
        }
        SortedMap<Integer, Terrain> terrainRanges = new TreeMap<>();
        float factor = Math.min(maxHeight, 320) / 128f; // Constrain to a reasonable height
        terrainRanges.put(minHeight - 1                    , topTerrain);
        terrainRanges.put((int) (32 * factor) + waterHeight, Terrain.PERMADIRT);
        terrainRanges.put((int) (48 * factor) + waterHeight, Terrain.STONE_MIX);
        terrainRanges.put((int) (80 * factor) + waterHeight, Terrain.DEEP_SNOW);
        Map<Filter, Layer> layerMap = new HashMap<>();
        layerMap.put(new HeightFilter(minHeight, maxHeight, (int) (64 * factor) + waterHeight, maxHeight, true), Frost.INSTANCE);
        return new SimpleTheme(0, waterHeight, terrainRanges, layerMap, minHeight, maxHeight, randomise, beaches);
    }

    private long seed;
    private int waterHeight, minHeight, maxHeight;
    private SortedMap<Integer, Terrain> terrainRanges;
    private boolean randomise, beaches;
    private Terrain[] terrainRangesTable;
    private Map<Filter, Layer> layerMap;
    private transient PerlinNoise perlinNoise = new PerlinNoise(0);
    private Layer[] layerCache, bitLayerCache;
    private int[][] layerLevelCache, bitLayerLevelCache;
    private Map<Layer, Integer> discreteValues;

    private static final Random random = new Random();
    private static final long serialVersionUID = 1L;
}
