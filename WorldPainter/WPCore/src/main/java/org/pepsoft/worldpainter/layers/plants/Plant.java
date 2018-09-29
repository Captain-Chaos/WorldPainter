/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.worldpainter.layers.plants;

import org.pepsoft.minecraft.Entity;
import org.pepsoft.minecraft.Material;
import org.pepsoft.minecraft.TileEntity;
import org.pepsoft.util.AttributeKey;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.Platform;
import org.pepsoft.worldpainter.exporting.MinecraftWorld;
import org.pepsoft.worldpainter.objects.WPObject;

import javax.vecmath.Point3i;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

import static org.pepsoft.minecraft.Constants.*;
import static org.pepsoft.minecraft.Material.AGE;
import static org.pepsoft.worldpainter.DefaultPlugin.JAVA_ANVIL_1_13;
import static org.pepsoft.worldpainter.layers.plants.Plant.Category.*;

/**
 *
 * @author pepijn
 */
public final class Plant implements WPObject {
    public Plant(String name, Material material, int height, int maxGrowth, Category category, String iconName) {
        this.name = name;
        if (category == CROPS) {
            // Adjust the material for the specified maximum growth factor:
            if (material.isNamed(MC_CARROTS) || material.isNamed(MC_POTATOES)) {
                if (maxGrowth == 3) {
                    this.material = material.withProperty(AGE, 7);
                } else {
                    this.material = material.withProperty(AGE, maxGrowth * 2);
                }
            } else {
                this.material =  material.withProperty(AGE, maxGrowth);
            }
        } else if (category == Category.NETHER) {
            this.material = material.withProperty(AGE, maxGrowth + ((maxGrowth > 0) ? 1 : 0));
        } else {
            this.material = material;
        }
        this.category = category;
        this.maxData = maxGrowth;
        this.iconName = iconName;
        dimensions = new Point3i(1, 1, height);
        growth = maxGrowth;
        platform = null;
    }
    
    private Plant(Plant plant, int growth, Platform platform) {
        name = plant.name;
        category = plant.category;
        maxData = plant.maxData;
        iconName = plant.iconName;
        this.growth = growth;
        this.platform = platform;
        switch (category) {
            case CACTUS:
            case SUGAR_CANE:
                material = plant.material;
                dimensions = new Point3i(1, 1, Math.min(growth + 1, plant.dimensions.z));
                break;
            case CROPS:
                if (plant.material.isNamed(MC_CARROTS) || plant.material.isNamed(MC_POTATOES)) {
                    if (growth == 3) {
                        material = plant.material.withProperty(AGE, 7);
                    } else {
                        material = plant.material.withProperty(AGE, growth * 2);
                    }
                } else {
                    material = plant.material.withProperty(AGE, growth);
                }
                dimensions = plant.dimensions;
                break;
            case NETHER:
                material = plant.material.withProperty(AGE, growth + ((growth > 0) ? 1 : 0));
                dimensions = plant.dimensions;
                break;
            default:
                material = plant.material;
                dimensions = plant.dimensions;
                break;
        }
    }
    
    public Category getCategory() {
        return category;
    }

    public int getMaxData() {
        return maxData;
    }
    
