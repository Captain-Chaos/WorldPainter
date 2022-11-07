package org.pepsoft.worldpainter.selection;

import org.pepsoft.util.ObservableBoolean;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.MapDragControl;
import org.pepsoft.worldpainter.RadiusControl;
import org.pepsoft.worldpainter.WorldPainterView;
import org.pepsoft.worldpainter.brushes.Brush;
import org.pepsoft.worldpainter.brushes.RotatedBrush;
import org.pepsoft.worldpainter.operations.RadiusOperation;
import org.pepsoft.worldpainter.operations.StandardOptionsPanel;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.Path2D;

/**
 * Created by Pepijn Schmitz on 03-11-16.
 */
public class EditSelectionOperation extends RadiusOperation {
    public EditSelectionOperation(WorldPainterView view, RadiusControl radiusControl, MapDragControl mapDragControl, ObservableBoolean selectionState) {
        super("Edit Selection", "Expand or shrink the selection", view, radiusControl, mapDragControl, "operation.selection.edit", "edit_selection");
        this.selectionState = selectionState;
    }

    @Override
    public JPanel getOptionsPanel() {
        return OPTIONS_PANEL;
    }

    @Override
    protected void tick(int centreX, int centreY, boolean inverse, boolean first, float dynamicLevel) {
        final Dimension dimension = getDimension();
        if (dimension == null) {
            // Probably some kind of race condition
            return;
        }

        // Create a geometric shape corresponding to the brush size, shape and
        // rotation
        Shape shape;
        final Brush brush = getBrush();
        final int brushRadius = brush.getRadius();
        switch (brush.getBrushShape()) {
            case BITMAP:
            case SQUARE:
                shape = new Rectangle(centreX - brushRadius, centreY - brushRadius, brushRadius * 2 + 1, brushRadius * 2 + 1);
                if (brush instanceof RotatedBrush) {
                    int rotation = ((RotatedBrush) brush).getDegrees();
                    if (rotation != 0) {
                        shape = new Path2D.Float(shape, AffineTransform.getRotateInstance(rotation / DEGREES_TO_RADIANS, centreX, centreY));
                    }
                }
                break;
            case CIRCLE:
                shape = new Arc2D.Float(centreX - brushRadius, centreY - brushRadius, brushRadius * 2 + 1, brushRadius * 2 + 1, 0.0f, 360.0f, Arc2D.CHORD);
                break;
            default:
                throw new InternalError();
        }

        dimension.setEventsInhibited(true);
        try {
            SelectionHelper selectionHelper = new SelectionHelper(dimension);
            if (inverse) {
                selectionHelper.removeFromSelection(shape);
            } else {
                selectionHelper.addToSelection(shape);
                // TODO: make this work correctly with undo/redo, and make "inside selection" ineffective when there is no selection, to avoid confusion
//                selectionState.setValue(true);
            }
        } finally {
            dimension.setEventsInhibited(false);
        }
    }

    private final ObservableBoolean selectionState;

    private static final JPanel OPTIONS_PANEL = new StandardOptionsPanel("Edit Selection", "<ul><li>Left-click to add to the selection<li>Right-click to remove from the selection</ul>");
    private static final double DEGREES_TO_RADIANS = 360 / (Math.PI * 2);
}