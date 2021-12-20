package org.pepsoft.worldpainter.platforms;

import org.jnbt.Tag;
import org.pepsoft.minecraft.ChunkStore;
import org.pepsoft.minecraft.Level;
import org.pepsoft.minecraft.NBTChunk;
import org.pepsoft.minecraft.RegionFile;
import org.pepsoft.minecraft.mapexplorer.JavaMapRecognizer;
import org.pepsoft.worldpainter.Platform;
import org.pepsoft.worldpainter.World2;
import org.pepsoft.worldpainter.exporting.*;
import org.pepsoft.worldpainter.mapexplorer.MapRecognizer;
import org.pepsoft.worldpainter.plugins.BlockBasedPlatformProvider;
import org.pepsoft.worldpainter.util.MinecraftUtil;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.google.common.primitives.Ints.toArray;
import static org.pepsoft.worldpainter.Constants.*;
import static org.pepsoft.worldpainter.util.MinecraftUtil.getRegionDir;

/**
 * Created by Pepijn on 9-3-2017.
 */
public abstract class JavaPlatformProvider extends AbstractPlatformProvider implements BlockBasedPlatformProvider {
    protected JavaPlatformProvider(String version, Platform platform) {
        super(version, platform);
    }

    public NBTChunk createChunk(Platform platform, Tag tag, int maxHeight) {
        return createChunk(platform, tag, maxHeight, false);
    }

    public abstract NBTChunk createChunk(Platform platform, Tag tag, int maxHeight, boolean readOnly);

    public abstract File[] getRegionFiles(Platform platform, File regionDir);

    public RegionFile getRegionFile(Platform platform, File regionDir, Point coords, boolean readOnly) throws IOException{
        return new RegionFile(getRegionFileFile(platform, regionDir, coords), readOnly);
    }

    public RegionFile getRegionFileIfExists(Platform platform, File regionDir, Point coords, boolean readOnly) throws IOException{
        File file = getRegionFileFile(platform, regionDir, coords);
        return file.isFile() ? new RegionFile(file, readOnly) : null;
    }

    protected abstract File getRegionFileFile(Platform platform, File regionDir, Point coords);

    // BlockBasedPlatformProvider

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
    public ChunkStore getChunkStore(Platform platform, File worldDir, int dimension) {
        ensurePlatformSupported(platform);
        Level level;
        File levelDatFile = new File(worldDir, "level.dat");
        try {
            level = Level.load(levelDatFile);
        } catch (IOException e) {
            throw new RuntimeException("I/O error while trying to read level.dat", e);
        }
        return new JavaChunkStore(this, getRegionDir(worldDir, dimension), false, null, level.getMaxHeight());
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
    public File selectBackupDir(File exportDir) {
        return new File(exportDir.getParentFile(), "backups");
    }

    @Override
    public MapRecognizer getMapRecognizer() {
        return new JavaMapRecognizer();
    }

    @Override
    public ExportSettings getDefaultExportSettings() {
        return new JavaExportSettings();
    }

    @Override
    public ExportSettingsEditor getExportSettingsEditor() {
        return new JavaExportSettingsEditor();
    }

    @SuppressWarnings("ConstantConditions") // Yes, we just checked that
    private boolean containsFiles(File dir) {
        return dir.isDirectory() && (dir.listFiles().length > 0);
    }
}