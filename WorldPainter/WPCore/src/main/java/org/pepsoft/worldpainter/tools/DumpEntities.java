/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.tools;

import java.io.File;
import java.io.IOException;
import org.jnbt.CompoundTag;
import org.jnbt.NBTInputStream;
import org.pepsoft.minecraft.ChunkImpl2;
import org.pepsoft.minecraft.Painting;
import org.pepsoft.minecraft.RegionFile;

/**
 *
 * @author pepijn
 */
public class DumpEntities {
    public static void main(String[] args) throws IOException {
        File worldDir = new File(args[0]);
        File[] regionFiles = new File(worldDir, "region").listFiles();
        for (File file: regionFiles) {
            RegionFile regionFile = new RegionFile(file);
            try {
                for (int x = 0; x < 32; x++) {
                    for (int z = 0; z < 32; z++) {
                        if (regionFile.containsChunk(x, z)) {
                            CompoundTag tag;
                            try (NBTInputStream in = new NBTInputStream(regionFile.getChunkDataInputStream(x, z))) {
                                tag = (CompoundTag) in.readTag();
                            }
                            ChunkImpl2 chunk = new ChunkImpl2(tag, 256);
                            /*&& (((Painting) entity).getTileX() == 40) && (((Painting) entity).getTileZ() == 31)*/
                            chunk.getEntities().stream().filter(entity -> (entity instanceof Painting)).forEach(System.out::println);
                        }
                    }
                }
            } finally {
                regionFile.close();
            }
        }
        
    }
}