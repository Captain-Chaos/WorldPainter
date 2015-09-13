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
import java.util.Iterator;
import java.util.List;

import static org.pepsoft.minecraft.Block.BLOCKS;
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
        try (NBTInputStream in = new NBTInputStream(RegionFileCache.getChunkDataInputStream(levelDatFile.getParentFile(), chunkX, chunkY, level.getVersion()))) {
            tag = (CompoundTag) in.readTag();
        }
        Chunk chunk = (level.getVersion() == SUPPORTED_VERSION_1)
                ? new ChunkImpl(tag, level.getMaxHeight())
                : new ChunkImpl2(tag, level.getMaxHeight());

        System.out.println("Biomes");
        System.out.println("X-->");
        for (int z = 0; z < 16; z++) {
            for (int x = 0; x < 16; x++) {
                System.out.printf("[%3d]", chunk.getBiome(x, z));
            }
            if (z == 0) {
                System.out.print(" Z");
            } else if (z == 1) {
                System.out.print(" |");
            } else if (z == 2) {
                System.out.print(" v");
            }
            System.out.println();
        }

        System.out.println("Blocks:");
        List<TileEntity> tileEntities = chunk.getTileEntities();
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
                        if (BLOCKS[blockType].tileEntity) {
                            int count = 0;
                            for (Iterator<TileEntity> i = tileEntities.iterator(); i.hasNext(); ) {
                                TileEntity tileEntity = i.next();
                                if ((tileEntity.getX() == x) && (tileEntity.getY() == y) && (tileEntity.getZ() == z)) {
                                    count++;
                                    i.remove();
                                }
                            }
                            if (count == 1) {
                                if (data > 0) {
                                    System.out.printf("[%3.3s:%2d]", BLOCK_TYPE_NAMES[blockType], data);
                                } else {
                                    System.out.printf("[%3.3s:  ]", BLOCK_TYPE_NAMES[blockType]);
                                }
                            } else {
                                System.out.printf("!%3.3s!%2d!", BLOCK_TYPE_NAMES[blockType], count);
                            }
                        } else {
                            if (data > 0) {
                                System.out.printf("[%3.3s:%2d]", BLOCK_TYPE_NAMES[blockType], data);
                            } else {
                                System.out.printf("[%3.3s:  ]", BLOCK_TYPE_NAMES[blockType]);
                            }
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
        if (! tileEntities.isEmpty()) {
            System.out.println("Unmatched tile entities!");
            for (TileEntity tileEntity: tileEntities) {
                System.out.println(tileEntity.getId() + "@" + tileEntity.getX() + "," + tileEntity.getY() + "," + tileEntity.getZ());
            }
        }
    }
}