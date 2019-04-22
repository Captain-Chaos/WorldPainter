package org.pepsoft.worldpainter;

import org.jnbt.NBTInputStream;
import org.jnbt.Tag;
import org.junit.BeforeClass;
import org.junit.Test;
import org.pepsoft.minecraft.Block;
import org.pepsoft.minecraft.Chunk;
import org.pepsoft.minecraft.Level;
import org.pepsoft.minecraft.RegionFile;
import org.pepsoft.util.FileUtils;
import org.pepsoft.util.ProgressReceiver;
import org.pepsoft.worldpainter.exporting.JavaMinecraftWorld;
import org.pepsoft.worldpainter.exporting.JavaWorldExporter;
import org.pepsoft.worldpainter.exporting.MinecraftWorld;
import org.pepsoft.worldpainter.layers.NotPresent;
import org.pepsoft.worldpainter.plugins.PlatformManager;
import org.pepsoft.worldpainter.plugins.WPPluginManager;
import org.pepsoft.worldpainter.util.MinecraftWorldUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.BitSet;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.pepsoft.worldpainter.Constants.*;
import static org.pepsoft.worldpainter.DefaultPlugin.JAVA_ANVIL;
import static org.pepsoft.worldpainter.DefaultPlugin.JAVA_ANVIL_1_13;

/**
 * Created by Pepijn Schmitz on 09-01-17.
 */
@SuppressWarnings({"ResultOfMethodCallIgnored", "SameParameterValue"})
public class RegressionIT {
    @BeforeClass
    public static void init() {
        WPPluginManager.initialise(null);
    }

    /**
     * Test whether a version 2.3.6-era world can still be loaded and exported,
     * in 1.12 and in 1.13 format. Check whether the result is the same as when
     * version 2.6.0 exported it, and that they are the same as each other.
     */
    @Test
    public void test2_3_6World() throws IOException, UnloadableWorldException, ProgressReceiver.OperationCancelled {
        World2 world = loadWorld("/testset/test-v2.3.6-1.world");
        File tmpBaseDir = createTmpBaseDir();
        File anvil12worldDir = exportJavaWorld(world, tmpBaseDir);
        world.setPlatform(JAVA_ANVIL_1_13);
        File anvil113worldDir = exportJavaWorld(world, tmpBaseDir);
//        try (ZipInputStream in = new ZipInputStream(RegressionIT.class.getResourceAsStream("/testset/test-v2.3.6-1-result.zip"))) {
//            ZipEntry zipEntry;
//            byte[] buffer = new byte[32768];
//            while ((zipEntry = in.getNextEntry()) != null) {
//                if (zipEntry.isDirectory()) {
//                    new File(tmpBaseDir, zipEntry.getName()).mkdir();
//                } else {
//                    try (FileOutputStream out = new FileOutputStream(new File(tmpBaseDir, zipEntry.getName()))) {
//                        int bytesRead;
//                        while ((bytesRead = in.read(buffer)) != -1) {
//                            out.write(buffer, 0, bytesRead);
//                        }
//                    }
//                }
//            }
//        }
        for (Dimension dimension: world.getDimensions()) {
            logger.info("Comparing dimension " + dimension.getName());
            Rectangle area = new Rectangle(dimension.getLowestX() << 5, dimension.getLowestY() << 5, dimension.getWidth() << 5, dimension.getHeight() << 5);
            try (MinecraftWorld anvil12World = new JavaMinecraftWorld(anvil12worldDir, dimension.getDim(), dimension.getMaxHeight(), JAVA_ANVIL, true, 256);
                    MinecraftWorld anvil113World = new JavaMinecraftWorld(anvil113worldDir, dimension.getDim(), dimension.getMaxHeight(), JAVA_ANVIL_1_13, true, 256)) {
                MinecraftWorldUtils.assertEquals("Anvil 1.2", anvil12World, "Anvil 1.13", anvil113World, area);
            }
        }
        FileUtils.deleteDir(tmpBaseDir);
    }

    protected File exportJavaWorld(World2 world, File baseDir) throws IOException, ProgressReceiver.OperationCancelled {
        // Prepare for export
        for (int i = 0; i < Terrain.CUSTOM_TERRAIN_COUNT; i++) {
            MixedMaterial material = world.getMixedMaterial(i);
            Terrain.setCustomMaterial(i, material);
        }

        // Export
        logger.info("Exporting world {}", world.getName());
        JavaWorldExporter worldExporter = new JavaWorldExporter(world);
        String name = world.getName() + "-" + world.getPlatform().id;
        worldExporter.export(baseDir, name, null, null);

        // Return the directory into which the world was exported
        return new File(baseDir, FileUtils.sanitiseName(name));
    }

    protected void verifyJavaWorld(File worldDir, int expectedVersion) throws IOException {
        Level level = Level.load(new File(worldDir, "level.dat"));
        assertEquals(expectedVersion, level.getVersion());
    }

