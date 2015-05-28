/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.worldpainter;

import java.awt.Rectangle;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.SortedMap;
import org.pepsoft.worldpainter.layers.FloodWithLava;
import static org.pepsoft.worldpainter.Constants.*;
import org.pepsoft.worldpainter.themes.SimpleTheme;
import org.pepsoft.worldpainter.themes.Theme;

/**
 *
 * @author pepijn
 */
public class HeightMapTileFactory extends AbstractTileFactory {
    public HeightMapTileFactory(long seed, HeightMap heightMap, int maxHeight, boolean floodWithLava, Theme theme) {
        this.seed = seed;
        this.heightMap = heightMap;
        this.maxHeight = maxHeight;
        this.floodWithLava = floodWithLava;
        heightMap.setSeed(seed);
        theme.setSeed(seed);
        this.theme = theme;
    }

    @Override
    public final int getMaxHeight() {
        return maxHeight;
    }

    @Override
    public long getSeed() {
        return seed;
    }

    @Override
    public void setSeed(long seed) {
        this.seed = seed;
        heightMap.setSeed(seed);
        theme.setSeed(seed);
    }

    public final void setMaxHeight(int maxHeight) {
        setMaxHeight(maxHeight, HeightTransform.IDENTITY);
    }
    
    @Override
    public final void setMaxHeight(int maxHeight, HeightTransform transform) {
        if (maxHeight != this.maxHeight) {
            this.maxHeight = maxHeight;
            theme.setMaxHeight(maxHeight, transform);
        }
    }

    public final int getWaterHeight() {
        return theme.getWaterHeight();
    }

    public final void setWaterHeight(int waterHeight) {
        theme.setWaterHeight(waterHeight);
    }

    public final boolean isFloodWithLava() {
        return floodWithLava;
    }

    public final HeightMap getHeightMap() {
        return heightMap;
    }
    
    public final float getBaseHeight() {
        return heightMap.getBaseHeight();
    }

    public final void setHeightMap(HeightMap heightMap) {
        if (heightMap == null) {
            throw new NullPointerException();
        }
        this.heightMap = heightMap;
    }

    public Theme getTheme() {
        return theme;
    }

    public void setTheme(Theme theme) {
        this.theme = theme;
        theme.setMaxHeight(maxHeight, HeightTransform.IDENTITY);
    }

    /**
     * Always returns <code>true</code> since height map tile factories are
     * endless.
     */
    @Override
    public boolean isTilePresent(int x, int y) {
        return true;
    }

    @Override
    public final Tile createTile(int tileX, int tileY) {
        final int maxY = getMaxHeight() - 1, myWaterHeight = getWaterHeight();
        final Tile tile = new Tile(tileX, tileY, maxHeight);
        tile.inhibitEvents();
        final int worldTileX = tileX * TILE_SIZE, worldTileY = tileY * TILE_SIZE;
        try {
            for (int x = 0; x < TILE_SIZE; x++) {
                for (int y = 0; y < TILE_SIZE; y++) {
                    final int blockX = worldTileX + x, blockY = worldTileY + y;
                    final float height = clamp(heightMap.getHeight(blockX, blockY), maxY);
                    tile.setHeight(x, y, height);
                    tile.setWaterLevel(x, y, myWaterHeight);
                    if (floodWithLava) {
                        tile.setBitLayerValue(FloodWithLava.INSTANCE, x, y, true);
                    }
                    theme.apply(tile, x, y);
                }
            }
            return tile;
        } finally {
            tile.releaseEvents();
        }
    }

    @Override
    public Rectangle getExtent() {
        return null; // Height map tile factories are endless
    }

    @Override
    public final void applyTheme(Tile tile, int x, int y) {
        theme.apply(tile, x, y);
    }

    protected final float clamp(float value, int max) {
        return (value < 0)
            ? 0
            : ((value > max)
                ? max
                : value);
    }

    protected final void setRandomise(boolean randomise) {
        this.randomise = randomise;
    }

    protected final void setBeaches(boolean beaches) {
        this.beaches = beaches;
    }
    
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        
        // Legacy map support
        if (maxHeight == 0) {
            maxHeight = 128;
        }
        if (version < 1) {
            theme = (terrainRanges != null)
                ? new SimpleTheme(seed, waterHeight, terrainRanges, null, maxHeight, randomise, beaches)
                : new SimpleTheme(seed, waterHeight, terrainRangesTable, maxHeight, randomise, beaches);
            waterHeight = -1;
            terrainRanges = null;
            terrainRangesTable = null;
            randomise = false;
            beaches = false;
        }
        version = CURRENT_VERSION;
    }
    
    @Deprecated
    int waterHeight = -1;
    
    @Deprecated
    private Terrain[] terrainRangesTable;
    private final boolean floodWithLava;
    private int maxHeight;
    @Deprecated
    private SortedMap<Integer, Terrain> terrainRanges;
    @Deprecated
    private boolean randomise, beaches;
    private long seed;
    private HeightMap heightMap;
    private Theme theme;
    private int version = CURRENT_VERSION;

    private static final long serialVersionUID = 2011032801L;
    
    private static final int CURRENT_VERSION = 1;
}