package org.pepsoft.worldpainter;

import org.pepsoft.worldpainter.heightMaps.AbstractHeightMap;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectOutputStream;

import static java.util.Objects.requireNonNull;
import static org.pepsoft.worldpainter.Constants.TILE_SIZE_BITS;
import static org.pepsoft.worldpainter.Constants.TILE_SIZE_MASK;

class TileCacheHeightMap extends AbstractHeightMap {
    TileCacheHeightMap(float[][][][] cache, int tileXOffset, int tileYOffset, float rangeMin, float rangeMax) {
        requireNonNull(cache);
        this.cache = cache;
        this.tileXOffset = tileXOffset;
        this.tileYOffset = tileYOffset;
        this.rangeMin = rangeMin;
        this.rangeMax = rangeMax;
        extent = new Rectangle(tileXOffset << TILE_SIZE_BITS, tileYOffset << TILE_SIZE_BITS, cache.length << TILE_SIZE_BITS, cache[0].length << TILE_SIZE_BITS);
    }

    @Override
    public Icon getIcon() {
        return null;
    }

    @Override
    public double[] getRange() {
        return new double[] {rangeMin, rangeMax};
    }

    @Override
    public Rectangle getExtent() {
        return extent;
    }

    @Override
    public double getHeight(int x, int y) {
        final int tileX = (x >> TILE_SIZE_BITS) - tileXOffset, tileY = (y >> TILE_SIZE_BITS) - tileYOffset;
        if ((tileX < 0) || (tileX >= cache.length) || (tileY < 0) || (tileY >= cache[0].length) || (cache[tileX][tileY] == null)) {
            return rangeMin;
        }
        return cache[tileX][tileY][x & TILE_SIZE_MASK][y & TILE_SIZE_MASK];
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        throw new NotSerializableException();
    }

    private final float[][][][] cache;
    private final int tileXOffset, tileYOffset;
    private final float rangeMin, rangeMax;
    private final Rectangle extent;
}