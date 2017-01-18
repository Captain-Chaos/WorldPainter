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
public abstract class RadiusOperation extends MouseOrTabletOperation implements BrushOperation {
    /**
     * Create a new <code>RadiusOperation</code>.
     *
     * @param name The short name of the operation. May be displayed on the operation's tool button.
     * @param description A longer description of the operation. May be displayed to the user as a tooltip.
     * @param view The WorldPainter view through which the dimension that is being edited is being displayed and on
     *             which the operation should install its listeners to register user mouse, keyboard and tablet actions.
     * @param radiusControl An object through which the operation can change the size of the brush.
     * @param mapDragControl An object through which the operation can temporarily disable map panning by dragging the
     *                       mouse.
     * @param statisticsKey The key with which use of this operation will be logged in the usage data sent back to the
     *                      developer. Should start with a reverse-DNS style identifier, optionally followed by some
     *                      basic or fundamental setting, if it has one.
     */
    public RadiusOperation(String name, String description, WorldPainterView view, RadiusControl radiusControl, MapDragControl mapDragControl, String statisticsKey) {
        super(name, description, view, statisticsKey);
        this.radiusControl = radiusControl;
        this.mapDragControl = mapDragControl;
    }

    /**
     * Create a new <code>RadiusOperation</code>.
     *
     * @param name The short name of the operation. May be displayed on the operation's tool button.
     * @param description A longer description of the operation. May be displayed to the user as a tooltip.
     * @param view The WorldPainter view through which the dimension that is being edited is being displayed and on
     *             which the operation should install its listeners to register user mouse, keyboard and tablet actions.
     * @param radiusControl An object through which the operation can change the size of the brush.
     * @param mapDragControl An object through which the operation can temporarily disable map panning by dragging the
     *                       mouse.
     * @param statisticsKey The key with which use of this operation will be logged in the usage data sent back to the
     *                      developer. Should start with a reverse-DNS style identifier, optionally followed by some
     *                      basic or fundamental setting, if it has one.
     * @param iconName The base name of the icon for the operation.
     */
    public RadiusOperation(String name, String description, WorldPainterView view, RadiusControl radiusControl, MapDragControl mapDragControl, String statisticsKey, String iconName) {
        super(name, description, view, statisticsKey, iconName);
        this.radiusControl = radiusControl;
        this.mapDragControl = mapDragControl;
    }

    /**
     * Create a new <code>RadiusOperation</code>.
     *
     * @param name The short name of the operation. May be displayed on the operation's tool button.
     * @param description A longer description of the operation. May be displayed to the user as a tooltip.
     * @param view The WorldPainter view through which the dimension that is being edited is being displayed and on
     *             which the operation should install its listeners to register user mouse, keyboard and tablet actions.
     * @param radiusControl An object through which the operation can change the size of the brush.
     * @param mapDragControl An object through which the operation can temporarily disable map panning by dragging the
     *                       mouse.
     * @param delay The delay in ms between each invocation of {@link #tick(int, int, boolean, boolean, float)} while
     *              this operation is being applied by the user.
     * @param statisticsKey The key with which use of this operation will be logged in the usage data sent back to the
     *                      developer. Should start with a reverse-DNS style identifier, optionally followed by some
     *                      basic or fundamental setting, if it has one.
     */
    public RadiusOperation(String name, String description, WorldPainterView view, RadiusControl radiusControl, MapDragControl mapDragControl, int delay, String statisticsKey) {
        super(name, description, view, delay, statisticsKey);
        this.radiusControl = radiusControl;
        this.mapDragControl = mapDragControl;
    }

    /**
     * Create a new <code>RadiusOperation</code>.
     *
     * @param name The short name of the operation. May be displayed on the operation's tool button.
     * @param description A longer description of the operation. May be displayed to the user as a tooltip.
     * @param view The WorldPainter view through which the dimension that is being edited is being displayed and on
     *             which the operation should install its listeners to register user mouse, keyboard and tablet actions.
     * @param radiusControl An object through which the operation can change the size of the brush.
     * @param mapDragControl An object through which the operation can temporarily disable map panning by dragging the
     *                       mouse.
     * @param delay The delay in ms between each invocation of {@link #tick(int, int, boolean, boolean, float)} while
     *              this operation is being applied by the user.
     * @param statisticsKey The key with which use of this operation will be logged in the usage data sent back to the
     *                      developer. Should start with a reverse-DNS style identifier, optionally followed by some
     *                      basic or fundamental setting, if it has one.
     * @param iconName The base name of the icon for the operation.
     */
    public RadiusOperation(String name, String description, WorldPainterView view, RadiusControl radiusControl, MapDragControl mapDragControl, int delay, String statisticsKey, String iconName) {
        super(name, description, view, delay, statisticsKey, iconName);
        this.radiusControl = radiusControl;
        this.mapDragControl = mapDragControl;
    }

    @Override
    public final Brush getBrush() {
        return brush;
    }

    @Override
    public final void setBrush(Brush brush) {
        this.brush = brush;
        if (brush != null) {
            brush.setRadius(radius);
            brush.setLevel(getLevel());
        }
        brushChanged(brush);
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
            brushChanged(brush);
        }
    }

    public final float getStrength(int centerX, int centerY, int x, int y) {
        return filterEnabled
            ? filter.modifyStrength(x, y, brush.getStrength(x - centerX, y - centerY))
            : brush.getStrength(x - centerX, y - centerY);
    }

    public final float getFullStrength(int centerX, int centerY, int x, int y) {
        return filterEnabled
            ? filter.modifyStrength(x, y, brush.getFullStrength(x - centerX, y - centerY))
            : brush.getFullStrength(x - centerX, y - centerY);
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
            SwingUtilities.invokeLater(() -> mapDragControl.setMapDraggingInhibited(false));
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
                SwingUtilities.invokeAndWait(() -> {
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
    
    protected void brushChanged(Brush newBrush) {
        // Do nothing
    }

    private final RadiusControl radiusControl;
    private final MapDragControl mapDragControl;
    private int radius;
    private Brush brush = null;
    private boolean filterEnabled;
    private Filter filter = null;
}