/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.importing;

import org.jnbt.CompoundTag;
import org.jnbt.NBTInputStream;
import org.jnbt.Tag;
import org.pepsoft.minecraft.*;
import org.pepsoft.util.ProgressReceiver;
import org.pepsoft.util.SubProgressReceiver;
import org.pepsoft.worldpainter.*;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.history.HistoryEntry;
import org.pepsoft.worldpainter.layers.*;
import org.pepsoft.worldpainter.layers.exporters.FrostExporter.FrostSettings;
import org.pepsoft.worldpainter.layers.exporters.ResourcesExporter.ResourcesExporterSettings;
import org.pepsoft.worldpainter.themes.SimpleTheme;
import org.pepsoft.worldpainter.vo.EventVO;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.regex.Pattern;

import static org.pepsoft.minecraft.Constants.*;
import static org.pepsoft.worldpainter.Constants.*;
import static org.pepsoft.worldpainter.DefaultPlugin.*;

/**
 *
 * @author pepijn
 */
public class JavaMapImporter {
    public JavaMapImporter(TileFactory tileFactory, File levelDatFile, boolean populateNewChunks, Set<Point> chunksToSkip, ReadOnlyOption readOnlyOption, Set<Integer> dimensionsToImport) {
        if ((tileFactory == null) || (levelDatFile == null) || (readOnlyOption == null) || (dimensionsToImport == null)) {
            throw new NullPointerException();
        }
        if (! levelDatFile.isFile()) {
            throw new IllegalArgumentException(levelDatFile + " does not exist or is not a regular file");
        }
        this.tileFactory = tileFactory;
        this.levelDatFile = levelDatFile;
        this.populateNewChunks = populateNewChunks;
        this.chunksToSkip = chunksToSkip;
        this.readOnlyOption = readOnlyOption;
        this.dimensionsToImport = dimensionsToImport;
    }
    
    public World2 doImport() throws IOException {
        try {
            return doImport(null);
        } catch (ProgressReceiver.OperationCancelled e) {
            throw new InternalError();
        }
    }
    
