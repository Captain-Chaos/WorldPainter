package org.pepsoft.worldpainter.util;

import org.pepsoft.worldpainter.Platform;
import org.pepsoft.worldpainter.biomeschemes.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Comparator.comparing;
import static org.pepsoft.worldpainter.DefaultPlugin.*;

/**
 * Utility methods for working with biomes.
 */
public final class BiomeUtils {
    private BiomeUtils() {
        // Prevent instantiation
    }

    public static List<Integer> getAllBiomes(Platform platform, CustomBiomeManager customBiomeManager) {
        final List<Integer> allBiomes = new ArrayList<>();
        final String[] biomeNames;
        final boolean sortByName;
        if (platform == JAVA_ANVIL_1_18) { // TODO Make this dynamic
            biomeNames = Minecraft1_18Biomes.BIOME_NAMES;
            sortByName = true;
        } else if ((platform == JAVA_ANVIL_1_15) || (platform == JAVA_ANVIL_1_17)) {
            biomeNames = Minecraft1_17Biomes.BIOME_NAMES;
            sortByName = false;
        } else if (platform == JAVA_MCREGION) {
            biomeNames = Minecraft1_1BiomeScheme.BIOME_NAMES;
            sortByName = false;
        } else {
            biomeNames = Minecraft1_12BiomeScheme.BIOME_NAMES;
            sortByName = false;
        }
        for (int i = 0; i < Minecraft1_17Biomes.BIOME_NAMES.length; i++) {
            if (biomeNames[i] != null) {
                allBiomes.add(i);
            }
        }
        if (sortByName) {
            allBiomes.sort(comparing(b -> biomeNames[b]));
        }
        List<CustomBiome> customBiomes = customBiomeManager.getCustomBiomes();
        if (customBiomes != null) {
            allBiomes.addAll(customBiomes.stream().map(CustomBiome::getId).collect(Collectors.toList()));
        }
        return allBiomes;
    }
}