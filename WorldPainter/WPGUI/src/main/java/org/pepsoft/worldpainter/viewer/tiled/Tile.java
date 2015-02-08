/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.viewer.tiled;

import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import static org.pepsoft.worldpainter.viewer.tiled.TileConstants.*;

/**
 *
 * @author pepijn
 */
class Tile {
    public Tile(int x, int y) {
        this.x = x;
        this.y = y;
        image = GRAPHICS_CONFIG.createCompatibleImage(TILE_SIZE, TILE_SIZE, Transparency.TRANSLUCENT);
        dirty = true;
    }
    
    final BufferedImage image;
    int x, y;
    boolean dirty;
    TilePaintJob paintJob;
    
    private static final GraphicsConfiguration GRAPHICS_CONFIG = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration();
}