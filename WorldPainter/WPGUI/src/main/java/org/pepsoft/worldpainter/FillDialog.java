/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/*
 * FillDialog.java
 *
 * Created on Mar 29, 2012, 1:07:15 PM
 */
package org.pepsoft.worldpainter;

import org.pepsoft.util.ObservableBoolean;
import org.pepsoft.util.ProgressReceiver;
import org.pepsoft.util.ProgressReceiver.OperationCancelled;
import org.pepsoft.util.swing.ProgressDialog;
import org.pepsoft.util.swing.ProgressTask;
import org.pepsoft.worldpainter.biomeschemes.AutoBiomeScheme;
import org.pepsoft.worldpainter.biomeschemes.BiomeHelper;
import org.pepsoft.worldpainter.biomeschemes.CustomBiomeManager;
import org.pepsoft.worldpainter.layers.Biome;
import org.pepsoft.worldpainter.layers.FloodWithLava;
import org.pepsoft.worldpainter.layers.Layer;
import org.pepsoft.worldpainter.operations.Filter;
import org.pepsoft.worldpainter.panels.BrushOptions.Listener;
import org.pepsoft.worldpainter.panels.DefaultFilter;
import org.pepsoft.worldpainter.selection.SelectionBlock;
import org.pepsoft.worldpainter.selection.SelectionChunk;
import org.pepsoft.worldpainter.selection.SelectionHelper;
import org.pepsoft.worldpainter.themes.TerrainListCellRenderer;

import javax.swing.*;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;

import static org.pepsoft.worldpainter.Constants.TILE_SIZE;
import static org.pepsoft.worldpainter.Constants.TILE_SIZE_BITS;

/**
 *
 * @author pepijn
 */
public class FillDialog extends WorldPainterDialog implements Listener {
    /** Creates new form FillDialog */
    public FillDialog(java.awt.Frame parent, Dimension dimension, Layer[] layers, ColourScheme colourScheme, Integer[] biomes, CustomBiomeManager customBiomeManager, WorldPainterView view, ObservableBoolean selectionState) {
        super(parent);
        this.dimension = dimension;
        this.colourScheme = colourScheme;
        this.view = view;
        biomeHelper = new BiomeHelper(new AutoBiomeScheme(null), colourScheme, customBiomeManager);
        
        initComponents();
        brushOptions1.setSelectionState(selectionState);
        
        comboBoxBiome.setModel(new DefaultComboBoxModel(biomes));
        comboBoxBiome.setRenderer(new BiomeListCellRenderer(colourScheme, customBiomeManager));
        
        comboBoxSetLayer.setModel(new DefaultComboBoxModel(layers));
        comboBoxSetLayer.setRenderer(new LayerListCellRenderer());
        
        Set<Layer> layersInUse = dimension.getAllLayers(false);
        layersInUse.removeAll(Arrays.asList(Biome.INSTANCE, FloodWithLava.INSTANCE, SelectionBlock.INSTANCE, SelectionChunk.INSTANCE));
        if (! layersInUse.isEmpty()) {
            comboBoxClearLayer.setModel(new DefaultComboBoxModel(layersInUse.toArray(new Layer[layersInUse.size()])));
            comboBoxClearLayer.setRenderer(new LayerListCellRenderer());
        } else {
            comboBoxClearLayer.setEnabled(false);
            radioButtonClearLayer.setEnabled(false);
        }

        comboBoxInvertLayer.setModel(new DefaultComboBoxModel(layers));
        comboBoxInvertLayer.setRenderer(new LayerListCellRenderer());

        brushOptions1.setListener(this);
        
        getRootPane().setDefaultButton(buttonFill);
        
        pack(); // The comboboxes' preferred sizes have changed because the
                // models have been set
        setLocationRelativeTo(parent);
        
        setControlStates();
    }

    public ColourScheme getColourScheme() {
        return colourScheme;
    }

    // BrushOptions.Listener
    
    @Override
    public void filterChanged(Filter newFilter) {
        filter = newFilter;
        pack();
    }
    
    private void setControlStates() {
        comboBoxTerrain.setEnabled(radioButtonTerrain.isSelected());
        comboBoxSetLayer.setEnabled(radioButtonSetLayer.isSelected());
        sliderLayerValue.setEnabled(radioButtonSetLayer.isSelected() && ((((Layer) comboBoxSetLayer.getSelectedItem()).getDataSize() == Layer.DataSize.BYTE) || (((Layer) comboBoxSetLayer.getSelectedItem()).getDataSize() == Layer.DataSize.NIBBLE)));
        comboBoxClearLayer.setEnabled(radioButtonClearLayer.isSelected());
        comboBoxInvertLayer.setEnabled(radioButtonInvertLayer.isSelected());
        comboBoxBiome.setEnabled(radioButtonBiome.isSelected());
        buttonFill.setEnabled(radioButtonTerrain.isSelected() || radioButtonSetLayer.isSelected() || radioButtonClearLayer.isSelected() || radioButtonInvertLayer.isSelected() || radioButtonBiome.isSelected() || radioButtonResetBiomes.isSelected() || radioButtonResetWater.isSelected() || radioButtonResetTerrain.isSelected() || radioButtonMakeBiomesPermanent.isSelected() || radioButtonAddToSelection.isSelected() || radioButtonRemoveFromSelection.isSelected());
    }
    
