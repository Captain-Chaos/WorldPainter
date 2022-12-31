package org.pepsoft.worldpainter.layers.plants;

import org.pepsoft.minecraft.Material;
import org.pepsoft.worldpainter.exporting.MinecraftWorld;

import static org.pepsoft.minecraft.Constants.*;

/**
 * A category of plants, determining mainly on what foundation they can be
 * placed.
 */
public enum Category {
    PLANTS_AND_FLOWERS {
        @Override
        boolean isValidFoundation(MinecraftWorld world, int x, int y, int z, boolean checkBlockBelow) {
            final Material material = world.getMaterialAt(x, y, z);
            return ((! checkBlockBelow) || material.modded || material.isNamed(MC_GRASS_BLOCK)
                    || material.isNamed(MC_DIRT)
                    || material.isNamed(MC_COARSE_DIRT)
                    || material.isNamed(MC_PODZOL)
                    || material.isNamed(MC_FARMLAND)
                    || material.isNamed(MC_ROOTED_DIRT)
                    || material.isNamed(MC_MOSS_BLOCK)
                    || material.isNamed(MC_MUD))
                && (! isFlooded(world, x, y, z));
        }
    },

    SAPLINGS {
        @Override
        boolean isValidFoundation(MinecraftWorld world, int x, int y, int z, boolean checkBlockBelow) {
            return PLANTS_AND_FLOWERS.isValidFoundation(world, x, y, z, checkBlockBelow);
        }
    },

    MUSHROOMS {
        @Override
        boolean isValidFoundation(MinecraftWorld world, int x, int y, int z, boolean checkBlockBelow) {
            final Material material = world.getMaterialAt(x, y, z);
            // If it's dark enough mushrooms can be placed on pretty much anything
            return ((! checkBlockBelow) || material.modded || (material.solid && material.opaque && material.natural)) && (! isFlooded(world, x, y, z));
        }
    },

    CROPS {
        @Override
        boolean isValidFoundation(MinecraftWorld world, int x, int y, int z, boolean checkBlockBelow) {
            final Material material = world.getMaterialAt(x, y, z);
            return ((! checkBlockBelow) || material.modded || material.isNamedOneOf(MC_FARMLAND, MC_GRASS_BLOCK, MC_DIRT, MC_COARSE_DIRT, MC_ROOTED_DIRT, MC_GRASS_PATH, MC_DIRT_PATH)) && (! isFlooded(world, x, y, z));
        }
    },

    SUGAR_CANE {
        @Override
        boolean isValidFoundation(MinecraftWorld world, int x, int y, int z, boolean checkBlockBelow) {
            final Material material = world.getMaterialAt(x, y, z);
            return ((! checkBlockBelow)
                    || material.modded
                    || material.isNamedOneOf(MC_GRASS_BLOCK, MC_DIRT, MC_COARSE_DIRT, MC_PODZOL, MC_SAND, MC_RED_SAND, MC_ROOTED_DIRT, MC_MOSS_BLOCK, MC_MUD))
                && (isWatery(world, x - 1, y, z)
                    || isWatery(world, x, y - 1, z)
                    || isWatery(world, x + 1, y, z)
                    || isWatery(world, x, y + 1, z))
                && (! isFlooded(world, x, y, z));
        }
    },

    CACTUS {
        @Override
        boolean isValidFoundation(MinecraftWorld world, int x, int y, int z, boolean checkBlockBelow) {
            final Material material = world.getMaterialAt(x, y, z);
            return ((! checkBlockBelow) || material.modded || material.isNamed(MC_SAND) || material.isNamed(MC_RED_SAND))
                    && (! isSolidOrCactus(world, x - 1, y, z + 1))
                    && (! isSolidOrCactus(world, x, y - 1, z + 1))
                    && (! isSolidOrCactus(world, x + 1, y, z + 1))
                    && (! isSolidOrCactus(world, x, y + 1, z + 1))
                    && (! isFlooded(world, x, y, z));
        }
    },

