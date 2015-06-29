/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.worldpainter.layers.plants;

import org.pepsoft.util.Version;
import org.pepsoft.worldpainter.biomeschemes.BiomeSchemeManager;
import org.pepsoft.worldpainter.layers.CustomLayerDialog;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.SortedMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.pepsoft.worldpainter.util.I18nHelper.m;

/**
 *
 * @author pepijn
 */
public class PlantDialog extends CustomLayerDialog<PlantLayer> {
    /**
     * Creates new form PlantDialog
     */
    public PlantDialog(Window parent, PlantLayer layer) {
        super(parent);
        this.layer = layer;
        
        initComponents();
        initPlantControls();
        setLocationRelativeTo(parent);

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
        
        loadSettings();
        setLabelColour();
        setControlStates();
        
        getRootPane().setDefaultButton(buttonOK);

        pack();
        setLocationRelativeTo(parent); // super() has already done so, but our size has changed so redo it
    }

    // CustomLayerDialog
    
    @Override
    public PlantLayer getSelectedLayer() {
        return layer;
    }

    private void initPlantControls() {
        panelPlantControls.setLayout(new GridBagLayout());
        panelPlantControls2.setLayout(new GridBagLayout());
        JPanel panel = panelPlantControls;
        Plant.Category category = Plant.ALL_PLANTS[0].getCategory();
        startNewCategory(panel, category);
        for (int i = 0; i < Plant.ALL_PLANTS.length; i++) {
            Plant plant = Plant.ALL_PLANTS[i];
            if (plant.getCategory() != category) {
                category = plant.getCategory();
                panel = panelPlantControls2;
                startNewCategory(panel, category);
            }
            addPlantRow(panel, plant, i);
        }
    }
    
    private void startNewCategory(JPanel panel, Plant.Category category) {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.anchor = GridBagConstraints.BASELINE_LEADING;
        constraints.insets = new Insets(4, 0, 4, 0);
        panel.add(new JLabel("<html><b>" + m(category) + "</b></html>"), constraints);
    }
    
    private void addPlantRow(final JPanel panel, final Plant plant, final int index) {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.anchor = GridBagConstraints.BASELINE_LEADING;
        constraints.insets = new Insets(1, 0, 1, 4);
        BufferedImage icon = findIcon(plant.getIconName());
        if (icon != null) {
            plantLabels[index] = new JLabel(plant.getName(), new ImageIcon(icon), JLabel.TRAILING);
        } else {
            plantLabels[index] = new JLabel(plant.getName());
        }
        panel.add(plantLabels[index], constraints);

        SpinnerModel spinnerModel = new SpinnerNumberModel(0, 0, Integer.MAX_VALUE, 1);
        spinners[index] = new JSpinner(spinnerModel);
        ((JSpinner.NumberEditor) spinners[index].getEditor()).getTextField().setColumns(3);
        spinners[index].addChangeListener(percentageListener);
        panel.add(spinners[index], constraints);
        
        percentageLabels[index] = new JLabel("100%");
        panel.add(percentageLabels[index], constraints);

        if (plant.getMaxData() > 0) {
            panel.add(new JLabel("Growth:"), constraints);
            
            spinnerModel = new SpinnerNumberModel(plant.getMaxData() + 1, 1, plant.getMaxData() + 1, 1);
            growthFromSpinners[index] = new JSpinner(spinnerModel);
            growthFromSpinners[index].addChangeListener(e -> {
                int newValue = (Integer) growthFromSpinners[index].getValue();
                if ((Integer) growthToSpinners[index].getValue() < newValue) {
                    growthToSpinners[index].setValue(newValue);
                }
            });
            panel.add(growthFromSpinners[index], constraints);
            
            panel.add(new JLabel("-"));

            constraints.gridwidth = GridBagConstraints.REMAINDER;
            spinnerModel = new SpinnerNumberModel(plant.getMaxData() + 1, 1, plant.getMaxData() + 1, 1);
            growthToSpinners[index] = new JSpinner(spinnerModel);
            growthToSpinners[index].addChangeListener(e -> {
                int newValue = (Integer) growthToSpinners[index].getValue();
                if ((Integer) growthFromSpinners[index].getValue() > newValue) {
                    growthFromSpinners[index].setValue(newValue);
                }
            });
            panel.add(growthToSpinners[index], constraints);
        } else {
            constraints.gridwidth = GridBagConstraints.REMAINDER;
            panel.add(new JLabel(), constraints);
        }
    }

