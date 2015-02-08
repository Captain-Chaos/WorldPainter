/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.worldpainter;

import java.awt.Rectangle;

/**
 *
 * @author pepijn
 */
public interface TileProvider {
    Rectangle getExtent();
    boolean isTilePresent(int x, int y);
    Tile getTile(int x, int y);
}