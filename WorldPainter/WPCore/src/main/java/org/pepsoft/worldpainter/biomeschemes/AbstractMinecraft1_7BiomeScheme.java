/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.biomeschemes;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.imageio.ImageIO;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import static org.pepsoft.minecraft.Constants.*;
import org.pepsoft.worldpainter.ColourScheme;

/**
 *
 * @author pepijn
 */
public abstract class AbstractMinecraft1_7BiomeScheme extends AbstractBiomeScheme {
    @Override
    public final String[] getBiomeNames() {
        return BIOME_NAMES;
    }

    @Override
    public final Set<Integer> getDryBiomes() {
        return DRY_BIOMES;
    }

    @Override
    public final Set<Integer> getColdBiomes() {
        return COLD_BIOMES;
    }

    @Override
    public final Set<Integer> getForestedBiomes() {
        return FORESTED_BIOMES;
    }

    @Override
    public final Set<Integer> getSwampyBiomes() {
        return SWAMPY_BIOMES;
    }

    @Override
    public final int getBiomeCount() {
        return BIOME_NAMES.length;
    }

    @Override
    public final int getColour(int biome, ColourScheme colourScheme) {
        switch (biome) {
            case BIOME_OCEAN:
            case BIOME_RIVER:
            case BIOME_DEEP_OCEAN:
                return colourScheme.getColour(BLK_WATER);
            case BIOME_PLAINS:
            case BIOME_EXTREME_HILLS:
            case BIOME_EXTREME_HILLS_EDGE:
            case BIOME_EXTREME_HILLS_M:
            case BIOME_EXTREME_HILLS_PLUS:
            case BIOME_EXTREME_HILLS_PLUS_M:
            case BIOME_SUNFLOWER_PLAINS:
                return colourScheme.getColour(BLK_GRASS);
            case BIOME_DESERT:
            case BIOME_BEACH:
            case BIOME_DESERT_HILLS:
            case BIOME_DESERT_M:
                return colourScheme.getColour(BLK_SAND);
            case BIOME_FOREST:
            case BIOME_FOREST_HILLS:
            case BIOME_SWAMPLAND:
            case BIOME_ROOFED_FOREST:
            case BIOME_ROOFED_FOREST_M:
            case BIOME_FLOWER_FOREST:
            case BIOME_SWAMPLAND_M:
                return colourScheme.getColour(BLK_LEAVES, DATA_OAK);
            case BIOME_JUNGLE:
            case BIOME_JUNGLE_HILLS:
            case BIOME_JUNGLE_EDGE:
            case BIOME_JUNGLE_M:
            case BIOME_JUNGLE_EDGE_M:
                return colourScheme.getColour(BLK_LEAVES, DATA_JUNGLE);
            case BIOME_ICE_PLAINS:
            case BIOME_ICE_MOUNTAINS:
            case BIOME_COLD_TAIGA:
            case BIOME_COLD_TAIGA_HILLS:
            case BIOME_COLD_TAIGA_M:
            case BIOME_ICE_PLAINS_SPIKES:
            case BIOME_COLD_BEACH:
                return colourScheme.getColour(BLK_SNOW);
            case BIOME_HELL:
                return colourScheme.getColour(BLK_NETHERRACK);
            case BIOME_SKY:
                return colourScheme.getColour(BLK_AIR);
            case BIOME_FROZEN_OCEAN:
            case BIOME_FROZEN_RIVER:
                return colourScheme.getColour(BLK_ICE);
            case BIOME_MUSHROOM_ISLAND:
            case BIOME_MUSHROOM_ISLAND_SHORE:
                return colourScheme.getColour(BLK_MYCELIUM);
            case BIOME_STONE_BEACH:
                return colourScheme.getColour(BLK_STONE);
            case BIOME_BIRCH_FOREST:
            case BIOME_BIRCH_FOREST_HILLS:
            case BIOME_BIRCH_FOREST_HILLS_M:
            case BIOME_BIRCH_FOREST_M:
                return colourScheme.getColour(BLK_LEAVES, DATA_BIRCH);
            case BIOME_TAIGA:
            case BIOME_TAIGA_HILLS:
            case BIOME_MEGA_TAIGA:
            case BIOME_MEGA_TAIGA_HILLS:
            case BIOME_MEGA_SPRUCE_TAIGA:
            case BIOME_MEGA_SPRUCE_TAIGA_HILLS:
            case BIOME_TAIGA_M:
                return colourScheme.getColour(BLK_LEAVES, DATA_PINE);
            case BIOME_SAVANNA:
            case BIOME_SAVANNA_M:
            case BIOME_SAVANNA_PLATEAU:
            case BIOME_SAVANNA_PLATEAU_M:
                return colourScheme.getColour(BLK_LEAVES2, DATA_ACACIA);
            case BIOME_MESA:
            case BIOME_MESA_BRYCE:
            case BIOME_MESA_PLATEAU:
            case BIOME_MESA_PLATEAU_F:
            case BIOME_MESA_PLATEAU_F_M:
            case BIOME_MESA_PLATEAU_M:
                return colourScheme.getColour(BLK_HARDENED_CLAY);
            default:
                throw new IllegalArgumentException(Integer.toString(biome));
        }
    }

