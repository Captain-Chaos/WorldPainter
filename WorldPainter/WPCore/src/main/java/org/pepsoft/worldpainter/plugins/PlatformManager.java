package org.pepsoft.worldpainter.plugins;

import org.pepsoft.minecraft.Chunk;
import org.pepsoft.minecraft.ChunkStore;
import org.pepsoft.worldpainter.Platform;
import org.pepsoft.worldpainter.World2;
import org.pepsoft.worldpainter.exporting.WorldExporter;

import java.io.File;
import java.util.*;

/**
 * Created by Pepijn on 12-2-2017.
 */
public class PlatformManager {
    private PlatformManager() {
        List<Platform> platforms = new ArrayList<>();
        for (PlatformProvider platformProvider: WPPluginManager.getInstance().getPlugins(PlatformProvider.class)) {
            List<Platform> supportedPlatforms = platformProvider.getSupportedPlatforms();
            platforms.addAll(supportedPlatforms);
            for (Platform platform: supportedPlatforms) {
                platformProviders.put(platform, platformProvider);
            }
        }
        allPlatforms = Collections.unmodifiableList(platforms);
    }

    public List<Platform> getAllPlatforms() {
        return allPlatforms;
    }

    public PlatformProvider getPlatformProvider(Platform platform) {
        return platformProviders.get(platform);
    }

    public Chunk createChunk(Platform platform, int x, int z, int maxHeight) {
        return platformProviders.get(platform).createChunk(platform, x, z, maxHeight);
    }

    public ChunkStore getChunkStore(Platform platform, File worldDir, int dimension) {
        return platformProviders.get(platform).getChunkStore(platform, worldDir, dimension);
    }

    public WorldExporter getExporter(World2 world) {
        return platformProviders.get(world.getPlatform()).getExporter(world);
    }

    public static PlatformManager getInstance() {
        return INSTANCE;
    }

    private final List<Platform> allPlatforms;
    private final Map<Platform, PlatformProvider> platformProviders = new HashMap<>();

    private static final PlatformManager INSTANCE = new PlatformManager();
}