/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.worldpainter.importing;

import org.pepsoft.minecraft.MapGenerator;
import org.pepsoft.util.MathUtils;
import org.pepsoft.util.PerlinNoise;
import org.pepsoft.util.ProgressReceiver;
import org.pepsoft.util.swing.TileProvider;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.*;
import org.pepsoft.worldpainter.heightMaps.BitmapHeightMap;
import org.pepsoft.worldpainter.heightMaps.TransformingHeightMap;
import org.pepsoft.worldpainter.history.HistoryEntry;
import org.pepsoft.worldpainter.layers.Frost;
import org.pepsoft.worldpainter.layers.Layer;
import org.pepsoft.worldpainter.layers.Resources;
import org.pepsoft.worldpainter.layers.Void;
import org.pepsoft.worldpainter.layers.exporters.ExporterSettings;
import org.pepsoft.worldpainter.layers.exporters.FrostExporter;
import org.pepsoft.worldpainter.layers.exporters.ResourcesExporter;
import org.pepsoft.worldpainter.themes.Theme;

import java.awt.*;
import java.io.File;
import java.util.Map;

import static org.pepsoft.minecraft.Constants.DEFAULT_MAX_HEIGHT_ANVIL;
import static org.pepsoft.minecraft.Constants.DEFAULT_WATER_LEVEL;
import static org.pepsoft.worldpainter.Constants.*;

/**
 *
 * @author SchmitzP
 */
public class HeightMapImporter {
    /**
     * Create a new WorldPainter world from the configured height map and import
     * settings.
     * 
     * @param progressReceiver The progress receiver to report progress to and
     *     check for cancellation with.
     * @return A new WorldPainter world based on the specified height map.
     * @throws org.pepsoft.util.ProgressReceiver.OperationCancelled If and when
     *     the specified progress received throws it (when the user cancels the
     *     operation).
     */
    public World2 importToNewWorld(ProgressReceiver progressReceiver) throws ProgressReceiver.OperationCancelled {
        Rectangle extent = heightMap.getExtent();
        logger.info("Importing world from height map {} (size: {}x{})", name, extent.width, extent.height);

        calculateFlags();
        final World2 world = new World2(platform, minecraftSeed, tileFactory, maxHeight);
        world.addHistoryEntry(HistoryEntry.WORLD_IMPORTED_FROM_HEIGHT_MAP, imageFile);
        world.setName(name);
        final Dimension dimension = world.getDimension(DIM_NORMAL);

        // Export settings
        final Configuration config = Configuration.getInstance();
        world.setCreateGoodiesChest(config.isDefaultCreateGoodiesChest());
        world.setMapFeatures(config.isDefaultMapFeatures());
        world.setGameType(config.getDefaultGameType());
        world.setAllowCheats(config.isDefaultAllowCheats());
        
        // Turn off smooth snow, except for high res imports
        if (! highRes) {
            FrostExporter.FrostSettings frostSettings = new FrostExporter.FrostSettings();
            frostSettings.setMode(FrostExporter.FrostSettings.MODE_FLAT);
            dimension.setLayerSettings(Frost.INSTANCE, frostSettings);
        }

        importToDimension(dimension, true, progressReceiver);

        MapGenerator generator = config.getDefaultGenerator();
        dimension.setGenerator(generator);
        Dimension defaults = config.getDefaultTerrainAndLayerSettings();
        dimension.setBorder(defaults.getBorder());
        dimension.setBorderSize(defaults.getBorderSize());
        dimension.setBorderLevel(worldWaterLevel);
        dimension.setBedrockWall(defaults.isBedrockWall());
        dimension.setSubsurfaceMaterial(defaults.getSubsurfaceMaterial());
        dimension.setPopulate(defaults.isPopulate());
        dimension.setTopLayerMinDepth(defaults.getTopLayerMinDepth());
        dimension.setTopLayerVariation(defaults.getTopLayerVariation());
        dimension.setBottomless(defaults.isBottomless());
        for (Map.Entry<Layer, ExporterSettings> entry: defaults.getAllLayerSettings().entrySet()) {
            dimension.setLayerSettings(entry.getKey(), entry.getValue().clone());
        }
        ((ResourcesExporter.ResourcesExporterSettings) dimension.getLayerSettings(Resources.INSTANCE)).setMinimumLevel(config.getDefaultResourcesMinimumLevel());

        dimension.setGridEnabled(config.isDefaultGridEnabled());
        dimension.setGridSize(config.getDefaultGridSize());
        dimension.setContoursEnabled(config.isDefaultContoursEnabled());
        dimension.setContourSeparation(config.getDefaultContourSeparation());
        world.setSpawnPoint(new Point(extent.x + extent.width / 2, extent.y + extent.height / 2));
        dimension.setLastViewPosition(world.getSpawnPoint());

        return world;
    }

