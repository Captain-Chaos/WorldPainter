/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.layers;

import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.MixedMaterial;
import org.pepsoft.worldpainter.Terrain;
import org.pepsoft.worldpainter.Tile;
import org.pepsoft.worldpainter.exporting.LayerExporter;
import org.pepsoft.worldpainter.layers.combined.CombinedLayerExporter;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.*;

import static org.pepsoft.worldpainter.Constants.TILE_SIZE;
import static org.pepsoft.worldpainter.layers.Layer.DataSize.*;

/**
 *
 * @author pepijn
 */
public class CombinedLayer extends CustomLayer implements LayerContainer {
    public CombinedLayer(String name, String description, int colour) {
        super(name, description, NIBBLE, 55, colour);
    }

    public Set<Layer> apply(Dimension dimension) {
        Set<Layer> addedLayers = new HashSet<Layer>();
        for (Tile tile : dimension.getTiles()) {
            addedLayers.addAll(apply(tile));
        }
        return addedLayers;
    }

    public Set<Layer> apply(Tile tile) {
        Set<Layer> addedLayers = new HashSet<Layer>();
        if (!tile.hasLayer(this)) {
            return Collections.emptySet();
        }
        tile.inhibitEvents();
        try {
            for (Layer layer : layers) {
                boolean layerAdded = false;
                final float factor = factors.get(layer);
                DataSize dataSize = layer.getDataSize();
                if ((dataSize == BIT) || (dataSize == BIT_PER_CHUNK)) {
                    for (int x = 0; x < TILE_SIZE; x++) {
                        for (int y = 0; y < TILE_SIZE; y++) {
                            float strength = Math.min(tile.getLayerValue(this, x, y) / 15.0f * factor, 1.0f);
                            if ((strength > 0.95f) || (Math.random() < strength)) {
                                tile.setBitLayerValue(layer, x, y, true);
                                layerAdded = true;
                            }
                        }
                    }
                } else {
                    int maxValue = (dataSize == NIBBLE) ? 15 : 255;
                    for (int x = 0; x < TILE_SIZE; x++) {
                        for (int y = 0; y < TILE_SIZE; y++) {
                            int value = Math.min((int) (tile.getLayerValue(this, x, y) * factor + 0.5f), maxValue);
                            if (value > 0) {
                                tile.setLayerValue(layer, x, y, value);
                                layerAdded = true;
                            }
                        }
                    }
                }
                if (layerAdded) {
                    addedLayers.add(layer);
                }
            }
            tile.clearLayerData(this);
        } finally {
            tile.releaseEvents();
        }
        return addedLayers;
    }

    @Override
    public void setName(String name) {
        super.setName(name);
    }

    public Terrain getTerrain() {
        return terrain;
    }

    public void setTerrain(Terrain terrain) {
        this.terrain = terrain;
    }

    @Override
    public List<Layer> getLayers() {
        return layers;
    }

    public void setLayers(List<Layer> layers) {
        if (layers == null) {
            throw new NullPointerException();
        }
        this.layers = layers;
    }

    public Map<Layer, Float> getFactors() {
        return factors;
    }

    public void setFactors(Map<Layer, Float> factors) {
        if (factors == null) {
            throw new NullPointerException();
        }
        this.factors = factors;
    }

    @Override
    public LayerExporter<CombinedLayer> getExporter() {
        return new CombinedLayerExporter(this);
    }

    @Override
    public List<Action> getActions() {
        List<Action> actions = new ArrayList<Action>();
        List<Action> superActions = super.getActions();
        if (superActions != null) {
            actions.addAll(superActions);
        }
        actions.add(new AbstractAction("Apply") {
            @Override
            public void actionPerformed(ActionEvent e) {
                Dimension dimension = (Dimension) getValue(KEY_DIMENSION);
                if ((dimension != null) && dimension.getAllLayers(false).contains(CombinedLayer.this)) {
                    dimension.armSavePoint();
                    apply(dimension);
                }
            }
            
            private static final long serialVersionUID = 1L;
        });
        return actions;
    }
    
    public boolean isMissingTerrainWarning() {
        return missingTerrainWarning;
    }
    
    public void resetMissingTerrainWarning() {
        missingTerrainWarning = false;
    }
    
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        if (customTerrainPresent) {
            MixedMaterial customTerrain = (MixedMaterial) in.readObject();
            if (customTerrain.equals(Terrain.getCustomMaterial(terrain.getCustomTerrainIndex()))) {
                // The exact same custom terrain is present, in the same slot.
                // Keep using it
            } else if (Terrain.getCustomMaterial(terrain.getCustomTerrainIndex()) == null) {
                // The slot that was previously used is empty, store the custom
                // terrain in it
                Terrain.setCustomMaterial(terrain.getCustomTerrainIndex(), customTerrain);
            } else {
                // The slot that was previously used contains a different mixed
                // material. Find another empty slot
                for (int i = 0; i < Terrain.CUSTOM_TERRAIN_COUNT; i++) {
                    if (Terrain.getCustomMaterial(i) == null) {
                        Terrain.setCustomMaterial(i, customTerrain);
                        terrain = Terrain.getCustomTerrain(i);
                        return;
                    }
                }
                // No more slots available. Not much we can do
                terrain = null;
                missingTerrainWarning = true;
            }
        } else if ((terrain != null) && terrain.isCustom()) {
            // This is an old layer, saved when WorldPainter did not yet store
            // the actual custom terrain settings with the layer. Not much we
            // can do
            terrain = null;
            missingTerrainWarning = true;
        }
    }
    
    private void writeObject(ObjectOutputStream out) throws IOException {
        // Make sure that the custom terrain definition gets saved along with
        // the layer
        customTerrainPresent = (terrain != null) && terrain.isCustom();
        out.defaultWriteObject();
        if (customTerrainPresent) {
            out.writeObject(Terrain.getCustomMaterial(terrain.getCustomTerrainIndex()));
        }
    }
    
    private static final long serialVersionUID = 1L;
    private Terrain terrain;
    private List<Layer> layers = Collections.emptyList();
    private Map<Layer, Float> factors = Collections.emptyMap();
    private boolean customTerrainPresent, missingTerrainWarning;
}