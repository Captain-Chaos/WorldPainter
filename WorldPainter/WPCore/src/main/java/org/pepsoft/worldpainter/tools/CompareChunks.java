package org.pepsoft.worldpainter.tools;

import org.jnbt.CompoundTag;
import org.jnbt.NBTInputStream;
import org.pepsoft.minecraft.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import static org.pepsoft.minecraft.Constants.DATA_VERSION_MC_1_12_2;
import static org.pepsoft.minecraft.Constants.VERSION_MCREGION;
import static org.pepsoft.minecraft.Material.AIR;

public class CompareChunks {
    public static void main(String[] args) throws IOException {
        File levelDatFile1 = new File(args[0]);
        File levelDatFile2 = new File(args[1]);
        int blockX = Integer.parseInt(args[2]);
        int blockZ = Integer.parseInt(args[3]);
        int chunkX = blockX >> 4;
        int chunkZ = blockZ >> 4;
        Chunk chunk1 = loadChunk(levelDatFile1, chunkX, chunkZ);
        Chunk chunk2 = loadChunk(levelDatFile2, chunkX, chunkZ);
        if (chunk1.getMaxHeight() != chunk2.getMaxHeight()) {
            throw new IllegalArgumentException("Map heights not equal");
        }

        for (int y = 0; y < chunk1.getMaxHeight(); y++) {
            boolean block1Found = false;
            boolean block2Found = false;
            x:
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    if (chunk1.getMaterial(x, y, z) != AIR) {
                        block1Found = true;
                        if (block2Found) {
                            break x;
                        }
                    }
                    if (chunk2.getMaterial(x, y, z) != AIR) {
                        block2Found = true;
                        if (block1Found) {
                            break x;
                        }
                    }
                }
            }
            if ((! block1Found) && (! block2Found)) {
                continue;
            }
            for (int z = 0; z < 16; z++) {
                for (int x = 0; x < 16; x++) {
                    Material material1 = chunk1.getMaterial(x, y, z);
                    Material material2 = chunk2.getMaterial(x, y, z);
                    if (! material1.equals(material2)) {
                        System.out.printf("Difference @ %d,%d,%d%n" +
                                "Material in chunk 1: %s%n" +
                                "Material in chunk 2: %s%n", x, y, z, material1.toFullString(), material2.toFullString());
                    }
                }
            }
        }
    }

    public static Chunk loadChunk(File levelDatFile, int chunkX, int chunkZ) throws IOException {
        JavaLevel level = JavaLevel.load(levelDatFile);
        CompoundTag chunkTag;
        try (InputStream chunkIn = RegionFileCache.getChunkDataInputStream(levelDatFile.getParentFile(), chunkX, chunkZ, level.getVersion())) {
            if (chunkIn != null) {
                try (NBTInputStream in = new NBTInputStream(chunkIn)) {
                    chunkTag = (CompoundTag) in.readTag();
                }
            } else {
                throw new IllegalArgumentException(String.format("Chunk %d,%d not present!%n", chunkX, chunkZ));
            }
        }
        return (level.getVersion() == VERSION_MCREGION)
                ? new MCRegionChunk(chunkTag, level.getMaxHeight())
                : (((level.getDataVersion() <= DATA_VERSION_MC_1_12_2) || (level.getDataVersion() == 0))
                ? new MC12AnvilChunk(chunkTag, level.getMaxHeight())
                : new MC115AnvilChunk(chunkTag, level.getMaxHeight()));
    }
}