    public void importToDimension(Dimension dimension, boolean createTiles, ProgressReceiver progressReceiver) throws ProgressReceiver.OperationCancelled {
        // Sanity checks
        if (dimension.getMaxHeight() != maxHeight) {
            throw new IllegalArgumentException(String.format("Dimension has different maxHeight (%d) than configured (%d)", dimension.getMaxHeight(), maxHeight));
        }

        if (dimension.getWorld() != null) {
            dimension.getWorld().addHistoryEntry(HistoryEntry.WORLD_HEIGHT_MAP_IMPORTED_TO_DIMENSION, dimension.getName(), imageFile);
        }

        final boolean useVoidBelow = voidBelowLevel > platform.minZ;
        final Rectangle extent = heightMap.getExtent();
        final int x1 = extent.x;
        final int x2 = extent.x + extent.width - 1;
        final int y1 = extent.y;
        final int y2 = extent.y + extent.height - 1;
        calculateFlags();
        final int tileX1 = extentInTiles.x;
        final int tileY1 = extentInTiles.y;
        final int tileX2 = extentInTiles.x + extentInTiles.width - 1;
        final int tileY2 = extentInTiles.y + extentInTiles.height - 1;
        final int totalTileCount = extentInTiles.width * extentInTiles.height;
        final int floor = Math.max(worldWaterLevel - 20, platform.minZ);
        final int variation = Math.min(15, (worldWaterLevel - floor) / 2);
        final PerlinNoise noiseGenerator = new PerlinNoise(0);
        noiseGenerator.setSeed(dimension.getSeed());
        int tileCount = 0;
        for (int tileX = tileX1; tileX <= tileX2; tileX++) {
            for (int tileY = tileY1; tileY <= tileY2; tileY++) {
                boolean tileIsNew;
                Tile tile = dimension.getTileForEditing(tileX, tileY);
                if (tile == null) {
                    if (createTiles) {
                        tile = tileFactory.createTile(tileX, tileY);
                        tileIsNew = true;
                    } else {
                        tileCount++;
                        if (progressReceiver != null) {
                            progressReceiver.setProgress((float) tileCount / totalTileCount);
                        }
                        continue;
                    }
                } else {
                    tileIsNew = false;
                    tile.inhibitEvents();
                }
                final int xOffset = tileX << TILE_SIZE_BITS;
                final int yOffset = tileY << TILE_SIZE_BITS;
                for (int x = 0; x < TILE_SIZE; x++) {
                    for (int y = 0; y < TILE_SIZE; y++) {
                        final int imageX = xOffset + x;
                        final int imageY = yOffset + y;
                        if ((imageX >= x1) && (imageX <= x2) && (imageY >= y1) && (imageY <= y2)) {
                            final float imageLevel = heightMap.getHeight(imageX, imageY);
                            final float height = calculateHeight(imageLevel);
                            if (onlyRaise && (! tileIsNew)) {
                                if (height > tile.getHeight(x, y)) {
                                    tile.setHeight(x, y, height);
                                    if (theme != null) {
                                        theme.apply(tile, x, y);
                                    }
                                }
                            } else {
                                tile.setHeight(x, y, height);
                                tile.setWaterLevel(x, y, worldWaterLevel);
                                if (useVoidBelow && (imageLevel < voidBelowLevel)) {
                                    tile.setBitLayerValue(org.pepsoft.worldpainter.layers.Void.INSTANCE, x, y, true);
                                }
                                if (theme != null) {
                                    theme.apply(tile, x, y);
                                }
                            }
                        } else if (tileIsNew) {
                            tile.setHeight(x, y, floor + (noiseGenerator.getPerlinNoise(imageX / MEDIUM_BLOBS, imageY / MEDIUM_BLOBS) + 0.5f) * variation);
                            tile.setTerrain(x, y, Terrain.BEACHES);
                            tile.setWaterLevel(x, y, worldWaterLevel);
                            if (useVoidBelow) {
                                tile.setBitLayerValue(org.pepsoft.worldpainter.layers.Void.INSTANCE, x, y, true);
                            }
                        }
                    }
                }
                if (tileIsNew) {
                    dimension.addTile(tile);
                } else {
                    tile.releaseEvents();
                }
                tileCount++;
                if (progressReceiver != null) {
                    progressReceiver.setProgress((float) tileCount / totalTileCount);
                }
            }
        }
    }

