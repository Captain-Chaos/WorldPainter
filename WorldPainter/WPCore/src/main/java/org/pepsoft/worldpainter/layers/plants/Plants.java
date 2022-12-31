package org.pepsoft.worldpainter.layers.plants;

import org.pepsoft.minecraft.Direction;
import org.pepsoft.minecraft.Material;
import org.pepsoft.worldpainter.Platform;
import org.pepsoft.worldpainter.exporting.MinecraftWorld;

import java.util.Random;

import static org.pepsoft.minecraft.Constants.*;
import static org.pepsoft.minecraft.Material.*;
import static org.pepsoft.worldpainter.layers.plants.Category.*;

/**
 * A collection of Minecraft plants. These are prototypes which cannot be
 * actually be rendered; you must always invoke
 * {@link Plant#realise(int, Platform)} to obtain a concrete instances of the
 * plant which can be rendered. The dimensions of the prototypes indicate the
 * maximum dimensions of the plant.
 */
public class Plants {
    public static void main(String[] args) {
        for (Plant plant: ALL_PLANTS) {
            System.out.println(plant);
        }
    }

    public static final Plant GRASS = new SimplePlant("Grass", Material.GRASS, PLANTS_AND_FLOWERS);
    public static final Plant FERN = new SimplePlant("Fern", Material.FERN, PLANTS_AND_FLOWERS);
    public static final Plant DEAD_SHRUB = new SimplePlant("Dead Shrub", Material.DEAD_SHRUBS, PLANTS_AND_FLOWERS) {
        @Override
        public Category isValidFoundation(MinecraftWorld world, int x, int y, int z, boolean checkBlockBelow) {
            final Material material = world.getMaterialAt(x, y, z);
            return ((! checkBlockBelow)
                    || material.modded
                    || material.isNamedOneOf(MC_GRASS_BLOCK, MC_SAND, MC_RED_SAND, MC_DIRT, MC_TERRACOTTA, MC_PODZOL, MC_COARSE_DIRT, MC_ROOTED_DIRT, MC_MOSS_BLOCK, MC_MUD)
                    || material.name.endsWith("_terracotta"))
                ? PLANTS_AND_FLOWERS
                : null;
        }
    };
    public static final Plant DANDELION = new SimplePlant("Dandelion", Material.DANDELION, PLANTS_AND_FLOWERS);
    public static final Plant POPPY = new SimplePlant("Poppy", Material.ROSE, PLANTS_AND_FLOWERS);
    public static final Plant BLUE_ORCHID = new SimplePlant("Blue Orchid", Material.BLUE_ORCHID, PLANTS_AND_FLOWERS);
    public static final Plant ALLIUM = new SimplePlant("Allium", Material.ALLIUM, PLANTS_AND_FLOWERS);
    public static final Plant AZURE_BLUET = new SimplePlant("Azure Bluet", Material.AZURE_BLUET, PLANTS_AND_FLOWERS);
    public static final Plant TULIP_RED = new SimplePlant("Red Tulip", Material.RED_TULIP, PLANTS_AND_FLOWERS);
    public static final Plant TULIP_ORANGE = new SimplePlant("Orange Tulip", Material.ORANGE_TULIP, PLANTS_AND_FLOWERS);
    public static final Plant TULIP_WHITE = new SimplePlant("White Tulip", Material.WHITE_TULIP, PLANTS_AND_FLOWERS);
    public static final Plant TULIP_PINK = new SimplePlant("Pink Tulip", Material.PINK_TULIP, PLANTS_AND_FLOWERS);
    public static final Plant OXEYE_DAISY = new SimplePlant("Oxeye Daisy", Material.OXEYE_DAISY, PLANTS_AND_FLOWERS);
    public static final Plant CORNFLOWER = new SimplePlant("Cornflower", Material.CORNFLOWER, PLANTS_AND_FLOWERS);
    public static final Plant LILY_OF_THE_VALLEY = new SimplePlant("Lily of the Valley", Material.LILY_OF_THE_VALLEY, PLANTS_AND_FLOWERS);
    public static final Plant WITHER_ROSE = new SimplePlant("Wither Rose", Material.WITHER_ROSE, NETHER);
    public static final Plant SUNFLOWER = new DoubleHighPlant("Sunflower", Material.SUNFLOWER_LOWER, "block/sunflower_front.png", PLANTS_AND_FLOWERS);
    public static final Plant LILAC = new DoubleHighPlant("Lilac", Material.LILAC_LOWER, PLANTS_AND_FLOWERS);
    public static final Plant TALL_GRASS = new DoubleHighPlant("Tall Grass", Material.TALL_GRASS_LOWER, PLANTS_AND_FLOWERS);
    public static final Plant LARGE_FERN = new DoubleHighPlant("Large Fern", Material.LARGE_FERN_LOWER, PLANTS_AND_FLOWERS);
    public static final Plant ROSE_BUSH = new DoubleHighPlant("Rose Bush", Material.ROSE_BUSH_LOWER, PLANTS_AND_FLOWERS);
    public static final Plant PEONY = new DoubleHighPlant("Peony", Material.PEONY_LOWER, PLANTS_AND_FLOWERS);
    public static final Plant SAPLING_OAK = new SimplePlant("Oak Sapling", Material.OAK_SAPLING, SAPLINGS);
    public static final Plant SAPLING_DARK_OAK = new SimplePlant("Dark Oak Sapling", Material.DARK_OAK_SAPLING, SAPLINGS);
    public static final Plant SAPLING_PINE = new SimplePlant("Pine Sapling", Material.PINE_SAPLING, SAPLINGS);
    public static final Plant SAPLING_BIRCH = new SimplePlant("Birch Sapling", Material.BIRCH_SAPLING, SAPLINGS);
    public static final Plant SAPLING_JUNGLE = new SimplePlant("Jungle Sapling", Material.JUNGLE_SAPLING, SAPLINGS);
    public static final Plant SAPLING_ACACIA = new SimplePlant("Acacia Sapling", Material.ACACIA_SAPLING, SAPLINGS);
    public static final Plant MUSHROOM_RED = new SimplePlant("Red Mushroom", Material.RED_MUSHROOM, MUSHROOMS);
    public static final Plant MUSHROOM_BROWN = new SimplePlant("Brown Mushroom", Material.BROWN_MUSHROOM, MUSHROOMS);
    public static final Plant WHEAT = new AgingPlant("Wheat", Material.WHEAT, "block/wheat_stage7.png", 8, CROPS);
    public static final Plant CARROTS = new AgingPlant("Carrots", Material.CARROTS, "block/carrots_stage3.png", 8, CROPS);
    public static final Plant POTATOES = new AgingPlant("Potatoes", Material.POTATOES, "block/potatoes_stage3.png", 8, CROPS);
    public static final Plant PUMPKIN_STEMS = new AgingPlant("Pumpkin Stem", Material.PUMPKIN_STEM, "block/pumpkin_side.png", 8, CROPS) {
        @Override
        public Plant realise(int growth, Platform platform) {
            return new SimplePlant("Pumpkin Stem", Material.PUMPKIN_STEM.withProperty(FACING, Direction.values()[RANDOM.nextInt(4)]), categories);
        }
    };
    public static final Plant MELON_STEMS = new AgingPlant("Melon Stem", Material.MELON_STEM, "block/melon_side.png", 8, CROPS) {
        @Override
        public Plant realise(int growth, Platform platform) {
            return new SimplePlant("Melon Stem", Material.MELON_STEM.withProperty(FACING, Direction.values()[RANDOM.nextInt(4)]), categories);
        }
    };
    public static final Plant BEETROOTS = new AgingPlant("Beetroots", Material.BEETROOTS, "block/beetroots_stage3.png", 4, CROPS);
    public static final Plant SWEET_BERRY_BUSH = new AgingPlant("Sweet Berry Bush", Material.SWEET_BERRY_BUSH, "block/sweet_berry_bush_stage3.png", 4, PLANTS_AND_FLOWERS);
    public static final Plant CACTUS = new VariableHeightPlant("Cactus", Material.CACTUS, "block/cactus_side.png", 3, Category.CACTUS);
    public static final Plant SUGAR_CANE = new VariableHeightPlant("Sugar Cane", Material.SUGAR_CANE, 3, Category.SUGAR_CANE);
    public static final Plant LILY_PAD = new SimplePlant("Lily Pad", Material.LILY_PAD, Category.FLOATING_PLANTS);
    public static final Plant NETHER_WART = new AgingPlant("Nether Wart", Material.NETHER_WART, "block/nether_wart_stage2.png", 4, Category.NETHER) {
        @Override
        public Plant realise(int growth, Platform platform) {
            return new SimplePlant("Nether Wart", Material.NETHER_WART.withProperty(AGE, growth - 1), categories) {
                @Override
                public Category isValidFoundation(MinecraftWorld world, int x, int y, int height, boolean checkBlockBelow) {
                    final Material material = world.getMaterialAt(x, y, height);
                    return ((! checkBlockBelow) || material.modded || material.isNamed(MC_SOUL_SAND)) ? NETHER : null;
                }
            };
        }
    };
    public static final Plant CHORUS_PLANT = new VariableHeightPlant("Chorus Plant", Material.CHORUS_PLANT, Material.CHORUS_FLOWER, "block/chorus_flower.png", 5, Category.END);
    public static final Plant TUBE_CORAL = new SimplePlant("Tube Coral", Material.TUBE_CORAL, WATER_PLANTS);
    public static final Plant BRAIN_CORAL = new SimplePlant("Brain Coral", Material.BRAIN_CORAL, WATER_PLANTS);
    public static final Plant BUBBLE_CORAL = new SimplePlant("Bubble Coral", Material.BUBBLE_CORAL, WATER_PLANTS);
    public static final Plant FIRE_CORAL = new SimplePlant("Fire Coral", Material.FIRE_CORAL, WATER_PLANTS);
    public static final Plant HORN_CORAL = new SimplePlant("Horn Coral", Material.HORN_CORAL, WATER_PLANTS);
    public static final Plant TUBE_CORAL_FAN = new SimplePlant("Tube Coral Fan", Material.TUBE_CORAL_FAN, WATER_PLANTS);
    public static final Plant BRAIN_CORAL_FAN = new SimplePlant("Brain Coral Fan", Material.BRAIN_CORAL_FAN, WATER_PLANTS);
    public static final Plant BUBBLE_CORAL_FAN = new SimplePlant("Bubble Coral Fan", Material.BUBBLE_CORAL_FAN, WATER_PLANTS);
    public static final Plant FIRE_CORAL_FAN = new SimplePlant("Fire Coral Fan", Material.FIRE_CORAL_FAN, WATER_PLANTS);
    public static final Plant HORN_CORAL_FAN = new SimplePlant("Horn Coral Fan", Material.HORN_CORAL_FAN, WATER_PLANTS);
    public static final Plant KELP = new VariableHeightPlant("Kelp", Material.KELP_PLANT, Material.KELP, 26, WATER_PLANTS) {
        @Override
        public VariableHeightPlant realise(int growth, Platform platform) {
            return new VariableHeightPlant("Kelp", Material.KELP_PLANT, Material.KELP.withProperty(AGE, RANDOM.nextInt(26)), growth, categories);
        }
    };
    public static final Plant SEAGRASS = new SimplePlant("Seagrass", Material.SEAGRASS, WATER_PLANTS);
    public static final Plant TALL_SEAGRASS = new DoubleHighPlant("Tall Seagrass", Material.TALL_SEAGRASS_LOWER, WATER_PLANTS);
    public static final Plant SEA_PICKLE = new AgingPlant("Sea Pickle", Material.SEA_PICKLE_1, "item/sea_pickle.png", 4, WATER_PLANTS) {
        @Override
        public AgingPlant realise(int growth, Platform platform) {
            return new AgingPlant(name, material.withProperty(PICKLES, growth), iconName, maxGrowth, categories);
        }
    };
    public static final Plant BAMBOO = new VariableHeightPlant("Bamboo", BAMBOO_NO_LEAVES, BAMBOO_LARGE_LEAVES, "item/bamboo.png", 16, PLANTS_AND_FLOWERS) {
        @Override
        public Plant realise(int growth, Platform platform) {
            return new VariableHeightPlant("Bamboo", BAMBOO_NO_LEAVES, BAMBOO_LARGE_LEAVES, "item/bamboo.png", growth, PLANTS_AND_FLOWERS) {
                @Override
                public Material getMaterial(int x, int y, int z) {
                    final int age = (growth > 4) ? 1 : 0;
                    switch (z) {
                        case 0:
                            return BAMBOO_NO_LEAVES.withProperty(AGE, age);
                        case 1:
                        case 2:
                            if (growth <= 4) {
                                return BAMBOO_SMALL_LEAVES.withProperty(AGE, age);
                            } else {
                                return BAMBOO_NO_LEAVES.withProperty(AGE, age);
                            }
                        default:
                            if (z >= growth - 2) {
                                return BAMBOO_LARGE_LEAVES.withProperty(AGE, age);
                            } else if (z == growth - 3) {
                                return BAMBOO_SMALL_LEAVES.withProperty(AGE, age);
                            } else {
                                return BAMBOO_NO_LEAVES.withProperty(AGE, age);
                            }
                    }
                }
            };
        }
    };
    public static final Plant SAPLING_AZALEA = new SimplePlant("Azalea", Material.AZALEA, "block/azalea_plant.png", SAPLINGS);
    public static final Plant SAPLING_FLOWERING_AZALEA = new SimplePlant("Flowering Azalea", Material.FLOWERING_AZALEA, "block/flowering_azalea_side.png", SAPLINGS);
    public static final Plant CRIMSON_FUNGUS = new SimplePlant("Crimson Fungus", Material.CRIMSON_FUNGUS, NETHER);
    public static final Plant WARPED_FUNGUS = new SimplePlant("Warped Fungus", Material.WARPED_FUNGUS, NETHER);
    public static final Plant CRIMSON_ROOTS = new SimplePlant("Crimson Roots", Material.CRIMSON_ROOTS, NETHER);
    public static final Plant WARPED_ROOTS = new SimplePlant("Warped Roots", Material.WARPED_ROOTS, NETHER);
    public static final Plant NETHER_SPROUTS = new SimplePlant("Nether Sprouts", Material.NETHER_SPROUTS, NETHER);
    public static final Plant TWISTING_VINES = new VariableHeightPlant("Twisting Vines", Material.TWISTING_VINES_PLANT, TWISTING_VINES_25, 10, MUSHROOMS); // TODO not really mushrooms, but for now those are presented as "Various"
    public static final Plant GLOW_LICHEN = new SimplePlant("Glow Lichen", Material.GLOW_LICHEN_DOWN, MUSHROOMS, WATER_PLANTS, HANGING_DRY_PLANTS, HANGING_WATER_PLANTS); // TODO not really mushrooms, but for now those are presented as "Various"
    public static final Plant MOSS_CARPET = new SimplePlant("Moss Carpet", Material.MOSS_CARPET, "block/moss_block.png", MUSHROOMS); // TODO not really mushrooms, but for now those are presented as "Various"
    public static final Plant BIG_DRIPLEAF = new VariableHeightPlant("Big Dripleaf", Material.BIG_DRIPLEAF_STEM_SOUTH, Material.BIG_DRIPLEAF_SOUTH, "block/big_dripleaf_top.png", 10, PLANTS_AND_FLOWERS, DRIPLEAF) {
        @Override
        public VariableHeightPlant realise(int growth, Platform platform) {
            final Direction facing = Direction.values()[RANDOM.nextInt(4)];
            return new VariableHeightPlant("Big Dripleaf", Material.BIG_DRIPLEAF_STEM_SOUTH.withProperty(MC_FACING, facing.toString()), Material.BIG_DRIPLEAF_SOUTH.withProperty(MC_FACING, facing.toString()), growth, categories);
        }
    };
    public static final Plant PUMPKIN = new SimplePlant("Pumpkin", Material.PUMPKIN, "block/pumpkin_side.png", PLANTS_AND_FLOWERS) {
        @Override
        public Category isValidFoundation(MinecraftWorld world, int x, int y, int height, boolean checkBlockBelow) {
            final Material material = world.getMaterialAt(x, y, height);
            return ((! checkBlockBelow) || material.modded || material.solid) ? PLANTS_AND_FLOWERS : null;
        }
    };
    public static final Plant MELON = new SimplePlant("Melon", Material.MELON, "block/melon_side.png", PLANTS_AND_FLOWERS) {
        @Override
        public Category isValidFoundation(MinecraftWorld world, int x, int y, int height, boolean checkBlockBelow) {
            final Material material = world.getMaterialAt(x, y, height);
            return ((! checkBlockBelow) || material.modded || material.solid) ? PLANTS_AND_FLOWERS : null;
        }
    };
    public static final Plant CARVED_PUMPKIN = new SimplePlant("Carved Pumpkin", Material.CARVED_PUMPKIN_SOUTH_FACE, MUSHROOMS) { // TODO not really mushrooms, but for now those are presented as "Various"
        @Override
        public Plant realise(int growth, Platform platform) {
            return new SimplePlant("Carved Pumpkin", Material.CARVED_PUMPKIN_SOUTH_FACE.withProperty(FACING, Direction.values()[RANDOM.nextInt(4)]), categories) {
                @Override
                public Category isValidFoundation(MinecraftWorld world, int x, int y, int height, boolean checkBlockBelow) {
                    final Material material = world.getMaterialAt(x, y, height);
                    return ((! checkBlockBelow) || material.modded || material.solid) ? MUSHROOMS : null;
                }
            };
        }
    };
    public static final Plant JACK_O_LANTERN = new SimplePlant("Jack-o'-lantern", Material.JACK_O_LANTERN_SOUTH_FACE, MUSHROOMS) { // TODO not really mushrooms, but for now those are presented as "Various"
        @Override
        public Plant realise(int growth, Platform platform) {
            return new SimplePlant("Jack-o'-lantern", Material.JACK_O_LANTERN_SOUTH_FACE.withProperty(FACING, Direction.values()[RANDOM.nextInt(4)]), categories) {
                @Override
                public Category isValidFoundation(MinecraftWorld world, int x, int y, int height, boolean checkBlockBelow) {
                    final Material material = world.getMaterialAt(x, y, height);
                    return ((! checkBlockBelow) || material.modded || material.solid) ? MUSHROOMS : null;
                }
            };
        }
    };
    public static final Plant VINE = new VariableHeightPlant("Vine", Material.VINE, 10, HANGING_DRY_PLANTS) {
        @Override
        public Plant realise(int growth, Platform platform) {
            final String directionProperty = Direction.values()[RANDOM.nextInt(4)].name().toLowerCase();
            return new VariableHeightPlant("Vine",
                    Material.VINE.withProperty(DOWN, true).withProperty(directionProperty, "true"),
                    Material.VINE.withProperty(directionProperty, "true"),
                    Material.VINE.withProperty(directionProperty, "true"),
                    growth,
                    categories);
        }
    };
    public static final Plant SPORE_BLOSSOM = new SimplePlant("Spore Blossoms", Material.SPORE_BLOSSOM, HANGING_DRY_PLANTS);
    public static final Plant WEEPING_VINES = new VariableHeightPlant("Weeping Vines", Material.WEEPING_VINES_PLANT, Material.WEEPING_VINES, "block/weeping_vines_plant.png", 10, HANGING_DRY_PLANTS);
    public static final Plant HANGING_ROOTS = new SimplePlant("Hanging Roots", Material.HANGING_ROOTS, HANGING_DRY_PLANTS, HANGING_WATER_PLANTS);
    public static final Plant GLOW_BERRIES = new VariableHeightPlant("Glow Berries", Material.CAVE_VINES_PLANT_NO_BERRIES, Material.CAVE_VINES_NO_BERRIES, "block/cave_vines_lit.png", 10, HANGING_DRY_PLANTS) {
        @Override
        public Plant realise(int growth, Platform platform) {
            return new VariableHeightPlant("Glow Berries", Material.CAVE_VINES_PLANT_NO_BERRIES, Material.CAVE_VINES_NO_BERRIES, "block/cave_vines_lit.png", growth, categories) {
                @Override
                public Material getMaterial(int x, int y, int z) {
                    // Randomly add berries to one in four blocks
                    return super.getMaterial(x, y, z).withProperty(BERRIES, RANDOM.nextInt(4) == 0);
                }
            };
        }
    };
    public static final Plant SMALL_DRIPLEAF = new DoubleHighPlant("Small Dripleaf", Material.SMALL_DRIPLEAF_SOUTH_LOWER, DRIPLEAF) {
        @Override
        public DoubleHighPlant realise(int growth, Platform platform) {
            return new DoubleHighPlant("Small Dripleaf", Material.SMALL_DRIPLEAF_SOUTH_LOWER.withProperty(FACING, Direction.values()[RANDOM.nextInt(4)]), DRIPLEAF, platform);
        }
    };
    public static final Plant MANGROVE_PROPAGULE = new SimplePlant("Mangrove Propagule", Material.MANGROVE_PROPAGULE, SAPLINGS, WATER_PLANTS);

