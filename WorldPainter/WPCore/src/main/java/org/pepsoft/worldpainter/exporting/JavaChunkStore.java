package org.pepsoft.worldpainter.exporting;

import com.google.common.collect.ImmutableSet;
import org.jnbt.CompoundTag;
import org.jnbt.NBTInputStream;
import org.jnbt.NBTOutputStream;
import org.pepsoft.minecraft.*;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.JavaPlatformProvider;
import org.pepsoft.worldpainter.Platform;
import org.pepsoft.worldpainter.layers.ReadOnly;
import org.pepsoft.worldpainter.plugins.PlatformManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.vecmath.Point3i;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import static org.pepsoft.minecraft.Block.BLOCK_TYPE_NAMES;
import static org.pepsoft.minecraft.Constants.*;
import static org.pepsoft.worldpainter.DefaultPlugin.*;

/**
 * Created by Pepijn on 15-12-2016.
 */
public class JavaChunkStore implements ChunkStore {
    public JavaChunkStore(Platform platform, File regionDir, boolean honourReadOnlyChunks, Dimension dimension, int maxHeight) {
        if (! ((platform == JAVA_MCREGION) || (platform == JAVA_ANVIL) || (platform == JAVA_ANVIL_1_13))) {
            throw new IllegalArgumentException("Unsupported platform " + platform);
        }
        this.platform = platform;
        this.platformProvider = (JavaPlatformProvider) PlatformManager.getInstance().getPlatformProvider(platform);
        this.regionDir = regionDir;
        this.honourReadOnlyChunks = honourReadOnlyChunks;
        this.dimension = dimension;
        this.maxHeight = maxHeight;
    }

    @Override
    public void saveChunk(Chunk chunk) {
//        chunksSaved++;
//        updateStatistics();
//        long start = System.currentTimeMillis();
        // Do some sanity checks first
        if ((platform == JAVA_MCREGION) || (platform == JAVA_ANVIL)) {
            // Check that all tile entities for which the chunk contains data are
            // actually there
            for (Iterator<TileEntity> i = chunk.getTileEntities().iterator(); i.hasNext(); ) {
                final TileEntity tileEntity = i.next();
                final Set<Integer> blockIds = LEGACY_TILE_ENTITY_MAP.get(tileEntity.getId());
                if (blockIds == null) {
                    logger.warn("Unknown tile entity ID \"" + tileEntity.getId() + "\" encountered @ " + tileEntity.getX() + "," + tileEntity.getZ() + "," + tileEntity.getY() + "; can't check whether the corresponding block is there!");
                } else {
                    final int existingBlockId = chunk.getBlockType(tileEntity.getX() & 0xf, tileEntity.getY(), tileEntity.getZ() & 0xf);
                    if (! blockIds.contains(existingBlockId)) {
                        // The block at the specified location
                        // is not a tile entity, or a different
                        // tile entity. Remove the data
                        i.remove();
                        if (logger.isDebugEnabled()) {
                            logger.debug("Removing tile entity " + tileEntity.getId() + " @ " + tileEntity.getX() + "," + tileEntity.getZ() + "," + tileEntity.getY() + " because the block at that location is a " + BLOCK_TYPE_NAMES[existingBlockId]);
                        }
                    }
                }
            }
            // Check that there aren't multiple tile entities (of the same type,
            // otherwise they would have been removed above) in the same location
            Set<Point3i> occupiedCoords = new HashSet<>();
            for (Iterator<TileEntity> i = chunk.getTileEntities().iterator(); i.hasNext(); ) {
                TileEntity tileEntity = i.next();
                Point3i coords = new Point3i(tileEntity.getX(), tileEntity.getZ(), tileEntity.getY());
                if (occupiedCoords.contains(coords)) {
                    // There is already tile data for that location in the chunk;
                    // remove this copy
                    i.remove();
                    logger.warn("Removing tile entity " + tileEntity.getId() + " @ " + tileEntity.getX() + "," + tileEntity.getZ() + "," + tileEntity.getY() + " because there is already a tile entity of the same type at that location");
                } else {
                    occupiedCoords.add(coords);
                }
            }
        } else if (platform == JAVA_ANVIL_1_13) {
            // TODOMC13: similar sanity checks
        }

        try {
            int x = chunk.getxPos(), z = chunk.getzPos();
            RegionFile regionFile = getOrCreateRegionFile(new Point(x >> 5, z >> 5));
            try (NBTOutputStream out = new NBTOutputStream(regionFile.getChunkDataOutputStream(x & 31, z & 31))) {
                out.writeTag(((NBTItem) chunk).toNBT());
            }
        } catch (IOException e) {
            throw new RuntimeException("I/O error saving chunk", e);
        }
//        timeSpentSaving += System.currentTimeMillis() - start;
    }

