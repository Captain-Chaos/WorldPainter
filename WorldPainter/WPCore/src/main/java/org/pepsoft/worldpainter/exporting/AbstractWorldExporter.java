package org.pepsoft.worldpainter.exporting;

import org.jetbrains.annotations.NotNull;
import org.pepsoft.minecraft.*;
import org.pepsoft.util.Box;
import org.pepsoft.util.ParallelProgressManager;
import org.pepsoft.util.ProgressReceiver;
import org.pepsoft.util.ProgressReceiver.OperationCancelled;
import org.pepsoft.util.SubProgressReceiver;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.Platform;
import org.pepsoft.worldpainter.Tile;
import org.pepsoft.worldpainter.World2;
import org.pepsoft.worldpainter.gardenofeden.GardenExporter;
import org.pepsoft.worldpainter.gardenofeden.Seed;
import org.pepsoft.worldpainter.layers.CombinedLayer;
import org.pepsoft.worldpainter.layers.CustomLayer;
import org.pepsoft.worldpainter.layers.GardenCategory;
import org.pepsoft.worldpainter.layers.Layer;
import org.pepsoft.worldpainter.plugins.BlockBasedPlatformProvider;
import org.pepsoft.worldpainter.plugins.PlatformManager;
import org.pepsoft.worldpainter.vo.AttributeKeyVO;
import org.pepsoft.worldpainter.vo.EventVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.*;
import java.util.concurrent.*;

import static org.pepsoft.minecraft.Constants.*;
import static org.pepsoft.minecraft.Material.AIR;
import static org.pepsoft.worldpainter.Constants.*;

/**
 * An abstract {@link WorldExporter} for block based platforms.
 *
 * <p>Created by Pepijn on 11-12-2016.
 */
