/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter;

import org.pepsoft.util.undo.Snapshot;
import org.pepsoft.util.undo.UndoManager;
import org.pepsoft.worldpainter.layers.Layer;
import org.pepsoft.worldpainter.layers.exporters.ExporterSettings;

import java.awt.*;
import java.beans.PropertyChangeListener;
import java.util.*;

/**
 *
 * @author pepijn
 */
public final class DimensionSnapshot extends Dimension {
    public DimensionSnapshot(Dimension dimension, Snapshot snapshot) {
        super(dimension.getWorld(), dimension.getMinecraftSeed(), dimension.getTileFactory(), dimension.getDim(), dimension.getMaxHeight());
        this.dimension = dimension;
        this.snapshot = snapshot;
        super.setBorder(dimension.getBorder());
        super.setBorderSize(dimension.getBorderSize());
        super.setBorderLevel(dimension.getBorderLevel());
        super.setMinecraftSeed(dimension.getMinecraftSeed());
        super.setSubsurfaceMaterial(dimension.getSubsurfaceMaterial());
        super.setBedrockWall(dimension.isBedrockWall());
        super.setDarkLevel(dimension.isDarkLevel());
        super.setPopulate(dimension.isPopulate());
        width = dimension.getWidth();
        height = dimension.getHeight();
        lowestX = dimension.getLowestX();
        highestX = dimension.getHighestX();
        lowestY = dimension.getLowestY();
        highestY = dimension.getHighestY();
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
    public void addTile(Tile tile) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void applyTheme(int x, int y) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void applyTheme(Point coords) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void unregister() {
        // Do nothing
    }

    @Override
    public void clearRedo() {
        // Do nothing
    }

    @Override
    public void armSavePoint() {
        // Do nothing
    }

    @Override
    public void clearUndo() {
        // Do nothing
    }

    @Override
    public boolean undoChanges() {
        return false;
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
        return height;
    }

    @Override
    public int getHighestX() {
        return highestX;
    }

    @Override
    public int getHighestY() {
        return highestY;
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
        return lowestX;
    }

    @Override
    public int getLowestY() {
        return lowestY;
    }

    @Override
    public Tile getTile(Point coords) {
        // In theory this is not correct, since the dimension might have gained
        // or lost tiles in the mean time. However the expected usage pattern
        // of the functionality is such that that should not happen in practice,
        // and creating tile snapshots of all tiles when the dimension snapshot
        // is created would be a performance hit
        TileSnapshot tileSnapshot = tileSnapshots.get(coords);
        if (tileSnapshot == null) {
            Tile tile = dimension.getTile(coords);
            if (tile != null) {
                tileSnapshot = new TileSnapshot(tile, snapshot);
                tileSnapshots.put(coords, tileSnapshot);
            }
        }
        return tileSnapshot;
    }

    @Override
    public Collection<? extends Tile> getTiles() {
        Collection<? extends Tile> tiles = dimension.getTiles();
        for (Tile tile: tiles) {
            Point coords = new Point(tile.getX(), tile.getY());
            if (! tileSnapshots.containsKey(coords)) {
                TileSnapshot tileSnapshot = new TileSnapshot(tile, snapshot);
                tileSnapshots.put(coords, tileSnapshot);
            }
        }
        return Collections.unmodifiableCollection(tileSnapshots.values());
    }

    @Override
    public Set<Point> getTileCoords() {
        // In theory this is not correct, since the dimension might have gained
        // or lost tiles in the mean time. However the expected usage pattern
        // of the functionality is such that that should not happen in practice,
        // and creating tile snapshots of all tiles when the dimension snapshot
        // is created would be a performance hit
        return dimension.getTileCoords();
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public boolean isEventsInhibited() {
        return false;
    }

    @Override
    public void removeTile(Tile tile) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setBedrockWall(boolean bedrockWall) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setBitLayerValueAt(Layer layer, int x, int y, boolean value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setBorder(Border border) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setBorderLevel(int borderLevel) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setBorderSize(int borderSize) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setDarkLevel(boolean darkLevel) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void changed() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setEventsInhibited(boolean eventsInhibited) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setHeightAt(int x, int y, float height) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setHeightAt(Point coords, float height) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setLayerSettings(Layer layer, ExporterSettings settings) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setLayerValueAt(Layer layer, int x, int y, int value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setMinecraftSeed(long minecraftSeed) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setPopulate(boolean populate) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setSubsurfaceMaterial(Terrain subsurfaceMaterial) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setTerrainAt(int x, int y, Terrain terrain) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setTerrainAt(Point coords, Terrain terrain) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setWaterLevelAt(int x, int y, int waterLevel) {
        throw new UnsupportedOperationException();
    }

    private final Dimension dimension;
    private final Snapshot snapshot;
    private final int width, height, lowestX, highestX, lowestY, highestY;
    private final Map<Point, TileSnapshot> tileSnapshots = new HashMap<>();
    
    private static final long serialVersionUID = 2011101501L;
}