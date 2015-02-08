/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.worldpainter;

import java.io.Serializable;

/**
 *
 * @author pepijn
 */
public interface TileFactory extends Serializable, TileProvider {
    int getMaxHeight();
    void setMaxHeight(int maxHeight, HeightTransform transform);
    long getSeed();
    void setSeed(long seed);
    Tile createTile(int x, int y);
    void applyTheme(Tile tile, int x, int y);
}