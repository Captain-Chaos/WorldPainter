/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.worldpainter.layers.combined;

import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.*;
import org.pepsoft.worldpainter.biomeschemes.CustomBiomeManager;
import org.pepsoft.worldpainter.layers.*;
import org.pepsoft.worldpainter.layers.exporters.ExporterSettings;
import org.pepsoft.worldpainter.layers.tunnel.TunnelLayer;
import org.pepsoft.worldpainter.themes.JSpinnerTableCellEditor;
import org.pepsoft.worldpainter.themes.TerrainListCellRenderer;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.pepsoft.util.CollectionUtils.listOf;
import static org.pepsoft.util.GUIUtils.scaleToUI;
import static org.pepsoft.worldpainter.Dimension.Role.CAVE_FLOOR;
import static org.pepsoft.worldpainter.layers.combined.CombinedLayerTableModel.COLUMN_FACTOR;
import static org.pepsoft.worldpainter.layers.combined.CombinedLayerTableModel.COLUMN_LAYER;
import static org.pepsoft.worldpainter.util.BiomeUtils.getAllBiomes;

/**
 *
 * @author Pepijn Schmitz
 */
@SuppressWarnings({"Convert2Lambda", "Anonymous2MethodRef", "unused", "FieldCanBeLocal"}) // Managed by NetBeans
public class CombinedLayerEditor extends AbstractLayerEditor<CombinedLayer> implements ListSelectionListener {
    /**
     * Creates new form CombinedLayerEditor
     */
    public CombinedLayerEditor() {
        initComponents();

        fieldName.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                settingsChanged();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                settingsChanged();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                settingsChanged();
            }
        });
        tableLayers.getSelectionModel().addListSelectionListener(this);
        scaleToUI(tableLayers);
    }

    // LayerEditor
    
    @Override
    public CombinedLayer createLayer() {
        return new CombinedLayer("My Combined Layer", "A combined layer", Color.ORANGE.getRGB());
    }

    @Override
    public void setLayer(CombinedLayer layer) {
        super.setLayer(layer);
        reset();
    }

    @Override
    public void commit() {
        if (! isCommitAvailable()) {
            throw new IllegalStateException("Settings invalid or incomplete");
        }
        saveSettings(layer);
    }

    @Override
    public void reset() {
        tableModel = new CombinedLayerTableModel(layer.getLayers(), layer.getFactors());
        tableModel.addTableModelListener(e -> settingsChanged());
        configureTable();

        comboBoxTerrain.setSelectedItem(layer.getTerrain());

        fieldName.setText(layer.getName());

        colourEditor1.setColour(layer.getColour());
        
        comboBoxBiome.setSelectedItem(layer.getBiome());

        checkBoxApplyTerrainAndBiomeOnExport.setSelected(layer.isApplyTerrainAndBiomeOnExport());
        
        settingsChanged();
    }

    @Override
    public ExporterSettings getSettings() {
        if (! isCommitAvailable()) {
            throw new IllegalStateException("Settings invalid or incomplete");
        }
        final CombinedLayer previewLayer = saveSettings(null);
        return new ExporterSettings() {
            @Override
            public boolean isApplyEverywhere() {
                return false;
            }

            @Override
            public CombinedLayer getLayer() {
                return previewLayer;
            }

            @Override
            public ExporterSettings clone() {
                throw new UnsupportedOperationException("Not supported");
            }
        };
    }

    @Override
    public void setContext(LayerEditorContext context) {
        super.setContext(context);
        
        final CustomBiomeManager customBiomeManager = context.getCustomBiomeManager();
        final ColourScheme colourScheme = context.getColourScheme();
        final Dimension dimension = context.getDimension();
        final Platform platform = dimension.getWorld().getPlatform();
        comboBoxTerrain.setRenderer(new TerrainListCellRenderer(colourScheme, "none"));
        comboBoxBiome.setRenderer(new BiomeListCellRenderer(colourScheme, customBiomeManager, "none", platform));

        List<Terrain> allTerrains = listOf(singletonList(null), asList(Terrain.getConfiguredValues()));
        comboBoxTerrain.setModel(new DefaultComboBoxModel<>(allTerrains.toArray(new Terrain[allTerrains.size()])));
        List<Integer> allBiomes = listOf(singletonList(-1), getAllBiomes(platform, customBiomeManager));
        comboBoxBiome.setModel(new DefaultComboBoxModel<>(allBiomes.toArray(new Integer[allBiomes.size()])));
        
        allLayers = context.getAllLayers();

        if (dimension.getAnchor().role == CAVE_FLOOR) {
            allLayers.removeIf(l -> ! TunnelLayer.isLayerSupportedForFloorDimension(l));
        }
    }

    @Override
    public boolean isCommitAvailable() {
        boolean terrainSelected = comboBoxTerrain.getSelectedItem() != null;
        boolean biomeSelected = (comboBoxBiome.getSelectedItem() != null) && ((Integer) comboBoxBiome.getSelectedItem() != -1);
        int layersSelected = tableModel.getRowCount();
        boolean nameEntered = ! fieldName.getText().trim().isEmpty();
        return nameEntered
            && ((terrainSelected && biomeSelected)
                || ((terrainSelected || biomeSelected) && (layersSelected > 0))
                || (layersSelected > 1));
    }

    // ListSelectionListener

    @Override
    public void valueChanged(ListSelectionEvent e) {
        setControlStates();
    }

    private void setControlStates() {
        checkBoxApplyTerrainAndBiomeOnExport.setEnabled((comboBoxTerrain.getSelectedItem() != null) || ((int) comboBoxBiome.getSelectedItem() != -1));
        buttonRemoveLayer.setEnabled(tableLayers.getSelectedRowCount() == 1);
        buttonEditLayerSettings.setEnabled((tableLayers.getSelectedRowCount() == 1) && (tableModel.getValueAt(tableLayers.getSelectedRow(), COLUMN_LAYER) instanceof CustomLayer));
    }

    private void addLayer() {
        List<Layer> availableLayers = new ArrayList<>(allLayers.size());
        availableLayers.addAll(allLayers.stream().filter(availableLayer -> (! availableLayer.equals(layer)) && (! tableModel.contains(availableLayer))).collect(Collectors.toList()));
        AddLayerDialog dialog = new AddLayerDialog(SwingUtilities.getWindowAncestor(this), availableLayers);
        dialog.setVisible(true);
        if (! dialog.isCancelled()) {
            tableModel.addRow(new CombinedLayerTableModel.Row(dialog.getSelectedLayer(), dialog.getSelectedFactor(), dialog.isHiddenSelected()));
            settingsChanged();
        }
    }

    private void removeLayer() {
        if (tableLayers.isEditing()) {
            tableLayers.getCellEditor().stopCellEditing();
        }
        final int selectedRow = tableLayers.getSelectedRow();
        if (selectedRow != -1) {
            tableModel.deleteRow(selectedRow);
            settingsChanged();
        }
    }
    
    private void editLayerSettings() {
        if (tableLayers.isEditing()) {
            tableLayers.getCellEditor().stopCellEditing();
        }
        final int selectedRow = tableLayers.getSelectedRow();
        if (selectedRow != -1) {
            final Layer layer = (Layer) tableModel.getValueAt(selectedRow, COLUMN_LAYER);
            if (App.getInstance().editCustomLayer((CustomLayer) layer)) {
                tableLayers.repaint();
                settingsChanged();
            }
        }
    }
    
    private void settingsChanged() {
        context.settingsChanged();
    }
    
    private CombinedLayer saveSettings(CombinedLayer layer) {
        if (layer == null) {
            layer = createLayer();
        }
        layer.setName(fieldName.getText().trim());
        layer.setBiome((Integer) comboBoxBiome.getSelectedItem());
        layer.setTerrain((Terrain) comboBoxTerrain.getSelectedItem());
        layer.setApplyTerrainAndBiomeOnExport(checkBoxApplyTerrainAndBiomeOnExport.isSelected());
        tableModel.saveSettings(layer);
        layer.setColour(colourEditor1.getColour());
        return layer;
    }

    private void configureTable() {
        tableLayers.setModel(tableModel);
        TableColumn layerColumn = tableLayers.getColumnModel().getColumn(COLUMN_LAYER);
        JComboBox<Layer> layerCellEditor = new JComboBox<>(allLayers.toArray(new Layer[allLayers.size()]));
        layerCellEditor.setRenderer(new LayerListCellRenderer());
        layerColumn.setCellEditor(new DefaultCellEditor(layerCellEditor));
        layerColumn.setCellRenderer(new LayerTableCellRenderer());
        SpinnerModel factorSpinnerModel = new SpinnerNumberModel(100, 1, 1500, 1);
        tableLayers.getColumnModel().getColumn(COLUMN_FACTOR).setCellEditor(new JSpinnerTableCellEditor(factorSpinnerModel));
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        comboBoxTerrain = new javax.swing.JComboBox<>();
        jLabel3 = new javax.swing.JLabel();
        comboBoxBiome = new javax.swing.JComboBox<>();
        jLabel4 = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        tableLayers = new javax.swing.JTable();
        buttonAddLayer = new javax.swing.JButton();
        jLabel5 = new javax.swing.JLabel();
        fieldName = new javax.swing.JTextField();
        jLabel6 = new javax.swing.JLabel();
        colourEditor1 = new org.pepsoft.worldpainter.ColourEditor();
        buttonRemoveLayer = new javax.swing.JButton();
        checkBoxApplyTerrainAndBiomeOnExport = new javax.swing.JCheckBox();
        buttonEditLayerSettings = new javax.swing.JButton();

        jLabel1.setText("Configure your combined layer on this screen.");

        jLabel2.setText("Terrain:");

        comboBoxTerrain.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                comboBoxTerrainActionPerformed(evt);
            }
        });

        jLabel3.setText("Biome:");

        comboBoxBiome.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                comboBoxBiomeActionPerformed(evt);
            }
        });

        jLabel4.setText("Layers:");

        tableLayers.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        jScrollPane1.setViewportView(tableLayers);

        buttonAddLayer.setText("Add");
        buttonAddLayer.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonAddLayerActionPerformed(evt);
            }
        });

        jLabel5.setText("Name:");

        fieldName.setColumns(20);

        jLabel6.setText("Colour:");

        buttonRemoveLayer.setText("Remove");
        buttonRemoveLayer.setEnabled(false);
        buttonRemoveLayer.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonRemoveLayerActionPerformed(evt);
            }
        });

        checkBoxApplyTerrainAndBiomeOnExport.setText("apply terrain and biome on Export");

        buttonEditLayerSettings.setText("Edit...");
        buttonEditLayerSettings.setEnabled(false);
        buttonEditLayerSettings.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonEditLayerSettingsActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addGap(0, 0, Short.MAX_VALUE)
                .addComponent(buttonEditLayerSettings)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(buttonRemoveLayer)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(buttonAddLayer))
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel1)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel2)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(comboBoxTerrain, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel3)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(comboBoxBiome, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jLabel4)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel5)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(fieldName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel6)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(colourEditor1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(checkBoxApplyTerrainAndBiomeOnExport))
                .addGap(0, 0, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jLabel1)
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(comboBoxTerrain, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3)
                    .addComponent(comboBoxBiome, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(checkBoxApplyTerrainAndBiomeOnExport)
                .addGap(18, 18, 18)
                .addComponent(jLabel4)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 26, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(buttonAddLayer)
                    .addComponent(buttonRemoveLayer)
                    .addComponent(buttonEditLayerSettings))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel5)
                    .addComponent(fieldName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel6)
                    .addComponent(colourEditor1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void comboBoxTerrainActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_comboBoxTerrainActionPerformed
        setControlStates();
        settingsChanged();
    }//GEN-LAST:event_comboBoxTerrainActionPerformed

    private void comboBoxBiomeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_comboBoxBiomeActionPerformed
        setControlStates();
        settingsChanged();
    }//GEN-LAST:event_comboBoxBiomeActionPerformed

    private void buttonAddLayerActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonAddLayerActionPerformed
        addLayer();
    }//GEN-LAST:event_buttonAddLayerActionPerformed

    private void buttonRemoveLayerActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonRemoveLayerActionPerformed
        removeLayer();
    }//GEN-LAST:event_buttonRemoveLayerActionPerformed

    private void buttonEditLayerSettingsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonEditLayerSettingsActionPerformed
        editLayerSettings();
    }//GEN-LAST:event_buttonEditLayerSettingsActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton buttonAddLayer;
    private javax.swing.JButton buttonEditLayerSettings;
    private javax.swing.JButton buttonRemoveLayer;
    private javax.swing.JCheckBox checkBoxApplyTerrainAndBiomeOnExport;
    private org.pepsoft.worldpainter.ColourEditor colourEditor1;
    private javax.swing.JComboBox<Integer> comboBoxBiome;
    private javax.swing.JComboBox<Terrain> comboBoxTerrain;
    private javax.swing.JTextField fieldName;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTable tableLayers;
    // End of variables declaration//GEN-END:variables

    private CombinedLayerTableModel tableModel;
    private List<Layer> allLayers;

    private static final long serialVersionUID = 1L;
}