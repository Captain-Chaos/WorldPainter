package org.pepsoft.worldpainter.biomeschemes;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * All Minecraft 1.7 and 1.8 biome IDs, plus names and other info.
 *
 * Created by pepijn on 27-4-15.
 */
public interface Minecraft1_7Biomes {
    int BIOME_OCEAN = 0;
    int BIOME_PLAINS = 1;
    int BIOME_DESERT = 2;
    int BIOME_EXTREME_HILLS = 3;
    int BIOME_FOREST = 4;
    int BIOME_TAIGA = 5;
    int BIOME_SWAMPLAND = 6;
    int BIOME_RIVER = 7;
    int BIOME_HELL = 8;
    int BIOME_SKY = 9;

    int BIOME_FROZEN_OCEAN = 10;
    int BIOME_FROZEN_RIVER = 11;
    int BIOME_ICE_PLAINS = 12;
    int BIOME_ICE_MOUNTAINS = 13;
    int BIOME_MUSHROOM_ISLAND = 14;
    int BIOME_MUSHROOM_ISLAND_SHORE = 15;
    int BIOME_BEACH = 16;
    int BIOME_DESERT_HILLS = 17;
    int BIOME_FOREST_HILLS = 18;
    int BIOME_TAIGA_HILLS = 19;

    int BIOME_EXTREME_HILLS_EDGE = 20;
    int BIOME_JUNGLE = 21;
    int BIOME_JUNGLE_HILLS = 22;
    int BIOME_JUNGLE_EDGE = 23;
    int BIOME_DEEP_OCEAN = 24;
    int BIOME_STONE_BEACH = 25;
    int BIOME_COLD_BEACH = 26;
    int BIOME_BIRCH_FOREST = 27;
    int BIOME_BIRCH_FOREST_HILLS = 28;
    int BIOME_ROOFED_FOREST = 29;

    int BIOME_COLD_TAIGA = 30;
    int BIOME_COLD_TAIGA_HILLS = 31;
    int BIOME_MEGA_TAIGA = 32;
    int BIOME_MEGA_TAIGA_HILLS = 33;
    int BIOME_EXTREME_HILLS_PLUS = 34;
    int BIOME_SAVANNA = 35;
    int BIOME_SAVANNA_PLATEAU = 36;
    int BIOME_MESA = 37;
    int BIOME_MESA_PLATEAU_F = 38;
    int BIOME_MESA_PLATEAU = 39;

    int BIOME_SUNFLOWER_PLAINS = 129;
    int BIOME_DESERT_M = 130;
    int BIOME_EXTREME_HILLS_M = 131;
    int BIOME_FLOWER_FOREST = 132;
    int BIOME_TAIGA_M = 133;
    int BIOME_SWAMPLAND_M = 134;

    int BIOME_ICE_PLAINS_SPIKES = 140;
    int BIOME_ICE_MOUNTAINS_SPIKES = 141;
    int BIOME_JUNGLE_M = 149;

    int BIOME_JUNGLE_EDGE_M = 151;
    int BIOME_BIRCH_FOREST_M = 155;
    int BIOME_BIRCH_FOREST_HILLS_M = 156;
    int BIOME_ROOFED_FOREST_M = 157;
    int BIOME_COLD_TAIGA_M = 158;

    int BIOME_MEGA_SPRUCE_TAIGA = 160;
    int BIOME_MEGA_SPRUCE_TAIGA_HILLS = 161;
    int BIOME_EXTREME_HILLS_PLUS_M = 162;
    int BIOME_SAVANNA_M = 163;
    int BIOME_SAVANNA_PLATEAU_M = 164;
    int BIOME_MESA_BRYCE = 165;
    int BIOME_MESA_PLATEAU_F_M = 166;
    int BIOME_MESA_PLATEAU_M = 167;

    int FIRST_UNALLOCATED_ID = BIOME_MESA_PLATEAU + 1;
    int HIGHEST_BIOME_ID = BIOME_MESA_PLATEAU_M;

