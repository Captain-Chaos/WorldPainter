/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.worldpainter.operations;

import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.WorldPainter;
import org.pepsoft.worldpainter.layers.FloodWithLava;
import org.pepsoft.worldpainter.painting.GeneralQueueLinearFloodFiller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;

import static org.pepsoft.util.swing.MessageUtils.showWarning;
import static org.pepsoft.worldpainter.Constants.TILE_SIZE;

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
        optionsPanel = floodWithLava
                ? new StandardOptionsPanel("Flood with Lava", "<ul><li>Left-click on dry land to flood with lava\n" +
                "<li>Left-click on lava to raise it by one\n" +
                "<li>Right-click on lava to lower it by one\n" +
                "<li>Click on water to turn it to lava\n" +
                "</ul>")
                : new StandardOptionsPanel("Flood with Water", "<ul><li>Left-click on dry land to flood with water\n" +
                "<li>Left-click on water to raise it by one\n" +
                "<li>Right-click on water to lower it by one\n" +
                "<li>Click on lava to turn it to water\n" +
                "</ul>");
    }
    
    @Override
    protected void tick(int centreX, int centreY, boolean inverse, boolean first, float dynamicLevel) {
        final Dimension dimension = getDimension();
        if (dimension == null) {
            // Probably some kind of race condition
            return;
        }

        // We have seen in the wild that this sometimes gets called recursively (perhaps someone clicks to flood more
        // than once and then it takes more than two seconds so it is continued in the background and event queue
        // processing is resumed?), which causes errors, so just ignore it if we are already flooding.
        if (alreadyFlooding) {
            logger.debug("Flood operation already in progress; ignoring repeated invocation");
            return;
        }
        alreadyFlooding = true;
        try {
            final Rectangle dimensionBounds = new Rectangle(dimension.getLowestX() * TILE_SIZE, dimension.getLowestY() * TILE_SIZE, dimension.getWidth() * TILE_SIZE, dimension.getHeight() * TILE_SIZE);
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
            final GeneralQueueLinearFloodFiller.FillMethod fillMethod;
            if (fluidPresent && (floodWithLava != dimension.getBitLayerValueAt(FloodWithLava.INSTANCE, centreX, centreY))) {
                // There is fluid present of a different type; don't change the
                // height, just change the type
                if (floodWithLava) {
                    fillMethod = new FloodFillMethod("Changing water to lava", dimensionBounds) {
                        @Override public boolean isBoundary(int x, int y) {
                            final int height = dimension.getIntHeightAt(x, y);
                            return (height == Integer.MIN_VALUE) // Not on a tile
                                    || (dimension.getWaterLevelAt(x, y) <= height) // Not flooded
                                    || dimension.getBitLayerValueAt(FloodWithLava.INSTANCE, x, y); // Not water
                        }

                        @Override public void fill(int x, int y) {
                            dimension.setBitLayerValueAt(FloodWithLava.INSTANCE, x, y, true);
                        }
                    };
                } else {
                    fillMethod = new FloodFillMethod("Changing lava to water", dimensionBounds) {
                        @Override public boolean isBoundary(int x, int y) {
                            final int height = dimension.getIntHeightAt(x, y);
                            return (height == Integer.MIN_VALUE) // Not on a tile
                                    || (dimension.getWaterLevelAt(x, y) <= height) // Not flooded
                                    || (! dimension.getBitLayerValueAt(FloodWithLava.INSTANCE, x, y)); // Not lava
                        }

                        @Override public void fill(int x, int y) {
                            dimension.setBitLayerValueAt(FloodWithLava.INSTANCE, x, y, false);
                        }
                    };
                }
            } else {
                final int height = Math.max(terrainHeight, waterLevel);
                if (inverse ? (height <= dimension.getMinHeight()) : (height >= (dimension.getMaxHeight() - 1))) {
                    // Already at the lowest or highest possible point
                    return;
                }
                final int floodToHeight = inverse ? (height - 1): (height + 1);
                if (inverse) {
                    if (floodWithLava) {
                        fillMethod = new FloodFillMethod("Lowering lava level", dimensionBounds) {
                            @Override public boolean isBoundary(int x, int y) {
                                final int height = dimension.getIntHeightAt(x, y);
                                return (height == Integer.MIN_VALUE) // Not on a tile
                                        || (dimension.getWaterLevelAt(x, y) <= height) // Not flooded
                                        || (dimension.getWaterLevelAt(x, y) <= floodToHeight); // Already at the required level or lower
                            }

                            @Override public void fill(int x, int y) {
                                dimension.setWaterLevelAt(x, y, floodToHeight);
                                dimension.setBitLayerValueAt(FloodWithLava.INSTANCE, x, y, true);
                            }
                        };
                    } else {
                        fillMethod = new FloodFillMethod("Lowering water level", dimensionBounds) {
                            @Override public boolean isBoundary(int x, int y) {
                                final int height = dimension.getIntHeightAt(x, y);
                                return (height == Integer.MIN_VALUE) // Not on a tile
                                        || (dimension.getWaterLevelAt(x, y) <= height) // Not flooded
                                        || (dimension.getWaterLevelAt(x, y) <= floodToHeight); // Already at the required level or lower
                            }

                            @Override public void fill(int x, int y) {
                                dimension.setWaterLevelAt(x, y, floodToHeight);
                                dimension.setBitLayerValueAt(FloodWithLava.INSTANCE, x, y, false);
                            }
                        };
                    }
                } else {
                    if (floodWithLava) {
                        fillMethod = new FloodFillMethod(fluidPresent ? "Raising lava level" : "Flooding with lava", dimensionBounds) {
                            @Override public boolean isBoundary(int x, int y) {
                                final int height = dimension.getIntHeightAt(x, y), waterLevel = dimension.getWaterLevelAt(x, y);
                                return (height == Integer.MIN_VALUE) // Not on a tile
                                        || (height >= floodToHeight) // Higher land encountered
                                        || (waterLevel >= floodToHeight); // Already at the required level or lower
                            }

                            @Override public void fill(int x, int y) {
                                dimension.setWaterLevelAt(x, y, floodToHeight);
                                dimension.setBitLayerValueAt(FloodWithLava.INSTANCE, x, y, true);
                            }
                        };
                    } else {
                        fillMethod = new FloodFillMethod(fluidPresent ? "Raising water level" : "Flooding with water", dimensionBounds) {
                            @Override public boolean isBoundary(int x, int y) {
                                final int height = dimension.getIntHeightAt(x, y), waterLevel = dimension.getWaterLevelAt(x, y);
                                return (height == Integer.MIN_VALUE) // Not on a tile
                                        || (height >= floodToHeight) // Higher land encountered
                                        || (waterLevel >= floodToHeight); // Already at the required level or higher
                            }

                            @Override public void fill(int x, int y) {
                                dimension.setWaterLevelAt(x, y, floodToHeight);
                                dimension.setBitLayerValueAt(FloodWithLava.INSTANCE, x, y, false);
                            }
                        };
                    }
                }
            }
            synchronized (dimension) {
                dimension.setEventsInhibited(true);
            }
            try {
                synchronized (dimension) {
                    dimension.rememberChanges();
                }
                final GeneralQueueLinearFloodFiller flooder = new GeneralQueueLinearFloodFiller(fillMethod);
                try {
                    if (! flooder.floodFill(centreX, centreY, SwingUtilities.getWindowAncestor(getView()))) {
                        // Cancelled by user
                        synchronized (dimension) {
                            if (dimension.undoChanges()) {
                                dimension.clearRedo();
                                dimension.armSavePoint();
                            }
                        }
                        return;
                    }
                    if (flooder.isBoundsHit()) {
                        showWarning(getView(), "The area to be flooded was too large and may not have been completely flooded.", "Area Too Large");
                    }
                } catch (IndexOutOfBoundsException e) {
                    // This most likely indicates that the area being flooded was too large
                    synchronized (dimension) {
                        if (dimension.undoChanges()) {
                            dimension.clearRedo();
                            dimension.armSavePoint();
                        }
                    }
                    JOptionPane.showMessageDialog(getView(), "The area to be flooded is too large or complex; please retry with a smaller area", "Area Too Large", JOptionPane.ERROR_MESSAGE);
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

    @Override
    public JPanel getOptionsPanel() {
        return optionsPanel;
    }

    private boolean alreadyFlooding;

    private final boolean floodWithLava;
    private final StandardOptionsPanel optionsPanel;

    private static final Logger logger = LoggerFactory.getLogger(Flood.class);

    static abstract class FloodFillMethod implements GeneralQueueLinearFloodFiller.FillMethod {
        protected FloodFillMethod(String description, Rectangle bounds) {
            this.description = description;
            this.bounds = bounds;
        }

        @Override
        public String getDescription() {
            return description;
        }

        @Override
        public Rectangle getBounds() {
            return bounds;
        }

        private final String description;
        private final Rectangle bounds;
    }
}