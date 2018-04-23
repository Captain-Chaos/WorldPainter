/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.layers;

import org.pepsoft.minecraft.Chunk;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.MixedMaterial;
import org.pepsoft.worldpainter.Terrain;
import org.pepsoft.worldpainter.Tile;
import org.pepsoft.worldpainter.exporting.*;
import org.pepsoft.worldpainter.layers.exporters.ExporterSettings;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.*;
import java.util.List;

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
        Set<Layer> addedLayers = new HashSet<>();
        for (Tile tile : dimension.getTiles()) {
            addedLayers.addAll(apply(tile));
        }
        return addedLayers;
    }

    public Set<Layer> apply(Tile tile) {
        Set<Layer> addedLayers = new HashSet<>();
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

    /**
     * Returns a dummy exporter, all methods of which throw an
     * {@link UnsupportedOperationException}, since combined layers must be
     * exported by {@link #apply(Dimension) applying} them and then exporting
     * its constituent layers, if any.
     *
     * <p>The exporter does implement {@link FirstPassLayerExporter} and
     * {@link SecondPassLayerExporter} though, to signal the fact that it can
     * contain layers for both phases.
     *
     * @return A dummy exporter which always throws
     * <code>UnsupportedOperationException</code>.
     */
    @Override
    public LayerExporter getExporter() {
        return EXPORTER;
    }

    @Override
    public List<Action> getActions() {
        List<Action> actions = new ArrayList<>();
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

    /**
     * If this combined layer contains a custom terrain, make sure that it is
     * installed. If the original material is available as a custom terrain
     * type, use that, otherwise install the material as a new custom terrain
     * type, assuming a slot is available. Should be invoked once, after a
     * combined layer has been loaded.
     *
     * <p>Returns <code>false</code> if a custom terrain was present
     * but it could not be restored because all custom terrain slots are in use,
     * <code>true</code> in all other circumstances.
     *
     * <p><strong>Please note:</strong> if this returns <code>true</code> a new
     * custom terrain <em>may</em> have been installed, so in that case the
     * invoker MUST check whether a button already exists for the custom terrain
     * used by this combined layer, and add one if not.
     *
     * @return <code>false</code> if a custom terrain was present but it could
     *     not be restored because all custom terrain slots are in use,
     *     <code>true</code> otherwise.
     */
    public boolean restoreCustomTerrain() {
        if (customTerrainPresent) {
            if (customTerrainMaterial == null) {
                // This should not be possible, but due to earlier bugs there
                // are worlds in the wild with a custom terrain without a stored
                // custom material. Not much we can do
                terrain = null;
                return false;
            } else if (customTerrainMaterial.equals(Terrain.getCustomMaterial(terrain.getCustomTerrainIndex()))) {
                // The exact same custom terrain is present, in the same slot.
                // Keep using it
                return true;
            } else if (Terrain.getCustomMaterial(terrain.getCustomTerrainIndex()) == null) {
                // The slot that was previously used is empty, store the custom
                // terrain in it
                Terrain.setCustomMaterial(terrain.getCustomTerrainIndex(), customTerrainMaterial);
                return true;
            } else {
                // The slot that was previously used contains a different mixed
                // material. Find another empty slot
                for (int i = 0; i < Terrain.CUSTOM_TERRAIN_COUNT; i++) {
                    if (Terrain.getCustomMaterial(i) == null) {
                        Terrain.setCustomMaterial(i, customTerrainMaterial);
                        terrain = Terrain.getCustomTerrain(i);
                        return true;
                    }
                }
                // No more slots available. Not much we can do
                terrain = null;
                return false;
            }
        } else {
            return (terrain == null) || (! terrain.isCustom());
        }
    }

    @Override
    public CombinedLayer clone() {
        CombinedLayer clone = (CombinedLayer) super.clone();
        if (! layers.isEmpty()) {
            clone.layers = new ArrayList<>(layers);
        }
        if (! factors.isEmpty()) {
            clone.factors = new HashMap<>(factors);
        }
        return clone;
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        if (customTerrainPresent) {
            customTerrainMaterial = (MixedMaterial) in.readObject();
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
    private boolean customTerrainPresent;
    private transient MixedMaterial customTerrainMaterial;

    private static final LayerExporter EXPORTER = new CombinedLayerExporter();

    static class CombinedLayerExporter implements FirstPassLayerExporter, SecondPassLayerExporter {
        @Override
        public Layer getLayer() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setSettings(ExporterSettings settings) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void render(Dimension dimension, Tile tile, Chunk chunk) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<Fixup> render(Dimension dimension, Rectangle area, Rectangle exportedArea, MinecraftWorld minecraftWorld) {
            throw new UnsupportedOperationException();
        }
    }
}