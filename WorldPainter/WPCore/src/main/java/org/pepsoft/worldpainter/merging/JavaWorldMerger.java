/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.merging;

import org.jnbt.CompoundTag;
import org.jnbt.NBTInputStream;
import org.jnbt.NBTOutputStream;
import org.jnbt.Tag;
import org.pepsoft.minecraft.*;
import org.pepsoft.util.FileUtils;
import org.pepsoft.util.ParallelProgressManager;
import org.pepsoft.util.ProgressReceiver;
import org.pepsoft.util.SubProgressReceiver;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.*;
import org.pepsoft.worldpainter.exporting.*;
import org.pepsoft.worldpainter.history.HistoryEntry;
import org.pepsoft.worldpainter.layers.*;
import org.pepsoft.worldpainter.plugins.PlatformManager;
import org.pepsoft.worldpainter.util.FileInUseException;
import org.pepsoft.worldpainter.vo.EventVO;

import java.awt.*;
import java.io.*;
import java.util.List;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.pepsoft.minecraft.Constants.*;
import static org.pepsoft.minecraft.Material.*;
import static org.pepsoft.worldpainter.Constants.*;

/**
 *
 * @author pepijn
 */
@SuppressWarnings("StringConcatenationInsideStringBufferAppend") // Readability
public class JavaWorldMerger extends JavaWorldExporter {
    public JavaWorldMerger(World2 world, File levelDatFile) {
        super(world);
        if (levelDatFile == null) {
            throw new NullPointerException();
        }
        if (! levelDatFile.isFile()) {
            throw new IllegalArgumentException(levelDatFile + " does not exist or is not a regular file");
        }
        this.levelDatFile = levelDatFile;
    }
    
    public File getLevelDatFile() {
        return levelDatFile;
    }
    
    public boolean isReplaceChunks() {
        return replaceChunks;
    }
    
    public void setReplaceChunks(boolean replaceChunks) {
        this.replaceChunks = replaceChunks;
    }

    /**
     * Whether to merge the part of the map <em>below</em> the surface.
     */
    public boolean isMergeUnderworld() {
        return mergeUnderworld;
    }

    public void setMergeUnderworld(final boolean mergeUnderworld) {
        this.mergeUnderworld = mergeUnderworld;
    }

    public int getSurfaceMergeDepth() {
        return surfaceMergeDepth;
    }

    public void setSurfaceMergeDepth(int surfaceMergeDepth) {
        this.surfaceMergeDepth = surfaceMergeDepth;
    }

    /**
     * Whether to clear any existing trees (wood and leaf blocks, as well as
     * vines, cocoa plants and saplings) above the surface.
     */
    public boolean isClearTrees() {
        return clearTrees;
    }

    public void setClearTrees(final boolean clearTrees) {
        this.clearTrees = clearTrees;
    }

    /**
     * Whether to remove any existing resource blocks (diamonds, coal, iron ore,
     * emeralds, redstone ore, gold ore; by changing them to stone blocks)
     * below the surface.
     */
    public boolean isClearResources() {
        return clearResources;
    }

    public void setClearResources(final boolean clearResources) {
        this.clearResources = clearResources;
    }

    /**
     * Whether to remove any existing caves (by changing all air blocks to the
     * most prevalent surrounding block, or stone if it is mostly surrounded by
     * air) below the surface. Note that all hollow spaces are filled in,
     * including dungeons, abandoned mineshafts and strongholds, but any man-
     * made blocks are not removed.
     */
    public boolean isFillCaves() {
        return fillCaves;
    }

    public void setFillCaves(final boolean fillCaves) {
        this.fillCaves = fillCaves;
    }

    /**
     * Whether to remove any above ground vegetation (tall grass, flowers,
     * mushrooms, crops, etc.) other than trees.
     */
    public boolean isClearVegetation() {
        return clearVegetation;
    }

    public void setClearVegetation(boolean clearVegetation) {
        this.clearVegetation = clearVegetation;
    }

    /**
     * Whether to remove all man-made blocks above ground (by replacing them
     * with air).
     */
    public boolean isClearManMadeAboveGround() {
        return clearManMadeAboveGround;
    }

    public void setClearManMadeAboveGround(boolean clearManMadeAboveGround) {
        this.clearManMadeAboveGround = clearManMadeAboveGround;
    }

    /**
     * Whether to remove all man-made blocks below ground (by replacing them
     * with whatever natural block it is surrounded by most, including air).
     */
    public boolean isClearManMadeBelowGround() {
        return clearManMadeBelowGround;
    }

    public void setClearManMadeBelowGround(boolean clearManMadeBelowGround) {
        this.clearManMadeBelowGround = clearManMadeBelowGround;
    }

    /**
     * Perform sanity checks to ensure the merge can proceed. Throws an
     * {@link IllegalArgumentException} if the check fails, with the message
     * describing the problem. Returns the loaded Minecraft level object as a
     * convenience.
     *
     * @param biomesOnly Whether to check sanity for a biomes only merge (when
     *                   <code>true</code>) or a full merge (when
     *                   <code>false</code>).
     * @return The loaded Minecraft level object, for convenience.
     * @throws IllegalArgumentException If there is a problem that would prevent
     * the merge from completing.
     * @throws IOException If the level.dat file could not be read due to an I/O
     * error.
     */
    public Level performSanityChecks(boolean biomesOnly) throws IOException {
        // Read existing level.dat file
        Level level = Level.load(levelDatFile);

        // Sanity checks
        if (biomesOnly) {
            int version = level.getVersion();
            if (version == VERSION_MCREGION) {
                throw new IllegalArgumentException("MCRegion (Minecraft 1.1) maps do not support biomes");
            } else if (version != VERSION_ANVIL) {
                throw new IllegalArgumentException("Version of existing map not supported: 0x" + Integer.toHexString(version));
            }
        } else {
            int existingMaxHeight = level.getMaxHeight();
            if (existingMaxHeight != world.getMaxHeight()) {
                throw new IllegalArgumentException("Existing map has different max height (" + existingMaxHeight + ") than WorldPainter world (" + world.getMaxHeight() + ")");
            }
            int version = level.getVersion();
            if ((version != VERSION_MCREGION) && (version != VERSION_ANVIL)) {
                throw new IllegalArgumentException("Version of existing map not supported: 0x" + Integer.toHexString(version));
            }

            // Dimension sanity checks
            for (Dimension dimension: world.getDimensions()) {
                if (existingMaxHeight != dimension.getMaxHeight()) {
                    throw new IllegalArgumentException("Dimension " + dimension.getName() + " has different max height (" + dimension.getMaxHeight() + ") than existing map (" + existingMaxHeight + ")");
                }
            }
        }

        return level;
    }

