package org.pepsoft.worldpainter.selection;

import org.pepsoft.util.ProgressReceiver;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.WorldPainter;
import org.pepsoft.worldpainter.WorldPainterView;
import org.pepsoft.worldpainter.brushes.BrushShape;
import org.pepsoft.worldpainter.operations.MouseOrTabletOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.beans.PropertyVetoException;

/**
 * Created by Pepijn Schmitz on 03-11-16.
 */
public class CopySelectionOperation extends MouseOrTabletOperation {
    public CopySelectionOperation(WorldPainterView view) {
        super("Copy Selection", "Copy the selection to another location", view, "operation.selection.copy", "copy_selection");
    }

    @Override
    public JPanel getOptionsPanel() {
        return optionsPanel;
    }

    @Override
    protected void activate() throws PropertyVetoException {
        super.activate();
        selectionHelper = new SelectionHelper(getDimension());
        WorldPainter view = (WorldPainter) getView();
        Rectangle bounds = selectionHelper.getSelectionBounds();
        if (bounds == null) {
            super.deactivate();
            throw new PropertyVetoException("No active selection", null);
        }
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
        selectionHelper.setOptions(options);
        Dimension dimension = getDimension();
        dimension.setEventsInhibited(true);
        try {
            // TODO: opening the progress dialog (and presumeably any dialog)
            // causes the JIDE docking framework to malfunction
//            ProgressDialog.executeTask(SwingUtilities.getWindowAncestor(getView()), new ProgressTask<Void>() {
//                @Override
//                public String getName() {
//                    return "Copying selection";
//                }
//
//                @Override
//                public Void execute(ProgressReceiver progressReceiver) throws ProgressReceiver.OperationCancelled {
                    selectionHelper.copySelection(centreX, centreY, null);
//                    return null;
//                }
//            }, false);
        } catch (ProgressReceiver.OperationCancelled e) {
            throw new InternalError(e);
        } finally {
            dimension.setEventsInhibited(false);
        }
    }

    private SelectionHelper selectionHelper;
    private final CopySelectionOperationOptions options = new CopySelectionOperationOptions();
    private final CopySelectionOperationOptionsPanel optionsPanel = new CopySelectionOperationOptionsPanel(options);

    private static final Logger logger = LoggerFactory.getLogger(CopySelectionOperation.class);
}