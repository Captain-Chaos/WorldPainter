/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.merging;

import org.jnbt.NBTInputStream;
import org.jnbt.Tag;
import org.pepsoft.minecraft.*;
import org.pepsoft.util.FileUtils;
import org.pepsoft.util.ParallelProgressManager;
import org.pepsoft.util.ProgressReceiver;
import org.pepsoft.util.SubProgressReceiver;
import org.pepsoft.worldpainter.*;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.exporting.*;
import org.pepsoft.worldpainter.history.HistoryEntry;
import org.pepsoft.worldpainter.layers.*;
import org.pepsoft.worldpainter.plugins.PlatformManager;
import org.pepsoft.worldpainter.util.FileInUseException;
import org.pepsoft.worldpainter.vo.EventVO;

import java.awt.*;
import java.io.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toMap;
import static org.pepsoft.minecraft.Constants.*;
import static org.pepsoft.minecraft.DataType.REGION;
import static org.pepsoft.minecraft.Material.*;
import static org.pepsoft.util.MathUtils.clamp;
import static org.pepsoft.util.mdc.MDCUtils.doWithMdcContext;
import static org.pepsoft.worldpainter.Constants.*;
import static org.pepsoft.worldpainter.Dimension.Anchor.*;
import static org.pepsoft.worldpainter.Dimension.Role.MASTER;
import static org.pepsoft.worldpainter.Platform.Capability.*;
import static org.pepsoft.worldpainter.platforms.PlatformUtils.determineNativePlatforms;

/**
 *
 * @author pepijn
 */
@SuppressWarnings("StringConcatenationInsideStringBufferAppend") // Readability
public class JavaWorldMerger extends JavaWorldExporter { // TODO can this be made a BlockBasedPlatformProviderWorldMerger?
    public JavaWorldMerger(World2 world, WorldExportSettings exportSettings, File mapDir, Platform platform) {
        super(world, exportSettings, platform);
        if (mapDir == null) {
            throw new NullPointerException();
        }
        if (! mapDir.isDirectory()) {
            throw new IllegalArgumentException(mapDir + " does not exist or is not a directory");
        } else if (! new File(mapDir, "level.dat").isFile()) {
            throw new IllegalArgumentException(mapDir + " does not contain a level.dat file");
        }
        worldDir = mapDir;
    }
    
    public File getMapDir() {
        return worldDir;
    }
    
    public boolean isReplaceChunks() {
        return replaceChunks;
    }
    
    public void setReplaceChunks(boolean replaceChunks) {
        this.replaceChunks = replaceChunks;
    }

    public boolean isMergeBlocksAboveGround() {
        return mergeBlocksAboveGround;
    }

    public void setMergeBlocksAboveGround(boolean mergeBlocksAboveGround) {
        this.mergeBlocksAboveGround = mergeBlocksAboveGround;
    }

    public boolean isMergeBlocksUnderground() {
        return mergeBlocksUnderground;
    }

    public void setMergeBlocksUnderground(final boolean mergeBlocksUnderground) {
        this.mergeBlocksUnderground = mergeBlocksUnderground;
    }

    public boolean isMergeBiomesAboveGround() {
        return mergeBiomesAboveGround;
    }

    public void setMergeBiomesAboveGround(boolean mergeBiomesAboveGround) {
        this.mergeBiomesAboveGround = mergeBiomesAboveGround;
    }

    public boolean isMergeBiomesUnderground() {
        return mergeBiomesUnderground;
    }

