/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.tools;

import org.pepsoft.minecraft.JavaLevel;
import org.pepsoft.worldpainter.DefaultPlugin;
import org.pepsoft.worldpainter.exporting.JavaMinecraftWorld;
import org.pepsoft.worldpainter.exporting.MinecraftWorld;

import java.io.File;
import java.io.IOException;

import static org.pepsoft.minecraft.Block.BLOCK_TYPE_NAMES;
import static org.pepsoft.minecraft.Constants.BLK_AIR;
import static org.pepsoft.minecraft.Constants.VERSION_MCREGION;

/**
 *
 * @author pepijn
 */
public class DumpLighting {
    public static void main(String[] args) throws IOException {
        File levelDatFile = new File(args[0]);
        int x = Integer.parseInt(args[1]);
        int y = Integer.parseInt(args[2]);
        int z = Integer.parseInt(args[3]);
        JavaLevel level = JavaLevel.load(levelDatFile);
        MinecraftWorld world = new JavaMinecraftWorld(levelDatFile.getParentFile(), 0, level.getMaxHeight(), level.getVersion() == VERSION_MCREGION ? DefaultPlugin.JAVA_MCREGION : DefaultPlugin.JAVA_ANVIL, true, CACHE_SIZE);
        for (int dy = 16; dy >= -62; dy--) {
            for (int dx = -16; dx <= 16; dx++) {
                int blockX = x + dx, blockZ = z;
                int blockType = world.getBlockTypeAt(blockX, blockZ, y + dy);
                System.out.print('[');
                System.out.print(blockType != BLK_AIR ? BLOCK_TYPE_NAMES[blockType].substring(0, 3).toUpperCase() : "   ");
                System.out.print(';');
                int skyLightLevel = world.getSkyLightLevel(blockX, blockZ, y + dy);
                if (skyLightLevel < 15) {
                    if (skyLightLevel < 10) {
                        System.out.print('0');
                    }
                    System.out.print(skyLightLevel);
                } else {
                    System.out.print("  ");
                }
//                int blockLightLevel = world.getBlockLightLevel(blockX, blockZ, y + dy);
//                if (blockLightLevel > 0) {
//                    if (blockLightLevel < 10) {
//                        System.out.print('0');
//                    }
//                    System.out.print(blockLightLevel);
//                } else {
//                    System.out.print("  ");
//                }
                System.out.print(']');
            }
            System.out.println();
        }
    }
    
    private static final int CACHE_SIZE = 100;
}