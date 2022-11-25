/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.layers.renderers;

import org.pepsoft.worldpainter.ColourScheme;

import static org.pepsoft.minecraft.Material.*;
import static org.pepsoft.worldpainter.layers.GardenCategory.*;

/**
 *
 * @author pepijn
 */
public class GardenCategoryRenderer implements NibbleLayerRenderer, ColourSchemeRenderer {
    public ColourScheme getColourScheme() {
        return colourScheme;
    }

    @Override
    public void setColourScheme(ColourScheme colourScheme) {
        this.colourScheme = colourScheme;
        cobblestoneColour = colourScheme.getColour(COBBLESTONE);
        grassColour = colourScheme.getColour(GRASS_BLOCK);
        dirtColour = colourScheme.getColour(DIRT);
        bricksColour = colourScheme.getColour(BRICKS);
        woodColour = colourScheme.getColour(WOODEN_PLANK_OAK);
        waterColour = colourScheme.getColour(WATER);
        objectColour = colourScheme.getColour(WOOL_MAGENTA);
        treeColour = colourScheme.getColour(LEAVES_OAK);
    }
    
    @Override
    public int getPixelColour(int x, int y, int underlyingColour, int value) {
        switch (value) {
            case CATEGORY_UNOCCUPIED:
                return underlyingColour;
            case CATEGORY_ROAD:
                return cobblestoneColour;
            case CATEGORY_FIELD:
                if ((x + y) % 2 == 0) {
                    return grassColour;
                } else {
                    return dirtColour;
                }
            case CATEGORY_BUILDING:
                return bricksColour;
            case CATEGORY_STREET_FURNITURE:
                return woodColour;
            case CATEGORY_WATER:
                return waterColour;
            case CATEGORY_TREE:
                return treeColour;
            case CATEGORY_OBJECT:
                return objectColour;
            default:
                throw new IllegalArgumentException(Integer.toString(value));
        }
    }
    
    private ColourScheme colourScheme;
    private int cobblestoneColour, grassColour, dirtColour, bricksColour, torchColour, woodColour, waterColour, objectColour, treeColour;
}