/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.biomeschemes;

import org.pepsoft.util.ColourUtils;
import org.pepsoft.worldpainter.BiomeScheme;
import org.pepsoft.worldpainter.ColourScheme;
import org.pepsoft.worldpainter.layers.renderers.VoidRenderer;

import static org.pepsoft.minecraft.Material.*;

/**
 * An abstract base class for {@link BiomeScheme}s which provide the biomes as
 * used in Minecraft 1.18 and later.
 *
 * @author pepijn
 */
public abstract class AbstractMinecraft1_21BiomeScheme extends AbstractBiomeScheme implements Minecraft1_21Biomes {
    @Override
    public final int getBiomeCount() {
        return HIGHEST_BIOME_ID + 1;
    }

    @Override
    public final int getColour(int biome, ColourScheme colourScheme) {
        switch (biome) {
            case BIOME_OCEAN:
            case BIOME_RIVER:
            case BIOME_DEEP_OCEAN:
            case BIOME_DEEP_WARM_OCEAN:
                return 0xff3f76e4;
            case BIOME_LUKEWARM_OCEAN:
            case BIOME_DEEP_LUKEWARM_OCEAN:
                return 0xff45adf2;
            case BIOME_WARM_OCEAN:
                return 0xff43d5ee;
            case BIOME_COLD_OCEAN:
            case BIOME_DEEP_COLD_OCEAN:
                return 0xff3d57d6;
            case BIOME_PLAINS:
            case BIOME_EXTREME_HILLS:
            case BIOME_EXTREME_HILLS_EDGE:
            case BIOME_EXTREME_HILLS_PLUS:
            case BIOME_EXTREME_HILLS_PLUS_M:
            case BIOME_SUNFLOWER_PLAINS:
            case BIOME_DRIPSTONE_CAVES:
            case BIOME_LUSH_CAVES:
            case BIOME_MEADOW:
            case BIOME_DEEP_DARK:
                return colourScheme.getColour(GRASS_BLOCK);
            case BIOME_EXTREME_HILLS_M:
                return colourScheme.getColour(GRAVEL);
            case BIOME_DESERT:
            case BIOME_BEACH:
            case BIOME_DESERT_HILLS:
            case BIOME_DESERT_M:
                return colourScheme.getColour(SAND);
            case BIOME_FOREST:
            case BIOME_FOREST_HILLS:
            case BIOME_SWAMPLAND:
            case BIOME_ROOFED_FOREST:
            case BIOME_ROOFED_FOREST_M:
            case BIOME_FLOWER_FOREST:
            case BIOME_SWAMPLAND_M:
            case BIOME_MANGROVE_SWAMP:
                return colourScheme.getColour(LEAVES_OAK);
            case BIOME_JUNGLE:
            case BIOME_JUNGLE_HILLS:
            case BIOME_JUNGLE_EDGE:
            case BIOME_JUNGLE_M:
            case BIOME_JUNGLE_EDGE_M:
                return colourScheme.getColour(LEAVES_JUNGLE);
            case BIOME_BAMBOO_JUNGLE:
            case BIOME_BAMBOO_JUNGLE_HILLS:
                return colourScheme.getColour(BAMBOO_NO_LEAVES);
            case BIOME_ICE_PLAINS:
            case BIOME_ICE_MOUNTAINS:
            case BIOME_COLD_TAIGA:
            case BIOME_COLD_TAIGA_HILLS:
            case BIOME_COLD_TAIGA_M:
            case BIOME_ICE_PLAINS_SPIKES:
            case BIOME_COLD_BEACH:
            case BIOME_FROZEN_PEAKS:
            case BIOME_GROVE:
            case BIOME_JAGGED_PEAKS:
            case BIOME_SNOWY_SLOPES:
                return colourScheme.getColour(SNOW);
            case BIOME_HELL:
            case BIOME_SOUL_SAND_VALLEY:
                return colourScheme.getColour(NETHERRACK);
            case BIOME_BASALT_DELTAS:
                return colourScheme.getColour(BASALT);
            case BIOME_CRIMSON_FOREST:
                return colourScheme.getColour(CRIMSON_NYLIUM);
            case BIOME_WARPED_FOREST:
                return colourScheme.getColour(WARPED_NYLIUM);
            case BIOME_SKY:
            case BIOME_SMALL_END_ISLANDS:
            case BIOME_END_MIDLANDS:
            case BIOME_END_HIGHLANDS:
            case BIOME_END_BARRENS:
                return colourScheme.getColour(END_STONE);
            case BIOME_FROZEN_OCEAN:
            case BIOME_FROZEN_RIVER:
            case BIOME_DEEP_FROZEN_OCEAN:
                return colourScheme.getColour(ICE);
            case BIOME_MUSHROOM_ISLAND:
            case BIOME_MUSHROOM_ISLAND_SHORE:
                return colourScheme.getColour(MYCELIUM);
            case BIOME_STONE_BEACH:
            case BIOME_STONY_PEAKS:
                return colourScheme.getColour(STONE);
            case BIOME_BIRCH_FOREST:
            case BIOME_BIRCH_FOREST_HILLS:
            case BIOME_BIRCH_FOREST_HILLS_M:
            case BIOME_BIRCH_FOREST_M:
                return colourScheme.getColour(LEAVES_BIRCH);
            case BIOME_TAIGA:
            case BIOME_TAIGA_HILLS:
            case BIOME_MEGA_TAIGA:
            case BIOME_MEGA_TAIGA_HILLS:
            case BIOME_MEGA_SPRUCE_TAIGA:
            case BIOME_MEGA_SPRUCE_TAIGA_HILLS:
            case BIOME_TAIGA_M:
                return colourScheme.getColour(LEAVES_PINE);
            case BIOME_SAVANNA:
            case BIOME_SAVANNA_M:
            case BIOME_SAVANNA_PLATEAU:
            case BIOME_SAVANNA_PLATEAU_M:
                return colourScheme.getColour(LEAVES_ACACIA);
            case BIOME_MESA:
            case BIOME_MESA_BRYCE:
            case BIOME_MESA_PLATEAU:
            case BIOME_MESA_PLATEAU_F:
            case BIOME_MESA_PLATEAU_F_M:
            case BIOME_MESA_PLATEAU_M:
                return colourScheme.getColour(HARDENED_CLAY);
            case BIOME_THE_VOID:
                return VoidRenderer.getColour();
            case BIOME_CHERRY_GROVE:
                return colourScheme.getColour(LEAVES_CHERRY);
            case BIOME_PALE_GARDEN:
                return ColourUtils.mix(colourScheme.getColour(LEAVES_OAK), 0xffffff);
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

    @Override
    public String getStringId(int biome) {
        if (MODERN_IDS[biome] == null) {
            throw new IllegalArgumentException(Integer.toString(biome));
        }
        return MODERN_IDS[biome];
    }
}