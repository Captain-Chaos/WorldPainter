package org.pepsoft.worldpainter.operations;

import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.MapDragControl;
import org.pepsoft.worldpainter.RadiusControl;
import org.pepsoft.worldpainter.WorldPainterView;
import org.pepsoft.worldpainter.painting.DimensionPainter;
import org.pepsoft.worldpainter.painting.Paint;

import javax.swing.*;

/**
 * Created by pepijn on 14-5-15.
 */
public class SprayPaint extends AbstractPaintOperation {
    public SprayPaint(WorldPainterView view, RadiusControl radiusControl, MapDragControl mapDragControl) {
        super("Spray Paint", "Spray paint any terrain, layer or biome onto the world", view, radiusControl, mapDragControl, 100, "operation.sprayPaint");
    }

    @Override
    public JPanel getOptionsPanel() {
        return OPTIONS_PANEL;
    }

    @Override
    protected void tick(int centreX, int centreY, boolean inverse, boolean first, float dynamicLevel) {
        painter.setUndo(inverse);
        Dimension dimension = getDimension();
        dimension.setEventsInhibited(true);
        try {
            painter.drawPoint(dimension, centreX, centreY, dynamicLevel);
        } finally {
            dimension.setEventsInhibited(false);
        }
    }

    @Override
    protected void paintChanged(Paint newPaint) {
        newPaint.setDither(true);
        painter.setPaint(newPaint);
    }

    private static final JPanel OPTIONS_PANEL = new StandardOptionsPanel("Spray Paint", "<ul>\n" +
            "    <li>Left-click to spray paint the currently selected paint on the indicated location\n" +
            "    <li>Right-click with a Layer selected to remove the layer\n" +
            "    <li>Right-click with a Terrain selected to reset to the current theme\n" +
            "    <li>Right-click with a Biome selected to reset to Auto Biome\n" +
            "</ul>");
    private final DimensionPainter painter = new DimensionPainter();
}