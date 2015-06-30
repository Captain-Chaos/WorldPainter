/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.tools;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.HeightMap;
import org.pepsoft.worldpainter.HeightMapTileFactory;
import org.pepsoft.worldpainter.TileFactory;
import org.pepsoft.worldpainter.World2;
import org.pepsoft.worldpainter.heightMaps.ConstantHeightMap;
import org.pepsoft.worldpainter.heightMaps.SumHeightMap;

/**
 *
 * @author pepijn
 */
public class RepairHeightMap {
    public static final void main(String[] args) throws IOException, ClassNotFoundException{
        System.out.println("Scanning world " + args[0]);
        World2 world;
        try (ObjectInputStream in = new ObjectInputStream(new GZIPInputStream(new FileInputStream(args[0])))) {
            world = (World2) in.readObject();
        }
        
        boolean repairsPerformed = false;
        for (Dimension dimension: world.getDimensions()) {
            repairsPerformed |= repairDimension(dimension);
        }
        
        if (repairsPerformed) {
            System.out.println("Repairs performed. Writing world out to " + args[0] + ".repaired");
            try (ObjectOutputStream out = new ObjectOutputStream(new GZIPOutputStream(new FileOutputStream(args[0] + ".repaired")))) {
                out.writeObject(world);
            }
        } else {
            System.out.println("No repairs performed.");
        }
    }
    
    private static final boolean repairDimension(Dimension dimension) {
        TileFactory tileFactory = dimension.getTileFactory();
        if (tileFactory instanceof HeightMapTileFactory) {
            HeightMap heightMap = ((HeightMapTileFactory) tileFactory).getHeightMap();
            if ((heightMap instanceof SumHeightMap) && ((((SumHeightMap) heightMap).getHeightMap1() == null) || (((SumHeightMap) heightMap).getHeightMap2() == null))) {
                System.out.println("Broken height map found in dimension " + dimension.getName() + "; replacing with default height map");
                heightMap = new ConstantHeightMap(46);
//                heightMap = new SumHeightMap(new ConstantHeightMap(58), new NoiseHeightMap(20, 1.0, 1));
                ((HeightMapTileFactory) tileFactory).setHeightMap(heightMap);
                return true;
            }
        }
        return false;
    }
}