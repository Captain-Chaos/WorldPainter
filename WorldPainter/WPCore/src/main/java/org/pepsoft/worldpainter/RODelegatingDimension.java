/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter;

import org.pepsoft.util.ProgressReceiver;
import org.pepsoft.util.ProgressReceiver.OperationCancelled;
import org.pepsoft.util.undo.UndoManager;
import org.pepsoft.worldpainter.biomeschemes.CustomBiome;
import org.pepsoft.worldpainter.gardenofeden.Garden;
import org.pepsoft.worldpainter.layers.Layer;
import org.pepsoft.worldpainter.layers.exporters.ExporterSettings;

import java.awt.*;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.util.List;
import java.util.*;

/**
 * A read-only implementation of {@link Dimension} which wraps another
 * {@code Dimension} and delegates all read  calls to it. All write calls throw
 * an {@link UnsupportedOperationException}.
 *
 * <p>In addition the methods which return tiles wrap the returned tiles in
 * {@link RODelegatingTile}s.
 *
 * <p>Intended to change the behaviour of one or more read calls by overriding
 * them.
 *
 * @author pepijn
 */
public abstract class RODelegatingDimension<T extends Tile> extends Dimension {
    public RODelegatingDimension(Dimension dimension) {
        super(dimension.getWorld(), dimension.getMinecraftSeed(), dimension.getTileFactory(), dimension.getAnchor());
        this.dimension = dimension;
    }

    @Override
    public World2 getWorld() {
        return dimension.getWorld();
    }

    @Override
    public Anchor getAnchor() {
        return dimension.getAnchor();
    }

    @Override
    public String getName() {
        return dimension.getName();
    }

    @Override
    public long getChangeNo() {
        return dimension.getChangeNo();
    }

    @Override
    public long getSeed() {
        return dimension.getSeed();
    }

    @Override
    public Terrain getSubsurfaceMaterial() {
        return dimension.getSubsurfaceMaterial();
    }

    @Override
    public boolean isPopulate() {
        return dimension.isPopulate();
    }

    @Override
    public Border getBorder() {
        return dimension.getBorder();
    }

    @Override
    public int getBorderLevel() {
        return dimension.getBorderLevel();
    }

    @Override
    public int getBorderSize() {
        return dimension.getBorderSize();
    }

    @Override
    public TileFactory getTileFactory() {
        return dimension.getTileFactory();
    }

    @Override
    public int getTileCount() {
        return dimension.getTileCount();
    }

    @Override
    public void removeTile(Point coords) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeTile(int tileX, int tileY) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getIntHeightAt(int x, int y) {
        return getIntHeightAt(x, y, Integer.MIN_VALUE);
    }

    @Override
    public int getIntHeightAt(int x, int y, int defaultValue) {
        return dimension.getIntHeightAt(x, y, defaultValue);
    }
    
    @Override
    public int getIntHeightAt(Point coords) {
        return getIntHeightAt(coords.x, coords.y, Integer.MIN_VALUE);
    }

    @Override
    public float getHeightAt(int x, int y) {
        return dimension.getHeightAt(x, y);
    }

    @Override
    public float getHeightAt(Point coords) {
        return getHeightAt(coords.x, coords.y);
    }

    @Override
    public int getRawHeightAt(int x, int y) {
        return dimension.getRawHeightAt(x, y);
    }

    @Override
    public int getRawHeightAt(Point coords) {
        return getRawHeightAt(coords.x, coords.y);
    }

    @Override
    public final void setRawHeightAt(int x, int y, int rawHeight) {
        throw new UnsupportedOperationException();
    }

    @Override
    public final void setRawHeightAt(Point coords, int rawHeight) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Terrain getTerrainAt(int x, int y) {
        return super.getTerrainAt(x, y);
    }

    @Override
    public Set<Terrain> getAllTerrains() {
        return dimension.getAllTerrains();
    }

    @Override
    public int getWaterLevelAt(int x, int y) {
        return dimension.getWaterLevelAt(x, y);
    }

    @Override
    public int getWaterLevelAt(Point coords) {
        return getWaterLevelAt(coords.x, coords.y);
    }

    @Override
    public int getLayerValueAt(Layer layer, int x, int y) {
        return dimension.getLayerValueAt(layer, x, y);
    }

    @Override
    public int getLayerValueAt(Layer layer, Point coords) {
        return getLayerValueAt(layer, coords.x, coords.y);
    }

    @Override
    public boolean getBitLayerValueAt(Layer layer, int x, int y) {
        return dimension.getBitLayerValueAt(layer, x, y);
    }

    @Override
    public float getDistanceToEdge(Layer layer, int x, int y, float maxDistance) {
        return dimension.getDistanceToEdge(layer, x, y, maxDistance);
    }

    @Override
    public final void clearLayerData(Layer layer) {
        throw new UnsupportedOperationException();
    }

    @Override
    public long getMinecraftSeed() {
        return dimension.getMinecraftSeed();
    }

    @Override
    public File getOverlay() {
        return dimension.getOverlay();
    }

    @Override
    public final void setOverlay(File overlay) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getOverlayOffsetX() {
        return dimension.getOverlayOffsetX();
    }

