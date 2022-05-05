/*
 * WorldPainter, a graphical and interactive map generator for Minecraft.
 * Copyright © 2011-2015  pepsoft.org, The Netherlands
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.pepsoft.worldpainter.painting;

import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.Tile;
import org.pepsoft.worldpainter.layers.Layer;

import static org.pepsoft.worldpainter.Constants.TILE_SIZE_BITS;
import static org.pepsoft.worldpainter.Constants.TILE_SIZE_MASK;

/**
 * Paint which paints a nibble sized layer using a brush, with undo and dynamic level supports.
 *
 * <p><strong>Note:</strong> does <em>not</em> do any event inhibitation management.
 *
 * <p>Created by pepijn on 15-05-15.
 */
public final class NibbleLayerPaint extends LayerPaint {
    public NibbleLayerPaint(Layer layer) {
        super(layer);
        if (layer.getDataSize() != Layer.DataSize.NIBBLE) {
            throw new IllegalArgumentException("Layer " + layer + " not nibble-sized");
        }
    }

    @Override
    public void apply(Dimension dimension, int centreX, int centreY, float dynamicLevel) {
        if (brush.getRadius() == 0) {
            // Special case: if the radius is 0, assume that the user wants to paint complete pixels instead of trying
            // to apply the brush
            applyPixel(dimension, centreX, centreY);
            return;
        }
        final int effectiveRadius = brush.getEffectiveRadius();
        final int x1 = centreX - effectiveRadius, y1 = centreY - effectiveRadius, x2 = centreX + effectiveRadius, y2 = centreY + effectiveRadius;
        final int tileX1 = x1 >> TILE_SIZE_BITS, tileY1 = y1 >> TILE_SIZE_BITS, tileX2 = x2 >> TILE_SIZE_BITS, tileY2 = y2 >> TILE_SIZE_BITS;
        if ((tileX1 == tileX2) && (tileY1 == tileY2)) {
            // The bounding box of the brush is entirely on one tile; optimize by painting directly to the tile
            final Tile tile = dimension.getTileForEditing(tileX1, tileY1);
            if (tile == null) {
                return;
            }
            final int x1InTile = x1 & TILE_SIZE_MASK, y1InTile = y1 & TILE_SIZE_MASK, x2InTile = x2 & TILE_SIZE_MASK, y2InTile = y2 & TILE_SIZE_MASK;
            final int tileXInWorld = tileX1 << TILE_SIZE_BITS, tileYInWorld = tileY1 << TILE_SIZE_BITS;
            for (int y = y1InTile; y <= y2InTile; y++) {
                for (int x = x1InTile; x <= x2InTile; x++) {
                    final int currentValue = tile.getLayerValue(layer, x, y);
                    final float strength = dynamicLevel * getStrength(centreX, centreY, tileXInWorld + x, tileYInWorld + y);
                    if (strength != 0f) {
                        int targetValue = 1 + Math.round(strength * 14);
                        if (targetValue > currentValue) {
                            tile.setLayerValue(layer, x, y, targetValue);
                        }
                    }
                }
            }
        } else {
            // The bounding box of the brush straddles more than one tile; paint to the dimension
            for (int y = y1; y <= y2; y++) {
                for (int x = x1; x <= x2; x++) {
                    final int currentValue = dimension.getLayerValueAt(layer, x, y);
                    final float strength = dynamicLevel * getStrength(centreX, centreY, x, y);
                    if (strength != 0f) {
                        int targetValue = 1 + Math.round(strength * 14);
                        if (targetValue > currentValue) {
                            dimension.setLayerValueAt(layer, x, y, targetValue);
                        }
                    }
                }
            }
        }
    }

    @Override
    public void remove(Dimension dimension, int centreX, int centreY, float dynamicLevel) {
        if (brush.getRadius() == 0) {
            // Special case: if the radius is 0, assume that the user wants to remove complete pixels instead of trying
            // to apply the brush
            removePixel(dimension, centreX, centreY);
            return;
        }
        final int effectiveRadius = brush.getEffectiveRadius();
        final int x1 = centreX - effectiveRadius, y1 = centreY - effectiveRadius, x2 = centreX + effectiveRadius, y2 = centreY + effectiveRadius;
        final int tileX1 = x1 >> TILE_SIZE_BITS, tileY1 = y1 >> TILE_SIZE_BITS, tileX2 = x2 >> TILE_SIZE_BITS, tileY2 = y2 >> TILE_SIZE_BITS;
        if ((tileX1 == tileX2) && (tileY1 == tileY2)) {
            // The bounding box of the brush is entirely on one tile; optimize by painting directly to the tile
            final Tile tile = dimension.getTileForEditing(tileX1, tileY1);
            if (tile == null) {
                return;
            }
            final int x1InTile = x1 & TILE_SIZE_MASK, y1InTile = y1 & TILE_SIZE_MASK, x2InTile = x2 & TILE_SIZE_MASK, y2InTile = y2 & TILE_SIZE_MASK;
            final int tileXInWorld = tileX1 << TILE_SIZE_BITS, tileYInWorld = tileY1 << TILE_SIZE_BITS;
            for (int y = y1InTile; y <= y2InTile; y++) {
                for (int x = x1InTile; x <= x2InTile; x++) {
                    final int currentValue = tile.getLayerValue(layer, x, y);
                    final float strength = dynamicLevel * getFullStrength(centreX, centreY, tileXInWorld + x, tileYInWorld + y);
                    if (strength != 0f) {
                        int targetValue = 14 - Math.round(strength * 14);
                        if (targetValue < currentValue) {
                            tile.setLayerValue(layer, x, y, targetValue);
                        }
                    }
                }
            }
        } else {
            // The bounding box of the brush straddles more than one tile; paint to the dimension
            for (int y = y1; y <= y2; y++) {
                for (int x = x1; x <= x2; x++) {
                    final int currentValue = dimension.getLayerValueAt(layer, x, y);
                    final float strength = dynamicLevel * getFullStrength(centreX, centreY, x, y);
                    if (strength != 0f) {
                        int targetValue = 14 - (int) (strength * 14 + 0.f);
                        if (targetValue < currentValue) {
                            dimension.setLayerValueAt(layer, x, y, targetValue);
                        }
                    }
                }
            }
        }
    }

    @Override
    public void applyPixel(Dimension dimension, int x, int y) {
        final Tile tile = dimension.getTileForEditing(x >> TILE_SIZE_BITS, y >> TILE_SIZE_BITS);
        if (tile != null) {
            final int xInTile = x & TILE_SIZE_MASK, yInTile = y & TILE_SIZE_MASK;
            final int value = 1 + Math.round(brush.getLevel() * 14);
            if (tile.getLayerValue(layer, xInTile, yInTile) < value) {
                tile.setLayerValue(layer, xInTile, yInTile, value);
            }
        }
    }

    @Override
    public void removePixel(Dimension dimension, int x, int y) {
        dimension.setLayerValueAt(layer, x, y, 0);
    }
}