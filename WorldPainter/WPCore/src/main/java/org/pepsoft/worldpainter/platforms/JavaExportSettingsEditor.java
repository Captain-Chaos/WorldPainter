/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.platforms;

import org.pepsoft.worldpainter.Platform;
import org.pepsoft.worldpainter.exporting.ExportSettings;
import org.pepsoft.worldpainter.exporting.ExportSettingsEditor;

import static org.pepsoft.worldpainter.Platform.Capability.NAME_BASED;
import static org.pepsoft.worldpainter.Platform.Capability.PRECALCULATED_LIGHT;
import static org.pepsoft.worldpainter.platforms.JavaExportSettings.FloatMode.*;

/**
 *
 * @author Pepijn
 */
public class JavaExportSettingsEditor extends ExportSettingsEditor {

    /**
     * Creates new form Java1_15PostProcessorSettings
     */
    public JavaExportSettingsEditor(Platform platform) {
        this.platform = platform;
        initComponents();
    }

    @Override
    public void setExportSettings(ExportSettings exportSettings) {
        JavaExportSettings javaSettings = (JavaExportSettings) exportSettings;
        switch (javaSettings.waterMode) {
            case LEAVE_FLOATING:
                radioButtonWaterFloat.setSelected(true);
                break;
            case DROP:
                radioButtonWaterDrop.setSelected(true);
                break;
            default:
                throw new IllegalArgumentException("Invalid water mode " + javaSettings.waterMode);
        }
        switch (javaSettings.lavaMode) {
            case LEAVE_FLOATING:
                radioButtonLavaFloat.setSelected(true);
                break;
            case DROP:
                radioButtonLavaDrop.setSelected(true);
                break;
            default:
                throw new IllegalArgumentException("Invalid lava mode " + javaSettings.lavaMode);
        }
        switch (javaSettings.sandMode) {
            case LEAVE_FLOATING:
                radioButtonSandFloat.setSelected(true);
                break;
            case DROP:
                radioButtonSandDrop.setSelected(true);
                break;
            case SUPPORT:
                radioButtonSandSupport.setSelected(true);
                break;
        }
        switch (javaSettings.gravelMode) {
            case LEAVE_FLOATING:
                radioButtonGravelFloat.setSelected(true);
                break;
            case DROP:
                radioButtonGravelDrop.setSelected(true);
                break;
            case SUPPORT:
                radioButtonGravelSupport.setSelected(true);
                break;
        }
        switch (javaSettings.cementMode) {
            case LEAVE_FLOATING:
                radioButtonCementFloat.setSelected(true);
                break;
            case DROP:
                radioButtonCementDrop.setSelected(true);
                break;
            case SUPPORT:
                radioButtonCementSupport.setSelected(true);
                break;
        }
        checkBoxWaterFlow.setSelected(javaSettings.flowWater);
        checkBoxLavaFlow.setSelected(javaSettings.flowLava);
        checkBoxSkyLight.setSelected(javaSettings.calculateSkyLight);
        checkBoxBlockLight.setSelected(javaSettings.calculateBlockLight);
        checkBoxLeafDistance.setSelected(javaSettings.calculateLeafDistance);
        checkBoxRemoveFloatingLeaves.setSelected(javaSettings.removeFloatingLeaves);
        checkBoxMakeAllLeavesPersistent.setSelected(javaSettings.makeAllLeavesPersistent);
        checkBoxRemovePlants.setSelected(javaSettings.isRemovePlants());
        setControlStates();
    }

    @Override
    public JavaExportSettings getExportSettings() {
        return new JavaExportSettings(
                radioButtonWaterFloat.isSelected() ? LEAVE_FLOATING : DROP,
                radioButtonLavaFloat.isSelected() ? LEAVE_FLOATING : DROP,
                radioButtonSandFloat.isSelected() ? LEAVE_FLOATING : (radioButtonSandSupport.isSelected() ? SUPPORT : DROP),
                radioButtonGravelFloat.isSelected() ? LEAVE_FLOATING : (radioButtonGravelSupport.isSelected() ? SUPPORT : DROP),
                radioButtonCementFloat.isSelected() ? LEAVE_FLOATING : (radioButtonCementSupport.isSelected() ? SUPPORT : DROP),
                checkBoxWaterFlow.isSelected(),
                checkBoxLavaFlow.isSelected(),
                checkBoxSkyLight.isSelected(),
                checkBoxBlockLight.isSelected(),
                checkBoxLeafDistance.isSelected(),
                checkBoxLeafDistance.isSelected() && checkBoxRemoveFloatingLeaves.isSelected(),
                checkBoxMakeAllLeavesPersistent.isSelected(),
                checkBoxRemovePlants.isSelected());
    }

