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
import org.pepsoft.worldpainter.biomeschemes.BiomeHelper;
import org.pepsoft.worldpainter.biomeschemes.CustomBiomeManager;
import org.pepsoft.worldpainter.layers.*;
import org.pepsoft.worldpainter.operations.Filter;
import org.pepsoft.worldpainter.panels.BrushOptions.Listener;
import org.pepsoft.worldpainter.panels.BrushOptions.MapSelectionListener;
import org.pepsoft.worldpainter.selection.SelectionBlock;
import org.pepsoft.worldpainter.selection.SelectionChunk;
import org.pepsoft.worldpainter.themes.TerrainListCellRenderer;
import org.pepsoft.worldpainter.tools.Eyedropper.PaintType;
import org.pepsoft.worldpainter.tools.Eyedropper.SelectionListener;

import javax.swing.*;
import java.util.EnumSet;
import java.util.Set;

import static java.util.Arrays.asList;
import static org.pepsoft.worldpainter.Constants.TILE_SIZE;
import static org.pepsoft.worldpainter.Constants.TILE_SIZE_BITS;
import static org.pepsoft.worldpainter.biomeschemes.Minecraft1_7Biomes.BIOME_PLAINS;
import static org.pepsoft.worldpainter.panels.BrushOptions.MENU_EXCEPT_ON;
import static org.pepsoft.worldpainter.panels.BrushOptions.MENU_ONLY_ON;
import static org.pepsoft.worldpainter.tools.Eyedropper.PaintType.*;

/**
 *
 * @author pepijn
 */
