/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.worldpainter;

import org.pepsoft.minecraft.MapGenerator;
import org.pepsoft.minecraft.Material;
import org.pepsoft.minecraft.SeededGenerator;
import org.pepsoft.util.AttributeKey;
import org.pepsoft.util.FileUtils;
import org.pepsoft.util.SystemUtils;
import org.pepsoft.util.swing.TiledImageViewer;
import org.pepsoft.worldpainter.Dimension.Border;
import org.pepsoft.worldpainter.TileRenderer.LightOrigin;
import org.pepsoft.worldpainter.exporting.ExportSettings;
import org.pepsoft.worldpainter.layers.CustomLayer;
import org.pepsoft.worldpainter.layers.Frost;
import org.pepsoft.worldpainter.layers.Layer;
import org.pepsoft.worldpainter.layers.Resources;
import org.pepsoft.worldpainter.layers.exporters.FrostExporter.FrostSettings;
import org.pepsoft.worldpainter.themes.Filter;
import org.pepsoft.worldpainter.themes.HeightFilter;
import org.pepsoft.worldpainter.themes.SimpleTheme;
import org.pepsoft.worldpainter.themes.Theme;
import org.pepsoft.worldpainter.util.MinecraftJarProvider;
import org.pepsoft.worldpainter.vo.EventVO;

import java.awt.*;
import java.io.*;
import java.util.List;
import java.util.*;

import static org.pepsoft.minecraft.Constants.DEFAULT_MAX_HEIGHT_ANVIL;
import static org.pepsoft.minecraft.Constants.DEFAULT_WATER_LEVEL;
import static org.pepsoft.minecraft.Material.DIRT;
import static org.pepsoft.worldpainter.Constants.DIM_NORMAL;
import static org.pepsoft.worldpainter.DefaultPlugin.JAVA_ANVIL;
import static org.pepsoft.worldpainter.DefaultPlugin.JAVA_ANVIL_1_15;
import static org.pepsoft.worldpainter.Generator.DEFAULT;
import static org.pepsoft.worldpainter.Generator.LARGE_BIOMES;
import static org.pepsoft.worldpainter.Terrain.ROCK;
import static org.pepsoft.worldpainter.Terrain.STONE_MIX;
import static org.pepsoft.worldpainter.World2.DEFAULT_OCEAN_SEED;

/**
 *
 * @author pepijn
 */
public final class Configuration implements Serializable, EventLogger, MinecraftJarProvider {
    public Configuration() {
        if (logger.isDebugEnabled()) {
            logger.debug("Creating new configuration");
        }
    }

    public UUID getUuid() {
        return uuid;
    }
    
    public synchronized boolean isHilly() {
        return hilly;
    }

    public synchronized void setHilly(boolean hilly) {
        this.hilly = hilly;
    }

    public synchronized boolean isLava() {
        return lava;
    }

    public synchronized void setLava(boolean lava) {
        this.lava = lava;
    }

    public synchronized int getLevel() {
        return level;
    }

    public synchronized void setLevel(int level) {
        this.level = level;
    }

    public synchronized boolean isMaximised() {
        return maximised;
    }

    public synchronized void setMaximised(boolean maximised) {
        this.maximised = maximised;
    }

    public synchronized Terrain getSurface() {
        return surface;
    }

    public synchronized void setSurface(Terrain surface) {
        this.surface = surface;
    }

    public synchronized int getWaterLevel() {
        return waterLevel;
    }

    public synchronized void setWaterLevel(int waterLevel) {
        this.waterLevel = waterLevel;
    }

    public synchronized Rectangle getWindowBounds() {
        return windowBounds;
    }

    public synchronized void setWindowBounds(Rectangle windowBounds) {
        this.windowBounds = windowBounds;
    }

    public synchronized File getExportDirectory(Platform platform) {
        return exportDirectoriesById.get(platform.id);
    }

    public synchronized void setExportDirectory(Platform platform, File exportDirectory) {
        exportDirectoriesById.put(platform.id, exportDirectory);
    }

    public synchronized File getSavesDirectory() {
        return savesDirectory;
    }

    public synchronized void setSavesDirectory(File savesDirectory) {
        this.savesDirectory = savesDirectory;
    }

    public synchronized File getWorldDirectory() {
        return worldDirectory;
    }

    public synchronized void setWorldDirectory(File worldDirectory) {
        this.worldDirectory = worldDirectory;
    }

    public synchronized Border getBorder() {
        return border2;
    }

    public synchronized void setBorder(Border border) {
        this.border2 = border;
    }

    public synchronized boolean isGoodies() {
        return goodies;
    }

    public synchronized void setGoodies(boolean goodies) {
        this.goodies = goodies;
    }

    public synchronized boolean isPopulate() {
        return populate;
    }

    public synchronized void setPopulate(boolean populate) {
        this.populate = populate;
    }

    public synchronized Terrain getUnderground() {
        return underground;
    }

    public synchronized void setUnderground(Terrain underground) {
        this.underground = underground;
    }

    public synchronized int getBorderLevel() {
        return borderLevel;
    }

    public synchronized void setBorderLevel(int borderLevel) {
        this.borderLevel = borderLevel;
    }

    public synchronized boolean isBeaches() {
        return beaches;
    }

    public synchronized void setBeaches(boolean beaches) {
        this.beaches = beaches;
    }

    public synchronized boolean isMergeWarningDisplayed() {
        return mergeWarningDisplayed;
    }

