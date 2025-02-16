/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.superflat;

import org.pepsoft.minecraft.SuperflatPreset;
import org.pepsoft.minecraft.SuperflatPreset.Layer;
import org.pepsoft.worldpainter.DefaultPlugin;
import org.pepsoft.worldpainter.Platform;
import org.pepsoft.worldpainter.WorldPainterDialog;
import org.pepsoft.worldpainter.biomeschemes.AbstractMinecraft1_1BiomeScheme;
import org.pepsoft.worldpainter.biomeschemes.Minecraft1_17Biomes;
import org.pepsoft.worldpainter.biomeschemes.Minecraft1_7Biomes;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.IntStream;

import static org.pepsoft.minecraft.Constants.MC_DIRT;
import static org.pepsoft.minecraft.Constants.MC_PLAINS;
import static org.pepsoft.worldpainter.DefaultPlugin.*;
import static org.pepsoft.worldpainter.Platform.Capability.NAMED_BIOMES;
import static org.pepsoft.worldpainter.biomeschemes.Minecraft1_21Biomes.BIOMES_BY_MODERN_ID;
import static org.pepsoft.worldpainter.biomeschemes.Minecraft1_21Biomes.MODERN_IDS;
import static org.pepsoft.worldpainter.biomeschemes.Minecraft1_7Biomes.BIOME_PLAINS;

/**
 *
 * @author Pepijn
 */
@SuppressWarnings({"unchecked", "rawtypes", "ConstantConditions", "Convert2Lambda", "Anonymous2MethodRef", "unused", "FieldCanBeLocal"}) // Managed by NetBeans
// Managed by NetBeans
public class EditSuperflatPresetDialog extends WorldPainterDialog {
    /**
     * Creates new form EditSuperflatPresetDialog
     */
    public EditSuperflatPresetDialog(Window parent, Platform platform, SuperflatPreset superflatPreset) {
        super(parent);
        this.platform = platform;
        this.superflatPreset = superflatPreset;

        initComponents();
        tableLayers.getSelectionModel().addListSelectionListener(e -> setControlStates());

        if (platform.capabilities.contains(NAMED_BIOMES)) {
            // TODO move available biomes to Platform
            final String[] availableBiomes = Arrays.stream(MODERN_IDS).filter(Objects::nonNull).distinct().sorted().toArray(String[]::new);
            comboBoxBiome.setModel(new DefaultComboBoxModel<>(availableBiomes));
            comboBoxBiome.setEditable(true);
            if (superflatPreset.getBiomeName() != null) {
                comboBoxBiome.setSelectedItem(superflatPreset.getBiomeName());
            } else if (MODERN_IDS[superflatPreset.getBiome()] != null) {
                comboBoxBiome.setSelectedItem(MODERN_IDS[superflatPreset.getBiome()]);
            } else {
                comboBoxBiome.setSelectedItem(MC_PLAINS);
            }
        } else if ((platform == JAVA_ANVIL_1_15) || (platform == JAVA_ANVIL_1_17)) {
            // TODO move available biomes to Platform
            final String[] availableBiomes = Arrays.stream(Minecraft1_17Biomes.BIOME_NAMES).filter(Objects::nonNull).map(biome -> "minecraft:" + biome.toLowerCase().replace(' ', '_')).toArray(String[]::new);
            comboBoxBiome.setModel(new DefaultComboBoxModel<>(availableBiomes));
            comboBoxBiome.setEditable(true);
            if (superflatPreset.getBiomeName() != null) {
                comboBoxBiome.setSelectedItem(superflatPreset.getBiomeName());
            } else if (Minecraft1_17Biomes.BIOME_NAMES[superflatPreset.getBiome()] != null) {
                comboBoxBiome.setSelectedItem("minecraft:"+ Minecraft1_17Biomes.BIOME_NAMES[superflatPreset.getBiome()].toLowerCase().replace(' ', '_'));
            } else {
                comboBoxBiome.setSelectedItem(MC_PLAINS);
            }
        } else {
            final String[] biomeNames;
            if (platform == DefaultPlugin.JAVA_MCREGION) {
                biomeNames = AbstractMinecraft1_1BiomeScheme.BIOME_NAMES;
            } else if (platform == DefaultPlugin.JAVA_ANVIL) {
                biomeNames = Minecraft1_7Biomes.BIOME_NAMES;
            } else {
                // Default to 1.17 biomes for now, even for other platforms
                // TODO move available biomes to Platform
                biomeNames = Minecraft1_17Biomes.BIOME_NAMES;
            }
            final Integer[] availableBiomes = IntStream.range(0, biomeNames.length).filter(i -> biomeNames[i] != null).boxed().toArray(Integer[]::new);
            comboBoxBiome.setModel(new DefaultComboBoxModel<>(availableBiomes));
            comboBoxBiome.setRenderer(new DefaultListCellRenderer() {
                @Override
                public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                    super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                    setText(biomeNames[(Integer) value] + " (" + value + ")");
                    return this;
                }
            });
            comboBoxBiome.setSelectedItem(superflatPreset.getBiome());
        }

