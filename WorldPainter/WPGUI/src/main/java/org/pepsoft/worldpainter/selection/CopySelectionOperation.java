package org.pepsoft.worldpainter.selection;

import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.WorldPainter;
import org.pepsoft.worldpainter.WorldPainterView;
import org.pepsoft.worldpainter.brushes.BrushShape;
import org.pepsoft.worldpainter.operations.MouseOrTabletOperation;

import java.awt.*;

/**
 * Created by Pepijn Schmitz on 03-11-16.
 */
public class CopySelectionOperation extends MouseOrTabletOperation {
    public CopySelectionOperation(WorldPainterView view) {
        super("Copy Selection", "Copy the selection to another location", view, "operation.selection.copy", "copy_selection");
    }

    @Override
    protected void activate() {
        super.activate();
        selectionHelper = new SelectionHelper(getDimension());
        WorldPainter view = (WorldPainter) getView();
        Rectangle bounds = selectionHelper.getSelectionBounds();
        bounds.translate(-bounds.x, -bounds.y);
        view.setCustomBrushShape(bounds);
        view.setBrushShape(BrushShape.CUSTOM);
        view.setDrawBrush(true);
    }

    @Override
    protected void deactivate() {
        getView().setDrawBrush(false);
        super.deactivate();
    }

    @Override
    protected void tick(int centreX, int centreY, boolean inverse, boolean first, float dynamicLevel) {
        Dimension dimension = getDimension();
        dimension.setEventsInhibited(true);
        try {
            selectionHelper.copySelection(centreX, centreY);
        } finally {
            dimension.setEventsInhibited(false);
        }
    }

    private SelectionHelper selectionHelper;
}