    private static BufferedImage findIcon(String name) {
        if (resourcesJar == RESOURCES_NOT_AVAILABLE) {
            return null;
        }
        try {
            if (resourcesJar == null) {
                SortedMap<Version, File> jars = BiomeSchemeManager.getAllMinecraftJars();
                if (! jars.isEmpty()) {
                    resourcesJar = jars.get(jars.lastKey());
                } else {
                    logger.warning("Could not find Minecraft jar for loading plant icons");
                    resourcesJar = RESOURCES_NOT_AVAILABLE;
                    return null;
                }
            }
            try (JarFile jarFile = new JarFile(resourcesJar)) {
                JarEntry entry = jarFile.getJarEntry("assets/minecraft/textures/" + name);
                if (entry != null) {
                    if (logger.isLoggable(Level.FINE)) {
                        logger.fine("Loading plant icon " + name + " from " + resourcesJar);
                    }
                    try (InputStream in = jarFile.getInputStream(entry)) {
                        return ImageIO.read(in);
                    }
                } else {
                    if (logger.isLoggable(Level.FINE)) {
                        logger.fine("Could not find plant icon " + name + " in Minecraft jar " + resourcesJar);
                    }
                    return null;
                }
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "I/O error while trying to load plant icon " + name + "; continuing without icon", e);
            resourcesJar = RESOURCES_NOT_AVAILABLE;
            return null;
        }
    }

    private void updatePercentages() {
        totalOccurrence = 0;
        for (JSpinner spinner: spinners) {
            totalOccurrence += (Integer) spinner.getValue();
        }
        if (normalFont == null) {
            normalFont = plantLabels[0].getFont().deriveFont(Font.PLAIN);
            boldFont = normalFont.deriveFont(Font.BOLD);
        }
        cropsSelected = false;
        for (int i = 0; i < spinners.length; i++) {
            int value = (Integer) spinners[i].getValue();
            if ((value == 0) && percentageLabels[i].isEnabled()) {
                percentageLabels[i].setEnabled(false);
                plantLabels[i].setFont(normalFont);
                percentageLabels[i].setText("   %");
                if ((growthFromSpinners[i] != null) && growthFromSpinners[i].isEnabled()) {
                    growthFromSpinners[i].setEnabled(false);
                    growthToSpinners[i].setEnabled(false);
                }
            } else if (value > 0) {
                if (! percentageLabels[i].isEnabled()) {
                    percentageLabels[i].setEnabled(true);
                    plantLabels[i].setFont(boldFont);
                }
                if (Plant.ALL_PLANTS[i].getCategory() == Plant.Category.CROPS) {
                    cropsSelected = true;
                }
                int percentage = value * 100 / totalOccurrence;
                if (percentage < 10) {
                    percentageLabels[i].setText("  " + percentage + "%");
                } else if (percentage < 100) {
                    percentageLabels[i].setText(" " + percentage + "%");
                } else {
                    percentageLabels[i].setText(percentage + "%");
                }
                if ((growthFromSpinners[i] != null) && (! growthFromSpinners[i].isEnabled())) {
                    growthFromSpinners[i].setEnabled(true);
                    growthToSpinners[i].setEnabled(true);
                }
            }
        }
        setControlStates();
    }

    private void setLabelColour() {
        labelColour.setBackground(new Color(selectedColour));
    }

    private void setControlStates() {
        checkBoxGenerateTilledDirt.setEnabled(cropsSelected);
        buttonOK.setEnabled((! fieldName.getText().trim().isEmpty()) && (totalOccurrence > 0));
    }

    private void pickColour() {
        Color pick = JColorChooser.showDialog(this, "Select Colour", new Color(selectedColour));
        if (pick != null) {
            selectedColour = pick.getRGB();
            setLabelColour();
        }
    }
    
