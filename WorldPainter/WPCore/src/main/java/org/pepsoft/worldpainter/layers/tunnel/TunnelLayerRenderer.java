/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.layers.tunnel;

import org.pepsoft.util.ColourUtils;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.layers.renderers.DimensionAwareRenderer;
import org.pepsoft.worldpainter.layers.renderers.TransparentColourRenderer;

/**
 *
 * @author pepijn
 */
public class TunnelLayerRenderer extends TransparentColourRenderer implements DimensionAwareRenderer {
    public TunnelLayerRenderer(TunnelLayer layer) {
        super(layer.getColour());
        this.layer = layer;
    }

    @Override
    public int getPixelColour(int x, int y, int underlyingColour, boolean value) {
        if (! value) {
            return underlyingColour;
        } else if (dimension != null) {
            switch (getEffect(x, y)) {
                case BREAKS_SURFACE:
                    return layer.getColour();
                case NONE:
                    return ColourUtils.mix(layer.getColour(), underlyingColour, 64);
                case UNDERGROUND:
                    return ColourUtils.mix(layer.getColour(), underlyingColour, 160);
                default:
                    throw new InternalError();
            }
        } else {
            return super.getPixelColour(x, y, underlyingColour, true);
        }
    }

    @Override
    public void setDimension(Dimension dimension) {
        this.dimension = dimension;
    }
    
    private Effect getEffect(int x, int y) {
        final int terrainHeight = dimension.getIntHeightAt(x, y);
        final int floorLevel;
        switch (layer.getFloorMode()) {
            case CONSTANT_DEPTH:
                floorLevel = terrainHeight - layer.getFloorLevel();
                break;
            case FIXED_HEIGHT:
                floorLevel = layer.getFloorLevel();
                break;
            case INVERTED_DEPTH:
                floorLevel = layer.getFloorLevel() - (terrainHeight - layer.getFloorLevel());
                break;
            default:
                throw new InternalError();
        }
        if (floorLevel >= terrainHeight) {
            return Effect.NONE;
        }
        final int roofLevel;
        switch (layer.getRoofMode()) {
            case CONSTANT_DEPTH:
                roofLevel = terrainHeight - layer.getRoofLevel();
                break;
            case FIXED_HEIGHT:
                roofLevel = layer.getRoofLevel();
                break;
            case INVERTED_DEPTH:
                roofLevel = layer.getRoofLevel()- (terrainHeight - layer.getRoofLevel());
                break;
            default:
                throw new InternalError();
        }
        if (floorLevel >= roofLevel) {
            return Effect.NONE;
        } else if (terrainHeight <= roofLevel) {
            return Effect.BREAKS_SURFACE;
        } else {
            return Effect.UNDERGROUND;
        }
    }
    
    private final TunnelLayer layer;
    private Dimension dimension;
    
    enum Effect {NONE, BREAKS_SURFACE, UNDERGROUND}
}