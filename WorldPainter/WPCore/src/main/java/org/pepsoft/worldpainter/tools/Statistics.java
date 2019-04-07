/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.tools;

import org.pepsoft.minecraft.ChunkStore;
import org.pepsoft.minecraft.Level;
import org.pepsoft.minecraft.Material;
import org.pepsoft.worldpainter.Platform;
import org.pepsoft.worldpainter.plugins.PlatformManager;

import java.io.File;
import java.io.IOException;

import static org.pepsoft.minecraft.Constants.VERSION_ANVIL;
import static org.pepsoft.minecraft.Constants.VERSION_MCREGION;
import static org.pepsoft.worldpainter.Constants.DIM_NORMAL;

/**
 *
 * @author pepijn
 */
public class Statistics {
    public static void main(String[] args) throws IOException {
        File worldDir = new File(args[0]);
        File levelDatFile = new File(worldDir, "level.dat");
        Level level = Level.load(levelDatFile);
        if ((level.getVersion() != VERSION_MCREGION) && (level.getVersion() != VERSION_ANVIL)) {
            throw new UnsupportedOperationException("Level format version " + level.getVersion() + " not supported");
        }
        int maxHeight = level.getMaxHeight();
        int maxY = maxHeight - 1;
        int[][] blockTypeCounts = new int[maxHeight >> 4][4096];
        int[][] blockTypeTotals = new int[maxHeight >> 4][4096];
//        int totalBlockCount = 0, totalBlocksPerLevel = 0;
        System.out.println("Scanning " + worldDir);
        PlatformManager platformManager = PlatformManager.getInstance();
        Platform platform = platformManager.identifyMap(worldDir);
        ChunkStore chunkStore = platformManager.getChunkStore(platform, worldDir, DIM_NORMAL);
        chunkStore.visitChunks(chunk -> {
            for (int xx = 0; xx < 16; xx++) {
                for (int zz = 0; zz < 16; zz++) {
                    for (int y = maxY; y >= 0; y--) {
                        int blockType = chunk.getBlockType(xx, y, zz);
                        int dataValue = chunk.getDataValue(xx, y, zz);
                        blockTypeCounts[y >> 4][(blockType << 4) | dataValue]++;
                        blockTypeTotals[y >> 4][(blockType << 4) | dataValue]++;
//                        totalBlockCount++;
                    }
                }
            }
            return true;
        });

        System.out.println("\tGranite\tDiorite\tAndesite");
        for (int y = 0; y < maxHeight >> 4; y++) {
            int stoneLikeTotal = blockTypeTotals[y][Material.STONE.index]
                               + blockTypeTotals[y][Material.GRANITE.index]
                               + blockTypeTotals[y][Material.DIORITE.index]
                               + blockTypeTotals[y][Material.ANDESITE.index];
//            System.out.println("Total stonelike blocks: " + stoneLikeTotal);
            System.out.print(y + "\t");
            System.out.printf("%6.2f‰\t", ((float) blockTypeTotals[y][Material.GRANITE.index] / stoneLikeTotal * 1000));
            System.out.printf("%6.2f‰\t", ((float) blockTypeTotals[y][Material.DIORITE.index] / stoneLikeTotal * 1000));
            System.out.printf("%6.2f‰%n", ((float) blockTypeTotals[y][Material.ANDESITE.index] / stoneLikeTotal * 1000));
        }
    }
}