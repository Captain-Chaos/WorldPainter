package org.pepsoft.worldpainter.tools;

import org.pepsoft.minecraft.MinecraftCoords;
import org.pepsoft.worldpainter.AbstractTool;
import org.pepsoft.worldpainter.Platform;
import org.pepsoft.worldpainter.platforms.JavaPlatformProvider;
import org.pepsoft.worldpainter.plugins.PlatformManager;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import static org.pepsoft.minecraft.DataType.REGION;

public class DumpRegions extends AbstractTool {
    public static void main(String[] args) {
        initialisePlatform();

        final File worldDir = new File(args[0]);
        final PlatformManager platformManager = PlatformManager.getInstance();
        final Platform platform = platformManager.identifyPlatform(worldDir);
        final JavaPlatformProvider platformProvider = (JavaPlatformProvider) platformManager.getPlatformProvider(platform);
        final File[] regionFiles = platformProvider.getRegionFiles(platform, new File(worldDir, "region"), REGION);
        int lowestX = Integer.MAX_VALUE, highestX = Integer.MIN_VALUE, lowestZ = Integer.MAX_VALUE, highestZ = Integer.MIN_VALUE;
        final Map<MinecraftCoords, File> regions = new HashMap<>();
        for (File file: regionFiles) {
            final String[] parts = file.getName().split("\\.");
            final int x = Integer.parseInt(parts[1]), z = Integer.parseInt(parts[2]);
            regions.put(new MinecraftCoords(x, z), file);
            if (x < lowestX) {
                lowestX = x;
            }
            if (x > highestX) {
                highestX = x;
            }
            if (z < lowestZ) {
                lowestZ = z;
            }
            if (z > highestZ) {
                highestZ = z;
            }
        }
        for (int z = lowestZ; z <= highestZ; z++) {
            for (int x = lowestX; x <= highestX; x++) {
                final MinecraftCoords key = new MinecraftCoords(x, z);
                if (regions.containsKey(key)) {
                    if (regions.get(key).length() == 0L) {
                        System.out.print("[ len==0]");
                    } else {
                        System.out.printf("[%3d,%3d]", x, z);
                    }
                } else {
                    System.out.print("         ");
                }
            }
            System.out.println();
        }
    }
}
