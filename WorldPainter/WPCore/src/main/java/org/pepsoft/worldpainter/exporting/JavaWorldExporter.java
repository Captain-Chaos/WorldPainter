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
import org.pepsoft.worldpainter.Dimension.Anchor;
import org.pepsoft.worldpainter.history.HistoryEntry;
import org.pepsoft.worldpainter.platforms.JavaPlatformProvider;
import org.pepsoft.worldpainter.util.FileInUseException;
import org.pepsoft.worldpainter.vo.EventVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.*;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.pepsoft.minecraft.Constants.*;
import static org.pepsoft.minecraft.SuperflatPreset.Structure.*;
import static org.pepsoft.util.mdc.MDCUtils.doWithMdcContext;
import static org.pepsoft.worldpainter.Constants.*;
import static org.pepsoft.worldpainter.DefaultPlugin.DEFAULT_JAVA_PLATFORMS;
import static org.pepsoft.worldpainter.DefaultPlugin.JAVA_MCREGION;
import static org.pepsoft.worldpainter.Dimension.Anchor.*;
import static org.pepsoft.worldpainter.Dimension.Border.ENDLESS_BARRIER;
import static org.pepsoft.worldpainter.Dimension.Border.ENDLESS_WATER;
import static org.pepsoft.worldpainter.Dimension.Role.DETAIL;
import static org.pepsoft.worldpainter.Platform.Capability.GENERATOR_PER_DIMENSION;
import static org.pepsoft.worldpainter.biomeschemes.Minecraft1_21Biomes.*;
import static org.pepsoft.worldpainter.util.BiomeUtils.getBiomeScheme;

/**
 *
 * @author pepijn
 */
public class JavaWorldExporter extends AbstractWorldExporter { // TODO can this be made a BlockBasedPlatformProviderWorldExporter?
    public JavaWorldExporter(World2 world, WorldExportSettings exportSettings) {
        super(world, exportSettings, world.getPlatform());
        this.platformProvider = (JavaPlatformProvider) super.platformProvider;
        if (! DEFAULT_JAVA_PLATFORMS.contains(world.getPlatform())) {
            throw new IllegalArgumentException("Unsupported platform " + platform);
        }
    }

    protected JavaWorldExporter(World2 world, WorldExportSettings exportSettings, Platform platform) {
        super(world, exportSettings, platform);
        this.platformProvider = (JavaPlatformProvider) super.platformProvider;
    }