    private void setControlStates() {
        checkBoxSkyLight.setEnabled(platform.capabilities.contains(PRECALCULATED_LIGHT));
        checkBoxBlockLight.setEnabled(platform.capabilities.contains(PRECALCULATED_LIGHT));
        checkBoxLeafDistance.setEnabled(platform.capabilities.contains(NAME_BASED));
        checkBoxRemoveFloatingLeaves.setEnabled(platform.capabilities.contains(NAME_BASED) && checkBoxLeafDistance.isSelected());
    }

    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The
     * content of this method is always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        buttonGroup1 = new javax.swing.ButtonGroup();
        buttonGroup2 = new javax.swing.ButtonGroup();
        buttonGroup3 = new javax.swing.ButtonGroup();
        buttonGroup4 = new javax.swing.ButtonGroup();
        buttonGroup5 = new javax.swing.ButtonGroup();
        jLabel1 = new javax.swing.JLabel();
        radioButtonWaterFloat = new javax.swing.JRadioButton();
        radioButtonWaterDrop = new javax.swing.JRadioButton();
        checkBoxWaterFlow = new javax.swing.JCheckBox();
        jLabel2 = new javax.swing.JLabel();
        radioButtonLavaFloat = new javax.swing.JRadioButton();
        radioButtonLavaDrop = new javax.swing.JRadioButton();
        checkBoxLavaFlow = new javax.swing.JCheckBox();
        jLabel3 = new javax.swing.JLabel();
        radioButtonGravelFloat = new javax.swing.JRadioButton();
        radioButtonGravelSupport = new javax.swing.JRadioButton();
        radioButtonGravelDrop = new javax.swing.JRadioButton();
        jLabel4 = new javax.swing.JLabel();
        radioButtonCementFloat = new javax.swing.JRadioButton();
        radioButtonCementSupport = new javax.swing.JRadioButton();
        radioButtonCementDrop = new javax.swing.JRadioButton();
        radioButtonSandFloat = new javax.swing.JRadioButton();
        jLabel5 = new javax.swing.JLabel();
        radioButtonSandSupport = new javax.swing.JRadioButton();
        radioButtonSandDrop = new javax.swing.JRadioButton();
        checkBoxLeafDistance = new javax.swing.JCheckBox();
        jLabel6 = new javax.swing.JLabel();
        checkBoxRemoveFloatingLeaves = new javax.swing.JCheckBox();
        checkBoxSkyLight = new javax.swing.JCheckBox();
        jLabel7 = new javax.swing.JLabel();
        checkBoxBlockLight = new javax.swing.JCheckBox();
        checkBoxMakeAllLeavesPersistent = new javax.swing.JCheckBox();
        checkBoxRemovePlants = new javax.swing.JCheckBox();
        jLabel8 = new javax.swing.JLabel();

        jLabel1.setText("Water:");

        buttonGroup1.add(radioButtonWaterFloat);
        radioButtonWaterFloat.setText("leave floating");

        buttonGroup1.add(radioButtonWaterDrop);
        radioButtonWaterDrop.setText("drop");

        checkBoxWaterFlow.setText("make unbounded water flow");

        jLabel2.setText("Lava:");

        buttonGroup2.add(radioButtonLavaFloat);
        radioButtonLavaFloat.setText("leave floating");

        buttonGroup2.add(radioButtonLavaDrop);
        radioButtonLavaDrop.setText("drop");

        checkBoxLavaFlow.setText("make unbounded lava flow");

        jLabel3.setText("Gravel:");

        buttonGroup3.add(radioButtonGravelFloat);
        radioButtonGravelFloat.setText("leave floating");

        buttonGroup3.add(radioButtonGravelSupport);
        radioButtonGravelSupport.setText("support with stone");

        buttonGroup3.add(radioButtonGravelDrop);
        radioButtonGravelDrop.setText("drop");

        jLabel4.setText("Cement:");

        buttonGroup4.add(radioButtonCementFloat);
        radioButtonCementFloat.setText("leave floating");

        buttonGroup4.add(radioButtonCementSupport);
        radioButtonCementSupport.setText("support with stone");

        buttonGroup4.add(radioButtonCementDrop);
        radioButtonCementDrop.setText("drop");

        buttonGroup5.add(radioButtonSandFloat);
        radioButtonSandFloat.setText("leave floating");

        jLabel5.setText("Sand:");

        buttonGroup5.add(radioButtonSandSupport);
        radioButtonSandSupport.setText("support with sandstone");

        buttonGroup5.add(radioButtonSandDrop);
        radioButtonSandDrop.setText("drop");