    public void merge(File backupDir, ProgressReceiver progressReceiver) throws IOException, ProgressReceiver.OperationCancelled {
        logger.info("Merging world " + world.getName() + " with map at " + levelDatFile.getParentFile());
        
        // Read existing level.dat file and perform sanity checks
        Level level = performSanityChecks(false);
        
        // Record start of export
        long start = System.currentTimeMillis();
        
        // Backup existing level
        File worldDir = levelDatFile.getParentFile();
        if (! worldDir.renameTo(backupDir)) {
            throw new FileInUseException("Could not move " + worldDir + " to " + backupDir);
        }
        if (! worldDir.mkdirs()) {
            throw new IOException("Could not create " + worldDir);
        }
        
        // Set the world to the same Minecraft version as the existing map, in
        // case it has changed. This affects the type of chunks created in the
        // first pass
        int version = level.getVersion();
        int dataVersion = level.getDataVersion();
        setPlatform((version == VERSION_MCREGION) ? DefaultPlugin.JAVA_MCREGION
                : ((dataVersion <= DATA_VERSION_MC_1_12_2) ? DefaultPlugin.JAVA_ANVIL : DefaultPlugin.JAVA_ANVIL_1_13));
        
        // Modify it if necessary and write it to the the new level
        if ((selectedDimensions == null) || selectedDimensions.contains(DIM_NORMAL)) {
            Dimension surfaceDimension = world.getDimension(DIM_NORMAL);
            level.setSeed(surfaceDimension.getMinecraftSeed());
            Point spawnPoint = world.getSpawnPoint();
            level.setSpawnX(spawnPoint.x);
            level.setSpawnY(Math.max(surfaceDimension.getIntHeightAt(spawnPoint), surfaceDimension.getWaterLevelAt(spawnPoint)));
            level.setSpawnZ(spawnPoint.y);
        }
 
        // Save the level.dat file. This will also create a session.lock file, hopefully kicking out any Minecraft
        // instances which may have the map open:
        level.save(worldDir);

        // Copy everything that we are not going to generate
        File[] files = backupDir.listFiles();
        //noinspection ConstantConditions // Cannot happen because we previously loaded level.dat from it
        for (File file: files) {
            if ((! file.getName().equalsIgnoreCase("level.dat"))
                    && (! file.getName().equalsIgnoreCase("level.dat_old"))
                    && (! file.getName().equalsIgnoreCase("session.lock"))
                    && (((selectedDimensions != null) && (! selectedDimensions.contains(DIM_NORMAL))) || (! file.getName().equalsIgnoreCase("region")))
                    && (! file.getName().equalsIgnoreCase("maxheight.txt"))
                    && (! file.getName().equalsIgnoreCase("Height.txt"))
                    && (((selectedDimensions != null) && (! selectedDimensions.contains(DIM_NETHER))) || (! file.getName().equalsIgnoreCase("DIM-1")))
                    && (((selectedDimensions != null) && (! selectedDimensions.contains(DIM_END))) || (! file.getName().equalsIgnoreCase("DIM1")))) {
                if (file.isFile()) {
                    FileUtils.copyFileToDir(file, worldDir);
                } else if (file.isDirectory()) {
                    FileUtils.copyDir(file, new File(worldDir, file.getName()));
                } else {
                    logger.warn("Not copying " + file + "; not a regular file or directory");
                }
            }
        }

        if ((selectedDimensions == null) ? (world.getDimension(DIM_NORMAL) != null) : selectedDimensions.contains(DIM_NORMAL)) {
            mergeDimension(worldDir, backupDir, world.getDimension(DIM_NORMAL), progressReceiver);
        }
        if ((selectedDimensions == null) ? (world.getDimension(DIM_NETHER) != null) : selectedDimensions.contains(DIM_NETHER)) {
            mergeDimension(worldDir, backupDir, world.getDimension(DIM_NETHER), progressReceiver);
        }
        if ((selectedDimensions == null) ? (world.getDimension(DIM_END) != null) : selectedDimensions.contains(DIM_END)) {
            mergeDimension(worldDir, backupDir, world.getDimension(DIM_END), progressReceiver);
        }

        // Update the session.lock file, hopefully kicking out any Minecraft instances which may have tried to open the
        // map in the mean time:
        File sessionLockFile = new File(worldDir, "session.lock");
        try (DataOutputStream sessionOut = new DataOutputStream(new FileOutputStream(sessionLockFile))) {
            sessionOut.writeLong(System.currentTimeMillis());
        }

        // Record the merge in the world history
        if (selectedDimensions == null) {
            world.addHistoryEntry(HistoryEntry.WORLD_MERGED_FULL, level.getName(), worldDir);
        } else {
            String dimNames = selectedDimensions.stream().map(dim -> {
                    switch (dim) {
                        case DIM_NORMAL:
                            return "Surface";
                        case DIM_NETHER:
                            return "Nether";
                        case DIM_END:
                            return "End";
                        default:
                            return Integer.toString(dim);
                    }
                }).collect(Collectors.joining(", "));
            world.addHistoryEntry(HistoryEntry.WORLD_MERGED_PARTIAL, level.getName(), worldDir, dimNames);
        }
        if (! levelDatFile.equals(world.getMergedWith())) {
            world.setMergedWith(levelDatFile);
        }

        // Log an event
        Configuration config = Configuration.getInstance();
        if (config != null) {
            EventVO event = new EventVO(EVENT_KEY_ACTION_MERGE_WORLD).duration(System.currentTimeMillis() - start);
            event.setAttribute(EventVO.ATTRIBUTE_TIMESTAMP, new Date(start));
            event.setAttribute(ATTRIBUTE_KEY_MAX_HEIGHT, world.getMaxHeight());
            event.setAttribute(ATTRIBUTE_KEY_PLATFORM, world.getPlatform().displayName);
            event.setAttribute(ATTRIBUTE_KEY_MAP_FEATURES, world.isMapFeatures());
            event.setAttribute(ATTRIBUTE_KEY_GAME_TYPE_NAME, world.getGameType().name());
            event.setAttribute(ATTRIBUTE_KEY_ALLOW_CHEATS, world.isAllowCheats());
            event.setAttribute(ATTRIBUTE_KEY_GENERATOR, world.getGenerator().name());
            if ((world.getPlatform() == DefaultPlugin.JAVA_ANVIL) && (world.getGenerator() == Generator.FLAT) && (world.getGeneratorOptions() != null)) {
                event.setAttribute(ATTRIBUTE_KEY_GENERATOR_OPTIONS, world.getGeneratorOptions());
            }
            if ((selectedDimensions == null) || selectedDimensions.contains(DIM_NORMAL)) {
                Dimension surfaceDimension = world.getDimension(0);
                event.setAttribute(ATTRIBUTE_KEY_TILES, surfaceDimension.getTiles().size());
                logLayers(surfaceDimension, event, "");
            }
            if (world.getImportedFrom() == null) {
                event.setAttribute(ATTRIBUTE_KEY_IMPORTED_WORLD, false);
            }
            config.logEvent(event);
        }
    }

    public String getWarnings() {
        return warnings;
    }

