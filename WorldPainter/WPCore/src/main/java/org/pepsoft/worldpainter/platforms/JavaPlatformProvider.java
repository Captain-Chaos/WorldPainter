package org.pepsoft.worldpainter.platforms;

import org.jnbt.Tag;
import org.pepsoft.minecraft.*;
import org.pepsoft.minecraft.mapexplorer.JavaMapRootNode;
import org.pepsoft.worldpainter.Platform;
import org.pepsoft.worldpainter.TileFactory;
import org.pepsoft.worldpainter.World2;
import org.pepsoft.worldpainter.exporting.*;
import org.pepsoft.worldpainter.importing.JavaMapImporter;
import org.pepsoft.worldpainter.importing.MapImporter;
import org.pepsoft.worldpainter.mapexplorer.MapExplorerSupport;
import org.pepsoft.worldpainter.mapexplorer.Node;
import org.pepsoft.worldpainter.plugins.BlockBasedPlatformProvider;
import org.pepsoft.worldpainter.plugins.MapImporterProvider;
import org.pepsoft.worldpainter.util.MinecraftUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.google.common.primitives.Ints.toArray;
import static java.util.Collections.singleton;
import static org.pepsoft.minecraft.DataType.REGION;
import static org.pepsoft.util.IconUtils.loadUnscaledImage;
import static org.pepsoft.util.IconUtils.scaleIcon;
import static org.pepsoft.worldpainter.Constants.*;
import static org.pepsoft.worldpainter.util.MinecraftUtil.getRegionDir;

/**
 * Created by Pepijn on 9-3-2017.
 */
public abstract class JavaPlatformProvider extends AbstractPlatformProvider implements BlockBasedPlatformProvider, MapExplorerSupport, MapImporterProvider {
    protected JavaPlatformProvider(String version, Platform platform) {
        super(version, platform);
    }

    public Set<DataType> getDataTypes() {
        return DATA_TYPES;
    }

    public NBTChunk createChunk(Platform platform, Map<DataType, Tag> tags, int maxHeight) {
        return createChunk(platform, tags, maxHeight, false);
    }

    public abstract NBTChunk createChunk(Platform platform, Map<DataType, Tag> tags, int maxHeight, boolean readOnly);

    public abstract File[] getRegionFiles(Platform platform, File regionDir, DataType dataType);

    /**
     * Get a region file. If {@code readOnly} is false, a region file will be created if it does not exist. Otherwise,
     * {@code null} will be returned if the region file does not exist.
     */
    public RegionFile getRegionFile(Platform platform, File regionDir, DataType dataType, Point coords, boolean readOnly) throws IOException{
        File file = getRegionFileFile(platform, regionDir, dataType, coords);
        if (file.isFile() || (! readOnly)) {
            return new RegionFile(file, readOnly);
        } else {
            return null;
        }
    }

    /**
     * Get a region file, if it exists. Otherwise, {@code null} will be returned.
     */
    public RegionFile getRegionFileIfExists(Platform platform, File regionDir, DataType dataType, Point coords, boolean readOnly) throws IOException{
        File file = getRegionFileFile(platform, regionDir, dataType, coords);
        return file.isFile() ? new RegionFile(file, readOnly) : null;
    }

    protected abstract File getRegionFileFile(Platform platform, File regionDir, DataType dataType, Point coords);

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
        JavaLevel level;
        File levelDatFile = new File(worldDir, "level.dat");
        try {
            level = JavaLevel.load(levelDatFile);
        } catch (IOException e) {
            throw new RuntimeException("I/O error while trying to read level.dat", e);
        }
        return new JavaChunkStore(this, getRegionDir(worldDir, dimension), level.getMaxHeight());
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

    public MapInfo identifyMap(File dir) {
        File file = new File(dir, "level.dat");
        if (file.isFile()
                // Distinguish from Bedrock Edition maps:
                && (! new File(dir, "db").isDirectory())
                && (! new File(dir, "levelname.txt").isFile())) {
            try {
                JavaLevel level = JavaLevel.load(file);
                return new MapInfo(dir, level.getPlatform(), level.getName(), ICON, level.getMaxHeight());
            } catch (IOException e) {
                logger.info("I/O error reading level.dat; assuming it is not a (supported) Java Minecraft level.dat file", e);
            }
        }
        return null;
    }

    // MapExplorerSupport

    @Override
    public Node getMapNode(File mapDir) {
        return new JavaMapRootNode(mapDir);
    }

    // MapImporterProvider

    @Override
    public MapImporter getImporter(File dir, TileFactory tileFactory, Set<MinecraftCoords> chunksToSkip, MapImporter.ReadOnlyOption readOnlyOption, Set<Integer> dimensionsToImport) {
        MapInfo mapInfo = identifyMap(dir);
        if (mapInfo != null) {
            ensurePlatformSupported(mapInfo.platform);
            return new JavaMapImporter(mapInfo.platform, tileFactory, new File(dir, "level.dat"), false, chunksToSkip, readOnlyOption, dimensionsToImport);
        } else {
            throw new IllegalArgumentException("Platform for map " + dir.getName() + " could not be identified");
        }
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

    public static final Icon ICON = new ImageIcon(scaleIcon(loadUnscaledImage("org/pepsoft/worldpainter/mapexplorer/maproot.png"), 16));

    private static final Set<DataType> DATA_TYPES = singleton(REGION);
    private static final Logger logger = LoggerFactory.getLogger(JavaPlatformProvider.class);
}