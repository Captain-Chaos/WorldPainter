/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.worldpainter.layers.groundcover;

import org.pepsoft.worldpainter.MixedMaterial;
import org.pepsoft.worldpainter.MixedMaterialManager;
import org.pepsoft.worldpainter.NoiseSettings;
import org.pepsoft.worldpainter.Platform;
import org.pepsoft.worldpainter.layers.AbstractLayerEditor;
import org.pepsoft.worldpainter.layers.exporters.ExporterSettings;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;

import static org.pepsoft.minecraft.Material.ROSE;

/**
 *
 * @author Pepijn Schmitz
 */
public class GroundCoverLayerEditor extends AbstractLayerEditor<GroundCoverLayer> {
    /**
     * Creates new form GroundCoverLayerEditor
     */
    public GroundCoverLayerEditor(Platform platform) {
        initComponents();
        this.platform = platform;

        mixedMaterialSelector1.setPlatform(platform);
        if ("true".equalsIgnoreCase(System.getProperty("org.pepsoft.worldpainter.westerosCraftMode"))) {
            // Leave checkbox and label visible
        } else if ("true".equalsIgnoreCase(System.getProperty("org.pepsoft.worldpainter.smoothGroundCover"))) {
            labelWesterosCraftFeature.setVisible(false);
        } else {
            checkBoxSmooth.setVisible(false);
            labelWesterosCraftFeature.setVisible(false);
        }
        final int maxThickness = platform.maxMaxHeight - platform.minMinHeight;
        ((SpinnerNumberModel) spinnerThickness.getModel()).setMaximum(maxThickness);
        ((SpinnerNumberModel) spinnerThickness.getModel()).setMinimum(-maxThickness);

        setControlStates();
        
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
        mixedMaterialSelector1.addPropertyChangeListener("material", event -> {
            if ((((MixedMaterial) event.getNewValue()).getMode() != MixedMaterial.Mode.LAYERED)
                    || (layer == null)) {
                comboBoxLayerAnchor.setSelectedItem(null);
            } else {
                comboBoxLayerAnchor.setSelectedIndex(layer.getLayerAnchor().ordinal());
            }
            settingsChanged();
        });

        programmaticChange = false;
    }

    // LayerEditor
    
    @Override
    public GroundCoverLayer createLayer() {
        return new GroundCoverLayer("My Ground Cover", MixedMaterial.create(platform, ROSE), Color.RED);
    }

    @Override
    public void setLayer(GroundCoverLayer layer) {
        super.setLayer(layer);
        reset();
    }

    @Override
    public void commit() {
        if (! isCommitAvailable()) {
            throw new IllegalStateException("Settings invalid or incomplete");
        }
        // Make sure the material is registered, in case it's new
        layer.setMaterial(MixedMaterialManager.getInstance().register(mixedMaterialSelector1.getMaterial()));
        saveSettings(layer);
    }

    @Override
    public void reset() {
        fieldName.setText(layer.getName());
        if (layer.getMaterial().getMode() == MixedMaterial.Mode.LAYERED) {
            comboBoxLayerAnchor.setSelectedIndex(layer.getLayerAnchor().ordinal());
        } else {
            comboBoxLayerAnchor.setSelectedItem(null);
        }
        spinnerThickness.setValue(layer.getThickness());
        paintPicker1.setPaint(layer.getPaint());
        paintPicker1.setOpacity(layer.getOpacity());
        switch (layer.getEdgeShape()) {
            case SHEER:
                radioButtonSheerEdge.setSelected(true);
                break;
            case LINEAR:
                radioButtonLinearEdge.setSelected(true);
                break;
            case SMOOTH:
                radioButtonSmoothEdge.setSelected(true);
                break;
            case ROUNDED:
                radioButtonRoundedEdge.setSelected(true);
                break;
        }
        spinnerEdgeWidth.setValue(layer.getEdgeWidth());
        if (layer.getNoiseSettings() != null) {
            noiseSettingsEditor1.setNoiseSettings(layer.getNoiseSettings());
        }
        checkBoxSmooth.setSelected(layer.isSmooth());
        mixedMaterialSelector1.setMaterial(layer.getMaterial());
        settingsChanged();
    }