    @Override
    public final void setOverlayOffsetX(int overlayOffsetX) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getOverlayOffsetY() {
        return dimension.getOverlayOffsetY();
    }

    @Override
    public final void setOverlayOffsetY(int overlayOffsetY) {
        throw new UnsupportedOperationException();
    }

    @Override
    public float getOverlayScale() {
        return dimension.getOverlayScale();
    }

    @Override
    public final void setOverlayScale(float overlayScale) {
        throw new UnsupportedOperationException();
    }

    @Override
    public float getOverlayTransparency() {
        return dimension.getOverlayTransparency();
    }

    @Override
    public final void setOverlayTransparency(float overlayTransparency) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isGridEnabled() {
        return dimension.isGridEnabled();
    }

    @Override
    public final void setGridEnabled(boolean gridEnabled) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getGridSize() {
        return dimension.getGridSize();
    }

    @Override
    public final void setGridSize(int gridSize) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isOverlayEnabled() {
        return dimension.isOverlayEnabled();
    }

    @Override
    public final void setOverlayEnabled(boolean overlayEnabled) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getMaxHeight() {
        return dimension.getMaxHeight();
    }

    @Override
    public final void setMaxHeight(int maxHeight) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getContourSeparation() {
        return dimension.getContourSeparation();
    }

    @Override
    public final void setContourSeparation(int contourSeparation) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isContoursEnabled() {
        return dimension.isContoursEnabled();
    }

    @Override
    public final void setContoursEnabled(boolean contoursEnabled) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getTopLayerMinDepth() {
        return dimension.getTopLayerMinDepth();
    }

    @Override
    public final void setTopLayerMinDepth(int topLayerMinDepth) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getTopLayerVariation() {
        return dimension.getTopLayerVariation();
    }

    @Override
    public final void setTopLayerVariation(int topLayerVariation) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isBottomless() {
        return dimension.isBottomless();
    }

    @Override
    public final void setBottomless(boolean bottomless) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Point getLastViewPosition() {
        return dimension.getLastViewPosition();
    }

    @Override
    public final void setLastViewPosition(Point lastViewPosition) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<CustomBiome> getCustomBiomes() {
        return dimension.getCustomBiomes();
    }

    @Override
    public final void setCustomBiomes(List<CustomBiome> customBiomes) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Garden getGarden() {
        return dimension.getGarden();
    }

    @Override
    public Set<Layer> getAllLayers(boolean applyCombinedLayers) {
        return dimension.getAllLayers(applyCombinedLayers);
    }

    @Override
    public boolean containsOneOf(Layer... layers) {
        return dimension.containsOneOf(layers);
    }

    @Override
    public Set<Layer> getMinimumLayers() {
        return dimension.getMinimumLayers();
    }

    @Override
    public WallType getWallType() {
        return dimension.getWallType();
    }

    @Override
    public final void setWallType(WallType wallType) {
        throw new UnsupportedOperationException();
    }

    @Override
    public WallType getRoofType() {
        return dimension.getRoofType();
    }

    @Override
    public final void setRoofType(WallType roofType) {
        throw new UnsupportedOperationException();
    }

    @Override
    public final boolean undoChanges() {
        return false;
    }

    @Override
    public final void clearUndo() {
        throw new UnsupportedOperationException();
    }

    @Override
    public final void clearRedo() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Dimension getSnapshot() {
        return dimension.getSnapshot();
    }

    @Override
    public int getTopLayerDepth(int x, int y, int z) {
        return dimension.getTopLayerDepth(x, y, z);
    }

    @Override
    final void ensureAllReadable() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeDimensionListener(Listener listener) {
        // Do nothing
    }

