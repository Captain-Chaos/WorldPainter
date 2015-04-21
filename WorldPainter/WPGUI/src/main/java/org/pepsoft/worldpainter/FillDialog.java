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

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Arrays;
import java.util.Set;
import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.DefaultComboBoxModel;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import org.pepsoft.util.ProgressReceiver;
import org.pepsoft.util.ProgressReceiver.OperationCancelled;
import org.pepsoft.util.swing.ProgressDialog;
import org.pepsoft.util.swing.ProgressTask;
import org.pepsoft.worldpainter.biomeschemes.AutoBiomeScheme;
import org.pepsoft.worldpainter.layers.Biome;
import org.pepsoft.worldpainter.layers.FloodWithLava;
import org.pepsoft.worldpainter.layers.Layer;
import org.pepsoft.worldpainter.themes.TerrainListCellRenderer;
import static org.pepsoft.worldpainter.Constants.*;
import org.pepsoft.worldpainter.biomeschemes.BiomeHelper;
import org.pepsoft.worldpainter.biomeschemes.CustomBiomeManager;
import org.pepsoft.worldpainter.operations.Filter;
import org.pepsoft.worldpainter.panels.BrushOptions.Listener;

/**
 *
 * @author pepijn
 */
public class FillDialog extends javax.swing.JDialog implements Listener {
    /** Creates new form FillDialog */
    public FillDialog(java.awt.Frame parent, Dimension dimension, Layer[] layers, ColourScheme colourScheme, Integer[] biomes, CustomBiomeManager customBiomeManager, WorldPainterView view) {
        super(parent, true);
        this.dimension = dimension;
        this.colourScheme = colourScheme;
        this.view = view;
        biomeHelper = new BiomeHelper(new AutoBiomeScheme(null), colourScheme, customBiomeManager);
        
        initComponents();
        
        comboBoxBiome.setModel(new DefaultComboBoxModel(biomes));
        comboBoxBiome.setRenderer(new BiomeListCellRenderer(new AutoBiomeScheme(null), colourScheme, customBiomeManager));
        
        comboBoxSetLayer.setModel(new DefaultComboBoxModel(layers));
        comboBoxSetLayer.setRenderer(new LayerListCellRenderer());
        
        Set<Layer> layersInUse = dimension.getAllLayers(false);
        layersInUse.removeAll(Arrays.asList(Biome.INSTANCE, FloodWithLava.INSTANCE));
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
        
        ActionMap actionMap = rootPane.getActionMap();
        actionMap.put("cancel", new AbstractAction("cancel") {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
            
            private static final long serialVersionUID = 1L;
        });

        InputMap inputMap = rootPane.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "cancel");

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
        buttonFill.setEnabled(radioButtonTerrain.isSelected() || radioButtonSetLayer.isSelected() || radioButtonClearLayer.isSelected() || radioButtonInvertLayer.isSelected() || radioButtonBiome.isSelected() || radioButtonResetBiomes.isSelected() || radioButtonResetWater.isSelected() || radioButtonResetTerrain.isSelected() || radioButtonMakeBiomesPermanent.isSelected());
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
                    }
                    return dimension;
                }
            });
            if (result == null) {
                // Cancelled by user
                if (dimension.undoChanges()) {
                    dimension.clearRedo();
                }
            }
            dimension.armSavePoint();
        } finally {
            view.setInhibitUpdates(false);
            dimension.setEventsInhibited(false);
        }
        dispose();
    }

    private void fillWithTerrain(ProgressReceiver progressReceiver) throws OperationCancelled {
        Terrain terrain = (Terrain) comboBoxTerrain.getSelectedItem();
        int totalTiles = dimension.getTileCount(), tileCount = 0;
        for (Tile tile: dimension.getTiles()) {
            final int worldTileX = tile.getX() << TILE_SIZE_BITS;
            final int worldTileY = tile.getY() << TILE_SIZE_BITS;
            tile.setEventsInhibited(true);
            try {
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
            } finally {
                tile.setEventsInhibited(false);
            }
            tileCount++;
            progressReceiver.setProgress((float) tileCount / totalTiles);
        }
    }

    private void fillWithLayer(ProgressReceiver progressReceiver) throws UnsupportedOperationException, OperationCancelled {
        Layer layer = (Layer) comboBoxSetLayer.getSelectedItem();
        if (layer.getDataSize() == Layer.DataSize.NIBBLE) {
            int baseLayerValue = Math.round((sliderLayerValue.getValue() + 2) / 6.667f);
            int totalTiles = dimension.getTileCount(), tileCount = 0;
            for (Tile tile: dimension.getTiles()) {
                final int worldTileX = tile.getX() << TILE_SIZE_BITS;
                final int worldTileY = tile.getY() << TILE_SIZE_BITS;
                tile.setEventsInhibited(true);
                try {
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
                } finally {
                    tile.setEventsInhibited(false);
                }
                tileCount++;
                progressReceiver.setProgress((float) tileCount / totalTiles);
            }
        } else if (layer.getDataSize() == Layer.DataSize.BIT) {
            int totalTiles = dimension.getTileCount(), tileCount = 0;
            for (Tile tile: dimension.getTiles()) {
                final int worldTileX = tile.getX() << TILE_SIZE_BITS;
                final int worldTileY = tile.getY() << TILE_SIZE_BITS;
                tile.setEventsInhibited(true);
                try {
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
                } finally {
                    tile.setEventsInhibited(false);
                }
                tileCount++;
                progressReceiver.setProgress((float) tileCount / totalTiles);
            }
        } else if (layer.getDataSize() == Layer.DataSize.BIT_PER_CHUNK) {
            int totalTiles = dimension.getTileCount(), tileCount = 0;
            for (Tile tile: dimension.getTiles()) {
                final int worldTileX = tile.getX() << TILE_SIZE_BITS;
                final int worldTileY = tile.getY() << TILE_SIZE_BITS;
                tile.setEventsInhibited(true);
                try {
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
                } finally {
                    tile.setEventsInhibited(false);
                }
                tileCount++;
                progressReceiver.setProgress((float) tileCount / totalTiles);
            }
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
                int totalTiles = dimension.getTileCount(), tileCount = 0;
                for (Tile tile: dimension.getTiles()) {
                    final int worldTileX = tile.getX() << TILE_SIZE_BITS;
                    final int worldTileY = tile.getY() << TILE_SIZE_BITS;
                    tile.setEventsInhibited(true);
                    try {
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
                    } finally {
                        tile.setEventsInhibited(false);
                    }
                    tileCount++;
                    progressReceiver.setProgress((float) tileCount / totalTiles);
                }
            } else if (layer.getDataSize() == Layer.DataSize.BIT) {
                int totalTiles = dimension.getTileCount(), tileCount = 0;
                for (Tile tile: dimension.getTiles()) {
                    final int worldTileX = tile.getX() << TILE_SIZE_BITS;
                    final int worldTileY = tile.getY() << TILE_SIZE_BITS;
                    tile.setEventsInhibited(true);
                    try {
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
                    } finally {
                        tile.setEventsInhibited(false);
                    }
                    tileCount++;
                    progressReceiver.setProgress((float) tileCount / totalTiles);
                }
            } else if (layer.getDataSize() == Layer.DataSize.BIT_PER_CHUNK) {
                int totalTiles = dimension.getTileCount(), tileCount = 0;
                for (Tile tile: dimension.getTiles()) {
                    final int worldTileX = tile.getX() << TILE_SIZE_BITS;
                    final int worldTileY = tile.getY() << TILE_SIZE_BITS;
                    tile.setEventsInhibited(true);
                    try {
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
                    } finally {
                        tile.setEventsInhibited(false);
                    }
                    tileCount++;
                    progressReceiver.setProgress((float) tileCount / totalTiles);
                }
            } else {
                throw new UnsupportedOperationException();
            }
        }
    }

    private void invertLayer(ProgressReceiver progressReceiver) throws UnsupportedOperationException, OperationCancelled {
        Layer layer = (Layer) comboBoxInvertLayer.getSelectedItem();
        if (layer.getDataSize() == Layer.DataSize.NIBBLE) {
            int totalTiles = dimension.getTileCount(), tileCount = 0;
            for (Tile tile: dimension.getTiles()) {
                final int worldTileX = tile.getX() << TILE_SIZE_BITS;
                final int worldTileY = tile.getY() << TILE_SIZE_BITS;
                tile.setEventsInhibited(true);
                try {
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
                } finally {
                    tile.setEventsInhibited(false);
                }
                tileCount++;
                progressReceiver.setProgress((float) tileCount / totalTiles);
            }
        } else if (layer.getDataSize() == Layer.DataSize.BIT) {
            int totalTiles = dimension.getTileCount(), tileCount = 0;
            for (Tile tile: dimension.getTiles()) {
                final int worldTileX = tile.getX() << TILE_SIZE_BITS;
                final int worldTileY = tile.getY() << TILE_SIZE_BITS;
                tile.setEventsInhibited(true);
                try {
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
                } finally {
                    tile.setEventsInhibited(false);
                }
                tileCount++;
                progressReceiver.setProgress((float) tileCount / totalTiles);
            }
        } else if (layer.getDataSize() == Layer.DataSize.BIT_PER_CHUNK) {
            int totalTiles = dimension.getTileCount(), tileCount = 0;
            for (Tile tile: dimension.getTiles()) {
                final int worldTileX = tile.getX() << TILE_SIZE_BITS;
                final int worldTileY = tile.getY() << TILE_SIZE_BITS;
                tile.setEventsInhibited(true);
                try {
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
                } finally {
                    tile.setEventsInhibited(false);
                }
                tileCount++;
                progressReceiver.setProgress((float) tileCount / totalTiles);
            }
        } else {
            throw new UnsupportedOperationException();
        }
    }

    private void fillWithBiome(ProgressReceiver progressReceiver) throws OperationCancelled {
        int biome = (Integer) comboBoxBiome.getSelectedItem();
        int totalTiles = dimension.getTileCount(), tileCount = 0;
        for (Tile tile: dimension.getTiles()) {
            final int worldTileX = tile.getX() << TILE_SIZE_BITS;
            final int worldTileY = tile.getY() << TILE_SIZE_BITS;
            tile.setEventsInhibited(true);
            try {
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
            } finally {
                tile.setEventsInhibited(false);
            }
            tileCount++;
            progressReceiver.setProgress((float) tileCount / totalTiles);
        }
    }
    
    private void resetBiomes(ProgressReceiver progressReceiver) throws OperationCancelled {
        if (filter == null) {
            dimension.clearLayerData(Biome.INSTANCE);
        } else {
            int totalTiles = dimension.getTileCount(), tileCount = 0;
            for (Tile tile: dimension.getTiles()) {
                final int worldTileX = tile.getX() << TILE_SIZE_BITS;
                final int worldTileY = tile.getY() << TILE_SIZE_BITS;
                tile.setEventsInhibited(true);
                try {
                    for (int x = 0; x < TILE_SIZE; x++) {
                        for (int y = 0; y < TILE_SIZE; y++) {
                            final float strength = filter.modifyStrength(worldTileX | x, worldTileY | y, 1.0f);
                            if ((strength > 0.95f) || (Math.random() < strength)) {
                                tile.setLayerValue(Biome.INSTANCE, x, y, 255);
                            }
                        }
                    }
                } finally {
                    tile.setEventsInhibited(false);
                }
                tileCount++;
                progressReceiver.setProgress((float) tileCount / totalTiles);
            }
        }
    }

    private void makeAutoBiomesPermanent(ProgressReceiver progressReceiver) throws OperationCancelled {
        int totalTiles = dimension.getTileCount(), tileCount = 0;
        for (Tile tile: dimension.getTiles()) {
            final int worldTileX = tile.getX() << TILE_SIZE_BITS;
            final int worldTileY = tile.getY() << TILE_SIZE_BITS;
            tile.setEventsInhibited(true);
            try {
                if (filter == null) {
                    for (int x = 0; x < TILE_SIZE; x++) {
                        for (int y = 0; y < TILE_SIZE; y++) {
                            if (tile.getLayerValue(Biome.INSTANCE, x, y) == 255) {
                                tile.setLayerValue(Biome.INSTANCE, x, y, dimension.getAutoBiome(tile, x, y));
                            }
                        }
                    }
                } else {
                    for (int x = 0; x < TILE_SIZE; x++) {
                        for (int y = 0; y < TILE_SIZE; y++) {
                            final float strength = filter.modifyStrength(worldTileX | x, worldTileY | y, 1.0f);
                            if (((strength > 0.95f) || (Math.random() < strength)) && (tile.getLayerValue(Biome.INSTANCE, x, y) == 255)) {
                                tile.setLayerValue(Biome.INSTANCE, x, y, dimension.getAutoBiome(tile, x, y));
                            }
                        }
                    }
                }
            } finally {
                tile.setEventsInhibited(false);
            }
            tileCount++;
            progressReceiver.setProgress((float) tileCount / totalTiles);
        }
    }
    
    private void resetWater(ProgressReceiver progressReceiver) throws OperationCancelled, UnsupportedOperationException {
        TileFactory tileFactory = dimension.getTileFactory();
        if (tileFactory instanceof HeightMapTileFactory) {
            int waterLevel = ((HeightMapTileFactory) tileFactory).getWaterHeight();
            boolean floodWithLava = ((HeightMapTileFactory) tileFactory).isFloodWithLava();
            int totalTiles = dimension.getTileCount(), tileCount = 0;
            for (Tile tile: dimension.getTiles()) {
                final int worldTileX = tile.getX() << TILE_SIZE_BITS;
                final int worldTileY = tile.getY() << TILE_SIZE_BITS;
                tile.setEventsInhibited(true);
                try {
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
                } finally {
                    tile.setEventsInhibited(false);
                }
                tileCount++;
                progressReceiver.setProgress((float) tileCount / totalTiles);
            }
        } else {
            throw new UnsupportedOperationException("Tile factory type " + tileFactory.getClass() + " not supported");
        }
    }

    private void resetTerrain(ProgressReceiver progressReceiver) throws OperationCancelled {
        int totalTiles = dimension.getTileCount(), tileCount = 0;
        dimension.setEventsInhibited(true);
        try {
            for (Tile tile: dimension.getTiles()) {
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
                tileCount++;
                progressReceiver.setProgress((float) tileCount / totalTiles);
            }
        } finally {
            dimension.setEventsInhibited(false);
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
        radioButtonTerrain = new javax.swing.JRadioButton();
        comboBoxTerrain = new javax.swing.JComboBox();
        radioButtonBiome = new javax.swing.JRadioButton();
        comboBoxBiome = new javax.swing.JComboBox();
        buttonCancel = new javax.swing.JButton();
        buttonFill = new javax.swing.JButton();
        radioButtonClearLayer = new javax.swing.JRadioButton();
        comboBoxClearLayer = new javax.swing.JComboBox();
        radioButtonSetLayer = new javax.swing.JRadioButton();
        comboBoxSetLayer = new javax.swing.JComboBox();
        sliderLayerValue = new javax.swing.JSlider();
        radioButtonInvertLayer = new javax.swing.JRadioButton();
        comboBoxInvertLayer = new javax.swing.JComboBox();
        radioButtonResetWater = new javax.swing.JRadioButton();
        radioButtonResetTerrain = new javax.swing.JRadioButton();
        jSeparator1 = new javax.swing.JSeparator();
        brushOptions1 = new org.pepsoft.worldpainter.panels.BrushOptions();
        radioButtonResetBiomes = new javax.swing.JRadioButton();
        radioButtonMakeBiomesPermanent = new javax.swing.JRadioButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Global Operations");

        jLabel1.setText("Perform a global operation:");

        buttonGroup1.add(radioButtonTerrain);
        radioButtonTerrain.setText("fill with terrain type:");
        radioButtonTerrain.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                radioButtonTerrainActionPerformed(evt);
            }
        });

        comboBoxTerrain.setModel(new DefaultComboBoxModel(Terrain.getConfiguredValues()));
        comboBoxTerrain.setEnabled(false);
        comboBoxTerrain.setRenderer(new TerrainListCellRenderer(colourScheme));

        buttonGroup1.add(radioButtonBiome);
        radioButtonBiome.setText("fill with biome:");
        radioButtonBiome.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                radioButtonBiomeActionPerformed(evt);
            }
        });

        comboBoxBiome.setEnabled(false);

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

        buttonGroup1.add(radioButtonClearLayer);
        radioButtonClearLayer.setText("remove a layer:");
        radioButtonClearLayer.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                radioButtonClearLayerActionPerformed(evt);
            }
        });

        comboBoxClearLayer.setEnabled(false);

        buttonGroup1.add(radioButtonSetLayer);
        radioButtonSetLayer.setText("fill with layer:");
        radioButtonSetLayer.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                radioButtonSetLayerActionPerformed(evt);
            }
        });

        comboBoxSetLayer.setEnabled(false);
        comboBoxSetLayer.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                comboBoxSetLayerActionPerformed(evt);
            }
        });

        sliderLayerValue.setMajorTickSpacing(7);
        sliderLayerValue.setMinimum(2);
        sliderLayerValue.setPaintTicks(true);
        sliderLayerValue.setSnapToTicks(true);
        sliderLayerValue.setEnabled(false);

        buttonGroup1.add(radioButtonInvertLayer);
        radioButtonInvertLayer.setText("invert a layer:");
        radioButtonInvertLayer.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                radioButtonInvertLayerActionPerformed(evt);
            }
        });

        comboBoxInvertLayer.setEnabled(false);

        buttonGroup1.add(radioButtonResetWater);
        radioButtonResetWater.setText("reset all water or lava");
        radioButtonResetWater.setToolTipText("This resets the fluid level and type (water or lava) to the default everywhere");
        radioButtonResetWater.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                radioButtonResetWaterActionPerformed(evt);
            }
        });

        buttonGroup1.add(radioButtonResetTerrain);
        radioButtonResetTerrain.setText("reset terrain type to default");
        radioButtonResetTerrain.setToolTipText("Reset the terrain type of the entire map to the altitude-dependent default");
        radioButtonResetTerrain.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                radioButtonResetTerrainActionPerformed(evt);
            }
        });

        jSeparator1.setOrientation(javax.swing.SwingConstants.VERTICAL);

        buttonGroup1.add(radioButtonResetBiomes);
        radioButtonResetBiomes.setText("reset biomes to automatic");
        radioButtonResetBiomes.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                radioButtonResetBiomesActionPerformed(evt);
            }
        });

        buttonGroup1.add(radioButtonMakeBiomesPermanent);
        radioButtonMakeBiomesPermanent.setText("make automatic biomes permanent");
        radioButtonMakeBiomesPermanent.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                radioButtonMakeBiomesPermanentActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel1)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(12, 12, 12)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(radioButtonSetLayer)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(comboBoxSetLayer, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(layout.createSequentialGroup()
                                .addGap(12, 12, 12)
                                .addComponent(sliderLayerValue, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(radioButtonClearLayer)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(comboBoxClearLayer, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(radioButtonBiome)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(comboBoxBiome, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(radioButtonInvertLayer)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(comboBoxInvertLayer, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addComponent(radioButtonResetWater)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(radioButtonTerrain)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(comboBoxTerrain, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addComponent(radioButtonResetTerrain)
                            .addComponent(radioButtonResetBiomes)
                            .addComponent(radioButtonMakeBiomesPermanent))
                        .addGap(18, 18, 18)
                        .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
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
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                        .addGroup(layout.createSequentialGroup()
                            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(radioButtonTerrain)
                                .addComponent(comboBoxTerrain, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(radioButtonResetTerrain)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(radioButtonSetLayer)
                                .addComponent(comboBoxSetLayer, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(sliderLayerValue, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(radioButtonClearLayer)
                                .addComponent(comboBoxClearLayer, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(radioButtonInvertLayer)
                                .addComponent(comboBoxInvertLayer, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(radioButtonBiome)
                                .addComponent(comboBoxBiome, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(radioButtonMakeBiomesPermanent)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(radioButtonResetBiomes)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(radioButtonResetWater))
                        .addComponent(jSeparator1))
                    .addComponent(brushOptions1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
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

    private void radioButtonTerrainActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_radioButtonTerrainActionPerformed
        setControlStates();
    }//GEN-LAST:event_radioButtonTerrainActionPerformed

    private void radioButtonBiomeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_radioButtonBiomeActionPerformed
        setControlStates();
    }//GEN-LAST:event_radioButtonBiomeActionPerformed

    private void buttonCancelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonCancelActionPerformed
        dispose();
    }//GEN-LAST:event_buttonCancelActionPerformed

    private void radioButtonSetLayerActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_radioButtonSetLayerActionPerformed
        setControlStates();
    }//GEN-LAST:event_radioButtonSetLayerActionPerformed

    private void radioButtonClearLayerActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_radioButtonClearLayerActionPerformed
        setControlStates();
    }//GEN-LAST:event_radioButtonClearLayerActionPerformed

    private void comboBoxSetLayerActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_comboBoxSetLayerActionPerformed
        setControlStates();
    }//GEN-LAST:event_comboBoxSetLayerActionPerformed

    private void radioButtonInvertLayerActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_radioButtonInvertLayerActionPerformed
        setControlStates();
    }//GEN-LAST:event_radioButtonInvertLayerActionPerformed

    private void radioButtonResetWaterActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_radioButtonResetWaterActionPerformed
        setControlStates();
    }//GEN-LAST:event_radioButtonResetWaterActionPerformed

    private void radioButtonResetTerrainActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_radioButtonResetTerrainActionPerformed
        setControlStates();
    }//GEN-LAST:event_radioButtonResetTerrainActionPerformed

    private void radioButtonResetBiomesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_radioButtonResetBiomesActionPerformed
        setControlStates();
    }//GEN-LAST:event_radioButtonResetBiomesActionPerformed

    private void radioButtonMakeBiomesPermanentActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_radioButtonMakeBiomesPermanentActionPerformed
        setControlStates();
    }//GEN-LAST:event_radioButtonMakeBiomesPermanentActionPerformed

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
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JRadioButton radioButtonBiome;
    private javax.swing.JRadioButton radioButtonClearLayer;
    private javax.swing.JRadioButton radioButtonInvertLayer;
    private javax.swing.JRadioButton radioButtonMakeBiomesPermanent;
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