    @Override
    public ExporterSettings getSettings() {
        if (! isCommitAvailable()) {
            throw new IllegalStateException("Settings invalid or incomplete");
        }
        final GroundCoverLayer previewLayer = saveSettings(null);
        return new ExporterSettings() {
            @Override
            public boolean isApplyEverywhere() {
                return false;
            }

            @Override
            public GroundCoverLayer getLayer() {
                return previewLayer;
            }

            @Override
            public ExporterSettings clone() {
                throw new UnsupportedOperationException("Not supported");
            }
        };
    }

    @Override
    public boolean isCommitAvailable() {
        return ((Integer) spinnerThickness.getValue() != 0) && (! fieldName.getText().trim().isEmpty());
    }

    @Override
    public void setContext(LayerEditorContext context) {
        super.setContext(context);
        mixedMaterialSelector1.setColourScheme(context.getColourScheme());
        mixedMaterialSelector1.setExtendedBlockIds(context.isExtendedBlockIds());
    }

    private void settingsChanged() {
        if (programmaticChange) {
            return;
        }
        setControlStates();
        context.settingsChanged();
    }
    
    private void setControlStates() {
        int thickness = (Integer) spinnerThickness.getValue();
        spinnerEdgeWidth.setEnabled((thickness < -1 || thickness > 1) && (! radioButtonSheerEdge.isSelected()));
        radioButtonSheerEdge.setEnabled(thickness < -1 || thickness > 1);
        radioButtonLinearEdge.setEnabled(thickness < -1 || thickness > 1);
        radioButtonSmoothEdge.setEnabled(thickness < -1 || thickness > 1);
        radioButtonRoundedEdge.setEnabled(thickness < -1 || thickness > 1);
        comboBoxLayerAnchor.setEnabled((mixedMaterialSelector1.getMaterial() != null) && (mixedMaterialSelector1.getMaterial().getMode() == MixedMaterial.Mode.LAYERED));
    }
    