    private void fill() {
        dimension.setEventsInhibited(true);
        dimension.rememberChanges();
        view.setInhibitUpdates(true);
        try {
            Dimension result = ProgressDialog.executeTask(this, new ProgressTask<Dimension>() {
                @Override
                public String getName() {
                    if (radioButtonTerrain.isSelected()) {
                        return "Filling with " + ((Terrain) comboBoxTerrain.getSelectedItem()).getName();
                    } else if (radioButtonSetLayer.isSelected()) {
                        return "Filling with " + ((Layer) comboBoxSetLayer.getSelectedItem()).getName();
                    } else if (radioButtonClearLayer.isSelected()) {
                        return "Clearing " + ((Layer) comboBoxSetLayer.getSelectedItem()).getName();
                    } else if (radioButtonInvertLayer.isSelected()) {
                        return "Inverting " + ((Layer) comboBoxInvertLayer.getSelectedItem()).getName();
                    } else if (radioButtonBiome.isSelected()) {
                        return "Filling with " + biomeHelper.getBiomeName((Integer) comboBoxBiome.getSelectedItem());
                    } else if (radioButtonResetBiomes.isSelected()) {
                        return "Resetting biomes to automatic";
                    } else if (radioButtonResetWater.isSelected()) {
                        return "Resetting water or lava";
                    } else if (radioButtonResetTerrain.isSelected()) {
                        return "Resetting terrain types";
                    } else if (radioButtonMakeBiomesPermanent.isSelected()) {
                        return "Making automatic biomes permanent";
                    } else if (radioButtonAddToSelection.isSelected()) {
                        return "Adding to selection";
                    } else if (radioButtonRemoveFromSelection.isSelected()) {
                        return "Removing from selection";
                    } else {
                        throw new InternalError();
                    }
                }

                @Override
                public Dimension execute(ProgressReceiver progressReceiver) throws OperationCancelled {
                    if (radioButtonTerrain.isSelected()) {
                        fillWithTerrain(progressReceiver);
                    } else if (radioButtonSetLayer.isSelected()) {
                        fillWithLayer(progressReceiver);
                    } else if (radioButtonClearLayer.isSelected()) {
                        clearLayer(progressReceiver);
                    } else if (radioButtonInvertLayer.isSelected()) {
                        invertLayer(progressReceiver);
                    } else if (radioButtonBiome.isSelected()) {
                        fillWithBiome(progressReceiver);
                    } else if (radioButtonResetBiomes.isSelected()) {
                        resetBiomes(progressReceiver);
                    } else if (radioButtonResetWater.isSelected()) {
                        resetWater(progressReceiver);
                    } else if (radioButtonResetTerrain.isSelected()) {
                        resetTerrain(progressReceiver);
                    } else if (radioButtonMakeBiomesPermanent.isSelected()) {
                        makeAutoBiomesPermanent(progressReceiver);
                    } else if (radioButtonAddToSelection.isSelected()) {
                        addToSelection(progressReceiver);
                    } else if (radioButtonRemoveFromSelection.isSelected()) {
                        removeFromSelection(progressReceiver);
                    }
                    return dimension;
                }
            });
            if (result == null) {
                // Cancelled by user
                if (dimension.undoChanges()) {
                    dimension.clearRedo();
                }
                cancel();
            } else {
                dimension.armSavePoint();
                ok();
            }
        } finally {
            view.setInhibitUpdates(false);
            dimension.setEventsInhibited(false);
        }
    }

    private void fillWithTerrain(ProgressReceiver progressReceiver) throws OperationCancelled {
        final Terrain terrain = (Terrain) comboBoxTerrain.getSelectedItem();
        dimension.visitTiles().forFilter(filter).andDo(tile -> {
            final int worldTileX = tile.getX() << TILE_SIZE_BITS;
            final int worldTileY = tile.getY() << TILE_SIZE_BITS;
            for (int x = 0; x < TILE_SIZE; x++) {
                for (int y = 0; y < TILE_SIZE; y++) {
                    boolean set;
                    if (filter == null) {
                        set = true;
                    } else {
                        float strength = filter.modifyStrength(worldTileX | x, worldTileY | y, 1.0f);
                        set = (strength > 0.95f) || (Math.random() < strength);
                    }
                    if (set && (tile.getTerrain(x, y) != terrain)) {
                        tile.setTerrain(x, y, terrain);
                    }
                }
            }
        }, progressReceiver);
    }