    public void setMergeBiomesUnderground(boolean mergeBiomesUnderground) {
        this.mergeBiomesUnderground = mergeBiomesUnderground;
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
     * Perform sanity checks to ensure the merge can proceed. Throws an {@link IllegalArgumentException} if the check
     * fails, with the message describing the problem. Returns the loaded Minecraft level object as a convenience.
     *
     * @return The loaded Minecraft level object, for convenience.
     * @throws IllegalArgumentException If there is a problem that would prevent the merge from completing.
     * @throws IOException If the level.dat file could not be read due to an I/O error.
     */
    public JavaLevel performSanityChecks() throws IOException {
        if (! (replaceChunks || mergeBiomesAboveGround || mergeBiomesUnderground || mergeBlocksAboveGround || mergeBlocksUnderground || clearTrees || clearResources || clearVegetation || clearManMadeAboveGround || clearManMadeBelowGround || fillCaves)) {
            throw new IllegalArgumentException("Nothing to do");
        } else if (replaceChunks && (mergeBiomesAboveGround || mergeBiomesUnderground || mergeBlocksAboveGround || mergeBlocksUnderground || clearTrees || clearResources || clearVegetation || clearManMadeAboveGround || clearManMadeBelowGround || fillCaves)) {
            throw new IllegalArgumentException("replaceChunks is mutually exclusive with other merge options");
        }

        // Read existing level.dat file
        JavaLevel level = JavaLevel.load(new File(worldDir, "level.dat"));

        // Sanity checks
        int version = level.getVersion();
        if ((version != VERSION_MCREGION) && (version != VERSION_ANVIL)) {
            throw new IllegalArgumentException("Version of existing map not supported: 0x" + Integer.toHexString(version));
        }

        if (mergeBiomesAboveGround && (! (platform.capabilities.contains(BIOMES) || platform.capabilities.contains(BIOMES_3D) || platform.capabilities.contains(NAMED_BIOMES)))) {
            throw new IllegalArgumentException(platform.displayName + " maps do not support biomes");
        }
        if (mergeBiomesUnderground && (! (platform.capabilities.contains(BIOMES_3D) || platform.capabilities.contains(NAMED_BIOMES)))) {
            throw new IllegalArgumentException(platform.displayName + " maps do not support 3D biomes");
        }

        if (mergeBlocksAboveGround || mergeBlocksUnderground || mergeBiomesUnderground) {
            // TODO support different map heights; just give a warning
            if (level.getMinHeight() != world.getMinHeight()) {
                throw new IllegalArgumentException("Existing map has different min height (" + level.getMinHeight() + ") than WorldPainter world (" + world.getMinHeight() + ")");
            }
            if (level.getMaxHeight() != world.getMaxHeight()) {
                throw new IllegalArgumentException("Existing map has different max height (" + level.getMaxHeight() + ") than WorldPainter world (" + world.getMaxHeight() + ")");
            }

            // Dimension sanity checks
            for (Dimension dimension: world.getDimensions()) {
                final int dim = dimension.getAnchor().dim;
                if ((dim < 0) || ((worldExportSettings.getDimensionsToExport() != null) && (! worldExportSettings.getDimensionsToExport().contains(dim)))) {
                    // Skip ceiling dimensions, or dimensions that are not going to be merged
                    continue;
                }
                final int mapDimMinHeight, mapDimMaxHeight;
                switch (dim) {
                    case DIM_NORMAL:
                        mapDimMinHeight = level.getMinHeight();
                        mapDimMaxHeight = level.getMaxHeight();
                        break;
                    case DIM_NETHER:
                        mapDimMinHeight = 0;
                        mapDimMaxHeight = DEFAULT_MAX_HEIGHT_NETHER;
                        break;
                    case DIM_END:
                        mapDimMinHeight = 0;
                        mapDimMaxHeight = DEFAULT_MAX_HEIGHT_END;
                        break;
                    default:
                        throw new IllegalArgumentException("Dimension " + dimension.getName() + " not supported for Merging");
                }
                if (mapDimMinHeight != dimension.getMinHeight()) {
                    throw new IllegalArgumentException("Dimension " + dimension.getName() + " has different min height (" + dimension.getMinHeight() + ") than existing map (" + mapDimMinHeight + ")");
                }
                if (mapDimMaxHeight != dimension.getMaxHeight()) {
                    throw new IllegalArgumentException("Dimension " + dimension.getName() + " has different max height (" + dimension.getMaxHeight() + ") than existing map (" + mapDimMaxHeight + ")");
                }
            }
        }

        return level;
    }

    public void merge(File backupDir, ProgressReceiver progressReceiver) throws IOException, ProgressReceiver.OperationCancelled {
        logger.info("Merging world " + world.getName() + " with map at " + worldDir);
        
        // Read existing level.dat file and perform sanity checks
        final JavaLevel level = performSanityChecks();

        doWithMdcContext(() -> {
            // Record start of export
            long start = System.currentTimeMillis();

            // Backup existing level
            if (! worldDir.renameTo(backupDir)) {
                throw new FileInUseException("Could not move " + worldDir + " to " + backupDir);
            }
            if (! worldDir.mkdirs()) {
                throw new IOException("Could not create " + worldDir);
            }

            // Modify it if necessary and write it to the new level
            final Set<Integer> selectedDimensions = worldExportSettings.getDimensionsToExport();
            if ((selectedDimensions == null) || selectedDimensions.contains(DIM_NORMAL)) {
                Dimension surfaceDimension = world.getDimension(NORMAL_DETAIL);
                level.setSeed(surfaceDimension.getMinecraftSeed());
                Point spawnPoint = world.getSpawnPoint();
                level.setSpawnX(spawnPoint.x);
                if (world.getSpawnPointDimension() != null) {
                    final Dimension spawnDimension = world.getDimension(world.getSpawnPointDimension());
                    level.setSpawnY(Math.max(spawnDimension.getIntHeightAt(spawnPoint.x, spawnPoint.y), spawnDimension.getWaterLevelAt(spawnPoint.x, spawnPoint.y)) + 1);
                } else {
                    level.setSpawnY(Math.max(getIntHeightAt(DIM_NORMAL, spawnPoint.x, spawnPoint.y), getWaterLevelAt(DIM_NORMAL, spawnPoint.x, spawnPoint.y)) + 1);
                }
                level.setSpawnZ(spawnPoint.y);
            }

            // TODO: copy EVERYTHING and then operate solely on the copied map? No need to copy things from the backup

            // Copy everything that we are not going to generate
            File[] files = backupDir.listFiles();
            for (File file: files) {
                if ((! file.getName().equalsIgnoreCase("level.dat"))
                        && (! file.getName().equalsIgnoreCase("level.dat_old"))
                        && (! file.getName().equalsIgnoreCase("session.lock"))
                        && (((selectedDimensions != null) && (! selectedDimensions.contains(DIM_NORMAL))) || (! file.getName().equalsIgnoreCase("region")))
                        && (((selectedDimensions != null) && (! selectedDimensions.contains(DIM_NORMAL))) || (! file.getName().equalsIgnoreCase("entities")))
                        && (! file.getName().equalsIgnoreCase("maxheight.txt"))
                        && (! file.getName().equalsIgnoreCase("Height.txt"))
                        && (((selectedDimensions != null) && (! selectedDimensions.contains(DIM_NETHER))) || (! file.getName().equalsIgnoreCase("DIM-1"))) // TODO still copy dirs other than region and entities
                        && (((selectedDimensions != null) && (! selectedDimensions.contains(DIM_END))) || (! file.getName().equalsIgnoreCase("DIM1")))) { // TODO still copy dirs other than region and entities
                    if (file.isFile()) {
                        FileUtils.copyFileToDir(file, worldDir);
                    } else if (file.isDirectory()) {
                        FileUtils.copyDir(file, new File(worldDir, file.getName()));
                    } else {
                        logger.warn("Not copying " + file + "; not a regular file or directory");
                    }
                }
            }

            // Save the level.dat file. This will also create a session.lock file, hopefully kicking out any Minecraft
            // instances which may have the map open:
            level.save(worldDir);

            if ((selectedDimensions == null) ? (world.isDimensionPresent(NORMAL_DETAIL)) : selectedDimensions.contains(DIM_NORMAL)) {
                mergeDimension(worldDir, backupDir, world.getDimension(NORMAL_DETAIL), progressReceiver); // TODO: this should be a SubProgressReceiver if we are exporting more than one dimension, or we should reset it
            }
            if ((selectedDimensions == null) ? (world.isDimensionPresent(NETHER_DETAIL)) : selectedDimensions.contains(DIM_NETHER)) {
                mergeDimension(worldDir, backupDir, world.getDimension(NETHER_DETAIL), progressReceiver); // TODO: this should be a SubProgressReceiver if we are exporting more than one dimension, or we should reset it
            }
            if ((selectedDimensions == null) ? (world.isDimensionPresent(END_DETAIL)) : selectedDimensions.contains(DIM_END)) {
                mergeDimension(worldDir, backupDir, world.getDimension(END_DETAIL), progressReceiver); // TODO: this should be a SubProgressReceiver if we are exporting more than one dimension, or we should reset it
            }

            // TODO: move player positions if necessary

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
                }).collect(joining(", "));
                world.addHistoryEntry(HistoryEntry.WORLD_MERGED_PARTIAL, level.getName(), worldDir, dimNames);
            }
            final File levelDatFile = new File(worldDir, "level.dat");
            if (! worldDir.equals(levelDatFile)) {
                world.setMergedWith(levelDatFile);
            }

            // Log an event
            Configuration config = Configuration.getInstance();
            if (config != null) {
                EventVO event = new EventVO(EVENT_KEY_ACTION_MERGE_WORLD).duration(System.currentTimeMillis() - start);
                event.setAttribute(EventVO.ATTRIBUTE_TIMESTAMP, new Date(start));
                event.setAttribute(ATTRIBUTE_KEY_MAX_HEIGHT, world.getMaxHeight());
                event.setAttribute(ATTRIBUTE_KEY_PLATFORM, platform.displayName);
                event.setAttribute(ATTRIBUTE_KEY_PLATFORM_ID, platform.id);
                event.setAttribute(ATTRIBUTE_KEY_MAP_FEATURES, world.isMapFeatures());
                event.setAttribute(ATTRIBUTE_KEY_GAME_TYPE_NAME, world.getGameType().name());
                event.setAttribute(ATTRIBUTE_KEY_ALLOW_CHEATS, world.isAllowCheats());
                event.setAttribute(ATTRIBUTE_KEY_GENERATOR, world.getDimension(NORMAL_DETAIL).getGenerator().getType().name());
                if ((selectedDimensions == null) || selectedDimensions.contains(DIM_NORMAL)) {
                    Dimension surfaceDimension = world.getDimension(NORMAL_DETAIL);
                    event.setAttribute(ATTRIBUTE_KEY_TILES, surfaceDimension.getTileCount());
                    logLayers(surfaceDimension, event, "");
                }
                if (world.getImportedFrom() == null) {
                    event.setAttribute(ATTRIBUTE_KEY_IMPORTED_WORLD, false);
                }
                config.logEvent(event);
            }

            aborted = false;
            return null;
        }, "world.name", world.getName(), "platform.id", platform.id, "world.minHeight", world.getMinHeight(), "world.maxHeight", world.getMaxHeight(), "mapDir", worldDir, "level.platform.id", level.getPlatform().id, "level.maxHeight", level.getMaxHeight());
    }

    public String getWarnings() {
        return warnings;
    }

    public boolean isAborted() {
        return aborted;
    }

    private void mergeDimension(final File worldDir, File backupWorldDir, final Dimension dimension, ProgressReceiver progressReceiver) {
        doWithMdcContext(() -> {
            if (progressReceiver != null) {
                progressReceiver.setMessage("merging " + dimension.getName() + " dimension");
            }
            final File dimensionDir, backupDimensionDir;
            final Dimension.Anchor anchor = dimension.getAnchor();
            final int dim = anchor.dim;
            switch (dim) {
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
                    throw new IllegalArgumentException("Dimension " + dim + " not supported");
            }
            final Set<DataType> dataTypes = platformProvider.getDataTypes(platform);
            for (DataType dataType: dataTypes) {
                File regionDir = new File(dimensionDir, dataType.name().toLowerCase());
                if (! regionDir.exists()) {
                    if (! regionDir.mkdirs()) {
                        throw new RuntimeException("Could not create directory " + regionDir);
                    }
                }
            }
            final ChunkFactory.Stats stats = new ChunkFactory.Stats(); // TODO actually return these stats

            final Dimension master = dimension.getWorld().getDimension(new Dimension.Anchor(dim, MASTER, false, 0));
            final Dimension combined = (master != null) ? new FlatteningDimension(dimension, new ScaledDimension(master, 16.0f)) : dimension;
            // TODO add ceiling support

            final Set<Point> selectedTiles = worldExportSettings.getTilesToExport();
            // TODO is it too late to do this now because we already created the combined dimension:
            final Map<Dimension, Object> savedSettings = world.getDimensions().stream()
                    .filter(d -> d.getAnchor().dim == dim)
                    .collect(toMap(identity(), d -> setupDimensionForExport(d, selectedTiles)));
            try {

                // Sort tiles into regions
                final Map<Point, Map<Point, Tile>> tilesByRegion = getTilesByRegion(combined);

                // Read the region coordinates of the existing map
                final File backupRegionDir = new File(backupDimensionDir, "region");
                // TODO: support any platform
                final Set<Point> allRegionCoords = new HashSet<>();
                File[] existingRegionFiles = platformProvider.getRegionFiles(platform, backupRegionDir, REGION);
                final Map<Point, File> existingRegions = new HashMap<>();
                if (existingRegionFiles != null) {
                    for (File file: existingRegionFiles) {
                        if (file.length() == 0L) {
                            continue;
                        }
                        final String[] parts = file.getName().split("\\.");
                        final int regionX = Integer.parseInt(parts[1]);
                        final int regionZ = Integer.parseInt(parts[2]);
                        existingRegions.put(new Point(regionX, regionZ), file);
                    }
                    allRegionCoords.addAll(existingRegions.keySet());
                }
                allRegionCoords.addAll(tilesByRegion.keySet());
                final int lowestRegionX;
                final int highestRegionX;
                final int lowestRegionZ;
                final int highestRegionZ;
                if (allRegionCoords.isEmpty()) {
                    // This means that the dimension is empty or no tiles were selected, and the existing map also has no
                    // region files for the dimension. Very strange, but it has been observed in the wild
                    lowestRegionX = highestRegionX = lowestRegionZ = highestRegionZ = 0;
                } else {
                    lowestRegionX = allRegionCoords.stream().mapToInt(p -> p.x).min().getAsInt();
                    highestRegionX = allRegionCoords.stream().mapToInt(p -> p.x).max().getAsInt();
                    lowestRegionZ = allRegionCoords.stream().mapToInt(p -> p.y).min().getAsInt();
                    highestRegionZ = allRegionCoords.stream().mapToInt(p -> p.y).max().getAsInt();
                }

                // Find all the existing region files of other types than REGION
                final Map<Point, Map<DataType, File>> additionalRegions = new HashMap<>();
                for (DataType dataType: dataTypes) {
                    if (dataType == REGION) {
                        // Already handled above
                        continue;
                    }
                    existingRegionFiles = platformProvider.getRegionFiles(platform, backupRegionDir, dataType);
                    if (existingRegionFiles != null) {
                        for (File file: existingRegionFiles) {
                            if (file.length() == 0L) {
                                continue;
                            }
                            final String[] parts = file.getName().split("\\.");
                            final Point coords = new Point(Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
                            additionalRegions.computeIfAbsent(coords, p -> new HashMap<>()).put(dataType, file);
                        }
                    }
                }

                // Sort the regions to export the first two rows together, and then
                // row by row, to get the optimum tempo of performing fixups
                final List<Point> sortedRegions = new ArrayList<>(allRegionCoords.size());
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
                final Map<Point, List<Fixup>> fixups = new HashMap<>();
                final Set<Point> exportedRegions = new HashSet<>();
                final ExecutorService executor = createExecutorService("merging", allRegionCoords.size() + additionalRegions.size());
                final ParallelProgressManager parallelProgressManager = (progressReceiver != null) ? new ParallelProgressManager(progressReceiver, sortedRegions.size() + additionalRegions.size()) : null;
                final AtomicBoolean abort = new AtomicBoolean();
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
                                    if (abort.get()) {
                                        return;
                                    }
                                    final ProgressReceiver progressReceiver1 = (parallelProgressManager != null) ? parallelProgressManager.createProgressReceiver() : null;
                                    if (progressReceiver1 != null) {
                                        try {
                                            progressReceiver1.checkForCancellation();
                                        } catch (ProgressReceiver.OperationCancelled e) {
                                            abort.set(true);
                                            return;
                                        }
                                    }
                                    try {
                                        final Map<Layer, LayerExporter> exporters = getExportersForRegion(combined, regionCoords);
                                        final WorldPainterChunkFactory chunkFactory = new WorldPainterChunkFactory(combined, exporters, platform, combined.getMaxHeight());

                                        final List<Fixup> regionFixups = new ArrayList<>();
                                        final WorldRegion minecraftWorld = new WorldRegion(regionCoords.x, regionCoords.y, combined.getMinHeight(), combined.getMaxHeight(), platform);
                                        try {
                                            final String regionWarnings = mergeRegion(minecraftWorld, backupRegionDir, combined, regionCoords, tiles, selectedTiles != null, exporters, chunkFactory, regionFixups, (progressReceiver1 != null) ? new SubProgressReceiver(progressReceiver1, 0.0f, 0.9f) : null);
                                            if (regionWarnings != null) {
                                                synchronized (JavaWorldMerger.this){
                                                    if (warnings == null) {
                                                        warnings = regionWarnings;
                                                    } else {
                                                        warnings = warnings + regionWarnings;
                                                    }
                                                }
                                            }
                                            if (logger.isDebugEnabled()) {
                                                logger.debug("Merged region " + regionCoords.x + "," + regionCoords.y);
                                            }
                                        } finally {
                                            minecraftWorld.save(worldDir, dim);
                                        }
                                        synchronized (fixups) {
                                            if (! regionFixups.isEmpty()) {
                                                fixups.put(new Point(regionCoords.x, regionCoords.y), regionFixups);
                                            }
                                            exportedRegions.add(regionCoords);
                                        }
                                        try {
                                            performFixupsIfNecessary(worldDir, combined, allRegionCoords, fixups, exportedRegions, stats, progressReceiver1);
                                        } catch (InvalidMapException e) {
                                            throw createInvalidMapException(e.getMessage(), backupWorldDir);
                                        }
                                    } catch (Throwable t) {
                                        logger.error("{} while merging region {},{} (message: \"{}\")", t.getClass().getSimpleName(), regionCoords.x, regionCoords.y, t.getMessage(), t);
                                        abort.set(true);
                                        if (progressReceiver1 != null) {
                                            progressReceiver1.exceptionThrown(t);
                                        }
                                    }
                                });
                            } else {
                                // Region only exists in existing world. Copy it to the new world
                                final Map<DataType, File> regions = new HashMap<>();
                                regions.put(REGION, existingRegions.get(regionCoords));
                                if (additionalRegions.containsKey(regionCoords)) {
                                    regions.putAll(additionalRegions.get(regionCoords));
                                }
                                executor.execute(() -> {
                                    if (abort.get()) {
                                        return;
                                    }
                                    if (progressReceiver != null) {
                                        try {
                                            progressReceiver.checkForCancellation();
                                        } catch (ProgressReceiver.OperationCancelled e) {
                                            abort.set(true);
                                            return;
                                        }
                                    }
                                    try {
                                        copyRegionsUnchanged(fixups, exportedRegions, regionCoords, regions, dimensionDir, (parallelProgressManager != null) ? parallelProgressManager.createProgressReceiver() : null);
                                    } catch (Throwable t) {
                                        logger.error("{} while copying region {},{} (message: \"{}\")", t.getClass().getSimpleName(), regionCoords.x, regionCoords.y, t.getMessage(), t);
                                        abort.set(true);
                                        if (progressReceiver != null) {
                                            progressReceiver.exceptionThrown(t);
                                        }
                                    }
                                });
                            }
                        } else {
                            // Region only exists in new world. Create it as new
                            if (logger.isDebugEnabled()) {
                                logger.debug("Region " + regionCoords + " does not exist in existing map and will be created as new");
                            }
                            executor.execute(() -> {
                                if (abort.get()) {
                                    return;
                                }
                                final ProgressReceiver progressReceiver1 = (parallelProgressManager != null) ? parallelProgressManager.createProgressReceiver() : null;
                                if (progressReceiver1 != null) {
                                    try {
                                        progressReceiver1.checkForCancellation();
                                    } catch (ProgressReceiver.OperationCancelled e) {
                                        abort.set(true);
                                        return;
                                    }
                                }
                                try {
                                    final Map<Layer, LayerExporter> exporters = getExportersForRegion(combined, regionCoords);
                                    final WorldPainterChunkFactory chunkFactory = new WorldPainterChunkFactory(combined, exporters, platform, combined.getMaxHeight());

                                    final WorldRegion minecraftWorld = new WorldRegion(regionCoords.x, regionCoords.y, combined.getMinHeight(), combined.getMaxHeight(), platform);
                                    ExportResults exportResults = null;
                                    try {
                                        exportResults = exportRegion(minecraftWorld, combined, null, regionCoords, selectedTiles != null, exporters, null, chunkFactory, null, (progressReceiver1 != null) ? new SubProgressReceiver(progressReceiver1, 0.9f, 0.1f) : null);
                                        if (logger.isDebugEnabled()) {
                                            logger.debug("Generated region " + regionCoords.x + "," + regionCoords.y);
                                        }
                                    } finally {
                                        if ((exportResults != null) && exportResults.chunksGenerated) {
                                            minecraftWorld.save(worldDir, dim);
                                        }
                                    }
                                    synchronized (fixups) {
                                        if ((exportResults.fixups != null) && (! exportResults.fixups.isEmpty())) {
                                            fixups.put(new Point(regionCoords.x, regionCoords.y), exportResults.fixups);
                                        }
                                        exportedRegions.add(regionCoords);
                                    }
                                    try {
                                        performFixupsIfNecessary(worldDir, combined, allRegionCoords, fixups, exportedRegions, stats, progressReceiver1);
                                    } catch (InvalidMapException e) {
                                        throw createInvalidMapException(e.getMessage(), backupWorldDir);
                                    }
                                } catch (Throwable t) {
                                    logger.error("{} while exporting region {},{} (message: \"{}\")", t.getClass().getSimpleName(), regionCoords.x, regionCoords.y, t.getMessage(), t);
                                    abort.set(true);
                                    if (progressReceiver1 != null) {
                                        progressReceiver1.exceptionThrown(t);
                                    }
                                }
                            });
                        }
                    }
                    additionalRegions.forEach((coords, regions) -> {
                        if (! allRegionCoords.contains(coords)) {
                            // This is a region file from a directory other than "region" which does not have a
                            // corresponding region file in "region" in either the old or new maps, so it was not
                            // processed yet. Just copy it
                            executor.execute(() -> {
                                if (abort.get()) {
                                    return;
                                }
                                if (progressReceiver != null) {
                                    try {
                                        progressReceiver.checkForCancellation();
                                    } catch (ProgressReceiver.OperationCancelled e) {
                                        abort.set(true);
                                        return;
                                    }
                                }
                                try {
                                    copyRegionsUnchanged(fixups, exportedRegions, coords, regions, dimensionDir, (parallelProgressManager != null) ? parallelProgressManager.createProgressReceiver() : null);
                                } catch (Throwable t) {
                                    logger.error("{} while copying region {},{} (message: \"{}\")", t.getClass().getSimpleName(), coords.x, coords.y, t.getMessage(), t);
                                    abort.set(true);
                                    if (progressReceiver != null) {
                                        progressReceiver.exceptionThrown(t);
                                    }
                                }
                            });
                        }
                    });
                } finally {
                    executor.shutdown();
                    try {
                        executor.awaitTermination(1000, TimeUnit.DAYS);
                    } catch (InterruptedException e) {
                        throw new RuntimeException("Thread interrupted while waiting for all tasks to finish", e);
                    }
                }

                // It's possible for there to be fixups left, if thread A was performing fixups and thread B added new
                // ones and then quit, or if regions were copied from the existing map
                if (! abort.get()) {
                    synchronized (fixups) {
                        if (! fixups.isEmpty()) {
                            if (progressReceiver != null) {
                                progressReceiver.setMessage("doing remaining fixups for " + dimension.getName());
                                progressReceiver.reset();
                            }
                            try {
                                performFixups(worldDir, dimension, stats, (progressReceiver != null) ? new SubProgressReceiver(progressReceiver, 0.9f, 0.1f) : null, fixups);
                            } catch (InvalidMapException e) {
                                throw createInvalidMapException(e.getMessage(), backupWorldDir);
                            }
                        }
                    }
                }

                if (progressReceiver != null) {
                    progressReceiver.setProgress(1.0f);
                }
                return null;
            } finally {

                // Undo any changes we made (such as applying any combined layers)
                savedSettings.forEach(this::restoreDimensionAfterExport);
            }
        }, "dimension.anchor", dimension.getAnchor(), "dimension.minHeight", dimension.getMinHeight(), "dimension.maxHeight", dimension.getMaxHeight());
    }

    /**
     * Returns all tiles that should be exported or merged for the specified dimension, based on the tile selection and
     * the Read-Only layer (if any), grouped by region.
     *
     * @param dimension The dimension on which to base the tile selection.
     * @return A map of region coordinates to maps of tile coordinates to tiles.
     */
    private Map<Point, Map<Point, Tile>> getTilesByRegion(Dimension dimension) {
        final Set<Point> selectedTiles = worldExportSettings.getTilesToExport();
        final boolean tileSelection = selectedTiles != null;
        final Map<Point, Map<Point, Tile>> tilesByRegion = new HashMap<>();
        if (tileSelection) {
            // Sanity check
            assert worldExportSettings.getDimensionsToExport().size() == 1;
            assert worldExportSettings.getDimensionsToExport().contains(dimension.getAnchor().dim);
            for (Point tileCoords: selectedTiles) {
                Tile tile = dimension.getTile(tileCoords);
                boolean nonReadOnlyChunksFound = false;
                outerLoop:
                for (int chunkX = 0; chunkX < TILE_SIZE; chunkX += 16) {
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
            }
        } else {
            for (Tile tile: dimension.getTiles()) {
                boolean nonReadOnlyChunksFound = false;
                outerLoop:
                for (int chunkX = 0; chunkX < TILE_SIZE; chunkX += 16) {
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
            }
        }
        return tilesByRegion;
    }

    private void copyRegionsUnchanged(Map<Point, List<Fixup>> fixups, Set<Point> exportedRegions, Point coords, Map<DataType, File> regions, File dimensionDir, ProgressReceiver progressReceiver) throws IOException, ProgressReceiver.OperationCancelled {
        if (logger.isDebugEnabled()) {
            logger.debug("Region " + coords + " does not exist in new world and will be copied from existing map");
        }
        if (progressReceiver != null) {
            progressReceiver.setMessage("Copying region " + coords.x + "," + coords.y + " unchanged");
        }
        final int fileCount = regions.size();
        int fileNo = 0;
        for (Map.Entry<DataType, File> entry: regions.entrySet()) {
            DataType type = entry.getKey();
            File file = entry.getValue();
            FileUtils.copyFileToDir(file, new File(dimensionDir, type.name().toLowerCase()), (progressReceiver != null)
                    ? ((fileCount == 1) ? progressReceiver : new SubProgressReceiver(progressReceiver, "Copying region " + coords.x + "," + coords.y + " of type " + type + " unchanged", (float) fileNo / fileCount, 1.0f / fileCount))
                    : null);
            fileNo++;
        }
        synchronized (fixups) {
            exportedRegions.add(coords);
        }
        if (logger.isDebugEnabled()) {
            logger.debug("Copied region " + coords.x + "," + coords.y);
        }
    }

    private String mergeRegion(MinecraftWorld minecraftWorld, File oldRegionDir, Dimension dimension, Point regionCoords, Map<Point, Tile> tiles, boolean tileSelection, Map<Layer, LayerExporter> exporters, ChunkFactory chunkFactory, List<Fixup> fixups, ProgressReceiver progressReceiver) {
        return doWithMdcContext(() -> {
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

            // Remove layers which have been excluded for export
            allLayers.removeIf(layer -> (layer instanceof CustomLayer) && (! ((CustomLayer) layer).isExport()));

            // Remove layers which should be disabled due to the world export settings
            applyWorldExportSettings(allLayers);

            List<Layer> secondaryPassLayers = new ArrayList<>();
            for (Layer layer: allLayers) {
                final Class<? extends LayerExporter> exporterType = layer.getExporterType();
                if ((exporterType != null) && SecondPassLayerExporter.class.isAssignableFrom(exporterType)) {
                    secondaryPassLayers.add(layer);
                }
            }
            Collections.sort(secondaryPassLayers);

            // TODO add ceiling support
            // TODO add timing support
            // First pass. Create terrain and apply layers which don't need access to neighbouring chunks
            final String warnings;
            final ExportResults exportResults;
            if ((exportResults = firstPass(minecraftWorld, dimension, regionCoords, tiles, tileSelection, exporters, chunkFactory, (progressReceiver != null) ? new SubProgressReceiver(progressReceiver, 0.0f, 0.3f) : null)).chunksGenerated) {
                // Second pass. Apply layers which need information from or apply changes to neighbouring chunks
                List<Fixup> myFixups = secondPass(secondaryPassLayers, dimension, minecraftWorld, exporters, tiles.values(), regionCoords,
                        exportResults, (progressReceiver != null) ? new SubProgressReceiver(progressReceiver, 0.3f, 0.1f) : null);
                if ((myFixups != null) && (! myFixups.isEmpty())) {
                    synchronized (fixups) {
                        fixups.addAll(myFixups);
                    }
                }

                // Merge chunks
                warnings = thirdPass(minecraftWorld, oldRegionDir, dimension, regionCoords, (progressReceiver != null) ? new SubProgressReceiver(progressReceiver, 0.4f, 0.25f) : null);

                // If all we did was replace biomes then post-processing and lighting are not needed
                if (mergeBlocksAboveGround || mergeBlocksUnderground || clearTrees || clearVegetation || clearManMadeAboveGround || clearResources || fillCaves || clearManMadeBelowGround) {
                    // Post processing. Fix covered grass blocks, things like that
                    final BlockBasedExportSettings exportSettings = (BlockBasedExportSettings) ((dimension.getExportSettings() instanceof BlockBasedExportSettings)
                            ? dimension.getExportSettings()
                            : platformProvider.getDefaultExportSettings(platform));
                    PlatformManager.getInstance().getPostProcessor(platform).postProcess(minecraftWorld, new Rectangle(regionCoords.x << 9, regionCoords.y << 9, 512, 512), exportSettings, (progressReceiver != null) ? new SubProgressReceiver(progressReceiver, 0.65f, 0.1f) : null);

                    // Third pass. Calculate lighting
                    if (BlockPropertiesCalculator.isBlockPropertiesPassNeeded(platform, worldExportSettings, exportSettings)) {
                        blockPropertiesPass(minecraftWorld, regionCoords, exportSettings, (progressReceiver != null) ? new SubProgressReceiver(progressReceiver, 0.75f, 0.25f) : null);
                    }
                }
            } else {
                // First pass produced no chunks; copy all chunks from the existing region
                warnings = copyAllChunksInRegion(minecraftWorld, oldRegionDir, dimension, regionCoords, (progressReceiver != null) ? new SubProgressReceiver(progressReceiver, 0.3f, 0.7f) : null);
            }
            if (progressReceiver != null) {
                progressReceiver.setProgress(1.0f);
            }
            return warnings;
        }, "region.coords", regionCoords);
    }

    private String thirdPass(MinecraftWorld minecraftWorld, File oldRegionDir, Dimension dimension, Point regionCoords, ProgressReceiver progressReceiver) throws IOException, ProgressReceiver.OperationCancelled {
        if (progressReceiver != null) {
            progressReceiver.setMessage("Merging changes into existing chunks");
        }

        // TODO: we used to do one extra ring of chunks here. Not sure why, perhaps we'll rediscover it...
        //  Edit: was it to get accurate lighting around the edges? NOTE: if we change this back we also need to re-
        //  instate multiple region file (by coordinates) support!
        final Map<DataType, RegionFile> regionFiles = new HashMap<>();
        final Set<DataType> dataTypes = platformProvider.getDataTypes(platform);
        for (DataType dataType: dataTypes) {
            RegionFile regionFile = platformProvider.getRegionFile(platform, oldRegionDir, dataType, regionCoords, true);
            if (regionFile != null) {
                regionFiles.put(dataType, regionFile);
            }
        }
        if (! regionFiles.containsKey(REGION)) {
            throw new IllegalStateException("No region files of type REGION found for coordinates " + regionCoords + " in " + oldRegionDir.getParent());
        }
        final StringBuilder reportBuilder = new StringBuilder();
        try {
            final int lowestChunkX = regionCoords.x << 5;
            final int highestChunkX = (regionCoords.x << 5) + 31;
            final int lowestChunkY = regionCoords.y << 5;
            final int highestChunkY = (regionCoords.y << 5) + 31;
            final int minHeight = dimension.getMinHeight();
            final int maxHeight = dimension.getMaxHeight();
            int chunkNo = 0;
            for (int chunkX = lowestChunkX; chunkX <= highestChunkX; chunkX++) {
                for (int chunkY = lowestChunkY; chunkY <= highestChunkY; chunkY++) {
                    chunkNo++;
                    if (progressReceiver != null) {
                        progressReceiver.setProgress((float) chunkNo / 1156);
                    }
                    final Chunk newChunk;
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
                    final int chunkXInRegion = chunkX & 0x1f;
                    final int chunkYInRegion = chunkY & 0x1f;
                    Chunk existingChunk = null;
                    if (regionFiles.get(REGION).containsChunk(chunkXInRegion, chunkYInRegion)) {
                        final Map<DataType, Tag> tags = new HashMap<>();
                        regionFiles.forEach((dataType, regionFile) -> {
                            try {
                                final InputStream chunkData = regionFiles.get(dataType).getChunkDataInputStream(chunkXInRegion, chunkYInRegion);
                                if ((chunkData == null) && (dataType == REGION)) {
                                    // This should never happen, since we checked with isChunkPresent(), but in practice
                                    // it does. Perhaps corrupted data?
                                    reportBuilder.append("Missing chunk data in existing map for chunk " + chunkXInRegion + ", " + chunkYInRegion + " in " + regionFile + "; skipping chunk" + EOL);
                                    logger.warn("Missing chunk data in existing map for chunk " + chunkXInRegion + ", " + chunkYInRegion + " in " + regionFile + "; skipping chunk");
                                    return;
                                } else if (chunkData != null) {
                                    try (NBTInputStream in = new NBTInputStream(chunkData)) {
                                        tags.put(dataType, in.readTag());
                                    }
                                }
                            } catch (IOException e) {
                                reportBuilder.append("I/O error while reading chunk in existing map " + chunkXInRegion + ", " + chunkYInRegion + " from file " + regionFile + " (message: \"" + e.getMessage() + "\"); skipping chunk" + EOL);
                                logger.error("I/O error while reading chunk in existing map " + chunkXInRegion + ", " + chunkYInRegion + " from file " + regionFile + "; skipping chunk", e);
                            } catch (IllegalArgumentException e) {
                                reportBuilder.append("Illegal argument exception while reading chunk in existing map " + chunkXInRegion + ", " + chunkYInRegion + " from file " + regionFile + " (message: \"" + e.getMessage() + "\"); skipping chunk" + EOL);
                                logger.error("Illegal argument exception while reading chunk in existing map " + chunkXInRegion + ", " + chunkYInRegion + " from file " + regionFile + "; skipping chunk", e);
                            } catch (ClassCastException e) {
                                reportBuilder.append("Class cast exception while reading chunk in existing map " + chunkXInRegion + ", " + chunkYInRegion + " from file " + regionFile + " (message: \"" + e.getMessage() + "\"); skipping chunk" + EOL);
                                logger.error("Class cast exception while reading chunk in existing map " + chunkXInRegion + ", " + chunkYInRegion + " from file " + regionFile + "; skipping chunk", e);
                            } catch (RegionFile.InvalidRegionFileException e) {
                                reportBuilder.append("Invalid region file while reading chunk in existing map " + chunkXInRegion + ", " + chunkYInRegion + " from file " + regionFile + " (message: \"" + e.getMessage() + "\"); skipping chunk" + EOL);
                                logger.error("Invalid region file while reading chunk in existing map " + chunkXInRegion + ", " + chunkYInRegion + " from file " + regionFile + "; skipping chunk", e);
                            }
                        });
                        if (! tags.containsKey(REGION)) {
                            continue;
                        }
                        existingChunk = platformProvider.createChunk(platform, tags, minHeight, maxHeight);
                    }
                    if (existingChunk != null) {
                        // Sanity checks
                        final Set<Platform> chunkNativePlatforms = determineNativePlatforms(existingChunk);
                        if ((chunkNativePlatforms != null) && (! chunkNativePlatforms.contains(platform))) {
                            throw createInvalidMapException("At least one of the existing chunks to be merged is in a different format (" + chunkNativePlatforms.stream().map(p -> p.displayName).collect(joining(" or ")) + ")", oldRegionDir.getParentFile());
                        } else if (existingChunk.getMinHeight() > dimension.getMinHeight()) {
                            throw createInvalidMapException("At least one of the existing chunks to be merged has a higher minimum build height (" + existingChunk.getMinHeight() + ") than the dimension being merged (" + dimension.getMinHeight() + ")", oldRegionDir.getParentFile());
                        } else if (existingChunk.getMaxHeight() < dimension.getMaxHeight()) {
                            throw createInvalidMapException("At least one of the existing chunks to be merged has a lower maximum build height (" + existingChunk.getMaxHeight() + ") than the dimension being merged (" + dimension.getMaxHeight() + ")", oldRegionDir.getParentFile());
                        }
                        if (newChunk != null) {
                            // Chunk exists in existing and new world; merge it
                            // Do any necessary processing of the existing chunk (clearing trees, etc.) No need to check
                            // for read-only; if the chunk was read-only it wouldn't exist in the new map, and we
                            // wouldn't be here
                            processExistingChunk(existingChunk);
                            try {
                                mergeChunk(existingChunk, newChunk, dimension);
                                minecraftWorld.addChunk(existingChunk);
                            } catch (NullPointerException e) {
                                reportBuilder.append("Null pointer exception while merging chunks @ " + chunkXInRegion + ", " + chunkYInRegion + " in region " + regionCoords + "; skipping chunk" + EOL);
                                logger.error("Null pointer exception while merging chunks @ " + chunkXInRegion + ", " + chunkYInRegion + " in region " + regionCoords + "; skipping chunk", e);
                                continue;
                            } catch (ArrayIndexOutOfBoundsException e) {
                                reportBuilder.append("Array index out of bounds while merging chunks @ " + chunkXInRegion + ", " + chunkYInRegion + " in region " + regionCoords + " (message: \"" + e.getMessage() + "\"); skipping chunk" + EOL);
                                logger.error("Array index out of bounds while merging chunks @ " + chunkXInRegion + ", " + chunkYInRegion + " in region " + regionCoords + "; skipping chunk", e);
                                continue;
                            }
                        } else {
                            // Chunk exists in existing world, but not in new one, copy old to new
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

    private static InvalidMapException createInvalidMapException(String reason, File backupDir) {
        return new InvalidMapException(reason + "\n" +
                "Please use the Optimize function in Minecraft to convert the map fully to the current format.\n" +
                "\n" +
                "The partially processed map is now probably corrupted.\n" +
                "You should replace it from the backup at " + backupDir + ".");
    }

    private void processExistingChunk(final Chunk existingChunk) {
        if (! (clearTrees || fillCaves || clearResources || clearVegetation || clearManMadeAboveGround || clearManMadeBelowGround)) {
            return;
        }
        final int minHeight = existingChunk.getMinHeight();
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                boolean aboveGround = true;
                for (int y = existingChunk.getHighestNonAirBlock(x, z); y >= minHeight; y--) {
                    Material existingBlock = existingChunk.getMaterial(x, y, z);
                    if (aboveGround) {
                        if ((clearTrees && existingBlock.treeRelated)
                                || (clearVegetation && existingBlock.vegetation)
                                || (clearManMadeAboveGround && (! existingBlock.natural))) {
                            clearBlock(existingChunk, x, z, y, existingBlock);
                        } else if (existingBlock.terrain) {
                            aboveGround = false;
                        }
                    }
                    if (! aboveGround) {
                        // Separate if-statements so that if both are enabled,
                        // man made blocks are correctly removed and then filled
                        // in
                        if (clearManMadeBelowGround && (! existingBlock.natural)) {
                            final Material newMaterial = findMostPrevalentSolidSurroundingMaterial(existingChunk, x, z, y);
                            if (newMaterial.air) { // TODO how to handle minecraft:light blocks?
                                clearBlock(existingChunk, x, z, y, existingBlock);
                            } else {
                                existingChunk.setMaterial(x, y, z, newMaterial);
                                existingChunk.setSkyLightLevel(x, y, z, 0);
                                existingChunk.setBlockLightLevel(x, y, z, 0);
                            }
                            existingBlock = existingChunk.getMaterial(x, y, z);
                        }
                        if (fillCaves && existingBlock.veryInsubstantial) {
                            final Material newMaterial = findMostPrevalentSolidSurroundingMaterial(existingChunk, x, z, y);
                            if (newMaterial.air) { // TODO how to handle minecraft:light blocks?
                                existingChunk.setMaterial(x, y, z, STONE);
                            } else {
                                existingChunk.setMaterial(x, y, z, newMaterial);
                            }
                            existingChunk.setSkyLightLevel(x, y, z, 0);
                            existingChunk.setBlockLightLevel(x, y, z, 0);
                        } else if (clearResources && existingBlock.resource) {
                            if (existingBlock.isNamedOneOf(MC_NETHER_QUARTZ_ORE, MC_NETHER_GOLD_ORE, MC_ANCIENT_DEBRIS)) {
                                existingChunk.setMaterial(x, y, z, NETHERRACK);
                            } else if (DEEPSLATE_ORES.contains(existingBlock)) {
                                existingChunk.setMaterial(x, y, z, DEEPSLATE_Y);
                            } else {
                                existingChunk.setMaterial(x, y, z, STONE);
                            }
                        }
                    }
                }
            }
        }
    }

    private void clearBlock(final Chunk chunk, final int x, final int y, final int height, final Material existingMaterial) {
        final int minZ = chunk.getMinHeight(), maxZ = chunk.getMaxHeight() - 1;
        if (existingMaterial.watery || existingMaterial.is(WATERLOGGED)) {
            chunk.setMaterial(x, height, y, STATIONARY_WATER);
            // TODO skylight adjustment for under water
            // TODO also set to water if water to the side or above
        } else if (existingMaterial.isNamed(MC_MUDDY_MANGROVE_ROOTS)) {
            chunk.setMaterial(x, height, y, MUD);
        } else {
            chunk.setMaterial(x, height, y, AIR);
            // Note that these lighting calculations aren't strictly necessary since
            // the lighting will be fully recalculated later on, but it doesn't hurt
            // and it might improve performance and/or fill in gaps in the logic
            final int skyLightLevelAbove = (height < maxZ) ? chunk.getSkyLightLevel(x, height + 1, y) : 15;
            if (skyLightLevelAbove == 15) {
                // Propagate full daylight down
                chunk.setSkyLightLevel(x, height, y, 15);
            } else {
                int skyLightLevelBelow = (height > minZ) ? chunk.getSkyLightLevel(x, height - 1, y) : 0;
                chunk.setSkyLightLevel(x, height, y, Math.max(Math.max(skyLightLevelAbove, skyLightLevelBelow) - 1, 0));
            }
        }
        int blockLightLevelAbove = (height < maxZ) ? chunk.getSkyLightLevel(x, height + 1, y) : 0;
        int blockLightLevelBelow = (height > minZ) ? chunk.getBlockLightLevel(x, height - 1, y) : 0;
        chunk.setBlockLightLevel(x, height, y, Math.max(Math.max(blockLightLevelAbove, blockLightLevelBelow) - 1, 0));
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
                        int newCount = materialCounts.merge(material, 1, Integer::sum);
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
    
    private String copyAllChunksInRegion(MinecraftWorld minecraftWorld, File oldRegionDir, Dimension dimension, Point regionCoords, ProgressReceiver progressReceiver) throws IOException, ProgressReceiver.OperationCancelled {
        if (progressReceiver != null) {
            progressReceiver.setMessage("Copying chunks unchanged");
        }

        final Map<DataType, RegionFile> regionFiles = new HashMap<>();
        final Set<DataType> dataTypes = platformProvider.getDataTypes(platform);
        for (DataType dataType: dataTypes) {
            RegionFile regionFile = platformProvider.getRegionFile(platform, oldRegionDir, dataType, regionCoords, true);
            if (regionFile != null) {
                regionFiles.put(dataType, regionFile);
            }
        }
        if (! regionFiles.containsKey(REGION)) {
            throw new IllegalStateException("No region files of type REGION found for coordinates " + regionCoords + " in " + oldRegionDir.getParent());
        }
        final StringBuilder reportBuilder = new StringBuilder();
        try {
            final int lowestChunkX = regionCoords.x << 5;
            final int highestChunkX = (regionCoords.x << 5) + 31;
            final int lowestChunkY = regionCoords.y << 5;
            final int highestChunkY = (regionCoords.y << 5) + 31;
            final int minHeight = dimension.getMinHeight();
            final int maxHeight = dimension.getMaxHeight();
            int chunkNo = 0;
            for (int chunkX = lowestChunkX; chunkX <= highestChunkX; chunkX++) {
                for (int chunkY = lowestChunkY; chunkY <= highestChunkY; chunkY++) {
                    chunkNo++;
                    if (progressReceiver != null) {
                        progressReceiver.setProgress((float) chunkNo / 1024);
                    }
                    final int chunkXInRegion = chunkX & 0x1f;
                    final int chunkYInRegion = chunkY & 0x1f;
                    if (regionFiles.get(REGION).containsChunk(chunkXInRegion, chunkYInRegion)) {
                        final Map<DataType, Tag> tags = new HashMap<>();
                        regionFiles.forEach((dataType, regionFile) -> {
                            try {
                                final InputStream chunkData = regionFiles.get(dataType).getChunkDataInputStream(chunkXInRegion, chunkYInRegion);
                                if ((chunkData == null) && (dataType == REGION)) {
                                    // This should never happen, since we checked with isChunkPresent(), but in practice
                                    // it does. Perhaps corrupted data?
                                    reportBuilder.append("Missing chunk data in existing map for chunk " + chunkXInRegion + ", " + chunkYInRegion + " in " + regionFile + "; skipping chunk" + EOL);
                                    logger.warn("Missing chunk data in existing map for chunk " + chunkXInRegion + ", " + chunkYInRegion + " in " + regionFile + "; skipping chunk");
                                    return;
                                } else if (chunkData != null) {
                                    try (NBTInputStream in = new NBTInputStream(chunkData)) {
                                        tags.put(dataType, in.readTag());
                                    }
                                }
                            } catch (IOException e) {
                                reportBuilder.append("I/O error while reading chunk in existing map " + chunkXInRegion + ", " + chunkYInRegion + " from file " + regionFile + " (message: \"" + e.getMessage() + "\"); skipping chunk" + EOL);
                                logger.error("I/O error while reading chunk in existing map " + chunkXInRegion + ", " + chunkYInRegion + " from file " + regionFile + "; skipping chunk", e);
                            } catch (IllegalArgumentException e) {
                                reportBuilder.append("Illegal argument exception while reading chunk in existing map " + chunkXInRegion + ", " + chunkYInRegion + " from file " + regionFile + " (message: \"" + e.getMessage() + "\"); skipping chunk" + EOL);
                                logger.error("Illegal argument exception while reading chunk in existing map " + chunkXInRegion + ", " + chunkYInRegion + " from file " + regionFile + "; skipping chunk", e);
                            } catch (ClassCastException e) {
                                reportBuilder.append("Class cast exception while reading chunk in existing map " + chunkXInRegion + ", " + chunkYInRegion + " from file " + regionFile + " (message: \"" + e.getMessage() + "\"); skipping chunk" + EOL);
                                logger.error("Class cast exception while reading chunk in existing map " + chunkXInRegion + ", " + chunkYInRegion + " from file " + regionFile + "; skipping chunk", e);
                            } catch (RegionFile.InvalidRegionFileException e) {
                                reportBuilder.append("Invalid region file while reading chunk in existing map " + chunkXInRegion + ", " + chunkYInRegion + " from file " + regionFile + " (message: \"" + e.getMessage() + "\"); skipping chunk" + EOL);
                                logger.error("Invalid region file while reading chunk in existing map " + chunkXInRegion + ", " + chunkYInRegion + " from file " + regionFile + "; skipping chunk", e);
                            }
                        });
                        if (! tags.containsKey(REGION)) {
                            continue;
                        }
                        // TODO: support any platform
                        minecraftWorld.addChunk(platformProvider.createChunk(platform, tags, minHeight, maxHeight));
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

    /**
     * Merge the changes contained in a dimension and new chunk generated from it, into an existing chunk.
     *
     * @param existingChunk The existing chunk into which to merge the changes.
     * @param newChunk      The new chunk from which to take the changes to merge.
     * @param dimension     The dimension from which to take the changes to merge.
     */
    private void mergeChunk(Chunk existingChunk, Chunk newChunk, Dimension dimension) {
        if (logger.isDebugEnabled()) {
            logger.debug("Merging chunks at " + existingChunk.getxPos() + "," + existingChunk.getzPos());
        }
        final int minHeight = existingChunk.getMinHeight(), oldMaxY = existingChunk.getHighestNonAirBlock(), newMaxY = newChunk.getHighestNonAirBlock(), maxHeight = existingChunk.getMaxHeight();
        final int chunkX = existingChunk.getxPos() << 4, chunkZ = existingChunk.getzPos() << 4;
        final boolean copy2DBiomes = existingChunk.isBiomesSupported() && newChunk.isBiomesAvailable();
        final boolean copy3DBiomes = existingChunk.is3DBiomesSupported() && newChunk.is3DBiomesAvailable();
        final boolean copyNamedBiomes = existingChunk.isNamedBiomesSupported() && newChunk.isNamedBiomesAvailable();
        // Handle the case that a platform does not support 3D biomes, but the chunk format does, in which case we
        // should always merge the entire column of biomes if we merge any of it:
        final boolean mergeBiomesUnderground = this.mergeBiomesUnderground || ((! (platform.capabilities.contains(BIOMES_3D) || platform.capabilities.contains(NAMED_BIOMES))) && mergeBiomesAboveGround);
        final boolean copyBiomes = (mergeBiomesAboveGround || mergeBiomesUnderground) && (copy3DBiomes || copyNamedBiomes);
        if (populateSupported && dimension.getBitLayerValueAt(Populate.INSTANCE, chunkX, chunkZ)) {
            existingChunk.setTerrainPopulated(false);
        }
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                if (mergeBiomesAboveGround && copy2DBiomes) {
                    existingChunk.setBiome(x, z, newChunk.getBiome(x, z));
                }
                final boolean biomeCopyColumn = copyBiomes && ((x % 4) == 1) && ((z % 4) == 1);
                if (dimension.getBitLayerValueAt(org.pepsoft.worldpainter.layers.Void.INSTANCE, chunkX | x, chunkZ | z)) {
                    // Void. Just empty the entire column
                    // TODO: only empty from the terrain height on downwards? or find some other way of preserving overhanging trees, that kind of thing?
                    // TODO: this prevents you from placing objects in the void; fix that!
                    for (int y = existingChunk.getHighestNonAirBlock(x, z); y >= minHeight; y--) {
                        existingChunk.setMaterial(x, y, z, AIR);
                        existingChunk.setBlockLightLevel(x, y, z, 0);
                        existingChunk.setSkyLightLevel(x, y, z, 15);
                        if (biomeCopyColumn) {
                            if (copy3DBiomes) {
                                existingChunk.set3DBiome(x >> 2, y >> 2, z >>2, newChunk.get3DBiome(x >> 2, y >> 2, z >> 2));
                            } else {
                                existingChunk.setNamedBiome(x >> 2, y >> 2, z >> 2, newChunk.getNamedBiome(x >> 2, y >> 2, z >> 2));
                            }
                        }
                    }
                } else {
                    final int newHeight = dimension.getIntHeightAt(chunkX | x, chunkZ | z);
                    final boolean frost = dimension.getBitLayerValueAt(Frost.INSTANCE, chunkX | x, chunkZ | z);
                    int oldHeight = minHeight - 1;
                    for (int y = oldMaxY; y >= minHeight; y--) {
                        if (existingChunk.getMaterial(x, y, z).terrain) {
                            // Terrain found
                            oldHeight = y;
                            break;
                        }
                    }
                    final int dy = newHeight - oldHeight;
                    if (dy > 0) {
                        // Terrain has been raised; go from top to bottom to avoid stepping on ourselves
                        final int mergeLimit = Math.min(newHeight - surfaceMergeDepth, oldHeight);
                        if (mergeBlocksAboveGround) {
                            // Merge above ground portion from new chunk
                            for (int y = Math.min(Math.max(oldMaxY + dy, newMaxY), maxHeight - 1); y >= newHeight + 1; y--) {
                                mergeAboveGroundBlock(existingChunk, newChunk, x, y, z, dy, frost);
                            }
                            // Merge surface layer blocks
                            for (int y = newHeight; y >= mergeLimit + 1; y--) {
                                mergeSurfaceBlock(existingChunk, newChunk, x, y, z, dy, minHeight, y < oldHeight);
                            }
                        }
                        // Merge underground portion if requested
                        if (mergeBlocksUnderground) {
                            for (int y = mergeLimit; y >= minHeight; y--) {
                                mergeUndergroundBlock(existingChunk, newChunk, x, y, z);
                            }
                        }
                        existingChunk.setHeight(x, z, Math.min(Math.max(existingChunk.getHeight(x, z) + dy, newChunk.getHeight(x, z)), maxHeight - 1));

                        // Move existing entities above old surface up
                        final int blockX = chunkX + x, blockZ = chunkZ + z;
                        for (Entity entity: existingChunk.getEntities()) {
                            final double[] pos = entity.getPos();
                            if ((pos[0] >= blockX) && (pos[0] < blockX + 1) && (pos[2] >= blockZ) && (pos[2] < blockZ + 1)) {
                                if (pos[1] > oldHeight) {
                                    pos[1] = Math.min(pos[1] + dy, maxHeight - 1);
                                    entity.setPos(pos);
                                }
                            }
                        }
                    } else if (dy < 0) {
                        // Terrain has been lowered
                        // Merge underground portion if requested
                        final int mergeLimit = Math.max(newHeight - surfaceMergeDepth, minHeight - 1);
                        if (mergeBlocksUnderground) {
                            for (int y = minHeight; y <= mergeLimit; y++) {
                                mergeUndergroundBlock(existingChunk, newChunk, x, y, z);
                            }
                        }
                        if (mergeBlocksAboveGround) {
                            for (int y = mergeLimit + 1; y <= newHeight; y++) {
                                mergeSurfaceBlock(existingChunk, newChunk, x, y, z, dy, minHeight, y < newHeight);
                            }
                            // TODOMC118 reinstate this:
                            // If the new ground height block is insubstantial in the
                            // existing chunk, and there is nothing substantial on the
                            // block in the new or existing chunks, remove it, so as not
                            // to create a weird one block layer of blocks over newly
                            // opened up voids such as caves, chasms, abandoned mines,
                            // etc.
//                            final Material existingMaterial = existingChunk.getMaterial(x, newHeight, z);
//                            if ((existingMaterial == AIR) || existingMaterial.insubstantial) {
//                                Material existingMaterialAbove = (newHeight < maxY) ? existingChunk.getMaterial(x, newHeight + 1, z) : AIR;
//                                Material newMaterialAbove = (((newHeight - dy) >= -1) && ((newHeight - dy) < maxY)) ? newChunk.getMaterial(x, newHeight + 1 - dy, z) : AIR;
//                                if (((newMaterialAbove == AIR) || newMaterialAbove.insubstantial) && ((existingMaterialAbove == AIR) || existingMaterialAbove.insubstantial)) {
//                                    newChunk.setMaterial(x, newHeight, z, AIR);
//                                    newChunk.setSkyLightLevel(x, newHeight, z, 0);
//                                    newChunk.setBlockLightLevel(x, newHeight, z, 0);
//                                }
//                            }
                            // Copy above ground portion from existing chunk, lowered by
                            // the appropriate amount
                            final int maxY = Math.min(Math.max(oldMaxY, newMaxY), maxHeight - 1);
                            for (int y = newHeight + 1; y <= maxY; y++) {
                                mergeAboveGroundBlock(existingChunk, newChunk, x, y, z, dy, frost);
                            }
                        }
                        existingChunk.setHeight(x, z, Math.min(Math.max(existingChunk.getHeight(x, z) + dy, newChunk.getHeight(x, z)), maxHeight - 1));

                        // Move existing entities above new surface down
                        final int blockX = chunkX + x, blockZ = chunkZ + z;
                        for (Entity entity: existingChunk.getEntities()) {
                            final double[] pos = entity.getPos();
                            if ((pos[0] >= blockX) && (pos[0] < blockX + 1) && (pos[2] >= blockZ) && (pos[2] < blockZ + 1)) {
                                if (pos[1] > newHeight) {
                                    pos[1] = Math.min(Math.max(pos[1] + dy, newHeight), maxHeight - 1);
                                    entity.setPos(pos);
                                }
                            }
                        }
                    } else {
                        // Terrain height has not changed. Copy everything from the
                        // existing chunk, except the top layer of the terrain.
                        final int mergeLimit = Math.max(newHeight - surfaceMergeDepth, minHeight - 1);
                        if (mergeBlocksUnderground) {
                            for (int y = minHeight; y <= mergeLimit; y++) {
                                mergeUndergroundBlock(existingChunk, newChunk, x, y, z);
                            }
                        }
                        if (mergeBlocksAboveGround) {
                            for (int y = mergeLimit + 1; y <= newHeight; y++) {
                                mergeSurfaceBlock(existingChunk, newChunk, x, y, z, 0, minHeight, y < newHeight);
                            }
                            final int maxY = Math.min(Math.max(oldMaxY, newMaxY), maxHeight - 1);
                            for (int y = newHeight + 1; y <= maxY; y++) {
                                mergeAboveGroundBlock(existingChunk, newChunk, x, y, z, 0, frost);
                            }
                        }
                    }
                    if (biomeCopyColumn) {
                        // For high worlds, go to at least 64 blocks above the surface:
                        final int biomesMaxY = Math.min(Math.max(Math.max(oldMaxY, newMaxY) + 64, 319), maxHeight - 1) >> 2,
                                biomesNewHeight = (newHeight - surfaceMergeDepth) >> 2;
                        for (int y = minHeight >> 2; y <= biomesMaxY; y++) {
                            if ((y >= biomesNewHeight) ? mergeBiomesAboveGround : mergeBiomesUnderground) {
                                if (copy3DBiomes) {
                                    existingChunk.set3DBiome(x >> 2, y, z >> 2, newChunk.get3DBiome(x >> 2, y, z >> 2));
                                } else {
                                    existingChunk.setNamedBiome(x >> 2, y, z >> 2, newChunk.getNamedBiome(x >> 2, y, z >> 2));
                                }
                            }
                        }
                    }
                }
            }
        }
        // Any entities in the new chunk are already at the correct height; we can just copy them all:
        existingChunk.getEntities().addAll(newChunk.getEntities());
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
    private void mergeSurfaceBlock(final Chunk existingChunk, final Chunk newChunk, final int x, final int y, final int z, final int dy, final int minHeight, final boolean preserveCaves) {
        final Material existingMaterial = ((y - dy) >= minHeight) ? existingChunk.getMaterial(x, y - dy, z) : null;
        if ((! preserveCaves) || (existingMaterial == null) || ((! existingMaterial.veryInsubstantial) && existingMaterial.natural)) {
            Material newMaterial = newChunk.getMaterial(x, y, z);

            if ((existingMaterial != null)
                    && (((newMaterial == DIRT) && existingMaterial.isNamed(MC_FARMLAND)) // Farmland is imported as dirt, so make sure to leave it intact.
                    || ((newMaterial == DIRT) && (existingMaterial == ROOTED_DIRT)) // Rooted dirt is imported as dirt, so make sure to leave it intact.
                    || ((newMaterial == STONE) && ((existingMaterial == INFESTED_STONE) || STONE_ORES.contains(existingMaterial))) // Infested stone and ores are imported as stone, so make sure to leave them intact.
                    || (newMaterial.isNamed(MC_DEEPSLATE) && (existingMaterial.isNamed(MC_INFESTED_DEEPSLATE) || DEEPSLATE_ORES.contains(existingMaterial))) // Infested deepslate and deepslate ores are imported as deepslate, so make sure to leave them intact.
                    || ((newMaterial == ICE) && existingMaterial.isNamed(MC_FROSTED_ICE)) // Frosted ice is imported as water + Frost, so make sure to leave it intact TODO do this for other forms of ice?
                    || ((newMaterial == MUD) && existingMaterial.isNamed(MC_MUDDY_MANGROVE_ROOTS)) // Muddy mangrove roots are imported as mud, so make sure to leave it intact.
                    || ((newMaterial == SAND) && existingMaterial.isNamed(MC_SUSPICIOUS_SAND)) // Suspicious sand is imported as sand, so make sure to leave it intact.
                    || ((newMaterial == GRAVEL) && existingMaterial.isNamed(MC_SUSPICIOUS_GRAVEL)) // Suspicious gravel is imported as gravel, so make sure to leave it intact.
            )) {
                newMaterial = existingMaterial;
            }

            existingChunk.setMaterial(x, y, z, newMaterial);
            existingChunk.setSkyLightLevel(x, y, z, newChunk.getSkyLightLevel(x, y, z));
            existingChunk.setBlockLightLevel(x, y, z, newChunk.getBlockLightLevel(x, y, z));
            if (newMaterial.tileEntity) {
                moveEntityTileData(existingChunk, newChunk, x, y, z, 0);
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
        final Material newMaterial = newChunk.getMaterial(x, y, z);
        if (! UNDERGROUND_MERGE_MATRIX[newMaterial.category][existingChunk.getMaterial(x, y, z).category]) {
            existingChunk.setMaterial(x, y, z, newMaterial);
            existingChunk.setSkyLightLevel(x, y, z, newChunk.getSkyLightLevel(x, y, z));
            existingChunk.setBlockLightLevel(x, y, z, newChunk.getBlockLightLevel(x, y, z));
            if (newMaterial.tileEntity) {
                moveEntityTileData(existingChunk, newChunk, x, y, z, 0);
            }
        }
    }

    /**
     * Merge one above ground block. Supports a changed surface height by specifying a delta between the Y coordinate of
     * the block to merge in the existing and new chunks. This method assumes that {@code existingChunk} will end up in
     * the final map, so will merge the change into that chunk.
     * 
     * <p>Coordinates are in Minecraft coordinate system.
     *
     * <p><strong>Note</strong> that this method can copy blocks within existingChunk, so it has to be invoked in the
     * right vertical direction in regard to the delta, so that it does not step on itself.
     * 
     * @param existingChunk The chunk from the existing map.
     * @param newChunk The newly generated chunk.
     * @param x The X coordinate of the block to merge.
     * @param y The Y coordinate of the block to merge, in the new chunk.
     * @param z The Z coordinate of the block to merge.
     * @param dy The number of blocks the terrain has been raised.
     * @param frost Whether the {@link Frost} layer was applied at the specified x,z coordinates in the new map.
     */
    private void mergeAboveGroundBlock(final Chunk existingChunk, final Chunk newChunk, final int x, final int y, final int z, final int dy, final boolean frost) {
        // Three steps, to keep things simpler:
        // First, move the existing block to the new height if necessary
        Material existingMaterial;
        if (((y - dy) < existingChunk.getMinHeight()) || ((y - dy) >= existingChunk.getMaxHeight())) {
            existingMaterial = AIR;
            if (dy != 0) {
                existingChunk.setMaterial(x, y, z, existingMaterial);
                existingChunk.setSkyLightLevel(x, y, z, ((y - dy) < existingChunk.getMinHeight()) ? 0 : 15);
                existingChunk.setBlockLightLevel(x, y, z, 0);

                // Also copy the 3D biome, if any (but only once per 4x4 column)
                if (((x % 4) == 1) && ((z % 4) == 1) && ((dy < -2) || (dy > 1))) {
                    if (existingChunk.is3DBiomesAvailable()) {
                        existingChunk.set3DBiome(x >> 2, y >> 2, z >> 2, ((y - dy) < existingChunk.getMinHeight()) ? existingChunk.get3DBiome(x >> 2, existingChunk.getMinHeight() >> 2, z >> 2) : existingChunk.get3DBiome(x >> 2, (existingChunk.getMaxHeight() >> 2) - 1, z >> 2));
                    } else if (existingChunk.isNamedBiomesAvailable()) {
                        existingChunk.setNamedBiome(x >> 2, y >> 2, z >> 2, ((y - dy) < existingChunk.getMinHeight()) ? existingChunk.getNamedBiome(x >> 2, existingChunk.getMinHeight() >> 2, z >> 2) : existingChunk.getNamedBiome(x >> 2, (existingChunk.getMaxHeight() >> 2) - 1, z >> 2));
                    }
                }
            }
        } else {
            existingMaterial = existingChunk.getMaterial(x, y - dy, z);
            if (dy != 0) {
                existingChunk.setMaterial(x, y, z, existingMaterial);
                existingChunk.setSkyLightLevel(x, y, z, existingChunk.getSkyLightLevel(x, y - dy, z));
                existingChunk.setBlockLightLevel(x, y, z, existingChunk.getBlockLightLevel(x, y - dy, z));
                if (existingMaterial.tileEntity) {
                    moveEntityTileData(existingChunk, existingChunk, x, y, z, dy);
                }
                if (dy < 0) {
                    // Terrain is being lowered, make sure to replace the source block with air. When the terrain is being
                    // raised, that's not necessary because mergeChunk() will fill that part in with blocks from the new
                    // map
                    existingChunk.setMaterial(x, y - dy, z, AIR);
                    existingChunk.setSkyLightLevel(x, y - dy, z, ((y - dy + 1) < existingChunk.getMaxHeight()) ? existingChunk.getSkyLightLevel(x, y - dy + 1, z) : 15);
                    existingChunk.setBlockLightLevel(x, y - dy, z, 0);
                }

                // Also copy the 3D biome, if any (but only once per 4x4 column)
                if (((x % 4) == 1) && ((z % 4) == 1) && ((dy < -2) || (dy > 1))) {
                    final int dyBiome = (dy + 2) >> 2;
                    final int biomeMinHeight = existingChunk.getMinHeight() >> 2;
                    final int biomeMaxHeight = existingChunk.getMaxHeight() >> 2;
                    if (existingChunk.is3DBiomesAvailable()) {
                        existingChunk.set3DBiome(x >> 2, y >> 2, z >> 2, existingChunk.get3DBiome(x >> 2, clamp(biomeMinHeight, (y >> 2) - dyBiome, biomeMaxHeight - 1), z >> 2));
                    } else if (existingChunk.isNamedBiomesAvailable()) {
                        existingChunk.setNamedBiome(x >> 2, y >> 2, z >> 2, existingChunk.getNamedBiome(x >> 2, clamp(biomeMinHeight, (y >> 2) - dyBiome, biomeMaxHeight - 1), z >> 2));
                    }
                }
            }
        }

        // If the corresponding material is the same in the new chunk, we are done
        final Material newMaterial = newChunk.getMaterial(x, y, z);
        if (newMaterial == existingMaterial) {
            return;
        }

        // Second, replace the existing block with the new block if that should take precedence
        boolean existingMaterialIsWatery = existingMaterial.isNamed(MC_WATER) || existingMaterial.is(WATERLOGGED) || existingMaterial.watery;
        final boolean newMaterialIsWatery = newMaterial.isNamed(MC_WATER) || newMaterial.is(WATERLOGGED) || newMaterial.watery;
        if  (existingMaterial.isNamedOneOf(MC_WATER, MC_ICE, MC_LAVA) // replace *all* fluids (and ice) from the existing map with fluids (or lack thereof) from the new map

                // replace air with non-air from the new map
                // TODO how to handle minecraft:light blocks?
                || (existingMaterial.air)

                // replace *all* blocks with substantial blocks from the new map
                || (! newMaterial.veryInsubstantial)

                // replace insubstantial blocks with insubstantial from the new map
                || (existingMaterial.insubstantial && newMaterial.insubstantial)

                // the Frost layer has not been applied and the existing block is snow
                || ((! frost) && existingMaterial.isNamed(MC_SNOW))

                // the existing block is insubstantial and the new block would wash it away
                || (newMaterialIsWatery && existingMaterial.veryInsubstantial && (! existingMaterial.hasProperty(WATERLOGGED)) && (! existingMaterial.watery))

                // the existing block is insubstantial and the new block would burn it away
                || (newMaterial.isNamed(MC_LAVA) && existingMaterial.veryInsubstantial)

                // the existing block is an underwater block which would now be above the water
                || (existingMaterial.watery && (! newMaterialIsWatery))) {

            // Copy the new block over the existing block, modifying it if necessary
            if (existingMaterial.isNamed(MC_SNOW) && newMaterial.isNamed(MC_SNOW)) {
                // If both the existing and new blocks are snow, use the highest snow level of the two, to leave smooth snow in the existing map intact
                existingChunk.setMaterial(x, y, z, SNOW.withProperty(LAYERS, Math.max(existingMaterial.getProperty(LAYERS), newMaterial.getProperty(LAYERS))));
            } else {
                existingChunk.setMaterial(x, y, z, newMaterial);
                if (newMaterial.tileEntity) {
                    moveEntityTileData(existingChunk, newChunk, x, y, z, 0);
                }
            }
            existingChunk.setSkyLightLevel(x, y, z, newChunk.getSkyLightLevel(x, y, z));
            existingChunk.setBlockLightLevel(x, y, z, newChunk.getBlockLightLevel(x, y, z));
            existingMaterial = newMaterial;
            existingMaterialIsWatery = newMaterialIsWatery;
        }

        // Third, manage the waterlogged property
        if (existingMaterial.hasProperty(WATERLOGGED) && (newMaterialIsWatery != existingMaterialIsWatery)) {
            // The block has a waterlogged property; manage it correctly
            if (newMaterialIsWatery) {
                existingChunk.setMaterial(x, y, z, existingMaterial.withProperty(WATERLOGGED, true));
            } else {
                existingChunk.setMaterial(x, y, z, existingMaterial.withProperty(WATERLOGGED, false));
            }
        }
    }

    private final File worldDir;
    private final ThreadLocal<Map<Material, Integer>> materialCountsRef = ThreadLocal.withInitial(HashMap::new);
    private boolean replaceChunks, mergeBlocksUnderground, clearTrees, clearResources, fillCaves, clearVegetation,
            clearManMadeAboveGround, clearManMadeBelowGround, mergeBlocksAboveGround, mergeBiomesAboveGround,
            mergeBiomesUnderground;
    private String warnings;
    private int surfaceMergeDepth = 1;
    private volatile boolean aborted = true;
    
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(JavaWorldMerger.class);
    private static final String EOL = System.getProperty("line.separator");

    // true means keep existing block               Existing map: Air:   Fluid: Insub: Manmd: Resrc: Solid:
    private static final boolean[][] UNDERGROUND_MERGE_MATRIX = {{false, false, true , true , false, false},  // Air in new map
                                                                 {false, false, false, true , false, false},  // Fluids in new map
                                                                 {false, false, false, true , false, false},  // Insubstantial in new map
                                                                 {false, false, false, false, false, false},  // Man-made in new map
                                                                 {true,  true,  true,  true,  false, false},  // Resource in new map
                                                                 {true,  true,  true,  true,  true , false}}; // Natural solid in new map
}