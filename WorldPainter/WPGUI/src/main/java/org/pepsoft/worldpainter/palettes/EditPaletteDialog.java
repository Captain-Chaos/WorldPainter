/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JDialog.java to edit this template
 */
package org.pepsoft.worldpainter.palettes;

import org.pepsoft.worldpainter.App;
import org.pepsoft.worldpainter.LayerListCellRenderer;
import org.pepsoft.worldpainter.WorldPainterDialog;
import org.pepsoft.worldpainter.layers.CustomLayer;
import org.pepsoft.worldpainter.layers.Layer;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import static java.util.Comparator.comparing;
import static java.util.Objects.requireNonNull;
import static org.pepsoft.util.swing.MessageUtils.beepAndShowError;

/**
 *
 * @author pepijn
 */
public class EditPaletteDialog extends WorldPainterDialog {

    /**
     * Creates new form EditPaletteDialog
     */
    public EditPaletteDialog(Window parent, PaletteManager paletteManager, Palette palette) {
        super(parent);
        requireNonNull(palette, "palette");
        this.paletteManager = paletteManager;
        this.palette = palette;

        initComponents();

        final DefaultListModel<CustomLayer> listModel = new DefaultListModel<>();
        palette.getLayers().forEach(listModel::addElement);
        listLayers.setModel(listModel);
        listLayers.setCellRenderer(new LayerListCellRenderer());
        textFieldName.setText(palette.getName());
        textFieldName.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                nameChanged();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                nameChanged();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                nameChanged();
            }
        });
        listLayers.addListSelectionListener(e -> setControlStates());

        getRootPane().setDefaultButton(buttonOk);
        scaleToUI();
        scaleWindowToUI();
        setLocationRelativeTo(parent);
    }

    private void setControlStates() {
        final boolean rowsSelected = ! listLayers.isSelectionEmpty();
        buttonTop.setEnabled(rowsSelected && (! listLayers.isSelectedIndex(0)));
        buttonUp.setEnabled(rowsSelected && (! listLayers.isSelectedIndex(0)));
        buttonDown.setEnabled(rowsSelected && (! listLayers.isSelectedIndex(listLayers.getModel().getSize() - 1)));
        buttonBottom.setEnabled(rowsSelected && (! listLayers.isSelectedIndex(listLayers.getModel().getSize() - 1)));
    }

    private boolean saveData() {
        boolean paletteChanged = false;
        final String newName = textFieldName.getText();
        if (! newName.equals(palette.getName())) {
            final Palette existingPalette = paletteManager.getPalette(newName);
            if ((existingPalette != null) && (existingPalette != palette)) {
                beepAndShowError(this, "There already is a palette named \"" + newName + "\"", "Name Already In Use");
                return false;
            }
            paletteManager.rename(palette, newName);
            paletteChanged = true;
        }
        if (orderChanged) {
            // Remove all the layers from the palette, and reset their index so their current order isn't restored when
            // adding them back
            for (Enumeration<CustomLayer> e = ((DefaultListModel<CustomLayer>) listLayers.getModel()).elements(); e.hasMoreElements(); ) {
                final CustomLayer layer = e.nextElement();
                paletteManager.unregister(layer);
                layer.setPaletteIndex(null);
            }
            // Add them back in the order they are on the list
            for (Enumeration<CustomLayer> e = ((DefaultListModel<CustomLayer>) listLayers.getModel()).elements(); e.hasMoreElements(); ) {
                final CustomLayer layer = e.nextElement();
                paletteManager.register(layer);
            }
            paletteChanged = true;
        }
        // Make sure to mark the dimension dirty to ensure it is saved even if the user does nothing else
        if (paletteChanged) {
            App.getInstance().getDimension().changed();
        }
        return true;
    }

    private void moveSelectedLayers(int destinationIndex) {
        final int sourceIndex = listLayers.getSelectedIndex();
        final List<CustomLayer> selectedLayers = removeSelectedLayers();
        insertLayers(destinationIndex, selectedLayers);
        listLayers.ensureIndexIsVisible((sourceIndex < destinationIndex) ? (destinationIndex + selectedLayers.size() - 1) : destinationIndex);
        orderChanged();
    }

    private List<CustomLayer> removeSelectedLayers() {
        final int sourceIndex = listLayers.getSelectedIndex();
        final List<CustomLayer> selectedRows = listLayers.getSelectedValuesList();
        final DefaultListModel<CustomLayer> model = (DefaultListModel<CustomLayer>) listLayers.getModel();
        selectedRows.forEach(row -> model.remove(sourceIndex));
        return selectedRows;
    }

    private void insertLayers(int index, List<CustomLayer> layers) {
        final DefaultListModel<CustomLayer> model = (DefaultListModel<CustomLayer>) listLayers.getModel();
        for (int i = 0; i < layers.size(); i++) {
            model.add(index + i, layers.get(i));
        }
        listLayers.addSelectionInterval(index, index + layers.size() - 1);
    }

    private void sortLayers() {
        final List<CustomLayer> layers = new ArrayList<>(palette.getLayers());
        layers.sort(comparing(Layer::getName));
        final DefaultListModel<CustomLayer> listModel = new DefaultListModel<>();
        layers.forEach(listModel::addElement);
        listLayers.setModel(listModel);
        orderChanged();
        buttonSort.setEnabled(false);
    }

    private void reset() {
        final DefaultListModel<CustomLayer> listModel = new DefaultListModel<>();
        palette.getLayers().forEach(listModel::addElement);
        listLayers.setModel(listModel);
        textFieldName.setText(palette.getName());
        buttonReset.setEnabled(false);
        buttonOk.setEnabled(false);
        buttonSort.setEnabled(true);
        orderChanged = false;
    }

    private void nameChanged() {
        buttonOk.setEnabled(true);
        buttonReset.setEnabled(true);
        buttonSort.setEnabled(true);
    }

    private void orderChanged() {
        nameChanged();
        orderChanged = true;
    }

    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The
     * content of this method is always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLabel1 = new javax.swing.JLabel();
        textFieldName = new javax.swing.JTextField();
        jLabel2 = new javax.swing.JLabel();
        buttonCancel = new javax.swing.JButton();
        buttonOk = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        listLayers = new javax.swing.JList<>();
        buttonTop = new javax.swing.JButton();
        buttonUp = new javax.swing.JButton();
        buttonDown = new javax.swing.JButton();
        buttonBottom = new javax.swing.JButton();
        buttonSort = new javax.swing.JButton();
        jLabel3 = new javax.swing.JLabel();
        buttonReset = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Edit Palette");

        jLabel1.setLabelFor(textFieldName);
        jLabel1.setText("Name:");

        textFieldName.setText("jTextField1");

        jLabel2.setLabelFor(listLayers);
        jLabel2.setText("Layer order:");

        buttonCancel.setText("Cancel");
        buttonCancel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonCancelActionPerformed(evt);
            }
        });

        buttonOk.setText("OK");
        buttonOk.setEnabled(false);
        buttonOk.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonOkActionPerformed(evt);
            }
        });

        listLayers.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_INTERVAL_SELECTION);
        jScrollPane1.setViewportView(listLayers);

        buttonTop.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/pepsoft/worldpainter/icons/top.png"))); // NOI18N
        buttonTop.setText("Top");
        buttonTop.setToolTipText("Move the selected layers to the top");
        buttonTop.setEnabled(false);
        buttonTop.setHorizontalAlignment(javax.swing.SwingConstants.LEADING);
        buttonTop.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonTopActionPerformed(evt);
            }
        });

        buttonUp.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/pepsoft/worldpainter/icons/up.png"))); // NOI18N
        buttonUp.setText("Up");
        buttonUp.setToolTipText("Move the selected layers up one row");
        buttonUp.setEnabled(false);
        buttonUp.setHorizontalAlignment(javax.swing.SwingConstants.LEADING);
        buttonUp.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonUpActionPerformed(evt);
            }
        });

        buttonDown.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/pepsoft/worldpainter/icons/down.png"))); // NOI18N
        buttonDown.setText("Down");
        buttonDown.setToolTipText("Move the selected layers down one row");
        buttonDown.setEnabled(false);
        buttonDown.setHorizontalAlignment(javax.swing.SwingConstants.LEADING);
        buttonDown.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonDownActionPerformed(evt);
            }
        });

        buttonBottom.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/pepsoft/worldpainter/icons/bottom.png"))); // NOI18N
        buttonBottom.setText("Bottom");
        buttonBottom.setToolTipText("Move the selected layers to the bottom");
        buttonBottom.setEnabled(false);
        buttonBottom.setHorizontalAlignment(javax.swing.SwingConstants.LEADING);
        buttonBottom.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonBottomActionPerformed(evt);
            }
        });

        buttonSort.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/pepsoft/worldpainter/icons/a_z.png"))); // NOI18N
        buttonSort.setText("Sort");
        buttonSort.setToolTipText("Sort all the layers alphabetically by name");
        buttonSort.setHorizontalAlignment(javax.swing.SwingConstants.LEADING);
        buttonSort.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonSortActionPerformed(evt);
            }
        });

        jLabel3.setFont(jLabel3.getFont().deriveFont((jLabel3.getFont().getStyle() | java.awt.Font.ITALIC)));
        jLabel3.setLabelFor(listLayers);
        jLabel3.setText("Hold shift to select multiple rows");

        buttonReset.setText("Reset");
        buttonReset.setEnabled(false);
        buttonReset.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonResetActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(buttonReset)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(buttonOk)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(buttonCancel))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 372, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(buttonBottom, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(buttonDown, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(buttonUp, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(buttonTop, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(buttonSort, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(textFieldName))
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel2)
                            .addComponent(jLabel3))
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(textFieldName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel3)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 206, Short.MAX_VALUE)
                        .addGap(18, 18, 18)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(buttonCancel)
                            .addComponent(buttonOk)
                            .addComponent(buttonReset)))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(buttonTop)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(buttonUp)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(buttonDown)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(buttonBottom)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(buttonSort)
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void buttonCancelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonCancelActionPerformed
        cancel();
    }//GEN-LAST:event_buttonCancelActionPerformed

    private void buttonOkActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonOkActionPerformed
        if (saveData()) {
            ok();
        }
    }//GEN-LAST:event_buttonOkActionPerformed

    private void buttonTopActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonTopActionPerformed
        moveSelectedLayers(0);
    }//GEN-LAST:event_buttonTopActionPerformed

    private void buttonUpActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonUpActionPerformed
        moveSelectedLayers(listLayers.getSelectedIndex() - 1);
    }//GEN-LAST:event_buttonUpActionPerformed

    private void buttonDownActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonDownActionPerformed
        moveSelectedLayers(listLayers.getSelectedIndex() + 1);
    }//GEN-LAST:event_buttonDownActionPerformed

    private void buttonBottomActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonBottomActionPerformed
        moveSelectedLayers(listLayers.getModel().getSize() - (listLayers.getMaxSelectionIndex() - listLayers.getMinSelectionIndex() + 1));
    }//GEN-LAST:event_buttonBottomActionPerformed

    private void buttonSortActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonSortActionPerformed
        sortLayers();
    }//GEN-LAST:event_buttonSortActionPerformed

    private void buttonResetActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonResetActionPerformed
        reset();
    }//GEN-LAST:event_buttonResetActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton buttonBottom;
    private javax.swing.JButton buttonCancel;
    private javax.swing.JButton buttonDown;
    private javax.swing.JButton buttonOk;
    private javax.swing.JButton buttonReset;
    private javax.swing.JButton buttonSort;
    private javax.swing.JButton buttonTop;
    private javax.swing.JButton buttonUp;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JList<CustomLayer> listLayers;
    private javax.swing.JTextField textFieldName;
    // End of variables declaration//GEN-END:variables

    private final PaletteManager paletteManager;
    private final Palette palette;
    private boolean orderChanged;
}