    private void fillWithLayer(ProgressReceiver progressReceiver) throws UnsupportedOperationException, OperationCancelled {
        Layer layer = (Layer) comboBoxSetLayer.getSelectedItem();
        if (layer.getDataSize() == Layer.DataSize.NIBBLE) {
            int baseLayerValue = Math.round((sliderLayerValue.getValue() + 2) / 6.667f);
            dimension.visitTiles().forFilter(filter).andDo(tile -> {
                final int worldTileX = tile.getX() << TILE_SIZE_BITS;
                final int worldTileY = tile.getY() << TILE_SIZE_BITS;
                for (int x = 0; x < TILE_SIZE; x++) {
                    for (int y = 0; y < TILE_SIZE; y++) {
                        int layerValue;
                        if (filter == null) {
                            layerValue = baseLayerValue;
                        } else {
                            layerValue = (int) (filter.modifyStrength(worldTileX | x, worldTileY | y, 1.0f) * baseLayerValue);
                        }
                        if (tile.getLayerValue(layer, x, y) < layerValue) {
                            tile.setLayerValue(layer, x, y, layerValue);
                        }
                    }
                }
            }, progressReceiver);
        } else if (layer.getDataSize() == Layer.DataSize.BIT) {
            dimension.visitTiles().forFilter(filter).andDo(tile -> {
                final int worldTileX = tile.getX() << TILE_SIZE_BITS;
                final int worldTileY = tile.getY() << TILE_SIZE_BITS;
                for (int x = 0; x < TILE_SIZE; x++) {
                    for (int y = 0; y < TILE_SIZE; y++) {
                        boolean set;
                        if (filter == null) {
                            set = true;
                        } else {
                            float strength = filter.modifyStrength(worldTileX | x, worldTileY | y, 1.0f);
                            set = (strength > 0.95f) || (Math.random() < strength);
                        }
                        if (set && (! tile.getBitLayerValue(layer, x, y))) {
                            tile.setBitLayerValue(layer, x, y, true);
                        }
                    }
                }
            }, progressReceiver);
        } else if (layer.getDataSize() == Layer.DataSize.BIT_PER_CHUNK) {
            dimension.visitTiles().forFilter(filter).andDo(tile -> {
                final int worldTileX = tile.getX() << TILE_SIZE_BITS;
                final int worldTileY = tile.getY() << TILE_SIZE_BITS;
                for (int x = 0; x < TILE_SIZE; x += 16) {
                    for (int y = 0; y < TILE_SIZE; y += 16) {
                        boolean set;
                        if (filter == null) {
                            set = true;
                        } else {
                            float strength = filter.modifyStrength(worldTileX | x, worldTileY | y, 1.0f);
                            set = (strength > 0.95f) || (Math.random() < strength);
                        }
                        if (set && (! tile.getBitLayerValue(layer, x, y))) {
                            tile.setBitLayerValue(layer, x, y, true);
                        }
                    }
                }
            }, progressReceiver);
        } else {
            throw new UnsupportedOperationException();
        }
    }

    private void clearLayer(ProgressReceiver progressReceiver) throws OperationCancelled {
        Layer layer = (Layer) comboBoxClearLayer.getSelectedItem();
        if (filter == null) {
            dimension.clearLayerData(layer);
        } else {
            if (layer.getDataSize() == Layer.DataSize.NIBBLE) {
                dimension.visitTiles().forFilter(filter).andDo(tile -> {
                    final int worldTileX = tile.getX() << TILE_SIZE_BITS;
                    final int worldTileY = tile.getY() << TILE_SIZE_BITS;
                    for (int x = 0; x < TILE_SIZE; x++) {
                        for (int y = 0; y < TILE_SIZE; y++) {
                            int oldLayervalue = tile.getLayerValue(layer, x, y);
                            int layerValue;
                            if (filter == null) {
                                layerValue = 0;
                            } else {
                                layerValue = Math.min(oldLayervalue, 15 - (int) (filter.modifyStrength(worldTileX | x, worldTileY | y, 1.0f) * 15));
                            }
                            if (oldLayervalue != layerValue) {
                                tile.setLayerValue(layer, x, y, layerValue);
                            }
                        }
                    }
                }, progressReceiver);
            } else if (layer.getDataSize() == Layer.DataSize.BIT) {
                dimension.visitTiles().forFilter(filter).andDo(tile -> {
                    final int worldTileX = tile.getX() << TILE_SIZE_BITS;
                    final int worldTileY = tile.getY() << TILE_SIZE_BITS;
                    for (int x = 0; x < TILE_SIZE; x++) {
                        for (int y = 0; y < TILE_SIZE; y++) {
                            boolean set;
                            if (filter == null) {
                                set = true;
                            } else {
                                float strength = filter.modifyStrength(worldTileX | x, worldTileY | y, 1.0f);
                                set = (strength > 0.95f) || (Math.random() < strength);
                            }
                            if (set && tile.getBitLayerValue(layer, x, y)) {
                                tile.setBitLayerValue(layer, x, y, false);
                            }
                        }
                    }
                }, progressReceiver);
            } else if (layer.getDataSize() == Layer.DataSize.BIT_PER_CHUNK) {
                dimension.visitTiles().forFilter(filter).andDo(tile -> {
                    final int worldTileX = tile.getX() << TILE_SIZE_BITS;
                    final int worldTileY = tile.getY() << TILE_SIZE_BITS;
                    for (int x = 0; x < TILE_SIZE; x += 16) {
                        for (int y = 0; y < TILE_SIZE; y += 16) {
                            boolean set;
                            if (filter == null) {
                                set = true;
                            } else {
                                float strength = filter.modifyStrength(worldTileX | x, worldTileY | y, 1.0f);
                                set = (strength > 0.95f) || (Math.random() < strength);
                            }
                            if (set && tile.getBitLayerValue(layer, x, y)) {
                                tile.setBitLayerValue(layer, x, y, false);
                            }
                        }
                    }
                }, progressReceiver);
            } else {
                throw new UnsupportedOperationException();
            }
        }
    }

