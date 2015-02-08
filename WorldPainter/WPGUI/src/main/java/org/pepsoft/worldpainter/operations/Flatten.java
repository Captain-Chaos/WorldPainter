/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.worldpainter.operations;

import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.MapDragControl;
import org.pepsoft.worldpainter.RadiusControl;
import org.pepsoft.worldpainter.WorldPainter;

/**
 *
 * @author pepijn
 */
public class Flatten extends RadiusOperation {
    public Flatten(WorldPainter view, RadiusControl radiusControl, MapDragControl mapDragControl) {
        super("Flatten", "Flatten an area", view, radiusControl, mapDragControl, 100, "operation.flatten");
    }

    @Override
    protected void tick(final int centreX, final int centreY, final boolean inverse, final boolean first, final float dynamicLevel) {
        final Dimension dimension = getDimension();
        if (first) {
            targetHeight = dimension.getHeightAt(centreX, centreY);
        }
//        System.out.println("targetHeight: " + targetHeight);
        dimension.setEventsInhibited(true);
        try {
            final int radius = getEffectiveRadius();
            for (int x = centreX - radius; x <= centreX + radius; x++) {
                for (int y = centreY - radius; y <= centreY + radius; y++) {
                    final float currentHeight = dimension.getHeightAt(x, y);
                    final float strength = dynamicLevel * getStrength(centreX, centreY, x, y);
                    final float newHeight = strength * targetHeight  + (1f - strength) * currentHeight;
                    dimension.setHeightAt(x, y, newHeight);
//                    if (y == centerY) {
//                        System.out.printf("[%5d] [%7.5f] [%5d]\n", currentHeight, strength, newHeight);
//                    }
                }
            }
        } finally {
            dimension.setEventsInhibited(false);
        }
    }
    
    private float targetHeight;
}