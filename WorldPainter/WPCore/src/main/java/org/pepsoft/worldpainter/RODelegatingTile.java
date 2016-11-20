/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter;

import java.io.PrintStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.pepsoft.util.undo.BufferKey;
import org.pepsoft.util.undo.UndoManager;
import org.pepsoft.worldpainter.gardenofeden.Seed;
import org.pepsoft.worldpainter.layers.Layer;

/**
 *
 * @author pepijn
 */
public class RODelegatingTile extends Tile {
    public RODelegatingTile(Tile tile) {
        super(tile.getX(), tile.getY(), tile.getMaxHeight(), false);
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
    public void setMaxHeight(int maxHeight, HeightTransform heightTransform) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getIntHeight(int x, int y) {
        return tile.getIntHeight(x, y);
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
    public void setRawHeight(int x, int y, int rawHeight) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Terrain getTerrain(int x, int y) {
        return tile.getTerrain(x, y); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int getWaterLevel(int x, int y) {
        return tile.getWaterLevel(x, y); //To change body of generated methods, choose Tools | Templates.
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
    public float getDistanceToEdge(Layer layer, int x, int y, float maxDistance) {
        return tile.getDistanceToEdge(layer, x, y, maxDistance); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int getLayerValue(Layer layer, int x, int y) {
        return tile.getLayerValue(layer, x, y); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void clearLayerData(Layer layer) {
        throw new UnsupportedOperationException();
    }

    @Override
    public HashSet<Seed> getSeeds() {
        return tile.getSeeds(); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean plantSeed(Seed seed) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeSeed(Seed seed) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeListener(Listener listener) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Tile transform(CoordinateTransform transform) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean repair(int maxHeight, PrintStream out) {
        throw new UnsupportedOperationException();
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
    void ensureAllReadable() {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void ensureReadable(TileBuffer buffer) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isEventsInhibited() {
        return false;
    }

    @Override
    public void register(UndoManager undoManager) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setBitLayerValue(Layer layer, int x, int y, boolean value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public synchronized void inhibitEvents() {
        throw new UnsupportedOperationException();
    }

    @Override
    public synchronized void releaseEvents() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setHeight(int x, int y, float height) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setLayerValue(Layer layer, int x, int y, int value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setTerrain(int x, int y, Terrain terrain) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setWaterLevel(int x, int y, int waterLevel) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String toString() {
        return "RODelegatingTile[x=" + getX() + ",y=" + getY() + "]";
    }

    protected final Tile tile;
    
    private static final long serialVersionUID = 1L;
}