    private void invertLayer(ProgressReceiver progressReceiver) throws UnsupportedOperationException, OperationCancelled {
        Layer layer = (Layer) comboBoxInvertLayer.getSelectedItem();
        if (layer.getDataSize() == Layer.DataSize.NIBBLE) {
            dimension.visitTiles().forFilter(filter).andDo(tile -> {
                final int worldTileX = tile.getX() << TILE_SIZE_BITS;
                final int worldTileY = tile.getY() << TILE_SIZE_BITS;
                for (int x = 0; x < TILE_SIZE; x++) {
                    for (int y = 0; y < TILE_SIZE; y++) {
                        boolean set;
                        if (filter == null) {
                            set = true;
                        } else {
                            float strength = filter.modifyStrength(worldTileX | x, worldTileY | y, 1.0f);
                            set = (strength > 0.95f) || (Math.random() < strength);
                        }
                        if (set) {
                            tile.setLayerValue(layer, x, y, 15 - tile.getLayerValue(layer, x, y));
                        }
                    }
                }
            }, progressReceiver);
        } else if (layer.getDataSize() == Layer.DataSize.BIT) {
            dimension.visitTiles().forFilter(filter).andDo(tile -> {
                final int worldTileX = tile.getX() << TILE_SIZE_BITS;
                final int worldTileY = tile.getY() << TILE_SIZE_BITS;
                for (int x = 0; x < TILE_SIZE; x++) {
                    for (int y = 0; y < TILE_SIZE; y++) {
                        boolean set;
                        if (filter == null) {
                            set = true;
                        } else {
                            float strength = filter.modifyStrength(worldTileX | x, worldTileY | y, 1.0f);
                            set = (strength > 0.95f) || (Math.random() < strength);
                        }
                        if (set) {
                            tile.setBitLayerValue(layer, x, y, ! tile.getBitLayerValue(layer, x, y));
                        }
                    }
                }
            }, progressReceiver);
        } else if (layer.getDataSize() == Layer.DataSize.BIT_PER_CHUNK) {
            dimension.visitTiles().forFilter(filter).andDo(tile -> {
                final int worldTileX = tile.getX() << TILE_SIZE_BITS;
                final int worldTileY = tile.getY() << TILE_SIZE_BITS;
                for (int x = 0; x < TILE_SIZE; x += 16) {
                    for (int y = 0; y < TILE_SIZE; y += 16) {
                        boolean set;
                        if (filter == null) {
                            set = true;
                        } else {
                            float strength = filter.modifyStrength(worldTileX | x, worldTileY | y, 1.0f);
                            set = (strength > 0.95f) || (Math.random() < strength);
                        }
                        if (set) {
                            tile.setBitLayerValue(layer, x, y, ! tile.getBitLayerValue(layer, x, y));
                        }
                    }
                }
            }, progressReceiver);
        } else {
            throw new UnsupportedOperationException();
        }
    }

    private void fillWithBiome(ProgressReceiver progressReceiver) throws OperationCancelled {
        int biome = (Integer) comboBoxBiome.getSelectedItem();
        dimension.visitTiles().forFilter(filter).andDo(tile -> {
            final int worldTileX = tile.getX() << TILE_SIZE_BITS;
            final int worldTileY = tile.getY() << TILE_SIZE_BITS;
            for (int x = 0; x < TILE_SIZE; x++) {
                for (int y = 0; y < TILE_SIZE; y++) {
                    boolean set;
                    if (filter == null) {
                        set = true;
                    } else {
                        float strength = filter.modifyStrength(worldTileX | x, worldTileY | y, 1.0f);
                        set = (strength > 0.95f) || (Math.random() < strength);
                    }
                    if (set) {
                        tile.setLayerValue(Biome.INSTANCE, x, y, biome);
                    }
                }
            }
        }, progressReceiver);
    }
    
    private void resetBiomes(ProgressReceiver progressReceiver) throws OperationCancelled {
        if (filter == null) {
            dimension.clearLayerData(Biome.INSTANCE);
        } else {
            dimension.visitTiles().forFilter(filter).andDo(tile -> {
                final int worldTileX = tile.getX() << TILE_SIZE_BITS;
                final int worldTileY = tile.getY() << TILE_SIZE_BITS;
                for (int x = 0; x < TILE_SIZE; x++) {
                    for (int y = 0; y < TILE_SIZE; y++) {
                        final float strength = filter.modifyStrength(worldTileX | x, worldTileY | y, 1.0f);
                        if ((strength > 0.95f) || (Math.random() < strength)) {
                            tile.setLayerValue(Biome.INSTANCE, x, y, 255);
                        }
                    }
                }
            }, progressReceiver);
        }
    }

    private void makeAutoBiomesPermanent(ProgressReceiver progressReceiver) throws OperationCancelled {
        dimension.visitTiles().forFilter(filter).andDo(tile -> {
            if (filter == null) {
                for (int x = 0; x < TILE_SIZE; x++) {
                    for (int y = 0; y < TILE_SIZE; y++) {
                        if (tile.getLayerValue(Biome.INSTANCE, x, y) == 255) {
                            tile.setLayerValue(Biome.INSTANCE, x, y, dimension.getAutoBiome(tile, x, y));
                        }
                    }
                }
            } else {
                final int worldTileX = tile.getX() << TILE_SIZE_BITS;
                final int worldTileY = tile.getY() << TILE_SIZE_BITS;
                for (int x = 0; x < TILE_SIZE; x++) {
                    for (int y = 0; y < TILE_SIZE; y++) {
                        final float strength = filter.modifyStrength(worldTileX | x, worldTileY | y, 1.0f);
                        if (((strength > 0.95f) || (Math.random() < strength)) && (tile.getLayerValue(Biome.INSTANCE, x, y) == 255)) {
                            tile.setLayerValue(Biome.INSTANCE, x, y, dimension.getAutoBiome(tile, x, y));
                        }
                    }
                }
            }
        }, progressReceiver);
    }
    