    public TileProvider getPreviewProvider(ColourScheme colourScheme, boolean contourLines, int contourSeparation, TileRenderer.LightOrigin lightOrigin) {
        if (tileFactory instanceof HeightMapTileFactory) {
            calculateFlags();
            HeightMap previewHeightMap;
            if (highRes) {
                previewHeightMap = heightMap.minus(imageLowLevel).times(levelScale).plus(worldLowLevel);
            } else {
                if (oneOnOne) {
                    if (mayBeScaled) {
                        previewHeightMap = heightMap;
                    } else {
                        previewHeightMap = heightMap.minus(0.4375f);
                    }
                } else {
                    previewHeightMap = heightMap.minus(imageLowLevel).times(levelScale).plus(worldLowLevel);
                }
            }

            HeightMapTileFactory heightMapTileFactory = (HeightMapTileFactory) this.tileFactory;
            Theme theme = ((this.theme != null) ? this.theme : heightMapTileFactory.getTheme()).clone();
            theme.setWaterHeight(worldWaterLevel);
            HeightMapTileFactory tileFactory = new PreviewTileFactory(1L, previewHeightMap, platform.minZ, maxHeight, heightMapTileFactory.isFloodWithLava(), theme, heightMap, voidBelowLevel);
            return new WPTileProvider(tileFactory, colourScheme, null, null, contourLines, contourSeparation, lightOrigin, false, null);
        } else {
            return null;
        }
    }

    // Properties

    public Platform getPlatform() {
        return platform;
    }

    public void setPlatform(Platform platform) {
        this.platform = platform;
    }

    public HeightMap getHeightMap() {
        return heightMap;
    }

    public void setHeightMap(HeightMap heightMap) {
        if ((heightMap != null) && (heightMap.getExtent() == null)) {
            throw new IllegalArgumentException("Height map must have an extent");
        }
        this.heightMap = heightMap;
    }

    public int getWorldLowLevel() {
        return worldLowLevel;
    }

    public void setWorldLowLevel(int worldLowLevel) {
        this.worldLowLevel = worldLowLevel;
    }

    public int getWorldWaterLevel() {
        return worldWaterLevel;
    }

    public void setWorldWaterLevel(int worldWaterLevel) {
        this.worldWaterLevel = worldWaterLevel;
    }

    public int getWorldHighLevel() {
        return worldHighLevel;
    }

    public void setWorldHighLevel(int worldHighLevel) {
        this.worldHighLevel = worldHighLevel;
    }

    public long getImageLowLevel() {
        return imageLowLevel;
    }

    public void setImageLowLevel(long imageLowLevel) {
        this.imageLowLevel = imageLowLevel;
    }

    public long getImageHighLevel() {
        return imageHighLevel;
    }

    public void setImageHighLevel(long imageHighLevel) {
        this.imageHighLevel = imageHighLevel;
    }

    public int getMaxHeight() {
        return maxHeight;
    }

    public void setMaxHeight(int maxHeight) {
        this.maxHeight = maxHeight;
    }

    public long getVoidBelowLevel() {
        return voidBelowLevel;
    }

    public void setVoidBelowLevel(long voidBelowLevel) {
        this.voidBelowLevel = voidBelowLevel;
    }

    public TileFactory getTileFactory() {
        return tileFactory;
    }

    public void setTileFactory(TileFactory tileFactory) {
        this.tileFactory = tileFactory;
        if ((tileFactory instanceof HeightMapTileFactory) && (theme == null)){
            setTheme(((HeightMapTileFactory) tileFactory).getTheme());
        }
    }

    public Theme getTheme() {
        return theme;
    }

