package org.pepsoft.worldpainter.platforms;

import com.google.common.collect.ImmutableMap;
import org.jnbt.CompoundTag;
import org.jnbt.Tag;
import org.pepsoft.minecraft.*;
import org.pepsoft.minecraft.mapexplorer.JavaMapRootNode;
import org.pepsoft.worldpainter.*;
import org.pepsoft.worldpainter.Dimension;
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
import static org.pepsoft.util.IconUtils.loadUnscaledImage;
import static org.pepsoft.util.IconUtils.scaleIcon;
import static org.pepsoft.worldpainter.Constants.*;
import static org.pepsoft.worldpainter.DefaultPlugin.*;
import static org.pepsoft.worldpainter.Generator.CUSTOM;
import static org.pepsoft.worldpainter.util.MinecraftUtil.getRegionDir;

/**
 * A platform provider for Minecraft Java Edition chunk-based platforms.
 *
 * <p>Created by Pepijn on 9-3-2017.
 */
public final class JavaPlatformProvider extends AbstractPlatformProvider implements BlockBasedPlatformProvider, MapExplorerSupport, MapImporterProvider {
    public JavaPlatformProvider() {
        super(Version.VERSION, DEFAULT_JAVA_PLATFORMS, "JavaPlatformProvider");
    }

    public Set<DataType> getDataTypes(Platform platform) {
        return implementations.get(platform).getDataTypes();
    }

    public NBTChunk createChunk(Platform platform, Map<DataType, Tag> tags, int minHeight, int maxHeight) {
        return createChunk(platform, tags, minHeight, maxHeight, false);
    }

    public NBTChunk createChunk(Platform platform, Map<DataType, Tag> tags, int minHeight, int maxHeight, boolean readOnly) {
        return implementations.get(platform).createChunk(tags, minHeight, maxHeight, readOnly);
    }

    public File[] getRegionFiles(Platform platform, File regionDir, DataType dataType) {
        return implementations.get(platform).getRegionFiles(regionDir, dataType);
    }

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
    public Chunk createChunk(Platform platform, int x, int z, int minHeight, int maxHeight) {
        return implementations.get(platform).createChunk(x, z, minHeight, maxHeight);
    }

    @Override
    public JavaChunkStore getChunkStore(Platform platform, File worldDir, int dimension) {
        ensurePlatformSupported(platform);
        JavaLevel level;
        File levelDatFile = new File(worldDir, "level.dat");
        try {
            level = JavaLevel.load(levelDatFile);
        } catch (IOException e) {
            throw new RuntimeException("I/O error while trying to read level.dat", e);
        }
        return new JavaChunkStore(platform, getRegionDir(worldDir, dimension), level.getMinHeight(), level.getMaxHeight());
    }

    @Override
    public PostProcessor getPostProcessor(Platform platform) {
        return implementations.get(platform).getPostProcessor();
    }

    @Override
    public WorldExporter getExporter(World2 world, WorldExportSettings exportSettings) {
        Platform platform = world.getPlatform();
        ensurePlatformSupported(platform);
        return new JavaWorldExporter(world, exportSettings);
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
                return new MapInfo(dir, level.getPlatform(), level.getName(), ICON, level.getMinHeight(), level.getMaxHeight());
            } catch (IOException e) {
                if (logger.isDebugEnabled()) {
                    logger.info("I/O error reading {}; assuming it is not a (supported) Java Minecraft level.dat file", file.getAbsolutePath(), e);
                } else {
                    logger.info("I/O error reading {}; assuming it is not a (supported) Java Minecraft level.dat file (type: {}, message: \"{}\")", file.getAbsolutePath(), e.getClass().getSimpleName(), e.getMessage());
                }
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
            return new JavaMapImporter(mapInfo.platform, tileFactory, new File(dir, "level.dat"), chunksToSkip, readOnlyOption, dimensionsToImport);
        } else {
            throw new IllegalArgumentException("Platform for map " + dir.getName() + " could not be identified");
        }
    }

    @Override
    public ExportSettings getDefaultExportSettings(Platform platform) {
        return new JavaExportSettings();
    }

    @Override
    public ExportSettingsEditor getExportSettingsEditor(Platform platform) {
        return new JavaExportSettingsEditor(platform);
    }

    @Override
    public String isCompatible(Platform platform, World2 world) {
        ensurePlatformSupported(platform);
        final String superReason = super.isCompatible(platform, world);
        if ((superReason == null) && platform.getAttribute(ATTRIBUTE_MC_VERSION).isAtLeast(V_1_17)) {
            for (Dimension dimension: world.getDimensions()) {
                final MapGenerator generator = dimension.getGenerator();
                if ((generator != null) && (generator.getType() == CUSTOM) && (! (((CustomGenerator) generator).getSettings() instanceof CompoundTag))) {
                    return "World type " + generator + " not supported by map format";
                }
            }
        }
        return superReason;
    }

    private File getRegionFileFile(Platform platform, File regionDir, DataType dataType, Point coords) {
        return implementations.get(platform).getRegionFileFile(regionDir, dataType, coords);
    }

    @SuppressWarnings("ConstantConditions") // Yes, we just checked that
    private boolean containsFiles(File dir) {
        return dir.isDirectory() && (dir.listFiles().length > 0);
    }

    private final Map<Platform, AbstractJavaPlatformProviderImpl> implementations = ImmutableMap.of(
            JAVA_MCREGION, new MCRegionPlatformProvider(),
            JAVA_ANVIL, new Anvil1_2PlatformProvider(),
            JAVA_ANVIL_1_15, new Anvil1_15PlatformProvider(),
            JAVA_ANVIL_1_17, new Anvil1_17PlatformProvider(),
            JAVA_ANVIL_1_18, new Anvil1_18PlatformProvider(),
            JAVA_ANVIL_1_19, new Anvil1_18PlatformProvider(),
            JAVA_ANVIL_1_20_5, new Anvil1_18PlatformProvider()
    );

    public static final Icon ICON = new ImageIcon(scaleIcon(loadUnscaledImage("org/pepsoft/worldpainter/mapexplorer/maproot.png"), 16));

    private static final Logger logger = LoggerFactory.getLogger(JavaPlatformProvider.class);
}