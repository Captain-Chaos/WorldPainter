package org.pepsoft.worldpainter.tools;

import org.pepsoft.minecraft.ChunkStore;
import org.pepsoft.minecraft.MC115AnvilChunk;
import org.pepsoft.minecraft.MC12AnvilChunk;
import org.pepsoft.minecraft.MCRegionChunk;
import org.pepsoft.worldpainter.AbstractTool;
import org.pepsoft.worldpainter.Platform;
import org.pepsoft.worldpainter.plugins.PlatformManager;

import java.awt.*;
import java.io.File;
import java.util.*;

import static java.util.Collections.synchronizedMap;
import static java.util.Collections.synchronizedSet;
import static org.pepsoft.worldpainter.Constants.DIM_NORMAL;

public class ScanMap extends AbstractTool {
    public static void main(String[] args) {
        initialisePlatform();

        int[] bounds = {Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE};
        Map<Point, String> statusMap = synchronizedMap(new HashMap<>());
        Set<String> heightmapTypes = synchronizedSet(new HashSet<>());
        File worldDir = new File(args[0]);
        PlatformManager platformManager = PlatformManager.getInstance();
        Platform platform = platformManager.identifyPlatform(worldDir);
        ChunkStore chunkStore = platformManager.getChunkStore(platform, worldDir, DIM_NORMAL);
        chunkStore.visitChunks(chunk -> {
            String status = "???";
            if (chunk instanceof MC115AnvilChunk) {
                status = ((MC115AnvilChunk) chunk).getStatus();
                heightmapTypes.addAll(((MC115AnvilChunk) chunk).getHeightMaps().keySet());
            } else if (chunk instanceof MC12AnvilChunk) {
                status = "112";
            } else if (chunk instanceof MCRegionChunk) {
                status = "MCR";
            }
            synchronized (bounds) {
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
            }
            statusMap.put(new Point(chunk.getxPos(), chunk.getzPos()), status);
            return true;
        });
        char[] line = new char[(bounds[1] - bounds[0]) * 4 + 3];
        for (int z = bounds[2]; z <= bounds[3]; z++) {
            Arrays.fill(line, ' ');
            for (int x = bounds[0]; x <= bounds[1]; x++) {
                if (x >  bounds[0]) {
                    line[(x - bounds[0]) * 4 - 1] = '|';
                }

                Point coords = new Point(x, z);
                if (statusMap.containsKey(coords)) {
                    String status = statusMap.get(coords);
                    if (status != null) {
                        line[(x - bounds[0]) * 4] = status.charAt(0);
                        line[(x - bounds[0]) * 4 + 1] = status.charAt(1);
                        line[(x - bounds[0]) * 4 + 2] = status.charAt(2);
                    } else {
                        line[(x - bounds[0]) * 4] = 'X';
                        line[(x - bounds[0]) * 4 + 1] = 'X';
                        line[(x - bounds[0]) * 4 + 2] = 'X';
                    }
                }
            }
            System.out.println(String.valueOf(line));
        }
        System.out.println("Statuses:");
        statusMap.values().stream().sorted().distinct().forEach(status -> System.out.println(status.substring(0, 3) + " -> " + status));
        System.out.println("XXX -> Chunk present but no status");
        System.out.println("Height map types encountered: " + heightmapTypes);
    }
}