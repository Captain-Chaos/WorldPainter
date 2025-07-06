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

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.worldpainter.operations;

import org.pepsoft.worldpainter.WorldPainterView;

/**
 * Deprecated. Only exists for backwards compatibility with existin plugins. Do not use.
 *
 * @deprecated Use {@link AbstractBrushOperation}.
 * @author pepijn
 */
@Deprecated
public abstract class RadiusOperation extends AbstractBrushOperation implements FilteredOperation {
    /**
     * @deprecated Use {@link AbstractBrushOperation}.
     */
    @Deprecated
    public RadiusOperation(String name, String description, WorldPainterView view, String statisticsKey) {
        super(name, description, view, statisticsKey);
    }

    /**
     * @deprecated Use {@link AbstractBrushOperation}.
     */
    @Deprecated
    public RadiusOperation(String name, String description, WorldPainterView view, String statisticsKey, String iconName) {
        super(name, description, view, statisticsKey, iconName);
    }

    /**
     * @deprecated Use {@link AbstractBrushOperation}.
     */
    @Deprecated
    public RadiusOperation(String name, String description, WorldPainterView view, int delay, String statisticsKey) {
        super(name, description, view, delay, statisticsKey);
    }

    /**
     * @deprecated Use {@link AbstractBrushOperation}.
     */
    @Deprecated
    public RadiusOperation(String name, String description, WorldPainterView view, int delay, String statisticsKey, String iconName) {
        super(name, description, view, delay, statisticsKey, iconName);
    }
}