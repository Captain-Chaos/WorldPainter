/*
 * WorldPainter, a graphical and interactive map generator for Minecraft.
 * Copyright � 2011-2015  pepsoft.org, The Netherlands
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

import org.pepsoft.worldpainter.brushes.Brush;
import org.pepsoft.worldpainter.operations.Filter;

/**
 * An abstract base class for Paints.
 *
 * Created by pepijn on 20-05-15.
 */
public abstract class AbstractPaint implements Paint {
    @Override
    public final Brush getBrush() {
        return brush;
    }

    @Override
    public final void setBrush(Brush brush) {
        this.brush = brush;
    }

    @Override
    public final Filter getFilter() {
        return filter;
    }

    @Override
    public final void setFilter(Filter filter) {
        this.filter = filter;
        filterEnabled = (filter != null);
    }

    @Override
    public boolean isDither() {
        return dither;
    }

    @Override
    public void setDither(boolean dither) {
        this.dither = dither;
    }

    protected final float getStrength(int centerX, int centerY, int x, int y) {
        return filterEnabled
            ? filter.modifyStrength(x, y, brush.getStrength(x - centerX, y - centerY))
            : brush.getStrength(x - centerX, y - centerY);
    }

    protected final float getFullStrength(int centerX, int centerY, int x, int y) {
        return filterEnabled
            ? filter.modifyStrength(x, y, brush.getFullStrength(x - centerX, y - centerY))
            : brush.getFullStrength(x - centerX, y - centerY);
    }

    protected Filter filter;
    protected Brush brush;
    protected boolean dither, filterEnabled;
}