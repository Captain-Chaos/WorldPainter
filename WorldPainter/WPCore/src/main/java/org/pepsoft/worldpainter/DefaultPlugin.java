/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter;

import com.google.common.collect.ImmutableList;
import org.pepsoft.util.AttributeKey;
import org.pepsoft.worldpainter.layers.*;
import org.pepsoft.worldpainter.plugins.AbstractPlugin;
import org.pepsoft.worldpainter.plugins.ContextProvider;
import org.pepsoft.worldpainter.plugins.LayerProvider;
import org.pepsoft.worldpainter.util.MinecraftJarProvider;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

import static org.pepsoft.minecraft.Constants.*;
import static org.pepsoft.worldpainter.Constants.*;
import static org.pepsoft.worldpainter.GameType.*;
import static org.pepsoft.worldpainter.Generator.*;
import static org.pepsoft.worldpainter.Platform.ATTRIBUTE_GRASS_BLOCK_NAME;
import static org.pepsoft.worldpainter.Platform.ATTRIBUTE_WATER_OPACITY;
import static org.pepsoft.worldpainter.Platform.Capability.*;

/**
 *
 * @author pepijn
 */
public class DefaultPlugin extends AbstractPlugin implements LayerProvider, ContextProvider, WPContext {
    public DefaultPlugin() {
        super("Default", Version.VERSION);
    }

    // LayerProvider
    
    @Override
    public List<Layer> getLayers() {
        return Arrays.asList(Frost.INSTANCE, Caves.INSTANCE, Caverns.INSTANCE, Chasms.INSTANCE, DeciduousForest.INSTANCE, PineForest.INSTANCE, SwampLand.INSTANCE, Jungle.INSTANCE, org.pepsoft.worldpainter.layers.Void.INSTANCE, Resources.INSTANCE/*, River.INSTANCE*/);
    }

    // ContextProvider

    @Override
    public WPContext getWPContextInstance() {
        return this;
    }
    
    // WPContext
    
    @Override
    public EventLogger getStatisticsRecorder() {
        return Configuration.getInstance();
    }

    @Override
    public MinecraftJarProvider getMinecraftJarProvider() {
        return Configuration.getInstance();
    }

    /**
     * The data version to use when exporting for the platform.
     */
    public static final AttributeKey<Integer> ATTRIBUTE_EXPORT_DATA_VERSION = new AttributeKey<>("org.pepsoft.exportDataVersion");

    /**
     * The highest data version supported by the platform.
     */
    public static final AttributeKey<Integer> ATTRIBUTE_LATEST_DATA_VERSION = new AttributeKey<>("org.pepsoft.latestDataVersion");

    /**
     * The version number of the oldest Minecraft Java Edition version the platform supports, or the equivalent
     * Minecraft version for platforms which do not support Minecraft Java Edition. Used among other things to determine
     * which blocks, biomes, etc. should be allowed. The default value is {@code v1.12.2}.
     */
    public static final AttributeKey<org.pepsoft.util.Version> ATTRIBUTE_MC_VERSION = new AttributeKey<>("org.pepsoft.minecraftVersion", V_1_12_2);

    public static final Platform JAVA_MCREGION = new Platform(
            "org.pepsoft.mcregion",
            "Minecraft 1.1 (MCRegion)",
            32, DEFAULT_MAX_HEIGHT_MCREGION, 2048,
            Integer.MIN_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MAX_VALUE,
            Arrays.asList(SURVIVAL, CREATIVE),
            Arrays.asList(DEFAULT, FLAT, NETHER, END),
            Arrays.asList(DIM_NORMAL, DIM_NETHER, DIM_END),
            EnumSet.of(PRECALCULATED_LIGHT, SET_SPAWN_POINT, BLOCK_BASED, SEED, POPULATE),
            ATTRIBUTE_MC_VERSION, new org.pepsoft.util.Version(1, 1),
            ATTRIBUTE_GRASS_BLOCK_NAME, MC_GRASS,
            ATTRIBUTE_WATER_OPACITY, 3);

    public static final Platform JAVA_ANVIL = new Platform(
            "org.pepsoft.anvil",
            "Minecraft 1.2 - 1.12",
            DEFAULT_MAX_HEIGHT_ANVIL, DEFAULT_MAX_HEIGHT_ANVIL, DEFAULT_MAX_HEIGHT_ANVIL,
            Integer.MIN_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MAX_VALUE,
            Arrays.asList(SURVIVAL, CREATIVE, ADVENTURE, HARDCORE),
            Arrays.asList(DEFAULT, FLAT, LARGE_BIOMES, AMPLIFIED, CUSTOM, NETHER, END),
            Arrays.asList(DIM_NORMAL, DIM_NETHER, DIM_END),
            EnumSet.of(BIOMES, PRECALCULATED_LIGHT, SET_SPAWN_POINT, BLOCK_BASED, SEED, POPULATE),
            ATTRIBUTE_MC_VERSION, new org.pepsoft.util.Version(1, 2),
            ATTRIBUTE_EXPORT_DATA_VERSION, DATA_VERSION_MC_1_12_2,
            ATTRIBUTE_LATEST_DATA_VERSION, DATA_VERSION_MC_1_12_2,
            ATTRIBUTE_GRASS_BLOCK_NAME, MC_GRASS,
            ATTRIBUTE_WATER_OPACITY, 3);

