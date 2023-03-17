/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.layers.pockets;

import org.pepsoft.worldpainter.*;
import org.pepsoft.worldpainter.layers.AbstractEditLayerDialog;
import org.pepsoft.worldpainter.themes.TerrainListCellRenderer;

import javax.swing.*;
import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

/**
 *
 * @author pepijn
 */
public class UndergroundPocketsDialog extends AbstractEditLayerDialog<UndergroundPocketsLayer> implements PropertyChangeListener {
    /**
     * Creates new form UndergroundPocketsDialog
     */
    public UndergroundPocketsDialog(Window parent, Platform platform, MixedMaterial material, ColourScheme colourScheme, int minHeight, int maxHeight, boolean extendedBlockIds) {
        this(parent, platform, material, null, colourScheme, minHeight, maxHeight, extendedBlockIds);
    }
    
    /**
     * Creates new form UndergroundPocketsDialog
     */
    public UndergroundPocketsDialog(Window parent, Platform platform, UndergroundPocketsLayer existingLayer, ColourScheme colourScheme, int minHeight, int maxHeight, boolean extendedBlockIds) {
        this(parent, platform, null, existingLayer, colourScheme, minHeight, maxHeight, extendedBlockIds);
    }
    
    /**
     * Creates new form UndergroundPocketsDialog
     */
    private UndergroundPocketsDialog(Window parent, Platform platform, MixedMaterial material, UndergroundPocketsLayer existingLayer, ColourScheme colourScheme, int minHeight, int maxHeight, boolean extendedBlockIds) {
        super(parent);
        this.colourScheme = colourScheme;
        
        initComponents();
        mixedMaterialChooser.setPlatform(platform);
        mixedMaterialChooser.setExtendedBlockIds(extendedBlockIds);
        mixedMaterialChooser.setColourScheme(colourScheme);
        mixedMaterialChooser.addPropertyChangeListener("material", this);
        
        if (existingLayer != null) {
            layer = existingLayer;
            fieldName.setText(existingLayer.getName());
            selectedColour = existingLayer.getColour();
            if (existingLayer.getMaterial() != null) {
                mixedMaterialChooser.setMaterial(existingLayer.getMaterial());
            } else {
                radioButtonTerrain.setSelected(true);
                comboBoxTerrain.setSelectedItem(existingLayer.getTerrain());
            }
            spinnerMinLevel.setValue(existingLayer.getMinLevel());
            spinnerMaxLevel.setValue(existingLayer.getMaxLevel());
            spinnerOccurrence.setValue(existingLayer.getFrequency());
            spinnerScale.setValue(existingLayer.getScale());
        } else {
            mixedMaterialChooser.setMaterial(material);
            spinnerMinLevel.setValue(minHeight);
            spinnerMaxLevel.setValue(maxHeight - 1);
        }
        ((SpinnerNumberModel) spinnerMinLevel.getModel()).setMinimum(minHeight);
        ((SpinnerNumberModel) spinnerMinLevel.getModel()).setMaximum(maxHeight - 1);
        ((SpinnerNumberModel) spinnerMaxLevel.getModel()).setMinimum(minHeight);
        ((SpinnerNumberModel) spinnerMaxLevel.getModel()).setMaximum(maxHeight - 1);
        spinnerOccurrence.setEditor(new JSpinner.NumberEditor(spinnerOccurrence, "0"));
        JSpinner.NumberEditor scaleEditor = new JSpinner.NumberEditor(spinnerScale, "0");
        scaleEditor.getTextField().setColumns(3);
        spinnerScale.setEditor(scaleEditor);
        spinnerMinLevel.setEditor(new JSpinner.NumberEditor(spinnerMinLevel, "0"));
        spinnerMaxLevel.setEditor(new JSpinner.NumberEditor(spinnerMaxLevel, "0"));
        
        setLabelColour();
        setControlStates();
        
        rootPane.setDefaultButton(buttonOK);
        scaleToUI();
        pack();
        scaleWindowToUI();
        setLocationRelativeTo(parent);
    }

    // AbstractEditLayerDialog

    @Override
    public UndergroundPocketsLayer getLayer() {
        return layer;
    }

