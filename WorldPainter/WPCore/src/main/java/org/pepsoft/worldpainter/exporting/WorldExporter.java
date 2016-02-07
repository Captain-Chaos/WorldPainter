/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.worldpainter.exporting;

import org.jetbrains.annotations.NotNull;
import org.pepsoft.minecraft.*;
import org.pepsoft.util.*;
import org.pepsoft.util.ProgressReceiver.OperationCancelled;
import org.pepsoft.worldpainter.*;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.gardenofeden.GardenExporter;
import org.pepsoft.worldpainter.gardenofeden.Seed;
import org.pepsoft.worldpainter.history.HistoryEntry;
import org.pepsoft.worldpainter.layers.CombinedLayer;
import org.pepsoft.worldpainter.layers.GardenCategory;
import org.pepsoft.worldpainter.layers.Layer;
import org.pepsoft.worldpainter.util.FileInUseException;
import org.pepsoft.worldpainter.vo.AttributeKeyVO;
import org.pepsoft.worldpainter.vo.EventVO;

import java.awt.*;
import java.io.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import static org.pepsoft.minecraft.Block.BLOCKS;
import static org.pepsoft.minecraft.Constants.*;
import static org.pepsoft.worldpainter.Constants.*;
import org.pepsoft.worldpainter.layers.CustomLayer;

/**
 *
 * @author pepijn
 */
public class WorldExporter {
    public WorldExporter(World2 world) {
        if (world == null) {
            throw new NullPointerException();
        }
        this.world = world;
        this.selectedTiles = world.getTilesToExport();
        this.selectedDimensions = world.getDimensionsToExport();
        if ((selectedTiles != null) && (selectedDimensions.size() != 1)) {
            throw new IllegalArgumentException("When a tile selection is present exactly one dimension must be selected");
        }
    }

    public World2 getWorld() {
        return world;
    }
    
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

