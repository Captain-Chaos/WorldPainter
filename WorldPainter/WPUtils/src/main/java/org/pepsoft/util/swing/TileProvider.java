/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.util.swing;

import java.awt.*;

/**
 *
 * @author pepijn
 */
public interface TileProvider {
    /**
     * Get the width and height of tiles (which must be square) in pixels. The
     * size must be a power of two!
     * 
     * @return The size in pixels of a tile.
     */
    int getTileSize();
    
    /**
     * Determines, if possible, if there is a tile present at a specified
     * location.
     *
     * @param x The X coordinate (in tiles) to check for the presence of a tile.
     * @param y The Y coordinate (in tiles) to check for the presence of a tile.
     * @return <code>true</code> if there is a tile there.
     */
    boolean isTilePresent(int x, int y);

    /**
     * Paint the tile at the specified tile coordinates to an image. The X
     * coordinate increases to the right and the Y coordinate increases towards
     * the bottom. An image of the correct size (or larger) will be provided on
     * which to paint the tile. The tile provider may be requested to paint the
     * tile in a different location on the provided image than the top left
     * corner.
     *
     * @param image The image on which to paint the tile
     * @param x The X coordinate (in tiles) of the tile to paint.
     * @param y The Y coordinate (in tiles) of the tile to paint.
     * @param dx The X coordinate at which to paint the tile in the image.
     * @param dy The Y coordinate at which to paint the tile in the image.
     */
    void paintTile(Image image, int x, int y, int dx, int dy);
    
    /**
     * Get the priority with which a specific tile should be rendered. A tile
     * consumer may choose to render higher priority tiles before lower
     * priority tiles. It is entirely up to the tile provider which values to
     * return, or even to always return the same value.
     * 
     * @param x The X coordinate (in tiles) of the tile.
     * @param y The Y coordinate (in tiles) of the tile.
     * @return The priority of the specified tile.
     */
    int getTilePriority(int x, int y);
    
    /**
     * Get the coordinates of the "main area of interest" of this tile provider,
     * if any. The tile provider may provide tiles outside this area, but the
     * scrollbars will not extend to them. If the tile provider has no
     * distinguishable area of interest (because it is endless, for instance),
     * it may return null.
     * 
     * @return The coordinates of the main area of interest of this tile
     *     provider.
     */
    Rectangle getExtent();
    
    /**
     * Register a tile listener which will be notified if the contents of a 
     * tile change.
     * 
     * @param tileListener The tile listener to register.
     */
    void addTileListener(TileListener tileListener);
    
    /**
     * Remove a previously registered tile listener.
     * 
     * @param tileListener The tile listener to remove.
     */
    void removeTileListener(TileListener tileListener);
    
    /**
     * Indicates whether the tile provider implements zooming itself (when
     * <code>true</code>) or whether the tile consumer should implement it (when
     * <code>false</code>). In the latter case {@link #getZoom()} and
     * {@link #setZoom(int)} will never be invoked.
     * 
     * @return <code>true</code> if the tile provider implements zooming.
     */
    boolean isZoomSupported();
    
    /**
     * Get the zoom as an exponent of two. In other words the tiles will be
     * displayed at a scale of 1:2^n, so that n=-1 means half size, n=0 means
     * 1:1 and 1 means double size, etc.
     * 
     * @return The zoom as an exponent of two.
     */
    int getZoom();
    
    /**
     * Set the zoom as an exponent of two. In other words the tiles will be
     * displayed at a scale of 1:2^n, so that n=-1 means half size, n=0 means
     * 1:1 and 1 means double size, etc.
     * 
     * @param zoom The zoom as an exponent of two.
     */
    void setZoom(int zoom);
}