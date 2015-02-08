/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter;

/**
 *
 * @author SchmitzP
 */
public abstract class AbstractTileFactory implements TileFactory {
    @Override
    public Tile getTile(int x, int y) {
        return createTile(x, y);
    }
    
    private static final long serialVersionUID = 1L;
}