    public World2 doImport(ProgressReceiver progressReceiver) throws IOException, ProgressReceiver.OperationCancelled {
        long start = System.currentTimeMillis();

        logger.info("Importing map from " + levelDatFile.getAbsolutePath());
        Level level = Level.load(levelDatFile);
        int version = level.getVersion();
        if ((version != SUPPORTED_VERSION_1) && (version != SUPPORTED_VERSION_2)) {
            throw new UnsupportedOperationException("Level format version " + version + " not supported");
        }
        String name = level.getName().trim();
        int maxHeight = level.getMaxHeight();
        World2 world = new World2((version == SUPPORTED_VERSION_1) ? JAVA_MCREGION : JAVA_ANVIL, maxHeight);
        world.addHistoryEntry(HistoryEntry.WORLD_IMPORTED_FROM_MINECRAFT_MAP, level.getName(), levelDatFile.getParentFile());
        world.setCreateGoodiesChest(false);
        world.setName(name);
        world.setSpawnPoint(new Point(level.getSpawnX(), level.getSpawnZ()));
        world.setImportedFrom(levelDatFile);
        world.setMapFeatures(level.isMapFeatures());
        if (level.isHardcore()) {
            world.setGameType(GameType.HARDCORE);
        } else {
            world.setGameType(GameType.values()[level.getGameType()]);
        }
        world.setGenerator(level.getGenerator());
        if (level.getGenerator() == Generator.CUSTOM) {
            world.setGeneratorOptions(level.getGeneratorName());
        } else {
            world.setGeneratorOptions(level.getGeneratorOptions());
        }
        world.setDifficulty(level.getDifficulty());
        if ((version == SUPPORTED_VERSION_2) && (level.getBorderSize() > 0.0)) {
            // If the world is version 0x4abd and actually has border settings,
            // load them
            world.getBorderSettings().setCentreX((int) (level.getBorderCenterX() + 0.5));
            world.getBorderSettings().setCentreY((int) (level.getBorderCenterZ() + 0.5));
            world.getBorderSettings().setSize((int) (level.getBorderSize() + 0.5));
            world.getBorderSettings().setSafeZone((int) (level.getBorderSafeZone() + 0.5));
            world.getBorderSettings().setWarningBlocks((int) (level.getBorderWarningBlocks()+ 0.5));
            world.getBorderSettings().setWarningTime((int) (level.getBorderWarningTime() + 0.5));
            world.getBorderSettings().setSizeLerpTarget((int) (level.getBorderSizeLerpTarget() + 0.5));
            world.getBorderSettings().setSizeLerpTime((int) level.getBorderSizeLerpTime());
            world.getBorderSettings().setDamagePerBlock((int) (level.getBorderDamagePerBlock() + 0.5));
        }
        File worldDir = levelDatFile.getParentFile();
        File regionDir = new File(worldDir, "region");
        File netherDir = new File(worldDir, "DIM-1/region");
        File endDir = new File(worldDir, "DIM1/region");
        int dimCount = 1;
        if (netherDir.isDirectory() && dimensionsToImport.contains(DIM_NETHER)) {
            dimCount++;
        }
        if (endDir.isDirectory() && dimensionsToImport.contains(DIM_END)) {
            dimCount++;
        }
        long minecraftSeed = level.getSeed();
        tileFactory.setSeed(minecraftSeed);
        Dimension dimension = new Dimension(world, minecraftSeed, tileFactory, DIM_NORMAL, maxHeight);
        dimension.setEventsInhibited(true);
        try {
            dimension.setCoverSteepTerrain(false);
            dimension.setSubsurfaceMaterial(Terrain.STONE);
            dimension.setBorderLevel(62);
            
            // Turn off smooth snow
            FrostSettings frostSettings = new FrostSettings();
            frostSettings.setMode(FrostSettings.MODE_FLAT);
            dimension.setLayerSettings(Frost.INSTANCE, frostSettings);
            
            ResourcesExporterSettings resourcesSettings = (ResourcesExporterSettings) dimension.getLayerSettings(Resources.INSTANCE);
            resourcesSettings.setMinimumLevel(0);
            if (version == SUPPORTED_VERSION_1) {
                resourcesSettings.setChance(BLK_EMERALD_ORE, 0);
            }
            Configuration config = Configuration.getInstance();
            dimension.setGridEnabled(config.isDefaultGridEnabled());
            dimension.setGridSize(config.getDefaultGridSize());
            dimension.setContoursEnabled(config.isDefaultContoursEnabled());
            dimension.setContourSeparation(config.getDefaultContourSeparation());
            String dimWarnings = importDimension(regionDir, dimension, version, (progressReceiver != null) ? new SubProgressReceiver(progressReceiver, 0.0f, 1.0f / dimCount) : null);
            if (dimWarnings != null) {
                if (warnings == null) {
                    warnings = dimWarnings;
                } else {
                    warnings = warnings + dimWarnings;
                }
            }
        } finally {
            dimension.setEventsInhibited(false);
        }
        world.addDimension(dimension);
        int dimNo = 1;
        if (netherDir.isDirectory() && dimensionsToImport.contains(DIM_NETHER)) {
            HeightMapTileFactory netherTileFactory = TileFactoryFactory.createNoiseTileFactory(minecraftSeed + 1, Terrain.NETHERRACK, maxHeight, 188, 192, true, false, 20f, 1.0);
            SimpleTheme theme = (SimpleTheme) netherTileFactory.getTheme();
            SortedMap<Integer, Terrain> terrainRanges = theme.getTerrainRanges();
            terrainRanges.clear();
            terrainRanges.put(-1, Terrain.NETHERRACK);
            theme.setTerrainRanges(terrainRanges);
            theme.setLayerMap(null);
            dimension = new Dimension(world, minecraftSeed + 1, netherTileFactory, DIM_NETHER, maxHeight);
            dimension.setEventsInhibited(true);
            try {
                dimension.setCoverSteepTerrain(false);
                dimension.setSubsurfaceMaterial(Terrain.NETHERRACK);
                ResourcesExporterSettings resourcesSettings = (ResourcesExporterSettings) dimension.getLayerSettings(Resources.INSTANCE);
                resourcesSettings.setMinimumLevel(0);
                if (version == SUPPORTED_VERSION_1) {
                    resourcesSettings.setChance(BLK_QUARTZ_ORE, 0);
                }
                String dimWarnings = importDimension(netherDir, dimension, version, (progressReceiver != null) ? new SubProgressReceiver(progressReceiver, (float) dimNo++ / dimCount, 1.0f / dimCount) : null);
                if (dimWarnings != null) {
                    if (warnings == null) {
                        warnings = dimWarnings;
                    } else {
                        warnings = warnings + dimWarnings;
                    }
                }
            } finally {
                dimension.setEventsInhibited(false);
            }
            world.addDimension(dimension);
        }
        if (endDir.isDirectory() && dimensionsToImport.contains(DIM_END)) {
            HeightMapTileFactory endTileFactory = TileFactoryFactory.createNoiseTileFactory(minecraftSeed + 2, Terrain.END_STONE, maxHeight, 32, 0, false, false, 20f, 1.0);
            SimpleTheme theme = (SimpleTheme) endTileFactory.getTheme();
            SortedMap<Integer, Terrain> terrainRanges = theme.getTerrainRanges();
            terrainRanges.clear();
            terrainRanges.put(-1, Terrain.END_STONE);
            theme.setTerrainRanges(terrainRanges);
            theme.setLayerMap(Collections.emptyMap());
            dimension = new Dimension(world, minecraftSeed + 2, endTileFactory, DIM_END, maxHeight);
            dimension.setEventsInhibited(true);
            try {
                dimension.setCoverSteepTerrain(false);
                dimension.setSubsurfaceMaterial(Terrain.END_STONE);
                String dimWarnings = importDimension(endDir, dimension, version, (progressReceiver != null) ? new SubProgressReceiver(progressReceiver, (float) dimNo / dimCount, 1.0f / dimCount) : null);
                if (dimWarnings != null) {
                    if (warnings == null) {
                        warnings = dimWarnings;
                    } else {
                        warnings = warnings + dimWarnings;
                    }
                }
            } finally {
                dimension.setEventsInhibited(false);
            }
            world.addDimension(dimension);
        }
        
        // Log an event
        Configuration config = Configuration.getInstance();
        if (config != null) {
            EventVO event = new EventVO(EVENT_KEY_ACTION_IMPORT_MAP).duration(System.currentTimeMillis() - start);
            event.setAttribute(EventVO.ATTRIBUTE_TIMESTAMP, new Date(start));
            event.setAttribute(ATTRIBUTE_KEY_MAX_HEIGHT, world.getMaxHeight());
            event.setAttribute(ATTRIBUTE_KEY_PLATFORM, world.getPlatform().displayName);
            event.setAttribute(ATTRIBUTE_KEY_MAP_FEATURES, world.isMapFeatures());
            event.setAttribute(ATTRIBUTE_KEY_GAME_TYPE_NAME, world.getGameType().name());
            event.setAttribute(ATTRIBUTE_KEY_ALLOW_CHEATS, world.isAllowCheats());
            event.setAttribute(ATTRIBUTE_KEY_GENERATOR, world.getGenerator().name());
            if (world.getPlatform().equals(JAVA_ANVIL) && (world.getGenerator() == Generator.FLAT)) {
                event.setAttribute(ATTRIBUTE_KEY_GENERATOR_OPTIONS, world.getGeneratorOptions());
            }
            event.setAttribute(ATTRIBUTE_KEY_TILES, dimension.getTiles().size());
            config.logEvent(event);
        }
        
        return world;
    }