    private void resetWater(ProgressReceiver progressReceiver) throws OperationCancelled, UnsupportedOperationException {
        TileFactory tileFactory = dimension.getTileFactory();
        if (tileFactory instanceof HeightMapTileFactory) {
            int waterLevel = ((HeightMapTileFactory) tileFactory).getWaterHeight();
            boolean floodWithLava = ((HeightMapTileFactory) tileFactory).isFloodWithLava();
            dimension.visitTiles().forFilter(filter).andDo(tile -> {
                final int worldTileX = tile.getX() << TILE_SIZE_BITS;
                final int worldTileY = tile.getY() << TILE_SIZE_BITS;
                if (floodWithLava) {
                    for (int x = 0; x < TILE_SIZE; x++) {
                        for (int y = 0; y < TILE_SIZE; y++) {
                            boolean set;
                            if (filter == null) {
                                set = true;
                            } else {
                                float strength = filter.modifyStrength(worldTileX | x, worldTileY | y, 1.0f);
                                set = (strength > 0.95f) || (Math.random() < strength);
                            }
                            if (set) {
                                tile.setWaterLevel(x, y, waterLevel);
                                tile.setBitLayerValue(FloodWithLava.INSTANCE, x, y, true);
                            }
                        }
                    }
                } else {
                    if (filter == null) {
                        tile.clearLayerData(FloodWithLava.INSTANCE);
                    }
                    for (int x = 0; x < TILE_SIZE; x++) {
                        for (int y = 0; y < TILE_SIZE; y++) {
                            boolean set;
                            if (filter == null) {
                                set = true;
                            } else {
                                float strength = filter.modifyStrength(worldTileX | x, worldTileY | y, 1.0f);
                                set = (strength > 0.95f) || (Math.random() < strength);
                            }
                            if (set) {
                                tile.setWaterLevel(x, y, waterLevel);
                                if (filter != null) {
                                    tile.setBitLayerValue(FloodWithLava.INSTANCE, x, y, false);
                                }
                            }
                        }
                    }
                }
            }, progressReceiver);
        } else {
            throw new UnsupportedOperationException("Tile factory type " + tileFactory.getClass() + " not supported");
        }
    }

    private void resetTerrain(ProgressReceiver progressReceiver) throws OperationCancelled {
        dimension.visitTiles().forFilter(filter).andDo(tile -> {
            final int worldTileX = tile.getX() << TILE_SIZE_BITS;
            final int worldTileY = tile.getY() << TILE_SIZE_BITS;
            for (int x = 0; x < TILE_SIZE; x++) {
                for (int y = 0; y < TILE_SIZE; y++) {
                    boolean set;
                    if (filter == null) {
                        set = true;
                    } else {
                        float strength = filter.modifyStrength(worldTileX | x, worldTileY | y, 1.0f);
                        set = (strength > 0.95f) || (Math.random() < strength);
                    }
                    if (set) {
                        dimension.applyTheme(worldTileX | x, worldTileY | y);
                    }
                }
            }
        }, progressReceiver);
    }

