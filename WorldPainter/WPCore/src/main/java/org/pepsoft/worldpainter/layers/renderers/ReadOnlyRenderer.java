/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.worldpainter.layers.renderers;

/**
 *
 * @author pepijn
 */
public class ReadOnlyRenderer implements BitLayerRenderer {
    public int getPixelColour(int x, int y, int underlyingColour, boolean value) {
        x = x & 0xf;
        y = y & 0xf;
        if (value  && ((x == y) || (x == (15 - y)))) {
            return BLACK;
        } else {
            return underlyingColour;
        }
    }

    private static final int BLACK = 0x000000;
}