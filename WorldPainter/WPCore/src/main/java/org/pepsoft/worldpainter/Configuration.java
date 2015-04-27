/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.worldpainter;

import org.pepsoft.minecraft.Material;
import org.pepsoft.util.FileUtils;
import org.pepsoft.util.SystemUtils;
import org.pepsoft.worldpainter.Dimension.Border;
import org.pepsoft.worldpainter.TileRenderer.LightOrigin;
import org.pepsoft.worldpainter.layers.CustomLayer;
import org.pepsoft.worldpainter.layers.Frost;
import org.pepsoft.worldpainter.layers.Layer;
import org.pepsoft.worldpainter.layers.Resources;
import org.pepsoft.worldpainter.layers.exporters.FrostExporter.FrostSettings;
import org.pepsoft.worldpainter.layers.exporters.ResourcesExporter.ResourcesExporterSettings;
import org.pepsoft.worldpainter.themes.Filter;
import org.pepsoft.worldpainter.themes.HeightFilter;
import org.pepsoft.worldpainter.themes.SimpleTheme;
import org.pepsoft.worldpainter.themes.Theme;
import org.pepsoft.worldpainter.util.MinecraftJarProvider;
import org.pepsoft.worldpainter.vo.EventVO;

import java.awt.*;
import java.io.*;
import java.util.*;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.pepsoft.minecraft.Constants.DEFAULT_MAX_HEIGHT_2;
import static org.pepsoft.minecraft.Material.DIRT;

/**
 *
 * @author pepijn
 */
