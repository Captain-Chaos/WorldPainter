/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.themes;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.SortedMap;
import java.util.TreeMap;
import org.pepsoft.util.PerlinNoise;
import static org.pepsoft.worldpainter.Constants.SMALL_BLOBS;
import static org.pepsoft.worldpainter.Constants.TINY_BLOBS;
import org.pepsoft.worldpainter.HeightTransform;
import org.pepsoft.worldpainter.Terrain;
import org.pepsoft.worldpainter.Tile;
import org.pepsoft.worldpainter.layers.Frost;
import org.pepsoft.worldpainter.layers.Layer;

/**
 *
 * @author SchmitzP
 */
public class SimpleTheme implements Theme, Cloneable {
    @Deprecated
    public SimpleTheme(long seed, int waterHeight, Terrain[] terrainRangesTable, int maxHeight, boolean randomise, boolean beaches) {
        setSeed(seed);
        setWaterHeight(waterHeight);
        this.maxHeight = terrainRangesTable.length;
        this.terrainRangesTable = terrainRangesTable;
        fixTerrainRangesTable();
        setMaxHeight(maxHeight, HeightTransform.IDENTITY);
        setRandomise(randomise);
        setBeaches(beaches);
    }
    
    public SimpleTheme(long seed, int waterHeight, SortedMap<Integer, Terrain> terrainRanges, Map<Filter, Layer> layerMap, int maxHeight, boolean randomise, boolean beaches) {
        setSeed(seed);
        setWaterHeight(waterHeight);
        this.maxHeight = maxHeight;
        this.terrainRangesTable = new Terrain[maxHeight];
        setTerrainRanges(terrainRanges);
        fixTerrainRangesTable();
        setLayerMap(layerMap);
        setRandomise(randomise);
        setBeaches(beaches);
    }