    private void loadSettings() {
        fieldName.setText(layer.getName());
        selectedColour = layer.getColour();
        checkBoxGenerateTilledDirt.setSelected(layer.isGenerateTilledDirt());
        for (int i = 0; i < Plant.ALL_PLANTS.length; i++) {
            PlantLayer.PlantSettings settings = layer.getSettings(i);
            if (settings != null) {
                spinners[i].setValue((int) settings.occurrence);
                if (growthFromSpinners[i] != null) {
                    growthFromSpinners[i].setValue(settings.dataValueFrom + 1);
                    growthToSpinners[i].setValue(settings.dataValueTo + 1);
                }
            } else {
                spinners[i].setValue(0);
                if (growthFromSpinners[i] != null) {
                    growthFromSpinners[i].setValue(Plant.ALL_PLANTS[i].getMaxData() + 1);
                    growthToSpinners[i].setValue(Plant.ALL_PLANTS[i].getMaxData() + 1);
                }
            }
        }
        updatePercentages();
    }
    
    private void saveSettings() {
        layer.setName(fieldName.getText().trim());
        layer.setColour(selectedColour);
        layer.setGenerateTilledDirt(checkBoxGenerateTilledDirt.isSelected());
        for (int i = 0; i < Plant.ALL_PLANTS.length; i++) {
            PlantLayer.PlantSettings settings = new PlantLayer.PlantSettings();
            settings.occurrence = (short) ((int) ((Integer) spinners[i].getValue()));
            if (growthFromSpinners[i] != null) {
                settings.dataValueFrom = (byte) ((Integer) growthFromSpinners[i].getValue() - 1);
                settings.dataValueTo = (byte) ((Integer) growthToSpinners[i].getValue() - 1);
            } else {
                settings.dataValueFrom = 0;
                settings.dataValueTo = 0;
            }
            layer.setSettings(i, settings);
        }
    }
    
