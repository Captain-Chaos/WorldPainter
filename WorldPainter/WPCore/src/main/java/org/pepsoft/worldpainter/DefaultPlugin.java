/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter;

import org.pepsoft.worldpainter.layers.*;
import org.pepsoft.worldpainter.plugins.AbstractPlugin;
import org.pepsoft.worldpainter.plugins.ContextProvider;
import org.pepsoft.worldpainter.plugins.LayerProvider;
import org.pepsoft.worldpainter.util.MinecraftJarProvider;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

import static org.pepsoft.minecraft.Constants.DEFAULT_MAX_HEIGHT_ANVIL;
import static org.pepsoft.minecraft.Constants.DEFAULT_MAX_HEIGHT_MCREGION;
import static org.pepsoft.worldpainter.Constants.*;
import static org.pepsoft.worldpainter.GameType.*;
import static org.pepsoft.worldpainter.Generator.*;
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

    public static final Platform JAVA_MCREGION = new Platform(
            "org.pepsoft.mcregion",
            "Minecraft 1.1 (MCRegion)",
            32, DEFAULT_MAX_HEIGHT_MCREGION, 2048,
            Integer.MIN_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MAX_VALUE,
            Arrays.asList(SURVIVAL, CREATIVE),
            Arrays.asList(DEFAULT, FLAT),
            Arrays.asList(DIM_NORMAL, DIM_NETHER, DIM_END),
            EnumSet.of(PRECALCULATED_LIGHT, SET_SPAWN_POINT, BLOCK_BASED, SEED));

    public static final Platform JAVA_ANVIL = new Platform(
            "org.pepsoft.anvil",
            "Minecraft 1.2 - 1.12 (Anvil)",
            DEFAULT_MAX_HEIGHT_ANVIL, DEFAULT_MAX_HEIGHT_ANVIL, DEFAULT_MAX_HEIGHT_ANVIL,
            Integer.MIN_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MAX_VALUE,
            Arrays.asList(SURVIVAL, CREATIVE, ADVENTURE, HARDCORE),
            Arrays.asList(DEFAULT, FLAT, LARGE_BIOMES, CUSTOM),
            Arrays.asList(DIM_NORMAL, DIM_NETHER, DIM_END),
            EnumSet.of(BIOMES, PRECALCULATED_LIGHT, SET_SPAWN_POINT, BLOCK_BASED, SEED));

    public static final Platform JAVA_ANVIL_1_13 = new Platform(
            "org.pepsoft.anvil.1.13",
            "Minecraft 1.13.2 or later (Anvil)",
            DEFAULT_MAX_HEIGHT_ANVIL, DEFAULT_MAX_HEIGHT_ANVIL, DEFAULT_MAX_HEIGHT_ANVIL,
            Integer.MIN_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MAX_VALUE,
            Arrays.asList(SURVIVAL, CREATIVE, ADVENTURE, HARDCORE),
            Arrays.asList(DEFAULT, FLAT, LARGE_BIOMES, CUSTOM),
            Arrays.asList(DIM_NORMAL, DIM_NETHER, DIM_END),
            EnumSet.of(BIOMES, PRECALCULATED_LIGHT, SET_SPAWN_POINT, BLOCK_BASED, NAME_BASED, SEED));
}