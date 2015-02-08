/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter;

import org.pepsoft.minecraft.Material;

/**
 *
 * @author pepijn
 */
public interface ColourScheme {
    int getColour(int blockType);
    int getColour(int blockType, int dataValue);
    int getColour(Material material);
}