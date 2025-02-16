package org.pepsoft.worldpainter.biomeschemes;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static java.util.Collections.unmodifiableMap;
import static org.pepsoft.minecraft.Constants.MC_PLAINS;

/**
 * A combination of Minecraft 1.17 and earlier biomes with Minecraft 1.18+ biomes, providing continuity between them.
 * Technical string IDs for both are provided. For Minecraft 1.18+-only biomes, high numerical IDs have been
 * synthesised. Display names for biomes that occur in Minecraft 1.18 are those for Minecraft 1.18, for legacy biomes
 * they are those from Minecraft 1.17.
 */
public interface Minecraft1_21Biomes extends Minecraft1_17Biomes {
    int BIOME_WINDSWEPT_HILLS = 3;

    int BIOME_SNOWY_PLAINS = 12;

    int BIOME_SPARSE_JUNGLE = 23;
    int BIOME_STONY_SHORE = 25;

    int BIOME_OLD_GROWTH_PINE_TAIGA = 32;
    int BIOME_WINDSWEPT_FOREST = 34;
    int BIOME_WOODED_BADLANDS = 38;

    int BIOME_WINDSWEPT_GRAVELLY_HILLS = 131;

    int BIOME_OLD_GROWTH_BIRCH_FOREST = 155;

    int BIOME_OLD_GROWTH_SPRUCE_TAIGA = 160;
    int BIOME_WINDSWEPT_SAVANNA = 163;

    int BIOME_PALE_GARDEN = 245;
    int BIOME_CHERRY_GROVE = 246;
    int BIOME_MANGROVE_SWAMP = 247;
    int BIOME_DEEP_DARK = 248;
    int BIOME_FROZEN_PEAKS = 249;
    int BIOME_GROVE = 250;
    int BIOME_JAGGED_PEAKS = 251;
    int BIOME_MEADOW = 252;
    int BIOME_SNOWY_SLOPES = 253;
    int BIOME_STONY_PEAKS = 254;

    int HIGHEST_BIOME_ID = BIOME_STONY_PEAKS;

    /**
     * Display names of the biomes,
     */
    String[] BIOME_NAMES = {
            "Ocean",
            "Plains",
            "Desert",
            "Windswept Hills",
            "Forest",
            "Taiga",
            "Swamp",
            "River",
            "Nether Wastes",
            "The End",

            "Frozen Ocean",
            "Frozen River",
            "Snowy Plains",
            "Snowy Mountains",
            "Mushroom Fields",
            "Mushroom Field Shore",
            "Beach",
            "Desert Hills",
            "Wooded Hills",
            "Taiga Hills",

            "Mountain Edge",
            "Jungle",
            "Jungle Hills",
            "Sparse Jungle",
            "Deep Ocean",
            "Stone Shore",
            "Snowy Beach",
            "Birch Forest",
            "Birch Forest Hills",
            "Dark Forest",

            "Snowy Taiga",
            "Snowy Taiga Hills",
            "Old Growth Pine Taiga",
            "Giant Tree Taiga Hills",
            "Windswept Forest",
            "Savanna",
            "Savanna Plateau",
            "Badlands",
            "Wooded Badlands",
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
            "Windswept Gravelly Hills",
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
            "Old Growth Birch Forest",
            "Tall Birch Hills",
            "Dark Forest Hills",
            "Snowy Taiga Mountains",
            null,

            "Old Growth Spruce Taiga",
            "Giant Spruce Taiga Hills",
            "Modified Gravelly Mountains",
            "Windswept Savanna",
            "Shattered Savanna Plateau",
            "Eroded Badlands",
            "Modified Wooded Badlands Plateau",
            "Modified Badlands Plateau",
            "Bamboo Jungle",
            "Bamboo Jungle Hills",

            "Soul Sand Valley",
            "Crimson Forest",
            "Warped Forest",
            "Basalt Deltas",
            "Dripstone Caves",
            "Lush Caves",
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
            "Pale Garden",
            "Cherry Grove",
            "Mangrove Swamp",
            "Deep Dark",
            "Frozen Peaks",

            "Grove",
            "Jagged Peaks",
            "Meadow",
            "Snowy Slopes",
            "Stony Peaks",
            // Add 1.18+ biomes that have no numerical ID from the end, replacing nulls above here. This is to minimise
            // the chance of collisions with IDs used as custom biomes for Minecraft 1.17- maps.
            null // Automatic biome/default value of Biome layer; don't map to biome to prevent confusion
    };

