/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.util.swing;

import java.awt.*;
import java.util.Set;

/**
 * A listener for changes in the contents of tiles.
 * 
 * @author Pepijn Schmitz
 */
public interface TileListener {
    /**
     * Invoked when the contents of a tile have changed and the tile should be
     * retrieved again.
     * 
     * @param source The tile provider from which the changed tile should be
     *    retrieved.
     * @param x The X coordinate (in tiles) of the changed tile.
     * @param y The Y coordinate (in tiles) of the changed tile.
     */
    void tileChanged(TileProvider source, int x, int y);

    /**
     * Invoked when the contents of one or more tiles have changed and the
     * tile(s) should be retrieved again.
     *
     * @param source The tile provider from which the changed tile(s) should be
     *     retrieved.
     * @param tiles The set of coordinates of the changed tile(s).
     */
    void tilesChanged(TileProvider source, Set<Point> tiles);
}