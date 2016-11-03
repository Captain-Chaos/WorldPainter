package org.pepsoft.worldpainter.selection;

import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.MapDragControl;
import org.pepsoft.worldpainter.RadiusControl;
import org.pepsoft.worldpainter.WorldPainterView;
import org.pepsoft.worldpainter.operations.RadiusOperation;

/**
 * Created by Pepijn Schmitz on 03-11-16.
 */
public class EditSelectionOperation extends RadiusOperation {
    public EditSelectionOperation(WorldPainterView view, RadiusControl radiusControl, MapDragControl mapDragControl) {
        super("EditSelectionOperation", "Expand or shrink the selection", view, radiusControl, mapDragControl, "operation.selection.edit");
    }

    @Override
    protected void tick(int centreX, int centreY, boolean inverse, boolean first, float dynamicLevel) {
        Dimension dimension = getDimension();
        dimension.setEventsInhibited(true);
        try {
            if (inverse) {
                SelectionHelper.removeFromSelection(dimension, getBrush(), centreX, centreY);
            } else {
                SelectionHelper.addToSelection(dimension, getBrush(), centreX, centreY);
            }
        } finally {
            dimension.setEventsInhibited(false);
        }
    }
}
