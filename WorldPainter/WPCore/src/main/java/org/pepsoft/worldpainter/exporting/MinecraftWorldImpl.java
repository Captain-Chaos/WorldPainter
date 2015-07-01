/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.worldpainter.exporting;

import org.jnbt.CompoundTag;
import org.jnbt.NBTInputStream;
import org.jnbt.NBTOutputStream;
import org.jnbt.Tag;
import org.pepsoft.minecraft.*;
import org.pepsoft.util.jobqueue.HashList;
import org.pepsoft.worldpainter.layers.ReadOnly;

import javax.vecmath.Point3i;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.pepsoft.minecraft.Block.BLOCK_TYPE_NAMES;
import static org.pepsoft.minecraft.Constants.BLK_AIR;
import static org.pepsoft.minecraft.Constants.SUPPORTED_VERSION_1;
import static org.pepsoft.worldpainter.Constants.*;

import org.pepsoft.worldpainter.Dimension;

/**
 *
 * @author pepijn
 */
public class MinecraftWorldImpl implements MinecraftWorld {
    public MinecraftWorldImpl(File worldDir, int dimension, int maxHeight, int version, boolean readOnly, int cacheSize) {
        this.version = version;
        this.readOnly = readOnly;
        this.maxHeight = maxHeight;
        switch (dimension) {
            case DIM_NORMAL:
                dimensionDir = worldDir;
                break;
            case DIM_NETHER:
                dimensionDir = new File(worldDir, "DIM-1");
                break;
            case DIM_END:
                dimensionDir = new File(worldDir, "DIM1");
                break;
            default:
                throw new IllegalArgumentException("Dimension " + dimension + " not supported");
        }
        if (! worldDir.isDirectory()) {
            throw new IllegalArgumentException(worldDir + " does not exist or is not a directory");
        }
        regionDir = new File(dimensionDir, "region");
        this.cacheSize = cacheSize;
        cache = new HashMap<>(cacheSize);
        lruList = new HashList<>(cacheSize);
        dirtyChunks = new HashSet<>(cacheSize);
        this.dimension = null;
        honourReadOnlyChunks = false;
    }
    
    public MinecraftWorldImpl(File worldDir, Dimension dimension, int version) {
        this(worldDir, dimension, version, false, false, -1);
    }

    public MinecraftWorldImpl(File worldDir, Dimension dimension, int version, boolean readOnly, boolean honourReadOnlyChunks, int cacheSize) {
        this.version = version;
        this.dimension = dimension;
        this.readOnly = readOnly;
        this.honourReadOnlyChunks = honourReadOnlyChunks;
        if ((dimension.getWorld().getDimensionToExport() == dimension.getDim()) && (dimension.getWorld().getTilesToExport() != null)) {
            lowestX = Integer.MAX_VALUE;
            highestX = Integer.MIN_VALUE;
            lowestZ = Integer.MAX_VALUE;
            highestZ = Integer.MIN_VALUE;
            for (Point tile: dimension.getWorld().getTilesToExport()) {
                int chunkX = tile.x << 3;
                int chunkZ = tile.y << 3;
                if (chunkX < lowestX) {
                    lowestX = chunkX;
                }
                if (chunkX + 7 > highestX) {
                    highestX = chunkX + 7;
                }
                if (chunkZ < lowestZ) {
                    lowestZ = chunkZ;
                }
                if (chunkX + 7 > highestZ) {
                    highestZ = chunkZ + 7;
                }
            }
        } else {
            Point northEastChunk = new Point((dimension.getHighestX() + 1) * TILE_SIZE - 1, dimension.getLowestY() *  TILE_SIZE);
            Point southWestChunk = new Point(dimension.getLowestX() * TILE_SIZE, (dimension.getHighestY() + 1) * TILE_SIZE - 1);
            lowestX = southWestChunk.x >> 4;
            highestX = northEastChunk.x >> 4;
            lowestZ = northEastChunk.y >> 4;
            highestZ = southWestChunk.y >> 4;
        }
        maxHeight = dimension.getMaxHeight();
        switch (dimension.getDim()) {
            case DIM_NORMAL:
                dimensionDir = worldDir;
                break;
            case DIM_NETHER:
                dimensionDir = new File(worldDir, "DIM-1");
                break;
            case DIM_END:
                dimensionDir = new File(worldDir, "DIM1");
                break;
            default:
                throw new IllegalArgumentException("Dimension " + dimension.getDim() + " not supported");
        }
        if (! worldDir.isDirectory()) {
            throw new IllegalArgumentException(worldDir + " does not exist or is not a directory");
        }
        regionDir = new File(dimensionDir, "region");
        this.cacheSize = (cacheSize >= 0) ? cacheSize : ((highestX - lowestX + 1 + 2 * dimension.getBorderSize()) * 2 + 50);
        cache = new HashMap<>(cacheSize);
        lruList = new HashList<>(cacheSize);
        dirtyChunks = new HashSet<>(cacheSize);
    }