    public String getWarnings() {
        return warnings;
    }
    
    private String importDimension(File regionDir, Dimension dimension, int version, ProgressReceiver progressReceiver) throws IOException, ProgressReceiver.OperationCancelled {
        if (progressReceiver != null) {
            progressReceiver.setMessage(dimension.getName() + " dimension");
        }
        final int maxHeight = dimension.getMaxHeight();
        final int maxY = maxHeight - 1;
        final Pattern regionFilePattern = (version == SUPPORTED_VERSION_1)
            ? Pattern.compile("r\\.-?\\d+\\.-?\\d+\\.mcr")
            : Pattern.compile("r\\.-?\\d+\\.-?\\d+\\.mca");
        final File[] regionFiles = regionDir.listFiles((dir, name) -> regionFilePattern.matcher(name).matches());
        if ((regionFiles == null) || (regionFiles.length == 0)) {
            throw new RuntimeException("The " + dimension.getName() + " dimension of this map has no region files!");
        }
        final Set<Point> newChunks = new HashSet<>();
//        final SortedSet<Material> manMadeBlockTypes = new TreeSet<Material>();
        final boolean importBiomes = (version == SUPPORTED_VERSION_2) && (dimension.getDim() == DIM_NORMAL);
        final int total = regionFiles.length * 1024;
        int count = 0;
        final StringBuilder reportBuilder = new StringBuilder();
        for (File file: regionFiles) {
            try {
                RegionFile regionFile = new RegionFile(file, true);
                try {
                    for (int x = 0; x < 32; x++) {
                        for (int z = 0; z < 32; z++) {
                            if (progressReceiver != null) {
                                progressReceiver.setProgress((float) count / total);
                                count++;
                            }
                            final Point chunkCoords = new Point((regionFile.getX() << 5) | x, (regionFile.getZ() << 5) | z);
                            if ((chunksToSkip != null) && chunksToSkip.contains(chunkCoords)) {
                                continue;
                            }
                            if (regionFile.containsChunk(x, z)) {
                                final Tag tag;
                                try {
                                    final InputStream chunkData = regionFile.getChunkDataInputStream(x, z);
                                    if (chunkData == null) {
                                        // This should never happen, since we checked
                                        // with isChunkPresent(), but in practice it
                                        // does. Perhaps corrupted data?
                                        reportBuilder.append("Missing chunk data for chunk " + x + ", " + z + " in " + file + "; skipping chunk" + EOL);
                                        logger.warn("Missing chunk data for chunk " + x + ", " + z + " in " + file + "; skipping chunk");
                                        continue;
                                    }
                                    try (NBTInputStream in = new NBTInputStream(chunkData)) {
                                        tag = in.readTag();
                                    }
                                } catch (IOException e) {
                                    reportBuilder.append("I/O error while reading chunk " + x + ", " + z + " from file " + file + " (message: \"" + e.getMessage() + "\"); skipping chunk" + EOL);
                                    logger.error("I/O error while reading chunk " + x + ", " + z + " from file " + file + "; skipping chunk", e);
                                    continue;
                                } catch (IllegalArgumentException e) {
                                    reportBuilder.append("Illegal argument exception while reading chunk " + x + ", " + z + " from file " + file + " (message: \"" + e.getMessage() + "\"); skipping chunk" + EOL);
                                    logger.error("Illegal argument exception while reading chunk " + x + ", " + z + " from file " + file + "; skipping chunk", e);
                                    continue;
                                } catch (NegativeArraySizeException e) {
                                    reportBuilder.append("Negative array size exception while reading chunk " + x + ", " + z + " from file " + file + " (message: \"" + e.getMessage() + "\"); skipping chunk" + EOL);
                                    logger.error("Negative array size exception while reading chunk " + x + ", " + z + " from file " + file + "; skipping chunk", e);
                                    continue;
                                }
                                final Chunk chunk = (version == SUPPORTED_VERSION_1)
                                    ? new ChunkImpl((CompoundTag) tag, maxHeight)
                                    : new ChunkImpl2((CompoundTag) tag, maxHeight);

                                final Point tileCoords = new Point(chunk.getxPos() >> 3, chunk.getzPos() >> 3);
                                Tile tile = dimension.getTile(tileCoords);
                                if (tile == null) {
                                    tile = dimension.getTileFactory().createTile(tileCoords.x, tileCoords.y);
                                    for (int xx = 0; xx < 8; xx++) {
                                        for (int yy = 0; yy < 8; yy++) {
                                            newChunks.add(new Point((tileCoords.x << TILE_SIZE_BITS) | (xx << 4), (tileCoords.y << TILE_SIZE_BITS) | (yy << 4)));
                                        }
                                    }
                                    dimension.addTile(tile);
                                }
                                newChunks.remove(new Point(chunk.getxPos() << 4, chunk.getzPos() << 4));

                                boolean manMadeStructuresBelowGround = false;
                                boolean manMadeStructuresAboveGround = false;
                                try {
                                    for (int xx = 0; xx < 16; xx++) {
                                        for (int zz = 0; zz < 16; zz++) {
                                            float height = -1.0f;
                                            int waterLevel = 0;
                                            boolean floodWithLava = false, frost = false;
                                            Terrain terrain = Terrain.BEDROCK;
                                            for (int y = maxY; y >= 0; y--) {
                                                int blockType = chunk.getBlockType(xx, y, zz);
                                                int data = chunk.getDataValue(xx, y, zz);
                                                if (! NATURAL_BLOCKS.get(blockType)) {
                                                    if (height == -1.0f) {
                                                        manMadeStructuresAboveGround = true;
                                                    } else {
                                                        manMadeStructuresBelowGround = true;
                                                    }
//                                                    manMadeBlockTypes.add(Material.get(blockType, data));
                                                }
                                                if ((blockType == BLK_SNOW) || (blockType == BLK_ICE)) {
                                                    frost = true;
                                                }
                                                if (((blockType == BLK_ICE) || (blockType == BLK_FROSTED_ICE) || (((blockType == BLK_STATIONARY_WATER) || (blockType == BLK_WATER) || (blockType == BLK_STATIONARY_LAVA) || (blockType == BLK_LAVA)) && (data == 0))) && (waterLevel == 0)) {
                                                    waterLevel = y;
                                                    if ((blockType == BLK_LAVA) || (blockType == BLK_STATIONARY_LAVA)) {
                                                        floodWithLava = true;
                                                    }
                                                } else if (height == -1.0f) {
                                                    final Material material = Material.get(blockType, data);
                                                    if (SPECIAL_TERRAIN_MAPPING.containsKey(material)) {
                                                        // Special terrain found
                                                        height = y - 0.4375f; // Value that falls in the middle of the lowest one eigthth which will still round to the same integer value and will receive a one layer thick smooth snow block (principle of least surprise)
                                                        terrain = SPECIAL_TERRAIN_MAPPING.get(material);
                                                    } else if (TERRAIN_MAPPING.containsKey(blockType)) {
                                                        // Terrain found
                                                        height = y - 0.4375f; // Value that falls in the middle of the lowest one eigthth which will still round to the same integer value and will receive a one layer thick smooth snow block (principle of least surprise)
                                                        terrain = TERRAIN_MAPPING.get(blockType);
                                                    }
                                                }
                                            }
                                            // Use smooth snow, if present, to better approximate world height, so smooth snow will survive merge
                                            final int intHeight = (int) (height + 0.5f);
                                            if ((height != -1.0f) && (intHeight < maxY) && (chunk.getBlockType(xx, intHeight + 1, zz) == BLK_SNOW)) {
                                                int data = chunk.getDataValue(xx, intHeight + 1, zz);
                                                height += data * 0.125;
                                                
                                            }
                                            if ((waterLevel == 0) && (height >= 61.5f)) {
                                                waterLevel = 62;
                                            }

                                            final int blockX = (chunk.getxPos() << 4) | xx;
                                            final int blockY = (chunk.getzPos() << 4) | zz;
                                            final Point coords = new Point(blockX, blockY);
                                            dimension.setTerrainAt(coords, terrain);
                                            dimension.setHeightAt(coords, Math.max(height, 0.0f));
                                            dimension.setWaterLevelAt(blockX, blockY, waterLevel);
                                            if (frost) {
                                                dimension.setBitLayerValueAt(Frost.INSTANCE, blockX, blockY, true);
                                            }
                                            if (floodWithLava) {
                                                dimension.setBitLayerValueAt(FloodWithLava.INSTANCE, blockX, blockY, true);
                                            }
                                            if (height == -1.0f) {
                                                dimension.setBitLayerValueAt(org.pepsoft.worldpainter.layers.Void.INSTANCE, blockX, blockY, true);
                                            }
                                            if (importBiomes && chunk.isBiomesAvailable()) {
                                                final int biome = chunk.getBiome(xx, zz);
                                                // If the biome is set (around the edges of the map Minecraft sets it to
                                                // 255, presumably as a marker that it has yet to be calculated), copy
                                                // it to the dimension. However, if it matches what the automatic biome
                                                // would be, don't copy it, so that WorldPainter will automatically
                                                // adjust the biome when the user makes changes
                                                if ((biome != 255) && (biome != dimension.getAutoBiome(blockX, blockY))) {
                                                    dimension.setLayerValueAt(Biome.INSTANCE, blockX, blockY, biome);
                                                }
                                            }
                                        }
                                    }
                                } catch (NullPointerException e) {
                                    reportBuilder.append("Null pointer exception while reading chunk " + x + ", " + z + " from file " + file + "; skipping chunk" + EOL);
                                    logger.error("Null pointer exception while reading chunk " + x + ", " + z + " from file " + file + "; skipping chunk", e);
                                    continue;
                                } catch (ArrayIndexOutOfBoundsException e) {
                                    reportBuilder.append("Array index out of bounds while reading chunk " + x + ", " + z + " from file " + file + " (message: \"" + e.getMessage() + "\"); skipping chunk" + EOL);
                                    logger.error("Array index out of bounds while reading chunk " + x + ", " + z + " from file " + file + "; skipping chunk", e);
                                    continue;
                                }

                                if (((readOnlyOption == ReadOnlyOption.MAN_MADE) && (manMadeStructuresBelowGround || manMadeStructuresAboveGround))
                                        || ((readOnlyOption == ReadOnlyOption.MAN_MADE_ABOVE_GROUND) && manMadeStructuresAboveGround)
                                        || (readOnlyOption == ReadOnlyOption.ALL)) {
                                    dimension.setBitLayerValueAt(ReadOnly.INSTANCE, chunk.getxPos() << 4, chunk.getzPos() << 4, true);
                                }
                            }
                        }
                    }
                } finally {
                    regionFile.close();
                }
            } catch (IOException e) {
                reportBuilder.append("I/O error while opening region file " + file + " (message: \"" + e.getMessage() + "\"); skipping region" + EOL);
                logger.error("I/O error while opening region file " + file + "; skipping region", e);
            }
        }
        
        // Process chunks that were only added to fill out a tile
        for (Point newChunkCoords: newChunks) {
            dimension.setBitLayerValueAt(NotPresent.INSTANCE, newChunkCoords.x, newChunkCoords.y, true);
            if (populateNewChunks) {
                dimension.setBitLayerValueAt(Populate.INSTANCE, newChunkCoords.x, newChunkCoords.y, true);
            }
        }
        
        if (progressReceiver != null) {
            progressReceiver.setProgress(1.0f);
        }
        
//        System.err.println("Man-made block types encountered:");
//        for (Material blockType: manMadeBlockTypes) {
//            System.err.println(blockType);
//        }
        
        return reportBuilder.length() != 0 ? reportBuilder.toString() : null;
    }
    