    @Override
    public void doInTransaction(Runnable task) {
        task.run();
    }

    /**
     * Saves all dirty chunks and closes all files. Ensures that all changes are
     * saved and no system resources are being used, but the objects can still
     * be used; any subsequent operations will open files as needed again.
     */
//    @Override
    public synchronized void flush() {
        try {
            if (logger.isDebugEnabled()) {
                logger.debug("Closing " + regionFiles.size() + " region files");
            }
            for (RegionFile regionFile: regionFiles.values()) {
                regionFile.close();
            }
        } catch (IOException e) {
            throw new RuntimeException("I/O error while closing region files", e);
        }
        regionFiles.clear();
//        float elapsed = (System.currentTimeMillis() - lastStatisticsTimestamp) / 1000f;
//        System.out.println("Loading " + chunksLoaded / elapsed + " chunks per second");
//        System.out.println("Saving " + chunksSaved / elapsed + " chunks per second");
//        System.out.println("Fast cache hits: " + fastCacheHits / elapsed + " per second");
//        System.out.println("Cache hits: " + cacheHits / elapsed + " per second");
    }

    @Override
    public boolean isChunkPresent(int x, int z) {
        return false;
    }

    /**
     * Load a chunk. Returns <code>null</code> if the chunk is outside the
     * WorldPainter world boundaries.
     *
     * @param x The X coordinate in the Minecraft coordinate system of the chunk
     *     to load.
     * @param z The Z coordinate in the Minecraft coordinate system of the chunk
     *     to load.
     * @return The specified chunk, or <code>null</code> if the coordinates are
     *     outside the WorldPainter world boundaries.
     */
    @Override
    public Chunk getChunk(int x, int z) {
//        updateStatistics();
//        if ((x < lowestX) || (x > highestX) || (z < lowestZ) || (z > highestZ)) {
//            return null;
//        }
//        long start = System.currentTimeMillis();
        try {
            RegionFile regionFile = getRegionFile(new Point(x >> 5, z >> 5));
            if (regionFile == null) {
                return null;
            }
            InputStream chunkIn = regionFile.getChunkDataInputStream(x & 31, z & 31);
            if (chunkIn != null) {
//                chunksLoaded++;
                try (NBTInputStream in = new NBTInputStream(chunkIn)) {
                    CompoundTag tag = (CompoundTag) in.readTag();
//                    timeSpentLoading += System.currentTimeMillis() - start;
                    boolean readOnly = honourReadOnlyChunks && dimension.getBitLayerValueAt(ReadOnly.INSTANCE, x << 4, z << 4);
                    return platformProvider.createChunk(platform, tag, maxHeight, readOnly);
                }
            } else {
//                timeSpentLoading += System.currentTimeMillis() - start;
                return null;
            }
        } catch (IOException e) {
            throw new RuntimeException("I/O error loading chunk", e);
        }
    }

    /**
     * Load a chunk. Returns <code>null</code> if the chunk is outside the
     * WorldPainter world boundaries.
     *
     * @param x The X coordinate in the Minecraft coordinate system of the chunk
     *     to load.
     * @param z The Z coordinate in the Minecraft coordinate system of the chunk
     *     to load.
     * @return The specified chunk, or <code>null</code> if the coordinates are
     *     outside the WorldPainter world boundaries.
     */
    @Override
    public Chunk getChunkForEditing(int x, int z) {
        return getChunk(x, z);
    }