    public int getHighestX() {
        return highestX;
    }

    public int getHighestZ() {
        return highestZ;
    }

    public int getLowestX() {
        return lowestX;
    }

    public int getLowestZ() {
        return lowestZ;
    }

    @Override
    public int getMaxHeight() {
        return maxHeight;
    }

    @Override
    public int getBlockTypeAt(int x, int y, int height) {
        Chunk chunk = getChunk(x >> 4, y >> 4);
        if (chunk != null) {
            return chunk.getBlockType(x & 0xf, height, y & 0xf);
        } else {
            return BLK_AIR;
        }
    }

    @Override
    public void setBlockTypeAt(int x, int y, int height, int blockType) {
        Chunk chunk = getChunkForEditing(x >> 4, y >> 4);
        if (chunk != null) {
            chunk.setBlockType(x & 0xf, height, y & 0xf, blockType);
        }
    }

    @Override
    public int getDataAt(int x, int y, int height) {
        Chunk chunk = getChunk(x >> 4, y >> 4);
        if (chunk != null) {
            return chunk.getDataValue(x & 0xf, height, y & 0xf);
        } else {
            return 0;
        }
    }

    @Override
    public void setDataAt(int x, int y, int height, int data) {
        Chunk chunk = getChunkForEditing(x >> 4, y >> 4);
        if (chunk != null) {
            chunk.setDataValue(x & 0xf, height, y & 0xf, data);
        }
    }

    @Override
    public Material getMaterialAt(int x, int y, int height) {
        Chunk chunk = getChunk(x >> 4, y >> 4);
        if (chunk != null) {
            return chunk.getMaterial(x & 0xf, height, y & 0xf);
        } else {
            return Material.AIR;
        }
    }

    @Override
    public void setMaterialAt(int x, int y, int height, Material material) {
        Chunk chunk = getChunkForEditing(x >> 4, y >> 4);
        if (chunk != null) {
            chunk.setMaterial(x & 0xf, height, y & 0xf, material);
        }
    }

    @Override
    public int getBlockLightLevel(int x, int y, int height) {
        Chunk chunk = getChunk(x >> 4, y >> 4);
        if (chunk != null) {
            return chunk.getBlockLightLevel(x & 0xf, height, y & 0xf);
        } else {
            return 0;
        }
    }

    @Override
    public void setBlockLightLevel(int x, int y, int height, int blockLightLevel) {
        Chunk chunk = getChunkForEditing(x >> 4, y >> 4);
        if (chunk != null) {
            chunk.setBlockLightLevel(x & 0xf, height, y & 0xf, blockLightLevel);
        }
    }

    @Override
    public int getSkyLightLevel(int x, int y, int height) {
        Chunk chunk = getChunk(x >> 4, y >> 4);
        if (chunk != null) {
            return chunk.getSkyLightLevel(x & 0xf, height, y & 0xf);
        } else {
            return 15;
        }
    }

    @Override
    public void setSkyLightLevel(int x, int y, int height, int skyLightLevel) {
        Chunk chunk = getChunkForEditing(x >> 4, y >> 4);
        if (chunk != null) {
            chunk.setSkyLightLevel(x & 0xf, height, y & 0xf, skyLightLevel);
        }
    }

    @Override
    public boolean isChunkPresent(int x, int z) {
        if ((x == cachedX) && (z == cachedZ)) {
            return true;
        }
        Chunk chunk = cache.get(new Point(x, z));
        if (chunk == null) {
            try {
                RegionFile regionFile = getRegionFile(new Point(x >> 5, z >> 5));
                return (regionFile != null) && regionFile.containsChunk(x & 31, z & 31);
            } catch (IOException e) {
                throw new RuntimeException("I/O error while trying to determine existence of chunk " + x + "," + z, e);
            }
        } else {
            return chunk != NON_EXISTANT_CHUNK;
        }
    }

