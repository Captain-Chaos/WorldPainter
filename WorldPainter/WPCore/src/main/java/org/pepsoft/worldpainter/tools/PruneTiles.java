/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.Tile;
import org.pepsoft.worldpainter.World2;

/**
 *
 * @author pepijn
 */
public class PruneTiles {
    public static void main(String[] args) throws IOException, ClassNotFoundException {
        File worldFile = new File(args[0]);
        int maxTileDistance = Integer.parseInt(args[1]);
        World2 world;
        try (ObjectInputStream in = new ObjectInputStream(new GZIPInputStream(new FileInputStream(worldFile)))) {
            world = (World2) in.readObject();
        }
        for (Dimension dimension: world.getDimensions()) {
            for (Tile tile: dimension.getTiles()) {
                int dx = Math.abs(tile.getX()), dy = Math.abs(tile.getY());
                if ((dx > maxTileDistance) || (dy > maxTileDistance)) {
                    // It's an outlier. Remove it
                    System.out.println("Removing tile at " + tile.getX() + ", " + tile.getY());
                    dimension.removeTile(tile);
                }
            }
        }
        try (ObjectOutputStream out = new ObjectOutputStream(new GZIPOutputStream(new FileOutputStream(worldFile)))) {
            out.writeObject(world);
        }
    }
}