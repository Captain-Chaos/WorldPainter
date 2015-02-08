/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.layers.combined;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.pepsoft.minecraft.Chunk;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.RODelegatingDimension;
import org.pepsoft.worldpainter.RODelegatingTile;
import org.pepsoft.worldpainter.Tile;
import org.pepsoft.worldpainter.exporting.AbstractLayerExporter;
import org.pepsoft.worldpainter.exporting.FirstPassLayerExporter;
import org.pepsoft.worldpainter.exporting.Fixup;
import org.pepsoft.worldpainter.exporting.LayerExporter;
import org.pepsoft.worldpainter.exporting.MinecraftWorld;
import org.pepsoft.worldpainter.exporting.SecondPassLayerExporter;
import org.pepsoft.worldpainter.layers.CombinedLayer;
import org.pepsoft.worldpainter.layers.Layer;

// ***************
// * NOT IN USE! *
// ***************

/**
 *
 * @author pepijn
 */
public class CombinedLayerExporter extends AbstractLayerExporter<CombinedLayer> implements FirstPassLayerExporter<CombinedLayer>, SecondPassLayerExporter<CombinedLayer> {
    public CombinedLayerExporter(CombinedLayer combinedLayer) {
        super(combinedLayer);
        List<FirstPassLayerExporter<?>> firstPassList = new ArrayList<FirstPassLayerExporter<?>>();
        List<SecondPassLayerExporter<?>> secondPassList = new ArrayList<SecondPassLayerExporter<?>>();
        for (Layer layer: combinedLayer.getLayers()) {
            LayerExporter<?> exporter = layer.getExporter();
            if (exporter instanceof FirstPassLayerExporter) {
                firstPassList.add((FirstPassLayerExporter<?>) exporter);
            }
            if (exporter instanceof SecondPassLayerExporter) {
                secondPassList.add((SecondPassLayerExporter<?>) exporter);
            }
        }
        firstPassExporters = firstPassList.toArray(new FirstPassLayerExporter<?>[firstPassList.size()]);
        secondPassExporters = secondPassList.toArray(new SecondPassLayerExporter<?>[secondPassList.size()]);
    }

    @Override
    public void render(Dimension dimension, Tile tile, Chunk chunk) {
        if (tile.hasLayer(layer)) {
            for (FirstPassLayerExporter<?> exporter: firstPassExporters) {
                final Layer exporterLayer = exporter.getLayer();
                float factor = layer.getFactors().get(exporterLayer);
                exporter.render(new MappingDimension(dimension, layer, exporterLayer, factor), new MappingTile(tile, layer, exporterLayer, factor), chunk);
            }
        }
    }

    @Override
    public List<Fixup> render(Dimension dimension, Rectangle area, Rectangle exportedArea, MinecraftWorld minecraftWorld) {
        final List<Fixup> fixups = new ArrayList<Fixup>();
        for (SecondPassLayerExporter<?> exporter: secondPassExporters) {
            final Layer exporterLayer = exporter.getLayer();
            float factor = layer.getFactors().get(exporterLayer);
            List<Fixup> layerFixups = exporter.render(new MappingDimension(dimension, layer, exporterLayer, factor), area, exportedArea, minecraftWorld);
            if (layerFixups != null) {
                fixups.addAll(layerFixups);
            }
        }
        return fixups.isEmpty() ? null : fixups;
    }
    
    private final FirstPassLayerExporter<?>[] firstPassExporters;
    private final SecondPassLayerExporter<?>[] secondPassExporters;
    
    static class MappingTile extends RODelegatingTile {
        public MappingTile(Tile tile, CombinedLayer from, Layer to, float factor) {
            super(tile);
            this.from = from;
            this.to = to;
            this.factor = factor;
        }

        @Override
        public List<Layer> getLayers() {
            List<Layer> layers = new ArrayList<Layer>();
            for (Layer layer: super.getLayers()) {
                if (layer.equals(from)) {
                    layers.add(to);
                } else {
                    layers.add(layer);
                }
            }
            return layers;
        }

        @Override
        public boolean hasLayer(Layer layer) {
            if (layer.equals(to)) {
                return super.hasLayer(from);
            } else {
                return super.hasLayer(layer);
            }
        }

        @Override
        public List<Layer> getLayers(Set<Layer> additionalLayers) {
            List<Layer> layers = new ArrayList<Layer>();
            for (Layer layer: super.getLayers(additionalLayers)) {
                if (layer.equals(from)) {
                    layers.add(to);
                } else {
                    layers.add(layer);
                }
            }
            return layers;
        }

