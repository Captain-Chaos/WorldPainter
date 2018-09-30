/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.importing;

import org.jnbt.NBTInputStream;
import org.pepsoft.minecraft.*;
import org.pepsoft.util.ProgressReceiver;
import org.pepsoft.util.SubProgressReceiver;
import org.pepsoft.worldpainter.*;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.history.HistoryEntry;
import org.pepsoft.worldpainter.layers.*;
import org.pepsoft.worldpainter.layers.exporters.FrostExporter.FrostSettings;
import org.pepsoft.worldpainter.layers.exporters.ResourcesExporter.ResourcesExporterSettings;
import org.pepsoft.worldpainter.plugins.PlatformManager;
import org.pepsoft.worldpainter.themes.SimpleTheme;
import org.pepsoft.worldpainter.vo.EventVO;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.regex.Pattern;

import static org.pepsoft.minecraft.Constants.*;
import static org.pepsoft.minecraft.Material.*;
import static org.pepsoft.worldpainter.Constants.*;
import static org.pepsoft.worldpainter.DefaultPlugin.*;
import static org.pepsoft.worldpainter.biomeschemes.Minecraft1_13Biomes.BIOME_NAMES;
import static org.pepsoft.worldpainter.biomeschemes.Minecraft1_13Biomes.HIGHEST_BIOME_ID;

/**
 *
 * @author pepijn
 */
public class JavaMapImporter extends MapImporter {
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
    
