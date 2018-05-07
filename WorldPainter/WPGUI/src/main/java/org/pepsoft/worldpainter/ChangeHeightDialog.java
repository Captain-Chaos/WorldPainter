/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/*
 * ChangeHeightDialog.java
 *
 * Created on 30-jan-2012, 17:54:20
 */
package org.pepsoft.worldpainter;

import org.pepsoft.util.ProgressReceiver;
import org.pepsoft.util.ProgressReceiver.OperationCancelled;
import org.pepsoft.util.swing.ProgressDialog;
import org.pepsoft.util.swing.ProgressTask;
import org.pepsoft.worldpainter.heightMaps.HeightMapUtils;
import org.pepsoft.worldpainter.history.HistoryEntry;
import org.pepsoft.worldpainter.layers.Resources;
import org.pepsoft.worldpainter.layers.exporters.ResourcesExporter.ResourcesExporterSettings;

import javax.swing.*;
import java.awt.*;

import static org.pepsoft.minecraft.Constants.DEFAULT_MAX_HEIGHT_2;
import static org.pepsoft.util.swing.ProgressDialog.NOT_CANCELABLE;

/**
 *
 * @author pepijn
 */
public class ChangeHeightDialog extends WorldPainterDialog {
    /** Creates new form ChangeHeightDialog */
    public ChangeHeightDialog(java.awt.Frame parent, World2 world) {
        super(parent);
        this.world = world;
        
        initComponents();
        
        int maxHeight = world.getMaxHeight();
        labelCurrentHeight.setText(Integer.toString(maxHeight));
        comboBoxNewHeight.setSelectedItem(Integer.toString(maxHeight));
        
        getRootPane().setDefaultButton(buttonOK);
        
        setLocationRelativeTo(parent);
    }
    
    private void calculateDefaults() {
        int oldMaxHeight = world.getMaxHeight();
        int newMaxHeight = Integer.parseInt((String) comboBoxNewHeight.getSelectedItem());
        int defaultTranslateAmount = (newMaxHeight - oldMaxHeight) / 2;
        int maxTranslateAmount = newMaxHeight - 1;
        int minTranslateAmount = -maxTranslateAmount;
        spinnerTranslateAmount.setValue(defaultTranslateAmount);
        ((SpinnerNumberModel) spinnerTranslateAmount.getModel()).setMinimum(minTranslateAmount);
        ((SpinnerNumberModel) spinnerTranslateAmount.getModel()).setMaximum(maxTranslateAmount);
        int defaultScale = newMaxHeight * 100 / oldMaxHeight;
        spinnerScaleAmount.setValue(defaultScale);
    }
    
    private void setControlStates() {
        int oldMaxHeight = world.getMaxHeight();
        int newMaxHeight = Integer.parseInt((String) comboBoxNewHeight.getSelectedItem());
        boolean translate = checkBoxTranslate.isSelected();
        boolean scale = checkBoxScale.isSelected();
        buttonOK.setEnabled((oldMaxHeight != newMaxHeight) || (translate && ((Integer) spinnerTranslateAmount.getValue() != 0)) || (scale && ((Integer) spinnerScaleAmount.getValue() != 100)));
        spinnerTranslateAmount.setEnabled(translate);
        spinnerScaleAmount.setEnabled(scale);
        labelWarning.setVisible(newMaxHeight != DEFAULT_MAX_HEIGHT_2);
    }
    
    private void doResize() {
        int oldMaxHeight = world.getMaxHeight();
        int newMaxHeight = Integer.parseInt((String) comboBoxNewHeight.getSelectedItem());
        if ((newMaxHeight != oldMaxHeight) && (world.getImportedFrom() != null) && (JOptionPane.showConfirmDialog(this, "<html>This world was imported from an existing map!<br>Are you <i>sure</i> you want to change the height?<br>You will not be able to merge it back to the existing map any more!</html>", "Import from Existing Map", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) != JOptionPane.YES_OPTION)) {
            return;
        }
        boolean scale = checkBoxScale.isSelected();
        int scaleAmount = (Integer) spinnerScaleAmount.getValue();
        boolean translate = checkBoxTranslate.isSelected();
        int translateAmount = (Integer) spinnerTranslateAmount.getValue();
        HeightTransform transform = HeightTransform.get(scale ? scaleAmount : 100, translate ? translateAmount : 0);
        resizeWorld(world, transform, newMaxHeight, this);
        if (newMaxHeight != oldMaxHeight) {
            world.addHistoryEntry(HistoryEntry.WORLD_MAX_HEIGHT_CHANGED, newMaxHeight);
        }
        if (translate) {
            for (Dimension dimension: world.getDimensions()) {
                world.addHistoryEntry(HistoryEntry.WORLD_DIMENSION_SHIFTED_VERTICALLY, dimension.getName(), translateAmount);
            }
        }
    }
    
