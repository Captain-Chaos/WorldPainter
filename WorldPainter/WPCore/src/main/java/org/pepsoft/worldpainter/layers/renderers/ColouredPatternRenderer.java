/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.layers.renderers;

import org.pepsoft.util.ColourUtils;

/**
 *
 * @author pepijn
 */
public class ColouredPatternRenderer implements NibbleLayerRenderer {
    public ColouredPatternRenderer(int colour, boolean[][] pattern) {
        this(colour, pattern, 0, 0);
    }
    
    public ColouredPatternRenderer(int colour, boolean[][] pattern, int dx, int dy) {
        this.colour = colour;
        this.pattern = pattern;
        this.dx = dx;
        this.dy = dy;
        if (pattern.length != 8) {
            throw new IllegalArgumentException("pattern");
        }
        for (boolean[] row: pattern) {
            if (row.length != 8) {
                throw new IllegalArgumentException("pattern");
            }
        }
    }

    @Override
    public int getPixelColour(int x, int y, int underlyingColour, int value) {
        if ((value > 0) && pattern[(y + dy) & 7][(x + dx) & 7]) {
            int intensity = value * 255 / 15;
            return ColourUtils.mix(colour, underlyingColour, intensity);
        } else {
            return underlyingColour;
        }
    }
    
    private final int colour, dx, dy;
    private final boolean[][]pattern;
}