package org.pepsoft.worldpainter.layers.plants;

import org.pepsoft.minecraft.Material;
import org.pepsoft.worldpainter.exporting.MinecraftWorld;

import static org.pepsoft.minecraft.Constants.*;
import static org.pepsoft.minecraft.Material.WATERLOGGED;

/**
 * A category of plants, determining mainly on what foundation they can be
 * placed.
 */
public enum Category {
    PLANTS_AND_FLOWERS {
        @Override
        boolean isValidFoundation(MinecraftWorld world, int x, int y, int z) {
            final Material material = world.getMaterialAt(x, y, z);
            return material.isNamed(MC_GRASS_BLOCK)
                    || material.isNamed(MC_DIRT)
                    || material.isNamed(MC_COARSE_DIRT)
                    || material.isNamed(MC_PODZOL)
                    || material.isNamed(MC_FARMLAND);
        }
    },

    SAPLINGS {
        @Override
        boolean isValidFoundation(MinecraftWorld world, int x, int y, int z) {
            final Material material = world.getMaterialAt(x, y, z);
            return material.isNamed(MC_GRASS_BLOCK)
                    || material.isNamed(MC_DIRT)
                    || material.isNamed(MC_COARSE_DIRT)
                    || material.isNamed(MC_PODZOL)
                    || material.isNamed(MC_FARMLAND);
        }
    },

    MUSHROOMS {
        @Override
        boolean isValidFoundation(MinecraftWorld world, int x, int y, int z) {
            final Material material = world.getMaterialAt(x, y, z);
            // If it's dark enough mushrooms can be placed on pretty much
            // anything
            return (! material.veryInsubstantial)
                    && (! material.isNamed(MC_GLASS))
                    && (! material.isNamed(MC_ICE));
        }
    },

    CROPS {
        @Override
        boolean isValidFoundation(MinecraftWorld world, int x, int y, int z) {
            return world.getMaterialAt(x, y, z).isNamed(MC_FARMLAND);
        }
    },

    SUGAR_CANE {
        @Override
        boolean isValidFoundation(MinecraftWorld world, int x, int y, int z) {
            final Material material = world.getMaterialAt(x, y, z);
            return (material.isNamed(MC_GRASS_BLOCK)
                    || material.isNamed(MC_DIRT)
                    || material.isNamed(MC_COARSE_DIRT)
                    || material.isNamed(MC_PODZOL)
                    || material.isNamed(MC_SAND)
                    || material.isNamed(MC_RED_SAND)
                    || material.isNamed(MC_FARMLAND))
                    && (isWater(world, x - 1, y, z)
                    || isWater(world, x, y - 1, z)
                    || isWater(world, x + 1, y, z)
                    || isWater(world, x, y + 1, z));
        }
    },

    CACTUS {
        @Override
        boolean isValidFoundation(MinecraftWorld world, int x, int y, int z) {
            final Material material = world.getMaterialAt(x, y, z);
            return (material.isNamed(MC_SAND) || material.isNamed(MC_RED_SAND))
                    && (! isSolid(world, x - 1, y, z + 1))
                    && (! isSolid(world, x, y - 1, z + 1))
                    && (! isSolid(world, x + 1, y, z + 1))
                    && (! isSolid(world, x, y + 1, z + 1));
        }
    },

    FLOATING_PLANTS {
        @Override
        boolean isValidFoundation(MinecraftWorld world, int x, int y, int z) {
            // Just check whether the location is flooded; a special case in
            // the exporter will check for the water surface
            return isWater(world, x, y, z + 1);
        }
    },

    NETHER {
        @Override
        boolean isValidFoundation(MinecraftWorld world, int x, int y, int z) {
            return world.getMaterialAt(x, y, z).isNamed(MC_SOUL_SAND);
        }
    },

    END {
        @Override
        boolean isValidFoundation(MinecraftWorld world, int x, int y, int z) {
            final Material material = world.getMaterialAt(x, y, z);
            return material.isNamed(MC_END_STONE) || material.isNamed(MC_CHORUS_PLANT);
        }
    },

    WATER_PLANTS {
        @Override
        boolean isValidFoundation(MinecraftWorld world, int x, int y, int z) {
            // TODOMC13 it's not clear on what blocks water plants can be
            //  planted so for now allow all solid blocks
            return world.getMaterialAt(x, y, z).solid && isWater(world, x, y, z + 1);
        }
    };

    abstract boolean isValidFoundation(MinecraftWorld world, int x, int y, int z);

    protected final boolean isSolid(MinecraftWorld world, int x, int y, int height) {
        Material material = world.getMaterialAt(x, y, height);
        return material.isNamed(MC_CACTUS) || (! material.veryInsubstantial);
    }

    protected final boolean isWater(MinecraftWorld world, int x, int y, int height) {
        Material material = world.getMaterialAt(x, y, height);
        return material.isNamed(MC_WATER) || material.getProperty(WATERLOGGED, false);
    }
}
