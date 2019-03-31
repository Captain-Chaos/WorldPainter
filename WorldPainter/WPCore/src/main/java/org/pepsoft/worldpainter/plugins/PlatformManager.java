package org.pepsoft.worldpainter.plugins;

import org.pepsoft.minecraft.Chunk;
import org.pepsoft.minecraft.ChunkStore;
import org.pepsoft.worldpainter.Platform;
import org.pepsoft.worldpainter.World2;
import org.pepsoft.worldpainter.exporting.PostProcessor;
import org.pepsoft.worldpainter.exporting.WorldExporter;
import org.pepsoft.worldpainter.mapexplorer.MapRecognizer;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
     * Identify the platform provider for a map.
     *
     * @param worldDir The directory to identify.
     * @return The platform provider which supports the format of the specified
     * map, or {@code null} if no platform provider claimed support.
     */
    public PlatformProvider identifyMap(File worldDir) {
        Set<PlatformProvider> candidates = new HashSet<>();
        for (PlatformProvider provider: getImplementations()) {
            MapRecognizer mapRecognizer = provider.getMapRecognizer();
            if ((mapRecognizer != null) && mapRecognizer.isMap(worldDir)) {
                candidates.add(provider);
            }
        }
        if (candidates.isEmpty()) {
            return null;
        } else if (candidates.size() == 1) {
            return candidates.iterator().next();
        } else {
            // If one of the candidates is ourselves, discount it, assuming that
            // the plugin did a more specific check and is probably right
            candidates.removeIf(provider -> provider.getName().equals("DefaultPlatforms"));
            if (candidates.size() == 1) {
                return candidates.iterator().next();
            } else {
                throw new RuntimeException("Multiple platform providers (" + candidates + ") claimed support for this map");
            }
        }
    }

    public static PlatformManager getInstance() {
        return INSTANCE;
    }

    private static final PlatformManager INSTANCE = new PlatformManager();
}