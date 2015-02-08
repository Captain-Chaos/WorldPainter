/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.viewer.tiled;

/**
 *
 * @author pepijn
 */
class TilePaintJob {
    public TilePaintJob(Tile tile) {
        this.tile = tile;
    }
    
    final Tile tile;
}