    private void mergeDimension(final File worldDir, File backupWorldDir, final Dimension dimension, ProgressReceiver progressReceiver) throws ProgressReceiver.OperationCancelled, IOException {
        if (progressReceiver != null) {
            progressReceiver.setMessage("merging " + dimension.getName() + " dimension");
        }
        final File dimensionDir, backupDimensionDir;
        switch (dimension.getDim()) {
            case org.pepsoft.worldpainter.Constants.DIM_NORMAL:
                dimensionDir = worldDir;
                backupDimensionDir = backupWorldDir;
                break;
            case org.pepsoft.worldpainter.Constants.DIM_NETHER:
                dimensionDir = new File(worldDir, "DIM-1");
                backupDimensionDir = new File(backupWorldDir, "DIM-1");
                break;
            case org.pepsoft.worldpainter.Constants.DIM_END:
                dimensionDir = new File(worldDir, "DIM1");
                backupDimensionDir = new File(backupWorldDir, "DIM1");
                break;
            default:
                throw new IllegalArgumentException("Dimension " + dimension.getDim() + " not supported");
        }
        File regionDir = new File(dimensionDir, "region");
        if (! regionDir.exists()) {
            regionDir.mkdirs();
        }
        
        dimension.rememberChanges();
        try {
            
            // Gather all layers used on the map
            final Map<Layer, LayerExporter> exporters = new HashMap<>();
            Set<Layer> allLayers = dimension.getAllLayers(false);
            allLayers.addAll(dimension.getMinimumLayers());
            // If there are combined layers, apply them and gather any newly
            // added layers, recursively
            boolean done;
            do {
                done = true;
                for (Layer layer: new HashSet<>(allLayers)) {
                    if (layer instanceof CombinedLayer) {
                        // Apply the combined layer
                        Set<Layer> addedLayers = ((CombinedLayer) layer).apply(dimension);
                        // Remove the combined layer from the list
                        allLayers.remove(layer);
                        // Add any layers it might have added
                        allLayers.addAll(addedLayers);
                        // Signal that we have to go around at least once more,
                        // in case any of the newly added layers are themselves
                        // combined layers
                        done = false;
                    }
                }
            } while (! done);

            // Load all layer settings into the exporters
            for (Layer layer: allLayers) {
                @SuppressWarnings("unchecked")
                LayerExporter exporter = layer.getExporter();
                if (exporter != null) {
                    exporter.setSettings(dimension.getLayerSettings(layer));
                    exporters.put(layer, exporter);
                }
            }

            // Sort tiles into regions
            int lowestRegionX = Integer.MAX_VALUE, highestRegionX = Integer.MIN_VALUE, lowestRegionZ = Integer.MAX_VALUE, highestRegionZ = Integer.MIN_VALUE;
            Map<Point, Map<Point, Tile>> tilesByRegion = new HashMap<>();
            final boolean tileSelection = selectedTiles != null;
            if (tileSelection) {
                // Sanity check
                assert selectedDimensions.size() == 1;
                assert selectedDimensions.contains(dimension.getDim());
                for (Point tileCoords: selectedTiles) {
                    Tile tile = dimension.getTile(tileCoords);
                    boolean nonReadOnlyChunksFound = false;
outerLoop:          for (int chunkX = 0; chunkX < TILE_SIZE; chunkX += 16) {
                        for (int chunkY = 0; chunkY < TILE_SIZE; chunkY += 16) {
                            if (! tile.getBitLayerValue(ReadOnly.INSTANCE, chunkX, chunkY)) {
                                nonReadOnlyChunksFound = true;
                                break outerLoop;
                            }
                        }
                    }
                    if (! nonReadOnlyChunksFound) {
                        // All chunks in this tile are marked read-only, so we can
                        // skip the entire tile. If all tiles in the region have
                        // only read-only chunks, the entire region does not have to
                        // be merged
                        continue;
                    }
                    int regionX = tileCoords.x >> 2;
                    int regionZ = tileCoords.y >> 2;
                    Point regionCoords = new Point(regionX, regionZ);
                    Map<Point, Tile> tilesForRegion = tilesByRegion.computeIfAbsent(regionCoords, k -> new HashMap<>());
                    tilesForRegion.put(tileCoords, tile);
                    if (regionX < lowestRegionX) {
                        lowestRegionX = regionX;
                    }
                    if (regionX > highestRegionX) {
                        highestRegionX = regionX;
                    }
                    if (regionZ < lowestRegionZ) {
                        lowestRegionZ = regionZ;
                    }
                    if (regionZ > highestRegionZ) {
                        highestRegionZ = regionZ;
                    }
                }
            } else {
                for (Tile tile: dimension.getTiles()) {
                    boolean nonReadOnlyChunksFound = false;
outerLoop:          for (int chunkX = 0; chunkX < TILE_SIZE; chunkX += 16) {
                        for (int chunkY = 0; chunkY < TILE_SIZE; chunkY += 16) {
                            if (! tile.getBitLayerValue(ReadOnly.INSTANCE, chunkX, chunkY)) {
                                nonReadOnlyChunksFound = true;
                                break outerLoop;
                            }
                        }
                    }
                    if (! nonReadOnlyChunksFound) {
                        // All chunks in this tile are marked read-only, so we can
                        // skip the entire tile. If all tiles in the region have
                        // only read-only chunks, the entire region does not have to
                        // be merged
                        continue;
                    }
                    int regionX = tile.getX() >> 2;
                    int regionZ = tile.getY() >> 2;
                    Point regionCoords = new Point(regionX, regionZ);
                    Map<Point, Tile> tilesForRegion = tilesByRegion.computeIfAbsent(regionCoords, k -> new HashMap<>());
                    tilesForRegion.put(new Point(tile.getX(), tile.getY()), tile);
                    if (regionX < lowestRegionX) {
                        lowestRegionX = regionX;
                    }
                    if (regionX > highestRegionX) {
                        highestRegionX = regionX;
                    }
                    if (regionZ < lowestRegionZ) {
                        lowestRegionZ = regionZ;
                    }
                    if (regionZ > highestRegionZ) {
                        highestRegionZ = regionZ;
                    }
                }
            }

            // Read the region coordinates of the existing map
            final File backupRegionDir = new File(backupDimensionDir, "region");
            // TODO: support any platform
            File[] existingRegionFiles = ((DefaultPlatformProvider) platformProvider).getRegionFiles(platform, backupRegionDir);
            Map<Point, File> existingRegions = new HashMap<>();
            for (File file: existingRegionFiles) {
                String[] parts = file.getName().split("\\.");
                int regionX = Integer.parseInt(parts[1]);
                int regionZ = Integer.parseInt(parts[2]);
                existingRegions.put(new Point(regionX, regionZ), file);
                if (regionX < lowestRegionX) {
                    lowestRegionX = regionX;
                }
                if (regionX > highestRegionX) {
                    highestRegionX = regionX;
                }
                if (regionZ < lowestRegionZ) {
                    lowestRegionZ = regionZ;
                }
                if (regionZ > highestRegionZ) {
                    highestRegionZ = regionZ;
                }
            }
            final Set<Point> allRegionCoords = new HashSet<>();
            allRegionCoords.addAll(tilesByRegion.keySet());
            allRegionCoords.addAll(existingRegions.keySet());

            // Sort the regions to export the first two rows together, and then
            // row by row, to get the optimum tempo of performing fixups
            List<Point> sortedRegions = new ArrayList<>(allRegionCoords.size());
            if (lowestRegionZ == highestRegionZ) {
                // No point in sorting it
                sortedRegions.addAll(allRegionCoords);
            } else {
                for (int x = lowestRegionX; x <= highestRegionX; x++) {
                    for (int z = lowestRegionZ; z <= (lowestRegionZ + 1); z++) {
                        Point regionCoords = new Point(x, z);
                        if (allRegionCoords.contains(regionCoords)) {
                            sortedRegions.add(regionCoords);
                        }
                    }
                }
                for (int z = lowestRegionZ + 2; z <= highestRegionZ; z++) {
                    for (int x = lowestRegionX; x <= highestRegionX; x++) {
                        Point regionCoords = new Point(x, z);
                        if (allRegionCoords.contains(regionCoords)) {
                            sortedRegions.add(regionCoords);
                        }
                    }
                }
            }

            // Merge each individual region
            final WorldPainterChunkFactory chunkFactory = new WorldPainterChunkFactory(dimension, exporters, world.getPlatform(), world.getMaxHeight());

            Runtime runtime = Runtime.getRuntime();
            runtime.gc();
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            long memoryInUse = totalMemory - freeMemory;
            long maxMemory = runtime.maxMemory();
            long maxMemoryAvailable = maxMemory - memoryInUse;
            int maxThreadsByMem = (int) (maxMemoryAvailable / 250000000L);
            int threads;
            if (System.getProperty("org.pepsoft.worldpainter.threads") != null) {
                threads = Math.max(Math.min(Integer.parseInt(System.getProperty("org.pepsoft.worldpainter.threads")), tilesByRegion.size()), 1);
            } else {
                threads = Math.max(Math.min(Math.min(maxThreadsByMem, runtime.availableProcessors()), allRegionCoords.size()), 1);
            }
            logger.info("Using " + threads + " thread(s) for merge (cores: " + runtime.availableProcessors() + ", available memory: " + (maxMemoryAvailable / 1048576L) + " MB)");

            final Map<Point, List<Fixup>> fixups = new HashMap<>();
            final Set<Point> exportedRegions = new HashSet<>();
            ExecutorService executor = Executors.newFixedThreadPool(threads, new ThreadFactory() {
                @Override
                public synchronized Thread newThread(Runnable r) {
                    Thread thread = new Thread(threadGroup, r, "Merger-" + nextID++);
                    thread.setPriority(Thread.MIN_PRIORITY);
                    return thread;
                }

                private final ThreadGroup threadGroup = new ThreadGroup("Mergers");
                private int nextID = 1;
            });
            final ParallelProgressManager parallelProgressManager = (progressReceiver != null) ? new ParallelProgressManager(progressReceiver, allRegionCoords.size()) : null;
            try {
                // Merge each individual region
                for (final Point regionCoords: sortedRegions) {
                    if (existingRegions.containsKey(regionCoords)) {
                        if (tilesByRegion.containsKey(regionCoords)) {
                            // Region exists in new and existing maps; merge it
                            if (logger.isDebugEnabled()) {
                                logger.debug("Region " + regionCoords + " will be merged");
                            }
                            final Map<Point, Tile> tiles = tilesByRegion.get(regionCoords);
                            executor.execute(() -> {
                                ProgressReceiver progressReceiver1 = (parallelProgressManager != null) ? parallelProgressManager.createProgressReceiver() : null;
                                if (progressReceiver1 != null) {
                                    try {
                                        progressReceiver1.checkForCancellation();
                                    } catch (ProgressReceiver.OperationCancelled e) {
                                        return;
                                    }
                                }
                                try {
                                    List<Fixup> regionFixups = new ArrayList<>();
                                    WorldRegion minecraftWorld = new WorldRegion(regionCoords.x, regionCoords.y, dimension.getMaxHeight(), platform);
                                    try {
                                        String regionWarnings = mergeRegion(minecraftWorld, backupRegionDir, dimension, regionCoords, tiles, tileSelection, exporters, chunkFactory, regionFixups, (progressReceiver1 != null) ? new SubProgressReceiver(progressReceiver1, 0.0f, 0.9f) : null);
                                        if (regionWarnings != null) {
                                            if (warnings == null) {
                                                warnings = regionWarnings;
                                            } else {
                                                warnings = warnings + regionWarnings;
                                            }
                                        }
                                        if (logger.isDebugEnabled()) {
                                            logger.debug("Merged region " + regionCoords.x + "," + regionCoords.y);
                                        }
                                    } finally {
                                        minecraftWorld.save(worldDir, dimension.getDim());
                                    }
                                    synchronized (fixups) {
                                        if (! regionFixups.isEmpty()) {
                                            fixups.put(new Point(regionCoords.x, regionCoords.y), regionFixups);
                                        }
                                        exportedRegions.add(regionCoords);
                                    }
                                    performFixupsIfNecessary(worldDir, dimension, allRegionCoords, fixups, exportedRegions, progressReceiver1);
                                } catch (Throwable t) {
                                    if (progressReceiver1 != null) {
                                        progressReceiver1.exceptionThrown(t);
                                    } else {
                                        logger.error("Exception while exporting region", t);
                                    }
                                }
                            });
                        } else {
                            // Region only exists in existing world. Copy it to the new
                            // world
                            if (logger.isDebugEnabled()) {
                                logger.debug("Region " + regionCoords + " does not exist in new world and will be copied from existing map");
                            }
                            ProgressReceiver subProgressReceiver = (parallelProgressManager != null) ? parallelProgressManager.createProgressReceiver() : null;
                            if (subProgressReceiver != null) {
                                subProgressReceiver.setMessage("Copying region " + regionCoords.x + "," + regionCoords.y + " unchanged");
                            }
                            FileUtils.copyFileToDir(existingRegions.get(regionCoords), regionDir, subProgressReceiver);
                            synchronized (fixups) {
                                exportedRegions.add(regionCoords);
                            }
                            if (logger.isDebugEnabled()) {
                                logger.debug("Copied region " + regionCoords.x + "," + regionCoords.y);
                            }
                        }
                    } else {
                        // Region only exists in new world. Create it as new
                        if (logger.isDebugEnabled()) {
                            logger.debug("Region " + regionCoords + " does not exist in existing map and will be created as new");
                        }
                        executor.execute(() -> {
                            ProgressReceiver progressReceiver1 = (parallelProgressManager != null) ? parallelProgressManager.createProgressReceiver() : null;
                            if (progressReceiver1 != null) {
                                try {
                                    progressReceiver1.checkForCancellation();
                                } catch (ProgressReceiver.OperationCancelled e) {
                                    return;
                                }
                            }
                            try {
                                WorldRegion minecraftWorld = new WorldRegion(regionCoords.x, regionCoords.y, dimension.getMaxHeight(), platform);
                                ExportResults exportResults = null;
                                try {
                                    exportResults = exportRegion(minecraftWorld, dimension, null, regionCoords, tileSelection, exporters, null, chunkFactory, null, (progressReceiver1 != null) ? new SubProgressReceiver(progressReceiver1, 0.9f, 0.1f) : null);
                                    if (logger.isDebugEnabled()) {
                                        logger.debug("Generated region " + regionCoords.x + "," + regionCoords.y);
                                    }
                                } finally {
                                    if ((exportResults != null) && exportResults.chunksGenerated) {
                                        minecraftWorld.save(worldDir, dimension.getDim());
                                    }
                                }
                                synchronized (fixups) {
                                    if ((exportResults.fixups != null) && (! exportResults.fixups.isEmpty())) {
                                        fixups.put(new Point(regionCoords.x, regionCoords.y), exportResults.fixups);
                                    }
                                    exportedRegions.add(regionCoords);
                                }
                                performFixupsIfNecessary(worldDir, dimension, allRegionCoords, fixups, exportedRegions, progressReceiver1);
                            } catch (Throwable t) {
                                if (progressReceiver1 != null) {
                                    progressReceiver1.exceptionThrown(t);
                                } else {
                                    logger.error("Exception while exporting region", t);
                                }
                            }
                        });
                    }
                }
            } finally {
                executor.shutdown();
                try {
                    executor.awaitTermination(1000, TimeUnit.DAYS);
                } catch (InterruptedException e) {
                    throw new RuntimeException("Thread interrupted while waiting for all tasks to finish", e);
                }
            }

            // It's possible for there to be fixups left, if thread A was
            // performing fixups and thread B added new ones and then quit, or
            // if regions were copied from the existing map
            synchronized (fixups) {
                if (! fixups.isEmpty()) {
                    if (progressReceiver != null) {
                        progressReceiver.setMessage("doing remaining fixups for " + dimension.getName());
                        progressReceiver.reset();
                    }
                    performFixups(worldDir, dimension, (progressReceiver != null) ? new SubProgressReceiver(progressReceiver, 0.9f, 0.1f) : null, fixups);
                }
            }

            if (progressReceiver != null) {
                progressReceiver.setProgress(1.0f);
            }
        } finally {
            
            // Undo any changes we made (such as applying any combined layers)
            if (dimension.undoChanges()) {
                // TODO: some kind of cleverer undo mechanism (undo history
                // cloning?) so we don't mess up the user's redo history
                dimension.clearRedo();
                dimension.armSavePoint();
            }
        }
    }

