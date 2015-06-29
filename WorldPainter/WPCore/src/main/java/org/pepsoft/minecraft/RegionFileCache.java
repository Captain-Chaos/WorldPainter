package org.pepsoft.minecraft;

/*
 ** 2011 January 5
 **
 ** The author disclaims copyright to this source code.  In place of
 ** a legal notice, here is a blessing:
 **
 **    May you do good and not evil.
 **    May you find forgiveness for yourself and forgive others.
 **    May you share freely, never taking more than you give.
 */

/*
 * 2011 February 16
 *
 * This source code is based on the work of Scaevolus (see notice above).
 * It has been slightly modified by Mojang AB to limit the maximum cache
 * size (relevant to extremely big worlds on Linux systems with limited
 * number of file handles). The region files are postfixed with ".mcr"
 * (Minecraft region file) instead of ".data" to differentiate from the
 * original McRegion files.
 *
 */

// A simple cache and wrapper for efficiently multiple RegionFiles simultaneously.

import java.io.*;
import java.lang.ref.*;
import java.util.*;
import static org.pepsoft.minecraft.Constants.*;

public final class RegionFileCache {

    private static final int MAX_CACHE_SIZE = 256;


    private static final Map<File, Reference<RegionFile>> cache = new HashMap<>();

    private RegionFileCache() {
    }

    public static synchronized RegionFile getRegionFileIfExists(File basePath, int chunkX, int chunkZ, int version) throws IOException {
        if ((version != SUPPORTED_VERSION_1) && (version != SUPPORTED_VERSION_2)) {
            throw new IllegalArgumentException("Not a supported version: 0x" + Integer.toHexString(version));
        }
        
        File regionDir = new File(basePath, "region");
        if (!regionDir.exists()) {
            return null;
        }
        File file = new File(regionDir, "r." + (chunkX >> 5) + "." + (chunkZ >> 5) + ((version == SUPPORTED_VERSION_1) ? ".mcr" : ".mca"));

        Reference<RegionFile> ref = cache.get(file);

        RegionFile regionFile = (ref != null) ? ref.get() : null;
        if (regionFile != null) {
            return regionFile;
        }
        
        if (! file.isFile()) {
            return null;
        }

        if (cache.size() >= MAX_CACHE_SIZE) {
            RegionFileCache.clear();
        }

        regionFile = new RegionFile(file);
        cache.put(file, new SoftReference<>(regionFile));
        return regionFile;
    }
    
    public static synchronized RegionFile getRegionFile(File basePath, int chunkX, int chunkZ, int version) throws IOException {
        if ((version != SUPPORTED_VERSION_1) && (version != SUPPORTED_VERSION_2)) {
            throw new IllegalArgumentException("Not a supported version: 0x" + Integer.toHexString(version));
        }
        
        File regionDir = new File(basePath, "region");
        File file = new File(regionDir, "r." + (chunkX >> 5) + "." + (chunkZ >> 5) + ((version == SUPPORTED_VERSION_1) ? ".mcr" : ".mca"));

        Reference<RegionFile> ref = cache.get(file);

        RegionFile regionFile = (ref != null) ? ref.get() : null;
        if (regionFile != null) {
            return regionFile;
        }

        if (!regionDir.exists()) {
            regionDir.mkdirs();
        }

        if (cache.size() >= MAX_CACHE_SIZE) {
            RegionFileCache.clear();
        }

        regionFile = new RegionFile(file);
        cache.put(file, new SoftReference<>(regionFile));
        return regionFile;
    }

    public static synchronized void clear() throws IOException {
        for (Reference<RegionFile> ref : cache.values()) {
            if (ref.get() != null) {
                ref.get().close();
            }
        }
        cache.clear();
    }

    public static int getSizeDelta(File basePath, int chunkX, int chunkZ, int version) throws IOException {
        RegionFile r = getRegionFile(basePath, chunkX, chunkZ, version);
        return r.getSizeDelta();
    }

    public static DataInputStream getChunkDataInputStream(File basePath, int chunkX, int chunkZ, int version) throws IOException {
        RegionFile r = getRegionFile(basePath, chunkX, chunkZ, version);
        return r.getChunkDataInputStream(chunkX & 31, chunkZ & 31);
    }

    public static DataOutputStream getChunkDataOutputStream(File basePath, int chunkX, int chunkZ, int version) throws IOException {
        RegionFile r = getRegionFile(basePath, chunkX, chunkZ, version);
        return r.getChunkDataOutputStream(chunkX & 31, chunkZ & 31);
    }
}