    // The code which uses this assumes there will never be more than 128 plants. If that ever happens it needs to be
    // overhauled! IMPORTANT: indices into this array are stored in layer settings! New entries MUST be added at the
    // end, and the order MUST never be changed!
    public static final Plant[] ALL_PLANTS = { GRASS, TALL_GRASS, FERN, LARGE_FERN, DEAD_SHRUB, DANDELION, POPPY,
            BLUE_ORCHID, ALLIUM, AZURE_BLUET, TULIP_RED, TULIP_ORANGE, TULIP_WHITE, TULIP_PINK, OXEYE_DAISY, SUNFLOWER,
            LILAC, ROSE_BUSH, PEONY, SAPLING_OAK, SAPLING_DARK_OAK, SAPLING_PINE, SAPLING_BIRCH, SAPLING_JUNGLE,
            SAPLING_ACACIA, MUSHROOM_RED, MUSHROOM_BROWN, WHEAT, CARROTS, POTATOES, PUMPKIN_STEMS, MELON_STEMS, CACTUS,
            SUGAR_CANE, LILY_PAD, BEETROOTS, NETHER_WART, CHORUS_PLANT, TUBE_CORAL, BRAIN_CORAL, BUBBLE_CORAL,
            FIRE_CORAL, HORN_CORAL, TUBE_CORAL_FAN, BRAIN_CORAL_FAN, BUBBLE_CORAL_FAN, FIRE_CORAL_FAN, HORN_CORAL_FAN,
            KELP, SEAGRASS, TALL_SEAGRASS, SEA_PICKLE, CORNFLOWER, LILY_OF_THE_VALLEY, WITHER_ROSE, SWEET_BERRY_BUSH,
            BAMBOO, SAPLING_AZALEA, SAPLING_FLOWERING_AZALEA, CRIMSON_FUNGUS, WARPED_FUNGUS, CRIMSON_ROOTS,
            WARPED_ROOTS, NETHER_SPROUTS, TWISTING_VINES, GLOW_LICHEN, MOSS_CARPET, BIG_DRIPLEAF, PUMPKIN, MELON,
            CARVED_PUMPKIN, JACK_O_LANTERN, VINE, SPORE_BLOSSOM, WEEPING_VINES, HANGING_ROOTS, GLOW_BERRIES,
            SMALL_DRIPLEAF, MANGROVE_PROPAGULE };

    private static final Random RANDOM = new Random();
}
