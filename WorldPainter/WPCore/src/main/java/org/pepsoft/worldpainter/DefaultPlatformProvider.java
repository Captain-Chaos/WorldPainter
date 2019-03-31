package org.pepsoft.worldpainter;

import com.google.common.collect.ImmutableList;
import org.pepsoft.minecraft.*;
import org.pepsoft.minecraft.mapexplorer.JavaMapRecognizer;
import org.pepsoft.worldpainter.exporting.*;
import org.pepsoft.worldpainter.mapexplorer.MapRecognizer;
import org.pepsoft.worldpainter.plugins.AbstractPlugin;
import org.pepsoft.worldpainter.plugins.BlockBasedPlatformProvider;
import org.pepsoft.worldpainter.util.MinecraftUtil;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.google.common.primitives.Ints.toArray;
import static org.pepsoft.worldpainter.Constants.*;
import static org.pepsoft.worldpainter.DefaultPlugin.JAVA_ANVIL;
import static org.pepsoft.worldpainter.DefaultPlugin.JAVA_MCREGION;
import static org.pepsoft.worldpainter.util.MinecraftUtil.getRegionDir;

/**
 * Created by Pepijn on 9-3-2017.
 */
public class DefaultPlatformProvider extends AbstractPlugin implements BlockBasedPlatformProvider {
    public DefaultPlatformProvider() {
        super("DefaultPlatforms", Version.VERSION);
    }

    @Override
    public List<Platform> getKeys() {
        return PLATFORMS;
    }

    @Override
    public int[] getDimensions(Platform platform, File worldDir) {
        ensurePlatformSupported(platform);
        List<Integer> dimensions = new ArrayList<>();
        for (int dim: new int[]{DIM_NORMAL, DIM_NETHER, DIM_END}) {
            if (containsFiles(getRegionDir(worldDir, dim))) {
                dimensions.add(dim);
            }
        }
        return toArray(dimensions);
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
        ensurePlatformSupported(platform);
        Level level;
        File levelDatFile = new File(worldDir, "level.dat");
        try {
            level = Level.load(levelDatFile);
        } catch (IOException e) {
            throw new RuntimeException("I/O error while trying to read level.dat", e);
        }
        return new JavaChunkStore(platform, getRegionDir(worldDir, dimension), false, null, level.getMaxHeight());
    }

    @Override
    public WorldExporter getExporter(World2 world) {
        Platform platform = world.getPlatform();
        ensurePlatformSupported(platform);
        return new JavaWorldExporter(world);
    }

    @Override
    public File getDefaultExportDir(Platform platform) {
        File minecraftDir = MinecraftUtil.findMinecraftDir();
        return (minecraftDir != null) ? new File(minecraftDir, "saves") : null;
    }

    @Override
    public PostProcessor getPostProcessor(Platform platform) {
        ensurePlatformSupported(platform);
        return new JavaPostProcessor();
    }

    @Override
    public MapRecognizer getMapRecognizer() {
        return new JavaMapRecognizer();
    }

    private void ensurePlatformSupported(Platform platform) {
        if (! PLATFORMS.contains(platform)) {
            throw new IllegalArgumentException("Platform " + platform + " not supported");
        }
    }

    @SuppressWarnings("ConstantConditions") // Yes, we just checked that
    private boolean containsFiles(File dir) {
        return dir.isDirectory() && (dir.listFiles().length > 0);
    }

    private static final List<Platform> PLATFORMS = ImmutableList.of(JAVA_ANVIL, JAVA_MCREGION);
}