    // PropertyChangeListener
    
    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getPropertyName().equals("material") && evt.getSource() == mixedMaterialChooser) {
            updateNameAndColour();
            setControlStates();
        }
    }

    @Override
    protected void ok() {
        String name = fieldName.getText();
        MixedMaterial material = radioButtonCustomMaterial.isSelected() ? mixedMaterialChooser.getMaterial() : null;
        if (material != null) {
            // Make sure the material is registered, in case it's new
            material = MixedMaterialManager.getInstance().register(material);
        }
        Terrain terrain = radioButtonTerrain.isSelected() ? (Terrain) comboBoxTerrain.getSelectedItem() : null;
        int occurrence = (Integer) spinnerOccurrence.getValue();
        int scale = (Integer) spinnerScale.getValue();
        int minLevel = (Integer) spinnerMinLevel.getValue();
        int maxLevel = (Integer) spinnerMaxLevel.getValue();
        if (layer == null) {
            layer = new UndergroundPocketsLayer(name, material, terrain, occurrence, minLevel, maxLevel, scale, selectedColour);
        } else {
            layer.setName(name);
            layer.setColour(selectedColour);
            layer.setMaterial(material);
            layer.setTerrain(terrain);
            layer.setFrequency(occurrence);
            layer.setMinLevel(minLevel);
            layer.setMaxLevel(maxLevel);
            layer.setScale(scale);
        }
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
    
    private void updateNameAndColour() {
        if (fieldName.isEnabled()) {
            if (radioButtonCustomMaterial.isSelected()) {
                MixedMaterial material = mixedMaterialChooser.getMaterial();
                if (material != null) {
                    fieldName.setText(material.toString());
                    if (material.getColour() != null) {
                        selectedColour = material.getColour();
                        setLabelColour();
                    }
                }
            } else {
                Terrain terrain = (Terrain) comboBoxTerrain.getSelectedItem();
                if (terrain != null) {
                    fieldName.setText(terrain.getName());
                }
            }
        }
    }
    
    private void setControlStates() {
        mixedMaterialChooser.setEnabled(radioButtonCustomMaterial.isSelected());
        comboBoxTerrain.setEnabled(radioButtonTerrain.isSelected());
        buttonOK.setEnabled((radioButtonCustomMaterial.isSelected() && (mixedMaterialChooser.getMaterial() != null))
            || (radioButtonTerrain.isSelected() && (comboBoxTerrain.getSelectedItem() != null)));
    }
    
    private void schedulePreviewUpdate() {
        if (previewUpdateTimer == null) {
            previewUpdateTimer = new Timer(250, e -> updatePreview());
            previewUpdateTimer.setRepeats(false);
        }
        previewUpdateTimer.restart();
    }
    
    private void updatePreview() {
        if (previewUpdateTimer != null) {
            previewUpdateTimer.stop(); // Superfluous?
            previewUpdateTimer = null;
        }
        MixedMaterial material = radioButtonCustomMaterial.isSelected() ? mixedMaterialChooser.getMaterial() : null;
        Terrain terrain = radioButtonTerrain.isSelected() ? (Terrain) comboBoxTerrain.getSelectedItem() : null;
        if ((material == null) && (terrain == null)) {
            return;
        }
        int occurrence = (Integer) spinnerOccurrence.getValue();
        int scale = (Integer) spinnerScale.getValue();
        int minLevel = (Integer) spinnerMinLevel.getValue();
        int maxLevel = (Integer) spinnerMaxLevel.getValue();
        UndergroundPocketsLayer tmpLayer = new UndergroundPocketsLayer("tmp", material, terrain, occurrence, minLevel, maxLevel, scale, 0);
        labelPreview.setIcon(new ImageIcon(UndergroundPocketsLayerExporter.createPreview(tmpLayer, labelPreview.getWidth(), labelPreview.getHeight())));
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
        jLabel1 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        buttonCancel = new javax.swing.JButton();
        buttonOK = new javax.swing.JButton();
        jLabel6 = new javax.swing.JLabel();
        fieldName = new javax.swing.JTextField();
        jLabel5 = new javax.swing.JLabel();
        buttonPickColour = new javax.swing.JButton();
        jLabel7 = new javax.swing.JLabel();
        spinnerOccurrence = new javax.swing.JSpinner();
        jLabel8 = new javax.swing.JLabel();
        jLabel9 = new javax.swing.JLabel();
        jLabel10 = new javax.swing.JLabel();
        spinnerScale = new javax.swing.JSpinner();
        spinnerMaxLevel = new javax.swing.JSpinner();
        jLabel11 = new javax.swing.JLabel();
        spinnerMinLevel = new javax.swing.JSpinner();
        jLabel12 = new javax.swing.JLabel();
        radioButtonCustomMaterial = new javax.swing.JRadioButton();
        radioButtonTerrain = new javax.swing.JRadioButton();
        comboBoxTerrain = new javax.swing.JComboBox();
        jPanel1 = new javax.swing.JPanel();
        labelPreview = new javax.swing.JLabel();
        mixedMaterialChooser = new org.pepsoft.worldpainter.MixedMaterialChooser();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Configure Underground Pockets Layer");
        addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentResized(java.awt.event.ComponentEvent evt) {
                formComponentResized(evt);
            }
        });

        jLabel1.setText("Select your custom material or terrain type:");

        jLabel4.setText("Colour:");

        buttonCancel.setText("Cancel");
        buttonCancel.addActionListener(this::buttonCancelActionPerformed);

        buttonOK.setText("OK");
        buttonOK.addActionListener(this::buttonOKActionPerformed);

        jLabel6.setText("Name:");

        fieldName.setColumns(10);

        jLabel5.setBackground(java.awt.Color.orange);
        jLabel5.setText("                 ");
        jLabel5.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        jLabel5.setOpaque(true);

        buttonPickColour.setText("...");
        buttonPickColour.addActionListener(this::buttonPickColourActionPerformed);

        jLabel7.setText("Occurrence:");

        spinnerOccurrence.setModel(new javax.swing.SpinnerNumberModel(10, 1, 1000, 1));
        spinnerOccurrence.addChangeListener(this::spinnerOccurrenceStateChanged);

        jLabel8.setText("Scale:");

        jLabel9.setText("Levels:");

        jLabel10.setText("‰");

        spinnerScale.setModel(new javax.swing.SpinnerNumberModel(Integer.valueOf(100), Integer.valueOf(1), null, Integer.valueOf(1)));
        spinnerScale.addChangeListener(this::spinnerScaleStateChanged);

        spinnerMaxLevel.setModel(new javax.swing.SpinnerNumberModel(255, 0, 255, 1));
        spinnerMaxLevel.addChangeListener(this::spinnerMaxLevelStateChanged);

        jLabel11.setText("%");

        spinnerMinLevel.setModel(new javax.swing.SpinnerNumberModel(0, 0, 255, 1));
        spinnerMinLevel.addChangeListener(this::spinnerMinLevelStateChanged);

        jLabel12.setText("-");

        buttonGroup1.add(radioButtonCustomMaterial);
        radioButtonCustomMaterial.setSelected(true);
        radioButtonCustomMaterial.setText("custom material:");
        radioButtonCustomMaterial.addActionListener(this::radioButtonCustomMaterialActionPerformed);

        buttonGroup1.add(radioButtonTerrain);
        radioButtonTerrain.setText("terrain:");
        radioButtonTerrain.addActionListener(this::radioButtonTerrainActionPerformed);

        comboBoxTerrain.setModel(new DefaultComboBoxModel(Terrain.VALUES));
        comboBoxTerrain.setEnabled(false);
        comboBoxTerrain.setRenderer(new TerrainListCellRenderer(colourScheme));
        comboBoxTerrain.addActionListener(this::comboBoxTerrainActionPerformed);

        jPanel1.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.LOWERED));

        labelPreview.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(labelPreview, javax.swing.GroupLayout.DEFAULT_SIZE, 128, Short.MAX_VALUE)
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(labelPreview, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(buttonOK)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(buttonCancel)
                        .addGap(11, 11, 11))
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel1)
                            .addGroup(layout.createSequentialGroup()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jLabel4)
                                    .addComponent(jLabel7)
                                    .addComponent(jLabel6)
                                    .addComponent(jLabel8)
                                    .addComponent(jLabel9))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(layout.createSequentialGroup()
                                        .addComponent(spinnerMinLevel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addGap(0, 0, 0)
                                        .addComponent(jLabel12)
                                        .addGap(0, 0, 0)
                                        .addComponent(spinnerMaxLevel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                    .addGroup(layout.createSequentialGroup()
                                        .addComponent(spinnerScale, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addGap(0, 0, 0)
                                        .addComponent(jLabel11))
                                    .addComponent(fieldName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addGroup(layout.createSequentialGroup()
                                        .addComponent(spinnerOccurrence, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addGap(0, 0, 0)
                                        .addComponent(jLabel10))
                                    .addGroup(layout.createSequentialGroup()
                                        .addComponent(jLabel5)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(buttonPickColour))))
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(radioButtonTerrain)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(comboBoxTerrain, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(radioButtonCustomMaterial)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(mixedMaterialChooser, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addContainerGap())))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(radioButtonCustomMaterial)
                            .addComponent(mixedMaterialChooser, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(radioButtonTerrain)
                            .addComponent(comboBoxTerrain, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(18, 18, 18)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel6)
                            .addComponent(fieldName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(19, 19, 19)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel4)
                            .addComponent(jLabel5)
                            .addComponent(buttonPickColour))
                        .addGap(18, 18, 18)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel7)
                            .addComponent(spinnerOccurrence, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel10))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel8)
                            .addComponent(spinnerScale, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel11))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel9)
                            .addComponent(spinnerMaxLevel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(spinnerMinLevel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel12)))
                    .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGap(18, 18, 18)
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

    private void buttonPickColourActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonPickColourActionPerformed
        pickColour();
    }//GEN-LAST:event_buttonPickColourActionPerformed

    private void spinnerMinLevelStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_spinnerMinLevelStateChanged
        int newMinValue = (Integer) spinnerMinLevel.getValue();
        int currentMaxValue = (Integer) spinnerMaxLevel.getValue();
        if (newMinValue > currentMaxValue) {
            spinnerMaxLevel.setValue(newMinValue);
        }
    }//GEN-LAST:event_spinnerMinLevelStateChanged

    private void spinnerMaxLevelStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_spinnerMaxLevelStateChanged
        int newMaxValue = (Integer) spinnerMaxLevel.getValue();
        int currentMinValue = (Integer) spinnerMinLevel.getValue();
        if (newMaxValue < currentMinValue) {
            spinnerMinLevel.setValue(newMaxValue);
        }
    }//GEN-LAST:event_spinnerMaxLevelStateChanged

    private void radioButtonCustomMaterialActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_radioButtonCustomMaterialActionPerformed
        updateNameAndColour();
        setControlStates();
    }//GEN-LAST:event_radioButtonCustomMaterialActionPerformed

    private void radioButtonTerrainActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_radioButtonTerrainActionPerformed
        updateNameAndColour();
        setControlStates();
    }//GEN-LAST:event_radioButtonTerrainActionPerformed

    private void comboBoxTerrainActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_comboBoxTerrainActionPerformed
        updateNameAndColour();
        setControlStates();
    }//GEN-LAST:event_comboBoxTerrainActionPerformed

    private void formComponentResized(java.awt.event.ComponentEvent evt) {//GEN-FIRST:event_formComponentResized
        schedulePreviewUpdate();
    }//GEN-LAST:event_formComponentResized

    private void spinnerOccurrenceStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_spinnerOccurrenceStateChanged
        schedulePreviewUpdate();
    }//GEN-LAST:event_spinnerOccurrenceStateChanged

    private void spinnerScaleStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_spinnerScaleStateChanged
        schedulePreviewUpdate();
    }//GEN-LAST:event_spinnerScaleStateChanged

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton buttonCancel;
    private javax.swing.ButtonGroup buttonGroup1;
    private javax.swing.JButton buttonOK;
    private javax.swing.JButton buttonPickColour;
    private javax.swing.JComboBox comboBoxTerrain;
    private javax.swing.JTextField fieldName;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JLabel labelPreview;
    private org.pepsoft.worldpainter.MixedMaterialChooser mixedMaterialChooser;
    private javax.swing.JRadioButton radioButtonCustomMaterial;
    private javax.swing.JRadioButton radioButtonTerrain;
    private javax.swing.JSpinner spinnerMaxLevel;
    private javax.swing.JSpinner spinnerMinLevel;
    private javax.swing.JSpinner spinnerOccurrence;
    private javax.swing.JSpinner spinnerScale;
    // End of variables declaration//GEN-END:variables
    
    private final ColourScheme colourScheme;
    private UndergroundPocketsLayer layer;
    private int selectedColour = Color.ORANGE.getRGB();
    private Timer previewUpdateTimer;

    private static final long serialVersionUID = 1L;
}