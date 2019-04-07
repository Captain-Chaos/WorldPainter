package org.pepsoft.worldpainter;

import com.google.common.collect.ImmutableList;
import org.jnbt.CompoundTag;
import org.jnbt.IntTag;
import org.jnbt.Tag;
import org.pepsoft.minecraft.*;
import org.pepsoft.minecraft.mapexplorer.JavaMapRecognizer;
import org.pepsoft.worldpainter.exporting.*;
import org.pepsoft.worldpainter.mapexplorer.MapRecognizer;
import org.pepsoft.worldpainter.plugins.AbstractPlugin;
import org.pepsoft.worldpainter.plugins.BlockBasedPlatformProvider;
import org.pepsoft.worldpainter.util.MinecraftUtil;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static com.google.common.primitives.Ints.toArray;
import static org.pepsoft.minecraft.Constants.DATA_VERSION_MC_1_12_2;
import static org.pepsoft.minecraft.Constants.TAG_DATA_VERSION;
import static org.pepsoft.worldpainter.Constants.*;
import static org.pepsoft.worldpainter.DefaultPlugin.*;
import static org.pepsoft.worldpainter.util.MinecraftUtil.getRegionDir;

/**
 * Created by Pepijn on 9-3-2017.
 */
public class DefaultPlatformProvider extends AbstractPlugin implements BlockBasedPlatformProvider {
    public DefaultPlatformProvider() {
        super("DefaultPlatforms", Version.VERSION);
    }

    public NBTChunk createChunk(Platform platform, Tag tag, int maxHeight) {
        return createChunk(platform, tag, maxHeight, false);
    }

    public NBTChunk createChunk(Platform platform, Tag tag, int maxHeight, boolean readOnly) {
        if ((platform == JAVA_MCREGION)) {
            return new MCRegionChunk((CompoundTag) tag, maxHeight, readOnly);
        } else if ((platform == JAVA_ANVIL) || (platform == JAVA_ANVIL_1_13)) {
            Tag dataVersionTag = ((CompoundTag) tag).getTag(TAG_DATA_VERSION);
            if ((dataVersionTag == null) || ((IntTag) dataVersionTag).getValue() <= DATA_VERSION_MC_1_12_2) {
                return new MC12AnvilChunk((CompoundTag) tag, maxHeight, readOnly);
            } else {
                return new MC113AnvilChunk((CompoundTag) tag, maxHeight, readOnly);
            }
        } else {
            throw new IllegalArgumentException("Platform " + platform + " not supported");
        }
    }

    public File[] getRegionFiles(Platform platform, File regionDir) {
        final Pattern regionFilePattern = (platform == JAVA_MCREGION)
                ? Pattern.compile("r\\.-?\\d+\\.-?\\d+\\.mcr")
                : Pattern.compile("r\\.-?\\d+\\.-?\\d+\\.mca");
        return regionDir.listFiles((dir, name) -> regionFilePattern.matcher(name).matches());
    }

    public RegionFile getRegionFile(Platform platform, File regionDir, Point coords, boolean readOnly) throws IOException{
        return new RegionFile(getRegionFileFile(platform, regionDir, coords), readOnly);
    }

    public RegionFile getRegionFileIfExists(Platform platform, File regionDir, Point coords, boolean readOnly) throws IOException{
        File file = getRegionFileFile(platform, regionDir, coords);
        return file.isFile() ? new RegionFile(file, readOnly) : null;
    }

    private File getRegionFileFile(Platform platform, File regionDir, Point coords) {
        if ((platform == JAVA_MCREGION)) {
            return new File(regionDir, "r." + coords.x + "." + coords.y + ".mcr");
        } else if ((platform == JAVA_ANVIL) || (platform == JAVA_ANVIL_1_13)) {
            return new File(regionDir, "r." + coords.x + "." + coords.y + ".mca");
        } else {
            throw new IllegalArgumentException("Platform " + platform + " not supported");
        }
    }

    // BlockBasedPlatformProvider

    @Override
    public List<Platform> getKeys() {
        return PLATFORMS;
    }

    @Override
    public int[] getDimensions(Platform platform, File worldDir) {
        ensurePlatformSupported(platform);
        List<Integer> dimensions = new ArrayList<>();
        for (int dim: new int[] {DIM_NORMAL, DIM_NETHER, DIM_END}) {
            if (containsFiles(getRegionDir(worldDir, dim))) {
                dimensions.add(dim);
            }
        }
        return toArray(dimensions);
    }

    @Override
    public Chunk createChunk(Platform platform, int x, int z, int maxHeight) {
        if (platform == JAVA_MCREGION) {
            return new MCRegionChunk(x, z, maxHeight);
        } else if (platform == JAVA_ANVIL) {
            return new MC12AnvilChunk(x, z, maxHeight);
        } else if (platform == JAVA_ANVIL_1_13) {
            return new MC113AnvilChunk(x, z, maxHeight);
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
        if ((platform == JAVA_MCREGION) || (platform == JAVA_ANVIL)) {
            return new Java1_2PostProcessor();
        } else if (platform == JAVA_ANVIL_1_13) {
            return new Java1_13PostProcessor();
        } else {
            throw new IllegalArgumentException("Platform " + platform + " not supported");
        }
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

    private static final List<Platform> PLATFORMS = ImmutableList.of(JAVA_ANVIL_1_13, JAVA_ANVIL, JAVA_MCREGION);
}