    @Override
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        // Do nothing
    }

    @Override
    public void removePropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        // Do nothing
    }

    @Override
    public final void transform(CoordinateTransform transform, ProgressReceiver progressReceiver) throws OperationCancelled {
        throw new UnsupportedOperationException();
    }

    @Override
    public void heightMapChanged(Tile tile) {
        // Do nothing
    }

    @Override
    public void terrainChanged(Tile tile) {
        // Do nothing
    }

    @Override
    public void waterLevelChanged(Tile tile) {
        // Do nothing
    }

    @Override
    public void seedsChanged(Tile tile) {
        // Do nothing
    }

    @Override
    public void layerDataChanged(Tile tile, Set<Layer> changedLayers) {
        // Do nothing
    }

    @Override
    public void allBitLayerDataChanged(Tile tile) {
        // Do nothing
    }

    @Override
    public void allNonBitlayerDataChanged(Tile tile) {
        // Do nothing
    }

    @Override
    public void addDimensionListener(Listener listener) {
        // Do nothing
    }

    @Override
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        // Do nothing
    }

    @Override
    public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        // Do nothing
    }

    @Override
    public final void addTile(Tile tile) {
        throw new UnsupportedOperationException();
    }

    @Override
    public final void applyTheme(int x, int y) {
        throw new UnsupportedOperationException();
    }

    @Override
    public final void applyTheme(Point coords) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void unregister() {
        // Do nothing
    }

    @Override
    public void armSavePoint() {
        // Do nothing
    }

    @Override
    public void register(UndoManager undoManager) {
        // Do nothing
    }

    @Override
    public boolean isUndoAvailable() {
        return false;
    }

    @Override
    public int getHeight() {
        return dimension.getHeight();
    }

    @Override
    public int getHighestX() {
        return dimension.getHighestX();
    }

    @Override
    public int getHighestY() {
        return dimension.getHighestY();
    }

    @Override
    public ExporterSettings getLayerSettings(Layer layer) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Map<Layer, ExporterSettings> getAllLayerSettings() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getLowestX() {
        return dimension.getLowestX();
    }

    @Override
    public int getLowestY() {
        return dimension.getLowestY();
    }

    @Override
    public synchronized int getFloodedCount(int x, int y, int r, boolean lava) {
        return dimension.getFloodedCount(x, y, r, lava);
    }

    @Override
    public float getSlope(int x, int y) {
        return dimension.getSlope(x, y);
    }

    @Override
    public final T getTile(int x, int y) {
        return getTile(new Point(x, y));
    }

    /**
     * If any transformations (scaling and/or translating) needs to be applied to the underlying tiles, this method
     * and {@link #getTiles} must be overridden. These implementations assume a 1:1 coordinate mapping.
     */
    @Override
    public T getTile(Point coords) {
        // In theory this is not correct, since the dimension might have gained or lost tiles in the mean time. However
        // the expected usage pattern of the functionality is such that that should not happen in practice, and creating
        // tile snapshots of all tiles when the dimension snapshot is created would be a performance hit
        final Reference<T> cachedTileRef = tileCache.get(coords);
        T cachedTile = (cachedTileRef != null) ? cachedTileRef.get() : null;
        if (cachedTile == null) {
            final Tile tile = dimension.getTile(coords);
            if (tile != null) {
                cachedTile = wrapTile(tile);
                tileCache.put(coords, new SoftReference<>(cachedTile));
            }
        }
        return cachedTile;
    }

    /**
     * If any transformations (scaling and/or translating) needs to be applied to the underlying tiles, this method
     * and {@link #getTile(Point)} must be overridden. These implementations assume a 1:1 coordinate mapping.
     */
    @Override
    public Collection<T> getTiles() {
        final Collection<? extends Tile> tiles = dimension.getTiles();
        final List<T> wrappedTiles = new ArrayList<>(tiles.size());
        for (Tile tile: tiles) {
            final Reference<T> cachedTileRef = tileCache.get(new Point(tile.getX(), tile.getY()));
            final T cachedTile = (cachedTileRef != null) ? cachedTileRef.get() : null;
            if (cachedTile != null) {
                wrappedTiles.add(cachedTile);
            } else {
                wrappedTiles.add(wrapTile(tile));
            }
        }
        return wrappedTiles;
    }

    @Override
    public Set<Point> getTileCoords() {
        return dimension.getTileCoords();
    }

    @Override
    public int getWidth() {
        return dimension.getWidth();
    }

    @Override
    public boolean isEventsInhibited() {
        return false;
    }

    @Override
    public final void removeTile(Tile tile) {
        throw new UnsupportedOperationException();
    }

    @Override
    public final void setBitLayerValueAt(Layer layer, int x, int y, boolean value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public final void setBorder(Border border) {
        throw new UnsupportedOperationException();
    }

    @Override
    public final void setBorderLevel(int borderLevel) {
        throw new UnsupportedOperationException();
    }

    @Override
    public final void setBorderSize(int borderSize) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void changed() {
        // Do nothing
    }

    @Override
    public void setEventsInhibited(boolean eventsInhibited) {
        // Do nothing
    }

    @Override
    public final void setHeightAt(int x, int y, float height) {
        throw new UnsupportedOperationException();
    }

    @Override
    public final void setHeightAt(Point coords, float height) {
        throw new UnsupportedOperationException();
    }

    @Override
    public final void setLayerSettings(Layer layer, ExporterSettings settings) {
        throw new UnsupportedOperationException();
    }

    @Override
    public final void setLayerValueAt(Layer layer, int x, int y, int value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public final void setMinecraftSeed(long minecraftSeed) {
        throw new UnsupportedOperationException();
    }

    @Override
    public final void setPopulate(boolean populate) {
        throw new UnsupportedOperationException();
    }

    @Override
    public final void setSubsurfaceMaterial(Terrain subsurfaceMaterial) {
        throw new UnsupportedOperationException();
    }

    @Override
    public final void setTerrainAt(int x, int y, Terrain terrain) {
        throw new UnsupportedOperationException();
    }

    @Override
    public final void setTerrainAt(Point coords, Terrain terrain) {
        throw new UnsupportedOperationException();
    }

    @Override
    public final void setWaterLevelAt(int x, int y, int waterLevel) {
        throw new UnsupportedOperationException();
    }

    protected abstract T wrapTile(Tile tile);

    protected final Dimension dimension;
    protected final Map<Point, Reference<T>> tileCache = new HashMap<>();

    private static final long serialVersionUID = 1L;
}