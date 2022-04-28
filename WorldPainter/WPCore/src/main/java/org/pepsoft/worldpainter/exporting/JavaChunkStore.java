package org.pepsoft.worldpainter.exporting;

import com.google.common.collect.ImmutableSet;
import org.jnbt.NBTInputStream;
import org.jnbt.NBTOutputStream;
import org.jnbt.Tag;
import org.pepsoft.minecraft.*;
import org.pepsoft.util.mdc.MDCThreadPoolExecutor;
import org.pepsoft.worldpainter.Platform;
import org.pepsoft.worldpainter.exception.WPRuntimeException;
import org.pepsoft.worldpainter.platforms.JavaPlatformProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Collections.singleton;
import static java.util.Collections.synchronizedSet;
import static java.util.Objects.requireNonNull;
import static org.pepsoft.minecraft.DataType.REGION;
import static org.pepsoft.worldpainter.DefaultPlugin.*;
import static org.pepsoft.worldpainter.util.ThreadUtils.chooseThreadCount;

/**
 * Created by Pepijn on 15-12-2016.
 */
public class JavaChunkStore implements ChunkStore {
    public JavaChunkStore(JavaPlatformProvider platformProvider, File regionDir, int maxHeight) {
        this.platformProvider = platformProvider;
        this.regionDir = regionDir;
        this.maxHeight = maxHeight;
        platform = platformProvider.getPlatform();
        if (! ((platform == JAVA_MCREGION) || (platform == JAVA_ANVIL) || (platform == JAVA_ANVIL_1_15) || (platform == JAVA_ANVIL_1_17) || (platform == JAVA_ANVIL_1_18))) {
            throw new IllegalArgumentException("Unsupported platform " + platform);
        }
        dataTypes = ImmutableSet.copyOf(platformProvider.getDataTypes());
    }

    @Override
    public int getChunkCount() {
        final AtomicInteger chunkCount = new AtomicInteger();
        try {
            visitRegions(regions -> {
                chunkCount.addAndGet(regions.get(REGION).getChunkCount());
                return true;
            }, true, "counting chunks", singleton(REGION));
        } catch (IOException e) {
            throw new RuntimeException("I/O error while visiting regions of " + regionDir, e);
        }
        return chunkCount.get();
    }

    @Override
    public Set<MinecraftCoords> getChunkCoords() {
        Set<MinecraftCoords> coords = synchronizedSet(new HashSet<>());
        try {
            visitRegions(regions -> {
                final RegionFile regionFile = regions.get(REGION);
                for (int x = 0; x < 32; x++) {
                    for (int z = 0; z < 32; z++) {
                        if (regionFile.containsChunk(x, z)) {
                            coords.add(new MinecraftCoords(regionFile.getX() * 32 + x, regionFile.getZ() * 32 + z));
                        }
                    }
                }
                return true;
            }, true, "collecting chunk coordinates", singleton(REGION));
        } catch (IOException e) {
            throw new RuntimeException("I/O error while visiting regions of " + regionDir, e);
        }
        return coords;
    }

    @Override
    public boolean visitChunks(ChunkVisitor visitor) {
        return visitChunks(visitor, true, "visiting chunks", dataTypes);
    }

    @Override
    public boolean visitChunksForEditing(ChunkVisitor visitor) {
        return visitChunks(visitor, false, "modifying chunks", dataTypes);
    }