    private final TileFactory tileFactory;
    private final File levelDatFile;
    private final boolean populateNewChunks;
    private final Set<Point> chunksToSkip;
    private final ReadOnlyOption readOnlyOption;
    private final Set<Integer> dimensionsToImport;
    private String warnings;
    
    public static final Map<Integer, Terrain> TERRAIN_MAPPING = new HashMap<>();
    public static final Map<Material, Terrain> SPECIAL_TERRAIN_MAPPING = new HashMap<>();
    public static final BitSet NATURAL_BLOCKS = new BitSet();

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(JavaMapImporter.class);
    private static final String EOL = System.getProperty("line.separator");
    
    static {
        TERRAIN_MAPPING.put(BLK_STONE, Terrain.STONE);
        TERRAIN_MAPPING.put(BLK_GRASS, Terrain.BARE_GRASS);
        TERRAIN_MAPPING.put(BLK_DIRT, Terrain.DIRT);
        TERRAIN_MAPPING.put(BLK_BEDROCK, Terrain.BEDROCK);
        TERRAIN_MAPPING.put(BLK_SAND, Terrain.SAND);
        TERRAIN_MAPPING.put(BLK_GRAVEL, Terrain.GRAVEL);
        TERRAIN_MAPPING.put(BLK_GOLD_ORE, Terrain.STONE);
        TERRAIN_MAPPING.put(BLK_IRON_ORE, Terrain.STONE);
        TERRAIN_MAPPING.put(BLK_COAL, Terrain.STONE);
        TERRAIN_MAPPING.put(BLK_LAPIS_LAZULI_ORE, Terrain.STONE);
        TERRAIN_MAPPING.put(BLK_DIAMOND_ORE, Terrain.STONE);
        TERRAIN_MAPPING.put(BLK_REDSTONE_ORE, Terrain.STONE);
        TERRAIN_MAPPING.put(BLK_GLOWING_REDSTONE_ORE, Terrain.STONE);
        TERRAIN_MAPPING.put(BLK_HIDDEN_SILVERFISH, Terrain.STONE);
        TERRAIN_MAPPING.put(BLK_SANDSTONE, Terrain.SANDSTONE);
        TERRAIN_MAPPING.put(BLK_OBSIDIAN, Terrain.OBSIDIAN);
        TERRAIN_MAPPING.put(BLK_TILLED_DIRT, Terrain.DIRT);
        TERRAIN_MAPPING.put(BLK_SNOW_BLOCK, Terrain.DEEP_SNOW);
        TERRAIN_MAPPING.put(BLK_CLAY, Terrain.CLAY);
        TERRAIN_MAPPING.put(BLK_NETHERRACK, Terrain.NETHERRACK);
        TERRAIN_MAPPING.put(BLK_QUARTZ_ORE, Terrain.NETHERRACK);
        TERRAIN_MAPPING.put(BLK_SOUL_SAND, Terrain.SOUL_SAND);
        TERRAIN_MAPPING.put(BLK_MYCELIUM, Terrain.MYCELIUM);
        TERRAIN_MAPPING.put(BLK_END_STONE, Terrain.END_STONE);
        TERRAIN_MAPPING.put(BLK_HARDENED_CLAY, Terrain.HARDENED_CLAY);
        TERRAIN_MAPPING.put(BLK_RED_SANDSTONE, Terrain.RED_SANDSTONE);
        TERRAIN_MAPPING.put(BLK_GRASS_PATH, Terrain.GRASS_PATH);
        TERRAIN_MAPPING.put(BLK_MAGMA, Terrain.MAGMA); // TODO: or should this be mapped to stone and magma added to the Resources layer?

        SPECIAL_TERRAIN_MAPPING.put(Material.RED_SAND, Terrain.RED_SAND);
        SPECIAL_TERRAIN_MAPPING.put(Material.PERMADIRT, Terrain.PERMADIRT);
        SPECIAL_TERRAIN_MAPPING.put(Material.PODZOL, Terrain.PODZOL);
        SPECIAL_TERRAIN_MAPPING.put(Material.WHITE_CLAY, Terrain.WHITE_STAINED_CLAY);
        SPECIAL_TERRAIN_MAPPING.put(Material.ORANGE_CLAY, Terrain.ORANGE_STAINED_CLAY);
        SPECIAL_TERRAIN_MAPPING.put(Material.MAGENTA_CLAY, Terrain.MAGENTA_STAINED_CLAY);
        SPECIAL_TERRAIN_MAPPING.put(Material.LIGHT_BLUE_CLAY, Terrain.LIGHT_BLUE_STAINED_CLAY);
        SPECIAL_TERRAIN_MAPPING.put(Material.YELLOW_CLAY, Terrain.YELLOW_STAINED_CLAY);
        SPECIAL_TERRAIN_MAPPING.put(Material.LIME_CLAY, Terrain.LIME_STAINED_CLAY);
        SPECIAL_TERRAIN_MAPPING.put(Material.PINK_CLAY, Terrain.PINK_STAINED_CLAY);
        SPECIAL_TERRAIN_MAPPING.put(Material.GREY_CLAY, Terrain.GREY_STAINED_CLAY);
        SPECIAL_TERRAIN_MAPPING.put(Material.LIGHT_GREY_CLAY, Terrain.LIGHT_GREY_STAINED_CLAY);
        SPECIAL_TERRAIN_MAPPING.put(Material.CYAN_CLAY, Terrain.CYAN_STAINED_CLAY);
        SPECIAL_TERRAIN_MAPPING.put(Material.PURPLE_CLAY, Terrain.PURPLE_STAINED_CLAY);
        SPECIAL_TERRAIN_MAPPING.put(Material.BLUE_CLAY, Terrain.BLUE_STAINED_CLAY);
        SPECIAL_TERRAIN_MAPPING.put(Material.BROWN_CLAY, Terrain.BROWN_STAINED_CLAY);
        SPECIAL_TERRAIN_MAPPING.put(Material.GREEN_CLAY, Terrain.GREEN_STAINED_CLAY);
        SPECIAL_TERRAIN_MAPPING.put(Material.RED_CLAY, Terrain.RED_STAINED_CLAY);
        SPECIAL_TERRAIN_MAPPING.put(Material.BLACK_CLAY, Terrain.BLACK_STAINED_CLAY);
        SPECIAL_TERRAIN_MAPPING.put(Material.GRANITE, Terrain.GRANITE);
        SPECIAL_TERRAIN_MAPPING.put(Material.DIORITE, Terrain.DIORITE);
        SPECIAL_TERRAIN_MAPPING.put(Material.ANDESITE, Terrain.ANDESITE);

        // Make sure the tile entity flag in the block database is consistent
        // with the tile entity map:
        Set<Integer> allTerrainBlockIds = new HashSet<>();
        allTerrainBlockIds.addAll(TERRAIN_MAPPING.keySet());
        for (int blockId: TERRAIN_MAPPING.keySet()) {
            if (! Block.BLOCKS[blockId].terrain) {
                throw new AssertionError("Block " + blockId + " not marked as terrain block!");
            }
        }
        for (Material material: SPECIAL_TERRAIN_MAPPING.keySet()) {
            allTerrainBlockIds.add(material.blockType);
            if (! material.block.terrain) {
                throw new AssertionError("Block " + material.blockType + " not marked as terrain block!");
            }
        }
        for (Block block: Block.BLOCKS) {
            if (block.terrain && (! allTerrainBlockIds.contains(block.id))) {
                throw new AssertionError("Block " + block.id + " marked as terrain but not present in terrain type map!");
            }
        }
        
        // Gather natural blocks:
        for (Block block: Block.BLOCKS) {
            if (block.natural) {
                NATURAL_BLOCKS.set(block.id);
            }
        }

        // Consider dungeons as natural for historical reasons:
        NATURAL_BLOCKS.set(BLK_MONSTER_SPAWNER);
        NATURAL_BLOCKS.set(BLK_CHEST);
        NATURAL_BLOCKS.set(BLK_COBBLESTONE);
        NATURAL_BLOCKS.set(BLK_MOSSY_COBBLESTONE);
    }
    
    public enum ReadOnlyOption {NONE, MAN_MADE, MAN_MADE_ABOVE_GROUND, ALL}
}