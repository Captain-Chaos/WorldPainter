/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/*
 * TerrainRangesEditor.java
 *
 * Created on Mar 28, 2012, 5:53:35 PM
 */
package org.pepsoft.worldpainter.themes;

import java.awt.Dialog;
import java.awt.Frame;
import java.awt.Window;
import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableCellRenderer;
import org.pepsoft.worldpainter.ColourScheme;
import org.pepsoft.worldpainter.Terrain;

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
        if (! terrainTableModel.isValid()) {
            JOptionPane.showMessageDialog(this, "You have configured multiple terrain types with the same levels!\nRemove, or change the level of, one of the duplicates.", "Duplicate Levels", JOptionPane.WARNING_MESSAGE);
            return false;
        }
        theme.setTerrainRanges(terrainTableModel.getTerrainRanges());
        theme.setRandomise(checkBoxRandomise.isSelected());
        boolean beaches = checkBoxBeaches.isSelected();
        theme.setBeaches(beaches);
        theme.setWaterHeight(((Number) spinnerWaterLevel.getValue()).intValue());
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
            jTable1.setModel(terrainTableModel);
            
            jTable1.setDefaultRenderer(Integer.class, new DefaultTableCellRenderer());
            jTable1.setDefaultRenderer(Terrain.class, new TerrainTableCellRenderer(colourScheme));
            jTable1.setDefaultRenderer(JButton.class, new JButtonTableCellRenderer());
            
            jTable1.setDefaultEditor(Integer.class, new JSpinnerTableCellEditor(new SpinnerNumberModel(1, 1, theme.getMaxHeight() - 1, 1)));
            JComboBox terrainEditor = new JComboBox(Terrain.getConfiguredValues());
            terrainEditor.setRenderer(new TerrainListCellRenderer(colourScheme));
            jTable1.setDefaultEditor(Terrain.class, new DefaultCellEditor(terrainEditor));
            jTable1.setDefaultEditor(JButton.class, new JButtonTableCellEditor(this));
            
            checkBoxBeaches.setSelected(theme.isBeaches());
            spinnerWaterLevel.setModel(new SpinnerNumberModel(theme.getWaterHeight(), 0, theme.getMaxHeight() - 1, 1));
            spinnerWaterLevel.setEnabled(checkBoxBeaches.isSelected());
            
            checkBoxRandomise.setSelected(theme.isRandomise());
        }
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        jTable1.setEnabled(enabled);
        checkBoxBeaches.setEnabled(enabled);
        spinnerWaterLevel.setEnabled(enabled && checkBoxBeaches.isSelected());
        buttonAdd.setEnabled(enabled);
        checkBoxRandomise.setEnabled(enabled);
    }

    public ChangeListener getChangeListener() {
        return changeListener;
    }

    public void setChangeListener(ChangeListener changeListener) {
        this.changeListener = changeListener;
    }

    // ButtonPressListener

    @Override
    public void buttonPressed(int row, int column) {
        terrainTableModel.deleteRow(row);
    }

    // TerrainRangesTableModel.ChangeListener
    
    @Override
    public void dataChanged(TerrainRangesTableModel model) {
        notifyChangeListener();
    }

    private void addTerrain() {
        Window window = SwingUtilities.getWindowAncestor(this);
        AddTerrainRangeDialog dialog = (window instanceof Frame) ? new AddTerrainRangeDialog((Frame) window, theme.getMaxHeight(), colourScheme) : new AddTerrainRangeDialog((Dialog) window, theme.getMaxHeight(), colourScheme);
        dialog.setVisible(true);
        if (! dialog.isCancelled()) {
            terrainTableModel.addRow(dialog.getSelectedLevel(), dialog.getSelectedTerrain());
        }
    }
    
    private void notifyChangeListener() {
        if (changeListener != null) {
            changeListener.settingsModified(this);
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

        jScrollPane1 = new javax.swing.JScrollPane();
        jTable1 = new javax.swing.JTable();
        buttonAdd = new javax.swing.JButton();
        checkBoxBeaches = new javax.swing.JCheckBox();
        spinnerWaterLevel = new javax.swing.JSpinner();
        checkBoxRandomise = new javax.swing.JCheckBox();
        jLabel1 = new javax.swing.JLabel();

        jTable1.setRowHeight(25);
        jScrollPane1.setViewportView(jTable1);

        buttonAdd.setText("Add...");
        buttonAdd.addActionListener(this::buttonAddActionPerformed);

        checkBoxBeaches.setText("beaches around water level:");
        checkBoxBeaches.setToolTipText("Whether to add beaches from two levels below the water level to two levels above.");
        checkBoxBeaches.addActionListener(this::checkBoxBeachesActionPerformed);

        spinnerWaterLevel.setEnabled(false);
        spinnerWaterLevel.addChangeListener(this::spinnerWaterLevelStateChanged);

        checkBoxRandomise.setText("noisy edges");
        checkBoxRandomise.setToolTipText("Whether to randomise the edges of the terrain types (except beaches).");
        checkBoxRandomise.addActionListener(this::checkBoxRandomiseActionPerformed);

        jLabel1.setText("Terrain:");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addComponent(checkBoxBeaches)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(spinnerWaterLevel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(checkBoxRandomise)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(buttonAdd))
            .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jLabel1)
                .addGap(0, 0, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 108, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(buttonAdd)
                    .addComponent(checkBoxBeaches)
                    .addComponent(spinnerWaterLevel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(checkBoxRandomise)))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void buttonAddActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonAddActionPerformed
        addTerrain();
    }//GEN-LAST:event_buttonAddActionPerformed

    private void checkBoxBeachesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_checkBoxBeachesActionPerformed
        spinnerWaterLevel.setEnabled(checkBoxBeaches.isSelected());
        notifyChangeListener();
    }//GEN-LAST:event_checkBoxBeachesActionPerformed

    private void spinnerWaterLevelStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_spinnerWaterLevelStateChanged
        notifyChangeListener();
    }//GEN-LAST:event_spinnerWaterLevelStateChanged

    private void checkBoxRandomiseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_checkBoxRandomiseActionPerformed
        notifyChangeListener();
    }//GEN-LAST:event_checkBoxRandomiseActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton buttonAdd;
    private javax.swing.JCheckBox checkBoxBeaches;
    private javax.swing.JCheckBox checkBoxRandomise;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTable jTable1;
    private javax.swing.JSpinner spinnerWaterLevel;
    // End of variables declaration//GEN-END:variables

    private SimpleTheme theme;
    private TerrainRangesTableModel terrainTableModel;
    private ColourScheme colourScheme;
    private ChangeListener changeListener;
    
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