    static void resizeWorld(final World2 world, final HeightTransform transform, final int newMaxHeight, final Window parent) {
        int tileCount = 0;
        for (Dimension dim: world.getDimensions()) {
            dim.setEventsInhibited(true);
            tileCount += dim.getTiles().size();
        }
        final int finalTileCount = tileCount;
        
        try {
            ProgressDialog.executeTask(parent, new ProgressTask<World2>() {
                @Override
                public String getName() {
                    return "Changing world height";
                }

                @Override
                public World2 execute(ProgressReceiver progressReceiver) throws OperationCancelled {
                    int tileNo = 0;
                    int oldMaxHeight = world.getMaxHeight();
                    for (Dimension dim: world.getDimensions()) {
                        dim.clearUndo();
                        dim.getTiles().forEach(org.pepsoft.worldpainter.Tile::inhibitEvents);
                        try {
                            for (Tile tile: dim.getTiles()) {
                                tile.setMaxHeight(newMaxHeight, transform);
                                tileNo++;
                                progressReceiver.setProgress((float) tileNo / finalTileCount);
                            }
                            dim.setMaxHeight(newMaxHeight);
                            TileFactory tileFactory = dim.getTileFactory();
                            if (tileFactory instanceof HeightMapTileFactory) {
                                HeightMapTileFactory heightMapTileFactory = (HeightMapTileFactory) tileFactory;
                                heightMapTileFactory.setMaxHeight(newMaxHeight, transform);
                                float baseHeight = heightMapTileFactory.getBaseHeight();
                                float transposeAmount = transform.transformHeight(baseHeight) - baseHeight;
                                heightMapTileFactory.setHeightMap(HeightMapUtils.transposeHeightMap(heightMapTileFactory.getHeightMap(), transposeAmount));
                            }
                            ResourcesExporterSettings resourcesSettings = (ResourcesExporterSettings) dim.getLayerSettings(Resources.INSTANCE);
                            if (resourcesSettings != null) {
                                for (int blockType: resourcesSettings.getBlockTypes()) {
                                    int maxLevel = resourcesSettings.getMaxLevel(blockType);
                                    if (maxLevel == (oldMaxHeight - 1)) {
                                        maxLevel = newMaxHeight - 1;
                                    } else if (maxLevel > 1) {
                                        maxLevel = clamp(transform.transformHeight(maxLevel), newMaxHeight - 1);
                                    }
                                    resourcesSettings.setMaxLevel(blockType, maxLevel);
                                    resourcesSettings.setMaxLevel(blockType, clamp(transform.transformHeight(resourcesSettings.getMaxLevel(blockType)), newMaxHeight - 1));
                                }
                            }
                            dim.clearUndo();
                            dim.armSavePoint();
                        } finally {
                            dim.getTiles().forEach(org.pepsoft.worldpainter.Tile::releaseEvents);
                        }
                    }
                    world.setMaxHeight(newMaxHeight);
                    return world;
                }
            }, NOT_CANCELABLE);
        } finally {
            for (Dimension dim: world.getDimensions()) {
                dim.setEventsInhibited(false);
            }
        }
    }

    private static int clamp(int level, int maxLevel) {
        if (level < 0) {
            return 0;
        } else if (level > maxLevel) {
            return maxLevel;
        } else {
            return level;
        }
    }
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        labelCurrentHeight = new javax.swing.JLabel();
        comboBoxNewHeight = new javax.swing.JComboBox();
        buttonCancel = new javax.swing.JButton();
        buttonOK = new javax.swing.JButton();
        jLabel5 = new javax.swing.JLabel();
        spinnerTranslateAmount = new javax.swing.JSpinner();
        label = new javax.swing.JLabel();
        spinnerScaleAmount = new javax.swing.JSpinner();
        jLabel7 = new javax.swing.JLabel();
        checkBoxScale = new javax.swing.JCheckBox();
        checkBoxTranslate = new javax.swing.JCheckBox();
        jLabel6 = new javax.swing.JLabel();
        jLabel8 = new javax.swing.JLabel();
        labelWarning = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Change Height");

        jLabel1.setText("Current height:");

        jLabel2.setText("New height:");

        labelCurrentHeight.setText("jLabel3");

