/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.tools;

import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.World2;

import java.awt.*;
import java.io.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

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
            for (Point tileCoords: dimension.getTileCoords()) {
                int dx = Math.abs(tileCoords.x), dy = Math.abs(tileCoords.y);
                if ((dx > maxTileDistance) || (dy > maxTileDistance)) {
                    // It's an outlier. Remove it
                    System.out.println("Removing tile at " + tileCoords.getX() + ", " + tileCoords.getY());
                    dimension.removeTile(tileCoords);
                }
            }
        }
        try (ObjectOutputStream out = new ObjectOutputStream(new GZIPOutputStream(new FileOutputStream(worldFile)))) {
            out.writeObject(world);
        }
    }
}