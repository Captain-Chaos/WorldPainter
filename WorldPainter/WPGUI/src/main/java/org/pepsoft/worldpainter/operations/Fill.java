package org.pepsoft.worldpainter.operations;

import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.WorldPainterView;
import org.pepsoft.worldpainter.brushes.Brush;
import org.pepsoft.worldpainter.painting.DimensionPainter;
import org.pepsoft.worldpainter.painting.Paint;

import javax.swing.*;

/**
 * Created by pepijn on 14-5-15.
 */
public class Fill extends MouseOrTabletOperation implements PaintOperation, BrushOperation {
    public Fill(WorldPainterView view) {
        super("Fill", "Flood fill an area of the world with any kind of layer or terrain", view, "operation.fill");
    }

    @Override
    protected void tick(int centreX, int centreY, boolean inverse, boolean first, float dynamicLevel) {
        painter.setUndo(inverse);
        Dimension dimension = getDimension();
        dimension.setEventsInhibited(true);
        try {
            painter.fill(dimension, centreX, centreY, SwingUtilities.getWindowAncestor(getView()));
        } finally {
            dimension.setEventsInhibited(false);
        }
    }

    @Override
    public Paint getPaint() {
        return painter.getPaint();
    }

    @Override
    public void setPaint(Paint paint) {
        if ((brush != null) && (paint != null)) {
            paint.setBrush(brush);
        }
        painter.setPaint(paint);
    }

    @Override
    public Brush getBrush() {
        return brush;
    }

    @Override
    public void setBrush(Brush brush) {
        this.brush = brush;
        if (painter.getPaint() != null) {
            painter.getPaint().setBrush(brush);
        }
    }

    private final DimensionPainter painter = new DimensionPainter();
    private Brush brush;
}