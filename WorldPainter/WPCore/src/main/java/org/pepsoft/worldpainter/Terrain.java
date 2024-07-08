/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.worldpainter;

import com.google.common.collect.ImmutableSet;
import org.pepsoft.minecraft.Material;
import org.pepsoft.util.IconUtils;
import org.pepsoft.util.PerlinNoise;
import org.pepsoft.util.RandomField;
import org.pepsoft.worldpainter.layers.plants.Plant;
import org.pepsoft.worldpainter.layers.plants.Plants;
import org.pepsoft.worldpainter.objects.GenericObject;
import org.pepsoft.worldpainter.objects.WPObject;

import java.awt.image.BufferedImage;
import java.util.*;

import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static java.util.Comparator.comparing;
import static org.pepsoft.minecraft.Constants.*;
import static org.pepsoft.minecraft.Material.*;
import static org.pepsoft.util.MathUtils.mod;
import static org.pepsoft.worldpainter.Constants.*;
import static org.pepsoft.worldpainter.DefaultPlugin.*;
import static org.pepsoft.worldpainter.Platform.Capability.NAME_BASED;
import static org.pepsoft.worldpainter.biomeschemes.Minecraft1_17Biomes.*;

/**
 *
 * @author pepijn
 */

////////////////////////////////////////////////////////////////////////////////
//                                                                            //
//                                   WARNING!                                 //
//                                                                            //
// These values are saved in tiles and on disk by their name AND by their     //
// ordinal! It is therefore very important NOT to change the names OR the     //
// order, and to add new entries at the end!                                  //
//                                                                            //
////////////////////////////////////////////////////////////////////////////////

