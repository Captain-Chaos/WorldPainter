package org.pepsoft.worldpainter.tools;

import org.pepsoft.minecraft.ChunkStore;
import org.pepsoft.minecraft.MC113AnvilChunk;
import org.pepsoft.worldpainter.Platform;
import org.pepsoft.worldpainter.plugins.PlatformManager;
import org.pepsoft.worldpainter.plugins.WPPluginManager;

import java.awt.*;
import java.io.File;
import java.util.*;

import static org.pepsoft.worldpainter.Constants.DIM_NORMAL;

public class ScanMap {
    public static void main(String[] args) {
        WPPluginManager.initialise(null);

        int[] bounds = {Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE};
        Map<Point, MC113AnvilChunk.Status> statusMap = new HashMap<>();
        Set<MC113AnvilChunk.HeightmapType> heightmapTypes = new HashSet<>();
        File worldDir = new File(args[0]);
        PlatformManager platformManager = PlatformManager.getInstance();
        Platform platform = platformManager.identifyMap(worldDir);
        ChunkStore chunkStore = platformManager.getChunkStore(platform, worldDir, DIM_NORMAL);
        chunkStore.visitChunks(chunk -> {
            MC113AnvilChunk.Status status = ((MC113AnvilChunk) chunk).getStatus();
            if (chunk.getxPos() < bounds[0]) {
                bounds[0] = chunk.getxPos();
            }
            if (chunk.getxPos() > bounds[1]) {
                bounds[1] = chunk.getxPos();
            }
            if (chunk.getzPos() < bounds[2]) {
                bounds[2] = chunk.getzPos();
            }
            if (chunk.getzPos() > bounds[3]) {
                bounds[3] = chunk.getzPos();
            }
            statusMap.put(new Point(chunk.getxPos(), chunk.getzPos()), status);
            heightmapTypes.addAll(((MC113AnvilChunk) chunk).getHeightMaps().keySet());
            return true;
        });
        char[] line = new char[(bounds[1] - bounds[0]) * 4 + 3];
        for (int z = bounds[2]; z <= bounds[3]; z++) {
            Arrays.fill(line, ' ');
            for (int x = bounds[0]; x <= bounds[1]; x++) {
                if (x >  bounds[0]) {
                    line[(x - bounds[0]) * 4 - 1] = '|';
                }
                MC113AnvilChunk.Status status = statusMap.get(new Point(x, z));
                if (status != null) {
                    String name = status.name();
                    line[(x - bounds[0]) * 4]     = name.charAt(0);
                    line[(x - bounds[0]) * 4 + 1] = name.charAt(1);
                    line[(x - bounds[0]) * 4 + 2] = name.charAt(2);
                }
            }
            System.out.println(String.valueOf(line));
        }
        System.out.println("Height map types encountered: " + heightmapTypes);
    }
}