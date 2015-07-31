/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.themes.impl.fancy;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.vecmath.Point3i;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.gardenofeden.Garden;
import org.pepsoft.worldpainter.gardenofeden.PathNode;
import org.pepsoft.worldpainter.layers.GardenCategory;
import org.pepsoft.worldpainter.util.GeometryUtil;

/**
 *
 * @author pepijn
 */
public class RiverNode extends PathNode {
    public RiverNode(Garden garden, Point3i location, int crossSectionalArea) {
        super(garden, 0, null, location, 1, GardenCategory.CATEGORY_WATER);
        this.crossSectionalArea = crossSectionalArea;
//        System.out.println("River started @ " + location);
    }

    public RiverNode(Garden garden, RiverNode parent, Point3i location) {
        super(garden, 0, parent, location, 1, GardenCategory.CATEGORY_WATER);
        parent.child = this;
        crossSectionalArea = parent.crossSectionalArea;
//        System.out.println("Extending river to " + location);
    }
    
    public RiverNode(Garden garden, RiverNode parent, Point3i location, RiverNode child) {
        super(garden, 0, parent, location, 1, GardenCategory.CATEGORY_WATER);
        parent.child = this;
        this.child = child;
        crossSectionalArea = parent.crossSectionalArea;
//        System.out.println("Connecting tributary to another river @ " + location);
    }
    
    @Override
    protected boolean sprout() {
        boolean endRiver = child != null;
        if (garden.isWater(location.x, location.y)) {
            // There is already water here; end the river here
            // TODO: grow lake?
            endRiver = true;
        }

        if (parent != null) {
            drawLine(parent.location, location, (int) Math.round(Math.sqrt(crossSectionalArea)), false, category);
        }
        
        if (endRiver) {
            return (parent != null);
        }
        
        // Find coordinates of lowest point on a surrounding circle
        // Midpoint circle ("Bresenham's") algorithm
        final boolean[] lowerPointFound = new boolean[1];
        final int[] lowestHeightCoords = new int[2];
        int radius = 0;
        for (int r = 3; (! lowerPointFound[0]) && (r <= 9); r += 2) {
            radius = r;
            final float[] lowestHeight = {garden.getHeight(location.x, location.y)};
            GeometryUtil.visitCircle(r, (dx, dy, d) -> {
                float height = garden.getHeight(location.x + dx, location.y + dy);
                if (height < lowestHeight[0]) {
                    lowestHeight[0] = height;
                    lowestHeightCoords[0] = location.x + dx;
                    lowestHeightCoords[1] = location.y + dy;
                    lowerPointFound[0] = true;
                }
                return true;
            });
        }
        
        if (lowerPointFound[0]) {
            // Find existing nodes to connect with (not belonging to the same
            // river)
            List<RiverNode> nearbyNodes = garden.findSeeds(RiverNode.class, lowestHeightCoords[0], lowestHeightCoords[1], radius);
            int ownNodesEncountered = 0;
            for (RiverNode riverNode: nearbyNodes) {
                if (riverNode.getOrigin() == getOrigin()) {
                    // Another node of same river
                    ownNodesEncountered++;
                    if (ownNodesEncountered < 2) {
                        continue;
                    } else {
                        // We seem to be circing the drain. Abort! Abort!
                        return true;
                    }
                }
                if (riverNode.child != null) {
                    garden.plantSeed(new RiverNode(garden, this, riverNode.location, riverNode.child));
                    addCrossSectionalArea(riverNode.child, crossSectionalArea);
                } else {
                    garden.plantSeed(new RiverNode(garden, this, riverNode.location));
                }
                return true;
            }

            garden.plantSeed(new RiverNode(garden, this, new Point3i(lowestHeightCoords[0], lowestHeightCoords[1], -1)));
            return true;
        } else {
            // No lower point around; end the river here
            // TODO: start a lake?
            return (parent != null);
        }
    }
    
    public RiverNode getOrigin() {
        if (parent != null) {
            return ((RiverNode) parent).getOrigin();
        } else {
            return this;
        }
    }

