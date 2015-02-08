/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.worldpainter.themes.impl.rules;

import org.pepsoft.worldpainter.Tile;

/**
 *
 * @author SchmitzP
 */
public class Context {
    public Context(Tile tile, int x, int y, long seed, int maxHeight, int waterHeight) {
        this.tile = tile;
        this.x = x;
        this.y = y;
        this.seed = seed;
        this.maxHeight = maxHeight;
        this.waterHeight = waterHeight;
    }

    final Tile tile;
    final int x, y, maxHeight, waterHeight;
    final long seed;
}