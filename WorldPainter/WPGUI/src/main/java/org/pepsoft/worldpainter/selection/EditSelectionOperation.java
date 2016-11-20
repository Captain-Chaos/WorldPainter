package org.pepsoft.worldpainter.selection;

import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.MapDragControl;
import org.pepsoft.worldpainter.RadiusControl;
import org.pepsoft.worldpainter.WorldPainterView;
import org.pepsoft.worldpainter.operations.RadiusOperation;

import javax.swing.*;

/**
 * Created by Pepijn Schmitz on 03-11-16.
 */
public class EditSelectionOperation extends RadiusOperation {
    public EditSelectionOperation(WorldPainterView view, RadiusControl radiusControl, MapDragControl mapDragControl, JToggleButton copySelectionButton) {
        super("Edit Selection", "Expand or shrink the selection", view, radiusControl, mapDragControl, "operation.selection.edit", "edit_selection");
        this.copySelectionButton = copySelectionButton;
    }

    @Override
    protected void tick(int centreX, int centreY, boolean inverse, boolean first, float dynamicLevel) {
        Dimension dimension = getDimension();
        dimension.setEventsInhibited(true);
        try {
            SelectionHelper selectionHelper = new SelectionHelper(dimension);
            if (inverse) {
                selectionHelper.removeFromSelection(getBrush(), centreX, centreY);
            } else {
                selectionHelper.addToSelection(getBrush(), centreX, centreY);
                if (! copySelectionButton.isEnabled()) {
                    copySelectionButton.setEnabled(true);
                }
            }
        } finally {
            dimension.setEventsInhibited(false);
        }
    }

    private final JToggleButton copySelectionButton;
}