    /**
     * Minecraft 1.18+ technical names of the biomes as stored on disk, indexed by the legacy numerical ID with which
     * they correspond, if any. <strong>Note</strong> that because biomes have been consolidated, some modern IDs can
     * map to multiple indices!
     */
    String[] MODERN_IDS = {
            "minecraft:ocean",
            MC_PLAINS,
            "minecraft:desert",
            "minecraft:windswept_hills",
            "minecraft:forest",
            "minecraft:taiga",
            "minecraft:swamp",
            "minecraft:river",
            "minecraft:nether_wastes",
            "minecraft:the_end",

            "minecraft:frozen_ocean",
            "minecraft:frozen_river",
            "minecraft:snowy_plains",
            "minecraft:snowy_plains",
            "minecraft:mushroom_fields",
            "minecraft:mushroom_fields",
            "minecraft:beach",
            "minecraft:desert",
            "minecraft:forest",
            "minecraft:taiga",

            "minecraft:windswept_hills",
            "minecraft:jungle",
            "minecraft:jungle",
            "minecraft:sparse_jungle",
            "minecraft:deep_ocean",
            "minecraft:stony_shore",
            "minecraft:snowy_beach",
            "minecraft:birch_forest",
            "minecraft:birch_forest",
            "minecraft:dark_forest",

            "minecraft:snowy_taiga",
            "minecraft:snowy_taiga",
            "minecraft:old_growth_pine_taiga",
            "minecraft:old_growth_pine_taiga",
            "minecraft:windswept_forest",
            "minecraft:savanna",
            "minecraft:savanna_plateau",
            "minecraft:badlands",
            "minecraft:wooded_badlands",
            "minecraft:badlands",

            "minecraft:small_end_islands",
            "minecraft:end_midlands",
            "minecraft:end_highlands",
            "minecraft:end_barrens",
            "minecraft:warm_ocean",
            "minecraft:lukewarm_ocean",
            "minecraft:cold_ocean",
            "minecraft:warm_ocean",
            "minecraft:deep_lukewarm_ocean",
            "minecraft:deep_cold_ocean",

            "minecraft:deep_frozen_ocean",
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
            "minecraft:the_void",
            null,
            "minecraft:sunflower_plains",

            "minecraft:desert",
            "minecraft:windswept_gravelly_hills",
            "minecraft:flower_forest",
            "minecraft:taiga",
            "minecraft:swamp",
            null,
            null,
            null,
            null,
            null,

            "minecraft:ice_spikes",
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            "minecraft:jungle",

            null,
            "minecraft:sparse_jungle",
            null,
            null,
            null,
            "minecraft:old_growth_birch_forest",
            "minecraft:old_growth_birch_forest",
            "minecraft:dark_forest",
            "minecraft:snowy_taiga",
            null,

            "minecraft:old_growth_spruce_taiga",
            "minecraft:old_growth_spruce_taiga",
            "minecraft:windswept_gravelly_hills",
            "minecraft:windswept_savanna",
            "minecraft:windswept_savanna",
            "minecraft:eroded_badlands",
            "minecraft:wooded_badlands",
            "minecraft:badlands",
            "minecraft:bamboo_jungle",
            "minecraft:bamboo_jungle",

            "minecraft:soul_sand_valley",
            "minecraft:crimson_forest",
            "minecraft:warped_forest",
            "minecraft:basalt_deltas",
            "minecraft:dripstone_caves",
            "minecraft:lush_caves",
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
            "minecraft:pale_garden",
            "minecraft:cherry_grove",
            "minecraft:mangrove_swamp",
            "minecraft:deep_dark",
            "minecraft:frozen_peaks",

            "minecraft:grove",
            "minecraft:jagged_peaks",
            "minecraft:meadow",
            "minecraft:snowy_slopes",
            "minecraft:stony_peaks",
            // Add 1.18+ biomes that have no numerical ID from the end, replacing nulls above here. This is to minimise
            // the chance of collisions with IDs used as custom biomes for Minecraft 1.17- maps.
            null
    };

    Map<String, Integer> BIOMES_BY_MODERN_ID = StaticInitialiser.biomesByModernId();

    boolean[][][] BIOME_PATTERNS = StaticInitialiser.loadPatterns();