    protected void verifyJavaDimension(File worldDir, Dimension dimension, int... expectedBlocks) throws IOException {
        World2 world = dimension.getWorld();
        logger.info("Verifying dimension {} of map {}", dimension.getName(), world.getName());

        boolean checkBounds;
        Rectangle expectedBounds = null;
        if (! dimension.containsOneOf(NotPresent.INSTANCE)) {
            checkBounds = true;
            int lowestTileX, highestTileX, lowestTileY, highestTileY;
            if (dimension.getWorld().getTilesToExport() != null) {
                lowestTileX = Integer.MAX_VALUE;
                highestTileX = Integer.MIN_VALUE;
                lowestTileY = Integer.MAX_VALUE;
                highestTileY = Integer.MIN_VALUE;
                for (Point tile : dimension.getWorld().getTilesToExport()) {
                    if (tile.x < lowestTileX) {
                        lowestTileX = tile.x;
                    }
                    if (tile.x > highestTileX) {
                        highestTileX = tile.x;
                    }
                    if (tile.y < lowestTileY) {
                        lowestTileY = tile.y;
                    }
                    if (tile.y > highestTileY) {
                        highestTileY = tile.y;
                    }
                }
            } else {
                lowestTileX = dimension.getLowestX();
                highestTileX = dimension.getHighestX();
                lowestTileY = dimension.getLowestY();
                highestTileY = dimension.getHighestY();
            }
            int expectedLowestChunkX = lowestTileX << 3;
            int expectedHighestChunkX = ((highestTileX + 1) << 3) - 1;
            int expectedLowestChunkZ = lowestTileY << 3;
            int expectedHighestChunkZ = ((highestTileY + 1) << 3) - 1;
            expectedBounds = new Rectangle(expectedLowestChunkX, expectedLowestChunkZ, expectedHighestChunkX - expectedLowestChunkX + 1, expectedHighestChunkZ - expectedLowestChunkZ + 1);
        } else {
            checkBounds = false;
            logger.warn("Skipping bounds check for dimension which contains the NotReady layer");
        }

        File regionDir;
        switch (dimension.getDim()) {
            case DIM_NORMAL:
                regionDir = new File(worldDir, "region");
                break;
            case DIM_NETHER:
                regionDir = new File(worldDir, "DIM-1/region");
                break;
            case DIM_END:
                regionDir = new File(worldDir, "DIM1/region");
                break;
            default:
                throw new IllegalArgumentException();
        }
        Platform platform = world.getPlatform();
        DefaultPlatformProvider platformProvider = (DefaultPlatformProvider) PlatformManager.getInstance().getPlatformProvider(platform);
        int maxHeight = dimension.getMaxHeight();
        Pattern regionFilePattern = (platform == DefaultPlugin.JAVA_MCREGION)
            ? Pattern.compile("r\\.(-?\\d+)\\.(-?\\d+)\\.mcr")
            : Pattern.compile("r\\.(-?\\d+)\\.(-?\\d+)\\.mca");
        int lowestChunkX = Integer.MAX_VALUE, highestChunkX = Integer.MIN_VALUE;
        int lowestChunkZ = Integer.MAX_VALUE, highestChunkZ = Integer.MIN_VALUE;
        BitSet blockTypes = new BitSet(256);
        for (File file: regionDir.listFiles()) {
            Matcher matcher = regionFilePattern.matcher(file.getName());
            if (matcher.matches()) {
                int regionX = Integer.parseInt(matcher.group(1));
                int regionZ = Integer.parseInt(matcher.group(2));
                try (RegionFile regionFile = new RegionFile(file, true)) {
                    for (int chunkX = 0; chunkX < 32; chunkX++) {
                        for (int chunkZ = 0; chunkZ < 32; chunkZ++) {
                            if (regionFile.containsChunk(chunkX, chunkZ)) {
                                int absChunkX = (regionX << 5) + chunkX;
                                int absChunkZ = (regionZ << 5) + chunkZ;
                                if (absChunkX < lowestChunkX) {
                                    lowestChunkX = absChunkX;
                                }
                                if (absChunkX > highestChunkX) {
                                    highestChunkX = absChunkX;
                                }
                                if (absChunkZ < lowestChunkZ) {
                                    lowestChunkZ = absChunkZ;
                                }
                                if (absChunkZ > highestChunkZ) {
                                    highestChunkZ = absChunkZ;
                                }
                                Chunk chunk;
                                try (NBTInputStream in = new NBTInputStream(regionFile.getChunkDataInputStream(chunkX, chunkZ))) {
                                    Tag tag = in.readTag();
                                    chunk = platformProvider.createChunk(platform, tag, maxHeight, true);
                                }

                                // Iterate over all blocks to check whether the
                                // basic data structure are present, and inventory
                                // all block types present
                                for (int x = 0; x < 16; x++) {
                                    for (int y = 0; y < maxHeight; y++) {
                                        for (int z = 0; z < 16; z++) {
                                            blockTypes.set(chunk.getBlockType(x, y, z));
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        if (checkBounds) {
            assertEquals(expectedBounds, new Rectangle(lowestChunkX, lowestChunkZ, highestChunkX - lowestChunkX + 1, highestChunkZ - lowestChunkZ + 1));
        }

        // Check blocks we know should definitely be present due to the terrain
        // types and layers used
        for (int expectedBlock: expectedBlocks) {
            assertTrue("expected block type " + Block.BLOCKS[expectedBlock].name + " missing", blockTypes.get(expectedBlock));
        }
    }

    private World2 loadWorld(String worldName) throws IOException, UnloadableWorldException {
        // Load the world
        logger.info("Loading world {}", worldName);
        WorldIO worldIO = new WorldIO();
        worldIO.load(RegressionIT.class.getResourceAsStream(worldName));
        return worldIO.getWorld();
    }

    private File createTmpBaseDir() {
        File tmpBaseDir = new File(System.getProperty("java.io.tmpdir"), UUID.randomUUID().toString());
        tmpBaseDir.mkdirs();
        return tmpBaseDir;
    }

    private static final Logger logger = LoggerFactory.getLogger(RegressionIT.class);
}