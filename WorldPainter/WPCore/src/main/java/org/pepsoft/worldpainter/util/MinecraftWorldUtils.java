package org.pepsoft.worldpainter.util;

import org.pepsoft.minecraft.Chunk;
import org.pepsoft.minecraft.Material;
import org.pepsoft.worldpainter.exporting.MinecraftWorld;

import java.awt.*;

public class MinecraftWorldUtils {
    public static void assertEquals(MinecraftWorld expected, MinecraftWorld actual, Rectangle area) {
        if (expected.getMaxHeight() != actual.getMaxHeight()) {
            throw new AssertionError("Expected maxHeight is " + expected.getMaxHeight() + " but actual maxHeight is " + actual.getMaxHeight());
        }
        for (int x = area.x; x < area.x + area.width; x++) {
            for (int z = area.y; z < area.y + area.height; z++) {
                Chunk expChunk = expected.getChunk(x, z);
                Chunk actChunk = actual.getChunk(x, z);
                if (expChunk != null) {
                    if (actChunk != null) {
                        assertEquals(expChunk, actChunk, expected.getMaxHeight());
                    } else {
                        throw new AssertionError("Chunk " + x + "," + z + " exists in expected but not in actual");
                    }
                } else if (actChunk != null) {
                    throw new AssertionError("Chunk " + x + "," + z + " exists in actual but not in expected");
                }
            }
        }
    }

    public static void assertEquals(Chunk expected, Chunk actual, int maxHeight) {
        int offsetX = expected.getxPos() << 4, offsetZ = expected.getzPos() << 4;
        for (int y = 0; y < maxHeight; y++) {
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    Material expMat = expected.getMaterial(x, y, z);
                    Material actMat = actual.getMaterial(x, y, z);
                    if (expMat == Material.SNOW_BLOCK) {
                        // Snow blocks were previously insubstantial but now are
                        // no longer, so they may have been replaced with solid
                        // or other insubstantial blocks
                    } else {
                        assertEquals(expMat, actMat, "material", offsetX + x, y, offsetZ + z);
                        assertEquals(expected.getSkyLightLevel(x, y, z), expected.getSkyLightLevel(x, y, z), "sky light", offsetX + x, y, offsetZ + z);
                        assertEquals(expected.getBlockLightLevel(x, y, z), actual.getBlockLightLevel(x, y, z), "block light", offsetX + x, y, offsetZ + z);
                    }
                }
            }
        }
    }

    public static void assertEquals(Object expected, Object actual, String type, int x, int y, int z) {
        if (! expected.equals(actual)) {
            throw new AssertionError(type + " " + expected + " != " + actual + " at " + x + "," + y + "," + z);
        }
    }
}