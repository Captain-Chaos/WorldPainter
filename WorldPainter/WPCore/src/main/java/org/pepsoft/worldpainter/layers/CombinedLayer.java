/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.layers;

import org.pepsoft.minecraft.Chunk;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.*;
import org.pepsoft.worldpainter.exporting.*;
import org.pepsoft.worldpainter.layers.exporters.ExporterSettings;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;
import java.util.*;

import static org.pepsoft.worldpainter.Constants.TILE_SIZE;
import static org.pepsoft.worldpainter.exporting.SecondPassLayerExporter.Stage.ADD_FEATURES;
import static org.pepsoft.worldpainter.exporting.SecondPassLayerExporter.Stage.CARVE;
import static org.pepsoft.worldpainter.layers.Layer.DataSize.*;

/**
 *
 * @author pepijn
 */
public class CombinedLayer extends CustomLayer implements LayerContainer {
    public CombinedLayer(String name, String description, Object paint) {
        super(name, description, NIBBLE, 55, paint);
    }

    public Set<Layer> apply(Dimension dimension) {
        return apply(dimension, null);
    }

    public Set<Layer> apply(Dimension dimension, Set<Point> selectedTiles) {
        Set<Layer> addedLayers = new HashSet<>();
        if (selectedTiles == null) {
            for (Tile tile: dimension.getTiles()) {
                addedLayers.addAll(apply(tile));
            }
        } else {
            for (Point coords: selectedTiles) {
                final Tile tile = dimension.getTile(coords);
                if (tile != null) {
                    addedLayers.addAll(apply(tile));
                }
            }
        }
        return addedLayers;
    }

    public Set<Layer> apply(Tile tile) {
        final boolean terrainConfigured = terrain != null;
        final int biome = getBiome();
        final boolean biomeConfigured = biome != -1;
        final Set<Layer> addedLayers = new HashSet<>();
        if (! tile.hasLayer(this)) {
            return Collections.emptySet();
        }
        tile.inhibitEvents();
        try {
            if (applyTerrainAndBiomeOnExport && (terrainConfigured || biomeConfigured)) {
                for (int x = 0; x < TILE_SIZE; x++) {
                    for (int y = 0; y < TILE_SIZE; y++) {
                        final float strength = tile.getLayerValue(this, x, y) / 15.0f;
                        if (strength > 0.0f) {
                            if (terrainConfigured && ((strength >= 0.5f) || ((Math.random() / 2) < strength))) {
                                tile.setTerrain(x, y, terrain);
                            }
                            if (biomeConfigured && ((strength >= 0.5f) || ((Math.random() / 2) < strength))) {
                                tile.setLayerValue(Biome.INSTANCE, x, y, biome);
                            }
                        }
                    }
                }
            }
            for (Layer layer : layers) {
                boolean layerAdded = false;
                final float factor = factors.get(layer);
                final DataSize dataSize = layer.getDataSize();
                if ((dataSize == BIT) || (dataSize == BIT_PER_CHUNK)) {
                    for (int x = 0; x < TILE_SIZE; x++) {
                        for (int y = 0; y < TILE_SIZE; y++) {
                            final float strength = Math.min(tile.getLayerValue(this, x, y) / 15.0f * factor, 1.0f);
                            if ((strength > 0.95f) || (Math.random() < strength)) {
                                tile.setBitLayerValue(layer, x, y, true);
                                layerAdded = true;
                            }
                        }
                    }
                } else {
                    final int maxValue = (dataSize == NIBBLE) ? 15 : 255;
                    for (int x = 0; x < TILE_SIZE; x++) {
                        for (int y = 0; y < TILE_SIZE; y++) {
                            final int value = Math.min(Math.round(tile.getLayerValue(this, x, y) * factor), maxValue);
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

    @Override
    public String getType() {
        return "Combined Layer";
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
            throw new NullPointerException("layers");
        }
        for (int i = 0; i < layers.size(); i++) {
            if (layers.get(i) == null) {
                throw new IllegalArgumentException("layers[" + i + "] == null");
            }
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

    public boolean isApplyTerrainAndBiomeOnExport() {
        return applyTerrainAndBiomeOnExport;
    }

    public void setApplyTerrainAndBiomeOnExport(boolean applyTerrainAndBiomeOnExport) {
        this.applyTerrainAndBiomeOnExport = applyTerrainAndBiomeOnExport;
    }

    @Override
    public Class<? extends LayerExporter> getExporterType() {
        return CombinedLayerExporter.class;
    }

    /**
     * Returns a dummy exporter, all methods of which throw an {@link UnsupportedOperationException}, since combined
     * layers must be exported by {@link #apply(Dimension, Set) applying} them and then exporting its constituent
     * layers, if any.
     *
     * <p>The exporter does implement {@link FirstPassLayerExporter} and {@link SecondPassLayerExporter} though,
     * to signal the fact that it can contain layers for both phases.
     *
     * @return A dummy exporter which always throws {@code UnsupportedOperationException}.
     */
    @Override
    public LayerExporter getExporter(Dimension dimension, Platform platform, ExporterSettings settings) {
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
     * <p>Returns {@code false} if a custom terrain was present
     * but it could not be restored because all custom terrain slots are in use,
     * {@code true} in all other circumstances.
     *
     * <p><strong>Please note:</strong> if this returns {@code true} a new
     * custom terrain <em>may</em> have been installed, so in that case the
     * invoker MUST check whether a button already exists for the custom terrain
     * used by this combined layer, and add one if not.
     *
     * @return {@code false} if a custom terrain was present but it could
     *     not be restored because all custom terrain slots are in use,
     *     {@code true} otherwise.
     */
    public boolean restoreCustomTerrain() {
        if (customTerrainPresent) {
            if (customTerrainMaterial == null) {
                // This should not be possible, but due to earlier bugs there are worlds in the wild with a custom
                // terrain without a stored custom material. Not much we can do
                terrain = null;
                return false;
            } else {
                // See if the same material is already present and if so keep using it. Find an empty slot otherwise
                int slot = -1;
                for (int i = 0; i < Terrain.CUSTOM_TERRAIN_COUNT; i++) {
                    if (customTerrainMaterial.equals(Terrain.getCustomMaterial(i))) {
                        slot = i;
                        break;
                    } else if ((slot == -1) && (Terrain.getCustomMaterial(i) == null)) {
                        slot = i;
                    }
                }
                if (slot != -1) {
                    Terrain.setCustomMaterial(slot, customTerrainMaterial);
                    terrain = Terrain.getCustomTerrain(slot);
                    return true;
                } else {
                    // No more slots available. Not much we can do
                    terrain = null;
                    return false;
                }
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
    private boolean customTerrainPresent, applyTerrainAndBiomeOnExport = true;
    private transient MixedMaterial customTerrainMaterial;

    private static final LayerExporter EXPORTER = new CombinedLayerExporter();

    static class CombinedLayerExporter implements FirstPassLayerExporter, SecondPassLayerExporter {
        @Override
        public Layer getLayer() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void render(Tile tile, Chunk chunk) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Set<Stage> getStages() {
            return EnumSet.of(CARVE, ADD_FEATURES);
        }

        @Override
        public List<Fixup> carve(Rectangle area, Rectangle exportedArea, MinecraftWorld minecraftWorld) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<Fixup> addFeatures(Rectangle area, Rectangle exportedArea, MinecraftWorld minecraftWorld) {
            throw new UnsupportedOperationException();
        }
    }
}