    private String mergeRegion(MinecraftWorld minecraftWorld, File oldRegionDir, Dimension dimension, Point regionCoords, Map<Point, Tile> tiles, boolean tileSelection, Map<Layer, LayerExporter> exporters, ChunkFactory chunkFactory, List<Fixup> fixups, ProgressReceiver progressReceiver) throws IOException, ProgressReceiver.OperationCancelled {
        if (progressReceiver != null) {
            progressReceiver.setMessage("Merging region " + regionCoords.x + "," + regionCoords.y + " of " + dimension.getName());
        }

        Set<Layer> allLayers = new HashSet<>();
        for (Tile tile: tiles.values()) {
            allLayers.addAll(tile.getLayers());
        }
        
        // Add layers that have been configured to be applied everywhere
        Set<Layer> minimumLayers = dimension.getMinimumLayers();
        allLayers.addAll(minimumLayers);
        
        List<Layer> secondaryPassLayers = new ArrayList<>();
        for (Layer layer: allLayers) {
            LayerExporter exporter = layer.getExporter();
            if (exporter instanceof SecondPassLayerExporter) {
                secondaryPassLayers.add(layer);
            }
        }
        Collections.sort(secondaryPassLayers);

        // First pass. Create terrain and apply layers which don't need access
        // to neighbouring chunks
        long t1 = System.currentTimeMillis();
        String warnings;
        if (firstPass(minecraftWorld, dimension, regionCoords, tiles, tileSelection, exporters, chunkFactory, false, (progressReceiver != null) ? new SubProgressReceiver(progressReceiver, 0.0f, 0.3f) : null).chunksGenerated) {
            // Second pass. Apply layers which need information from or apply
            // changes to neighbouring chunks
            long t2 = System.currentTimeMillis();
            List<Fixup> myFixups = secondPass(secondaryPassLayers, dimension, minecraftWorld, exporters, tiles.values(), regionCoords, (progressReceiver != null) ? new SubProgressReceiver(progressReceiver, 0.3f, 0.1f) : null);
            if ((myFixups != null) && (! myFixups.isEmpty())) {
                synchronized (fixups) {
                    fixups.addAll(myFixups);
                }
            }

            // Merge chunks
            long t3 = System.currentTimeMillis();
            warnings = thirdPass(minecraftWorld, oldRegionDir, dimension, regionCoords, (progressReceiver != null) ? new SubProgressReceiver(progressReceiver, 0.4f, 0.25f) : null);

            // Post processing. Fix covered grass blocks, things like that
            long t4 = System.currentTimeMillis();
            PlatformManager.getInstance().getPostProcessor(platform).postProcess(minecraftWorld, new Rectangle(regionCoords.x << 9, regionCoords.y << 9, 512, 512), (progressReceiver != null) ? new SubProgressReceiver(progressReceiver, 0.65f, 0.1f) : null);

            // Third pass. Calculate lighting
            long t5 = System.currentTimeMillis();
            lightingPass(minecraftWorld, regionCoords, (progressReceiver != null) ? new SubProgressReceiver(progressReceiver, 0.75f, 0.25f) : null);
            long t6 = System.currentTimeMillis();
            if ("true".equalsIgnoreCase(System.getProperty("org.pepsoft.worldpainter.devMode"))) {
                String timingMessage = (t2 - t1) + ", " + (t3 - t2) + ", " + (t4 - t3) + ", " + (t5 - t4) + ", " + (t6 - t5) + ", " + (t6 - t1);
//                System.out.println("Merge timing: " + timingMessage);
                synchronized (TIMING_FILE_LOCK) {
                    try (PrintWriter out = new PrintWriter(new FileOutputStream("mergetimings.csv", true))) {
                        out.println(timingMessage);
                    }
                }
            }
        } else {
            // First pass produced no chunks; copy all chunks from the existing
            // region
            warnings = copyAllChunks(minecraftWorld, oldRegionDir, dimension, regionCoords, (progressReceiver != null) ? new SubProgressReceiver(progressReceiver, 0.3f, 0.7f) : null);
        }
        return warnings;
    }
    
