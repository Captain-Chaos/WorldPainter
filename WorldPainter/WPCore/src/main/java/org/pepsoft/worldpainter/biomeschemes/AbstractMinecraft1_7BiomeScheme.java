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
 * used in Minecraft 1.7 to 1.12.1.
 *
 * @author pepijn
 */
public abstract class AbstractMinecraft1_7BiomeScheme extends AbstractBiomeScheme implements Minecraft1_7Biomes {
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
                return colourScheme.getColour(BLK_END_STONE);
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
    
    static final boolean[][][] BIOME_PATTERNS = new boolean[168][][];

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