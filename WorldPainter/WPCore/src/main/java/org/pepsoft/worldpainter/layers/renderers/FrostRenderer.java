/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.worldpainter.layers.renderers;

/**
 *
 * @author pepijn
 */
public class FrostRenderer implements BitLayerRenderer {
    @Override
    public int getPixelColour(int x, int y, int underlyingColour, boolean value) {
        if (value) {
            int red   =  255 - ((255 - ((underlyingColour & 0xFF0000) >> 16)) / 2);
            int green =  255 - ((255 - ((underlyingColour & 0x00FF00) >>  8)) / 2);
            int blue  =  255 - ((255 -  (underlyingColour & 0x0000FF)       ) / 2);
            return (red << 16) | (green << 8) | blue;
        } else {
            return underlyingColour;
        }
    }
}