@SuppressWarnings("ConstantConditions") // Future-proofing
public enum Terrain {
    GRASS("Grass", GRASS_BLOCK, GRASS_BLOCK, "grass with flowers, tall grass and ferns here and there, and seagrass and kelp under water", BIOME_PLAINS) {
        @Override
        public WPObject getSurfaceObject(Platform platform, long seed, int x, int y, int waterBlocksAbove) {
            final Random rnd = new Random(seed + (x * 65537L) + (y * 4099L));
            final int rndNr = rnd.nextInt(FLOWER_INCIDENCE);
            if ((waterBlocksAbove > 0) && platform.capabilities.contains(NAME_BASED)) {
                if (grassNoise.getSeed() != (seed + GRASS_SEED_OFFSET)) {
                    grassNoise.setSeed(seed + GRASS_SEED_OFFSET);
                    tallGrassNoise.setSeed(seed + DOUBLE_TALL_GRASS_SEED_OFFSET);
                }
                final float grassValue = grassNoise.getPerlinNoise(x / SMALL_BLOBS, y / SMALL_BLOBS, 1 / SMALL_BLOBS) + (rnd.nextFloat() * 0.3f - 0.15f);
                if (grassValue > GRASS_CHANCE) {
                    if (tallGrassNoise.getPerlinNoise(x / SMALL_BLOBS, y / SMALL_BLOBS, 1 / SMALL_BLOBS) > 0) {
                        // Double tallness
                        if (grassValue > DOUBLE_TALL_GRASS_CHANCE) {
                            return Plants.KELP.realise(rnd.nextInt(Math.min(waterBlocksAbove, 26)) + 1, platform);
                        } else {
                            if ((rnd.nextInt(4) == 0) || (waterBlocksAbove < 2)) {
                                return Plants.SEAGRASS.realise(1, platform);
                            } else {
                                return Plants.TALL_SEAGRASS.realise(1, platform);
                            }
                        }
                    } else {
                        if ((grassValue > FERN_CHANCE) && (waterBlocksAbove >= 2)) {
                            return Plants.TALL_SEAGRASS.realise(1, platform);
                        } else {
                            return Plants.SEAGRASS.realise(1, platform);
                        }
                    }
                }
            } else if (waterBlocksAbove == 0) {
                if (rndNr == 0) {
                    if (dandelionNoise.getSeed() != (seed + DANDELION_SEED_OFFSET)) {
                        dandelionNoise.setSeed(seed + DANDELION_SEED_OFFSET);
                        roseNoise.setSeed(seed + ROSE_SEED_OFFSET);
                        flowerTypeField.setSeed(seed + FLOWER_TYPE_FIELD_OFFSET);
                    }
                    // Keep the "1 / SMALL_BLOBS" and the two noise generators for consistency with existing maps
                    if ((dandelionNoise.getPerlinNoise(x / SMALL_BLOBS, y / SMALL_BLOBS, 1 / SMALL_BLOBS) > FLOWER_CHANCE)
                            || (roseNoise.getPerlinNoise(x / SMALL_BLOBS, y / SMALL_BLOBS, 1 / SMALL_BLOBS) > FLOWER_CHANCE)) {
                        return FLOWER_TYPES[flowerTypeField.getValue(x, y)].realise(1, platform);
                    }
                } else {
                    if (grassNoise.getSeed() != (seed + GRASS_SEED_OFFSET)) {
                        grassNoise.setSeed(seed + GRASS_SEED_OFFSET);
                        tallGrassNoise.setSeed(seed + DOUBLE_TALL_GRASS_SEED_OFFSET);
                    }
                    // Keep the "1 / SMALL_BLOBS" for consistency with existing maps
                    final float grassValue = grassNoise.getPerlinNoise(x / SMALL_BLOBS, y / SMALL_BLOBS, 1 / SMALL_BLOBS) + (rnd.nextFloat() * 0.3f - 0.15f);
                    if (grassValue > GRASS_CHANCE) {
                        if (tallGrassNoise.getPerlinNoise(x / SMALL_BLOBS, y / SMALL_BLOBS, 1 / SMALL_BLOBS) > 0) {
                            // Double tallness
                            if (grassValue > DOUBLE_TALL_GRASS_CHANCE) {
                                if (rnd.nextInt(4) == 0) {
                                    return Plants.LARGE_FERN.realise(1, platform);
                                } else {
                                    return Plants.TALL_GRASS.realise(1, platform);
                                }
                            } else {
                                if (rnd.nextInt(4) == 0) {
                                    return Plants.FERN.realise(1, platform);
                                } else {
                                    return Plants.GRASS.realise(1, platform);
                                }
                            }
                        } else {
                            if (grassValue > FERN_CHANCE) {
                                return Plants.FERN.realise(1, platform);
                            } else {
                                return Plants.GRASS.realise(1, platform);
                            }
                        }
                    }
                }
            }
            return null;
        }

        private final PerlinNoise dandelionNoise = new PerlinNoise(0);
        private final PerlinNoise roseNoise = new PerlinNoise(0);
        private final PerlinNoise grassNoise = new PerlinNoise(0);
        private final RandomField flowerTypeField = new RandomField(4, SMALL_BLOBS, 0);
        private final PerlinNoise tallGrassNoise = new PerlinNoise(0);

        private final Plant[] FLOWER_TYPES = {
            Plants.DANDELION,
            Plants.POPPY,
            Plants.BLUE_ORCHID,
            Plants.ALLIUM,
            Plants.AZURE_BLUET,
            Plants.TULIP_RED,
            Plants.TULIP_ORANGE,
            Plants.TULIP_WHITE,
            Plants.TULIP_PINK,
            Plants.OXEYE_DAISY,
            Plants.SUNFLOWER,
            Plants.LILAC,
            Plants.ROSE_BUSH,
            Plants.PEONY,
            Plants.DANDELION, // Again to make them a bit more common
            Plants.POPPY,     // Again to make them a bit more common
        };

        private static final long DANDELION_SEED_OFFSET = 145351781L;
        private static final long ROSE_SEED_OFFSET = 28286488L;
        private static final long GRASS_SEED_OFFSET = 169191195L;
        private static final long FLOWER_TYPE_FIELD_OFFSET = 65226710L;
        private static final long DOUBLE_TALL_GRASS_SEED_OFFSET = 31695680L;
        private static final int FLOWER_INCIDENCE = 10;
    },
    DIRT("Dirt", BLK_DIRT, BLK_DIRT, "bare dirt", BIOME_PLAINS),
    SAND("Sand", BLK_SAND, BLK_SAND, "bare sand", BIOME_PLAINS),
    SANDSTONE("Sandstone", BLK_SANDSTONE, BLK_SANDSTONE, "sandstone", BIOME_PLAINS),
    STONE("Stone", BLK_STONE, BLK_STONE, "bare stone", BIOME_PLAINS),
    ROCK("Rock", "a mix of stone and cobblestone", BIOME_PLAINS) {
        @Override
        public Material getMaterial(Platform platform, long seed, int x, int y, int z, int height) {
            final int dz = z - height;
            if (dz > 0) {
                return AIR;
            } else {
                if (perlinNoise.getSeed() != (seed + STONE_SEED_OFFSET)) {
                    perlinNoise.setSeed(seed + STONE_SEED_OFFSET);
                }
                if (perlinNoise.getPerlinNoise(x / TINY_BLOBS, y / TINY_BLOBS, z / TINY_BLOBS) > 0) {
                    return Material.STONE;
                } else {
                    return Material.COBBLESTONE;
                }
            }
        }

        private final PerlinNoise perlinNoise = new PerlinNoise(0);

        private static final int STONE_SEED_OFFSET = 188434540;
    },
    WATER("Water", STATIONARY_WATER, STATIONARY_WATER, "⚠ water instead of terrain (NOTE: you may want the Flood tool instead!)", BIOME_RIVER),
    LAVA("Lava", STATIONARY_LAVA, STATIONARY_LAVA, "⚠ lava instead of terrain (NOTE: you may want the Lava tool instead!)", BIOME_PLAINS),
    @Deprecated
    SNOW("Snow on Rock", "[DEPRECATED] a thin layer of snow on a mix of stone and cobblestone", BIOME_ICE_PLAINS, 1) {
        @Override
        public Material getMaterial(Platform platform, long seed, int x, int y, int z, int height) {
            if (perlinNoise.getSeed() != (seed + STONE_SEED_OFFSET)) {
                perlinNoise.setSeed(seed + STONE_SEED_OFFSET);
            }
            if (perlinNoise.getPerlinNoise(x / TINY_BLOBS, y / TINY_BLOBS, z / TINY_BLOBS) > 0) {
                return Material.STONE;
            } else {
                return Material.COBBLESTONE;
            }
        }

        @Override
        public WPObject getSurfaceObject(Platform platform, long seed, int x, int y, int waterBlocksAbove) {
            return (waterBlocksAbove == 0) ? OBJECT_SNOW : null;
        }

        @Override
        public int getColour(long seed, int x, int y, int z, int height, Platform platform, ColourScheme colourScheme) {
            return colourScheme.getColour(Material.SNOW);
        }

        private final PerlinNoise perlinNoise = new PerlinNoise(0);
        private final WPObject OBJECT_SNOW = new GenericObject("Snow", 1, 1, 1, new Material[] { Material.SNOW });

        private static final int STONE_SEED_OFFSET = 188434540;
    },
    DEEP_SNOW("Deep Snow", BLK_SNOW_BLOCK, BLK_SNOW_BLOCK, "⚠ snow blocks instead of terrain (NOTE: you may want the Frost layer instead!)", BIOME_ICE_PLAINS),
    GRAVEL("Gravel", BLK_GRAVEL, BLK_GRAVEL, "gravel", BIOME_PLAINS),
    CLAY("Clay", BLK_CLAY, BLK_CLAY, "clay", BIOME_PLAINS),
    COBBLESTONE("Cobblestone", BLK_COBBLESTONE, BLK_COBBLESTONE, "cobblestone", BIOME_PLAINS),
    MOSSY_COBBLESTONE("Mossy Cobblestone", BLK_MOSSY_COBBLESTONE, BLK_MOSSY_COBBLESTONE, "mossy cobblestone", BIOME_PLAINS),
    NETHERRACK("Netherrack", BLK_NETHERRACK, BLK_NETHERRACK, "netherrack", BIOME_PLAINS),
    SOUL_SAND("Soul Sand", BLK_SOUL_SAND, BLK_SOUL_SAND, "soul sand", BIOME_HELL),
    OBSIDIAN("Obsidian", BLK_OBSIDIAN, BLK_OBSIDIAN, "extremely tough volcanic glass", BIOME_PLAINS),
    BEDROCK("Bedrock", BLK_BEDROCK, BLK_BEDROCK, "unbreakable bedrock", BIOME_PLAINS),
    DESERT("Desert", Material.SAND, Material.SAND, "sand with here and there a cactus or dead shrub", BIOME_DESERT) {
        @Override
        public WPObject getSurfaceObject(Platform platform, long seed, int x, int y, int waterBlocksAbove) {
            if (waterBlocksAbove > 0) {
                return null;
            }
            final int rnd = new Random(seed + (x * 65537L) + (y * 4099L)).nextInt(CACTUS_CHANCE);
            final int cactusHeight;
            boolean shrub = false;
            if (rnd < 3) {
                cactusHeight = rnd + 1;
            } else {
                cactusHeight = 0;
                if (rnd < 6) {
                    shrub = true;
                }
            }
            if (shrub) {
                return Plants.DEAD_SHRUB.realise(1, platform);
            } else if (cactusHeight == 0) {
                return null;
            } else {
                return Plants.CACTUS.realise(cactusHeight, platform);
            }
        }

        private static final int CACTUS_CHANCE = 1000;
    },
    NETHERLIKE("Netherlike", "netherrack with pockets of lava, soul sand and glowstone and patches of fire on top", BIOME_HELL, 1) {
        @Override
        public Material getMaterial(Platform platform, long seed, int x, int y, int z, int height) {
            if (glowstoneNoise.getSeed() != (seed + GLOWSTONE_SEED_OFFSET)) {
                glowstoneNoise.setSeed(seed + GLOWSTONE_SEED_OFFSET);
                soulSandNoise.setSeed(seed + SOUL_SAND_SEED_OFFSET);
                lavaNoise.setSeed(seed + LAVA_SEED_OFFSET);
            }
            if (glowstoneNoise.getPerlinNoise(x / SMALL_BLOBS, y / SMALL_BLOBS, z / SMALL_BLOBS) > .4) {
                return GLOWSTONE;
            } else if(soulSandNoise.getPerlinNoise(x / SMALL_BLOBS, y / SMALL_BLOBS, z / SMALL_BLOBS) > .4) {
                return Material.SOUL_SAND;
            } else if(lavaNoise.getPerlinNoise(x / SMALL_BLOBS, y / SMALL_BLOBS, z / SMALL_BLOBS) > .4) {
                return Material.LAVA;
            } else {
                return Material.NETHERRACK;
            }
        }

        @Override
        public WPObject getSurfaceObject(Platform platform, long seed, int x, int y, int waterBlocksAbove) {
            if (waterBlocksAbove > 0) {
                return null;
            }
            final int rnd = new Random(seed + (x * 65537L) + (y * 4099L)).nextInt(FIRE_CHANCE);
            if (rnd == 0) {
                return OBJECT_FIRE;
            } else {
                return null;
            }
        }

        private final PerlinNoise glowstoneNoise = new PerlinNoise(0);
        private final PerlinNoise soulSandNoise = new PerlinNoise(0);
        private final PerlinNoise lavaNoise = new PerlinNoise(0);
        private final WPObject OBJECT_FIRE = new GenericObject("Fire", 1, 1, 1, new Material[] { Material.FIRE });

        private static final int GLOWSTONE_SEED_OFFSET =  57861047;
        private static final int LAVA_SEED_OFFSET      = 189831882;
        private static final int SOUL_SAND_SEED_OFFSET =  81867522;
        private static final int FIRE_CHANCE           =       150;
    },
    @Deprecated
    RESOURCES("Resources", "stone on the surface with pockets of coal, ores, gravel and dirt, lava and water, etc.", BIOME_PLAINS) {
        @Override
        public Material getMaterial(Platform platform, long seed, int x, int y, int z, int height) {
            if (goldNoise.getSeed() != (seed + GOLD_SEED_OFFSET)) {
                goldNoise.setSeed(       seed + GOLD_SEED_OFFSET);
                ironNoise.setSeed(       seed + IRON_SEED_OFFSET);
                coalNoise.setSeed(       seed + COAL_SEED_OFFSET);
                lapisLazuliNoise.setSeed(seed + LAPIS_LAZULI_SEED_OFFSET);
                diamondNoise.setSeed(    seed + DIAMOND_SEED_OFFSET);
                redstoneNoise.setSeed(   seed + REDSTONE_SEED_OFFSET);
                waterNoise.setSeed(      seed + WATER_SEED_OFFSET);
                lavaNoise.setSeed(       seed + LAVA_SEED_OFFSET);
                dirtNoise.setSeed(       seed + DIRT_SEED_OFFSET);
                gravelNoise.setSeed(     seed + GRAVEL_SEED_OFFSET);
            }
            final double dx = x / TINY_BLOBS, dy = y / TINY_BLOBS, dz = z / TINY_BLOBS;
            final double dirtX = x / SMALL_BLOBS, dirtY = y / SMALL_BLOBS, dirtZ = z / SMALL_BLOBS;
            if ((z <= COAL_LEVEL) && (coalNoise.getPerlinNoise(dx, dy, dz) >= COAL_CHANCE)) {
                return COAL;
            } else if ((z <= DIRT_LEVEL) && (dirtNoise.getPerlinNoise(dirtX, dirtY, dirtZ) >= DIRT_CHANCE)) {
                return Material.DIRT;
            } else if ((z <= GRAVEL_LEVEL) && (gravelNoise.getPerlinNoise(dirtX, dirtY, dirtZ) >= GRAVEL_CHANCE)) {
                return Material.GRAVEL;
            } else if ((z <= REDSTONE_LEVEL) && (redstoneNoise.getPerlinNoise(dx, dy, dz) >= REDSTONE_CHANCE)) {
                return REDSTONE_ORE;
            } else if ((z <= IRON_LEVEL) && (ironNoise.getPerlinNoise(dx, dy, dz) >= IRON_CHANCE)) {
                return IRON_ORE;
            } else if ((z <= WATER_LEVEL) && (waterNoise.getPerlinNoise(dx, dy, dz) >= WATER_CHANCE)) {
                return Material.WATER;
            } else if ((z <= LAVA_LEVEL) && (lavaNoise.getPerlinNoise(dx, dy, dz) >= (LAVA_CHANCE + (z * z / 65536f)))) {
//                System.out.println("Lava at level " + z);
//                if (z > highestLava) {
//                    highestLava = z;
//                }
//                System.out.println("Highest lava: " + highestLava);
                return Material.LAVA;
            } else if ((z <= GOLD_LEVEL) && (goldNoise.getPerlinNoise(dx, dy, dz) >= GOLD_CHANCE)) {
                return GOLD_ORE;
            } else if ((z <= LAPIS_LAZULI_LEVEL) && (lapisLazuliNoise.getPerlinNoise(dx, dy, dz) >= LAPIS_LAZULI_CHANCE)) {
                return LAPIS_LAZULI_ORE;
            } else if ((z <= DIAMOND_LEVEL) && (diamondNoise.getPerlinNoise(dx, dy, dz) >= DIAMOND_CHANCE)) {
                return DIAMOND_ORE;
            } else {
                return Material.STONE;
            }
        }

        private final PerlinNoise goldNoise        = new PerlinNoise(0);
        private final PerlinNoise ironNoise        = new PerlinNoise(0);
        private final PerlinNoise coalNoise        = new PerlinNoise(0);
        private final PerlinNoise lapisLazuliNoise = new PerlinNoise(0);
        private final PerlinNoise diamondNoise     = new PerlinNoise(0);
        private final PerlinNoise redstoneNoise    = new PerlinNoise(0);
        private final PerlinNoise waterNoise       = new PerlinNoise(0);
        private final PerlinNoise lavaNoise        = new PerlinNoise(0);
        private final PerlinNoise dirtNoise        = new PerlinNoise(0);
        private final PerlinNoise gravelNoise      = new PerlinNoise(0);

//        private int highestLava = 0;

        private static final long GOLD_SEED_OFFSET         = 148503743;
        private static final long IRON_SEED_OFFSET         = 171021655;
        private static final long COAL_SEED_OFFSET         = 81779663;
        private static final long LAPIS_LAZULI_SEED_OFFSET = 174377337;
        private static final long DIAMOND_SEED_OFFSET      = 14554756;
        private static final long REDSTONE_SEED_OFFSET     = 48636151;
        private static final long WATER_SEED_OFFSET        = 42845153;
        private static final long LAVA_SEED_OFFSET         = 62452072;
        private static final long DIRT_SEED_OFFSET         = 193567846;
        private static final long GRAVEL_SEED_OFFSET       = 19951397;
    },
    BEACHES("Beaches", "grass with patches of sand, gravel and clay, and here and there seagrass and kelp under water", BIOME_BEACH) {
        @Override
        public Material getMaterial(Platform platform, long seed, int x, int y, int z, int height) {
            if (sandNoise.getSeed() != (seed + SAND_SEED_OFFSET)) {
                sandNoise.setSeed(seed + SAND_SEED_OFFSET);
                clayNoise.setSeed(seed + CLAY_SEED_OFFSET);
            }
            float noise = clayNoise.getPerlinNoise(x / SMALL_BLOBS, y / SMALL_BLOBS, z / SMALL_BLOBS);
            if (noise >= BEACH_CLAY_CHANCE) {
                return Material.CLAY;
            } else {
                noise = sandNoise.getPerlinNoise(x / HUGE_BLOBS, y / HUGE_BLOBS, z / SMALL_BLOBS);
                noise += sandNoise.getPerlinNoise(x / SMALL_BLOBS, y / SMALL_BLOBS, z / SMALL_BLOBS) / 2;
                if (noise >= BEACH_SAND_CHANCE) {
                    return Material.SAND;
                } else if (-noise >= BEACH_GRAVEL_CHANCE) {
                    return Material.GRAVEL;
                } else {
                    return Material.GRASS_BLOCK;
                }
            }
        }

        @Override
        public WPObject getSurfaceObject(Platform platform, long seed, int x, int y, int waterBlocksAbove) {
            if (platform.capabilities.contains(NAME_BASED) && (waterBlocksAbove > 0)) {
                final Random rnd = new Random(seed + (x * 65537L) + (y * 4099L));
                if (grassNoise.getSeed() != (seed + GRASS_SEED_OFFSET)) {
                    grassNoise.setSeed(seed + GRASS_SEED_OFFSET);
                    tallGrassNoise.setSeed(seed + DOUBLE_TALL_GRASS_SEED_OFFSET);
                }
                final float grassValue = grassNoise.getPerlinNoise(x / SMALL_BLOBS, y / SMALL_BLOBS, 1 / SMALL_BLOBS) + (rnd.nextFloat() * 0.3f - 0.15f);
                if (grassValue > GRASS_CHANCE) {
                    if (tallGrassNoise.getPerlinNoise(x / SMALL_BLOBS, y / SMALL_BLOBS, 1 / SMALL_BLOBS) > 0) {
                        // Double tallness
                        if (grassValue > DOUBLE_TALL_GRASS_CHANCE) {
                            return Plants.KELP.realise(rnd.nextInt(Math.min(waterBlocksAbove, 26)) + 1, platform);
                        } else {
                            if ((rnd.nextInt(4) == 0) || (waterBlocksAbove < 2)) {
                                return Plants.SEAGRASS.realise(1, platform);
                            } else {
                                return Plants.TALL_SEAGRASS.realise(1, platform);
                            }
                        }
                    } else {
                        if ((grassValue > FERN_CHANCE) && (waterBlocksAbove >= 2)) {
                            return Plants.TALL_SEAGRASS.realise(1, platform);
                        } else {
                            return Plants.SEAGRASS.realise(1, platform);
                        }
                    }
                }
            }
            return null;
        }

        private final PerlinNoise sandNoise = new PerlinNoise(0);
        private final PerlinNoise clayNoise = new PerlinNoise(0);
        private final PerlinNoise grassNoise = new PerlinNoise(0);
        private final PerlinNoise tallGrassNoise = new PerlinNoise(0);

        private static final long SAND_SEED_OFFSET = 26796036;
        private static final long CLAY_SEED_OFFSET = 161603308;
        private static final long GRASS_SEED_OFFSET = 169191195L;
        private static final long DOUBLE_TALL_GRASS_SEED_OFFSET = 31695680L;
    },
    CUSTOM_1("Custom 1",                                  "custom material one", BIOME_PLAINS) {
        @Override public Material getMaterial(Platform platform, long seed, int x, int y, int z, int height) {return helper.getMaterial(seed, x, y, z, height);}

        @Override public Material getMaterial(Platform platform, long seed, int x, int y, float z, int height) {return helper.getMaterial(seed, x, y, z, height);}

        @Override public String getName() {return helper.getName();}

        @Override public BufferedImage getIcon(ColourScheme colourScheme) {return helper.getIcon(colourScheme);}

        @Override public boolean isCustom() {return true;}

        @Override public boolean isConfigured() {return helper.isConfigured();}

        @Override public int getDefaultBiome() {return helper.getDefaultBiome();}

        @Override public int getCustomTerrainIndex() {return helper.getCustomTerrainIndex();}

        @Override public int getColour(long seed, int x, int y, int z, int height, Platform platform, ColourScheme colourScheme) {return helper.getColour(seed, x, y, z, height, colourScheme);}

        @Override public int getColour(long seed, int x, int y, float z, int height, Platform platform, ColourScheme colourScheme) {return helper.getColour(seed, x, y, z, height, colourScheme);}

        private final CustomTerrainHelper helper = new CustomTerrainHelper(0);
    },
    CUSTOM_2("Custom 2",                                  "custom material two", BIOME_PLAINS) {
        @Override public Material getMaterial(Platform platform, long seed, int x, int y, int z, int height) {return helper.getMaterial(seed, x, y, z, height);}

        @Override public Material getMaterial(Platform platform, long seed, int x, int y, float z, int height) {return helper.getMaterial(seed, x, y, z, height);}

        @Override public String getName() {return helper.getName();}

        @Override public BufferedImage getIcon(ColourScheme colourScheme) {return helper.getIcon(colourScheme);}

        @Override public boolean isCustom() {return true;}

        @Override public boolean isConfigured() {return helper.isConfigured();}

        @Override public int getDefaultBiome() {return helper.getDefaultBiome();}

        @Override public int getCustomTerrainIndex() {return helper.getCustomTerrainIndex();}

        @Override public int getColour(long seed, int x, int y, int z, int height, Platform platform, ColourScheme colourScheme) {return helper.getColour(seed, x, y, z, height, colourScheme);}

        @Override public int getColour(long seed, int x, int y, float z, int height, Platform platform, ColourScheme colourScheme) {return helper.getColour(seed, x, y, z, height, colourScheme);}

        private final CustomTerrainHelper helper = new CustomTerrainHelper(1);
    },
    CUSTOM_3("Custom 3",                                  "custom material three", BIOME_PLAINS) {
        @Override public Material getMaterial(Platform platform, long seed, int x, int y, int z, int height) {return helper.getMaterial(seed, x, y, z, height);}

        @Override public Material getMaterial(Platform platform, long seed, int x, int y, float z, int height) {return helper.getMaterial(seed, x, y, z, height);}

        @Override public String getName() {return helper.getName();}

        @Override public BufferedImage getIcon(ColourScheme colourScheme) {return helper.getIcon(colourScheme);}

        @Override public boolean isCustom() {return true;}

        @Override public boolean isConfigured() {return helper.isConfigured();}

        @Override public int getDefaultBiome() {return helper.getDefaultBiome();}

        @Override public int getCustomTerrainIndex() {return helper.getCustomTerrainIndex();}

        @Override public int getColour(long seed, int x, int y, int z, int height, Platform platform, ColourScheme colourScheme) {return helper.getColour(seed, x, y, z, height, colourScheme);}

        @Override public int getColour(long seed, int x, int y, float z, int height, Platform platform, ColourScheme colourScheme) {return helper.getColour(seed, x, y, z, height, colourScheme);}

        private final CustomTerrainHelper helper = new CustomTerrainHelper(2);
    },
    CUSTOM_4("Custom 4",                                  "custom material four", BIOME_PLAINS) {
        @Override public Material getMaterial(Platform platform, long seed, int x, int y, int z, int height) {return helper.getMaterial(seed, x, y, z, height);}

        @Override public Material getMaterial(Platform platform, long seed, int x, int y, float z, int height) {return helper.getMaterial(seed, x, y, z, height);}

        @Override public String getName() {return helper.getName();}

        @Override public BufferedImage getIcon(ColourScheme colourScheme) {return helper.getIcon(colourScheme);}

        @Override public boolean isCustom() {return true;}

        @Override public boolean isConfigured() {return helper.isConfigured();}

        @Override public int getDefaultBiome() {return helper.getDefaultBiome();}

        @Override public int getCustomTerrainIndex() {return helper.getCustomTerrainIndex();}

        @Override public int getColour(long seed, int x, int y, int z, int height, Platform platform, ColourScheme colourScheme) {return helper.getColour(seed, x, y, z, height, colourScheme);}

        @Override public int getColour(long seed, int x, int y, float z, int height, Platform platform, ColourScheme colourScheme) {return helper.getColour(seed, x, y, z, height, colourScheme);}

        private final CustomTerrainHelper helper = new CustomTerrainHelper(3);
    },
    CUSTOM_5("Custom 5",                                  "custom material five", BIOME_PLAINS) {
        @Override public Material getMaterial(Platform platform, long seed, int x, int y, int z, int height) {return helper.getMaterial(seed, x, y, z, height);}

        @Override public Material getMaterial(Platform platform, long seed, int x, int y, float z, int height) {return helper.getMaterial(seed, x, y, z, height);}

        @Override public String getName() {return helper.getName();}

        @Override public BufferedImage getIcon(ColourScheme colourScheme) {return helper.getIcon(colourScheme);}

        @Override public boolean isCustom() {return true;}

        @Override public boolean isConfigured() {return helper.isConfigured();}

        @Override public int getDefaultBiome() {return helper.getDefaultBiome();}

        @Override public int getCustomTerrainIndex() {return helper.getCustomTerrainIndex();}

        @Override public int getColour(long seed, int x, int y, int z, int height, Platform platform, ColourScheme colourScheme) {return helper.getColour(seed, x, y, z, height, colourScheme);}

        @Override public int getColour(long seed, int x, int y, float z, int height, Platform platform, ColourScheme colourScheme) {return helper.getColour(seed, x, y, z, height, colourScheme);}

        private final CustomTerrainHelper helper = new CustomTerrainHelper(4);
    },
    MYCELIUM("Mycelium", BLK_MYCELIUM, BLK_DIRT, "mycelium", BIOME_MUSHROOM_ISLAND),
    END_STONE("End Stone", BLK_END_STONE, BLK_END_STONE, "end stone", BIOME_SKY),
    BARE_GRASS("Bare Grass", BLK_GRASS, BLK_GRASS, "bare grass (no flowers, etc.)", BIOME_PLAINS),
    CUSTOM_6("Custom 6",                                  "custom material six", BIOME_PLAINS) {
        @Override public Material getMaterial(Platform platform, long seed, int x, int y, int z, int height) {return helper.getMaterial(seed, x, y, z, height);}

        @Override public Material getMaterial(Platform platform, long seed, int x, int y, float z, int height) {return helper.getMaterial(seed, x, y, z, height);}

        @Override public String getName() {return helper.getName();}

        @Override public BufferedImage getIcon(ColourScheme colourScheme) {return helper.getIcon(colourScheme);}

        @Override public boolean isCustom() {return true;}

        @Override public boolean isConfigured() {return helper.isConfigured();}

        @Override public int getDefaultBiome() {return helper.getDefaultBiome();}

        @Override public int getCustomTerrainIndex() {return helper.getCustomTerrainIndex();}

        @Override public int getColour(long seed, int x, int y, int z, int height, Platform platform, ColourScheme colourScheme) {return helper.getColour(seed, x, y, z, height, colourScheme);}

        @Override public int getColour(long seed, int x, int y, float z, int height, Platform platform, ColourScheme colourScheme) {return helper.getColour(seed, x, y, z, height, colourScheme);}

        private final CustomTerrainHelper helper = new CustomTerrainHelper(5);
    },
    CUSTOM_7("Custom 7",                                  "custom material seven", BIOME_PLAINS) {
        @Override public Material getMaterial(Platform platform, long seed, int x, int y, int z, int height) {return helper.getMaterial(seed, x, y, z, height);}

        @Override public Material getMaterial(Platform platform, long seed, int x, int y, float z, int height) {return helper.getMaterial(seed, x, y, z, height);}

        @Override public String getName() {return helper.getName();}

        @Override public BufferedImage getIcon(ColourScheme colourScheme) {return helper.getIcon(colourScheme);}

        @Override public boolean isCustom() {return true;}

        @Override public boolean isConfigured() {return helper.isConfigured();}

        @Override public int getDefaultBiome() {return helper.getDefaultBiome();}

        @Override public int getCustomTerrainIndex() {return helper.getCustomTerrainIndex();}

        @Override public int getColour(long seed, int x, int y, int z, int height, Platform platform, ColourScheme colourScheme) {return helper.getColour(seed, x, y, z, height, colourScheme);}

        @Override public int getColour(long seed, int x, int y, float z, int height, Platform platform, ColourScheme colourScheme) {return helper.getColour(seed, x, y, z, height, colourScheme);}

        private final CustomTerrainHelper helper = new CustomTerrainHelper(6);
    },
    CUSTOM_8("Custom 8",                                  "custom material eight", BIOME_PLAINS) {
        @Override public Material getMaterial(Platform platform, long seed, int x, int y, int z, int height) {return helper.getMaterial(seed, x, y, z, height);}

        @Override public Material getMaterial(Platform platform, long seed, int x, int y, float z, int height) {return helper.getMaterial(seed, x, y, z, height);}

        @Override public String getName() {return helper.getName();}

        @Override public BufferedImage getIcon(ColourScheme colourScheme) {return helper.getIcon(colourScheme);}

        @Override public boolean isCustom() {return true;}

        @Override public boolean isConfigured() {return helper.isConfigured();}

        @Override public int getDefaultBiome() {return helper.getDefaultBiome();}

        @Override public int getCustomTerrainIndex() {return helper.getCustomTerrainIndex();}

        @Override public int getColour(long seed, int x, int y, int z, int height, Platform platform, ColourScheme colourScheme) {return helper.getColour(seed, x, y, z, height, colourScheme);}

        @Override public int getColour(long seed, int x, int y, float z, int height, Platform platform, ColourScheme colourScheme) {return helper.getColour(seed, x, y, z, height, colourScheme);}

        private final CustomTerrainHelper helper = new CustomTerrainHelper(7);
    },
    CUSTOM_9("Custom 9",                                  "custom material nine", BIOME_PLAINS) {
        @Override public Material getMaterial(Platform platform, long seed, int x, int y, int z, int height) {return helper.getMaterial(seed, x, y, z, height);}

        @Override public Material getMaterial(Platform platform, long seed, int x, int y, float z, int height) {return helper.getMaterial(seed, x, y, z, height);}

        @Override public String getName() {return helper.getName();}

        @Override public BufferedImage getIcon(ColourScheme colourScheme) {return helper.getIcon(colourScheme);}

        @Override public boolean isCustom() {return true;}

        @Override public boolean isConfigured() {return helper.isConfigured();}

        @Override public int getDefaultBiome() {return helper.getDefaultBiome();}

        @Override public int getCustomTerrainIndex() {return helper.getCustomTerrainIndex();}

        @Override public int getColour(long seed, int x, int y, int z, int height, Platform platform, ColourScheme colourScheme) {return helper.getColour(seed, x, y, z, height, colourScheme);}

        @Override public int getColour(long seed, int x, int y, float z, int height, Platform platform, ColourScheme colourScheme) {return helper.getColour(seed, x, y, z, height, colourScheme);}

        private final CustomTerrainHelper helper = new CustomTerrainHelper(8);
    },
    CUSTOM_10("Custom 10",                                "custom material ten", BIOME_PLAINS) {
        @Override public Material getMaterial(Platform platform, long seed, int x, int y, int z, int height) {return helper.getMaterial(seed, x, y, z, height);}

        @Override public Material getMaterial(Platform platform, long seed, int x, int y, float z, int height) {return helper.getMaterial(seed, x, y, z, height);}

        @Override public String getName() {return helper.getName();}

        @Override public BufferedImage getIcon(ColourScheme colourScheme) {return helper.getIcon(colourScheme);}

        @Override public boolean isCustom() {return true;}

        @Override public boolean isConfigured() {return helper.isConfigured();}

        @Override public int getDefaultBiome() {return helper.getDefaultBiome();}

        @Override public int getCustomTerrainIndex() {return helper.getCustomTerrainIndex();}

        @Override public int getColour(long seed, int x, int y, int z, int height, Platform platform, ColourScheme colourScheme) {return helper.getColour(seed, x, y, z, height, colourScheme);}

        @Override public int getColour(long seed, int x, int y, float z, int height, Platform platform, ColourScheme colourScheme) {return helper.getColour(seed, x, y, z, height, colourScheme);}

        private final CustomTerrainHelper helper = new CustomTerrainHelper(9);
    },
    CUSTOM_11("Custom 11",                                "custom material eleven", BIOME_PLAINS) {
        @Override public Material getMaterial(Platform platform, long seed, int x, int y, int z, int height) {return helper.getMaterial(seed, x, y, z, height);}

        @Override public Material getMaterial(Platform platform, long seed, int x, int y, float z, int height) {return helper.getMaterial(seed, x, y, z, height);}

        @Override public String getName() {return helper.getName();}

        @Override public BufferedImage getIcon(ColourScheme colourScheme) {return helper.getIcon(colourScheme);}

        @Override public boolean isCustom() {return true;}

        @Override public boolean isConfigured() {return helper.isConfigured();}

        @Override public int getDefaultBiome() {return helper.getDefaultBiome();}

        @Override public int getCustomTerrainIndex() {return helper.getCustomTerrainIndex();}

        @Override public int getColour(long seed, int x, int y, int z, int height, Platform platform, ColourScheme colourScheme) {return helper.getColour(seed, x, y, z, height, colourScheme);}

        @Override public int getColour(long seed, int x, int y, float z, int height, Platform platform, ColourScheme colourScheme) {return helper.getColour(seed, x, y, z, height, colourScheme);}

        private final CustomTerrainHelper helper = new CustomTerrainHelper(10);
    },
    CUSTOM_12("Custom 12",                                "custom material twelve", BIOME_PLAINS) {
        @Override public Material getMaterial(Platform platform, long seed, int x, int y, int z, int height) {return helper.getMaterial(seed, x, y, z, height);}

        @Override public Material getMaterial(Platform platform, long seed, int x, int y, float z, int height) {return helper.getMaterial(seed, x, y, z, height);}

        @Override public String getName() {return helper.getName();}

        @Override public BufferedImage getIcon(ColourScheme colourScheme) {return helper.getIcon(colourScheme);}

        @Override public boolean isCustom() {return true;}

        @Override public boolean isConfigured() {return helper.isConfigured();}

        @Override public int getDefaultBiome() {return helper.getDefaultBiome();}

        @Override public int getCustomTerrainIndex() {return helper.getCustomTerrainIndex();}

        @Override public int getColour(long seed, int x, int y, int z, int height, Platform platform, ColourScheme colourScheme) {return helper.getColour(seed, x, y, z, height, colourScheme);}

        @Override public int getColour(long seed, int x, int y, float z, int height, Platform platform, ColourScheme colourScheme) {return helper.getColour(seed, x, y, z, height, colourScheme);}

        private final CustomTerrainHelper helper = new CustomTerrainHelper(11);
    },
    CUSTOM_13("Custom 13",                                "custom material thirteen", BIOME_PLAINS) {
        @Override public Material getMaterial(Platform platform, long seed, int x, int y, int z, int height) {return helper.getMaterial(seed, x, y, z, height);}

        @Override public Material getMaterial(Platform platform, long seed, int x, int y, float z, int height) {return helper.getMaterial(seed, x, y, z, height);}

        @Override public String getName() {return helper.getName();}

        @Override public BufferedImage getIcon(ColourScheme colourScheme) {return helper.getIcon(colourScheme);}

        @Override public boolean isCustom() {return true;}

        @Override public boolean isConfigured() {return helper.isConfigured();}

        @Override public int getDefaultBiome() {return helper.getDefaultBiome();}

        @Override public int getCustomTerrainIndex() {return helper.getCustomTerrainIndex();}

        @Override public int getColour(long seed, int x, int y, int z, int height, Platform platform, ColourScheme colourScheme) {return helper.getColour(seed, x, y, z, height, colourScheme);}

        @Override public int getColour(long seed, int x, int y, float z, int height, Platform platform, ColourScheme colourScheme) {return helper.getColour(seed, x, y, z, height, colourScheme);}

        private final CustomTerrainHelper helper = new CustomTerrainHelper(12);
    },
    CUSTOM_14("Custom 14",                                "custom material fourteen", BIOME_PLAINS) {
        @Override public Material getMaterial(Platform platform, long seed, int x, int y, int z, int height) {return helper.getMaterial(seed, x, y, z, height);}

        @Override public Material getMaterial(Platform platform, long seed, int x, int y, float z, int height) {return helper.getMaterial(seed, x, y, z, height);}

        @Override public String getName() {return helper.getName();}

        @Override public BufferedImage getIcon(ColourScheme colourScheme) {return helper.getIcon(colourScheme);}

        @Override public boolean isCustom() {return true;}

        @Override public boolean isConfigured() {return helper.isConfigured();}

        @Override public int getDefaultBiome() {return helper.getDefaultBiome();}

        @Override public int getCustomTerrainIndex() {return helper.getCustomTerrainIndex();}

        @Override public int getColour(long seed, int x, int y, int z, int height, Platform platform, ColourScheme colourScheme) {return helper.getColour(seed, x, y, z, height, colourScheme);}

        @Override public int getColour(long seed, int x, int y, float z, int height, Platform platform, ColourScheme colourScheme) {return helper.getColour(seed, x, y, z, height, colourScheme);}

        private final CustomTerrainHelper helper = new CustomTerrainHelper(13);
    },
    CUSTOM_15("Custom 15",                                "custom material fifteen", BIOME_PLAINS) {
        @Override public Material getMaterial(Platform platform, long seed, int x, int y, int z, int height) {return helper.getMaterial(seed, x, y, z, height);}

        @Override public Material getMaterial(Platform platform, long seed, int x, int y, float z, int height) {return helper.getMaterial(seed, x, y, z, height);}

        @Override public String getName() {return helper.getName();}

        @Override public BufferedImage getIcon(ColourScheme colourScheme) {return helper.getIcon(colourScheme);}

        @Override public boolean isCustom() {return true;}

        @Override public boolean isConfigured() {return helper.isConfigured();}

        @Override public int getDefaultBiome() {return helper.getDefaultBiome();}

        @Override public int getCustomTerrainIndex() {return helper.getCustomTerrainIndex();}

        @Override public int getColour(long seed, int x, int y, int z, int height, Platform platform, ColourScheme colourScheme) {return helper.getColour(seed, x, y, z, height, colourScheme);}

        @Override public int getColour(long seed, int x, int y, float z, int height, Platform platform, ColourScheme colourScheme) {return helper.getColour(seed, x, y, z, height, colourScheme);}

        private final CustomTerrainHelper helper = new CustomTerrainHelper(14);
    },
    CUSTOM_16("Custom 16",                                "custom material sixteen", BIOME_PLAINS) {
        @Override public Material getMaterial(Platform platform, long seed, int x, int y, int z, int height) {return helper.getMaterial(seed, x, y, z, height);}

        @Override public Material getMaterial(Platform platform, long seed, int x, int y, float z, int height) {return helper.getMaterial(seed, x, y, z, height);}

        @Override public String getName() {return helper.getName();}

        @Override public BufferedImage getIcon(ColourScheme colourScheme) {return helper.getIcon(colourScheme);}

        @Override public boolean isCustom() {return true;}

        @Override public boolean isConfigured() {return helper.isConfigured();}

        @Override public int getDefaultBiome() {return helper.getDefaultBiome();}

        @Override public int getCustomTerrainIndex() {return helper.getCustomTerrainIndex();}

        @Override public int getColour(long seed, int x, int y, int z, int height, Platform platform, ColourScheme colourScheme) {return helper.getColour(seed, x, y, z, height, colourScheme);}

        @Override public int getColour(long seed, int x, int y, float z, int height, Platform platform, ColourScheme colourScheme) {return helper.getColour(seed, x, y, z, height, colourScheme);}

        private final CustomTerrainHelper helper = new CustomTerrainHelper(15);
    },
    CUSTOM_17("Custom 17",                                "custom material seventeen", BIOME_PLAINS) {
        @Override public Material getMaterial(Platform platform, long seed, int x, int y, int z, int height) {return helper.getMaterial(seed, x, y, z, height);}

        @Override public Material getMaterial(Platform platform, long seed, int x, int y, float z, int height) {return helper.getMaterial(seed, x, y, z, height);}

        @Override public String getName() {return helper.getName();}

        @Override public BufferedImage getIcon(ColourScheme colourScheme) {return helper.getIcon(colourScheme);}

        @Override public boolean isCustom() {return true;}

        @Override public boolean isConfigured() {return helper.isConfigured();}

        @Override public int getDefaultBiome() {return helper.getDefaultBiome();}

        @Override public int getCustomTerrainIndex() {return helper.getCustomTerrainIndex();}

        @Override public int getColour(long seed, int x, int y, int z, int height, Platform platform, ColourScheme colourScheme) {return helper.getColour(seed, x, y, z, height, colourScheme);}

        @Override public int getColour(long seed, int x, int y, float z, int height, Platform platform, ColourScheme colourScheme) {return helper.getColour(seed, x, y, z, height, colourScheme);}

        private final CustomTerrainHelper helper = new CustomTerrainHelper(16);
    },
    CUSTOM_18("Custom 18",                                "custom material eighteen", BIOME_PLAINS) {
        @Override public Material getMaterial(Platform platform, long seed, int x, int y, int z, int height) {return helper.getMaterial(seed, x, y, z, height);}

        @Override public Material getMaterial(Platform platform, long seed, int x, int y, float z, int height) {return helper.getMaterial(seed, x, y, z, height);}

        @Override public String getName() {return helper.getName();}

        @Override public BufferedImage getIcon(ColourScheme colourScheme) {return helper.getIcon(colourScheme);}

        @Override public boolean isCustom() {return true;}

        @Override public boolean isConfigured() {return helper.isConfigured();}

        @Override public int getDefaultBiome() {return helper.getDefaultBiome();}

        @Override public int getCustomTerrainIndex() {return helper.getCustomTerrainIndex();}

        @Override public int getColour(long seed, int x, int y, int z, int height, Platform platform, ColourScheme colourScheme) {return helper.getColour(seed, x, y, z, height, colourScheme);}

        @Override public int getColour(long seed, int x, int y, float z, int height, Platform platform, ColourScheme colourScheme) {return helper.getColour(seed, x, y, z, height, colourScheme);}

        private final CustomTerrainHelper helper = new CustomTerrainHelper(17);
    },
    CUSTOM_19("Custom 19",                                "custom material nineteen", BIOME_PLAINS) {
        @Override public Material getMaterial(Platform platform, long seed, int x, int y, int z, int height) {return helper.getMaterial(seed, x, y, z, height);}

        @Override public Material getMaterial(Platform platform, long seed, int x, int y, float z, int height) {return helper.getMaterial(seed, x, y, z, height);}

        @Override public String getName() {return helper.getName();}

        @Override public BufferedImage getIcon(ColourScheme colourScheme) {return helper.getIcon(colourScheme);}

        @Override public boolean isCustom() {return true;}

        @Override public boolean isConfigured() {return helper.isConfigured();}

        @Override public int getDefaultBiome() {return helper.getDefaultBiome();}

        @Override public int getCustomTerrainIndex() {return helper.getCustomTerrainIndex();}

        @Override public int getColour(long seed, int x, int y, int z, int height, Platform platform, ColourScheme colourScheme) {return helper.getColour(seed, x, y, z, height, colourScheme);}

        @Override public int getColour(long seed, int x, int y, float z, int height, Platform platform, ColourScheme colourScheme) {return helper.getColour(seed, x, y, z, height, colourScheme);}

        private final CustomTerrainHelper helper = new CustomTerrainHelper(18);
    },
    CUSTOM_20("Custom 20",                                "custom material twenty", BIOME_PLAINS) {
        @Override public Material getMaterial(Platform platform, long seed, int x, int y, int z, int height) {return helper.getMaterial(seed, x, y, z, height);}

        @Override public Material getMaterial(Platform platform, long seed, int x, int y, float z, int height) {return helper.getMaterial(seed, x, y, z, height);}

        @Override public String getName() {return helper.getName();}

        @Override public BufferedImage getIcon(ColourScheme colourScheme) {return helper.getIcon(colourScheme);}

        @Override public boolean isCustom() {return true;}

        @Override public boolean isConfigured() {return helper.isConfigured();}

        @Override public int getDefaultBiome() {return helper.getDefaultBiome();}

        @Override public int getCustomTerrainIndex() {return helper.getCustomTerrainIndex();}

        @Override public int getColour(long seed, int x, int y, int z, int height, Platform platform, ColourScheme colourScheme) {return helper.getColour(seed, x, y, z, height, colourScheme);}

        @Override public int getColour(long seed, int x, int y, float z, int height, Platform platform, ColourScheme colourScheme) {return helper.getColour(seed, x, y, z, height, colourScheme);}

        private final CustomTerrainHelper helper = new CustomTerrainHelper(19);
    },
    CUSTOM_21("Custom 21",                                "custom material twenty-one", BIOME_PLAINS) {
        @Override public Material getMaterial(Platform platform, long seed, int x, int y, int z, int height) {return helper.getMaterial(seed, x, y, z, height);}

        @Override public Material getMaterial(Platform platform, long seed, int x, int y, float z, int height) {return helper.getMaterial(seed, x, y, z, height);}

        @Override public String getName() {return helper.getName();}

        @Override public BufferedImage getIcon(ColourScheme colourScheme) {return helper.getIcon(colourScheme);}

        @Override public boolean isCustom() {return true;}

        @Override public boolean isConfigured() {return helper.isConfigured();}

        @Override public int getDefaultBiome() {return helper.getDefaultBiome();}

        @Override public int getCustomTerrainIndex() {return helper.getCustomTerrainIndex();}

        @Override public int getColour(long seed, int x, int y, int z, int height, Platform platform, ColourScheme colourScheme) {return helper.getColour(seed, x, y, z, height, colourScheme);}

        @Override public int getColour(long seed, int x, int y, float z, int height, Platform platform, ColourScheme colourScheme) {return helper.getColour(seed, x, y, z, height, colourScheme);}

        private final CustomTerrainHelper helper = new CustomTerrainHelper(20);
    },
    CUSTOM_22("Custom 22",                                "custom material twenty-two", BIOME_PLAINS) {
        @Override public Material getMaterial(Platform platform, long seed, int x, int y, int z, int height) {return helper.getMaterial(seed, x, y, z, height);}

        @Override public Material getMaterial(Platform platform, long seed, int x, int y, float z, int height) {return helper.getMaterial(seed, x, y, z, height);}

        @Override public String getName() {return helper.getName();}

        @Override public BufferedImage getIcon(ColourScheme colourScheme) {return helper.getIcon(colourScheme);}

        @Override public boolean isCustom() {return true;}

        @Override public boolean isConfigured() {return helper.isConfigured();}

        @Override public int getDefaultBiome() {return helper.getDefaultBiome();}

        @Override public int getCustomTerrainIndex() {return helper.getCustomTerrainIndex();}

        @Override public int getColour(long seed, int x, int y, int z, int height, Platform platform, ColourScheme colourScheme) {return helper.getColour(seed, x, y, z, height, colourScheme);}

        @Override public int getColour(long seed, int x, int y, float z, int height, Platform platform, ColourScheme colourScheme) {return helper.getColour(seed, x, y, z, height, colourScheme);}

        private final CustomTerrainHelper helper = new CustomTerrainHelper(21);
    },
    CUSTOM_23("Custom 23",                                "custom material twenty-three", BIOME_PLAINS) {
        @Override public Material getMaterial(Platform platform, long seed, int x, int y, int z, int height) {return helper.getMaterial(seed, x, y, z, height);}

        @Override public Material getMaterial(Platform platform, long seed, int x, int y, float z, int height) {return helper.getMaterial(seed, x, y, z, height);}

        @Override public String getName() {return helper.getName();}

        @Override public BufferedImage getIcon(ColourScheme colourScheme) {return helper.getIcon(colourScheme);}

        @Override public boolean isCustom() {return true;}

        @Override public boolean isConfigured() {return helper.isConfigured();}

        @Override public int getDefaultBiome() {return helper.getDefaultBiome();}

        @Override public int getCustomTerrainIndex() {return helper.getCustomTerrainIndex();}

        @Override public int getColour(long seed, int x, int y, int z, int height, Platform platform, ColourScheme colourScheme) {return helper.getColour(seed, x, y, z, height, colourScheme);}

        @Override public int getColour(long seed, int x, int y, float z, int height, Platform platform, ColourScheme colourScheme) {return helper.getColour(seed, x, y, z, height, colourScheme);}

        private final CustomTerrainHelper helper = new CustomTerrainHelper(22);
    },
    CUSTOM_24("Custom 24",                                "custom material twenty-four", BIOME_PLAINS) {
        @Override public Material getMaterial(Platform platform, long seed, int x, int y, int z, int height) {return helper.getMaterial(seed, x, y, z, height);}

        @Override public Material getMaterial(Platform platform, long seed, int x, int y, float z, int height) {return helper.getMaterial(seed, x, y, z, height);}

        @Override public String getName() {return helper.getName();}

        @Override public BufferedImage getIcon(ColourScheme colourScheme) {return helper.getIcon(colourScheme);}

        @Override public boolean isCustom() {return true;}

        @Override public boolean isConfigured() {return helper.isConfigured();}

        @Override public int getDefaultBiome() {return helper.getDefaultBiome();}

        @Override public int getCustomTerrainIndex() {return helper.getCustomTerrainIndex();}

        @Override public int getColour(long seed, int x, int y, int z, int height, Platform platform, ColourScheme colourScheme) {return helper.getColour(seed, x, y, z, height, colourScheme);}

        @Override public int getColour(long seed, int x, int y, float z, int height, Platform platform, ColourScheme colourScheme) {return helper.getColour(seed, x, y, z, height, colourScheme);}

        private final CustomTerrainHelper helper = new CustomTerrainHelper(23);
    },
    PERMADIRT("Coarse Dirt", Material.PERMADIRT, Material.PERMADIRT, "dirt on which no grass will grow", BIOME_PLAINS),
    PODZOL("Podzol", Material.PODZOL, Material.DIRT, "podzol", BIOME_PLAINS),
    RED_SAND("Red Sand", Material.RED_SAND, Material.RED_SAND, "red sand", BIOME_MESA),
    HARDENED_CLAY("Terracotta", Material.HARDENED_CLAY, Material.HARDENED_CLAY, "terracotta", BIOME_MESA),
    WHITE_STAINED_CLAY("White Stained Terracotta", Material.WHITE_CLAY, Material.WHITE_CLAY, "white stained terracotta", BIOME_MESA),
    ORANGE_STAINED_CLAY("Orange Stained Terracotta", Material.ORANGE_CLAY, Material.ORANGE_CLAY, "orange stained terracotta", BIOME_MESA),
    MAGENTA_STAINED_CLAY("Magenta Stained Terracotta", Material.MAGENTA_CLAY, Material.MAGENTA_CLAY, "magenta stained terracotta", BIOME_PLAINS),
    LIGHT_BLUE_STAINED_CLAY("Light Blue Stained Terracotta", Material.LIGHT_BLUE_CLAY, Material.LIGHT_BLUE_CLAY, "light blue stained terracotta", BIOME_PLAINS),
    YELLOW_STAINED_CLAY("Yellow Stained Terracotta", Material.YELLOW_CLAY, Material.YELLOW_CLAY, "yellow stained terracotta", BIOME_MESA),
    LIME_STAINED_CLAY("Lime Stained Terracotta", Material.LIME_CLAY, Material.LIME_CLAY, "lime stained terracotta", BIOME_PLAINS),
    PINK_STAINED_CLAY("Pink Stained Terracotta", Material.PINK_CLAY, Material.PINK_CLAY, "pink stained terracotta", BIOME_PLAINS),
    GREY_STAINED_CLAY("Grey Stained Terracotta", Material.GREY_CLAY, Material.GREY_CLAY, "grey stained terracotta", BIOME_PLAINS),
    LIGHT_GREY_STAINED_CLAY("Light Grey Stained Terracotta", Material.LIGHT_GREY_CLAY, Material.LIGHT_GREY_CLAY, "light grey stained terracotta", BIOME_MESA),
    CYAN_STAINED_CLAY("Cyan Stained Terracotta", Material.CYAN_CLAY, Material.CYAN_CLAY, "cyan stained terracotta", BIOME_PLAINS),
    PURPLE_STAINED_CLAY("Purple Stained Terracotta", Material.PURPLE_CLAY, Material.PURPLE_CLAY, "purple stained terracotta", BIOME_PLAINS),
    BLUE_STAINED_CLAY("Blue Stained Terracotta", Material.BLUE_CLAY, Material.BLUE_CLAY, "blue stained terracotta", BIOME_PLAINS),
    BROWN_STAINED_CLAY("Brown Stained Terracotta", Material.BROWN_CLAY, Material.BROWN_CLAY, "brown stained terracotta", BIOME_MESA),
    GREEN_STAINED_CLAY("Green Stained Terracotta", Material.GREEN_CLAY, Material.GREEN_CLAY, "green stained terracotta", BIOME_PLAINS),
    RED_STAINED_CLAY("Red Stained Terracotta", Material.RED_CLAY, Material.RED_CLAY, "red stained terracotta", BIOME_MESA),
    BLACK_STAINED_CLAY("Black Stained Terracotta", Material.BLACK_CLAY, Material.BLACK_CLAY, "black stained terracotta", BIOME_PLAINS),
    MESA("Mesa", "layers of red sand, hardened clay and stained clay, with here and there a dead shrub", BIOME_MESA, 1) {
        @Override
        public Material getMaterial(Platform platform, final long seed, final int x, final int y, final int z, final int height) {
            return getMaterial(platform, seed, x, y, (float) z, height);
        }

        @Override
        public Material getMaterial(Platform platform, final long seed, final int x, final int y, final float z, final int height) {
            if (seed != this.seed) {
                init(seed);
            }
            return LAYERS[mod((int) (z + (perlinNoise.getPerlinNoise(x / GIGANTIC_BLOBS, y / GIGANTIC_BLOBS) * 4 + perlinNoise.getPerlinNoise(x / HUGE_BLOBS, y / HUGE_BLOBS) + perlinNoise.getPerlinNoise(x / LARGE_BLOBS, y / LARGE_BLOBS) + perlinNoise.getPerlinNoise(x / SMALL_BLOBS, y / SMALL_BLOBS) / 4 + 3.125f) * 8), LAYER_COUNT)];
        }

        @Override
        public WPObject getSurfaceObject(Platform platform, long seed, int x, int y, int waterBlocksAbove) {
            if (waterBlocksAbove > 0) {
                return null;
            }
            final int rnd = new Random(seed + (x * 65537L) + (y * 4099L)).nextInt(SHRUB_CHANCE);
            if (rnd < 3) {
                return Plants.DEAD_SHRUB.realise(1, platform);
            } else {
                return null;
            }
        }

        private void init(long seed) {
            this.seed = seed;
            perlinNoise.setSeed(seed + NOISE_SEED_OFFSET);
            final Random random = new Random(seed);
            Arrays.fill(LAYERS, Material.HARDENED_CLAY);
            for (int i = 0; i < LAYER_COUNT / 2; i++) {
                final int index = random.nextInt(LAYER_COUNT - 1);
                final Material material = MATERIALS[random.nextInt(MATERIALS.length)];
                LAYERS[index] = material;
                LAYERS[index + 1] = material;
            }
        }

        private final Material[] LAYERS = new Material[LAYER_COUNT];
        private final PerlinNoise perlinNoise = new PerlinNoise(0);
        private long seed = Long.MIN_VALUE;

        private final Material[] MATERIALS = {Material.RED_SAND, Material.HARDENED_CLAY, Material.WHITE_CLAY, Material.LIGHT_GREY_CLAY, Material.YELLOW_CLAY, Material.ORANGE_CLAY, Material.RED_CLAY, Material.BROWN_CLAY};

        private static final int LAYER_COUNT = 64;
        private static final int SHRUB_CHANCE = 500;
        private static final long NOISE_SEED_OFFSET = 110335839L;
    },
    RED_DESERT("Red Desert", Material.RED_SAND, Material.RED_SAND, "red sand with here and there a cactus or dead shrub", BIOME_MESA) {
        @Override
        public WPObject getSurfaceObject(Platform platform, long seed, int x, int y, int waterBlocksAbove) {
            if (waterBlocksAbove > 0) {
                return null;
            }
            final int rnd = new Random(seed + (x * 65537L) + (y * 4099L)).nextInt(CACTUS_CHANCE);
            final int cactusHeight;
            boolean shrub = false;
            if (rnd < 3) {
                cactusHeight = rnd + 1;
            } else {
                cactusHeight = 0;
                if (rnd < 12) {
                    shrub = true;
                }
            }
            if (shrub) {
                return Plants.DEAD_SHRUB.realise(1, platform);
            } else if (cactusHeight == 0) {
                return null;
            } else {
                return Plants.CACTUS.realise(cactusHeight, platform);
            }
        }

        private static final int CACTUS_CHANCE = 2000;
    },
    RED_SANDSTONE("Red Sandstone", BLK_RED_SANDSTONE, BLK_RED_SANDSTONE, "red sandstone", BIOME_MESA),
    GRANITE("Granite", Material.GRANITE, Material.GRANITE, "granite", BIOME_PLAINS),
    DIORITE("Diorite", Material.DIORITE, Material.DIORITE, "diorite", BIOME_PLAINS),
    ANDESITE("Andesite", Material.ANDESITE, Material.ANDESITE, "andesite", BIOME_PLAINS),
    STONE_MIX("Stone Mix", "stone or deepslate with patches of granite, diorite, andesite and tuff", BIOME_PLAINS) {
        @Override
        public Material getMaterial(Platform platform, long seed, int x, int y, int z, int height) {
            if (graniteNoise.getSeed() != (seed + GRANITE_SEED_OFFSET)) {
                graniteNoise.setSeed(seed + GRANITE_SEED_OFFSET);
                dioriteNoise.setSeed(seed + DIORITE_SEED_OFFSET);
                andesiteNoise.setSeed(seed + ANDESITE_SEED_OFFSET);
                RANDOM.setSeed(seed);
            }
            if (z>0 || ( z>-5 && z >= -RANDOM.nextInt(5))) { // TODO this is not stable
                if (graniteNoise.getPerlinNoise(x / SMALL_BLOBS, y / SMALL_BLOBS, z / SMALL_BLOBS) > GRANITE_CHANCE) {
                    return Material.GRANITE;
                } else if(dioriteNoise.getPerlinNoise(x / SMALL_BLOBS, y / SMALL_BLOBS, z / SMALL_BLOBS) > DIORITE_CHANCE) {
                    return Material.DIORITE;
                } else if(andesiteNoise.getPerlinNoise(x / SMALL_BLOBS, y / SMALL_BLOBS, z / SMALL_BLOBS) > ANDESITE_CHANCE) {
                    return Material.ANDESITE;
                } else {
                    return Material.STONE;
                }
            } else {
                if (graniteNoise.getPerlinNoise(x / SMALL_BLOBS, y / SMALL_BLOBS, z / SMALL_BLOBS) > GRANITE_CHANCE) {
                    return Material.TUFF;
                } else if(dioriteNoise.getPerlinNoise(x / SMALL_BLOBS, y / SMALL_BLOBS, z / SMALL_BLOBS) > DIORITE_CHANCE) {
                    return Material.DEEPSLATE_X;
                } else if(andesiteNoise.getPerlinNoise(x / SMALL_BLOBS, y / SMALL_BLOBS, z / SMALL_BLOBS) > ANDESITE_CHANCE) {
                    return Material.DEEPSLATE_Z;
                } else {
                    return Material.DEEPSLATE_Y;
                }
            }
        }

        private final PerlinNoise graniteNoise  = new PerlinNoise(0);
        private final PerlinNoise dioriteNoise  = new PerlinNoise(0);
        private final PerlinNoise andesiteNoise = new PerlinNoise(0);

        private static final int GRANITE_SEED_OFFSET  = 145827825;
        private static final int DIORITE_SEED_OFFSET  =  59606124;
        private static final int ANDESITE_SEED_OFFSET =  87772192;

        private final Random RANDOM = new Random();
    },
    CUSTOM_25("Custom 25",                                  "custom material twenty-five", BIOME_PLAINS) {
        @Override public Material getMaterial(Platform platform, long seed, int x, int y, int z, int height) {return helper.getMaterial(seed, x, y, z, height);}

        @Override public Material getMaterial(Platform platform, long seed, int x, int y, float z, int height) {return helper.getMaterial(seed, x, y, z, height);}

        @Override public String getName() {return helper.getName();}

        @Override public BufferedImage getIcon(ColourScheme colourScheme) {return helper.getIcon(colourScheme);}

        @Override public boolean isCustom() {return true;}

        @Override public boolean isConfigured() {return helper.isConfigured();}

        @Override public int getDefaultBiome() {return helper.getDefaultBiome();}

        @Override public int getCustomTerrainIndex() {return helper.getCustomTerrainIndex();}

        @Override public int getColour(long seed, int x, int y, int z, int height, Platform platform, ColourScheme colourScheme) {return helper.getColour(seed, x, y, z, height, colourScheme);}

        @Override public int getColour(long seed, int x, int y, float z, int height, Platform platform, ColourScheme colourScheme) {return helper.getColour(seed, x, y, z, height, colourScheme);}

        private final CustomTerrainHelper helper = new CustomTerrainHelper(24);
    },
    CUSTOM_26("Custom 26",                                  "custom material twenty-six", BIOME_PLAINS) {
        @Override public Material getMaterial(Platform platform, long seed, int x, int y, int z, int height) {return helper.getMaterial(seed, x, y, z, height);}

        @Override public Material getMaterial(Platform platform, long seed, int x, int y, float z, int height) {return helper.getMaterial(seed, x, y, z, height);}

        @Override public String getName() {return helper.getName();}

        @Override public BufferedImage getIcon(ColourScheme colourScheme) {return helper.getIcon(colourScheme);}

        @Override public boolean isCustom() {return true;}

        @Override public boolean isConfigured() {return helper.isConfigured();}

        @Override public int getDefaultBiome() {return helper.getDefaultBiome();}

        @Override public int getCustomTerrainIndex() {return helper.getCustomTerrainIndex();}

        @Override public int getColour(long seed, int x, int y, int z, int height, Platform platform, ColourScheme colourScheme) {return helper.getColour(seed, x, y, z, height, colourScheme);}

        @Override public int getColour(long seed, int x, int y, float z, int height, Platform platform, ColourScheme colourScheme) {return helper.getColour(seed, x, y, z, height, colourScheme);}

        private final CustomTerrainHelper helper = new CustomTerrainHelper(25);
    },
    CUSTOM_27("Custom 27",                                  "custom material twenty-seven", BIOME_PLAINS) {
        @Override public Material getMaterial(Platform platform, long seed, int x, int y, int z, int height) {return helper.getMaterial(seed, x, y, z, height);}

        @Override public Material getMaterial(Platform platform, long seed, int x, int y, float z, int height) {return helper.getMaterial(seed, x, y, z, height);}

        @Override public String getName() {return helper.getName();}

        @Override public BufferedImage getIcon(ColourScheme colourScheme) {return helper.getIcon(colourScheme);}

        @Override public boolean isCustom() {return true;}

        @Override public boolean isConfigured() {return helper.isConfigured();}

        @Override public int getDefaultBiome() {return helper.getDefaultBiome();}

        @Override public int getCustomTerrainIndex() {return helper.getCustomTerrainIndex();}

        @Override public int getColour(long seed, int x, int y, int z, int height, Platform platform, ColourScheme colourScheme) {return helper.getColour(seed, x, y, z, height, colourScheme);}

        @Override public int getColour(long seed, int x, int y, float z, int height, Platform platform, ColourScheme colourScheme) {return helper.getColour(seed, x, y, z, height, colourScheme);}

        private final CustomTerrainHelper helper = new CustomTerrainHelper(26);
    },
    CUSTOM_28("Custom 28",                                  "custom material twenty-eight", BIOME_PLAINS) {
        @Override public Material getMaterial(Platform platform, long seed, int x, int y, int z, int height) {return helper.getMaterial(seed, x, y, z, height);}

        @Override public Material getMaterial(Platform platform, long seed, int x, int y, float z, int height) {return helper.getMaterial(seed, x, y, z, height);}

        @Override public String getName() {return helper.getName();}

        @Override public BufferedImage getIcon(ColourScheme colourScheme) {return helper.getIcon(colourScheme);}

        @Override public boolean isCustom() {return true;}

        @Override public boolean isConfigured() {return helper.isConfigured();}

        @Override public int getDefaultBiome() {return helper.getDefaultBiome();}

        @Override public int getCustomTerrainIndex() {return helper.getCustomTerrainIndex();}

        @Override public int getColour(long seed, int x, int y, int z, int height, Platform platform, ColourScheme colourScheme) {return helper.getColour(seed, x, y, z, height, colourScheme);}

        @Override public int getColour(long seed, int x, int y, float z, int height, Platform platform, ColourScheme colourScheme) {return helper.getColour(seed, x, y, z, height, colourScheme);}

        private final CustomTerrainHelper helper = new CustomTerrainHelper(27);
    },
    CUSTOM_29("Custom 29",                                  "custom material twenty-nine", BIOME_PLAINS) {
        @Override public Material getMaterial(Platform platform, long seed, int x, int y, int z, int height) {return helper.getMaterial(seed, x, y, z, height);}

        @Override public Material getMaterial(Platform platform, long seed, int x, int y, float z, int height) {return helper.getMaterial(seed, x, y, z, height);}

        @Override public String getName() {return helper.getName();}

        @Override public BufferedImage getIcon(ColourScheme colourScheme) {return helper.getIcon(colourScheme);}

        @Override public boolean isCustom() {return true;}

        @Override public boolean isConfigured() {return helper.isConfigured();}

        @Override public int getDefaultBiome() {return helper.getDefaultBiome();}

        @Override public int getCustomTerrainIndex() {return helper.getCustomTerrainIndex();}

        @Override public int getColour(long seed, int x, int y, int z, int height, Platform platform, ColourScheme colourScheme) {return helper.getColour(seed, x, y, z, height, colourScheme);}

        @Override public int getColour(long seed, int x, int y, float z, int height, Platform platform, ColourScheme colourScheme) {return helper.getColour(seed, x, y, z, height, colourScheme);}

        private final CustomTerrainHelper helper = new CustomTerrainHelper(28);
    },
    CUSTOM_30("Custom 30",                                  "custom material thirty", BIOME_PLAINS) {
        @Override public Material getMaterial(Platform platform, long seed, int x, int y, int z, int height) {return helper.getMaterial(seed, x, y, z, height);}

        @Override public Material getMaterial(Platform platform, long seed, int x, int y, float z, int height) {return helper.getMaterial(seed, x, y, z, height);}

        @Override public String getName() {return helper.getName();}

        @Override public BufferedImage getIcon(ColourScheme colourScheme) {return helper.getIcon(colourScheme);}

        @Override public boolean isCustom() {return true;}

        @Override public boolean isConfigured() {return helper.isConfigured();}

        @Override public int getDefaultBiome() {return helper.getDefaultBiome();}

        @Override public int getCustomTerrainIndex() {return helper.getCustomTerrainIndex();}

        @Override public int getColour(long seed, int x, int y, int z, int height, Platform platform, ColourScheme colourScheme) {return helper.getColour(seed, x, y, z, height, colourScheme);}

        @Override public int getColour(long seed, int x, int y, float z, int height, Platform platform, ColourScheme colourScheme) {return helper.getColour(seed, x, y, z, height, colourScheme);}

        private final CustomTerrainHelper helper = new CustomTerrainHelper(29);
    },
    CUSTOM_31("Custom 31",                                  "custom material thirty-one", BIOME_PLAINS) {
        @Override public Material getMaterial(Platform platform, long seed, int x, int y, int z, int height) {return helper.getMaterial(seed, x, y, z, height);}

        @Override public Material getMaterial(Platform platform, long seed, int x, int y, float z, int height) {return helper.getMaterial(seed, x, y, z, height);}

        @Override public String getName() {return helper.getName();}

        @Override public BufferedImage getIcon(ColourScheme colourScheme) {return helper.getIcon(colourScheme);}

        @Override public boolean isCustom() {return true;}

        @Override public boolean isConfigured() {return helper.isConfigured();}

        @Override public int getDefaultBiome() {return helper.getDefaultBiome();}

        @Override public int getCustomTerrainIndex() {return helper.getCustomTerrainIndex();}

        @Override public int getColour(long seed, int x, int y, int z, int height, Platform platform, ColourScheme colourScheme) {return helper.getColour(seed, x, y, z, height, colourScheme);}

        @Override public int getColour(long seed, int x, int y, float z, int height, Platform platform, ColourScheme colourScheme) {return helper.getColour(seed, x, y, z, height, colourScheme);}

        private final CustomTerrainHelper helper = new CustomTerrainHelper(30);
    },
    CUSTOM_32("Custom 32",                                  "custom material thirty-two", BIOME_PLAINS) {
        @Override public Material getMaterial(Platform platform, long seed, int x, int y, int z, int height) {return helper.getMaterial(seed, x, y, z, height);}

        @Override public Material getMaterial(Platform platform, long seed, int x, int y, float z, int height) {return helper.getMaterial(seed, x, y, z, height);}

        @Override public String getName() {return helper.getName();}

        @Override public BufferedImage getIcon(ColourScheme colourScheme) {return helper.getIcon(colourScheme);}

        @Override public boolean isCustom() {return true;}

        @Override public boolean isConfigured() {return helper.isConfigured();}

        @Override public int getDefaultBiome() {return helper.getDefaultBiome();}

        @Override public int getCustomTerrainIndex() {return helper.getCustomTerrainIndex();}

        @Override public int getColour(long seed, int x, int y, int z, int height, Platform platform, ColourScheme colourScheme) {return helper.getColour(seed, x, y, z, height, colourScheme);}

        @Override public int getColour(long seed, int x, int y, float z, int height, Platform platform, ColourScheme colourScheme) {return helper.getColour(seed, x, y, z, height, colourScheme);}

        private final CustomTerrainHelper helper = new CustomTerrainHelper(31);
    },
    CUSTOM_33("Custom 33",                                  "custom material thirty-three", BIOME_PLAINS) {
        @Override public Material getMaterial(Platform platform, long seed, int x, int y, int z, int height) {return helper.getMaterial(seed, x, y, z, height);}

        @Override public Material getMaterial(Platform platform, long seed, int x, int y, float z, int height) {return helper.getMaterial(seed, x, y, z, height);}

        @Override public String getName() {return helper.getName();}

        @Override public BufferedImage getIcon(ColourScheme colourScheme) {return helper.getIcon(colourScheme);}

        @Override public boolean isCustom() {return true;}

        @Override public boolean isConfigured() {return helper.isConfigured();}

        @Override public int getDefaultBiome() {return helper.getDefaultBiome();}

        @Override public int getCustomTerrainIndex() {return helper.getCustomTerrainIndex();}

        @Override public int getColour(long seed, int x, int y, int z, int height, Platform platform, ColourScheme colourScheme) {return helper.getColour(seed, x, y, z, height, colourScheme);}

        @Override public int getColour(long seed, int x, int y, float z, int height, Platform platform, ColourScheme colourScheme) {return helper.getColour(seed, x, y, z, height, colourScheme);}

        private final CustomTerrainHelper helper = new CustomTerrainHelper(32);
    },
    CUSTOM_34("Custom 34",                                  "custom material thirty-four", BIOME_PLAINS) {
        @Override public Material getMaterial(Platform platform, long seed, int x, int y, int z, int height) {return helper.getMaterial(seed, x, y, z, height);}

        @Override public Material getMaterial(Platform platform, long seed, int x, int y, float z, int height) {return helper.getMaterial(seed, x, y, z, height);}

        @Override public String getName() {return helper.getName();}

        @Override public BufferedImage getIcon(ColourScheme colourScheme) {return helper.getIcon(colourScheme);}

        @Override public boolean isCustom() {return true;}

        @Override public boolean isConfigured() {return helper.isConfigured();}

        @Override public int getDefaultBiome() {return helper.getDefaultBiome();}

        @Override public int getCustomTerrainIndex() {return helper.getCustomTerrainIndex();}

        @Override public int getColour(long seed, int x, int y, int z, int height, Platform platform, ColourScheme colourScheme) {return helper.getColour(seed, x, y, z, height, colourScheme);}

        @Override public int getColour(long seed, int x, int y, float z, int height, Platform platform, ColourScheme colourScheme) {return helper.getColour(seed, x, y, z, height, colourScheme);}

        private final CustomTerrainHelper helper = new CustomTerrainHelper(33);
    },
    CUSTOM_35("Custom 35",                                  "custom material thirty-five", BIOME_PLAINS) {
        @Override public Material getMaterial(Platform platform, long seed, int x, int y, int z, int height) {return helper.getMaterial(seed, x, y, z, height);}

        @Override public Material getMaterial(Platform platform, long seed, int x, int y, float z, int height) {return helper.getMaterial(seed, x, y, z, height);}

        @Override public String getName() {return helper.getName();}

        @Override public BufferedImage getIcon(ColourScheme colourScheme) {return helper.getIcon(colourScheme);}

        @Override public boolean isCustom() {return true;}

        @Override public boolean isConfigured() {return helper.isConfigured();}

        @Override public int getDefaultBiome() {return helper.getDefaultBiome();}

        @Override public int getCustomTerrainIndex() {return helper.getCustomTerrainIndex();}

        @Override public int getColour(long seed, int x, int y, int z, int height, Platform platform, ColourScheme colourScheme) {return helper.getColour(seed, x, y, z, height, colourScheme);}

        @Override public int getColour(long seed, int x, int y, float z, int height, Platform platform, ColourScheme colourScheme) {return helper.getColour(seed, x, y, z, height, colourScheme);}

        private final CustomTerrainHelper helper = new CustomTerrainHelper(34);
    },
    CUSTOM_36("Custom 36",                                  "custom material thirty-six", BIOME_PLAINS) {
        @Override public Material getMaterial(Platform platform, long seed, int x, int y, int z, int height) {return helper.getMaterial(seed, x, y, z, height);}

        @Override public Material getMaterial(Platform platform, long seed, int x, int y, float z, int height) {return helper.getMaterial(seed, x, y, z, height);}

        @Override public String getName() {return helper.getName();}

        @Override public BufferedImage getIcon(ColourScheme colourScheme) {return helper.getIcon(colourScheme);}

        @Override public boolean isCustom() {return true;}

        @Override public boolean isConfigured() {return helper.isConfigured();}

        @Override public int getDefaultBiome() {return helper.getDefaultBiome();}

        @Override public int getCustomTerrainIndex() {return helper.getCustomTerrainIndex();}

        @Override public int getColour(long seed, int x, int y, int z, int height, Platform platform, ColourScheme colourScheme) {return helper.getColour(seed, x, y, z, height, colourScheme);}

        @Override public int getColour(long seed, int x, int y, float z, int height, Platform platform, ColourScheme colourScheme) {return helper.getColour(seed, x, y, z, height, colourScheme);}

        private final CustomTerrainHelper helper = new CustomTerrainHelper(35);
    },
    CUSTOM_37("Custom 37",                                  "custom material thirty-seven", BIOME_PLAINS) {
        @Override public Material getMaterial(Platform platform, long seed, int x, int y, int z, int height) {return helper.getMaterial(seed, x, y, z, height);}

        @Override public Material getMaterial(Platform platform, long seed, int x, int y, float z, int height) {return helper.getMaterial(seed, x, y, z, height);}

        @Override public String getName() {return helper.getName();}

        @Override public BufferedImage getIcon(ColourScheme colourScheme) {return helper.getIcon(colourScheme);}

        @Override public boolean isCustom() {return true;}

        @Override public boolean isConfigured() {return helper.isConfigured();}

        @Override public int getDefaultBiome() {return helper.getDefaultBiome();}

        @Override public int getCustomTerrainIndex() {return helper.getCustomTerrainIndex();}

        @Override public int getColour(long seed, int x, int y, int z, int height, Platform platform, ColourScheme colourScheme) {return helper.getColour(seed, x, y, z, height, colourScheme);}

        @Override public int getColour(long seed, int x, int y, float z, int height, Platform platform, ColourScheme colourScheme) {return helper.getColour(seed, x, y, z, height, colourScheme);}

        private final CustomTerrainHelper helper = new CustomTerrainHelper(36);
    },
    CUSTOM_38("Custom 38",                                  "custom material thirty-eight", BIOME_PLAINS) {
        @Override public Material getMaterial(Platform platform, long seed, int x, int y, int z, int height) {return helper.getMaterial(seed, x, y, z, height);}

        @Override public Material getMaterial(Platform platform, long seed, int x, int y, float z, int height) {return helper.getMaterial(seed, x, y, z, height);}

        @Override public String getName() {return helper.getName();}

        @Override public BufferedImage getIcon(ColourScheme colourScheme) {return helper.getIcon(colourScheme);}

        @Override public boolean isCustom() {return true;}

        @Override public boolean isConfigured() {return helper.isConfigured();}

        @Override public int getDefaultBiome() {return helper.getDefaultBiome();}

        @Override public int getCustomTerrainIndex() {return helper.getCustomTerrainIndex();}

        @Override public int getColour(long seed, int x, int y, int z, int height, Platform platform, ColourScheme colourScheme) {return helper.getColour(seed, x, y, z, height, colourScheme);}

        @Override public int getColour(long seed, int x, int y, float z, int height, Platform platform, ColourScheme colourScheme) {return helper.getColour(seed, x, y, z, height, colourScheme);}

        private final CustomTerrainHelper helper = new CustomTerrainHelper(37);
    },
    CUSTOM_39("Custom 39",                                  "custom material thirty-nine", BIOME_PLAINS) {
        @Override public Material getMaterial(Platform platform, long seed, int x, int y, int z, int height) {return helper.getMaterial(seed, x, y, z, height);}

        @Override public Material getMaterial(Platform platform, long seed, int x, int y, float z, int height) {return helper.getMaterial(seed, x, y, z, height);}

        @Override public String getName() {return helper.getName();}

        @Override public BufferedImage getIcon(ColourScheme colourScheme) {return helper.getIcon(colourScheme);}

        @Override public boolean isCustom() {return true;}

        @Override public boolean isConfigured() {return helper.isConfigured();}

        @Override public int getDefaultBiome() {return helper.getDefaultBiome();}

        @Override public int getCustomTerrainIndex() {return helper.getCustomTerrainIndex();}

        @Override public int getColour(long seed, int x, int y, int z, int height, Platform platform, ColourScheme colourScheme) {return helper.getColour(seed, x, y, z, height, colourScheme);}

        @Override public int getColour(long seed, int x, int y, float z, int height, Platform platform, ColourScheme colourScheme) {return helper.getColour(seed, x, y, z, height, colourScheme);}

        private final CustomTerrainHelper helper = new CustomTerrainHelper(38);
    },
    CUSTOM_40("Custom 40",                                  "custom material forty", BIOME_PLAINS) {
        @Override public Material getMaterial(Platform platform, long seed, int x, int y, int z, int height) {return helper.getMaterial(seed, x, y, z, height);}

        @Override public Material getMaterial(Platform platform, long seed, int x, int y, float z, int height) {return helper.getMaterial(seed, x, y, z, height);}

        @Override public String getName() {return helper.getName();}

        @Override public BufferedImage getIcon(ColourScheme colourScheme) {return helper.getIcon(colourScheme);}

        @Override public boolean isCustom() {return true;}

        @Override public boolean isConfigured() {return helper.isConfigured();}

        @Override public int getDefaultBiome() {return helper.getDefaultBiome();}

        @Override public int getCustomTerrainIndex() {return helper.getCustomTerrainIndex();}

        @Override public int getColour(long seed, int x, int y, int z, int height, Platform platform, ColourScheme colourScheme) {return helper.getColour(seed, x, y, z, height, colourScheme);}

        @Override public int getColour(long seed, int x, int y, float z, int height, Platform platform, ColourScheme colourScheme) {return helper.getColour(seed, x, y, z, height, colourScheme);}

        private final CustomTerrainHelper helper = new CustomTerrainHelper(39);
    },
    CUSTOM_41("Custom 41",                                  "custom material forty-one", BIOME_PLAINS) {
        @Override public Material getMaterial(Platform platform, long seed, int x, int y, int z, int height) {return helper.getMaterial(seed, x, y, z, height);}

        @Override public Material getMaterial(Platform platform, long seed, int x, int y, float z, int height) {return helper.getMaterial(seed, x, y, z, height);}

        @Override public String getName() {return helper.getName();}

        @Override public BufferedImage getIcon(ColourScheme colourScheme) {return helper.getIcon(colourScheme);}

        @Override public boolean isCustom() {return true;}

        @Override public boolean isConfigured() {return helper.isConfigured();}

        @Override public int getDefaultBiome() {return helper.getDefaultBiome();}

        @Override public int getCustomTerrainIndex() {return helper.getCustomTerrainIndex();}

        @Override public int getColour(long seed, int x, int y, int z, int height, Platform platform, ColourScheme colourScheme) {return helper.getColour(seed, x, y, z, height, colourScheme);}

        @Override public int getColour(long seed, int x, int y, float z, int height, Platform platform, ColourScheme colourScheme) {return helper.getColour(seed, x, y, z, height, colourScheme);}

        private final CustomTerrainHelper helper = new CustomTerrainHelper(40);
    },
    CUSTOM_42("Custom 42",                                  "life, the universe and everything", BIOME_PLAINS) {
        @Override public Material getMaterial(Platform platform, long seed, int x, int y, int z, int height) {return helper.getMaterial(seed, x, y, z, height);}

        @Override public Material getMaterial(Platform platform, long seed, int x, int y, float z, int height) {return helper.getMaterial(seed, x, y, z, height);}

        @Override public String getName() {return helper.getName();}

        @Override public BufferedImage getIcon(ColourScheme colourScheme) {return helper.getIcon(colourScheme);}

        @Override public boolean isCustom() {return true;}

        @Override public boolean isConfigured() {return helper.isConfigured();}

        @Override public int getDefaultBiome() {return helper.getDefaultBiome();}

        @Override public int getCustomTerrainIndex() {return helper.getCustomTerrainIndex();}

        @Override public int getColour(long seed, int x, int y, int z, int height, Platform platform, ColourScheme colourScheme) {return helper.getColour(seed, x, y, z, height, colourScheme);}

        @Override public int getColour(long seed, int x, int y, float z, int height, Platform platform, ColourScheme colourScheme) {return helper.getColour(seed, x, y, z, height, colourScheme);}

        private final CustomTerrainHelper helper = new CustomTerrainHelper(41);
    },
    CUSTOM_43("Custom 43",                                  "custom material forty-three", BIOME_PLAINS) {
        @Override public Material getMaterial(Platform platform, long seed, int x, int y, int z, int height) {return helper.getMaterial(seed, x, y, z, height);}

        @Override public Material getMaterial(Platform platform, long seed, int x, int y, float z, int height) {return helper.getMaterial(seed, x, y, z, height);}

        @Override public String getName() {return helper.getName();}

        @Override public BufferedImage getIcon(ColourScheme colourScheme) {return helper.getIcon(colourScheme);}

        @Override public boolean isCustom() {return true;}

        @Override public boolean isConfigured() {return helper.isConfigured();}

        @Override public int getDefaultBiome() {return helper.getDefaultBiome();}

        @Override public int getCustomTerrainIndex() {return helper.getCustomTerrainIndex();}

        @Override public int getColour(long seed, int x, int y, int z, int height, Platform platform, ColourScheme colourScheme) {return helper.getColour(seed, x, y, z, height, colourScheme);}

        @Override public int getColour(long seed, int x, int y, float z, int height, Platform platform, ColourScheme colourScheme) {return helper.getColour(seed, x, y, z, height, colourScheme);}

        private final CustomTerrainHelper helper = new CustomTerrainHelper(42);
    },
    CUSTOM_44("Custom 44",                                  "custom material forty-four", BIOME_PLAINS) {
        @Override public Material getMaterial(Platform platform, long seed, int x, int y, int z, int height) {return helper.getMaterial(seed, x, y, z, height);}

        @Override public Material getMaterial(Platform platform, long seed, int x, int y, float z, int height) {return helper.getMaterial(seed, x, y, z, height);}

        @Override public String getName() {return helper.getName();}

        @Override public BufferedImage getIcon(ColourScheme colourScheme) {return helper.getIcon(colourScheme);}

        @Override public boolean isCustom() {return true;}

        @Override public boolean isConfigured() {return helper.isConfigured();}

        @Override public int getDefaultBiome() {return helper.getDefaultBiome();}

        @Override public int getCustomTerrainIndex() {return helper.getCustomTerrainIndex();}

        @Override public int getColour(long seed, int x, int y, int z, int height, Platform platform, ColourScheme colourScheme) {return helper.getColour(seed, x, y, z, height, colourScheme);}

        @Override public int getColour(long seed, int x, int y, float z, int height, Platform platform, ColourScheme colourScheme) {return helper.getColour(seed, x, y, z, height, colourScheme);}

        private final CustomTerrainHelper helper = new CustomTerrainHelper(43);
    },
    CUSTOM_45("Custom 45",                                  "custom material forty-five", BIOME_PLAINS) {
        @Override public Material getMaterial(Platform platform, long seed, int x, int y, int z, int height) {return helper.getMaterial(seed, x, y, z, height);}

        @Override public Material getMaterial(Platform platform, long seed, int x, int y, float z, int height) {return helper.getMaterial(seed, x, y, z, height);}

        @Override public String getName() {return helper.getName();}

        @Override public BufferedImage getIcon(ColourScheme colourScheme) {return helper.getIcon(colourScheme);}

        @Override public boolean isCustom() {return true;}

        @Override public boolean isConfigured() {return helper.isConfigured();}

        @Override public int getDefaultBiome() {return helper.getDefaultBiome();}

        @Override public int getCustomTerrainIndex() {return helper.getCustomTerrainIndex();}

        @Override public int getColour(long seed, int x, int y, int z, int height, Platform platform, ColourScheme colourScheme) {return helper.getColour(seed, x, y, z, height, colourScheme);}

        @Override public int getColour(long seed, int x, int y, float z, int height, Platform platform, ColourScheme colourScheme) {return helper.getColour(seed, x, y, z, height, colourScheme);}

        private final CustomTerrainHelper helper = new CustomTerrainHelper(44);
    },
    CUSTOM_46("Custom 46",                                  "custom material forty-six", BIOME_PLAINS) {
        @Override public Material getMaterial(Platform platform, long seed, int x, int y, int z, int height) {return helper.getMaterial(seed, x, y, z, height);}

        @Override public Material getMaterial(Platform platform, long seed, int x, int y, float z, int height) {return helper.getMaterial(seed, x, y, z, height);}

        @Override public String getName() {return helper.getName();}

        @Override public BufferedImage getIcon(ColourScheme colourScheme) {return helper.getIcon(colourScheme);}

        @Override public boolean isCustom() {return true;}

        @Override public boolean isConfigured() {return helper.isConfigured();}

        @Override public int getDefaultBiome() {return helper.getDefaultBiome();}

        @Override public int getCustomTerrainIndex() {return helper.getCustomTerrainIndex();}

        @Override public int getColour(long seed, int x, int y, int z, int height, Platform platform, ColourScheme colourScheme) {return helper.getColour(seed, x, y, z, height, colourScheme);}

        @Override public int getColour(long seed, int x, int y, float z, int height, Platform platform, ColourScheme colourScheme) {return helper.getColour(seed, x, y, z, height, colourScheme);}

        private final CustomTerrainHelper helper = new CustomTerrainHelper(45);
    },
    CUSTOM_47("Custom 47",                                  "custom material forty-seven", BIOME_PLAINS) {
        @Override public Material getMaterial(Platform platform, long seed, int x, int y, int z, int height) {return helper.getMaterial(seed, x, y, z, height);}

        @Override public Material getMaterial(Platform platform, long seed, int x, int y, float z, int height) {return helper.getMaterial(seed, x, y, z, height);}

        @Override public String getName() {return helper.getName();}

        @Override public BufferedImage getIcon(ColourScheme colourScheme) {return helper.getIcon(colourScheme);}

        @Override public boolean isCustom() {return true;}

        @Override public boolean isConfigured() {return helper.isConfigured();}

        @Override public int getDefaultBiome() {return helper.getDefaultBiome();}

        @Override public int getCustomTerrainIndex() {return helper.getCustomTerrainIndex();}

        @Override public int getColour(long seed, int x, int y, int z, int height, Platform platform, ColourScheme colourScheme) {return helper.getColour(seed, x, y, z, height, colourScheme);}

        @Override public int getColour(long seed, int x, int y, float z, int height, Platform platform, ColourScheme colourScheme) {return helper.getColour(seed, x, y, z, height, colourScheme);}

        private final CustomTerrainHelper helper = new CustomTerrainHelper(46);
    },
    CUSTOM_48("Custom 48",                                  "custom material forty-eight", BIOME_PLAINS) {
        @Override public Material getMaterial(Platform platform, long seed, int x, int y, int z, int height) {return helper.getMaterial(seed, x, y, z, height);}

        @Override public Material getMaterial(Platform platform, long seed, int x, int y, float z, int height) {return helper.getMaterial(seed, x, y, z, height);}

        @Override public String getName() {return helper.getName();}

        @Override public BufferedImage getIcon(ColourScheme colourScheme) {return helper.getIcon(colourScheme);}

        @Override public boolean isCustom() {return true;}

        @Override public boolean isConfigured() {return helper.isConfigured();}

        @Override public int getDefaultBiome() {return helper.getDefaultBiome();}

        @Override public int getCustomTerrainIndex() {return helper.getCustomTerrainIndex();}

        @Override public int getColour(long seed, int x, int y, int z, int height, Platform platform, ColourScheme colourScheme) {return helper.getColour(seed, x, y, z, height, colourScheme);}

        @Override public int getColour(long seed, int x, int y, float z, int height, Platform platform, ColourScheme colourScheme) {return helper.getColour(seed, x, y, z, height, colourScheme);}

        private final CustomTerrainHelper helper = new CustomTerrainHelper(47);
    },
    GRASS_PATH("Dirt Path", "dirt path", BIOME_PLAINS) {
        @Override
        public Material getMaterial(Platform platform, long seed, int x, int y, int z, int height) {
            final int dz = z - height;
            if (dz == 0) {
                if ((platform == JAVA_MCREGION) || (platform == JAVA_ANVIL) || (platform == JAVA_ANVIL_1_15)) {
                    return Material.GRASS_PATH;
                } else {
                    return DIRT_PATH;
                }
            } else {
                return GRASS_BLOCK;
            }
        }
    },
    MAGMA("Magma", BLK_MAGMA, BLK_MAGMA, "magma", BIOME_PLAINS), // TODO: or should this be mapped to stone and magma added to the Resources layer?
    CUSTOM_49("Custom 49", "custom material forty-nine", BIOME_PLAINS) {
        @Override public Material getMaterial(Platform platform, long seed, int x, int y, int z, int height) {return helper.getMaterial(seed, x, y, z, height);}

        @Override public Material getMaterial(Platform platform, long seed, int x, int y, float z, int height) {return helper.getMaterial(seed, x, y, z, height);}

        @Override public String getName() {return helper.getName();}

        @Override public BufferedImage getIcon(ColourScheme colourScheme) {return helper.getIcon(colourScheme);}

        @Override public boolean isCustom() {return true;}

        @Override public boolean isConfigured() {return helper.isConfigured();}

        @Override public int getDefaultBiome() {return helper.getDefaultBiome();}

        @Override public int getCustomTerrainIndex() {return helper.getCustomTerrainIndex();}

        @Override public int getColour(long seed, int x, int y, int z, int height, Platform platform, ColourScheme colourScheme) {return helper.getColour(seed, x, y, z, height, colourScheme);}

        @Override public int getColour(long seed, int x, int y, float z, int height, Platform platform, ColourScheme colourScheme) {return helper.getColour(seed, x, y, z, height, colourScheme);}

        private final CustomTerrainHelper helper = new CustomTerrainHelper(48);
    },
    CUSTOM_50("Custom 50", "custom material fifty", BIOME_PLAINS) {
        @Override public Material getMaterial(Platform platform, long seed, int x, int y, int z, int height) {return helper.getMaterial(seed, x, y, z, height);}

        @Override public Material getMaterial(Platform platform, long seed, int x, int y, float z, int height) {return helper.getMaterial(seed, x, y, z, height);}

        @Override public String getName() {return helper.getName();}

        @Override public BufferedImage getIcon(ColourScheme colourScheme) {return helper.getIcon(colourScheme);}

        @Override public boolean isCustom() {return true;}

        @Override public boolean isConfigured() {return helper.isConfigured();}

        @Override public int getDefaultBiome() {return helper.getDefaultBiome();}

        @Override public int getCustomTerrainIndex() {return helper.getCustomTerrainIndex();}

        @Override public int getColour(long seed, int x, int y, int z, int height, Platform platform, ColourScheme colourScheme) {return helper.getColour(seed, x, y, z, height, colourScheme);}

        @Override public int getColour(long seed, int x, int y, float z, int height, Platform platform, ColourScheme colourScheme) {return helper.getColour(seed, x, y, z, height, colourScheme);}

        private final CustomTerrainHelper helper = new CustomTerrainHelper(49);
    },
    CUSTOM_51("Custom 51", "custom material fifty-one", BIOME_PLAINS) {
        @Override public Material getMaterial(Platform platform, long seed, int x, int y, int z, int height) {return helper.getMaterial(seed, x, y, z, height);}

        @Override public Material getMaterial(Platform platform, long seed, int x, int y, float z, int height) {return helper.getMaterial(seed, x, y, z, height);}

        @Override public String getName() {return helper.getName();}

        @Override public BufferedImage getIcon(ColourScheme colourScheme) {return helper.getIcon(colourScheme);}

        @Override public boolean isCustom() {return true;}

        @Override public boolean isConfigured() {return helper.isConfigured();}

        @Override public int getDefaultBiome() {return helper.getDefaultBiome();}

        @Override public int getCustomTerrainIndex() {return helper.getCustomTerrainIndex();}

        @Override public int getColour(long seed, int x, int y, int z, int height, Platform platform, ColourScheme colourScheme) {return helper.getColour(seed, x, y, z, height, colourScheme);}

        @Override public int getColour(long seed, int x, int y, float z, int height, Platform platform, ColourScheme colourScheme) {return helper.getColour(seed, x, y, z, height, colourScheme);}

        private final CustomTerrainHelper helper = new CustomTerrainHelper(50);
    },
    CUSTOM_52("Custom 52", "custom material fifty-two", BIOME_PLAINS) {
        @Override public Material getMaterial(Platform platform, long seed, int x, int y, int z, int height) {return helper.getMaterial(seed, x, y, z, height);}

        @Override public Material getMaterial(Platform platform, long seed, int x, int y, float z, int height) {return helper.getMaterial(seed, x, y, z, height);}

        @Override public String getName() {return helper.getName();}

        @Override public BufferedImage getIcon(ColourScheme colourScheme) {return helper.getIcon(colourScheme);}

        @Override public boolean isCustom() {return true;}

        @Override public boolean isConfigured() {return helper.isConfigured();}

        @Override public int getDefaultBiome() {return helper.getDefaultBiome();}

        @Override public int getCustomTerrainIndex() {return helper.getCustomTerrainIndex();}

        @Override public int getColour(long seed, int x, int y, int z, int height, Platform platform, ColourScheme colourScheme) {return helper.getColour(seed, x, y, z, height, colourScheme);}

        @Override public int getColour(long seed, int x, int y, float z, int height, Platform platform, ColourScheme colourScheme) {return helper.getColour(seed, x, y, z, height, colourScheme);}

        private final CustomTerrainHelper helper = new CustomTerrainHelper(51);
    },
    CUSTOM_53("Custom 53", "custom material fifty-three", BIOME_PLAINS) {
        @Override public Material getMaterial(Platform platform, long seed, int x, int y, int z, int height) {return helper.getMaterial(seed, x, y, z, height);}

        @Override public Material getMaterial(Platform platform, long seed, int x, int y, float z, int height) {return helper.getMaterial(seed, x, y, z, height);}

        @Override public String getName() {return helper.getName();}

        @Override public BufferedImage getIcon(ColourScheme colourScheme) {return helper.getIcon(colourScheme);}

        @Override public boolean isCustom() {return true;}

        @Override public boolean isConfigured() {return helper.isConfigured();}

        @Override public int getDefaultBiome() {return helper.getDefaultBiome();}

        @Override public int getCustomTerrainIndex() {return helper.getCustomTerrainIndex();}

        @Override public int getColour(long seed, int x, int y, int z, int height, Platform platform, ColourScheme colourScheme) {return helper.getColour(seed, x, y, z, height, colourScheme);}

        @Override public int getColour(long seed, int x, int y, float z, int height, Platform platform, ColourScheme colourScheme) {return helper.getColour(seed, x, y, z, height, colourScheme);}

        private final CustomTerrainHelper helper = new CustomTerrainHelper(52);
    },
    CUSTOM_54("Custom 54", "custom material fifty-four", BIOME_PLAINS) {
        @Override public Material getMaterial(Platform platform, long seed, int x, int y, int z, int height) {return helper.getMaterial(seed, x, y, z, height);}

        @Override public Material getMaterial(Platform platform, long seed, int x, int y, float z, int height) {return helper.getMaterial(seed, x, y, z, height);}

        @Override public String getName() {return helper.getName();}

        @Override public BufferedImage getIcon(ColourScheme colourScheme) {return helper.getIcon(colourScheme);}

        @Override public boolean isCustom() {return true;}

        @Override public boolean isConfigured() {return helper.isConfigured();}

        @Override public int getDefaultBiome() {return helper.getDefaultBiome();}

        @Override public int getCustomTerrainIndex() {return helper.getCustomTerrainIndex();}

        @Override public int getColour(long seed, int x, int y, int z, int height, Platform platform, ColourScheme colourScheme) {return helper.getColour(seed, x, y, z, height, colourScheme);}

        @Override public int getColour(long seed, int x, int y, float z, int height, Platform platform, ColourScheme colourScheme) {return helper.getColour(seed, x, y, z, height, colourScheme);}

        private final CustomTerrainHelper helper = new CustomTerrainHelper(53);
    },
    CUSTOM_55("Custom 55", "custom material fifty-five", BIOME_PLAINS) {
        @Override public Material getMaterial(Platform platform, long seed, int x, int y, int z, int height) {return helper.getMaterial(seed, x, y, z, height);}

        @Override public Material getMaterial(Platform platform, long seed, int x, int y, float z, int height) {return helper.getMaterial(seed, x, y, z, height);}

        @Override public String getName() {return helper.getName();}

        @Override public BufferedImage getIcon(ColourScheme colourScheme) {return helper.getIcon(colourScheme);}

        @Override public boolean isCustom() {return true;}

        @Override public boolean isConfigured() {return helper.isConfigured();}

        @Override public int getDefaultBiome() {return helper.getDefaultBiome();}

        @Override public int getCustomTerrainIndex() {return helper.getCustomTerrainIndex();}

        @Override public int getColour(long seed, int x, int y, int z, int height, Platform platform, ColourScheme colourScheme) {return helper.getColour(seed, x, y, z, height, colourScheme);}

        @Override public int getColour(long seed, int x, int y, float z, int height, Platform platform, ColourScheme colourScheme) {return helper.getColour(seed, x, y, z, height, colourScheme);}

        private final CustomTerrainHelper helper = new CustomTerrainHelper(54);
    },
    CUSTOM_56("Custom 56", "custom material fifty-six", BIOME_PLAINS) {
        @Override public Material getMaterial(Platform platform, long seed, int x, int y, int z, int height) {return helper.getMaterial(seed, x, y, z, height);}

        @Override public Material getMaterial(Platform platform, long seed, int x, int y, float z, int height) {return helper.getMaterial(seed, x, y, z, height);}

        @Override public String getName() {return helper.getName();}

        @Override public BufferedImage getIcon(ColourScheme colourScheme) {return helper.getIcon(colourScheme);}

        @Override public boolean isCustom() {return true;}

        @Override public boolean isConfigured() {return helper.isConfigured();}

        @Override public int getDefaultBiome() {return helper.getDefaultBiome();}

        @Override public int getCustomTerrainIndex() {return helper.getCustomTerrainIndex();}

        @Override public int getColour(long seed, int x, int y, int z, int height, Platform platform, ColourScheme colourScheme) {return helper.getColour(seed, x, y, z, height, colourScheme);}

        @Override public int getColour(long seed, int x, int y, float z, int height, Platform platform, ColourScheme colourScheme) {return helper.getColour(seed, x, y, z, height, colourScheme);}

        private final CustomTerrainHelper helper = new CustomTerrainHelper(55);
    },
    CUSTOM_57("Custom 57", "custom material fifty-seven", BIOME_PLAINS) {
        @Override public Material getMaterial(Platform platform, long seed, int x, int y, int z, int height) {return helper.getMaterial(seed, x, y, z, height);}

        @Override public Material getMaterial(Platform platform, long seed, int x, int y, float z, int height) {return helper.getMaterial(seed, x, y, z, height);}

        @Override public String getName() {return helper.getName();}

        @Override public BufferedImage getIcon(ColourScheme colourScheme) {return helper.getIcon(colourScheme);}

        @Override public boolean isCustom() {return true;}

        @Override public boolean isConfigured() {return helper.isConfigured();}

        @Override public int getDefaultBiome() {return helper.getDefaultBiome();}

        @Override public int getCustomTerrainIndex() {return helper.getCustomTerrainIndex();}

        @Override public int getColour(long seed, int x, int y, int z, int height, Platform platform, ColourScheme colourScheme) {return helper.getColour(seed, x, y, z, height, colourScheme);}

        @Override public int getColour(long seed, int x, int y, float z, int height, Platform platform, ColourScheme colourScheme) {return helper.getColour(seed, x, y, z, height, colourScheme);}

        private final CustomTerrainHelper helper = new CustomTerrainHelper(56);
    },
    CUSTOM_58("Custom 58", "custom material fifty-eight", BIOME_PLAINS) {
        @Override public Material getMaterial(Platform platform, long seed, int x, int y, int z, int height) {return helper.getMaterial(seed, x, y, z, height);}

        @Override public Material getMaterial(Platform platform, long seed, int x, int y, float z, int height) {return helper.getMaterial(seed, x, y, z, height);}

        @Override public String getName() {return helper.getName();}

        @Override public BufferedImage getIcon(ColourScheme colourScheme) {return helper.getIcon(colourScheme);}

        @Override public boolean isCustom() {return true;}

        @Override public boolean isConfigured() {return helper.isConfigured();}

        @Override public int getDefaultBiome() {return helper.getDefaultBiome();}

        @Override public int getCustomTerrainIndex() {return helper.getCustomTerrainIndex();}

        @Override public int getColour(long seed, int x, int y, int z, int height, Platform platform, ColourScheme colourScheme) {return helper.getColour(seed, x, y, z, height, colourScheme);}

        @Override public int getColour(long seed, int x, int y, float z, int height, Platform platform, ColourScheme colourScheme) {return helper.getColour(seed, x, y, z, height, colourScheme);}

        private final CustomTerrainHelper helper = new CustomTerrainHelper(57);
    },
    CUSTOM_59("Custom 59", "custom material fifty-nine", BIOME_PLAINS) {
        @Override public Material getMaterial(Platform platform, long seed, int x, int y, int z, int height) {return helper.getMaterial(seed, x, y, z, height);}

        @Override public Material getMaterial(Platform platform, long seed, int x, int y, float z, int height) {return helper.getMaterial(seed, x, y, z, height);}

        @Override public String getName() {return helper.getName();}

        @Override public BufferedImage getIcon(ColourScheme colourScheme) {return helper.getIcon(colourScheme);}

        @Override public boolean isCustom() {return true;}

        @Override public boolean isConfigured() {return helper.isConfigured();}

        @Override public int getDefaultBiome() {return helper.getDefaultBiome();}

        @Override public int getCustomTerrainIndex() {return helper.getCustomTerrainIndex();}

        @Override public int getColour(long seed, int x, int y, int z, int height, Platform platform, ColourScheme colourScheme) {return helper.getColour(seed, x, y, z, height, colourScheme);}

        @Override public int getColour(long seed, int x, int y, float z, int height, Platform platform, ColourScheme colourScheme) {return helper.getColour(seed, x, y, z, height, colourScheme);}

        private final CustomTerrainHelper helper = new CustomTerrainHelper(58);
    },
    CUSTOM_60("Custom 60", "custom material sixty", BIOME_PLAINS) {
        @Override public Material getMaterial(Platform platform, long seed, int x, int y, int z, int height) {return helper.getMaterial(seed, x, y, z, height);}

        @Override public Material getMaterial(Platform platform, long seed, int x, int y, float z, int height) {return helper.getMaterial(seed, x, y, z, height);}

        @Override public String getName() {return helper.getName();}

        @Override public BufferedImage getIcon(ColourScheme colourScheme) {return helper.getIcon(colourScheme);}

        @Override public boolean isCustom() {return true;}

        @Override public boolean isConfigured() {return helper.isConfigured();}

        @Override public int getDefaultBiome() {return helper.getDefaultBiome();}

        @Override public int getCustomTerrainIndex() {return helper.getCustomTerrainIndex();}

        @Override public int getColour(long seed, int x, int y, int z, int height, Platform platform, ColourScheme colourScheme) {return helper.getColour(seed, x, y, z, height, colourScheme);}

        @Override public int getColour(long seed, int x, int y, float z, int height, Platform platform, ColourScheme colourScheme) {return helper.getColour(seed, x, y, z, height, colourScheme);}

        private final CustomTerrainHelper helper = new CustomTerrainHelper(59);
    },
    CUSTOM_61("Custom 61", "custom material sixty-one", BIOME_PLAINS) {
        @Override public Material getMaterial(Platform platform, long seed, int x, int y, int z, int height) {return helper.getMaterial(seed, x, y, z, height);}

        @Override public Material getMaterial(Platform platform, long seed, int x, int y, float z, int height) {return helper.getMaterial(seed, x, y, z, height);}

        @Override public String getName() {return helper.getName();}

        @Override public BufferedImage getIcon(ColourScheme colourScheme) {return helper.getIcon(colourScheme);}

        @Override public boolean isCustom() {return true;}

        @Override public boolean isConfigured() {return helper.isConfigured();}

        @Override public int getDefaultBiome() {return helper.getDefaultBiome();}

        @Override public int getCustomTerrainIndex() {return helper.getCustomTerrainIndex();}

        @Override public int getColour(long seed, int x, int y, int z, int height, Platform platform, ColourScheme colourScheme) {return helper.getColour(seed, x, y, z, height, colourScheme);}

        @Override public int getColour(long seed, int x, int y, float z, int height, Platform platform, ColourScheme colourScheme) {return helper.getColour(seed, x, y, z, height, colourScheme);}

        private final CustomTerrainHelper helper = new CustomTerrainHelper(60);
    },
    CUSTOM_62("Custom 62", "custom material sixty-two", BIOME_PLAINS) {
        @Override public Material getMaterial(Platform platform, long seed, int x, int y, int z, int height) {return helper.getMaterial(seed, x, y, z, height);}

        @Override public Material getMaterial(Platform platform, long seed, int x, int y, float z, int height) {return helper.getMaterial(seed, x, y, z, height);}

        @Override public String getName() {return helper.getName();}

        @Override public BufferedImage getIcon(ColourScheme colourScheme) {return helper.getIcon(colourScheme);}

        @Override public boolean isCustom() {return true;}

        @Override public boolean isConfigured() {return helper.isConfigured();}

        @Override public int getDefaultBiome() {return helper.getDefaultBiome();}

        @Override public int getCustomTerrainIndex() {return helper.getCustomTerrainIndex();}

        @Override public int getColour(long seed, int x, int y, int z, int height, Platform platform, ColourScheme colourScheme) {return helper.getColour(seed, x, y, z, height, colourScheme);}

        @Override public int getColour(long seed, int x, int y, float z, int height, Platform platform, ColourScheme colourScheme) {return helper.getColour(seed, x, y, z, height, colourScheme);}

        private final CustomTerrainHelper helper = new CustomTerrainHelper(61);
    },
    CUSTOM_63("Custom 63", "custom material sixty-three", BIOME_PLAINS) {
        @Override public Material getMaterial(Platform platform, long seed, int x, int y, int z, int height) {return helper.getMaterial(seed, x, y, z, height);}

        @Override public Material getMaterial(Platform platform, long seed, int x, int y, float z, int height) {return helper.getMaterial(seed, x, y, z, height);}

        @Override public String getName() {return helper.getName();}

        @Override public BufferedImage getIcon(ColourScheme colourScheme) {return helper.getIcon(colourScheme);}

        @Override public boolean isCustom() {return true;}

        @Override public boolean isConfigured() {return helper.isConfigured();}

        @Override public int getDefaultBiome() {return helper.getDefaultBiome();}

        @Override public int getCustomTerrainIndex() {return helper.getCustomTerrainIndex();}

        @Override public int getColour(long seed, int x, int y, int z, int height, Platform platform, ColourScheme colourScheme) {return helper.getColour(seed, x, y, z, height, colourScheme);}

        @Override public int getColour(long seed, int x, int y, float z, int height, Platform platform, ColourScheme colourScheme) {return helper.getColour(seed, x, y, z, height, colourScheme);}

        private final CustomTerrainHelper helper = new CustomTerrainHelper(62);
    },
    CUSTOM_64("Custom 64", "custom material sixty-four", BIOME_PLAINS) {
        @Override public Material getMaterial(Platform platform, long seed, int x, int y, int z, int height) {return helper.getMaterial(seed, x, y, z, height);}

        @Override public Material getMaterial(Platform platform, long seed, int x, int y, float z, int height) {return helper.getMaterial(seed, x, y, z, height);}

        @Override public String getName() {return helper.getName();}

        @Override public BufferedImage getIcon(ColourScheme colourScheme) {return helper.getIcon(colourScheme);}

        @Override public boolean isCustom() {return true;}

        @Override public boolean isConfigured() {return helper.isConfigured();}

        @Override public int getDefaultBiome() {return helper.getDefaultBiome();}

        @Override public int getCustomTerrainIndex() {return helper.getCustomTerrainIndex();}

        @Override public int getColour(long seed, int x, int y, int z, int height, Platform platform, ColourScheme colourScheme) {return helper.getColour(seed, x, y, z, height, colourScheme);}

        @Override public int getColour(long seed, int x, int y, float z, int height, Platform platform, ColourScheme colourScheme) {return helper.getColour(seed, x, y, z, height, colourScheme);}

        private final CustomTerrainHelper helper = new CustomTerrainHelper(63);
    },
    CUSTOM_65("Custom 65", "custom material sixty-five", BIOME_PLAINS) {
        @Override public Material getMaterial(Platform platform, long seed, int x, int y, int z, int height) {return helper.getMaterial(seed, x, y, z, height);}

        @Override public Material getMaterial(Platform platform, long seed, int x, int y, float z, int height) {return helper.getMaterial(seed, x, y, z, height);}

        @Override public String getName() {return helper.getName();}

        @Override public BufferedImage getIcon(ColourScheme colourScheme) {return helper.getIcon(colourScheme);}

        @Override public boolean isCustom() {return true;}

        @Override public boolean isConfigured() {return helper.isConfigured();}

        @Override public int getDefaultBiome() {return helper.getDefaultBiome();}

        @Override public int getCustomTerrainIndex() {return helper.getCustomTerrainIndex();}

        @Override public int getColour(long seed, int x, int y, int z, int height, Platform platform, ColourScheme colourScheme) {return helper.getColour(seed, x, y, z, height, colourScheme);}

        @Override public int getColour(long seed, int x, int y, float z, int height, Platform platform, ColourScheme colourScheme) {return helper.getColour(seed, x, y, z, height, colourScheme);}

        private final CustomTerrainHelper helper = new CustomTerrainHelper(64);
    },
    CUSTOM_66("Custom 66", "custom material sixty-six", BIOME_PLAINS) {
        @Override public Material getMaterial(Platform platform, long seed, int x, int y, int z, int height) {return helper.getMaterial(seed, x, y, z, height);}

        @Override public Material getMaterial(Platform platform, long seed, int x, int y, float z, int height) {return helper.getMaterial(seed, x, y, z, height);}

        @Override public String getName() {return helper.getName();}

        @Override public BufferedImage getIcon(ColourScheme colourScheme) {return helper.getIcon(colourScheme);}

        @Override public boolean isCustom() {return true;}

        @Override public boolean isConfigured() {return helper.isConfigured();}

        @Override public int getDefaultBiome() {return helper.getDefaultBiome();}

        @Override public int getCustomTerrainIndex() {return helper.getCustomTerrainIndex();}

        @Override public int getColour(long seed, int x, int y, int z, int height, Platform platform, ColourScheme colourScheme) {return helper.getColour(seed, x, y, z, height, colourScheme);}

        @Override public int getColour(long seed, int x, int y, float z, int height, Platform platform, ColourScheme colourScheme) {return helper.getColour(seed, x, y, z, height, colourScheme);}

        private final CustomTerrainHelper helper = new CustomTerrainHelper(65);
    },
    CUSTOM_67("Custom 67", "custom material sixty-seven", BIOME_PLAINS) {
        @Override public Material getMaterial(Platform platform, long seed, int x, int y, int z, int height) {return helper.getMaterial(seed, x, y, z, height);}

        @Override public Material getMaterial(Platform platform, long seed, int x, int y, float z, int height) {return helper.getMaterial(seed, x, y, z, height);}

        @Override public String getName() {return helper.getName();}

        @Override public BufferedImage getIcon(ColourScheme colourScheme) {return helper.getIcon(colourScheme);}

        @Override public boolean isCustom() {return true;}

        @Override public boolean isConfigured() {return helper.isConfigured();}

        @Override public int getDefaultBiome() {return helper.getDefaultBiome();}

        @Override public int getCustomTerrainIndex() {return helper.getCustomTerrainIndex();}

        @Override public int getColour(long seed, int x, int y, int z, int height, Platform platform, ColourScheme colourScheme) {return helper.getColour(seed, x, y, z, height, colourScheme);}

        @Override public int getColour(long seed, int x, int y, float z, int height, Platform platform, ColourScheme colourScheme) {return helper.getColour(seed, x, y, z, height, colourScheme);}

        private final CustomTerrainHelper helper = new CustomTerrainHelper(66);
    },
    CUSTOM_68("Custom 68", "custom material sixty-eight", BIOME_PLAINS) {
        @Override public Material getMaterial(Platform platform, long seed, int x, int y, int z, int height) {return helper.getMaterial(seed, x, y, z, height);}

        @Override public Material getMaterial(Platform platform, long seed, int x, int y, float z, int height) {return helper.getMaterial(seed, x, y, z, height);}

        @Override public String getName() {return helper.getName();}

        @Override public BufferedImage getIcon(ColourScheme colourScheme) {return helper.getIcon(colourScheme);}

        @Override public boolean isCustom() {return true;}

        @Override public boolean isConfigured() {return helper.isConfigured();}

        @Override public int getDefaultBiome() {return helper.getDefaultBiome();}

        @Override public int getCustomTerrainIndex() {return helper.getCustomTerrainIndex();}

        @Override public int getColour(long seed, int x, int y, int z, int height, Platform platform, ColourScheme colourScheme) {return helper.getColour(seed, x, y, z, height, colourScheme);}

        @Override public int getColour(long seed, int x, int y, float z, int height, Platform platform, ColourScheme colourScheme) {return helper.getColour(seed, x, y, z, height, colourScheme);}

        private final CustomTerrainHelper helper = new CustomTerrainHelper(67);
    },
    CUSTOM_69("Custom 69", "custom material sixty-nine", BIOME_PLAINS) {
        @Override public Material getMaterial(Platform platform, long seed, int x, int y, int z, int height) {return helper.getMaterial(seed, x, y, z, height);}

        @Override public Material getMaterial(Platform platform, long seed, int x, int y, float z, int height) {return helper.getMaterial(seed, x, y, z, height);}

        @Override public String getName() {return helper.getName();}

        @Override public BufferedImage getIcon(ColourScheme colourScheme) {return helper.getIcon(colourScheme);}

        @Override public boolean isCustom() {return true;}

        @Override public boolean isConfigured() {return helper.isConfigured();}

        @Override public int getDefaultBiome() {return helper.getDefaultBiome();}

        @Override public int getCustomTerrainIndex() {return helper.getCustomTerrainIndex();}

        @Override public int getColour(long seed, int x, int y, int z, int height, Platform platform, ColourScheme colourScheme) {return helper.getColour(seed, x, y, z, height, colourScheme);}

        @Override public int getColour(long seed, int x, int y, float z, int height, Platform platform, ColourScheme colourScheme) {return helper.getColour(seed, x, y, z, height, colourScheme);}

        private final CustomTerrainHelper helper = new CustomTerrainHelper(68);
    },
    CUSTOM_70("Custom 70", "custom material seventy", BIOME_PLAINS) {
        @Override public Material getMaterial(Platform platform, long seed, int x, int y, int z, int height) {return helper.getMaterial(seed, x, y, z, height);}

        @Override public Material getMaterial(Platform platform, long seed, int x, int y, float z, int height) {return helper.getMaterial(seed, x, y, z, height);}

        @Override public String getName() {return helper.getName();}

        @Override public BufferedImage getIcon(ColourScheme colourScheme) {return helper.getIcon(colourScheme);}

        @Override public boolean isCustom() {return true;}

        @Override public boolean isConfigured() {return helper.isConfigured();}

        @Override public int getDefaultBiome() {return helper.getDefaultBiome();}

        @Override public int getCustomTerrainIndex() {return helper.getCustomTerrainIndex();}

        @Override public int getColour(long seed, int x, int y, int z, int height, Platform platform, ColourScheme colourScheme) {return helper.getColour(seed, x, y, z, height, colourScheme);}

        @Override public int getColour(long seed, int x, int y, float z, int height, Platform platform, ColourScheme colourScheme) {return helper.getColour(seed, x, y, z, height, colourScheme);}

        private final CustomTerrainHelper helper = new CustomTerrainHelper(69);
    },
    CUSTOM_71("Custom 71", "custom material seventy-one", BIOME_PLAINS) {
        @Override public Material getMaterial(Platform platform, long seed, int x, int y, int z, int height) {return helper.getMaterial(seed, x, y, z, height);}

        @Override public Material getMaterial(Platform platform, long seed, int x, int y, float z, int height) {return helper.getMaterial(seed, x, y, z, height);}

        @Override public String getName() {return helper.getName();}

        @Override public BufferedImage getIcon(ColourScheme colourScheme) {return helper.getIcon(colourScheme);}

        @Override public boolean isCustom() {return true;}

        @Override public boolean isConfigured() {return helper.isConfigured();}

        @Override public int getDefaultBiome() {return helper.getDefaultBiome();}

        @Override public int getCustomTerrainIndex() {return helper.getCustomTerrainIndex();}

        @Override public int getColour(long seed, int x, int y, int z, int height, Platform platform, ColourScheme colourScheme) {return helper.getColour(seed, x, y, z, height, colourScheme);}

        @Override public int getColour(long seed, int x, int y, float z, int height, Platform platform, ColourScheme colourScheme) {return helper.getColour(seed, x, y, z, height, colourScheme);}

        private final CustomTerrainHelper helper = new CustomTerrainHelper(70);
    },
    CUSTOM_72("Custom 72", "custom material seventy-two", BIOME_PLAINS) {
        @Override public Material getMaterial(Platform platform, long seed, int x, int y, int z, int height) {return helper.getMaterial(seed, x, y, z, height);}

        @Override public Material getMaterial(Platform platform, long seed, int x, int y, float z, int height) {return helper.getMaterial(seed, x, y, z, height);}

        @Override public String getName() {return helper.getName();}

        @Override public BufferedImage getIcon(ColourScheme colourScheme) {return helper.getIcon(colourScheme);}

        @Override public boolean isCustom() {return true;}

        @Override public boolean isConfigured() {return helper.isConfigured();}

        @Override public int getDefaultBiome() {return helper.getDefaultBiome();}

        @Override public int getCustomTerrainIndex() {return helper.getCustomTerrainIndex();}

        @Override public int getColour(long seed, int x, int y, int z, int height, Platform platform, ColourScheme colourScheme) {return helper.getColour(seed, x, y, z, height, colourScheme);}

        @Override public int getColour(long seed, int x, int y, float z, int height, Platform platform, ColourScheme colourScheme) {return helper.getColour(seed, x, y, z, height, colourScheme);}

        private final CustomTerrainHelper helper = new CustomTerrainHelper(71);
    },
    CUSTOM_73("Custom 73", "custom material seventy-three", BIOME_PLAINS) {
        @Override public Material getMaterial(Platform platform, long seed, int x, int y, int z, int height) {return helper.getMaterial(seed, x, y, z, height);}

        @Override public Material getMaterial(Platform platform, long seed, int x, int y, float z, int height) {return helper.getMaterial(seed, x, y, z, height);}

        @Override public String getName() {return helper.getName();}

        @Override public BufferedImage getIcon(ColourScheme colourScheme) {return helper.getIcon(colourScheme);}

        @Override public boolean isCustom() {return true;}

        @Override public boolean isConfigured() {return helper.isConfigured();}

        @Override public int getDefaultBiome() {return helper.getDefaultBiome();}

        @Override public int getCustomTerrainIndex() {return helper.getCustomTerrainIndex();}

        @Override public int getColour(long seed, int x, int y, int z, int height, Platform platform, ColourScheme colourScheme) {return helper.getColour(seed, x, y, z, height, colourScheme);}

        @Override public int getColour(long seed, int x, int y, float z, int height, Platform platform, ColourScheme colourScheme) {return helper.getColour(seed, x, y, z, height, colourScheme);}

        private final CustomTerrainHelper helper = new CustomTerrainHelper(72);
    },
    CUSTOM_74("Custom 74", "custom material seventy-four", BIOME_PLAINS) {
        @Override public Material getMaterial(Platform platform, long seed, int x, int y, int z, int height) {return helper.getMaterial(seed, x, y, z, height);}

        @Override public Material getMaterial(Platform platform, long seed, int x, int y, float z, int height) {return helper.getMaterial(seed, x, y, z, height);}

        @Override public String getName() {return helper.getName();}

        @Override public BufferedImage getIcon(ColourScheme colourScheme) {return helper.getIcon(colourScheme);}

        @Override public boolean isCustom() {return true;}

        @Override public boolean isConfigured() {return helper.isConfigured();}

        @Override public int getDefaultBiome() {return helper.getDefaultBiome();}

        @Override public int getCustomTerrainIndex() {return helper.getCustomTerrainIndex();}

        @Override public int getColour(long seed, int x, int y, int z, int height, Platform platform, ColourScheme colourScheme) {return helper.getColour(seed, x, y, z, height, colourScheme);}

        @Override public int getColour(long seed, int x, int y, float z, int height, Platform platform, ColourScheme colourScheme) {return helper.getColour(seed, x, y, z, height, colourScheme);}

        private final CustomTerrainHelper helper = new CustomTerrainHelper(73);
    },
    CUSTOM_75("Custom 75", "custom material seventy-five", BIOME_PLAINS) {
        @Override public Material getMaterial(Platform platform, long seed, int x, int y, int z, int height) {return helper.getMaterial(seed, x, y, z, height);}

        @Override public Material getMaterial(Platform platform, long seed, int x, int y, float z, int height) {return helper.getMaterial(seed, x, y, z, height);}

        @Override public String getName() {return helper.getName();}

        @Override public BufferedImage getIcon(ColourScheme colourScheme) {return helper.getIcon(colourScheme);}

        @Override public boolean isCustom() {return true;}

        @Override public boolean isConfigured() {return helper.isConfigured();}

        @Override public int getDefaultBiome() {return helper.getDefaultBiome();}

        @Override public int getCustomTerrainIndex() {return helper.getCustomTerrainIndex();}

        @Override public int getColour(long seed, int x, int y, int z, int height, Platform platform, ColourScheme colourScheme) {return helper.getColour(seed, x, y, z, height, colourScheme);}

        @Override public int getColour(long seed, int x, int y, float z, int height, Platform platform, ColourScheme colourScheme) {return helper.getColour(seed, x, y, z, height, colourScheme);}

        private final CustomTerrainHelper helper = new CustomTerrainHelper(74);
    },
    CUSTOM_76("Custom 76", "custom material seventy-six", BIOME_PLAINS) {
        @Override public Material getMaterial(Platform platform, long seed, int x, int y, int z, int height) {return helper.getMaterial(seed, x, y, z, height);}

        @Override public Material getMaterial(Platform platform, long seed, int x, int y, float z, int height) {return helper.getMaterial(seed, x, y, z, height);}

        @Override public String getName() {return helper.getName();}

        @Override public BufferedImage getIcon(ColourScheme colourScheme) {return helper.getIcon(colourScheme);}

        @Override public boolean isCustom() {return true;}

        @Override public boolean isConfigured() {return helper.isConfigured();}

        @Override public int getDefaultBiome() {return helper.getDefaultBiome();}

        @Override public int getCustomTerrainIndex() {return helper.getCustomTerrainIndex();}

        @Override public int getColour(long seed, int x, int y, int z, int height, Platform platform, ColourScheme colourScheme) {return helper.getColour(seed, x, y, z, height, colourScheme);}

        @Override public int getColour(long seed, int x, int y, float z, int height, Platform platform, ColourScheme colourScheme) {return helper.getColour(seed, x, y, z, height, colourScheme);}

        private final CustomTerrainHelper helper = new CustomTerrainHelper(75);
    },
    CUSTOM_77("Custom 77", "custom material seventy-seven", BIOME_PLAINS) {
        @Override public Material getMaterial(Platform platform, long seed, int x, int y, int z, int height) {return helper.getMaterial(seed, x, y, z, height);}

        @Override public Material getMaterial(Platform platform, long seed, int x, int y, float z, int height) {return helper.getMaterial(seed, x, y, z, height);}

        @Override public String getName() {return helper.getName();}

        @Override public BufferedImage getIcon(ColourScheme colourScheme) {return helper.getIcon(colourScheme);}

        @Override public boolean isCustom() {return true;}

        @Override public boolean isConfigured() {return helper.isConfigured();}

        @Override public int getDefaultBiome() {return helper.getDefaultBiome();}

        @Override public int getCustomTerrainIndex() {return helper.getCustomTerrainIndex();}

        @Override public int getColour(long seed, int x, int y, int z, int height, Platform platform, ColourScheme colourScheme) {return helper.getColour(seed, x, y, z, height, colourScheme);}

        @Override public int getColour(long seed, int x, int y, float z, int height, Platform platform, ColourScheme colourScheme) {return helper.getColour(seed, x, y, z, height, colourScheme);}

        private final CustomTerrainHelper helper = new CustomTerrainHelper(76);
    },
    CUSTOM_78("Custom 78", "custom material seventy-eight", BIOME_PLAINS) {
        @Override public Material getMaterial(Platform platform, long seed, int x, int y, int z, int height) {return helper.getMaterial(seed, x, y, z, height);}

        @Override public Material getMaterial(Platform platform, long seed, int x, int y, float z, int height) {return helper.getMaterial(seed, x, y, z, height);}

        @Override public String getName() {return helper.getName();}

        @Override public BufferedImage getIcon(ColourScheme colourScheme) {return helper.getIcon(colourScheme);}

        @Override public boolean isCustom() {return true;}

        @Override public boolean isConfigured() {return helper.isConfigured();}

        @Override public int getDefaultBiome() {return helper.getDefaultBiome();}

        @Override public int getCustomTerrainIndex() {return helper.getCustomTerrainIndex();}

        @Override public int getColour(long seed, int x, int y, int z, int height, Platform platform, ColourScheme colourScheme) {return helper.getColour(seed, x, y, z, height, colourScheme);}

        @Override public int getColour(long seed, int x, int y, float z, int height, Platform platform, ColourScheme colourScheme) {return helper.getColour(seed, x, y, z, height, colourScheme);}

        private final CustomTerrainHelper helper = new CustomTerrainHelper(77);
    },
    CUSTOM_79("Custom 79", "custom material seventy-nine", BIOME_PLAINS) {
        @Override public Material getMaterial(Platform platform, long seed, int x, int y, int z, int height) {return helper.getMaterial(seed, x, y, z, height);}

        @Override public Material getMaterial(Platform platform, long seed, int x, int y, float z, int height) {return helper.getMaterial(seed, x, y, z, height);}

        @Override public String getName() {return helper.getName();}

        @Override public BufferedImage getIcon(ColourScheme colourScheme) {return helper.getIcon(colourScheme);}

        @Override public boolean isCustom() {return true;}

        @Override public boolean isConfigured() {return helper.isConfigured();}

        @Override public int getDefaultBiome() {return helper.getDefaultBiome();}

        @Override public int getCustomTerrainIndex() {return helper.getCustomTerrainIndex();}

        @Override public int getColour(long seed, int x, int y, int z, int height, Platform platform, ColourScheme colourScheme) {return helper.getColour(seed, x, y, z, height, colourScheme);}

        @Override public int getColour(long seed, int x, int y, float z, int height, Platform platform, ColourScheme colourScheme) {return helper.getColour(seed, x, y, z, height, colourScheme);}

        private final CustomTerrainHelper helper = new CustomTerrainHelper(78);
    },
    CUSTOM_80("Custom 80", "custom material eighty", BIOME_PLAINS) {
        @Override public Material getMaterial(Platform platform, long seed, int x, int y, int z, int height) {return helper.getMaterial(seed, x, y, z, height);}

        @Override public Material getMaterial(Platform platform, long seed, int x, int y, float z, int height) {return helper.getMaterial(seed, x, y, z, height);}

        @Override public String getName() {return helper.getName();}

        @Override public BufferedImage getIcon(ColourScheme colourScheme) {return helper.getIcon(colourScheme);}

        @Override public boolean isCustom() {return true;}

        @Override public boolean isConfigured() {return helper.isConfigured();}

        @Override public int getDefaultBiome() {return helper.getDefaultBiome();}

        @Override public int getCustomTerrainIndex() {return helper.getCustomTerrainIndex();}

        @Override public int getColour(long seed, int x, int y, int z, int height, Platform platform, ColourScheme colourScheme) {return helper.getColour(seed, x, y, z, height, colourScheme);}

        @Override public int getColour(long seed, int x, int y, float z, int height, Platform platform, ColourScheme colourScheme) {return helper.getColour(seed, x, y, z, height, colourScheme);}

        private final CustomTerrainHelper helper = new CustomTerrainHelper(79);
    },
    CUSTOM_81("Custom 81", "custom material eighty-one", BIOME_PLAINS) {
        @Override public Material getMaterial(Platform platform, long seed, int x, int y, int z, int height) {return helper.getMaterial(seed, x, y, z, height);}

        @Override public Material getMaterial(Platform platform, long seed, int x, int y, float z, int height) {return helper.getMaterial(seed, x, y, z, height);}

        @Override public String getName() {return helper.getName();}

        @Override public BufferedImage getIcon(ColourScheme colourScheme) {return helper.getIcon(colourScheme);}

        @Override public boolean isCustom() {return true;}

        @Override public boolean isConfigured() {return helper.isConfigured();}

        @Override public int getDefaultBiome() {return helper.getDefaultBiome();}

        @Override public int getCustomTerrainIndex() {return helper.getCustomTerrainIndex();}

        @Override public int getColour(long seed, int x, int y, int z, int height, Platform platform, ColourScheme colourScheme) {return helper.getColour(seed, x, y, z, height, colourScheme);}

        @Override public int getColour(long seed, int x, int y, float z, int height, Platform platform, ColourScheme colourScheme) {return helper.getColour(seed, x, y, z, height, colourScheme);}

        private final CustomTerrainHelper helper = new CustomTerrainHelper(80);
    },
    CUSTOM_82("Custom 82", "custom material eighty-two", BIOME_PLAINS) {
        @Override public Material getMaterial(Platform platform, long seed, int x, int y, int z, int height) {return helper.getMaterial(seed, x, y, z, height);}

        @Override public Material getMaterial(Platform platform, long seed, int x, int y, float z, int height) {return helper.getMaterial(seed, x, y, z, height);}

        @Override public String getName() {return helper.getName();}

        @Override public BufferedImage getIcon(ColourScheme colourScheme) {return helper.getIcon(colourScheme);}

        @Override public boolean isCustom() {return true;}

        @Override public boolean isConfigured() {return helper.isConfigured();}

        @Override public int getDefaultBiome() {return helper.getDefaultBiome();}

        @Override public int getCustomTerrainIndex() {return helper.getCustomTerrainIndex();}

        @Override public int getColour(long seed, int x, int y, int z, int height, Platform platform, ColourScheme colourScheme) {return helper.getColour(seed, x, y, z, height, colourScheme);}

        @Override public int getColour(long seed, int x, int y, float z, int height, Platform platform, ColourScheme colourScheme) {return helper.getColour(seed, x, y, z, height, colourScheme);}

        private final CustomTerrainHelper helper = new CustomTerrainHelper(81);
    },
    CUSTOM_83("Custom 83", "custom material eighty-three", BIOME_PLAINS) {
        @Override public Material getMaterial(Platform platform, long seed, int x, int y, int z, int height) {return helper.getMaterial(seed, x, y, z, height);}

        @Override public Material getMaterial(Platform platform, long seed, int x, int y, float z, int height) {return helper.getMaterial(seed, x, y, z, height);}

        @Override public String getName() {return helper.getName();}

        @Override public BufferedImage getIcon(ColourScheme colourScheme) {return helper.getIcon(colourScheme);}

        @Override public boolean isCustom() {return true;}

        @Override public boolean isConfigured() {return helper.isConfigured();}

        @Override public int getDefaultBiome() {return helper.getDefaultBiome();}

        @Override public int getCustomTerrainIndex() {return helper.getCustomTerrainIndex();}

        @Override public int getColour(long seed, int x, int y, int z, int height, Platform platform, ColourScheme colourScheme) {return helper.getColour(seed, x, y, z, height, colourScheme);}

        @Override public int getColour(long seed, int x, int y, float z, int height, Platform platform, ColourScheme colourScheme) {return helper.getColour(seed, x, y, z, height, colourScheme);}

        private final CustomTerrainHelper helper = new CustomTerrainHelper(82);
    },
    CUSTOM_84("Custom 84", "custom material eighty-four", BIOME_PLAINS) {
        @Override public Material getMaterial(Platform platform, long seed, int x, int y, int z, int height) {return helper.getMaterial(seed, x, y, z, height);}

        @Override public Material getMaterial(Platform platform, long seed, int x, int y, float z, int height) {return helper.getMaterial(seed, x, y, z, height);}

        @Override public String getName() {return helper.getName();}

        @Override public BufferedImage getIcon(ColourScheme colourScheme) {return helper.getIcon(colourScheme);}

        @Override public boolean isCustom() {return true;}

        @Override public boolean isConfigured() {return helper.isConfigured();}

        @Override public int getDefaultBiome() {return helper.getDefaultBiome();}

        @Override public int getCustomTerrainIndex() {return helper.getCustomTerrainIndex();}

        @Override public int getColour(long seed, int x, int y, int z, int height, Platform platform, ColourScheme colourScheme) {return helper.getColour(seed, x, y, z, height, colourScheme);}

        @Override public int getColour(long seed, int x, int y, float z, int height, Platform platform, ColourScheme colourScheme) {return helper.getColour(seed, x, y, z, height, colourScheme);}

        private final CustomTerrainHelper helper = new CustomTerrainHelper(83);
    },
    CUSTOM_85("Custom 85", "custom material eighty-five", BIOME_PLAINS) {
        @Override public Material getMaterial(Platform platform, long seed, int x, int y, int z, int height) {return helper.getMaterial(seed, x, y, z, height);}

        @Override public Material getMaterial(Platform platform, long seed, int x, int y, float z, int height) {return helper.getMaterial(seed, x, y, z, height);}

        @Override public String getName() {return helper.getName();}

        @Override public BufferedImage getIcon(ColourScheme colourScheme) {return helper.getIcon(colourScheme);}

        @Override public boolean isCustom() {return true;}

        @Override public boolean isConfigured() {return helper.isConfigured();}

        @Override public int getDefaultBiome() {return helper.getDefaultBiome();}

        @Override public int getCustomTerrainIndex() {return helper.getCustomTerrainIndex();}

        @Override public int getColour(long seed, int x, int y, int z, int height, Platform platform, ColourScheme colourScheme) {return helper.getColour(seed, x, y, z, height, colourScheme);}

        @Override public int getColour(long seed, int x, int y, float z, int height, Platform platform, ColourScheme colourScheme) {return helper.getColour(seed, x, y, z, height, colourScheme);}

        private final CustomTerrainHelper helper = new CustomTerrainHelper(84);
    },
    CUSTOM_86("Custom 86", "custom material eighty-six", BIOME_PLAINS) {
        @Override public Material getMaterial(Platform platform, long seed, int x, int y, int z, int height) {return helper.getMaterial(seed, x, y, z, height);}

        @Override public Material getMaterial(Platform platform, long seed, int x, int y, float z, int height) {return helper.getMaterial(seed, x, y, z, height);}

        @Override public String getName() {return helper.getName();}

        @Override public BufferedImage getIcon(ColourScheme colourScheme) {return helper.getIcon(colourScheme);}

        @Override public boolean isCustom() {return true;}

        @Override public boolean isConfigured() {return helper.isConfigured();}

        @Override public int getDefaultBiome() {return helper.getDefaultBiome();}

        @Override public int getCustomTerrainIndex() {return helper.getCustomTerrainIndex();}

        @Override public int getColour(long seed, int x, int y, int z, int height, Platform platform, ColourScheme colourScheme) {return helper.getColour(seed, x, y, z, height, colourScheme);}

        @Override public int getColour(long seed, int x, int y, float z, int height, Platform platform, ColourScheme colourScheme) {return helper.getColour(seed, x, y, z, height, colourScheme);}

        private final CustomTerrainHelper helper = new CustomTerrainHelper(85);
    },
    CUSTOM_87("Custom 87", "custom material eighty-seven", BIOME_PLAINS) {
        @Override public Material getMaterial(Platform platform, long seed, int x, int y, int z, int height) {return helper.getMaterial(seed, x, y, z, height);}

        @Override public Material getMaterial(Platform platform, long seed, int x, int y, float z, int height) {return helper.getMaterial(seed, x, y, z, height);}

        @Override public String getName() {return helper.getName();}

        @Override public BufferedImage getIcon(ColourScheme colourScheme) {return helper.getIcon(colourScheme);}

        @Override public boolean isCustom() {return true;}

        @Override public boolean isConfigured() {return helper.isConfigured();}

        @Override public int getDefaultBiome() {return helper.getDefaultBiome();}

        @Override public int getCustomTerrainIndex() {return helper.getCustomTerrainIndex();}

        @Override public int getColour(long seed, int x, int y, int z, int height, Platform platform, ColourScheme colourScheme) {return helper.getColour(seed, x, y, z, height, colourScheme);}

        @Override public int getColour(long seed, int x, int y, float z, int height, Platform platform, ColourScheme colourScheme) {return helper.getColour(seed, x, y, z, height, colourScheme);}

        private final CustomTerrainHelper helper = new CustomTerrainHelper(86);
    },
    CUSTOM_88("Custom 88", "custom material eighty-eight", BIOME_PLAINS) {
        @Override public Material getMaterial(Platform platform, long seed, int x, int y, int z, int height) {return helper.getMaterial(seed, x, y, z, height);}

        @Override public Material getMaterial(Platform platform, long seed, int x, int y, float z, int height) {return helper.getMaterial(seed, x, y, z, height);}

        @Override public String getName() {return helper.getName();}

        @Override public BufferedImage getIcon(ColourScheme colourScheme) {return helper.getIcon(colourScheme);}

        @Override public boolean isCustom() {return true;}

        @Override public boolean isConfigured() {return helper.isConfigured();}

        @Override public int getDefaultBiome() {return helper.getDefaultBiome();}

        @Override public int getCustomTerrainIndex() {return helper.getCustomTerrainIndex();}

        @Override public int getColour(long seed, int x, int y, int z, int height, Platform platform, ColourScheme colourScheme) {return helper.getColour(seed, x, y, z, height, colourScheme);}

        @Override public int getColour(long seed, int x, int y, float z, int height, Platform platform, ColourScheme colourScheme) {return helper.getColour(seed, x, y, z, height, colourScheme);}

        private final CustomTerrainHelper helper = new CustomTerrainHelper(87);
    },
    CUSTOM_89("Custom 89", "custom material eighty-nine", BIOME_PLAINS) {
        @Override public Material getMaterial(Platform platform, long seed, int x, int y, int z, int height) {return helper.getMaterial(seed, x, y, z, height);}

        @Override public Material getMaterial(Platform platform, long seed, int x, int y, float z, int height) {return helper.getMaterial(seed, x, y, z, height);}

        @Override public String getName() {return helper.getName();}

        @Override public BufferedImage getIcon(ColourScheme colourScheme) {return helper.getIcon(colourScheme);}

        @Override public boolean isCustom() {return true;}

        @Override public boolean isConfigured() {return helper.isConfigured();}

        @Override public int getDefaultBiome() {return helper.getDefaultBiome();}

        @Override public int getCustomTerrainIndex() {return helper.getCustomTerrainIndex();}

        @Override public int getColour(long seed, int x, int y, int z, int height, Platform platform, ColourScheme colourScheme) {return helper.getColour(seed, x, y, z, height, colourScheme);}

        @Override public int getColour(long seed, int x, int y, float z, int height, Platform platform, ColourScheme colourScheme) {return helper.getColour(seed, x, y, z, height, colourScheme);}

        private final CustomTerrainHelper helper = new CustomTerrainHelper(88);
    },
    CUSTOM_90("Custom 90", "custom material ninety", BIOME_PLAINS) {
        @Override public Material getMaterial(Platform platform, long seed, int x, int y, int z, int height) {return helper.getMaterial(seed, x, y, z, height);}

        @Override public Material getMaterial(Platform platform, long seed, int x, int y, float z, int height) {return helper.getMaterial(seed, x, y, z, height);}

        @Override public String getName() {return helper.getName();}

        @Override public BufferedImage getIcon(ColourScheme colourScheme) {return helper.getIcon(colourScheme);}

        @Override public boolean isCustom() {return true;}

        @Override public boolean isConfigured() {return helper.isConfigured();}

        @Override public int getDefaultBiome() {return helper.getDefaultBiome();}

        @Override public int getCustomTerrainIndex() {return helper.getCustomTerrainIndex();}

        @Override public int getColour(long seed, int x, int y, int z, int height, Platform platform, ColourScheme colourScheme) {return helper.getColour(seed, x, y, z, height, colourScheme);}

        @Override public int getColour(long seed, int x, int y, float z, int height, Platform platform, ColourScheme colourScheme) {return helper.getColour(seed, x, y, z, height, colourScheme);}

        private final CustomTerrainHelper helper = new CustomTerrainHelper(89);
    },
    CUSTOM_91("Custom 91", "custom material ninety-one", BIOME_PLAINS) {
        @Override public Material getMaterial(Platform platform, long seed, int x, int y, int z, int height) {return helper.getMaterial(seed, x, y, z, height);}

        @Override public Material getMaterial(Platform platform, long seed, int x, int y, float z, int height) {return helper.getMaterial(seed, x, y, z, height);}

        @Override public String getName() {return helper.getName();}

        @Override public BufferedImage getIcon(ColourScheme colourScheme) {return helper.getIcon(colourScheme);}

        @Override public boolean isCustom() {return true;}

        @Override public boolean isConfigured() {return helper.isConfigured();}

        @Override public int getDefaultBiome() {return helper.getDefaultBiome();}

        @Override public int getCustomTerrainIndex() {return helper.getCustomTerrainIndex();}

        @Override public int getColour(long seed, int x, int y, int z, int height, Platform platform, ColourScheme colourScheme) {return helper.getColour(seed, x, y, z, height, colourScheme);}

        @Override public int getColour(long seed, int x, int y, float z, int height, Platform platform, ColourScheme colourScheme) {return helper.getColour(seed, x, y, z, height, colourScheme);}

        private final CustomTerrainHelper helper = new CustomTerrainHelper(90);
    },
    CUSTOM_92("Custom 92", "custom material ninety-two", BIOME_PLAINS) {
        @Override public Material getMaterial(Platform platform, long seed, int x, int y, int z, int height) {return helper.getMaterial(seed, x, y, z, height);}

        @Override public Material getMaterial(Platform platform, long seed, int x, int y, float z, int height) {return helper.getMaterial(seed, x, y, z, height);}

        @Override public String getName() {return helper.getName();}

        @Override public BufferedImage getIcon(ColourScheme colourScheme) {return helper.getIcon(colourScheme);}

        @Override public boolean isCustom() {return true;}

        @Override public boolean isConfigured() {return helper.isConfigured();}

        @Override public int getDefaultBiome() {return helper.getDefaultBiome();}

        @Override public int getCustomTerrainIndex() {return helper.getCustomTerrainIndex();}

        @Override public int getColour(long seed, int x, int y, int z, int height, Platform platform, ColourScheme colourScheme) {return helper.getColour(seed, x, y, z, height, colourScheme);}

        @Override public int getColour(long seed, int x, int y, float z, int height, Platform platform, ColourScheme colourScheme) {return helper.getColour(seed, x, y, z, height, colourScheme);}

        private final CustomTerrainHelper helper = new CustomTerrainHelper(91);
    },
    CUSTOM_93("Custom 93", "custom material ninety-three", BIOME_PLAINS) {
        @Override public Material getMaterial(Platform platform, long seed, int x, int y, int z, int height) {return helper.getMaterial(seed, x, y, z, height);}

        @Override public Material getMaterial(Platform platform, long seed, int x, int y, float z, int height) {return helper.getMaterial(seed, x, y, z, height);}

        @Override public String getName() {return helper.getName();}

        @Override public BufferedImage getIcon(ColourScheme colourScheme) {return helper.getIcon(colourScheme);}

        @Override public boolean isCustom() {return true;}

        @Override public boolean isConfigured() {return helper.isConfigured();}

        @Override public int getDefaultBiome() {return helper.getDefaultBiome();}

        @Override public int getCustomTerrainIndex() {return helper.getCustomTerrainIndex();}

        @Override public int getColour(long seed, int x, int y, int z, int height, Platform platform, ColourScheme colourScheme) {return helper.getColour(seed, x, y, z, height, colourScheme);}

        @Override public int getColour(long seed, int x, int y, float z, int height, Platform platform, ColourScheme colourScheme) {return helper.getColour(seed, x, y, z, height, colourScheme);}

        private final CustomTerrainHelper helper = new CustomTerrainHelper(92);
    },
    CUSTOM_94("Custom 94", "custom material ninety-four", BIOME_PLAINS) {
        @Override public Material getMaterial(Platform platform, long seed, int x, int y, int z, int height) {return helper.getMaterial(seed, x, y, z, height);}

        @Override public Material getMaterial(Platform platform, long seed, int x, int y, float z, int height) {return helper.getMaterial(seed, x, y, z, height);}

        @Override public String getName() {return helper.getName();}

        @Override public BufferedImage getIcon(ColourScheme colourScheme) {return helper.getIcon(colourScheme);}

        @Override public boolean isCustom() {return true;}

        @Override public boolean isConfigured() {return helper.isConfigured();}

        @Override public int getDefaultBiome() {return helper.getDefaultBiome();}

        @Override public int getCustomTerrainIndex() {return helper.getCustomTerrainIndex();}

        @Override public int getColour(long seed, int x, int y, int z, int height, Platform platform, ColourScheme colourScheme) {return helper.getColour(seed, x, y, z, height, colourScheme);}

        @Override public int getColour(long seed, int x, int y, float z, int height, Platform platform, ColourScheme colourScheme) {return helper.getColour(seed, x, y, z, height, colourScheme);}

        private final CustomTerrainHelper helper = new CustomTerrainHelper(93);
    },
    CUSTOM_95("Custom 95", "custom material ninety-five", BIOME_PLAINS) {
        @Override public Material getMaterial(Platform platform, long seed, int x, int y, int z, int height) {return helper.getMaterial(seed, x, y, z, height);}

        @Override public Material getMaterial(Platform platform, long seed, int x, int y, float z, int height) {return helper.getMaterial(seed, x, y, z, height);}

        @Override public String getName() {return helper.getName();}

        @Override public BufferedImage getIcon(ColourScheme colourScheme) {return helper.getIcon(colourScheme);}

        @Override public boolean isCustom() {return true;}

        @Override public boolean isConfigured() {return helper.isConfigured();}

        @Override public int getDefaultBiome() {return helper.getDefaultBiome();}

        @Override public int getCustomTerrainIndex() {return helper.getCustomTerrainIndex();}

        @Override public int getColour(long seed, int x, int y, int z, int height, Platform platform, ColourScheme colourScheme) {return helper.getColour(seed, x, y, z, height, colourScheme);}

        @Override public int getColour(long seed, int x, int y, float z, int height, Platform platform, ColourScheme colourScheme) {return helper.getColour(seed, x, y, z, height, colourScheme);}

        private final CustomTerrainHelper helper = new CustomTerrainHelper(94);
    },
    CUSTOM_96("Custom 96", "custom material ninety-six", BIOME_PLAINS) {
        @Override public Material getMaterial(Platform platform, long seed, int x, int y, int z, int height) {return helper.getMaterial(seed, x, y, z, height);}

        @Override public Material getMaterial(Platform platform, long seed, int x, int y, float z, int height) {return helper.getMaterial(seed, x, y, z, height);}

        @Override public String getName() {return helper.getName();}

        @Override public BufferedImage getIcon(ColourScheme colourScheme) {return helper.getIcon(colourScheme);}

        @Override public boolean isCustom() {return true;}

        @Override public boolean isConfigured() {return helper.isConfigured();}

        @Override public int getDefaultBiome() {return helper.getDefaultBiome();}

        @Override public int getCustomTerrainIndex() {return helper.getCustomTerrainIndex();}

        @Override public int getColour(long seed, int x, int y, int z, int height, Platform platform, ColourScheme colourScheme) {return helper.getColour(seed, x, y, z, height, colourScheme);}

        @Override public int getColour(long seed, int x, int y, float z, int height, Platform platform, ColourScheme colourScheme) {return helper.getColour(seed, x, y, z, height, colourScheme);}

        private final CustomTerrainHelper helper = new CustomTerrainHelper(95);
    },
    DEEPSLATE("Deepslate", DEEPSLATE_Y, DEEPSLATE_Y, "deepslate", BIOME_PLAINS),
    TUFF("Tuff", Material.TUFF, Material.TUFF, "tuff", BIOME_PLAINS),
    BASALT("Basalt", Material.BASALT, Material.BASALT, "basalt", BIOME_HELL),
    BLACKSTONE("Blackstone", Material.BLACKSTONE, Material.BLACKSTONE, "blackstone", BIOME_HELL),
    SOUL_SOIL("Soul Soil", Material.SOUL_SOIL, Material.SOUL_SOIL, "soul soil", BIOME_HELL),
    WARPED_NYLIUM("Warped Nylium", Material.WARPED_NYLIUM, Material.NETHERRACK, "warped nylium", BIOME_HELL),
    CRIMSON_NYLIUM("Crimson Nylium", Material.CRIMSON_NYLIUM, Material.NETHERRACK, "crimson nylium", BIOME_HELL),
    CALCITE("Calcite", Material.CALCITE, Material.CALCITE, "calcite", BIOME_PLAINS),
    MUD("Mud", Material.MUD, Material.MUD, "mud", BIOME_PLAINS),
    BARE_BEACHES("Bare Beaches", "grass with patches of sand, gravel and clay, and no plants", BIOME_BEACH) {
        @Override
        public Material getMaterial(Platform platform, long seed, int x, int y, int z, int height) {
            if (sandNoise.getSeed() != (seed + SAND_SEED_OFFSET)) {
                sandNoise.setSeed(seed + SAND_SEED_OFFSET);
                clayNoise.setSeed(seed + CLAY_SEED_OFFSET);
            }
            float noise = clayNoise.getPerlinNoise(x / SMALL_BLOBS, y / SMALL_BLOBS, z / SMALL_BLOBS);
            if (noise >= BEACH_CLAY_CHANCE) {
                return Material.CLAY;
            } else {
                noise = sandNoise.getPerlinNoise(x / HUGE_BLOBS, y / HUGE_BLOBS, z / SMALL_BLOBS);
                noise += sandNoise.getPerlinNoise(x / SMALL_BLOBS, y / SMALL_BLOBS, z / SMALL_BLOBS) / 2;
                if (noise >= BEACH_SAND_CHANCE) {
                    return Material.SAND;
                } else if (-noise >= BEACH_GRAVEL_CHANCE) {
                    return Material.GRAVEL;
                } else {
                    return Material.GRASS_BLOCK;
                }
            }
        }

        private final PerlinNoise sandNoise = new PerlinNoise(0);
        private final PerlinNoise clayNoise = new PerlinNoise(0);

        private static final long SAND_SEED_OFFSET = 26796036;
        private static final long CLAY_SEED_OFFSET = 161603308;
    },
    MOSS("Moss", MOSS_BLOCK, MOSS_BLOCK, "moss", BIOME_PLAINS);

