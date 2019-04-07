package org.pepsoft.worldpainter.layers.plants;

import org.pepsoft.minecraft.Material;
import org.pepsoft.worldpainter.exporting.MinecraftWorld;

import static org.pepsoft.minecraft.Constants.*;
import static org.pepsoft.worldpainter.layers.plants.Category.*;

public class Plants {
    public static void main(String[] args) {
        for (Plant plant: ALL_PLANTS) {
            System.out.println(plant);
        }
    }

    public static final Plant GRASS = new Plant("Grass", Material.GRASS, PLANTS_AND_FLOWERS, "blocks/tallgrass.png");
    public static final Plant FERN = new Plant("Fern", Material.FERN, PLANTS_AND_FLOWERS, "blocks/fern.png");
    public static final Plant DEAD_SHRUB = new Plant("Dead Shrub", Material.DEAD_SHRUBS, PLANTS_AND_FLOWERS, "blocks/deadbush.png") {
        @Override
        public boolean isValidFoundation(MinecraftWorld world, int x, int y, int z) {
            final Material material = world.getMaterialAt(x, y, z);
            return material.isNamed(MC_SAND) || material.isNamed(MC_RED_SAND) || material.isNamed(MC_TERRACOTTA);
        }
    };
    public static final Plant DANDELION = new Plant("Dandelion", Material.DANDELION, PLANTS_AND_FLOWERS, "blocks/flower_dandelion.png");
    public static final Plant POPPY = new Plant("Poppy", Material.ROSE, PLANTS_AND_FLOWERS, "blocks/flower_rose.png");
    public static final Plant BLUE_ORCHID = new Plant("Blue Orchid", Material.BLUE_ORCHID, PLANTS_AND_FLOWERS, "blocks/flower_blue_orchid.png");
    public static final Plant ALLIUM = new Plant("Allium", Material.ALLIUM, PLANTS_AND_FLOWERS, "blocks/flower_allium.png");
    public static final Plant AZURE_BLUET = new Plant("Azure Bluet", Material.AZURE_BLUET, PLANTS_AND_FLOWERS, "blocks/flower_houstonia.png");
    public static final Plant TULIP_RED = new Plant("Red Tulip", Material.RED_TULIP, PLANTS_AND_FLOWERS, "blocks/flower_tulip_red.png");
    public static final Plant TULIP_ORANGE = new Plant("Orange Tulip", Material.ORANGE_TULIP, PLANTS_AND_FLOWERS, "blocks/flower_tulip_orange.png");
    public static final Plant TULIP_WHITE = new Plant("White Tulip", Material.WHITE_TULIP, PLANTS_AND_FLOWERS, "blocks/flower_tulip_white.png");
    public static final Plant TULIP_PINK = new Plant("Pink Tulip", Material.PINK_TULIP, PLANTS_AND_FLOWERS, "blocks/flower_tulip_pink.png");
    public static final Plant OXEYE_DAISY = new Plant("Oxeye Daisy", Material.OXEYE_DAISY, PLANTS_AND_FLOWERS, "blocks/flower_oxeye_daisy.png");
    public static final Plant SUNFLOWER = new DoubleHighPlant("Sunflower", Material.SUNFLOWER, PLANTS_AND_FLOWERS, "blocks/double_plant_sunflower_front.png");
    public static final Plant LILAC = new DoubleHighPlant("Lilac", Material.LILAC, PLANTS_AND_FLOWERS, "blocks/double_plant_syringa_top.png");
    public static final Plant TALL_GRASS = new DoubleHighPlant("Tall Grass", Material.TALL_GRASS, PLANTS_AND_FLOWERS, "blocks/double_plant_grass_top.png");
    public static final Plant LARGE_FERN = new DoubleHighPlant("Large Fern", Material.LARGE_FERN, PLANTS_AND_FLOWERS, "blocks/double_plant_fern_top.png");
    public static final Plant ROSE_BUSH = new DoubleHighPlant("Rose Bush", Material.ROSE_BUSH, PLANTS_AND_FLOWERS, "blocks/double_plant_rose_top.png");
    public static final Plant PEONY = new DoubleHighPlant("Peony", Material.PEONY, PLANTS_AND_FLOWERS, "blocks/double_plant_paeonia_top.png");
    public static final Plant SAPLING_OAK = new Plant("Oak Sapling", Material.OAK_SAPLING, SAPLINGS, "blocks/sapling_oak.png");
    public static final Plant SAPLING_DARK_OAK = new Plant("Dark Oak Sapling", Material.DARK_OAK_SAPLING, SAPLINGS, "blocks/sapling_roofed_oak.png");
    public static final Plant SAPLING_PINE = new Plant("Pine Sapling", Material.PINE_SAPLING, SAPLINGS, "blocks/sapling_spruce.png");
    public static final Plant SAPLING_BIRCH = new Plant("Birch Sapling", Material.BIRCH_SAPLING, SAPLINGS, "blocks/sapling_birch.png");
    public static final Plant SAPLING_JUNGLE = new Plant("Jungle Sapling", Material.JUNGLE_SAPLING, SAPLINGS, "blocks/sapling_jungle.png");
    public static final Plant SAPLING_ACACIA = new Plant("Acacia Sapling", Material.ACACIA_SAPLING, SAPLINGS, "blocks/sapling_acacia.png");
    public static final Plant MUSHROOM_RED = new Plant("Red Mushroom", Material.RED_MUSHROOM, MUSHROOMS, "blocks/mushroom_red.png");
    public static final Plant MUSHROOM_BROWN = new Plant("Brown Mushroom", Material.BROWN_MUSHROOM, MUSHROOMS, "blocks/mushroom_brown.png");
    public static final Plant WHEAT = new Plant("Wheat", Material.WHEAT, 1, 7, CROPS, "items/wheat.png");
    public static final Plant CARROTS = new Plant("Carrots", Material.CARROTS, 1, 3, CROPS, "items/carrot.png");
    public static final Plant POTATOES = new Plant("Potatoes", Material.POTATOES, 1, 3, CROPS, "items/potato.png");
    public static final Plant PUMPKIN_STEMS = new Plant("Pumpkin Stems", Material.PUMPKIN_STEM, 1, 7, CROPS, "blocks/pumpkin_side.png");
    public static final Plant MELON_STEMS = new Plant("Melon Stems", Material.MELON_STEM, 1, 7, CROPS, "blocks/melon_side.png");
    public static final Plant BEETROOTS = new Plant("Beetroots", Material.BEETROOTS, 1, 3, CROPS, "items/beetroot.png");
    public static final Plant CACTUS = new Plant("Cactus", Material.CACTUS, 3, 2, Category.CACTUS, "blocks/cactus_side.png");
    public static final Plant SUGAR_CANE = new Plant("Sugar Cane", Material.SUGAR_CANE, 3, 2, Category.SUGAR_CANE, "items/reeds.png");
    public static final Plant LILY_PAD = new Plant("Lily Pad", Material.LILY_PAD, Category.FLOATING_PLANTS, "blocks/waterlily.png");
    public static final Plant NETHER_WART = new Plant("Nether Wart", Material.NETHER_WART, 1, 2, Category.NETHER, "items/nether_wart.png");
    public static final Plant CHORUS_PLANT = new Plant("Chorus Plant", Material.CHORUS_FLOWER, Category.END, "blocks/chorus_flower.png");
    public static final Plant TUBE_CORAL = new Plant("Tube Coral", Material.TUBE_CORAL, WATER_PLANTS, "blocks/tube_coral.png");
    public static final Plant BRAIN_CORAL = new Plant("Brain Coral", Material.BRAIN_CORAL, WATER_PLANTS, "blocks/brain_coral.png");
    public static final Plant BUBBLE_CORAL = new Plant("Bubble Coral", Material.BUBBLE_CORAL, WATER_PLANTS, "blocks/bubble_coral.png");
    public static final Plant FIRE_CORAL = new Plant("Fire Coral", Material.FIRE_CORAL, WATER_PLANTS, "blocks/fire_coral.png");
    public static final Plant HORN_CORAL = new Plant("Horn Coral", Material.HORN_CORAL, WATER_PLANTS, "blocks/horn_coral.png");
    public static final Plant TUBE_CORAL_FAN = new Plant("Tube Coral Fan", Material.TUBE_CORAL_FAN, WATER_PLANTS, "blocks/tube_coral_fan.png");
    public static final Plant BRAIN_CORAL_FAN = new Plant("Brain Coral Fan", Material.BRAIN_CORAL_FAN, WATER_PLANTS, "blocks/brain_coral_fan.png");
    public static final Plant BUBBLE_CORAL_FAN = new Plant("Bubble Coral Fan", Material.BUBBLE_CORAL_FAN, WATER_PLANTS, "blocks/bubble_coral_fan.png");
    public static final Plant FIRE_CORAL_FAN = new Plant("Fire Coral Fan", Material.FIRE_CORAL_FAN, WATER_PLANTS, "blocks/fire_coral_fan.png");
    public static final Plant HORN_CORAL_FAN = new Plant("Horn Coral Fan", Material.HORN_CORAL_FAN, WATER_PLANTS, "blocks/horn_coral_fan.png");
    public static final Plant KELP = new Plant("Kelp", Material.KELP, 26, 25, WATER_PLANTS, "blocks/horn_coral_fan.png");
    public static final Plant SEAGRASS = new Plant("Seagrass", Material.SEAGRASS, WATER_PLANTS, "blocks/horn_coral_fan.png");
    public static final Plant TALL_SEAGRASS = new DoubleHighPlant("Tall Seagrass", Material.TALL_SEAGRASS, WATER_PLANTS, "blocks/horn_coral_fan.png");
    public static final Plant SEA_PICKLE = new Plant("Sea Pickle", Material.SEA_PICKLE, 1, 3, WATER_PLANTS, "blocks/horn_coral_fan.png");

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
}
