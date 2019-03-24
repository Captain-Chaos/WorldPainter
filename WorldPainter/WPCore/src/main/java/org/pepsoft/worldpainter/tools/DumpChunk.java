/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.tools;

import org.jnbt.CompoundTag;
import org.jnbt.NBTInputStream;
import org.pepsoft.minecraft.*;
import org.pepsoft.util.DataUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import static org.pepsoft.minecraft.Constants.DATA_VERSION_MC_1_12_2;
import static org.pepsoft.minecraft.Constants.VERSION_MCREGION;
import static org.pepsoft.minecraft.Material.AIR;

/**
 *
 * @author pepijn
 */
public class DumpChunk {
    public static void main(String[] args) throws IOException {
        File levelDatFile = new File(args[0]);
        int blockX = Integer.parseInt(args[1]);
        int blockZ = Integer.parseInt(args[2]);
        int chunkX = blockX >> 4;
        int chunkZ = blockZ >> 4;
        Level level = Level.load(levelDatFile);
        CompoundTag chunkTag;
        try (InputStream chunkIn = RegionFileCache.getChunkDataInputStream(levelDatFile.getParentFile(), chunkX, chunkZ, level.getVersion())) {
            if (chunkIn != null) {
                try (NBTInputStream in = new NBTInputStream(chunkIn)) {
                    chunkTag = (CompoundTag) in.readTag();
                }
            } else {
                System.err.printf("Chunk %d,%d not present!%n", chunkX, chunkZ);
                System.exit(1);
                return;
            }
        }
        Chunk chunk = (level.getVersion() == VERSION_MCREGION)
                ? new MCRegionChunk(chunkTag, level.getMaxHeight())
                : (((level.getDataVersion() <= DATA_VERSION_MC_1_12_2) || (level.getDataVersion() == 0))
                    ? new MC12AnvilChunk(chunkTag, level.getMaxHeight())
                    : new MC113AnvilChunk(chunkTag, level.getMaxHeight()));

        if (! (chunk instanceof MCRegionChunk)) {
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
        }

        if (chunk instanceof MC113AnvilChunk) {
            for (Map.Entry<MC113AnvilChunk.HeightmapType, long[]> entry: ((MC113AnvilChunk) chunk).getHeightMaps().entrySet()) {
                System.out.println("Heightmap (type: " + entry.getKey() + ")");
                System.out.println("X-->");
                long[][] data = DataUtils.unpackDataArray(entry.getValue(), 9, 16);
                for (int z = 0; z < 16; z++) {
                    for (int x = 0; x < 16; x++) {
                        System.out.printf("[%3d]", data[z][x]);
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
            }
        }

        System.out.println("Blocks:");
        List<Entity> entities = chunk.getEntities();
        List<TileEntity> tileEntities = chunk.getTileEntities();
        for (int y = 0; y < level.getMaxHeight(); y++) {
            boolean blockFound = false;
x:          for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    if (chunk.getMaterial(x, y, z) != AIR) {
                        blockFound = true;
                        break x;
                    }
                }
            }
            if (! blockFound) {
                continue;
            }
            System.out.println("X-->");
            SortedMap<String, Set<Material>> materialsInSlice = new TreeMap<>();
            for (int z = 0; z < 16; z++) {
                for (int x = 0; x < 16; x++) {
                    Material material = chunk.getMaterial(x, y, z);
                    if (material != AIR) {
                        String name = material.name;
                        name = name.substring(name.indexOf(':') + 1);
                        String property = "";
                        Map<String, String> propertyMap = material.getProperties();
                        if (propertyMap != null) {
                            for (Map.Entry<String, String> entry: propertyMap.entrySet()) {
                                String value = entry.getValue();
                                if (value.equals("true")) {
                                    property = entry.getKey();
                                    break;
                                } else if (! value.equals("false")) {
                                    property = value;
                                    break;
                                }
                            }
                        }
                        String tag;
                        if (material.tileEntity) {
                            int count = 0;
                            for (Iterator<TileEntity> i = tileEntities.iterator(); i.hasNext(); ) {
                                TileEntity tileEntity = i.next();
                                if ((tileEntity.getX() == x) && (tileEntity.getY() == y) && (tileEntity.getZ() == z)) {
                                    count++;
                                    i.remove();
                                }
                            }
                            if (count == 1) {
                                tag = String.format("[%3.3s:%2.2s]", name, property);
                            } else {
                                tag = String.format("!%3.3s!%2d!", name, count);
                            }
                        } else {
                            tag = String.format("[%3.3s:%2.2s]", name, property);
                        }
                        System.out.print(tag);
                        materialsInSlice.computeIfAbsent(tag, materialsForTag -> new HashSet<>()).add(material);
//                        if (chunk.getBlockLightLevel(x, y, z) == 0) {
//                            System.out.printf("[%3.3s:  ]", name);
//                        } else {
//                            System.out.printf("[%3.3s:%2d]", name, chunk.getBlockLightLevel(x, y, z));
//                        }
//                        System.out.printf("[%3.3s:%2d]", name, chunk.getBlockLightLevel(x, y, z));
                    } else {
                        if (chunk.getSkyLightLevel(x, y, z) == 15) {
                            System.out.print("[   :  ]");
                        } else {
                            System.out.printf("[   :%2d]", chunk.getSkyLightLevel(x, y, z));
                        }
//                        if (chunk.getBlockLightLevel(x, y, z) == 0) {
//                            System.out.print("[   :  ]");
//                        } else {
//                            System.out.printf("[   :%2d]", chunk.getBlockLightLevel(x, y, z));
//                        }
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
            materialsInSlice.forEach((tag, materials) -> System.out.println(tag + ": " + materials));

            for (Entity entity: entities) {
                if ((entity.getPos()[1] >= y) && (entity.getPos()[1] < (y + 1))) {
                    System.out.println("Entity " + entity.getId() + ": " + entity.toString().replaceAll("[\\r\\n]", ""));
                }
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