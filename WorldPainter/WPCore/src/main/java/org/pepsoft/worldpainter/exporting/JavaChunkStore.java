package org.pepsoft.worldpainter.exporting;

import org.jnbt.CompoundTag;
import org.jnbt.NBTInputStream;
import org.jnbt.NBTOutputStream;
import org.pepsoft.minecraft.*;
import org.pepsoft.worldpainter.DefaultPlugin;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.Platform;
import org.pepsoft.worldpainter.layers.ReadOnly;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.vecmath.Point3i;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import static org.pepsoft.minecraft.Block.BLOCK_TYPE_NAMES;

/**
 * Created by Pepijn on 15-12-2016.
 */
public class JavaChunkStore implements ChunkStore {
    public JavaChunkStore(Platform platform, File regionDir, boolean honourReadOnlyChunks, Dimension dimension, int maxHeight) {
        this.platform = platform;
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
        // Check that all tile entities for which the chunk contains data are
        // actually there
        for (Iterator<TileEntity> i = chunk.getTileEntities().iterator(); i.hasNext(); ) {
            final TileEntity tileEntity = i.next();
            final Set<Integer> blockIds = Constants.TILE_ENTITY_MAP.get(tileEntity.getId());
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
                    return platform.equals(DefaultPlugin.JAVA_MCREGION) ? new ChunkImpl(tag, maxHeight, readOnly) : new ChunkImpl2(tag, maxHeight, readOnly);
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
            regionFile = openRegionFile(regionCoords);
            if (regionFile != null) {
                regionFiles.put(regionCoords, regionFile);
            }
        }
        return regionFile;
    }

    private RegionFile getOrCreateRegionFile(Point regionCoords) throws IOException {
        RegionFile regionFile = regionFiles.get(regionCoords);
        if (regionFile == null) {
            regionFile = openOrCreateRegionFile(regionCoords);
            regionFiles.put(regionCoords, regionFile);
        }
        return regionFile;
    }

    private RegionFile openRegionFile(Point regionCoords) throws IOException {
        File file = new File(regionDir, "r." + regionCoords.x + "." + regionCoords.y + (platform.equals(DefaultPlugin.JAVA_MCREGION) ? ".mcr" : ".mca"));
        return file.exists() ? new RegionFile(file) : null;
    }

    private RegionFile openOrCreateRegionFile(Point regionCoords) throws IOException {
        File file = new File(regionDir, "r." + regionCoords.x + "." + regionCoords.y + (platform.equals(DefaultPlugin.JAVA_MCREGION) ? ".mcr" : ".mca"));
        return new RegionFile(file);
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
    private final File regionDir;
    private final Map<Point, RegionFile> regionFiles = new HashMap<>();
    private final boolean honourReadOnlyChunks;
    private final Dimension dimension;
    private final int maxHeight;

    private static final Logger logger = LoggerFactory.getLogger(JavaChunkStore.class);
}