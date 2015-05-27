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

import org.pepsoft.worldpainter.Terrain;
import org.pepsoft.worldpainter.brushes.Brush;
import org.pepsoft.worldpainter.layers.Layer;
import org.pepsoft.worldpainter.operations.Filter;

/**
 * Created by pepijn on 15-05-15.
 */
public final class PaintFactory {
    private PaintFactory() {
        // Prevent instantiation
    }

    public static Paint createLayerPaint(Layer layer) {
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

    public static void updatePaint(Paint paint, Brush brush) {

    }

    public static void updatePaint(Paint paint, Filter filter) {

    }
}