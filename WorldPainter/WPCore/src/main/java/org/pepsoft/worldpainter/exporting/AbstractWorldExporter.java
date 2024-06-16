package org.pepsoft.worldpainter.exporting;

import org.pepsoft.minecraft.*;
import org.pepsoft.util.Box;
import org.pepsoft.util.ParallelProgressManager;
import org.pepsoft.util.ProgressReceiver;
import org.pepsoft.util.ProgressReceiver.OperationCancelled;
import org.pepsoft.util.SubProgressReceiver;
import org.pepsoft.util.mdc.MDCCapturingRuntimeException;
import org.pepsoft.util.mdc.MDCThreadPoolExecutor;
import org.pepsoft.util.undo.UndoManager;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.Dimension.Anchor;
import org.pepsoft.worldpainter.*;
import org.pepsoft.worldpainter.gardenofeden.GardenExporter;
import org.pepsoft.worldpainter.gardenofeden.Seed;
import org.pepsoft.worldpainter.layers.*;
import org.pepsoft.worldpainter.layers.pockets.UndergroundPocketsLayer;
import org.pepsoft.worldpainter.layers.tunnel.TunnelLayer;
import org.pepsoft.worldpainter.platforms.JavaExportSettings;
import org.pepsoft.worldpainter.plugins.BlockBasedPlatformProvider;
import org.pepsoft.worldpainter.plugins.PlatformManager;
import org.pepsoft.worldpainter.vo.AttributeKeyVO;
import org.pepsoft.worldpainter.vo.EventVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static org.pepsoft.minecraft.ChunkFactory.Stats.*;
import static org.pepsoft.minecraft.Constants.*;
import static org.pepsoft.util.ExceptionUtils.chainContains;
import static org.pepsoft.util.mdc.MDCUtils.doWithMdcContext;
import static org.pepsoft.worldpainter.Constants.*;
import static org.pepsoft.worldpainter.DefaultPlugin.JAVA_ANVIL;
import static org.pepsoft.worldpainter.DefaultPlugin.JAVA_MCREGION;
import static org.pepsoft.worldpainter.Dimension.Role.DETAIL;
import static org.pepsoft.worldpainter.Dimension.Role.MASTER;
import static org.pepsoft.worldpainter.Platform.Capability.POPULATE;
import static org.pepsoft.worldpainter.exporting.WorldExportSettings.Step.*;
import static org.pepsoft.worldpainter.layers.tunnel.TunnelLayer.Mode.CUSTOM_DIMENSION;
import static org.pepsoft.worldpainter.util.ThreadUtils.chooseThreadCount;

/**
 * An abstract {@link WorldExporter} for block based platforms.
 *
 * <p>Created by Pepijn on 11-12-2016.
 */
public abstract class AbstractWorldExporter implements WorldExporter {
    protected AbstractWorldExporter(World2 world, WorldExportSettings worldExportSettings, Platform platform) {
        if (world == null) {
            throw new NullPointerException();
        }
        this.world = world;
        this.platform = platform;
        this.worldExportSettings = (worldExportSettings != null)
                ? worldExportSettings
                : (world.getExportSettings() != null ? world.getExportSettings() : new WorldExportSettings());
        platformProvider = (BlockBasedPlatformProvider) PlatformManager.getInstance().getPlatformProvider(platform);
        populateSupported = platform.capabilities.contains(POPULATE);
    }

    @Override
    public World2 getWorld() {
        return world;
    }

    @Override
    public File selectBackupDir(File worldDir) throws IOException {
        File baseDir = worldDir.getParentFile();
        File minecraftDir = baseDir.getParentFile();
        File backupsDir = new File(minecraftDir, "backups");
        if ((! backupsDir.isDirectory()) &&  (! backupsDir.mkdirs())) {
            backupsDir = new File(System.getProperty("user.home"), "WorldPainter Backups");
            if ((! backupsDir.isDirectory()) && (! backupsDir.mkdirs())) {
                throw new IOException("Could not create " + backupsDir);
            }
        }
        return new File(backupsDir, worldDir.getName() + "." + DATE_FORMAT.format(new Date()));
    }

