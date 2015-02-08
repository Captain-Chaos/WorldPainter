/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/*
 * ExportTileSelectionDialog.java
 *
 * Created on Apr 7, 2012, 5:38:27 PM
 */
package org.pepsoft.worldpainter;

import java.awt.Component;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.KeyStroke;
import org.pepsoft.worldpainter.layers.Layer;
import static org.pepsoft.worldpainter.Constants.*;
import org.pepsoft.worldpainter.biomeschemes.CustomBiomeManager;

/**
 *
 * @author pepijn
 */
public class ExportTileSelectionDialog extends javax.swing.JDialog implements WindowListener {
    /** Creates new form ExportTileSelectionDialog */
    public ExportTileSelectionDialog(java.awt.Dialog parent, World2 world, int selectedDimension, Set<Point> selectedTiles, ColourScheme colourScheme, BiomeScheme biomeScheme, CustomBiomeManager customBiomeManager, Collection<Layer> hiddenLayers, boolean contourLines, int contourSeparation, TileRenderer.LightOrigin lightOrigin) {
        super(parent, true);
        this.world = world;
        initComponents();
        
        List<Integer> dimensions = new ArrayList<Integer>();
        for (Dimension dimension: world.getDimensions()) {
            dimensions.add(dimension.getDim());
        }
        jComboBox1.setModel(new DefaultComboBoxModel(dimensions.toArray()));
        jComboBox1.setSelectedItem(selectedDimension);
        jComboBox1.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value != null) {
                    switch ((Integer) value) {
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
                }
                return this;
            }
            
            private static final long serialVersionUID = 1L;
        });
        
        ActionMap actionMap = rootPane.getActionMap();
        actionMap.put("cancel", new AbstractAction("cancel") {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
            
            private static final long serialVersionUID = 1L;
        });
        
        tileSelector1.setColourScheme(colourScheme);
        tileSelector1.setBiomeScheme(biomeScheme);
        tileSelector1.setHiddenLayers(hiddenLayers);
        tileSelector1.setContourLines(contourLines);
        tileSelector1.setContourSeparation(contourSeparation);
        tileSelector1.setLightOrigin(lightOrigin);
        tileSelector1.setDimension(world.getDimension(selectedDimension));
        tileSelector1.setCustomBiomeManager(customBiomeManager);
        if (selectedTiles != null) {
            tileSelector1.setSelectedTiles(selectedTiles);
        }

        InputMap inputMap = rootPane.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "cancel");
        
        getRootPane().setDefaultButton(buttonClose);
        
        setLocationRelativeTo(parent);
        
        addWindowListener(this);
    }
    
    public int getSelectedDimension() {
        return (Integer) jComboBox1.getSelectedItem();
    }
    
    public Set<Point> getSelectedTiles() {
        return tileSelector1.getSelectedTiles();
    }

    public void selectAll() {
        tileSelector1.selectAllTiles();
    }
    
    // WindowListener

    @Override
    public void windowOpened(WindowEvent e) {
        tileSelector1.moveToCentre();
    }

    @Override public void windowClosed(WindowEvent e) {}
    @Override public void windowClosing(WindowEvent e) {}
    @Override public void windowIconified(WindowEvent e) {}
    @Override public void windowDeiconified(WindowEvent e) {}
    @Override public void windowActivated(WindowEvent e) {}
    @Override public void windowDeactivated(WindowEvent e) {}

    private void setSpawn() {
        world.setSpawnPoint(tileSelector1.getCurrentLocation());
        tileSelector1.refresh();
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
        jComboBox1 = new javax.swing.JComboBox();
        jLabel2 = new javax.swing.JLabel();
        buttonClose = new javax.swing.JButton();
        tileSelector1 = new org.pepsoft.worldpainter.TileSelector();
        buttonSetSpawn = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Select Tiles For Export");

        jLabel1.setText("Dimension to export:");

        jComboBox1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jComboBox1ActionPerformed(evt);
            }
        });

        jLabel2.setText("WorldPainter works in tiles of 128 by 128 blocks. Select tiles to export:");

        buttonClose.setText("OK");
        buttonClose.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonCloseActionPerformed(evt);
            }
        });

        buttonSetSpawn.setText("Set Spawn");
        buttonSetSpawn.setToolTipText("Move the spawn point to the indicated location");
        buttonSetSpawn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonSetSpawnActionPerformed(evt);
            }
        });

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
                                .addComponent(jLabel1)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jComboBox1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addComponent(jLabel2))
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(buttonSetSpawn)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(buttonClose))
                    .addComponent(tileSelector1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(jComboBox1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(tileSelector1, javax.swing.GroupLayout.DEFAULT_SIZE, 364, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(buttonClose)
                    .addComponent(buttonSetSpawn))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jComboBox1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jComboBox1ActionPerformed
        int selectedDimension = getSelectedDimension();
        tileSelector1.setDimension(world.getDimension(selectedDimension));
        tileSelector1.moveToCentre();
        tileSelector1.clearSelection();
        buttonSetSpawn.setEnabled(selectedDimension == DIM_NORMAL);
    }//GEN-LAST:event_jComboBox1ActionPerformed

    private void buttonCloseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonCloseActionPerformed
        dispose();
    }//GEN-LAST:event_buttonCloseActionPerformed

    private void buttonSetSpawnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonSetSpawnActionPerformed
        setSpawn();
    }//GEN-LAST:event_buttonSetSpawnActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton buttonClose;
    private javax.swing.JButton buttonSetSpawn;
    private javax.swing.JComboBox jComboBox1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private org.pepsoft.worldpainter.TileSelector tileSelector1;
    // End of variables declaration//GEN-END:variables
    
    private final World2 world;
    
    private static final long serialVersionUID = 1L;
}