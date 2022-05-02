package org.pepsoft.worldpainter.util;

import org.pepsoft.minecraft.Chunk;
import org.pepsoft.worldpainter.BiomeScheme;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.Platform;
import org.pepsoft.worldpainter.biomeschemes.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Comparator.comparing;
import static org.pepsoft.worldpainter.DefaultPlugin.*;
import static org.pepsoft.worldpainter.biomeschemes.Minecraft1_18Biomes.MODERN_IDS;

/**
 * This class uses the Minecraft coordinate system (y is vertical).
 */
public final class BiomeUtils {
    public BiomeUtils() {
        customBiomeNames = null;
    }

    public BiomeUtils(Dimension dimension) {
        customBiomeNames = new String[256];
        if (dimension.getCustomBiomes() != null) {
            dimension.getCustomBiomes().forEach(customBiome -> customBiomeNames[customBiome.getId()] = customBiome.getName());
        }
    }

    /**
     * Sets the biome of an entire vertical column of a chunk, regardless of what type of biomes the chunk supports.
     */
    public void set2DBiome(Chunk chunk, int x, int z, int biome) {
        if (chunk.isBiomesSupported()) {
            chunk.setBiome(x, z, biome);
        } else if (chunk.is3DBiomesSupported()) {
            final int blockX = x >> 2, blockZ = z >> 2, maxBlockY = chunk.getMaxHeight() >> 2; // TODO maxHeight can be extremely high, e.g. for CubicChunks! Currently not a problem because it does not support biomes, but something to consider
            for (int blockY = chunk.getMinHeight() >> 2; blockY < maxBlockY; blockY++) {
                chunk.set3DBiome(blockX, blockY, blockZ, biome);
            }
        } else if (chunk.isNamedBiomesSupported()) {
            final String biomeName = findBiomeName(biome);
            final int blockX = x >> 2, blockZ = z >> 2, maxBlockY = chunk.getMaxHeight() >> 2; // TODO maxHeight can be extremely high, e.g. for CubicChunks! Currently not a problem because it does not support biomes, but something to consider
            for (int blockY = chunk.getMinHeight() >> 2; blockY < maxBlockY; blockY++) {
                chunk.setNamedBiome(blockX, blockY, blockZ, biomeName);
            }
        } else {
            throw new IllegalArgumentException("Chunk of type " + chunk.getClass().getSimpleName() + " does not support any type of biomes");
        }
    }

    /**
     * Sets the biome of a 4x4x4 block of a chunk, regardless of whether the chunk supports 3D or named biomes.
     *
     * @param x The X coordinate, in 4x4x4 blocks, inside the chunk.
     * @param y The Y coordinate, in 4x4x4 blocks, inside the chunk.
     * @param z The Z coordinate, in 4x4x4 blocks, inside the chunk.
     */
    public void set3DBiome(Chunk chunk, int x, int y, int z, int biome) {
        if (chunk.is3DBiomesSupported()) {
            chunk.set3DBiome(x, y, z, biome);
        } else if (chunk.isNamedBiomesSupported()) {
            final String biomeName = findBiomeName(biome);
            chunk.setNamedBiome(x, y, z, biomeName);
        } else {
            throw new IllegalArgumentException("Chunk of type " + chunk.getClass().getSimpleName() + " does not support any type of 3D biomes");
        }
    }

    public static List<Integer> getAllBiomes(Platform platform, CustomBiomeManager customBiomeManager) {
        final List<Integer> allBiomes = new ArrayList<>();
        final BiomeScheme biomeScheme = getBiomeScheme(platform);
        for (int i = 0; i < biomeScheme.getBiomeCount(); i++) {
            if (biomeScheme.isBiomePresent(i)) {
                allBiomes.add(i);
            }
        }
        if (platform == JAVA_ANVIL_1_18) {
            allBiomes.sort(comparing(biomeScheme::getBiomeName));
        }
        List<CustomBiome> customBiomes = customBiomeManager.getCustomBiomes();
        if (customBiomes != null) {
            allBiomes.addAll(customBiomes.stream().map(CustomBiome::getId).collect(Collectors.toList()));
        }
        return allBiomes;
    }

    public static BiomeScheme getBiomeScheme(Platform platform) {
        if (platform == JAVA_ANVIL_1_18) { // TODO Make this dynamic
            return StaticBiomeInfo.INSTANCE;
        } else if ((platform == JAVA_ANVIL_1_15) || (platform == JAVA_ANVIL_1_17)) {
            return Minecraft1_17BiomeInfo.INSTANCE;
        } else if (platform == JAVA_MCREGION) {
            return Minecraft1_1BiomeInfo.INSTANCE;
        } else {
            return Minecraft1_12BiomeInfo.INSTANCE;
        }
    }

    private String findBiomeName(int biome) {
        String biomeName = MODERN_IDS[biome];
        if (biomeName == null) {
            biomeName = customBiomeNames[biome];
            if (biomeName == null) {
                throw new IllegalArgumentException("Biome " + biome + " is not a valid default or custom biome");
            }
        }
        return biomeName;
    }

    private final String[] customBiomeNames;
}