@SuppressWarnings({"unused", "FieldCanBeLocal"}) // Managed by NetBeans
public class FillDialog extends WPDialogWithPaintSelection implements Listener, MapSelectionListener {
    /** Creates new form FillDialog */
    public FillDialog(App app, Dimension dimension, Layer[] layers, ColourScheme colourScheme, Integer[] biomes, CustomBiomeManager customBiomeManager, WorldPainterView view, ObservableBoolean selectionState) {
        super(app);
        this.dimension = dimension;
        this.colourScheme = colourScheme;
        this.view = view;
        final Platform platform = dimension.getWorld().getPlatform();
        biomeHelper = new BiomeHelper(colourScheme, customBiomeManager, platform);
        
        initComponents();
        brushOptions1.setColourScheme(colourScheme);
        brushOptions1.setCustomBiomeManager(customBiomeManager);
        brushOptions1.setPlatform(platform);
        brushOptions1.setMinHeight(dimension.getMinHeight());
        brushOptions1.setMaxHeight(dimension.getMaxHeight());
        brushOptions1.setSelectionState(selectionState);

        comboBoxBiome.setModel(new DefaultComboBoxModel<>(biomes));
        comboBoxBiome.setRenderer(new BiomeListCellRenderer(colourScheme, customBiomeManager, platform));
        
        comboBoxSetLayer.setModel(new DefaultComboBoxModel<>(layers));
        comboBoxSetLayer.setRenderer(new LayerListCellRenderer());
        
        Set<Layer> layersInUse = dimension.getAllLayers(false);
        layersInUse.removeAll(asList(Biome.INSTANCE, FloodWithLava.INSTANCE, SelectionBlock.INSTANCE, SelectionChunk.INSTANCE, NotPresent.INSTANCE, NotPresentBlock.INSTANCE));
        if (! layersInUse.isEmpty()) {
            comboBoxClearLayer.setModel(new DefaultComboBoxModel<>(layersInUse.toArray(new Layer[layersInUse.size()])));
            comboBoxClearLayer.setRenderer(new LayerListCellRenderer());
        } else {
            comboBoxClearLayer.setEnabled(false);
            radioButtonClearLayer.setEnabled(false);
        }

        comboBoxInvertLayer.setModel(new DefaultComboBoxModel<>(layers));
        comboBoxInvertLayer.setRenderer(new LayerListCellRenderer());

        brushOptions1.setListener(this);
        brushOptions1.setMapSelectionListener(this);

        ((SpinnerNumberModel) spinnerTerrainHeight.getModel()).setMinimum(dimension.getMinHeight());
        ((SpinnerNumberModel) spinnerTerrainHeight.getModel()).setMaximum(dimension.getMaxHeight() - 1);
        final TileFactory tileFactory = dimension.getTileFactory();
        if (tileFactory instanceof HeightMapTileFactory) {
            spinnerTerrainHeight.setValue(((HeightMapTileFactory) tileFactory).getWaterHeight());
        } else {
            spinnerTerrainHeight.setValue(62);
        }
        
        getRootPane().setDefaultButton(buttonFill);

        scaleToUI();
        pack();
        setLocationRelativeTo(app);
        
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

    // BrushOptions.MapSelectionListener

    @Override
    public void mapSelectionRequested(String descriptor, boolean addAnother) {
        selectFromMap(null, new SelectionListener() {
            @Override
            public void terrainSelected(Terrain terrain) {
                switch (descriptor) {
                    case MENU_ONLY_ON:
                        if (addAnother) {
                            brushOptions1.addOnlyOn(terrain);
                        } else {
                            brushOptions1.setOnlyOn(terrain);
                        }
                        break;
                    case MENU_EXCEPT_ON:
                        if (addAnother) {
                            brushOptions1.addExceptOn(terrain);
                        } else {
                            brushOptions1.setExceptOn(terrain);
                        }
                        break;
                }
            }

            @Override
            public void layerSelected(Layer layer, int value) {
                switch (descriptor) {
                    case MENU_ONLY_ON:
                        if (addAnother) {
                            brushOptions1.addOnlyOn(layer, value);
                        } else {
                            brushOptions1.setOnlyOn(layer, value);
                        }
                        break;
                    case MENU_EXCEPT_ON:
                        if (addAnother) {
                            brushOptions1.addExceptOn(layer, value);
                        } else {
                            brushOptions1.setExceptOn(layer, value);
                        }
                        break;
                }
            }

            @Override public void selectionCancelled(boolean byUser) {}
        });
    }

    private void setControlStates() {
        comboBoxTerrain.setEnabled(radioButtonTerrain.isSelected());
        buttonFillTerrainSelectOnMap.setEnabled(radioButtonTerrain.isSelected());
        comboBoxSetLayer.setEnabled(radioButtonSetLayer.isSelected());
        buttonFillLayerSelectOnMap.setEnabled(radioButtonSetLayer.isSelected());
        sliderLayerValue.setEnabled(radioButtonSetLayer.isSelected() && ((((Layer) comboBoxSetLayer.getSelectedItem()).getDataSize() == Layer.DataSize.BYTE) || (((Layer) comboBoxSetLayer.getSelectedItem()).getDataSize() == Layer.DataSize.NIBBLE)));
        comboBoxClearLayer.setEnabled(radioButtonClearLayer.isSelected());
        buttonRemoveLayerSelectOnMap.setEnabled(radioButtonClearLayer.isSelected());
        comboBoxInvertLayer.setEnabled(radioButtonInvertLayer.isSelected());
        buttonInvertLayerSelectOnMap.setEnabled(radioButtonInvertLayer.isSelected());
        comboBoxBiome.setEnabled(radioButtonBiome.isSelected());
        buttonFillBiomeSelectOnMap.setEnabled(radioButtonBiome.isSelected());
        comboBoxTerrainOperation.setEnabled(radioButtonTerrainHeight.isSelected());
        spinnerTerrainHeight.setEnabled(radioButtonTerrainHeight.isSelected());
        buttonFill.setEnabled(radioButtonTerrain.isSelected() || radioButtonSetLayer.isSelected() || radioButtonClearLayer.isSelected() || radioButtonInvertLayer.isSelected() || radioButtonBiome.isSelected() || radioButtonResetBiomes.isSelected() || radioButtonResetWater.isSelected() || radioButtonResetTerrain.isSelected() || radioButtonMakeBiomesPermanent.isSelected() || radioButtonAddToSelection.isSelected() || radioButtonRemoveFromSelection.isSelected() || radioButtonTerrainHeight.isSelected());
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
                        return "Clearing " + ((Layer) comboBoxClearLayer.getSelectedItem()).getName();
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
                    } else if (radioButtonTerrainHeight.isSelected()) {
                        switch ((String) comboBoxTerrainOperation.getSelectedItem()) {
                            case "raise":
                                return "Raising terrain level to " + spinnerTerrainHeight.getValue();
                            case "lower":
                                return "Lowering terrain level to " + spinnerTerrainHeight.getValue();
                            default:
                                return "Setting terrain level to " + spinnerTerrainHeight.getValue();
                        }
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
                    } else if (radioButtonTerrainHeight.isSelected()) {
                        changeTerrainHeight(progressReceiver);
                    }
                    return dimension;
                }
            });
            if (result == null) {
                // Cancelled by user
                if (dimension.undoChanges()) {
                    dimension.clearRedo();
                }
                if (! checkBoxKeepOpen.isSelected()) {
                    cancel();
                }
            } else {
                dimension.armSavePoint();
                if (! checkBoxKeepOpen.isSelected()) {
                    ok();
                } else {
                    buttonCancel.setText("Close");
                }
            }
        } finally {
            view.setInhibitUpdates(false);
            dimension.setEventsInhibited(false);
        }
    }

    private void fillWithTerrain(ProgressReceiver progressReceiver) throws OperationCancelled {
        final Terrain terrain = (Terrain) comboBoxTerrain.getSelectedItem();
        dimension.visitTilesForEditing().forFilter(filter).andDo(tile -> {
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
            dimension.visitTilesForEditing().forFilter(filter).andDo(tile -> {
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
            dimension.visitTilesForEditing().forFilter(filter).andDo(tile -> {
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
            dimension.visitTilesForEditing().forFilter(filter).andDo(tile -> {
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
                dimension.visitTilesForEditing().forFilter(filter).andDo(tile -> {
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
                dimension.visitTilesForEditing().forFilter(filter).andDo(tile -> {
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
                dimension.visitTilesForEditing().forFilter(filter).andDo(tile -> {
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
            dimension.visitTilesForEditing().forFilter(filter).andDo(tile -> {
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
            dimension.visitTilesForEditing().forFilter(filter).andDo(tile -> {
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
            dimension.visitTilesForEditing().forFilter(filter).andDo(tile -> {
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
        dimension.visitTilesForEditing().forFilter(filter).andDo(tile -> {
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
            dimension.visitTilesForEditing().forFilter(filter).andDo(tile -> {
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
        dimension.visitTilesForEditing().forFilter(filter).andDo(tile -> {
            if (filter == null) {
                for (int x = 0; x < TILE_SIZE; x++) {
                    for (int y = 0; y < TILE_SIZE; y++) {
                        if (tile.getLayerValue(Biome.INSTANCE, x, y) == 255) {
                            tile.setLayerValue(Biome.INSTANCE, x, y, dimension.getAutoBiome(tile, x, y, BIOME_PLAINS));
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
                            tile.setLayerValue(Biome.INSTANCE, x, y, dimension.getAutoBiome(tile, x, y, BIOME_PLAINS));
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
            dimension.visitTilesForEditing().forFilter(filter).andDo(tile -> {
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
        dimension.visitTilesForEditing().forFilter(filter).andDo(tile -> {
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
        dimension.visitTilesForEditing().forFilter(filter).andDo(tile -> {
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
            dimension.visitTilesForEditing().forFilter(filter).andDo(tile -> {
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

    private void changeTerrainHeight(ProgressReceiver progressReceiver) throws OperationCancelled {
        dimension.visitTilesForEditing().forFilter(filter).andDo(tile -> {
            final int worldTileX = tile.getX() << TILE_SIZE_BITS;
            final int worldTileY = tile.getY() << TILE_SIZE_BITS;
            final float targetHeight = (Integer) spinnerTerrainHeight.getValue();
            final boolean filterSet = filter != null;
            switch ((String) comboBoxTerrainOperation.getSelectedItem()) {
                case "raise":
                    for (int x = 0; x < TILE_SIZE; x++) {
                        for (int y = 0; y < TILE_SIZE; y++) {
                            final float strength = filterSet
                                    ? filter.modifyStrength(worldTileX | x, worldTileY | y, 1.0f)
                                    : 1.0f;
                            if (strength > 0.0f) {
                                final float currentHeight = tile.getHeight(x, y);
                                if (currentHeight < targetHeight) {
                                    tile.setHeight(x, y, (strength < 1.0f)
                                            ? (strength * targetHeight) + ((1.0f - strength) * currentHeight)
                                            : targetHeight);
                                }
                            }
                        }
                    }
                    break;
                case "lower":
                    for (int x = 0; x < TILE_SIZE; x++) {
                        for (int y = 0; y < TILE_SIZE; y++) {
                            final float strength = filterSet
                                    ? filter.modifyStrength(worldTileX | x, worldTileY | y, 1.0f)
                                    : 1.0f;
                            if (strength > 0.0f) {
                                final float currentHeight = tile.getHeight(x, y);
                                if (currentHeight > targetHeight) {
                                    tile.setHeight(x, y, (strength < 1.0f)
                                            ? (strength * targetHeight) + ((1.0f - strength) * currentHeight)
                                            : targetHeight);
                                }
                            }
                        }
                    }
                    break;
                default:
                    for (int x = 0; x < TILE_SIZE; x++) {
                        for (int y = 0; y < TILE_SIZE; y++) {
                            final float strength = filterSet
                                    ? filter.modifyStrength(worldTileX | x, worldTileY | y, 1.0f)
                                    : 1.0f;
                            if (strength > 0.0f) {
                                tile.setHeight(x, y, (strength < 1.0f)
                                        ? (strength * targetHeight) + ((1.0f - strength) * tile.getHeight(x, y))
                                        : targetHeight);
                            }
                        }
                    }
                    break;
            }
        }, progressReceiver);
    }
    
    private void selectOnMap(JComboBox<?> comboBox, PaintType... paintTypes) {
        selectFromMap(EnumSet.copyOf(asList(paintTypes)), new SelectionListener() {
            @Override
            public void terrainSelected(Terrain terrain) {
                comboBox.setSelectedItem(terrain);
            }

            @Override
            public void layerSelected(Layer layer, int value) {
                if (comboBox == comboBoxBiome) {
                    comboBox.setSelectedItem(value);
                } else {
                    comboBox.setSelectedItem(layer);
                }
            }

            @Override public void selectionCancelled(boolean byUser) {}
        });
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings({"unchecked", "DataFlowIssue", "Convert2Lambda", "Anonymous2MethodRef"}) // Managed by NetBeans
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
        comboBoxBiome = new javax.swing.JComboBox<>();
        radioButtonSetLayer = new javax.swing.JRadioButton();
        radioButtonResetBiomes = new javax.swing.JRadioButton();
        radioButtonClearLayer = new javax.swing.JRadioButton();
        radioButtonResetTerrain = new javax.swing.JRadioButton();
        radioButtonTerrain = new javax.swing.JRadioButton();
        comboBoxClearLayer = new javax.swing.JComboBox<>();
        radioButtonResetWater = new javax.swing.JRadioButton();
        comboBoxSetLayer = new javax.swing.JComboBox<>();
        comboBoxInvertLayer = new javax.swing.JComboBox<>();
        comboBoxTerrain = new javax.swing.JComboBox<>();
        radioButtonInvertLayer = new javax.swing.JRadioButton();
        radioButtonBiome = new javax.swing.JRadioButton();
        radioButtonMakeBiomesPermanent = new javax.swing.JRadioButton();
        radioButtonAddToSelection = new javax.swing.JRadioButton();
        radioButtonRemoveFromSelection = new javax.swing.JRadioButton();
        buttonFillTerrainSelectOnMap = new javax.swing.JButton();
        buttonFillLayerSelectOnMap = new javax.swing.JButton();
        buttonRemoveLayerSelectOnMap = new javax.swing.JButton();
        buttonInvertLayerSelectOnMap = new javax.swing.JButton();
        buttonFillBiomeSelectOnMap = new javax.swing.JButton();
        radioButtonTerrainHeight = new javax.swing.JRadioButton();
        comboBoxTerrainOperation = new javax.swing.JComboBox<>();
        jLabel10 = new javax.swing.JLabel();
        spinnerTerrainHeight = new javax.swing.JSpinner();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        checkBoxKeepOpen = new javax.swing.JCheckBox();
        jLabel8 = new javax.swing.JLabel();
        jLabel9 = new javax.swing.JLabel();

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

        buttonFillTerrainSelectOnMap.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/pepsoft/worldpainter/icons/eyedropper.png"))); // NOI18N
        buttonFillTerrainSelectOnMap.setToolTipText("Select a terrain type from the map.");
        buttonFillTerrainSelectOnMap.setEnabled(false);
        buttonFillTerrainSelectOnMap.setMargin(new java.awt.Insets(2, 2, 2, 2));
        buttonFillTerrainSelectOnMap.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonFillTerrainSelectOnMapActionPerformed(evt);
            }
        });

        buttonFillLayerSelectOnMap.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/pepsoft/worldpainter/icons/eyedropper.png"))); // NOI18N
        buttonFillLayerSelectOnMap.setToolTipText("Select a layer from the map.");
        buttonFillLayerSelectOnMap.setEnabled(false);
        buttonFillLayerSelectOnMap.setMargin(new java.awt.Insets(2, 2, 2, 2));
        buttonFillLayerSelectOnMap.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonFillLayerSelectOnMapActionPerformed(evt);
            }
        });

        buttonRemoveLayerSelectOnMap.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/pepsoft/worldpainter/icons/eyedropper.png"))); // NOI18N
        buttonRemoveLayerSelectOnMap.setToolTipText("Select a layer from the map.");
        buttonRemoveLayerSelectOnMap.setEnabled(false);
        buttonRemoveLayerSelectOnMap.setMargin(new java.awt.Insets(2, 2, 2, 2));
        buttonRemoveLayerSelectOnMap.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonRemoveLayerSelectOnMapActionPerformed(evt);
            }
        });

        buttonInvertLayerSelectOnMap.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/pepsoft/worldpainter/icons/eyedropper.png"))); // NOI18N
        buttonInvertLayerSelectOnMap.setToolTipText("Select a layer from the map.");
        buttonInvertLayerSelectOnMap.setEnabled(false);
        buttonInvertLayerSelectOnMap.setMargin(new java.awt.Insets(2, 2, 2, 2));
        buttonInvertLayerSelectOnMap.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonInvertLayerSelectOnMapActionPerformed(evt);
            }
        });

        buttonFillBiomeSelectOnMap.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/pepsoft/worldpainter/icons/eyedropper.png"))); // NOI18N
        buttonFillBiomeSelectOnMap.setToolTipText("Select a biome from the map.");
        buttonFillBiomeSelectOnMap.setEnabled(false);
        buttonFillBiomeSelectOnMap.setMargin(new java.awt.Insets(2, 2, 2, 2));
        buttonFillBiomeSelectOnMap.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonFillBiomeSelectOnMapActionPerformed(evt);
            }
        });

        buttonGroup1.add(radioButtonTerrainHeight);
        radioButtonTerrainHeight.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                radioButtonTerrainHeightActionPerformed(evt);
            }
        });

        comboBoxTerrainOperation.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "raise", "set", "lower" }));
        comboBoxTerrainOperation.setEnabled(false);

        jLabel10.setText("terrain height to");

        spinnerTerrainHeight.setModel(new javax.swing.SpinnerNumberModel(62, -64, 384, 1));
        spinnerTerrainHeight.setEnabled(false);

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(radioButtonSetLayer)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(comboBoxSetLayer, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(buttonFillLayerSelectOnMap))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(12, 12, 12)
                        .addComponent(sliderLayerValue, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(radioButtonClearLayer)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(comboBoxClearLayer, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(buttonRemoveLayerSelectOnMap))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(radioButtonBiome)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(comboBoxBiome, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(buttonFillBiomeSelectOnMap))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(radioButtonInvertLayer)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(comboBoxInvertLayer, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(buttonInvertLayerSelectOnMap))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(radioButtonTerrain)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(comboBoxTerrain, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(buttonFillTerrainSelectOnMap))
                    .addComponent(radioButtonResetTerrain)
                    .addComponent(radioButtonResetBiomes)
                    .addComponent(radioButtonMakeBiomesPermanent)
                    .addComponent(radioButtonAddToSelection)
                    .addComponent(radioButtonRemoveFromSelection)
                    .addComponent(radioButtonResetWater)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(radioButtonTerrainHeight)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(comboBoxTerrainOperation, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel10)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(spinnerTerrainHeight, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(radioButtonTerrain)
                    .addComponent(comboBoxTerrain, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(buttonFillTerrainSelectOnMap))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(radioButtonResetTerrain)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(radioButtonResetWater)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(radioButtonTerrainHeight)
                    .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(comboBoxTerrainOperation, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jLabel10)
                        .addComponent(spinnerTerrainHeight, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                    .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(radioButtonSetLayer)
                                        .addComponent(comboBoxSetLayer, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                    .addComponent(buttonFillLayerSelectOnMap))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(sliderLayerValue, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(radioButtonClearLayer)
                                    .addComponent(comboBoxClearLayer, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                            .addComponent(buttonRemoveLayerSelectOnMap))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(radioButtonInvertLayer)
                            .addComponent(comboBoxInvertLayer, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addComponent(buttonInvertLayerSelectOnMap))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
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
                    .addComponent(buttonFillBiomeSelectOnMap))
                .addContainerGap())
        );

        jLabel3.setForeground(new java.awt.Color(0, 0, 255));
        jLabel3.setText("<html><u>Change height...</u></html>");
        jLabel3.setToolTipText("Raise or lower the entire dimension");
        jLabel3.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        jLabel3.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jLabel3MouseClicked(evt);
            }
        });

        jLabel4.setText("-");

        jLabel5.setForeground(new java.awt.Color(0, 0, 255));
        jLabel5.setText("<html><u>Rotate...</u></html>");
        jLabel5.setToolTipText("Rotate the entire dimension in 90 degree steps");
        jLabel5.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        jLabel5.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jLabel5MouseClicked(evt);
            }
        });

        jLabel6.setText("-");

        jLabel7.setForeground(new java.awt.Color(0, 0, 255));
        jLabel7.setText("<html><u>Shift...</u></html>");
        jLabel7.setToolTipText("Shift the entire dimension horizontally in 128 block steps");
        jLabel7.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        jLabel7.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jLabel7MouseClicked(evt);
            }
        });

        jLabel2.setText("Other global tools:");

        checkBoxKeepOpen.setText("keep this window open");
        checkBoxKeepOpen.setHorizontalTextPosition(javax.swing.SwingConstants.LEADING);

        jLabel8.setText("-");

        jLabel9.setForeground(new java.awt.Color(0, 0, 255));
        jLabel9.setText("<html><u>Scale...</u></html>");
        jLabel9.setToolTipText("Scale the entire dimension up or down by an arbitrary amount");
        jLabel9.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        jLabel9.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jLabel9MouseClicked(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel2)
                    .addComponent(jLabel1)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, 2, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(brushOptions1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(layout.createSequentialGroup()
                        .addGap(6, 6, 6)
                        .addComponent(jLabel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel4)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel6)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel7, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel8)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel9, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(checkBoxKeepOpen)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
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
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                        .addComponent(jSeparator1, javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(brushOptions1, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addGap(6, 6, 6)
                .addComponent(jLabel2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel4)
                    .addComponent(jLabel5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel6)
                    .addComponent(jLabel7, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel8)
                    .addComponent(jLabel9, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(buttonCancel)
                    .addComponent(buttonFill)
                    .addComponent(checkBoxKeepOpen))
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

    private void jLabel3MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jLabel3MouseClicked
        app.changeWorldHeight(this);
    }//GEN-LAST:event_jLabel3MouseClicked

    private void jLabel5MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jLabel5MouseClicked
        app.rotateWorld(this);
    }//GEN-LAST:event_jLabel5MouseClicked

    private void jLabel7MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jLabel7MouseClicked
        app.shiftWorld(this);
    }//GEN-LAST:event_jLabel7MouseClicked

    private void jLabel9MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jLabel9MouseClicked
        app.scaleWorld(this);
    }//GEN-LAST:event_jLabel9MouseClicked

    private void buttonFillTerrainSelectOnMapActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonFillTerrainSelectOnMapActionPerformed
        selectOnMap(comboBoxTerrain, TERRAIN);
    }//GEN-LAST:event_buttonFillTerrainSelectOnMapActionPerformed

    private void buttonFillLayerSelectOnMapActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonFillLayerSelectOnMapActionPerformed
        selectOnMap(comboBoxSetLayer, LAYER);
    }//GEN-LAST:event_buttonFillLayerSelectOnMapActionPerformed

    private void buttonRemoveLayerSelectOnMapActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonRemoveLayerSelectOnMapActionPerformed
        selectOnMap(comboBoxClearLayer, LAYER, ANNOTATION);
    }//GEN-LAST:event_buttonRemoveLayerSelectOnMapActionPerformed

    private void buttonInvertLayerSelectOnMapActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonInvertLayerSelectOnMapActionPerformed
        selectOnMap(comboBoxInvertLayer, LAYER);
    }//GEN-LAST:event_buttonInvertLayerSelectOnMapActionPerformed

    private void buttonFillBiomeSelectOnMapActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonFillBiomeSelectOnMapActionPerformed
        selectOnMap(comboBoxBiome, BIOME);
    }//GEN-LAST:event_buttonFillBiomeSelectOnMapActionPerformed

    private void radioButtonTerrainHeightActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_radioButtonTerrainHeightActionPerformed
        setControlStates();
    }//GEN-LAST:event_radioButtonTerrainHeightActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private org.pepsoft.worldpainter.panels.BrushOptions brushOptions1;
    private javax.swing.JButton buttonCancel;
    private javax.swing.JButton buttonFill;
    private javax.swing.JButton buttonFillBiomeSelectOnMap;
    private javax.swing.JButton buttonFillLayerSelectOnMap;
    private javax.swing.JButton buttonFillTerrainSelectOnMap;
    private javax.swing.ButtonGroup buttonGroup1;
    private javax.swing.JButton buttonInvertLayerSelectOnMap;
    private javax.swing.JButton buttonRemoveLayerSelectOnMap;
    private javax.swing.JCheckBox checkBoxKeepOpen;
    private javax.swing.JComboBox<Integer> comboBoxBiome;
    private javax.swing.JComboBox<Layer> comboBoxClearLayer;
    private javax.swing.JComboBox<Layer> comboBoxInvertLayer;
    private javax.swing.JComboBox<Layer> comboBoxSetLayer;
    private javax.swing.JComboBox<Terrain> comboBoxTerrain;
    private javax.swing.JComboBox<String> comboBoxTerrainOperation;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
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
    private javax.swing.JRadioButton radioButtonTerrainHeight;
    private javax.swing.JSlider sliderLayerValue;
    private javax.swing.JSpinner spinnerTerrainHeight;
    // End of variables declaration//GEN-END:variables

    private final ColourScheme colourScheme;
    private final Dimension dimension;
    private final BiomeHelper biomeHelper;
    private final WorldPainterView view;
    private Filter filter;

    private static final long serialVersionUID = 1L;
}