    String[] BIOME_NAMES = {
            "Ocean",
            "Plains",
            "Desert",
            "Extreme Hills",
            "Forest",
            "Taiga",
            "Swampland",
            "River",
            "Nether",
            "End",

            "Frozen Ocean",
            "Frozen River",
            "Ice Plains",
            "Ice Mountains",
            "Mushroom Island",
            "Mushroom Island Shore",
            "Beach",
            "Desert Hills",
            "Forest Hills",
            "Taiga Hills",

            "Extreme Hills Edge",
            "Jungle",
            "Jungle Hills",
            "Jungle Edge",
            "Deep Ocean",
            "Stone Beach",
            "Cold Beach",
            "Birch Forest",
            "Birch Forest Hills",
            "Roofed Forest",

            "Cold Taiga",
            "Cold Taiga Hills",
            "Mega Taiga",
            "Mega Taiga Hills",
            "Extreme Hills+",
            "Savanna",
            "Savanna Plateau",
            "Mesa",
            "Mesa Plateau F",
            "Mesa Plateau",

            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,

            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,

            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,

            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,

            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,

            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,

            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,

            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,

            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            "Sunflower Plains",

            "Desert M",
            "Extreme Hills M",
            "Flower Forest",
            "Taiga M",
            "Swampland M",
            null,
            null,
            null,
            null,
            null,

            "Ice Plains Spikes",
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            "Jungle M",

            null,
            "Jungle Edge M",
            null,
            null,
            null,
            "Birch Forest M",
            "Birch Forest Hills M",
            "Roofed Forest M",
            "Cold Taiga M",
            null,

            "Mega Spruce Taiga",
            "Mega Spruce Taiga Hills",
            "Extreme Hills+ M",
            "Savanna M",
            "Savanna Plateau M",
            "Mesa (Bryce)",
            "Mesa Plateau F M",
            "Mesa Plateau M"
        };

    Set<Integer> DRY_BIOMES = new HashSet<>(Arrays.asList(BIOME_DESERT, BIOME_DESERT_HILLS, BIOME_DESERT_M, BIOME_HELL,
            BIOME_SAVANNA, BIOME_SAVANNA_M, BIOME_MESA, BIOME_MESA_BRYCE, BIOME_SAVANNA_PLATEAU,
            BIOME_SAVANNA_PLATEAU_M, BIOME_MESA_PLATEAU, BIOME_MESA_PLATEAU_F, BIOME_MESA_PLATEAU_F_M,
            BIOME_MESA_PLATEAU_M));
    Set<Integer> COLD_BIOMES = new HashSet<>(Arrays.asList(BIOME_FROZEN_OCEAN, BIOME_FROZEN_RIVER, BIOME_ICE_MOUNTAINS,
            BIOME_ICE_PLAINS, BIOME_TAIGA_HILLS, BIOME_ICE_PLAINS_SPIKES, BIOME_COLD_BEACH, BIOME_COLD_TAIGA,
            BIOME_COLD_TAIGA_M));
    Set<Integer> FORESTED_BIOMES = new HashSet<>(Arrays.asList(BIOME_FOREST, BIOME_SWAMPLAND, BIOME_TAIGA,
            BIOME_FOREST_HILLS, BIOME_TAIGA_HILLS, BIOME_JUNGLE, BIOME_JUNGLE_HILLS, BIOME_JUNGLE_EDGE,
            BIOME_JUNGLE_EDGE_M, BIOME_JUNGLE_M, BIOME_BIRCH_FOREST, BIOME_BIRCH_FOREST_HILLS,
            BIOME_BIRCH_FOREST_HILLS_M, BIOME_BIRCH_FOREST_M, BIOME_TAIGA_M, BIOME_COLD_TAIGA, BIOME_COLD_TAIGA_HILLS,
            BIOME_COLD_TAIGA_M, BIOME_MEGA_SPRUCE_TAIGA, BIOME_MEGA_SPRUCE_TAIGA_HILLS, BIOME_MEGA_TAIGA,
            BIOME_MEGA_TAIGA_HILLS, BIOME_ROOFED_FOREST, BIOME_ROOFED_FOREST_M, BIOME_SAVANNA, BIOME_SAVANNA_M,
            BIOME_SAVANNA_PLATEAU, BIOME_SAVANNA_PLATEAU_M));
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

                image = ImageIO.read(ClassLoader.getSystemResourceAsStream("org/pepsoft/worldpainter/icons/jungle_hills_pattern.png"));
                patterns[BIOME_JUNGLE_HILLS] = createPattern(image);
                patterns[BIOME_JUNGLE_M] = createPattern(image);

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