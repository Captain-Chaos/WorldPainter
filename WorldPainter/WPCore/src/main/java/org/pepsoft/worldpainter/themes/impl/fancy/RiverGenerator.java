/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.themes.impl.fancy;

import java.util.HashSet;
import java.util.Random;
import javax.vecmath.Point3i;
import org.pepsoft.util.undo.UndoManager;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.Tile;
import static org.pepsoft.worldpainter.Constants.*;
import org.pepsoft.worldpainter.gardenofeden.Garden;

/**
 *
 * @author pepijn
 */
public class RiverGenerator {
    public RiverGenerator(Dimension dimension) {
        this.dimension = dimension;
    }
    

    public void generateRivers() {
        dimension.setEventsInhibited(true);
        UndoManager undoManager = new UndoManager(2);
        dimension.register(undoManager);
        snapshot = dimension.getSnapshot();
        dimension.armSavePoint();
        garden = dimension.getGarden();
        try {
            dimension.getTiles().forEach(this::generateRivers);
            
            // Grow seeds until all activity has ceased
            while (! garden.tick());
            
            // Apply the river nodes to the landscape
            // A river source
            dimension.getGarden().getSeeds().stream().filter(seed -> (seed instanceof RiverNode) && (seed.getParent() == null)).forEach(seed -> {
                // A river source
                ((RiverNode) seed).apply(dimension, snapshot, new HashSet<>());
            });
        } finally {
            garden = null;
            snapshot = null;
            dimension.unregister();
            dimension.setEventsInhibited(false);
        }
    }
    
    public void generateRivers(Tile tile) {
        long seed = dimension.getSeed() + tile.getX() * 65537 + tile.getY();
        Random random = new Random(seed);
        for (int x = 0; x < TILE_SIZE; x++) {
            for (int y = 0; y < TILE_SIZE; y++) {
                if (random.nextInt(1000) == 0) {
                    generateRiver((tile.getX() << TILE_SIZE_BITS) | x, (tile.getY() << TILE_SIZE_BITS) | y);
                }
            }
        }
    }

    public void generateRiver(int x, int y) {
//        System.out.println("Start coordinates: " + x + ", " + y);
        int waterLevel = snapshot.getWaterLevelAt(x, y);
        float height = snapshot.getHeightAt(x, y);
        int intHeight = (int) (height + 0.5f);
        if (waterLevel > intHeight) {
            // Already flooded
        } else {
            garden.plantSeed(new RiverNode(garden, new Point3i(x, y, -1), 1));
        }
//        while (true) {
//            if (waterLevel > intHeight) {
//                // Already flooded
////                System.out.println("Already flooded");
//                return;
//            } else {
//                int lowestSurroundingDryHeight = getLowestSurroundingDryHeight(x, y);
//                if (lowestSurroundingDryHeight == 0) {
//                    // At bedrock; can't go any deeper
////                    System.out.println("Lowest surrounding dry block is at level 0");
//                    return;
//                } else if (lowestSurroundingDryHeight == Integer.MAX_VALUE) {
//                    // This means that all surrounding blocks are flooded.
//                    dimension.setWaterLevelAt(x, y, getLowestSurroundingWaterLevel(x, y));
//                    return;
//                } else {
//                    dimension.setHeightAt(x, y, lowestSurroundingDryHeight - 1);
//                    dimension.setWaterLevelAt(x, y, lowestSurroundingDryHeight);
//                }
//            }
//            int nextX = 0, nextY = 0;
//            float lowestSurroundingHeight = height;
//            for (int dx = -2; dx <= 2; dx++) {
//                for (int dy = -2; dy <= 2; dy++) {
//                    if ((dx != 0) || (dy != 0)) {
//                        float surroundingHeight = snapshot.getHeightAt(x + dx, y + dy);
//                        if (surroundingHeight < lowestSurroundingHeight) {
//                            lowestSurroundingHeight = surroundingHeight;
//                            nextX = x + dx;
//                            nextY = y + dy;
//                        }
//                    }
//                }
//            }
//            if (lowestSurroundingHeight == height) {
//                // No lower block found; end of river
//                // TODO: start a lake?
////                System.out.println("No lower surrounding block found");
//                return;
//            }
//            x = nextX;
//            y = nextY;
////            System.out.println("Next coordinates: " + x + ", " + y);
//            waterLevel = snapshot.getWaterLevelAt(x, y);
//            height = snapshot.getHeightAt(x, y);
//            intHeight = (int) (height + 0.5f);
//        }
    }
    
    public int getLowestSurroundingDryHeight(int x, int y) {
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
 
    public int getLowestSurroundingWaterLevel(int x, int y) {
        int lowestSurroundingWaterLevel = Integer.MAX_VALUE;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if ((dx != 0) || (dy != 0)) {
                    int waterLevel = snapshot.getWaterLevelAt(x + dx, y + dy);
                    if ((waterLevel > snapshot.getIntHeightAt(x + dx, y + dy)) && (waterLevel < lowestSurroundingWaterLevel)) {
                        if (waterLevel == 0) {
                            return 0;
                        } else {
                            lowestSurroundingWaterLevel = waterLevel;
                        }
                    }
                }
            }
        }
        return lowestSurroundingWaterLevel;
    }
    
    private final Dimension dimension;
    private Dimension snapshot;
    private Garden garden;
}