    @Override
    public final boolean[][] getPattern(int biome) {
        return BIOME_PATTERNS[biome];
    }
    
    @Override
    public boolean isBiomePresent(int biome) {
        return (biome <= HIGHEST_BIOME_ID) && (BIOME_NAMES[biome] != null);
    }

    @Override
    public String getBiomeName(int biome) {
        return BIOME_NAMES[biome];
    }
    
    /**
     * Create a class loader for the specified Minecraft jar, including the jars
     * specified in its associated json descriptor file.
     * 
     * @param minecraftJar The Minecraft jar for which to create a class loader.
     * @param libDir The directory from which to load the dependencies.
     * @return A class loader containing all (headless) dependencies for the
     *     specified Minecraft jar.
     */
    protected final ClassLoader getClassLoader(File minecraftJar, File libDir) {
        List<URL> classpath = new ArrayList<URL>(25);

        try {
            // Construct a classpath, starting with the Minecraft jar itself
            classpath.add(minecraftJar.toURI().toURL());

            // Find and parse the json descriptor, adding the libraries to the
            // claspath
            File minecraftJarDir = minecraftJar.getParentFile();
            String minecraftJarName = minecraftJar.getName();
            String jsonFileName = minecraftJarName.substring(0, minecraftJarName.length() - 4) + ".json";
            File jsonFile = new File(minecraftJarDir, jsonFileName);
            JSONParser jsonParser = new JSONParser();
            FileReader in = new FileReader(jsonFile);
            try {
                Map<?, ?> rootNode = (Map<?, ?>) jsonParser.parse(in);
                List<Map<?, ?>> librariesNode = (List<Map<?, ?>>) rootNode.get("libraries");
                for (Map<?, ?> libraryNode: librariesNode) {
                    if (libraryNode.containsKey("rules")) {
                        // For now we just skip any library that has rules, on
                        // the assumption that it is a platform dependent
                        // library that's only needed by the actual game client
                        // and not by the Minecraft core
                        continue;
                    }
                    String libraryDescriptor = (String) libraryNode.get("name");
                    String[] parts = libraryDescriptor.split(":");
                    String libraryGroup = parts[0];
                    String libraryName = parts[1];
                    String libraryVersion = parts[2];
                    File libraryDir = new File(libDir, libraryGroup.replace('.', '/') + '/' + libraryName + '/' + libraryVersion);
                    File libraryFile = new File(libraryDir, libraryName + '-' + libraryVersion + ".jar");
                    classpath.add(libraryFile.toURI().toURL());
                }
            } finally {
                in.close();
            }
        } catch (IOException e) {
            throw new RuntimeException("I/O error while trying to load Minecraft jar descriptor json file", e);
        } catch (ParseException e) {
            throw new RuntimeException("Parsing error while trying to load Minecraft jar descriptor json file", e);
        }
        
        // Create the class loader and return it
        return new URLClassLoader(classpath.toArray(new URL[classpath.size()]));
    }
    
