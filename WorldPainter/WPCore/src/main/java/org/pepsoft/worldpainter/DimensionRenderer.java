/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.worldpainter;

import org.pepsoft.worldpainter.layers.Layer;

import java.awt.*;
import java.util.Set;

import static org.pepsoft.worldpainter.Constants.TILE_SIZE;

/**
 *
 * @author pepijn
 */
public class DimensionRenderer {
    public DimensionRenderer(Dimension dimension, TileRenderer tileRenderer) {
        setDimension(dimension);
        setTileRenderer(tileRenderer);
    }

    public void addHiddenLayer(Layer hiddenLayer) {
        tileRenderer.addHiddenLayer(hiddenLayer);
    }

    public void removeHiddenLayer(Layer layer) {
        tileRenderer.removeHiddenLayer(layer);
    }

    public Set<Layer> getHiddenLayers() {
        return tileRenderer.getHiddenLayers();
    }
    
    public void setHiddenLayers(Set<Layer> hiddenLayers) {
        tileRenderer.setHiddenLayers(hiddenLayers);
    }

    public void renderTile(Image image, int x, int y) {
        Tile tile = dimension.getTile(x, y);
        if (tile != null) {
            tileRenderer.renderTile(tile, image, (x - dimension.getLowestX()) * TILE_SIZE, (y - dimension.getLowestY()) * TILE_SIZE);
        }
    }

    public final Dimension getDimension() {
        return dimension;
    }

    public final void setDimension(Dimension dimension) {
        if (dimension == null) {
            throw new NullPointerException();
        }
        this.dimension = dimension;
        if (tileRenderer != null) {
            tileRenderer.setTileProvider(dimension);
        }
    }

    public final TileRenderer getTileRenderer() {
        return tileRenderer;
    }

    public final void setTileRenderer(TileRenderer tileRenderer) {
        if (tileRenderer == null) {
            throw new NullPointerException();
        }
        this.tileRenderer = tileRenderer;
        tileRenderer.setTileProvider(dimension);
    }

    private Dimension dimension;
    private TileRenderer tileRenderer;
}