    public boolean isValidFoundation(MinecraftWorld world, int x, int y, int height) {
        final Material material = world.getMaterialAt(x, y, height);
        switch (category) {
            case CACTUS:
                return (material.isNamed(MC_SAND) || material.isNamed(MC_RED_SAND))
                    && (! isSolid(world, x - 1, y, height + 1))
                    && (! isSolid(world, x, y - 1, height + 1))
                    && (! isSolid(world, x + 1, y, height + 1))
                    && (! isSolid(world, x, y + 1, height + 1));
            case CROPS:
                return material.isNamed(MC_FARMLAND);
            case MUSHROOMS:
                // If it's dark enough mushrooms can be placed on pretty much
                // anything
                return (! material.veryInsubstantial)
                    && (! material.isNamed(MC_GLASS))
                    && (! material.isNamed(MC_ICE));
            case PLANTS_AND_FLOWERS:
                return material.isNamed(MC_GRASS_BLOCK)
                    || material.isNamed(MC_DIRT)
                    || material.isNamed(MC_COARSE_DIRT)
                    || material.isNamed(MC_PODZOL)
                    || material.isNamed(MC_FARMLAND)
                    || ((this.material == Material.DEAD_SHRUBS) && (material.isNamed(MC_SAND) || material.isNamed(MC_RED_SAND) || material.isNamed(MC_TERRACOTTA)));
            case SAPLINGS:
                return material.isNamed(MC_GRASS_BLOCK)
                    || material.isNamed(MC_DIRT)
                    || material.isNamed(MC_COARSE_DIRT)
                    || material.isNamed(MC_PODZOL)
                    || material.isNamed(MC_FARMLAND);
            case SUGAR_CANE:
                return (material.isNamed(MC_GRASS_BLOCK)
                        || material.isNamed(MC_DIRT)
                        || material.isNamed(MC_COARSE_DIRT)
                        || material.isNamed(MC_PODZOL)
                        || material.isNamed(MC_SAND)
                        || material.isNamed(MC_RED_SAND)
                        || material.isNamed(MC_FARMLAND))
                    && (isWater(world, x - 1, y, height)
                        || isWater(world, x, y - 1, height)
                        || isWater(world, x + 1, y, height)
                        || isWater(world, x, y + 1, height));
            case WATER_PLANTS:
                // Just check whether the location is flooded; a special case in
                // the exporter will check for the water surface
                return isWater(world, x, y, height + 1);
            case NETHER:
                return material.isNamed(MC_SOUL_SAND);
            case END:
                return material.isNamed(MC_END_STONE) || material.isNamed(MC_CHORUS_PLANT);
            default:
                throw new InternalError();
        }
    }
    
    public Plant getPlant(int growth, Platform platform) {
        return new Plant(this, growth, platform);
    }
    
