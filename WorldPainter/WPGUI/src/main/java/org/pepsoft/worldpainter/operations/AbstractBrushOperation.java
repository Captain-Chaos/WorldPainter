package org.pepsoft.worldpainter.operations;

import jpen.PButton;
import jpen.PButtonEvent;
import jpen.PKind;
import jpen.PKindEvent;
import org.pepsoft.worldpainter.MapDragControl;
import org.pepsoft.worldpainter.RadiusControl;
import org.pepsoft.worldpainter.WorldPainterView;
import org.pepsoft.worldpainter.brushes.Brush;

import javax.swing.*;
import java.lang.reflect.InvocationTargetException;

/**
 * An operation which needs access to the current brush, including radius and filter support.
 *
 * @author pepijn
 */
public abstract class AbstractBrushOperation extends MouseOrTabletOperation implements BrushOperation, FilteredOperation {
    protected AbstractBrushOperation(String name, String description, String statisticsKey) {
        super(name, description, statisticsKey);
    }

    protected AbstractBrushOperation(String name, String description, String statisticsKey, String iconName) {
        super(name, description, statisticsKey, iconName);
    }

    protected AbstractBrushOperation(String name, String description, int delay, String statisticsKey) {
        super(name, description, delay, statisticsKey);
    }

    protected AbstractBrushOperation(String name, String description, int delay, String statisticsKey, String iconName) {
        super(name, description, delay, statisticsKey, iconName);
    }

    /**
     * @deprecated Use {@link #AbstractBrushOperation(String, String, String)}.
     */
    @Deprecated
    protected AbstractBrushOperation(String name, String description, WorldPainterView view, String statisticsKey) {
        super(name, description, view, statisticsKey);
    }

    /**
     * @deprecated Use {@link #AbstractBrushOperation(String, String, String, String)}.
     */
    @Deprecated
    protected AbstractBrushOperation(String name, String description, WorldPainterView view, String statisticsKey, String iconName) {
        super(name, description, view, statisticsKey, iconName);
    }

    /**
     * @deprecated Use {@link #AbstractBrushOperation(String, String, int, String)}.
     */
    @Deprecated
    protected AbstractBrushOperation(String name, String description, WorldPainterView view, int delay, String statisticsKey) {
        super(name, description, view, delay, statisticsKey);
    }

    /**
     * @deprecated Use {@link #AbstractBrushOperation(String, String, int, String, String)}.
     */
    @Deprecated
    protected AbstractBrushOperation(String name, String description, WorldPainterView view, int delay, String statisticsKey, String iconName) {
        super(name, description, view, delay, statisticsKey, iconName);
    }

    /**
     * Convenience method for implementations for getting the strength with which to apply an operation for a set of
     * coordinates taking into account the current brush, intensity and filter settings.
     *
     * @param centerX The X coordinate where the operation is being applied.
     * @param centerY The Y coordinate where the operation is being applied.
     * @param x       The X coordinate for which the applicable strength is being queries.
     * @param y       The X coordinate for which the applicable strength is being queries.
     * @return A number from 0.0 (do not apply operation) to 1.0 (apply operation maximally)
     */
    public final float getStrength(int centerX, int centerY, int x, int y) {
        return filterEnabled
                ? filter.modifyStrength(x, y, getBrush().getStrength(x - centerX, y - centerY))
                : getBrush().getStrength(x - centerX, y - centerY);
    }

    /**
     * Convenience method for implementations for getting the strength with which to apply an operation for a set of
     * coordinates taking into account the current brush and filter settings, <em>except</em> the intensity, which is
     * assumed to be 100%.
     *
     * @param centerX The X coordinate where the operation is being applied.
     * @param centerY The Y coordinate where the operation is being applied.
     * @param x       The X coordinate for which the applicable strength is being queries.
     * @param y       The X coordinate for which the applicable strength is being queries.
     * @return A number from 0.0 (do not apply operation) to 1.0 (apply operation maximally)
     */
    public final float getFullStrength(int centerX, int centerY, int x, int y) {
        return filterEnabled
                ? filter.modifyStrength(x, y, getBrush().getFullStrength(x - centerX, y - centerY))
                : getBrush().getFullStrength(x - centerX, y - centerY);
    }

    /**
     * Convenience method for implementations for getting the effective radius of the current brush given the current
     * radius.
     *
     * @return The effective radius of the current brush given the current radius.
     */
    public final int getEffectiveRadius() {
        return (getBrush() != null) ? getBrush().getEffectiveRadius() : radius;
    }

    // BrushOperation

    @Override
    public final Brush getBrush() {
        return brush;
    }

    @Override
    public final void setBrush(Brush brush) {
        this.brush = brush;
        if (brush != null) {
            brush.setLevel(getLevel());
        }
        brushChanged(brush);
    }

    @Override
    public final int getRadius() {
        return radius;
    }

    @Override
    public final void setRadius(int radius) {
        if (radius < 0) {
            throw new IllegalArgumentException();
        }
        this.radius = radius;
        final Brush brush = getBrush();
        if (brush != null) {
            brush.setRadius(radius);
            brushChanged(brush);
        }
    }

    // MouseOrTabletOperation

    @Override
    public void setView(WorldPainterView view) {
        super.setView(view);
        radiusControl = view.getRadiusControl();
        mapDragControl = view.getMapDragControl();
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

    @Override
    protected void deactivate() {
        mapDragControl.setMapDraggingInhibited(false);
        super.deactivate();
    }

    // FilteredOperation

    @Override
    public Filter getFilter() {
        return filter;
    }

    @Override
    public void setFilter(Filter filter) {
        this.filter = filter;
        filterEnabled = (filter != null);
    }

    protected void brushChanged(Brush newBrush) {
        if (newBrush != null) {
            newBrush.setRadius(radius);
        }
    }

    private RadiusControl radiusControl;
    private MapDragControl mapDragControl;
    private Brush brush;
    private int radius;
    private boolean filterEnabled;
    private Filter filter = null;
}