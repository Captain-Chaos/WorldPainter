package org.pepsoft.worldpainter.plugins;

import org.pepsoft.minecraft.Chunk;
import org.pepsoft.minecraft.ChunkStore;
import org.pepsoft.worldpainter.Platform;
import org.pepsoft.worldpainter.World2;
import org.pepsoft.worldpainter.exporting.PostProcessor;
import org.pepsoft.worldpainter.exporting.WorldExporter;

import java.io.File;
import java.util.List;

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

    public static PlatformManager getInstance() {
        return INSTANCE;
    }

    private static final PlatformManager INSTANCE = new PlatformManager();
}