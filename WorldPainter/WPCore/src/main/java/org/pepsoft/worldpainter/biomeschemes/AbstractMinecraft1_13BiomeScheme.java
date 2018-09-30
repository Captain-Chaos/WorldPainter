/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.biomeschemes;

import org.pepsoft.worldpainter.BiomeScheme;
import org.pepsoft.worldpainter.ColourScheme;
import org.pepsoft.worldpainter.layers.renderers.VoidRenderer;

import static org.pepsoft.minecraft.Constants.*;
import static org.pepsoft.worldpainter.biomeschemes.AbstractMinecraft1_7BiomeScheme.BIOME_PATTERNS;

/**
 * An abstract base class for {@link BiomeScheme}s which provide the biomes as
 * used in Minecraft 1.13 and later.
 *
 * @author pepijn
 */
public abstract class AbstractMinecraft1_13BiomeScheme extends AbstractBiomeScheme implements Minecraft1_13Biomes {
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
            case BIOME_WARM_OCEAN:
            case BIOME_LUKEWARM_OCEAN:
            case BIOME_COLD_OCEAN:
            case BIOME_DEEP_WARM_OCEAN:
            case BIOME_DEEP_LUKEWARM_OCEAN:
            case BIOME_DEEP_COLD_OCEAN:
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
            case BIOME_END_SMALL_ISLANDS:
            case BIOME_END_MIDLANDS:
            case BIOME_END_HIGHLANDS:
            case BIOME_END_BARRENS:
                return colourScheme.getColour(BLK_END_STONE);
            case BIOME_FROZEN_OCEAN:
            case BIOME_FROZEN_RIVER:
            case BIOME_DEEP_FROZEN_OCEAN:
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
            case BIOME_VOID:
                return VoidRenderer.getColour();
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
}