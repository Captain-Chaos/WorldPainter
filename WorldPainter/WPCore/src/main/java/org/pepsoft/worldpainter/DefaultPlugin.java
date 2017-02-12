/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter;

import org.pepsoft.minecraft.*;
import org.pepsoft.worldpainter.exporting.JavaChunkStore;
import org.pepsoft.worldpainter.exporting.JavaWorldExporter;
import org.pepsoft.worldpainter.exporting.WorldExporter;
import org.pepsoft.worldpainter.layers.*;
import org.pepsoft.worldpainter.plugins.AbstractPlugin;
import org.pepsoft.worldpainter.plugins.ContextProvider;
import org.pepsoft.worldpainter.plugins.LayerProvider;
import org.pepsoft.worldpainter.plugins.PlatformProvider;
import org.pepsoft.worldpainter.util.MinecraftJarProvider;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.pepsoft.minecraft.Constants.*;
import static org.pepsoft.worldpainter.Constants.DIM_END;
import static org.pepsoft.worldpainter.Constants.DIM_NETHER;
import static org.pepsoft.worldpainter.Constants.DIM_NORMAL;
import static org.pepsoft.worldpainter.GameType.*;
import static org.pepsoft.worldpainter.Generator.DEFAULT;
import static org.pepsoft.worldpainter.Generator.FLAT;
import static org.pepsoft.worldpainter.Generator.LARGE_BIOMES;

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
                    regionDir = new File(worldDir, "regions");
                    break;
                case DIM_NETHER:
                    regionDir = new File(worldDir, "DIM-1/regions");
                    break;
                case DIM_END:
                    regionDir = new File(worldDir, "DIM1/regions");
                    break;
                default:
                    throw new IllegalArgumentException("Dimension " + dimension + " not supported");
            }
            return new JavaChunkStore(platform, regionDir, false, null, platform.getStandardMaxHeight());
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

    public static final Platform JAVA_MCREGION = new Platform("org.pepsoft.mcregion", "Java/MCRegion", false, 32, DEFAULT_MAX_HEIGHT_1, 2048, Arrays.asList(SURVIVAL, CREATIVE), Arrays.asList(DEFAULT, FLAT));

    public static final Platform JAVA_ANVIL = new Platform("org.pepsoft.anvil", "Java/Anvil", true, DEFAULT_MAX_HEIGHT_2, DEFAULT_MAX_HEIGHT_2, DEFAULT_MAX_HEIGHT_2, Arrays.asList(SURVIVAL, CREATIVE, ADVENTURE, HARDCORE), Arrays.asList(DEFAULT, FLAT, LARGE_BIOMES));

    private static final List<Platform> PLATFORMS = Collections.unmodifiableList(Arrays.asList(JAVA_ANVIL, JAVA_MCREGION));
}