/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.viewer.tiled;

import org.pepsoft.worldpainter.viewer.Layer;

/**
 * A producer of tiles for a tiled view layer.
 * 
 * @author pepijn
 */
public interface TileFactory {
    Layer getLayer();
}