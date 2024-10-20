/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.worldpainter;

import org.jnbt.CompoundTag;
import org.jnbt.XMLTransformer;
import org.pepsoft.minecraft.*;
import org.pepsoft.util.*;
import org.pepsoft.util.undo.UndoManager;
import org.pepsoft.worldpainter.Dimension.Anchor;
import org.pepsoft.worldpainter.exporting.WorldExportSettings;
import org.pepsoft.worldpainter.history.HistoryEntry;
import org.pepsoft.worldpainter.layers.Biome;
import org.pepsoft.worldpainter.layers.CustomLayer;
import org.pepsoft.worldpainter.layers.Layer;
import org.pepsoft.worldpainter.layers.tunnel.TunnelLayer;

import java.awt.*;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.*;
import java.util.List;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static com.google.common.collect.ImmutableSortedSet.toImmutableSortedSet;
import static java.util.Comparator.comparingInt;
import static java.util.Objects.requireNonNull;
import static org.pepsoft.minecraft.Material.WOOL_MAGENTA;
import static org.pepsoft.worldpainter.Constants.*;
import static org.pepsoft.worldpainter.Dimension.Anchor.*;
import static org.pepsoft.worldpainter.Dimension.Role.DETAIL;
import static org.pepsoft.worldpainter.Generator.END;
import static org.pepsoft.worldpainter.Generator.NETHER;
import static org.pepsoft.worldpainter.World2.Warning.MISSING_CUSTOM_TERRAINS;
import static org.pepsoft.worldpainter.biomeschemes.Minecraft1_7Biomes.BIOME_PLAINS;
import static org.pepsoft.worldpainter.layers.tunnel.TunnelLayer.LayerMode.FLOATING;

/**
 *
 * @author pepijn
 */
public class World2 extends InstanceKeeper implements Serializable, Cloneable {
    public World2(Platform platform, int minHeight, int maxHeight) {
        if (platform == null) {
            throw new NullPointerException();
        } else if ((minHeight < platform.minMinHeight) || (minHeight > platform.maxMinHeight)) {
            throw new IllegalArgumentException("minHeight " + minHeight + " outside platform " + platform.displayName + " minHeight limits (" + platform.minMinHeight + " - " + platform.maxMinHeight + ")");
        } else if ((maxHeight < platform.minMaxHeight) || (maxHeight > platform.maxMaxHeight)) {
            throw new IllegalArgumentException("maxHeight " + maxHeight + " outside platform " + platform.displayName + " maxHeight limits (" + platform.minMaxHeight + " - " + platform.maxMaxHeight + ")");
        }
        this.platform = platform;
        this.minHeight = minHeight;
        this.maxheight = maxHeight;
    }
    
    public World2(Platform platform, long minecraftSeed, TileFactory tileFactory) {
        if (platform == null) {
            throw new NullPointerException();
        } else if ((tileFactory.getMinHeight() < platform.minMinHeight) || (tileFactory.getMinHeight() > platform.maxMinHeight)) {
            throw new IllegalArgumentException("tileFactory.minHeight " + tileFactory.getMinHeight() + " < " + platform.minMinHeight + " or > " + platform.maxMinHeight);
        } else if ((tileFactory.getMaxHeight() < platform.minMaxHeight) || (tileFactory.getMaxHeight() > platform.maxMaxHeight)) {
            throw new IllegalArgumentException("tileFactory.maxHeight " + tileFactory.getMaxHeight() + " < " + platform.minMaxHeight + " or > " + platform.maxMaxHeight);
        }
        this.platform = platform;
        this.minHeight = tileFactory.getMinHeight();
        this.maxheight = tileFactory.getMaxHeight();
        Dimension dim = new Dimension(this, "Surface", minecraftSeed, tileFactory, NORMAL_DETAIL);
        addDimension(dim);
    }
    
