package org.pepsoft.worldpainter.selection;

import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.WorldPainterView;
import org.pepsoft.worldpainter.operations.MouseOrTabletOperation;

/**
 * Created by Pepijn Schmitz on 03-11-16.
 */
public class CopySelectionOperation extends MouseOrTabletOperation {
    public CopySelectionOperation(WorldPainterView view) {
        super("Copy Selection", "Copy the selection to another location", view, "operation.selection.copy", "copy_selection");
    }

    @Override
    protected void tick(int centreX, int centreY, boolean inverse, boolean first, float dynamicLevel) {
        Dimension dimension = getDimension();
        dimension.setEventsInhibited(true);
        try {
            new SelectionHelper(dimension).copySelection(centreX, centreY);
        } finally {
            dimension.setEventsInhibited(false);
        }
    }
}