    public void apply(final Dimension dimension, final Dimension dimensionSnapshot, Set<RiverNode> processedNodes) {
        if (processedNodes.contains(this)) {
            logger.error("Loop in river!");
            return;
        } else {
            processedNodes.add(this);
        }
        if (parent != null) {
            final int d = (int) Math.round(Math.sqrt(crossSectionalArea));
            final int r = d / 2;
            final boolean eastWest = Math.abs(location.x - parent.location.x) > Math.abs(location.y - parent.location.y);
            doAlongLine(parent.location.x, parent.location.y, location.x, location.y, (x, y) -> {
                if (d == 1) {
                    if (eastWest) {
                        int lowestSurroundingDryHeight = getLowestSurroundingDryHeight(dimensionSnapshot, x, y);
                        dimension.setHeightAt(x, y, lowestSurroundingDryHeight - 1);
                        dimension.setWaterLevelAt(x, y, Math.max(dimension.getWaterLevelAt(x, y), lowestSurroundingDryHeight));
                        lowestSurroundingDryHeight = getLowestSurroundingDryHeight(dimensionSnapshot, x + 1, y);
                        dimension.setHeightAt(x + 1, y, lowestSurroundingDryHeight - 1);
                        dimension.setWaterLevelAt(x + 1, y, Math.max(dimension.getWaterLevelAt(x + 1, y), lowestSurroundingDryHeight));
                    } else {
                        int lowestSurroundingDryHeight = getLowestSurroundingDryHeight(dimensionSnapshot, x, y);
                        dimension.setHeightAt(x, y, lowestSurroundingDryHeight - 1);
                        dimension.setWaterLevelAt(x, y, Math.max(dimension.getWaterLevelAt(x, y), lowestSurroundingDryHeight));
                        lowestSurroundingDryHeight = getLowestSurroundingDryHeight(dimensionSnapshot, x, y + 1);
                        dimension.setHeightAt(x, y + 1, lowestSurroundingDryHeight - 1);
                        dimension.setWaterLevelAt(x, y + 1, Math.max(dimension.getWaterLevelAt(x, y + 1), lowestSurroundingDryHeight));
                    }
                } else if (d == 2) {
                    for (int dx = 0; dx < 2; dx++) {
                        for (int dy = 0; dy < 2; dy++) {
                            int lowestSurroundingDryHeight = getLowestSurroundingDryHeight(dimensionSnapshot, x + dx, y + dy);
                            dimension.setHeightAt(x + dx, y + dy, lowestSurroundingDryHeight - 1);
                            dimension.setWaterLevelAt(x + dx, y + dy, Math.max(dimension.getWaterLevelAt(x + dx, y + dy), lowestSurroundingDryHeight));
                        }
                    }
                } else {
                    GeometryUtil.visitFilledCircle(r - 1, (dx, dy, d1) -> {
                        int lowestSurroundingDryHeight = getLowestSurroundingDryHeight(dimensionSnapshot, x + dx, y + dy);
                        dimension.setHeightAt(x + dx, y + dy, lowestSurroundingDryHeight - 1);
                        dimension.setWaterLevelAt(x + dx, y + dy, Math.max(dimension.getWaterLevelAt(x + dx, y + dy), lowestSurroundingDryHeight));
                        return true;
                    });
                }
                return true;
            });
        }
        if (child != null) {
            child.apply(dimension, dimensionSnapshot, processedNodes);
        }
        garden.removeSeed(this);
    }
    
    public int getSlope(int x, int y) {
        int slope1 = Math.abs(garden.getIntHeight(x - 1, y) - garden.getIntHeight(x + 1, y));
        int slope2 = Math.abs(garden.getIntHeight(x - 1, y - 1) - garden.getIntHeight(x + 1, y + 1));
        int slope3 = Math.abs(garden.getIntHeight(x, y - 1) - garden.getIntHeight(x, y + 1));
        int slope4 = Math.abs(garden.getIntHeight(x + 1, y - 1) - garden.getIntHeight(x - 1, y + 1));
        return Math.max(Math.max(slope1, slope2), Math.max(slope3, slope4));
    }

    private void addCrossSectionalArea(RiverNode node, int crossSectionalArea) {
        Set<RiverNode> processedNodes = new HashSet<>();
        while (node != null) {
            if (processedNodes.contains(node)) {
                logger.error("Loop in river!");
                return;
            } else {
                processedNodes.add(node);
            }
            node.crossSectionalArea += crossSectionalArea;
            drawLine(node.parent.location, node.location, (int) Math.round(Math.sqrt(node.crossSectionalArea)), false, category);
            node = node.child;
        }
    }

    private int getLowestSurroundingDryHeight(Dimension snapshot, int x, int y) {
        int lowestSurroundingDryHeight = Integer.MAX_VALUE;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if ((dx != 0) || (dy != 0)) {
                    int height = snapshot.getIntHeightAt(x + dx, y + dy);
                    if ((height >= snapshot.getWaterLevelAt(x + dx, y + dy)) && (height < lowestSurroundingDryHeight)) {
                        if (height == 0) {
                            return 0;
                        } else {
                            lowestSurroundingDryHeight = height;
                        }
                    }
                }
            }
        }
        return lowestSurroundingDryHeight;
    }
    
    /**
     * The surface area of the cross section of the river's profile
     */
    private int crossSectionalArea;
    
    private RiverNode child;
    
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(RiverNode.class);
    private static final long serialVersionUID = 1L;
}