    /**
     * Export a dimension by exporting each region (512 block by 512 block area)
     * separately and in parallel as much as possible, taking into account the
     * number of CPU cores and available memory.
     *
     * <p>If an exception occurs and a progress receiver has been specified, the
     * exception is reported to the progress receiver and the export continues.
     * If there is no progress receiver, the export is aborted and the first
     * exception rethrown.
     *
     * @throws OperationCancelled If the progress receiver threw an
     * {@code OperationCancelled} exception.
     * @throws RuntimeException If an exception occurs during the export and no
     * progress receiver has been specified.
     */
    protected ChunkFactory.Stats parallelExportRegions(Dimension dimension, File worldDir, ProgressReceiver progressReceiver) throws OperationCancelled {
        if (progressReceiver != null) {
            progressReceiver.setMessage("Exporting " + dimension.getName() + " dimension");
        }

        long start = System.currentTimeMillis();

        final Anchor anchor = dimension.getAnchor();
        final int dim = anchor.dim;
        final Dimension ceiling = dimension.getWorld().getDimension(new Anchor(dim, anchor.role, true, 0));
        final Dimension master = dimension.getWorld().getDimension(new Anchor(dim, MASTER, false, 0));
        final Dimension combined = (master != null) ? new FlatteningDimension(dimension, new ScaledDimension(master, 16.0f)) : dimension;

        final ChunkFactory.Stats collectedStats = new ChunkFactory.Stats();
        final boolean tilesSelected = worldExportSettings.getTilesToExport() != null;
        final Set<Point> selectedTiles = tilesSelected ? worldExportSettings.getTilesToExport() : null;
        // TODO is it too late to do this now because we already created the combined dimension:
        final Map<Dimension, Object> savedSettings = world.getDimensions().stream()
                .filter(d -> d.getAnchor().dim == dim)
                .collect(toMap(identity(), d -> setupDimensionForExport(d, selectedTiles)));
        try {
            // Determine regions to export
            int lowestRegionX = Integer.MAX_VALUE, highestRegionX = Integer.MIN_VALUE, lowestRegionZ = Integer.MAX_VALUE, highestRegionZ = Integer.MIN_VALUE;
            final Set<Point> regions = new HashSet<>(), exportedRegions = new HashSet<>();
            if (tilesSelected) {
                // Sanity check
                assert worldExportSettings.getDimensionsToExport().size() == 1;
                assert worldExportSettings.getDimensionsToExport().contains(dim);
                for (Point tile: selectedTiles) {
                    int regionX = tile.x >> 2;
                    int regionZ = tile.y >> 2;
                    regions.add(new Point(regionX, regionZ));
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
                for (Point tileCoords: combined.getTileCoords()) {
                    // Also add regions for any bedrock wall and/or border tiles, if present
                    int r = (((dimension.getBorder() != null) && (! dimension.getBorder().isEndless())) ? dimension.getBorderSize() : 0)
                            + (((dimension.getBorder() == null) || (! dimension.getBorder().isEndless())) && (dimension.getWallType() != null) ? 1 : 0);
                    for (int dx = -r; dx <= r; dx++) {
                        for (int dy = -r; dy <= r; dy++) {
                            int regionX = (tileCoords.x + dx) >> 2;
                            int regionZ = (tileCoords.y + dy) >> 2;
                            regions.add(new Point(regionX, regionZ));
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
                }
                if (ceiling != null) {
                    for (Point tileCoords: ceiling.getTileCoords()) {
                        int regionX = tileCoords.x >> 2;
                        int regionZ = tileCoords.y >> 2;
                        regions.add(new Point(regionX, regionZ));
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
            }

            // Sort the regions to export the first two rows together, and then
            // row by row, to get the optimum tempo of performing fixups
            java.util.List<Point> sortedRegions = new ArrayList<>(regions.size());
            if (lowestRegionZ == highestRegionZ) {
                // No point in sorting it
                sortedRegions.addAll(regions);
            } else {
                for (int x = lowestRegionX; x <= highestRegionX; x++) {
                    for (int z = lowestRegionZ; z <= (lowestRegionZ + 1); z++) {
                        Point regionCoords = new Point(x, z);
                        if (regions.contains(regionCoords)) {
                            sortedRegions.add(regionCoords);
                        }
                    }
                }
                for (int z = lowestRegionZ + 2; z <= highestRegionZ; z++) {
                    for (int x = lowestRegionX; x <= highestRegionX; x++) {
                        Point regionCoords = new Point(x, z);
                        if (regions.contains(regionCoords)) {
                            sortedRegions.add(regionCoords);
                        }
                    }
                }
            }

            final Map<Point, List<Fixup>> fixups = new HashMap<>();
            final ExecutorService executor = createExecutorService("exporting", sortedRegions.size());
            final RuntimeException[] exception = new RuntimeException[1];
            final ParallelProgressManager parallelProgressManager = (progressReceiver != null) ? new ParallelProgressManager(progressReceiver, regions.size()) : null;
            final AtomicBoolean abort = new AtomicBoolean();
            try {
                // Export each individual region
                for (Point region: sortedRegions) {
                    final Point regionCoords = region;
                    executor.execute(() -> {
                        if (abort.get()) {
                            return;
                        }
                        ProgressReceiver progressReceiver1 = (parallelProgressManager != null) ? parallelProgressManager.createProgressReceiver() : null;
                        if (progressReceiver1 != null) {
                            try {
                                progressReceiver1.checkForCancellation();
                            } catch (OperationCancelled e) {
                                abort.set(true);
                                return;
                            }
                        }
                        try {
                            final int minHeight = dimension.getMinHeight(), maxHeight = dimension.getMaxHeight();
                            final Map<Layer, LayerExporter> exporters = getExportersForRegion(combined, regionCoords);
                            final Map<Layer, LayerExporter> ceilingExporters = (ceiling != null) ? getExportersForRegion(ceiling, region) : null;
                            final WorldPainterChunkFactory chunkFactory = new WorldPainterChunkFactory(combined, exporters, platform, maxHeight);
                            final WorldPainterChunkFactory ceilingChunkFactory = (ceiling != null) ? new WorldPainterChunkFactory(ceiling, ceilingExporters, platform, maxHeight) : null;

                            WorldRegion worldRegion = new WorldRegion(regionCoords.x, regionCoords.y, minHeight, maxHeight, platform);
                            ExportResults exportResults = null;
                            try {
                                exportResults = exportRegion(worldRegion, combined, ceiling, regionCoords, tilesSelected, exporters, ceilingExporters, chunkFactory, ceilingChunkFactory, (progressReceiver1 != null) ? new SubProgressReceiver(progressReceiver1, 0.0f, 0.9f) : null);
                                if (logger.isDebugEnabled()) {
                                    logger.debug("Generated region " + regionCoords.x + "," + regionCoords.y);
                                }
                                if (exportResults.chunksGenerated) {
                                    synchronized (collectedStats) {
                                        collectedStats.landArea += exportResults.stats.landArea;
                                        collectedStats.surfaceArea += exportResults.stats.surfaceArea;
                                        collectedStats.waterArea += exportResults.stats.waterArea;
                                        exportResults.stats.timings.forEach(
                                                (layer, duration) -> collectedStats.timings.computeIfAbsent(layer, k -> new AtomicLong()).addAndGet(duration.get()));
                                    }
                                }
                            } finally {
                                if ((exportResults != null) && exportResults.chunksGenerated) {
                                    long saveStart = System.nanoTime();
                                    worldRegion.save(worldDir, dim);
                                    long saveDuration = System.nanoTime() - saveStart;
                                    collectedStats.timings.computeIfAbsent(DISK_WRITING, k -> new AtomicLong()).addAndGet(saveDuration);
                                    if (logger.isDebugEnabled()) {
                                        logger.debug("Saving region took {} ms", saveDuration / 1_000_000);
                                    }
                                }
                            }
                            synchronized (fixups) {
                                if ((exportResults.fixups != null) && (!exportResults.fixups.isEmpty())) {
                                    fixups.put(new Point(regionCoords.x, regionCoords.y), exportResults.fixups);
                                }
                                exportedRegions.add(regionCoords);
                            }
                            performFixupsIfNecessary(worldDir, combined, regions, fixups, exportedRegions, collectedStats, progressReceiver1);
                        } catch (Throwable t) {
                            if (chainContains(t, OperationCancelled.class)) {
                                logger.debug("Operation cancelled on thread {} (message: \"{}\")", Thread.currentThread().getName(), t.getMessage());
                            } else {
                                logger.error(t.getClass().getSimpleName() + " while exporting region {},{} (message: \"{}\")", region.x, region.y, t.getMessage(), t);
                            }
                            abort.set(true);
                            if (progressReceiver1 != null) {
                                progressReceiver1.exceptionThrown(t);
                            } else {
                                if (exception[0] == null) {
                                    exception[0] = new RuntimeException(t.getClass().getSimpleName() + " while exporting region" + region.x + "," + region.y, exception[0]);
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
                    throw new MDCCapturingRuntimeException("Thread interrupted while waiting for all tasks to finish", e);
                }
            }

            // If there is a progress receiver then we have reported any exceptions to it, but if not then we should
            // rethrow the recorded exception, if any
            if (exception[0] != null) {
                throw exception[0];
            }

            if (! abort.get()) {
                // It's possible for there to be fixups left, if thread A was performing fixups and thread B added new
                // ones and then quit
                synchronized (fixups) {
                    if (! fixups.isEmpty()) {
                        if (progressReceiver != null) {
                            progressReceiver.setMessage("Doing remaining fixups for " + dimension.getName());
                            progressReceiver.reset();
                        }
                        performFixups(worldDir, combined, collectedStats, progressReceiver, fixups);
                    }
                }
            }

            // Calculate total size of dimension
            collectedStats.time = System.currentTimeMillis() - start;

            if (progressReceiver != null) {
                progressReceiver.setProgress(1.0f);
            }
        } finally {

            // Undo any changes we made (such as applying any combined layers)
            savedSettings.forEach(this::restoreDimensionAfterExport);
        }

        return collectedStats;
    }

    protected final void logLayers(org.pepsoft.worldpainter.Dimension dimension, EventVO event, String prefix) {
        StringBuilder sb = new StringBuilder();
        for (Layer layer: dimension.getAllLayers(false)) {
            if (sb.length() > 0) {
                sb.append(',');
            }
            sb.append(layer.getName());
        }
        if (sb.length() > 0) {
            event.setAttribute(new AttributeKeyVO<>(prefix + "layers"), sb.toString());
        }
    }

    protected Object setupDimensionForExport(Dimension dimension, Set<Point> selectedTiles) {
        final Map<String, Object> savedSettings = new HashMap<>();

        // Make sure that an undo manager is installed so all data changes can be rolled back
        if (! dimension.isUndoAvailable()) {
            final UndoManager undoManager = new UndoManager(2);
            dimension.registerUndoManager(undoManager);
            savedSettings.put("undoManager", undoManager);
        }
        dimension.rememberChanges();

        // Make all leaves persistent if directed by the world export settings
        if ((worldExportSettings.getStepsToSkip() != null) && worldExportSettings.getStepsToSkip().contains(LEAVES)) {
            ExportSettings exportSettings = dimension.getExportSettings();
            savedSettings.put("exportSettings", exportSettings);
            if (exportSettings == null) {
                exportSettings = platformProvider.getDefaultExportSettings(platform);
            }
            if (exportSettings instanceof JavaExportSettings) {
                exportSettings = ((JavaExportSettings) exportSettings).withMakeAllLeavesPersistent(true);
                dimension.setExportSettings(exportSettings);
            }
        }

        // Gather all layers used on the map/selected tiles
        final Set<Layer> allLayers;
        if (selectedTiles == null) {
            allLayers = dimension.getAllLayers(false);
        } else {
            allLayers = new HashSet<>();
            for (Point coords: selectedTiles) {
                final Tile tile = dimension.getTile(coords);
                // The tile could be null if e.g. this is the ceiling dimension, which doesn't have to have tiles
                // everywhere the surface does
                if (tile != null) {
                    allLayers.addAll(tile.getLayers());
                }
            }
        }
        allLayers.addAll(dimension.getMinimumLayers());

        // If there are combined layers, apply them and gather any newly added layers, recursively
        boolean done;
        do {
            done = true;
            for (Layer layer: new HashSet<>(allLayers)) {
                if ((layer instanceof CombinedLayer) && ((CombinedLayer) layer).isExport()) {
                    // Apply the combined layer
                    final Set<Layer> addedLayers = ((CombinedLayer) layer).apply(dimension, selectedTiles);
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

        // Update the floor dimensions, if any, of all TunnelLayers
        for (CustomLayer customLayer: dimension.getCustomLayers()) {
            if ((customLayer instanceof TunnelLayer) && (((TunnelLayer) customLayer).getFloorMode() == CUSTOM_DIMENSION)) {
                ((TunnelLayer) customLayer).updateFloorDimensionTiles(dimension);
            }
        }

        return savedSettings;
    }

    /**
     * Restores changes that were performed by {@link #setupDimensionForExport(Dimension, Set)}.
     *
     * @param dimension     The dimension to restore.
     * @param savedSettings The object that was returned from {@link #setupDimensionForExport(Dimension, Set)}.
     */
    @SuppressWarnings("unchecked") // Responsibility of caller
    protected void restoreDimensionAfterExport(Dimension dimension, Object savedSettings) {
        final Map<String, Object> map = (Map<String, Object>) savedSettings;
        if ((map != null) && map.containsKey("exportSettings")) {
            dimension.setExportSettings((ExportSettings) map.get("exportSettings"));
        }

        if (dimension.undoChanges()) {
            // TODO: some kind of cleverer undo mechanism (undo history cloning?) so we don't mess up the user's redo
            //  history
            dimension.clearRedo();
            dimension.armSavePoint();
        }

        if ((map != null) && map.containsKey("undoManager")) {
            dimension.unregisterUndoManager();
        }
    }

    protected Map<Layer, LayerExporter> getExportersForRegion(Dimension dimension, Point regionCoords) {
        // Gather all layers used in the region TODO this should only be the tiles that will actually be exported, taking tile selection and Read Only layer into account
        final Map<Layer, LayerExporter> exporters = new HashMap<>();
        final Set<Layer> allLayers = new HashSet<>(dimension.getMinimumLayers());
        // Include an extra ring of tiles TODO why?
        final int tileX1 = (regionCoords.x << 2) - 1, tileX2 = tileX1 + 5, tileY1 = (regionCoords.y << 2) - 1, tileY2 = tileY1 + 5;
        for (int tileX = tileX1; tileX <= tileX2; tileX++) {
            for (int tileY = tileY1; tileY <= tileY2; tileY++) {
                final Tile tile = dimension.getTile(tileX, tileY);
                if (tile != null) {
                    allLayers.addAll(tile.getLayers());
                }
            }
        }

        // If there are combined layers, replace them with their constituent layers, recursively
        boolean done;
        do {
            done = true;
            for (Layer layer: new HashSet<>(allLayers)) {
                if ((layer instanceof CombinedLayer) && ((CombinedLayer) layer).isExport()) {
                    // Remove the combined layer from the list
                    allLayers.remove(layer);
                    // Add any layers it contains
                    allLayers.addAll(((CombinedLayer) layer).getLayers());
                    // Signal that we have to go around at least once more,
                    // in case any of the newly added layers are themselves
                    // combined layers
                    done = false;
                }
            }
        } while (! done);

        // Remove layers which have been excluded for export
        allLayers.removeIf(layer -> (layer instanceof CustomLayer) && (! ((CustomLayer) layer).isExport()));

        // Remove layers which should be disabled due to the world export settings
        applyWorldExportSettings(allLayers);

        // Create all the exporters
        for (Layer layer: allLayers) {
            final LayerExporter exporter = layer.getExporter(dimension, platform, dimension.getLayerSettings(layer));
            if (exporter != null) {
                exporters.put(layer, exporter);
            }
        }
        return exporters;
    }

    protected ExportResults firstPass(MinecraftWorld minecraftWorld, Dimension dimension, Point regionCoords, Map<Point, Tile> tiles, boolean tileSelection, Map<Layer, LayerExporter> exporters, ChunkFactory chunkFactory, boolean ceiling, ProgressReceiver progressReceiver) throws OperationCancelled {
        if (logger.isDebugEnabled()) {
            logger.debug("Start of first pass for region {},{}", regionCoords.x, regionCoords.y);
        }
        if (progressReceiver != null) {
            if (ceiling) {
                progressReceiver.setMessage("Generating ceiling");
            } else {
                progressReceiver.setMessage("Generating landscape");
            }
        }
        final int lowestChunkX = (regionCoords.x << 5) - 1;
        final int highestChunkX = (regionCoords.x << 5) + 32;
        final int lowestChunkY = (regionCoords.y << 5) - 1;
        final int highestChunkY = (regionCoords.y << 5) + 32;
        final int lowestRegionChunkX = lowestChunkX + 1;
        final int highestRegionChunkX = highestChunkX - 1;
        final int lowestRegionChunkY = lowestChunkY + 1;
        final int highestRegionChunkY = highestChunkY - 1;
        final ExportResults exportResults = new ExportResults();
        int chunkNo = 0;
        final int ceilingDelta = dimension.getMaxHeight() - dimension.getCeilingHeight();

        final int startingChunkX;
        final int endingChunkX;
        final int startingChunkY;
        final int endingChunkY;

        if (ceiling){ //we must generate a chunk on both sides if using a ceiling
            startingChunkX=lowestChunkX;
            endingChunkX=highestChunkX;
            startingChunkY=lowestChunkY;
            endingChunkY=highestChunkY;
        }else{
            startingChunkX=lowestRegionChunkX;
            endingChunkX=highestRegionChunkX;
            startingChunkY=lowestRegionChunkY;
            endingChunkY=highestRegionChunkY;
        }

        int numberOfChunksToGenerate =(endingChunkX-startingChunkX)*(endingChunkY-startingChunkY);

        for (int chunkX = startingChunkX; chunkX <= endingChunkX; chunkX++) {
            for (int chunkY = startingChunkY; chunkY <= endingChunkY; chunkY++) {
                final ChunkFactory.ChunkCreationResult chunkCreationResult = createChunk(dimension, chunkFactory, tiles, chunkX, chunkY, tileSelection, exporters, ceiling);
                if (chunkCreationResult != null) {
                    if ((!ceiling)||((chunkX >= lowestRegionChunkX) && (chunkX <= highestRegionChunkX) && (chunkY >= lowestRegionChunkY) && (chunkY <= highestRegionChunkY))) {
                        exportResults.chunksGenerated = true;
                        exportResults.stats.landArea += chunkCreationResult.stats.landArea;
                        exportResults.stats.surfaceArea += chunkCreationResult.stats.surfaceArea;
                        exportResults.stats.waterArea += chunkCreationResult.stats.waterArea;
                        chunkCreationResult.stats.timings.forEach(
                                (stage, duration) -> exportResults.stats.timings.computeIfAbsent(stage, k -> new AtomicLong()).addAndGet(duration.get()));
                    }
                    if (ceiling) {
                        final Chunk invertedChunk = new InvertedChunk(chunkCreationResult.chunk, ceilingDelta, platform);
                        Chunk existingChunk = minecraftWorld.getChunkForEditing(chunkX, chunkY);
                        if (existingChunk == null) {
                            existingChunk = platformProvider.createChunk(platform, chunkX, chunkY, dimension.getMinHeight(), dimension.getMaxHeight());
                            minecraftWorld.addChunk(existingChunk);
                        }
                        mergeChunks(invertedChunk, existingChunk);
                    } else {
                        minecraftWorld.addChunk(chunkCreationResult.chunk);
                    }
                }
                chunkNo++;
                if (progressReceiver != null) {
                    progressReceiver.setProgress((float) chunkNo / numberOfChunksToGenerate);
                }
            }
        }
        if (logger.isDebugEnabled()) {
            logger.debug("End of first pass for region {},{}", regionCoords.x, regionCoords.y);
        }
        return exportResults;
    }

    protected List<Fixup> secondPass(List<Layer> secondaryPassLayers, Dimension dimension, MinecraftWorld minecraftWorld,
                                     Map<Layer, LayerExporter> exporters, Collection<Tile> tiles, Point regionCoords,
                                     ExportResults exportResults, ProgressReceiver progressReceiver) throws OperationCancelled {
        if (logger.isDebugEnabled()) {
            logger.debug("Start of second pass for region {},{}", regionCoords.x, regionCoords.y);
        }
        final int stageCount = secondaryPassLayers.stream().mapToInt(layer -> {
            final SecondPassLayerExporter exporter = (SecondPassLayerExporter) exporters.get(layer);
            if (exporter == null) {
                throw new IllegalStateException("Exporter missing for layer " + layer + " of type " + layer.getClass().getSimpleName());
            }
            return exporter.getStages().size();
        }).sum();

        // Garden / seeds first pass
        final GardenExporter gardenExporter = new GardenExporter();
        final Set<Seed> firstPassProcessedSeeds = new HashSet<>();
        long start = System.nanoTime();
        tiles.stream().filter(tile -> tile.getLayers().contains(GardenCategory.INSTANCE))
                .forEach(tile -> gardenExporter.firstPass(dimension, tile, platform, minecraftWorld, firstPassProcessedSeeds));
        if (! firstPassProcessedSeeds.isEmpty()) {
            exportResults.stats.timings.computeIfAbsent(SEEDS, k -> new AtomicLong()).addAndGet(System.nanoTime() - start);
        }

        int counter = 0;
        final Rectangle area = new Rectangle((regionCoords.x << 9) - 16, (regionCoords.y << 9) - 16, 544, 544);
        final Rectangle exportedArea = new Rectangle((regionCoords.x << 9), (regionCoords.y << 9), 512, 512);
        final List<Fixup> fixups = new ArrayList<>();
        for (SecondPassLayerExporter.Stage stage: SecondPassLayerExporter.Stage.values()) {
            if (logger.isDebugEnabled()) {
                logger.debug("Start of {} stage for region {},{}", stage, regionCoords.x, regionCoords.y);
            }
            for (Layer layer: secondaryPassLayers) {
                final SecondPassLayerExporter exporter = (SecondPassLayerExporter) exporters.get(layer);
                if (! exporter.getStages().contains(stage)) {
                    continue;
                }
                if (logger.isDebugEnabled()) {
                    logger.debug("Stage {} for layer {} for region {},{}", stage, layer, regionCoords.x, regionCoords.y);
                }
                if (progressReceiver != null) {
                    if (minecraftWorld instanceof InvertedWorld) {
                        progressReceiver.setMessage("Exporting layer " + layer + " for ceiling (" + stage.name().toLowerCase() + " stage)");
                    } else {
                        progressReceiver.setMessage("Exporting layer " + layer + " (" + stage.name().toLowerCase() + " stage)");
                    }
                }
                final List<Fixup> layerFixups;
                start = System.nanoTime();
                switch (stage) {
                    case CARVE:
                        layerFixups = exporter.carve(area, exportedArea, minecraftWorld);
                        break;
                    case ADD_FEATURES:
                        layerFixups = exporter.addFeatures(area, exportedArea, minecraftWorld);
                        break;
                    default:
                        throw new InternalError();
                }
                exportResults.stats.timings.computeIfAbsent(layer, k -> new AtomicLong()).addAndGet(System.nanoTime() - start);
                if (layerFixups != null) {
                    fixups.addAll(layerFixups);
                }
                if (progressReceiver != null) {
                    counter++;
                    progressReceiver.setProgress((float) counter / stageCount);
                }
            }
        }

        // Garden / seeds second pass
        final Set<Seed> secondPassProcessedSeeds = new HashSet<>();
        start = System.nanoTime();
        tiles.stream().filter(tile -> tile.getLayers().contains(GardenCategory.INSTANCE)).forEach(tile -> {
            gardenExporter.secondPass(dimension, tile, platform, minecraftWorld, secondPassProcessedSeeds);
        });
        if (! secondPassProcessedSeeds.isEmpty()) {
            exportResults.stats.timings.computeIfAbsent(SEEDS, k -> new AtomicLong()).addAndGet(System.nanoTime() - start);
        }

        // TODO: trying to do this for every region should work but is not very elegant
        if ((dimension.getAnchor().dim == DIM_NORMAL) && world.isCreateGoodiesChest()) {
            final Point goodiesPoint = (Point) world.getSpawnPoint().clone();
            goodiesPoint.translate(3, 3);
            final Anchor spawnDimension = world.getSpawnPointDimension();
            final int height = (spawnDimension != null)
                    ? (world.getDimension(spawnDimension).getIntHeightAt(goodiesPoint.x, goodiesPoint.y) + 1)
                    : (getIntHeightAt(DIM_NORMAL, goodiesPoint.x, goodiesPoint.y) + 1);
            if ((height >= dimension.getMinHeight()) && (height < dimension.getMaxHeight())) {
                final Chunk chunk = minecraftWorld.getChunk(goodiesPoint.x >> 4, goodiesPoint.y >> 4);
                if (chunk != null) {
                    chunk.setMaterial(goodiesPoint.x & 0xf, height, goodiesPoint.y & 0xf, Material.CHEST_NORTH);
                    Chest goodiesChest = createGoodiesChest(platform);
                    goodiesChest.setX(goodiesPoint.x);
                    goodiesChest.setY(height);
                    goodiesChest.setZ(goodiesPoint.y);
                    chunk.getTileEntities().add(goodiesChest);
                }
            }
        }

        if (logger.isDebugEnabled()) {
            logger.debug("End of second pass for region {},{}", regionCoords.x, regionCoords.y);
        }
        return fixups;
    }

    protected void blockPropertiesPass(MinecraftWorld minecraftWorld, Point regionCoords, BlockBasedExportSettings exportSettings, ProgressReceiver progressReceiver) throws OperationCancelled {
        float maxIterations = 0;
        final StringBuilder nounsBuilder = new StringBuilder();
        if (exportSettings.isCalculateSkyLight() || exportSettings.isCalculateBlockLight()) {
            nounsBuilder.append("lighting");
            maxIterations = 16;
        }
        if (exportSettings.isCalculateLeafDistance()) {
            if (nounsBuilder.length() > 0) {
                nounsBuilder.append(" and ");
            }
            nounsBuilder.append("leaf distances");
            maxIterations = Math.max(maxIterations, 7);
        }
        final String nouns = nounsBuilder.toString();
        if (progressReceiver != null) {
            progressReceiver.setMessage("Calculating initial " + nouns);
        }
        BlockPropertiesCalculator calculator = new BlockPropertiesCalculator(minecraftWorld, platform, worldExportSettings, exportSettings);

        // Calculate primary light
        int lowMark = Integer.MAX_VALUE, highMark = Integer.MIN_VALUE;
        int lowestChunkX = (regionCoords.x << 5) - 1;
        int highestChunkX = (regionCoords.x << 5) + 32;
        int lowestChunkY = (regionCoords.y << 5) - 1;
        int highestChunkY = (regionCoords.y << 5) + 32;
        int total = highestChunkX - lowestChunkX + 1, count = 0;
        for (int chunkX = lowestChunkX; chunkX <= highestChunkX; chunkX++) {
            for (int chunkY = lowestChunkY; chunkY <= highestChunkY; chunkY++) {
                Chunk chunk = minecraftWorld.getChunk(chunkX, chunkY);
                if (chunk != null) {
                    int[] levels = calculator.firstPass(chunk);
                    if (levels[0] < lowMark) {
                        lowMark = levels[0];
                    }
                    if (levels[1] > highMark) {
                        highMark = levels[1];
                    }
                }
            }
            if (progressReceiver != null) {
                progressReceiver.setProgress(0.2f * ++count / total);
            }
        }

        if (lowMark != Integer.MAX_VALUE) {
            if (progressReceiver != null) {
                progressReceiver.setMessage("Propagating " + nouns);
            }

            // Calculate secondary light
            calculator.setDirtyArea(new Box((regionCoords.x << 9) - 16, ((regionCoords.x + 1) << 9) + 16, lowMark, highMark + 1, (regionCoords.y << 9) - 16, ((regionCoords.y + 1) << 9) + 16));
            int iteration = 1;
            while (calculator.secondPass()) {
                if (progressReceiver != null) {
                    progressReceiver.setProgress(0.2f + 0.8f * (iteration++ / maxIterations));
                }
            }
            calculator.finalise();
        }

        if (progressReceiver != null) {
            progressReceiver.setProgress(1.0f);
        }
    }

    protected ExportResults exportRegion(MinecraftWorld minecraftWorld,
                                         Dimension dimension, Dimension ceiling,
                                         Point regionCoords,
                                         boolean tileSelection,
                                         Map<Layer, LayerExporter> exporters, Map<Layer, LayerExporter> ceilingExporters,
                                         ChunkFactory chunkFactory, ChunkFactory ceilingChunkFactory,
                                         ProgressReceiver progressReceiver) {
        return doWithMdcContext(() -> {
            if (progressReceiver != null) {
                progressReceiver.setMessage("Exporting region " + regionCoords.x + "," + regionCoords.y + " of " + dimension.getName());
            }
            final int lowestTileX = (regionCoords.x << 2) - 1;
            final int highestTileX = lowestTileX + 5;
            final int lowestTileY = (regionCoords.y << 2) - 1;
            final int highestTileY = lowestTileY + 5;
            final Map<Point, Tile> tiles = new HashMap<>(), ceilingTiles = new HashMap<>();
            for (int tileX = lowestTileX; tileX <= highestTileX; tileX++) {
                for (int tileY = lowestTileY; tileY <= highestTileY; tileY++) {
                    final Point tileCoords = new Point(tileX, tileY);
                    Tile tile = dimension.getTile(tileCoords);
                    if ((tile != null) && ((! tileSelection) || worldExportSettings.getTilesToExport().contains(tileCoords))) {
                        tiles.put(tileCoords, tile);
                    }
                    if (ceiling != null) {
                        tile = ceiling.getTile(tileCoords);
                        if ((tile != null) && ((! tileSelection) || worldExportSettings.getTilesToExport().contains(tileCoords))) {
                            ceilingTiles.put(tileCoords, tile);
                        }
                    }
                }
            }

            final List<Layer> secondaryPassLayers = new ArrayList<>(), ceilingSecondaryPassLayers = new ArrayList<>();
            for (Layer layer: exporters.keySet()) {
                final Class<? extends LayerExporter> exporterType = layer.getExporterType();
                if ((exporterType != null) && SecondPassLayerExporter.class.isAssignableFrom(exporterType)) {
                    secondaryPassLayers.add(layer);
                }
            }
            Collections.sort(secondaryPassLayers);

            // Set up export of ceiling
            if (ceiling != null) {
                for (Layer layer: ceilingExporters.keySet()) {
                    final Class<? extends LayerExporter> exporterType = layer.getExporterType();
                    if ((exporterType != null) && SecondPassLayerExporter.class.isAssignableFrom(exporterType)) {
                        ceilingSecondaryPassLayers.add(layer);
                    }
                }
                Collections.sort(ceilingSecondaryPassLayers);
            }

            // First pass. Create terrain and apply layers which don't need access to neighbouring chunks
            ExportResults exportResults = firstPass(minecraftWorld, dimension, regionCoords, tiles, tileSelection, exporters,chunkFactory, false,
                    (progressReceiver != null) ? new SubProgressReceiver(progressReceiver, 0.0f, ((ceiling != null) ? 0.225f : 0.45f) /* TODO why doesn't this work? */) : null);

            ExportResults ceilingExportResults = null;
            if (ceiling != null) {
                // First pass for the ceiling. Create terrain and apply layers which don't need access to neighbouring
                // chunks
                ceilingExportResults = firstPass(minecraftWorld, ceiling, regionCoords, ceilingTiles, tileSelection, ceilingExporters, ceilingChunkFactory, true,
                        (progressReceiver != null) ? new SubProgressReceiver(progressReceiver, 0.225f, 0.225f) : null);
            }

            if (exportResults.chunksGenerated || ((ceiling != null) && ceilingExportResults.chunksGenerated)) {
                // Second pass. Apply layers which need information from or apply changes to neighbouring chunks
                List<Fixup> myFixups = secondPass(secondaryPassLayers, dimension, minecraftWorld, exporters, tiles.values(), regionCoords,
                        exportResults, (progressReceiver != null) ? new SubProgressReceiver(progressReceiver, 0.45f, (ceiling != null) ? 0.05f : 0.1f) : null);
                if ((myFixups != null) && (! myFixups.isEmpty())) {
                    exportResults.fixups = myFixups;
                }

                if (ceiling != null) {
                    // Second pass for ceiling. Apply layers which need information from or apply changes to
                    // neighbouring chunks. Fixups are not supported for the ceiling for now. TODO: implement
                    secondPass(ceilingSecondaryPassLayers, ceiling, new InvertedWorld(minecraftWorld, ceiling.getMaxHeight() - ceiling.getCeilingHeight(), platform), ceilingExporters, ceilingTiles.values(), regionCoords,
                            ceilingExportResults, (progressReceiver != null) ? new SubProgressReceiver(progressReceiver, 0.4f, 0.05f) : null);

                    // Add ceiling timings to surface timings
                    ceilingExportResults.stats.timings.forEach(
                            (stage, duration) -> exportResults.stats.timings.computeIfAbsent(stage, k -> new AtomicLong()).addAndGet(duration.get()));
                }

                // Post-processing. Fix covered grass blocks, things like that
                final BlockBasedExportSettings exportSettings = getExportSettings(dimension, platform);
                long start = System.nanoTime();
                PlatformManager.getInstance().getPostProcessor(platform).postProcess(minecraftWorld, new Rectangle(regionCoords.x << 9, regionCoords.y << 9, 512, 512), exportSettings,
                        (progressReceiver != null) ? new SubProgressReceiver(progressReceiver, 0.55f, 0.1f) : null);
                exportResults.stats.timings.put(POST_PROCESSING, new AtomicLong(System.nanoTime() - start));

                // Third pass. Calculate lighting and/or leaf distances (if requested, and supported by the platform)
                if (BlockPropertiesCalculator.isBlockPropertiesPassNeeded(platform, worldExportSettings, exportSettings)) {
                    start = System.nanoTime();
                    blockPropertiesPass(minecraftWorld, regionCoords, exportSettings, (progressReceiver != null) ? new SubProgressReceiver(progressReceiver, 0.65f, 0.35f) : null);
                    exportResults.stats.timings.put(BLOCK_PROPERTIES, new AtomicLong(System.nanoTime() - start));
                }
            }

            if (progressReceiver != null) {
                progressReceiver.setProgress(1.0f);
            }

            return exportResults;
        }, "region.coords", regionCoords);
    }

    protected ChunkFactory.ChunkCreationResult createChunk(Dimension dimension, ChunkFactory chunkFactory, Map<Point, Tile> tiles, int chunkX, int chunkY, boolean tileSelection, Map<Layer, LayerExporter> exporters, boolean ceiling) {
        final int tileX = chunkX >> 3;
        final int tileY = chunkY >> 3;
        final Point tileCoords = new Point(tileX, tileY);
        final Dimension.Border borderType = dimension.getBorder();
        final boolean endlessBorder = (borderType != null) && borderType.isEndless();
        final boolean border = (borderType != null) && (! endlessBorder) && (dimension.getBorderSize() > 0);
        if (tileSelection) {
            // Tile selection. Don't export bedrock wall or border tiles
            if (tiles.containsKey(tileCoords)) {
                return chunkFactory.createChunk(chunkX, chunkY);
            } else {
                return null;
            }
        } else {
            final Tile tile = dimension.getTile(tileCoords);
            if (tile != null) {
                final ChunkFactory.ChunkCreationResult result = chunkFactory.createChunk(chunkX, chunkY);
                // If the chunk is marked as NotPresent we might want to render a border chunk here
                if ((result == null) && border && tile.getBitLayerValue(NotPresent.INSTANCE, (chunkX & 0x7) << 4, (chunkY & 0x7) << 4)) {
                    return BorderChunkFactory.create(chunkX, chunkY, dimension, platform, exporters);
                } else {
                    return result;
                }
            } else if ((! ceiling) && (! endlessBorder)) {
                // Might be a border or wall chunk (but not if this is a ceiling dimension or the border is an endless
                // border)
                if (border && isBorderChunk(dimension, chunkX, chunkY)) {
                    return BorderChunkFactory.create(chunkX, chunkY, dimension, platform, exporters);
                } else if ((dimension.getWallType() != null)
                        && (border
                            ? (isBorderChunk(dimension, chunkX - 1, chunkY) || isBorderChunk(dimension, chunkX, chunkY - 1) || isBorderChunk(dimension, chunkX + 1, chunkY) || isBorderChunk(dimension, chunkX, chunkY + 1))
                            : (isWorldChunk(dimension, chunkX - 1, chunkY) || isWorldChunk(dimension, chunkX, chunkY - 1) || isWorldChunk(dimension, chunkX + 1, chunkY) || isWorldChunk(dimension, chunkX, chunkY + 1)))) {
                    // Bedrock or barrier wall is turned on and a neighbouring chunk is a border chunk (if there is a
                    // border), or a world chunk (if there is no border)
                    return WallChunk.create(chunkX, chunkY, dimension, platform);
                } else {
                    // Outside known space
                    return null;
                }
            } else {
                // Not a world tile, and we're a ceiling dimension, or the border is an endless border, so we don't
                // export borders and bedrock walls
                return null;
            }
        }
    }

    protected boolean isReadyForFixups(Set<Point> regionsToExport, Set<Point> exportedRegions, Point coords) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if ((dx == 0) && (dy ==0)) {
                    continue;
                }
                Point checkCoords = new Point(coords.x + dx, coords.y + dy);
                if (regionsToExport.contains(checkCoords) && (! exportedRegions.contains(checkCoords))) {
                    // A surrounding region should be exported and hasn't yet
                    // been, so the fixups can't be performed yet
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Apply all fixups which can be applied because all surrounding regions
     * have been exported (or are not going to be), but only if another thread
     * is not already doing it
     */
    protected void performFixupsIfNecessary(final File worldDir, final Dimension dimension, final Set<Point> regionsToExport,
                                            final Map<Point, List<Fixup>> fixups, final Set<Point> exportedRegions,
                                            final ChunkFactory.Stats stats, final ProgressReceiver progressReceiver) throws ProgressReceiver.OperationCancelled {
        if (performingFixups.tryAcquire()) {
            try {
                Map<Point, List<Fixup>> myFixups = new HashMap<>();
                synchronized (fixups) {
                    for (Iterator<Entry<Point, List<Fixup>>> i = fixups.entrySet().iterator(); i.hasNext(); ) {
                        Entry<Point, List<Fixup>> entry = i.next();
                        Point fixupRegionCoords = entry.getKey();
                        if (isReadyForFixups(regionsToExport, exportedRegions, fixupRegionCoords)) {
                            myFixups.put(fixupRegionCoords, entry.getValue());
                            i.remove();
                        }
                    }
                }
                if (! myFixups.isEmpty()) {
                    performFixups(worldDir, dimension, stats, (progressReceiver != null) ? new SubProgressReceiver(progressReceiver, 0.9f, 0.1f) : null, myFixups);
                }
            } finally {
                performingFixups.release();
            }
        }
    }

    protected void performFixups(final File worldDir, final Dimension dimension, final ChunkFactory.Stats stats,
                                 final ProgressReceiver progressReceiver, final Map<Point, List<Fixup>> fixups) throws OperationCancelled {
        long start = System.nanoTime();
        int count = 0, total = 0;
        for (Entry<Point, List<Fixup>> entry: fixups.entrySet()) {
            total += entry.getValue().size();
        }
        // Make sure to honour the read-only layer: TODO: this means nothing at the moment. Is it still relevant?
        final CachingMinecraftWorld minecraftWorld = new CachingMinecraftWorld(worldDir, dimension.getAnchor().dim, dimension.getMinHeight(), dimension.getMaxHeight(), platform, false, 512);
        final long duration;
        try {
            final ExportSettings exportSettings = getExportSettings(dimension, platform);
            for (Entry<Point, List<Fixup>> entry: fixups.entrySet()) {
                if (progressReceiver != null) {
                    progressReceiver.setMessage("Performing fixups for region " + entry.getKey().x + "," + entry.getKey().y);
                }
                final List<Fixup> regionFixups = entry.getValue();
                if (logger.isDebugEnabled()) {
                    logger.debug("Performing " + regionFixups.size() + " fixups for region " + entry.getKey().x + "," + entry.getKey().y);
                }
                for (Fixup fixup: regionFixups) {
                    fixup.fixup(minecraftWorld, dimension, platform, worldExportSettings, exportSettings);
                    if (progressReceiver != null) {
                        progressReceiver.setProgress((float) ++count / total);
                    }
                }
            }
            duration = System.nanoTime() - start;
            stats.timings.computeIfAbsent(FIXUPS, k -> new AtomicLong()).addAndGet(duration);
        } finally {
            start = System.nanoTime();
            minecraftWorld.close();
            stats.timings.computeIfAbsent(DISK_WRITING, k -> new AtomicLong()).addAndGet(System.nanoTime() - start);
        }
        if (logger.isTraceEnabled()) {
            logger.trace("Fixups for " + fixups.size() + " regions took " + (duration / 1_000_000) + " ms");
        }
    }

    // Coordinates are in Minecraft coordinate system
    /**
     * Move tile entity data from a source chunk to a potentially different height in a destination chunk. The source
     * and destination chunks may be the same.
     *
     * @param toChunk   The destination chunk.
     * @param fromChunk The source chunk.
     * @param x         The X coordinate.
     * @param y         The Y coordinate in the destination chunk.
     * @param z         The Z coordinate.
     * @param dy        The delta to subtract from {@code y} to obtain the Y coordinate in the source chunk. In other
     *                  words: how many blocks to move the tile entity data up.
     */
    protected void moveEntityTileData(Chunk toChunk, Chunk fromChunk, int x, int y, int z, int dy) {
        if ((toChunk == fromChunk) && (dy == 0)) {
            return;
        }
        final int existingBlockDX = fromChunk.getxPos() << 4, existingBlockDZ = fromChunk.getzPos() << 4;

        // First remove any tile entity data which may already be there
        toChunk.getTileEntities().removeIf(entity -> (entity.getY() == y) && ((entity.getX() - existingBlockDX) == x) && ((entity.getZ() - existingBlockDZ) == z));

        // Copy the tile entity data
        final List<TileEntity> fromEntities = fromChunk.getTileEntities();
        for (TileEntity entity: fromEntities) {
            if ((entity.getY() == (y - dy)) && ((entity.getX() - existingBlockDX) == x) && ((entity.getZ() - existingBlockDZ) == z)) {
                logger.debug("Moving tile entity " + entity.getId() + " from  " + x + "," + (y - dy) + "," + z + " to  " + x + "," + y + "," + z);
                entity.setY(y);
                toChunk.getTileEntities().add(entity);
                break;
            }
        }

        // Delete the tile entity data in the old location. Do this in a separate iteration, since toChunk may be the
        // same one as fromChunk
        fromEntities.removeIf(entity -> (entity.getY() == (y - dy)) && ((entity.getX() - existingBlockDX) == x) && ((entity.getZ() - existingBlockDZ) == z));
    }

    protected final ExecutorService createExecutorService(String operation, int jobCount) {
        return MDCThreadPoolExecutor.newFixedThreadPool(chooseThreadCount(operation, jobCount), new ThreadFactory() {
            @Override
            public synchronized Thread newThread(Runnable r) {
                Thread thread = new Thread(threadGroup, r, operation.toLowerCase().replaceAll("\\s+", "-") + "-" + nextID++);
                thread.setPriority(Thread.MIN_PRIORITY);
                return thread;
            }

            private final ThreadGroup threadGroup = new ThreadGroup(operation);
            private int nextID = 1;
        });
    }

    protected final void applyWorldExportSettings(Collection<? extends Layer> layers) {
        final Set<WorldExportSettings.Step> stepsToSkip = worldExportSettings.getStepsToSkip();
        if ((stepsToSkip != null) && (! stepsToSkip.isEmpty())) {
            layers.removeIf(layer -> {
                final boolean rc = (stepsToSkip.contains(CAVES) && ((layer instanceof Caverns) || (layer instanceof Chasms) || (layer instanceof Caves) || (layer instanceof TunnelLayer)))
                        || (stepsToSkip.contains(RESOURCES) && ((layer instanceof Resources) || (layer instanceof UndergroundPocketsLayer)));
                if (rc) {
                    logger.debug("Disabling layer {} due to world export settings", layer);
                }
                return rc;
            });
        }
    }

    protected final int getIntHeightAt(int dim, int x, int y) {
        final Dimension dimension = world.getDimension(new Anchor(dim, DETAIL, false, 0));
        final Tile tile = dimension.getTile(x >> TILE_SIZE_BITS, y >> TILE_SIZE_BITS);
        if (tile != null) {
            if (tile.getBitLayerValue(NotPresent.INSTANCE, x & TILE_SIZE_MASK, y & TILE_SIZE_MASK)) {
                final Tile masterTile = getMasterTile(dim, x, y);
                if (masterTile != null) {
                    return masterTile.getIntHeight((x >> 4) & TILE_SIZE_MASK, (y >> 4) & TILE_SIZE_MASK);
                }
            }
            return tile.getIntHeight(x & TILE_SIZE_MASK, y & TILE_SIZE_MASK);
        } else {
            final Tile masterTile = getMasterTile(dim, x, y);
            if (masterTile != null) {
                return masterTile.getIntHeight((x >> 4) & TILE_SIZE_MASK, (y >> 4) & TILE_SIZE_MASK);
            }
            return Integer.MIN_VALUE;
        }
    }

    protected final int getWaterLevelAt(int dim, int x, int y) {
        final Dimension dimension = world.getDimension(new Anchor(dim, DETAIL, false, 0));
        final Tile tile = dimension.getTile(x >> TILE_SIZE_BITS, y >> TILE_SIZE_BITS);
        if (tile != null) {
            if (tile.getBitLayerValue(NotPresent.INSTANCE, x & TILE_SIZE_MASK, y & TILE_SIZE_MASK)) {
                final Tile masterTile = getMasterTile(dim, x, y);
                if (masterTile != null) {
                    return masterTile.getWaterLevel((x >> 4) & TILE_SIZE_MASK, (y >> 4) & TILE_SIZE_MASK);
                }
            }
            return tile.getWaterLevel(x & TILE_SIZE_MASK, y & TILE_SIZE_MASK);
        } else {
            final Tile masterTile = getMasterTile(dim, x, y);
            if (masterTile != null) {
                return masterTile.getWaterLevel((x >> 4) & TILE_SIZE_MASK, (y >> 4) & TILE_SIZE_MASK);
            }
            return Integer.MIN_VALUE;
        }
    }

    private Tile getMasterTile(int dim, int x, int y) {
        final Dimension masterDimension = world.getDimension(new Anchor(dim, MASTER, false, 0));
        if (masterDimension != null) {
            return  masterDimension.getTile(x >> (TILE_SIZE_BITS + 4), y >> (TILE_SIZE_BITS + 4));
        } else {
            return null;
        }
    }

    /**
     * Merge the non-air blocks from the source chunk into the destination chunk.
     *
     * @param source The source chunk.
     * @param destination The destination chunk.
     */
    private void mergeChunks(Chunk source, Chunk destination) {
        final int maxHeight = source.getMaxHeight();
        if (maxHeight != destination.getMaxHeight()) {
            throw new IllegalArgumentException("Different maxHeights");
        }
        for (int y = 0; y < maxHeight; y++) {
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    Material destinationMaterial = destination.getMaterial(x, y, z);
                    if (! destinationMaterial.solid) {
                        // Insubstantial blocks in the destination are only replaced by solid ones; air is replaced by
                        // anything that's not air. TODO: how to handle minecraft:light blocks?
                        Material sourceMaterial = source.getMaterial(x, y, z);
                        if ((destinationMaterial.air) ? ((! sourceMaterial.air)) : sourceMaterial.solid) {
                            destination.setMaterial(x, y, z, sourceMaterial);
                            destination.setBlockLightLevel(x, y, z, source.getBlockLightLevel(x, y, z));
                            destination.setSkyLightLevel(x, y, z, source.getSkyLightLevel(x, y, z));
                            if (sourceMaterial.tileEntity) {
                                moveEntityTileData(destination, source, x, y, z, 0);
                            }
                        }
                    }
                }
            }
        }
        for (Entity entity: source.getEntities()) {
            destination.getEntities().add(entity);
        }
    }

    private boolean isWorldChunk(Dimension dimension, int x, int y) {
        return dimension.isTilePresent(x >> 3, y >> 3);
    }

    private boolean isBorderChunk(Dimension dimension, int x, int y) {
        final int tileX = x >> 3, tileY = y >> 3;
        final int borderSize = dimension.getBorderSize();
        if ((dimension.getBorder() == null) || (borderSize == 0)) {
            // There is no border configured, so definitely no border chunk
            return false;
        } else if (dimension.isTilePresent(tileX, tileY)) {
            // There is a tile here, so definitely no border chunk
            return false;
        } else {
            // Check whether there is a tile within a radius of *borderSize*,
            // in which case we are on a border tile
            for (int dx = -borderSize; dx <= borderSize; dx++) {
                for (int dy = -borderSize; dy <= borderSize; dy++) {
                    if (dimension.isTilePresent(tileX + dx, tileY + dy)) {
                        // Tile found, we are a border chunk!
                        return true;
                    }
                }
            }
            // No tiles found within a radius of *borderSize*, we are no border
            // chunk
            return false;
        }
    }

    private Chest createGoodiesChest(Platform platform) {
        List<InventoryItem> list = new ArrayList<>();
        if (platform == JAVA_MCREGION) {
            list.add(new InventoryItem((short) ITM_DIAMOND_SWORD, 0, 1, 0));
            list.add(new InventoryItem((short) ITM_DIAMOND_SHOVEL, 0, 1, 1));
            list.add(new InventoryItem((short) ITM_DIAMOND_PICKAXE, 0, 1, 2));
            list.add(new InventoryItem((short) ITM_DIAMOND_AXE, 0, 1, 3));
            list.add(new InventoryItem((short) BLK_SAPLING, 0, 64, 4));
            list.add(new InventoryItem((short) BLK_SAPLING, 1, 64, 5));
            list.add(new InventoryItem((short) BLK_SAPLING, 2, 64, 6));
            list.add(new InventoryItem((short) BLK_BROWN_MUSHROOM, 0, 64, 7));
            list.add(new InventoryItem((short) BLK_RED_MUSHROOM, 0, 64, 8));
            list.add(new InventoryItem((short) ITM_BONE, 0, 64, 9));
            list.add(new InventoryItem((short) ITM_WATER_BUCKET, 0, 1, 10));
            list.add(new InventoryItem((short) ITM_WATER_BUCKET, 0, 1, 11));
            list.add(new InventoryItem((short) ITM_COAL, 0, 64, 12));
            list.add(new InventoryItem((short) ITM_IRON_INGOT, 0, 64, 13));
            list.add(new InventoryItem((short) BLK_CACTUS, 0, 64, 14));
            list.add(new InventoryItem((short) ITM_SUGAR_CANE, 0, 64, 15));
            list.add(new InventoryItem((short) BLK_TORCH, 0, 64, 16));
            list.add(new InventoryItem((short) ITM_BED, 0, 1, 17));
            list.add(new InventoryItem((short) BLK_OBSIDIAN, 0, 64, 18));
            list.add(new InventoryItem((short) ITM_FLINT_AND_STEEL, 0, 1, 19));
            list.add(new InventoryItem((short) BLK_WOOD, 0, 64, 20));
            list.add(new InventoryItem((short) BLK_CRAFTING_TABLE, 0, 1, 21));
            list.add(new InventoryItem((short) BLK_END_PORTAL_FRAME, 0, 12, 22));
            list.add(new InventoryItem((short) ITM_EYE_OF_ENDER, 0, 12, 23));
        } else if (platform == JAVA_ANVIL) {
            list.add(new InventoryItem(ID_DIAMOND_SWORD, 1, 0));
            list.add(new InventoryItem(ID_DIAMOND_SHOVEL, 1, 1));
            list.add(new InventoryItem(ID_DIAMOND_PICKAXE, 1, 2));
            list.add(new InventoryItem(ID_DIAMOND_AXE, 1, 3));
            list.add(new InventoryItem(ID_SAPLING, 0, 64, 4));
            list.add(new InventoryItem(ID_SAPLING, 1, 64, 5));
            list.add(new InventoryItem(ID_SAPLING, 2, 64, 6));
            list.add(new InventoryItem(ID_BROWN_MUSHROOM, 64, 7));
            list.add(new InventoryItem(ID_RED_MUSHROOM, 64, 8));
            list.add(new InventoryItem(ID_BONE, 64, 9));
            list.add(new InventoryItem(ID_WATER_BUCKET, 1, 10));
            list.add(new InventoryItem(ID_WATER_BUCKET, 1, 11));
            list.add(new InventoryItem(ID_COAL, 64, 12));
            list.add(new InventoryItem(ID_IRON_INGOT, 64, 13));
            list.add(new InventoryItem(ID_CACTUS, 64, 14));
            list.add(new InventoryItem(ID_REEDS, 64, 15));
            list.add(new InventoryItem(ID_TORCH, 64, 16));
            list.add(new InventoryItem(ID_BED, 1, 17));
            list.add(new InventoryItem(ID_OBSIDIAN, 64, 18));
            list.add(new InventoryItem(ID_FLINT_AND_STEEL, 1, 19));
            list.add(new InventoryItem(ID_LOG, 64, 20));
            list.add(new InventoryItem(ID_CRAFTING_TABLE, 1, 21));
            list.add(new InventoryItem(ID_END_PORTAL_FRAME, 12, 22));
            list.add(new InventoryItem(ID_ENDER_EYE, 12, 23));
        } else {
            list.add(new InventoryItem(ID_DIAMOND_SWORD, 1, 0));
            list.add(new InventoryItem(ID_DIAMOND_SHOVEL, 1, 1));
            list.add(new InventoryItem(ID_DIAMOND_PICKAXE, 1, 2));
            list.add(new InventoryItem(ID_DIAMOND_AXE, 1, 3));
            list.add(new InventoryItem(ID_OAK_SAPLING, 64, 4));
            list.add(new InventoryItem(ID_SPRUCE_SAPLING, 64, 5));
            list.add(new InventoryItem(ID_BIRCH_SAPLING, 64, 6));
            list.add(new InventoryItem(ID_BROWN_MUSHROOM, 64, 7));
            list.add(new InventoryItem(ID_RED_MUSHROOM, 64, 8));
            list.add(new InventoryItem(ID_BONE, 64, 9));
            list.add(new InventoryItem(ID_WATER_BUCKET, 1, 10));
            list.add(new InventoryItem(ID_WATER_BUCKET, 1, 11));
            list.add(new InventoryItem(ID_COAL, 64, 12));
            list.add(new InventoryItem(ID_IRON_INGOT, 64, 13));
            list.add(new InventoryItem(ID_CACTUS, 64, 14));
            list.add(new InventoryItem(ID_SUGAR_CANE, 64, 15));
            list.add(new InventoryItem(ID_TORCH, 64, 16));
            list.add(new InventoryItem(ID_RED_BED, 1, 17));
            list.add(new InventoryItem(ID_OBSIDIAN, 64, 18));
            list.add(new InventoryItem(ID_FLINT_AND_STEEL, 1, 19));
            list.add(new InventoryItem(ID_OAK_LOG, 64, 20));
            list.add(new InventoryItem(ID_CRAFTING_TABLE, 1, 21));
            list.add(new InventoryItem(ID_END_PORTAL_FRAME, 12, 22));
            list.add(new InventoryItem(ID_ENDER_EYE, 12, 23));
        }
        Chest chest = new Chest(platform);
        chest.setItems(list);
        return chest;
    }

    private BlockBasedExportSettings getExportSettings(Dimension dimension, Platform platform) {
        final ExportSettings dimensionExportSettings = dimension.getExportSettings();
        if (dimensionExportSettings instanceof BlockBasedExportSettings) {
            return (BlockBasedExportSettings) dimensionExportSettings;
        } else {
            final BlockBasedExportSettings platformDefaultExportSettings = (BlockBasedExportSettings) platformProvider.getDefaultExportSettings(platform); // We wouldn't be here if the platform wasn't block-based, so the cast is safe
            return (platformDefaultExportSettings != null) ? platformDefaultExportSettings : DEFAULT_EXPORT_SETTINGS;
        }
    }

    protected final World2 world;
    protected final BlockBasedPlatformProvider platformProvider;
    protected final Semaphore performingFixups = new Semaphore(1);
    protected final Platform platform;
    protected final WorldExportSettings worldExportSettings;
    protected final boolean populateSupported;

    public static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMddHHmmss");


    private static final BlockBasedExportSettings DEFAULT_EXPORT_SETTINGS = new BlockBasedExportSettings() {
        @Override
        public boolean isCalculateSkyLight() {
            return true;
        }

        @Override
        public boolean isCalculateBlockLight() {
            return true;
        }

        @Override
        public boolean isCalculateLeafDistance() {
            return true;
        }

        @Override
        public boolean isRemoveFloatingLeaves() {
            return false;
        }
    };

    private static final Logger logger = LoggerFactory.getLogger(AbstractWorldExporter.class);

    public static class ExportResults {
        /**
         * Whether any chunks were actually generated for this region.
         */
        public boolean chunksGenerated;

        /**
         * Statistics for the generated chunks, if any
         */
        public final ChunkFactory.Stats stats = new ChunkFactory.Stats();

        /**
         * Fixups which have to be performed synchronously after all regions
         * have been generated
         */
        public List<Fixup> fixups;
    }
}