    @Override
    public void apply(Tile tile, int x, int y) {
        int height = tile.getIntHeight(x, y);
        Terrain terrain = getTerrain(x, y, clamp(height, maxHeight - 1));
        if (tile.getTerrain(x, y) != terrain) {
            tile.setTerrain(x, y, terrain);
        }
        if (layerCache != null) {
            for (int i = 0; i < layerCache.length; i++) {
                int level = layerLevelCache[i][height];
                if (level != tile.getLayerValue(layerCache[i], x, y)) {
                    tile.setLayerValue(layerCache[i], x, y, level);
                }
            }
        }
        if (bitLayerCache != null) {
            for (int i = 0; i < bitLayerCache.length; i++) {
                int level = bitLayerLevelCache[i][height];
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
        this.terrainRanges = terrainRanges;
        for (int i = 0; i < maxHeight; i++) {
            terrainRangesTable[i] = terrainRanges.get(terrainRanges.headMap(i).lastKey());
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
    public final int getMaxHeight() {
        return maxHeight;
    }

    public final void setMaxHeight(int maxHeight) {
        setMaxHeight(maxHeight, HeightTransform.IDENTITY);
    }

    @Override
    public final void setMaxHeight(int maxHeight, HeightTransform transform) {
        if (maxHeight != this.maxHeight) {
            this.maxHeight = maxHeight;
            waterHeight = clamp(transform.transformHeight(waterHeight), maxHeight - 1);
            Terrain[] oldTerrainRangesTable = terrainRangesTable;
            terrainRangesTable = new Terrain[maxHeight];
            if (terrainRanges != null) {
                SortedMap<Integer, Terrain> oldTerrainRanges = this.terrainRanges;
                terrainRanges = new TreeMap<>();
                for (Map.Entry<Integer, Terrain> oldEntry: oldTerrainRanges.entrySet()) {
                    terrainRanges.put(oldEntry.getKey() < 0
                        ? oldEntry.getKey()
                        : clamp(transform.transformHeight(oldEntry.getKey()), maxHeight - 1), oldEntry.getValue());
                }
                for (int i = 0; i < maxHeight; i++) {
                    terrainRangesTable[i] = terrainRanges.get(terrainRanges.headMap(i).lastKey());
                }
            } else {
                // No terrain ranges map set; this is probably because it is
                // an old map. All we can do is extend the last entry
                System.arraycopy(oldTerrainRangesTable, 0, terrainRangesTable, 0, Math.min(oldTerrainRangesTable.length, terrainRangesTable.length));
                if (terrainRangesTable.length > oldTerrainRangesTable.length) {
                    for (int i = oldTerrainRangesTable.length; i < terrainRangesTable.length; i++) {
                        terrainRangesTable[i] = oldTerrainRangesTable[oldTerrainRangesTable.length - 1];
                    }
                }
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

    protected Terrain getTerrain(int x, int y, int height) {
        if (beaches && (height >= (waterHeight - 2)) && (height <= (waterHeight + 1))) {
            return Terrain.BEACHES;
        } else {
            if (isRandomise()) {
                height += perlinNoise.getPerlinNoise(x / SMALL_BLOBS, y / SMALL_BLOBS, height / SMALL_BLOBS) * 5;
                height += perlinNoise.getPerlinNoise(x / TINY_BLOBS, y / TINY_BLOBS, height / TINY_BLOBS) * 5;
                return terrainRangesTable[clamp(height, getMaxHeight() - 1)];
            } else {
                return terrainRangesTable[height];
            }
        }
    }
    
    protected final int clamp(int value, int max) {
        return (value < 0)
            ? 0
            : ((value > max)
                ? max
                : value);
    }

    private void initCaches() {
        if (layerMap != null) {
            List<Layer> layers = new ArrayList<>(layerMap.size());
            List<Layer> bitLayers = new ArrayList<>(layerMap.size());
            List<int[]> layerLevels = new ArrayList<>(layerMap.size());
            List<int[]> bitLayerLevels = new ArrayList<>(layerMap.size());
            layerLevelCache = new int[layerMap.size()][maxHeight];
            for (Map.Entry<Filter, Layer> entry: layerMap.entrySet()) {
                Layer layer = entry.getValue();
                Filter filter = entry.getKey();
                int[] levels = new int[maxHeight];
                for (int z = 0; z < maxHeight; z++) {
                    levels[z] = filter.getLevel(0, 0, z, 15);
                }
                if (layer.getDataSize() == Layer.DataSize.BIT) {
                    bitLayers.add(layer);
                    bitLayerLevels.add(levels);
                } else if (layer.getDataSize() == Layer.DataSize.NIBBLE) {
                    layers.add(layer);
                    layerLevels.add(levels);
                } else {
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
     * This ensures there are no nulls in the terrain ranges table. There
     * already shouldn't be, but we've had reports from the wild about it
     * happening, so as a workaround fix it here. TODO: find out how there can
     * be holes in the terrain ranges table.
     */
    private void fixTerrainRangesTable() {
        for (int i = 0; i < terrainRangesTable.length; i++) {
            if (terrainRangesTable[i] == null) {
                // Least problematic default seems to be bare grass
                terrainRangesTable[i] = Terrain.BARE_GRASS;
            }
        }
    }
    
    public static SimpleTheme createDefault(Terrain topTerrain, int maxHeight, int waterHeight) {
        return createDefault(topTerrain, maxHeight, waterHeight, false, true);
    }
    
    public static SimpleTheme createDefault(Terrain topTerrain, int maxHeight, int waterHeight, boolean randomise, boolean beaches) {
        SortedMap<Integer, Terrain> terrainRanges = new TreeMap<>();
        float factor = maxHeight / 128f;
        terrainRanges.put(-1                               , topTerrain);
        terrainRanges.put((int) (32 * factor) + waterHeight, Terrain.PERMADIRT);
        terrainRanges.put((int) (48 * factor) + waterHeight, Terrain.ROCK);
        terrainRanges.put((int) (80 * factor) + waterHeight, Terrain.DEEP_SNOW);
        Map<Filter, Layer> layerMap = new HashMap<>();
        layerMap.put(new HeightFilter(maxHeight, (int) (64 * factor) + waterHeight, maxHeight, true), Frost.INSTANCE);
        return new SimpleTheme(0, waterHeight, terrainRanges, layerMap, maxHeight, randomise, beaches);
    }

    private long seed;
    private int waterHeight, maxHeight;
    private SortedMap<Integer, Terrain> terrainRanges;
    private boolean randomise, beaches;
    private Terrain[] terrainRangesTable;
    private Map<Filter, Layer> layerMap;
    private transient PerlinNoise perlinNoise = new PerlinNoise(0);
    private Layer[] layerCache, bitLayerCache;
    private int[][] layerLevelCache, bitLayerLevelCache;
    
    private static final long SEED_OFFSET = 131;
    private static final Random random = new Random();
    private static final long serialVersionUID = 1L;
}
