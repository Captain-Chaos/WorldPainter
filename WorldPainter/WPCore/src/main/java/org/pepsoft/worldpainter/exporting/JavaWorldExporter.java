/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.worldpainter.exporting;

import org.pepsoft.minecraft.*;
import org.pepsoft.minecraft.SuperflatPreset.Structure;
import org.pepsoft.util.FileUtils;
import org.pepsoft.util.ProgressReceiver;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.*;
import org.pepsoft.worldpainter.history.HistoryEntry;
import org.pepsoft.worldpainter.platforms.JavaPlatformProvider;
import org.pepsoft.worldpainter.util.FileInUseException;
import org.pepsoft.worldpainter.vo.EventVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.*;
import java.util.*;

import static org.pepsoft.minecraft.Constants.*;
import static org.pepsoft.minecraft.SuperflatPreset.Structure.*;
import static org.pepsoft.worldpainter.Constants.*;
import static org.pepsoft.worldpainter.DefaultPlugin.*;
import static org.pepsoft.worldpainter.Dimension.Border.ENDLESS_BARRIER;
import static org.pepsoft.worldpainter.Dimension.Border.ENDLESS_WATER;
import static org.pepsoft.worldpainter.Platform.Capability.GENERATOR_PER_DIMENSION;
import static org.pepsoft.worldpainter.biomeschemes.Minecraft1_19Biomes.*;

/**
 *
 * @author pepijn
 */
public class JavaWorldExporter extends AbstractWorldExporter { // TODO can this be made a BlockBasedPlatformProviderWorldExporter?
    public JavaWorldExporter(World2 world) {
        super(world, world.getPlatform());
        this.platformProvider = (JavaPlatformProvider) super.platformProvider;
        if ((! (platform == JAVA_ANVIL))
                && (! (platform == JAVA_MCREGION))
                && (! (platform == JAVA_ANVIL_1_15))
                && (! (platform == JAVA_ANVIL_1_17))
                && (! (platform == JAVA_ANVIL_1_18))) {
            throw new IllegalArgumentException("Unsupported platform " + platform);
        }
    }

    protected JavaWorldExporter(World2 world, Platform platform) {
        super(world, platform);
        this.platformProvider = (JavaPlatformProvider) super.platformProvider;
    }

