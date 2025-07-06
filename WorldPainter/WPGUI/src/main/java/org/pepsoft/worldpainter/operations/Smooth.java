/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.worldpainter.operations;

import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.WorldPainter;

import javax.swing.*;
import java.util.Arrays;

/**
 *
 * @author pepijn
 */
public class Smooth extends AbstractBrushOperation {
    public Smooth(WorldPainter view) {
        super("Smooth", "Smooth the terrain out", view, 100, "operation.smooth");
    }

    @Override
    public JPanel getOptionsPanel() {
        return optionsPanel;
    }

    @Override
    protected void tick(int centreX, int centreY, boolean inverse, boolean first, float dynamicLevel) {
        final Dimension dimension = getDimension();
        if (dimension == null) {
            // Probably some kind of race condition
            return;
        }

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
        boolean applyTheme = options.isApplyTheme();
        dimension.setEventsInhibited(true);
        try {
            for (int x = 0; x < diameter + 10; x++) {
                for (int y = 0; y < diameter + 10; y++) {
                    float currentHeight = dimension.getHeightAt(centreX - radius + x - 5, centreY - radius + y - 5);
                    if (currentHeight != -Float.MAX_VALUE) {
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
                        float strength = dynamicLevel * getStrength(centreX, centreY, centreX + x - radius - 10, centreY + y - radius - 5);
                        if (strength > 0.0f) {
                            float newHeight = strength * (totals[x - 5][y] / sampleCounts[x - 5][y]) + (1 - strength) * currentHeights[x - 5][y];
                            dimension.setHeightAt(x + centreX - radius - 10, y + centreY - radius - 5, newHeight);
                            if (applyTheme) {
                                dimension.applyTheme(x + centreX - radius - 10, y + centreY - radius - 5);
                            }
                        }
                    }
                }
            }
        } finally {
            dimension.setEventsInhibited(false);
        }
    }
    
    private final TerrainShapingOptions<Smooth> options = new TerrainShapingOptions<>();
    private final TerrainShapingOptionsPanel optionsPanel = new TerrainShapingOptionsPanel("Smooth", "<p>Click to smooth the terrain out", options);
    private float[][] totals, currentHeights;
    private int[][] sampleCounts;
}