    /**
     * Merge only the biomes, leave everything else the same.
     */
    public void mergeBiomes(File backupDir, ProgressReceiver progressReceiver) throws IOException, ProgressReceiver.OperationCancelled {
        // Read existing level.dat file and perform sanity checks
        Level level = performSanityChecks(true);

        int dataVersion = level.getDataVersion();
        Platform platform = (dataVersion <= DATA_VERSION_MC_1_12_2) ? DefaultPlugin.JAVA_ANVIL : DefaultPlugin.JAVA_ANVIL_1_13;
        // TODO support any platform
        DefaultPlatformProvider platformProvider = (DefaultPlatformProvider) PlatformManager.getInstance().getPlatformProvider(platform);

        // Backup existing level
        File worldDir = levelDatFile.getParentFile();
        if (! worldDir.renameTo(backupDir)) {
            throw new FileInUseException("Could not move " + worldDir + " to " + backupDir);
        }
        if (! worldDir.mkdirs()) {
            throw new IOException("Could not create " + worldDir);
        }
        
        // Copy everything that we are not going to generate (this includes the
        // Nether and End dimensions)
        File[] files = backupDir.listFiles();
        //noinspection ConstantConditions // Cannot happen because we previously loaded level.dat from it
        for (File file: files) {
            if ((! file.getName().equalsIgnoreCase("session.lock"))
                    && (! file.getName().equalsIgnoreCase("region"))) {
                if (file.isFile()) {
                    FileUtils.copyFileToDir(file, worldDir);
                } else if (file.isDirectory()) {
                    FileUtils.copyDir(file, new File(worldDir, file.getName()));
                } else {
                    logger.warn("Not copying " + file + "; not a regular file or directory");
                }
            }
        }

        // Write session.lock file
        File sessionLockFile = new File(worldDir, "session.lock");
        try (DataOutputStream sessionOut = new DataOutputStream(new FileOutputStream(sessionLockFile))) {
            sessionOut.writeLong(System.currentTimeMillis());
        }
        
        // Process all chunks and copy just the biomes
        if (progressReceiver != null) {
            progressReceiver.setMessage("Merging biomes");
        }
        // Find all the region files of the existing level
        File[] oldRegionFiles = platformProvider.getRegionFiles(platform, new File(backupDir, "region"));

        // Process each region file, copying every chunk unmodified, except
        // for the biomes
        @SuppressWarnings("ConstantConditions") // Can only happen for corrupted maps
        int totalChunkCount = oldRegionFiles.length * 32 * 32, chunkCount = 0;
        File newRegionDir = new File(worldDir, "region");
        newRegionDir.mkdirs();
        Dimension dimension = world.getDimension(DIM_NORMAL);
        for (File file: oldRegionFiles) {
            try (RegionFile oldRegion = new RegionFile(file)) {
                String[] parts = file.getName().split("\\.");
                int regionX = Integer.parseInt(parts[1]);
                int regionZ = Integer.parseInt(parts[2]);
                File newRegionFile = new File(newRegionDir, "r." + regionX + "." + regionZ + ".mca");
                try (RegionFile newRegion = new RegionFile(newRegionFile)) {
                    for (int x = 0; x < 32; x++) {
                        for (int z = 0; z < 32; z++) {
                            if (oldRegion.containsChunk(x, z)) {
                                NBTChunk chunk;
                                try (NBTInputStream in = new NBTInputStream(oldRegion.getChunkDataInputStream(x, z))) {
                                    CompoundTag tag = (CompoundTag) in.readTag();
                                    chunk = platformProvider.createChunk(platform, tag, level.getMaxHeight());
                                }
                                int chunkX = chunk.getxPos(), chunkZ = chunk.getzPos();
                                for (int xx = 0; xx < 16; xx++) {
                                    for (int zz = 0; zz < 16; zz++) {
                                        chunk.setBiome(xx, zz, dimension.getLayerValueAt(Biome.INSTANCE, (chunkX << 4) | xx, (chunkZ << 4) | zz));
                                    }
                                }
                                try (NBTOutputStream out = new NBTOutputStream(newRegion.getChunkDataOutputStream(x, z))) {
                                    out.writeTag(chunk.toNBT());
                                }
                            }
                            chunkCount++;
                            if (progressReceiver != null) {
                                progressReceiver.setProgress((float) chunkCount / totalChunkCount);
                            }
                        }
                    }
                }
            }
        }

        // Rewrite session.lock file
        try (DataOutputStream sessionOut = new DataOutputStream(new FileOutputStream(sessionLockFile))) {
            sessionOut.writeLong(System.currentTimeMillis());
        }
    }
    
    private String thirdPass(MinecraftWorld minecraftWorld, File oldRegionDir, Dimension dimension, Point regionCoords, ProgressReceiver progressReceiver) throws IOException, ProgressReceiver.OperationCancelled {
        if (progressReceiver != null) {
            progressReceiver.setMessage("Merging existing blocks with new");
        }

        // TODO: we used to do one extra ring of chunks here. Not sure why, perhaps we'll rediscover it...
        int lowestChunkX = regionCoords.x << 5;
        int highestChunkX = (regionCoords.x << 5) + 31;
        int lowestChunkY = regionCoords.y << 5;
        int highestChunkY = (regionCoords.y << 5) + 31;
        int maxHeight = dimension.getMaxHeight();
        Map<Point, RegionFile> regionFiles = new HashMap<>();
        Set<Point> damagedRegions = new HashSet<>();
        StringBuilder reportBuilder = new StringBuilder();
        try {
            int chunkNo = 0;
            for (int chunkX = lowestChunkX; chunkX <= highestChunkX; chunkX++) {
                for (int chunkY = lowestChunkY; chunkY <= highestChunkY; chunkY++) {
                    chunkNo++;
                    if (progressReceiver != null) {
                        progressReceiver.setProgress((float) chunkNo / 1156);
                    }
                    Chunk newChunk;
                    if (dimension.getTile(chunkX >> 3, chunkY >> 3) == null) {
                        // The tile for this chunk does not exist in the new
                        // world, so the chunk from the existing world should
                        // be copied
                        newChunk = null;
                    } else {
                        newChunk = minecraftWorld.getChunk(chunkX, chunkY);
                    }
                    if (replaceChunks && (newChunk != null)) {
                        // The chunk exists in the new world, and replace all
                        // chunks has been requested, so leave the new chunk
                        // as is
                        if (logger.isDebugEnabled()) {
                            logger.debug("Using chunk from new world at " +chunkX + "," + chunkY);
                        }
                        continue;
                    }
                    int regionX = chunkX >> 5;
                    int regionY = chunkY >> 5;
                    Point coords = new Point(regionX, regionY);
                    if (damagedRegions.contains(coords)) {
                        // We can't read this region, which we have already
                        // reported and logged earlier
                        continue;
                    }
                    RegionFile regionFile = regionFiles.get(coords);
                    if (regionFile == null) {
                        try {
                            // TODO support any platform
                            regionFile = ((DefaultPlatformProvider) platformProvider).getRegionFile(platform, oldRegionDir, coords, true);
                            regionFiles.put(coords, regionFile);
                        } catch (IOException e) {
                            reportBuilder.append("I/O error while opening region " + regionX + "," + regionY + " (message: \"" + e.getMessage() + "\"); skipping region" + EOL);
                            logger.error("I/O error while opening region " + regionX + "," + regionY + "; skipping region", e);
                            damagedRegions.add(coords);
                            continue;
                        }
                    }
                    int chunkXInRegion = chunkX & 0x1f;
                    int chunkYInRegion = chunkY & 0x1f;
                    if (regionFile.containsChunk(chunkXInRegion, chunkYInRegion)) {
                        Chunk existingChunk;
                        try {
                            DataInputStream chunkData = regionFile.getChunkDataInputStream(chunkXInRegion, chunkYInRegion);
                            if (chunkData == null) {
                                // This should never happen, since we checked with
                                // isChunkPresent(), but in practice it does. Perhaps
                                // corrupted data?
                                reportBuilder.append("Missing chunk data in existing map for chunk " + chunkXInRegion + ", " + chunkYInRegion + " in " + regionFile + "; skipping chunk" + EOL);
                                logger.warn("Missing chunk data in existing map for chunk " + chunkXInRegion + ", " + chunkYInRegion + " in " + regionFile + "; skipping chunk");
                                continue;
                            }
                            try (NBTInputStream in = new NBTInputStream(chunkData)) {
                                // TODO: support any platform
                                existingChunk = ((DefaultPlatformProvider) platformProvider).createChunk(platform, in.readTag(), maxHeight);
                            }
                        } catch (IOException e) {
                            reportBuilder.append("I/O error while reading chunk in existing map " + chunkXInRegion + ", " + chunkYInRegion + " from file " + regionFile + " (message: \"" + e.getMessage() + "\"); skipping chunk" + EOL);
                            logger.error("I/O error while reading chunk in existing map " + chunkXInRegion + ", " + chunkYInRegion + " from file " + regionFile + "; skipping chunk", e);
                            continue;
                        } catch (IllegalArgumentException e) {
                            reportBuilder.append("Illegal argument exception while reading chunk in existing map " + chunkXInRegion + ", " + chunkYInRegion + " from file " + regionFile + " (message: \"" + e.getMessage() + "\"); skipping chunk" + EOL);
                            logger.error("Illegal argument exception while reading chunk in existing map " + chunkXInRegion + ", " + chunkYInRegion + " from file " + regionFile + "; skipping chunk", e);
                            continue;
                        } catch (ClassCastException e) {
                            reportBuilder.append("Class cast exception while reading chunk in existing map " + chunkXInRegion + ", " + chunkYInRegion + " from file " + regionFile + " (message: \"" + e.getMessage() + "\"); skipping chunk" + EOL);
                            logger.error("Class cast exception while reading chunk in existing map " + chunkXInRegion + ", " + chunkYInRegion + " from file " + regionFile + "; skipping chunk", e);
                            continue;
                        }
                        if (newChunk != null) {
                            // Chunk exists in existing and new world; merge it
                            // Do any necessary processing of the existing chunk
                            // (clearing trees, etc.) No need to check for
                            // read-only; if the chunk was read-only it
                            // wouldn't exist in the new map and we wouldn't
                            // be here
                            processExistingChunk(existingChunk);
                            try {
                                newChunk = mergeChunk(existingChunk, newChunk, dimension);
                                minecraftWorld.addChunk(newChunk);
                            } catch (NullPointerException e) {
                                reportBuilder.append("Null pointer exception while reading chunk in existing map " + chunkXInRegion + ", " + chunkYInRegion + " from file " + regionFile + "; skipping chunk" + EOL);
                                logger.error("Null pointer exception while reading chunk in existing map " + chunkXInRegion + ", " + chunkYInRegion + " from file " + regionFile + "; skipping chunk", e);
                                continue;
                            } catch (ArrayIndexOutOfBoundsException e) {
                                reportBuilder.append("Array index out of bounds while reading chunk in existing map " + chunkXInRegion + ", " + chunkYInRegion + " from file " + regionFile + " (message: \"" + e.getMessage() + "\"); skipping chunk" + EOL);
                                logger.error("Array index out of bounds while reading chunk in existing map " + chunkXInRegion + ", " + chunkYInRegion + " from file " + regionFile + "; skipping chunk", e);
                                continue;
                            }
                        } else {
                            // Chunk exists in existing world, but not in new
                            // one, copy old to new
                            if (logger.isDebugEnabled()) {
                                logger.debug("Using chunk from existing map at " + chunkX + "," + chunkY);
                            }
                            minecraftWorld.addChunk(existingChunk);
                        }
                    }
                }
            }
        } finally {
            for (RegionFile regionFile: regionFiles.values()) {
                regionFile.close();
            }
        }
        if (progressReceiver != null) {
            progressReceiver.setProgress(1.0f);
        }
        return reportBuilder.length() != 0 ? reportBuilder.toString() : null;
    }

