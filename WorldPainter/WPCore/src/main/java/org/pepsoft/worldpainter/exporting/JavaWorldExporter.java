/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.worldpainter.exporting;

import org.jnbt.StringTag;
import org.pepsoft.minecraft.ChunkFactory;
import org.pepsoft.minecraft.Level;
import org.pepsoft.minecraft.SuperflatPreset;
import org.pepsoft.util.FileUtils;
import org.pepsoft.util.ProgressReceiver;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.*;
import org.pepsoft.worldpainter.history.HistoryEntry;
import org.pepsoft.worldpainter.util.FileInUseException;
import org.pepsoft.worldpainter.vo.EventVO;

import java.awt.*;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;

import static org.pepsoft.minecraft.Constants.*;
import static org.pepsoft.minecraft.SuperflatPreset.Structure.*;
import static org.pepsoft.worldpainter.Constants.*;
import static org.pepsoft.worldpainter.DefaultPlugin.*;
import static org.pepsoft.worldpainter.Dimension.Border.ENDLESS_WATER;
import static org.pepsoft.worldpainter.biomeschemes.Minecraft1_13Biomes.BIOME_VOID;
import static org.pepsoft.worldpainter.biomeschemes.Minecraft1_7Biomes.BIOME_OCEAN;
import static org.pepsoft.worldpainter.biomeschemes.Minecraft1_7Biomes.BIOME_PLAINS;

/**
 *
 * @author pepijn
 */
public class JavaWorldExporter extends AbstractWorldExporter { // TODO can this be made a BlockBasedPlatformProviderWorldExporter?
    public JavaWorldExporter(World2 world) {
        super(world, world.getPlatform());
        if ((! (platform == JAVA_ANVIL))
                && (! (platform == JAVA_MCREGION))
                && (! (platform == JAVA_ANVIL_1_14))) {
            throw new IllegalArgumentException("Unsupported platform " + platform);
        }
    }

    protected JavaWorldExporter(World2 world, Platform platform) {
        super(world, platform);
    }

