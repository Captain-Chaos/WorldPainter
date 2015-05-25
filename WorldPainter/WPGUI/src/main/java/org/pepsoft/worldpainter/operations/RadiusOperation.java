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

import java.lang.reflect.InvocationTargetException;
import javax.swing.SwingUtilities;
import jpen.PButton;
import jpen.PButtonEvent;
import jpen.PKind;
import jpen.PKindEvent;
import org.pepsoft.worldpainter.MapDragControl;
import org.pepsoft.worldpainter.RadiusControl;
import org.pepsoft.worldpainter.WorldPainterView;
import org.pepsoft.worldpainter.brushes.Brush;

/**
 * An operation which uses a brush to affect an area of the dimension.
 *
 * @author pepijn
 */
public abstract class RadiusOperation extends MouseOrTabletOperation {
    public RadiusOperation(String name, String description, WorldPainterView view, RadiusControl radiusControl, MapDragControl mapDragControl, String statisticsKey) {
        super(name, description, view, statisticsKey);
        this.radiusControl = radiusControl;
        this.mapDragControl = mapDragControl;
    }

    public RadiusOperation(String name, String description, WorldPainterView view, RadiusControl radiusControl, MapDragControl mapDragControl, int delay, String statisticsKey) {
        super(name, description, view, delay, statisticsKey);
        this.radiusControl = radiusControl;
        this.mapDragControl = mapDragControl;
    }

    public final Brush getBrush() {
        return brush;
    }

    public final void setBrush(Brush brush) {
        this.brush = brush;
        if (brush != null) {
            brush.setRadius(radius);
            brush.setLevel(getLevel());
        }
        brushChanged();
    }

    public final int getEffectiveRadius() {
        return (brush != null) ? brush.getEffectiveRadius() : radius;
    }

    public final void setRadius(int radius) {
        if (radius < 0) {
            throw new IllegalArgumentException();
        }
        this.radius = radius;
        if (brush != null) {
            brush.setRadius(radius);
            brushChanged();
        }
    }

    public final float getStrength(int centerX, int centerY, int x, int y) {
        return filterEnabled
            ? filter.modifyStrength(x, y, brush.getStrength(centerX, centerY, x, y))
            : brush.getStrength(centerX, centerY, x, y);
    }

    public final float getFullStrength(int centerX, int centerY, int x, int y) {
        return filterEnabled
            ? filter.modifyStrength(x, y, brush.getFullStrength(centerX, centerY, x, y))
            : brush.getFullStrength(centerX, centerY, x, y);
    }
    
    @Override
    public void setLevel(float level) {
        super.setLevel(level);
        if (brush != null) {
            brush.setLevel(level);
        }
    }
    
    @Override
    public void penKindEvent(PKindEvent pke) {
        final PKind.Type type = pke.kind.getType();
        if ((type != PKind.Type.ERASER) && (type != PKind.Type.STYLUS)) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    mapDragControl.setMapDraggingInhibited(false);
                }
            });
        }
        super.penKindEvent(pke);
    }
    
    @Override
    public void penButtonEvent(PButtonEvent pbe) {
        PKind.Type penKindType = pbe.pen.getKind().getType();
        final PButton.Type buttonType = pbe.button.getType();
        if (pbe.button.value
                && ((penKindType == PKind.Type.STYLUS) || (penKindType == PKind.Type.ERASER))
                && ((buttonType == PButton.Type.CENTER) || (buttonType == PButton.Type.RIGHT))) {
            try {
                SwingUtilities.invokeAndWait(new Runnable() {
                    @Override
                    public void run() {
                        // Stylus button pressed
                        if (buttonType == PButton.Type.CENTER) {
                            radiusControl.decreaseRadius(1);
                        } else {
                            radiusControl.increaseRadius(1);
                        }
                        // It should not be too late to do this, since this
                        // event is being dispatched synchronously:
                        if (! mapDragControl.isMapDraggingInhibited()) {
                            mapDragControl.setMapDraggingInhibited(true);
                        }
                    }
                });
            } catch (InterruptedException e) {
                throw new RuntimeException("Thread interrupted while processing radius event", e);
            } catch (InvocationTargetException e) {
                throw new RuntimeException("Exception thrown while processing radius event", e);
            }
        } else {
            super.penButtonEvent(pbe);
        }
    }

    public Filter getFilter() {
        return filter;
    }

    public void setFilter(Filter filter) {
        this.filter = filter;
        filterEnabled = (filter != null);
    }

    @Override
    protected void deactivate() {
        mapDragControl.setMapDraggingInhibited(false);
        super.deactivate();
    }
    
    protected void brushChanged() {
        // Do nothing
    }

    private final RadiusControl radiusControl;
    private final MapDragControl mapDragControl;
    private int radius;
    private Brush brush = null;
    private boolean filterEnabled;
    private Filter filter = null;
}