    Terrain(String name, String description, int defaultBiome) {
        this(name, Material.STONE, Material.STONE, description, defaultBiome);
    }

    Terrain(String name, String description, int defaultBiome, int toppingHeight) {
        this(name, Material.STONE, Material.STONE, description, defaultBiome);
    }

    Terrain(String name, int topMaterial, int topLayerMaterial, String description, int defaultBiome) {
        this(name, Material.get(topMaterial), Material.get(topLayerMaterial), description, defaultBiome);
    }

    Terrain(String name, Material topMaterial, Material topLayerMaterial, String description, int defaultBiome) {
        this.name = name;
        this.topMaterial = topMaterial;
        this.topLayerMaterial = topLayerMaterial;
        this.description = description;
        this.defaultBiome = defaultBiome;
        icon = IconUtils.loadUnscaledImage("org/pepsoft/worldpainter/icons/" + name().toLowerCase() + ".png");
    }

    public String getName() {
        return name;
    }

    /**
     * Get the material to use for this terrain type at a specific location in
     * the world, relative to the surface, for a specific platform.
     *
     * <p>The default implementation forwards to
     * {@link #getMaterial(Platform, long, int, int, int, int)}.
     *
     * @param platform The platform for which to get the block type.
     * @param seed The world seed.
     * @param x The absolute X position of the block in WorldPainter coordinates.
     * @param y The absolute Y position of the block in WorldPainter coordinates.
     * @param z The absolute Z position of the block in WorldPainter coordinates.
     * @param height The height of the terrain at the specified X and Y
     *     coordinates.
     * @return The material at the specified location in the terrain.
     */
    public Material getMaterial(final Platform platform, final long seed, final int x, final int y, final float z, final int height) {
        return getMaterial(platform, seed, x, y, Math.round(z), height);
    }