    public long getChangeNo() {
        long totalChangeNo = changeNo + borderSettings.getChangeNo();
        for (Dimension dimension: dimensionsByAnchor.values()) {
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
        requireNonNull(name);
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

    /**
     * The {@code level.dat} file of the map this world was Imported from.
     */
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

    /**
     * @deprecated Use {@link #getDimension(Anchor)}.
     */
    @Deprecated
    public Dimension getDimension(int dim) {
        switch (dim) {
            case -3:
                return getDimension(END_DETAIL_CEILING);
            case -2:
                return getDimension(NETHER_DETAIL_CEILING);
            case -1:
                return getDimension(NORMAL_DETAIL_CEILING);
            case DIM_NORMAL:
                return getDimension(NORMAL_DETAIL);
            case DIM_NETHER:
                return getDimension(NETHER_DETAIL);
            case DIM_END:
                return getDimension(END_DETAIL);
            default:
                return null;
        }
    }

    public boolean isDimensionPresent(Anchor anchor) {
        return dimensionsByAnchor.containsKey(anchor);
    }

    public Dimension getDimension(Anchor anchor) {
        return dimensionsByAnchor.get(anchor);
    }

    public Set<Dimension> getDimensions() {
        return new HashSet<>(dimensionsByAnchor.values());
    }

    public Set<Dimension> getDimensionsWithRole(Dimension.Role role, boolean inverted, int id) {
        return dimensionsByAnchor.values().stream()
                .filter(dimension -> {
                    final Anchor anchor = dimension.getAnchor();
                    return (anchor.role == role) && (anchor.invert == inverted) && (anchor.id == id);
                }).collect(toImmutableSortedSet(comparingInt(dimension -> dimension.getAnchor().dim)));
    }

    public final void addDimension(Dimension dimension) {
        if (dimensionsByAnchor.containsKey(dimension.getAnchor())) {
            throw new IllegalStateException("Dimension " + dimension.getAnchor() + " already exists");
        } else if (dimension.getMinHeight() < minHeight) {
            throw new IllegalStateException("Dimension has lower min height (" + dimension.getMinHeight() + ") than world (" + minHeight + ")");
        } else if (dimension.getMaxHeight() > maxheight) {
            throw new IllegalStateException("Dimension has higher max height (" + dimension.getMaxHeight() + ") than world (" + maxheight + ")");
        } else {
            if (dimension.getWorld() != this) {
                throw new IllegalArgumentException("Dimension belongs to another world");
            }
            dimensionsByAnchor.put(dimension.getAnchor(), dimension);
            if (dimension.getAnchor().dim == DIM_NORMAL) {
                TileFactory tileFactory = dimension.getTileFactory();
                if (tileFactory instanceof HeightMapTileFactory) {
                    if (((HeightMapTileFactory) tileFactory).getWaterHeight() < 32) {
                        // Low level
                        generator = Generator.FLAT;
                    }
                }
            }
        }
        if (! dimension.getAnchor().equals(NORMAL_DETAIL)) {
            history.add(new HistoryEntry(HistoryEntry.WORLD_DIMENSION_ADDED, dimension.getName()));
        }
    }

    public Dimension removeDimension(Anchor anchor) {
        if (dimensionsByAnchor.containsKey(anchor)) {
            changeNo++;
            Dimension dimension = dimensionsByAnchor.remove(anchor);
            history.add(new HistoryEntry(HistoryEntry.WORLD_DIMENSION_REMOVED, dimension.getName()));
            return dimension;
        } else {
            throw new IllegalStateException("Dimension " + anchor + " does not exist");
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

    public int getMinHeight() {
        return minHeight;
    }

    public void setMinHeight(int minHeight) {
        if (minHeight != this.minHeight) {
            final int oldMinHeight = this.minHeight;
            this.minHeight = minHeight;
            changeNo++;
            propertyChangeSupport.firePropertyChange("minHeight", oldMinHeight, minHeight);
        }
    }

    public int getMaxHeight() {
        return maxheight;
    }

    public void setMaxHeight(int maxHeight) {
        if (maxHeight != this.maxheight) {
            final int oldMaxHeight = this.maxheight;
            this.maxheight = maxHeight;
            changeNo++;
            propertyChangeSupport.firePropertyChange("maxHeight", oldMaxHeight, maxHeight);
        }
    }

    /**
     * Returns the generator type setting of the surface dimension.
     *
     * @deprecated Use {@link Dimension#getGenerator()}.
     */
    @Deprecated
    public Generator getGenerator() {
        return dimensionsByAnchor.get(NORMAL_DETAIL).getGenerator().getType();
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
            changeNo++;
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

    /**
     * Returns the custom generator name setting of the surface dimension, or {@code null} if the surface dimension
     * generator is not a custom generator.
     *
     * @deprecated Use {@link Dimension#getGenerator()}.
     */
    @Deprecated
    public String getGeneratorOptions() {
        return (dimensionsByAnchor.get(NORMAL_DETAIL).getGenerator() instanceof CustomGenerator)
                ? ((CustomGenerator) dimensionsByAnchor.get(NORMAL_DETAIL).getGenerator()).getName()
                : null;
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

    public WorldExportSettings getExportSettings() {
        return exportSettings;
    }

    public void setExportSettings(WorldExportSettings exportSettings) {
        if (! Objects.equals(exportSettings, this.exportSettings)) {
            final WorldExportSettings oldExportSettings = this.exportSettings;
            this.exportSettings = exportSettings;
            changeNo++;
            propertyChangeSupport.firePropertyChange("exportSettings", oldExportSettings, exportSettings);
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

    /**
     * The {@code level.dat} file of the map with which this world was last Merged.
     */
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

    public List<File> getDataPacks() {
        return dataPacks;
    }

    public void setDataPacks(List<File> dataPacks) {
        if (! Objects.equals(dataPacks, this.dataPacks)) {
            final List<File> oldDataPacks = this.dataPacks;
            this.dataPacks = dataPacks;
            changeNo++;
            propertyChangeSupport.firePropertyChange("dataPacks", oldDataPacks, dataPacks);
        }
    }

    public Anchor getSpawnPointDimension() {
        return spawnPointDimension;
    }

    public void setSpawnPointDimension(Anchor spawnPointDimension) {
        if (! Objects.equals(spawnPointDimension, this.spawnPointDimension)) {
            final Anchor oldSpawnPointDimension = this.spawnPointDimension;
            this.spawnPointDimension = spawnPointDimension;
            changeNo++;
            propertyChangeSupport.firePropertyChange("spawnPointDimension", oldSpawnPointDimension, spawnPointDimension);
        }
    }

    public <T> Optional<T> getAttribute(AttributeKey<T> key) {
        return Optional.ofNullable(
                (attributes != null)
                        ? (attributes.containsKey(key.key) ? (T) attributes.get(key.key) : key.defaultValue)
                        : key.defaultValue);
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
        int dimCount = dimensionsByAnchor.size(), dim = 0;
        for (Dimension dimension: dimensionsByAnchor.values()) {
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
     * @param anchor The anchor of the dimension to transform.
     * @param transform The coordinate transform to apply.
     * @param progressReceiver A progress receiver which will be informed of
     *     rotation progress.
     */
    public void transform(Anchor anchor, CoordinateTransform transform, ProgressReceiver progressReceiver) throws ProgressReceiver.OperationCancelled {
        dimensionsByAnchor.get(anchor).transform(transform, progressReceiver);
        if (anchor.equals(NORMAL_DETAIL)) {
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
        for (Dimension dimension: dimensionsByAnchor.values()) {
            dimension.clearLayerData(layer);
        }
    }
    
    public long measureSize() {
        dimensionsByAnchor.values().forEach(org.pepsoft.worldpainter.Dimension::ensureAllReadable);
        return MemoryUtils.getSize(this, new HashSet<>(Arrays.asList(UndoManager.class, Dimension.Listener.class, PropertyChangeSupport.class, Layer.class, Terrain.class)));
    }

    public synchronized void save(ZipOutputStream out) throws IOException {
        // First serialise everything but the dimensions to a separate file
        out.putNextEntry(new ZipEntry("world-data.bin"));
        try {
            final Map<Anchor, Dimension> savedDimensions = dimensionsByAnchor;
            try {
                dimensionsByAnchor = null;
                final ObjectOutputStream dataout = new ObjectOutputStream(out);
                dataout.writeObject(this);
                dataout.flush();
            } finally {
                dimensionsByAnchor = savedDimensions;
            }
        } finally {
            out.closeEntry();
        }

        // Then serialise the dimensions individually
        for (Dimension dimension: dimensionsByAnchor.values()) {
            dimension.save(out);
        }
    }

    /**
     * Get the set of warnings generated during loading, if any.
     * 
     * @return The set of warnings generated during loading, if any. May be
     *     {@code null}.
     */
    Set<Warning> getWarnings() {
        return warnings;
    }

    private void addWarning(Warning warning) {
        if (warnings == null) {
            warnings = EnumSet.of(warning);
        } else {
            warnings.add(warning);
        }
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        // We keep having difficulties on Windows with Files being Windows- specific subclasses of File which don't
        // serialise correctly and end up being null somehow. Work around the problem by making sure all Files are
        // actually java.io.Files
        dataPacks = FileUtils.absolutise(dataPacks);

        out.defaultWriteObject();
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        propertyChangeSupport = new PropertyChangeSupport(this);

        if (dimensionsByAnchor == null) {
            dimensionsByAnchor = new HashMap<>();
        }

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
                            mixedMaterials[i] = MixedMaterial.create(DefaultPlugin.JAVA_ANVIL, customMaterials[i]);
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
                    addWarning(Warning.AUTO_BIOMES_DISABLED);
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
                        addWarning(Warning.AUTO_BIOMES_ENABLED);
                    }
                } else {
                    // Neither custom nor automatic biomes was enabled; all
                    // tiles *should* be uninitialised, but clear the layer data
                    // anyway just to make sure. Schedule a warning to warn the user
                    // that automatic biomes are now active
                    dim.clearLayerData(Biome.INSTANCE);
                    addWarning(Warning.AUTO_BIOMES_ENABLED);
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
                case org.pepsoft.minecraft.Constants.VERSION_MCREGION:
                    platform = DefaultPlugin.JAVA_MCREGION;
                    break;
                case org.pepsoft.minecraft.Constants.VERSION_ANVIL:
                    platform = DefaultPlugin.JAVA_ANVIL;
                    break;
                default:
                    platform = (maxheight == org.pepsoft.minecraft.Constants.DEFAULT_MAX_HEIGHT_ANVIL) ? DefaultPlugin.JAVA_ANVIL : DefaultPlugin.JAVA_MCREGION;
            }
            version = -1;
            if (gameType == -1) {
                // No idea how this can happen, but it has been observed in the wild
                addWarning(Warning.GAME_TYPE_RESET);
                gameTypeObj = GameType.SURVIVAL;
            } else {
                gameTypeObj = GameType.values()[gameType];
                gameType = -1;
            }
        }
        if (wpVersion < 7) {
            if ((generatorOptions != null) && (! generatorOptions.trim().isEmpty())) {
                // Is it in version 3 string format?
                try {
                    superflatPreset = SuperflatPreset.fromMinecraft1_12_2(generatorOptions.trim());
                    generatorOptions = null;
                } catch (IllegalArgumentException e) {
                    // Is it in XML format from the alpha/beta?
                    try {
                        CompoundTag tag = (CompoundTag) XMLTransformer.fromXML(new StringReader(generatorOptions));
                        superflatPreset = SuperflatPreset.fromMinecraft1_15_2(tag);
                        generatorOptions = null;
                    } catch (ClassCastException | IOException e2) {
                        // It's not recognisable; give up
                    }
                }
            }
        }
        if (wpVersion < 8) {
            // Check for missing custom terrain types and warn/repair
            BitSet customTerrainsInUse = new BitSet();
            for (Dimension dimension: dimensions.values()) {
                // If this object is being deserialized because it is referred to from a Dimension (which happens when
                // deserializing the Configuration), then dimension.tiles is not initialised yet and will be null.
                // getAllTerrains() will return an empty set in that case, which is fine, because such a world could not
                // contain any missing custom terrains
                for (Terrain terrain: dimension.getAllTerrains()) {
                    if (terrain.isCustom()) {
                        customTerrainsInUse.set(terrain.getCustomTerrainIndex());
                    }
                }
            }
            customTerrainsInUse.stream().forEach(index -> {
                if (getMixedMaterial(index) == null) {
                    logger.error("Mixed material {} was missing; replaced with magenta wool", index);
                    MixedMaterial replacementMaterial = new MixedMaterial("Missing Custom Terrain " + index, new MixedMaterial.Row(WOOL_MAGENTA, 1, 1.0f), BIOME_PLAINS, null);
                    setMixedMaterial(index, replacementMaterial);
                    addWarning(MISSING_CUSTOM_TERRAINS);
                }
            });
        }
        if (wpVersion < 9) {
            dimensions.values().forEach(dimension -> {
                switch (dimension.getAnchor().dim) {
                    case DIM_NORMAL:
                        MapGenerator generator = MapGenerator.fromLegacySettings(this.generator, dimension.getMinecraftSeed(), generatorName, generatorOptions, platform, this::addWarning);
                        dimension.setGenerator(generator);
                        break;
                    case DIM_NETHER:
                        dimension.setGenerator(new SeededGenerator(NETHER, dimension.getSeed()));
                        break;
                    case DIM_END:
                        dimension.setGenerator(new SeededGenerator(END, dimension.getSeed()));
                        break;
                }
            });
            this.generator = null;
            generatorName = null;
            generatorOptions = null;
        }
        if (wpVersion < 10) {
            dimensions.values().forEach(dimension -> dimensionsByAnchor.put(dimension.getAnchor(), dimension));
            dimensions = null;
        }
        if (wpVersion < 11) {
            if (((dimensionsToExport != null) && (! dimensionsToExport.isEmpty())) || ((tilesToExport != null) && (! tilesToExport.isEmpty()))) {
                exportSettings = new WorldExportSettings(((dimensionsToExport != null) && (! dimensionsToExport.isEmpty())) ? dimensionsToExport : null,
                        ((tilesToExport != null) && (! tilesToExport.isEmpty())) ? tilesToExport : null,
                        null);
            }
            dimensionsToExport = null;
            tilesToExport = null;
        }
        if (wpVersion < 12) {
            minHeight = platform.minZ;
        }
        if (wpVersion < 13) {
            // Floating dimensions got their own anchor.role
            final Set<Dimension> dimensionsToAdd = new HashSet<>();
            for (Iterator<Map.Entry<Anchor, Dimension>> i = dimensionsByAnchor.entrySet().iterator(); i.hasNext(); ) {
                final Map.Entry<Anchor, Dimension> entry = i.next();
                if (entry.getKey() == null) {
                    // If the dimension in question is Configuration.defaultTerrainAndLayerSettings then that dimension
                    // and its World2 might still be being deserialised at this point, meaning that entry.key is still
                    // null, as is dimension.anchor. That dimension is never accessed via its anchor, so we just leave
                    // it like that
                    continue;
                }
                if (entry.getKey().role == Dimension.Role.CAVE_FLOOR) {
                    final Dimension dimension = entry.getValue();
                    if (findFloatingLayer(dimension) != null) {
                        i.remove();
                        dimension.changeAnchorToFloatingFloor();
                        dimensionsToAdd.add(dimension);
                    }
                }
            }
            dimensionsToAdd.forEach(dimension -> dimensionsByAnchor.put(dimension.getAnchor(), dimension));
        }
        wpVersion = CURRENT_WP_VERSION;

        // The number of custom terrains increases now and again; correct old
        // worlds for it
        if (mixedMaterials.length != Terrain.CUSTOM_TERRAIN_COUNT) {
            mixedMaterials = Arrays.copyOf(mixedMaterials, Terrain.CUSTOM_TERRAIN_COUNT);
        }
    }

    private TunnelLayer findFloatingLayer(Dimension caveFloorDimension) {
        final Anchor floorAnchor = caveFloorDimension.getAnchor();
        for (CustomLayer layer: dimensionsByAnchor.get(new Anchor(floorAnchor.dim, DETAIL, floorAnchor.invert, 0)).getCustomLayers()) {
            if ((layer instanceof TunnelLayer)
                    && (((TunnelLayer) layer).getLayerMode() == FLOATING)
                    && (((TunnelLayer) layer).getFloorDimensionId() != null)
                    && (((TunnelLayer) layer).getFloorDimensionId() == floorAnchor.id)) {
                return (TunnelLayer) layer;
            }
        }
        return null;
    }

    private String name = "Generated World";
    private boolean createGoodiesChest = true;
    private Point spawnPoint = new Point(0, 0);
    private File importedFrom;
    @Deprecated
    private SortedMap<Integer, Dimension> dimensions;
    private boolean mapFeatures = true;
    @Deprecated
    private int gameType;
    @Deprecated
    private Material[] customMaterials;
    @Deprecated
    private int biomeAlgorithm = -1;
    private int maxheight; // Typo, but there are already worlds in the wild with this, so leave it
    private transient PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);
    @Deprecated
    private String generatorName;
    @Deprecated
    private int version;
    @Deprecated
    private Generator generator;
    private boolean dontAskToConvertToAnvil = true;
    private boolean customBiomes;
    @Deprecated
    private int dimensionToExport;
    @Deprecated
    private Set<Point> tilesToExport;
    private boolean askToRotate, allowMerging = true;
    private Direction upIs = Direction.NORTH;
    private boolean allowCheats;
    @Deprecated
    private String generatorOptions;
    private MixedMaterial[] mixedMaterials = new MixedMaterial[Terrain.CUSTOM_TERRAIN_COUNT];
    private boolean extendedBlockIds;
    private int wpVersion = CURRENT_WP_VERSION;
    private int difficulty = org.pepsoft.minecraft.Constants.DIFFICULTY_NORMAL;
    private List<HistoryEntry> history = new ArrayList<>();
    private BorderSettings borderSettings = new BorderSettings();
    private File mergedWith;
    @Deprecated
    private Set<Integer> dimensionsToExport;
    private Platform platform;
    private GameType gameTypeObj = GameType.SURVIVAL;
    private Map<String, Object> attributes;
    @Deprecated
    private SuperflatPreset superflatPreset;
    private Map<Anchor, Dimension> dimensionsByAnchor = new HashMap<>();
    private WorldExportSettings exportSettings;
    private List<File> dataPacks;
    private int minHeight;
    private Anchor spawnPointDimension;
    private transient Set<Warning> warnings;
    private transient Map<String, Object> metadata;
    private transient long changeNo;

    @Deprecated
    public static final int BIOME_ALGORITHM_NONE                = -1;
    @Deprecated
    public static final int BIOME_ALGORITHM_CUSTOM_BIOMES       =  6;
    @Deprecated
    public static final int BIOME_ALGORITHM_AUTO_BIOMES         =  7; 
    
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
     * <p>May be {@code null} if no non-standard plugins were present.
     */
    public static final String METADATA_KEY_PLUGINS = "org.pepsoft.worldpainter.plugins";

    /**
     * A string containing the name of the world.
     */
    public static final String METADATA_KEY_NAME = "name";

    private static final int CURRENT_WP_VERSION = 13;

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(World2.class);
    private static final long serialVersionUID = 2011062401L;

    public enum Warning {
        /**
         * Warn the user that automatic biomes are now the default and are enabled.
         */
        AUTO_BIOMES_ENABLED,

        /**
         * Warn the user that automatic biomes were previously in use but are now disabled.
         */
        AUTO_BIOMES_DISABLED,

        /**
         * Warn the user that one or more custom terrain types were missing and have been replaced with magenta wool.
         */
        MISSING_CUSTOM_TERRAINS,

        /**
         * Warn the user that the Superflat settings could not be parsed and were reset to defaults.
         */
        SUPERFLAT_SETTINGS_RESET,

        /**
         * The game type was lost and was reset to Survival.
         */
        GAME_TYPE_RESET
    }
    
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