        comboBoxNewHeight.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "32", "64", "128", "256", "512", "1024", "2048" }));
        comboBoxNewHeight.addActionListener(this::comboBoxNewHeightActionPerformed);

        buttonCancel.setText("Cancel");
        buttonCancel.addActionListener(this::buttonCancelActionPerformed);

        buttonOK.setText("OK");
        buttonOK.setEnabled(false);
        buttonOK.addActionListener(this::buttonOKActionPerformed);

        jLabel5.setText("Terrain and water levels:");

        spinnerTranslateAmount.setModel(new javax.swing.SpinnerNumberModel(0, -127, 127, 1));
        spinnerTranslateAmount.setEnabled(false);
        spinnerTranslateAmount.addChangeListener(this::spinnerTranslateAmountStateChanged);

        label.setText("blocks");

        spinnerScaleAmount.setModel(new javax.swing.SpinnerNumberModel(100, 1, 9999, 1));
        spinnerScaleAmount.setEnabled(false);
        spinnerScaleAmount.addChangeListener(this::spinnerScaleAmountStateChanged);

        jLabel7.setText("%");

        checkBoxScale.setText("Scale");
        checkBoxScale.setToolTipText("<html>Scale the levels by the specified percentage;<br>\nlevels that are (still) too low or high will be cut off.</html>");
        checkBoxScale.addChangeListener(this::checkBoxScaleStateChanged);

        checkBoxTranslate.setText("Shift");
        checkBoxTranslate.setToolTipText("<html>Shift the levels up or down by the specified number of blocks;<br>\nnegative means down; levels which are (still) too low or high will be cut off.</html>");
        checkBoxTranslate.addChangeListener(this::checkBoxTranslateStateChanged);

        jLabel6.setText("<html><b>Note:</b> this operation cannot be undone!</html>");

        jLabel8.setText("(If both are enabled scale will be applied first, then shift.)");

        labelWarning.setFont(labelWarning.getFont().deriveFont(labelWarning.getFont().getStyle() | java.awt.Font.BOLD));
        labelWarning.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/pepsoft/worldpainter/icons/error.png"))); // NOI18N
        labelWarning.setText("Only Minecraft 1.1, with mods!");

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
                        .addComponent(buttonCancel))
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jLabel1)
                                    .addComponent(jLabel2))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(labelCurrentHeight)
                                    .addGroup(layout.createSequentialGroup()
                                        .addComponent(comboBoxNewHeight, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addGap(18, 18, 18)
                                        .addComponent(labelWarning))))
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(checkBoxTranslate)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(layout.createSequentialGroup()
                                        .addComponent(spinnerTranslateAmount, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addGap(0, 0, 0)
                                        .addComponent(label))
                                    .addGroup(layout.createSequentialGroup()
                                        .addComponent(spinnerScaleAmount, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addGap(0, 0, 0)
                                        .addComponent(jLabel7))))
                            .addComponent(jLabel5)
                            .addComponent(checkBoxScale)
                            .addComponent(jLabel6, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel8))
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel6, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(labelCurrentHeight))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(comboBoxNewHeight, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(labelWarning))
                .addGap(18, 18, 18)
                .addComponent(jLabel5)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(checkBoxScale)
                    .addComponent(spinnerScaleAmount, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel7))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(checkBoxTranslate)
                    .addComponent(spinnerTranslateAmount, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(label))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel8)
                .addGap(18, 18, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(buttonCancel)
                    .addComponent(buttonOK))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void comboBoxNewHeightActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_comboBoxNewHeightActionPerformed
        calculateDefaults();
        setControlStates();
    }//GEN-LAST:event_comboBoxNewHeightActionPerformed

    private void buttonCancelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonCancelActionPerformed
        cancel();
    }//GEN-LAST:event_buttonCancelActionPerformed

    private void checkBoxScaleStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_checkBoxScaleStateChanged
        setControlStates();
    }//GEN-LAST:event_checkBoxScaleStateChanged

    private void checkBoxTranslateStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_checkBoxTranslateStateChanged
        setControlStates();
    }//GEN-LAST:event_checkBoxTranslateStateChanged

    private void buttonOKActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonOKActionPerformed
        doResize();
        ok();
    }//GEN-LAST:event_buttonOKActionPerformed

    private void spinnerScaleAmountStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_spinnerScaleAmountStateChanged
        setControlStates();
    }//GEN-LAST:event_spinnerScaleAmountStateChanged

    private void spinnerTranslateAmountStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_spinnerTranslateAmountStateChanged
        setControlStates();
    }//GEN-LAST:event_spinnerTranslateAmountStateChanged

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton buttonCancel;
    private javax.swing.JButton buttonOK;
    private javax.swing.JCheckBox checkBoxScale;
    private javax.swing.JCheckBox checkBoxTranslate;
    private javax.swing.JComboBox comboBoxNewHeight;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel label;
    private javax.swing.JLabel labelCurrentHeight;
    private javax.swing.JLabel labelWarning;
    private javax.swing.JSpinner spinnerScaleAmount;
    private javax.swing.JSpinner spinnerTranslateAmount;
    // End of variables declaration//GEN-END:variables

    private final World2 world;

    private static final long serialVersionUID = 1L;
}