    @Override
    public synchronized void addChunk(Chunk chunk) {
        if (readOnly) {
            throw new IllegalStateException("Read only");
        }
        if (chunkExists(chunk.getCoords())) {
            throw new IllegalStateException("Existing chunk at " + chunk.getxPos() + ", " + chunk.getzPos());
        }
        maintainCache();
        int chunkX = chunk.getxPos(), chunkZ = chunk.getzPos();
        Point coords = new Point(chunkX, chunkZ);
        cache.put(coords, chunk);
        dirtyChunks.add(coords);
        lruList.addToEnd(coords);
        if ((chunkX == cachedX) && (chunkZ == cachedZ)) {
            cachedChunk = chunk;
            cachedForEditing = true;
        }
        if (chunkX < lowestX) {
            lowestX = chunkX;
        }
        if (chunkX > highestX) {
            highestX = chunkX;
        }
        if (chunkZ < lowestZ) {
            lowestZ = chunkZ;
        }
        if (chunkZ > highestZ) {
            highestZ = chunkZ;
        }
    }

    @Override
    public int getHighestNonAirBlock(int x, int y) {
        Chunk chunk = getChunk(x >> 4, y >> 4);
        if (chunk != null) {
            return chunk.getHighestNonAirBlock(x & 0xf, y & 0xf);
        } else {
            return -1;
        }
    }

    public synchronized void replaceChunk(Chunk chunk) {
        if (readOnly) {
            throw new IllegalStateException("Read only");
        }
        if (! chunkExists(chunk.getCoords())) {
            throw new IllegalStateException("No existing chunk at " + chunk.getxPos() + ", " + chunk.getzPos());
        }
        maintainCache();
        int chunkX = chunk.getxPos(), chunkZ = chunk.getzPos();
        Point coords = new Point(chunkX, chunkZ);
        cache.put(coords, chunk);
        dirtyChunks.add(coords);
        lruList.addToEnd(coords);
        if ((chunkX == cachedX) && (chunkZ == cachedZ)) {
            cachedChunk = chunk;
            cachedForEditing = true;
        }
    }

    public boolean chunkExists(int x, int z) {
        return chunkExists(new Point(x, z));
    }