    /**
     * Get the material to use for this terrain type at a specific location in
     * the world, relative to the surface, for a specific platform.
     *
     * @param platform The platform for which to get the block type.
     * @param seed The world seed.
     * @param x The absolute X position of the block in WorldPainter coordinates.
     * @param y The absolute Y position of the block in WorldPainter coordinates.
     * @param z The absolute Z position of the block in WorldPainter coordinates.
     * @param height The height of the terrain at the specified X and Y
     *     coordinates.
     * @return The material at the specified location in the terrain.
     */
    public Material getMaterial(final Platform platform, final long seed, final int x, final int y, final int z, final int height) {
        final int dz = z - height;
        if (dz > 0) {
            return Material.AIR;
        } else if (dz == 0) {
            return topMaterial;
        } else {
            return topLayerMaterial;
        }
    }

    // TODO: override this where necessary before seriously using this!
    public Set<Material> getAllMaterials() {
        return (topMaterial == topLayerMaterial) ? singleton(topMaterial) : ImmutableSet.of(topMaterial, topLayerMaterial);
    }

    /**
     * Get the {@link WPObject object}, if any (such as a {@link Plant}), to place on top of the terrain at the
     * specified coordinates. The default implementation always returns {@code null}.
     *
     * <p>These objects must be small and simple, and in particular they must consist of one block or column of blocks.
     * It may not extend sideways, as these objects are placed during the first export phase. Tile and block entities
     * are ignored.
     */
    public WPObject getSurfaceObject(Platform platform, long seed, int x, int y, int waterBlocksAbove) {
        return null;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Get the unscaled icon. This icon has an unspecified size.
     */
    public BufferedImage getIcon(ColourScheme colourScheme) {
        return icon;
    }

    /**
     * Get the icon, scaled to the specified size, adjusted for the current GUI scale.
     */
    public final BufferedImage getScaledIcon(int size, ColourScheme colourScheme) {
        return IconUtils.scaleIcon(getIcon(colourScheme), size);
    }

    public int getColour(final long seed, final int x, final int y, final float z, final int height, final Platform platform, final ColourScheme colourScheme) {
        try {
            return colourScheme.getColour(getMaterial(platform, seed, x, y, z, height));
        } catch (RuntimeException e) {
            throw new RuntimeException(e.getClass().getSimpleName() + " while getting colour of material " + getMaterial(platform, seed, x, y, z, height) + " @ " + x + "," + y + "," + z + "," + height + " for terrain " + this, e);
        }
    }

    public int getColour(final long seed, final int x, final int y, final int z, final int height, final Platform platform, final ColourScheme colourScheme) {
        try {
            return colourScheme.getColour(getMaterial(platform, seed, x, y, z, height));
        } catch (RuntimeException e) {
            throw new RuntimeException(e.getClass().getSimpleName() + " while getting colour of material " + getMaterial(platform, seed, x, y, z, height) + " @ " + x + "," + y + "," + z + "," + height + " for terrain " + this, e);
        }
    }

    public int getDefaultBiome() {
        return defaultBiome;
    }

    public boolean isCustom() {
        return false;
    }

    public boolean isConfigured() {
        return true;
    }

    public int getCustomTerrainIndex() {
        throw new IllegalArgumentException("Not a custom terrain");
    }

    // Object

    @Override
    public String toString() {
        return name;
    }

    public static boolean isCustomMaterialConfigured(int index) {
        return customMaterials[index] != null;
    }

    public static int getConfiguredCustomMaterialCount() {
        return (int) Arrays.stream(customMaterials).filter(Objects::nonNull).count();
    }

    public static MixedMaterial getCustomMaterial(int index) {
        return customMaterials[index];
    }
    
    public static void setCustomMaterial(int index, MixedMaterial material) {
        customMaterials[index] = material;
    }
    
    public static Terrain getCustomTerrain(int index) {
        return (index < 48)
                ? ((index < 24)
                    ? VALUES[index + 47]
                    : VALUES[index + 52])
                : VALUES[index + 54];
    }
    
    public static Terrain[] getConfiguredValues() {
        final ArrayList<Terrain> values = new ArrayList<>(VALUES.length);
        values.addAll(asList(PICK_LIST));
        final List<Terrain> customValues = new ArrayList<>(96);
        for (Terrain terrain: VALUES) {
            if (terrain.isCustom() && terrain.isConfigured()) {
                customValues.add(terrain);
            }
        }
        if (! customValues.isEmpty()) {
            customValues.sort(comparing(Terrain::getName));
            values.addAll(customValues);
        }
        return values.toArray(new Terrain[values.size()]);
    }

    private final Material topMaterial, topLayerMaterial;
    private final String name, description;
    private final BufferedImage icon;
    private final int defaultBiome;
    
    public static final int CUSTOM_TERRAIN_COUNT = 96;

    static final MixedMaterial[] customMaterials = new MixedMaterial[CUSTOM_TERRAIN_COUNT];

    static final int GOLD_LEVEL         = 32;
    static final int IRON_LEVEL         = 48;
    static final int COAL_LEVEL         = Integer.MAX_VALUE;
    static final int LAPIS_LAZULI_LEVEL = 32;
    static final int DIAMOND_LEVEL      = 16;
    static final int REDSTONE_LEVEL     = 16;
    static final int WATER_LEVEL        = Integer.MAX_VALUE;
    static final int LAVA_LEVEL         = 80;
    static final int DIRT_LEVEL         = Integer.MAX_VALUE;
    static final int GRAVEL_LEVEL       = Integer.MAX_VALUE;
        
    static final float GOLD_CHANCE         = PerlinNoise.getLevelForPromillage(1);
    static final float IRON_CHANCE         = PerlinNoise.getLevelForPromillage(5);
    static final float COAL_CHANCE         = PerlinNoise.getLevelForPromillage(9);
    static final float LAPIS_LAZULI_CHANCE = PerlinNoise.getLevelForPromillage(1);
    static final float DIAMOND_CHANCE      = PerlinNoise.getLevelForPromillage(1);
    static final float REDSTONE_CHANCE     = PerlinNoise.getLevelForPromillage(6);
    static final float WATER_CHANCE        = PerlinNoise.getLevelForPromillage(1);
    static final float LAVA_CHANCE         = PerlinNoise.getLevelForPromillage(1);
    static final float DIRT_CHANCE         = PerlinNoise.getLevelForPromillage(9);
    static final float GRAVEL_CHANCE       = PerlinNoise.getLevelForPromillage(9);
    
    static final float FLOWER_CHANCE       = PerlinNoise.getLevelForPromillage(10);
    static final float FERN_CHANCE         = PerlinNoise.getLevelForPromillage(10);
    static final float GRASS_CHANCE        = PerlinNoise.getLevelForPromillage(100);

    static final float DOUBLE_TALL_GRASS_CHANCE = PerlinNoise.getLevelForPromillage(50);
    
    static final float BEACH_SAND_CHANCE   = PerlinNoise.getLevelForPromillage(400) * 1.5f;
    static final float BEACH_GRAVEL_CHANCE = PerlinNoise.getLevelForPromillage(200) * 1.5f;
    static final float BEACH_CLAY_CHANCE   = PerlinNoise.getLevelForPromillage(40);

    static final float GRANITE_CHANCE  = PerlinNoise.getLevelForPromillage(45);
    static final float DIORITE_CHANCE  = PerlinNoise.getLevelForPromillage(45);
    static final float ANDESITE_CHANCE = PerlinNoise.getLevelForPromillage(45);

    /**
     * This information is now public, so don't change it! Only add new values
     * at the end!
     */
    public static final Terrain[] VALUES = {
        GRASS,
        BARE_GRASS,
        DIRT,
        PERMADIRT,
        PODZOL,
        SAND,
        RED_SAND,
        DESERT,
        RED_DESERT,
        MESA,

        HARDENED_CLAY,
        WHITE_STAINED_CLAY,
        ORANGE_STAINED_CLAY,
        MAGENTA_STAINED_CLAY,
        LIGHT_BLUE_STAINED_CLAY,
        YELLOW_STAINED_CLAY,
        LIME_STAINED_CLAY,
        PINK_STAINED_CLAY,
        GREY_STAINED_CLAY,
        LIGHT_GREY_STAINED_CLAY,

        CYAN_STAINED_CLAY,
        PURPLE_STAINED_CLAY,
        BLUE_STAINED_CLAY,
        BROWN_STAINED_CLAY,
        GREEN_STAINED_CLAY,
        RED_STAINED_CLAY,
        BLACK_STAINED_CLAY,
        SANDSTONE,
        STONE,
        ROCK,

        COBBLESTONE,
        MOSSY_COBBLESTONE,
        OBSIDIAN,
        BEDROCK,
        GRAVEL,
        CLAY,
        BEACHES,
        WATER,
        LAVA,
        SNOW,

        DEEP_SNOW,
        NETHERRACK,
        SOUL_SAND,
        NETHERLIKE,
        MYCELIUM,
        END_STONE,
        RESOURCES,
        CUSTOM_1,
        CUSTOM_2,
        CUSTOM_3,

        CUSTOM_4,
        CUSTOM_5,
        CUSTOM_6,
        CUSTOM_7,
        CUSTOM_8,
        CUSTOM_9,
        CUSTOM_10,
        CUSTOM_11,
        CUSTOM_12,
        CUSTOM_13,

        CUSTOM_14,
        CUSTOM_15,
        CUSTOM_16,
        CUSTOM_17,
        CUSTOM_18,
        CUSTOM_19,
        CUSTOM_20,
        CUSTOM_21,
        CUSTOM_22,
        CUSTOM_23,

        CUSTOM_24,
        RED_SANDSTONE,
        GRANITE,
        DIORITE,
        ANDESITE,
        STONE_MIX,
        CUSTOM_25,
        CUSTOM_26,
        CUSTOM_27,
        CUSTOM_28,

        CUSTOM_29,
        CUSTOM_30,
        CUSTOM_31,
        CUSTOM_32,
        CUSTOM_33,
        CUSTOM_34,
        CUSTOM_35,
        CUSTOM_36,
        CUSTOM_37,
        CUSTOM_38,

        CUSTOM_39,
        CUSTOM_40,
        CUSTOM_41,
        CUSTOM_42,
        CUSTOM_43,
        CUSTOM_44,
        CUSTOM_45,
        CUSTOM_46,
        CUSTOM_47,
        CUSTOM_48,

        GRASS_PATH,
        MAGMA,
        CUSTOM_49,
        CUSTOM_50,
        CUSTOM_51,
        CUSTOM_52,
        CUSTOM_53,
        CUSTOM_54,
        CUSTOM_55,
        CUSTOM_56,

        CUSTOM_57,
        CUSTOM_58,
        CUSTOM_59,
        CUSTOM_60,
        CUSTOM_61,
        CUSTOM_62,
        CUSTOM_63,
        CUSTOM_64,
        CUSTOM_65,
        CUSTOM_66,

        CUSTOM_67,
        CUSTOM_68,
        CUSTOM_69,
        CUSTOM_70,
        CUSTOM_71,
        CUSTOM_72,
        CUSTOM_73,
        CUSTOM_74,
        CUSTOM_75,
        CUSTOM_76,

        CUSTOM_77,
        CUSTOM_78,
        CUSTOM_79,
        CUSTOM_80,
        CUSTOM_81,
        CUSTOM_82,
        CUSTOM_83,
        CUSTOM_84,
        CUSTOM_85,
        CUSTOM_86,

        CUSTOM_87,
        CUSTOM_88,
        CUSTOM_89,
        CUSTOM_90,
        CUSTOM_91,
        CUSTOM_92,
        CUSTOM_93,
        CUSTOM_94,
        CUSTOM_95,
        CUSTOM_96,
            
        DEEPSLATE,
        TUFF,
        BASALT,
        BLACKSTONE,
        SOUL_SOIL,
        WARPED_NYLIUM,
        CRIMSON_NYLIUM,
        CALCITE,
        MUD,
        BARE_BEACHES,

        MOSS
    };

    /**
     * This list is meant to present to the user. It has a more logical order
     * and lacks the custom and deprecated terrain types. This list may be
     * changed in any way.
     */
    public static final Terrain[] PICK_LIST = {
        GRASS,
        BARE_GRASS,
        GRASS_PATH,
        DIRT,
        PERMADIRT,
        PODZOL,
        MOSS,
        MUD,
        SAND,
        RED_SAND,

        DESERT,
        RED_DESERT,
        MESA,
        HARDENED_CLAY,
        SANDSTONE,
        RED_SANDSTONE,
        STONE_MIX,
        STONE,
        GRANITE,
        DIORITE,

        ANDESITE,
        CALCITE,
        ROCK,
        COBBLESTONE,
        MOSSY_COBBLESTONE,
        OBSIDIAN,
        DEEPSLATE,
        TUFF,
        BEDROCK,
        GRAVEL,

        CLAY,
        BEACHES,
        WATER,
        LAVA,
        MAGMA,
        DEEP_SNOW,
        NETHERRACK,
        BASALT,
        BLACKSTONE,
        SOUL_SAND,

        SOUL_SOIL,
        NETHERLIKE,
        WARPED_NYLIUM,
        CRIMSON_NYLIUM,
        MYCELIUM,
        END_STONE,
        WHITE_STAINED_CLAY,
        ORANGE_STAINED_CLAY,
        MAGENTA_STAINED_CLAY,
        LIGHT_BLUE_STAINED_CLAY,

        YELLOW_STAINED_CLAY,
        LIME_STAINED_CLAY,
        PINK_STAINED_CLAY,
        GREY_STAINED_CLAY,
        LIGHT_GREY_STAINED_CLAY,
        CYAN_STAINED_CLAY,
        PURPLE_STAINED_CLAY,
        BLUE_STAINED_CLAY,
        BROWN_STAINED_CLAY,
        GREEN_STAINED_CLAY,

        RED_STAINED_CLAY,
        BLACK_STAINED_CLAY
    };

    public static final Set<Terrain> STAINED_TERRACOTTAS = ImmutableSet.of(WHITE_STAINED_CLAY, ORANGE_STAINED_CLAY,
            MAGENTA_STAINED_CLAY, LIGHT_BLUE_STAINED_CLAY, YELLOW_STAINED_CLAY, LIME_STAINED_CLAY, PINK_STAINED_CLAY,
            GREY_STAINED_CLAY, LIGHT_GREY_STAINED_CLAY, CYAN_STAINED_CLAY, PURPLE_STAINED_CLAY, BLUE_STAINED_CLAY,
            BROWN_STAINED_CLAY, GREEN_STAINED_CLAY, RED_STAINED_CLAY, BLACK_STAINED_CLAY);

    /*
     * A helper method for generating additional custom terrain types. Should be
     * edited before use.
     */
//    public static void main(String[] args) {
//        String[] tens = {"forty", "fifty", "sixty", "seventy", "eighty", "ninety"};
//        String[] ones = {"", "-one", "-two", "-three", "-four", "-five", "-six", "-seven", "-eight", "-nine"};
//        for (int i = 49; i <= 96; i++) {
//            System.out.printf("    CUSTOM_%1$d(\"Custom %1$d\", \"custom material %3$s%4$s\", BIOME_PLAINS) {%n" +
//                            "        @Override public Material getMaterial(long seed, int x, int y, int z, int height) {return helper.getMaterial(seed, x, y, z, height);}%n" +
//                            "%n" +
//                            "        @Override public Material getMaterial(long seed, int x, int y, float z, int height) {return helper.getMaterial(seed, x, y, z, height);}%n" +
//                            "%n" +
//                            "        @Override public String getName() {return helper.getName();}%n" +
//                            "%n" +
//                            "        @Override public BufferedImage getIcon(ColourScheme colourScheme) {return helper.getIcon(colourScheme);}%n" +
//                            "%n" +
//                            "        @Override public boolean isCustom() {return true;}%n" +
//                            "%n" +
//                            "        @Override public boolean isConfigured() {return helper.isConfigured();}%n" +
//                            "%n" +
//                            "        @Override public int getDefaultBiome() {return helper.getDefaultBiome();}%n" +
//                            "%n" +
//                            "        @Override public int getCustomTerrainIndex() {return helper.getCustomTerrainIndex();}%n" +
//                            "%n" +
//                            "        @Override public int getColour(long seed, int x, int y, int z, int height, ColourScheme colourScheme) {return helper.getColour(seed, x, y, z, height, colourScheme);}%n" +
//                            "%n" +
//                            "        @Override public int getColour(long seed, int x, int y, float z, int height, ColourScheme colourScheme) {return helper.getColour(seed, x, y, z, height, colourScheme);}%n" +
//                            "%n" +
//                            "        private final CustomTerrainHelper helper = new CustomTerrainHelper(%2$d);%n" +
//                            "    },%n",
//                    i,
//                    i - 1,
//                    tens[(i / 10) - 4],
//                    ones[i % 10]);
//        }
//        for (int i = 49; i <= 96; i++) {
//            System.out.println("       Terrain.CUSTOM_" + i);
//        }
//    }
}