    public static final int BIOME_OCEAN = 0;
    public static final int BIOME_PLAINS = 1;
    public static final int BIOME_DESERT = 2;
    public static final int BIOME_EXTREME_HILLS = 3;
    public static final int BIOME_FOREST = 4;
    public static final int BIOME_TAIGA = 5;
    public static final int BIOME_SWAMPLAND = 6;
    public static final int BIOME_RIVER = 7;
    public static final int BIOME_HELL = 8;
    public static final int BIOME_SKY = 9;
    public static final int BIOME_FROZEN_OCEAN = 10;
    public static final int BIOME_FROZEN_RIVER = 11;
    public static final int BIOME_ICE_PLAINS = 12;
    public static final int BIOME_ICE_MOUNTAINS = 13;
    public static final int BIOME_MUSHROOM_ISLAND = 14;
    public static final int BIOME_MUSHROOM_ISLAND_SHORE = 15;
    public static final int BIOME_BEACH = 16;
    public static final int BIOME_DESERT_HILLS = 17;
    public static final int BIOME_FOREST_HILLS = 18;
    public static final int BIOME_TAIGA_HILLS = 19;
    public static final int BIOME_EXTREME_HILLS_EDGE = 20;
    public static final int BIOME_JUNGLE = 21;
    public static final int BIOME_JUNGLE_HILLS = 22;
    public static final int BIOME_JUNGLE_EDGE = 23;
    public static final int BIOME_DEEP_OCEAN = 24;
    public static final int BIOME_STONE_BEACH = 25;
    public static final int BIOME_COLD_BEACH = 26;
    public static final int BIOME_BIRCH_FOREST = 27;
    public static final int BIOME_BIRCH_FOREST_HILLS = 28;
    public static final int BIOME_ROOFED_FOREST = 29;
    public static final int BIOME_COLD_TAIGA = 30;
    public static final int BIOME_COLD_TAIGA_HILLS = 31;
    public static final int BIOME_MEGA_TAIGA = 32;
    public static final int BIOME_MEGA_TAIGA_HILLS = 33;
    public static final int BIOME_EXTREME_HILLS_PLUS = 34;
    public static final int BIOME_SAVANNA = 35;
    public static final int BIOME_SAVANNA_PLATEAU = 36;
    public static final int BIOME_MESA = 37;
    public static final int BIOME_MESA_PLATEAU_F = 38;
    public static final int BIOME_MESA_PLATEAU = 39;
    public static final int BIOME_SUNFLOWER_PLAINS = 129;
    public static final int BIOME_DESERT_M = 130;
    public static final int BIOME_EXTREME_HILLS_M = 131;
    public static final int BIOME_FLOWER_FOREST = 132;
    public static final int BIOME_TAIGA_M = 133;
    public static final int BIOME_SWAMPLAND_M = 134;
    public static final int BIOME_ICE_PLAINS_SPIKES = 140;
    public static final int BIOME_ICE_MOUNTAINS_SPIKES = 141;
    public static final int BIOME_JUNGLE_M = 149;
    public static final int BIOME_JUNGLE_EDGE_M = 151;
    public static final int BIOME_BIRCH_FOREST_M = 155;
    public static final int BIOME_BIRCH_FOREST_HILLS_M = 156;
    public static final int BIOME_ROOFED_FOREST_M = 157;
    public static final int BIOME_COLD_TAIGA_M = 158;
    public static final int BIOME_MEGA_SPRUCE_TAIGA = 160;
    public static final int BIOME_MEGA_SPRUCE_TAIGA_HILLS = 161;
    public static final int BIOME_EXTREME_HILLS_PLUS_M = 162;
    public static final int BIOME_SAVANNA_M = 163;
    public static final int BIOME_SAVANNA_PLATEAU_M = 164;
    public static final int BIOME_MESA_BRYCE = 165;
    public static final int BIOME_MESA_PLATEAU_F_M = 166;
    public static final int BIOME_MESA_PLATEAU_M = 167;

    public static final int HIGHEST_BIOME_ID = BIOME_MESA_PLATEAU_M;
    
