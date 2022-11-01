/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.tools;

import org.pepsoft.minecraft.*;
import org.pepsoft.util.DataUtils;
import org.pepsoft.worldpainter.AbstractTool;
import org.pepsoft.worldpainter.Platform;
import org.pepsoft.worldpainter.plugins.PlatformManager;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static java.util.stream.Collectors.joining;
import static org.pepsoft.minecraft.Material.AIR;
import static org.pepsoft.minecraft.Material.LEVEL;
import static org.pepsoft.worldpainter.Constants.DIM_NORMAL;

/**
 *
 * @author pepijn
 */
public class DumpChunk extends AbstractTool {
    public static void main(String[] args) throws IOException, ClassNotFoundException {
        initialisePlatform();

        final File levelDatFile = new File(args[0]);
        final int chunkX = Integer.parseInt(args[1]);
        final int chunkZ = Integer.parseInt(args[2]);
        final JavaLevel level = JavaLevel.load(levelDatFile);
        final File worldDir = levelDatFile.getParentFile();
        final Platform platform = PlatformManager.getInstance().identifyPlatform(worldDir);
        final Chunk chunk = PlatformManager.getInstance().getChunkStore(platform, worldDir, DIM_NORMAL).getChunk(chunkX, chunkZ);

        if (chunk.isBiomesAvailable()) {
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
        } else if (chunk.is3DBiomesAvailable()) {
            System.out.println("Biomes");
            for (int y = 0; y < level.getMaxHeight() >> 2; y++) {
                System.out.println("X-->");
                for (int z = 0; z < 4; z++) {
                    for (int x = 0; x < 4; x++) {
                        System.out.printf("[%3d]", chunk.get3DBiome(x, y, z));
                    }
                    if (z == 0) {
                        System.out.print(" Z");
                    } else if (z == 1) {
                        System.out.print(" |");
                    } else if (z == 2) {
                        System.out.print(" v");
                    } else {
                        System.out.print(" Y: " + y);
                    }
                    System.out.println();
                }
            }
        }

        if ((chunk instanceof MC115AnvilChunk) || (chunk instanceof MC118AnvilChunk)) {
            final Map<String, long[]> heightMaps = (chunk instanceof MC115AnvilChunk)
                    ? ((MC115AnvilChunk) chunk).getHeightMaps()
                    : ((MC118AnvilChunk) chunk).getHeightMaps();
            for (Map.Entry<String, long[]> entry: heightMaps.entrySet()) {
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
                    Integer waterLevel = material.getProperty(LEVEL);
                    if (material != AIR) {
                        String name = material.name;
                        name = name.substring(name.indexOf(':') + 1);
//                        String property = "";
//                        Map<String, String> propertyMap = material.getProperties();
//                        if (propertyMap != null) {
//                            for (Map.Entry<String, String> entry: propertyMap.entrySet()) {
//                                String value = entry.getValue();
//                                if (value.equals("true")) {
//                                    property = entry.getKey();
//                                    break;
//                                } else if (! value.equals("false")) {
//                                    property = value;
//                                    break;
//                                }
//                            }
//                        }
//                        String tag;
//                        if (material.tileEntity) {
//                            int count = 0;
//                            for (Iterator<TileEntity> i = tileEntities.iterator(); i.hasNext(); ) {
//                                TileEntity tileEntity = i.next();
//                                if ((tileEntity.getX() == x) && (tileEntity.getY() == y) && (tileEntity.getZ() == z)) {
//                                    count++;
//                                    i.remove();
//                                }
//                            }
//                            if (count == 1) {
//                                tag = String.format("[%3.3s:%2.2s]", name, property);
//                            } else {
//                                tag = String.format("!%3.3s!%2d!", name, count);
//                            }
//                        } else {
//                            tag = String.format("[%3.3s:%2.2s]", name, property);
//                        }
//                        System.out.print(tag);
//                        if (chunk.getSkyLightLevel(x, y, z) == 15) {
//                            System.out.printf("[%3.3s:  ]", name);
//                        } else {
//                            System.out.printf("[%3.3s:%2d]", name, chunk.getSkyLightLevel(x, y, z));
//                        }
                        if (waterLevel == null) {
                            System.out.printf("[%3.3s:  ]", name);
                        } else {
                            System.out.printf("[%3.3s:%2d]", name, waterLevel);
                        }
                        materialsInSlice.computeIfAbsent(name, materialsForTag -> new HashSet<>()).add(material);
//                        if (chunk.getBlockLightLevel(x, y, z) == 0) {
//                            System.out.printf("[%3.3s:  ]", name);
//                        } else {
//                            System.out.printf("[%3.3s:%2d]", name, chunk.getBlockLightLevel(x, y, z));
//                        }
//                        System.out.printf("[%3.3s:%2d]", name, chunk.getBlockLightLevel(x, y, z));
                    } else {
//                        if (chunk.getSkyLightLevel(x, y, z) == 15) {
//                            System.out.print("[   :  ]");
//                        } else {
//                            System.out.printf("[   :%2d]", chunk.getSkyLightLevel(x, y, z));
//                        }
                        if (waterLevel == null) {
                            System.out.print("[   :  ]");
                        } else {
                            System.out.printf("[   :%2d]", waterLevel);
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
            materialsInSlice.forEach((tag, materials) -> System.out.println(tag + ": " + materials.stream().map(Material::toFullString).collect(joining(", "))));

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