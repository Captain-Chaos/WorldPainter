package org.pepsoft.worldpainter.plugins;

import com.google.common.collect.ImmutableList;
import org.pepsoft.minecraft.Chunk;
import org.pepsoft.minecraft.ChunkStore;
import org.pepsoft.worldpainter.Platform;
import org.pepsoft.worldpainter.World2;
import org.pepsoft.worldpainter.exporting.PostProcessor;
import org.pepsoft.worldpainter.exporting.WorldExporter;
import org.pepsoft.worldpainter.plugins.PlatformProvider.MapInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.util.stream.Collectors.toList;
import static org.pepsoft.worldpainter.DefaultPlugin.*;

/**
 * Created by Pepijn on 12-2-2017.
 */
public class PlatformManager extends AbstractProviderManager<Platform, PlatformProvider> {
    private PlatformManager() {
        super(PlatformProvider.class);
    }

    public List<Platform> getAllPlatforms() {
        return getKeys();
    }

    public PlatformProvider getPlatformProvider(Platform platform) {
        return getImplementation(platform);
    }

    public Chunk createChunk(Platform platform, int x, int z, int maxHeight) {
        return ((BlockBasedPlatformProvider) getImplementation(platform)).createChunk(platform, x, z, maxHeight);
    }

    public ChunkStore getChunkStore(Platform platform, File worldDir, int dimension) {
        return ((BlockBasedPlatformProvider) getImplementation(platform)).getChunkStore(platform, worldDir, dimension);
    }

    public WorldExporter getExporter(World2 world) {
        return getImplementation(world.getPlatform()).getExporter(world);
    }

    public File getDefaultExportDir(Platform platform) {
        return getImplementation(platform).getDefaultExportDir(platform);
    }

    public PostProcessor getPostProcessor(Platform platform) {
        return ((BlockBasedPlatformProvider) getImplementation(platform)).getPostProcessor(platform);
    }

    /**
     * Identify the platform a map.
     *
     * @param worldDir The directory to identify.
     * @return The platform of the specified map, or {@code null} if no platform provider claimed support.
     */
    public Platform identifyPlatform(File worldDir) {
        MapInfo mapInfo = identifyMap(worldDir);
        return (mapInfo != null) ? mapInfo.platform : null;
    }

    /**
     * Identify a map.
     *
     * @param worldDir The directory to identify.
     * @return The identifying information, including platform, of the specified map, or {@code null} if no platform
     * provider claimed support.
     */
    public MapInfo identifyMap(File worldDir) {
        Set<MapInfo> candidates = new HashSet<>();
        for (PlatformProvider provider: getImplementations()) {
            try {
                MapInfo mapInfo = provider.identifyMap(worldDir);
                if (mapInfo != null) {
                    candidates.add(mapInfo);
                }
            } catch (RuntimeException e) {
                logger.warn("{} while asking provider {} to identify {}; skipping platform", e.getClass().getSimpleName(), provider.getClass().getName(), worldDir, e);
            }
        }
        if (candidates.isEmpty()) {
            return null;
        } else if (candidates.size() == 1) {
            return candidates.iterator().next();
        } else {
            Set<MapInfo> defaultCandidates = new HashSet<>(), pluginCandidates = new HashSet<>();
            candidates.forEach(mapInfo -> {
                if (DEFAULT_PLATFORMS.contains(mapInfo.platform)) {
                    defaultCandidates.add(mapInfo);
                } else {
                    pluginCandidates.add(mapInfo);
                }
            });
            if (pluginCandidates.size() == 1) {
                return candidates.iterator().next();
            } else if (pluginCandidates.size() > 1) {
                throw new RuntimeException("Multiple platform providers (" + pluginCandidates + ") claimed support for this map");
            } else {
                // Multiple default platforms matched; pick the newest one
                for (int i = DEFAULT_PLATFORMS.size() - 1; i >= 0; i--) {
                    Platform platform = DEFAULT_PLATFORMS.get(i);
                    List<MapInfo> mapInfos = candidates.stream().filter(mapInfo -> mapInfo.platform == platform).collect(toList());
                    if (! mapInfos.isEmpty()) {
                        return mapInfos.get(0);
                    }
                }
                throw new InternalError("Should never happen");
            }
        }
    }

    public static PlatformManager getInstance() {
        return INSTANCE;
    }

    // TODO: make this more generic
    public static final List<Platform> DEFAULT_PLATFORMS = ImmutableList.of(JAVA_MCREGION, JAVA_ANVIL, JAVA_ANVIL_1_15, JAVA_ANVIL_1_17, JAVA_ANVIL_1_18);

    private static final PlatformManager INSTANCE = new PlatformManager();
    private static final Logger logger = LoggerFactory.getLogger(PlatformManager.class);
}