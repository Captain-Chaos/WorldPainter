package org.pepsoft.worldpainter.tools;

import org.jnbt.*;
import org.pepsoft.minecraft.*;
import org.pepsoft.worldpainter.AbstractTool;
import org.pepsoft.worldpainter.Platform;
import org.pepsoft.worldpainter.platforms.JavaPlatformProvider;
import org.pepsoft.worldpainter.plugins.PlatformManager;
import org.pepsoft.worldpainter.plugins.PlatformProvider;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.*;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.synchronizedMap;
import static java.util.Collections.synchronizedSet;
import static org.pepsoft.worldpainter.Constants.DIM_NORMAL;

public class ScanMap extends AbstractTool {
    public static void main(String[] args) throws IOException {
        initialisePlatform();
        dumpTileEntities(args);
    }

    private static void dumpTileEntities(String[] args) {
        File worldDir = new File(args[0]);
        PlatformManager platformManager = PlatformManager.getInstance();
        Platform platform = platformManager.identifyPlatform(worldDir);
        ChunkStore chunkStore = platformManager.getChunkStore(platform, worldDir, DIM_NORMAL);
        chunkStore.visitChunks(chunk -> {
            List<TileEntity> tileEntities = chunk.getTileEntities();
            if (tileEntities != null) {
                tileEntities.forEach(System.out::println);
            }
            return true;
        });
    }

    private static void findTag(String[] args) throws IOException {
        File worldDir = new File(args[0]);
        PlatformManager platformManager = PlatformManager.getInstance();
        Platform platform = platformManager.identifyPlatform(worldDir);
        PlatformProvider platformProvider = platformManager.getPlatformProvider(platform);
        if (platformProvider instanceof JavaPlatformProvider) {
            final JavaPlatformProvider javaPlatformProvider = (JavaPlatformProvider) platformProvider;
            for (DataType dataType: javaPlatformProvider.getDataTypes(platform)) {
                final File[] regionFiles = javaPlatformProvider.getRegionFiles(platform, new File(worldDir, "region"), dataType);
                if (regionFiles == null) {
                    continue;
                }
                for (File file: regionFiles) {
                    try (RegionFile regionFile = new RegionFile(file, true)) {
                        System.out.println("Scanning region " + dataType + ": " + regionFile.getX() + "," + regionFile.getZ());
                        for (int x = 0; x < 32; x++) {
                            for (int z = 0; z < 32; z++) {
                                if (regionFile.containsChunk(x, z)) {
                                    try (NBTInputStream in = new NBTInputStream(regionFile.getChunkDataInputStream(x, z))) {
                                        final Tag tag = in.readTag();
                                        if (scanTag(tag, args[1])) {
                                            System.out.println('"' + args[1] + "\" found in chunk: ");
                                            System.out.println(tag);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private static boolean scanTag(Tag tag, String searchString) {
        if (tag.getName().contains(searchString)) {
            return true;
        }
        if (tag instanceof CompoundTag) {
            for (Map.Entry<String, Tag> entry: ((CompoundTag) tag).getValue().entrySet()) {
                if (entry.getKey().contains(searchString)) {
                    return true;
                }
                if (scanTag(entry.getValue(), searchString)) {
                    return true;
                }
            }
        } else if (tag instanceof StringTag) {
            if (((StringTag) tag).getValue().contains(searchString)) {
                return true;
            }
        } else if (tag instanceof ListTag) {
            for (Tag value: ((ListTag<?>) tag).getValue()) {
                if (scanTag(value, searchString)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static void findTileEntity(String[] args) {
        File worldDir = new File(args[0]);
        PlatformManager platformManager = PlatformManager.getInstance();
        Platform platform = platformManager.identifyPlatform(worldDir);
        ChunkStore chunkStore = platformManager.getChunkStore(platform, worldDir, DIM_NORMAL);
        chunkStore.visitChunks(chunk -> {
            if (chunk instanceof SectionedChunk) {
                SectionedChunk sectionedChunk = (SectionedChunk) chunk;
                for (SectionedChunk.Section section: sectionedChunk.getSections()) {
                    if (section == null) {
                        continue;
                    }
                    final List<TileEntity> tileEntities = chunk.getTileEntities();
                    if (tileEntities != null) {
                        for (TileEntity tileEntity : tileEntities) {
                            if (tileEntity.getId().equals(args[1])) {
                                System.out.println("Sign found @ " + tileEntity.getX() + "," + tileEntity.getY() + "," + tileEntity.getZ() + ": " + tileEntity);
                                System.out.println("Text2 in base 64: " + Base64.getEncoder().encodeToString(((StringTag) tileEntity.toNBT().getTag("Text2")).getValue().getBytes(UTF_8)));
                            }
                        }
                    }
                }
                return true;
            } else {
                throw new UnsupportedOperationException("Non sectioned chunks not yet supported");
            }
        });
    }

    private static void dumpChunks(String[] args) {
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