        layersTableModel = new SuperflatPresetLayersTableModel(superflatPreset.getLayers());
        tableLayers.setModel(layersTableModel);

        if ((platform == JAVA_MCREGION) || (platform == JAVA_ANVIL) || (platform == JAVA_ANVIL_1_15)) {
            structuresTableModel = new SuperflatPresetStructuresTableModel(superflatPreset.getStructures());
            tableStructures.setModel(structuresTableModel);
            checkBoxFeatures.setVisible(false);
            checkBoxLakes.setVisible(false);
        } else {
            // TODO add new flags? strongholds, lakes?
            structuresTableModel = null;
            labelStructures.setVisible(false);
            tableStructures.setVisible(false);
            checkBoxFeatures.setSelected(superflatPreset.isFeatures());
            checkBoxLakes.setSelected(superflatPreset.isLakes());
        }

        setControlStates();
        
        getRootPane().setDefaultButton(buttonOK);
        scaleToUI();
        pack();
        setLocationRelativeTo(parent);
    }

    private void setControlStates() {
        buttonLayerDelete.setEnabled(tableLayers.getSelectedRowCount() > 0);
    }

    private boolean save() {
        Object selectedBiome = comboBoxBiome.getSelectedItem();
        if (selectedBiome instanceof String) {
            superflatPreset.setBiomeName((String) selectedBiome);
            superflatPreset.setBiome(BIOMES_BY_MODERN_ID.getOrDefault(selectedBiome, BIOME_PLAINS));
        } else if (selectedBiome instanceof Integer) {
            superflatPreset.setBiome((Integer) selectedBiome);
            if (MODERN_IDS[(Integer) selectedBiome] != null) {
                superflatPreset.setBiomeName(MODERN_IDS[(Integer) selectedBiome]);
            } else {
                superflatPreset.setBiomeName(MC_PLAINS);
            }
        }
        superflatPreset.setLayers(layersTableModel.getLayers());
        if ((platform == JAVA_MCREGION) || (platform == JAVA_ANVIL) || (platform == JAVA_ANVIL_1_15)) {
            superflatPreset.setStructures(structuresTableModel.getStructures());
        } else {
            superflatPreset.setFeatures(checkBoxFeatures.isSelected());
            superflatPreset.setLakes(checkBoxLakes.isSelected());
        }
        return true;
    }

    private void addLayer() {
        layersTableModel.addLayer(new Layer(MC_DIRT, 1));
    }

    private void deleteLayer() {
        layersTableModel.deleteLayer(tableLayers.getSelectedRow());
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLabel1 = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        tableLayers = new javax.swing.JTable();
        buttonLayerAdd = new javax.swing.JButton();
        buttonLayerDelete = new javax.swing.JButton();
        jLabel2 = new javax.swing.JLabel();
        comboBoxBiome = new javax.swing.JComboBox();
        buttonCancel = new javax.swing.JButton();
        buttonOK = new javax.swing.JButton();
        tableStructures = new javax.swing.JTable();
        labelStructures = new javax.swing.JLabel();
        checkBoxFeatures = new javax.swing.JCheckBox();
        checkBoxLakes = new javax.swing.JCheckBox();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Edit Superflat Preset");

        jLabel1.setText("Biome:");

        tableLayers.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        jScrollPane1.setViewportView(tableLayers);

        buttonLayerAdd.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/pepsoft/worldpainter/icons/brick_add.png"))); // NOI18N
        buttonLayerAdd.setToolTipText("Add a layer.");
        buttonLayerAdd.setMargin(new java.awt.Insets(2, 2, 2, 2));
        buttonLayerAdd.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonLayerAddActionPerformed(evt);
            }
        });

        buttonLayerDelete.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/pepsoft/worldpainter/icons/brick_delete.png"))); // NOI18N
        buttonLayerDelete.setToolTipText("Delete the selected layer.");
        buttonLayerDelete.setEnabled(false);
        buttonLayerDelete.setMargin(new java.awt.Insets(2, 2, 2, 2));
        buttonLayerDelete.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonLayerDeleteActionPerformed(evt);
            }
        });

        jLabel2.setText("Layers:");

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

        tableStructures.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        tableStructures.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {

            }
        ));
        tableStructures.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        tableStructures.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                tableStructuresMouseClicked(evt);
            }
        });

        labelStructures.setText("Structures:");

        checkBoxFeatures.setText("features");

        checkBoxLakes.setText("lakes");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(tableStructures, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 614, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(buttonLayerAdd, javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(buttonLayerDelete, javax.swing.GroupLayout.Alignment.TRAILING)))
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(checkBoxFeatures)
                            .addComponent(jLabel2)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jLabel1)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(comboBoxBiome, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addComponent(labelStructures))
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(checkBoxLakes)
                                .addGap(0, 0, Short.MAX_VALUE))
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                .addGap(0, 0, Short.MAX_VALUE)
                                .addComponent(buttonOK)))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(buttonCancel)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(comboBoxBiome, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addComponent(jLabel2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(buttonLayerAdd)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(buttonLayerDelete))
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 143, Short.MAX_VALUE))
                .addGap(18, 18, 18)
                .addComponent(labelStructures)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(tableStructures, javax.swing.GroupLayout.DEFAULT_SIZE, 87, Short.MAX_VALUE)
                .addGap(10, 10, 10)
                .addComponent(checkBoxFeatures)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(checkBoxLakes)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(buttonOK)
                    .addComponent(buttonCancel))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void buttonCancelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonCancelActionPerformed
        cancel();
    }//GEN-LAST:event_buttonCancelActionPerformed

    private void buttonOKActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonOKActionPerformed
        if (tableLayers.isEditing()) {
            tableLayers.getCellEditor().stopCellEditing();
        }
        if (tableStructures.isEditing()) {
            tableStructures.getCellEditor().stopCellEditing();
        }
        if (save()) {
            ok();
        }
    }//GEN-LAST:event_buttonOKActionPerformed

    private void buttonLayerAddActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonLayerAddActionPerformed
        addLayer();
    }//GEN-LAST:event_buttonLayerAddActionPerformed

    private void buttonLayerDeleteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonLayerDeleteActionPerformed
        deleteLayer();
    }//GEN-LAST:event_buttonLayerDeleteActionPerformed

    private void tableStructuresMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_tableStructuresMouseClicked
        // TODO add your handling code here:
    }//GEN-LAST:event_tableStructuresMouseClicked

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton buttonCancel;
    private javax.swing.JButton buttonLayerAdd;
    private javax.swing.JButton buttonLayerDelete;
    private javax.swing.JButton buttonOK;
    private javax.swing.JCheckBox checkBoxFeatures;
    private javax.swing.JCheckBox checkBoxLakes;
    private javax.swing.JComboBox comboBoxBiome;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JLabel labelStructures;
    private javax.swing.JTable tableLayers;
    private javax.swing.JTable tableStructures;
    // End of variables declaration//GEN-END:variables

    private final SuperflatPreset superflatPreset;
    private final Platform platform;
    private final SuperflatPresetLayersTableModel layersTableModel;
    private final SuperflatPresetStructuresTableModel structuresTableModel;
}