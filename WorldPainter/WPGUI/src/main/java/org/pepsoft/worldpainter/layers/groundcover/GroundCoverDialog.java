/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/*
 * GroundCoverDialog.java
 *
 * Created on Apr 22, 2012, 9:01:10 PM
 */
package org.pepsoft.worldpainter.layers.groundcover;

import org.pepsoft.worldpainter.ColourScheme;
import org.pepsoft.worldpainter.MixedMaterial;
import org.pepsoft.worldpainter.MixedMaterialManager;
import org.pepsoft.worldpainter.NoiseSettings;
import org.pepsoft.worldpainter.layers.CustomLayerDialog;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;

/**
 *
 * @author pepijn
 */
public class GroundCoverDialog extends CustomLayerDialog<GroundCoverLayer> {
    public GroundCoverDialog(Window parent, MixedMaterial material, ColourScheme colourScheme, boolean extendedBlockIds) {
        this(parent, material, null, colourScheme, extendedBlockIds);
    }
    
    public GroundCoverDialog(Window parent, GroundCoverLayer existingLayer, ColourScheme colourScheme, boolean extendedBlockIds) {
        this(parent, null, existingLayer, colourScheme, extendedBlockIds);
    }
    
    public GroundCoverDialog(Window parent, MixedMaterial material, GroundCoverLayer existingLayer, ColourScheme colourScheme, boolean extendedBlockIds) {
        super(parent);

        initComponents();
        mixedMaterialSelector1.setColourScheme(colourScheme);
        mixedMaterialSelector1.setExtendedBlockIds(extendedBlockIds);
        
        if (! "true".equalsIgnoreCase(System.getProperty("org.pepsoft.worldpainter.westerosCraftMode"))) {
            checkBoxSmooth.setVisible(false);
            labelWesterosCraftFeature.setVisible(false);
        }
        
        if (existingLayer != null) {
            layer = existingLayer;
            fieldName.setText(existingLayer.getName());
            spinnerThickness.setValue(existingLayer.getThickness());
            selectedColour = existingLayer.getColour();
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
        } else {
            fieldName.setText(material.getName());
            if (material.getColour() != null) {
                selectedColour = material.getColour();
            }
            mixedMaterialSelector1.setMaterial(material);
        }
        
        setLabelColour();
        setControlStates();
        
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
        
        rootPane.setDefaultButton(buttonOK);
        pack();
        setLocationRelativeTo(parent);
    }
    
    @Override
    public GroundCoverLayer getSelectedLayer() {
        return layer;
    }
    
