/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JDialog.java to edit this template
 */
package org.pepsoft.worldpainter;

import org.pepsoft.util.DesktopUtils;
import org.pepsoft.worldpainter.TileRenderer.LightOrigin;
import org.pepsoft.worldpainter.biomeschemes.CustomBiomeManager;
import org.pepsoft.worldpainter.exporting.WorldExportSettings;
import org.pepsoft.worldpainter.layers.Layer;

import javax.swing.*;
import java.awt.*;
import java.util.EnumSet;
import java.util.Set;

import static java.util.Collections.singleton;
import static org.pepsoft.worldpainter.Constants.*;
import static org.pepsoft.worldpainter.Dimension.Role.DETAIL;
import static org.pepsoft.worldpainter.exporting.WorldExportSettings.Step.*;

/**
 *
 * @author pepijn
 */
public class TestExportDialog extends WorldPainterDialog {
    /**
     * Creates new form TestExportDialog
     */
    public TestExportDialog(Window parent, World2 world, ColourScheme colourScheme, CustomBiomeManager customBiomeManager, Set<Layer> hiddenLayers, boolean contourLines, int contourSeparation, LightOrigin lightOrigin) {
        super(parent);
        this.world = world;
        this.colourScheme = colourScheme;
        this.customBiomeManager = customBiomeManager;
        this.hiddenLayers = hiddenLayers;
        this.contourLines = contourLines;
        this.contourSeparation = contourSeparation;
        this.lightOrigin = lightOrigin;
        final WorldExportSettings exportSettings;
        if (world.getExportSettings() != null) {
            exportSettings = world.getExportSettings();
        } else {
            exportSettings = new WorldExportSettings();
            exportSettings.setDimensionsToExport(singleton(DIM_NORMAL));
        }
        selectedTiles = exportSettings.getTilesToExport();
        selectedDimension = (exportSettings.getDimensionsToExport() != null) ? exportSettings.getDimensionsToExport().iterator().next() : DIM_NORMAL; // TODO support multiple selected dimensions

        initComponents();

        final Integer[] dimensions = world.getDimensionsWithRole(DETAIL, false, 0).stream().map(d -> d.getAnchor().dim).sorted().toArray(Integer[]::new);
        comboBoxDimension.setModel(new DefaultComboBoxModel<>(dimensions));
        comboBoxDimension.setSelectedItem(selectedDimension);
        comboBoxDimension.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                switch ((int) value) {
                    case DIM_NORMAL:
                        setText("Surface");
                        break;
                    case DIM_NETHER:
                        setText("Nether");
                        break;
                    case DIM_END:
                        setText("End");
                        break;
                }
                return this;
            }
        });
        checkBoxDimension.setEnabled(dimensions.length > 1);
        if ((exportSettings.getDimensionsToExport() == null) && (dimensions.length > 1)) {
            checkBoxDimension.setSelected(false);
        }
        if ((selectedTiles != null) && (! selectedTiles.isEmpty())) {
            checkBoxTiles.setText("export subset of tiles (" + selectedTiles.size() + " tiles selected)");
            checkBoxTiles.setSelected(true);
        }
        final Set<WorldExportSettings.Step> stepsToSkip = exportSettings.getStepsToSkip();
        checkBoxCaves.setSelected((stepsToSkip == null) || stepsToSkip.contains(CAVES));
        checkBoxResources.setSelected((stepsToSkip == null) || stepsToSkip.contains(RESOURCES));
        checkBoxLighting.setSelected((stepsToSkip == null) || stepsToSkip.contains(LIGHTING));
        checkBoxLeaves.setSelected((stepsToSkip == null) || stepsToSkip.contains(LEAVES));
        checkBoxUnderground.setSelected(checkBoxCaves.isSelected() && checkBoxResources.isSelected());

        pack();
        setLocationRelativeTo(parent);
        setControlStates();
    }

    private void setControlStates() {
        if (checkBoxTiles.isSelected()) {
            labelSelectTiles.setForeground(Color.BLUE);
            labelSelectTiles.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        } else {
            labelSelectTiles.setForeground(null);
            labelSelectTiles.setCursor(null);
        }
        comboBoxDimension.setEnabled(checkBoxDimension.isSelected() && (comboBoxDimension.getItemCount() > 1));
        checkBoxTiles.setEnabled(checkBoxDimension.isSelected());
        checkBoxCaves.setEnabled(! checkBoxUnderground.isSelected());
        checkBoxResources.setEnabled(! checkBoxUnderground.isSelected());
    }

    private void selectTiles() {
        if (checkBoxTiles.isSelected()) {
            ExportTileSelectionDialog dialog = new ExportTileSelectionDialog(this, world, selectedDimension, selectedTiles, colourScheme, customBiomeManager, hiddenLayers, contourLines, contourSeparation, lightOrigin);
            dialog.setVisible(true);
            if (! dialog.isCancelled()) {
                selectedDimension = dialog.getSelectedDimension();
                comboBoxDimension.setSelectedItem(selectedDimension);
                selectedTiles = dialog.getSelectedTiles();
                checkBoxTiles.setText("export subset of tiles (" + selectedTiles.size() + " tiles selected)");
            }
            if ((selectedTiles == null) || selectedTiles.isEmpty()) {
                checkBoxTiles.setSelected(false);
            }
            setControlStates();
        }
    }

    private void export() {
        if ((checkBoxTiles.isSelected()) && ((selectedTiles == null) || selectedTiles.isEmpty())) {
            checkBoxTiles.requestFocusInWindow();
            DesktopUtils.beep();
            JOptionPane.showMessageDialog(this, "No tiles have been selected for export.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        final Set<WorldExportSettings.Step> skipSteps = EnumSet.noneOf(WorldExportSettings.Step.class);
        if (checkBoxCaves.isSelected()) {
            skipSteps.add(CAVES);
        }
        if (checkBoxResources.isSelected()) {
            skipSteps.add(RESOURCES);
        }
        if (checkBoxLighting.isSelected()) {
            skipSteps.add(LIGHTING);
        }
        if (checkBoxLeaves.isSelected()) {
            skipSteps.add(LEAVES);
        }
        final WorldExportSettings exportSettings;
        if (checkBoxDimension.isSelected()) {
            if (checkBoxTiles.isSelected()) {
                exportSettings = new WorldExportSettings(singleton(selectedDimension), selectedTiles, skipSteps.isEmpty() ? null : skipSteps);
            } else if (comboBoxDimension.getItemCount() > 1) {
                exportSettings = new WorldExportSettings(singleton((Integer) comboBoxDimension.getSelectedItem()), null, skipSteps.isEmpty() ? null : skipSteps);
            } else {
                exportSettings = (skipSteps.isEmpty() ? null : new WorldExportSettings(null, null, skipSteps));
            }
        } else {
            exportSettings = (skipSteps.isEmpty() ? null : new WorldExportSettings(null, null, skipSteps));
        }
        world.setExportSettings(exportSettings);
        ok();
    }

    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The
     * content of this method is always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jButton2 = new javax.swing.JButton();
        jButton1 = new javax.swing.JButton();
        jButton3 = new javax.swing.JButton();
        jLabel1 = new javax.swing.JLabel();
        checkBoxTiles = new javax.swing.JCheckBox();
        labelSelectTiles = new javax.swing.JLabel();
        checkBoxUnderground = new javax.swing.JCheckBox();
        checkBoxCaves = new javax.swing.JCheckBox();
        checkBoxResources = new javax.swing.JCheckBox();
        checkBoxLighting = new javax.swing.JCheckBox();
        checkBoxLeaves = new javax.swing.JCheckBox();
        checkBoxDimension = new javax.swing.JCheckBox();
        comboBoxDimension = new javax.swing.JComboBox<>();
        jLabel3 = new javax.swing.JLabel();

        jButton2.setText("jButton2");

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Test Export");

        jButton1.setText("Cancel");
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });

        jButton3.setText("Export");
        jButton3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton3ActionPerformed(evt);
            }
        });

        jLabel1.setText("<html>Perform a test export with reduced area and/or features<br>for faster export:</html>");

        checkBoxTiles.setText("export subset of tiles");
        checkBoxTiles.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                checkBoxTilesActionPerformed(evt);
            }
        });

        labelSelectTiles.setForeground(new java.awt.Color(0, 0, 255));
        labelSelectTiles.setText("<html><u>select tiles</u></html>");
        labelSelectTiles.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        labelSelectTiles.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                labelSelectTilesMouseClicked(evt);
            }
        });

        checkBoxUnderground.setSelected(true);
        checkBoxUnderground.setText("do not export underground features");
        checkBoxUnderground.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                checkBoxUndergroundActionPerformed(evt);
            }
        });

        checkBoxCaves.setSelected(true);
        checkBoxCaves.setText("do not export caves, caverns, chasms and custom cave/tunnel layers");
        checkBoxCaves.setEnabled(false);

        checkBoxResources.setSelected(true);
        checkBoxResources.setText("do not export resources and custom underground pockets layers");
        checkBoxResources.setEnabled(false);

        checkBoxLighting.setSelected(true);
        checkBoxLighting.setText("do not perform lighting");

        checkBoxLeaves.setSelected(true);
        checkBoxLeaves.setText("do not perform leaf decay calculations (no leaf blocks will decay)");

        checkBoxDimension.setSelected(true);
        checkBoxDimension.setText("export only the");
        checkBoxDimension.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                checkBoxDimensionActionPerformed(evt);
            }
        });

        comboBoxDimension.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                comboBoxDimensionActionPerformed(evt);
            }
        });

        jLabel3.setText("dimension");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addGap(0, 0, Short.MAX_VALUE)
                                .addComponent(jButton3)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jButton1))
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(checkBoxTiles)
                                .addGap(0, 0, Short.MAX_VALUE)))
                        .addContainerGap())
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(checkBoxDimension)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(comboBoxDimension, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabel3))
                            .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(checkBoxUnderground)
                            .addGroup(layout.createSequentialGroup()
                                .addGap(21, 21, 21)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(checkBoxCaves)
                                    .addComponent(labelSelectTiles, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(checkBoxResources)))
                            .addComponent(checkBoxLighting)
                            .addComponent(checkBoxLeaves))
                        .addGap(0, 13, Short.MAX_VALUE))))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(checkBoxDimension)
                    .addComponent(comboBoxDimension, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel3))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(checkBoxTiles)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(labelSelectTiles, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(checkBoxUnderground)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(checkBoxCaves)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(checkBoxResources)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(checkBoxLighting)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(checkBoxLeaves)
                .addGap(18, 18, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButton1)
                    .addComponent(jButton3))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void labelSelectTilesMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_labelSelectTilesMouseClicked
        selectTiles();
    }//GEN-LAST:event_labelSelectTilesMouseClicked

    private void checkBoxUndergroundActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_checkBoxUndergroundActionPerformed
        checkBoxCaves.setSelected(checkBoxUnderground.isSelected());
        checkBoxResources.setSelected(checkBoxUnderground.isSelected());
        setControlStates();
    }//GEN-LAST:event_checkBoxUndergroundActionPerformed

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        cancel();
    }//GEN-LAST:event_jButton1ActionPerformed

    private void jButton3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton3ActionPerformed
        export();
    }//GEN-LAST:event_jButton3ActionPerformed

    private void checkBoxTilesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_checkBoxTilesActionPerformed
        setControlStates();
        if (checkBoxTiles.isSelected() && ((selectedTiles == null) || selectedTiles.isEmpty())) {
            selectTiles();
        }
    }//GEN-LAST:event_checkBoxTilesActionPerformed

    private void checkBoxDimensionActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_checkBoxDimensionActionPerformed
        if (! checkBoxDimension.isSelected()) {
            checkBoxTiles.setSelected(false);
        }
        setControlStates();
    }//GEN-LAST:event_checkBoxDimensionActionPerformed

    private void comboBoxDimensionActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_comboBoxDimensionActionPerformed
        final int newSelectedDimension = (int) comboBoxDimension.getSelectedItem();
        if (newSelectedDimension != selectedDimension) {
            selectedDimension = newSelectedDimension;
            selectedTiles = null;
            checkBoxTiles.setSelected(false);
            checkBoxTiles.setText("export subset of tiles");
        }
    }//GEN-LAST:event_comboBoxDimensionActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JCheckBox checkBoxCaves;
    private javax.swing.JCheckBox checkBoxDimension;
    private javax.swing.JCheckBox checkBoxLeaves;
    private javax.swing.JCheckBox checkBoxLighting;
    private javax.swing.JCheckBox checkBoxResources;
    private javax.swing.JCheckBox checkBoxTiles;
    private javax.swing.JCheckBox checkBoxUnderground;
    private javax.swing.JComboBox<Integer> comboBoxDimension;
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton2;
    private javax.swing.JButton jButton3;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel labelSelectTiles;
    // End of variables declaration//GEN-END:variables

    private int selectedDimension;
    private Set<Point> selectedTiles;
    private final World2 world;
    private final ColourScheme colourScheme;
    private final CustomBiomeManager customBiomeManager;
    private final Set<Layer> hiddenLayers;
    private final boolean contourLines;
    private final int contourSeparation;
    private final LightOrigin lightOrigin;
}