/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.exporting;

import java.awt.Point;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jnbt.CompoundTag;
import org.jnbt.NBTInputStream;
import org.jnbt.NBTOutputStream;
import org.jnbt.Tag;
import org.pepsoft.minecraft.Chunk;
import org.pepsoft.minecraft.ChunkImpl;
import org.pepsoft.minecraft.ChunkImpl2;
import org.pepsoft.minecraft.Entity;
import org.pepsoft.minecraft.Material;
import org.pepsoft.minecraft.RegionFile;
import org.pepsoft.minecraft.RegionFileCache;
import org.pepsoft.minecraft.TileEntity;

import static org.pepsoft.minecraft.Constants.SUPPORTED_VERSION_1;
import static org.pepsoft.minecraft.Constants.SUPPORTED_VERSION_2;

//import java.util.ArrayDeque;
//import java.util.Deque;

/**
 *
 * @author pepijn
 */
public class ChunkCache {
    public ChunkCache(File dir, int maxHeight, int version) {
        if ((version != SUPPORTED_VERSION_1) && (version != SUPPORTED_VERSION_2)) {
            throw new IllegalArgumentException("Not a supported version: 0x" + Integer.toHexString(version));
        }
        this.dir = dir;
        this.maxHeight = maxHeight;
        this.version = version;
        long maxMemory = Runtime.getRuntime().maxMemory();
        minimumFreeMemory = Math.min(maxMemory / 4, 256 * 1024 * 1024);
//        System.out.println("Creating cache; minimum free memory: " + minimumFreeMemory + " bytes");
    }
    
    public boolean chunkExists(Point coords) throws IOException {
        Chunk cachedChunk = cache.get(coords);
        if (cachedChunk == NON_EXISTANT_CHUNK) {
            return false;
        } else if (cachedChunk != null) {
            return true;
        } else {
            RegionFile regionFile = RegionFileCache.getRegionFile(dir, coords.x, coords.y, version);
            openRegionFiles.add(regionFile);
            return regionFile.containsChunk(coords.x & 31, coords.y & 31);
        }
    }
    
    public Chunk getChunk(Point coords) throws IOException {
//        System.out.println("Getting chunk " + coords.x + "," + coords.y);
        Chunk chunk = cache.get(coords);
        if (chunk == null) {
            chunk = loadChunk(coords);
            if (chunk == null) {
//                System.out.println("Chunk " + coords.x + "," + coords.y + " does not exist");
                cache.put(coords, NON_EXISTANT_CHUNK);
                return null;
            }
            cache.put(coords, chunk);
//        } else {
//            System.out.println("Cache hit for chunk " + coords.x + "," + coords.y);
        } else if (chunk == NON_EXISTANT_CHUNK) {
            chunk = null;
        } else {
//            mruChunks.remove(chunk);
//            mruChunks.addFirst(chunk);
            maintainCache();
        }
        return chunk;
    }
    
    public Chunk getChunkForEditing(Point coords) throws IOException {
//        System.out.println("Getting chunk " + coords.x + "," + coords.y + " for editing");
        Chunk chunk = getChunk(coords);
        if (chunk != null) {
            dirtyChunks.add(chunk);
        }
        return chunk;
    }
    
    /**
     * Save all dirty chunks and then clear the cache and release all resources.
     */
    public void flush() throws IOException {
//        System.out.println("Flushing cache");
        for (Chunk dirtyChunk: dirtyChunks) {
            saveChunk(dirtyChunk);
        }
        dirtyChunks.clear();
        flushRegionFiles();
        cache.clear();
//        mruChunks.clear();
    }
    
