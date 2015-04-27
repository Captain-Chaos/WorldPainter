/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.layers.renderers;

/**
 *
 * @author pepijn
 */
public class VoidRenderer implements BitLayerRenderer {
    @Override
    public int getPixelColour(int x, int y, int underlyingColour, boolean value) {
        return value ? colour : underlyingColour;
    }

    public static int getColour() {
        return colour;
    }

    public static void setColour(int colour) {
        VoidRenderer.colour = colour;
    }
    
    private static int colour = 0xC0FFFF;
}