    public synchronized boolean chunkExists(Point coords) {
        if ((coords.x == cachedX) && (coords.y == cachedZ)) {
            return cachedChunk != null;
        }
        Chunk chunkFromCache = cache.get(coords);
        if (chunkFromCache == NON_EXISTANT_CHUNK) {
            return false;
        } else if (chunkFromCache != null) {
            return true;
        }
        try {
            int regionX = coords.x >> 5;
            int regionZ = coords.y >> 5;
            Point regionCoords = new Point(regionX, regionZ);
            RegionFile regionFile = getRegionFile(regionCoords);
            if (regionFile != null) {
                return regionFile.containsChunk(coords.x & 31, coords.y & 31);
            } else {
                return false;
            }
        } catch (IOException e) {
            throw new RuntimeException("I/O error while determining existance of chunk " + coords.x + ", " + coords.y, e);
        }
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
    
    private RegionFile openRegionFile(Point regionCoords) throws IOException {
        File file = new File(regionDir, "r." + regionCoords.x + "." + regionCoords.y + ((version == SUPPORTED_VERSION_1) ? ".mcr" : ".mca"));
        return file.exists() ? new RegionFile(file) : null;
    }

    @Override
    public synchronized Chunk getChunk(int x, int z) {
        if ((x == cachedX) && (z == cachedZ)) {
//            fastCacheHits++;
            return cachedChunk;
        }
//        long start = System.currentTimeMillis(), loadingTime = 0;
        cachedX = x;
        cachedZ = z;
        cachedForEditing = false;
        Point coords = new Point(x, z);
        cachedChunk = cache.get(coords);
        if (cachedChunk == null) {
//            loadingTime = System.currentTimeMillis();
            cachedChunk = loadChunk(x, z);
//            loadingTime -= System.currentTimeMillis();
            maintainCache();
            if (cachedChunk != null) {
                cache.put(coords, cachedChunk);
            } else {
                cache.put(coords, NON_EXISTANT_CHUNK);
            }
        } else {
//            cacheHits++;
            if (cachedChunk == NON_EXISTANT_CHUNK) {
                cachedChunk = null;
            }
        }
        lruList.addToEnd(coords);
//        timeSpentGetting += (System.currentTimeMillis() - start) - loadingTime;
        return cachedChunk;
    }

    @Override
    public synchronized Chunk getChunkForEditing(int x, int z) {
        if (readOnly) {
            throw new IllegalStateException("Read only");
        }
        if ((x == cachedX) && (z == cachedZ)) {
            if ((! cachedForEditing) && (cachedChunk != null) && (! cachedChunk.isReadOnly())) {
                dirtyChunks.add(new Point(x, z));
                cachedForEditing = true;
            }
//            fastCacheHits++;
            return cachedChunk;
        }
        Point coords = new Point(x, z);
        cachedChunk = getChunk(x, z);
        if ((cachedChunk != null) && (! cachedChunk.isReadOnly())) {
            dirtyChunks.add(coords);
        }
        cachedForEditing = true;
        return cachedChunk;
    }

    public void saveDirtyChunks() {
        for (Point coords: dirtyChunks) {
            saveChunk(cache.get(coords));
        }
        dirtyChunks.clear();
        cachedForEditing = false;
    }

    /**
     * Saves all dirty chunks and closes all files. Ensures that all changes are
     * saved and no system resources are being used, but the objects can still
     * be used; any subsequent operations will open files as needed again.
     */
    public synchronized void flush() {
        saveDirtyChunks();
        try {
            if (logger.isLoggable(Level.FINE)) {
                logger.fine("Closing " + regionFiles.size() + " region files");
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
    public void addEntity(int x, int y, int height, Entity entity) {
        addEntity(x + 0.5, y + 0.5, height + 1.5, entity);
    }

    @Override
    public void addEntity(double x, double y, double height, Entity entity) {
        if (readOnly) {
            throw new IllegalStateException("Read only");
        }
        Chunk chunk = getChunkForEditing(((int) x) >> 4, ((int) y) >> 4);
        if ((chunk != null) && (! chunk.isReadOnly())) {
            Entity clone = (Entity) entity.clone();
            clone.setPos(new double[] {x, height, y});
            chunk.getEntities().add(clone);
        }
    }

    @Override
    public void addTileEntity(int x, int y, int height, TileEntity tileEntity) {
        if (readOnly) {
            throw new IllegalStateException("Read only");
        }
        Chunk chunk = getChunkForEditing(x >> 4, y >> 4);
        if ((chunk != null) && (! chunk.isReadOnly())) {
            TileEntity clone = (TileEntity) tileEntity.clone();
            clone.setX(x);
            clone.setY(height);
            clone.setZ(y);
            chunk.getTileEntities().add(clone);
        }
    }
    
    public int getCacheSize() {
        return cache.size();
    }

    private synchronized void saveChunk(Chunk chunk) {
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
                logger.warning("Unknown tile entity ID \"" + tileEntity.getId() + "\" encountered @ " + tileEntity.getX() + "," + tileEntity.getZ() + "," + tileEntity.getY() + "; can't check whether the corresponding block is there!");
            } else {
                final int existingBlockId = chunk.getBlockType(tileEntity.getX() & 0xf, tileEntity.getY(), tileEntity.getZ() & 0xf);
                if (! blockIds.contains(existingBlockId)) {
                    // The block at the specified location
                    // is not a tile entity, or a different
                    // tile entity. Remove the data
                    i.remove();
                    if (logger.isLoggable(Level.FINE)) {
                        logger.fine("Removing tile entity " + tileEntity.getId() + " @ " + tileEntity.getX() + "," + tileEntity.getZ() + "," + tileEntity.getY() + " because the block at that location is a " + BLOCK_TYPE_NAMES[existingBlockId]);
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
                logger.warning("Removing tile entity " + tileEntity.getId() + " @ " + tileEntity.getX() + "," + tileEntity.getZ() + "," + tileEntity.getY() + " because there is already a tile entity of the same type at that location");
            } else {
                occupiedCoords.add(coords);
            }
        }
        
        try {
            int x = chunk.getxPos(), z = chunk.getzPos();
            RegionFile regionFile = getRegionFile(new Point(x >> 5, z >> 5));
            try (NBTOutputStream out = new NBTOutputStream(regionFile.getChunkDataOutputStream(x & 31, z & 31))) {
                out.writeTag(chunk.toNBT());
            }
        } catch (IOException e) {
            throw new RuntimeException("I/O error saving chunk", e);
        }
//        timeSpentSaving += System.currentTimeMillis() - start;
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
    private Chunk loadChunk(int x, int z) {
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
                    return (version == SUPPORTED_VERSION_1) ? new ChunkImpl(tag, maxHeight, readOnly) : new ChunkImpl2(tag, maxHeight, readOnly);
                }
            } else {
//                timeSpentLoading += System.currentTimeMillis() - start;
                return null;
            }
        } catch (IOException e) {
            throw new RuntimeException("I/O error loading chunk", e);
        }
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
    
    private void maintainCache() {
        while (cache.size() >= cacheSize) {
            Point lruCoords = lruList.remove(0);
            Chunk lruChunk = cache.remove(lruCoords);
            if (dirtyChunks.contains(lruCoords)) {
                saveChunk(lruChunk);
                dirtyChunks.remove(lruCoords);
            }
        }
    }

    private final File dimensionDir, regionDir;
    private final Map<Point, Chunk> cache;
    private final HashList<Point> lruList;
    private final Set<Point> dirtyChunks;
    private final Map<Point, RegionFile> regionFiles = new HashMap<>();
    private final int maxHeight, version, cacheSize;
    private Chunk cachedChunk;
    private int cachedX = Integer.MIN_VALUE, cachedZ = Integer.MIN_VALUE;
    private boolean cachedForEditing;
    private int lowestX, highestX, lowestZ, highestZ;
    private final boolean readOnly, honourReadOnlyChunks;
    private final Dimension dimension;
//    private long lastStatisticsTimestamp;
//    private int chunksLoaded, chunksSaved, fastCacheHits, cacheHits;
//    private long timeSpentLoading, timeSpentSaving, timeSpentGetting;

    private static final Logger logger = Logger.getLogger(MinecraftWorldImpl.class.getName());

    private static final Chunk NON_EXISTANT_CHUNK = new Chunk() {
        @Override public int getBlockLightLevel(int x, int y, int z) {return 0;}
        @Override public int getBlockType(int x, int y, int z) {return 0;}
        @Override public void setBlockType(int x, int y, int z, int blockType) {}
        @Override public int getDataValue(int x, int y, int z) {return 0;}
        @Override public void setDataValue(int x, int y, int z, int dataValue) {}
        @Override public int getHeight(int x, int z) {return 0;}
        @Override public int getSkyLightLevel(int x, int y, int z) {return 0;}
        @Override public int getxPos() {return 0;}
        @Override public int getzPos() {return 0;}
        @Override public boolean isTerrainPopulated() {return false;}
        @Override public Tag toNBT() {return null;}
        @Override public void setBlockLightLevel(int x, int y, int z, int blockLightLevel) {}
        @Override public void setHeight(int x, int z, int height) {}
        @Override public Point getCoords() {return null;}
        @Override public void setSkyLightLevel(int x, int y, int z, int skyLightLevel) {}
        @Override public Material getMaterial(int x, int y, int z) {return null;}
        @Override public void setMaterial(int x, int y, int z, Material material) {}
        @Override public List<Entity> getEntities() {return null;}
        @Override public List<TileEntity> getTileEntities() {return null;}
        @Override public int getMaxHeight() {return 0;}
        @Override public void setTerrainPopulated(boolean terrainPopulated) {}
        @Override public boolean isBiomesAvailable() {return false;}
        @Override public int getBiome(int x, int z) {return 0;}
        @Override public void setBiome(int x, int z, int biome) {}
        @Override public boolean isReadOnly() {return false;}
        @Override public boolean isLightPopulated() {return false;}
        @Override public void setLightPopulated(boolean lightPopulated) {}
        @Override public long getInhabitedTime() {return 0;}
        @Override public void setInhabitedTime(long inhabitedTime) {}
        @Override public int getHighestNonAirBlock(int x, int z) {return 0;}
        @Override public int getHighestNonAirBlock() {return 0;}
    };
}