/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.tools;

import java.io.File;
import java.io.IOException;
import org.pepsoft.minecraft.Constants;
import org.pepsoft.minecraft.Level;
import org.pepsoft.worldpainter.exporting.MinecraftWorld;
import org.pepsoft.worldpainter.exporting.MinecraftWorldImpl;

/**
 *
 * @author pepijn
 */
public class DumpWater {
    public static final void main(String[] args) throws IOException {
        File levelDatFile = new File(args[0]);
        int x = Integer.parseInt(args[1]);
        int y = Integer.parseInt(args[2]);
        int z = Integer.parseInt(args[3]);
        Level level = Level.load(levelDatFile);
        MinecraftWorld world = new MinecraftWorldImpl(levelDatFile.getParentFile(), 0, level.getMaxHeight(), level.getVersion(), true, CACHE_SIZE);
        for (int dy = 16; dy >= -16; dy--) {
            for (int dx = -16; dx <= 16; dx++) {
                int blockX = x + dx, blockZ = z;
                int blockType = world.getBlockTypeAt(blockX, blockZ, y + dy);
                System.out.print('[');
                System.out.print(blockType != Constants.BLK_AIR ? Constants.BLOCK_TYPE_NAMES[blockType].substring(0, 3).toUpperCase() : "   ");
                System.out.print(';');
                int data = world.getDataAt(blockX, blockZ, y + dy);
                if (data > 0) {
                    if (data < 10) {
                        System.out.print('0');
                    }
                    System.out.print(data);
                } else {
                    System.out.print("  ");
                }
                System.out.print(']');
            }
            System.out.println();
        }
    }
    
    private static final int CACHE_SIZE = 100;
}