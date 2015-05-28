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
import org.pepsoft.worldpainter.layers.Layer;

import java.awt.image.BufferedImage;

/**
 * An abstract base class for layer Paints.
 *
 * Created by pepijn on 15-05-15.
 */
public abstract class LayerPaint extends AbstractPaint {
    protected LayerPaint(Layer layer) {
        this.layer = layer;
    }

    public final Layer getLayer() {
        return layer;
    }

    @Override
    public final BufferedImage getIcon(ColourScheme colourScheme) {
        return layer.getIcon();
    }

    protected final Layer layer;
}