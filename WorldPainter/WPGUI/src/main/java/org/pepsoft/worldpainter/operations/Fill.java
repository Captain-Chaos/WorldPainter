package org.pepsoft.worldpainter.operations;

import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.WorldPainterView;
import org.pepsoft.worldpainter.brushes.Brush;
import org.pepsoft.worldpainter.painting.DimensionPainter;
import org.pepsoft.worldpainter.painting.Paint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;

/**
 * Created by pepijn on 14-5-15.
 */
public class Fill extends AbstractBrushOperation implements PaintOperation {
    public Fill(WorldPainterView view) {
        super("Fill", "Flood fill an area of the world with any kind of layer or terrain", view, "operation.fill");
    }

    @Override
    protected void tick(int centreX, int centreY, boolean inverse, boolean first, float dynamicLevel) {
        // We have seen in the wild that this sometimes gets called recursively (perhaps someone clicks to fill more
        // than once and then it takes more than two seconds so it is continued in the background and event queue
        // processing is resumed?), which causes errors, so just ignore it if we are already filling.
        if (alreadyFilling) {
            logger.debug("Fill operation already in progress; ignoring repeated invocation");
            return;
        }
        alreadyFilling = true;
        try {
            final Dimension dimension = getDimension();
            painter.setUndo(inverse);
            synchronized (dimension) {
                dimension.setEventsInhibited(true);
            }
            try {
                if (! painter.fill(dimension, centreX, centreY, SwingUtilities.getWindowAncestor(getView()))) {
                    JOptionPane.showMessageDialog(getView(), "The area to be filled was too large and may not have been completely filled.", "Area Too Large", JOptionPane.WARNING_MESSAGE);
                }
            } catch (IndexOutOfBoundsException e) {
                // This most likely indicates that the area being flooded was too large
                synchronized (dimension) {
                    if (dimension.undoChanges()) {
                        dimension.clearRedo();
                        dimension.armSavePoint();
                    }
                }
                JOptionPane.showMessageDialog(getView(), "The area to be filled is too large or complex; please retry with a smaller area", "Area Too Large", JOptionPane.ERROR_MESSAGE);
            } finally {
                synchronized (dimension) {
                    dimension.setEventsInhibited(false);
                }
            }
        } finally {
            alreadyFilling = false;
        }
    }

    @Override
    public Paint getPaint() {
        return painter.getPaint();
    }

    @Override
    public void setPaint(Paint paint) {
        if (getBrush() != null) {
            paint.setBrush(getBrush());
        }
        painter.setPaint(paint);
    }

    @Override
    protected void brushChanged(Brush newBrush) {
        if (painter.getPaint() != null) {
            painter.getPaint().setBrush(newBrush);
        }
    }

    private final DimensionPainter painter = new DimensionPainter();
    private boolean alreadyFilling;

    private static final Logger logger = LoggerFactory.getLogger(Fill.class);
}