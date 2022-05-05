/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/*
 * TerrainRangesEditor.java
 *
 * Created on Mar 28, 2012, 5:53:35 PM
 */
package org.pepsoft.worldpainter.themes.impl.simple;

import org.pepsoft.worldpainter.App;
import org.pepsoft.worldpainter.ColourScheme;
import org.pepsoft.worldpainter.Terrain;
import org.pepsoft.worldpainter.layers.Layer;
import org.pepsoft.worldpainter.layers.LayerTableCellRenderer;
import org.pepsoft.worldpainter.themes.*;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.util.ArrayList;

/**
 *
 * @author pepijn
 */
public class SimpleThemeEditor extends javax.swing.JPanel implements ButtonPressListener, TerrainRangesTableModel.ChangeListener {
    /** Creates new form TerrainRangesEditor */
    public SimpleThemeEditor() {
        initComponents();
    }

    public boolean save() {
        if (terrainTableModel == null) {
            return true;
        }
        if (! programmaticChange) {
            if (tableTerrain.isEditing()) {
                tableTerrain.getCellEditor().stopCellEditing();
            }
            if (! terrainTableModel.isValid()) {
                JOptionPane.showMessageDialog(this, "You have configured multiple terrain types with the same levels!\nRemove, or change the level of, one of the duplicates.", "Duplicate Levels", JOptionPane.WARNING_MESSAGE);
                return false;
            }
            if (tableLayers.isEditing()) {
                tableLayers.getCellEditor().stopCellEditing();
            }
        }
        theme.setTerrainRanges(terrainTableModel.getTerrainRanges());
        theme.setRandomise(checkBoxRandomise.isSelected());
        boolean beaches = checkBoxBeaches.isSelected();
        theme.setBeaches(beaches);
        theme.setWaterHeight(((Number) spinnerWaterLevel.getValue()).intValue());
        theme.setLayerMap(layerTableModel.getLayerMap());
        return true;
    }

    public ColourScheme getColourScheme() {
        return colourScheme;
    }

    public void setColourScheme(ColourScheme colourScheme) {
        this.colourScheme = colourScheme;
    }

    public SimpleTheme getTheme() {
        return theme;
    }

    public void setTheme(SimpleTheme theme) {
        this.theme = theme;
        if (theme != null) {
            terrainTableModel = new TerrainRangesTableModel(theme.getTerrainRanges());
            terrainTableModel.setChangeListener(this);
            tableTerrain.setModel(terrainTableModel);
            
            tableTerrain.setDefaultRenderer(Integer.class, new DefaultTableCellRenderer());
            tableTerrain.setDefaultRenderer(Terrain.class, new TerrainTableCellRenderer(colourScheme));
            tableTerrain.setDefaultRenderer(JButton.class, new JButtonTableCellRenderer());
            
            tableTerrain.setDefaultEditor(Integer.class, new JSpinnerTableCellEditor(new SpinnerNumberModel(1, 1, theme.getMaxHeight() - 1, 1)));
            JComboBox terrainEditor = new JComboBox(Terrain.getConfiguredValues());
            terrainEditor.setRenderer(new TerrainListCellRenderer(colourScheme));
            tableTerrain.setDefaultEditor(Terrain.class, new DefaultCellEditor(terrainEditor));
            tableTerrain.setDefaultEditor(JButton.class, new JButtonTableCellEditor(this));
            
            checkBoxBeaches.setSelected(theme.isBeaches());
            spinnerWaterLevel.setModel(new SpinnerNumberModel(theme.getWaterHeight(), 0, theme.getMaxHeight() - 1, 1));
            spinnerWaterLevel.setEnabled(checkBoxBeaches.isSelected());
            
            checkBoxRandomise.setSelected(theme.isRandomise());
            
            layerTableModel = new LayerRangesTableModel(theme.getMinHeight(), theme.getMaxHeight(), theme.getLayerMap());
            tableLayers.setModel(layerTableModel);

            tableLayers.setDefaultRenderer(Layer.class, new LayerTableCellRenderer());
            tableLayers.setDefaultRenderer(JButton.class, new JButtonTableCellRenderer());
            
            tableLayers.setDefaultEditor(Integer.class, new JSpinnerTableCellEditor(new SpinnerNumberModel(1, 1, theme.getMaxHeight() - 1, 1)));
            tableLayers.setDefaultEditor(JButton.class, new JButtonTableCellEditor(this));
        }
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        tableTerrain.setEnabled(enabled);
        checkBoxBeaches.setEnabled(enabled);
        spinnerWaterLevel.setEnabled(enabled && checkBoxBeaches.isSelected());
        buttonAddTerrain.setEnabled(enabled);
        checkBoxRandomise.setEnabled(enabled);
    }

    public ChangeListener getChangeListener() {
        return changeListener;
    }

    public void setChangeListener(ChangeListener changeListener) {
        this.changeListener = changeListener;
    }

    public void setWaterHeight(int waterHeight) {
        spinnerWaterLevel.setValue(waterHeight);
    }

    // ButtonPressListener

    @Override
    public void buttonPressed(JTable source, int row, int column) {
        if (source == tableTerrain) {
            terrainTableModel.deleteRow(row);
        } else if (source == tableLayers) {
            layerTableModel.deleteRow(row);
        } else {
            throw new IllegalArgumentException("Unknown source " + source);
        }
    }

    // TerrainRangesTableModel.ChangeListener
    
    @Override
    public void dataChanged(TerrainRangesTableModel model) {
        notifyChangeListener();
    }