    @Override
    public Map<Integer, ChunkFactory.Stats> export(File baseDir, String name, File backupDir, ProgressReceiver progressReceiver) throws IOException, ProgressReceiver.OperationCancelled {
        // Sanity checks
        if ((selectedTiles == null) && (selectedDimensions != null)) {
            throw new IllegalArgumentException("Exporting a subset of dimensions not supported");
        }
        if ((world.getGenerator() == Generator.CUSTOM) && ((world.getGeneratorOptions() == null) || world.getGeneratorOptions().trim().isEmpty())) {
            throw new IllegalArgumentException("Custom world generator name not set");
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
        Level level = new Level(world.getMaxHeight(), platform);
        level.setSeed(dim0.getMinecraftSeed());
        level.setName(name);
        Point spawnPoint = world.getSpawnPoint();
        level.setSpawnX(spawnPoint.x);
        level.setSpawnY(Math.max(dim0.getIntHeightAt(spawnPoint), dim0.getWaterLevelAt(spawnPoint)));
        level.setSpawnZ(spawnPoint.y);
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
        Dimension.Border dim0Border = dim0.getBorder();
        boolean endlessBorder = (dim0Border != null) && dim0Border.isEndless();
        if (endlessBorder) {
            SuperflatPreset.Builder superflatPresetBuilder;
            switch (dim0Border) {
                case ENDLESS_LAVA:
                case ENDLESS_WATER:
                    superflatPresetBuilder = SuperflatPreset.builder((dim0Border == ENDLESS_WATER) ? BIOME_OCEAN : BIOME_PLAINS, MINESHAFT, BIOME_1, DUNGEON, DECORATION, OCEANMONUMENT);
                    boolean bottomless = dim0.isBottomless();
                    int borderLevel = dim0.getBorderLevel() + 1;
                    int oceanDepth = Math.max(Math.min(borderLevel / 2, 20), 1);
                    int stoneDepth = Math.max(borderLevel - oceanDepth - (bottomless ? 0 : 1) - 5, 0);
                    int dirtDepth = Math.max(borderLevel - oceanDepth - (bottomless ? 0 : 1) - stoneDepth, 0);
                    if (! bottomless) {
                        superflatPresetBuilder.addLayer(MC_BEDROCK, 1);
                    }
                    if (stoneDepth > 0) {
                        superflatPresetBuilder.addLayer(MC_STONE, stoneDepth);
                    }
                    if (dirtDepth > 0) {
                        superflatPresetBuilder.addLayer(MC_DIRT, dirtDepth);
                    }
                    if (oceanDepth > 0) {
                        superflatPresetBuilder.addLayer((dim0Border == ENDLESS_WATER) ? MC_WATER : MC_LAVA, oceanDepth);
                    }
                    break;
                case ENDLESS_VOID:
                    superflatPresetBuilder = SuperflatPreset.builder((platform == JAVA_ANVIL_1_14) ? BIOME_VOID : BIOME_PLAINS);
                    superflatPresetBuilder.addLayer(MC_AIR, 1);
                    break;
                default:
                    throw new InternalError();
            }
            if (platform != JAVA_ANVIL_1_14) {
                level.setGeneratorOptions(new StringTag(TAG_GENERATOR_OPTIONS_, superflatPresetBuilder.build().toMinecraft1_12_2()));
            } else {
                level.setGeneratorOptions(superflatPresetBuilder.build().toMinecraft1_13_2());
            }
            level.setGenerator(Generator.FLAT);
        } else {
            if (world.getGenerator() == Generator.CUSTOM) {
                level.setGeneratorName(world.getGeneratorOptions());
            } else {
                level.setGenerator(world.getGenerator());
            }
        }
        level.setMapFeatures(world.isMapFeatures());
        if ((platform != JAVA_MCREGION)) {
            if ((! endlessBorder) && (world.getGenerator() == Generator.FLAT) && ((world.getGeneratorOptions() != null) || (world.getSuperflatPreset() != null))) {
                if (world.getSuperflatPreset() != null) {
                    level.setGeneratorOptions((platform == JAVA_ANVIL)
                            ? new StringTag(TAG_GENERATOR_OPTIONS_, world.getSuperflatPreset().toMinecraft1_12_2())
                            : world.getSuperflatPreset().toMinecraft1_13_2());
                } else {
                    level.setGeneratorOptions(new StringTag(TAG_GENERATOR_OPTIONS_, world.getGeneratorOptions()));
                }
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
            event.setAttribute(ATTRIBUTE_KEY_MAP_FEATURES, world.isMapFeatures());
            event.setAttribute(ATTRIBUTE_KEY_GAME_TYPE_NAME, world.getGameType().name());
            event.setAttribute(ATTRIBUTE_KEY_ALLOW_CHEATS, world.isAllowCheats());
            event.setAttribute(ATTRIBUTE_KEY_GENERATOR, world.getGenerator().name());
            if ((platform == JAVA_ANVIL) && (world.getGenerator() == Generator.FLAT) && (world.getGeneratorOptions() != null)) {
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
        File regionDir = new File(dimensionDir, "region");
        if (! regionDir.exists()) {
            if (! regionDir.mkdirs()) {
                throw new RuntimeException("Could not create directory " + regionDir);
            }
        }

        ChunkFactory.Stats collectedStats = parallelExportRegions(dimension, worldDir, progressReceiver);

        // Calculate total size of dimension
        Set<Point> regions = new HashSet<>();
        if (selectedTiles != null) {
            for (Point tile: selectedTiles) {
                regions.add(new Point(tile.x >> 2, tile.y >> 2));
            }
        } else {
            for (Tile tile: dimension.getTiles()) {
                // Also add regions for any bedrock wall and/or border
                // tiles, if present
                int r = (((dimension.getBorder() != null) && (! dimension.getBorder().isEndless())) ? dimension.getBorderSize() : 0)
                        + (((dimension.getBorder() == null) || (! dimension.getBorder().isEndless())) && dimension.isBedrockWall() ? 1 : 0);
                for (int dx = -r; dx <= r; dx++) {
                    for (int dy = -r; dy <= r; dy++) {
                        regions.add(new Point((tile.getX() + dx) >> 2, (tile.getY() + dy) >> 2));
                    }
                }
            }
            if (ceiling != null) {
                for (Tile tile: ceiling.getTiles()) {
                    regions.add(new Point(tile.getX() >> 2, tile.getY() >> 2));
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

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(JavaWorldExporter.class);
    private static final String DEFAULT_GENERATOR_OPTIONS = "village,mineshaft(chance=0.01),stronghold(distance=32 count=3 spread=3),biome_1(distance=32),dungeon,decoration,lake,lava_lake,oceanmonument(spacing=32 separation=5)";
}