package org.pepsoft.worldpainter.biomeschemes;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * All Minecraft 1.13 biome IDs, plus names and other info.
 */
public interface Minecraft1_14Biomes extends Minecraft1_7Biomes {
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

    int BIOME_BAMBOO_JUNGLE = 168;
    int BIOME_BAMBOO_JUNGLE_HILLS = 169;

    int FIRST_UNALLOCATED_ID = BIOME_DEEP_FROZEN_OCEAN + 1;
    int HIGHEST_BIOME_ID = BIOME_BAMBOO_JUNGLE_HILLS;

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
            "Modified Badlands Plateau",
            "Bamboo Jungle",
            "Bamboo Jungle Hills"
    };

    Set<Integer> DRY_BIOMES = new HashSet<>(Arrays.asList(BIOME_DESERT, BIOME_DESERT_HILLS, BIOME_DESERT_M, BIOME_HELL,
            BIOME_SAVANNA, BIOME_SAVANNA_M, BIOME_MESA, BIOME_MESA_BRYCE, BIOME_SAVANNA_PLATEAU,
            BIOME_SAVANNA_PLATEAU_M, BIOME_MESA_PLATEAU, BIOME_MESA_PLATEAU_F, BIOME_MESA_PLATEAU_F_M,
            BIOME_MESA_PLATEAU_M));
    Set<Integer> COLD_BIOMES = new HashSet<>(Arrays.asList(BIOME_FROZEN_OCEAN, BIOME_FROZEN_RIVER, BIOME_ICE_MOUNTAINS,
            BIOME_ICE_PLAINS, BIOME_TAIGA_HILLS, BIOME_ICE_PLAINS_SPIKES, BIOME_COLD_BEACH, BIOME_COLD_TAIGA,
            BIOME_COLD_TAIGA_M, BIOME_DEEP_FROZEN_OCEAN));
    Set<Integer> FORESTED_BIOMES = new HashSet<>(Arrays.asList(BIOME_FOREST, BIOME_SWAMPLAND, BIOME_TAIGA,
            BIOME_FOREST_HILLS, BIOME_TAIGA_HILLS, BIOME_JUNGLE, BIOME_JUNGLE_HILLS, BIOME_JUNGLE_EDGE,
            BIOME_JUNGLE_EDGE_M, BIOME_JUNGLE_M, BIOME_BIRCH_FOREST, BIOME_BIRCH_FOREST_HILLS,
            BIOME_BIRCH_FOREST_HILLS_M, BIOME_BIRCH_FOREST_M, BIOME_TAIGA_M, BIOME_COLD_TAIGA, BIOME_COLD_TAIGA_HILLS,
            BIOME_COLD_TAIGA_M, BIOME_MEGA_SPRUCE_TAIGA, BIOME_MEGA_SPRUCE_TAIGA_HILLS, BIOME_MEGA_TAIGA,
            BIOME_MEGA_TAIGA_HILLS, BIOME_ROOFED_FOREST, BIOME_ROOFED_FOREST_M, BIOME_SAVANNA, BIOME_SAVANNA_M,
            BIOME_SAVANNA_PLATEAU, BIOME_SAVANNA_PLATEAU_M, BIOME_BAMBOO_JUNGLE, BIOME_BAMBOO_JUNGLE_HILLS));
    Set<Integer> SWAMPY_BIOMES = new HashSet<>(Arrays.asList(BIOME_SWAMPLAND, BIOME_SWAMPLAND_M));

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

                image = ImageIO.read(ClassLoader.getSystemResourceAsStream("org/pepsoft/worldpainter/icons/deciduous_trees_pattern.png"));
                patterns[BIOME_FOREST] = createPattern(image);
                patterns[BIOME_FLOWER_FOREST] = createPattern(image);

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

                image = ImageIO.read(ClassLoader.getSystemResourceAsStream("org/pepsoft/worldpainter/icons/pine_hills_pattern.png"));
                patterns[BIOME_MEGA_TAIGA_HILLS] = createPattern(image);
                patterns[BIOME_MEGA_SPRUCE_TAIGA_HILLS] = createPattern(image);

                image = ImageIO.read(ClassLoader.getSystemResourceAsStream("org/pepsoft/worldpainter/icons/jungle_trees_pattern.png"));
                patterns[BIOME_JUNGLE] = createPattern(image);
                patterns[BIOME_BAMBOO_JUNGLE] = createPattern(image);

                image = ImageIO.read(ClassLoader.getSystemResourceAsStream("org/pepsoft/worldpainter/icons/jungle_hills_pattern.png"));
                patterns[BIOME_JUNGLE_HILLS] = createPattern(image);
                patterns[BIOME_JUNGLE_M] = createPattern(image);
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
    }
}