    public Map<Integer, ChunkFactory.Stats> export(File baseDir, String name, File backupDir, ProgressReceiver progressReceiver) throws IOException, ProgressReceiver.OperationCancelled {
        // Sanity checks
        if ((world.getVersion() != SUPPORTED_VERSION_1) && (world.getVersion() != SUPPORTED_VERSION_2)) {
            throw new IllegalArgumentException("Not a supported version: 0x" + Integer.toHexString(world.getVersion()));
        }
        if ((selectedTiles == null) && (selectedDimensions != null)) {
            throw new IllegalArgumentException("Exporting a subset of dimensions not supported");
        }
        
        // Backup existing level
        File worldDir = new File(baseDir, FileUtils.sanitiseName(name));
        logger.info("Exporting world " + world.getName() + " to map at " + worldDir);
        if (worldDir.isDirectory()) {
            logger.info("Directory already exists; backing up to " + backupDir);
            if (! worldDir.renameTo(backupDir)) {
                throw new FileInUseException("Could not move " + worldDir + " to " + backupDir);
            }
        }
        
        // Record start of export
        long start = System.currentTimeMillis();
        
        // Export dimensions
        Dimension dim0 = world.getDimension(0);
        Level level = new Level(world.getMaxHeight(), world.getVersion());
        level.setSeed(dim0.getMinecraftSeed());
        level.setName(name);
        Point spawnPoint = world.getSpawnPoint();
        level.setSpawnX(spawnPoint.x);
        level.setSpawnY(Math.max(dim0.getIntHeightAt(spawnPoint), dim0.getWaterLevelAt(spawnPoint)));
        level.setSpawnZ(spawnPoint.y);
        level.setMapFeatures(world.isMapFeatures());
        if (world.getGameType() <= GAME_TYPE_ADVENTURE) {
            level.setGameType(world.getGameType());
            level.setHardcore(false);
            level.setDifficulty(world.getDifficulty());
            level.setAllowCommands(world.isAllowCheats());
        } else if (world.getGameType() == World2.GAME_TYPE_HARDCORE) {
            level.setGameType(GAME_TYPE_SURVIVAL);
            level.setHardcore(true);
            level.setDifficulty(DIFFICULTY_HARD);
            level.setDifficultyLocked(true);
            level.setAllowCommands(false);
        } else {
            throw new InternalError("Don't know how to encode game type " + world.getGameType());
        }
        level.setGenerator(world.getGenerator());
        if (world.getVersion() == SUPPORTED_VERSION_2) {
            if ((world.getGenerator() == Generator.FLAT) && (world.getGeneratorOptions() != null)) {
                level.setGeneratorOptions(world.getGeneratorOptions());
            }
            World2.BorderSettings borderSettings = world.getBorderSettings();
            level.setBorderCenterX(borderSettings.getCentreX());
            level.setBorderCenterZ(borderSettings.getCentreY());
            level.setBorderSize(borderSettings.getSize());
            level.setBorderSafeZone(borderSettings.getSafeZone());
            level.setBorderWarningBlocks(borderSettings.getWarningBlocks());
            level.setBorderWarningTime(borderSettings.getWarningTime());
            level.setBorderSizeLerpTarget(borderSettings.getSizeLerpTarget());
            level.setBorderSizeLerpTime(borderSettings.getSizeLerpTime());
            level.setBorderDamagePerBlock(borderSettings.getDamagePerBlock());
        }
        // Save the level.dat file. This will also create a session.lock file, hopefully kicking out any Minecraft
        // instances which may have the map open:
        level.save(worldDir);
        Map<Integer, ChunkFactory.Stats> stats = new HashMap<>();
        int selectedDimension;
        if (selectedTiles == null) {
            selectedDimension = -1;
            boolean first = true;
            for (Dimension dimension: world.getDimensions()) {
                if (dimension.getDim() < 0) {
                    // This dimension will be exported as part of another
                    // dimension, so skip it
                    continue;
                }
                if (first) {
                    first = false;
                } else {
                    progressReceiver.reset();
                }
                stats.put(dimension.getDim(), exportDimension(worldDir, dimension, world.getVersion(), progressReceiver));
            }
        } else {
            selectedDimension = selectedDimensions.iterator().next();
            stats.put(selectedDimension, exportDimension(worldDir, world.getDimension(selectedDimension), world.getVersion(), progressReceiver));
        }
        
        // Update the session.lock file, hopefully kicking out any Minecraft instances which may have tried to open the
        // map in the mean time:
        File sessionLockFile = new File(worldDir, "session.lock");
        try (DataOutputStream sessionOut = new DataOutputStream(new FileOutputStream(sessionLockFile))) {
            sessionOut.writeLong(System.currentTimeMillis());
        }

        // Record the export in the world history
        if (selectedTiles == null) {
            world.addHistoryEntry(HistoryEntry.WORLD_EXPORTED_FULL, name, worldDir);
        } else {
            world.addHistoryEntry(HistoryEntry.WORLD_EXPORTED_PARTIAL, name, worldDir, world.getDimension(selectedDimension).getName());
        }

        // Log an event
        Configuration config = Configuration.getInstance();
        if (config != null) {
            EventVO event = new EventVO(EVENT_KEY_ACTION_EXPORT_WORLD).duration(System.currentTimeMillis() - start);
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
            Dimension dimension = world.getDimension(0);
            event.setAttribute(ATTRIBUTE_KEY_TILES, dimension.getTiles().size());
            logLayers(dimension, event, "");
            dimension = world.getDimension(1);
            if (dimension != null) {
                event.setAttribute(ATTRIBUTE_KEY_NETHER_TILES, dimension.getTiles().size());
                logLayers(dimension, event, "nether.");
            }
            dimension = world.getDimension(2);
            if (dimension != null) {
                event.setAttribute(ATTRIBUTE_KEY_END_TILES, dimension.getTiles().size());
                logLayers(dimension, event, "end.");
            }
            if (selectedDimension != -1) {
                event.setAttribute(ATTRIBUTE_KEY_EXPORTED_DIMENSION, selectedDimension);
                event.setAttribute(ATTRIBUTE_KEY_EXPORTED_DIMENSION_TILES, selectedTiles.size());
            }
            if (world.getImportedFrom() != null) {
                event.setAttribute(ATTRIBUTE_KEY_IMPORTED_WORLD, true);
            }
            config.logEvent(event);
        }
        
        return stats;
    }
    
