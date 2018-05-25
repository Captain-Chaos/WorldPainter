/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.worldpainter;

import org.pepsoft.minecraft.Direction;
import org.pepsoft.minecraft.Material;
import org.pepsoft.util.AttributeKey;
import org.pepsoft.util.MemoryUtils;
import org.pepsoft.util.ProgressReceiver;
import org.pepsoft.util.SubProgressReceiver;
import org.pepsoft.util.undo.UndoManager;
import org.pepsoft.worldpainter.history.HistoryEntry;
import org.pepsoft.worldpainter.layers.Biome;
import org.pepsoft.worldpainter.layers.Layer;

import java.awt.*;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.*;
import java.util.List;

import static org.pepsoft.worldpainter.Constants.DIM_NORMAL;
import static org.pepsoft.worldpainter.Constants.TILE_SIZE;

/**
 *
 * @author pepijn
 */
public class World2 extends InstanceKeeper implements Serializable, Cloneable {
    public World2(Platform platform, int maxHeight) {
        if (platform == null) {
            throw new NullPointerException();
        } else if ((maxHeight < platform.minMaxHeight) || (maxHeight > platform.maxMaxHeight)) {
            throw new IllegalArgumentException();
        }
        this.platform = platform;
        this.maxheight = maxHeight;
    }
    
    public World2(Platform platform, long minecraftSeed, TileFactory tileFactory, int maxHeight) {
        if (platform == null) {
            throw new NullPointerException();
        } else if ((maxHeight < platform.minMaxHeight) || (maxHeight > platform.maxMaxHeight)) {
            throw new IllegalArgumentException();
        }
        this.platform = platform;
        this.maxheight = maxHeight;
        Dimension dim = new Dimension(this, minecraftSeed, tileFactory, 0, maxHeight);
        addDimension(dim);
    }
    
    public long getChangeNo() {
        long totalChangeNo = changeNo + borderSettings.getChangeNo();
        for (Dimension dimension: dimensions.values()) {
            totalChangeNo += dimension.getChangeNo();
        }
        if (logger.isDebugEnabled()) {
            logger.debug("World change no: {}", totalChangeNo);
        }
        return totalChangeNo;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        if (! (name.equals(this.name))) {
            String oldName = this.name;
            this.name = name;
            changeNo++;
            propertyChangeSupport.firePropertyChange("name", oldName, name);
        }
    }

    public boolean isCreateGoodiesChest() {
        return createGoodiesChest;
    }

    public void setCreateGoodiesChest(boolean createGoodiesChest) {
        if (createGoodiesChest != this.createGoodiesChest) {
            this.createGoodiesChest = createGoodiesChest;
            changeNo++;
            propertyChangeSupport.firePropertyChange("createGoodiesChest", ! createGoodiesChest, createGoodiesChest);
        }
    }

    public Point getTileCoordinates(int worldX, int worldY) {
        int tileX = (int) Math.floor((double) worldX / TILE_SIZE);
        int tileY = (int) Math.floor((double) worldY / TILE_SIZE);
        return new Point(tileX, tileY);
    }
    
    public Point getTileCoordinates(Point worldCoords) {
        return getTileCoordinates(worldCoords.x, worldCoords.y);
    }

    public Point getSpawnPoint() {
        return spawnPoint;
    }

    public void setSpawnPoint(Point spawnPoint) {
        if (! spawnPoint.equals(this.spawnPoint)) {
            Point oldSpawnPoint = this.spawnPoint;
            this.spawnPoint = spawnPoint;
            changeNo++;
            propertyChangeSupport.firePropertyChange("spawnPoint", oldSpawnPoint, spawnPoint);
        }
    }

    public File getImportedFrom() {
        return importedFrom;
    }

