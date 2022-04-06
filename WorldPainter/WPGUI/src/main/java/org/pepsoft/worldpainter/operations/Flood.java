/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.worldpainter.operations;

import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.QueueLinearFloodFiller;
import org.pepsoft.worldpainter.WorldPainter;
import org.pepsoft.worldpainter.layers.FloodWithLava;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;

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
        // We have seen in the wild that this sometimes gets called recursively (perhaps someone clicks to flood more
        // than once and then it takes more than two seconds so it is continued in the background and event queue
        // processing is resumed?), which causes errors, so just ignore it if we are already flooding.
        if (alreadyFlooding) {
            logger.debug("Flood operation already in progress; ignoring repeated invocation");
            return;
        }
        alreadyFlooding = true;
        try {
            Dimension dimension = getDimension();
            final int terrainHeight = dimension.getIntHeightAt(centreX, centreY);
            if (terrainHeight == Integer.MIN_VALUE) {
                // Not on a tile
                return;
            }
            final int waterLevel = dimension.getWaterLevelAt(centreX, centreY);
            final boolean fluidPresent = waterLevel > terrainHeight;
            if (inverse && (! fluidPresent)) {
                // No point lowering the water level if there is no water...
                return;
            }
            final int height = Math.max(terrainHeight, waterLevel);
            final int floodToHeight;
            if (fluidPresent && (floodWithLava != dimension.getBitLayerValueAt(FloodWithLava.INSTANCE, centreX, centreY))) {
                // There is fluid present of a different type; don't change the
                // height, just change the type
                floodToHeight = height;
                inverse = false;
            } else {
                if (inverse ? (height <= dimension.getMinHeight()) : (height >= (dimension.getMaxHeight() - 1))) {
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
                final QueueLinearFloodFiller flooder = new QueueLinearFloodFiller(dimension, floodToHeight, floodWithLava, inverse);
                if (!flooder.floodFill(centreX, centreY, SwingUtilities.getWindowAncestor(getView()))) {
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
        } finally {
            alreadyFlooding = false;
        }
    }

    private boolean alreadyFlooding;

    private final boolean floodWithLava;

    private static final Logger logger = LoggerFactory.getLogger(Flood.class);
}
