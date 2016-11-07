package org.pepsoft.worldpainter.selection;

import org.pepsoft.util.ProgressReceiver;
import org.pepsoft.util.swing.ProgressDialog;
import org.pepsoft.util.swing.ProgressTask;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.WorldPainter;
import org.pepsoft.worldpainter.WorldPainterView;
import org.pepsoft.worldpainter.brushes.BrushShape;
import org.pepsoft.worldpainter.operations.MouseOrTabletOperation;

import javax.swing.*;
import java.awt.*;

/**
 * Created by Pepijn Schmitz on 03-11-16.
 */
public class CopySelectionOperation extends MouseOrTabletOperation {
    public CopySelectionOperation(WorldPainterView view) {
        super("Copy Selection", "Copy the selection to another location", view, "operation.selection.copy", "copy_selection");
    }

    @Override
    public CopySelectionOperationOptionsPanel getOptionsPanel() {
        return optionsPanel;
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
        System.err.printf("tick(%d, %d, %b, %b, %f)%n", centreX, centreY, inverse, first, dynamicLevel);
        new Throwable().printStackTrace();
        selectionHelper.setOptions(options);
        Dimension dimension = getDimension();
        dimension.setEventsInhibited(true);
        try {
            ProgressDialog.executeTask(SwingUtilities.getWindowAncestor(getView()), new ProgressTask<Void>() {
                @Override
                public String getName() {
                    return "Copying selection";
                }

                @Override
                public Void execute(ProgressReceiver progressReceiver) throws ProgressReceiver.OperationCancelled {
                    selectionHelper.copySelection(centreX, centreY, progressReceiver);
                    return null;
                }
            }, false);
        } finally {
            dimension.setEventsInhibited(false);
        }
    }

    private SelectionHelper selectionHelper;
    private final CopySelectionOperationOptions options = new CopySelectionOperationOptions();
    private final CopySelectionOperationOptionsPanel optionsPanel = new CopySelectionOperationOptionsPanel(options);
}