    class StaticInitialiser {
        @SuppressWarnings("ConstantConditions") // Our responsibility
        private static boolean[][][] loadPatterns() {
            boolean[][][] patterns = new boolean[256][][];
            try {
                BufferedImage image = ImageIO.read(ClassLoader.getSystemResourceAsStream("org/pepsoft/worldpainter/icons/swamp_pattern.png"));
                patterns[BIOME_SWAMPLAND] = createPattern(image);
                patterns[BIOME_SWAMPLAND_M] = createPattern(image);

                image = ImageIO.read(ClassLoader.getSystemResourceAsStream("org/pepsoft/worldpainter/icons/mountains_pattern.png"));
                patterns[BIOME_EXTREME_HILLS] = createPattern(image);
                patterns[BIOME_EXTREME_HILLS_M] = createPattern(image);
                patterns[BIOME_EXTREME_HILLS_PLUS] = createPattern(image);
                patterns[BIOME_EXTREME_HILLS_PLUS_M] = createPattern(image);
                patterns[BIOME_ICE_MOUNTAINS] = createPattern(image);
                patterns[BIOME_END_HIGHLANDS] = createPattern(image);
                patterns[BIOME_FROZEN_PEAKS] = createPattern(image);
                patterns[BIOME_JAGGED_PEAKS] = createPattern(image);
                patterns[BIOME_STONY_PEAKS] = createPattern(image);

                image = ImageIO.read(ClassLoader.getSystemResourceAsStream("org/pepsoft/worldpainter/icons/deciduous_trees_pattern.png"));
                patterns[BIOME_FOREST] = createPattern(image);
                patterns[BIOME_FLOWER_FOREST] = createPattern(image);
                patterns[BIOME_CRIMSON_FOREST] = createPattern(image);
                patterns[BIOME_WARPED_FOREST] = createPattern(image);
                patterns[BIOME_PALE_GARDEN] = createPattern(image);

                image = ImageIO.read(ClassLoader.getSystemResourceAsStream("org/pepsoft/worldpainter/icons/roofed_trees_pattern.png"));
                patterns[BIOME_ROOFED_FOREST] = createPattern(image);

                image = ImageIO.read(ClassLoader.getSystemResourceAsStream("org/pepsoft/worldpainter/icons/birch_trees_pattern.png"));
                patterns[BIOME_BIRCH_FOREST] = createPattern(image);

                image = ImageIO.read(ClassLoader.getSystemResourceAsStream("org/pepsoft/worldpainter/icons/spruce_trees_pattern.png"));
                patterns[BIOME_TAIGA] = createPattern(image);
                patterns[BIOME_COLD_TAIGA] = createPattern(image);

                image = ImageIO.read(ClassLoader.getSystemResourceAsStream("org/pepsoft/worldpainter/icons/pine_trees_pattern.png"));
                patterns[BIOME_MEGA_TAIGA] = createPattern(image);
                patterns[BIOME_MEGA_SPRUCE_TAIGA] = createPattern(image);

                image = ImageIO.read(ClassLoader.getSystemResourceAsStream("org/pepsoft/worldpainter/icons/hills_pattern.png"));
                patterns[BIOME_DESERT_HILLS] = createPattern(image);
                patterns[BIOME_EXTREME_HILLS_EDGE] = createPattern(image);
                patterns[BIOME_END_MIDLANDS] = createPattern(image);
                patterns[BIOME_SNOWY_SLOPES] = createPattern(image);

                image = ImageIO.read(ClassLoader.getSystemResourceAsStream("org/pepsoft/worldpainter/icons/deciduous_hills_pattern.png"));
                patterns[BIOME_FOREST_HILLS] = createPattern(image);

                image = ImageIO.read(ClassLoader.getSystemResourceAsStream("org/pepsoft/worldpainter/icons/roofed_hills_pattern.png"));
                patterns[BIOME_ROOFED_FOREST_M] = createPattern(image);

                image = ImageIO.read(ClassLoader.getSystemResourceAsStream("org/pepsoft/worldpainter/icons/birch_hills_pattern.png"));
                patterns[BIOME_BIRCH_FOREST_M] = createPattern(image);
                patterns[BIOME_BIRCH_FOREST_HILLS] = createPattern(image);
                patterns[BIOME_BIRCH_FOREST_HILLS_M] = createPattern(image);

                image = ImageIO.read(ClassLoader.getSystemResourceAsStream("org/pepsoft/worldpainter/icons/spruce_hills_pattern.png"));
                patterns[BIOME_TAIGA_M] = createPattern(image);
                patterns[BIOME_TAIGA_HILLS] = createPattern(image);
                patterns[BIOME_COLD_TAIGA_HILLS] = createPattern(image);
                patterns[BIOME_COLD_TAIGA_M] = createPattern(image);
                patterns[BIOME_GROVE] = createPattern(image);

                image = ImageIO.read(ClassLoader.getSystemResourceAsStream("org/pepsoft/worldpainter/icons/pine_hills_pattern.png"));
                patterns[BIOME_MEGA_TAIGA_HILLS] = createPattern(image);
                patterns[BIOME_MEGA_SPRUCE_TAIGA_HILLS] = createPattern(image);

                image = ImageIO.read(ClassLoader.getSystemResourceAsStream("org/pepsoft/worldpainter/icons/jungle_trees_pattern.png"));
                patterns[BIOME_JUNGLE] = createPattern(image);

                image = ImageIO.read(ClassLoader.getSystemResourceAsStream("org/pepsoft/worldpainter/icons/bamboo_pattern.png"));
                patterns[BIOME_BAMBOO_JUNGLE] = createPattern(image);

                image = ImageIO.read(ClassLoader.getSystemResourceAsStream("org/pepsoft/worldpainter/icons/jungle_hills_pattern.png"));
                patterns[BIOME_JUNGLE_HILLS] = createPattern(image);
                patterns[BIOME_JUNGLE_M] = createPattern(image);

                image = ImageIO.read(ClassLoader.getSystemResourceAsStream("org/pepsoft/worldpainter/icons/bamboo_hills_pattern.png"));
                patterns[BIOME_BAMBOO_JUNGLE_HILLS] = createPattern(image);

                image = ImageIO.read(ClassLoader.getSystemResourceAsStream("org/pepsoft/worldpainter/icons/savanna_pattern.png"));
                patterns[BIOME_SAVANNA] = createPattern(image);
                patterns[BIOME_SAVANNA_PLATEAU] = createPattern(image);

                image = ImageIO.read(ClassLoader.getSystemResourceAsStream("org/pepsoft/worldpainter/icons/savanna_hills_pattern.png"));
                patterns[BIOME_SAVANNA_M] = createPattern(image);
                patterns[BIOME_SAVANNA_PLATEAU_M] = createPattern(image);

                image = ImageIO.read(ClassLoader.getSystemResourceAsStream("org/pepsoft/worldpainter/icons/mesa_pattern.png"));
                patterns[BIOME_MESA] = createPattern(image);
                patterns[BIOME_MESA_PLATEAU] = createPattern(image);
                patterns[BIOME_MESA_PLATEAU_F] = createPattern(image);
                patterns[BIOME_MESA_PLATEAU_F_M] = createPattern(image);
                patterns[BIOME_MESA_PLATEAU_M] = createPattern(image);

                image = ImageIO.read(ClassLoader.getSystemResourceAsStream("org/pepsoft/worldpainter/icons/mesa_bryce_pattern.png"));
                patterns[BIOME_MESA_BRYCE] = createPattern(image);

                image = ImageIO.read(ClassLoader.getSystemResourceAsStream("org/pepsoft/worldpainter/icons/jungle_edge_pattern.png"));
                patterns[BIOME_JUNGLE_EDGE] = createPattern(image);

                image = ImageIO.read(ClassLoader.getSystemResourceAsStream("org/pepsoft/worldpainter/icons/jungle_edge_hills_pattern.png"));
                patterns[BIOME_JUNGLE_EDGE_M] = createPattern(image);

                image = ImageIO.read(ClassLoader.getSystemResourceAsStream("org/pepsoft/worldpainter/icons/ice_spikes.png"));
                patterns[BIOME_ICE_SPIKES] = createPattern(image);

                image = ImageIO.read(ClassLoader.getSystemResourceAsStream("org/pepsoft/worldpainter/icons/mangrove_trees_pattern.png"));
                patterns[BIOME_MANGROVE_SWAMP] = createPattern(image);

                image = ImageIO.read(ClassLoader.getSystemResourceAsStream("org/pepsoft/worldpainter/icons/cherry_grove_pattern.png"));
                patterns[BIOME_CHERRY_GROVE] = createPattern(image);

                image = ImageIO.read(ClassLoader.getSystemResourceAsStream("org/pepsoft/worldpainter/icons/basalt_deltas_pattern.png"));
                patterns[BIOME_BASALT_DELTAS] = createPattern(image);
            } catch (IOException e) {
                throw new RuntimeException("I/O error loading image", e);
            }
            return patterns;
        }

        private static boolean[][] createPattern(BufferedImage image) {
            boolean[][] pattern = new boolean[16][];
            for (int x = 0; x < 16; x++) {
                pattern[x] = new boolean[16];
                for (int y = 0; y < 16; y++) {
                    pattern[x][y] = image.getRGB(x, y) != -1;
                }
            }
            return pattern;
        }

        private static Map<String, Integer> biomesByModernId() {
            Map<String, Integer> biomesByModernId = new HashMap<>();
            for (int i = 0; i < 256; i++) {
                if ((MODERN_IDS[i] != null) && (! biomesByModernId.containsKey(MODERN_IDS[i]))) {
                    biomesByModernId.put(MODERN_IDS[i], i);
                }
            }
            return unmodifiableMap(biomesByModernId);
        }
    }
}