        checkBoxLeafDistance.setText("calculate distance property");
        checkBoxLeafDistance.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                checkBoxLeafDistanceActionPerformed(evt);
            }
        });

        jLabel6.setText("Leaves:");

        checkBoxRemoveFloatingLeaves.setText("remove floating leaf blocks");

        checkBoxSkyLight.setText("calculate sky light");

        jLabel7.setText("Light:");

        checkBoxBlockLight.setText("calculate block light");

        checkBoxMakeAllLeavesPersistent.setText("<html>make <i>all</i> leaves persistent</html>");

        checkBoxRemovePlants.setText("remove from invalid blocks");

        jLabel8.setText("Plants:");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel1)
                    .addComponent(jLabel2)
                    .addComponent(jLabel3)
                    .addComponent(jLabel4)
                    .addComponent(jLabel5)
                    .addComponent(jLabel6)
                    .addComponent(jLabel7)
                    .addComponent(jLabel8))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(checkBoxRemovePlants)
                    .addComponent(checkBoxMakeAllLeavesPersistent, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(checkBoxLeafDistance)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(checkBoxSkyLight)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(checkBoxBlockLight))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(radioButtonSandFloat)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(radioButtonSandSupport)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(radioButtonSandDrop))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(radioButtonCementFloat)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(radioButtonCementSupport)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(radioButtonCementDrop))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(radioButtonLavaFloat)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(radioButtonLavaDrop))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(radioButtonWaterFloat)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(radioButtonWaterDrop))
                    .addComponent(checkBoxWaterFlow)
                    .addComponent(checkBoxLavaFlow)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(radioButtonGravelFloat)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(radioButtonGravelSupport)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(radioButtonGravelDrop))
                    .addGroup(layout.createSequentialGroup()
                        .addGap(21, 21, 21)
                        .addComponent(checkBoxRemoveFloatingLeaves)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(radioButtonWaterFloat)
                    .addComponent(radioButtonWaterDrop))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(checkBoxWaterFlow)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(radioButtonLavaFloat)
                    .addComponent(radioButtonLavaDrop))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(checkBoxLavaFlow)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(radioButtonSandFloat)
                    .addComponent(jLabel5)
                    .addComponent(radioButtonSandSupport)
                    .addComponent(radioButtonSandDrop))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(radioButtonGravelFloat)
                    .addComponent(jLabel3)
                    .addComponent(radioButtonGravelSupport)
                    .addComponent(radioButtonGravelDrop))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(radioButtonCementFloat)
                    .addComponent(jLabel4)
                    .addComponent(radioButtonCementSupport)
                    .addComponent(radioButtonCementDrop))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(checkBoxSkyLight)
                    .addComponent(jLabel7)
                    .addComponent(checkBoxBlockLight))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(checkBoxLeafDistance)
                    .addComponent(jLabel6))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(checkBoxRemoveFloatingLeaves)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(checkBoxMakeAllLeavesPersistent, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(checkBoxRemovePlants)
                    .addComponent(jLabel8))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void checkBoxLeafDistanceActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_checkBoxLeafDistanceActionPerformed
        setControlStates();
    }//GEN-LAST:event_checkBoxLeafDistanceActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.ButtonGroup buttonGroup1;
    private javax.swing.ButtonGroup buttonGroup2;
    private javax.swing.ButtonGroup buttonGroup3;
    private javax.swing.ButtonGroup buttonGroup4;
    private javax.swing.ButtonGroup buttonGroup5;
    private javax.swing.JCheckBox checkBoxBlockLight;
    private javax.swing.JCheckBox checkBoxLavaFlow;
    private javax.swing.JCheckBox checkBoxLeafDistance;
    private javax.swing.JCheckBox checkBoxMakeAllLeavesPersistent;
    private javax.swing.JCheckBox checkBoxRemoveFloatingLeaves;
    private javax.swing.JCheckBox checkBoxRemovePlants;
    private javax.swing.JCheckBox checkBoxSkyLight;
    private javax.swing.JCheckBox checkBoxWaterFlow;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JRadioButton radioButtonCementDrop;
    private javax.swing.JRadioButton radioButtonCementFloat;
    private javax.swing.JRadioButton radioButtonCementSupport;
    private javax.swing.JRadioButton radioButtonGravelDrop;
    private javax.swing.JRadioButton radioButtonGravelFloat;
    private javax.swing.JRadioButton radioButtonGravelSupport;
    private javax.swing.JRadioButton radioButtonLavaDrop;
    private javax.swing.JRadioButton radioButtonLavaFloat;
    private javax.swing.JRadioButton radioButtonSandDrop;
    private javax.swing.JRadioButton radioButtonSandFloat;
    private javax.swing.JRadioButton radioButtonSandSupport;
    private javax.swing.JRadioButton radioButtonWaterDrop;
    private javax.swing.JRadioButton radioButtonWaterFloat;
    // End of variables declaration//GEN-END:variables

    private final Platform platform;
}