    private Chunk loadChunk(Point coords) throws IOException {
        int x = coords.x, z = coords.y;
//        System.out.println("Loading chunk " + x + "," + z);
        RegionFile regionFile = RegionFileCache.getRegionFile(dir, x, z, version);
        openRegionFiles.add(regionFile);
        DataInputStream dataIn = regionFile.getChunkDataInputStream(x & 0x1F, z & 0x1F);
        if (dataIn == null) {
            return null;
        }
        try {
            NBTInputStream in = new NBTInputStream(dataIn);
            CompoundTag tag = (CompoundTag) in.readTag();
            return (version == SUPPORTED_VERSION_1) ? new ChunkImpl(tag, maxHeight) : new ChunkImpl2(tag, maxHeight);
        } finally {
            dataIn.close();
        }
    }
    
    private void saveChunk(Chunk chunk) throws IOException {
        int x = chunk.getxPos(), z = chunk.getzPos();
//        System.out.println("Saving chunk " + x + "," + z);
        RegionFile regionFile = RegionFileCache.getRegionFile(dir, x, z, version);
        openRegionFiles.add(regionFile);
        NBTOutputStream out = new NBTOutputStream(regionFile.getChunkDataOutputStream(x & 0x1F, z & 0x1F));
        try {
            out.writeTag(chunk.toNBT());
        } finally {
            out.close();
        }
    }
    
    private void maintainCache() throws IOException {
        while ((! cache.isEmpty()) && (getUnusedMemory() < minimumFreeMemory)) {
//            Chunk lruChunk = mruChunks.removeLast();
//            System.out.println("Removing chunk " + lruChunk.getxPos() + "," + lruChunk.getzPos());
//            if (dirtyChunks.contains(lruChunk)) {
//                saveChunk(lruChunk);
//                dirtyChunks.remove(lruChunk);
//            }
//            cache.remove(new Point(lruChunk.getxPos(), lruChunk.getzPos()));
        }
    }
    
    private void flushRegionFiles() throws IOException {
        for (RegionFile regionFile: openRegionFiles) {
//            System.out.println("Closing region file");
            regionFile.close();
        }
        openRegionFiles.clear();
        RegionFileCache.clear();
    }
    
    private long getUnusedMemory() {
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory();
        long freeMemory = runtime.freeMemory();
        long totalMemory = runtime.totalMemory();
        long memoryInUse = totalMemory - freeMemory;
        return maxMemory - memoryInUse;
    }
    
    private final File dir;
    private final int maxHeight, version;
    private final long minimumFreeMemory;
    private final Map<Point, Chunk> cache = new HashMap<Point, Chunk>();
    private final Set<Chunk> dirtyChunks = new HashSet<Chunk>();
//    private final Deque<Chunk> mruChunks = new ArrayDeque<Chunk>();
    private final Set<RegionFile> openRegionFiles = new HashSet<RegionFile>();
    
    private static final Chunk NON_EXISTANT_CHUNK = new Chunk() {
        @Override public int getBlockLightLevel(int x, int y, int z) {return 0;}
        @Override public void setBlockLightLevel(int x, int y, int z, int blockLightLevel) {}
        @Override public int getBlockType(int x, int y, int z) {return 0;}
        @Override public void setBlockType(int x, int y, int z, int blockType) {}
        @Override public int getDataValue(int x, int y, int z) {return 0;}
        @Override public void setDataValue(int x, int y, int z, int dataValue) {}
        @Override public int getHeight(int x, int z) {return 0;}
        @Override public void setHeight(int x, int z, int height) {}
        @Override public int getSkyLightLevel(int x, int y, int z) {return 0;}
        @Override public void setSkyLightLevel(int x, int y, int z, int skyLightLevel) {}
        @Override public int getxPos() {return 0;}
        @Override public int getzPos() {return 0;}
        @Override public Point getCoords() {return null;}
        @Override public boolean isTerrainPopulated() {return false;}
        @Override public Material getMaterial(int x, int y, int z) {return null;}
        @Override public void setMaterial(int x, int y, int z, Material material) {}
        @Override public List<Entity> getEntities() {return null;}
        @Override public List<TileEntity> getTileEntities() {return null;}
        @Override public int getMaxHeight() {return 0;}
        @Override public Tag toNBT() {return null;}
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
    };
}