    @SuppressWarnings("ConstantConditions") // Clarity
    protected JavaLevel createWorld(File worldDir, String name) throws IOException {
        Dimension dim0 = world.getDimension(NORMAL_DETAIL);
        JavaLevel level = JavaLevel.create(platform, world.getMinHeight(), world.getMaxHeight());
        level.setSeed(dim0.getMinecraftSeed());
        level.setName(name);

        // Set the spawn point if applicable, and if necessary move it to the selected tiles
        final Set<Integer> selectedDimensions = worldExportSettings.getDimensionsToExport();
        if ((selectedDimensions == null) || selectedDimensions.contains(DIM_NORMAL)) {
            Point spawnPoint = world.getSpawnPoint();
            final Set<Point> selectedTiles = worldExportSettings.getTilesToExport();
            if ((selectedTiles != null) && (! selectedTiles.contains(new Point(spawnPoint.x >> TILE_SIZE_BITS, spawnPoint.y >> TILE_SIZE_BITS)))) {
                int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;
                for (Point tileCoords: selectedTiles) {
                    if ((tileCoords.x << TILE_SIZE_BITS) < minX) {
                        minX = (tileCoords.x << TILE_SIZE_BITS);
                    }
                    if (((tileCoords.x << TILE_SIZE_BITS) + TILE_SIZE - 1) > maxX) {
                        maxX = ((tileCoords.x << TILE_SIZE_BITS) + TILE_SIZE - 1);
                    }
                    if ((tileCoords.y << TILE_SIZE_BITS) < minY) {
                        minY = (tileCoords.y << TILE_SIZE_BITS);
                    }
                    if (((tileCoords.y << TILE_SIZE_BITS) + TILE_SIZE - 1) > maxY) {
                        maxY = ((tileCoords.y << TILE_SIZE_BITS) + TILE_SIZE - 1);
                    }
                }
                spawnPoint = new Point((minX + maxX) / 2, (minY + maxY) / 2);
                // TODO if the spawn point was not on the surface, figure out if its dimension exists here as well and if so place it there
                level.setSpawn(spawnPoint.x, Math.max(getIntHeightAt(DIM_NORMAL, spawnPoint.x, spawnPoint.y), getWaterLevelAt(DIM_NORMAL, spawnPoint.x, spawnPoint.y)) + 1, spawnPoint.y);
            } else {
                if (world.getSpawnPointDimension() != null) {
                    final Dimension spawnDimension = world.getDimension(world.getSpawnPointDimension());
                    level.setSpawn(spawnPoint.x, Math.max(spawnDimension.getIntHeightAt(spawnPoint.x, spawnPoint.y), spawnDimension.getWaterLevelAt(spawnPoint.x, spawnPoint.y)) + 1, spawnPoint.y);
                } else {
                    level.setSpawn(spawnPoint.x, Math.max(getIntHeightAt(DIM_NORMAL, spawnPoint.x, spawnPoint.y), getWaterLevelAt(DIM_NORMAL, spawnPoint.x, spawnPoint.y)) + 1, spawnPoint.y);
                }
            }
        }

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
            final Anchor anchor = dimension.getAnchor();
            if (anchor.invert || (anchor.role != DETAIL)) {
                // Ceiling dimension or wrong role
                continue;
            } else if ((! platform.capabilities.contains(GENERATOR_PER_DIMENSION)) && (anchor.dim != DIM_NORMAL)) {
                // This platform only supports generator settings for the surface dimension, and this is not the surface dimension
                continue;
            }
            Dimension.Border dimensionBorder = dimension.getBorder();
            if ((dimensionBorder != null) && dimensionBorder.isEndless()) {
                final SuperflatPreset.Builder superflatPresetBuilder;
                final int biome;
                final Structure[] structures;
                switch (anchor.dim) {
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
                                biome = getBiomeScheme(platform).isBiomePresent(BIOME_THE_VOID) ? BIOME_THE_VOID : BIOME_PLAINS;
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
                        final int borderLevel = dimension.getBorderLevel() - dimension.getMinHeight() + 1;
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
                        superflatPresetBuilder.addLayer(MC_BARRIER, dimension.getMaxHeight() - dimension.getMinHeight() - ((dimension.getRoofType() == Dimension.WallType.BEDROCK) ? 1 : 0));
                        if (dimension.getRoofType() == Dimension.WallType.BEDROCK) {
                            superflatPresetBuilder.addLayer(MC_BEDROCK, 1);
                        }
                        break;
                    default:
                        throw new InternalError();
                }
                if ((dimension.getRoofType() != null) && (dimensionBorder != ENDLESS_BARRIER)) {
                    int totalDepth = superflatPresetBuilder.getLayerDepth();
                    superflatPresetBuilder.addLayer(MC_AIR, dimension.getMaxHeight() - dimension.getMinHeight() - totalDepth - 1);
                    superflatPresetBuilder.addLayer((dimension.getRoofType() == Dimension.WallType.BEDROCK) ? MC_BEDROCK : MC_BARRIER, 1);
                }
                level.setGenerator(anchor.dim, new SuperflatGenerator(superflatPresetBuilder.build()));
            } else {
                level.setGenerator(anchor.dim, dimension.getGenerator());
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
        return level;
    }

    @Override
    public Map<Integer, ChunkFactory.Stats> export(File baseDir, String name, File backupDir, ProgressReceiver progressReceiver) throws IOException, ProgressReceiver.OperationCancelled {
        return doWithMdcContext(() -> {
            // Sanity checks
            final Set<Point> selectedTiles = worldExportSettings.getTilesToExport();
            final Set<Integer> selectedDimensions = worldExportSettings.getDimensionsToExport();
            if ((selectedTiles != null) && ((selectedDimensions == null) || (selectedDimensions.size() != 1))) {
                throw new IllegalArgumentException("If a tile selection is active then exactly one dimension must be selected");
            }

            // Backup existing level
            File worldDir = new File(baseDir, FileUtils.sanitiseName(name));
            logger.info("Exporting world " + world.getName() + " to map at " + worldDir + " in " + platform.displayName + " format");
            if (worldDir.isDirectory()) {
                if (backupDir != null) {
                    logger.info("Directory already exists; backing up to " + backupDir);
                    if (! worldDir.renameTo(backupDir)) {
                        throw new FileInUseException("Could not move " + worldDir + " to " + backupDir);
                    }
                } else {
                    throw new IllegalStateException("Directory already exists and no backup directory specified");
                }
            }

            // Record start of export
            long start = System.currentTimeMillis();

            // Create the level.dat file
            final JavaLevel level = createWorld(worldDir, name);

            // Lock the level.dat file, to keep Minecraft out until we are done
            File levelDatFile = new File(worldDir, "level.dat");
            // We need to load the level.dat file later when creating chunk stores, so keep that working even though the
            // file is exclusively locked:
            JavaLevel.setCachedLevel(levelDatFile, level);
            try (RandomAccessFile lockedFile = new RandomAccessFile(levelDatFile, "rw")) {
                lockedFile.getChannel().lock();

                // Copy the manually configured data packs, if any
                copyDataPacks(worldDir);

                Map<Integer, ChunkFactory.Stats> stats = new HashMap<>();
                int selectedDimension;
                if (selectedTiles == null) {
                    selectedDimension = -1;
                    boolean first = true;
                    for (Dimension dimension: world.getDimensionsWithRole(DETAIL, false, 0)) {
                        if ((selectedDimensions != null) && (! selectedDimensions.contains(dimension.getAnchor().dim))) {
                            // This dimension has not been selected to be exported, so skip it
                            continue;
                        }
                        if (first) {
                            first = false;
                        } else if (progressReceiver != null) {
                            progressReceiver.reset();
                        }
                        stats.put(dimension.getAnchor().dim, exportDimension(worldDir, dimension, progressReceiver));
                    }
                } else {
                    selectedDimension = selectedDimensions.iterator().next();
                    stats.put(selectedDimension, exportDimension(worldDir, world.getDimension(new Anchor(selectedDimension, DETAIL, false, 0)), progressReceiver));
                }

                // Update the session.lock file, hopefully kicking out any Minecraft instances which may have tried to
                // open the map in the mean time:
                File sessionLockFile = new File(worldDir, "session.lock");
                try (DataOutputStream sessionOut = new DataOutputStream(new FileOutputStream(sessionLockFile))) {
                    sessionOut.writeLong(System.currentTimeMillis());
                }

                // Record the export in the world history
                if (selectedTiles == null) {
                    world.addHistoryEntry(HistoryEntry.WORLD_EXPORTED_FULL, name, worldDir);
                } else {
                    world.addHistoryEntry(HistoryEntry.WORLD_EXPORTED_PARTIAL, name, worldDir, world.getDimension(new Anchor(selectedDimension, DETAIL, false, 0)).getName());
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
                    event.setAttribute(ATTRIBUTE_KEY_GENERATOR, world.getDimension(NORMAL_DETAIL).getGenerator().getType().name());
                    Dimension dimension = world.getDimension(NORMAL_DETAIL);
                    event.setAttribute(ATTRIBUTE_KEY_TILES, dimension.getTileCount());
                    logLayers(dimension, event, "");
                    dimension = world.getDimension(NETHER_DETAIL);
                    if (dimension != null) {
                        event.setAttribute(ATTRIBUTE_KEY_NETHER_TILES, dimension.getTileCount());
                        logLayers(dimension, event, "nether.");
                    }
                    dimension = world.getDimension(END_DETAIL);
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
        }, "world.name", world.getName(), "platform.id", platform.id, "world.minHeight", world.getMinHeight(), "world.maxHeight", world.getMaxHeight(), "baseDir", baseDir);
    }

    protected ChunkFactory.Stats exportDimension(File worldDir, Dimension dimension, ProgressReceiver progressReceiver) {
        return doWithMdcContext(() -> {
            final Anchor anchor = dimension.getAnchor();
            final File dimensionDir;
            switch (anchor.dim) {
                case DIM_NORMAL:
                    dimensionDir = worldDir;
                    break;
                case DIM_NETHER:
                    dimensionDir = new File(worldDir, "DIM-1");
                    break;
                case DIM_END:
                    dimensionDir = new File(worldDir, "DIM1");
                    break;
                default:
                    throw new IllegalArgumentException("Dimension " + anchor.dim + " not supported");
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

            if (progressReceiver != null) {
                progressReceiver.setProgress(1.0f);
            }

            return collectedStats;
        }, "dimension.anchor", dimension.getAnchor(), "dimension.minHeight", dimension.getMinHeight(), "dimension.maxHeight", dimension.getMaxHeight());
    }

    private void copyDataPacks(File worldDir) throws IOException {
        if (world.getDataPacks() != null) {
            final File dataPacksDir = new File(worldDir, "datapacks");
            for (File dataPackFile: world.getDataPacks()) {
                if (! dataPackFile.exists()) {
                    logger.error("Data pack file " + dataPackFile + " does not exist; skipping data pack");
                } else if (! dataPackFile.isFile()) {
                    logger.error("Data pack file " + dataPackFile + " is not a regular file; skipping data pack");
                } else if (! dataPackFile.canRead()) {
                    logger.error("Access denied to data pack file " + dataPackFile + "; skipping data pack");
                } else {
                    if (!dataPacksDir.exists()) {
                        if (!dataPacksDir.mkdirs()) {
                            throw new IOException("Could not create data packs directory");
                        }
                    }
                    FileUtils.copyFileToDir(dataPackFile, dataPacksDir);
                }
            }
        }
    }

    protected final JavaPlatformProvider platformProvider;

    private static final Logger logger = LoggerFactory.getLogger(JavaWorldExporter.class);
}