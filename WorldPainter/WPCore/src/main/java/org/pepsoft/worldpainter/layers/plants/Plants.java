package org.pepsoft.worldpainter.layers.plants;

import org.pepsoft.minecraft.Direction;
import org.pepsoft.minecraft.Material;
import org.pepsoft.worldpainter.Platform;
import org.pepsoft.worldpainter.exporting.MinecraftWorld;

import java.util.Optional;
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
        public boolean isValidFoundation(MinecraftWorld world, int x, int y, int z) {
            final Material material = world.getMaterialAt(x, y, z);
            return material.isNamed(MC_SAND) || material.isNamed(MC_RED_SAND) || material.isNamed(MC_TERRACOTTA);
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
    public static final Plant SUNFLOWER = new DoubleHighPlant("Sunflower", Material.SUNFLOWER, PLANTS_AND_FLOWERS, "block/sunflower_front.png");
    public static final Plant LILAC = new DoubleHighPlant("Lilac", Material.LILAC, PLANTS_AND_FLOWERS);
    public static final Plant TALL_GRASS = new DoubleHighPlant("Tall Grass", Material.TALL_GRASS, PLANTS_AND_FLOWERS);
    public static final Plant LARGE_FERN = new DoubleHighPlant("Large Fern", Material.LARGE_FERN, PLANTS_AND_FLOWERS);
    public static final Plant ROSE_BUSH = new DoubleHighPlant("Rose Bush", Material.ROSE_BUSH, PLANTS_AND_FLOWERS);
    public static final Plant PEONY = new DoubleHighPlant("Peony", Material.PEONY, PLANTS_AND_FLOWERS);
    public static final Plant SAPLING_OAK = new SimplePlant("Oak Sapling", Material.OAK_SAPLING, SAPLINGS);
    public static final Plant SAPLING_DARK_OAK = new SimplePlant("Dark Oak Sapling", Material.DARK_OAK_SAPLING, SAPLINGS);
    public static final Plant SAPLING_PINE = new SimplePlant("Pine Sapling", Material.PINE_SAPLING, SAPLINGS);
    public static final Plant SAPLING_BIRCH = new SimplePlant("Birch Sapling", Material.BIRCH_SAPLING, SAPLINGS);
    public static final Plant SAPLING_JUNGLE = new SimplePlant("Jungle Sapling", Material.JUNGLE_SAPLING, SAPLINGS);
    public static final Plant SAPLING_ACACIA = new SimplePlant("Acacia Sapling", Material.ACACIA_SAPLING, SAPLINGS);
    public static final Plant MUSHROOM_RED = new SimplePlant("Red Mushroom", Material.RED_MUSHROOM, MUSHROOMS);
    public static final Plant MUSHROOM_BROWN = new SimplePlant("Brown Mushroom", Material.BROWN_MUSHROOM, MUSHROOMS);
    public static final Plant WHEAT = new AgingPlant("Wheat", Material.WHEAT, CROPS, "block/wheat_stage7.png", 8);
    public static final Plant CARROTS = new AgingPlant("Carrots", Material.CARROTS, CROPS, "block/carrots_stage3.png", 8);
    public static final Plant POTATOES = new AgingPlant("Potatoes", Material.POTATOES, CROPS, "block/potatoes_stage3.png", 8);
    public static final Plant PUMPKIN_STEMS = new AgingPlant("Pumpkin Stems", Material.PUMPKIN_STEM, CROPS, "block/pumpkin_side.png", 8) {
        @Override
        public Material getMaterial(int x, int y, int z) {
            return material.withProperty(FACING, Direction.values()[RANDOM.nextInt(4)]);
        }
    };
    public static final Plant MELON_STEMS = new AgingPlant("Melon Stems", Material.MELON_STEM, CROPS, "block/melon_side.png", 8) {
        @Override
        public Material getMaterial(int x, int y, int z) {
            return material.withProperty(FACING, Direction.values()[RANDOM.nextInt(4)]);
        }
    };
    public static final Plant BEETROOTS = new AgingPlant("Beetroots", Material.BEETROOTS, CROPS, "block/beetroots_stage3.png", 4);
    public static final Plant CACTUS = new VariableHeightPlant("Cactus", Material.CACTUS, Category.CACTUS, "block/cactus_side.png", 3);
    public static final Plant SUGAR_CANE = new VariableHeightPlant("Sugar Cane", Material.SUGAR_CANE, Category.SUGAR_CANE, 3);
    public static final Plant LILY_PAD = new SimplePlant("Lily Pad", Material.LILY_PAD, Category.FLOATING_PLANTS);
    public static final Plant NETHER_WART = new AgingPlant("Nether Wart", Material.NETHER_WART, Category.NETHER, "block/nether_wart_stage2.png", 4);
    public static final Plant CHORUS_PLANT = new VariableHeightPlant("Chorus Plant", Material.CHORUS_PLANT, Material.CHORUS_FLOWER, Category.END, "block/chorus_flower.png", 5);
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
    public static final Plant KELP = new VariableHeightPlant("Kelp", Material.KELP_PLANT, Material.KELP, WATER_PLANTS, 26) {
        @Override
        Optional<Material> getTopMaterial() {
            return Optional.of(Material.KELP.withProperty(AGE, RANDOM.nextInt(26)));
        }
    };
    public static final Plant SEAGRASS = new SimplePlant("Seagrass", Material.SEAGRASS, WATER_PLANTS);
    public static final Plant TALL_SEAGRASS = new DoubleHighPlant("Tall Seagrass", Material.TALL_SEAGRASS, WATER_PLANTS);
    public static final AgingPlant SEA_PICKLE = new AgingPlant("Sea Pickle", Material.SEA_PICKLE, WATER_PLANTS, "item/sea_pickle.png", 4) {
        @Override
        public AgingPlant realise(int growth, Platform platform) {
            return new AgingPlant(name, material.withProperty(PICKLES, growth), category, iconName, maxGrowth);
        }
    };

    // The code which uses this assumes there will never be more than 128
    // plants. If that ever happens it needs to be overhauled!
    // IMPORTANT: indices into this array are stored in layer settings! New
    // entries MUST be added at the end, and the order MUST never be changed!
    public static final Plant[] ALL_PLANTS = {GRASS, TALL_GRASS,
            FERN, LARGE_FERN, DEAD_SHRUB, DANDELION, POPPY, BLUE_ORCHID, ALLIUM,
            AZURE_BLUET, TULIP_RED, TULIP_ORANGE, TULIP_WHITE, TULIP_PINK,
            OXEYE_DAISY, SUNFLOWER, LILAC, ROSE_BUSH, PEONY, SAPLING_OAK,
            SAPLING_DARK_OAK, SAPLING_PINE, SAPLING_BIRCH, SAPLING_JUNGLE,
            SAPLING_ACACIA, MUSHROOM_RED, MUSHROOM_BROWN, WHEAT, CARROTS, POTATOES,
            PUMPKIN_STEMS, MELON_STEMS, CACTUS, SUGAR_CANE, LILY_PAD, BEETROOTS,
            NETHER_WART, CHORUS_PLANT, TUBE_CORAL, BRAIN_CORAL, BUBBLE_CORAL,
            FIRE_CORAL, HORN_CORAL, TUBE_CORAL_FAN, BRAIN_CORAL_FAN,
            BUBBLE_CORAL_FAN, FIRE_CORAL_FAN, HORN_CORAL_FAN, KELP, SEAGRASS,
            TALL_SEAGRASS, SEA_PICKLE};

    private static final Random RANDOM = new Random();
}
