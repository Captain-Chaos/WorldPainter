/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.threedeeview;

import java.awt.image.BufferedImage;
import org.pepsoft.worldpainter.Tile;

/**
 *
 * @author pepijn
 */
public class RenderResult {
    public RenderResult(Tile tile, BufferedImage image) {
        this.tile = tile;
        this.image = image;
    }
    
    public Tile getTile() {
        return tile;
    }
    
    public BufferedImage getImage() {
        return image;
    }
    
    private final Tile tile;
    private final BufferedImage image;
}
