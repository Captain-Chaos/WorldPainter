package org.pepsoft.worldpainter.operations;

import org.pepsoft.worldpainter.WorldPainterView;
import org.pepsoft.worldpainter.brushes.Brush;

import java.beans.PropertyVetoException;

/**
 * An operation which needs access to the current brush.
 *
 * @author pepijn
 */
public abstract class AbstractBrushOperation extends MouseOrTabletOperation implements BrushOperation {
    protected AbstractBrushOperation(String name, String description, WorldPainterView view, String statisticsKey) {
        super(name, description, view, statisticsKey);
    }

    protected AbstractBrushOperation(String name, String description, WorldPainterView view, String statisticsKey, String iconName) {
        super(name, description, view, statisticsKey, iconName);
    }

    protected AbstractBrushOperation(String name, String description, WorldPainterView view, int delay, String statisticsKey) {
        super(name, description, view, delay, statisticsKey);
    }

    protected AbstractBrushOperation(String name, String description, WorldPainterView view, int delay, String statisticsKey, String iconName) {
        super(name, description, view, delay, statisticsKey, iconName);
    }

    protected AbstractBrushOperation(String name, String description, String statisticsKey) {
        super(name, description, statisticsKey);
    }

    protected AbstractBrushOperation(String name, String description, int delay, String statisticsKey) {
        super(name, description, delay, statisticsKey);
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

    // MouseOrTabletOperation

    @Override
    public void setLevel(float level) {
        super.setLevel(level);
        if (brush != null) {
            brush.setLevel(level);
        }
    }

    @Override
    protected void activate() throws PropertyVetoException {
        super.activate();
        if (getView() != null) {
            getView().setDrawBrush(! hideBrush);
        }
    }

    @Override
    protected void deactivate() {
        super.deactivate();
        if ((! hideBrush) && (getView() != null)) {
            getView().setDrawBrush(false);
        }
    }

    protected void brushChanged(Brush newBrush) {
        // Do nothing
    }

    protected final void hideBrush() {
        if (! hideBrush) {
            hideBrush = true;
            if (getView() != null) {
                getView().setDrawBrush(false);
            }
        }
    }

    private Brush brush;
    private boolean hideBrush = false;
}