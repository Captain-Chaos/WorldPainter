/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.worldpainter.layers.renderers;

import org.pepsoft.worldpainter.ColourScheme;

import static org.pepsoft.minecraft.Material.WOOLS;

/**
 *
 * @author SchmitzP
 */
public class AnnotationsRenderer implements NibbleLayerRenderer, ColourSchemeRenderer {
    @Override
    public int getPixelColour(int x, int y, int underlyingColour, int value) {
        return colours[value];
    }

    public ColourScheme getColourScheme() {
        return colourScheme;
    }

    @Override
    public void setColourScheme(ColourScheme colourScheme) {
        this.colourScheme = colourScheme;
        for (int i = 1; i < 16; i++) {
            colours[i] = colourScheme.getColour(WOOLS[i - ((i < 8) ? 1 : 0)]);
        }
    }
    
    private ColourScheme colourScheme;
    private final int[] colours = new int[16];
}