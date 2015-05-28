package org.pepsoft.worldpainter.operations;

import org.pepsoft.worldpainter.MapDragControl;
import org.pepsoft.worldpainter.RadiusControl;
import org.pepsoft.worldpainter.WorldPainterView;
import org.pepsoft.worldpainter.painting.DimensionPainter;
import org.pepsoft.worldpainter.painting.Paint;

/**
 * Created by pepijn on 14-5-15.
 */
public class Paintbrush extends AbstractPaintOperation {
    public Paintbrush(WorldPainterView view, RadiusControl radiusControl, MapDragControl mapDragControl) {
        super("Paintbrush", "Paint freehand dots and lines with any terrain, layer or biome", view, radiusControl, mapDragControl, 100, "operation.paintbrush");
    }

    @Override
    protected void tick(int centreX, int centreY, boolean inverse, boolean first, float dynamicLevel) {
        painter.setUndo(inverse);
        painter.drawPoint(centreX, centreY, dynamicLevel);
    }

    @Override
    protected void activate() {
        super.activate();
        painter.setDimension(getDimension());
    }

    @Override
    protected void paintChanged(Paint newPaint) {
        painter.setPaint(newPaint);
    }

    private final DimensionPainter painter = new DimensionPainter();
}