    private void clear() {
        for (int i = 0; i < Plant.ALL_PLANTS.length; i++) {
            spinners[i].setValue(0);
            if (growthFromSpinners[i] != null) {
                growthFromSpinners[i].setValue(Plant.ALL_PLANTS[i].getMaxData() + 1);
                growthToSpinners[i].setValue(Plant.ALL_PLANTS[i].getMaxData() + 1);
            }
        }
        updatePercentages();
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
        fieldName = new javax.swing.JTextField();
        jLabel2 = new javax.swing.JLabel();
        labelColour = new javax.swing.JLabel();
        buttonColour = new javax.swing.JButton();
        panelPlantControls = new javax.swing.JPanel();
        buttonCancel = new javax.swing.JButton();
        buttonOK = new javax.swing.JButton();
        panelPlantControls2 = new javax.swing.JPanel();
        jLabel3 = new javax.swing.JLabel();
        buttonClear = new javax.swing.JButton();
        buttonReset = new javax.swing.JButton();
        checkBoxGenerateTilledDirt = new javax.swing.JCheckBox();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Configure Custom Plants Layer");

        jLabel1.setText("Name:");

        fieldName.setColumns(20);
        fieldName.setText("jTextField1");

        jLabel2.setText("Colour:");

        labelColour.setText("                 ");
        labelColour.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        labelColour.setOpaque(true);

        buttonColour.setText("...");
        buttonColour.addActionListener(this::buttonColourActionPerformed);

        javax.swing.GroupLayout panelPlantControlsLayout = new javax.swing.GroupLayout(panelPlantControls);
        panelPlantControls.setLayout(panelPlantControlsLayout);
        panelPlantControlsLayout.setHorizontalGroup(
            panelPlantControlsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 225, Short.MAX_VALUE)
        );
        panelPlantControlsLayout.setVerticalGroup(
            panelPlantControlsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 47, Short.MAX_VALUE)
        );

        buttonCancel.setText("Cancel");
        buttonCancel.addActionListener(this::buttonCancelActionPerformed);

        buttonOK.setText("OK");
        buttonOK.addActionListener(this::buttonOKActionPerformed);

        javax.swing.GroupLayout panelPlantControls2Layout = new javax.swing.GroupLayout(panelPlantControls2);
        panelPlantControls2.setLayout(panelPlantControls2Layout);
        panelPlantControls2Layout.setHorizontalGroup(
            panelPlantControls2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 0, Short.MAX_VALUE)
        );
        panelPlantControls2Layout.setVerticalGroup(
            panelPlantControls2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 0, Short.MAX_VALUE)
        );

        jLabel3.setText("<html>Note that plants will only be placed<br>where Minecraft allows it!</html>");

        buttonClear.setText("Clear");
        buttonClear.addActionListener(this::buttonClearActionPerformed);

        buttonReset.setText("Reset");
        buttonReset.addActionListener(this::buttonResetActionPerformed);

        checkBoxGenerateTilledDirt.setSelected(true);
        checkBoxGenerateTilledDirt.setText("turn grass and dirt beneath crops to tilled dirt");
        checkBoxGenerateTilledDirt.setEnabled(false);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(checkBoxGenerateTilledDirt)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(buttonReset)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(buttonClear)
                        .addGap(18, 18, 18)
                        .addComponent(buttonOK)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(buttonCancel)
                        .addContainerGap())
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(panelPlantControls, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(panelPlantControls2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jLabel1)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(fieldName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(18, 18, 18)
                                .addComponent(jLabel2)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(labelColour)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(buttonColour)
                                .addGap(0, 0, Short.MAX_VALUE)))
                        .addGap(12, 12, 12))))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(buttonCancel)
                            .addComponent(buttonOK)
                            .addComponent(buttonClear)
                            .addComponent(buttonReset)))
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel1)
                            .addComponent(fieldName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel2)
                            .addComponent(labelColour)
                            .addComponent(buttonColour))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(checkBoxGenerateTilledDirt)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(panelPlantControls2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(panelPlantControls, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(18, 18, 18)
                        .addComponent(jLabel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void buttonOKActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonOKActionPerformed
        saveSettings();
        ok();
    }//GEN-LAST:event_buttonOKActionPerformed

    private void buttonCancelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonCancelActionPerformed
        cancel();
    }//GEN-LAST:event_buttonCancelActionPerformed

    private void buttonColourActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonColourActionPerformed
        pickColour();
    }//GEN-LAST:event_buttonColourActionPerformed

    private void buttonResetActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonResetActionPerformed
        loadSettings();
    }//GEN-LAST:event_buttonResetActionPerformed

    private void buttonClearActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonClearActionPerformed
        clear();
    }//GEN-LAST:event_buttonClearActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton buttonCancel;
    private javax.swing.JButton buttonClear;
    private javax.swing.JButton buttonColour;
    private javax.swing.JButton buttonOK;
    private javax.swing.JButton buttonReset;
    private javax.swing.JCheckBox checkBoxGenerateTilledDirt;
    private javax.swing.JTextField fieldName;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel labelColour;
    private javax.swing.JPanel panelPlantControls;
    private javax.swing.JPanel panelPlantControls2;
    // End of variables declaration//GEN-END:variables

    private final PlantLayer layer;
    private final JSpinner[] spinners = new JSpinner[Plant.ALL_PLANTS.length];
    private final JLabel[] plantLabels = new JLabel[Plant.ALL_PLANTS.length], percentageLabels = new JLabel[Plant.ALL_PLANTS.length];
    private final JSpinner[] growthFromSpinners = new JSpinner[Plant.ALL_PLANTS.length], growthToSpinners = new JSpinner[Plant.ALL_PLANTS.length];
    private int selectedColour = Color.ORANGE.getRGB(), totalOccurrence;
    private boolean cropsSelected;
    private Font normalFont, boldFont;

    private final ChangeListener percentageListener = e -> updatePercentages();

    private static File resourcesJar;
    private static final File RESOURCES_NOT_AVAILABLE = new File("~~~RESOURCES_NOT_AVAILABLE~~~");
    private static final Logger logger = Logger.getLogger(PlantDialog.class.getName());
}