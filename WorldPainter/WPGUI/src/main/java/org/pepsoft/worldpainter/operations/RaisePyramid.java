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
public class RaisePyramid extends MouseOrTabletOperation {
    public RaisePyramid(WorldPainter worldPainter) {
        super("Raise Pyramid", "Raises a square pyramid out of the ground", worldPainter, 100, "operation.raisePyramid", "pyramid");
    }

    @Override
    public JPanel getOptionsPanel() {
        return OPTIONS_PANEL;
    }

    @Override
    protected void tick(int centreX, int centreY, boolean inverse, boolean first, float dynamicLevel) {
        final Dimension dimension = getDimension();
        if (dimension == null) {
            // Probably some kind of race condition
            return;
        }
        float height = dimension.getHeightAt(centreX, centreY);
        dimension.setEventsInhibited(true);
        try {
            if (height < (dimension.getMaxHeight() - 1.5f)) {
                dimension.setHeightAt(centreX, centreY, height + 1);
            }
            dimension.setTerrainAt(centreX, centreY, Terrain.SANDSTONE);
            int maxR = dimension.getMaxHeight() - dimension.getMinHeight();
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
        for (int i = -r; i <= r; i++) {
            float actualHeight = dimension.getHeightAt(x + i, y - r);
            if (actualHeight < desiredHeight) {
                raised = true;
                dimension.setHeightAt(x + i, y - r, desiredHeight);
                dimension.setTerrainAt(x + i, y - r, Terrain.SANDSTONE);
            }
            actualHeight = dimension.getHeightAt(x + i, y + r);
            if (actualHeight < desiredHeight) {
                raised = true;
                dimension.setHeightAt(x + i, y + r, desiredHeight);
                dimension.setTerrainAt(x + i, y + r, Terrain.SANDSTONE);
            }
        }
        for (int i = -r + 1; i < r; i++) {
            float actualHeight = dimension.getHeightAt(x - r, y + i);
            if (actualHeight < desiredHeight) {
                raised = true;
                dimension.setHeightAt(x - r, y + i, desiredHeight);
                dimension.setTerrainAt(x - r, y + i, Terrain.SANDSTONE);
            }
            actualHeight = dimension.getHeightAt(x + r, y + i);
            if (actualHeight < desiredHeight) {
                raised = true;
                dimension.setHeightAt(x + r, y + i, desiredHeight);
                dimension.setTerrainAt(x + r, y + i, Terrain.SANDSTONE);
            }
        }
        return raised;
    }

    private static final StandardOptionsPanel OPTIONS_PANEL = new StandardOptionsPanel("Raise Pyramid", "<p>Click to raise a four-sided sandstone pyramid from the ground");
}