    public synchronized void setMergeWarningDisplayed(boolean mergeWarningDisplayed) {
        this.mergeWarningDisplayed = mergeWarningDisplayed;
    }

    public synchronized boolean isImportWarningDisplayed() {
        return importWarningDisplayed;
    }

    public synchronized void setImportWarningDisplayed(boolean importWarningDisplayed) {
        this.importWarningDisplayed = importWarningDisplayed;
    }

    public synchronized Boolean getPingAllowed() {
        return pingAllowed;
    }

    public synchronized void setPingAllowed(Boolean pingAllowed) {
        this.pingAllowed = pingAllowed;
        if (Boolean.TRUE.equals(pingAllowed) && (eventLog == null)) {
            eventLog = new LinkedList<>();
        } else if (Boolean.FALSE.equals(pingAllowed)) {
            eventLog = null;
        }
    }

    public synchronized Map<Integer, File> getMinecraftJars() {
        return minecraftJars;
    }
    
    @Override
    public synchronized File getMinecraftJar(int biomeAlgorithm) {
        return minecraftJars.get(biomeAlgorithm);
    }
    
    public synchronized void setMinecraftJar(int biomeAlgorithm, File minecraftJar) {
        if (minecraftJar != null) {
            minecraftJars.put(biomeAlgorithm, minecraftJar);
        } else {
            minecraftJars.remove(biomeAlgorithm);
        }
    }
    
    public synchronized void setMinecraftJars(Map<Integer, File> minecraftJars) {
        if (minecraftJars == null) {
            throw new NullPointerException();
        }
        this.minecraftJars = minecraftJars;
    }

    public synchronized DonationStatus getDonationStatus() {
        return donationStatus;
    }

    public synchronized void setDonationStatus(DonationStatus donationStatus) {
        this.donationStatus = donationStatus;
    }

    public synchronized int getLaunchCount() {
        return launchCount;
    }

    public synchronized void setLaunchCount(int launchCount) {
        this.launchCount = launchCount;
    }

    public synchronized File getCustomObjectsDirectory() {
        return customObjectsDirectory;
    }

    public synchronized void setCustomObjectsDirectory(File customObjectsDirectory) {
        this.customObjectsDirectory = customObjectsDirectory;
    }

    public synchronized boolean isCheckForUpdates() {
        return checkForUpdates;
    }

    public synchronized void setCheckForUpdates(boolean checkForUpdates) {
        this.checkForUpdates = checkForUpdates;
    }

    public synchronized int getDefaultContourSeparation() {
        return defaultContourSeparation;
    }

    public synchronized void setDefaultContourSeparation(int defaultContourSeparation) {
        this.defaultContourSeparation = defaultContourSeparation;
    }

    public synchronized boolean isDefaultContoursEnabled() {
        return defaultContoursEnabled;
    }

    public synchronized void setDefaultContoursEnabled(boolean defaultContoursEnabled) {
        this.defaultContoursEnabled = defaultContoursEnabled;
    }

    public synchronized boolean isDefaultGridEnabled() {
        return defaultGridEnabled;
    }

    public synchronized void setDefaultGridEnabled(boolean defaultGridEnabled) {
        this.defaultGridEnabled = defaultGridEnabled;
    }

    public synchronized int getDefaultGridSize() {
        return defaultGridSize;
    }

    public synchronized void setDefaultGridSize(int defaultGridSize) {
        this.defaultGridSize = defaultGridSize;
    }

    public synchronized int getDefaultHeight() {
        return (defaultHeight != CIRCULAR_WORLD) ? defaultHeight : defaultWidth;
    }

    public synchronized void setDefaultHeight(int defaultHeight) {
        if (this.defaultHeight != CIRCULAR_WORLD) {
            this.defaultHeight = defaultHeight;
        }
    }

    public synchronized int getDefaultMaxHeight() {
        return defaultMaxHeight;
    }

    public synchronized void setDefaultMaxHeight(int defaultMaxHeight) {
        this.defaultMaxHeight = defaultMaxHeight;
    }

    public synchronized boolean isDefaultViewDistanceEnabled() {
        return defaultViewDistanceEnabled;
    }

    public synchronized void setDefaultViewDistanceEnabled(boolean defaultViewDistanceEnabled) {
        this.defaultViewDistanceEnabled = defaultViewDistanceEnabled;
    }

    public synchronized boolean isDefaultWalkingDistanceEnabled() {
        return defaultWalkingDistanceEnabled;
    }

    public synchronized void setDefaultWalkingDistanceEnabled(boolean defaultWalkingDistanceEnabled) {
        this.defaultWalkingDistanceEnabled = defaultWalkingDistanceEnabled;
    }

    public synchronized int getDefaultWidth() {
        return defaultWidth;
    }

    public synchronized void setDefaultWidth(int defaultWidth) {
        this.defaultWidth = defaultWidth;
    }

    public synchronized boolean isUndoEnabled() {
        return undoEnabled;
    }

    public synchronized void setUndoEnabled(boolean undoEnabled) {
        this.undoEnabled = undoEnabled;
    }

    public synchronized int getUndoLevels() {
        return undoLevels;
    }

    public synchronized void setUndoLevels(int undoLevels) {
        this.undoLevels = undoLevels;
    }

    public synchronized Dimension getDefaultTerrainAndLayerSettings() {
        return defaultTerrainAndLayerSettings;
    }

