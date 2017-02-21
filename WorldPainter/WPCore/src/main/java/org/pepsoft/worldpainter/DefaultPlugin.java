/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter;

import org.pepsoft.minecraft.Chunk;
import org.pepsoft.minecraft.ChunkImpl;
import org.pepsoft.minecraft.ChunkImpl2;
import org.pepsoft.minecraft.ChunkStore;
import org.pepsoft.worldpainter.exporting.JavaChunkStore;
import org.pepsoft.worldpainter.exporting.JavaWorldExporter;
import org.pepsoft.worldpainter.exporting.WorldExporter;
import org.pepsoft.worldpainter.layers.*;
import org.pepsoft.worldpainter.plugins.AbstractPlugin;
import org.pepsoft.worldpainter.plugins.ContextProvider;
import org.pepsoft.worldpainter.plugins.LayerProvider;
import org.pepsoft.worldpainter.plugins.PlatformProvider;
import org.pepsoft.worldpainter.util.MinecraftJarProvider;
import org.pepsoft.worldpainter.util.MinecraftUtil;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

import static org.pepsoft.minecraft.Constants.DEFAULT_MAX_HEIGHT_1;
import static org.pepsoft.minecraft.Constants.DEFAULT_MAX_HEIGHT_2;
import static org.pepsoft.worldpainter.Constants.*;
import static org.pepsoft.worldpainter.GameType.*;
import static org.pepsoft.worldpainter.Generator.*;
import static org.pepsoft.worldpainter.Platform.Capability.*;

/**
 *
 * @author pepijn
 */
public class DefaultPlugin extends AbstractPlugin implements LayerProvider, ContextProvider, WPContext, PlatformProvider {
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

    // PlatformProvider

    @Override
    public List<Platform> getSupportedPlatforms() {
        return PLATFORMS;
    }

    @Override
    public Chunk createChunk(Platform platform, int x, int z, int maxHeight) {
        if (platform.equals(JAVA_MCREGION)) {
            return new ChunkImpl(x, z, maxHeight);
        } else if (platform.equals(JAVA_ANVIL)) {
            return new ChunkImpl2(x, z, maxHeight);
        } else {
            throw new IllegalArgumentException("Platform " + platform + " not supported");
        }
    }

    @Override
    public ChunkStore getChunkStore(Platform platform, File worldDir, int dimension) {
        if (platform.equals(JAVA_MCREGION) || platform.equals(JAVA_ANVIL)) {
            File regionDir;
            switch (dimension) {
                case DIM_NORMAL:
                    regionDir = new File(worldDir, "region");
                    break;
                case DIM_NETHER:
                    regionDir = new File(worldDir, "DIM-1/region");
                    break;
                case DIM_END:
                    regionDir = new File(worldDir, "DIM1/region");
                    break;
                default:
                    throw new IllegalArgumentException("Dimension " + dimension + " not supported");
            }
            return new JavaChunkStore(platform, regionDir, false, null, platform.standardMaxHeight);
        } else {
            throw new IllegalArgumentException("Platform " + platform + " not supported");
        }
    }

    @Override
    public WorldExporter getExporter(World2 world) {
        Platform platform = world.getPlatform();
        if (platform.equals(JAVA_MCREGION) || platform.equals(JAVA_ANVIL)) {
            return new JavaWorldExporter(world);
        } else {
            throw new IllegalArgumentException("Platform " + platform + " not supported");
        }
    }

    @Override
    public File getDefaultExportDir(Platform platform) {
        File minecraftDir = MinecraftUtil.findMinecraftDir();
        return (minecraftDir != null) ? new File(minecraftDir, "saves") : null;
    }

    public static final Platform JAVA_MCREGION = new Platform(
            "org.pepsoft.mcregion",
            "Minecraft 1.1 (MCRegion)",
            32, DEFAULT_MAX_HEIGHT_1, 2048,
            Integer.MIN_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MAX_VALUE,
            Arrays.asList(SURVIVAL, CREATIVE),
            Arrays.asList(DEFAULT, FLAT),
            Arrays.asList(DIM_NORMAL, DIM_NETHER, DIM_END),
            EnumSet.of(PRECALCULATED_LIGHT, SET_SPAWN_POINT));

    public static final Platform JAVA_ANVIL = new Platform(
            "org.pepsoft.anvil",
            "Minecraft 1.2 or later (Anvil)",
            DEFAULT_MAX_HEIGHT_2, DEFAULT_MAX_HEIGHT_2, DEFAULT_MAX_HEIGHT_2,
            Integer.MIN_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MAX_VALUE,
            Arrays.asList(SURVIVAL, CREATIVE, ADVENTURE, HARDCORE),
            Arrays.asList(DEFAULT, FLAT, LARGE_BIOMES),
            Arrays.asList(DIM_NORMAL, DIM_NETHER, DIM_END),
            EnumSet.of(BIOMES, PRECALCULATED_LIGHT, SET_SPAWN_POINT));

    private static final List<Platform> PLATFORMS = Collections.unmodifiableList(Arrays.asList(JAVA_ANVIL, JAVA_MCREGION));
}