    @Override
    protected void ok() {
        MixedMaterial material = mixedMaterialSelector1.getMaterial();
        // Make sure the material is registered, in case it's new
        material = MixedMaterialManager.getInstance().register(material);
        if (layer == null) {
            layer = new GroundCoverLayer(fieldName.getText(), material, selectedColour);
        } else {
            layer.setName(fieldName.getText());
            layer.setMaterial(material);
            layer.setColour(selectedColour);
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
        super.ok();
    }
    
    private void pickColour() {
        Color pick = JColorChooser.showDialog(this, "Select Colour", new Color(selectedColour));
        if (pick != null) {
            selectedColour = pick.getRGB();
            setLabelColour();
        }
    }
    
    private void setLabelColour() {
        jLabel5.setBackground(new Color(selectedColour));
    }
    
    private void setControlStates() {
        int thickness = (Integer) spinnerThickness.getValue();
        spinnerEdgeWidth.setEnabled((thickness < -1 || thickness > 1) && (! radioButtonSheerEdge.isSelected()));
        radioButtonSheerEdge.setEnabled(thickness < -1 || thickness > 1);
        radioButtonLinearEdge.setEnabled(thickness < -1 || thickness > 1);
        radioButtonSmoothEdge.setEnabled(thickness < -1 || thickness > 1);
        radioButtonRoundedEdge.setEnabled(thickness < -1 || thickness > 1);
        buttonOK.setEnabled((thickness != 0) && (! fieldName.getText().trim().isEmpty()));
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
        jLabel2 = new javax.swing.JLabel();
        buttonOK = new javax.swing.JButton();
        buttonCancel = new javax.swing.JButton();
        jLabel4 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        jButton1 = new javax.swing.JButton();
        jLabel6 = new javax.swing.JLabel();
        fieldName = new javax.swing.JTextField();
        jLabel7 = new javax.swing.JLabel();
        spinnerThickness = new javax.swing.JSpinner();
        jLabel9 = new javax.swing.JLabel();
        jLabel10 = new javax.swing.JLabel();
        radioButtonSheerEdge = new javax.swing.JRadioButton();
        radioButtonLinearEdge = new javax.swing.JRadioButton();
        jLabel11 = new javax.swing.JLabel();
        radioButtonSmoothEdge = new javax.swing.JRadioButton();
        jLabel12 = new javax.swing.JLabel();
        spinnerEdgeWidth = new javax.swing.JSpinner();
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
        mixedMaterialSelector1 = new org.pepsoft.worldpainter.MixedMaterialChooser();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Define Custom Ground Cover Layer");
        setResizable(false);

        jLabel2.setText("Material:");

        buttonOK.setText("OK");
        buttonOK.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonOKActionPerformed(evt);
            }
        });

        buttonCancel.setText("Cancel");
        buttonCancel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonCancelActionPerformed(evt);
            }
        });

        jLabel4.setText("Colour:");

        jLabel5.setBackground(java.awt.Color.orange);
        jLabel5.setText("                 ");
        jLabel5.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        jLabel5.setOpaque(true);

        jButton1.setText("...");
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });

        jLabel6.setText("Name:");

        fieldName.setColumns(10);

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

        jLabel12.setText("width:");

        spinnerEdgeWidth.setModel(new javax.swing.SpinnerNumberModel(1, 1, 255, 1));
        spinnerEdgeWidth.setEnabled(false);

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
                                .addComponent(jLabel2)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(mixedMaterialSelector1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
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
                                    .addGroup(layout.createSequentialGroup()
                                        .addComponent(jLabel4)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(jLabel5)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(jButton1))
                                    .addGroup(layout.createSequentialGroup()
                                        .addComponent(jLabel6)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(fieldName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                                .addGap(18, 18, 18)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jLabel15)
                                    .addGroup(layout.createSequentialGroup()
                                        .addComponent(checkBoxSmooth)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(labelWesterosCraftFeature)))))
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(buttonOK)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(buttonCancel)
                        .addGap(11, 11, 11))))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(mixedMaterialSelector1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
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
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel4)
                    .addComponent(jLabel5)
                    .addComponent(jButton1)
                    .addComponent(jLabel15))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 6, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(buttonCancel)
                    .addComponent(buttonOK))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void buttonCancelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonCancelActionPerformed
        cancel();
    }//GEN-LAST:event_buttonCancelActionPerformed

    private void buttonOKActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonOKActionPerformed
        ok();
    }//GEN-LAST:event_buttonOKActionPerformed

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        pickColour();
    }//GEN-LAST:event_jButton1ActionPerformed

    private void spinnerThicknessStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_spinnerThicknessStateChanged
        setControlStates();
    }//GEN-LAST:event_spinnerThicknessStateChanged

    private void radioButtonSheerEdgeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_radioButtonSheerEdgeActionPerformed
        setControlStates();
    }//GEN-LAST:event_radioButtonSheerEdgeActionPerformed

    private void radioButtonLinearEdgeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_radioButtonLinearEdgeActionPerformed
        setControlStates();
    }//GEN-LAST:event_radioButtonLinearEdgeActionPerformed

    private void radioButtonSmoothEdgeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_radioButtonSmoothEdgeActionPerformed
        setControlStates();
    }//GEN-LAST:event_radioButtonSmoothEdgeActionPerformed

    private void radioButtonRoundedEdgeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_radioButtonRoundedEdgeActionPerformed
        setControlStates();
    }//GEN-LAST:event_radioButtonRoundedEdgeActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton buttonCancel;
    private javax.swing.ButtonGroup buttonGroup1;
    private javax.swing.JButton buttonOK;
    private javax.swing.JCheckBox checkBoxSmooth;
    private javax.swing.JTextField fieldName;
    private javax.swing.JButton jButton1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel14;
    private javax.swing.JLabel jLabel15;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JLabel labelWesterosCraftFeature;
    private org.pepsoft.worldpainter.MixedMaterialChooser mixedMaterialSelector1;
    private org.pepsoft.worldpainter.NoiseSettingsEditor noiseSettingsEditor1;
    private javax.swing.JRadioButton radioButtonLinearEdge;
    private javax.swing.JRadioButton radioButtonRoundedEdge;
    private javax.swing.JRadioButton radioButtonSheerEdge;
    private javax.swing.JRadioButton radioButtonSmoothEdge;
    private javax.swing.JSpinner spinnerEdgeWidth;
    private javax.swing.JSpinner spinnerThickness;
    // End of variables declaration//GEN-END:variables
    
    private GroundCoverLayer layer;
    private int selectedColour = Color.RED.getRGB();

    private static final long serialVersionUID = 1L;
}