public abstract class AbstractWorldExporter implements WorldExporter {
    protected AbstractWorldExporter(World2 world) {
        if (world == null) {
            throw new NullPointerException();
        }
        this.world = world;
        this.platform = world.getPlatform();
        platformProvider = (BlockBasedPlatformProvider) PlatformManager.getInstance().getPlatformProvider(platform);
        this.selectedTiles = world.getTilesToExport();
        this.selectedDimensions = world.getDimensionsToExport();
        if ((selectedTiles != null) && (selectedDimensions.size() != 1)) {
            throw new IllegalArgumentException("When a tile selection is present exactly one dimension must be selected");
        }
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
     * <code>OperationCancelled</code> exception.
     * @throws RuntimeException If an exception occurs during the export and no
     * progress receiver has been specified.
     */
    protected ChunkFactory.Stats parallelExportRegions(Dimension dimension, File worldDir, ProgressReceiver progressReceiver) throws OperationCancelled {
        if (progressReceiver != null) {
            progressReceiver.setMessage("Exporting " + dimension.getName() + " dimension");
        }

        long start = System.currentTimeMillis();

        final Dimension ceiling;
        switch (dimension.getDim()) {
            case DIM_NORMAL:
                ceiling = dimension.getWorld().getDimension(DIM_NORMAL_CEILING);
                break;
            case DIM_NETHER:
                ceiling = dimension.getWorld().getDimension(DIM_NETHER_CEILING);
                break;
            case DIM_END:
                ceiling = dimension.getWorld().getDimension(DIM_END_CEILING);
                break;
            default:
                throw new IllegalArgumentException("Dimension " + dimension.getDim() + " not supported");
        }

        final ChunkFactory.Stats collectedStats = new ChunkFactory.Stats();
        dimension.rememberChanges();
        if (ceiling != null) {
            ceiling.rememberChanges();
        }
        try {
            final Map<Layer, LayerExporter> exporters = setupDimensionForExport(dimension);
            final Map<Layer, LayerExporter> ceilingExporters = (ceiling != null) ? setupDimensionForExport(ceiling) : null;

            // Determine regions to export
            int lowestRegionX = Integer.MAX_VALUE, highestRegionX = Integer.MIN_VALUE, lowestRegionZ = Integer.MAX_VALUE, highestRegionZ = Integer.MIN_VALUE;
            final Set<Point> regions = new HashSet<>(), exportedRegions = new HashSet<>();
            final boolean tileSelection = selectedTiles != null;
            if (tileSelection) {
                // Sanity check
                assert selectedDimensions.size() == 1;
                assert selectedDimensions.contains(dimension.getDim());
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
                for (Tile tile: dimension.getTiles()) {
                    // Also add regions for any bedrock wall and/or border
                    // tiles, if present
                    int r = (((dimension.getBorder() != null) && (! dimension.getBorder().isEndless())) ? dimension.getBorderSize() : 0)
                            + (((dimension.getBorder() == null) || (! dimension.getBorder().isEndless())) && dimension.isBedrockWall() ? 1 : 0);
                    for (int dx = -r; dx <= r; dx++) {
                        for (int dy = -r; dy <= r; dy++) {
                            int regionX = (tile.getX() + dx) >> 2;
                            int regionZ = (tile.getY() + dy) >> 2;
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
                    for (Tile tile: ceiling.getTiles()) {
                        int regionX = tile.getX() >> 2;
                        int regionZ = tile.getY() >> 2;
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

            final WorldPainterChunkFactory chunkFactory = new WorldPainterChunkFactory(dimension, exporters, platform, world.getMaxHeight());
            final WorldPainterChunkFactory ceilingChunkFactory = (ceiling != null) ? new WorldPainterChunkFactory(ceiling, ceilingExporters, platform, world.getMaxHeight()) : null;

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
                threads = Math.max(Math.min(Integer.parseInt(System.getProperty("org.pepsoft.worldpainter.threads")), sortedRegions.size()), 1);
            } else {
                threads = Math.max(Math.min(Math.min(maxThreadsByMem, runtime.availableProcessors()), sortedRegions.size()), 1);
            }
            logger.info("Using " + threads + " thread(s) for export (cores: " + runtime.availableProcessors() + ", available memory: " + (maxMemoryAvailable / 1048576L) + " MB)");

            final Map<Point, List<Fixup>> fixups = new HashMap<>();
            ExecutorService executor = Executors.newFixedThreadPool(threads, new ThreadFactory() {
                @Override
                public synchronized Thread newThread(Runnable r) {
                    Thread thread = new Thread(threadGroup, r, "Exporter-" + nextID++);
                    thread.setPriority(Thread.MIN_PRIORITY);
                    return thread;
                }

                private final ThreadGroup threadGroup = new ThreadGroup("Exporters");
                private int nextID = 1;
            });
            final RuntimeException[] exception = new RuntimeException[1];
            final ParallelProgressManager parallelProgressManager = (progressReceiver != null) ? new ParallelProgressManager(progressReceiver, regions.size()) : null;
            try {
                // Export each individual region
                for (Point region: sortedRegions) {
                    final Point regionCoords = region;
                    executor.execute(() -> {
                        ProgressReceiver progressReceiver1 = (parallelProgressManager != null) ? parallelProgressManager.createProgressReceiver() : null;
                        if (progressReceiver1 != null) {
                            try {
                                progressReceiver1.checkForCancellation();
                            } catch (OperationCancelled e) {
                                return;
                            }
                        }
                        try {
                            WorldRegion worldRegion = new WorldRegion(regionCoords.x, regionCoords.y, dimension.getMaxHeight(), platform);
                            ExportResults exportResults = null;
                            try {
                                exportResults = exportRegion(worldRegion, dimension, ceiling, regionCoords, tileSelection, exporters, ceilingExporters, chunkFactory, ceilingChunkFactory, (progressReceiver1 != null) ? new SubProgressReceiver(progressReceiver1, 0.0f, 0.9f) : null);
                                if (logger.isDebugEnabled()) {
                                    logger.debug("Generated region " + regionCoords.x + "," + regionCoords.y);
                                }
                                if (exportResults.chunksGenerated) {
                                    synchronized (collectedStats) {
                                        collectedStats.landArea += exportResults.stats.landArea;
                                        collectedStats.surfaceArea += exportResults.stats.surfaceArea;
                                        collectedStats.waterArea += exportResults.stats.waterArea;
                                    }
                                }
                            } finally {
                                if ((exportResults != null) && exportResults.chunksGenerated) {
                                    long saveStart = System.currentTimeMillis();
                                    worldRegion.save(worldDir, dimension.getDim());
                                    if (logger.isDebugEnabled()) {
                                        logger.debug("Saving region took {} ms", System.currentTimeMillis() - saveStart);
                                    }
                                }
                            }
                            synchronized (fixups) {
                                if ((exportResults.fixups != null) && (! exportResults.fixups.isEmpty())) {
                                    fixups.put(new Point(regionCoords.x, regionCoords.y), exportResults.fixups);
                                }
                                exportedRegions.add(regionCoords);
                            }
                            performFixupsIfNecessary(worldDir, dimension, regions, fixups, exportedRegions, progressReceiver1);
                        } catch (Throwable t) {
                            if (progressReceiver1 != null) {
                                progressReceiver1.exceptionThrown(t);
                            } else {
                                logger.error(t.getClass().getSimpleName() + " while exporting region {},{}", region.x, region.y, t);
                                if (exception[0] == null) {
                                    exception[0] = new RuntimeException(t.getClass().getSimpleName() + " while exporting region" + region.x + "," + region.y, exception[0]);
                                }
                                executor.shutdownNow();
                            }
                        }
                    });
                }
            } finally {
                executor.shutdown();
                try {
                    executor.awaitTermination(366, TimeUnit.DAYS);
                } catch (InterruptedException e) {
                    throw new RuntimeException("Thread interrupted while waiting for all tasks to finish", e);
                }
            }

            // If there is a progress receiver then we have reported any
            // exceptions to it, but if not then we should rethrow the recorded
            // exception, if any
            if (exception[0] != null) {
                throw exception[0];
            }

            // It's possible for there to be fixups left, if thread A was
            // performing fixups and thread B added new ones and then quit
            synchronized (fixups) {
                if (! fixups.isEmpty()) {
                    if (progressReceiver != null) {
                        progressReceiver.setMessage("Doing remaining fixups for " + dimension.getName());
                        progressReceiver.reset();
                    }
                    performFixups(worldDir, dimension, progressReceiver, fixups);
                }
            }

            // Calculate total size of dimension
            collectedStats.time = System.currentTimeMillis() - start;

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

            if (ceiling != null) {
                // Undo any changes we made (such as applying any combined layers)
                if (ceiling.undoChanges()) {
                    // TODO: some kind of cleverer undo mechanism (undo history
                    // cloning?) so we don't mess up the user's redo history
                    ceiling.clearRedo();
                    ceiling.armSavePoint();
                }
            }
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

    @NotNull
    protected Map<Layer, LayerExporter> setupDimensionForExport(Dimension dimension) {
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
                if ((layer instanceof CombinedLayer) && ((CombinedLayer) layer).isExport()) {
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

        // Remove layers which have been excluded for export
        allLayers.removeIf(layer -> (layer instanceof CustomLayer) && (!((CustomLayer) layer).isExport()));

        // Load all layer settings into the exporters
        for (Layer layer: allLayers) {
            @SuppressWarnings("unchecked")
            LayerExporter exporter = layer.getExporter();
            if (exporter != null) {
                exporter.setSettings(dimension.getLayerSettings(layer));
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
        int lowestChunkX = (regionCoords.x << 5) - 1;
        int highestChunkX = (regionCoords.x << 5) + 32;
        int lowestChunkY = (regionCoords.y << 5) - 1;
        int highestChunkY = (regionCoords.y << 5) + 32;
        int lowestRegionChunkX = lowestChunkX + 1;
        int highestRegionChunkX = highestChunkX - 1;
        int lowestRegionChunkY = lowestChunkY + 1;
        int highestRegionChunkY = highestChunkY - 1;
        ExportResults exportResults = new ExportResults();
        int chunkNo = 0;
        int ceilingDelta = dimension.getMaxHeight() - dimension.getCeilingHeight();
        for (int chunkX = lowestChunkX; chunkX <= highestChunkX; chunkX++) {
            for (int chunkY = lowestChunkY; chunkY <= highestChunkY; chunkY++) {
                ChunkFactory.ChunkCreationResult chunkCreationResult = createChunk(dimension, chunkFactory, tiles, chunkX, chunkY, tileSelection, exporters, ceiling);
                if (chunkCreationResult != null) {
                    if ((chunkX >= lowestRegionChunkX) && (chunkX <= highestRegionChunkX) && (chunkY >= lowestRegionChunkY) && (chunkY <= highestRegionChunkY)) {
                        exportResults.chunksGenerated = true;
                        exportResults.stats.landArea += chunkCreationResult.stats.landArea;
                        exportResults.stats.surfaceArea += chunkCreationResult.stats.surfaceArea;
                        exportResults.stats.waterArea += chunkCreationResult.stats.waterArea;
                    }
                    if (ceiling) {
                        Chunk invertedChunk = new InvertedChunk(chunkCreationResult.chunk, ceilingDelta);
                        Chunk existingChunk = minecraftWorld.getChunkForEditing(chunkX, chunkY);
                        if (existingChunk == null) {
                            existingChunk = platformProvider.createChunk(platform, chunkX, chunkY, world.getMaxHeight());
                            minecraftWorld.addChunk(existingChunk);
                        }
                        mergeChunks(invertedChunk, existingChunk);
                    } else {
                        minecraftWorld.addChunk(chunkCreationResult.chunk);
                    }
                }
                chunkNo++;
                if (progressReceiver != null) {
                    progressReceiver.setProgress((float) chunkNo / 1156);
                }
            }
        }
        if (logger.isDebugEnabled()) {
            logger.debug("End of first pass for region {},{}", regionCoords.x, regionCoords.y);
        }
        return exportResults;
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
                        // Insubstantial blocks in the destination are only
                        // replaced by solid ones; air is replaced by anything
                        // that's not air
                        Material sourceMaterial = source.getMaterial(x, y, z);
                        if ((destinationMaterial == AIR) ? (sourceMaterial != AIR) : sourceMaterial.solid) {
                            destination.setMaterial(x, y, z, source.getMaterial(x, y, z));
                            destination.setBlockLightLevel(x, y, z, source.getBlockLightLevel(x, y, z));
                            destination.setSkyLightLevel(x, y, z, source.getSkyLightLevel(x, y, z));
                        }
                    }
                }
            }
        }
    }

    protected List<Fixup> secondPass(List<Layer> secondaryPassLayers, Dimension dimension, MinecraftWorld minecraftWorld, Map<Layer, LayerExporter> exporters, Collection<Tile> tiles, Point regionCoords, ProgressReceiver progressReceiver) throws OperationCancelled {
        // Apply other secondary pass layers
        if (logger.isDebugEnabled()) {
            logger.debug("Start of second pass for region {},{}", regionCoords.x, regionCoords.y);
        }
        int layerCount = secondaryPassLayers.size(), counter = 0;
        Rectangle area = new Rectangle((regionCoords.x << 9) - 16, (regionCoords.y << 9) - 16, 544, 544);
        Rectangle exportedArea = new Rectangle((regionCoords.x << 9), (regionCoords.y << 9), 512, 512);
        List<Fixup> fixups = new ArrayList<>();
//        boolean frost = false;
        for (Layer layer: secondaryPassLayers) {
//            if (layer instanceof Frost) {
//                frost = true;
//                continue;
//            }
            @SuppressWarnings("unchecked")
            SecondPassLayerExporter exporter = (SecondPassLayerExporter) exporters.get(layer);
            if (logger.isDebugEnabled()) {
                logger.debug("Exporting layer {} for region {},{}", layer, regionCoords.x, regionCoords.y);
            }
            if (progressReceiver != null) {
                if (minecraftWorld instanceof InvertedWorld) {
                    progressReceiver.setMessage("Exporting layer " + layer + " for ceiling");
                } else {
                    progressReceiver.setMessage("Exporting layer " + layer);
                }
            }
            List<Fixup> layerFixups = exporter.render(dimension, area, exportedArea, minecraftWorld, platform);
            if (layerFixups != null) {
                fixups.addAll(layerFixups);
            }
            if (progressReceiver != null) {
                counter++;
                progressReceiver.setProgress((float) counter / layerCount);
            }
        }

        // Garden / seeds first and second pass
        GardenExporter gardenExporter = new GardenExporter();
        Set<Seed> firstPassProcessedSeeds = new HashSet<>();
        Set<Seed> secondPassProcessedSeeds = new HashSet<>();
        tiles.stream().filter(tile -> tile.getLayers().contains(GardenCategory.INSTANCE)).forEach(tile -> {
            gardenExporter.firstPass(dimension, tile, minecraftWorld, firstPassProcessedSeeds);
            gardenExporter.secondPass(dimension, tile, minecraftWorld, secondPassProcessedSeeds);
        });

        // Apply frost layer
        // TODO: why did we used to do this in a separate step? There must have been a reason...
//        if (frost) {
//            @SuppressWarnings("unchecked")
//            SecondPassLayerExporter<Layer> exporter = (SecondPassLayerExporter<Layer>) exporters.get(Frost.INSTANCE);
//            exporter.render(dimension, area, exportedArea, minecraftWorld);
//            if (progressReceiver != null) {
//                counter++;
//                progressReceiver.setProgress((float) counter / layerCount);
//            }
//        }

        // TODO: trying to do this for every region should work but is not very
        // elegant
        if ((dimension.getDim() == 0) && world.isCreateGoodiesChest()) {
            Point goodiesPoint = (Point) world.getSpawnPoint().clone();
            goodiesPoint.translate(3, 3);
            int height = Math.min(dimension.getIntHeightAt(goodiesPoint) + 1, dimension.getMaxHeight() - 1);
            minecraftWorld.setMaterialAt(goodiesPoint.x, goodiesPoint.y, height, Material.CHEST_NORTH);
            Chunk chunk = minecraftWorld.getChunk(goodiesPoint.x >> 4, goodiesPoint.y >> 4);
            if ((chunk != null) && (chunk.getTileEntities() != null)) {
                Chest goodiesChest = createGoodiesChest();
                goodiesChest.setX(goodiesPoint.x);
                goodiesChest.setY(height);
                goodiesChest.setZ(goodiesPoint.y);
                chunk.getTileEntities().add(goodiesChest);
            }
        }

        if (logger.isDebugEnabled()) {
            logger.debug("End of second pass for region {},{}", regionCoords.x, regionCoords.y);
        }
        return fixups;
    }

    protected void lightingPass(MinecraftWorld minecraftWorld, Point regionCoords, ProgressReceiver progressReceiver) throws OperationCancelled {
        if (progressReceiver != null) {
            progressReceiver.setMessage("Calculating primary light");
        }
        LightingCalculator lightingVolume = new LightingCalculator(minecraftWorld, platform);

        // Calculate primary light
        int lightingLowMark = Integer.MAX_VALUE, lightingHighMark = Integer.MIN_VALUE;
        int lowestChunkX = (regionCoords.x << 5) - 1;
        int highestChunkX = (regionCoords.x << 5) + 32;
        int lowestChunkY = (regionCoords.y << 5) - 1;
        int highestChunkY = (regionCoords.y << 5) + 32;
        int total = highestChunkX - lowestChunkX + 1, count = 0;
        for (int chunkX = lowestChunkX; chunkX <= highestChunkX; chunkX++) {
            for (int chunkY = lowestChunkY; chunkY <= highestChunkY; chunkY++) {
                Chunk chunk = minecraftWorld.getChunk(chunkX, chunkY);
                if (chunk != null) {
                    int[] levels = lightingVolume.calculatePrimaryLight(chunk);
                    if (levels[0] < lightingLowMark) {
                        lightingLowMark = levels[0];
                    }
                    if (levels[1] > lightingHighMark) {
                        lightingHighMark = levels[1];
                    }
                }
            }
            if (progressReceiver != null) {
                progressReceiver.setProgress(0.2f * ++count / total);
            }
        }

        if (lightingLowMark != Integer.MAX_VALUE) {
            if (progressReceiver != null) {
                progressReceiver.setMessage("Propagating light");
            }

            // Calculate secondary light
            Box originalDirtyArea = new Box((regionCoords.x << 9) - 16, ((regionCoords.x + 1) << 9) + 16, lightingLowMark, lightingHighMark + 1, (regionCoords.y << 9) - 16, ((regionCoords.y + 1) << 9) + 16);
            int originalVolume = originalDirtyArea.getVolume();
            Box dirtyArea = originalDirtyArea.clone();
            lightingVolume.setDirtyArea(dirtyArea);
            while (lightingVolume.calculateSecondaryLight()) {
                if (progressReceiver != null) {
                    progressReceiver.setProgress(0.2f + 0.8f * (originalVolume - dirtyArea.getVolume()) / originalVolume);
                }
            }
        }

        if (progressReceiver != null) {
            progressReceiver.setProgress(1.0f);
        }
    }

    protected ExportResults exportRegion(MinecraftWorld minecraftWorld, Dimension dimension, Dimension ceiling, Point regionCoords, boolean tileSelection, Map<Layer, LayerExporter> exporters, Map<Layer, LayerExporter> ceilingExporters, ChunkFactory chunkFactory, ChunkFactory ceilingChunkFactory, ProgressReceiver progressReceiver) throws OperationCancelled, IOException {
        if (progressReceiver != null) {
            progressReceiver.setMessage("Exporting region " + regionCoords.x + "," + regionCoords.y + " of " + dimension.getName());
        }
        int lowestTileX = (regionCoords.x << 2) - 1;
        int highestTileX = lowestTileX + 5;
        int lowestTileY = (regionCoords.y << 2) - 1;
        int highestTileY = lowestTileY + 5;
        Map<Point, Tile> tiles = new HashMap<>(), ceilingTiles = new HashMap<>();
        for (int tileX = lowestTileX; tileX <= highestTileX; tileX++) {
            for (int tileY = lowestTileY; tileY <= highestTileY; tileY++) {
                Point tileCoords = new Point(tileX, tileY);
                Tile tile = dimension.getTile(tileCoords);
                if ((tile != null) && ((! tileSelection) || dimension.getWorld().getTilesToExport().contains(tileCoords))) {
                    tiles.put(tileCoords, tile);
                }
                if (ceiling != null) {
                    tile = ceiling.getTile(tileCoords);
                    if ((tile != null) && ((! tileSelection) || dimension.getWorld().getTilesToExport().contains(tileCoords))) {
                        ceilingTiles.put(tileCoords, tile);
                    }
                }
            }
        }

        Set<Layer> allLayers = new HashSet<>(), allCeilingLayers = new HashSet<>();
        for (Tile tile: tiles.values()) {
            allLayers.addAll(tile.getLayers());
        }

        // Add layers that have been configured to be applied everywhere
        Set<Layer> minimumLayers = dimension.getMinimumLayers(), ceilingMinimumLayers = (ceiling != null) ? ceiling.getMinimumLayers() : null;
        allLayers.addAll(minimumLayers);

        // Remove layers which have been excluded for export
        allLayers.removeIf(layer -> (layer instanceof CustomLayer) && (!((CustomLayer) layer).isExport()));

        List<Layer> secondaryPassLayers = new ArrayList<>(), ceilingSecondaryPassLayers = new ArrayList<>();
        for (Layer layer: allLayers) {
            LayerExporter exporter = layer.getExporter();
            if (exporter instanceof SecondPassLayerExporter) {
                secondaryPassLayers.add(layer);
            }
        }
        Collections.sort(secondaryPassLayers);

        // Set up export of ceiling
        if (ceiling != null) {
            for (Tile tile: ceilingTiles.values()) {
                allCeilingLayers.addAll(tile.getLayers());
            }

            allCeilingLayers.addAll(ceilingMinimumLayers);

            // Remove layers which have been excluded for export
            allCeilingLayers.removeIf(layer -> (layer instanceof CustomLayer) && (!((CustomLayer) layer).isExport()));

            for (Layer layer: allCeilingLayers) {
                LayerExporter exporter = layer.getExporter();
                if (exporter instanceof SecondPassLayerExporter) {
                    ceilingSecondaryPassLayers.add(layer);
                }
            }
            Collections.sort(ceilingSecondaryPassLayers);
        }

        long t1 = System.currentTimeMillis();
        // First pass. Create terrain and apply layers which don't need access
        // to neighbouring chunks
        ExportResults exportResults = firstPass(minecraftWorld, dimension, regionCoords, tiles, tileSelection, exporters, chunkFactory, false, (progressReceiver != null) ? new SubProgressReceiver(progressReceiver, 0.0f, ((ceiling != null) ? 0.225f : 0.45f)) : null);

        ExportResults ceilingExportResults = null;
        if (ceiling != null) {
            // First pass for the ceiling. Create terrain and apply layers which
            // don't need access to neighbouring chunks
            ceilingExportResults = firstPass(minecraftWorld, ceiling, regionCoords, ceilingTiles, tileSelection, ceilingExporters, ceilingChunkFactory, true, (progressReceiver != null) ? new SubProgressReceiver(progressReceiver, 0.225f, 0.225f) : null);
        }

        if (exportResults.chunksGenerated || ((ceiling != null) && ceilingExportResults.chunksGenerated)) {
            // Second pass. Apply layers which need information from or apply
            // changes to neighbouring chunks
            long t2 = System.currentTimeMillis();
            List<Fixup> myFixups = secondPass(secondaryPassLayers, dimension, minecraftWorld, exporters, tiles.values(), regionCoords, (progressReceiver != null) ? new SubProgressReceiver(progressReceiver, 0.45f, (ceiling != null) ? 0.05f : 0.1f) : null);
            if ((myFixups != null) && (! myFixups.isEmpty())) {
                exportResults.fixups = myFixups;
            }

            if (ceiling != null) {
                // Second pass for ceiling. Apply layers which need information
                // from or apply changes to neighbouring chunks. Fixups are not
                // supported for the ceiling for now. TODO: implement
                secondPass(ceilingSecondaryPassLayers, ceiling, new InvertedWorld(minecraftWorld, ceiling.getMaxHeight() - ceiling.getCeilingHeight()), ceilingExporters, ceilingTiles.values(), regionCoords, (progressReceiver != null) ? new SubProgressReceiver(progressReceiver, 0.4f, 0.05f) : null);
            }

            // Post processing. Fix covered grass blocks, things like that
            long t3 = System.currentTimeMillis();
            PlatformManager.getInstance().getPostProcessor(platform).postProcess(minecraftWorld, new Rectangle(regionCoords.x << 9, regionCoords.y << 9, 512, 512), (progressReceiver != null) ? new SubProgressReceiver(progressReceiver, 0.55f, 0.1f) : null);

            // Third pass. Calculate lighting
            long t4 = System.currentTimeMillis();
            lightingPass(minecraftWorld, regionCoords, (progressReceiver != null) ? new SubProgressReceiver(progressReceiver, 0.65f, 0.35f) : null);
            long t5 = System.currentTimeMillis();
            if ("true".equalsIgnoreCase(System.getProperty("org.pepsoft.worldpainter.devMode"))) {
                String timingMessage = (t2 - t1) + ", " + (t3 - t2) + ", " + (t4 - t3) + ", " + (t5 - t4) + ", " + (t5 - t1);
//                System.out.println("Export timing: " + timingMessage);
                synchronized (TIMING_FILE_LOCK) {
                    try (PrintWriter out = new PrintWriter(new FileOutputStream("exporttimings.csv", true))) {
                        out.println(timingMessage);
                    }
                }
            }
        }

        return exportResults;
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
            if (dimension.getTile(tileCoords) != null) {
                return chunkFactory.createChunk(chunkX, chunkY);
            } else if ((! ceiling) && (! endlessBorder)) {
                // Might be a border or bedrock wall chunk (but not if this is a
                // ceiling dimension or the border is an endless border)
                if (border && isBorderChunk(dimension, chunkX, chunkY)) {
                    return BorderChunkFactory.create(chunkX, chunkY, dimension, exporters);
                } else if (dimension.isBedrockWall()
                        && (border
                            ? (isBorderChunk(dimension, chunkX - 1, chunkY) || isBorderChunk(dimension, chunkX, chunkY - 1) || isBorderChunk(dimension, chunkX + 1, chunkY) || isBorderChunk(dimension, chunkX, chunkY + 1))
                            : (isWorldChunk(dimension, chunkX - 1, chunkY) || isWorldChunk(dimension, chunkX, chunkY - 1) || isWorldChunk(dimension, chunkX + 1, chunkY) || isWorldChunk(dimension, chunkX, chunkY + 1)))) {
                    // Bedrock wall is turned on and a neighbouring chunk is a
                    // border chunk (if there is a border), or a world chunk (if
                    // there is no border)
                    return BedrockWallChunk.create(chunkX, chunkY, dimension);
                } else {
                    // Outside known space
                    return null;
                }
            } else {
                // Not a world tile, and we're a ceiling dimension, or the
                // border is an endless border, so we don't export borders and
                // bedrock walls
                return null;
            }
        }
    }

    protected void setPlatform(Platform platform) {
        world.setPlatform(platform);
        this.platform = platform;
    }

    private boolean isWorldChunk(Dimension dimension, int x, int y) {
        return dimension.getTile(x >> 3, y >> 3) != null;
    }

    private boolean isBorderChunk(Dimension dimension, int x, int y) {
        final int tileX = x >> 3, tileY = y >> 3;
        final int borderSize = dimension.getBorderSize();
        if ((dimension.getBorder() == null) || (borderSize == 0)) {
            // There is no border configured, so definitely no border chunk
            return false;
        } else if (dimension.getTile(tileX, tileY) != null) {
            // There is a tile here, so definitely no border chunk
            return false;
        } else {
            // Check whether there is a tile within a radius of *borderSize*,
            // in which case we are on a border tile
            for (int dx = -borderSize; dx <= borderSize; dx++) {
                for (int dy = -borderSize; dy <= borderSize; dy++) {
                    if (dimension.getTile(tileX + dx, tileY + dy) != null) {
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

    private Chest createGoodiesChest() {
        // TODOMC13 migrate to Minecraft 1.13
        List<InventoryItem> list = new ArrayList<>();
        list.add(new InventoryItem(ITM_DIAMOND_SWORD,    0,  1,  0));
        list.add(new InventoryItem(ITM_DIAMOND_SHOVEL,   0,  1,  1));
        list.add(new InventoryItem(ITM_DIAMOND_PICKAXE,  0,  1,  2));
        list.add(new InventoryItem(ITM_DIAMOND_AXE,      0,  1,  3));
        list.add(new InventoryItem(BLK_SAPLING,          0, 64,  4));
        list.add(new InventoryItem(BLK_SAPLING,          1, 64,  5));
        list.add(new InventoryItem(BLK_SAPLING,          2, 64,  6));
        list.add(new InventoryItem(BLK_BROWN_MUSHROOM,   0, 64,  7));
        list.add(new InventoryItem(BLK_RED_MUSHROOM,     0, 64,  8));
        list.add(new InventoryItem(ITM_BONE,             0, 64,  9));
        list.add(new InventoryItem(ITM_WATER_BUCKET,     0,  1, 10));
        list.add(new InventoryItem(ITM_WATER_BUCKET,     0,  1, 11));
        list.add(new InventoryItem(ITM_COAL,             0, 64, 12));
        list.add(new InventoryItem(ITM_IRON_INGOT,       0, 64, 13));
        list.add(new InventoryItem(BLK_CACTUS,           0, 64, 14));
        list.add(new InventoryItem(ITM_SUGAR_CANE,       0, 64, 15));
        list.add(new InventoryItem(BLK_TORCH,            0, 64, 16));
        list.add(new InventoryItem(ITM_BED,              0,  1, 17));
        list.add(new InventoryItem(BLK_OBSIDIAN,         0, 64, 18));
        list.add(new InventoryItem(ITM_FLINT_AND_STEEL,  0,  1, 19));
        list.add(new InventoryItem(BLK_WOOD,             0, 64, 20));
        list.add(new InventoryItem(BLK_CRAFTING_TABLE,   0,  1, 21));
        list.add(new InventoryItem(BLK_END_PORTAL_FRAME, 0, 12, 22));
        list.add(new InventoryItem(ITM_EYE_OF_ENDER,     0, 12, 23));
        Chest chest = new Chest();
        chest.setItems(list);
        return chest;
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
    protected void performFixupsIfNecessary(final File worldDir, final Dimension dimension, final Set<Point> regionsToExport, final Map<Point, List<Fixup>> fixups, final Set<Point> exportedRegions, final ProgressReceiver progressReceiver) throws ProgressReceiver.OperationCancelled {
        if (performingFixups.tryAcquire()) {
            try {
                Map<Point, List<Fixup>> myFixups = new HashMap<>();
                synchronized (fixups) {
                    for (Iterator<Map.Entry<Point, List<Fixup>>> i = fixups.entrySet().iterator(); i.hasNext(); ) {
                        Map.Entry<Point, List<Fixup>> entry = i.next();
                        Point fixupRegionCoords = entry.getKey();
                        if (isReadyForFixups(regionsToExport, exportedRegions, fixupRegionCoords)) {
                            myFixups.put(fixupRegionCoords, entry.getValue());
                            i.remove();
                        }
                    }
                }
                if (! myFixups.isEmpty()) {
                    performFixups(worldDir, dimension, (progressReceiver != null) ? new SubProgressReceiver(progressReceiver, 0.9f, 0.1f) : null, myFixups);
                }
            } finally {
                performingFixups.release();
            }
        }
    }

    protected void performFixups(final File worldDir, final Dimension dimension, final ProgressReceiver progressReceiver, final Map<Point, List<Fixup>> fixups) throws OperationCancelled {
        long start = System.currentTimeMillis();
        int count = 0, total = 0;
        for (Map.Entry<Point, List<Fixup>> entry: fixups.entrySet()) {
            total += entry.getValue().size();
        }
        // Make sure to honour the read-only layer: TODO: this means nothing at the moment. Is it still relevant?
        try (CachingMinecraftWorld minecraftWorld = new CachingMinecraftWorld(worldDir, dimension.getDim(), dimension.getMaxHeight(), platform, false, 512)) {
            for (Map.Entry<Point, List<Fixup>> entry: fixups.entrySet()) {
                if (progressReceiver != null) {
                    progressReceiver.setMessage("Performing fixups for region " + entry.getKey().x + "," + entry.getKey().y);
                }
                List<Fixup> regionFixups = entry.getValue();
                if (logger.isDebugEnabled()) {
                    logger.debug("Performing " + regionFixups.size() + " fixups for region " + entry.getKey().x + "," + entry.getKey().y);
                }
                for (Fixup fixup: regionFixups) {
                    fixup.fixup(minecraftWorld, dimension, platform);
                    if (progressReceiver != null) {
                        progressReceiver.setProgress((float) ++count / total);
                    }
                }
            }
        }
        if (logger.isTraceEnabled()) {
            logger.trace("Fixups for " + fixups.size() + " regions took " + (System.currentTimeMillis() - start) + " ms");
        }
    }

    protected final World2 world;
    protected final BlockBasedPlatformProvider platformProvider;
    protected final Set<Integer> selectedDimensions;
    protected final Set<Point> selectedTiles;
    protected final Semaphore performingFixups = new Semaphore(1);
    protected Platform platform;

    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMddHHmmss");
    private static final Object TIMING_FILE_LOCK = new Object();
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