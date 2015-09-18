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

import org.pepsoft.worldpainter.ColourScheme;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.Terrain;
import org.pepsoft.worldpainter.brushes.Brush;
import org.pepsoft.worldpainter.layers.CombinedLayer;
import org.pepsoft.worldpainter.layers.Layer;
import org.pepsoft.worldpainter.operations.Filter;

import java.awt.image.BufferedImage;

/**
 * Created by pepijn on 15-05-15.
 */
public final class PaintFactory {
    private PaintFactory() {
        // Prevent instantiation
    }

    public static Paint createLayerPaint(Layer layer) {
        if (layer instanceof CombinedLayer) {
            return new CombinedLayerPaint((CombinedLayer) layer);
        } else {
            switch (layer.getDataSize()) {
                case BIT:
                case BIT_PER_CHUNK:
                    return new BitLayerPaint(layer);
                case NIBBLE:
                    return new NibbleLayerPaint(layer);
                default:
                    throw new UnsupportedOperationException("Data size " + layer.getDataSize() + " not supported");
            }
        }
    }

    public static Paint createDiscreteLayerPaint(Layer layer, int value) {
        switch (layer.getDataSize()) {
            case NIBBLE:
            case BYTE:
                return new DiscreteLayerPaint(layer, value);
            default:
                throw new UnsupportedOperationException("Data size " + layer.getDataSize() + " not supported");
        }
    }

    public static Paint createTerrainPaint(Terrain terrain) {
        return new TerrainPaint(terrain);
    }

    public static class NullPaint implements Paint {
        @Override public Brush getBrush() {return null;}
        @Override public void setBrush(Brush brush) {}
        @Override public Filter getFilter() {return null;}
        @Override public void setFilter(Filter filter) {}
        @Override public boolean isDither() {return false;}
        @Override public void setDither(boolean dither) {}
        @Override public void apply(Dimension dimension, int x, int y, float dynamicLevel) {}
        @Override public void remove(Dimension dimension, int x, int y, float dynamicLevel) {}
        @Override public void applyPixel(Dimension dimension, int x, int y) {}
        @Override public void removePixel(Dimension dimension, int x, int y) {}
        @Override public BufferedImage getIcon(ColourScheme colourScheme) {return null;}
    }

    public static final Paint NULL_PAINT = new NullPaint();
}