    @Override
    public void close() {
        flush();
    }

    private RegionFile getRegionFile(Point regionCoords) throws IOException {
        RegionFile regionFile = regionFiles.get(regionCoords);
        if (regionFile == null) {
            regionFile = platformProvider.getRegionFileIfExists(platform, regionDir, regionCoords, false);
            if (regionFile != null) {
                regionFiles.put(regionCoords, regionFile);
            }
        }
        return regionFile;
    }

    private RegionFile getOrCreateRegionFile(Point regionCoords) throws IOException {
        RegionFile regionFile = regionFiles.get(regionCoords);
        if (regionFile == null) {
            regionFile = platformProvider.getRegionFile(platform, regionDir, regionCoords, false);
            regionFiles.put(regionCoords, regionFile);
        }
        return regionFile;
    }

    //    private void updateStatistics() {
//        long now = System.currentTimeMillis();
//        if ((now - lastStatisticsTimestamp) > 5000) {
//            float elapsed = (now - lastStatisticsTimestamp) / 1000f;
//            System.out.println("Cached chunks: " + cache.size());
//            System.out.println("Dirty chunks: " + dirtyChunks.size());
//            System.out.println("Loading " + chunksLoaded / elapsed + " chunks per second");
//            System.out.println("Saving " + chunksSaved / elapsed + " chunks per second");
//            System.out.println("Fast cache hits: " + fastCacheHits / elapsed + " per second");
//            System.out.println("Cache hits: " + cacheHits / elapsed + " per second");
//            System.out.println("Time spent getting chunks: " + timeSpentGetting + " ms");
//            if (chunksLoaded > 0) {
//                System.out.println("Time spent loading chunks: " + timeSpentLoading + " ms (" + (timeSpentLoading / chunksLoaded) + " ms per chunk");
//            }
//            if (chunksSaved > 0) {
//                System.out.println("Time spent saving chunks: " + timeSpentSaving + " ms (" + (timeSpentSaving / chunksSaved) + " ms per chunk");
//            }
//            lastStatisticsTimestamp = now;
//            chunksLoaded = 0;
//            chunksSaved = 0;
//            fastCacheHits = 0;
//            cacheHits = 0;
//            timeSpentGetting = 0;
//            timeSpentLoading = 0;
//            timeSpentSaving = 0;
//        }
//    }

    private final Platform platform;
    private final JavaPlatformProvider platformProvider;
    private final File regionDir;
    private final Map<Point, RegionFile> regionFiles = new HashMap<>();
    private final boolean honourReadOnlyChunks;
    private final Dimension dimension;
    private final int maxHeight;

    private static final Map<String, Set<Integer>> LEGACY_TILE_ENTITY_MAP = new HashMap<>();