    private void processExistingChunk(final Chunk existingChunk) {
        if (! (clearTrees || fillCaves || clearResources || clearVegetation || clearManMadeAboveGround || clearManMadeBelowGround)) {
            return;
        }
        int maxZ = world.getMaxHeight() - 1;
        for (int x = 0; x < 16; x++) {
            for (int y = 0; y < 16; y++) {
                boolean aboveGround = true;
                for (int z = maxZ; z >= 0; z--) {
                    Material existingBlock = existingChunk.getMaterial(x, z, y);
                    if (aboveGround) {
                        if ((clearTrees && existingBlock.treeRelated)
                                || (clearVegetation && existingBlock.vegetation)
                                || (clearManMadeAboveGround && (! existingBlock.natural))) {
                            setToAir(existingChunk, x, y, z);
                        } else if (existingBlock.terrain) {
                            aboveGround = false;
                        }
                    } else {
                        // Separate if-statements so that if both are enabled,
                        // man made blocks are correctly removed and then filled
                        // in
                        if (clearManMadeBelowGround && (! existingBlock.natural)) {
                            final Material newMaterial = findMostPrevalentSolidSurroundingMaterial(existingChunk, x, y, z);
                            if (newMaterial == AIR) {
                                setToAir(existingChunk, x, y, z);
                            } else {
                                existingChunk.setMaterial(x, z, y, newMaterial);
                                existingChunk.setSkyLightLevel(x, z, y, 0);
                                existingChunk.setBlockLightLevel(x, z, y, 0);
                            }
                            existingBlock = existingChunk.getMaterial(x, z, y);
                        }
                        if (fillCaves && existingBlock.veryInsubstantial) {
                            final Material newMaterial = findMostPrevalentSolidSurroundingMaterial(existingChunk, x, y, z);
                            if (newMaterial == AIR) {
                                existingChunk.setMaterial(x, z, y, STONE);
                            } else {
                                existingChunk.setMaterial(x, z, y, newMaterial);
                            }
                            existingChunk.setSkyLightLevel(x, z, y, 0);
                            existingChunk.setBlockLightLevel(x, z, y, 0);
                        } else if (clearResources && existingBlock.resource) {
                            if (existingBlock.isNamed(MC_NETHER_QUARTZ_ORE)) {
                                existingChunk.setMaterial(x, z, y, NETHERRACK);
                            } else {
                                existingChunk.setMaterial(x, z, y, STONE);
                            }
                        }
                    }
                }
            }
        }
    }

    private void setToAir(final Chunk chunk, final int x, final int y, final int z) {
        chunk.setMaterial(x, z, y, AIR);
        // Note that these lighting calculations aren't strictly necessary since
        // the lighting will be fully recalculated later on, but it doesn't hurt
        // and it might improve performance and/or fill in gaps in the logic
        int maxZ = world.getMaxHeight() - 1;
        int skyLightLevelAbove = (z < maxZ) ? chunk.getSkyLightLevel(x, z + 1, y) : 15;
        int skyLightLevelBelow = (z > 0) ? chunk.getSkyLightLevel(x, z - 1, y) : 0;
        int blockLightLevelAbove = (z < maxZ) ? chunk.getSkyLightLevel(x, z + 1, y) : 0;
        int blockLightLevelBelow = (z > 0) ? chunk.getBlockLightLevel(x, z - 1, y) : 0;
        if (skyLightLevelAbove == 15) {
            // Propagate full daylight down
            chunk.setSkyLightLevel(x, z, y, 15);
        } else {
            chunk.setSkyLightLevel(x, z, y, Math.max(Math.max(skyLightLevelAbove, skyLightLevelBelow) - 1, 0));
        }
        chunk.setBlockLightLevel(x, z, y, Math.max(Math.max(blockLightLevelAbove, blockLightLevelBelow) - 1, 0));
    }