    public synchronized void setDefaultTerrainAndLayerSettings(Dimension defaultTerrainAndLayerSettings) {
        if (defaultTerrainAndLayerSettings.getLayerSettings(Resources.INSTANCE) != null) {
            defaultTerrainAndLayerSettings.setLayerSettings(Resources.INSTANCE, null);
        }
        this.defaultTerrainAndLayerSettings = defaultTerrainAndLayerSettings;
    }

    public synchronized boolean isToolbarsLocked() {
        return toolbarsLocked;
    }

    public synchronized void setToolbarsLocked(boolean toolbarsLocked) {
        this.toolbarsLocked = toolbarsLocked;
    }

    public synchronized int getWorldFileBackups() {
        return worldFileBackups;
    }

    public synchronized void setWorldFileBackups(int worldFileBackups) {
        this.worldFileBackups = worldFileBackups;
    }

    public synchronized float getDefaultRange() {
        return defaultRange;
    }

    public synchronized void setDefaultRange(float defaultRange) {
        this.defaultRange = defaultRange;
    }

    public synchronized double getDefaultScale() {
        return defaultScale;
    }

    public synchronized void setDefaultScale(double defaultScale) {
        this.defaultScale = defaultScale;
    }

    public synchronized LightOrigin getDefaultLightOrigin() {
        return defaultLightOrigin;
    }

    public synchronized void setDefaultLightOrigin(LightOrigin defaultLightOrigin) {
        this.defaultLightOrigin = defaultLightOrigin;
    }
    
    public synchronized boolean isDefaultCircularWorld() {
        return defaultHeight == CIRCULAR_WORLD;
    }
    
    public synchronized void setDefaultCircularWorld(boolean defaultCircularWorld) {
        if (defaultCircularWorld && (defaultHeight != CIRCULAR_WORLD)) {
            defaultHeight = CIRCULAR_WORLD;
        } else if ((! defaultCircularWorld) && (defaultHeight == CIRCULAR_WORLD)) {
            defaultHeight = defaultWidth;
        }
    }

    public synchronized int getMaximumBrushSize() {
        return maximumBrushSize;
    }

    public synchronized void setMaximumBrushSize(int maximumBrushSize) {
        this.maximumBrushSize = maximumBrushSize;
    }

    public synchronized List<CustomLayer> getCustomLayers() {
        return customLayers;
    }

    public synchronized List<MixedMaterial> getMixedMaterials() {
        return mixedMaterials;
    }

    public synchronized boolean isEasyMode() {
        return false;
//        return easyMode;
    }

//    public synchronized void setEasyMode(final boolean easyMode) {
//        this.easyMode = easyMode;
//    }

    public synchronized boolean isDefaultExtendedBlockIds() {
        return defaultExtendedBlockIds;
    }

    public synchronized void setDefaultExtendedBlockIds(boolean defaultExtendedBlockIds) {
        this.defaultExtendedBlockIds = defaultExtendedBlockIds;
    }

    public synchronized File getLayerDirectory() {
        return layerDirectory;
    }

    public synchronized void setLayerDirectory(File layerDirectory) {
        this.layerDirectory = layerDirectory;
    }

    public synchronized File getTerrainDirectory() {
        return terrainDirectory;
    }

    public synchronized void setTerrainDirectory(File terrainDirectory) {
        this.terrainDirectory = terrainDirectory;
    }

    public synchronized File getHeightMapsDirectory() {
        return heightMapsDirectory;
    }
    
    public synchronized void setHeightMapsDirectory(File heightMapsDirectory) {
        this.heightMapsDirectory = heightMapsDirectory;
    }

    public synchronized Theme getHeightMapDefaultTheme() {
        return heightMapDefaultTheme;
    }

    public synchronized void setHeightMapDefaultTheme(Theme heightMapDefaultTheme) {
        this.heightMapDefaultTheme = heightMapDefaultTheme;
    }

    public synchronized boolean isDefaultCreateGoodiesChest() {
        return defaultCreateGoodiesChest;
    }

    public synchronized void setDefaultCreateGoodiesChest(boolean defaultCreateGoodiesChest) {
        this.defaultCreateGoodiesChest = defaultCreateGoodiesChest;
    }

    public synchronized boolean isDefaultMapFeatures() {
        return defaultMapFeatures;
    }

    public synchronized void setDefaultMapFeatures(boolean defaultMapFeatures) {
        this.defaultMapFeatures = defaultMapFeatures;
    }

    public synchronized boolean isDefaultAllowCheats() {
        return defaultAllowCheats;
    }

    public synchronized void setDefaultAllowCheats(boolean defaultAllowCheats) {
        this.defaultAllowCheats = defaultAllowCheats;
    }

    public synchronized MapGenerator getDefaultGenerator() {
        return defaultGeneratorObj;
    }

    public synchronized void setDefaultGenerator(MapGenerator defaultGenerator) {
        this.defaultGeneratorObj = defaultGenerator;
    }

    public synchronized GameType getDefaultGameType() {
        return defaultGameTypeObj;
    }

    public synchronized void setDefaultGameType(GameType defaultGameType) {
        defaultGameTypeObj = defaultGameType;
    }

    public synchronized byte[] getDefaultJideLayoutData() {
        return defaultJideLayoutData;
    }

    public synchronized void setDefaultJideLayoutData(byte[] defaultJideLayoutData) {
        this.defaultJideLayoutData = defaultJideLayoutData;
    }

    public synchronized Map<String, byte[]> getJideLayoutData() {
        return jideLayoutData;
    }