    @Override
    public void saveChunk(Chunk chunk) {
        final int x = chunk.getxPos(), z = chunk.getzPos();
        final Map<DataType, ? extends Tag> tags = ((NBTItem) chunk).toMultipleNBT();
        platformProvider.getDataTypes().forEach(type -> {
            try {
                if (tags.containsKey(type)) {
                    final RegionFile regionFile = getOrCreateRegionFile(new Point(x >> 5, z >> 5), type);
                    try (NBTOutputStream out = new NBTOutputStream(regionFile.getChunkDataOutputStream(x & 31, z & 31))) {
                        out.writeTag(tags.get(type));
                    }
                } else {
                    final RegionFile regionFile = getRegionFile(new Point(x >> 5, z >> 5), type);
                    if ((regionFile != null) && regionFile.containsChunk(x & 31, z & 31)) {
                        regionFile.delete(x & 31, z & 31);
                    }
                }
            } catch (IOException e) {
                throw new WPRuntimeException("I/O error saving chunk @" + x + "," + z + " to region of type " + type, e);
            }
        });
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
    @Override
    public void flush() {
        synchronized (regionFiles) {
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
        }
    }

    @Override
    public boolean isChunkPresent(int x, int z) {
        return false;
    }

    /**
     * Load a chunk. Returns {@code null} if the chunk does not exist in the map.
     *
     * @param x The X coordinate in the Minecraft coordinate system of the chunk to load.
     * @param z The Z coordinate in the Minecraft coordinate system of the chunk to load.
     * @return The specified chunk, or {@code null} if the chunk does not exist in the map.
     */
    @Override
    public Chunk getChunk(int x, int z) {
        try {
            final Map<DataType, Tag> tags = new HashMap<>();
            for (DataType type: dataTypes) {
                final RegionFile regionFile = getRegionFile(new Point(x >> 5, z >> 5), type);
                if (regionFile == null) {
                    continue;
                }
                final InputStream chunkIn = regionFile.getChunkDataInputStream(x & 31, z & 31);
                if (chunkIn != null) {
                    try (NBTInputStream in = new NBTInputStream(chunkIn)) {
                        tags.put(type, in.readTag());
                    }
                }
            }
            if (! tags.containsKey(REGION)) {
                return null;
            }
            return platformProvider.createChunk(platform, tags, maxHeight, false);
        } catch (IOException e) {
            throw new RuntimeException("I/O error loading chunk", e);
        }
    }

    /**
     * Load a chunk. Returns {@code null} if the chunk does not exist in the map.
     *
     * @param x The X coordinate in the Minecraft coordinate system of the chunk to load.
     * @param z The Z coordinate in the Minecraft coordinate system of the chunk to load.
     * @return The specified chunk, or {@code null} if the chunk does not exist in the map.
     */
    @Override
    public Chunk getChunkForEditing(int x, int z) {
        return getChunk(x, z);
    }

    @Override
    public void close() {
        flush();
    }

    private RegionFile getRegionFile(Point regionCoords, DataType type) throws IOException {
        synchronized (regionFiles) {
            final RegionKey key = new RegionKey(type, regionCoords);
            RegionFile regionFile = regionFiles.get(key);
            if (regionFile == null) {
                regionFile = platformProvider.getRegionFileIfExists(platform, regionDir, type, regionCoords, false);
                if (regionFile != null) {
                    regionFiles.put(key, regionFile);
                }
            }
            return regionFile;
        }
    }

    private RegionFile getOrCreateRegionFile(Point regionCoords, DataType type) throws IOException {
        synchronized (regionFiles) {
            final RegionKey key = new RegionKey(type, regionCoords);
            RegionFile regionFile = regionFiles.get(key);
            if (regionFile == null) {
                regionFile = platformProvider.getRegionFile(platform, regionDir, type, regionCoords, false);
                regionFiles.put(key, regionFile);
            }
            return regionFile;
        }
    }

    @FunctionalInterface interface RegionVisitor {
        /**
         * Visit a Minecraft region file.
         *
         * <p>For convenience, the visitor may throw checked exceptions. They will be wrapped in a
         * {@link WPRuntimeException} if this happens.
         *
         * @param regions The region file(s) to be visited.
         * @return {@code true} if more chunks should be visited, or {@code false} if no more chunks need to be visited.
         */
        boolean visitRegion(Map<DataType, RegionFile> regions) throws Exception;
    }

    private boolean visitRegions(RegionVisitor visitor, boolean readOnly, String operation, Set<DataType> dataTypes) throws IOException {
        flush();

        final Pattern regionFilePattern = (platform == JAVA_MCREGION)
                ? Pattern.compile("r\\.(-?\\d+)\\.(-?\\d+)\\.mcr")
                : Pattern.compile("r\\.(-?\\d+)\\.(-?\\d+)\\.mca");
        final File[] files = requireNonNull(regionDir.listFiles((dir, name) -> {
            if (! regionFilePattern.matcher(name).matches()) {
                return false;
            } else if (new File(dir, name).length() == 0) {
                // TODO should we be skipping empty files when readOnly is false?
                if (! readOnly) {
                    logger.warn("Skipping empty region file {}", name);
                }
                return false;
            }
            return true;
        }));

        long start = System.currentTimeMillis();
        try {
            if (readOnly && (files.length > 1) && (! "1".equals(System.getProperty("org.pepsoft.worldpainter.threads")))) {
                return visitRegionsInParallel(files, regionFilePattern, visitor, operation);
            }

            for (File file: requireNonNull(files)) {
                final Matcher matcher = regionFilePattern.matcher(file.getName());
                if (matcher.matches()) {
                    final Map<DataType, RegionFile> regionFilesToVisit = new HashMap<>();
                    final Point coords = new Point(Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2)));
                    for (DataType type: dataTypes) {
                        final RegionKey key = new RegionKey(type, coords);
                        final RegionFile regionFile;
                        if (regionFiles.containsKey(key)) {
                            regionFilesToVisit.put(type, regionFiles.get(key));
                        } else {
                            regionFile = (type == REGION) ? new RegionFile(file, readOnly) : platformProvider.getRegionFile(platform, regionDir, type, coords, readOnly);
                            if (regionFile != null) {
                                regionFiles.put(key, regionFile);
                                regionFilesToVisit.put(type, regionFile);
                            }
                        }
                    }
                    try {
                        if (! visitor.visitRegion(regionFilesToVisit)) {
                            return false;
                        }
                    } catch (RuntimeException e) {
                        throw e;
                    } catch (Exception e) {
                        throw new WPRuntimeException("Checked exception visiting region file " + file, e);
                    } finally {
                        for (Map.Entry<DataType, RegionFile> entry: regionFilesToVisit.entrySet()) {
                            entry.getValue().close();
                            regionFiles.remove(new RegionKey(entry.getKey(), coords));
                        }
                    }
                }
            }
            return true;
        } finally {
            logger.debug("Visiting {} regions for {} took {} ms", files.length, operation, System.currentTimeMillis() - start);
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored") // We're just after the groups
    private boolean visitRegionsInParallel(File[] files, Pattern regionFilePattern, RegionVisitor visitor, String operation) {
        final int threadCount = chooseThreadCount(operation, files.length);
        final ExecutorService executor = MDCThreadPoolExecutor.newFixedThreadPool(threadCount, new ThreadFactory() {
            @Override
            public synchronized Thread newThread(Runnable r) {
                Thread thread = new Thread(threadGroup, r, "Chunk-Visitor-" + nextID++);
                thread.setPriority(Thread.MIN_PRIORITY);
                return thread;
            }

            private final ThreadGroup threadGroup = new ThreadGroup("Chunk Visitors");
            private int nextID = 1;
        });
        final Throwable[] exception = new Throwable[1];
        final AtomicBoolean cancelled = new AtomicBoolean();
        try {
            for (File file: files) {
                if (file.length() == 0L) {
                    continue;
                }
                executor.execute(() -> {
                    synchronized (exception) {
                        if ((exception[0] != null) || cancelled.get()) {
                            logger.debug("Skipping file {} because of previous exception, or because the visitor previously returned false", file);
                            return;
                        }
                    }
                    try {
                        final Map<DataType, RegionFile> regionFilesToVisit = new HashMap<>();
                        final Matcher matcher = regionFilePattern.matcher(file.getName());
                        matcher.find();
                        final Point coords = new Point(Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2)));
                        synchronized (regionFiles) {
                            for (DataType type: dataTypes) {
                                final RegionKey key = new RegionKey(type, coords);
                                final RegionFile regionFile;
                                if (regionFiles.containsKey(key)) {
                                    regionFilesToVisit.put(type, regionFiles.get(key));
                                } else {
                                    regionFile = (type == REGION) ? new RegionFile(file, true) : platformProvider.getRegionFile(platform, regionDir, type, coords, true);
                                    if (regionFile != null) {
                                        regionFiles.put(key, regionFile);
                                        regionFilesToVisit.put(type, regionFile);
                                    }
                                }
                            }
                        }
                        try {
                            if (! visitor.visitRegion(regionFilesToVisit)) {
                                cancelled.set(true);
                            }
                        } finally {
                            synchronized (regionFiles) {
                                for (Map.Entry<DataType, RegionFile> entry: regionFilesToVisit.entrySet()) {
                                    entry.getValue().close();
                                    regionFiles.remove(new RegionKey(entry.getKey(), coords));
                                }
                            }
                        }
                    } catch (Throwable e) {
                        logger.error(e.getClass().getSimpleName() + " while visiting region file " + file + " (message: " + e.getMessage() + ")", e);
                        synchronized (exception) {
                            if (exception[0] == null) {
                                exception[0] = e;
                            }
                        }
                    }
                });
            }
        } finally {
            executor.shutdown();
            try {
                executor.awaitTermination(366, TimeUnit.DAYS);
            } catch (InterruptedException e) {
                throw new WPRuntimeException("Thread interrupted while waiting for all tasks to finish", e);
            }
        }
        synchronized (exception) {
            if (exception[0] != null) {
                throw new WPRuntimeException(exception[0].getClass().getSimpleName() + " while visiting region files (message: " + exception[0].getMessage() + ")", exception[0]);
            } else {
                return ! cancelled.get();
            }
        }
    }

    private boolean visitChunks(ChunkVisitor visitor, boolean readOnly, String operation, Set<DataType> dataTypes) {
        try {
            return visitRegions(regions -> {
                for (int x = 0; x < 32; x++) {
                    for (int z = 0; z < 32; z++) {
                        boolean exceptionFromChunkVisitor = false;
                        try {
                            if (regions.get(REGION).containsChunk(x, z)) {
                                final Map<DataType, Tag> tags = new HashMap<>();
                                for (Map.Entry<DataType, RegionFile> entry: regions.entrySet()) {
                                    final InputStream chunkIn = entry.getValue().getChunkDataInputStream(x & 31, z & 31);
                                    if (chunkIn != null) {
                                        try (NBTInputStream in = new NBTInputStream(chunkIn)) {
                                            tags.put(entry.getKey(), in.readTag());
                                        }
                                    }
                                }
                                Chunk chunk = platformProvider.createChunk(platform, tags, maxHeight, readOnly);
                                exceptionFromChunkVisitor = true;
                                if (visitor.visitChunk(chunk)) {
                                    if (! readOnly) {
                                        saveChunk(chunk);
                                    }
                                } else {
                                    return false;
                                }
                            }
                        } catch (RuntimeException e) {
                            // If it was the chunk visitor that threw the exception just propagate it; if it was the
                            // loading of the chunk that failed then log it and report it to the visitor
                            if (exceptionFromChunkVisitor) {
                                throw e;
                            } else {
                                logger.error("{} while visiting chunk {},{} in regions {} (message: \"{}\")", e.getClass().getSimpleName(), x, z, regions, e.getMessage(), e);
                                if (! visitor.chunkError(new MinecraftCoords(x, z), e.getClass().getSimpleName() + ": " + e.getMessage())) {
                                    return false;
                                }
                            }
                        } catch (Exception e) {
                            throw new WPRuntimeException("Checked exception visiting chunk " + x + "," + z + " in regions " + regions, e);
                        }
                    }
                }
                return true;
            }, readOnly, operation, dataTypes);
        } catch (IOException e) {
            throw new RuntimeException("I/O error while visiting regions of " + regionDir, e);
        }
    }

    private final Platform platform;
    private final JavaPlatformProvider platformProvider;
    private final File regionDir;
    private final Map<RegionKey, RegionFile> regionFiles = new HashMap<>();
    private final int maxHeight;
    private final Set<DataType> dataTypes;

    public static void main(String[] args) {
        System.out.println("All checks passed");
    }

    private static final Logger logger = LoggerFactory.getLogger(JavaChunkStore.class);

    static class RegionKey {
        RegionKey(DataType type, Point coords) {
            this.type = type;
            this.coords = coords;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            RegionKey regionKey = (RegionKey) o;
            return type == regionKey.type && coords.equals(regionKey.coords);
        }

        @Override
        public int hashCode() {
            return Objects.hash(type, coords);
        }

        private final DataType type;
        private final Point coords;
    }
}