public final class Configuration implements Serializable, EventLogger, MinecraftJarProvider {
    public Configuration() {
        if (logger.isLoggable(Level.FINE)) {
            logger.fine("Creating new configuration");
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

    public synchronized File getExportDirectory() {
        return exportDirectory;
    }

    public synchronized void setExportDirectory(File exportDirectory) {
        this.exportDirectory = exportDirectory;
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
            eventLog = new LinkedList<EventVO>();
        } else if (Boolean.FALSE.equals(pingAllowed)) {
            eventLog = null;
        }
    }

    public synchronized int getColourschemeIndex() {
        return colourschemeIndex;
    }

    public synchronized void setColourschemeIndex(int colourschemeIndex) {
        this.colourschemeIndex = colourschemeIndex;
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

    public Theme getHeightMapDefaultTheme() {
        return heightMapDefaultTheme;
    }

    public void setHeightMapDefaultTheme(Theme heightMapDefaultTheme) {
        this.heightMapDefaultTheme = heightMapDefaultTheme;
    }

    public boolean isDefaultCreateGoodiesChest() {
        return defaultCreateGoodiesChest;
    }

    public void setDefaultCreateGoodiesChest(boolean defaultCreateGoodiesChest) {
        this.defaultCreateGoodiesChest = defaultCreateGoodiesChest;
    }

    public boolean isDefaultMapFeatures() {
        return defaultMapFeatures;
    }

    public void setDefaultMapFeatures(boolean defaultMapFeatures) {
        this.defaultMapFeatures = defaultMapFeatures;
    }

    public boolean isDefaultAllowCheats() {
        return defaultAllowCheats;
    }

    public void setDefaultAllowCheats(boolean defaultAllowCheats) {
        this.defaultAllowCheats = defaultAllowCheats;
    }

    public Generator getDefaultGenerator() {
        return defaultGenerator;
    }

    public void setDefaultGenerator(Generator defaultGenerator) {
        this.defaultGenerator = defaultGenerator;
    }

    public int getDefaultGameType() {
        return defaultGameType;
    }

    public void setDefaultGameType(int defaultGameType) {
        this.defaultGameType = defaultGameType;
    }

    public String getDefaultGeneratorOptions() {
        return defaultGeneratorOptions;
    }

    public void setDefaultGeneratorOptions(String defaultGeneratorOptions) {
        this.defaultGeneratorOptions = defaultGeneratorOptions;
    }

    public byte[] getDefaultJideLayoutData() {
        return defaultJideLayoutData;
    }

    public void setDefaultJideLayoutData(byte[] defaultJideLayoutData) {
        this.defaultJideLayoutData = defaultJideLayoutData;
    }

    public Map<String, byte[]> getJideLayoutData() {
        return jideLayoutData;
    }

    public void setJideLayoutData(Map<String, byte[]> jideLayoutData) {
        this.jideLayoutData = jideLayoutData;
    }

    public LookAndFeel getLookAndFeel() {
        return lookAndFeel;
    }

    public void setLookAndFeel(LookAndFeel lookAndFeel) {
        this.lookAndFeel = lookAndFeel;
    }

    @Override
    public synchronized void logEvent(EventVO event) {
        if (eventLog != null) {
            eventLog.add(event);
        }
    }
    
    public synchronized List<EventVO> getEventLog() {
        return (eventLog != null) ? new ArrayList<EventVO>(eventLog) : null;
    }
    
    public synchronized void clearStatistics() {
        if (eventLog != null) {
            eventLog = new LinkedList<EventVO>();
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
        ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(configFile));
        try {
            out.writeObject(this);
        } finally {
            out.close();
        }
    }
    
    @SuppressWarnings("deprecation")
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        
        // Legacy config
        if ((border != null) && (border2 == null)) {
            border2 = Border.valueOf(border.name());
            border = null;
        }
        if (customMaterials == null) {
            customMaterials = new Material[] {Material.DIRT, Material.DIRT, Material.DIRT, Material.DIRT, Material.DIRT};
        }
        if (minecraftJars == null) {
            minecraftJars = new HashMap<Integer, File>();
            if (minecraft1_9_p3Jar != null) {
                minecraftJars.put(Constants.BIOME_ALGORITHM_1_9, minecraft1_9_p3Jar);
                minecraft1_9_p3Jar = null;
            }
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
            defaultMaxHeight = DEFAULT_MAX_HEIGHT_2;
        }
        if (defaultTerrainAndLayerSettings == null) {
            defaultTerrainAndLayerSettings = new Dimension(World2.DEFAULT_OCEAN_SEED, TileFactoryFactory.createNoiseTileFactory(new Random().nextLong(), surface, defaultMaxHeight, level, waterLevel, lava, beaches, 20, 1.0), Constants.DIM_NORMAL, defaultMaxHeight);
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
        if (version < 5) {
            // Reset the Resource setting preferences once, which might have
            // been unintentionally altered due to a bug
            defaultTerrainAndLayerSettings.setLayerSettings(Resources.INSTANCE, new ResourcesExporterSettings(defaultMaxHeight));
        }
        if (version < 6) {
            if (! Boolean.FALSE.equals(pingAllowed)) {
                eventLog = new LinkedList<EventVO>();
            }
        }
        if (version < 7) {
            customLayers = new ArrayList<CustomLayer>();
            mixedMaterials = new ArrayList<MixedMaterial>();
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
                    Map<Filter, Layer> layerMap = new HashMap<Filter, Layer>();
                    boolean frostAdded = false;
                    for (Iterator<Map.Entry<Integer, Terrain>> i = terrainRanges.entrySet().iterator(); i.hasNext(); ) {
                        Map.Entry<Integer, Terrain> entry = i.next();
                        if (entry.getValue() == Terrain.SNOW) {
                            if (! frostAdded) {
                                layerMap.put(new HeightFilter(defaultMaxHeight, entry.getKey(), defaultMaxHeight - 1, theme.isRandomise()), Frost.INSTANCE);
                                frostAdded = true;
                            }
                            entry.setValue(Terrain.ROCK);
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
            defaultGameType = World2.GAME_TYPE_SURVIVAL;
        }
        if (version < 10) {
            if (defaultTerrainAndLayerSettings.getSubsurfaceMaterial() == Terrain.STONE) {
                defaultTerrainAndLayerSettings.setSubsurfaceMaterial(Terrain.STONE_MIX);
            }
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
        ObjectInputStream in = new ObjectInputStream(new FileInputStream(configFile));
        try {
            return (Configuration) in.readObject();
        } finally {
            in.close();
        }
    }
    
    public static synchronized Configuration getInstance() {
        return instance;
    }

    public static synchronized void setInstance(Configuration instance) {
        Configuration.instance = instance;
    }

    public static File getConfigDir() {
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

    private static File getConfigFile() {
        return new File(getConfigDir(), "config");
    }
    
    private static void migrateConfiguration() throws IOException {
        File newConfigDir = getConfigDir();
        File oldConfigDir = new File(System.getProperty("user.home"), ".worldpainter");
        logger.info("Configuration found in old location (" + oldConfigDir + "); migrating it to new location (" + newConfigDir + ")");
        File[] oldContents = oldConfigDir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                // Skip log files, since the new log file is already open in the
                // new location
                return ! name.startsWith("logfile");
            }
        });
        for (File oldFile: oldContents) {
            if (oldFile.isDirectory()) {
                FileUtils.copyDir(oldFile, newConfigDir);
            } else if (oldFile.isFile()) {
                FileUtils.copyFile(oldFile, newConfigDir);
            } else {
                logger.warning("Directory entry encountered which was neither file nor directory: " + oldFile);
            }
        }
        FileUtils.deleteDir(oldConfigDir);
    }

    private Rectangle windowBounds;
    private boolean maximised, hilly = true, lava, goodies = true, populate, beaches = true, mergeWarningDisplayed, importWarningDisplayed;
    private int level = 58, waterLevel = 62, borderLevel = 62;
    private Terrain surface = Terrain.GRASS, underground = Terrain.RESOURCES;
    private File worldDirectory, exportDirectory, savesDirectory, customObjectsDirectory;
    @Deprecated
    private File minecraft1_9_p3Jar;
    @Deprecated
    private World.Border border;
    private Border border2;
    private Boolean pingAllowed;
    @Deprecated
    private Material[] customMaterials = {DIRT, DIRT, DIRT, DIRT, DIRT};
    private int colourschemeIndex, launchCount;
    private Map<Integer, File> minecraftJars = new HashMap<Integer, File>();
    private DonationStatus donationStatus;
    private UUID uuid = UUID.randomUUID();
    // Default view and world settings
    private boolean checkForUpdates = true, undoEnabled = true, defaultGridEnabled, defaultContoursEnabled = true, defaultViewDistanceEnabled, defaultWalkingDistanceEnabled;
    private int undoLevels = 100, defaultGridSize = 128, defaultContourSeparation = 10, defaultWidth = 5, defaultHeight = 5, defaultMaxHeight = World2.DEFAULT_MAX_HEIGHT;
    private Dimension defaultTerrainAndLayerSettings = new Dimension(World2.DEFAULT_OCEAN_SEED, TileFactoryFactory.createNoiseTileFactory(new Random().nextLong(), surface, defaultMaxHeight, level, waterLevel, lava, beaches, 20, 1.0), Constants.DIM_NORMAL, defaultMaxHeight);
    private boolean toolbarsLocked;
    private int version = CURRENT_VERSION, worldFileBackups = 3;
    private float defaultRange = 20;
    private double defaultScale = 1.0;
    private LightOrigin defaultLightOrigin = LightOrigin.NORTHWEST;
    private int maximumBrushSize = 300;
    private List<EventVO> eventLog = new LinkedList<EventVO>();
    private List<CustomLayer> customLayers = new ArrayList<CustomLayer>();
    private List<MixedMaterial> mixedMaterials = new ArrayList<MixedMaterial>();
//    private boolean easyMode = true;
    private boolean defaultExtendedBlockIds;
    private File layerDirectory, terrainDirectory, heightMapsDirectory;
    private Theme heightMapDefaultTheme;
    private boolean defaultCreateGoodiesChest = true, defaultMapFeatures = true, defaultAllowCheats;
    private Generator defaultGenerator = Generator.DEFAULT;
    private int defaultGameType = World2.GAME_TYPE_SURVIVAL;
    private String defaultGeneratorOptions;
    private byte[] defaultJideLayoutData;
    private Map<String, byte[]> jideLayoutData;
    private LookAndFeel lookAndFeel;

    private static Configuration instance;
    private static final Logger logger = Logger.getLogger(Configuration.class.getName());
    private static final long serialVersionUID = 2011041801L;
    private static final int CIRCULAR_WORLD = -1;
    private static final int CURRENT_VERSION = 10;
    
    public enum DonationStatus {DONATED, NO_THANK_YOU}
    
    public enum LookAndFeel {SYSTEM, METAL, NIMBUS, DARK_METAL, DARK_NIMBUS}
}