    public synchronized void setJideLayoutData(Map<String, byte[]> jideLayoutData) {
        this.jideLayoutData = jideLayoutData;
    }

    public synchronized LookAndFeel getLookAndFeel() {
        return lookAndFeel;
    }

    public synchronized void setLookAndFeel(LookAndFeel lookAndFeel) {
        this.lookAndFeel = lookAndFeel;
    }

    public synchronized AccelerationType getAccelerationType() {
        return accelerationType;
    }

    public synchronized void setAccelerationType(AccelerationType accelerationType) {
        this.accelerationType = accelerationType;
    }

    public synchronized OverlayType getOverlayType() {
        return overlayType;
    }

    public synchronized void setOverlayType(OverlayType overlayType) {
        this.overlayType = overlayType;
    }

    public synchronized int getShowCalloutCount() {
        return showCalloutCount;
    }

    public synchronized void setShowCalloutCount(int showCalloutCount) {
        this.showCalloutCount = showCalloutCount;
    }

    public synchronized List<File> getRecentFiles() {
        return recentFiles;
    }

    public synchronized void setRecentFiles(List<File> recentFiles) {
        this.recentFiles = recentFiles;
    }

    public synchronized List<File> getRecentScriptFiles() {
        return recentScriptFiles;
    }

    public synchronized void setRecentScriptFiles(List<File> recentScriptFiles) {
        this.recentScriptFiles = recentScriptFiles;
    }

    public synchronized File getMasksDirectory() {
        return masksDirectory;
    }

    public synchronized void setMasksDirectory(File masksDirectory) {
        this.masksDirectory = masksDirectory;
    }

    public synchronized File getBackgroundImage() {
        return backgroundImage;
    }

    public synchronized void setBackgroundImage(File backgroundImage) {
        this.backgroundImage = backgroundImage;
    }

    public synchronized TiledImageViewer.BackgroundImageMode getBackgroundImageMode() {
        return backgroundImageMode;
    }

    public synchronized void setBackgroundImageMode(TiledImageViewer.BackgroundImageMode backgroundImageMode) {
        this.backgroundImageMode = backgroundImageMode;
    }

    public synchronized int getBackgroundColour() {
        return backgroundColour;
    }

    public synchronized void setBackgroundColour(int backgroundColour) {
        this.backgroundColour = backgroundColour;
    }

    public synchronized boolean isShowBorders() {
        return showBorders;
    }

    public synchronized void setShowBorders(boolean showBorders) {
        this.showBorders = showBorders;
    }

    public synchronized boolean isShowBiomes() {
        return showBiomes;
    }

    public synchronized void setShowBiomes(boolean showBiomes) {
        this.showBiomes = showBiomes;
    }

    public synchronized Platform getDefaultPlatform() {
        return Platform.getById(defaultPlatformId);
    }

    public synchronized void setDefaultPlatform(Platform defaultPlatform) {
        this.defaultPlatformId = defaultPlatform.id;
    }

    public synchronized boolean isAutosaveEnabled() {
        return autosaveEnabled;
    }

    public synchronized void setAutosaveEnabled(boolean autosaveEnabled) {
        this.autosaveEnabled = autosaveEnabled;
    }

    public synchronized int getAutosaveDelay() {
        return autosaveDelay;
    }

    public synchronized void setAutosaveDelay(int autosaveDelay) {
        this.autosaveDelay = autosaveDelay;
    }

    public synchronized int getAutosaveInterval() {
        return autosaveInterval;
    }

    public synchronized void setAutosaveInterval(int autosaveInterval) {
        this.autosaveInterval = autosaveInterval;
    }

    public synchronized boolean isSnapshotWarningDisplayed() {
        return snapshotWarningDisplayed;
    }

    public synchronized void setSnapshotWarningDisplayed(boolean snapshotWarningDisplayed) {
        this.snapshotWarningDisplayed = snapshotWarningDisplayed;
    }

    public synchronized boolean isBeta118WarningDisplayed() {
        return beta118WarningDisplayed;
    }

    public synchronized void setBeta118WarningDisplayed(boolean beta118WarningDisplayed) {
        this.beta118WarningDisplayed = beta118WarningDisplayed;
    }

    public synchronized int getMinimumFreeSpaceForMaps() {
        return minimumFreeSpaceForMaps;
    }

    public synchronized void setMinimumFreeSpaceForMaps(int minimumFreeSpaceForMaps) {
        this.minimumFreeSpaceForMaps = minimumFreeSpaceForMaps;
    }

    public synchronized boolean isAutoDeleteBackups() {
        return autoDeleteBackups;
    }

    public synchronized void setAutoDeleteBackups(boolean autoDeleteBackups) {
        this.autoDeleteBackups = autoDeleteBackups;
    }

    public synchronized ExportSettings getDefaultExportSettings() {
        return defaultExportSettings;
    }

    public synchronized void setDefaultExportSettings(ExportSettings defaultExportSettings) {
        this.defaultExportSettings = defaultExportSettings;
    }

    // Transient settings which aren't stored on disk

    public boolean isAutosaveInhibited() {
        return autosaveInhibited;
    }

    public void setAutosaveInhibited(boolean autosaveInhibited) {
        this.autosaveInhibited = autosaveInhibited;
    }

    public boolean isSafeMode() {
        return safeMode;
    }

    public void setSafeMode(boolean safeMode) {
        this.safeMode = safeMode;
    }

    public float getUiScale() {
        return uiScale;
    }

