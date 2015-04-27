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
import org.pepsoft.worldpainter.*;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.exporting.*;
import org.pepsoft.worldpainter.layers.*;
import org.pepsoft.worldpainter.util.FileInUseException;
import org.pepsoft.worldpainter.vo.EventVO;

import java.awt.*;
import java.io.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import static org.pepsoft.minecraft.Block.BLOCKS;
import static org.pepsoft.minecraft.Constants.*;
import static org.pepsoft.worldpainter.Constants.*;

/**
 *
 * @author pepijn
 */
public class WorldMerger extends WorldExporter {
    public WorldMerger(World2 world, File levelDatFile) {
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
     * Whether to merge the part of the map <em>above</em> the surface.
     */
    public boolean isMergeOverworld() {
        return mergeOverworld;
    }

    public void setMergeOverworld(final boolean mergeOverworld) {
        this.mergeOverworld = mergeOverworld;
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

    // TODO: support for extra dimensions!
    public void merge(File backupDir, ProgressReceiver progressReceiver) throws IOException, ProgressReceiver.OperationCancelled {
        logger.info("Merging world " + world.getName() + " with map at " + levelDatFile.getParentFile());
        
        // Read existing level.dat file
        Level level = Level.load(levelDatFile);
        
        // Sanity checks
        int existingMaxHeight = level.getMaxHeight();
        if (existingMaxHeight != world.getMaxHeight()) {
            throw new IllegalArgumentException("Level has different max height (" + existingMaxHeight + ") than WorldPainter world (" + world.getMaxHeight() + ")");
        }
        int version = level.getVersion();
        if ((version != SUPPORTED_VERSION_1) && (version != SUPPORTED_VERSION_2)) {
            throw new IllegalArgumentException("Not a supported version: 0x" + Integer.toHexString(version));
        }
        
        // Dimension sanity checks
        Dimension dimension = world.getDimension(0);
        if (existingMaxHeight != dimension.getMaxHeight()) {
            throw new IllegalArgumentException("Dimension " + dimension.getDim() + " has different max height (" + dimension.getMaxHeight() + ") than existing level (" + existingMaxHeight + ")");
        }
        File worldDir = levelDatFile.getParentFile();
        
        // Record start of export
        long start = System.currentTimeMillis();
        
        // Backup existing level
        if (! worldDir.renameTo(backupDir)) {
            throw new FileInUseException("Could not move " + worldDir + " to " + backupDir);
        }
        if (! worldDir.mkdirs()) {
            throw new IOException("Could not create " + worldDir);
        }
        
        // Set the world to the same Minecraft version as the existing map, in
        // case it has changed. This affects the type of chunks created in the
        // first pass
        world.setVersion(version);
        
        // Modify it if necessary and write it to the the new level
        level.setSeed(dimension.getMinecraftSeed());
        Point spawnPoint = world.getSpawnPoint();
        level.setSpawnX(spawnPoint.x);
        level.setSpawnY(Math.max(dimension.getIntHeightAt(spawnPoint), dimension.getWaterLevelAt(spawnPoint)));
        level.setSpawnZ(spawnPoint.y);
        
        // Copy everything that we are not going to generate (this includes the
        // Nether and End dimensions)
        File[] files = backupDir.listFiles();
        for (File file: files) {
            if ((! file.getName().equalsIgnoreCase("level.dat"))
                    && (! file.getName().equalsIgnoreCase("level.dat_old"))
                    && (! file.getName().equalsIgnoreCase("session.lock"))
                    && (! file.getName().equalsIgnoreCase("region"))
                    && (! file.getName().equalsIgnoreCase("maxheight.txt"))
                    && (! file.getName().equalsIgnoreCase("Height.txt"))) {
                if (file.isFile()) {
                    FileUtils.copyFile(file, worldDir);
                } else if (file.isDirectory()) {
                    FileUtils.copyDir(file, worldDir);
                } else {
                    logger.warning("Not copying " + file + "; not a regular file or directory");
                }
            }
        }
        
        level.save(worldDir);
        
        mergeDimension(worldDir, backupDir, dimension, version, progressReceiver);

        // Log an event
        Configuration config = Configuration.getInstance();
        if (config != null) {
            EventVO event = new EventVO(EVENT_KEY_ACTION_MERGE_WORLD).duration(System.currentTimeMillis() - start);
            event.setAttribute(EventVO.ATTRIBUTE_TIMESTAMP, new Date(start));
            event.setAttribute(ATTRIBUTE_KEY_MAX_HEIGHT, world.getMaxHeight());
            event.setAttribute(ATTRIBUTE_KEY_VERSION, world.getVersion());
            event.setAttribute(ATTRIBUTE_KEY_MAP_FEATURES, world.isMapFeatures());
            event.setAttribute(ATTRIBUTE_KEY_GAME_TYPE, world.getGameType());
            event.setAttribute(ATTRIBUTE_KEY_ALLOW_CHEATS, world.isAllowCheats());
            event.setAttribute(ATTRIBUTE_KEY_GENERATOR, world.getGenerator().name());
            if ((world.getVersion() == SUPPORTED_VERSION_2) && (world.getGenerator() == Generator.FLAT)) {
                event.setAttribute(ATTRIBUTE_KEY_GENERATOR_OPTIONS, world.getGeneratorOptions());
            }
            dimension = world.getDimension(0);
            event.setAttribute(ATTRIBUTE_KEY_TILES, dimension.getTiles().size());
            logLayers(dimension, event, "");
            if (world.getImportedFrom() == null) {
                event.setAttribute(ATTRIBUTE_KEY_IMPORTED_WORLD, false);
            }
            config.logEvent(event);
        }
    }

    public String getWarnings() {
        return warnings;
    }

    private void mergeDimension(final File worldDir, File backupWorldDir, final Dimension dimension, final int version, ProgressReceiver progressReceiver) throws ProgressReceiver.OperationCancelled, IOException {
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
            final Map<Layer, LayerExporter<Layer>> exporters = new HashMap<Layer, LayerExporter<Layer>>();
            Set<Layer> allLayers = dimension.getAllLayers(false);
            allLayers.addAll(dimension.getMinimumLayers());
            // If there are combined layers, apply them and gather any newly
            // added layers, recursively
            boolean done;
            do {
                done = true;
                for (Layer layer: new HashSet<Layer>(allLayers)) {
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
                LayerExporter<Layer> exporter = (LayerExporter<Layer>) layer.getExporter();
                if (exporter != null) {
                    exporter.setSettings(dimension.getLayerSettings(layer));
                    exporters.put(layer, exporter);
                }
            }

            // Sort tiles into regions
            int lowestRegionX = Integer.MAX_VALUE, highestRegionX = Integer.MIN_VALUE, lowestRegionZ = Integer.MAX_VALUE, highestRegionZ = Integer.MIN_VALUE;
            Map<Point, Map<Point, Tile>> tilesByRegion = new HashMap<Point, Map<Point, Tile>>();
            final boolean tileSelection = selectedTiles != null;
            if (tileSelection) {
                // Sanity check
                assert selectedDimension == dimension.getDim();
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
                    Map<Point, Tile> tilesForRegion = tilesByRegion.get(regionCoords);
                    if (tilesForRegion == null) {
                        tilesForRegion = new HashMap<Point, Tile>();
                        tilesByRegion.put(regionCoords, tilesForRegion);
                    }
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
                    Map<Point, Tile> tilesForRegion = tilesByRegion.get(regionCoords);
                    if (tilesForRegion == null) {
                        tilesForRegion = new HashMap<Point, Tile>();
                        tilesByRegion.put(regionCoords, tilesForRegion);
                    }
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
            final Pattern regionFilePattern = (version == SUPPORTED_VERSION_2)
                ? Pattern.compile("r\\.-?\\d+\\.-?\\d+\\.mca")
                : Pattern.compile("r\\.-?\\d+\\.-?\\d+\\.mcr");
            File[] existingRegionFiles = backupRegionDir.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return regionFilePattern.matcher(name).matches();
                }
            });
            Map<Point, File> existingRegions = new HashMap<Point, File>();
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
            final Set<Point> allRegionCoords = new HashSet<Point>();
            allRegionCoords.addAll(tilesByRegion.keySet());
            allRegionCoords.addAll(existingRegions.keySet());

            // Sort the regions to export the first two rows together, and then
            // row by row, to get the optimum tempo of performing fixups
            List<Point> sortedRegions = new ArrayList<Point>(allRegionCoords.size());
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
            final WorldPainterChunkFactory chunkFactory = new WorldPainterChunkFactory(dimension, exporters, world.getVersion(), world.getMaxHeight());

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

            final Map<Point, List<Fixup> >fixups = new HashMap<Point, List<Fixup>>();
            final Set<Point> exportedRegions = new HashSet<Point>();
            ExecutorService executor = Executors.newFixedThreadPool(threads);
            final ParallelProgressManager parallelProgressManager = (progressReceiver != null) ? new ParallelProgressManager(progressReceiver, allRegionCoords.size()) : null;
            try {
                // Merge each individual region
                for (final Point regionCoords: sortedRegions) {
                    if (existingRegions.containsKey(regionCoords)) {
                        if (tilesByRegion.containsKey(regionCoords)) {
                            // Region exists in new and existing maps; merge it
                            final Map<Point, Tile> tiles = tilesByRegion.get(regionCoords);
                            executor.execute(new Runnable() {
                                @Override
                                public void run() {
                                    ProgressReceiver progressReceiver = (parallelProgressManager != null) ? parallelProgressManager.createProgressReceiver() : null;
                                    if (progressReceiver != null) {
                                        try {
                                            progressReceiver.checkForCancellation();
                                        } catch (ProgressReceiver.OperationCancelled e) {
                                            return;
                                        }
                                    }
                                    try {
                                        List<Fixup> regionFixups = new ArrayList<Fixup>();
                                        WorldRegion minecraftWorld = new WorldRegion(regionCoords.x, regionCoords.y, dimension.getMaxHeight(), version);
                                        try {
                                            String regionWarnings = mergeRegion(minecraftWorld, backupRegionDir, dimension, regionCoords, tiles, tileSelection, exporters, chunkFactory, regionFixups, (progressReceiver != null) ? new SubProgressReceiver(progressReceiver, 0.0f, 0.9f) : null);
                                            if (regionWarnings != null) {
                                                if (warnings == null) {
                                                    warnings = regionWarnings;
                                                } else {
                                                    warnings = warnings + regionWarnings;
                                                }
                                            }
                                            if (logger.isLoggable(java.util.logging.Level.FINE)) {
                                                logger.fine("Merged region " + regionCoords.x + "," + regionCoords.y);
                                            }
                                        } finally {
                                            minecraftWorld.save(dimensionDir);
                                        }
                                        synchronized (fixups) {
                                            if (! regionFixups.isEmpty()) {
                                                fixups.put(new Point(regionCoords.x, regionCoords.y), regionFixups);
                                            }
                                            exportedRegions.add(regionCoords);
                                        }
                                        // Apply all fixups which can be applied because
                                        // all surrounding regions have been exported
                                        // (or are not going to be), but only if another
                                        // thread is not already doing it
                                        if (performingFixups.tryAcquire()) {
                                            try {
                                                Map<Point, List<Fixup>> myFixups = new HashMap<Point, List<Fixup>>();
                                                synchronized (fixups) {
                                                    for (Iterator<Map.Entry<Point, List<Fixup>>> i = fixups.entrySet().iterator(); i.hasNext(); ) {
                                                        Map.Entry<Point, List<Fixup>> entry = i.next();
                                                        Point fixupRegionCoords = entry.getKey();
                                                        if (isReadyForFixups(allRegionCoords, exportedRegions, fixupRegionCoords)) {
                                                            myFixups.put(fixupRegionCoords, entry.getValue());
                                                            i.remove();
                                                        }
                                                    }
                                                }
                                                if (! myFixups.isEmpty()) {
                                                    performFixups(worldDir, dimension, version, (progressReceiver != null) ? new SubProgressReceiver(progressReceiver, 0.9f, 0.1f) : null, myFixups);
                                                }
                                            } finally {
                                                performingFixups.release();
                                            }
                                        }
                                    } catch (Throwable t) {
                                        if (progressReceiver != null) {
                                            progressReceiver.exceptionThrown(t);
                                        } else {
                                            logger.log(java.util.logging.Level.SEVERE, "Exception while exporting region", t);
                                        }
                                    }
                                }
                            });
                        } else {
                            // Region only exists in existing world. Copy it to the new
                            // world
                            ProgressReceiver subProgressReceiver = (parallelProgressManager != null) ? parallelProgressManager.createProgressReceiver() : null;
                            FileUtils.copyFile(existingRegions.get(regionCoords), regionDir, subProgressReceiver);
                            synchronized (fixups) {
                                exportedRegions.add(regionCoords);
                            }
                            if (logger.isLoggable(java.util.logging.Level.FINE)) {
                                logger.fine("Copied region " + regionCoords.x + "," + regionCoords.y);
                            }
                        }
                    } else {
                        // Region only exists in new world. Create it as new
                        executor.execute(new Runnable() {
                            @Override
                            public void run() {
                                ProgressReceiver progressReceiver = (parallelProgressManager != null) ? parallelProgressManager.createProgressReceiver() : null;
                                if (progressReceiver != null) {
                                    try {
                                        progressReceiver.checkForCancellation();
                                    } catch (ProgressReceiver.OperationCancelled e) {
                                        return;
                                    }
                                }
                                try {
                                    WorldRegion minecraftWorld = new WorldRegion(regionCoords.x, regionCoords.y, dimension.getMaxHeight(), version);
                                    ExportResults exportResults = null;
                                    try {
                                        exportResults = exportRegion(minecraftWorld, dimension, null, regionCoords, tileSelection, exporters, null, chunkFactory, null, (progressReceiver != null) ? new SubProgressReceiver(progressReceiver, 0.9f, 0.1f) : null);
                                        if (logger.isLoggable(java.util.logging.Level.FINE)) {
                                            logger.fine("Generated region " + regionCoords.x + "," + regionCoords.y);
                                        }
                                    } finally {
                                        if ((exportResults != null) && exportResults.chunksGenerated) {
                                            minecraftWorld.save(dimensionDir);
                                        }
                                    }
                                    synchronized (fixups) {
                                        if ((exportResults.fixups != null) && (! exportResults.fixups.isEmpty())) {
                                            fixups.put(new Point(regionCoords.x, regionCoords.y), exportResults.fixups);
                                        }
                                        exportedRegions.add(regionCoords);
                                    }
                                    // Apply all fixups which can be applied because
                                    // all surrounding regions have been exported
                                    // (or are not going to be), but only if another
                                    // thread is not already doing it
                                    if (performingFixups.tryAcquire()) {
                                        try {
                                            Map<Point, List<Fixup>> myFixups = new HashMap<Point, List<Fixup>>();
                                            synchronized (fixups) {
                                                for (Iterator<Map.Entry<Point, List<Fixup>>> i = fixups.entrySet().iterator(); i.hasNext(); ) {
                                                    Map.Entry<Point, List<Fixup>> entry = i.next();
                                                    Point fixupRegionCoords = entry.getKey();
                                                    if (isReadyForFixups(allRegionCoords, exportedRegions, fixupRegionCoords)) {
                                                        myFixups.put(fixupRegionCoords, entry.getValue());
                                                        i.remove();
                                                    }
                                                }
                                            }
                                            if (! myFixups.isEmpty()) {
                                                performFixups(worldDir, dimension, version, (progressReceiver != null) ? new SubProgressReceiver(progressReceiver, 0.9f, 0.1f) : null, myFixups);
                                            }
                                        } finally {
                                            performingFixups.release();
                                        }
                                    }
                                } catch (Throwable t) {
                                    if (progressReceiver != null) {
                                        progressReceiver.exceptionThrown(t);
                                    } else {
                                        logger.log(java.util.logging.Level.SEVERE, "Exception while exporting region", t);
                                    }
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
            // performing fixups and thread B added new ones and then quit
            synchronized (fixups) {
                if (! fixups.isEmpty()) {
                    if (progressReceiver != null) {
                        progressReceiver.setMessage("doing remaining fixups for " + dimension.getName());
                        progressReceiver.reset();
                    }
                    performFixups(worldDir, dimension, version, (progressReceiver != null) ? new SubProgressReceiver(progressReceiver, 0.9f, 0.1f) : null, fixups);
                }
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
    
    private String mergeRegion(MinecraftWorld minecraftWorld, File oldRegionDir, Dimension dimension, Point regionCoords, Map<Point, Tile> tiles, boolean tileSelection, Map<Layer, LayerExporter<Layer>> exporters, ChunkFactory chunkFactory, List<Fixup> fixups, ProgressReceiver progressReceiver) throws IOException, ProgressReceiver.OperationCancelled {
        Set<Layer> allLayers = new HashSet<Layer>();
        for (Tile tile: tiles.values()) {
            allLayers.addAll(tile.getLayers());
        }
        
        // Add layers that have been configured to be applied everywhere
        Set<Layer> minimumLayers = dimension.getMinimumLayers();
        allLayers.addAll(minimumLayers);
        
        List<Layer> secondaryPassLayers = new ArrayList<Layer>();
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
            List<Fixup> myFixups = secondPass(secondaryPassLayers, minimumLayers, dimension, minecraftWorld, exporters, tiles.values(), regionCoords, (progressReceiver != null) ? new SubProgressReceiver(progressReceiver, 0.3f, 0.1f) : null);
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
            postProcess(minecraftWorld, regionCoords, (progressReceiver != null) ? new SubProgressReceiver(progressReceiver, 0.65f, 0.1f) : null);

            // Third pass. Calculate lighting
            long t5 = System.currentTimeMillis();
            lightingPass(minecraftWorld, regionCoords, (progressReceiver != null) ? new SubProgressReceiver(progressReceiver, 0.75f, 0.25f) : null);
            long t6 = System.currentTimeMillis();
            if ("true".equalsIgnoreCase(System.getProperty("org.pepsoft.worldpainter.devMode"))) {
                String timingMessage = (t2 - t1) + ", " + (t3 - t2) + ", " + (t4 - t3) + ", " + (t5 - t4) + ", " + (t6 - t5) + ", " + (t6 - t1);
//                System.out.println("Merge timing: " + timingMessage);
                synchronized (TIMING_FILE_LOCK) {
                    PrintWriter out = new PrintWriter(new FileOutputStream("mergetimings.csv", true));
                    try {
                        out.println(timingMessage);
                    } finally {
                        out.close();
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
        // Read existing level.dat file
        Level level = Level.load(levelDatFile);
        
        // Sanity checks
        int existingMaxHeight = level.getMaxHeight();
        if (existingMaxHeight != world.getMaxHeight()) {
            throw new IllegalArgumentException("Level has different max height (" + existingMaxHeight + ") than WorldPainter world (" + world.getMaxHeight() + ")");
        }
        int version = level.getVersion();
        if (version != SUPPORTED_VERSION_2) {
            throw new IllegalArgumentException("Not a supported version: 0x" + Integer.toHexString(version));
        }
        
        // Dimension sanity checks
        Dimension dimension = world.getDimension(0);
        if (existingMaxHeight != dimension.getMaxHeight()) {
            throw new IllegalArgumentException("Dimension " + dimension.getDim() + " has different max height (" + dimension.getMaxHeight() + ") than existing level (" + existingMaxHeight + ")");
        }
        File worldDir = levelDatFile.getParentFile();
        
        // Backup existing level
        if (! worldDir.renameTo(backupDir)) {
            throw new FileInUseException("Could not move " + worldDir + " to " + backupDir);
        }
        if (! worldDir.mkdirs()) {
            throw new IOException("Could not create " + worldDir);
        }
        
        // Set the world to the same Minecraft version as the existing map, in
        // case it has changed. This affects the type of chunks created in the
        // first pass
        world.setVersion(version);
        
        // Modify it if necessary and write it to the the new level
        level.setSeed(dimension.getMinecraftSeed());
        
        // Copy everything that we are not going to generate (this includes the
        // Nether and End dimensions)
        File[] files = backupDir.listFiles();
        for (File file: files) {
            if ((! file.getName().equalsIgnoreCase("level.dat"))
                    && (! file.getName().equalsIgnoreCase("level.dat_old"))
                    && (! file.getName().equalsIgnoreCase("session.lock"))
                    && (! file.getName().equalsIgnoreCase("region"))
                    && (! file.getName().equalsIgnoreCase("maxheight.txt"))
                    && (! file.getName().equalsIgnoreCase("Height.txt"))) {
                if (file.isFile()) {
                    FileUtils.copyFile(file, worldDir);
                } else if (file.isDirectory()) {
                    FileUtils.copyDir(file, worldDir);
                } else {
                    logger.warning("Not copying " + file + "; not a regular file or directory");
                }
            }
        }

        level.save(worldDir);
        
        // Process all chunks and copy just the biomes
        if (progressReceiver != null) {
            progressReceiver.setMessage("merging biomes");
        }
        // Find all the region files of the existing level
        File oldRegionDir = new File(backupDir, "region");
        final Pattern regionFilePattern = Pattern.compile("r\\.-?\\d+\\.-?\\d+\\.mca");
        File[] oldRegionFiles = oldRegionDir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return regionFilePattern.matcher(name).matches();
            }
        });

        // Process each region file, copying every chunk unmodified, except
        // for the biomes
        int totalChunkCount = oldRegionFiles.length * 32 * 32, chunkCount = 0;
        File newRegionDir = new File(worldDir, "region");
        newRegionDir.mkdirs();
        for (File file: oldRegionFiles) {
            RegionFile oldRegion = new RegionFile(file);
            try {
                String[] parts = file.getName().split("\\.");
                int regionX = Integer.parseInt(parts[1]);
                int regionZ = Integer.parseInt(parts[2]);
                File newRegionFile = new File(newRegionDir, "r." + regionX + "." + regionZ + ".mca");
                RegionFile newRegion = new RegionFile(newRegionFile);
                try {
                    for (int x = 0; x < 32; x++) {
                        for (int z = 0; z < 32; z++) {
                            if (oldRegion.containsChunk(x, z)) {
                                ChunkImpl2 chunk;
                                NBTInputStream in = new NBTInputStream(oldRegion.getChunkDataInputStream(x, z));
                                try {
                                    CompoundTag tag = (CompoundTag) in.readTag();
                                    chunk = new ChunkImpl2(tag, level.getMaxHeight());
                                } finally {
                                    in.close();
                                }
                                int chunkX = chunk.getxPos(), chunkZ = chunk.getzPos();
                                for (int xx = 0; xx < 16; xx++) {
                                    for (int zz = 0; zz < 16; zz++) {
                                        chunk.setBiome(xx, zz, dimension.getLayerValueAt(Biome.INSTANCE, (chunkX << 4) | xx, (chunkZ << 4) | zz));
                                    }
                                }
                                NBTOutputStream out = new NBTOutputStream(newRegion.getChunkDataOutputStream(x, z));
                                try {
                                    out.writeTag(chunk.toNBT());
                                } finally {
                                    out.close();
                                }
                            }
                            chunkCount++;
                            if (progressReceiver != null) {
                                progressReceiver.setProgress((float) chunkCount / totalChunkCount);
                            }
                        }
                    }
                } finally {
                    newRegion.close();
                }
            } finally {
                oldRegion.close();
            }
        }
    }
    
    private String thirdPass(MinecraftWorld minecraftWorld, File oldRegionDir, Dimension dimension, Point regionCoords, ProgressReceiver progressReceiver) throws IOException, ProgressReceiver.OperationCancelled {
        int lowestChunkX = (regionCoords.x << 5) - 1;
        int highestChunkX = (regionCoords.x << 5) + 32;
        int lowestChunkY = (regionCoords.y << 5) - 1;
        int highestChunkY = (regionCoords.y << 5) + 32;
        int version = dimension.getWorld().getVersion();
        int maxHeight = dimension.getMaxHeight();
        Map<Point, RegionFile> regionFiles = new HashMap<Point, RegionFile>();
        Set<Point> damagedRegions = new HashSet<Point>();
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
                        File file = new File(oldRegionDir, "r." + regionX + "." + regionY + ((version == SUPPORTED_VERSION_2) ? ".mca" : ".mcr"));
                        try {
                            regionFile = new RegionFile(file);
                            regionFiles.put(coords, regionFile);
                        } catch (IOException e) {
                            reportBuilder.append("I/O error while opening region file " + file + " (message: \"" + e.getMessage() + "\"); skipping region" + EOL);
                            logger.log(java.util.logging.Level.SEVERE, "I/O error while opening region file " + file + "; skipping region", e);
                            damagedRegions.add(coords);
                            continue;
                        }
                    }
                    int chunkXInRegion = chunkX & 0x1f;
                    int chunkYInRegion = chunkY & 0x1f;
                    if (regionFile.containsChunk(chunkXInRegion, chunkYInRegion)) {
                        Tag tag;
                        try {
                            DataInputStream chunkData = regionFile.getChunkDataInputStream(chunkXInRegion, chunkYInRegion);
                            if (chunkData == null) {
                                // This should never happen, since we checked with
                                // containsChunk(), but in practice it does. Perhaps
                                // corrupted data?
                                reportBuilder.append("Missing chunk data in existing map for chunk " + chunkXInRegion + ", " + chunkYInRegion + " in " + regionFile + "; skipping chunk" + EOL);
                                logger.warning("Missing chunk data in existing map for chunk " + chunkXInRegion + ", " + chunkYInRegion + " in " + regionFile + "; skipping chunk");
                                continue;
                            }
                            NBTInputStream in = new NBTInputStream(chunkData);
                            try {
                                tag = in.readTag();
                            } finally {
                                in.close();
                            }
                        } catch (IOException e) {
                            reportBuilder.append("I/O error while reading chunk in existing map " + chunkXInRegion + ", " + chunkYInRegion + " from file " + regionFile + " (message: \"" + e.getMessage() + "\"); skipping chunk" + EOL);
                            logger.log(java.util.logging.Level.SEVERE, "I/O error while reading chunk in existing map " + chunkXInRegion + ", " + chunkYInRegion + " from file " + regionFile + "; skipping chunk", e);
                            continue;
                        } catch (IllegalArgumentException e) {
                            reportBuilder.append("Illegal argument exception while reading chunk in existing map " + chunkXInRegion + ", " + chunkYInRegion + " from file " + regionFile + " (message: \"" + e.getMessage() + "\"); skipping chunk" + EOL);
                            logger.log(java.util.logging.Level.SEVERE, "Illegal argument exception while reading chunk in existing map " + chunkXInRegion + ", " + chunkYInRegion + " from file " + regionFile + "; skipping chunk", e);
                            continue;
                        }
                        Chunk existingChunk = (version == SUPPORTED_VERSION_2)
                                ? new ChunkImpl2((CompoundTag) tag, maxHeight)
                                : new ChunkImpl((CompoundTag) tag, maxHeight);
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
                                logger.log(java.util.logging.Level.SEVERE, "Null pointer exception while reading chunk in existing map " + chunkXInRegion + ", " + chunkYInRegion + " from file " + regionFile + "; skipping chunk", e);
                                continue;
                            } catch (ArrayIndexOutOfBoundsException e) {
                                reportBuilder.append("Array index out of bounds while reading chunk in existing map " + chunkXInRegion + ", " + chunkYInRegion + " from file " + regionFile + " (message: \"" + e.getMessage() + "\"); skipping chunk" + EOL);
                                logger.log(java.util.logging.Level.SEVERE, "Array index out of bounds while reading chunk in existing map " + chunkXInRegion + ", " + chunkYInRegion + " from file " + regionFile + "; skipping chunk", e);
                                continue;
                            }
                        } else {
                            // Chunk exists in existing world, but not in new
                            // one, copy old to new
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
                    final Block existingBlock = BLOCKS[existingChunk.getBlockType(x, z, y)];
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
                            final int newBlockType = findMostPrevalentSolidSurroundingMaterial(existingChunk, x, y, z);
                            if (newBlockType == BLK_AIR) {
                                setToAir(existingChunk, x, y, z);
                            } else {
                                existingChunk.setMaterial(x, z, y, Material.get(newBlockType));
                                existingChunk.setSkyLightLevel(x, z, y, 0);
                                existingChunk.setBlockLightLevel(x, z, y, 0);
                            }
                        }
                        if (fillCaves && existingBlock.veryInsubstantial) {
                            final int newBlockType = findMostPrevalentSolidSurroundingMaterial(existingChunk, x, y, z);
                            if (newBlockType == BLK_AIR) {
                                existingChunk.setMaterial(x, z, y, Material.STONE);
                            } else {
                                existingChunk.setMaterial(x, z, y, Material.get(newBlockType));
                            }
                            existingChunk.setSkyLightLevel(x, z, y, 0);
                            existingChunk.setBlockLightLevel(x, z, y, 0);
                        } else if (clearResources && existingBlock.resource) {
                            if (existingBlock.id == BLK_QUARTZ_ORE) {
                                existingChunk.setMaterial(x, z, y, Material.NETHERRACK);
                            } else {
                                existingChunk.setMaterial(x, z, y, Material.STONE);
                            }
                        }
                    }
                }
            }
        }
    }

    private void setToAir(final Chunk chunk, final int x, final int y, final int z) {
        chunk.setMaterial(x, z, y, Material.AIR);
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
    private int findMostPrevalentSolidSurroundingMaterial(Chunk existingChunk, int x, int y, int z) {
        int[] histogram = histogramRef.get();
        if (histogram == null) {
            histogram = new int[256];
            histogramRef.set(histogram);
        } else {
            Arrays.fill(histogram, 0);
        }
        int highestBlockType = -1;
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
                    int blockType = existingChunk.getBlockType(xx, zz, yy);
                    if (SOLID_BLOCKS.get(blockType)) {
                        histogram[blockType]++;
                        if (histogram[blockType] > highestBlockType) {
                            highestBlockType = blockType;
                        }
                    }
                }
            }
        }
        if (highestBlockType > -1) {
            return highestBlockType;
        } else {
            return BLK_AIR;
        }
    }
    
    private String copyAllChunks(MinecraftWorld minecraftWorld, File oldRegionDir, Dimension dimension, Point regionCoords, ProgressReceiver progressReceiver) throws IOException, ProgressReceiver.OperationCancelled {
        int lowestChunkX = regionCoords.x << 5;
        int highestChunkX = (regionCoords.x << 5) + 31;
        int lowestChunkY = regionCoords.y << 5;
        int highestChunkY = (regionCoords.y << 5) + 31;
        int version = dimension.getWorld().getVersion();
        int maxHeight = dimension.getMaxHeight();
        Map<Point, RegionFile> regionFiles = new HashMap<Point, RegionFile>();
        Set<Point> damagedRegions = new HashSet<Point>();
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
                        File file = new File(oldRegionDir, "r." + regionX + "." + regionY + ((version == SUPPORTED_VERSION_2) ? ".mca" : ".mcr"));
                        try {
                            regionFile = new RegionFile(file);
                            regionFiles.put(coords, regionFile);
                        } catch (IOException e) {
                            reportBuilder.append("I/O error while opening region file " + file + " (message: \"" + e.getMessage() + "\"); skipping region" + EOL);
                            logger.log(java.util.logging.Level.SEVERE, "I/O error while opening region file " + file + "; skipping region", e);
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
                                // with containsChunk(), but in practice it
                                // does. Perhaps corrupted data?
                                reportBuilder.append("Missing chunk data for chunk " + chunkXInRegion + ", " + chunkYInRegion + " in " + regionFile + "; skipping chunk" + EOL);
                                logger.warning("Missing chunk data for chunk " + chunkXInRegion + ", " + chunkYInRegion + " in " + regionFile + "; skipping chunk");
                                continue;
                            }
                            NBTInputStream in = new NBTInputStream(chunkData);
                            try {
                                tag = in.readTag();
                            } finally {
                                in.close();
                            }
                        } catch (IOException e) {
                            reportBuilder.append("I/O error while reading chunk " + chunkXInRegion + ", " + chunkYInRegion + " from file " + regionFile + " (message: \"" + e.getMessage() + "\"); skipping chunk" + EOL);
                            logger.log(java.util.logging.Level.SEVERE, "I/O error while reading chunk " + chunkXInRegion + ", " + chunkYInRegion + " from file " + regionFile + "; skipping chunk", e);
                            continue;
                        } catch (IllegalArgumentException e) {
                            reportBuilder.append("Illegal argument exception while reading chunk " + chunkXInRegion + ", " + chunkYInRegion + " from file " + regionFile + " (message: \"" + e.getMessage() + "\"); skipping chunk" + EOL);
                            logger.log(java.util.logging.Level.SEVERE, "Illegal argument exception while reading chunk " + chunkXInRegion + ", " + chunkYInRegion + " from file " + regionFile + "; skipping chunk", e);
                            continue;
                        }
                        Chunk existingChunk = (version == SUPPORTED_VERSION_1)
                            ? new ChunkImpl((CompoundTag) tag, maxHeight)
                            : new ChunkImpl2((CompoundTag) tag, maxHeight);
                        minecraftWorld.addChunk(existingChunk);
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
        int maxY = existingChunk.getMaxHeight() - 1;
        int chunkX = existingChunk.getxPos() << 4, chunkZ = existingChunk.getzPos() << 4;
        List<Entity> newChunkEntities = newChunk.getEntities();
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                if (dimension.getBitLayerValueAt(org.pepsoft.worldpainter.layers.Void.INSTANCE, chunkX | x, chunkZ | z)) {
                    // Void. Just empty the entire column
                    for (int y = 0; y <= maxY; y++) {
                        newChunk.setMaterial(x, y, z, Material.AIR);
                        newChunk.setBlockLightLevel(x, y, z, 0);
                        newChunk.setSkyLightLevel(x, y, z, 15);
                    }
                } else {
                    final int newHeight = dimension.getIntHeightAt(chunkX | x, chunkZ | z);
                    final boolean frost = dimension.getBitLayerValueAt(Frost.INSTANCE, chunkX | x, chunkZ | z);
                    int oldHeight = 0;
                    for (int y = maxY; y >= 0; y--) {
                        int oldBlockType = existingChunk.getBlockType(x, y, z);
                        if (BLOCKS[oldBlockType].terrain) {
                            // Terrain found
                            oldHeight = y;
                            break;
                        }
                    }
                    final int dy = newHeight - oldHeight;
                    if (dy > 0) {
                        // Terrain has been raised
                        // Copy underground portion from existing chunk
                        for (int y = 0; y <= oldHeight; y++) {
                            mergeUndergroundBlock(existingChunk, newChunk, x, y, z);
                        }
                        // Copy above ground portion from existing chunk, raised by
                        // the appropriate amount
                        for (int y = newHeight + 1; y <= maxY; y++) {
                            mergeSurfaceBlock(existingChunk, newChunk, x, y, z, dy, frost);
                        }
                        newChunk.setHeight(x, z, Math.min(existingChunk.getHeight(x, z) + dy, maxY));
                    } else if (dy < 0) {
                        // Terrain has been lowered
                        // Copy underground portion from existing chunk
                        for (int y = 0; y <= (newHeight - 1); y++) {
                            mergeUndergroundBlock(existingChunk, newChunk, x, y, z);
                        }
                        // If the new ground height block is insubstantial in the
                        // existing chunk, and there is nothing substantial on the
                        // block in the new or existing chunks, remove it, so as not
                        // to create a weird one block layer of blocks over newly
                        // opened up voids such as caves, chasms, abandoned mines,
                        // etc.
                        int mergeStartHeight = newHeight + 1;
                        final int existingBlockType = existingChunk.getBlockType(x, newHeight, z);
                        if ((existingBlockType == BLK_AIR) || BLOCKS[existingBlockType].insubstantial) {
                            int existingBlockAboveType = (newHeight < maxY) ? existingChunk.getBlockType(x, newHeight + 1, z) : BLK_AIR;
                            int newBlockAboveType = (((newHeight - dy) >= -1) && ((newHeight - dy) < maxY)) ? newChunk.getBlockType(x, newHeight + 1 - dy, z) : BLK_AIR;
                            if (((newBlockAboveType == BLK_AIR) || BLOCKS[newBlockAboveType].insubstantial) && ((existingBlockAboveType == BLK_AIR) || BLOCKS[existingBlockAboveType].insubstantial)) {
                                newChunk.setBlockType(x, newHeight, z, BLK_AIR);
                                newChunk.setDataValue(x, newHeight, z, 0);
                                newChunk.setSkyLightLevel(x, newHeight, z, 0);
                                newChunk.setBlockLightLevel(x, newHeight, z, 0);
                                if (newHeight < maxY) {
                                    // Also set the block above to air, to avoid
                                    // floating flowers, tall grass, etc.
                                    newChunk.setBlockType(x, newHeight + 1, z, BLK_AIR);
                                    newChunk.setDataValue(x, newHeight + 1, z, 0);
                                    newChunk.setSkyLightLevel(x, newHeight + 1, z, 0);
                                    newChunk.setBlockLightLevel(x, newHeight + 1, z, 0);
                                    mergeStartHeight = newHeight + 2;
                                }
                            }
                        }
                        // Copy above ground portion from existing chunk, lowered by
                        // the appropriate amount
                        for (int y = mergeStartHeight; y <= (maxY + dy); y++) {
                            mergeSurfaceBlock(existingChunk, newChunk, x, y, z, dy, frost);
                        }
                        newChunk.setHeight(x, z, Math.min(existingChunk.getHeight(x, z) + dy, maxY));
                    } else {
                        // Terrain height has not changed. Copy everything from the
                        // existing chunk, except the top layer of the terrain.
                        for (int y = 0; y <= (newHeight - 1); y++) {
                            mergeUndergroundBlock(existingChunk, newChunk, x, y, z);
                        }
                        for (int y = newHeight + 1; y <= maxY; y++) {
                            mergeSurfaceBlock(existingChunk, newChunk, x, y, z, 0, frost);
                        }
                    }
                    // Tilled earth is imported as dirt, so make sure to leave
                    // it intact
                    if ((newChunk.getBlockType(x, newHeight, z) == BLK_DIRT) && (existingChunk.getBlockType(x, oldHeight, z) == BLK_TILLED_DIRT)) {
                        newChunk.setMaterial(x, newHeight, z, existingChunk.getMaterial(x, oldHeight, z));
                    }
                    final int blockX = chunkX + x, blockZ = chunkZ + z;
                    for (Entity entity: existingChunk.getEntities()) {
                        double[] pos = entity.getPos();
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
            double[] pos = entity.getPos();
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

    private void mergeUndergroundBlock(final Chunk existingChunk, final Chunk newChunk, final int x, final int y, final int z) {
        int existingBlockType = existingChunk.getBlockType(x, y, z);
        newChunk.setBlockType(x, y, z, existingBlockType);
        newChunk.setDataValue(x, y, z, existingChunk.getDataValue(x, y, z));
        newChunk.setSkyLightLevel(x, y, z, existingChunk.getSkyLightLevel(x, y, z));
        newChunk.setBlockLightLevel(x, y, z, existingChunk.getBlockLightLevel(x, y, z));
        if (BLOCKS[existingBlockType].tileEntity) {
            copyEntityTileData(existingChunk, newChunk, x, y, z, 0);
        }
    }

    // Coordinates are in Minecraft coordinate system
    private void mergeSurfaceBlock(Chunk existingChunk, Chunk newChunk, int x, int y, int z, int dy, boolean frost) {
        int existingBlockType = existingChunk.getBlockType(x, y - dy, z);
        int newBlockType = newChunk.getBlockType(x, y, z);
        int mergedBlockType, mergedDataValue, mergedSkylightLevel, mergedBlockLightLevel;
        if ((existingBlockType == BLK_AIR) // replace *all* fluids (and ice) from the existing map with fluids (or lack thereof) from the new map
                || (existingBlockType == BLK_ICE)
                || (existingBlockType == BLK_WATER)
                || (existingBlockType == BLK_STATIONARY_WATER)
                || (existingBlockType == BLK_LAVA)
                || (existingBlockType == BLK_STATIONARY_LAVA)) {
            mergedBlockType = newBlockType;
//            if (mergedBlockType == BLK_STATIONARY_WATER) {
//                mergedBlockType = BLK_WATER;
//            }
            mergedDataValue = newChunk.getDataValue(x, y, z);
            mergedBlockLightLevel = newChunk.getBlockLightLevel(x, y, z);
            mergedSkylightLevel = newChunk.getSkyLightLevel(x, y, z);
        } else if (((BLOCKS[existingBlockType].insubstantial // the existing block is insubstantial and the new block is not
                        && (newBlockType != BLK_AIR)
                        && (! BLOCKS[newBlockType].insubstantial))
                    && (! (frost // the existing block is not snow or the Frost layer has not been applied to the current column or the new block is solid
                        && (existingBlockType == BLK_SNOW)
                        && ((newBlockType == BLK_AIR)
                            || BLOCKS[newBlockType].insubstantial))))

                // the Frost layer has not been applied and the existing block is snow
                || ((! frost)
                    && (existingBlockType == BLK_SNOW))

                // the existing block is insubstantial and the new block is a fluid which would burn it or wash it away
                || (((newBlockType == BLK_WATER) || (newBlockType == BLK_STATIONARY_WATER) || (newBlockType == BLK_LAVA) || (newBlockType == BLK_STATIONARY_LAVA))
                    && BLOCKS[existingBlockType].insubstantial)) {
            mergedBlockType       = newBlockType;
            mergedDataValue       = newChunk.getDataValue(x, y, z);
            mergedSkylightLevel   = newChunk.getSkyLightLevel(x, y, z);
            mergedBlockLightLevel = newChunk.getBlockLightLevel(x, y, z);
        } else {
            mergedBlockType = existingBlockType;
            if ((existingBlockType == BLK_SNOW) && (newBlockType == BLK_SNOW)) {
                // If both the existing and new blocks are snow, use the highest snow level of the two, to leave smooth snow in the existing map intact
                mergedDataValue = Math.max(existingChunk.getDataValue(x, y - dy, z), newChunk.getDataValue(x, y, z));
            } else {
                mergedDataValue = existingChunk.getDataValue(x, y - dy, z);
            }
            mergedSkylightLevel   = existingChunk.getSkyLightLevel(  x, y - dy, z);
            mergedBlockLightLevel = existingChunk.getBlockLightLevel(x, y - dy, z);
            if (BLOCKS[existingBlockType].tileEntity) {
                copyEntityTileData(existingChunk, newChunk, x, y, z, dy);
            }
        }

        newChunk.setBlockType(x, y, z, mergedBlockType);
        newChunk.setDataValue(x, y, z, mergedDataValue);
        newChunk.setSkyLightLevel(x, y, z, mergedSkylightLevel);
        newChunk.setBlockLightLevel(x, y, z, mergedBlockLightLevel);
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
    private final ThreadLocal<int[]> histogramRef = new ThreadLocal<int[]>();
    private boolean replaceChunks, mergeOverworld, mergeUnderworld, clearTrees,
        clearResources, fillCaves, clearVegetation,
        clearManMadeAboveGround, clearManMadeBelowGround;
    private String warnings;
    
    private static final Logger logger = Logger.getLogger(WorldMerger.class.getName());
    private static final Object TIMING_FILE_LOCK = new Object();
    private static final String EOL = System.getProperty("line.separator");
    private static final BitSet SOLID_BLOCKS = new BitSet();
    
    static {
        for (Block block: BLOCKS) {
            if (block.natural
                    && (! block.resource)
                    && block.opaque) {
                SOLID_BLOCKS.set(block.id);
            }
        }
    }
}