/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.worldpainter.layers.plants;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.vecmath.Point3i;
import static org.pepsoft.minecraft.Constants.*;
import org.pepsoft.minecraft.Entity;
import org.pepsoft.minecraft.Material;
import org.pepsoft.minecraft.TileEntity;
import org.pepsoft.util.Version;
import org.pepsoft.worldpainter.biomeschemes.BiomeSchemeManager;
import org.pepsoft.worldpainter.exporting.MinecraftWorld;
import org.pepsoft.worldpainter.objects.WPObject;
import static org.pepsoft.worldpainter.layers.plants.Plant.Category.*;

/**
 *
 * @author pepijn
 */
public class Plant implements WPObject {
    public Plant(String name, Material material, int height, int maxData, Category category, String iconName) {
        this.name = name;
        this.material = material;
        this.category = category;
        this.maxData = maxData;
        dimensions = new Point3i(1, 1, height);
        icon = (iconName != null) ? findIcon(iconName) : null;
        growth = maxData;
    }
    
    private Plant(Plant plant, int growth) {
        name = plant.name;
        category = plant.category;
        maxData = plant.maxData;
        icon = plant.icon;
        this.growth = growth;
        switch (category) {
            case CACTUS:
            case SUGAR_CANE:
                material = plant.material;
                dimensions = new Point3i(1, 1, Math.min(growth + 1, plant.dimensions.z));
                break;
            case CROPS:
                if ((plant.material.getBlockType() == BLK_CARROTS) || (plant.material.getBlockType() == BLK_POTATOES)) {
                    if (growth == 3) {
                        material = Material.get(plant.material.getBlockType(), 7);
                    } else {
                        material = Material.get(plant.material.getBlockType(), growth * 2);
                    }
                } else {
                    material = Material.get(plant.material.getBlockType(), growth);
                }
                dimensions = plant.dimensions;
                break;
            default:
                throw new InternalError();
        }
    }
    
    public BufferedImage getIcon() {
        return icon;
    }
    
    public Category getCategory() {
        return category;
    }

    public int getMaxData() {
        return maxData;
    }
    
    public boolean isValidFoundation(MinecraftWorld world, int x, int y, int height) {
        final int blockType = world.getBlockTypeAt(x, y, height);
        switch (category) {
            case CACTUS:
                return (blockType == BLK_SAND)
                    && (! isSolid(world, x - 1, y, height + 1))
                    && (! isSolid(world, x, y - 1, height + 1))
                    && (! isSolid(world, x + 1, y, height + 1))
                    && (! isSolid(world, x, y + 1, height + 1));
            case CROPS:
                return blockType == BLK_TILLED_DIRT;
            case MUSHROOMS:
                // If it's dark enough mushrooms can be placed on pretty much
                // anything
                return (! VERY_INSUBSTANTIAL_BLOCKS.get(blockType))
                    && (blockType != BLK_GLASS)
                    && (blockType != BLK_ICE);
            case PLANTS_AND_FLOWERS:
            case SAPLINGS:
                return (blockType == BLK_GRASS)
                    || (blockType == BLK_DIRT)
                    || (blockType == BLK_TILLED_DIRT);
            case SUGAR_CANE:
                return ((blockType == BLK_GRASS)
                        || (blockType == BLK_DIRT)
                        || (blockType == BLK_SAND)
                        || (blockType == BLK_TILLED_DIRT))
                    && (isWater(world, x - 1, y, height)
                        || isWater(world, x, y - 1, height)
                        || isWater(world, x + 1, y, height)
                        || isWater(world, x, y + 1, height));
            case WATER_PLANTS:
                // Just check whether the location is flooded; a special case in
                // the exporter will check for the water surface
                final int blockAbove = world.getBlockTypeAt(x, y, height + 1);
                return (blockAbove == BLK_WATER) || (blockAbove == BLK_STATIONARY_WATER);
            default:
                throw new InternalError();
        }
    }
    
