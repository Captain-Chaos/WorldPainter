/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter;

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
public class RODelegatingTile extends ReadOnlyTile {
    public RODelegatingTile(Tile tile) {
        super(tile.getX(), tile.getY(), tile.getMinHeight(), tile.getMaxHeight(), false);
        this.tile = tile;
    }

    @Override
    public int getFloodedCount(int x, int y, int r, boolean lava) {
        return tile.getFloodedCount(x, y, r, lava);
    }

    @Override
    public List<Layer> getActiveLayers(int x, int y) {
        return tile.getActiveLayers(x, y);
    }

    @Override
    public float getSlope(int x, int y) {
        return tile.getSlope(x, y);
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
    public int getLowestRawHeight() {
        return tile.getLowestRawHeight();
    }

    @Override
    public int getHighestRawHeight() {
        return tile.getHighestRawHeight();
    }

    @Override
    public int[] getRawHeightRange() {
        return tile.getRawHeightRange();
    }

    @Override
    public Terrain getTerrain(int x, int y) {
        return tile.getTerrain(x, y); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Set<Terrain> getAllTerrains() {
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
    public HashSet<Seed> getSeeds() {
        return tile.getSeeds(); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean repair(int minHeight, int maxHeight, PrintStream out) {
        return true;
    }

    @Override
    public String toString() {
        return "RODelegatingTile[x=" + getX() + ",y=" + getY() + "]";
    }

    protected final Tile tile;
    
    private static final long serialVersionUID = 1L;
}