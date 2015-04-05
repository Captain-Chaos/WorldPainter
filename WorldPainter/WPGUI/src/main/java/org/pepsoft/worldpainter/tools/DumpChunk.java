/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.tools;

import org.jnbt.CompoundTag;
import org.jnbt.NBTInputStream;
import org.pepsoft.minecraft.*;

import java.io.File;
import java.io.IOException;

import static org.pepsoft.minecraft.Block.BLOCK_TYPE_NAMES;
import static org.pepsoft.minecraft.Constants.SUPPORTED_VERSION_1;

/**
 *
 * @author pepijn
 */
public class DumpChunk {
    public static void main(String[] args) throws IOException {
        File levelDatFile = new File(args[0]);
        int chunkX = Integer.parseInt(args[1]);
        int chunkY = Integer.parseInt(args[2]);
        Level level = Level.load(levelDatFile);
        CompoundTag tag;
        NBTInputStream in = new NBTInputStream(RegionFileCache.getChunkDataInputStream(levelDatFile.getParentFile(), chunkX, chunkY, level.getVersion()));
        try {
            tag = (CompoundTag) in.readTag();
        } finally {
            in.close();
        }
        Chunk chunk = (level.getVersion() == SUPPORTED_VERSION_1)
                ? new ChunkImpl(tag, level.getMaxHeight())
                : new ChunkImpl2(tag, level.getMaxHeight());
        for (int y = 0; y < level.getMaxHeight(); y++) {
            boolean blockFound = false;
x:          for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    if (chunk.getBlockType(x, y, z) != 0) {
                        blockFound = true;
                        break x;
                    }
                }
            }
            if (! blockFound) {
                break;
            }
            System.out.println("X-->");
            for (int z = 0; z < 16; z++) {
                for (int x = 0; x < 16; x++) {
                    int blockType = chunk.getBlockType(x, y, z);
                    int data = chunk.getDataValue(x, y, z);
                    if (blockType > 0) {
                        if (data > 0) {
                            System.out.printf("[%3.3s:%2d]", BLOCK_TYPE_NAMES[blockType], data);
                        } else {
                            System.out.printf("[%3.3s:  ]", BLOCK_TYPE_NAMES[blockType]);
                        }
                    } else {
                        System.out.print("[   :  ]");
                    }
                }
                if (z == 0) {
                    System.out.print(" Z");
                } else if (z == 1) {
                    System.out.print(" |");
                } else if (z == 2) {
                    System.out.print(" v");
                } else if (z == 15) {
                    System.out.print(" Y: " + y);
                }
                System.out.println();
            }
        }
    }
}