    static {
        LEGACY_TILE_ENTITY_MAP.put(ID_AIRPORTAL, Collections.singleton(BLK_END_PORTAL));
        LEGACY_TILE_ENTITY_MAP.put(ID_BEACON, Collections.singleton(BLK_BEACON));
        LEGACY_TILE_ENTITY_MAP.put(ID_CAULDRON, Collections.singleton(BLK_BREWING_STAND));
        LEGACY_TILE_ENTITY_MAP.put(ID_CHEST, ImmutableSet.of(BLK_CHEST, BLK_TRAPPED_CHEST));
        LEGACY_TILE_ENTITY_MAP.put(ID_COMPARATOR, ImmutableSet.of(BLK_REDSTONE_COMPARATOR_UNPOWERED, BLK_REDSTONE_COMPARATOR_POWERED));
        LEGACY_TILE_ENTITY_MAP.put(ID_CONTROL, ImmutableSet.of(BLK_COMMAND_BLOCK, BLK_CHAIN_COMMAND_BLOCK, BLK_REPEATING_COMMAND_BLOCK));
        LEGACY_TILE_ENTITY_MAP.put(ID_DLDETECTOR, ImmutableSet.of(BLK_DAYLIGHT_SENSOR, BLK_DAYLIGHT_SENSOR_INVERTED));
        LEGACY_TILE_ENTITY_MAP.put(ID_DROPPER, Collections.singleton(BLK_DROPPER));
        LEGACY_TILE_ENTITY_MAP.put(ID_ENCHANTTABLE, Collections.singleton(BLK_ENCHANTMENT_TABLE));
        LEGACY_TILE_ENTITY_MAP.put(ID_ENDERCHEST, Collections.singleton(BLK_ENDER_CHEST));
        LEGACY_TILE_ENTITY_MAP.put(ID_FLOWERPOT, Collections.singleton(BLK_FLOWER_POT));
        LEGACY_TILE_ENTITY_MAP.put(ID_FURNACE, ImmutableSet.of(BLK_FURNACE, BLK_BURNING_FURNACE));
        LEGACY_TILE_ENTITY_MAP.put(ID_HOPPER, Collections.singleton(BLK_HOPPER));
        LEGACY_TILE_ENTITY_MAP.put(ID_MOBSPAWNER, Collections.singleton(BLK_MONSTER_SPAWNER));
        LEGACY_TILE_ENTITY_MAP.put(ID_MUSIC, Collections.singleton(BLK_NOTE_BLOCK));
        LEGACY_TILE_ENTITY_MAP.put(ID_PISTON, Collections.singleton(BLK_PISTON_HEAD));
        LEGACY_TILE_ENTITY_MAP.put(ID_RECORDPLAYER, Collections.singleton(BLK_JUKEBOX));
        LEGACY_TILE_ENTITY_MAP.put(ID_SIGN, ImmutableSet.of(BLK_SIGN, BLK_WALL_SIGN));
        LEGACY_TILE_ENTITY_MAP.put(ID_SKULL, Collections.singleton(BLK_HEAD));
        LEGACY_TILE_ENTITY_MAP.put(ID_TRAP, Collections.singleton(BLK_DISPENSER));
        LEGACY_TILE_ENTITY_MAP.put(ID_BANNER, ImmutableSet.of(BLK_STANDING_BANNER, BLK_WALL_BANNER));
        LEGACY_TILE_ENTITY_MAP.put(ID_STRUCTURE, Collections.singleton(BLK_STRUCTURE_BLOCK));
        LEGACY_TILE_ENTITY_MAP.put(ID_SHULKER_BOX, ImmutableSet.of(BLK_WHITE_SHULKER_BOX, BLK_ORANGE_SHULKER_BOX, BLK_MAGENTA_SHULKER_BOX, BLK_LIGHT_BLUE_SHULKER_BOX, BLK_YELLOW_SHULKER_BOX, BLK_LIME_SHULKER_BOX, BLK_PINK_SHULKER_BOX, BLK_GREY_SHULKER_BOX, BLK_LIGHT_GREY_SHULKER_BOX, BLK_CYAN_SHULKER_BOX, BLK_PURPLE_SHULKER_BOX, BLK_BLUE_SHULKER_BOX, BLK_BROWN_SHULKER_BOX, BLK_GREEN_SHULKER_BOX, BLK_RED_SHULKER_BOX, BLK_BLACK_SHULKER_BOX));

        // Make sure the tile entity flag in the block database is consistent
        // with the tile entity map:
        Set<Integer> allTileEntityIds = new HashSet<>();
        for (Set<Integer> blockIdSet: LEGACY_TILE_ENTITY_MAP.values()) {
            allTileEntityIds.addAll(blockIdSet);
            for (int blockId: blockIdSet) {
                if (! Block.BLOCKS[blockId].tileEntity) {
                    throw new AssertionError("Block " + blockId + " not marked as tile entity!");
                }
            }
        }
        for (Block block: Block.BLOCKS) {
            if (block.tileEntity && (! allTileEntityIds.contains(block.id))) {
                throw new AssertionError("Block " + block.id + " marked as tile entity but not present in tile entity map!");
            }
        }
    }

    public static void main(String[] args) {
        System.out.println("All checks passed");
    }

    private static final Logger logger = LoggerFactory.getLogger(JavaChunkStore.class);
}