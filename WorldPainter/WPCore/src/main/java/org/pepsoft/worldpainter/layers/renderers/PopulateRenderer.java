/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.worldpainter.layers.renderers;

/**
 *
 * @author pepijn
 */
public class PopulateRenderer implements BitLayerRenderer {
    @Override
    public int getPixelColour(int x, int y, int underlyingColour, boolean value) {
        x = x & 0xf;
        y = y & 0xf;
        if (value  && ((x == 0) || (x == 15) || (y == 0) || (y == 15))) {
            return GREEN;
        } else {
            return underlyingColour;
        }
    }

    private static final int GREEN = 0x008000;
}