    private GroundCoverLayer saveSettings(GroundCoverLayer layer) {
        if (layer == null) {
            layer = new GroundCoverLayer(fieldName.getText(), mixedMaterialSelector1.getMaterial(), paintPicker1.getPaint());
        } else {
            layer.setName(fieldName.getText());
            layer.setPaint(paintPicker1.getPaint());
        }
        layer.setOpacity(paintPicker1.getOpacity());
        if (mixedMaterialSelector1.getMaterial().getMode() == MixedMaterial.Mode.LAYERED) {
            layer.setLayerAnchor(GroundCoverLayer.LayerAnchor.values()[comboBoxLayerAnchor.getSelectedIndex()]);
        }
        layer.setThickness((Integer) spinnerThickness.getValue());
        if (radioButtonSheerEdge.isSelected()) {
            layer.setEdgeShape(GroundCoverLayer.EdgeShape.SHEER);
        } else if (radioButtonLinearEdge.isSelected()) {
            layer.setEdgeShape(GroundCoverLayer.EdgeShape.LINEAR);
        } else if (radioButtonSmoothEdge.isSelected()) {
            layer.setEdgeShape(GroundCoverLayer.EdgeShape.SMOOTH);
        } else if (radioButtonRoundedEdge.isSelected()) {
            layer.setEdgeShape(GroundCoverLayer.EdgeShape.ROUNDED);
        }
        layer.setEdgeWidth((Integer) spinnerEdgeWidth.getValue());
        NoiseSettings noiseSettings = noiseSettingsEditor1.getNoiseSettings();
        if (noiseSettings.getRange() == 0) {
            layer.setNoiseSettings(null);
        } else {
            layer.setNoiseSettings(noiseSettings);
        }
        layer.setSmooth(checkBoxSmooth.isSelected());
        return layer;
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        buttonGroup1 = new javax.swing.ButtonGroup();
        radioButtonRoundedEdge = new javax.swing.JRadioButton();
        jLabel13 = new javax.swing.JLabel();
        noiseSettingsEditor1 = new org.pepsoft.worldpainter.NoiseSettingsEditor();
        jLabel1 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jLabel8 = new javax.swing.JLabel();
        jLabel14 = new javax.swing.JLabel();
        jLabel15 = new javax.swing.JLabel();
        checkBoxSmooth = new javax.swing.JCheckBox();
        labelWesterosCraftFeature = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        mixedMaterialSelector1 = new org.pepsoft.worldpainter.MixedMaterialChooser();
        fieldName = new javax.swing.JTextField();
        jLabel7 = new javax.swing.JLabel();
        spinnerThickness = new javax.swing.JSpinner();
        jLabel9 = new javax.swing.JLabel();
        jLabel10 = new javax.swing.JLabel();
        radioButtonSheerEdge = new javax.swing.JRadioButton();
        radioButtonLinearEdge = new javax.swing.JRadioButton();
        jLabel11 = new javax.swing.JLabel();
        radioButtonSmoothEdge = new javax.swing.JRadioButton();
        jLabel2 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jLabel12 = new javax.swing.JLabel();
        spinnerEdgeWidth = new javax.swing.JSpinner();
        comboBoxLayerAnchor = new javax.swing.JComboBox<>();
        jLabel16 = new javax.swing.JLabel();
        paintPicker1 = new org.pepsoft.worldpainter.layers.renderers.PaintPicker();

        buttonGroup1.add(radioButtonRoundedEdge);
        radioButtonRoundedEdge.setText("rounded");
        radioButtonRoundedEdge.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                radioButtonRoundedEdgeActionPerformed(evt);
            }
        });

        jLabel13.setText("Variation:");

        jLabel1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/pepsoft/worldpainter/icons/edge_sheer.png"))); // NOI18N

        jLabel3.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/pepsoft/worldpainter/icons/edge_linear.png"))); // NOI18N

        jLabel8.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/pepsoft/worldpainter/icons/edge_smooth.png"))); // NOI18N

        jLabel14.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/pepsoft/worldpainter/icons/edge_rounded.png"))); // NOI18N

        jLabel15.setText(" ");

        checkBoxSmooth.setText("Smooth:");
        checkBoxSmooth.setHorizontalTextPosition(javax.swing.SwingConstants.LEADING);

        labelWesterosCraftFeature.setFont(new java.awt.Font("Times New Roman", 1, 18)); // NOI18N
        labelWesterosCraftFeature.setText("W ");

        jLabel6.setText("Name:");

        fieldName.setColumns(15);

        jLabel7.setText("Thickness:");

        spinnerThickness.setModel(new javax.swing.SpinnerNumberModel(1, -255, 255, 1));
        spinnerThickness.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                spinnerThicknessStateChanged(evt);
            }
        });

        jLabel9.setText("(negative values will dig down into the terrain)");

        jLabel10.setText("Edge ");

        buttonGroup1.add(radioButtonSheerEdge);
        radioButtonSheerEdge.setSelected(true);
        radioButtonSheerEdge.setText("sheer");
        radioButtonSheerEdge.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                radioButtonSheerEdgeActionPerformed(evt);
            }
        });

        buttonGroup1.add(radioButtonLinearEdge);
        radioButtonLinearEdge.setText("linear");
        radioButtonLinearEdge.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                radioButtonLinearEdgeActionPerformed(evt);
            }
        });

        jLabel11.setText("shape:");

        buttonGroup1.add(radioButtonSmoothEdge);
        radioButtonSmoothEdge.setText("smooth");
        radioButtonSmoothEdge.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                radioButtonSmoothEdgeActionPerformed(evt);
            }
        });

        jLabel2.setText("Material:");

        jLabel4.setText("Paint:");

        jLabel12.setText("width:");

        spinnerEdgeWidth.setModel(new javax.swing.SpinnerNumberModel(1, 1, 255, 1));
        spinnerEdgeWidth.setEnabled(false);
        spinnerEdgeWidth.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                spinnerEdgeWidthStateChanged(evt);
            }
        });

        comboBoxLayerAnchor.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Bedrock", "Terrain", "Top of ground cover" }));
        comboBoxLayerAnchor.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                comboBoxLayerAnchorActionPerformed(evt);
            }
        });

        jLabel16.setText("Layers relative to:");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jLabel7)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(spinnerThickness, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel9))
            .addGroup(layout.createSequentialGroup()
                .addComponent(jLabel13)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(noiseSettingsEditor1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addGroup(layout.createSequentialGroup()
                .addComponent(jLabel10)
                .addGap(0, 0, 0)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel12)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(spinnerEdgeWidth, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel11)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(radioButtonSheerEdge)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(radioButtonLinearEdge)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel3)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(radioButtonSmoothEdge)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel8)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(radioButtonRoundedEdge)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel14))))
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel6)
                    .addComponent(jLabel4))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(paintPicker1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(fieldName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel15)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(checkBoxSmooth)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(labelWesterosCraftFeature))))
            .addGroup(layout.createSequentialGroup()
                .addComponent(jLabel2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel16)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(comboBoxLayerAnchor, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(mixedMaterialSelector1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(mixedMaterialSelector1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(comboBoxLayerAnchor, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel16))
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel7)
                    .addComponent(spinnerThickness, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel9))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel13)
                    .addComponent(noiseSettingsEditor1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel14)
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jLabel10)
                        .addComponent(radioButtonSheerEdge)
                        .addComponent(radioButtonLinearEdge)
                        .addComponent(jLabel11)
                        .addComponent(radioButtonSmoothEdge)
                        .addComponent(radioButtonRoundedEdge)
                        .addComponent(jLabel1)
                        .addComponent(jLabel3)
                        .addComponent(jLabel8)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel12)
                    .addComponent(spinnerEdgeWidth, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel6)
                    .addComponent(fieldName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(checkBoxSmooth)
                    .addComponent(labelWesterosCraftFeature))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel4)
                    .addComponent(jLabel15)
                    .addComponent(paintPicker1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void radioButtonRoundedEdgeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_radioButtonRoundedEdgeActionPerformed
        settingsChanged();
    }//GEN-LAST:event_radioButtonRoundedEdgeActionPerformed

    private void spinnerThicknessStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_spinnerThicknessStateChanged
        settingsChanged();
    }//GEN-LAST:event_spinnerThicknessStateChanged

    private void radioButtonSheerEdgeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_radioButtonSheerEdgeActionPerformed
        settingsChanged();
    }//GEN-LAST:event_radioButtonSheerEdgeActionPerformed

    private void radioButtonLinearEdgeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_radioButtonLinearEdgeActionPerformed
        settingsChanged();
    }//GEN-LAST:event_radioButtonLinearEdgeActionPerformed

    private void radioButtonSmoothEdgeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_radioButtonSmoothEdgeActionPerformed
        settingsChanged();
    }//GEN-LAST:event_radioButtonSmoothEdgeActionPerformed

    private void spinnerEdgeWidthStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_spinnerEdgeWidthStateChanged
        settingsChanged();
    }//GEN-LAST:event_spinnerEdgeWidthStateChanged

    private void comboBoxLayerAnchorActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_comboBoxLayerAnchorActionPerformed
        settingsChanged();
    }//GEN-LAST:event_comboBoxLayerAnchorActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.ButtonGroup buttonGroup1;
    private javax.swing.JCheckBox checkBoxSmooth;
    private javax.swing.JComboBox<String> comboBoxLayerAnchor;
    private javax.swing.JTextField fieldName;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel14;
    private javax.swing.JLabel jLabel15;
    private javax.swing.JLabel jLabel16;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JLabel labelWesterosCraftFeature;
    private org.pepsoft.worldpainter.MixedMaterialChooser mixedMaterialSelector1;
    private org.pepsoft.worldpainter.NoiseSettingsEditor noiseSettingsEditor1;
    private org.pepsoft.worldpainter.layers.renderers.PaintPicker paintPicker1;
    private javax.swing.JRadioButton radioButtonLinearEdge;
    private javax.swing.JRadioButton radioButtonRoundedEdge;
    private javax.swing.JRadioButton radioButtonSheerEdge;
    private javax.swing.JRadioButton radioButtonSmoothEdge;
    private javax.swing.JSpinner spinnerEdgeWidth;
    private javax.swing.JSpinner spinnerThickness;
    // End of variables declaration//GEN-END:variables

    private final Platform platform;
    private boolean programmaticChange = true;
}