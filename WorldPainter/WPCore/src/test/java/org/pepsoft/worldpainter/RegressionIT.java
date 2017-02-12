package org.pepsoft.worldpainter;

import org.jnbt.CompoundTag;
import org.jnbt.NBTInputStream;
import org.jnbt.Tag;
import org.junit.Test;
import org.pepsoft.minecraft.*;
import org.pepsoft.util.FileUtils;
import org.pepsoft.util.ProgressReceiver;
import org.pepsoft.worldpainter.exporting.JavaWorldExporter;
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
import static org.pepsoft.minecraft.Constants.*;
import static org.pepsoft.worldpainter.Constants.*;

import org.pepsoft.worldpainter.Dimension;

/**
 * Created by Pepijn Schmitz on 09-01-17.
 */
@SuppressWarnings({"ResultOfMethodCallIgnored", "SameParameterValue"})
public class RegressionIT {
    @Test
    public void test2_3_6World() throws IOException, UnloadableWorldException, ProgressReceiver.OperationCancelled {
        World2 world = loadWorld("/testset/test-v2.3.6-1.world");
        File tmpBaseDir = createTmpBaseDir();
        try {
            File worldDir = exportWorld(world, tmpBaseDir);
            verifyJavaWorld(worldDir, SUPPORTED_VERSION_2);
            verifyJavaDimension(worldDir, world.getDimension(DIM_NORMAL),
                // Bedrock
                BLK_BEDROCK,

                // Stone Mix underground material
                BLK_STONE, BLK_GRAVEL, BLK_DIRT,

                // Surface
                BLK_GRASS, BLK_TALL_GRASS,

                // Air
                BLK_AIR,

                // Lakes
                BLK_STATIONARY_WATER,

                // Resources layer
                BLK_WATER, BLK_LAVA,

                // Various tree layers
                BLK_WOOD, BLK_LEAVES, BLK_VINES,

                // Desert terrain type
                BLK_SAND, BLK_CACTUS,

                // Frost layer
                BLK_SNOW,

                // Netherlike terrain type
                BLK_NETHERRACK, BLK_FIRE,

                // Annotations
                BLK_WOOL,

                // Plants
                BLK_LARGE_FLOWERS, BLK_WHEAT, BLK_CARROTS, BLK_POTATOES, BLK_PUMPKIN_STEM, BLK_MELON_STEM,

                // Deep Snow layer
                BLK_SNOW_BLOCK);
            verifyJavaDimension(worldDir, world.getDimension(DIM_NETHER),
                // Nether
                BLK_NETHERRACK, BLK_SOUL_SAND, BLK_GLOWSTONE, BLK_FIRE, BLK_LAVA, BLK_AIR);
            verifyJavaDimension(worldDir, world.getDimension(DIM_END),
                // End
                BLK_END_STONE, BLK_AIR);
        } finally {
            FileUtils.deleteDir(tmpBaseDir);
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

    private File exportWorld(World2 world, File baseDir) throws IOException, UnloadableWorldException, ProgressReceiver.OperationCancelled {
        // Prepare for export
        for (int i = 0; i < Terrain.CUSTOM_TERRAIN_COUNT; i++) {
            MixedMaterial material = world.getMixedMaterial(i);
            Terrain.setCustomMaterial(i, material);
        }

        // Export
        logger.info("Exporting world {}", world.getName());
        JavaWorldExporter worldExporter = new JavaWorldExporter(world);
        worldExporter.export(baseDir, world.getName(), null, null);

        // Return the directory into which the world was exported
        return new File(baseDir, FileUtils.sanitiseName(world.getName()));
    }

    private void verifyJavaWorld(File worldDir, int expectedVersion) throws IOException {
        Level level = Level.load(new File(worldDir, "level.dat"));
        assertEquals(expectedVersion, level.getVersion());
    }

    private void verifyJavaDimension(File worldDir, Dimension dimension, int... expectedBlocks) throws IOException {
        World2 world = dimension.getWorld();
        logger.info("Verifying dimension {} of map {}", dimension.getName(), world.getName());

        int expectedLowestChunkX = dimension.getLowestX() << 3, expectedHighestChunkX = ((dimension.getHighestX() + 1) << 3) - 1;
        int expectedLowestChunkZ = dimension.getLowestY() << 3, expectedHighestChunkZ = ((dimension.getHighestY() + 1) << 3) - 1;
        Rectangle expectedBounds = new Rectangle(expectedLowestChunkX, expectedLowestChunkZ, expectedHighestChunkX - expectedLowestChunkX + 1, expectedHighestChunkZ - expectedLowestChunkZ + 1);

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
        int maxHeight = dimension.getMaxHeight();
        Pattern regionFilePattern = platform.equals(DefaultPlugin.JAVA_MCREGION)
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
                                    chunk = platform.equals(DefaultPlugin.JAVA_MCREGION)
                                            ? new ChunkImpl((CompoundTag) tag, maxHeight, true)
                                            : new ChunkImpl2((CompoundTag) tag, maxHeight, true);
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
        assertEquals(expectedBounds, new Rectangle(lowestChunkX, lowestChunkZ, highestChunkX - lowestChunkX + 1, highestChunkZ - lowestChunkZ + 1));

        // Check blocks we know should definitely be present due to the terrain
        // types and layers used
        for (int expectedBlock: expectedBlocks) {
            assertTrue("expected block type " + Block.BLOCKS[expectedBlock].name + " missing", blockTypes.get(expectedBlock));
        }
    }

    private static final Logger logger = LoggerFactory.getLogger(RegressionIT.class);
}