    public void setUiScale(float uiScale) {
        this.uiScale = uiScale;
    }

    public int getDefaultResourcesMinimumLevel() {
        return defaultResourcesMinimumLevel;
    }

    public void setDefaultResourcesMinimumLevel(int defaultResourcesMinimumLevel) {
        this.defaultResourcesMinimumLevel = defaultResourcesMinimumLevel;
    }

    public <T> T getAdvancedSetting(AttributeKey<T> key) {
        String value = System.getProperty(ADVANCED_SETTING_PREFIX + '.' + key.key);
        if (value != null) {
            return key.toValue(value);
        } else {
            return key.defaultValue;
        }
    }

    /**
     * Get the current configuration data version of this configuration.
     *
     * @return The current configuration data version of this configuration.
     */
    public int getVersion() {
        return version;
    }

    /**
     * Get the previous configuration data version of this configuration, which
     * may be lower than the current version if the configuration was upgraded
     * during load.
     *
     * @return The previous version of the configuration data as it existed on
     * disk before loading, or {@code -1} if this configuration was not
     * loaded from disk.
     */
    public int getPreviousVersion() {
        return previousVersion;
    }

    @Override
    public synchronized void logEvent(EventVO event) {
        if (eventLog != null) {
            eventLog.add(event);
        }
    }
    
    public synchronized List<EventVO> getEventLog() {
        return (eventLog != null) ? new ArrayList<>(eventLog) : null;
    }

    public synchronized void removeEvents(Collection<EventVO> events) {
        if (eventLog != null) {
            eventLog.removeAll(events);
        }
    }

    public synchronized void clearStatistics() {
        if (eventLog != null) {
            eventLog = new LinkedList<>();
        }
    }

    public synchronized void save() throws IOException {
        logger.info("Saving configuration to " + getConfigFile().getAbsolutePath());
        if (! getConfigDir().isDirectory()) {
            getConfigDir().mkdirs();
        }
        save(getConfigFile());
    }
    