    @SuppressWarnings("ConstantConditions") // Clarity
    @Override
    public Map<Integer, ChunkFactory.Stats> export(File baseDir, String name, File backupDir, ProgressReceiver progressReceiver) throws IOException, ProgressReceiver.OperationCancelled {
        // Sanity checks
        final Set<Point> selectedTiles = world.getTilesToExport();
        final Set<Integer> selectedDimensions = world.getDimensionsToExport();
        if ((selectedTiles != null) && ((selectedDimensions == null) || (selectedDimensions.size() != 1))) {
            throw new IllegalArgumentException("If a tile selection is active then exactly one dimension must be selected");
        }

        // Backup existing level
        File worldDir = new File(baseDir, FileUtils.sanitiseName(name));
        logger.info("Exporting world " + world.getName() + " to map at " + worldDir + " in " + platform.displayName + " format");
        if (worldDir.isDirectory()) {
            if (backupDir != null) {
                logger.info("Directory already exists; backing up to " + backupDir);
                if (!worldDir.renameTo(backupDir)) {
                    throw new FileInUseException("Could not move " + worldDir + " to " + backupDir);
                }
            } else {
                throw new IllegalStateException("Directory already exists and no backup directory specified");
            }
        }
        
        // Record start of export
        long start = System.currentTimeMillis();
        
        // Export dimensions
        Dimension dim0 = world.getDimension(0);
        JavaLevel level = JavaLevel.create(platform, world.getMaxHeight());
        level.setSeed(dim0.getMinecraftSeed());
        level.setName(name);
        Point spawnPoint = world.getSpawnPoint();
        level.setSpawn(spawnPoint.x, Math.max(dim0.getIntHeightAt(spawnPoint), dim0.getWaterLevelAt(spawnPoint)) + 1,spawnPoint.y);
        if (world.getGameType() == GameType.HARDCORE) {
            level.setGameType(GAME_TYPE_SURVIVAL);
            level.setHardcore(true);
            level.setDifficulty(DIFFICULTY_HARD);
            level.setDifficultyLocked(true);
            level.setAllowCommands(false);
        } else {
            level.setGameType(world.getGameType().ordinal());
            level.setHardcore(false);
            level.setDifficulty(world.getDifficulty());
            level.setAllowCommands(world.isAllowCheats());
        }
        for (Dimension dimension: world.getDimensions()) {
            if (dimension.getDim() < 0) {
                // Ceiling dimension
                continue;
            } else if ((! platform.capabilities.contains(GENERATOR_PER_DIMENSION)) && (dimension.getDim() != DIM_NORMAL)) {
                // This platform only supports generator settings for the surface dimension, and this is not the surface dimension
                continue;
            }
            Dimension.Border dimensionBorder = dimension.getBorder();
            if ((dimensionBorder != null) && dimensionBorder.isEndless()) {
                final SuperflatPreset.Builder superflatPresetBuilder;
                final int biome;
                final Structure[] structures;
                switch (dimension.getDim()) {
                    case DIM_NETHER:
                        biome = BIOME_HELL;
                        structures = null; // TODO are there Nether structures we could put here?
                        break;
                    case DIM_END:
                        biome = BIOME_THE_END;
                        structures = null; // TODO are there End structures we could put here?
                        break;
                    default:
                        switch (dimensionBorder) {
                            case ENDLESS_WATER:
                                biome = BIOME_OCEAN;
                                break;
                            case ENDLESS_VOID:
                            case ENDLESS_BARRIER:
                                biome = ((platform == JAVA_ANVIL_1_15) || (platform == JAVA_ANVIL_1_17) || (platform == JAVA_ANVIL_1_18) /* TODO make dynamic */) ? BIOME_THE_VOID : BIOME_PLAINS;
                                break;
                            default:
                                biome = BIOME_PLAINS;
                                break;
                        }
                        structures = new Structure[] {MINESHAFT, BIOME_1, DUNGEON, DECORATION, OCEANMONUMENT}; // TODO expand with Minecraft 1.14+ structures?
                        break;
                }
                switch (dimensionBorder) {
                    case ENDLESS_LAVA:
                    case ENDLESS_WATER:
                        superflatPresetBuilder = SuperflatPreset.builder(biome, structures);
                        final boolean bottomless = dimension.isBottomless();
                        final int borderLevel = dimension.getBorderLevel() - platform.minZ + 1;
                        final int oceanDepth = Math.max(Math.min(borderLevel / 2, 20), 1);
                        final int deepSlateDepth = (dimension.getMinHeight() < 0)
                                ? Math.min(Math.max(borderLevel - oceanDepth - (bottomless ? 0 : 1) - 5, 0), bottomless ? 64 : 63)
                                : 0;
                        final int stoneDepth = Math.max(borderLevel - oceanDepth - deepSlateDepth - (bottomless ? 0 : 1) - 5, 0);
                        final int dirtDepth = Math.max(borderLevel - oceanDepth - deepSlateDepth - stoneDepth - (bottomless ? 0 : 1), 0);
                        if (! bottomless) {
                            superflatPresetBuilder.addLayer(MC_BEDROCK, 1);
                        }
                        if (deepSlateDepth > 0) {
                            superflatPresetBuilder.addLayer(MC_DEEPSLATE, deepSlateDepth);
                        }
                        if (stoneDepth > 0) {
                            superflatPresetBuilder.addLayer(MC_STONE, stoneDepth);
                        }
                        if (dirtDepth > 0) {
                            superflatPresetBuilder.addLayer(MC_DIRT, dirtDepth);
                        }
                        if (oceanDepth > 0) {
                            superflatPresetBuilder.addLayer((dimensionBorder == ENDLESS_WATER) ? MC_WATER : MC_LAVA, oceanDepth);
                        }
                        break;
                    case ENDLESS_VOID:
                        superflatPresetBuilder = SuperflatPreset.builder(biome);
                        superflatPresetBuilder.addLayer(MC_AIR, 1);
                        break;
                    case ENDLESS_BARRIER:
                        superflatPresetBuilder = SuperflatPreset.builder(biome);
                        superflatPresetBuilder.addLayer(MC_BARRIER, dimension.getMaxHeight() - platform.minZ - ((dimension.getRoofType() == Dimension.WallType.BEDROCK) ? 1 : 0));
                        if (dimension.getRoofType() == Dimension.WallType.BEDROCK) {
                            superflatPresetBuilder.addLayer(MC_BEDROCK, 1);
                        }
                        break;
                    default:
                        throw new InternalError();
                }
                if ((dimension.getRoofType() != null) && (dimensionBorder != ENDLESS_BARRIER)) {
                    int totalDepth = superflatPresetBuilder.getLayerDepth();
                    superflatPresetBuilder.addLayer(MC_AIR, dimension.getMaxHeight() - platform.minZ - totalDepth - 1);
                    superflatPresetBuilder.addLayer((dimension.getRoofType() == Dimension.WallType.BEDROCK) ? MC_BEDROCK : MC_BARRIER, 1);
                }
                level.setGenerator(dimension.getDim(), new SuperflatGenerator(superflatPresetBuilder.build()));
            } else {
                level.setGenerator(dimension.getDim(), dimension.getGenerator());
            }
        }
        level.setMapFeatures(world.isMapFeatures());
        if ((platform != JAVA_MCREGION)) {
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

        // Lock the level.dat file, to keep Minecraft out until we are done
        File levelDatFile = new File(worldDir, "level.dat");
        // We need to load the level.dat file later when creating chunk stores, so keep that working even though the
        // file is exclusively locked:
        JavaLevel.setCachedLevel(levelDatFile, level);
        try (RandomAccessFile lockedFile = new RandomAccessFile(levelDatFile, "rw")) {
            lockedFile.getChannel().lock();

            Map<Integer, ChunkFactory.Stats> stats = new HashMap<>();
            int selectedDimension;
            if (selectedTiles == null) {
                selectedDimension = -1;
                boolean first = true;
                for (Dimension dimension: world.getDimensions()) {
                    if ((dimension.getDim() < 0) || ((selectedDimensions != null) && (! selectedDimensions.contains(dimension.getDim())))) {
                        // This dimension will be exported as part of another dimension, or it has not been selected to
                        // be exported, so skip it
                        continue;
                    }
                    if (first) {
                        first = false;
                    } else if (progressReceiver != null) {
                        progressReceiver.reset();
                    }
                    stats.put(dimension.getDim(), exportDimension(worldDir, dimension, progressReceiver));
                }
            } else {
                selectedDimension = selectedDimensions.iterator().next();
                stats.put(selectedDimension, exportDimension(worldDir, world.getDimension(selectedDimension), progressReceiver));
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
                event.setAttribute(ATTRIBUTE_KEY_PLATFORM, platform.displayName);
                event.setAttribute(ATTRIBUTE_KEY_PLATFORM_ID, platform.id);
                event.setAttribute(ATTRIBUTE_KEY_MAP_FEATURES, world.isMapFeatures());
                event.setAttribute(ATTRIBUTE_KEY_GAME_TYPE_NAME, world.getGameType().name());
                event.setAttribute(ATTRIBUTE_KEY_ALLOW_CHEATS, world.isAllowCheats());
                event.setAttribute(ATTRIBUTE_KEY_GENERATOR, dim0.getGenerator().getType().name());
                Dimension dimension = world.getDimension(0);
                event.setAttribute(ATTRIBUTE_KEY_TILES, dimension.getTileCount());
                logLayers(dimension, event, "");
                dimension = world.getDimension(1);
                if (dimension != null) {
                    event.setAttribute(ATTRIBUTE_KEY_NETHER_TILES, dimension.getTileCount());
                    logLayers(dimension, event, "nether.");
                }
                dimension = world.getDimension(2);
                if (dimension != null) {
                    event.setAttribute(ATTRIBUTE_KEY_END_TILES, dimension.getTileCount());
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
        } finally {
            JavaLevel.setCachedLevel(null, null);
        }
    }

    protected ChunkFactory.Stats exportDimension(File worldDir, Dimension dimension, ProgressReceiver progressReceiver) throws ProgressReceiver.OperationCancelled {
        File dimensionDir;
        Dimension ceiling;
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
        for (DataType dataType: platformProvider.getDataTypes(platform)) {
            File regionDir = new File(dimensionDir, dataType.name().toLowerCase());
            if (! regionDir.exists()) {
                if (! regionDir.mkdirs()) {
                    throw new RuntimeException("Could not create directory " + regionDir);
                }
            }
        }

        ChunkFactory.Stats collectedStats = parallelExportRegions(dimension, worldDir, progressReceiver);

        // Calculate total size of dimension
        Set<Point> regions = new HashSet<>();
        if (world.getTilesToExport() != null) {
            for (Point tile: world.getTilesToExport()) {
                regions.add(new Point(tile.x >> 2, tile.y >> 2));
            }
        } else {
            for (Point tileCoords: dimension.getTileCoords()) {
                // Also add regions for any bedrock wall and/or border
                // tiles, if present
                int r = (((dimension.getBorder() != null) && (! dimension.getBorder().isEndless())) ? dimension.getBorderSize() : 0)
                        + (((dimension.getBorder() == null) || (! dimension.getBorder().isEndless())) && (dimension.getWallType() != null) ? 1 : 0);
                for (int dx = -r; dx <= r; dx++) {
                    for (int dy = -r; dy <= r; dy++) {
                        regions.add(new Point((tileCoords.x + dx) >> 2, (tileCoords.y + dy) >> 2));
                    }
                }
            }
            if (ceiling != null) {
                for (Point tileCoords: ceiling.getTileCoords()) {
                    regions.add(new Point(tileCoords.x >> 2, tileCoords.y >> 2));
                }
            }
        }
        for (Point region: regions) {
            File file = new File(dimensionDir, "region/r." + region.x + "." + region.y + ((platform == JAVA_ANVIL) ? ".mca" : ".mcr"));
            collectedStats.size += file.length();
        }

        if (progressReceiver != null) {
            progressReceiver.setProgress(1.0f);
        }

        return collectedStats;
    }

    protected final JavaPlatformProvider platformProvider;

    private static final Logger logger = LoggerFactory.getLogger(JavaWorldExporter.class);
}