    /**
     * Finds the most prevalent natural, non-ore, solid block type surrounding
     * a particular block (inside the same chunk).
     */
    private Material findMostPrevalentSolidSurroundingMaterial(Chunk existingChunk, int x, int y, int z) {
        Map<Material, Integer> materialCounts = materialCountsRef.get();
        materialCounts.clear();
        int mostPrevalentMaterialCount = 0;
        Material mostPrevalentMaterial = AIR;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if ((dx == 0) && (dy == 0) && (dz == 0)) {
                        continue;
                    }
                    int xx = x + dx, yy = y + dy, zz = z + dz;
                    if ((xx < 0) || (xx > 15) || (yy < 0) || (yy > 15) || (zz < 0) || (zz >= existingChunk.getMaxHeight())) {
                        continue;
                    }
                    Material material = existingChunk.getMaterial(xx, zz, yy);
                    if (material.solid && (! material.resource) && material.opaque) {
                        int newCount = materialCounts.merge(material, 1, (count, delta) -> count + delta);
                        if (newCount > mostPrevalentMaterialCount) {
                            mostPrevalentMaterialCount = newCount;
                            mostPrevalentMaterial = material;
                        }
                    }
                }
            }
        }
        return mostPrevalentMaterial;
    }
    
    private String copyAllChunks(MinecraftWorld minecraftWorld, File oldRegionDir, Dimension dimension, Point regionCoords, ProgressReceiver progressReceiver) throws IOException, ProgressReceiver.OperationCancelled {
        if (progressReceiver != null) {
            progressReceiver.setMessage("Copying chunks unchanged");
        }

        int lowestChunkX = regionCoords.x << 5;
        int highestChunkX = (regionCoords.x << 5) + 31;
        int lowestChunkY = regionCoords.y << 5;
        int highestChunkY = (regionCoords.y << 5) + 31;
        int maxHeight = dimension.getMaxHeight();
        Map<Point, RegionFile> regionFiles = new HashMap<>();
        Set<Point> damagedRegions = new HashSet<>();
        StringBuilder reportBuilder = new StringBuilder();
        try {
            int chunkNo = 0;
            for (int chunkX = lowestChunkX; chunkX <= highestChunkX; chunkX++) {
                for (int chunkY = lowestChunkY; chunkY <= highestChunkY; chunkY++) {
                    chunkNo++;
                    if (progressReceiver != null) {
                        progressReceiver.setProgress((float) chunkNo / 1024);
                    }
                    int regionX = chunkX >> 5;
                    int regionY = chunkY >> 5;
                    Point coords = new Point(regionX, regionY);
                    if (damagedRegions.contains(coords)) {
                        // We can't read this region, which we have already
                        // reported and logged earlier
                        continue;
                    }
                    RegionFile regionFile = regionFiles.get(coords);
                    if (regionFile == null) {
                        try {
                            // TODO: support any platform
                            regionFile = ((DefaultPlatformProvider) platformProvider).getRegionFile(platform, oldRegionDir, coords, true);
                            regionFiles.put(coords, regionFile);
                        } catch (IOException e) {
                            reportBuilder.append("I/O error while opening region " + regionX + "," + regionY + " (message: \"" + e.getMessage() + "\"); skipping region" + EOL);
                            logger.error("I/O error while opening region " + regionX + "," + regionY + "; skipping region", e);
                            damagedRegions.add(coords);
                            continue;
                        }
                    }
                    int chunkXInRegion = chunkX & 0x1f;
                    int chunkYInRegion = chunkY & 0x1f;
                    if (regionFile.containsChunk(chunkXInRegion, chunkYInRegion)) {
                        Tag tag;
                        try {
                            InputStream chunkData = regionFile.getChunkDataInputStream(chunkXInRegion, chunkYInRegion);
                            if (chunkData == null) {
                                // This should never happen, since we checked
                                // with isChunkPresent(), but in practice it
                                // does. Perhaps corrupted data?
                                reportBuilder.append("Missing chunk data for chunk " + chunkXInRegion + ", " + chunkYInRegion + " in " + regionFile + "; skipping chunk" + EOL);
                                logger.warn("Missing chunk data for chunk " + chunkXInRegion + ", " + chunkYInRegion + " in " + regionFile + "; skipping chunk");
                                continue;
                            }
                            try (NBTInputStream in = new NBTInputStream(chunkData)) {
                                tag = in.readTag();
                            }
                        } catch (IOException e) {
                            reportBuilder.append("I/O error while reading chunk " + chunkXInRegion + ", " + chunkYInRegion + " from file " + regionFile + " (message: \"" + e.getMessage() + "\"); skipping chunk" + EOL);
                            logger.error("I/O error while reading chunk " + chunkXInRegion + ", " + chunkYInRegion + " from file " + regionFile + "; skipping chunk", e);
                            continue;
                        } catch (IllegalArgumentException e) {
                            reportBuilder.append("Illegal argument exception while reading chunk " + chunkXInRegion + ", " + chunkYInRegion + " from file " + regionFile + " (message: \"" + e.getMessage() + "\"); skipping chunk" + EOL);
                            logger.error("Illegal argument exception while reading chunk " + chunkXInRegion + ", " + chunkYInRegion + " from file " + regionFile + "; skipping chunk", e);
                            continue;
                        }
                        // TODO: support any platform
                        minecraftWorld.addChunk(((DefaultPlatformProvider) platformProvider).createChunk(platform, tag, maxHeight));
                    }
                }
            }
        } finally {
            for (RegionFile regionFile: regionFiles.values()) {
                regionFile.close();
            }
        }
        if (progressReceiver != null) {
            progressReceiver.setProgress(1.0f);
        }
        return reportBuilder.length() != 0 ? reportBuilder.toString() : null;
    }
    
    private Chunk mergeChunk(Chunk existingChunk, Chunk newChunk, Dimension dimension) {
        if (logger.isDebugEnabled()) {
            logger.debug("Merging chunks at " + existingChunk.getxPos() + "," + existingChunk.getzPos());
        }
        int maxY = existingChunk.getMaxHeight() - 1;
        int chunkX = existingChunk.getxPos() << 4, chunkZ = existingChunk.getzPos() << 4;
        List<Entity> newChunkEntities = newChunk.getEntities();
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                if (dimension.getBitLayerValueAt(org.pepsoft.worldpainter.layers.Void.INSTANCE, chunkX | x, chunkZ | z)) {
                    // Void. Just empty the entire column
                    // TODO: only empty from the terrain height on downwards? or find some other way of preserving overhanging trees, that kind of thing?
                    for (int y = 0; y <= maxY; y++) {
                        newChunk.setMaterial(x, y, z, AIR);
                        newChunk.setBlockLightLevel(x, y, z, 0);
                        newChunk.setSkyLightLevel(x, y, z, 15);
                    }
                } else {
                    final int newHeight = dimension.getIntHeightAt(chunkX | x, chunkZ | z);
                    final boolean frost = dimension.getBitLayerValueAt(Frost.INSTANCE, chunkX | x, chunkZ | z);
                    int oldHeight = 0;
                    for (int y = maxY; y >= 0; y--) {
                        Material oldMaterial = existingChunk.getMaterial(x, y, z);
                        if (oldMaterial.terrain) {
                            // Terrain found
                            oldHeight = y;
                            break;
                        }
                    }
                    final int dy = newHeight - oldHeight;
                    if (dy > 0) {
                        // Terrain has been raised
                        // Copy or merge underground portion from existing chunk
                        final int mergeLimit = Math.min(newHeight - surfaceMergeDepth, oldHeight);
                        for (int y = 0; y <= mergeLimit; y++) {
                            mergeUndergroundBlock(existingChunk, newChunk, x, y, z);
                        }
                        // Merge surface layer blocks
                        for (int y = mergeLimit + 1; y <= newHeight; y++) {
                            mergeSurfaceBlock(existingChunk, newChunk, x, y, z, y < oldHeight);
                        }
                        // Merge above ground portion from existing chunk, raised by
                        // the appropriate amount
                        for (int y = newHeight + 1; y <= maxY; y++) {
                            mergeAboveGroundBlock(existingChunk, newChunk, x, y, z, dy, frost);
                        }
                        newChunk.setHeight(x, z, Math.min(existingChunk.getHeight(x, z) + dy, maxY));
                    } else if (dy < 0) {
                        // Terrain has been lowered
                        // Copy underground portion from existing chunk
                        final int mergeLimit = newHeight - surfaceMergeDepth;
                        for (int y = 0; y <= mergeLimit; y++) {
                            mergeUndergroundBlock(existingChunk, newChunk, x, y, z);
                        }
                        // If the new ground height block is insubstantial in the
                        // existing chunk, and there is nothing substantial on the
                        // block in the new or existing chunks, remove it, so as not
                        // to create a weird one block layer of blocks over newly
                        // opened up voids such as caves, chasms, abandoned mines,
                        // etc.
                        final int mergeStartHeight = newHeight + 1;
                        final Material existingMaterial = existingChunk.getMaterial(x, newHeight, z);
                        if ((existingMaterial == AIR) || existingMaterial.insubstantial) {
                            Material existingMaterialAbove = (newHeight < maxY) ? existingChunk.getMaterial(x, newHeight + 1, z) : AIR;
                            Material newMaterialAbove = (((newHeight - dy) >= -1) && ((newHeight - dy) < maxY)) ? newChunk.getMaterial(x, newHeight + 1 - dy, z) : AIR;
                            if (((newMaterialAbove == AIR) || newMaterialAbove.insubstantial) && ((existingMaterialAbove == AIR) || existingMaterialAbove.insubstantial)) {
                                newChunk.setMaterial(x, newHeight, z, AIR);
                                newChunk.setSkyLightLevel(x, newHeight, z, 0);
                                newChunk.setBlockLightLevel(x, newHeight, z, 0);
                            }
                        }
                        // Copy above ground portion from existing chunk, lowered by
                        // the appropriate amount
                        for (int y = mergeStartHeight; y <= (maxY + dy); y++) {
                            mergeAboveGroundBlock(existingChunk, newChunk, x, y, z, dy, frost);
                        }
                        newChunk.setHeight(x, z, Math.min(existingChunk.getHeight(x, z) + dy, maxY));
                    } else {
                        // Terrain height has not changed. Copy everything from the
                        // existing chunk, except the top layer of the terrain.
                        final int mergeLimit = newHeight - surfaceMergeDepth;
                        for (int y = 0; y <= mergeLimit; y++) {
                            mergeUndergroundBlock(existingChunk, newChunk, x, y, z);
                        }
                        for (int y = newHeight + 1; y <= maxY; y++) {
                            mergeAboveGroundBlock(existingChunk, newChunk, x, y, z, 0, frost);
                        }
                    }
                    Material newTerrainMaterial = newChunk.getMaterial(x, newHeight, z);
                    Material oldTerrainMaterial = existingChunk.getMaterial(x, oldHeight, z);
                    if (((newTerrainMaterial == DIRT) && oldTerrainMaterial.isNamed(MC_FARMLAND)) // Tilled earth is imported as dirt, so make sure to leave it intact.
                            || (newTerrainMaterial.isNamed(MC_WATER) && oldTerrainMaterial.isNamed(MC_FROSTED_ICE))) { // Frosted ice is imported as water, so make sure to leave it intact

                        newChunk.setMaterial(x, newHeight, z, oldTerrainMaterial);
                    }
                    final int blockX = chunkX + x, blockZ = chunkZ + z;
                    for (Entity entity: existingChunk.getEntities()) {
                        final double[] pos = entity.getPos();
                        if ((pos[0] >= blockX) && (pos[0] < blockX + 1) && (pos[2] >= blockZ) && (pos[2] < blockZ + 1)) {
                            if (pos[1] > oldHeight) {
                                pos[1] = pos[1] + dy;
                                if (pos[1] > maxY + 2) {
                                    pos[1] = maxY + 2;
                                }
                                entity.setPos(pos);
                            }
                            newChunkEntities.add(entity);
                        }
                    }
                }
            }
        }
        for (Entity entity: existingChunk.getEntities()) {
            final double[] pos = entity.getPos();
            if ((pos[0] < chunkX) || (pos[0] >= chunkX + 16) || (pos[2] < chunkZ) || (pos[2] >= chunkZ + 16)) {
                // The entity has wandered outside of the chunk, we
                // don't have the information to determine how much to
                // adjust its vertical position; just copy it, since in
                // practice most chunks will not have changed height
                // anyway, so at least in those cases the result will be
                // correct
                newChunkEntities.add(entity);
            }
        }
        newChunk.setInhabitedTime(existingChunk.getInhabitedTime());
        return newChunk;
    }

    /**
     * Merge one surface layer block.
     * 
     * @param existingChunk The chunk from the existing map.
     * @param newChunk The newly generated chunk.
     * @param x The X coordinate of the block to merge.
     * @param y The Y coordinate of the block to merge.
     * @param z The Z coordinate of the block to merge.
     * @param preserveCaves Whether empty blocks from the existing chunk should be preserved.
     */
    private void mergeSurfaceBlock(final Chunk existingChunk, final Chunk newChunk, final int x, final int y, final int z, final boolean preserveCaves) {
        final Material existingMaterial = existingChunk.getMaterial(x, y, z);
        if (preserveCaves && (existingMaterial.veryInsubstantial || (! existingMaterial.natural))) {
            newChunk.setMaterial(x, y, z, existingMaterial);
            newChunk.setSkyLightLevel(x, y, z, existingChunk.getSkyLightLevel(x, y, z));
            newChunk.setBlockLightLevel(x, y, z, existingChunk.getBlockLightLevel(x, y, z));
            if (existingMaterial.tileEntity) {
                copyEntityTileData(existingChunk, newChunk, x, y, z, 0);
            }
        }
    }

    /**
     * Merge one underground block.
     * 
     * @param existingChunk The chunk from the existing map.
     * @param newChunk The newly generated chunk.
     * @param x The X coordinate of the block to merge.
     * @param y The Y coordinate of the block to merge.
     * @param z The Z coordinate of the block to merge.
     */
    private void mergeUndergroundBlock(final Chunk existingChunk, final Chunk newChunk, final int x, final int y, final int z) {
        if (mergeUnderworld) {
            final Material existingMaterial = existingChunk.getMaterial(x, y, z);
            if (UNDERGROUND_MERGE_MATRIX[newChunk.getMaterial(x, y, z).category][existingMaterial.category]) {
                newChunk.setMaterial(x, y, z, existingMaterial);
                newChunk.setSkyLightLevel(x, y, z, existingChunk.getSkyLightLevel(x, y, z));
                newChunk.setBlockLightLevel(x, y, z, existingChunk.getBlockLightLevel(x, y, z));
                if (existingMaterial.tileEntity) {
                    copyEntityTileData(existingChunk, newChunk, x, y, z, 0);
                }
            }
        } else {
            final Material existingMaterial = existingChunk.getMaterial(x, y, z);
            newChunk.setMaterial(x, y, z, existingMaterial);
            newChunk.setSkyLightLevel(x, y, z, existingChunk.getSkyLightLevel(x, y, z));
            newChunk.setBlockLightLevel(x, y, z, existingChunk.getBlockLightLevel(x, y, z));
            if (existingMaterial.tileEntity) {
                copyEntityTileData(existingChunk, newChunk, x, y, z, 0);
            }
        }
    }

    /**
     * Merge one above ground block. Supports a changed surface height by
     * specifying a delta between the Y coordinate of the block to merge in the
     * existing and new chunks. This method assumes that {@code newChunk} will
     * end up in the final map, so will merge the change into that chunk.
     * 
     * <p>Coordinates are in Minecraft coordinate system.
     * 
     * @param existingChunk The chunk from the existing map.
     * @param newChunk The newly generated chunk.
     * @param x The X coordinate of the block to merge.
     * @param y The Y coordinate of the block to merge, in the new chunk.
     * @param z The Z coordinate of the block to merge.
     * @param dy The difference between the Y coordinate in the new chunk and
     *     the Y coordinate of the corresponding block in the existing chunk.
     * @param frost Whether the {@link Frost} layer was applied at the specified
     *     x,z coordinates in the new map.
     */
    private void mergeAboveGroundBlock(final Chunk existingChunk, final Chunk newChunk, final int x, final int y, final int z, final int dy, final boolean frost) {
        final Material existingMaterial = existingChunk.getMaterial(x, y - dy, z);
        final Material newMaterial = newChunk.getMaterial(x, y, z);
        if  (((existingMaterial == AIR) // replace *all* fluids (and ice) from the existing map with fluids (or lack thereof) from the new map
                    || existingMaterial.isNamedOneOf(MC_ICE, MC_WATER, MC_LAVA))

                || (existingMaterial.insubstantial // the existing block is insubstantial and the new block is not (but treat water and lava separately below)
                        && (newMaterial != AIR)
                        && newMaterial.isNotNamedOneOf(MC_WATER, MC_LAVA)
                        && (! newMaterial.insubstantial))

                // the Frost layer has not been applied and the existing block is snow
                || ((! frost) && existingMaterial.isNamed(MC_SNOW))

                // the existing block is insubstantial and the new block would wash it away
                || (newMaterial.isNamedOneOf(MC_WATER) && existingMaterial.insubstantial && existingMaterial.dry)

                // the existing block is insubstantial and the new block would burn it away
                || (newMaterial.isNamedOneOf(MC_LAVA) && existingMaterial.insubstantial)

                // the existing block is an underwater block which would now be above the water
                || ((! existingMaterial.dry) && (! newMaterial.isNamed(MC_WATER)))) {
            // Leave the new block in place
        } else {
            // Copy the existing block over the new block, modifying it if necessary
            if (existingMaterial.isNamed(MC_SNOW) && newMaterial.isNamed(MC_SNOW)) {
                // If both the existing and new blocks are snow, use the highest snow level of the two, to leave smooth snow in the existing map intact
                newChunk.setMaterial(x, y, z, SNOW.withProperty(LAYERS, Math.max(existingMaterial.getProperty(LAYERS), newMaterial.getProperty(LAYERS))));
            } else {
                if (existingMaterial.hasProperty(WATERLOGGED)) {
                    // The block has a waterlogged property; manage it correctly
                    boolean existingMaterialIsWaterLogged = existingMaterial.getProperty(WATERLOGGED);
                    boolean newMaterialIsWater = newMaterial.isNamed(MC_WATER) || newMaterial.getProperty(WATERLOGGED, false);
                    if (existingMaterialIsWaterLogged && (! newMaterialIsWater)) {
                        newChunk.setMaterial(x, y, z, existingMaterial.withProperty(WATERLOGGED, false));
                    } else if ((! existingMaterialIsWaterLogged) && newMaterialIsWater) {
                        newChunk.setMaterial(x, y, z, existingMaterial.withProperty(WATERLOGGED, true));
                    } else {
                        newChunk.setMaterial(x, y, z, existingMaterial);
                    }
                } else {
                    // Just use the existing block as-is
                    newChunk.setMaterial(x, y, z, existingMaterial);
                }
                if (existingMaterial.tileEntity) {
                    copyEntityTileData(existingChunk, newChunk, x, y, z, dy); // TODOMC13 this doesn't seem to be working?
                }
            }
            newChunk.setSkyLightLevel(x, y, z, existingChunk.getSkyLightLevel(  x, y - dy, z));
            newChunk.setBlockLightLevel(x, y, z, existingChunk.getBlockLightLevel(x, y - dy, z));
        }
    }

    // Coordinates are in Minecraft coordinate system
    private void copyEntityTileData(Chunk fromChunk, Chunk toChunk, int x, int y, int z, int dy) {
        int existingBlockDX = fromChunk.getxPos() << 4, existingBlockDZ = fromChunk.getzPos() << 4;
        List<TileEntity> fromEntities = fromChunk.getTileEntities();
        for (TileEntity entity: fromEntities) {
            if ((entity.getY() == (y - dy)) && ((entity.getX() - existingBlockDX) == x) && ((entity.getZ() - existingBlockDZ) == z)) {
                entity.setY(y);
                toChunk.getTileEntities().add(entity);
                return;
            }
        }
    }

    private final File levelDatFile;
    private final ThreadLocal<Map<Material, Integer>> materialCountsRef = ThreadLocal.withInitial(HashMap::new);
    private boolean replaceChunks, mergeUnderworld, clearTrees, clearResources,
        fillCaves, clearVegetation, clearManMadeAboveGround,
        clearManMadeBelowGround;
    private String warnings;
    private int surfaceMergeDepth = 1;
    
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(JavaWorldMerger.class);
    private static final Object TIMING_FILE_LOCK = new Object();
    private static final String EOL = System.getProperty("line.separator");

    // true means copy existing block               Existing map: Air:   Fluid: Insub: Manmd: Resrc: Solid:
    private static final boolean[][] UNDERGROUND_MERGE_MATRIX = {{false, false, true , true , false, false},  // Air in new map
                                                                 {false, false, false, true , false, false},  // Fluids in new map
                                                                 {false, false, false, true , false, false},  // Insubstantial in new map
                                                                 {false, false, false, false, false, false},  // Man-made in new map
                                                                 {true,  true,  true,  true,  false, false},  // Resource in new map
                                                                 {true,  true,  true,  true,  true , false}}; // Natural solid in new map
}