    public static final Platform JAVA_ANVIL_1_15 = new Platform(
            "org.pepsoft.anvil.1.13",
            "Minecraft 1.15 - 1.16",
            DEFAULT_MAX_HEIGHT_ANVIL, DEFAULT_MAX_HEIGHT_ANVIL, DEFAULT_MAX_HEIGHT_ANVIL,
            Integer.MIN_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MAX_VALUE,
            Arrays.asList(SURVIVAL, CREATIVE, ADVENTURE, HARDCORE),
            Arrays.asList(DEFAULT, FLAT, LARGE_BIOMES, AMPLIFIED, CUSTOM, NETHER, END),
            Arrays.asList(DIM_NORMAL, DIM_NETHER, DIM_END),
            EnumSet.of(BIOMES, PRECALCULATED_LIGHT, SET_SPAWN_POINT, BLOCK_BASED, NAME_BASED, SEED, LEAF_DISTANCES),
            ATTRIBUTE_MC_VERSION, new org.pepsoft.util.Version(1, 15),
            ATTRIBUTE_EXPORT_DATA_VERSION, DATA_VERSION_MC_1_15_2,
            ATTRIBUTE_LATEST_DATA_VERSION, DATA_VERSION_MC_1_16_5);

    public static final Platform JAVA_ANVIL_1_17 = new Platform(
            "org.pepsoft.anvil.1.17",
            "Minecraft 1.17",
            new int[] { 256, 320, 512, 1024, 1536, 2032 }, 256,
            Integer.MIN_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MAX_VALUE, 0,
            Arrays.asList(SURVIVAL, CREATIVE, ADVENTURE, HARDCORE),
            Arrays.asList(DEFAULT, FLAT, LARGE_BIOMES, AMPLIFIED, CUSTOM, NETHER, END),
            Arrays.asList(DIM_NORMAL, DIM_NETHER, DIM_END),
            EnumSet.of(PRECALCULATED_LIGHT, SET_SPAWN_POINT, BLOCK_BASED, NAME_BASED, SEED, BIOMES_3D, GENERATOR_PER_DIMENSION, LEAF_DISTANCES),
            ATTRIBUTE_MC_VERSION, new org.pepsoft.util.Version(1, 17),
            ATTRIBUTE_EXPORT_DATA_VERSION, DATA_VERSION_MC_1_17,
            ATTRIBUTE_LATEST_DATA_VERSION, DATA_VERSION_MC_1_17_1);

    public static final Platform JAVA_ANVIL_1_18 = new Platform(
            "org.pepsoft.anvil.1.18",
            "Minecraft 1.18",
            new int[] { 256, 320, 512, 1024, 1536, 2032 }, 320,
            Integer.MIN_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MAX_VALUE, -64,
            Arrays.asList(SURVIVAL, CREATIVE, ADVENTURE, HARDCORE),
            Arrays.asList(DEFAULT, FLAT, LARGE_BIOMES, AMPLIFIED, CUSTOM, NETHER, END),
            Arrays.asList(DIM_NORMAL, DIM_NETHER, DIM_END),
            EnumSet.of(PRECALCULATED_LIGHT, SET_SPAWN_POINT, BLOCK_BASED, NAME_BASED, SEED, NAMED_BIOMES, POPULATE, GENERATOR_PER_DIMENSION, LEAF_DISTANCES),
            ATTRIBUTE_MC_VERSION, new org.pepsoft.util.Version(1, 18),
            ATTRIBUTE_EXPORT_DATA_VERSION, DATA_VERSION_MC_1_18_0,
            ATTRIBUTE_LATEST_DATA_VERSION, DATA_VERSION_MC_1_18_2);

    public static final Platform JAVA_ANVIL_1_19 = new Platform(
            "org.pepsoft.anvil.1.19",
            "Minecraft 1.19 or later",
            new int[] { 256, 320, 512, 1024, 1536, 2032 }, 320,
            Integer.MIN_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MAX_VALUE, -64,
            Arrays.asList(SURVIVAL, CREATIVE, ADVENTURE, HARDCORE),
            Arrays.asList(DEFAULT, FLAT, LARGE_BIOMES, AMPLIFIED, CUSTOM, NETHER, END),
            Arrays.asList(DIM_NORMAL, DIM_NETHER, DIM_END),
            EnumSet.of(PRECALCULATED_LIGHT, SET_SPAWN_POINT, BLOCK_BASED, NAME_BASED, SEED, NAMED_BIOMES, POPULATE, GENERATOR_PER_DIMENSION, LEAF_DISTANCES),
            ATTRIBUTE_MC_VERSION, new org.pepsoft.util.Version(1, 19),
            ATTRIBUTE_EXPORT_DATA_VERSION, DATA_VERSION_MC_1_19_0);

    /**
     * The default set of Minecraft Java Edition-based platforms supported by WorldPainter, ordered by release date from old to new.
     */
    public static List<Platform> DEFAULT_JAVA_PLATFORMS = ImmutableList.of(JAVA_MCREGION, JAVA_ANVIL, JAVA_ANVIL_1_15, JAVA_ANVIL_1_17, JAVA_ANVIL_1_18, JAVA_ANVIL_1_19);
}