    private void addToSelection(ProgressReceiver progressReceiver) throws OperationCancelled {
        final boolean[][] blocksSet = new boolean[16][16];
        dimension.visitTiles().forFilter(filter).andDo(tile -> {
            final boolean tileHasChunkSelection = tile.hasLayer(SelectionChunk.INSTANCE);
            if (filter == null) {
                // This is slightly odd, but whatever. Just add all chunks to
                // the selection
                tile.clearLayerData(SelectionBlock.INSTANCE);
                for (int chunkX = 0; chunkX < TILE_SIZE; chunkX += 16) {
                    for (int chunkY = 0; chunkY < TILE_SIZE; chunkY += 16) {
                        if ((! tileHasChunkSelection) || (! tile.getBitLayerValue(SelectionChunk.INSTANCE, chunkX, chunkY))) {
                            tile.setBitLayerValue(SelectionChunk.INSTANCE, chunkX, chunkY, true);
                        }
                    }
                }
            } else {
                final int worldTileX = tile.getX() << TILE_SIZE_BITS;
                final int worldTileY = tile.getY() << TILE_SIZE_BITS;
                final boolean tileHasBlockSelection = tile.hasLayer(SelectionBlock.INSTANCE);
                // Check per chunk whether the entire chunk would be selected, so
                // we can use the more efficient per-chunk selection layer
chunks:         for (int chunkX = 0; chunkX < TILE_SIZE; chunkX += 16) {
                    for (int chunkY = 0; chunkY < TILE_SIZE; chunkY += 16) {
                        if (tileHasChunkSelection && tile.getBitLayerValue(SelectionChunk.INSTANCE, chunkX, chunkY)) {
                            // The chunk is already entirely selected, so we can
                            // just skip it
                            continue chunks;
                        }
                        boolean chunkEntirelySelected = true;
                        boolean noSelection = true;
                        for (int xInChunk = 0; xInChunk < 16; xInChunk++) {
                            for (int yInChunk = 0; yInChunk < 16; yInChunk++) {
                                float strength = filter.modifyStrength(worldTileX | chunkX | xInChunk, worldTileY | chunkY | yInChunk, 1.0f);
                                boolean select = (strength > 0.95f) || (Math.random() < strength);
                                blocksSet[xInChunk][yInChunk] = select;
                                if (! select) {
                                    chunkEntirelySelected = false;
                                } else {
                                    noSelection = false;
                                }
                            }
                        }

                        if (noSelection) {
                            // Nothing has to be selected; we don't have to
                            // make any changes to the chunk
                        } else if (chunkEntirelySelected) {
                            // The chunk is entirely selected; optimise by using
                            // the per-chunk selection layer, and remove any
                            // existing per-block selection layer
                            tile.setBitLayerValue(SelectionChunk.INSTANCE, chunkX, chunkY, true);
                            if (tileHasBlockSelection) {
                                for (int xInChunk = 0; xInChunk < 16; xInChunk++) {
                                    for (int yInChunk = 0; yInChunk < 16; yInChunk++) {
                                        tile.setBitLayerValue(SelectionBlock.INSTANCE, chunkX | xInChunk, chunkY | yInChunk, false);
                                    }
                                }
                            }
                        } else {
                            // The chunk is not entirely selected, so apply the
                            // selection per-block. TODO: recognise when the chunk becomes entirely selected so we should use the per-block layer
                            for (int xInChunk = 0; xInChunk < 16; xInChunk++) {
                                for (int yInChunk = 0; yInChunk < 16; yInChunk++) {
                                    if (blocksSet[xInChunk][yInChunk]) {
                                        tile.setBitLayerValue(SelectionBlock.INSTANCE, chunkX | xInChunk, chunkY | yInChunk, true);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }, progressReceiver);
    }

    private void removeFromSelection(ProgressReceiver progressReceiver) throws OperationCancelled {
        final boolean[][] blocksDeselected = new boolean[16][16];
        if (filter == null) {
            dimension.clearLayerData(SelectionChunk.INSTANCE);
            dimension.clearLayerData(SelectionBlock.INSTANCE);
        } else {
            dimension.visitTiles().forFilter(filter).andDo(tile -> {
                final boolean tileHasChunkSelection = tile.hasLayer(SelectionChunk.INSTANCE);
                final boolean tileHasBlockSelection = tile.hasLayer(SelectionBlock.INSTANCE);
                if ((! tileHasChunkSelection) && (! tileHasBlockSelection)) {
                    // There is no selection in this tile so we can just skip it
                    return;
                }
                final int worldTileX = tile.getX() << TILE_SIZE_BITS;
                final int worldTileY = tile.getY() << TILE_SIZE_BITS;
                // Check per chunk whether the entire chunk would be deselected,
                // so we can use the more efficient per-chunk selection layer
                for (int chunkX = 0; chunkX < TILE_SIZE; chunkX += 16) {
                    for (int chunkY = 0; chunkY < TILE_SIZE; chunkY += 16) {
                        boolean chunkEntirelyDeselected = true;
                        boolean noDeselection = true;
                        for (int xInChunk = 0; xInChunk < 16; xInChunk++) {
                            for (int yInChunk = 0; yInChunk < 16; yInChunk++) {
                                float strength = filter.modifyStrength(worldTileX | chunkX | xInChunk, worldTileY | chunkY | yInChunk, 1.0f);
                                boolean deselect = (strength > 0.95f) || (Math.random() < strength);
                                blocksDeselected[xInChunk][yInChunk] = deselect;
                                if (! deselect) {
                                    chunkEntirelyDeselected = false;
                                } else {
                                    noDeselection = false;
                                }
                            }
                        }

                        if (noDeselection) {
                            // Nothing has to be deselected; we don't have to
                            // make any changes to the chunk
                        } else if (chunkEntirelyDeselected) {
                            // The chunk should be entirely deselected; just
                            // remove the layers
                            if (tileHasChunkSelection) {
                                tile.setBitLayerValue(SelectionChunk.INSTANCE, chunkX, chunkY, false);
                            }
                            if (tileHasBlockSelection) {
                                for (int xInChunk = 0; xInChunk < 16; xInChunk++) {
                                    for (int yInChunk = 0; yInChunk < 16; yInChunk++) {
                                        tile.setBitLayerValue(SelectionBlock.INSTANCE, chunkX | xInChunk, chunkY | yInChunk, false);
                                    }
                                }
                            }
                        } else {
                            // The chunk should not be entirely deselected
                            if (tileHasChunkSelection && tile.getBitLayerValue(SelectionChunk.INSTANCE, chunkX, chunkY)) {
                                // The chunk is currently entirely selected;
                                // commute it to per-block
                                tile.setBitLayerValue(SelectionChunk.INSTANCE, chunkX, chunkY, false);
                                for (int xInChunk = 0; xInChunk < 16; xInChunk++) {
                                    for (int yInChunk = 0; yInChunk < 16; yInChunk++) {
                                        if (! blocksDeselected[xInChunk][yInChunk]) {
                                            tile.setBitLayerValue(SelectionBlock.INSTANCE, chunkX | xInChunk, chunkY | yInChunk, true);
                                        }
                                    }
                                }
                            } else {
                                // The chunk is already using per-block
                                // selection; just remove the blocks to deselect
                                for (int xInChunk = 0; xInChunk < 16; xInChunk++) {
                                    for (int yInChunk = 0; yInChunk < 16; yInChunk++) {
                                        if (blocksDeselected[xInChunk][yInChunk]) {
                                            tile.setBitLayerValue(SelectionBlock.INSTANCE, chunkX | xInChunk, chunkY | yInChunk, false);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }, progressReceiver);
        }
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        buttonGroup1 = new javax.swing.ButtonGroup();
        jLabel1 = new javax.swing.JLabel();
        buttonCancel = new javax.swing.JButton();
        buttonFill = new javax.swing.JButton();
        jSeparator1 = new javax.swing.JSeparator();
        brushOptions1 = new org.pepsoft.worldpainter.panels.BrushOptions();
        jPanel1 = new javax.swing.JPanel();
        sliderLayerValue = new javax.swing.JSlider();
        comboBoxBiome = new javax.swing.JComboBox();
        radioButtonSetLayer = new javax.swing.JRadioButton();
        radioButtonResetBiomes = new javax.swing.JRadioButton();
        radioButtonClearLayer = new javax.swing.JRadioButton();
        radioButtonResetTerrain = new javax.swing.JRadioButton();
        radioButtonTerrain = new javax.swing.JRadioButton();
        comboBoxClearLayer = new javax.swing.JComboBox();
        radioButtonResetWater = new javax.swing.JRadioButton();
        comboBoxSetLayer = new javax.swing.JComboBox();
        comboBoxInvertLayer = new javax.swing.JComboBox();
        comboBoxTerrain = new javax.swing.JComboBox();
        radioButtonInvertLayer = new javax.swing.JRadioButton();
        radioButtonBiome = new javax.swing.JRadioButton();
        radioButtonMakeBiomesPermanent = new javax.swing.JRadioButton();
        radioButtonAddToSelection = new javax.swing.JRadioButton();
        radioButtonRemoveFromSelection = new javax.swing.JRadioButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Global Operations");

        jLabel1.setText("Perform a global operation:");

        buttonCancel.setText("Cancel");
        buttonCancel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonCancelActionPerformed(evt);
            }
        });

        buttonFill.setText("Go");
        buttonFill.setEnabled(false);
        buttonFill.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonFillActionPerformed(evt);
            }
        });

        jSeparator1.setOrientation(javax.swing.SwingConstants.VERTICAL);

        sliderLayerValue.setMajorTickSpacing(7);
        sliderLayerValue.setMinimum(2);
        sliderLayerValue.setPaintTicks(true);
        sliderLayerValue.setSnapToTicks(true);
        sliderLayerValue.setEnabled(false);

        comboBoxBiome.setEnabled(false);

        buttonGroup1.add(radioButtonSetLayer);
        radioButtonSetLayer.setText("fill with layer:");
        radioButtonSetLayer.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                radioButtonSetLayerActionPerformed(evt);
            }
        });

        buttonGroup1.add(radioButtonResetBiomes);
        radioButtonResetBiomes.setText("reset biomes to automatic");
        radioButtonResetBiomes.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                radioButtonResetBiomesActionPerformed(evt);
            }
        });

        buttonGroup1.add(radioButtonClearLayer);
        radioButtonClearLayer.setText("remove a layer:");
        radioButtonClearLayer.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                radioButtonClearLayerActionPerformed(evt);
            }
        });

