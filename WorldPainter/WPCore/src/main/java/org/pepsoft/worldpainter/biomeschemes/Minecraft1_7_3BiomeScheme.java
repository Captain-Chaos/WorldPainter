/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.biomeschemes;

import net.minecraft.v1_7_3.BiomeGenerator;
import org.pepsoft.worldpainter.ColourScheme;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;

import static org.pepsoft.minecraft.Constants.*;

/**
 *
 * @author pepijn
 */
public final class Minecraft1_7_3BiomeScheme extends AbstractBiomeScheme {
    @Override
    public int[] getBiomes(int x, int y, int width, int height) {
        return biomeGenerator.getBiomes(null, x, y, width, height);
    }
    
    @Override
    public void getBiomes(int x, int y, int width, int height, int[] buffer) {
        int[] biomes = biomeGenerator.getBiomes(null, x, y, width, height);
        System.arraycopy(biomes, 0, buffer, 0, Math.min(biomes.length, buffer.length));
    }

    @Override
    public int getBiomeCount() {
        return BIOME_NAMES.length;
    }
    
    @Override
    public void setSeed(long seed) {
        if (seed != this.seed) {
            this.seed = seed;
            biomeGenerator = new BiomeGenerator(seed);
        }
    }

    @Override
    public final int getColour(int biome, ColourScheme colourScheme) {
        switch (biome) {
            case 0:
                return 0;
            case BIOME_PLAINS:
                return colourScheme.getColour(BLK_GRASS);
            case BIOME_DESERT:
                return colourScheme.getColour(BLK_SAND);
            case BIOME_FOREST:
                return colourScheme.getColour(BLK_LEAVES);
            case BIOME_TAIGA:
                return colourScheme.getColour(BLK_SNOW);
            case BIOME_SWAMPLAND:
                return colourScheme.getColour(BLK_LEAVES);
            case BIOME_HELL:
                return colourScheme.getColour(BLK_NETHERRACK);
            case BIOME_SKY:
                return colourScheme.getColour(BLK_AIR);
            case BIOME_SEASONAL_FOREST:
                return colourScheme.getColour(BLK_LEAVES);
            case BIOME_SAVANNA:
                return colourScheme.getColour(BLK_GRASS);
            case BIOME_SHRUBLAND:
                return colourScheme.getColour(BLK_LEAVES);
            case BIOME_ICE_DESERT:
                return colourScheme.getColour(BLK_SNOW);
            case BIOME_TUNDRA:
                return colourScheme.getColour(BLK_SNOW);
            case BIOME_RAINFOREST:
                return colourScheme.getColour(BLK_LEAVES);
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
        return biome <= BIOME_RAINFOREST;
    }

    @Override
    public String getBiomeName(int biome) {
        return BIOME_NAMES[biome];
    }
    
    public static final int BIOME_SWAMPLAND       =  1;
    public static final int BIOME_SEASONAL_FOREST =  2;
    public static final int BIOME_FOREST          =  3;
    public static final int BIOME_SAVANNA         =  4;
    public static final int BIOME_SHRUBLAND       =  5;
    public static final int BIOME_TAIGA           =  6;
    public static final int BIOME_DESERT          =  7;
    public static final int BIOME_PLAINS          =  8;
    public static final int BIOME_ICE_DESERT      =  9;
    public static final int BIOME_TUNDRA          = 10;
    public static final int BIOME_HELL            = 11;
    public static final int BIOME_SKY             = 12;
    public static final int BIOME_RAINFOREST      = 13;
    
    public static final String[] BIOME_NAMES = {
        null,
        "Swampland",
        "Seasonal Forest",
        "Forest",
        "Savanna",
        "Shrubland",
        "Taiga",
        "Desert",
        "Plains",
        "Ice Desert",
        "Tundra",
        "Hell",
        "Sky",
        "Rainforest"
    };
    
    private long seed = Long.MIN_VALUE;
    private BiomeGenerator biomeGenerator;
    private static final boolean[][][] BIOME_PATTERNS = new boolean[23][][];

    static {
        try {
            BufferedImage image = ImageIO.read(ClassLoader.getSystemResourceAsStream("org/pepsoft/worldpainter/icons/swamp_pattern.png"));
            BIOME_PATTERNS[BIOME_SWAMPLAND] = createPattern(image);

            image = ImageIO.read(ClassLoader.getSystemResourceAsStream("org/pepsoft/worldpainter/icons/deciduous_trees_pattern.png"));
            BIOME_PATTERNS[BIOME_FOREST] = createPattern(image);
            BIOME_PATTERNS[BIOME_RAINFOREST] = createPattern(image);
            BIOME_PATTERNS[BIOME_SEASONAL_FOREST] = createPattern(image);

            image = ImageIO.read(ClassLoader.getSystemResourceAsStream("org/pepsoft/worldpainter/icons/pine_trees_pattern.png"));
            BIOME_PATTERNS[BIOME_TAIGA] = createPattern(image);
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