/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.worldpainter.operations;

import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.WorldPainter;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;

/**
 *
 * @author pepijn
 */
public class Smooth extends RadiusOperation {
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

        final Rectangle boundingBox = getBoundingBox();
        if ((totals == null) || (totals.length < (boundingBox.width + 10))) {
            totals = new float[boundingBox.width + 10][boundingBox.height + 10];
            currentHeights = new float[boundingBox.width + 10][boundingBox.height + 10];
            sampleCounts = new int[boundingBox.width + 10][boundingBox.height + 10];
        } else {
            for (int i = 0; i < boundingBox.width + 10; i++) {
                Arrays.fill(totals[i], 0.0f);
                Arrays.fill(currentHeights[i], 0.0f);
                Arrays.fill(sampleCounts[i], 0);
            }
        }
        final boolean applyTheme = options.isApplyTheme();
        dimension.setEventsInhibited(true);
        try {
            for (int x = 0; x < boundingBox.width + 10; x++) {
                for (int y = 0; y < boundingBox.height + 10; y++) {
                    final float currentHeight = dimension.getHeightAt(centreX + boundingBox.x + x - 5, centreY + boundingBox.y + y - 5);
                    if (currentHeight != -Float.MAX_VALUE) {
                        currentHeights[x][y] = currentHeight;
                        final int dxFrom = Math.max(x - 5, 0);
                        final int dxTo = Math.min(x + 5, boundingBox.width + 9);
                        final int dyFrom = Math.max(y - 5, 0);
                        final int dyTo = Math.min(y + 5, boundingBox.height + 9);
                        for (int dx = dxFrom; dx <= dxTo; dx++) {
                            for (int dy = dyFrom; dy <= dyTo; dy++) {
                                totals[dx][dy] += currentHeight;
                                sampleCounts[dx][dy]++;
                            }
                        }
                    }
                }
                if (x >= 10) {
                    for (int y = 5; y < boundingBox.height + 5; y++) {
                        final float strength = dynamicLevel * getStrength(centreX, centreY, centreX + x + boundingBox.x - 10, centreY + y + boundingBox.y - 5);
                        if (strength > 0.0f) {
                            final float newHeight = strength * (totals[x - 5][y] / sampleCounts[x - 5][y]) + (1 - strength) * currentHeights[x - 5][y];
                            dimension.setHeightAt(x + centreX + boundingBox.x - 10, y + centreY + boundingBox.y - 5, newHeight);
                            if (applyTheme) {
                                dimension.applyTheme(x + centreX + boundingBox.x - 10, y + centreY + boundingBox.y - 5);
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