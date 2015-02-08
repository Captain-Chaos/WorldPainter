/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.layers.combined;

import java.awt.Window;
import java.util.ArrayList;
import java.util.List;
import javax.swing.DefaultCellEditor;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableColumn;
import org.pepsoft.worldpainter.BiomeListCellRenderer;
import org.pepsoft.worldpainter.BiomeScheme;
import org.pepsoft.worldpainter.ColourScheme;
import org.pepsoft.worldpainter.LayerListCellRenderer;
import org.pepsoft.worldpainter.Terrain;
import org.pepsoft.worldpainter.biomeschemes.CustomBiome;
import org.pepsoft.worldpainter.biomeschemes.CustomBiomeManager;
import org.pepsoft.worldpainter.layers.CombinedLayer;
import org.pepsoft.worldpainter.layers.CustomLayerDialog;
import org.pepsoft.worldpainter.layers.Layer;
import static org.pepsoft.worldpainter.layers.combined.CombinedLayerTableModel.*;
import org.pepsoft.worldpainter.themes.JSpinnerTableCellEditor;
import org.pepsoft.worldpainter.themes.TerrainListCellRenderer;

/**
 *
 * @author pepijn
 */
public class CombinedLayerDialog extends CustomLayerDialog<CombinedLayer> {
    /**
     * Creates new form CombinedLayerDialog
     */
    public CombinedLayerDialog(Window parent, BiomeScheme biomeScheme, ColourScheme colourScheme, CustomBiomeManager customBiomeManager, CombinedLayer layer, List<Layer> allLayers) {
        super(parent);
        this.layer = layer;
        this.biomeScheme = biomeScheme;
        this.colourScheme = colourScheme;
        this.allLayers = allLayers;
        this.customBiomeManager = customBiomeManager;
        
        initComponents();
        
        tableModel = new CombinedLayerTableModel(layer.getLayers(), layer.getFactors());
        tableModel.addTableModelListener(new TableModelListener() {
            @Override
            public void tableChanged(TableModelEvent e) {
                setControlStates();
            }
        });
        configureTable();

        comboBoxTerrain.setSelectedItem(layer.getTerrain());

        fieldName.setText(layer.getName());
        fieldName.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                setControlStates();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                setControlStates();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                setControlStates();
            }
        });

        colourEditor1.setColour(layer.getColour());
        
        List<Integer> allBiomes = new ArrayList<Integer>();
        allBiomes.add(-1);
        int biomeCount = biomeScheme.getBiomeCount();
        for (int i = 0; i < biomeCount; i++) {
            if (biomeScheme.isBiomePresent(i)) {
                allBiomes.add(i);
            }
        }
        List<CustomBiome> customBiomes = customBiomeManager.getCustomBiomes();
        if (customBiomes != null) {
            for (CustomBiome customBiome: customBiomes) {
                allBiomes.add(customBiome.getId());
            }
        }
        comboBoxBiome.setModel(new DefaultComboBoxModel(allBiomes.toArray()));
        comboBoxBiome.setSelectedItem(layer.getBiome());
        
        rootPane.setDefaultButton(buttonOK);
        pack();
        setLocationRelativeTo(parent);
        
        setControlStates();
    }

    @Override
    public CombinedLayer getSelectedLayer() {
        return layer;
    }

    private void addLayer() {
        List<Layer> availableLayers = new ArrayList<Layer>(allLayers.size());
        for (Layer layer: allLayers) {
            if ((! layer.equals(this.layer)) && (! tableModel.contains(layer))) {
                availableLayers.add(layer);
            }
        }
        AddLayerDialog dialog = new AddLayerDialog(this, availableLayers);
        dialog.setVisible(true);
        if (! dialog.isCancelled()) {
            tableModel.addRow(new Row(dialog.getSelectedLayer(), dialog.getSelectedFactor(), dialog.isHiddenSelected()));
        }
    }
    
    private void saveSettings() {
        layer.setName(fieldName.getText().trim());
        layer.setBiome((Integer) comboBoxBiome.getSelectedItem());
        layer.setTerrain((Terrain) comboBoxTerrain.getSelectedItem());
        tableModel.saveSettings(layer);
        layer.setColour(colourEditor1.getColour());
    }

    private void configureTable() {
        tableLayers.setModel(tableModel);
        TableColumn layerColumn = tableLayers.getColumnModel().getColumn(COLUMN_LAYER);
        JComboBox layerCellEditor = new JComboBox(allLayers.toArray());
        layerCellEditor.setRenderer(new LayerListCellRenderer());
        layerColumn.setCellEditor(new DefaultCellEditor(layerCellEditor));
        layerColumn.setCellRenderer(new LayerTableCellRenderer());
        SpinnerModel factorSpinnerModel = new SpinnerNumberModel(100, 1, 1500, 1);
        tableLayers.getColumnModel().getColumn(COLUMN_FACTOR).setCellEditor(new JSpinnerTableCellEditor(factorSpinnerModel));
    }

    private void setControlStates() {
        boolean terrainSelected = comboBoxTerrain.getSelectedItem() != null;
        boolean biomeSelected = (comboBoxBiome.getSelectedItem() != null) && ((Integer) comboBoxBiome.getSelectedItem() != -1);
        int layersSelected = tableModel.getRowCount();
        boolean nameEntered = ! fieldName.getText().trim().isEmpty();
        buttonOK.setEnabled(nameEntered
            && ((terrainSelected && biomeSelected)
                || ((terrainSelected || biomeSelected) && (layersSelected > 0))
                || (layersSelected > 1)));
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        comboBoxTerrain = new javax.swing.JComboBox();
        jLabel3 = new javax.swing.JLabel();
        comboBoxBiome = new javax.swing.JComboBox();
        jLabel4 = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        tableLayers = new javax.swing.JTable();
        buttonAddLayer = new javax.swing.JButton();
        jLabel5 = new javax.swing.JLabel();
        fieldName = new javax.swing.JTextField();
        jLabel6 = new javax.swing.JLabel();
        colourEditor1 = new org.pepsoft.worldpainter.ColourEditor();
        buttonCancel = new javax.swing.JButton();
        buttonOK = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Configure Combined Layer");

        jLabel1.setText("Configure your combined layer on this screen.");

        jLabel2.setText("Terrain:");

        comboBoxTerrain.setModel(new DefaultComboBoxModel(Terrain.getConfiguredValues()));
        comboBoxTerrain.setRenderer(new TerrainListCellRenderer(colourScheme, "none"));
        comboBoxTerrain.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                comboBoxTerrainActionPerformed(evt);
            }
        });

        jLabel3.setText("Biome:");

        comboBoxBiome.setRenderer(new BiomeListCellRenderer(biomeScheme, colourScheme, customBiomeManager, "none"));
        comboBoxBiome.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                comboBoxBiomeActionPerformed(evt);
            }
        });

        jLabel4.setText("Layers:");

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

        buttonCancel.setText("Cancel");
        buttonCancel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonCancelActionPerformed(evt);
            }
        });

        buttonOK.setText("OK");
        buttonOK.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonOKActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
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
                                .addComponent(colourEditor1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                .addComponent(buttonOK)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(buttonCancel))
                            .addComponent(buttonAddLayer, javax.swing.GroupLayout.Alignment.TRAILING))))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel1)
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(comboBoxTerrain, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3)
                    .addComponent(comboBoxBiome, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addComponent(jLabel4)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 126, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(buttonAddLayer)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel5)
                    .addComponent(fieldName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel6)
                    .addComponent(colourEditor1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(buttonCancel)
                    .addComponent(buttonOK))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void buttonAddLayerActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonAddLayerActionPerformed
        addLayer();
    }//GEN-LAST:event_buttonAddLayerActionPerformed

    private void buttonCancelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonCancelActionPerformed
        cancel();
    }//GEN-LAST:event_buttonCancelActionPerformed

    private void buttonOKActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonOKActionPerformed
        if (tableLayers.isEditing()) {
            tableLayers.getCellEditor().stopCellEditing();
        }
        saveSettings();
        ok();
    }//GEN-LAST:event_buttonOKActionPerformed

    private void comboBoxTerrainActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_comboBoxTerrainActionPerformed
        setControlStates();
    }//GEN-LAST:event_comboBoxTerrainActionPerformed

    private void comboBoxBiomeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_comboBoxBiomeActionPerformed
        setControlStates();
    }//GEN-LAST:event_comboBoxBiomeActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton buttonAddLayer;
    private javax.swing.JButton buttonCancel;
    private javax.swing.JButton buttonOK;
    private org.pepsoft.worldpainter.ColourEditor colourEditor1;
    private javax.swing.JComboBox comboBoxBiome;
    private javax.swing.JComboBox comboBoxTerrain;
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

    private final CombinedLayer layer;
    private final ColourScheme colourScheme;
    private final BiomeScheme biomeScheme;
    private final CombinedLayerTableModel tableModel;
    private final List<Layer> allLayers;
    private final CustomBiomeManager customBiomeManager;

    private static final long serialVersionUID = 1L;
}