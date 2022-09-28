/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.layers.tunnel;

import org.pepsoft.util.ColourUtils;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.Dimension.Anchor;
import org.pepsoft.worldpainter.layers.renderers.DimensionAwareRenderer;
import org.pepsoft.worldpainter.layers.renderers.TransparentColourRenderer;

import static org.pepsoft.worldpainter.Dimension.Role.CAVE_FLOOR;

/**
 *
 * @author pepijn
 */
public class TunnelLayerRenderer extends TransparentColourRenderer implements DimensionAwareRenderer {
    public TunnelLayerRenderer(TunnelLayer layer) {
        super(layer.getColour());
        this.layer = layer;
        layerFloorLevel = layer.getFloorLevel();
        layerRoofLevel = layer.getRoofLevel();
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
        if (layer.getFloorDimensionId() != null) {
            final Anchor anchor = dimension.getAnchor();
            floorDimension = dimension.getWorld().getDimension(new Anchor(anchor.dim, CAVE_FLOOR, anchor.invert, layer.getFloorDimensionId()));
            if ((floorDimension != null) && (! floorDimension.containsOneOf(layer))) {
                floorDimension = null;
            }
        } else {
            floorDimension = null;
        }
    }
    
    private Effect getEffect(int x, int y) {
        final int terrainHeight = dimension.getIntHeightAt(x, y);
        final int floorLevel;
        switch (layer.getFloorMode()) {
            case CONSTANT_DEPTH:
                floorLevel = terrainHeight - layerFloorLevel;
                break;
            case FIXED_HEIGHT:
                floorLevel = layerFloorLevel;
                break;
            case INVERTED_DEPTH:
                floorLevel = layerFloorLevel - (terrainHeight - layerFloorLevel);
                break;
            case CUSTOM_DIMENSION:
                floorLevel = (floorDimension != null) ? floorDimension.getIntHeightAt(x, y) : layerFloorLevel;
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
                roofLevel = terrainHeight - layerRoofLevel;
                break;
            case FIXED_HEIGHT:
                roofLevel = layerRoofLevel;
                break;
            case INVERTED_DEPTH:
                roofLevel = layerRoofLevel - (terrainHeight - layerRoofLevel);
                break;
            case FIXED_HEIGHT_ABOVE_FLOOR:
                roofLevel = floorLevel + layerRoofLevel;
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
    private final int layerFloorLevel, layerRoofLevel;
    private Dimension dimension, floorDimension;
    
    enum Effect {NONE, BREAKS_SURFACE, UNDERGROUND}
}