    public Plant withGrowth(int growth) {
        if ((maxData == 0) || (growth == this.growth)) {
            return this;
        } else {
            return new Plant(this, growth);
        }
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
                    return UPPER_DOUBLE_HIGH_PLANT;
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
    public Map<String, Serializable> getAttributes() {
        return null;
    }

    @Override
    public <T extends Serializable> T getAttribute(String key, T _default) {
        return _default;
    }

    @Override
    public void setAttributes(Map<String, Serializable> attributes) {
        throw new UnsupportedOperationException("Plant is unmodifiable");
    }

    @Override
    public void setAttribute(String key, Serializable value) {
        throw new UnsupportedOperationException("Plant is unmodifiable");
    }

    @Override
    public WPObject clone() {
        return this; // Plants are unmodifiable
    }

    @Override
    public String toString() {
        return name;
    }
    
    private boolean isSolid(MinecraftWorld world, int x, int y, int height) {
        int blockType = world.getBlockTypeAt(x, y, height);
        return (blockType == BLK_CACTUS) || (! VERY_INSUBSTANTIAL_BLOCKS.get(blockType));
    }
    
    private boolean isWater(MinecraftWorld world, int x, int y, int height) {
        final int blockType = world.getBlockTypeAt(x, y, height);
        return (blockType == BLK_WATER) || (blockType == BLK_STATIONARY_WATER);
    }
    
    public static void main(String[] args) {
        for (Plant plant: ALL_PLANTS) {
            System.out.println(plant);
        }
    }
    
    private static BufferedImage findIcon(String name) {
        if (resourcesJar == RESOURCES_NOT_AVAILABLE) {
            return null;
        }
        try {
            if (resourcesJar == null) {
                SortedMap<Version, File> jars = BiomeSchemeManager.getAllMinecraftJars();
                if (! jars.isEmpty()) {
                    resourcesJar = jars.get(jars.lastKey());
                } else {
                    logger.warning("Could not find Minecraft jar for loading plant icons");
                    resourcesJar = RESOURCES_NOT_AVAILABLE;
                    return null;
                }
            }
            JarFile jarFile = new JarFile(resourcesJar);
            try {
                JarEntry entry = jarFile.getJarEntry("assets/minecraft/textures/" + name);
                if (entry != null) {
                    if (logger.isLoggable(Level.FINE)) {
                        logger.fine("Loading plant icon " + name + " from " + resourcesJar);
                    }
                    InputStream in = jarFile.getInputStream(entry);
                    try {
                        return ImageIO.read(in);
                    } finally {
                        in.close();
                    }
                } else {
                    if (logger.isLoggable(Level.FINE)) {
                        logger.fine("Could not find plant icon " + name + " in Minecraft jar " + resourcesJar);
                    }
                    return null;
                }
            } finally {
                jarFile.close();
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "I/O error while trying to load plant icon " + name + "; continuing without icon", e);
            resourcesJar = RESOURCES_NOT_AVAILABLE;
            return null;
        }
    }

    private final String name;
    private final Point3i dimensions;
    private final Material material;
    private final BufferedImage icon;
    private final Category category;
    private final int maxData, growth;
    
    private static File resourcesJar;
    
    private static final Material UPPER_DOUBLE_HIGH_PLANT = Material.get(BLK_LARGE_FLOWERS, 8);
    private static final Logger logger = Logger.getLogger(Plant.class.getName());
    private static final File RESOURCES_NOT_AVAILABLE = new File("~~~RESOURCES_NOT_AVAILABLE~~~");
    
    public static final Plant TALL_GRASS        = new Plant("Tall Grass",        Material.get(BLK_TALL_GRASS, 1),    1, 0, PLANTS_AND_FLOWERS, "blocks/tallgrass.png");
    public static final Plant FERN              = new Plant("Fern",              Material.get(BLK_TALL_GRASS, 2),    1, 0, PLANTS_AND_FLOWERS, "blocks/fern.png");
    public static final Plant DEAD_SHRUB        = new Plant("Dead Shrub",        Material.DEAD_SHRUBS,               1, 0, PLANTS_AND_FLOWERS, "blocks/deadbush.png");
    public static final Plant DANDELION         = new Plant("Dandelion",         Material.DANDELION,                 1, 0, PLANTS_AND_FLOWERS, "blocks/flower_dandelion.png");
    public static final Plant POPPY             = new Plant("Poppy",             Material.ROSE,                      1, 0, PLANTS_AND_FLOWERS, "blocks/flower_rose.png");
    public static final Plant BLUE_ORCHID       = new Plant("Blue Orchid",       Material.get(BLK_ROSE, 1),          1, 0, PLANTS_AND_FLOWERS, "blocks/flower_blue_orchid.png");
    public static final Plant ALLIUM            = new Plant("Allium",            Material.get(BLK_ROSE, 2),          1, 0, PLANTS_AND_FLOWERS, "blocks/flower_allium.png");
    public static final Plant AZURE_BLUET       = new Plant("Azure Bluet",       Material.get(BLK_ROSE, 3),          1, 0, PLANTS_AND_FLOWERS, "blocks/flower_houstonia.png");
    public static final Plant TULIP_RED         = new Plant("Red Tulip",         Material.get(BLK_ROSE, 4),          1, 0, PLANTS_AND_FLOWERS, "blocks/flower_tulip_red.png");
    public static final Plant TULIP_ORANGE      = new Plant("Orange Tulip",      Material.get(BLK_ROSE, 5),          1, 0, PLANTS_AND_FLOWERS, "blocks/flower_tulip_orange.png");
    public static final Plant TULIP_WHITE       = new Plant("White Tulip",       Material.get(BLK_ROSE, 6),          1, 0, PLANTS_AND_FLOWERS, "blocks/flower_tulip_white.png");
    public static final Plant TULIP_PINK        = new Plant("Pink Tulip",        Material.get(BLK_ROSE, 7),          1, 0, PLANTS_AND_FLOWERS, "blocks/flower_tulip_pink.png");
    public static final Plant OXEYE_DAISY       = new Plant("Oxeye Daisy",       Material.get(BLK_ROSE, 8),          1, 0, PLANTS_AND_FLOWERS, "blocks/flower_oxeye_daisy.png");
    public static final Plant SUNFLOWER         = new Plant("Sunflower",         Material.get(BLK_LARGE_FLOWERS),    2, 0, PLANTS_AND_FLOWERS, "blocks/double_plant_sunflower_front.png");
    public static final Plant LILAC             = new Plant("Lilac",             Material.get(BLK_LARGE_FLOWERS, 1), 2, 0, PLANTS_AND_FLOWERS, "blocks/double_plant_syringa_top.png");
    public static final Plant DOUBLE_TALL_GRASS = new Plant("Double Tall Grass", Material.get(BLK_LARGE_FLOWERS, 2), 2, 0, PLANTS_AND_FLOWERS, "blocks/double_plant_grass_top.png");
    public static final Plant LARGE_FERN        = new Plant("Large Fern",        Material.get(BLK_LARGE_FLOWERS, 3), 2, 0, PLANTS_AND_FLOWERS, "blocks/double_plant_fern_top.png");
    public static final Plant ROSE_BUSH         = new Plant("Rose Bush",         Material.get(BLK_LARGE_FLOWERS, 4), 2, 0, PLANTS_AND_FLOWERS, "blocks/double_plant_rose_top.png");
    public static final Plant PEONY             = new Plant("Peony",             Material.get(BLK_LARGE_FLOWERS, 5), 2, 0, PLANTS_AND_FLOWERS, "blocks/double_plant_paeonia_top.png");

    public static final Plant SAPLING_OAK      = new Plant("Oak Sapling",      Material.get(BLK_SAPLING, DATA_OAK),      1, 0, SAPLINGS, "blocks/sapling_oak.png");
    public static final Plant SAPLING_DARK_OAK = new Plant("Dark Oak Sapling", Material.get(BLK_SAPLING, DATA_DARK_OAK), 1, 0, SAPLINGS, "blocks/sapling_roofed_oak.png");
    public static final Plant SAPLING_PINE     = new Plant("Pine Sapling",     Material.get(BLK_SAPLING, DATA_PINE),     1, 0, SAPLINGS, "blocks/sapling_spruce.png");
    public static final Plant SAPLING_BIRCH    = new Plant("Birch Sapling",    Material.get(BLK_SAPLING, DATA_BIRCH),    1, 0, SAPLINGS, "blocks/sapling_birch.png");
    public static final Plant SAPLING_JUNGLE   = new Plant("Jungle Sapling",   Material.get(BLK_SAPLING, DATA_JUNGLE),   1, 0, SAPLINGS, "blocks/sapling_jungle.png");
    public static final Plant SAPLING_ACACIA   = new Plant("Acacia Sapling",   Material.get(BLK_SAPLING, DATA_ACACIA),   1, 0, SAPLINGS, "blocks/sapling_acacia.png");

    public static final Plant MUSHROOM_RED   = new Plant("Red Mushroom",   Material.RED_MUSHROOM,   1, 0, MUSHROOMS, "blocks/mushroom_red.png");
    public static final Plant MUSHROOM_BROWN = new Plant("Brown Mushroom", Material.BROWN_MUSHROOM, 1, 0, MUSHROOMS, "blocks/mushroom_brown.png");

    public static final Plant WHEAT         = new Plant("Wheat",         Material.WHEAT,                 1, 7, CROPS, "items/wheat.png");
    public static final Plant CARROTS       = new Plant("Carrots",       Material.get(BLK_CARROTS),      1, 3, CROPS, "items/carrot.png");
    public static final Plant POTATOES      = new Plant("Potatoes",      Material.get(BLK_POTATOES),     1, 3, CROPS, "items/potato.png");
    public static final Plant PUMPKIN_STEMS = new Plant("Pumpkin Stems", Material.get(BLK_PUMPKIN_STEM), 1, 7, CROPS, "blocks/pumpkin_side.png");
    public static final Plant MELON_STEMS   = new Plant("Melon Stems",   Material.get(BLK_MELON_STEM),   1, 7, CROPS, "blocks/melon_side.png");
    
    public static final Plant CACTUS = new Plant("Cactus", Material.CACTUS, 3, 2, Category.CACTUS, "blocks/cactus_side.png");

    public static final Plant SUGAR_CANE = new Plant("Sugar Cane", Material.SUGAR_CANE, 3, 2, Category.SUGAR_CANE, "items/reeds.png");

    public static final Plant LILY_PAD = new Plant("Lily Pad", Material.LILY_PAD, 1, 0, Category.WATER_PLANTS, "blocks/waterlily.png");
    
    // TODO: nether wart?
    
    // The code which uses this assumes there will never be more than 128
    // plants. If that ever happens it needs to be overhauled!
    // IMPORTANT: indices into this array are stored in layer settings! New
    // entries MUST be added at the end, and the order MUST never be changed!
    public static final Plant[] ALL_PLANTS = {TALL_GRASS, DOUBLE_TALL_GRASS,
        FERN, LARGE_FERN, DEAD_SHRUB, DANDELION, POPPY, BLUE_ORCHID, ALLIUM,
        AZURE_BLUET, TULIP_RED, TULIP_ORANGE, TULIP_WHITE, TULIP_PINK,
        OXEYE_DAISY, SUNFLOWER, LILAC, ROSE_BUSH, PEONY, SAPLING_OAK,
        SAPLING_DARK_OAK, SAPLING_PINE, SAPLING_BIRCH, SAPLING_JUNGLE,
        SAPLING_ACACIA, MUSHROOM_RED, MUSHROOM_BROWN, WHEAT, CARROTS, POTATOES,
        PUMPKIN_STEMS, MELON_STEMS, CACTUS, SUGAR_CANE, LILY_PAD};
    
    public enum Category {PLANTS_AND_FLOWERS, SAPLINGS, MUSHROOMS, CROPS, SUGAR_CANE, CACTUS, WATER_PLANTS}
}