    public synchronized void save(File configFile) throws IOException {
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(configFile))) {
            out.writeObject(this);
        }
    }
    
    @SuppressWarnings("deprecation")
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();

        previousVersion = version;
        
        // Legacy config
        if ((border != null) && (border2 == null)) {
            border2 = Border.valueOf(border.name());
            border = null;
        }
        if (customMaterials == null) {
            customMaterials = new Material[] {Material.DIRT, Material.DIRT, Material.DIRT, Material.DIRT, Material.DIRT};
        }
        if (minecraftJars == null) {
            minecraftJars = new HashMap<>();
        }
        if (uuid == null) {
            uuid = UUID.randomUUID();
        }
        if (undoLevels == 0) {
            checkForUpdates = true;
            undoEnabled = true;
            defaultContoursEnabled = true;
            undoLevels = 100;
            defaultGridSize = 128;
            defaultContourSeparation = 10;
            defaultWidth = 5;
            defaultHeight = 5;
            defaultMaxHeight = DEFAULT_MAX_HEIGHT_ANVIL;
        }
        if (defaultTerrainAndLayerSettings == null) {
            defaultTerrainAndLayerSettings = new World2(JAVA_ANVIL_1_15, World2.DEFAULT_OCEAN_SEED, TileFactoryFactory.createNoiseTileFactory(new Random().nextLong(), surface, JAVA_ANVIL_1_15.minZ, defaultMaxHeight, level, waterLevel, lava, beaches, 20, 1.0), defaultMaxHeight).getDimension(DIM_NORMAL);
        }
        
        // New legacy mechanism with version number
        if (version < 1) {
            worldFileBackups = 3;
        }
        if (version < 2) {
            defaultRange = 20;
            defaultScale = 1.0;
            defaultLightOrigin = LightOrigin.NORTHWEST;
        }
        if (version < 3) {
            String maxRadiusStr = System.getProperty("org.pepsoft.worldpainter.maxRadius");
            if ((maxRadiusStr != null) && (! maxRadiusStr.trim().isEmpty())) {
                try {
                    maximumBrushSize = Integer.parseInt(maxRadiusStr);
                } catch (NumberFormatException e) {
                    maximumBrushSize = 300;
                }
            } else {
                maximumBrushSize = 300;
            }
        }
        if (version < 4) {
            // Turn on smooth snow for everyone once
            FrostSettings frostSettings = (FrostSettings) defaultTerrainAndLayerSettings.getLayerSettings(Frost.INSTANCE);
            if (frostSettings != null) {
                frostSettings.setMode(FrostSettings.MODE_SMOOTH);
            }
        }
        if (version < 6) {
            if (! Boolean.FALSE.equals(pingAllowed)) {
                eventLog = new LinkedList<>();
            }
        }
        if (version < 7) {
            customLayers = new ArrayList<>();
            mixedMaterials = new ArrayList<>();
        }
        if (version < 8) {
            // Check whether the default terrain map still has the deprecated
            // "snow on rock" terrain type, and if so replace it with a layer.
            // Note that this isn't perfect: it assumes that the "snow on rock"
            // terrain type, if it exists, is the highest one and should
            // continue to the top of the map
            if ((defaultTerrainAndLayerSettings.getTileFactory() instanceof HeightMapTileFactory)
                    && (((HeightMapTileFactory) defaultTerrainAndLayerSettings.getTileFactory()).getTheme() instanceof SimpleTheme)) {
                SimpleTheme theme = (SimpleTheme) ((HeightMapTileFactory) defaultTerrainAndLayerSettings.getTileFactory()).getTheme();
                // Very old maps don't have terrainRanges set. They are out of
                // luck; it's not worth migrating them as well
                if (theme.getTerrainRanges() != null) {
                    SortedMap<Integer, Terrain> terrainRanges = theme.getTerrainRanges();
                    Map<Filter, Layer> layerMap = new HashMap<>();
                    boolean frostAdded = false;
                    for (Map.Entry<Integer, Terrain> entry : terrainRanges.entrySet()) {
                        if (entry.getValue() == Terrain.SNOW) {
                            if (!frostAdded) {
                                layerMap.put(new HeightFilter(0, defaultMaxHeight, entry.getKey(), defaultMaxHeight - 1, theme.isRandomise()), Frost.INSTANCE);
                                frostAdded = true;
                            }
                            entry.setValue(Terrain.STONE_MIX);
                        }
                    }
                    if (! layerMap.isEmpty()) {
                        theme.setLayerMap(layerMap);
                    }
                }
            }
        }
        if (version < 9) {
            // Set default export settings
            defaultCreateGoodiesChest = true;
            defaultGenerator = Generator.DEFAULT;
            defaultMapFeatures = true;
            defaultGameType = 0;
        }
        if (version < 10) {
            if (defaultTerrainAndLayerSettings.getSubsurfaceMaterial() == Terrain.STONE) {
                defaultTerrainAndLayerSettings.setSubsurfaceMaterial(Terrain.STONE_MIX);
            }
        }
        if (version < 11) {
            switch (SystemUtils.getOS()) {
                case WINDOWS:
                    accelerationType = AccelerationType.DIRECT3D;
                    break;
                case MAC:
                    accelerationType = AccelerationType.DEFAULT;
                    break;
                case LINUX:
                    accelerationType = AccelerationType.XRENDER;
                    break;
                default:
                    accelerationType = AccelerationType.DEFAULT;
                    break;
            }
            // Previous default; principle of least surprise:
            overlayType = OverlayType.SCALE_ON_LOAD;
        }
        if (version < 12) {
            defaultJideLayoutData = null;
            jideLayoutData = null;
            showCalloutCount = 3;
        }
        if (version < 13) {
            backgroundImageMode = TiledImageViewer.BackgroundImageMode.REPEAT;
            backgroundColour = -1;
            showBiomes = showBorders = true;
        }
        if (version < 14) {
            if (defaultGameType >= 0) {
                defaultGameTypeObj = GameType.values()[defaultGameType];
                defaultGameType = -1;
            } else {
                // Not sure how this could have happened, but it has been
                // observed
                defaultGameTypeObj = GameType.SURVIVAL;
                defaultGameType = -1;
            }
        }
        if (version < 15) {
            defaultPlatform = JAVA_ANVIL;
            exportDirectories = new HashMap<>();
            if (exportDirectory != null) {
                exportDirectories.put(JAVA_ANVIL, exportDirectory);
                exportDirectories.put(DefaultPlugin.JAVA_MCREGION, exportDirectory);
                exportDirectory = null;
            }
        }
        if (version < 16) {
            autosaveEnabled = true;
            autosaveDelay = 10000; // Ten seconds
            autosaveInterval = 300000; // Five minutes
        }
        if (version < 17) {
            defaultPlatformId = defaultPlatform.id;
            exportDirectoriesById = new HashMap<>();
            for (Map.Entry<Platform, File> entry: exportDirectories.entrySet()) {
                exportDirectoriesById.put(entry.getKey().id, entry.getValue());
            }
            defaultPlatform = null;
            exportDirectories = null;
        }
        if (version < 18) {
            // Do nothing; this only exists to signal Dynmap metadata removal
            // because it may be corrupted
        }
        if (version < 19) {
            if (defaultPlatformId.equals(JAVA_ANVIL.id)) {
                defaultPlatformId = JAVA_ANVIL_1_15.id;
            }
        }
        if (version < 20) {
            // Check whether the default terrain map still has the "rock" terrain type, which is inconsistent and
            // problematic, and if so replace it with "stone mix".
            if ((defaultTerrainAndLayerSettings.getTileFactory() instanceof HeightMapTileFactory)
                    && (((HeightMapTileFactory) defaultTerrainAndLayerSettings.getTileFactory()).getTheme() instanceof SimpleTheme)) {
                SimpleTheme theme = (SimpleTheme) ((HeightMapTileFactory) defaultTerrainAndLayerSettings.getTileFactory()).getTheme();
                // Very old maps don't have terrainRanges set. They are out of
                // luck; it's not worth migrating them as well
                if (theme.getTerrainRanges() != null) {
                    theme.getTerrainRanges().entrySet().forEach(entry -> {
                        if (entry.getValue() == ROCK) {
                            entry.setValue(STONE_MIX);
                        }
                    });
                }
            }
        }
        if (version < 21) {
            if (minimumFreeSpaceForMaps == 0) {
                minimumFreeSpaceForMaps = 5;
                autoDeleteBackups = true;
            }
            if (defaultGeneratorObj == null) {
                defaultGeneratorObj = MapGenerator.fromLegacySettings(defaultGenerator, DEFAULT_OCEAN_SEED, null, defaultGeneratorOptions, Platform.getById(defaultPlatformId));
                defaultGenerator = null;
                defaultGeneratorOptions = null;
            }
            if ((defaultGeneratorObj.getType() == DEFAULT) && (getDefaultPlatform().supportedGenerators.contains(LARGE_BIOMES))){
                defaultGeneratorObj = new SeededGenerator(LARGE_BIOMES, DEFAULT_OCEAN_SEED);
            }
        }
        if (version < 22) {
            defaultResourcesMinimumLevel = 8;
        }
        if (defaultTerrainAndLayerSettings.getLayerSettings(Resources.INSTANCE) != null) {
            defaultTerrainAndLayerSettings.setLayerSettings(Resources.INSTANCE, null);
        }
        version = CURRENT_VERSION;
        
        // Bug fix: make sure terrain ranges map conforms to surface material setting
        TileFactory tileFactory = defaultTerrainAndLayerSettings.getTileFactory();
        if ((tileFactory instanceof HeightMapTileFactory) && (((HeightMapTileFactory) tileFactory).getTheme() instanceof SimpleTheme)) {
            SortedMap<Integer, Terrain> defaultTerrainRanges = ((SimpleTheme) ((HeightMapTileFactory) tileFactory).getTheme()).getTerrainRanges();
            // Find what is probably meant to be the surface material. With the
            // default settings this should be -1, but if someone configured a
            // default underwater material, try not to change that
            int surfaceLevel = defaultTerrainRanges.headMap(waterLevel + 3).lastKey();
            defaultTerrainRanges.put(surfaceLevel, surface);
        }
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        // We keep having difficulties on Windows with Files being Windows-
        // specific subclasses of File which don't serialise correctly and end
        // up being null somehow. Work around the problem by making sure all
        // Files are actually java.io.Files
        worldDirectory = FileUtils.absolutise(worldDirectory);
        savesDirectory = FileUtils.absolutise(savesDirectory);
        customObjectsDirectory = FileUtils.absolutise(customObjectsDirectory);
        minecraftJars = FileUtils.absolutise(minecraftJars);
        layerDirectory = FileUtils.absolutise(layerDirectory);
        terrainDirectory = FileUtils.absolutise(terrainDirectory);
        heightMapsDirectory = FileUtils.absolutise(heightMapsDirectory);
        recentFiles = FileUtils.absolutise(recentFiles);
        recentScriptFiles = FileUtils.absolutise(recentScriptFiles);
        masksDirectory = FileUtils.absolutise(masksDirectory);
        backgroundImage = FileUtils.absolutise(backgroundImage);
        exportDirectoriesById = FileUtils.absolutise(exportDirectoriesById);
        
        out.defaultWriteObject();
    }

    public static synchronized Configuration load() throws IOException, ClassNotFoundException {
        File configFile = getConfigFile();
        if (! configFile.isFile()) {
            File oldConfigFile = new File(System.getProperty("user.home"), ".worldpainter/config");
            if (oldConfigFile.isFile()) {
                migrateConfiguration();
            }
        }
        if (configFile.isFile()) {
            logger.info("Loading configuration from " + configFile.getAbsolutePath());
            return load(configFile);
        } else {
            return null;
        }
    }

    public static synchronized Configuration load(File configFile) throws IOException, ClassNotFoundException {
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(configFile))) {
            return (Configuration) in.readObject();
        }
    }
    
    public static synchronized Configuration getInstance() {
        return instance;
    }

    public static synchronized void setInstance(Configuration instance) {
        Configuration.instance = instance;
    }

    public static File getConfigDir() {
        if (Version.isSnapshot()) {
            if (SystemUtils.isMac()) {
                return new File(System.getProperty("user.home"), "Library/Application Support/WorldPainter [SNAPSHOT]");
            } else if (SystemUtils.isWindows()) {
                String appDataStr = System.getenv("APPDATA");
                if (appDataStr != null) {
                    return new File(appDataStr, "WorldPainter [SNAPSHOT]");
                } else {
                    return new File(System.getProperty("user.home"), ".worldpainter-snapshot");
                }
            } else {
                return new File(System.getProperty("user.home"), ".worldpainter-snapshot");
            }
        } else {
            if (SystemUtils.isMac()) {
                return new File(System.getProperty("user.home"), "Library/Application Support/WorldPainter");
            } else if (SystemUtils.isWindows()) {
                String appDataStr = System.getenv("APPDATA");
                if (appDataStr != null) {
                    return new File(appDataStr, "WorldPainter");
                } else {
                    return new File(System.getProperty("user.home"), ".worldpainter");
                }
            } else {
                return new File(System.getProperty("user.home"), ".worldpainter");
            }
        }
    }

    public static File getConfigFile() {
        return new File(getConfigDir(), "config");
    }
    
    private static void migrateConfiguration() throws IOException {
        File newConfigDir = getConfigDir();
        File oldConfigDir = new File(System.getProperty("user.home"), ".worldpainter");
        logger.info("Configuration found in old location (" + oldConfigDir + "); migrating it to new location (" + newConfigDir + ")");
        File[] oldContents = oldConfigDir.listFiles((dir, name) -> {
            // Skip log files, since the new log file is already open in the
            // new location
            return ! name.startsWith("logfile");
        });
        //noinspection ConstantConditions // This method is only invoked when oldContents in fact exists
        for (File oldFile: oldContents) {
            if (oldFile.isDirectory()) {
                FileUtils.copyDir(oldFile, new File(newConfigDir, oldFile.getName()));
            } else if (oldFile.isFile()) {
                FileUtils.copyFileToDir(oldFile, newConfigDir);
            } else {
                logger.warn("Directory entry encountered which was neither file nor directory: " + oldFile);
            }
        }
        FileUtils.deleteDir(oldConfigDir);
    }

    private Rectangle windowBounds;
    private boolean maximised, hilly = true, lava, goodies = true, populate, beaches = true, mergeWarningDisplayed, importWarningDisplayed;
    private int level = 58, waterLevel = DEFAULT_WATER_LEVEL, borderLevel = DEFAULT_WATER_LEVEL;
    private Terrain surface = Terrain.GRASS, underground = Terrain.RESOURCES;
    private File worldDirectory;
    @Deprecated
    private File exportDirectory;
    private File savesDirectory, customObjectsDirectory;
    @Deprecated
    private World.Border border;
    private Border border2;
    private Boolean pingAllowed;
    @Deprecated
    private Material[] customMaterials = {DIRT, DIRT, DIRT, DIRT, DIRT};
    @Deprecated
    private int colourschemeIndex;
    private int launchCount;
    private Map<Integer, File> minecraftJars = new HashMap<>();
    private DonationStatus donationStatus;
    private UUID uuid = UUID.randomUUID();
    // Default view and world settings
    private boolean checkForUpdates = true, undoEnabled = true, defaultGridEnabled, defaultContoursEnabled = true, defaultViewDistanceEnabled, defaultWalkingDistanceEnabled;
    private int undoLevels = 100, defaultGridSize = 128, defaultContourSeparation = 10, defaultWidth = 5, defaultHeight = 5, defaultMaxHeight = World2.DEFAULT_MAX_HEIGHT;
    private Dimension defaultTerrainAndLayerSettings = new World2(DEFAULT_PLATFORM, World2.DEFAULT_OCEAN_SEED, TileFactoryFactory.createNoiseTileFactory(new Random().nextLong(), surface, DEFAULT_PLATFORM.minZ, defaultMaxHeight, level, waterLevel, lava, beaches, 20, 1.0), defaultMaxHeight).getDimension(DIM_NORMAL);
    private boolean toolbarsLocked;
    private int version = CURRENT_VERSION, worldFileBackups = 3;
    private float defaultRange = 20, uiScale;
    private double defaultScale = 1.0;
    private LightOrigin defaultLightOrigin = LightOrigin.NORTHWEST;
    private int maximumBrushSize = 300;
    private List<EventVO> eventLog = new LinkedList<>();
    private List<CustomLayer> customLayers = new ArrayList<>();
    private List<MixedMaterial> mixedMaterials = new ArrayList<>();
