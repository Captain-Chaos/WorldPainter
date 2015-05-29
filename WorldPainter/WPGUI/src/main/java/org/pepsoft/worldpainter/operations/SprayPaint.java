package org.pepsoft.worldpainter.operations;

import org.pepsoft.worldpainter.MapDragControl;
import org.pepsoft.worldpainter.RadiusControl;
import org.pepsoft.worldpainter.WorldPainterView;
import org.pepsoft.worldpainter.painting.DimensionPainter;
import org.pepsoft.worldpainter.painting.Paint;

/**
 * Created by pepijn on 14-5-15.
 */
public class SprayPaint extends AbstractPaintOperation {
    public SprayPaint(WorldPainterView view, RadiusControl radiusControl, MapDragControl mapDragControl) {
        super("Spray Paint", "Spray paint any terrain, layer or biome onto the world", view, radiusControl, mapDragControl, 100, "operation.sprayPaint");
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
        newPaint.setDither(true);
        painter.setPaint(newPaint);
    }

    private final DimensionPainter painter = new DimensionPainter();
}