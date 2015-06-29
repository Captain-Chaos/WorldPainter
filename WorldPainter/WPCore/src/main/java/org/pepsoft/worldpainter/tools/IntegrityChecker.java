/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.tools;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.zip.GZIPInputStream;
import org.pepsoft.worldpainter.Constants;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.Tile;
import org.pepsoft.worldpainter.World2;

/**
 *
 * @author pepijn
 */
public class IntegrityChecker {
    public static void main(String[] args) throws IOException, ClassNotFoundException {
        System.out.println("Loading " + args[0]);
        World2 world;
        try (ObjectInputStream in = new ObjectInputStream(new GZIPInputStream(new FileInputStream(args[0])))) {
            world = (World2) in.readObject();
        }
        for (Dimension dimension: world.getDimensions()) {
            float maxHeight = (dimension.getMaxHeight() - 1) + 0.5f;
            System.out.println("Checking integrity of " + dimension.getName() + " dimension");
            for (Tile tile: dimension.getTiles()) {
                boolean tileReported = false;
                for (int x = 0; x < Constants.TILE_SIZE; x++) {
                    for (int y = 0; y < Constants.TILE_SIZE; y++) {
                        float height = tile.getHeight(x, y);
                        if (height < -0.5f) {
                            if (! tileReported) {
                                System.out.println("Tile " + tile.getX() + "," + tile.getY());
                                tileReported = true;
                            }
                            System.out.println("Height " + height + " < -0.5 @ " + x + "," + y);
                        } else if (height > maxHeight) {
                            if (! tileReported) {
                                System.out.println("Tile " + tile.getX() + "," + tile.getY());
                                tileReported = true;
                            }
                            System.out.println("Height " + height + " > " + maxHeight + " @ " + x + "," + y);
                        }
                    }
                }
            }
        }
    }
}