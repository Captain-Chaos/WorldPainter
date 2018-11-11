/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.worldpainter.exporting;

import org.jnbt.StringTag;
import org.jnbt.XMLTransformer;
import org.pepsoft.minecraft.ChunkFactory;
import org.pepsoft.minecraft.Level;
import org.pepsoft.util.FileUtils;
import org.pepsoft.util.ProgressReceiver;
import org.pepsoft.worldpainter.*;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.history.HistoryEntry;
import org.pepsoft.worldpainter.util.FileInUseException;
import org.pepsoft.worldpainter.vo.EventVO;

import java.awt.*;
import java.io.*;
import java.util.*;

import static org.pepsoft.minecraft.Constants.*;
import static org.pepsoft.worldpainter.Constants.*;
import static org.pepsoft.worldpainter.DefaultPlugin.*;

/**
 *
 * @author pepijn
 */
public class JavaWorldExporter extends AbstractWorldExporter {
    public JavaWorldExporter(World2 world) {
        super(world);
        if ((! (platform == JAVA_ANVIL))
                && (! (platform == JAVA_MCREGION))
                && (! (platform == JAVA_ANVIL_1_13))) {
            throw new IllegalArgumentException("Unsupported platform " + platform);
        }
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
        logger.info("Exporting world " + world.getName() + " to map at " + worldDir);
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
        Level level = new Level(world.getMaxHeight(), world.getPlatform());
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
            if (platform == JAVA_ANVIL_1_13) {
                // TODOMC13
                throw new UnsupportedOperationException("Endless border not yet supported for Minecraft 1.13");
            } else {
                StringBuilder generatorOptions = new StringBuilder("3;");
                switch (dim0Border) {
                    case ENDLESS_LAVA:
                    case ENDLESS_WATER:
                        boolean bottomless = dim0.isBottomless();
                        int borderLevel = dim0.getBorderLevel();
                        int oceanDepth = Math.min(borderLevel / 2, 20);
                        int dirtDepth = borderLevel - oceanDepth - (bottomless ? 1 : 0);
                        if (! bottomless) {
                            generatorOptions.append("1*minecraft:bedrock,");
                        }
                        generatorOptions.append(dirtDepth);
                        generatorOptions.append("*minecraft:dirt,");
                        generatorOptions.append(oceanDepth);
                        generatorOptions.append((dim0Border == Dimension.Border.ENDLESS_WATER) ? "*minecraft:water;0;" : "*minecraft:lava;1;");
                        break;
                    case ENDLESS_VOID:
                        generatorOptions.append("1*minecraft:air;1;");
                        break;
                }
                generatorOptions.append(DEFAULT_GENERATOR_OPTIONS);
                level.setGeneratorOptions(XMLTransformer.fromXML(new StringReader(generatorOptions.toString())));
            }
            level.setMapFeatures(false);
            level.setGenerator(Generator.FLAT);
        } else {
            level.setMapFeatures(world.isMapFeatures());
            if (world.getGenerator() == Generator.CUSTOM) {
                level.setGeneratorName(world.getGeneratorOptions());
            } else {
                level.setGenerator(world.getGenerator());
            }
        }
        if ((world.getPlatform() != JAVA_MCREGION)) {
            if ((! endlessBorder) && (world.getGenerator() == Generator.FLAT) && (world.getGeneratorOptions() != null)) {
                level.setGeneratorOptions((platform == JAVA_ANVIL)
                        ? new StringTag(TAG_GENERATOR_OPTIONS, world.getGeneratorOptions())
                        : XMLTransformer.fromXML(new StringReader(world.getGeneratorOptions())));
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
            event.setAttribute(ATTRIBUTE_KEY_PLATFORM, world.getPlatform().displayName);
            event.setAttribute(ATTRIBUTE_KEY_MAP_FEATURES, world.isMapFeatures());
            event.setAttribute(ATTRIBUTE_KEY_GAME_TYPE_NAME, world.getGameType().name());
            event.setAttribute(ATTRIBUTE_KEY_ALLOW_CHEATS, world.isAllowCheats());
            event.setAttribute(ATTRIBUTE_KEY_GENERATOR, world.getGenerator().name());
            if ((world.getPlatform() == JAVA_ANVIL) && (world.getGenerator() == Generator.FLAT) && (world.getGeneratorOptions() != null)) {
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