package org.pepsoft.worldpainter.biomeschemes;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;

/**
 * All Minecraft 1.17 biome IDs, plus names and other info.
 */
public interface Minecraft1_17Biomes extends Minecraft1_7Biomes {
    int BIOME_MOUNTAINS = 3;
    int BIOME_SWAMP = 6;
    int BIOME_NETHER_WASTES = 8;
    int BIOME_THE_END = 9;

    int BIOME_SNOWY_TUNDRA = 12;
    int BIOME_SNOWY_MOUNTAINS = 13;
    int BIOME_MUSHROOM_FIELDS = 14;
    int BIOME_MUSHROOM_FIELD_SHORE = 15;
    int BIOME_WOODED_HILLS = 18;

    int BIOME_MOUNTAIN_EDGE = 20;
    int BIOME_STONE_SHORE = 25;
    int BIOME_SNOWY_BEACH = 26;
    int BIOME_DARK_FOREST = 29;

    int BIOME_SNOWY_TAIGA = 30;
    int BIOME_SNOWY_TAIGA_HILLS = 31;
    int BIOME_GIANT_TREE_TAIGA = 32;
    int BIOME_GIANT_TREE_TAIGA_HILLS = 33;
    int BIOME_WOODED_MOUNTAINS = 34;
    int BIOME_BADLANDS = 37;
    int BIOME_WOODED_BADLANDS_PLATEAU = 38;
    int BIOME_BADLANDS_PLATEAU = 39;

    int BIOME_SMALL_END_ISLANDS = 40;
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

    int BIOME_THE_VOID = 127;

    int BIOME_DESERT_LAKES = 130;
    int BIOME_GRAVELLY_MOUNTAINS = 131;
    int BIOME_TAIGA_MOUNTAINS = 133;
    int BIOME_SWAMP_HILLS = 134;

    int BIOME_ICE_SPIKES = 140;
    int BIOME_MODIFIED_JUNGLE = 149;

    int BIOME_MODIFIED_JUNGLE_EDGE = 151;
    int BIOME_TALL_BIRCH_FOREST = 155;
    int BIOME_TALL_BIRCH_HILLS = 156;
    int BIOME_DARK_FOREST_HILLS = 157;
    int BIOME_SNOWY_TAIGA_MOUNTAINS = 158;

    int BIOME_GIANT_SPRUCE_TAIGA = 160;
    int BIOME_GIANT_SPRUCE_TAIGA_HILLS = 161;
    int BIOME_MODIFIED_GRAVELLY_MOUNTAINS = 162;
    int BIOME_SHATTERED_SAVANNA = 163;
    int BIOME_SHATTERED_SAVANNA_PLATEAU = 164;
    int BIOME_ERODED_BADLANDS = 165;
    int BIOME_MODIFIED_WOODED_BADLANDS_PLATEAU = 166;
    int BIOME_MODIFIED_BADLANDS_PLATEAU = 167;
    int BIOME_BAMBOO_JUNGLE = 168;
    int BIOME_BAMBOO_JUNGLE_HILLS = 169;

    int BIOME_SOUL_SAND_VALLEY = 170;
    int BIOME_CRIMSON_FOREST = 171;
    int BIOME_WARPED_FOREST = 172;
    int BIOME_BASALT_DELTAS = 173;
    int BIOME_DRIPSTONE_CAVES = 174;
    int BIOME_LUSH_CAVES = 175;

    int FIRST_UNALLOCATED_ID = BIOME_DEEP_FROZEN_OCEAN + 1;
    int HIGHEST_BIOME_ID = BIOME_LUSH_CAVES;

    String[] BIOME_NAMES = {
            "Ocean",
            "Plains",
            "Desert",
            "Mountains",
            "Forest",
            "Taiga",
            "Swamp",
            "River",
            "Nether Wastes",
            "The End",

            "Frozen Ocean",
            "Frozen River",
            "Snowy Tundra",
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
            "Modified Gravelly Mountains",
            "Shattered Savanna",
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
            "Lush Caves"
    };

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
                patterns[BIOME_CRIMSON_FOREST] = createPattern(image);
                patterns[BIOME_WARPED_FOREST] = createPattern(image);

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
                patterns[BIOME_BAMBOO_JUNGLE] = createPattern(image); // TODOMC118 create specific bamboo icon

                image = ImageIO.read(ClassLoader.getSystemResourceAsStream("org/pepsoft/worldpainter/icons/jungle_hills_pattern.png"));
                patterns[BIOME_JUNGLE_HILLS] = createPattern(image);
                patterns[BIOME_JUNGLE_M] = createPattern(image);
                patterns[BIOME_BAMBOO_JUNGLE_HILLS] = createPattern(image); // TODOMC118 create specific bamboo icon

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