    public static final String[] BIOME_NAMES = {
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
    public static final Set<Integer> DRY_BIOMES = new HashSet<Integer>(Arrays.asList(BIOME_DESERT, BIOME_DESERT_HILLS, BIOME_DESERT_M, BIOME_HELL, BIOME_SAVANNA, BIOME_SAVANNA_M, BIOME_MESA, BIOME_MESA_BRYCE, BIOME_SAVANNA_PLATEAU, BIOME_SAVANNA_PLATEAU_M, BIOME_MESA_PLATEAU, BIOME_MESA_PLATEAU_F, BIOME_MESA_PLATEAU_F_M, BIOME_MESA_PLATEAU_M));
    public static final Set<Integer> COLD_BIOMES = new HashSet<Integer>(Arrays.asList(BIOME_FROZEN_OCEAN, BIOME_FROZEN_RIVER, BIOME_ICE_MOUNTAINS, BIOME_ICE_PLAINS, BIOME_TAIGA_HILLS, BIOME_ICE_PLAINS_SPIKES, BIOME_COLD_BEACH, BIOME_COLD_TAIGA, BIOME_COLD_TAIGA_M));
    public static final Set<Integer> FORESTED_BIOMES = new HashSet<Integer>(Arrays.asList(BIOME_FOREST, BIOME_SWAMPLAND, BIOME_TAIGA, BIOME_FOREST_HILLS, BIOME_TAIGA_HILLS, BIOME_JUNGLE, BIOME_JUNGLE_HILLS, BIOME_JUNGLE_EDGE, BIOME_JUNGLE_EDGE_M, BIOME_JUNGLE_M, BIOME_BIRCH_FOREST, BIOME_BIRCH_FOREST_HILLS, BIOME_BIRCH_FOREST_HILLS_M, BIOME_BIRCH_FOREST_M, BIOME_TAIGA_M, BIOME_COLD_TAIGA, BIOME_COLD_TAIGA_HILLS, BIOME_COLD_TAIGA_M, BIOME_MEGA_SPRUCE_TAIGA, BIOME_MEGA_SPRUCE_TAIGA_HILLS, BIOME_MEGA_TAIGA, BIOME_MEGA_TAIGA_HILLS, BIOME_ROOFED_FOREST, BIOME_ROOFED_FOREST_M, BIOME_SAVANNA, BIOME_SAVANNA_M, BIOME_SAVANNA_PLATEAU, BIOME_SAVANNA_PLATEAU_M));
    public static final Set<Integer> SWAMPY_BIOMES = new HashSet<Integer>(Arrays.asList(BIOME_SWAMPLAND, BIOME_SWAMPLAND_M));
    
    private static final boolean[][][] BIOME_PATTERNS = new boolean[168][][];

    static {
        try {
            BufferedImage image = ImageIO.read(ClassLoader.getSystemResourceAsStream("org/pepsoft/worldpainter/icons/swamp_pattern.png"));
            BIOME_PATTERNS[BIOME_SWAMPLAND] = createPattern(image);
            BIOME_PATTERNS[BIOME_SWAMPLAND_M] = createPattern(image);

            image = ImageIO.read(ClassLoader.getSystemResourceAsStream("org/pepsoft/worldpainter/icons/mountains_pattern.png"));
            BIOME_PATTERNS[BIOME_EXTREME_HILLS] = createPattern(image);
            BIOME_PATTERNS[BIOME_EXTREME_HILLS_M] = createPattern(image);
            BIOME_PATTERNS[BIOME_EXTREME_HILLS_PLUS] = createPattern(image);
            BIOME_PATTERNS[BIOME_EXTREME_HILLS_PLUS_M] = createPattern(image);
            BIOME_PATTERNS[BIOME_ICE_MOUNTAINS] = createPattern(image);

            image = ImageIO.read(ClassLoader.getSystemResourceAsStream("org/pepsoft/worldpainter/icons/deciduous_trees_pattern.png"));
            BIOME_PATTERNS[BIOME_FOREST] = createPattern(image);
            BIOME_PATTERNS[BIOME_FLOWER_FOREST] = createPattern(image);

            image = ImageIO.read(ClassLoader.getSystemResourceAsStream("org/pepsoft/worldpainter/icons/roofed_trees_pattern.png"));
            BIOME_PATTERNS[BIOME_ROOFED_FOREST] = createPattern(image);
            
            image = ImageIO.read(ClassLoader.getSystemResourceAsStream("org/pepsoft/worldpainter/icons/birch_trees_pattern.png"));
            BIOME_PATTERNS[BIOME_BIRCH_FOREST] = createPattern(image);
            
            image = ImageIO.read(ClassLoader.getSystemResourceAsStream("org/pepsoft/worldpainter/icons/spruce_trees_pattern.png"));
            BIOME_PATTERNS[BIOME_TAIGA] = createPattern(image);
            BIOME_PATTERNS[BIOME_COLD_TAIGA] = createPattern(image);

            image = ImageIO.read(ClassLoader.getSystemResourceAsStream("org/pepsoft/worldpainter/icons/pine_trees_pattern.png"));
            BIOME_PATTERNS[BIOME_MEGA_TAIGA] = createPattern(image);
            BIOME_PATTERNS[BIOME_MEGA_SPRUCE_TAIGA] = createPattern(image);
            
            image = ImageIO.read(ClassLoader.getSystemResourceAsStream("org/pepsoft/worldpainter/icons/hills_pattern.png"));
            BIOME_PATTERNS[BIOME_DESERT_HILLS] = createPattern(image);
            BIOME_PATTERNS[BIOME_EXTREME_HILLS_EDGE] = createPattern(image);

            image = ImageIO.read(ClassLoader.getSystemResourceAsStream("org/pepsoft/worldpainter/icons/deciduous_hills_pattern.png"));
            BIOME_PATTERNS[BIOME_FOREST_HILLS] = createPattern(image);

            image = ImageIO.read(ClassLoader.getSystemResourceAsStream("org/pepsoft/worldpainter/icons/roofed_hills_pattern.png"));
            BIOME_PATTERNS[BIOME_ROOFED_FOREST_M] = createPattern(image);
            
            image = ImageIO.read(ClassLoader.getSystemResourceAsStream("org/pepsoft/worldpainter/icons/birch_hills_pattern.png"));
            BIOME_PATTERNS[BIOME_BIRCH_FOREST_M] = createPattern(image);
            BIOME_PATTERNS[BIOME_BIRCH_FOREST_HILLS] = createPattern(image);
            BIOME_PATTERNS[BIOME_BIRCH_FOREST_HILLS_M] = createPattern(image);
            
            image = ImageIO.read(ClassLoader.getSystemResourceAsStream("org/pepsoft/worldpainter/icons/spruce_hills_pattern.png"));
            BIOME_PATTERNS[BIOME_TAIGA_M] = createPattern(image);
            BIOME_PATTERNS[BIOME_TAIGA_HILLS] = createPattern(image);
            BIOME_PATTERNS[BIOME_COLD_TAIGA_HILLS] = createPattern(image);
            BIOME_PATTERNS[BIOME_COLD_TAIGA_M] = createPattern(image);

            image = ImageIO.read(ClassLoader.getSystemResourceAsStream("org/pepsoft/worldpainter/icons/pine_hills_pattern.png"));
            BIOME_PATTERNS[BIOME_MEGA_TAIGA_HILLS] = createPattern(image);
            BIOME_PATTERNS[BIOME_MEGA_SPRUCE_TAIGA_HILLS] = createPattern(image);
            
            image = ImageIO.read(ClassLoader.getSystemResourceAsStream("org/pepsoft/worldpainter/icons/jungle_trees_pattern.png"));
            BIOME_PATTERNS[BIOME_JUNGLE] = createPattern(image);

            image = ImageIO.read(ClassLoader.getSystemResourceAsStream("org/pepsoft/worldpainter/icons/jungle_hills_pattern.png"));
            BIOME_PATTERNS[BIOME_JUNGLE_HILLS] = createPattern(image);
            BIOME_PATTERNS[BIOME_JUNGLE_M] = createPattern(image);

            image = ImageIO.read(ClassLoader.getSystemResourceAsStream("org/pepsoft/worldpainter/icons/savanna_pattern.png"));
            BIOME_PATTERNS[BIOME_SAVANNA] = createPattern(image);
            BIOME_PATTERNS[BIOME_SAVANNA_PLATEAU] = createPattern(image);

            image = ImageIO.read(ClassLoader.getSystemResourceAsStream("org/pepsoft/worldpainter/icons/savanna_hills_pattern.png"));
            BIOME_PATTERNS[BIOME_SAVANNA_M] = createPattern(image);
            BIOME_PATTERNS[BIOME_SAVANNA_PLATEAU_M] = createPattern(image);
            
            image = ImageIO.read(ClassLoader.getSystemResourceAsStream("org/pepsoft/worldpainter/icons/mesa_pattern.png"));
            BIOME_PATTERNS[BIOME_MESA] = createPattern(image);
            BIOME_PATTERNS[BIOME_MESA_PLATEAU] = createPattern(image);
            BIOME_PATTERNS[BIOME_MESA_PLATEAU_F] = createPattern(image);
            BIOME_PATTERNS[BIOME_MESA_PLATEAU_F_M] = createPattern(image);
            BIOME_PATTERNS[BIOME_MESA_PLATEAU_M] = createPattern(image);

            image = ImageIO.read(ClassLoader.getSystemResourceAsStream("org/pepsoft/worldpainter/icons/mesa_bryce_pattern.png"));
            BIOME_PATTERNS[BIOME_MESA_BRYCE] = createPattern(image);

            image = ImageIO.read(ClassLoader.getSystemResourceAsStream("org/pepsoft/worldpainter/icons/jungle_edge_pattern.png"));
            BIOME_PATTERNS[BIOME_JUNGLE_EDGE] = createPattern(image);

            image = ImageIO.read(ClassLoader.getSystemResourceAsStream("org/pepsoft/worldpainter/icons/jungle_edge_hills_pattern.png"));
            BIOME_PATTERNS[BIOME_JUNGLE_EDGE_M] = createPattern(image);
        } catch (IOException e) {
            throw new RuntimeException("I/O error loading image", e);
        }
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