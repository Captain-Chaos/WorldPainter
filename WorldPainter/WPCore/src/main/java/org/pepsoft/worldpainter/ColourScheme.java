/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter;

import org.pepsoft.minecraft.Material;
import org.pepsoft.worldpainter.colourschemes.HardcodedColourScheme;

/**
 * A provider of colours of Minecraft blocks. The colours are specified as {@code int}s in packed RGB format, that
 * is to say bits 0 through 7 specify the blue component as a number from 0 to 255 (inclusive); bits 8 through 15 the
 * green component and bits 16 through 23 the red component.
 *
 * @author pepijn
 */
public interface ColourScheme {
    /**
     * Get the colour of a particular Minecraft material.
     *
     * @param material The material for which to get the colour.
     * @return The colour of the specified material in packed RGB format.
     */
    int getColour(Material material);
    
    ColourScheme DEFAULT = new HardcodedColourScheme();
}