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
import static org.pepsoft.worldpainter.Constants.*;
import org.pepsoft.worldpainter.Tile;
import org.pepsoft.worldpainter.World2;
import org.pepsoft.worldpainter.layers.FloodWithLava;

/**
 *
 * @author pepijn
 */
public class ResetLava {
    public static void main(String[] args) throws IOException, ClassNotFoundException {
        System.out.println("Loading world " + args[0]);
        File worldFile = new File(args[0]);
        int waterLevel = Integer.parseInt(args[1]);
        World2 world;
        try (ObjectInputStream in = new ObjectInputStream(new GZIPInputStream(new FileInputStream(worldFile)))) {
            world = (World2) in.readObject();
        }
        System.out.println("World loaded");
        Dimension dim0 = world.getDimension(0);
        for (Tile tile: dim0.getTiles()) {
            for (int x = 0; x < TILE_SIZE; x++) {
                for (int y = 0; y < TILE_SIZE; y++) {
//                    if ((tile.getWaterLevel(x, y) > (tile.getIntHeight(x, y))) && tile.getBitLayerValue(FloodWithLava.INSTANCE, x, y)) {
                        tile.setBitLayerValue(FloodWithLava.INSTANCE, x, y, false);
                        tile.setWaterLevel(x, y, waterLevel);
//                    }
                }
            }
            System.out.print('.');
        }
        System.out.println();
        System.out.println("Saving world " + args[0]);
        try (ObjectOutputStream out = new ObjectOutputStream(new GZIPOutputStream(new FileOutputStream(worldFile)))) {
            out.writeObject(world);
        }
        System.out.println("World saved");
    }
}