package org.pepsoft.worldpainter;

import org.junit.BeforeClass;
import org.junit.Test;
import org.pepsoft.minecraft.ChunkStore;
import org.pepsoft.minecraft.Level;
import org.pepsoft.minecraft.Material;
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
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
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

    protected void verifyJavaDimension(File worldDir, Dimension dimension, Set<Material> expectedMaterials) {
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

        Platform platform = world.getPlatform();
        DefaultPlatformProvider platformProvider = (DefaultPlatformProvider) PlatformManager.getInstance().getPlatformProvider(platform);
        int maxHeight = dimension.getMaxHeight();
        int[] lowestChunkX = {Integer.MAX_VALUE}, highestChunkX = {Integer.MIN_VALUE};
        int[] lowestChunkZ = {Integer.MAX_VALUE}, highestChunkZ = {Integer.MIN_VALUE};
        Set<Material> materials = new HashSet<>();
        ChunkStore chunkStore = platformProvider.getChunkStore(platform, worldDir, dimension.getDim());
        chunkStore.visitChunks(chunk -> {
            if (chunk.getxPos() < lowestChunkX[0]) {
                lowestChunkX[0] = chunk.getxPos();
            }
            if (chunk.getxPos() > highestChunkX[0]) {
                highestChunkX[0] = chunk.getxPos();
            }
            if (chunk.getzPos() < lowestChunkZ[0]) {
                lowestChunkZ[0] = chunk.getzPos();
            }
            if (chunk.getzPos() > highestChunkZ[0]) {
                highestChunkZ[0] = chunk.getzPos();
            }

            // Iterate over all blocks to check whether the
            // basic data structure are present, and inventory
            // all block types present
            for (int x = 0; x < 16; x++) {
                for (int y = 0; y < maxHeight; y++) {
                    for (int z = 0; z < 16; z++) {
                        materials.add(chunk.getMaterial(x, y, z));
                    }
                }
            }
            return true;
        });
        if (checkBounds) {
            assertEquals(expectedBounds, new Rectangle(lowestChunkX[0], lowestChunkZ[0], highestChunkX[0] - lowestChunkX[0] + 1, highestChunkZ[0] - lowestChunkZ[0] + 1));
        }

        // Check blocks we know should definitely be present due to the terrain
        // types and layers used
        for (Material expectedMaterial: expectedMaterials) {
            assertTrue("expected block type " + expectedMaterial + " missing", materials.contains(expectedMaterial));
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