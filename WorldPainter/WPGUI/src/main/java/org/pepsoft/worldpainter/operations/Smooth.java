/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.worldpainter.operations;

import java.util.Arrays;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.MapDragControl;
import org.pepsoft.worldpainter.RadiusControl;
import org.pepsoft.worldpainter.WorldPainter;

/**
 *
 * @author pepijn
 */
public class Smooth extends RadiusOperation {
    public Smooth(WorldPainter view, RadiusControl radiusControl, MapDragControl mapDragControl) {
        super("Smooth", "Smooth the terrain out", view, radiusControl, mapDragControl, 100, "operation.smooth");
    }

    @Override
    protected void tick(int centreX, int centreY, boolean inverse, boolean first, float dynamicLevel) {
        int radius = getEffectiveRadius(), diameter = radius * 2 + 1;
        if ((totals == null) || (totals.length < (diameter + 10))) {
            totals = new float[diameter + 10][diameter + 10];
            currentHeights = new float[diameter + 10][diameter + 10];
            sampleCounts = new int[diameter + 10][diameter + 10];
        } else {
            for (int i = 0; i < diameter + 10; i++) {
                Arrays.fill(totals[i], 0.0f);
                Arrays.fill(currentHeights[i], 0.0f);
                Arrays.fill(sampleCounts[i], 0);
            }
        }
        Dimension dimension = getDimension();
        dimension.setEventsInhibited(true);
        try {
            for (int x = 0; x < diameter + 10; x++) {
                for (int y = 0; y < diameter + 10; y++) {
                    float currentHeight = dimension.getHeightAt(centreX - radius + x - 5, centreY - radius + y - 5);
                    if (currentHeight != Float.MIN_VALUE) {
                        currentHeights[x][y] = currentHeight;
                        int dxFrom = Math.max(x - 5, 0);
                        int dxTo = Math.min(x + 5, diameter + 9);
                        int dyFrom = Math.max(y - 5, 0);
                        int dyTo = Math.min(y + 5, diameter + 9);
                        for (int dx = dxFrom; dx <= dxTo; dx++) {
                            for (int dy = dyFrom; dy <= dyTo; dy++) {
                                totals[dx][dy] += currentHeight;
                                sampleCounts[dx][dy]++;
                            }
                        }
                    }
                }
                if (x >= 10) {
                    for (int y = 5; y < diameter + 5; y++) {
                        float averageHeight = totals[x - 5][y] / sampleCounts[x - 5][y];
                        float strength = dynamicLevel * getStrength(centreX, centreY, centreX + x - radius - 10, centreY + y - radius - 5);
                        float newHeight = strength * averageHeight + (1 - strength) * currentHeights[x - 5][y];
                        dimension.setHeightAt(x + centreX - radius - 10, y + centreY - radius - 5, newHeight);
                    }
                }
            }
        } finally {
            dimension.setEventsInhibited(false);
        }
    }
    
    private float[][] totals, currentHeights;
    private int[][] sampleCounts;
}