    protected ExportResults firstPass(MinecraftWorld minecraftWorld, Dimension dimension, Point regionCoords, Map<Point, Tile> tiles, boolean tileSelection, Map<Layer, LayerExporter<Layer>> exporters, ChunkFactory chunkFactory, boolean ceiling, ProgressReceiver progressReceiver) throws ProgressReceiver.OperationCancelled, IOException {
        if (logger.isDebugEnabled()) {
            logger.debug("Start of first pass for region {},{}", regionCoords.x, regionCoords.y);
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
                            existingChunk = (world.getVersion() == SUPPORTED_VERSION_1) ? new ChunkImpl(chunkX, chunkY, world.getMaxHeight()) : new ChunkImpl2(chunkX, chunkY, world.getMaxHeight());
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
                    int destinationBlock = destination.getBlockType(x, y, z);
                    if (! BLOCKS[destinationBlock].solid) {
                        // Insubstantial blocks in the destination are only
                        // replaced by solid ones; air is replaced by anything
                        // that's not air
                        int sourceBlock = source.getBlockType(x, y, z);
                        if ((destinationBlock == BLK_AIR) ? (sourceBlock != BLK_AIR) : BLOCKS[sourceBlock].solid) {
                            destination.setMaterial(x, y, z, source.getMaterial(x, y, z));
                            destination.setBlockLightLevel(x, y, z, source.getBlockLightLevel(x, y, z));
                            destination.setSkyLightLevel(x, y, z, source.getSkyLightLevel(x, y, z));
                        }
                    }
                }
            }
        }
    }
    
    protected List<Fixup> secondPass(List<Layer> secondaryPassLayers, Dimension dimension, MinecraftWorld minecraftWorld, Map<Layer, LayerExporter<Layer>> exporters, Collection<Tile> tiles, Point regionCoords, ProgressReceiver progressReceiver) throws ProgressReceiver.OperationCancelled {
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
            SecondPassLayerExporter<Layer> exporter = (SecondPassLayerExporter<Layer>) exporters.get(layer);
            if (logger.isDebugEnabled()) {
                logger.debug("Exporting layer {} for region {},{}", layer, regionCoords.x, regionCoords.y);
            }
            List<Fixup> layerFixups = exporter.render(dimension, area, exportedArea, minecraftWorld);
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
            if (chunk != null) {
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

    protected void lightingPass(MinecraftWorld minecraftWorld, Point regionCoords, ProgressReceiver progressReceiver) throws ProgressReceiver.OperationCancelled {
        LightingCalculator lightingVolume = new LightingCalculator(minecraftWorld);
        
        // Calculate primary light
        int lightingLowMark = Integer.MAX_VALUE, lightingHighMark = Integer.MIN_VALUE;
        int lowestChunkX = (regionCoords.x << 5) - 1;
        int highestChunkX = (regionCoords.x << 5) + 32;
        int lowestChunkY = (regionCoords.y << 5) - 1;
        int highestChunkY = (regionCoords.y << 5) + 32;
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
        }

        if (lightingLowMark != Integer.MAX_VALUE) {
            if (progressReceiver != null) {
                progressReceiver.setProgress(0.2f);
            }

            // Calculate secondary light
            lightingVolume.setDirtyArea(new Box((regionCoords.x << 9) - 16, ((regionCoords.x + 1) << 9) + 15, lightingLowMark, lightingHighMark, (regionCoords.y << 9) - 16, ((regionCoords.y + 1) << 9) + 15));
            while (lightingVolume.calculateSecondaryLight());
        }
        
        if (progressReceiver != null) {
            progressReceiver.setProgress(1.0f);
        }
    }
    
    protected final ExportResults exportRegion(MinecraftWorld minecraftWorld, Dimension dimension, Dimension ceiling, Point regionCoords, boolean tileSelection, Map<Layer, LayerExporter<Layer>> exporters, Map<Layer, LayerExporter<Layer>> ceilingExporters, ChunkFactory chunkFactory, ChunkFactory ceilingChunkFactory, ProgressReceiver progressReceiver) throws ProgressReceiver.OperationCancelled, IOException {
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
        for (Iterator<Layer> i = allLayers.iterator(); i.hasNext(); ) {
            Layer layer = i.next();
            if ((layer instanceof CustomLayer) && (! ((CustomLayer) layer).isExport())) {
                i.remove();
            }
        }
        
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
            for (Iterator<Layer> i = allCeilingLayers.iterator(); i.hasNext(); ) {
                Layer layer = i.next();
                if ((layer instanceof CustomLayer) && (! ((CustomLayer) layer).isExport())) {
                    i.remove();
                }
            }

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
            PostProcessor.postProcess(minecraftWorld, new Rectangle(regionCoords.x << 9, regionCoords.y << 9, 512, 512), (progressReceiver != null) ? new SubProgressReceiver(progressReceiver, 0.55f, 0.1f) : null);

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
    
    protected final void logLayers(Dimension dimension, EventVO event, String prefix) {
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
    
    private ChunkFactory.ChunkCreationResult createChunk(Dimension dimension, ChunkFactory chunkFactory, Map<Point, Tile> tiles, int chunkX, int chunkY, boolean tileSelection, Map<Layer, LayerExporter<Layer>> exporters, boolean ceiling) {
        final int tileX = chunkX >> 3;
        final int tileY = chunkY >> 3;
        final Point tileCoords = new Point(tileX, tileY);
        final boolean border = (dimension.getBorder() != null) && (dimension.getBorderSize() > 0);
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
            } else if (! ceiling) {
                // Might be a border or bedrock wall chunk (but not if this is a
                // ceiling dimension
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
                // Not a world tile, and we're a ceiling dimension so we don't
                // export borders and bedrock walls
                return null;
            }
        }
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

    private ChunkFactory.Stats exportDimension(final File worldDir, final Dimension dimension, final int version, ProgressReceiver progressReceiver) throws ProgressReceiver.OperationCancelled, IOException {
        if (progressReceiver != null) {
            progressReceiver.setMessage("exporting " + dimension.getName() + " dimension");
        }
        
        long start = System.currentTimeMillis();

        final File dimensionDir;
        final Dimension ceiling;
        switch (dimension.getDim()) {
            case DIM_NORMAL:
                dimensionDir = worldDir;
                ceiling = dimension.getWorld().getDimension(DIM_NORMAL_CEILING);
                break;
            case DIM_NETHER:
                dimensionDir = new File(worldDir, "DIM-1");
                ceiling = dimension.getWorld().getDimension(DIM_NETHER_CEILING);
                break;
            case DIM_END:
                dimensionDir = new File(worldDir, "DIM1");
                ceiling = dimension.getWorld().getDimension(DIM_END_CEILING);
                break;
            default:
                throw new IllegalArgumentException("Dimension " + dimension.getDim() + " not supported");
        }
        File regionDir = new File(dimensionDir, "region");
        if (! regionDir.exists()) {
            if (! regionDir.mkdirs()) {
                throw new RuntimeException("Could not create directory " + regionDir);
            }
        }

        final ChunkFactory.Stats collectedStats = new ChunkFactory.Stats();
        boolean wasDirty = dimension.isDirty(), ceilingWasDirty = (ceiling != null) && ceiling.isDirty();
        dimension.rememberChanges();
        if (ceiling != null) {
            ceiling.rememberChanges();
        }
        try {

            final Map<Layer, LayerExporter<Layer>> exporters = setupDimensionForExport(dimension);
            final Map<Layer, LayerExporter<Layer>> ceilingExporters = (ceiling != null) ? setupDimensionForExport(ceiling) : null;

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
                    int r = ((dimension.getBorder() != null) ? dimension.getBorderSize() : 0) + (dimension.isBedrockWall() ? 1 : 0);
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
            List<Point> sortedRegions = new ArrayList<>(regions.size());
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
            
            final WorldPainterChunkFactory chunkFactory = new WorldPainterChunkFactory(dimension, exporters, world.getVersion(), world.getMaxHeight());
            final WorldPainterChunkFactory ceilingChunkFactory = (ceiling != null) ? new WorldPainterChunkFactory(ceiling, ceilingExporters, world.getVersion(), world.getMaxHeight()) : null;

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
            ExecutorService executor = Executors.newFixedThreadPool(threads);
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
                            WorldRegion minecraftWorld = new WorldRegion(regionCoords.x, regionCoords.y, dimension.getMaxHeight(), version);
                            ExportResults exportResults = null;
                            try {
                                exportResults = exportRegion(minecraftWorld, dimension, ceiling, regionCoords, tileSelection, exporters, ceilingExporters, chunkFactory, ceilingChunkFactory, (progressReceiver1 != null) ? new SubProgressReceiver(progressReceiver1, 0.0f, 0.9f) : null);
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
                                    Map<Point, List<Fixup>> myFixups = new HashMap<>();
                                    synchronized (fixups) {
                                        for (Iterator<Map.Entry<Point, List<Fixup>>> i = fixups.entrySet().iterator(); i.hasNext(); ) {
                                            Map.Entry<Point, List<Fixup>> entry = i.next();
                                            Point fixupRegionCoords = entry.getKey();
                                            if (isReadyForFixups(regions, exportedRegions, fixupRegionCoords)) {
                                                myFixups.put(fixupRegionCoords, entry.getValue());
                                                i.remove();
                                            }
                                        }
                                    }
                                    if (! myFixups.isEmpty()) {
                                        performFixups(worldDir, dimension, version, (progressReceiver1 != null) ? new SubProgressReceiver(progressReceiver1, 0.9f, 0.1f) : null, myFixups);
                                    }
                                } finally {
                                    performingFixups.release();
                                }
                            }
                        } catch (Throwable t) {
                            if (progressReceiver1 != null) {
                                progressReceiver1.exceptionThrown(t);
                            } else {
                                logger.error("Exception while exporting region", t);
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
            
            // It's possible for there to be fixups left, if thread A was
            // performing fixups and thread B added new ones and then quit
            synchronized (fixups) {
                if (! fixups.isEmpty()) {
                    if (progressReceiver != null) {
                        progressReceiver.setMessage("doing remaining fixups for " + dimension.getName());
                        progressReceiver.reset();
                    }
                    performFixups(worldDir, dimension, version, progressReceiver, fixups);
                }
            }
            
            // Calculate total size of dimension
            for (Point region: regions) {
                File file = new File(dimensionDir, "region/r." + region.x + "." + region.y + ((version == SUPPORTED_VERSION_2) ? ".mca" : ".mcr"));
                collectedStats.size += file.length();
            }
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
            
            // If the dimension wasn't dirty make sure it still isn't
            dimension.setDirty(wasDirty);

            if (ceiling != null) {
                // Undo any changes we made (such as applying any combined layers)
                if (ceiling.undoChanges()) {
                    // TODO: some kind of cleverer undo mechanism (undo history
                    // cloning?) so we don't mess up the user's redo history
                    ceiling.clearRedo();
                    ceiling.armSavePoint();
                }

                // If the dimension wasn't dirty make sure it still isn't
                ceiling.setDirty(ceilingWasDirty);
            }
        }

        return collectedStats;
    }

    @NotNull
    private Map<Layer, LayerExporter<Layer>> setupDimensionForExport(Dimension dimension) {
        // Gather all layers used on the map
        final Map<Layer, LayerExporter<Layer>> exporters = new HashMap<>();
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
        for (Iterator<Layer> i = allLayers.iterator(); i.hasNext(); ) {
            Layer layer = i.next();
            if ((layer instanceof CustomLayer) && (! ((CustomLayer) layer).isExport())) {
                i.remove();
            }
        }
        
        // Load all layer settings into the exporters
        for (Layer layer: allLayers) {
            @SuppressWarnings("unchecked")
            LayerExporter<Layer> exporter = (LayerExporter<Layer>) layer.getExporter();
            if (exporter != null) {
                exporter.setSettings(dimension.getLayerSettings(layer));
                exporters.put(layer, exporter);
            }
        }
        return exporters;
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

    protected void performFixups(final File worldDir, final Dimension dimension, final int version, final ProgressReceiver progressReceiver, final Map<Point, List<Fixup>> fixups) throws OperationCancelled {
        long start = System.currentTimeMillis();
        // Make sure to honour the read-only layer:
        MinecraftWorldImpl minecraftWorld = new MinecraftWorldImpl(worldDir, dimension, version, false, true, 512);
        int count = 0, total = fixups.size();
        for (Map.Entry<Point, List<Fixup>> entry: fixups.entrySet()) {
            List<Fixup> regionFixups = entry.getValue();
            if (logger.isDebugEnabled()) {
                logger.debug("Performing " + regionFixups.size() + " fixups for region " + entry.getKey().x + "," + entry.getKey().y);
            }
            for (Fixup fixup: regionFixups) {
                fixup.fixup(minecraftWorld, dimension);
            }
            if (logger.isDebugEnabled()) {
                logger.debug("Flushing region files (chunks in cache: " + minecraftWorld.getCacheSize() + ")");
            }
            minecraftWorld.flush(); // Might affect performance of other threads also performing fixups, but should not cause errors
            if (progressReceiver != null) {
                progressReceiver.setProgress((float) ++count / total);
            }
        }
        if (logger.isTraceEnabled()) {
            logger.trace("Fixups for " + fixups.size() + " regions took " + (System.currentTimeMillis() - start) + " ms");
        }
    }

    private Chest createGoodiesChest() {
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
    
    protected final World2 world;
    protected final Set<Integer> selectedDimensions;
    protected final Set<Point> selectedTiles;
    protected final Semaphore performingFixups = new Semaphore(1);

    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMddHHmmss");
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(WorldExporter.class);
    private static final Object TIMING_FILE_LOCK = new Object();
    
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