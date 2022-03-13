/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.worldpainter;

import org.pepsoft.worldpainter.layers.FloodWithLava;
import org.pepsoft.worldpainter.themes.SimpleTheme;
import org.pepsoft.worldpainter.themes.Theme;

import java.awt.*;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.SortedMap;

import static org.pepsoft.util.MathUtils.clamp;
import static org.pepsoft.worldpainter.Constants.TILE_SIZE;
import static org.pepsoft.worldpainter.Constants.TILE_SIZE_BITS;

/**
 *
 * @author pepijn
 */
public class HeightMapTileFactory extends AbstractTileFactory {
    public HeightMapTileFactory(long seed, HeightMap heightMap, int minHeight, int maxHeight, boolean floodWithLava, Theme theme) {
        this.seed = seed;
        this.minHeight = minHeight;
        this.heightMap = heightMap;
        this.maxHeight = maxHeight;
        this.floodWithLava = floodWithLava;
        heightMap.setSeed(seed);
        theme.setSeed(seed);
        this.theme = theme;
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
    public long getSeed() {
        return seed;
    }

    @Override
    public void setSeed(long seed) {
        this.seed = seed;
        heightMap.setSeed(seed);
        theme.setSeed(seed);
    }

    @Override
    public final void setMinMaxHeight(int minHeight, int maxHeight, HeightTransform transform) {
        this.minHeight = minHeight;
        this.maxHeight = maxHeight;
        theme.setMinMaxHeight(minHeight, maxHeight, transform);
        if (! transform.isIdentity()) {
            heightMap = transform.transformHeightMap(heightMap);
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
        theme.setMinMaxHeight(minHeight, maxHeight, HeightTransform.IDENTITY);
    }

    @Override
    public boolean isTilePresent(int x, int y) {
        Rectangle extent = getExtent();
        return (extent == null) || extent.contains(x, y);
    }

    @Override
    public Tile createTile(int tileX, int tileY) {
        final int maxZ = maxHeight - 1, myWaterHeight = getWaterHeight();
        final Tile tile = new Tile(tileX, tileY, minHeight, maxHeight);
        tile.inhibitEvents();
        final int worldTileX = tileX * TILE_SIZE, worldTileY = tileY * TILE_SIZE;
        try {
            for (int x = 0; x < TILE_SIZE; x++) {
                for (int y = 0; y < TILE_SIZE; y++) {
                    final int blockX = worldTileX + x, blockY = worldTileY + y;
                    final float height = clamp(minHeight, heightMap.getHeight(blockX, blockY), maxZ);
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
        Rectangle heightMapExtent = heightMap.getExtent();
        if (heightMapExtent != null) {
            int tileX1 = heightMapExtent.x >> TILE_SIZE_BITS;
            int tileY1 = heightMapExtent.y >> TILE_SIZE_BITS;
            int tileX2 = (heightMapExtent.x + heightMapExtent.width - 1) >> TILE_SIZE_BITS;
            int tileY2 = (heightMapExtent.y + heightMapExtent.height - 1) >> TILE_SIZE_BITS;
            return new Rectangle(tileX1, tileY1, (tileX2 - tileX1) + 1, (tileY2 - tileY1) + 1);
        } else {
            return null;
        }
    }

    @Override
    public final void applyTheme(Tile tile, int x, int y) {
        theme.apply(tile, x, y);
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
                ? new SimpleTheme(seed, waterHeight, terrainRanges, null, minHeight, maxHeight, randomise, beaches)
                : new SimpleTheme(seed, waterHeight, terrainRangesTable, minHeight, maxHeight, randomise, beaches);
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
    private int minHeight, maxHeight;
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