    public void setImportedFrom(File importedFrom) {
        if ((importedFrom == null) ? (this.importedFrom != null) : (! importedFrom.equals(this.importedFrom))) {
            File oldImportedFrom = this.importedFrom;
            this.importedFrom = importedFrom;
            propertyChangeSupport.firePropertyChange("importedFrom", oldImportedFrom, importedFrom);
        }
    }

    public boolean isMapFeatures() {
        return mapFeatures;
    }
    
    public void setMapFeatures(boolean mapFeatures) {
        if (mapFeatures != this.mapFeatures) {
            this.mapFeatures = mapFeatures;
            changeNo++;
            propertyChangeSupport.firePropertyChange("mapFeatures", ! mapFeatures, mapFeatures);
        }
    }

    public GameType getGameType() {
        return gameTypeObj;
    }

    public void setGameType(GameType gameType) {
        if (gameType != gameTypeObj) {
            GameType oldGameType = gameTypeObj;
            gameTypeObj = gameType;
            changeNo++;
            propertyChangeSupport.firePropertyChange("gameType", oldGameType, gameType);
        }
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        propertyChangeSupport.addPropertyChangeListener(listener);
    }

    public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        propertyChangeSupport.addPropertyChangeListener(propertyName, listener);
    }
    
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        propertyChangeSupport.removePropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        propertyChangeSupport.removePropertyChangeListener(propertyName, listener);
    }
    
    public Dimension getDimension(int dim) {
        return dimensions.get(dim);
    }
    
    public Dimension[] getDimensions() {
        return dimensions.values().toArray(new Dimension[dimensions.size()]);
    }
    
    public final void addDimension(Dimension dimension) {
        if (dimensions.containsKey(dimension.getDim())) {
            throw new IllegalStateException("Dimension " + dimension.getDim() + " already exists");
        } else if (dimension.getMaxHeight() != maxheight) {
            throw new IllegalStateException("Dimension has different max height (" + dimension.getMaxHeight() + ") than world (" + maxheight + ")");
        } else {
            if (dimension.getWorld() != this) {
                throw new IllegalArgumentException("Dimension belongs to another world");
            }
            dimensions.put(dimension.getDim(), dimension);
            if (dimension.getDim() == 0) {
                TileFactory tileFactory = dimension.getTileFactory();
                if (tileFactory instanceof HeightMapTileFactory) {
                    if (((HeightMapTileFactory) tileFactory).getWaterHeight() < 32) {
                        // Low level
                        generator = Generator.FLAT;
                    }
                }
            }
        }
        if (dimension.getDim() != DIM_NORMAL) {
            history.add(new HistoryEntry(HistoryEntry.WORLD_DIMENSION_ADDED, dimension.getName()));
        }
    }

    public Dimension removeDimension(int dim) {
        if (dimensions.containsKey(dim)) {
            changeNo++;
            Dimension dimension = dimensions.remove(dim);
            history.add(new HistoryEntry(HistoryEntry.WORLD_DIMENSION_REMOVED, dimension.getName()));
            return dimension;
        } else {
            throw new IllegalStateException("Dimension " + dim + " does not exist");
        }
    }

    public MixedMaterial getMixedMaterial(int index) {
        return mixedMaterials[index];
    }
    
    public void setMixedMaterial(int index, MixedMaterial material) {
        if ((material == null) ? (mixedMaterials[index] != null) : (! material.equals(mixedMaterials[index]))) {
            MixedMaterial oldMaterial = mixedMaterials[index];
            mixedMaterials[index] = material;
            changeNo++;
            propertyChangeSupport.fireIndexedPropertyChange("mixedMaterial", index, oldMaterial, material);
        }
    }

    public int getMaxHeight() {
        return maxheight;
    }

    public void setMaxHeight(int maxHeight) {
        if (maxHeight != this.maxheight) {
            int oldMaxHeight = this.maxheight;
            this.maxheight = maxHeight;
            changeNo++;
            propertyChangeSupport.firePropertyChange("maxHeight", oldMaxHeight, maxHeight);
        }
    }

    public Generator getGenerator() {
        return generator;
    }

    public void setGenerator(Generator generator) {
        if (generator != this.generator) {
            Generator oldGenerator = this.generator;
            this.generator = generator;
            changeNo++;
            propertyChangeSupport.firePropertyChange("generator", oldGenerator, generator);
        }
    }

    public Platform getPlatform() {
        return platform;
    }

    public void setPlatform(Platform platform) {
        if (platform == null) {
            throw new NullPointerException();
        }
        if (platform != this.platform) {
            Platform oldPlatform = this.platform;
            this.platform = platform;
            if (! oldPlatform.equals(platform)) {
                changeNo++;
            }
            propertyChangeSupport.firePropertyChange("platform", oldPlatform, platform);
        }
    }

    public boolean isAskToConvertToAnvil() {
        // Inverted so that legacy worlds have the correct setting
        return !dontAskToConvertToAnvil;
    }

    public void setAskToConvertToAnvil(boolean askToConvertToAnvil) {
        // Inverted so that legacy worlds have the correct setting
        if (askToConvertToAnvil == dontAskToConvertToAnvil) {
            dontAskToConvertToAnvil = !askToConvertToAnvil;
            changeNo++;
            propertyChangeSupport.firePropertyChange("askToConvertToAnvil", askToConvertToAnvil, !askToConvertToAnvil);
        }
    }

    public Set<Point> getTilesToExport() {
        return tilesToExport;
    }

    public void setTilesToExport(Set<Point> tilesToExport) {
        this.tilesToExport = tilesToExport;
    }

    public boolean isAskToRotate() {
        return askToRotate;
    }

    public void setAskToRotate(boolean askToRotate) {
        if (askToRotate != this.askToRotate) {
            this.askToRotate = askToRotate;
            changeNo++;
            propertyChangeSupport.firePropertyChange("askToRotate", !askToRotate, askToRotate);
        }
    }

    public Direction getUpIs() {
        return upIs;
    }

    public void setUpIs(Direction upIs) {
        if (upIs != this.upIs) {
            Direction oldUpIs = this.upIs;
            this.upIs = upIs;
            changeNo++;
            propertyChangeSupport.firePropertyChange("upIs", oldUpIs, upIs);
        }
    }

    public boolean isAllowMerging() {
        return allowMerging;
    }

    public void setAllowMerging(boolean allowMerging) {
        this.allowMerging = allowMerging;
    }

    public boolean isAllowCheats() {
        return allowCheats;
    }

    public void setAllowCheats(boolean allowCheats) {
        if (allowCheats != this.allowCheats) {
            this.allowCheats = allowCheats;
            changeNo++;
            propertyChangeSupport.firePropertyChange("allowCheats", !allowCheats, allowCheats);
        }
    }

    public String getGeneratorOptions() {
        return generatorOptions;
    }

    public void setGeneratorOptions(String generatorOptions) {
        if ((generatorOptions != null) ? (! generatorOptions.equals(this.generatorOptions)) : (this.generatorOptions != null)) {
            String oldGeneratorOptions = this.generatorOptions;
            this.generatorOptions = generatorOptions;
            changeNo++;
            propertyChangeSupport.firePropertyChange("generatorOptions", oldGeneratorOptions, generatorOptions);
        }
    }

    public boolean isExtendedBlockIds() {
        return extendedBlockIds;
    }

    public void setExtendedBlockIds(boolean extendedBlockIds) {
        if (extendedBlockIds != this.extendedBlockIds) {
            this.extendedBlockIds = extendedBlockIds;
            changeNo++;
            propertyChangeSupport.firePropertyChange("extendedBlockIds", !extendedBlockIds, extendedBlockIds);
        }
    }

    public int getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(int difficulty) {
        if (difficulty != this.difficulty) {
            int oldDifficulty = this.difficulty;
            this.difficulty = difficulty;
            changeNo++;
            propertyChangeSupport.firePropertyChange("difficulty", oldDifficulty, difficulty);
        }
    }

    public List<HistoryEntry> getHistory() {
        synchronized (history) {
            return history;
        }
    }

    public void addHistoryEntry(int key, Serializable... args) {
        synchronized (history) {
            history.add(new HistoryEntry(key, args));
        }
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    public BorderSettings getBorderSettings() {
        return borderSettings;
    }

    public File getMergedWith() {
        return mergedWith;
    }

    public void setMergedWith(File mergedWith) {
        if ((mergedWith == null) ? (this.mergedWith != null) : (! mergedWith.equals(this.mergedWith))) {
            File oldMergedWith = this.mergedWith;
            this.mergedWith = mergedWith;
            changeNo++;
            propertyChangeSupport.firePropertyChange("mergedWith", oldMergedWith, mergedWith);
        }
    }

    public Set<Integer> getDimensionsToExport() {
        return dimensionsToExport;
    }

    public void setDimensionsToExport(Set<Integer> dimensionsToExport) {
        if ((dimensionsToExport == null) ? (this.dimensionsToExport != null) : (! dimensionsToExport.equals(this.dimensionsToExport))) {
            Set<Integer> oldMergedDimensions = this.dimensionsToExport;
            this.dimensionsToExport = dimensionsToExport;
            changeNo++;
            propertyChangeSupport.firePropertyChange("mergedDimensions", oldMergedDimensions, dimensionsToExport);
        }
    }

    public <T> T getAttribute(AttributeKey<T> key) {
        if (attributes != null) {
            return attributes.containsKey(key.key) ? (T) attributes.get(key.key) : key.defaultValue;
        } else {
            return key.defaultValue;
        }
    }

    public <T> void setAttribute(AttributeKey<T> key, T value) {
        if ((value != null) ? value.equals(key.defaultValue) : (key.defaultValue == null)) {
            // Setting value to default
            attributes.remove(key.key);
            if (attributes.isEmpty()) {
                attributes = null;
            }
        } else {
            if (attributes == null) {
                attributes = new HashMap<>();
            }
            attributes.put(key.key, value);
        }
        changeNo++;
    }

    /**
     * Transforms all dimensions of this world horizontally. If an undo manager
     * is installed this operation will destroy all undo info.
     * 
     * @param transform The coordinate transform to apply.
     * @param progressReceiver A progress receiver which will be informed of
     *     rotation progress.
     */
    public void transform(CoordinateTransform transform, ProgressReceiver progressReceiver) throws ProgressReceiver.OperationCancelled {
        int dimCount = dimensions.size(), dim = 0;
        for (Dimension dimension: dimensions.values()) {
            dimension.transform(transform, (progressReceiver != null) ? new SubProgressReceiver(progressReceiver, (float) dim / dimCount, 1.0f / dimCount) : null);
            dim++;
        }
        Point oldSpawnPoint = spawnPoint;
        spawnPoint = transform.transform(spawnPoint);
        propertyChangeSupport.firePropertyChange("spawnPoint", oldSpawnPoint, spawnPoint);
        Direction oldUpIs = upIs;
        upIs = transform.inverseTransform(upIs);
        propertyChangeSupport.firePropertyChange("upIs", oldUpIs, upIs);
        changeNo++;
    }
    
    /**
     * Transforms one dimension horizontally. If it's the surface dimension also
     * transforms any surface-related metadata stored in the world. If an undo
     * manager is installed this operation will destroy all undo info.
     *
     * @param dim The index of the dimension to transform.
     * @param transform The coordinate transform to apply.
     * @param progressReceiver A progress receiver which will be informed of
     *     rotation progress.
     */
    public void transform(int dim, CoordinateTransform transform, ProgressReceiver progressReceiver) throws ProgressReceiver.OperationCancelled {
        dimensions.get(dim).transform(transform, progressReceiver);
        if (dim == DIM_NORMAL) {
            Point oldSpawnPoint = spawnPoint;
            spawnPoint = transform.transform(spawnPoint);
            propertyChangeSupport.firePropertyChange("spawnPoint", oldSpawnPoint, spawnPoint);
            Direction oldUpIs = upIs;
            upIs = transform.inverseTransform(upIs);
            propertyChangeSupport.firePropertyChange("upIs", oldUpIs, upIs);
        }
        changeNo++;
    }

    public void clearLayerData(Layer layer) {
        for (Dimension dimension: dimensions.values()) {
            dimension.clearLayerData(layer);
        }
    }
    
    @SuppressWarnings("unchecked")
    public int measureSize() {
        dimensions.values().forEach(org.pepsoft.worldpainter.Dimension::ensureAllReadable);
        return MemoryUtils.getSize(this, new HashSet<>(Arrays.asList(UndoManager.class, Dimension.Listener.class, PropertyChangeSupport.class, Layer.class, Terrain.class)));
    }

    /**
     * Get the set of warnings generated during loading, if any.
     * 
     * @return The set of warnings generated during loading, if any. May be
     *     <code>null</code>.
     */
    Set<Warning> getWarnings() {
        return warnings;
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        propertyChangeSupport = new PropertyChangeSupport(this);

        // Legacy maps
        if (wpVersion < 1) {
            if (maxheight == 0) {
                maxheight = 128;
            }
            if (generator == null) {
                generator = Generator.DEFAULT;
                if ((generatorName != null) && generatorName.equals("FLAT")) {
                    generator = Generator.FLAT;
                } else {
                    TileFactory tileFactory = dimensions.containsKey(0) ? dimensions.get(0).getTileFactory() : null;
                    if (tileFactory instanceof HeightMapTileFactory) {
                        if (((HeightMapTileFactory) tileFactory).getWaterHeight() < 32) {
                            // Low level
                            generator = Generator.FLAT;
                        }
                    }
                }
            }
            if (biomeAlgorithm == BIOME_ALGORITHM_CUSTOM_BIOMES) {
                customBiomes = true;
            }
            if (upIs == null) {
                upIs = Direction.WEST;
                askToRotate = true;
            }
            if ((! allowMerging) && (biomeAlgorithm != BIOME_ALGORITHM_NONE)) {
                customBiomes = false;
            }
            if (mixedMaterials == null) {
                mixedMaterials = new MixedMaterial[Terrain.CUSTOM_TERRAIN_COUNT];
                if (customMaterials != null) {
                    for (int i = 0; i < customMaterials.length; i++) {
                        if (customMaterials[i] != null) {
                            mixedMaterials[i] = MixedMaterial.create(customMaterials[i]);
                        }
                    }
                    customMaterials = null;
                }
            }
            
            Dimension dim = dimensions.get(DIM_NORMAL);
            if (dim != null) {
                // Migrate to refactored automatic biomes
                if (biomeAlgorithm == BIOME_ALGORITHM_AUTO_BIOMES) {
                    // Automatic biomes was enabled; biome information should
                    // be present throughout; no need to do anything. Schedule a
                    // warning to warn the user about the change though
                    warnings = EnumSet.of(Warning.AUTO_BIOMES_DISABLED);
                } else if (customBiomes) {
                    // Custom biomes was enabled; a mix of initialised and
                    // uninitialised tiles may be present which would be
                    // problematic, as the initialised tiles would have been
                    // initialised to zero and the default is now 255. Check what
                    // the situation is and take appropriate steps
                    boolean tilesWithBiomesFound = false, tilesWithoutBiomesFound = false;
                    for (Tile tile: dim.getTiles()) {
                        if (tile.hasLayer(Biome.INSTANCE)) {
                            tilesWithBiomesFound = true;
                        } else {
                            tilesWithoutBiomesFound = true;
                        }
                    }
                    if (tilesWithBiomesFound) {
                        if (tilesWithoutBiomesFound) {
                            // There is a mix of initialised and uninitialiased
                            // tiles. Initialise the uninitialised ones to zero.
                            // No need to warn the user as there is no change in
                            // behaviour
                            for (Tile tile: dim.getTiles()) {
                                if (! tile.hasLayer(Biome.INSTANCE)) {
                                    for (int x = 0; x < TILE_SIZE; x++) {
                                        for (int y = 0; y < TILE_SIZE; y++) {
                                            tile.setLayerValue(Biome.INSTANCE, x, y, 0);
                                        }
                                    }
                                }
                            }
                        } else {
                            // There are only initialised tiles. Good, we can leave
                            // it like that, and don't have to warn the user, as the
                            // behaviour will not change
                        }
                    } else if (tilesWithoutBiomesFound) {
                        // There are only uninitialised tiles. Leave it like that,
                        // but warn the user that automatic biomes is now active
                        warnings = EnumSet.of(Warning.AUTO_BIOMES_ENABLED);
                    }
                } else {
                    // Neither custom nor automatic biomes was enabled; all
                    // tiles *should* be uninitialised, but clear the layer data
                    // anyway just to make sure. Schedule a warning to warn the user
                    // that automatic biomes are now active
                    dim.clearLayerData(Biome.INSTANCE);
                    warnings = EnumSet.of(Warning.AUTO_BIOMES_ENABLED);
                }
            }
        }
        if (wpVersion < 2) {
            if (gameType == 3) {
                difficulty = org.pepsoft.minecraft.Constants.DIFFICULTY_HARD;
            } else {
                difficulty = org.pepsoft.minecraft.Constants.DIFFICULTY_NORMAL;
            }
        }
        if (wpVersion < 3) {
            history = new ArrayList<>();
            history.add(new HistoryEntry(HistoryEntry.WORLD_LEGACY_PRE_2_0_0));
        }
        if (wpVersion < 4) {
            borderSettings = new BorderSettings();
        }
        if (wpVersion < 5) {
            if (tilesToExport != null) {
                dimensionsToExport = Collections.singleton(dimensionToExport);
            } else {
                dimensionsToExport = null;
            }
            dimensionToExport = -1;
        }
        if (wpVersion < 6) {
            switch (version) {
                case org.pepsoft.minecraft.Constants.SUPPORTED_VERSION_1:
                    platform = DefaultPlugin.JAVA_MCREGION;
                    break;
                case org.pepsoft.minecraft.Constants.SUPPORTED_VERSION_2:
                    platform = DefaultPlugin.JAVA_ANVIL;
                    break;
                default:
                    platform = (maxheight == org.pepsoft.minecraft.Constants.DEFAULT_MAX_HEIGHT_2) ? DefaultPlugin.JAVA_ANVIL : DefaultPlugin.JAVA_MCREGION;
            }
            version = -1;
            gameTypeObj = GameType.values()[gameType];
            gameType = -1;
        }
        wpVersion = CURRENT_WP_VERSION;
        
        // Bug fix: fix the maxHeight of the dimensions, which somehow is not
        // always correctly set (possibly only on imported worlds from
        // non-standard height maps due to a bug which should be fixed).
        dimensions.values().stream()
            .filter(dimension -> (dimension.getMaxHeight() != maxheight) && (dimension.getMaxHeight() != 0))
            .forEach(dimension -> {
                logger.warn("Fixing maxHeight of dimension " + dimension.getDim() + " (was " + dimension.getMaxHeight() + ", should be " + maxheight + ")");
                dimension.setMaxHeight(maxheight);
            });
        
        // The number of custom terrains increases now and again; correct old
        // worlds for it
        if (mixedMaterials.length != Terrain.CUSTOM_TERRAIN_COUNT) {
            mixedMaterials = Arrays.copyOf(mixedMaterials, Terrain.CUSTOM_TERRAIN_COUNT);
        }
    }

    private String name = "Generated World";
    private boolean createGoodiesChest = true;
    private Point spawnPoint = new Point(0, 0);
    private File importedFrom;
    private final SortedMap<Integer, Dimension> dimensions = new TreeMap<>();
    private boolean mapFeatures = true;
    @Deprecated
    private int gameType;
    @Deprecated
    private Material[] customMaterials;
    @Deprecated
    private int biomeAlgorithm = -1;
    private int maxheight = DEFAULT_MAX_HEIGHT; // Typo, but there are already worlds in the wild with this, so leave it
    private transient PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);
    @Deprecated
    private String generatorName;
    @Deprecated
    private int version;
    /**
     * The type of terrain generator to choose.
     */
    private Generator generator = Generator.DEFAULT;
    private boolean dontAskToConvertToAnvil = true;
    private boolean customBiomes;
    @Deprecated
    private int dimensionToExport;
    private Set<Point> tilesToExport;
    private boolean askToRotate, allowMerging = true;
    private Direction upIs = Direction.NORTH;
    private boolean allowCheats;
    private String generatorOptions;
    private MixedMaterial[] mixedMaterials = new MixedMaterial[Terrain.CUSTOM_TERRAIN_COUNT];
    private boolean extendedBlockIds;
    private int wpVersion = CURRENT_WP_VERSION;
    private int difficulty = org.pepsoft.minecraft.Constants.DIFFICULTY_NORMAL;
    private List<HistoryEntry> history = new ArrayList<>();
    private BorderSettings borderSettings = new BorderSettings();
    private File mergedWith;
    private Set<Integer> dimensionsToExport;
    private Platform platform;
    private GameType gameTypeObj = GameType.SURVIVAL;
    private Map<String, Object> attributes;
    private transient Set<Warning> warnings;
    private transient Map<String, Object> metadata;
    private transient long changeNo;

    @Deprecated
    public static final int BIOME_ALGORITHM_NONE                = -1;
    @Deprecated
    public static final int BIOME_ALGORITHM_CUSTOM_BIOMES       =  6;
    @Deprecated
    public static final int BIOME_ALGORITHM_AUTO_BIOMES         =  7; 
    
    public static final int DEFAULT_MAX_HEIGHT = org.pepsoft.minecraft.Constants.DEFAULT_MAX_HEIGHT_2;
    public static final long DEFAULT_OCEAN_SEED = 27594263L; // A seed with a large ocean around the origin, and not many mushroom islands nearby. Should be used with Large Biomes
    public static final long DEFAULT_LAND_SEED = 227290L; // A seed with a huge continent around the origin. Should be used with Large Biomes
    
    /**
     * A {@link String} containing the WorldPainter version with which this file
     * was saved.
     */
    public static final String METADATA_KEY_WP_VERSION = "org.pepsoft.worldpainter.wp.version";

    /**
     * A {@link String} containing the WorldPainter build with which this file
     * was saved.
     */
    public static final String METADATA_KEY_WP_BUILD = "org.pepsoft.worldpainter.wp.build";

    /**
     * A {@link Date} containing the time at which this file was saved.
     */
    public static final String METADATA_KEY_TIMESTAMP = "org.pepsoft.worldpainter.timestamp";

    /**
     * An optional two dimensional {@link String}[][2] array containing the
     * plugins installed in the WorldPainter instance which saved the file. One
     * row per plugin, which each row containing two elements, the first being
     * the plugin name, the second being the plugin version.
     *
     * <p>May be <code>null</code> if no non-standard plugins were present.
     */
    public static final String METADATA_KEY_PLUGINS = "org.pepsoft.worldpainter.plugins";

    private static final int CURRENT_WP_VERSION = 6;

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(World2.class);
    private static final long serialVersionUID = 2011062401L;

    enum Warning {AUTO_BIOMES_ENABLED, AUTO_BIOMES_DISABLED}
    
    public static class BorderSettings implements Serializable, org.pepsoft.util.undo.Cloneable<BorderSettings> {
        public int getCentreX() {
            return centreX;
        }

        public void setCentreX(int centreX) {
            if (centreX != this.centreX) {
                this.centreX = centreX;
                changeNo++;
            }
        }

        public int getCentreY() {
            return centreY;
        }

        public void setCentreY(int centreY) {
            if (centreY != this.centreY) {
                this.centreY = centreY;
                changeNo++;
            }
        }

        public int getSize() {
            return size;
        }

        public void setSize(int size) {
            if (size != this.size) {
                this.size = size;
                changeNo++;
            }
        }

        public int getSafeZone() {
            return safeZone;
        }

        public void setSafeZone(int safeZone) {
            if (safeZone != this.safeZone) {
                this.safeZone = safeZone;
                changeNo++;
            }
        }

        public int getWarningBlocks() {
            return warningBlocks;
        }

        public void setWarningBlocks(int warningBlocks) {
            if (warningBlocks != this.warningBlocks) {
                this.warningBlocks = warningBlocks;
                changeNo++;
            }
        }

        public int getWarningTime() {
            return warningTime;
        }

        public void setWarningTime(int warningTime) {
            if (warningTime != this.warningTime) {
                this.warningTime = warningTime;
                changeNo++;
            }
        }

        public int getSizeLerpTarget() {
            return sizeLerpTarget;
        }

        public void setSizeLerpTarget(int sizeLerpTarget) {
            if (sizeLerpTarget != this.sizeLerpTarget) {
                this.sizeLerpTarget = sizeLerpTarget;
                changeNo++;
            }
        }

        public int getSizeLerpTime() {
            return sizeLerpTime;
        }

        public void setSizeLerpTime(int sizeLerpTime) {
            if (sizeLerpTime != this.sizeLerpTime) {
                this.sizeLerpTime = sizeLerpTime;
                changeNo++;
            }
        }

        public float getDamagePerBlock() {
            return damagePerBlock;
        }

        public void setDamagePerBlock(float damagePerBlock) {
            if (damagePerBlock != this.damagePerBlock) {
                this.damagePerBlock = damagePerBlock;
                changeNo++;
            }
        }

        public long getChangeNo() {
            return changeNo;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            BorderSettings that = (BorderSettings) o;

            if (centreX != that.centreX) return false;
            if (centreY != that.centreY) return false;
            if (size != that.size) return false;
            if (safeZone != that.safeZone) return false;
            if (warningBlocks != that.warningBlocks) return false;
            if (warningTime != that.warningTime) return false;
            if (sizeLerpTarget != that.sizeLerpTarget) return false;
            if (sizeLerpTime != that.sizeLerpTime) return false;
            return Float.compare(that.damagePerBlock, damagePerBlock) == 0;
        }

        @Override
        public int hashCode() {
            int result = centreX;
            result = 31 * result + centreY;
            result = 31 * result + size;
            result = 31 * result + safeZone;
            result = 31 * result + warningBlocks;
            result = 31 * result + warningTime;
            result = 31 * result + sizeLerpTarget;
            result = 31 * result + sizeLerpTime;
            result = 31 * result + (damagePerBlock != +0.0f ? Float.floatToIntBits(damagePerBlock) : 0);
            return result;
        }

        @Override
        public BorderSettings clone() {
            try {
                return (BorderSettings) super.clone();
            } catch (CloneNotSupportedException e) {
                throw new InternalError();
            }
        }

        private int centreX, centreY, size = 60000000, safeZone = 5, warningBlocks = 5, warningTime = 15, sizeLerpTarget = 60000000, sizeLerpTime;
        private float damagePerBlock = 0.2f;
        private transient long changeNo;
        
        private static final long serialVersionUID = 1L;
    }
}