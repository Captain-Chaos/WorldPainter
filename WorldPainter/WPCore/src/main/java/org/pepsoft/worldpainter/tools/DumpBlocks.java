package org.pepsoft.worldpainter.tools;

import org.pepsoft.minecraft.Level;
import org.pepsoft.worldpainter.Platform;
import org.pepsoft.worldpainter.exporting.JavaMinecraftWorld;
import org.pepsoft.worldpainter.exporting.MinecraftWorld;

import java.io.File;
import java.io.IOException;

import static org.pepsoft.minecraft.Constants.DATA_VERSION_MC_1_12_2;
import static org.pepsoft.minecraft.Constants.VERSION_MCREGION;
import static org.pepsoft.worldpainter.Constants.DIM_NORMAL;
import static org.pepsoft.worldpainter.DefaultPlugin.*;

public class DumpBlocks {
    public static void main(String[] args) throws IOException {
        File levelDatFile = new File(args[0]);
        int blockX = Integer.parseInt(args[1]);
        int blockY = Integer.parseInt(args[2]);
        int blockZ = Integer.parseInt(args[3]);
        Level level = Level.load(levelDatFile);
        Platform platform = (level.getVersion() == VERSION_MCREGION) ? JAVA_MCREGION : ((level.getDataVersion() <= DATA_VERSION_MC_1_12_2) ? JAVA_ANVIL : JAVA_ANVIL_1_13);
        MinecraftWorld world = new JavaMinecraftWorld(levelDatFile.getParentFile(), DIM_NORMAL, level.getMaxHeight(), platform, true, 16);
        for (int dx = -4; dx <= 4; dx++) {
            for (int dz = -4; dz <= 4; dz++) {
                dumpBlock(world, blockX + dx, blockY, blockZ + dz);
            }
        }
    }

    private static void dumpBlock(MinecraftWorld world, int x, int y, int z) {

    }
}