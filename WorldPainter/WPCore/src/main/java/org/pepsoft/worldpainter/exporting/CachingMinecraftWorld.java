package org.pepsoft.worldpainter.exporting;

import org.pepsoft.minecraft.*;
import org.pepsoft.util.jobqueue.HashList;
import org.pepsoft.worldpainter.Platform;
import org.pepsoft.worldpainter.plugins.PlatformManager;

import java.awt.*;
import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.pepsoft.minecraft.Constants.BLK_AIR;

/**
 * Created by Pepijn on 15-12-2016.
 */
public class CachingMinecraftWorld implements MinecraftWorld {
    public CachingMinecraftWorld(File worldDir, int dimension, int maxHeight, Platform platform, boolean readOnly, int cacheSize) {
        this.maxHeight = maxHeight;
        this.cacheSize = cacheSize;
        this.readOnly = readOnly;
        cache = new HashMap<>(cacheSize);
        lruList = new HashList<>(cacheSize);
        dirtyChunks = new HashSet<>(cacheSize);
        chunkStore = PlatformManager.getInstance().getChunkStore(platform, worldDir, dimension);
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
            return chunkStore.isChunkPresent(x, z);
        } else {
            return chunk != NON_EXISTANT_CHUNK;
        }
    }

//    @Override
    public synchronized void addChunk(Chunk chunk) {
        if (readOnly) {
            throw new IllegalStateException("Read only");
        }
        int chunkX = chunk.getxPos(), chunkZ = chunk.getzPos();
        if (isChunkPresent(chunkX, chunkZ)) {
            throw new IllegalStateException("Existing chunk at " + chunkX + ", " + chunkZ);
        }
        maintainCache();
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

    public synchronized void replaceChunk(Chunk chunk) {
        if (readOnly) {
            throw new IllegalStateException("Read only");
        }
        int chunkX = chunk.getxPos(), chunkZ = chunk.getzPos();
        if (! isChunkPresent(chunkX, chunkZ)) {
            throw new IllegalStateException("No existing chunk at " + chunkX + ", " + chunkZ);
        }
        maintainCache();
        Point coords = new Point(chunkX, chunkZ);
        cache.put(coords, chunk);
        dirtyChunks.add(coords);
        lruList.addToEnd(coords);
        if ((chunkX == cachedX) && (chunkZ == cachedZ)) {
            cachedChunk = chunk;
            cachedForEditing = true;
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
            cachedChunk = chunkStore.getChunk(x, z);
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

    /**
     * Saves all dirty chunks and closes all files. Ensures that all changes are
     * saved and no system resources are being used, but the objects can still
     * be used; any subsequent operations will open files as needed again.
     */
//    @Override
    public synchronized void flush() {
        saveDirtyChunks();
        chunkStore.flush();
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

    @Override
    public void close() {
        flush();
        chunkStore.close();
    }

    public int getCacheSize() {
        return cache.size();
    }

    public void saveDirtyChunks() {
        chunkStore.doInTransaction(() -> {
            for (Point coords : dirtyChunks) {
                chunkStore.saveChunk(cache.get(coords));
            }
        });
        dirtyChunks.clear();
        cachedForEditing = false;
    }

    private void maintainCache() {
        chunkStore.doInTransaction(() -> {
            while (cache.size() >= cacheSize) {
                Point lruCoords = lruList.remove(0);
                Chunk lruChunk = cache.remove(lruCoords);
                if (dirtyChunks.contains(lruCoords)) {
                    chunkStore.saveChunk(lruChunk);
                    dirtyChunks.remove(lruCoords);
                }
            }
        });
    }

    private final Map<Point, Chunk> cache;
    private final HashList<Point> lruList;
    private final Set<Point> dirtyChunks;
    private final int maxHeight, cacheSize;
    private final ChunkStore chunkStore;
    private Chunk cachedChunk;
    private int cachedX = Integer.MIN_VALUE, cachedZ = Integer.MIN_VALUE;
    private boolean cachedForEditing;
    private int lowestX, highestX, lowestZ, highestZ;
    private final boolean readOnly;

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
        @Override public void setBlockLightLevel(int x, int y, int z, int blockLightLevel) {}
        @Override public void setHeight(int x, int z, int height) {}
        @Override public MinecraftCoords getCoords() {return null;}
        @Override public void setSkyLightLevel(int x, int y, int z, int skyLightLevel) {}
        @Override public Material getMaterial(int x, int y, int z) {return null;}
        @Override public void setMaterial(int x, int y, int z, Material material) {}
        @Override public java.util.List<Entity> getEntities() {return null;}
        @Override public java.util.List<TileEntity> getTileEntities() {return null;}
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