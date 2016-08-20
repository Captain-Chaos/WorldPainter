/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.worldpainter.operations;

import javax.swing.SwingUtilities;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.QueueLinearFloodFiller;
import org.pepsoft.worldpainter.WorldPainter;
import org.pepsoft.worldpainter.layers.FloodWithLava;

/**
 *
 * @author pepijn
 */
public class Flood extends MouseOrTabletOperation {
    public Flood(WorldPainter view, boolean floodWithLava) {
        super(floodWithLava ? "Lava" : "Flood", "Flood an area with " + (floodWithLava ? "lava" : "water"),
                view,
                "operation.flood." + (floodWithLava ? "lava" : "water"),
                floodWithLava ? "flood_with_lava" : "flood");
        this.floodWithLava = floodWithLava;
    }
    
    @Override
    protected void tick(int centreX, int centreY, boolean inverse, boolean first, float dynamicLevel) {
        Dimension dimension = getDimension();
        int terrainHeight = dimension.getIntHeightAt(centreX, centreY);
        if (terrainHeight == -1) {
            // Not on a tile
            return;
        }
        int waterLevel = dimension.getWaterLevelAt(centreX, centreY);
        boolean fluidPresent = waterLevel > terrainHeight;
        if (inverse && (! fluidPresent)) {
            // No point lowering the water level if there is no water...
            return;
        }
        int height = Math.max(terrainHeight, waterLevel);
        int floodToHeight;
        if (fluidPresent && (floodWithLava != dimension.getBitLayerValueAt(FloodWithLava.INSTANCE, centreX, centreY))) {
            // There is fluid present of a different type; don't change the
            // height, just change the type
            floodToHeight = height;
            inverse = false;
        } else {
            if (inverse ? (height <= 0) : (height >= (dimension.getMaxHeight() - 1))) {
                // Already at the lowest or highest possible point
                return;
            }
            floodToHeight = inverse ? height : (height + 1);
        }
        synchronized (dimension) {
            dimension.setEventsInhibited(true);
        }
        try {
            synchronized (dimension) {
                dimension.rememberChanges();
            }
            QueueLinearFloodFiller flooder = new QueueLinearFloodFiller(dimension, floodToHeight, floodWithLava, inverse);
            if (! flooder.floodFill(centreX, centreY, SwingUtilities.getWindowAncestor(getView()))) {
                // Cancelled by user
                synchronized (dimension) {
                    if (dimension.undoChanges()) {
                        dimension.clearRedo();
                        dimension.armSavePoint();
                    }
                }
            }
        } finally {
            synchronized (dimension) {
                dimension.setEventsInhibited(false);
            }
        }
    }

    private final boolean floodWithLava;
}
