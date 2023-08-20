package org.pepsoft.worldpainter.util;

import com.google.common.collect.ImmutableSet;
import org.pepsoft.minecraft.Chunk;
import org.pepsoft.minecraft.Material;
import org.pepsoft.worldpainter.exporting.MinecraftWorld;

import java.awt.*;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static org.pepsoft.minecraft.Constants.*;
import static org.pepsoft.minecraft.Material.WATERLOGGED;

public class MinecraftWorldUtils {
    public static void assertEquals(String role1, MinecraftWorld world1, String role2, MinecraftWorld world2, Rectangle area) {
        for (int x = area.x; x < area.x + area.width; x++) {
            for (int z = area.y; z < area.y + area.height; z++) {
                Chunk chunk1 = world1.getChunk(x, z);
                Chunk chunk2 = world2.getChunk(x, z);
                if (chunk1 != null) {
                    if (chunk2 != null) {
                        assertEquals(role1, chunk1, role2, chunk2);
                    } else {
                        throw new AssertionError("Chunk " + x + "," + z + " exists in " + role1 + " but not in " + role2);
                    }
                } else if (chunk2 != null) {
                    throw new AssertionError("Chunk " + x + "," + z + " exists in " + role2 + " but not in " + role1);
                }
            }
        }
    }

    public static void assertEquals(String role1, Chunk chunk1, String role2, Chunk chunk2) {
        int offsetX = chunk1.getxPos() << 4, offsetZ = chunk1.getzPos() << 4;
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int maxY = Math.max(chunk1.getHighestNonAirBlock(x, z), chunk2.getHighestNonAirBlock(x, z));
                for (int y = 0; y <= maxY; y++) {
                    Material material1 = chunk1.getMaterial(x, y, z);
                    Material material2 = chunk2.getMaterial(x, y, z);
                    if (material1.name.equals("legacy:block_175")
                            && material1.getProperty("data_value").equals("8")
                            && material2.isNamedOneOf(MC_PEONY, MC_SUNFLOWER, MC_LILAC, MC_TALL_GRASS, MC_LARGE_FERN, MC_ROSE_BUSH)
                            && material2.getProperty(MC_HALF).equals("upper")) {
                        // Double high plant upper half block, which is a different block in 1.2 and 1.15
                        continue;
                    } else if (y <= 62) {
                        if (material1.blockType == BLK_STATIONARY_WATER) {
                            // Minecraft 1.12 has no underwater plants; so just check that the block also contains water
                            if (material2.isNotNamed(MC_WATER) && (!material2.watery) && (!material2.is(WATERLOGGED))) {
                                throw new AssertionError(role2 + " material " + material2 + " != " + material1 + " at " + x + "," + y + "," + z);
                            } else {
                                continue;
                            }
                        } else {
                            assertEquals(material1, role2, material2, "material", offsetX + x, y, offsetZ + z);
                        }
                    } else if (material1.isNamedOneOf(MC_AIR, MC_VINE, MC_OAK_LEAVES, MC_LILY_PAD, MC_DEAD_BUSH)
                            && material2.isNamedOneOf(MC_AIR, MC_VINE, MC_OAK_LEAVES, MC_LILY_PAD, MC_DEAD_BUSH)) {
                        // Some blocks are not exported the same between 1.12 and 1.15, so related blocks are sometimes
                        // different
                        // TODO find out why and remove this exception!
                        continue;
                    } else {
                        assertEquals(material1, role2, material2, "material", offsetX + x, y, offsetZ + z);
                    }
                    if (material1.isNotNamed(MC_WATER)) {
                        // The light distribution under water has changed in MC 1.13, so don't check it
                        assertEquals(chunk1.getSkyLightLevel(x, y, z), role2, chunk1.getSkyLightLevel(x, y, z), "sky light", offsetX + x, y, offsetZ + z);
                        assertEquals(chunk1.getBlockLightLevel(x, y, z), role2, chunk2.getBlockLightLevel(x, y, z), "block light", offsetX + x, y, offsetZ + z);
                    }
                }
            }
        }
    }

    @SuppressWarnings("StringEquality") // Interned string
    public static void assertEquals(Material material1, String role2, Material material2, String type, int x, int y, int z) {
        if (material1.name == material2.name) {
            Map<String, String> properties1 = material1.getProperties();
            Map<String, String> properties2 = material2.getProperties();
            if (properties1 != null) {
                properties1 = properties1.entrySet().stream().filter(entry -> ! IGNORED_PROPERTIES.contains(entry.getKey())).collect(Collectors.toMap(Entry::getKey, Entry::getValue));
                if (properties1.isEmpty()) {
                    properties1 = null;
                }
            }
            if (properties2 != null) {
                properties2 = properties2.entrySet().stream().filter(entry -> ! IGNORED_PROPERTIES.contains(entry.getKey())).collect(Collectors.toMap(Entry::getKey, Entry::getValue));
                if (properties2.isEmpty()) {
                    properties2 = null;
                }
            }
            if (Objects.equals(properties1, properties2)) {
                return;
            }
        }
        throw new AssertionError(role2 + " " + type + " " + material2 + " != " + material1 + " at " + x + "," + y + "," + z);
    }

    public static void assertEquals(Object object1, String role2, Object object2, String type, int x, int y, int z) {
        if (! object1.equals(object2)) {
            throw new AssertionError(role2 + " " + type + " " + object2 + " != " + object1 + " at " + x + "," + y + "," + z);
        }
    }

    private static final Set<String> IGNORED_PROPERTIES = ImmutableSet.of(MC_WATERLOGGED, MC_FACING, MC_SNOWY, MC_DISTANCE, MC_DOWN, MC_WEST, MC_NORTH, MC_EAST, MC_SOUTH);
}