    FLOATING_PLANTS {
        @Override
        boolean isValidFoundation(MinecraftWorld world, int x, int y, int z, boolean checkBlockBelow) {
            // Just check whether the location is flooded; a special case in
            // the exporter will check for the water surface
            return isWatery(world, x, y, z + 1);
        }
    },

    NETHER {
        @Override
        boolean isValidFoundation(MinecraftWorld world, int x, int y, int z, boolean checkBlockBelow) {
            final Material material = world.getMaterialAt(x, y, z);
            return PLANTS_AND_FLOWERS.isValidFoundation(world, x, y, z, checkBlockBelow)
                    || (((! checkBlockBelow) || material.modded || material.isNamed(MC_SOUL_SOIL)) && (! isFlooded(world, x, y, z)));
        }
    },

    END {
        @Override
        boolean isValidFoundation(MinecraftWorld world, int x, int y, int z, boolean checkBlockBelow) {
            final Material material = world.getMaterialAt(x, y, z);
            return ((! checkBlockBelow) || material.modded || material.isNamed(MC_END_STONE) || material.isNamed(MC_CHORUS_PLANT)) && (! isFlooded(world, x, y, z));
        }
    },

    WATER_PLANTS {
        @Override
        boolean isValidFoundation(MinecraftWorld world, int x, int y, int z, boolean checkBlockBelow) {
            // TODOMC13 it's not clear on what blocks water plants can be planted so for now allow all solid, opaque and
            //  natural blocks
            final Material material = world.getMaterialAt(x, y, z);
            return ((! checkBlockBelow) || material.modded || (material.solid && material.opaque && material.natural)) && world.getMaterialAt(x, y, z + 1).containsWater();
        }
    },

    HANGING_DRY_PLANTS {
        @Override
        boolean isValidFoundation(MinecraftWorld world, int x, int y, int z, boolean checkBlockBelow) {
            final Material material = world.getMaterialAt(x, y, z);
            return ((! checkBlockBelow) || material.modded || (material.solid && material.opaque && material.natural)) && (! isFlooded(world, x, y, z));
        }
    },

    HANGING_WATER_PLANTS {
        @Override
        boolean isValidFoundation(MinecraftWorld world, int x, int y, int z, boolean checkBlockBelow) {
            final Material material = world.getMaterialAt(x, y, z);
            return ((! checkBlockBelow) || material.modded || (material.solid && material.opaque && material.natural)) && isFlooded(world, x, y, z);
        }
    },

    DRIPLEAF {
        @Override
        boolean isValidFoundation(MinecraftWorld world, int x, int y, int z, boolean checkBlockBelow) {
            if (checkBlockBelow) {
                Material material = world.getMaterialAt(x, y, z);
                return material.modded || (world.getMaterialAt(x, y, z + 1).containsWater()
                        ? material.isNamedOneOf(MC_CLAY, MC_MOSS_BLOCK, MC_DIRT, MC_COARSE_DIRT, MC_FARMLAND, MC_GRASS_BLOCK, MC_PODZOL, MC_ROOTED_DIRT, MC_MYCELIUM, MC_MUD)
                        : material.isNamedOneOf(MC_CLAY, MC_MOSS_BLOCK));
            } else {
                return true;
            }
        }
    };

    abstract boolean isValidFoundation(MinecraftWorld world, int x, int y, int z, boolean checkBlockBelow);

    protected final boolean isSolidOrCactus(MinecraftWorld world, int x, int y, int z) {
        Material material = world.getMaterialAt(x, y, z);
        return material.isNamed(MC_CACTUS) || (! material.veryInsubstantial);
    }

    protected final boolean isWatery(MinecraftWorld world, int x, int y, int z) {
        return world.getMaterialAt(x, y, z).containsWater();
    }

    protected final boolean isFlooded(MinecraftWorld world, int x, int y, int z) {
        final Material materialAbove = world.getMaterialAt(x, y, z + 1);
        return materialAbove.containsWater() || materialAbove.isNamed(MC_LAVA);
    }
}