    public World2 doImport(ProgressReceiver progressReceiver) throws IOException, ProgressReceiver.OperationCancelled {
        long start = System.currentTimeMillis();

        logger.info("Importing map from " + levelDatFile.getAbsolutePath());
        Level level = Level.load(levelDatFile);
        int version = level.getVersion();
        if ((version != VERSION_MCREGION) && (version != VERSION_ANVIL)) {
            throw new UnsupportedOperationException("Level format version " + version + " not supported");
        }
        String name = level.getName().trim();
        int maxHeight = level.getMaxHeight();
        Platform platform = (version == VERSION_MCREGION) ? JAVA_MCREGION : ((level.getDataVersion() <= DATA_VERSION_MC_1_12_2) ? JAVA_ANVIL : JAVA_ANVIL_1_13);
        World2 world = new World2(platform, maxHeight);
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
        if ((version == VERSION_ANVIL) && (level.getBorderSize() > 0.0)) {
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
            if (version == VERSION_MCREGION) {
                resourcesSettings.setChance(EMERALD_ORE, 0);
            }
            Configuration config = Configuration.getInstance();
            dimension.setGridEnabled(config.isDefaultGridEnabled());
            dimension.setGridSize(config.getDefaultGridSize());
            dimension.setContoursEnabled(config.isDefaultContoursEnabled());
            dimension.setContourSeparation(config.getDefaultContourSeparation());
            String dimWarnings = importDimension(regionDir, dimension, platform, (progressReceiver != null) ? new SubProgressReceiver(progressReceiver, 0.0f, 1.0f / dimCount) : null);
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
                if (version == VERSION_MCREGION) {
                    resourcesSettings.setChance(QUARTZ_ORE, 0);
                }
                String dimWarnings = importDimension(netherDir, dimension, platform, (progressReceiver != null) ? new SubProgressReceiver(progressReceiver, (float) dimNo++ / dimCount, 1.0f / dimCount) : null);
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
                String dimWarnings = importDimension(endDir, dimension, platform, (progressReceiver != null) ? new SubProgressReceiver(progressReceiver, (float) dimNo / dimCount, 1.0f / dimCount) : null);
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
            if ((world.getPlatform() == JAVA_ANVIL) && (world.getGenerator() == Generator.FLAT)) {
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
    
    @SuppressWarnings({"StringConcatenationInsideStringBufferAppend", "StringEquality"}) // Readability; Material names are interned
    private String importDimension(File regionDir, Dimension dimension, Platform platform, ProgressReceiver progressReceiver) throws ProgressReceiver.OperationCancelled {
        if (progressReceiver != null) {
            progressReceiver.setMessage(dimension.getName() + " dimension");
        }
        final int maxHeight = dimension.getMaxHeight();
        final int maxY = maxHeight - 1;
        final Pattern regionFilePattern = (platform == JAVA_MCREGION)
            ? Pattern.compile("r\\.-?\\d+\\.-?\\d+\\.mcr")
            : Pattern.compile("r\\.-?\\d+\\.-?\\d+\\.mca");
        final File[] regionFiles = regionDir.listFiles((dir, name) -> regionFilePattern.matcher(name).matches());
        if ((regionFiles == null) || (regionFiles.length == 0)) {
            throw new RuntimeException("The " + dimension.getName() + " dimension of this map has no region files!");
        }
        final Set<Point> newChunks = new HashSet<>();
        final Set<String> manMadeBlockTypes = new HashSet<>();
        final Set<Integer> unknownBiomes = new HashSet<>();
        final boolean importBiomes = dimension.getDim() == DIM_NORMAL;
        final int total = regionFiles.length * 1024;
        int count = 0;
        final StringBuilder reportBuilder = new StringBuilder();
        final JavaPlatformProvider platformProvider = (JavaPlatformProvider) PlatformManager.getInstance().getPlatformProvider(platform);
        for (File file: regionFiles) {
            try {
                try (RegionFile regionFile = new RegionFile(file, true)) {
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
                                final Chunk chunk;
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
                                        chunk = platformProvider.createChunk(platform, in.readTag(), maxHeight);
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
                                } catch (ClassCastException e) {
                                    reportBuilder.append("Class cast exception while reading chunk " + x + ", " + z + " from file " + file + " (message: \"" + e.getMessage() + "\"); skipping chunk" + EOL);
                                    logger.error("Class cast exception while reading chunk " + x + ", " + z + " from file " + file + "; skipping chunk", e);
                                    continue;
                                }

                                if ((chunk instanceof MC113AnvilChunk) && (((MC113AnvilChunk) chunk).getStatus() == MC113AnvilChunk.Status.EMPTY)) {
                                    if (logger.isDebugEnabled()) {
                                        logger.debug("Skipping \"empty\" chunk at {},{}", chunk.getxPos(), chunk.getzPos());
                                    }
                                    continue;
                                }

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
                                                Material material = chunk.getMaterial(xx, y, zz);
                                                if (!material.natural) {
                                                    if (height == -1.0f) {
                                                        manMadeStructuresAboveGround = true;
                                                    } else {
                                                        manMadeStructuresBelowGround = true;
                                                    }
                                                    manMadeBlockTypes.add(material.name);
                                                }
                                                String name = material.name;
                                                if ((name == MC_SNOW) || (name == MC_ICE)) {
                                                    frost = true;
                                                }
                                                if ((waterLevel == 0) && ((name == MC_ICE) || (name == MC_FROSTED_ICE) || (name == MC_BUBBLE_COLUMN) || (((name == MC_WATER) || (name == MC_LAVA)) && (material.getProperty(LEVEL) == 0)) || material.is(WATERLOGGED))) {
                                                    waterLevel = y;
                                                    if (name == MC_LAVA) {
                                                        floodWithLava = true;
                                                    }
                                                } else if (height == -1.0f) {
                                                    if (TERRAIN_MAPPING.containsKey(name)) {
                                                        // Terrain found
                                                        height = y - 0.4375f; // Value that falls in the middle of the lowest one eighth which will still round to the same integer value and will receive a one layer thick smooth snow block (principle of least surprise)
                                                        terrain = TERRAIN_MAPPING.get(name);
                                                    }
                                                }
                                            }
                                            // Use smooth snow, if present, to better approximate world height, so smooth snow will survive merge
                                            final int intHeight = (int) (height + 0.5f);
                                            if ((height != -1.0f) && (intHeight < maxY)) {
                                                Material materialAbove = chunk.getMaterial(xx, intHeight + 1, zz);
                                                if (materialAbove.isNamed(MC_SNOW)) {
                                                    int layers = materialAbove.getProperty(LAYERS);
                                                    height += layers * 0.125;
                                                }
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
                                                if (((biome > HIGHEST_BIOME_ID) || (BIOME_NAMES[biome] == null)) && (biome != 255)) {
                                                    unknownBiomes.add(biome);
                                                }
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
        
        System.err.println("Man-made block types encountered:");
        manMadeBlockTypes.forEach(System.err::println);
        System.err.println("Unknown biome IDs encountered:");
        unknownBiomes.forEach(System.err::println);
        
        return reportBuilder.length() != 0 ? reportBuilder.toString() : null;
    }
    
    private final TileFactory tileFactory;
    private final File levelDatFile;
    private final boolean populateNewChunks;
    private final Set<Point> chunksToSkip;
    private final ReadOnlyOption readOnlyOption;
    private final Set<Integer> dimensionsToImport;
    private String warnings;
    
    public static final Map<String, Terrain> TERRAIN_MAPPING = new HashMap<>();

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(JavaMapImporter.class);
    private static final String EOL = System.getProperty("line.separator");
    
    static {
        TERRAIN_MAPPING.put(MC_STONE, Terrain.STONE);
        TERRAIN_MAPPING.put(MC_ANDESITE, Terrain.STONE);
        TERRAIN_MAPPING.put(MC_DIORITE, Terrain.STONE);
        TERRAIN_MAPPING.put(MC_GRANITE, Terrain.STONE);
        TERRAIN_MAPPING.put(MC_GRASS_BLOCK, Terrain.BARE_GRASS);
        TERRAIN_MAPPING.put(MC_DIRT, Terrain.DIRT);
        TERRAIN_MAPPING.put(MC_COARSE_DIRT, Terrain.PERMADIRT);
        TERRAIN_MAPPING.put(MC_PODZOL, Terrain.PODZOL);
        TERRAIN_MAPPING.put(MC_FARMLAND, Terrain.DIRT);
        TERRAIN_MAPPING.put(MC_BEDROCK, Terrain.BEDROCK);
        TERRAIN_MAPPING.put(MC_SAND, Terrain.SAND);
        TERRAIN_MAPPING.put(MC_RED_SAND, Terrain.RED_SAND);
        TERRAIN_MAPPING.put(MC_GRAVEL, Terrain.GRAVEL);
        TERRAIN_MAPPING.put(MC_GOLD_ORE, Terrain.STONE);
        TERRAIN_MAPPING.put(MC_IRON_ORE, Terrain.STONE);
        TERRAIN_MAPPING.put(MC_COAL_ORE, Terrain.STONE);
        TERRAIN_MAPPING.put(MC_LAPIS_ORE, Terrain.STONE);
        TERRAIN_MAPPING.put(MC_DIAMOND_ORE, Terrain.STONE);
        TERRAIN_MAPPING.put(MC_REDSTONE_ORE, Terrain.STONE);
        TERRAIN_MAPPING.put(MC_INFESTED_STONE, Terrain.STONE);
        TERRAIN_MAPPING.put(MC_SANDSTONE, Terrain.SANDSTONE);
        TERRAIN_MAPPING.put(MC_RED_SANDSTONE, Terrain.RED_SANDSTONE);
        TERRAIN_MAPPING.put(MC_OBSIDIAN, Terrain.OBSIDIAN);
        TERRAIN_MAPPING.put(MC_SNOW_BLOCK, Terrain.DEEP_SNOW);
        TERRAIN_MAPPING.put(MC_CLAY, Terrain.CLAY);
        TERRAIN_MAPPING.put(MC_NETHERRACK, Terrain.NETHERRACK);
        TERRAIN_MAPPING.put(MC_NETHER_QUARTZ_ORE, Terrain.NETHERRACK);
        TERRAIN_MAPPING.put(MC_SOUL_SAND, Terrain.SOUL_SAND);
        TERRAIN_MAPPING.put(MC_MYCELIUM, Terrain.MYCELIUM);
        TERRAIN_MAPPING.put(MC_END_STONE, Terrain.END_STONE);
        TERRAIN_MAPPING.put(MC_TERRACOTTA, Terrain.HARDENED_CLAY);
        TERRAIN_MAPPING.put(MC_GRASS_PATH, Terrain.GRASS_PATH);
        TERRAIN_MAPPING.put(MC_MAGMA_BLOCK, Terrain.MAGMA); // TODO: or should this be mapped to stone and magma added to the Resources layer?
        TERRAIN_MAPPING.put(MC_WHITE_TERRACOTTA, Terrain.WHITE_STAINED_CLAY);
        TERRAIN_MAPPING.put(MC_ORANGE_TERRACOTTA, Terrain.ORANGE_STAINED_CLAY);
        TERRAIN_MAPPING.put(MC_MAGENTA_TERRACOTTA, Terrain.MAGENTA_STAINED_CLAY);
        TERRAIN_MAPPING.put(MC_LIGHT_BLUE_TERRACOTTA, Terrain.LIGHT_BLUE_STAINED_CLAY);
        TERRAIN_MAPPING.put(MC_YELLOW_TERRACOTTA, Terrain.YELLOW_STAINED_CLAY);
        TERRAIN_MAPPING.put(MC_LIME_TERRACOTTA, Terrain.LIME_STAINED_CLAY);
        TERRAIN_MAPPING.put(MC_PINK_TERRACOTTA, Terrain.PINK_STAINED_CLAY);
        TERRAIN_MAPPING.put(MC_GRAY_TERRACOTTA, Terrain.GREY_STAINED_CLAY);
        TERRAIN_MAPPING.put(MC_LIGHT_GRAY_TERRACOTTA, Terrain.LIGHT_GREY_STAINED_CLAY);
        TERRAIN_MAPPING.put(MC_CYAN_TERRACOTTA, Terrain.CYAN_STAINED_CLAY);
        TERRAIN_MAPPING.put(MC_PURPLE_TERRACOTTA, Terrain.PURPLE_STAINED_CLAY);
        TERRAIN_MAPPING.put(MC_BLUE_TERRACOTTA, Terrain.BLUE_STAINED_CLAY);
        TERRAIN_MAPPING.put(MC_BROWN_TERRACOTTA, Terrain.BROWN_STAINED_CLAY);
        TERRAIN_MAPPING.put(MC_GREEN_TERRACOTTA, Terrain.GREEN_STAINED_CLAY);
        TERRAIN_MAPPING.put(MC_RED_TERRACOTTA, Terrain.RED_STAINED_CLAY);
        TERRAIN_MAPPING.put(MC_BLACK_TERRACOTTA, Terrain.BLACK_STAINED_CLAY);
    }

    static {
        TERRAIN_MAPPING.forEach((name, terrain) -> {
            if (! Material.getDefault(name).terrain) {
                throw new IllegalStateException("Material named \"" + name + "\" not marked as terrain");
            }
        });
        Material.getAllMaterials().forEach(material -> {
            if (material.terrain && (material.namespace != LEGACY) && (! TERRAIN_MAPPING.containsKey(material.name))) {
                // TODOMC13 once this is fixed, turn this into an exception:
                System.err.printf("Material \"%s\" missing from terrain mapping%n", material);
            }
        });
    }
}