/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.worldpainter.operations;

import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.Terrain;
import org.pepsoft.worldpainter.WorldPainter;

import javax.swing.*;

/**
 *
 * @author pepijn
 */
public class RaiseRotatedPyramid extends MouseOrTabletOperation {
    public RaiseRotatedPyramid(WorldPainter worldPainter) {
        super("Raise Rotated Pyramid", "Raises a square, but rotated 45 degrees, pyramid out of the ground", worldPainter, 100, "operation.raiseRotatedPyramid", "pyramid");
    }

    @Override
    public JPanel getOptionsPanel() {
        return OPTIONS_PANEL;
    }

    protected void tick(int centreX, int centreY, boolean inverse, boolean first, float dynamicLevel) {
        Dimension dimension = getDimension();
        float height = dimension.getHeightAt(centreX, centreY);
        dimension.setEventsInhibited(true);
        try {
            if (height < (dimension.getMaxHeight() - 1.5f)) {
                dimension.setHeightAt(centreX, centreY, height + 1);
            }
            dimension.setTerrainAt(centreX, centreY, Terrain.SANDSTONE);
            int maxR = dimension.getMaxHeight();
            for (int r = 1; r < maxR; r++) {
                if (! raiseRing(dimension, centreX, centreY, r, height--)) {
                    break;
                }
            }
        } finally {
            dimension.setEventsInhibited(false);
        }
    }

    private boolean raiseRing(Dimension dimension, int x, int y, int r, float desiredHeight) {
        boolean raised = false;
        for (int i = 0; i < r; i++) {
            float actualHeight = dimension.getHeightAt(x - r + i, y - i);
            if (actualHeight < desiredHeight) {
                raised = true;
                dimension.setHeightAt(x - r + i, y - i, desiredHeight);
                dimension.setTerrainAt(x - r + i, y - i, Terrain.SANDSTONE);
            }
            actualHeight = dimension.getHeightAt(x + i, y - r + i);
            if (actualHeight < desiredHeight) {
                raised = true;
                dimension.setHeightAt(x + i, y - r + i, desiredHeight);
                dimension.setTerrainAt(x + i, y - r + i, Terrain.SANDSTONE);
            }
            actualHeight = dimension.getHeightAt(x + r - i, y + i);
            if (actualHeight < desiredHeight) {
                raised = true;
                dimension.setHeightAt(x + r - i, y + i, desiredHeight);
                dimension.setTerrainAt(x + r - i, y + i, Terrain.SANDSTONE);
            }
            actualHeight = dimension.getHeightAt(x - i, y + r - i);
            if (actualHeight < desiredHeight) {
                raised = true;
                dimension.setHeightAt(x - i, y + r - i, desiredHeight);
                dimension.setTerrainAt(x - i, y + r - i, Terrain.SANDSTONE);
            }
        }
        return raised;
    }

    private static final StandardOptionsPanel OPTIONS_PANEL = new StandardOptionsPanel("Raise Rotated Pyramid", "<p>Click to raise a 45&deg; rotated four-sided sandstone pyramid from the ground");
}