        @Override
        public boolean getBitLayerValue(Layer layer, int x, int y) {
            if (layer.equals(to)) {
                return super.getBitLayerValue(from, x, y);
            } else {
                return super.getBitLayerValue(layer, x, y);
            }
        }

        @Override
        public int getBitLayerCount(Layer layer, int x, int y, int r) {
            if (layer.equals(to)) {
                return super.getBitLayerCount(from, x, y, r);
            } else {
                return super.getBitLayerCount(layer, x, y, r);
            }
        }

        @Override
        public float getDistanceToEdge(Layer layer, int x, int y, float maxDistance) {
            if (layer.equals(to)) {
                return super.getDistanceToEdge(from, x, y, maxDistance);
            } else {
                return super.getDistanceToEdge(layer, x, y, maxDistance);
            }
        }

        @Override
        public int getLayerValue(Layer layer, int x, int y) {
            if (layer.equals(to)) {
                int value = super.getLayerValue(from, x, y);
                return (int) (value * factor + 0.5f);
            } else {
                return super.getLayerValue(layer, x, y);
            }
        }
    
        private final CombinedLayer from;
        private final Layer to;
        private final float factor;
        
        private static final long serialVersionUID = 1L;
    }
    
    static class MappingDimension extends RODelegatingDimension {
        public MappingDimension(Dimension dimension, CombinedLayer from, Layer to, float factor) {
            super(dimension);
            this.from = from;
            this.to = to;
            this.factor = factor;
        }

        @Override
        public Tile getTile(Point coords) {
            // In theory this is not correct, since the dimension might have gained
            // or lost tiles in the mean time. However the expected usage pattern
            // of the functionality is such that that should not happen in practice,
            // and creating tile snapshots of all tiles when the dimension snapshot
            // is created would be a performance hit
            MappingTile cachedTile = tileCache.get(coords);
            if (cachedTile == null) {
                Tile tile = dimension.getTile(coords);
                if (tile != null) {
                    cachedTile = new MappingTile(tile, from, to, factor);
                    tileCache.put(coords, cachedTile);
                }
            }
            return cachedTile;
        }

        @Override
        public Collection<? extends Tile> getTiles() {
            Collection<? extends Tile> tiles = dimension.getTiles();
            for (Tile tile: tiles) {
                Point coords = new Point(tile.getX(), tile.getY());
                if (! tileCache.containsKey(coords)) {
                    MappingTile cachedTile = new MappingTile(tile, from , to, factor);
                    tileCache.put(coords, cachedTile);
                }
            }
            return Collections.unmodifiableCollection(tileCache.values());
        }

        @Override
        public int getLayerValueAt(Layer layer, int x, int y) {
            if (layer.equals(to)){
                return super.getLayerValueAt(from, x, y);
            } else {
                return super.getLayerValueAt(layer, x, y);
            }
        }

        @Override
        public int getLayerValueAt(Layer layer, Point coords) {
            if (layer.equals(to)){
                return super.getLayerValueAt(from, coords);
            } else {
                return super.getLayerValueAt(layer, coords);
            }
        }

        @Override
        public boolean getBitLayerValueAt(Layer layer, int x, int y) {
            if (layer.equals(to)){
                return super.getBitLayerValueAt(from, x, y);
            } else {
                return super.getBitLayerValueAt(layer, x, y);
            }
        }

        @Override
        public int getBitLayerCount(Layer layer, int x, int y, int r) {
            if (layer.equals(to)){
                return super.getBitLayerCount(from, x, y, r);
            } else {
                return super.getBitLayerCount(layer, x, y, r);
            }
        }

        @Override
        public float getDistanceToEdge(Layer layer, int x, int y, float maxDistance) {
            if (layer.equals(to)){
                return super.getDistanceToEdge(from, x, y, maxDistance);
            } else {
                return super.getDistanceToEdge(layer, x, y, maxDistance);
            }
        }

        @Override
        public Set<Layer> getAllLayers(boolean applyCombinedLayers) {
            Set<Layer> layers = new HashSet<Layer>();
            for (Layer layer: super.getAllLayers(applyCombinedLayers)) {
                if (layer.equals(from)) {
                    layers.add(to);
                } else {
                    layers.add(layer);
                }
            }
            return layers;
        }

        @Override
        public Set<Layer> getMinimumLayers() {
            Set<Layer> layers = new HashSet<Layer>();
            for (Layer layer: super.getMinimumLayers()) {
                if (layer.equals(from)) {
                    layers.add(to);
                } else {
                    layers.add(layer);
                }
            }
            return layers;
        }

        private final CombinedLayer from;
        private final Layer to;
        private final float factor;
        private final Map<Point, MappingTile> tileCache = new HashMap<Point, MappingTile>();
        
        private static final long serialVersionUID = 1L;
    }
}