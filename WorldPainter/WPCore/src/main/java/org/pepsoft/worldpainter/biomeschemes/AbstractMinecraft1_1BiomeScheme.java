/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.biomeschemes;

import org.pepsoft.worldpainter.BiomeScheme;
import org.pepsoft.worldpainter.ColourScheme;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;

import static org.pepsoft.minecraft.Constants.*;

/**
 * An abstract base class for {@link BiomeScheme}s which provide the biomes as
 * used in Minecraft 1.1.
 *
 * @author pepijn
 */
public abstract class AbstractMinecraft1_1BiomeScheme extends AbstractBiomeScheme {
    @Override
    public final int getBiomeCount() {
        return BIOME_NAMES.length;
    }

    @Override
    public final int getColour(int biome, ColourScheme colourScheme) {
        switch (biome) {
            case BIOME_OCEAN:
                return colourScheme.getColour(BLK_WATER);
            case BIOME_PLAINS:
                return colourScheme.getColour(BLK_GRASS);
            case BIOME_DESERT:
                return colourScheme.getColour(BLK_SAND);
            case BIOME_EXTREME_HILLS:
                return colourScheme.getColour(BLK_GRASS);
            case BIOME_FOREST:
                return colourScheme.getColour(BLK_LEAVES);
            case BIOME_TAIGA:
                return colourScheme.getColour(BLK_SNOW);
            case BIOME_SWAMPLAND:
                return colourScheme.getColour(BLK_LEAVES);
            case BIOME_RIVER:
                return colourScheme.getColour(BLK_WATER);
            case BIOME_HELL:
                return colourScheme.getColour(BLK_NETHERRACK);
            case BIOME_SKY:
                return colourScheme.getColour(BLK_AIR);
            case BIOME_FROZEN_OCEAN:
                return colourScheme.getColour(BLK_ICE);
            case BIOME_FROZEN_RIVER:
                return colourScheme.getColour(BLK_ICE);
            case BIOME_ICE_PLAINS:
                return colourScheme.getColour(BLK_SNOW);
            case BIOME_ICE_MOUNTAINS:
                return colourScheme.getColour(BLK_SNOW);
            case BIOME_MUSHROOM_ISLAND:
                return colourScheme.getColour(BLK_MYCELIUM);
            case BIOME_MUSHROOM_ISLAND_SHORE:
                return colourScheme.getColour(BLK_MYCELIUM);
            case BIOME_BEACH:
                return colourScheme.getColour(BLK_SAND);
            case BIOME_DESERT_HILLS:
                return colourScheme.getColour(BLK_SAND);
            case BIOME_FOREST_HILLS:
                return colourScheme.getColour(BLK_LEAVES);
            case BIOME_TAIGA_HILLS:
                return colourScheme.getColour(BLK_SNOW);
            case BIOME_EXTREME_HILLS_EDGE:
                return colourScheme.getColour(BLK_GRASS);
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
        return biome <= BIOME_EXTREME_HILLS_EDGE;
    }

    @Override
    public String getBiomeName(int biome) {
        return BIOME_NAMES[biome];
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
    public static final String[] BIOME_NAMES = {
        "Ocean",
        "Plains",
        "Desert",
        "Extreme Hills",
        "Forest",
        "Taiga",
        "Swampland",
        "River",
        "Hell",
        "Sky",
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
        "Extreme Hills Edge"
    };

    private static final boolean[][][] BIOME_PATTERNS = new boolean[21][][];

    static {
        try {
            BufferedImage image = ImageIO.read(ClassLoader.getSystemResourceAsStream("org/pepsoft/worldpainter/icons/swamp_pattern.png"));
            BIOME_PATTERNS[BIOME_SWAMPLAND] = createPattern(image);

            image = ImageIO.read(ClassLoader.getSystemResourceAsStream("org/pepsoft/worldpainter/icons/mountains_pattern.png"));
            BIOME_PATTERNS[BIOME_EXTREME_HILLS] = createPattern(image);
            BIOME_PATTERNS[BIOME_ICE_MOUNTAINS] = createPattern(image);

            image = ImageIO.read(ClassLoader.getSystemResourceAsStream("org/pepsoft/worldpainter/icons/deciduous_trees_pattern.png"));
            BIOME_PATTERNS[BIOME_FOREST] = createPattern(image);

            image = ImageIO.read(ClassLoader.getSystemResourceAsStream("org/pepsoft/worldpainter/icons/pine_trees_pattern.png"));
            BIOME_PATTERNS[BIOME_TAIGA] = createPattern(image);

            image = ImageIO.read(ClassLoader.getSystemResourceAsStream("org/pepsoft/worldpainter/icons/hills_pattern.png"));
            BIOME_PATTERNS[BIOME_DESERT_HILLS] = createPattern(image);
            BIOME_PATTERNS[BIOME_EXTREME_HILLS_EDGE] = createPattern(image);

            image = ImageIO.read(ClassLoader.getSystemResourceAsStream("org/pepsoft/worldpainter/icons/deciduous_hills_pattern.png"));
            BIOME_PATTERNS[BIOME_FOREST_HILLS] = createPattern(image);

            image = ImageIO.read(ClassLoader.getSystemResourceAsStream("org/pepsoft/worldpainter/icons/pine_hills_pattern.png"));
            BIOME_PATTERNS[BIOME_TAIGA_HILLS] = createPattern(image);
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