/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter;

import org.pepsoft.util.undo.BufferKey;
import org.pepsoft.util.undo.UndoManager;
import org.pepsoft.worldpainter.gardenofeden.Seed;
import org.pepsoft.worldpainter.layers.Layer;

import java.io.PrintStream;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A read-only implementation of {@link Tile} which wraps another {@code Tile}
 * and delegates all read  calls to it. All write calls throw an
 * {@link UnsupportedOperationException}.
 *
 * <p>Intended to change the behaviour of one or more read calls by overriding
 * them.
 *
 * @author pepijn
 */
public class RODelegatingTile extends Tile {
    public RODelegatingTile(Tile tile) {
        super(tile.getX(), tile.getY(), tile.getMinHeight(), tile.getMaxHeight(), false);
        this.tile = tile;
    }

    @Override
    public synchronized int getFloodedCount(int x, int y, int r, boolean lava) {
        return tile.getFloodedCount(x, y, r, lava);
    }

    @Override
    public List<Layer> getActiveLayers(int x, int y) {
        return tile.getActiveLayers(x, y);
    }

    @Override
    public synchronized float getSlope(int x, int y) {
        return tile.getSlope(x, y);
    }

    @Override
    public void addListener(Listener listener) {
        // Do nothing
    }

    @Override
    public void unregister() {
        // Do nothing
    }

    @Override
    public final void setMinMaxHeight(int minHeight, int maxHeight, HeightTransform heightTransform) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getIntHeight(int x, int y) {
        return tile.getIntHeight(x, y);
    }

    @Override
    public int getLowestIntHeight() {
        return tile.getLowestIntHeight();
    }

    @Override
    public int getHighestIntHeight() {
        return tile.getHighestIntHeight();
    }

    @Override
    public float getHeight(int x, int y) {
        return tile.getHeight(x, y); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int getRawHeight(int x, int y) {
        return tile.getRawHeight(x, y); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public final void setRawHeight(int x, int y, int rawHeight) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Terrain getTerrain(int x, int y) {
        return tile.getTerrain(x, y); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public synchronized Set<Terrain> getAllTerrains() {
        return tile.getAllTerrains();
    }

    @Override
    public int getWaterLevel(int x, int y) {
        return tile.getWaterLevel(x, y); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int getHighestWaterLevel() {
        return tile.getHighestWaterLevel();
    }

    @Override
    public List<Layer> getLayers() {
        return tile.getLayers(); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean containsOneOf(Layer... layers) {
        return tile.containsOneOf(layers);
    }

    @Override
    public boolean hasLayer(Layer layer) {
        return tile.hasLayer(layer);
    }

    @Override
    public List<Layer> getLayers(Set<Layer> additionalLayers) {
        return tile.getLayers(additionalLayers); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean getBitLayerValue(Layer layer, int x, int y) {
        return tile.getBitLayerValue(layer, x, y); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int getBitLayerCount(Layer layer, int x, int y, int r) {
        return tile.getBitLayerCount(layer, x, y, r); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Map<Layer, Integer> getLayersAt(int x, int y) {
        return tile.getLayersAt(x, y);
    }

    @Override
    public float getDistanceToEdge(Layer layer, int x, int y, float maxDistance) {
        return tile.getDistanceToEdge(layer, x, y, maxDistance); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int getLayerValue(Layer layer, int x, int y) {
        return tile.getLayerValue(layer, x, y); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public final void clearLayerData(Layer layer) {
        throw new UnsupportedOperationException();
    }

    @Override
    public final void clearLayerData(int x, int y, Set<Layer> excludedLayers) {
        throw new UnsupportedOperationException();
    }

    @Override
    public HashSet<Seed> getSeeds() {
        return tile.getSeeds(); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public final boolean plantSeed(Seed seed) {
        throw new UnsupportedOperationException();
    }

    @Override
    public final void removeSeed(Seed seed) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeListener(Listener listener) {
        throw new UnsupportedOperationException();
    }

    @Override
    public final Tile transform(CoordinateTransform transform) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean repair(int minHeight, int maxHeight, PrintStream out) {
        return true;
    }

    @Override
    public void savePointArmed() {
        // Do nothing
    }

    @Override
    public void savePointCreated() {
        // Do nothing
    }

    @Override
    public void undoPerformed() {
        // Do nothing
    }

    @Override
    public void redoPerformed() {
        // Do nothing
    }

    @Override
    public void bufferChanged(BufferKey<?> key) {
        // Do nothing
    }

    @Override
    final void ensureAllReadable() {
        throw new UnsupportedOperationException();
    }

    @Override
    void convertBiomeData() {
        // Do nothing
    }

    @Override
    protected final void ensureReadable(TileBuffer buffer) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isEventsInhibited() {
        return false;
    }

    @Override
    public void register(UndoManager undoManager) {
        // Do nothing
    }

    @Override
    public final void setBitLayerValue(Layer layer, int x, int y, boolean value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void inhibitEvents() {
        // Do nothing
    }

    @Override
    public void releaseEvents() {
        // Do nothing
    }

    @Override
    public final void setHeight(int x, int y, float height) {
        throw new UnsupportedOperationException();
    }

    @Override
    public final void setLayerValue(Layer layer, int x, int y, int value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public final void setTerrain(int x, int y, Terrain terrain) {
        throw new UnsupportedOperationException();
    }

    @Override
    public final void setWaterLevel(int x, int y, int waterLevel) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String toString() {
        return "RODelegatingTile[x=" + getX() + ",y=" + getY() + "]";
    }

    protected final Tile tile;
    
    private static final long serialVersionUID = 1L;
}