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

import javax.vecmath.Point3i;
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
import static org.pepsoft.minecraft.Block.BLOCK_TYPE_NAMES;
import static org.pepsoft.minecraft.Constants.*;
import static org.pepsoft.minecraft.DataType.REGION;
import static org.pepsoft.worldpainter.DefaultPlugin.*;
import static org.pepsoft.worldpainter.Platform.Capability.NAME_BASED;
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
        // Do some sanity checks first
        if (! platform.capabilities.contains(NAME_BASED)) {
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
        } else {
            // TODOMC13: similar sanity checks
        }

        final int x = chunk.getxPos(), z = chunk.getzPos();
        final Map<DataType, ? extends Tag> tags = ((NBTItem) chunk).toMultipleNBT();
        tags.forEach((type, tag) -> {
            try {
                final RegionFile regionFile = getOrCreateRegionFile(new Point(x >> 5, z >> 5), type);
                try (NBTOutputStream out = new NBTOutputStream(regionFile.getChunkDataOutputStream(x & 31, z & 31))) {
                    out.writeTag(tag);
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

    private static final Map<String, Set<Integer>> LEGACY_TILE_ENTITY_MAP = new HashMap<>();

    static {
        LEGACY_TILE_ENTITY_MAP.put(LEGACY_ID_AIRPORTAL, Collections.singleton(BLK_END_PORTAL));
        LEGACY_TILE_ENTITY_MAP.put(LEGACY_ID_BEACON, Collections.singleton(BLK_BEACON));
        LEGACY_TILE_ENTITY_MAP.put(LEGACY_ID_CAULDRON, Collections.singleton(BLK_BREWING_STAND));
        LEGACY_TILE_ENTITY_MAP.put(LEGACY_ID_CHEST, ImmutableSet.of(BLK_CHEST, BLK_TRAPPED_CHEST));
        LEGACY_TILE_ENTITY_MAP.put(LEGACY_ID_COMPARATOR, ImmutableSet.of(BLK_REDSTONE_COMPARATOR_UNPOWERED, BLK_REDSTONE_COMPARATOR_POWERED));
        LEGACY_TILE_ENTITY_MAP.put(LEGACY_ID_CONTROL, ImmutableSet.of(BLK_COMMAND_BLOCK, BLK_CHAIN_COMMAND_BLOCK, BLK_REPEATING_COMMAND_BLOCK));
        LEGACY_TILE_ENTITY_MAP.put(LEGACY_ID_DLDETECTOR, ImmutableSet.of(BLK_DAYLIGHT_SENSOR, BLK_DAYLIGHT_SENSOR_INVERTED));
        LEGACY_TILE_ENTITY_MAP.put(LEGACY_ID_DROPPER, Collections.singleton(BLK_DROPPER));
        LEGACY_TILE_ENTITY_MAP.put(LEGACY_ID_ENCHANTTABLE, Collections.singleton(BLK_ENCHANTMENT_TABLE));
        LEGACY_TILE_ENTITY_MAP.put(LEGACY_ID_ENDERCHEST, Collections.singleton(BLK_ENDER_CHEST));
        LEGACY_TILE_ENTITY_MAP.put(LEGACY_ID_FLOWERPOT, Collections.singleton(BLK_FLOWER_POT));
        LEGACY_TILE_ENTITY_MAP.put(LEGACY_ID_FURNACE, ImmutableSet.of(BLK_FURNACE, BLK_BURNING_FURNACE));
        LEGACY_TILE_ENTITY_MAP.put(LEGACY_ID_HOPPER, Collections.singleton(BLK_HOPPER));
        LEGACY_TILE_ENTITY_MAP.put(LEGACY_ID_MOBSPAWNER, Collections.singleton(BLK_MONSTER_SPAWNER));
        LEGACY_TILE_ENTITY_MAP.put(LEGACY_ID_MUSIC, Collections.singleton(BLK_NOTE_BLOCK));
        LEGACY_TILE_ENTITY_MAP.put(LEGACY_ID_PISTON, Collections.singleton(BLK_PISTON_HEAD));
        LEGACY_TILE_ENTITY_MAP.put(LEGACY_ID_RECORDPLAYER, Collections.singleton(BLK_JUKEBOX));
        LEGACY_TILE_ENTITY_MAP.put(LEGACY_ID_SIGN, ImmutableSet.of(BLK_SIGN, BLK_WALL_SIGN));
        LEGACY_TILE_ENTITY_MAP.put(LEGACY_ID_SKULL, Collections.singleton(BLK_HEAD));
        LEGACY_TILE_ENTITY_MAP.put(LEGACY_ID_TRAP, Collections.singleton(BLK_DISPENSER));
        LEGACY_TILE_ENTITY_MAP.put(LEGACY_ID_BANNER, ImmutableSet.of(BLK_STANDING_BANNER, BLK_WALL_BANNER));
        LEGACY_TILE_ENTITY_MAP.put(LEGACY_ID_STRUCTURE, Collections.singleton(BLK_STRUCTURE_BLOCK));
        LEGACY_TILE_ENTITY_MAP.put(LEGACY_ID_SHULKER_BOX, ImmutableSet.of(BLK_WHITE_SHULKER_BOX, BLK_ORANGE_SHULKER_BOX, BLK_MAGENTA_SHULKER_BOX, BLK_LIGHT_BLUE_SHULKER_BOX, BLK_YELLOW_SHULKER_BOX, BLK_LIME_SHULKER_BOX, BLK_PINK_SHULKER_BOX, BLK_GREY_SHULKER_BOX, BLK_LIGHT_GREY_SHULKER_BOX, BLK_CYAN_SHULKER_BOX, BLK_PURPLE_SHULKER_BOX, BLK_BLUE_SHULKER_BOX, BLK_BROWN_SHULKER_BOX, BLK_GREEN_SHULKER_BOX, BLK_RED_SHULKER_BOX, BLK_BLACK_SHULKER_BOX));

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