    private void addTerrain() {
        AddTerrainRangeDialog dialog = new AddTerrainRangeDialog(SwingUtilities.getWindowAncestor(this), theme.getMaxHeight(), colourScheme);
        dialog.setVisible(true);
        if (! dialog.isCancelled()) {
            terrainTableModel.addRow(dialog.getSelectedLevel(), dialog.getSelectedTerrain());
        }
    }

    private void addLayer() {
        Window window = SwingUtilities.getWindowAncestor(this);
        AddLayerDialog dialog = new AddLayerDialog(window, new ArrayList<>(App.getInstance().getAllLayers()), theme.getMinHeight(), theme.getMaxHeight());
        dialog.setVisible(true);
        if (! dialog.isCancelled()) {
            layerTableModel.addRow(dialog.getSelectedFilter(), dialog.getSelectedLayer());
        }
    }
    
    private void notifyChangeListener() {
        if (changeListener != null) {
            programmaticChange = true;
            try {
                changeListener.settingsModified(this);
            } finally {
                programmaticChange = false;
            }
        }
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jButton1 = new javax.swing.JButton();
        jTabbedPane1 = new javax.swing.JTabbedPane();
        jPanel1 = new javax.swing.JPanel();
        checkBoxBeaches = new javax.swing.JCheckBox();
        jScrollPane1 = new javax.swing.JScrollPane();
        tableTerrain = new javax.swing.JTable();
        buttonAddTerrain = new javax.swing.JButton();
        spinnerWaterLevel = new javax.swing.JSpinner();
        checkBoxRandomise = new javax.swing.JCheckBox();
        jPanel2 = new javax.swing.JPanel();
        buttonAddLayer = new javax.swing.JButton();
        jScrollPane2 = new javax.swing.JScrollPane();
        tableLayers = new javax.swing.JTable();

        jButton1.setText("jButton1");

        checkBoxBeaches.setText("beaches around water level:");
        checkBoxBeaches.setToolTipText("Whether to add beaches from two levels below the water level to two levels above.");
        checkBoxBeaches.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                checkBoxBeachesActionPerformed(evt);
            }
        });

        tableTerrain.setRowHeight(25);
        jScrollPane1.setViewportView(tableTerrain);

        buttonAddTerrain.setText("Add...");
        buttonAddTerrain.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonAddTerrainActionPerformed(evt);
            }
        });

        spinnerWaterLevel.setEnabled(false);
        spinnerWaterLevel.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                spinnerWaterLevelStateChanged(evt);
            }
        });

        checkBoxRandomise.setText("noisy edges");
        checkBoxRandomise.setToolTipText("Whether to randomise the edges of the terrain types (except beaches).");
        checkBoxRandomise.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                checkBoxRandomiseActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addComponent(checkBoxBeaches)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(spinnerWaterLevel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(checkBoxRandomise)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 26, Short.MAX_VALUE)
                .addComponent(buttonAddTerrain))
            .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 167, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(buttonAddTerrain)
                    .addComponent(checkBoxBeaches)
                    .addComponent(spinnerWaterLevel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(checkBoxRandomise))
                .addGap(0, 0, 0))
        );

        jTabbedPane1.addTab("Terrain", jPanel1);

        buttonAddLayer.setText("Add...");
        buttonAddLayer.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonAddLayerActionPerformed(evt);
            }
        });

        jScrollPane2.setViewportView(tableLayers);

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 383, Short.MAX_VALUE)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(buttonAddLayer)))
                .addGap(0, 0, 0))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 167, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(buttonAddLayer)
                .addGap(0, 0, 0))
        );

        jTabbedPane1.addTab("Layers", jPanel2);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jTabbedPane1)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jTabbedPane1, javax.swing.GroupLayout.Alignment.TRAILING)
        );
    }// </editor-fold>//GEN-END:initComponents

    private void checkBoxRandomiseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_checkBoxRandomiseActionPerformed
        notifyChangeListener();
    }//GEN-LAST:event_checkBoxRandomiseActionPerformed

    private void spinnerWaterLevelStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_spinnerWaterLevelStateChanged
        notifyChangeListener();
    }//GEN-LAST:event_spinnerWaterLevelStateChanged

    private void buttonAddTerrainActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonAddTerrainActionPerformed
        addTerrain();
    }//GEN-LAST:event_buttonAddTerrainActionPerformed

    private void checkBoxBeachesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_checkBoxBeachesActionPerformed
        spinnerWaterLevel.setEnabled(checkBoxBeaches.isSelected());
        notifyChangeListener();
    }//GEN-LAST:event_checkBoxBeachesActionPerformed

    private void buttonAddLayerActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonAddLayerActionPerformed
        addLayer();
    }//GEN-LAST:event_buttonAddLayerActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton buttonAddLayer;
    private javax.swing.JButton buttonAddTerrain;
    private javax.swing.JCheckBox checkBoxBeaches;
    private javax.swing.JCheckBox checkBoxRandomise;
    private javax.swing.JButton jButton1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JSpinner spinnerWaterLevel;
    private javax.swing.JTable tableLayers;
    private javax.swing.JTable tableTerrain;
    // End of variables declaration//GEN-END:variables

    private SimpleTheme theme;
    private TerrainRangesTableModel terrainTableModel;
    private ColourScheme colourScheme;
    private ChangeListener changeListener;
    private LayerRangesTableModel layerTableModel;
    private boolean programmaticChange;
    
    private static final long serialVersionUID = 1L;
    
    public interface ChangeListener {
        /**
         * Indicates that the user made changes in the specified theme editor.
         * <strong>Note:</strong> the changes have <em>not</em> been saved to
         * the theme yet!
         * 
         * @param editor The editor in which changes were made.
         */
        void settingsModified(SimpleThemeEditor editor);
    }
}