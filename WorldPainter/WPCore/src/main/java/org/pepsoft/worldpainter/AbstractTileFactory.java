/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter;

import java.awt.*;
import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.util.HashMap;
import java.util.Map;

import static java.util.Collections.synchronizedMap;

/**
 * An abstract base class for {@link TileFactory}s, which provides caching of
 * tiles to improve performance in contexts where the same tile will be
 * requested multiple times.
 *
 * @author SchmitzP
 */
public abstract class AbstractTileFactory implements TileFactory {
    /**
     * Delegates to {@link #createTile(int, int)}, but implements a memory
     * sensitive tile cache and if the tile has been previously created and is
     * still in the cache, returns the cached tile. If the tile is currently
     * being created on another thread, this method blocks until that is done.
     *
     * <p>This method is thread safe (if the underlying tile factory is).
     *
     * @param x The X coordinate of the tile to get.
     * @param y The Y coordinate of the tile to get.
     * @return The tile at the specified coordinates.
     */
    @Override
    public synchronized Tile getTile(int x, int y) {
        Point coords = new Point(x, y);
        TileCacheEntry tileCacheEntry = tileCache.computeIfAbsent(coords, point -> new TileCacheEntry());
        synchronized (tileCacheEntry) {
            Tile tile = (tileCacheEntry.tileRef != null) ? tileCacheEntry.tileRef.get() : null;
            if (tile == null) {
                tile = createTile(x, y);
                tileCacheEntry.tileRef = new SoftReference<>(tile);
            }
            return tile;
        }
    }

    private final Map<Point, TileCacheEntry> tileCache = synchronizedMap(new HashMap<>());

    private static final long serialVersionUID = 1L;

    static class TileCacheEntry {
        Reference<Tile> tileRef;
    }
}