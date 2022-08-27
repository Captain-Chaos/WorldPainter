package org.pepsoft.worldpainter;

import org.pepsoft.worldpainter.layers.Layer;
import org.pepsoft.worldpainter.layers.exporters.ExporterSettings;

import java.awt.*;
import java.util.Map;
import java.util.Set;

// TODO this is a naive implementation which delegates everything to the Dimension API, which means that it has to look
//  up the tile anew for every coordinate. This should be made smarter
// TODO this supports only the functionality currently needed to export dimensions. Other methods throw an
//  UnsupportedOperationException
public class ScaledDimension extends RODelegatingDimension<Tile> {
    public ScaledDimension(Dimension dimension, float scale) {
        super(dimension);
        this.scale = scale;
        scalingHelper = new ScalingHelper(dimension.tiles, dimension.getTileFactory(), scale);
        tileCoords = scalingHelper.getTileCoords();
        setScale(dimension.getScale() / scale);
    }

    @Override
    public int getTileCount() {
        return tileCoords.size();
    }

    @Override
    public int getIntHeightAt(int x, int y, int defaultValue) {
        return Math.round(scalingHelper.getHeightAt(x, y));
    }

    @Override
    public float getHeightAt(int x, int y) {
        return scalingHelper.getHeightAt(x, y);
    }

    @Override
    public int getRawHeightAt(int x, int y) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getWaterLevelAt(int x, int y) {
        return dimension.getWaterLevelAt(Math.round(x / scale), Math.round(y / scale));
    }

    @Override
    public int getLayerValueAt(Layer layer, int x, int y) {
        if (layer.discrete) {
            return dimension.getLayerValueAt(layer, Math.round(x / scale), Math.round(y / scale));
        } else {
            return scalingHelper.getLayerValueAt(layer, x, y);
        }
    }

    @Override
    public boolean getBitLayerValueAt(Layer layer, int x, int y) {
        if (layer.discrete) {
            return dimension.getBitLayerValueAt(layer, Math.round(x / scale), Math.round(y / scale));
        } else {
            return scalingHelper.getBitLayerValueAt(layer, x, y);
        }
    }

    @Override
    public float getDistanceToEdge(Layer layer, int x, int y, float maxDistance) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getHeight() {
        return scalingHelper.getHighestTileY() - scalingHelper.getLowestTileY() + 1;
    }

    @Override
    public int getHighestX() {
        return scalingHelper.getHighestTileX();
    }

    @Override
    public int getHighestY() {
        return scalingHelper.getHighestTileY();
    }

    @Override
    public ExporterSettings getLayerSettings(Layer layer) {
        return dimension.getLayerSettings(layer);
    }

    @Override
    public Map<Layer, ExporterSettings> getAllLayerSettings() {
        return dimension.getAllLayerSettings();
    }

    @Override
    public int getLowestX() {
        return scalingHelper.getLowestTileX();
    }

    @Override
    public int getLowestY() {
        return scalingHelper.getLowestTileY();
    }

    @Override
    public int getFloodedCount(int x, int y, int r, boolean lava) {
        throw new UnsupportedOperationException();
    }

    @Override
    public float getSlope(int x, int y) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<Point> getTileCoords() {
        return tileCoords;
    }

    @Override
    public int getWidth() {
        return scalingHelper.getHighestTileX() - scalingHelper.getLowestTileX() + 1;
    }

    @Override
    public boolean isBorderTile(int x, int y) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected Tile doGetTile(Point coords) {
        return scalingHelper.createScaledTile(coords.x, coords.y);
    }

    private final float scale;
    private final ScalingHelper scalingHelper;
    private final Set<Point> tileCoords;
}