//    private boolean easyMode = true;
    private boolean defaultExtendedBlockIds;
    private File layerDirectory, terrainDirectory, heightMapsDirectory, masksDirectory, backgroundImage;
    private Theme heightMapDefaultTheme;
    private boolean defaultCreateGoodiesChest = true, defaultMapFeatures = true, defaultAllowCheats;
    @Deprecated
    private Generator defaultGenerator;
    @Deprecated
    private int defaultGameType;
    @Deprecated
    private String defaultGeneratorOptions;
    private byte[] defaultJideLayoutData;
    private Map<String, byte[]> jideLayoutData;
    private LookAndFeel lookAndFeel;
    private OverlayType overlayType = OverlayType.OPTIMISE_ON_LOAD;
    private int showCalloutCount = 3;
    private List<File> recentFiles;
    private List<File> recentScriptFiles;
    private TiledImageViewer.BackgroundImageMode backgroundImageMode = TiledImageViewer.BackgroundImageMode.REPEAT;
    private int backgroundColour = -1;
    private boolean showBorders = true, showBiomes = true;
    private GameType defaultGameTypeObj = GameType.SURVIVAL;
    @Deprecated
    private Platform defaultPlatform;
    @Deprecated
    private Map<Platform, File> exportDirectories;
    private boolean autosaveEnabled = true;
    private int autosaveDelay = 10000, autosaveInterval = 300000; // Ten seconds delay; five minutes interval
    private String defaultPlatformId = DEFAULT_PLATFORM.id;
    private Map<String, File> exportDirectoriesById = new HashMap<>();
    private boolean snapshotWarningDisplayed;
    private boolean beta118WarningDisplayed;
    private int minimumFreeSpaceForMaps = 1;
    private boolean autoDeleteBackups = true;
    private MapGenerator defaultGeneratorObj = new SeededGenerator(LARGE_BIOMES, DEFAULT_OCEAN_SEED);
    private ExportSettings defaultExportSettings;
    private int defaultResourcesMinimumLevel = 8;

    /**
     * The acceleration type is only stored here at runtime. It is saved to disk
     * using the Preferences API.
     */
    private transient AccelerationType accelerationType;

    // Runtime settings which aren't stored on disk
    private transient boolean autosaveInhibited, safeMode;
    private transient int previousVersion = -1;

    private static Configuration instance;
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Configuration.class);
    private static final long serialVersionUID = 2011041801L;
    private static final int CIRCULAR_WORLD = -1;
    private static final int CURRENT_VERSION = 22;

    public static final String ADVANCED_SETTING_PREFIX = "org.pepsoft.worldpainter";
    public static final Platform DEFAULT_PLATFORM = JAVA_ANVIL_1_15;

    public enum DonationStatus {DONATED, NO_THANK_YOU}
    
    public enum LookAndFeel {SYSTEM, METAL, NIMBUS, DARK_METAL, DARK_NIMBUS}

    public enum OverlayType {SCALE_ON_LOAD, OPTIMISE_ON_LOAD, SCALE_ON_PAINT}
}