    // WPObject
    
    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        throw new UnsupportedOperationException("Plant is unmodifiable");
    }

    @Override
    public Point3i getDimensions() {
        return dimensions;
    }

    @Override
    public Material getMaterial(int x, int y, int z) {
        if (z > 0) {
            switch (category) {
                case PLANTS_AND_FLOWERS:
                    if (platform == JAVA_ANVIL_1_13) {
                        return material.withProperty("half", "upper");
                    } else {
                        return UPPER_DOUBLE_HIGH_PLANT;
                    }
                case CACTUS:
                    return Material.CACTUS;
                case SUGAR_CANE:
                    return Material.SUGAR_CANE;
                default:
                    throw new UnsupportedOperationException("Don't know what material to provide for category " + category);
            }
        } else {
            return material;
        }
    }

    @Override
    public boolean getMask(int x, int y, int z) {
        return true;
    }

    @Override
    public List<Entity> getEntities() {
        return null;
    }

    @Override
    public List<TileEntity> getTileEntities() {
        return null;
    }

    @Override
    public void prepareForExport(Dimension dimension) {
        // Do nothing
    }

    @Override
    public Map<String, Serializable> getAttributes() {
        return null;
    }

    @Override
    public <T extends Serializable> T getAttribute(AttributeKey<T> key) {
        return key.defaultValue;
    }

    @Override
    public void setAttributes(Map<String, Serializable> attributes) {
        throw new UnsupportedOperationException("Plant is unmodifiable");
    }

    @Override
    public <T extends Serializable> void setAttribute(AttributeKey<T> key, T value) {
        throw new UnsupportedOperationException("Plant is unmodifiable");
    }

    @Override
    public Point3i getOffset() {
        return new Point3i(0, 0, 0);
    }

    @SuppressWarnings("CloneDoesntCallSuperClone") // Plants are unmodifiable
    @Override
    public WPObject clone() {
        return this;
    }

    @Override
    public String toString() {
        return name;
    }
    
    private boolean isSolid(MinecraftWorld world, int x, int y, int height) {
        Material material = world.getMaterialAt(x, y, height);
        return material.isNamed(MC_CACTUS) || (! material.veryInsubstantial);
    }
    
    private boolean isWater(MinecraftWorld world, int x, int y, int height) {
        return world.getMaterialAt(x, y, height).isNamed(MC_WATER);
    }
    
    public static void main(String[] args) {
        for (Plant plant: ALL_PLANTS) {
            System.out.println(plant);
        }
    }
    
    String getIconName() {
        return iconName;
    }

    private final String name, iconName;
    private final Point3i dimensions;
    private final Material material;
    private final Category category;
    private final int maxData, growth;
    private final Platform platform;
    
    @SuppressWarnings("deprecation") // Legacy support
    private static final Material UPPER_DOUBLE_HIGH_PLANT = Material.get(BLK_LARGE_FLOWERS, 8);

    public static final Plant GRASS        = new Plant("Grass",       Material.GRASS,       1, 0, PLANTS_AND_FLOWERS, "blocks/tallgrass.png");
    public static final Plant FERN         = new Plant("Fern",        Material.FERN,        1, 0, PLANTS_AND_FLOWERS, "blocks/fern.png");
    public static final Plant DEAD_SHRUB   = new Plant("Dead Shrub",  Material.DEAD_SHRUBS, 1, 0, PLANTS_AND_FLOWERS, "blocks/deadbush.png");
    public static final Plant DANDELION    = new Plant("Dandelion",   Material.DANDELION,   1, 0, PLANTS_AND_FLOWERS, "blocks/flower_dandelion.png");
    public static final Plant POPPY        = new Plant("Poppy",       Material.ROSE,        1, 0, PLANTS_AND_FLOWERS, "blocks/flower_rose.png");
    public static final Plant BLUE_ORCHID  = new Plant("Blue Orchid", Material.BLUE_ORCHID, 1, 0, PLANTS_AND_FLOWERS, "blocks/flower_blue_orchid.png");
    public static final Plant ALLIUM       = new Plant("Allium",      Material.ALLIUM,      1, 0, PLANTS_AND_FLOWERS, "blocks/flower_allium.png");
    public static final Plant AZURE_BLUET  = new Plant("Azure Bluet", Material.AZURE_BLUET, 1, 0, PLANTS_AND_FLOWERS, "blocks/flower_houstonia.png");
    public static final Plant TULIP_RED    = new Plant("Red Tulip",   Material.RED_TULIP,   1, 0, PLANTS_AND_FLOWERS, "blocks/flower_tulip_red.png");
    public static final Plant TULIP_ORANGE = new Plant("Orange Tulip",Material.ORANGE_TULIP,1, 0, PLANTS_AND_FLOWERS, "blocks/flower_tulip_orange.png");
    public static final Plant TULIP_WHITE  = new Plant("White Tulip", Material.WHITE_TULIP, 1, 0, PLANTS_AND_FLOWERS, "blocks/flower_tulip_white.png");
    public static final Plant TULIP_PINK   = new Plant("Pink Tulip",  Material.PINK_TULIP,  1, 0, PLANTS_AND_FLOWERS, "blocks/flower_tulip_pink.png");
    public static final Plant OXEYE_DAISY  = new Plant("Oxeye Daisy", Material.OXEYE_DAISY, 1, 0, PLANTS_AND_FLOWERS, "blocks/flower_oxeye_daisy.png");
    public static final Plant SUNFLOWER    = new Plant("Sunflower",   Material.SUNFLOWER,   2, 0, PLANTS_AND_FLOWERS, "blocks/double_plant_sunflower_front.png");
    public static final Plant LILAC        = new Plant("Lilac",       Material.LILAC,       2, 0, PLANTS_AND_FLOWERS, "blocks/double_plant_syringa_top.png");
    public static final Plant TALL_GRASS   = new Plant("Tall Grass",  Material.TALL_GRASS,  2, 0, PLANTS_AND_FLOWERS, "blocks/double_plant_grass_top.png");
    public static final Plant LARGE_FERN   = new Plant("Large Fern",  Material.LARGE_FERN,  2, 0, PLANTS_AND_FLOWERS, "blocks/double_plant_fern_top.png");
    public static final Plant ROSE_BUSH    = new Plant("Rose Bush",   Material.ROSE_BUSH,   2, 0, PLANTS_AND_FLOWERS, "blocks/double_plant_rose_top.png");
    public static final Plant PEONY        = new Plant("Peony",       Material.PEONY,       2, 0, PLANTS_AND_FLOWERS, "blocks/double_plant_paeonia_top.png");

    public static final Plant SAPLING_OAK      = new Plant("Oak Sapling",      Material.OAK_SAPLING,      1, 0, SAPLINGS, "blocks/sapling_oak.png");
    public static final Plant SAPLING_DARK_OAK = new Plant("Dark Oak Sapling", Material.DARK_OAK_SAPLING, 1, 0, SAPLINGS, "blocks/sapling_roofed_oak.png");
    public static final Plant SAPLING_PINE     = new Plant("Pine Sapling",     Material.PINE_SAPLING,     1, 0, SAPLINGS, "blocks/sapling_spruce.png");
    public static final Plant SAPLING_BIRCH    = new Plant("Birch Sapling",    Material.BIRCH_SAPLING,    1, 0, SAPLINGS, "blocks/sapling_birch.png");
    public static final Plant SAPLING_JUNGLE   = new Plant("Jungle Sapling",   Material.JUNGLE_SAPLING,   1, 0, SAPLINGS, "blocks/sapling_jungle.png");
    public static final Plant SAPLING_ACACIA   = new Plant("Acacia Sapling",   Material.ACACIA_SAPLING,   1, 0, SAPLINGS, "blocks/sapling_acacia.png");

    public static final Plant MUSHROOM_RED   = new Plant("Red Mushroom",   Material.RED_MUSHROOM,   1, 0, MUSHROOMS, "blocks/mushroom_red.png");
    public static final Plant MUSHROOM_BROWN = new Plant("Brown Mushroom", Material.BROWN_MUSHROOM, 1, 0, MUSHROOMS, "blocks/mushroom_brown.png");

    public static final Plant WHEAT         = new Plant("Wheat",         Material.WHEAT,        1, 7, CROPS, "items/wheat.png");
    public static final Plant CARROTS       = new Plant("Carrots",       Material.CARROTS,      1, 3, CROPS, "items/carrot.png");
    public static final Plant POTATOES      = new Plant("Potatoes",      Material.POTATOES,     1, 3, CROPS, "items/potato.png");
    public static final Plant PUMPKIN_STEMS = new Plant("Pumpkin Stems", Material.PUMPKIN_STEM, 1, 7, CROPS, "blocks/pumpkin_side.png");
    public static final Plant MELON_STEMS   = new Plant("Melon Stems",   Material.MELON_STEM,   1, 7, CROPS, "blocks/melon_side.png");
    public static final Plant BEETROOTS     = new Plant("Beetroots",     Material.BEETROOTS,    1, 3, CROPS, "items/beetroot.png");

    public static final Plant CACTUS = new Plant("Cactus", Material.CACTUS, 3, 2, Category.CACTUS, "blocks/cactus_side.png");

    public static final Plant SUGAR_CANE = new Plant("Sugar Cane", Material.SUGAR_CANE, 3, 2, Category.SUGAR_CANE, "items/reeds.png");

    public static final Plant LILY_PAD = new Plant("Lily Pad", Material.LILY_PAD, 1, 0, Category.WATER_PLANTS, "blocks/waterlily.png");

    public static final Plant NETHER_WART = new Plant("Nether Wart", Material.NETHER_WART, 1, 2, Category.NETHER, "items/nether_wart.png");

    public static final Plant CHORUS_PLANT = new Plant("Chorus Plant", Material.CHORUS_FLOWER, 1, 0, Category.END, "blocks/chorus_flower.png");

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
        NETHER_WART, CHORUS_PLANT};
    
    public enum Category {PLANTS_AND_FLOWERS, SAPLINGS, MUSHROOMS, CROPS, SUGAR_CANE, CACTUS, WATER_PLANTS, NETHER, END}
}