        buttonGroup1.add(radioButtonResetTerrain);
        radioButtonResetTerrain.setText("reset terrain and layers to theme");
        radioButtonResetTerrain.setToolTipText("Reset the terrain type of the entire map to the altitude-dependent default");
        radioButtonResetTerrain.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                radioButtonResetTerrainActionPerformed(evt);
            }
        });

        buttonGroup1.add(radioButtonTerrain);
        radioButtonTerrain.setText("fill with terrain type:");
        radioButtonTerrain.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                radioButtonTerrainActionPerformed(evt);
            }
        });

        comboBoxClearLayer.setEnabled(false);

        buttonGroup1.add(radioButtonResetWater);
        radioButtonResetWater.setText("reset all water or lava");
        radioButtonResetWater.setToolTipText("This resets the fluid level and type (water or lava) to the default everywhere");
        radioButtonResetWater.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                radioButtonResetWaterActionPerformed(evt);
            }
        });

        comboBoxSetLayer.setEnabled(false);
        comboBoxSetLayer.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                comboBoxSetLayerActionPerformed(evt);
            }
        });

        comboBoxInvertLayer.setEnabled(false);

        comboBoxTerrain.setModel(new DefaultComboBoxModel(Terrain.getConfiguredValues()));
        comboBoxTerrain.setEnabled(false);
        comboBoxTerrain.setRenderer(new TerrainListCellRenderer(colourScheme));

        buttonGroup1.add(radioButtonInvertLayer);
        radioButtonInvertLayer.setText("invert a layer:");
        radioButtonInvertLayer.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                radioButtonInvertLayerActionPerformed(evt);
            }
        });

        buttonGroup1.add(radioButtonBiome);
        radioButtonBiome.setText("fill with biome:");
        radioButtonBiome.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                radioButtonBiomeActionPerformed(evt);
            }
        });

        buttonGroup1.add(radioButtonMakeBiomesPermanent);
        radioButtonMakeBiomesPermanent.setText("make automatic biomes permanent");
        radioButtonMakeBiomesPermanent.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                radioButtonMakeBiomesPermanentActionPerformed(evt);
            }
        });

        buttonGroup1.add(radioButtonAddToSelection);
        radioButtonAddToSelection.setText("add to selection");
        radioButtonAddToSelection.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                radioButtonAddToSelectionActionPerformed(evt);
            }
        });

        buttonGroup1.add(radioButtonRemoveFromSelection);
        radioButtonRemoveFromSelection.setText("remove from selection");
        radioButtonRemoveFromSelection.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                radioButtonRemoveFromSelectionActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(radioButtonSetLayer)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(comboBoxSetLayer, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(12, 12, 12)
                        .addComponent(sliderLayerValue, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(radioButtonClearLayer)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(comboBoxClearLayer, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(radioButtonBiome)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(comboBoxBiome, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(radioButtonInvertLayer)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(comboBoxInvertLayer, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(radioButtonTerrain)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(comboBoxTerrain, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(radioButtonResetTerrain)
                    .addComponent(radioButtonResetBiomes)
                    .addComponent(radioButtonMakeBiomesPermanent)
                    .addComponent(radioButtonAddToSelection)
                    .addComponent(radioButtonRemoveFromSelection)
                    .addComponent(radioButtonResetWater))
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(radioButtonTerrain)
                    .addComponent(comboBoxTerrain, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(radioButtonResetTerrain)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(radioButtonResetWater)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(radioButtonSetLayer)
                    .addComponent(comboBoxSetLayer, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(sliderLayerValue, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(radioButtonClearLayer)
                    .addComponent(comboBoxClearLayer, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(radioButtonInvertLayer)
                    .addComponent(comboBoxInvertLayer, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(radioButtonBiome)
                    .addComponent(comboBoxBiome, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(radioButtonMakeBiomesPermanent)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(radioButtonResetBiomes)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(radioButtonAddToSelection)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(radioButtonRemoveFromSelection))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel1)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 0, 0)
                        .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, 2, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(brushOptions1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(buttonFill)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(buttonCancel)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel1)
                .addGap(0, 0, 0)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jSeparator1)
                    .addComponent(brushOptions1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(buttonCancel)
                    .addComponent(buttonFill))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void buttonFillActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonFillActionPerformed
        fill();
    }//GEN-LAST:event_buttonFillActionPerformed

    private void buttonCancelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonCancelActionPerformed
        cancel();
    }//GEN-LAST:event_buttonCancelActionPerformed

    private void radioButtonMakeBiomesPermanentActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_radioButtonMakeBiomesPermanentActionPerformed
        setControlStates();
    }//GEN-LAST:event_radioButtonMakeBiomesPermanentActionPerformed

    private void radioButtonBiomeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_radioButtonBiomeActionPerformed
        setControlStates();
    }//GEN-LAST:event_radioButtonBiomeActionPerformed

    private void radioButtonInvertLayerActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_radioButtonInvertLayerActionPerformed
        setControlStates();
    }//GEN-LAST:event_radioButtonInvertLayerActionPerformed

    private void comboBoxSetLayerActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_comboBoxSetLayerActionPerformed
        setControlStates();
    }//GEN-LAST:event_comboBoxSetLayerActionPerformed

    private void radioButtonResetWaterActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_radioButtonResetWaterActionPerformed
        setControlStates();
    }//GEN-LAST:event_radioButtonResetWaterActionPerformed

    private void radioButtonTerrainActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_radioButtonTerrainActionPerformed
        setControlStates();
    }//GEN-LAST:event_radioButtonTerrainActionPerformed

    private void radioButtonResetTerrainActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_radioButtonResetTerrainActionPerformed
        setControlStates();
    }//GEN-LAST:event_radioButtonResetTerrainActionPerformed

    private void radioButtonClearLayerActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_radioButtonClearLayerActionPerformed
        setControlStates();
    }//GEN-LAST:event_radioButtonClearLayerActionPerformed

    private void radioButtonResetBiomesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_radioButtonResetBiomesActionPerformed
        setControlStates();
    }//GEN-LAST:event_radioButtonResetBiomesActionPerformed

    private void radioButtonSetLayerActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_radioButtonSetLayerActionPerformed
        setControlStates();
    }//GEN-LAST:event_radioButtonSetLayerActionPerformed

    private void radioButtonAddToSelectionActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_radioButtonAddToSelectionActionPerformed
        setControlStates();
    }//GEN-LAST:event_radioButtonAddToSelectionActionPerformed

    private void radioButtonRemoveFromSelectionActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_radioButtonRemoveFromSelectionActionPerformed
        setControlStates();
    }//GEN-LAST:event_radioButtonRemoveFromSelectionActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private org.pepsoft.worldpainter.panels.BrushOptions brushOptions1;
    private javax.swing.JButton buttonCancel;
    private javax.swing.JButton buttonFill;
    private javax.swing.ButtonGroup buttonGroup1;
    private javax.swing.JComboBox comboBoxBiome;
    private javax.swing.JComboBox comboBoxClearLayer;
    private javax.swing.JComboBox comboBoxInvertLayer;
    private javax.swing.JComboBox comboBoxSetLayer;
    private javax.swing.JComboBox comboBoxTerrain;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JRadioButton radioButtonAddToSelection;
    private javax.swing.JRadioButton radioButtonBiome;
    private javax.swing.JRadioButton radioButtonClearLayer;
    private javax.swing.JRadioButton radioButtonInvertLayer;
    private javax.swing.JRadioButton radioButtonMakeBiomesPermanent;
    private javax.swing.JRadioButton radioButtonRemoveFromSelection;
    private javax.swing.JRadioButton radioButtonResetBiomes;
    private javax.swing.JRadioButton radioButtonResetTerrain;
    private javax.swing.JRadioButton radioButtonResetWater;
    private javax.swing.JRadioButton radioButtonSetLayer;
    private javax.swing.JRadioButton radioButtonTerrain;
    private javax.swing.JSlider sliderLayerValue;
    // End of variables declaration//GEN-END:variables

    private final ColourScheme colourScheme;
    private final Dimension dimension;
    private final BiomeHelper biomeHelper;
    private final WorldPainterView view;
    private Filter filter;
    
    private static final long serialVersionUID = 1L;
}