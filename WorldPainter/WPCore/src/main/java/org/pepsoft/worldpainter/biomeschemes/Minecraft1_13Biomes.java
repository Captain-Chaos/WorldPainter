package org.pepsoft.worldpainter.biomeschemes;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * All Minecraft 1.13 biome IDs, plus names and other info.
 */
public interface Minecraft1_13Biomes extends Minecraft1_7Biomes {
    int BIOME_END_SMALL_ISLANDS = 40;
    int BIOME_END_MIDLANDS = 41;
    int BIOME_END_HIGHLANDS = 42;
    int BIOME_END_BARRENS = 43;
    int BIOME_WARM_OCEAN = 44;
    int BIOME_LUKEWARM_OCEAN = 45;
    int BIOME_COLD_OCEAN = 46;
    int BIOME_DEEP_WARM_OCEAN = 47;
    int BIOME_DEEP_LUKEWARM_OCEAN = 48;
    int BIOME_DEEP_COLD_OCEAN = 49;
    int BIOME_DEEP_FROZEN_OCEAN = 50;

    int BIOME_VOID = 127;

    int FIRST_UNALLOCATED_ID = BIOME_DEEP_FROZEN_OCEAN + 1;

    String[] BIOME_NAMES = {
            "Ocean",
            "Plains",
            "Desert",
            "Mountains",
            "Forest",
            "Taiga",
            "Swamp",
            "River",
            "Nether",
            "End",

            "Frozen Ocean",
            "Frozen River",
            "Snowy Tundra",
            "Snowy Mountains",
            "Mushroom Fields",
            "Mushroom Fields Shore",
            "Beach",
            "Desert Hills",
            "Wooded Hills",
            "Taiga Hills",

            "Mountain Edge",
            "Jungle",
            "Jungle Hills",
            "Jungle Edge",
            "Deep Ocean",
            "Stone Shore",
            "Snowy Beach",
            "Birch Forest",
            "Birch Forest Hills",
            "Dark Forest",

            "Snowy Taiga",
            "Snowy Taiga Hills",
            "Giant Tree Taiga",
            "Giant Tree Taiga Hills",
            "Wooded Mountains",
            "Savanna",
            "Savanna Plateau",
            "Badlands",
            "Wooded Badlands Plateau",
            "Badlands Plateau",

            "Small End Islands",
            "End Midlands",
            "End Highlands",
            "End Barrens",
            "Warm Ocean",
            "Lukewarm Ocean",
            "Cold Ocean",
            "Deep Warm Ocean",
            "Deep Lukewarm Ocean",
            "Deep Cold Ocean",

            "Deep Frozen Ocean",
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,

            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,

            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,

            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,

            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,

            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,

            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,

            null,
            null,
            null,
            null,
            null,
            null,
            null,
            "The Void",
            null,
            "Sunflower Plains",

            "Desert Lakes",
            "Gravelly Mountains",
            "Flower Forest",
            "Taiga Mountains",
            "Swamp Hills",
            null,
            null,
            null,
            null,
            null,

            "Ice Spikes",
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            "Modified Jungle",

            null,
            "Modified Jungle Edge",
            null,
            null,
            null,
            "Tall Birch Forest",
            "Tall Birch Hills",
            "Dark Forest Hills",
            "Snowy Taiga Mountains",
            null,

            "Giant Spruce Taiga",
            "Giant Spruce Taiga Hills",
            "Gravelly Mountains+",
            "Shattered Savanna",
            "Shattered Savanna Plateau",
            "Eroded Badlands",
            "Modified Wooded Badlands Plateau",
            "Modified Badlands Plateau"
    };

    Set<Integer> DRY_BIOMES = new HashSet<>(Arrays.asList(BIOME_DESERT, BIOME_DESERT_HILLS, BIOME_DESERT_M, BIOME_HELL, BIOME_SAVANNA, BIOME_SAVANNA_M, BIOME_MESA, BIOME_MESA_BRYCE, BIOME_SAVANNA_PLATEAU, BIOME_SAVANNA_PLATEAU_M, BIOME_MESA_PLATEAU, BIOME_MESA_PLATEAU_F, BIOME_MESA_PLATEAU_F_M, BIOME_MESA_PLATEAU_M));
    Set<Integer> COLD_BIOMES = new HashSet<>(Arrays.asList(BIOME_FROZEN_OCEAN, BIOME_FROZEN_RIVER, BIOME_ICE_MOUNTAINS, BIOME_ICE_PLAINS, BIOME_TAIGA_HILLS, BIOME_ICE_PLAINS_SPIKES, BIOME_COLD_BEACH, BIOME_COLD_TAIGA, BIOME_COLD_TAIGA_M, BIOME_DEEP_FROZEN_OCEAN));
    Set<Integer> FORESTED_BIOMES = new HashSet<>(Arrays.asList(BIOME_FOREST, BIOME_SWAMPLAND, BIOME_TAIGA, BIOME_FOREST_HILLS, BIOME_TAIGA_HILLS, BIOME_JUNGLE, BIOME_JUNGLE_HILLS, BIOME_JUNGLE_EDGE, BIOME_JUNGLE_EDGE_M, BIOME_JUNGLE_M, BIOME_BIRCH_FOREST, BIOME_BIRCH_FOREST_HILLS, BIOME_BIRCH_FOREST_HILLS_M, BIOME_BIRCH_FOREST_M, BIOME_TAIGA_M, BIOME_COLD_TAIGA, BIOME_COLD_TAIGA_HILLS, BIOME_COLD_TAIGA_M, BIOME_MEGA_SPRUCE_TAIGA, BIOME_MEGA_SPRUCE_TAIGA_HILLS, BIOME_MEGA_TAIGA, BIOME_MEGA_TAIGA_HILLS, BIOME_ROOFED_FOREST, BIOME_ROOFED_FOREST_M, BIOME_SAVANNA, BIOME_SAVANNA_M, BIOME_SAVANNA_PLATEAU, BIOME_SAVANNA_PLATEAU_M));
    Set<Integer> SWAMPY_BIOMES = new HashSet<>(Arrays.asList(BIOME_SWAMPLAND, BIOME_SWAMPLAND_M));
}