    public void setTheme(Theme theme) {
        this.theme = theme;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public File getImageFile() {
        return imageFile;
    }

    public void setImageFile(File imageFile) {
        this.imageFile = imageFile;
    }

    public boolean isOnlyRaise() {
        return onlyRaise;
    }

    public void setOnlyRaise(boolean onlyRaise) {
        this.onlyRaise = onlyRaise;
    }

    public long getMinecraftSeed() {
        return minecraftSeed;
    }

    public void setMinecraftSeed(long minecraftSeed) {
        this.minecraftSeed = minecraftSeed;
    }

    private void calculateFlags() {
        // If the height map is a bitmap height map, or a transforming height map with a scale of 100% and based on a
        // bitmap height map, then it is definitely unscaled, meaning we can apply a delta to the bitmap values to make
        // each block height 1/8 higher, in order to make smooth snow work less surprisingly
        mayBeScaled = ! ((heightMap instanceof BitmapHeightMap)
                || ((heightMap instanceof TransformingHeightMap)
                    && (((TransformingHeightMap) heightMap).getScaleX() == 100)
                    && (((TransformingHeightMap) heightMap).getScaleY() == 100)
                    && (((TransformingHeightMap) heightMap).getBaseHeightMap() instanceof BitmapHeightMap)));
        oneOnOne = (worldLowLevel == imageLowLevel) && (worldHighLevel == imageHighLevel);
        highRes = (imageHighLevel >= maxHeight) && (worldHighLevel < maxHeight);
        levelScale = (float) (worldHighLevel - worldLowLevel) / (imageHighLevel - imageLowLevel);
        maxZ = maxHeight - 1;

        Rectangle extent = heightMap.getExtent();
        int tileX1 = extent.x >> TILE_SIZE_BITS;
        int tileY1 = extent.y >> TILE_SIZE_BITS;
        int tileX2 = (extent.x + extent.width - 1) >> TILE_SIZE_BITS;
        int tileY2 = (extent.y + extent.height - 1) >> TILE_SIZE_BITS;
        extentInTiles = new Rectangle(tileX1, tileY1, tileX2 - tileX1 + 1, tileY2 - tileY1 + 1);
    }

    private float calculateHeight(final float imageLevel) {
        if (highRes) {
            return MathUtils.clamp(platform.minZ, (imageLevel - imageLowLevel) * levelScale + worldLowLevel, maxZ);
        } else {
            return MathUtils.clamp(platform.minZ, oneOnOne
                ? (mayBeScaled ? imageLevel : (imageLevel - 0.4375f))
                : ((imageLevel - imageLowLevel) * levelScale + worldLowLevel), maxZ);
        }
    }

    private Platform platform = Configuration.getInstance().getDefaultPlatform();
    private HeightMap heightMap;
    private int worldLowLevel, worldWaterLevel = DEFAULT_WATER_LEVEL, worldHighLevel = DEFAULT_MAX_HEIGHT_ANVIL - 1, maxHeight = DEFAULT_MAX_HEIGHT_ANVIL, maxZ;
    private long imageLowLevel, imageHighLevel = DEFAULT_MAX_HEIGHT_ANVIL - 1, voidBelowLevel;
    private TileFactory tileFactory;
    private Theme theme;
    private String name;
    private boolean onlyRaise, oneOnOne, highRes, mayBeScaled;
    private File imageFile;
    private float levelScale;
    private long minecraftSeed = World2.DEFAULT_OCEAN_SEED;
    private Rectangle extentInTiles;

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(HeightMapImporter.class);

    private static class PreviewTileFactory extends HeightMapTileFactory {
        private PreviewTileFactory(long seed, HeightMap heightMap, int minHeight, int maxHeight, boolean floodWithLava, Theme theme, HeightMap imageHeightMap, long voidBelow) {
            super(seed, heightMap, minHeight, maxHeight, floodWithLava, theme);
            this.imageHeightMap = imageHeightMap;
            this.voidBelow = voidBelow;
        }

        @Override
        public Tile createTile(int tileX, int tileY) {
            final Tile tile = super.createTile(tileX, tileY);
            tile.inhibitEvents();
            final int worldTileX = tileX * TILE_SIZE, worldTileY = tileY * TILE_SIZE;
            try {
                for (int x = 0; x < TILE_SIZE; x++) {
                    for (int y = 0; y < TILE_SIZE; y++) {
                        final int blockX = worldTileX + x, blockY = worldTileY + y;
                        if (imageHeightMap.getHeight(blockX, blockY) < voidBelow) {
                            tile.setBitLayerValue(Void.INSTANCE, x, y, true);
                        }
                    }
                }
                return tile;
            } finally {
                tile.releaseEvents();
            }
        }

        private final HeightMap imageHeightMap;
        private final long voidBelow;
    }
}