/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.layers.bo2;

import org.pepsoft.worldpainter.ColourScheme;
import org.pepsoft.worldpainter.objects.WPObject;

import javax.swing.*;
import javax.swing.JSpinner.NumberEditor;
import javax.vecmath.Point3i;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.pepsoft.minecraft.Block.BLOCK_TYPE_NAMES;
import static org.pepsoft.worldpainter.objects.WPObject.*;

/**
 *
 * @author pepijn
 */
public class EditObjectAttributes extends javax.swing.JDialog {
    public EditObjectAttributes(Window parent, WPObject object, ColourScheme colourScheme) {
        this(parent, Collections.singleton(object), colourScheme);
    }
    
    public EditObjectAttributes(Window parent, Collection<WPObject> objects, ColourScheme colourScheme) {
        super(parent, ModalityType.DOCUMENT_MODAL);
        this.objects = objects;
        this.colourScheme = colourScheme;
        
        if (objects.isEmpty()) {
            throw new IllegalArgumentException("Collection of objects may not be empty");
        }
        
        initComponents();
        
        // Set the spinner to not use thousands separators to make it slightly
        // smaller
        spinnerFrequency.setEditor(new NumberEditor(spinnerFrequency, "0"));
        
        if (objects.size() == 1) {
            WPObject object = objects.iterator().next();
            fieldName.setText(object.getName());
            file = object.getAttribute(ATTRIBUTE_FILE);
            if (file != null) {
                labelFile.setText(file.getAbsolutePath());
                if (! file.exists()) {
                    labelFile.setForeground(Color.RED);
                }
            }
            Point3i offset = object.getOffset();
            offsets.put(object, offset);
            String offsetStr = "<html><u>" + offset.x + ", " + offset.y + ", " + offset.z + "</u></html>";
            labelOffset.setText(offsetStr);
            checkBoxRandomRotation.setSelected(object.getAttribute(ATTRIBUTE_RANDOM_ROTATION));
            checkBoxRandomRotation.setTristateMode(false);
            checkBoxOnAir.setSelected(! object.getAttribute(ATTRIBUTE_NEEDS_FOUNDATION));
            checkBoxOnAir.setTristateMode(false);
            checkBoxUnderLava.setSelected(object.getAttribute(ATTRIBUTE_SPAWN_IN_LAVA));
            checkBoxUnderLava.setTristateMode(false);
            checkBoxUnderWater.setSelected(object.getAttribute(ATTRIBUTE_SPAWN_IN_WATER));
            checkBoxUnderWater.setTristateMode(false);
            checkBoxOnSolidLand.setSelected(object.getAttribute(ATTRIBUTE_SPAWN_ON_LAND));
            checkBoxOnSolidLand.setTristateMode(false);
            checkBoxOnWater.setSelected(object.getAttribute(ATTRIBUTE_SPAWN_ON_WATER));
            checkBoxOnWater.setTristateMode(false);
            checkBoxOnLava.setSelected(object.getAttribute(ATTRIBUTE_SPAWN_ON_LAVA));
            checkBoxOnLava.setTristateMode(false);
            // Remove "no change" choices
            ((DefaultComboBoxModel) comboBoxCollisionMode.getModel()).removeElementAt(0);
            ((DefaultComboBoxModel) comboBoxUndergroundMode.getModel()).removeElementAt(0);
            ((DefaultComboBoxModel) comboBoxLeafDecayMode.getModel()).removeElementAt(0);
            comboBoxCollisionMode.setSelectedIndex(object.getAttribute(ATTRIBUTE_COLLISION_MODE) - 1);
            comboBoxUndergroundMode.setSelectedIndex(object.getAttribute(ATTRIBUTE_UNDERGROUND_MODE) - 1);
            comboBoxLeafDecayMode.setSelectedIndex(object.getAttribute(ATTRIBUTE_LEAF_DECAY_MODE) - 1);
            spinnerFrequency.setValue(object.getAttribute(ATTRIBUTE_FREQUENCY));
            if (object.getAttribute(ATTRIBUTE_REPLACE_WITH_AIR) != null) {
                int[] replaceWithBlock = object.getAttribute(ATTRIBUTE_REPLACE_WITH_AIR);
                checkBoxReplace.setSelected(true);
                comboBoxReplaceBlockId.setSelectedIndex(replaceWithBlock[0]);
                spinnerReplaceData.setValue(replaceWithBlock[1]);
            } else {
                // Magenta wool by default
                comboBoxReplaceBlockId.setSelectedIndex(35);
            }
            checkBoxExtendFoundation.setSelected(object.getAttribute(ATTRIBUTE_EXTEND_FOUNDATION));
            checkBoxExtendFoundation.setTristateMode(false);
            WPObjectPreviewer previewer = new WPObjectPreviewer();
            previewer.setObject(object);
            jPanel1.add(previewer, BorderLayout.CENTER);
        } else {
            labelFile.setText(objects.size() + " objects selected");
            fieldName.setText("multiple");
            fieldName.setEnabled(false);
            file = null;
            long frequencyTotal = 0;
            int firstFrequency = -1;
            boolean allFrequenciesIdentical = true;
            Point3i origin = new Point3i();
            for (WPObject object: objects) {
                if (! object.getOffset().equals(origin)) {
                    offsets.put(object, object.getOffset());
                }
                int frequency = object.getAttribute(ATTRIBUTE_FREQUENCY);
                frequencyTotal += frequency;
                if (firstFrequency == -1) {
                    firstFrequency = frequency;
                } else if (frequency != firstFrequency) {
                    allFrequenciesIdentical = false;
                }
            }
            labelOffset.setText("multiple");
            checkBoxRandomRotation.setMixed(true);
            checkBoxOnAir.setMixed(true);
            checkBoxUnderLava.setMixed(true);
            checkBoxUnderWater.setMixed(true);
            checkBoxOnSolidLand.setMixed(true);
            checkBoxOnWater.setMixed(true);
            checkBoxOnLava.setMixed(true);
            labelOffset.setCursor(null);
            labelOffset.setForeground(null);
            int averageFrequency = (int) (frequencyTotal / objects.size());
            spinnerFrequency.setValue(averageFrequency);
            if (! allFrequenciesIdentical) {
                checkBoxFrequencyActive.setSelected(false);
                checkBoxFrequencyActive.setToolTipText("<html>The relative frequencies of the selected objects are not all the same.<br>Check the checkbox if you want to set them all to the same value.</html>");
                checkBoxFrequencyActive.setEnabled(true);
                spinnerFrequency.setEnabled(false);
            }
            checkBoxReplace.setEnabled(false);
            checkBoxExtendFoundation.setMixed(true);
        }
        pack();
        
        ActionMap actionMap = rootPane.getActionMap();
        actionMap.put("cancel", new AbstractAction("cancel") {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
            
            private static final long serialVersionUID = 1L;
        });

        InputMap inputMap = rootPane.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "cancel");
        
        getRootPane().setDefaultButton(buttonOK);
        
        setLocationRelativeTo(parent);
        
        setControlStates();
    }

    public boolean isCancelled() {
        return cancelled;
    }

    private void editOffset() {
        if (objects.size() > 1) {
            return;
        }
        WPObject object = objects.iterator().next();
        Point3i offset = offsets.get(object);
        OffsetEditor dialog = new OffsetEditor(this, (offset != null) ? offset : new Point3i(), object, colourScheme);
        dialog.setVisible(true);
        if (! dialog.isCancelled()) {
            offset = dialog.getOffset();
            offsets.put(object, offset);
            String offsetStr = "<html><u>" + offset.x + ", " + offset.y + ", " + offset.z + "</u></html>";
            labelOffset.setText(offsetStr);
        }
    }

    private void ok() {
        boolean singleSelection = objects.size() == 1;
        for (WPObject object: objects) {
            if (singleSelection && (! fieldName.getText().trim().isEmpty())) {
                object.setName(fieldName.getText().trim());
            }
            Map<String, Serializable> attributes = object.getAttributes();
            if (attributes == null) {
                attributes = new HashMap<>();
            }
            if (checkBoxFrequencyActive.isSelected()) {
                int frequency = (Integer) spinnerFrequency.getValue();
                if (frequency != 100) {
                    attributes.put(ATTRIBUTE_FREQUENCY.key, frequency);
                } else {
                    attributes.remove(ATTRIBUTE_FREQUENCY.key);
                }
            }
            Point3i offset = offsets.get(object);
            if ((offset != null) && ((offset.x != 0) || (offset.y != 0) || (offset.z != 0))) {
                attributes.put(ATTRIBUTE_OFFSET.key, offset);
            } else {
                attributes.remove(ATTRIBUTE_OFFSET.key);
            }
            if (! checkBoxRandomRotation.isMixed()) {
                attributes.put(ATTRIBUTE_RANDOM_ROTATION.key, checkBoxRandomRotation.isSelected());
            }
            if (! checkBoxOnAir.isMixed()) {
                attributes.put(ATTRIBUTE_NEEDS_FOUNDATION.key, ! checkBoxOnAir.isSelected());
            }
            if (! checkBoxUnderLava.isMixed()) {
                attributes.put(ATTRIBUTE_SPAWN_IN_LAVA.key, checkBoxUnderLava.isSelected());
            }
            if (! checkBoxUnderWater.isMixed()) {
                attributes.put(ATTRIBUTE_SPAWN_IN_WATER.key, checkBoxUnderWater.isSelected());
            }
            if (! checkBoxOnSolidLand.isMixed()) {
                attributes.put(ATTRIBUTE_SPAWN_ON_LAND.key, checkBoxOnSolidLand.isSelected());
            }
            if (! checkBoxOnWater.isMixed()) {
                attributes.put(ATTRIBUTE_SPAWN_ON_WATER.key, checkBoxOnWater.isSelected());
            }
            if (! checkBoxOnLava.isMixed()) {
                attributes.put(ATTRIBUTE_SPAWN_ON_LAVA.key, checkBoxOnLava.isSelected());
            }
            if (singleSelection || comboBoxCollisionMode.getSelectedIndex() > 0) {
                attributes.put(ATTRIBUTE_COLLISION_MODE.key, comboBoxCollisionMode.getSelectedIndex() + (singleSelection ? 1 : 0));
            }
            if (singleSelection || comboBoxUndergroundMode.getSelectedIndex() > 0) {
                attributes.put(ATTRIBUTE_UNDERGROUND_MODE.key, comboBoxUndergroundMode.getSelectedIndex() + (singleSelection ? 1 : 0));
            }
            if (singleSelection || comboBoxLeafDecayMode.getSelectedIndex() > 0) {
                attributes.put(ATTRIBUTE_LEAF_DECAY_MODE.key, comboBoxLeafDecayMode.getSelectedIndex() + (singleSelection ? 1 : 0));
            }
            if (singleSelection) {
                if (checkBoxReplace.isSelected()) {
                    attributes.put(ATTRIBUTE_REPLACE_WITH_AIR.key, new int[] {comboBoxReplaceBlockId.getSelectedIndex(), (Integer) spinnerReplaceData.getValue()});
                } else {
                    attributes.remove(ATTRIBUTE_REPLACE_WITH_AIR.key);
                }
            }
            if (! checkBoxExtendFoundation.isMixed()) {
                attributes.put(ATTRIBUTE_EXTEND_FOUNDATION.key, checkBoxExtendFoundation.isSelected());
            }
            if (! attributes.isEmpty()) {
                object.setAttributes(attributes);
            } else {
                object.setAttributes(null);
            }
        }
        cancelled = false;
        dispose();
    }

    private void autoOffset() {
        boolean singleSelection = objects.size() == 1;
        for (WPObject object: objects) {
            int offsetZ = Integer.MIN_VALUE, lowestX = 0, highestX = 0, lowestY = 0, highestY = 0;
            Point3i dimensions = object.getDimensions();
            for (int z = 0; (z < dimensions.z) && (offsetZ == Integer.MIN_VALUE); z++) {
                for (int x = 0; x < dimensions.x; x++) {
                    for (int y = 0; y < dimensions.y; y++) {
                        if (object.getMask(x, y, z)) {
                            if (offsetZ == Integer.MIN_VALUE) {
                                offsetZ = z;
                                lowestX = highestX = x;
                                lowestY = highestY = y;
                            } else {
                                if (x < lowestX) {
                                    lowestX = x;
                                } else if (x > highestX) {
                                    highestX = x;
                                }
                                if (y < lowestY) {
                                    lowestY = y;
                                } else if (y > highestY) {
                                    highestY = y;
                                }
                            }
                        }
                    }
                }
            }
            if (offsetZ == Integer.MIN_VALUE) {
                // This object has size zero or consists of nothing but air!
                offsets.clear();
                if (singleSelection) {
                    labelOffset.setText("<html><u>0, 0, 0</u></html>");
                }
            } else {
                Point3i offset = new Point3i(-(lowestX + highestX) / 2, -(lowestY + highestY) / 2, -offsetZ);
                offsets.put(object, offset);
                if (singleSelection) {
                    String offsetStr = "<html><u>" + offset.x + ", " + offset.y + ", " + offset.z + "</u></html>";
                    labelOffset.setText(offsetStr);
                }
            }
        }
        if (! singleSelection) {
            JOptionPane.showMessageDialog(this, objects.size() + " offsets autoset");
        }
    }

    private void resetOffset() {
        offsets.clear();
        boolean singleSelection = objects.size() == 1;
        if (singleSelection) {
            labelOffset.setText("<html><u>0, 0, 0</u></html>");
        } else {
            JOptionPane.showMessageDialog(this, objects.size() + " offsets reset");
        }
    }
    
    private void setControlStates() {
        boolean replaceBlocks = checkBoxReplace.isSelected();
        comboBoxReplaceBlockId.setEnabled(replaceBlocks);
        spinnerReplaceData.setEnabled(replaceBlocks);
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
        jLabel2 = new javax.swing.JLabel();
        labelFile = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        labelOffset = new javax.swing.JLabel();
        buttonOffsetAuto = new javax.swing.JButton();
        buttonOffsetReset = new javax.swing.JButton();
        buttonCancel = new javax.swing.JButton();
        buttonOK = new javax.swing.JButton();
        fieldName = new javax.swing.JTextField();
        jPanel1 = new javax.swing.JPanel();
        jLabel4 = new javax.swing.JLabel();
        spinnerFrequency = new javax.swing.JSpinner();
        jLabel5 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        comboBoxCollisionMode = new javax.swing.JComboBox();
        jLabel7 = new javax.swing.JLabel();
        comboBoxUndergroundMode = new javax.swing.JComboBox();
        jLabel8 = new javax.swing.JLabel();
        checkBoxRandomRotation = new org.pepsoft.worldpainter.util.TristateCheckBox();
        checkBoxOnSolidLand = new org.pepsoft.worldpainter.util.TristateCheckBox();
        checkBoxOnAir = new org.pepsoft.worldpainter.util.TristateCheckBox();
        checkBoxOnWater = new org.pepsoft.worldpainter.util.TristateCheckBox();
        checkBoxUnderWater = new org.pepsoft.worldpainter.util.TristateCheckBox();
        checkBoxUnderLava = new org.pepsoft.worldpainter.util.TristateCheckBox();
        checkBoxOnLava = new org.pepsoft.worldpainter.util.TristateCheckBox();
        checkBoxFrequencyActive = new javax.swing.JCheckBox();
        jLabel9 = new javax.swing.JLabel();
        comboBoxLeafDecayMode = new javax.swing.JComboBox();
        checkBoxReplace = new javax.swing.JCheckBox();
        jLabel10 = new javax.swing.JLabel();
        comboBoxReplaceBlockId = new javax.swing.JComboBox();
        jLabel11 = new javax.swing.JLabel();
        spinnerReplaceData = new javax.swing.JSpinner();
        checkBoxExtendFoundation = new org.pepsoft.worldpainter.util.TristateCheckBox();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Edit Object Attributes");

        jLabel1.setText("Name:");

        jLabel2.setText("File:");

        labelFile.setText("jLabel3");

        jLabel3.setText("Offset:");

        labelOffset.setForeground(new java.awt.Color(0, 0, 255));
        labelOffset.setText("<html><u>offset</u></html>");
        labelOffset.setToolTipText("Click to edit the offset.");
        labelOffset.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        labelOffset.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                labelOffsetMouseClicked(evt);
            }
        });

        buttonOffsetAuto.setText("Auto");
        buttonOffsetAuto.setToolTipText("This will try to set the offset to the base of the object.");
        buttonOffsetAuto.setHorizontalTextPosition(javax.swing.SwingConstants.LEADING);
        buttonOffsetAuto.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonOffsetAutoActionPerformed(evt);
            }
        });

        buttonOffsetReset.setText("Zero");
        buttonOffsetReset.setToolTipText("This will set the offset to all zeroes.");
        buttonOffsetReset.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonOffsetResetActionPerformed(evt);
            }
        });

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

        fieldName.setColumns(20);
        fieldName.setText("jTextField1");

        jPanel1.setLayout(new java.awt.BorderLayout());

        jLabel4.setText("Relative frequency:");
        jLabel4.setToolTipText("The frequency of this object relative to the other objects in the layer.");

        spinnerFrequency.setModel(new javax.swing.SpinnerNumberModel(100, 1, 9999, 1));
        spinnerFrequency.setToolTipText("The frequency of this object relative to the other objects in the layer.");

        jLabel5.setText("%");

        jLabel6.setText("Collide with:");
        jLabel6.setToolTipText("<html>Determines which existing blocks an object will collide with (and therefore not be rendered).<br>\n<strong>Note</strong> that only above ground blocks are considered!</html>");

        comboBoxCollisionMode.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "no change", "any blocks", "solid blocks", "nothing" }));
        comboBoxCollisionMode.setToolTipText("<html>Determines which existing blocks an object will collide with (and therefore not be rendered).<br>\n<strong>Note</strong> that only above ground blocks are considered!</html>");

        jLabel7.setText("Replace underground blocks:");
        jLabel7.setToolTipText("Determines whether existing underground blocks should be replaced by blocks from the object.");

        comboBoxUndergroundMode.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "no change", "always", "if object block is solid", "if existing block is air" }));
        comboBoxUndergroundMode.setToolTipText("Determines whether existing underground blocks should be replaced by blocks from the object.");

        jLabel8.setText("Spawn:");

        checkBoxRandomRotation.setText("random rotation and mirroring");

        checkBoxOnSolidLand.setText("on solid land");

        checkBoxOnAir.setText("on air");

        checkBoxOnWater.setText("on water");

        checkBoxUnderWater.setText("under water");

        checkBoxUnderLava.setText("under lava");

        checkBoxOnLava.setText("on lava");

        checkBoxFrequencyActive.setSelected(true);
        checkBoxFrequencyActive.setText(" ");
        checkBoxFrequencyActive.setEnabled(false);
        checkBoxFrequencyActive.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                checkBoxFrequencyActiveActionPerformed(evt);
            }
        });

        jLabel9.setText("Leaf blocks should:");

        comboBoxLeafDecayMode.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "no change", "behave as exported", "decay", "not decay" }));

        checkBoxReplace.setText("replace with air:");
        checkBoxReplace.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                checkBoxReplaceActionPerformed(evt);
            }
        });

        jLabel10.setText("block ID:");

        comboBoxReplaceBlockId.setModel(new DefaultComboBoxModel(BLOCK_TYPES)
        );
        comboBoxReplaceBlockId.setEnabled(false);

        jLabel11.setText(", data:");

        spinnerReplaceData.setModel(new javax.swing.SpinnerNumberModel(2, 0, 15, 1));
        spinnerReplaceData.setEnabled(false);

        checkBoxExtendFoundation.setText("extend foundation to ground");

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
                                .addComponent(fieldName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jLabel3)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(labelOffset, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(18, 18, 18)
                                .addComponent(buttonOffsetAuto)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(buttonOffsetReset))
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jLabel4)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(checkBoxFrequencyActive)
                                .addGap(0, 0, 0)
                                .addComponent(spinnerFrequency, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(0, 0, 0)
                                .addComponent(jLabel5))
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jLabel6)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(comboBoxCollisionMode, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jLabel7)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(comboBoxUndergroundMode, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addComponent(checkBoxRandomRotation, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jLabel8)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(checkBoxOnSolidLand, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(checkBoxUnderWater, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(checkBoxUnderLava, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(checkBoxOnAir, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(checkBoxOnWater, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(checkBoxOnLava, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jLabel9)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(comboBoxLeafDecayMode, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(checkBoxReplace)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabel10)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(comboBoxReplaceBlockId, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(0, 0, 0)
                                .addComponent(jLabel11)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(spinnerReplaceData, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addComponent(checkBoxExtendFoundation, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(18, 18, 18)
                        .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, 256, Short.MAX_VALUE))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                .addComponent(jLabel2)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(labelFile))
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                .addComponent(buttonOK)
                                .addGap(77, 77, 77))
                            .addComponent(buttonCancel, javax.swing.GroupLayout.Alignment.TRAILING))))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(labelFile))
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel1)
                            .addComponent(fieldName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(18, 18, 18)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel3)
                            .addComponent(labelOffset, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(buttonOffsetAuto)
                            .addComponent(buttonOffsetReset))
                        .addGap(18, 18, 18)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel4)
                            .addComponent(spinnerFrequency, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel5)
                            .addComponent(checkBoxFrequencyActive))
                        .addGap(18, 18, 18)
                        .addComponent(checkBoxRandomRotation, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel8)
                            .addComponent(checkBoxOnSolidLand, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(checkBoxOnAir, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(checkBoxUnderWater, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(checkBoxOnWater, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(checkBoxUnderLava, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(checkBoxOnLava, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(18, 18, 18)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel6)
                            .addComponent(comboBoxCollisionMode, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel7)
                            .addComponent(comboBoxUndergroundMode, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel9)
                            .addComponent(comboBoxLeafDecayMode, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(18, 18, 18)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(checkBoxReplace)
                            .addComponent(jLabel10)
                            .addComponent(comboBoxReplaceBlockId, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel11)
                            .addComponent(spinnerReplaceData, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(18, 18, 18)
                        .addComponent(checkBoxExtendFoundation, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(buttonOK)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(buttonCancel)
                        .addContainerGap())))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void labelOffsetMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_labelOffsetMouseClicked
        editOffset();
    }//GEN-LAST:event_labelOffsetMouseClicked

    private void buttonCancelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonCancelActionPerformed
        dispose();
    }//GEN-LAST:event_buttonCancelActionPerformed

    private void buttonOffsetAutoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonOffsetAutoActionPerformed
        autoOffset();
    }//GEN-LAST:event_buttonOffsetAutoActionPerformed

    private void buttonOffsetResetActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonOffsetResetActionPerformed
        resetOffset();
    }//GEN-LAST:event_buttonOffsetResetActionPerformed

    private void buttonOKActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonOKActionPerformed
        ok();
    }//GEN-LAST:event_buttonOKActionPerformed

    private void checkBoxFrequencyActiveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_checkBoxFrequencyActiveActionPerformed
        spinnerFrequency.setEnabled(checkBoxFrequencyActive.isSelected());
    }//GEN-LAST:event_checkBoxFrequencyActiveActionPerformed

    private void checkBoxReplaceActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_checkBoxReplaceActionPerformed
        setControlStates();
    }//GEN-LAST:event_checkBoxReplaceActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton buttonCancel;
    private javax.swing.JButton buttonOK;
    private javax.swing.JButton buttonOffsetAuto;
    private javax.swing.JButton buttonOffsetReset;
    private org.pepsoft.worldpainter.util.TristateCheckBox checkBoxExtendFoundation;
    private javax.swing.JCheckBox checkBoxFrequencyActive;
    private org.pepsoft.worldpainter.util.TristateCheckBox checkBoxOnAir;
    private org.pepsoft.worldpainter.util.TristateCheckBox checkBoxOnLava;
    private org.pepsoft.worldpainter.util.TristateCheckBox checkBoxOnSolidLand;
    private org.pepsoft.worldpainter.util.TristateCheckBox checkBoxOnWater;
    private org.pepsoft.worldpainter.util.TristateCheckBox checkBoxRandomRotation;
    private javax.swing.JCheckBox checkBoxReplace;
    private org.pepsoft.worldpainter.util.TristateCheckBox checkBoxUnderLava;
    private org.pepsoft.worldpainter.util.TristateCheckBox checkBoxUnderWater;
    private javax.swing.JComboBox comboBoxCollisionMode;
    private javax.swing.JComboBox comboBoxLeafDecayMode;
    private javax.swing.JComboBox comboBoxReplaceBlockId;
    private javax.swing.JComboBox comboBoxUndergroundMode;
    private javax.swing.JTextField fieldName;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JLabel labelFile;
    private javax.swing.JLabel labelOffset;
    private javax.swing.JSpinner spinnerFrequency;
    private javax.swing.JSpinner spinnerReplaceData;
    // End of variables declaration//GEN-END:variables

    private final Collection<WPObject> objects;
    private final File file;
    private final Map<WPObject, Point3i> offsets = new HashMap<>();
    private final ColourScheme colourScheme;
    private boolean cancelled = true;
    
    private static final String[] BLOCK_TYPES = new String[256];
    
    static {
        for (int i = 0; i < 256; i++) {
            if ((i >= BLOCK_TYPE_NAMES.length) || (BLOCK_TYPE_NAMES[i] == null)) {
                BLOCK_TYPES[i] = Integer.toString(i);
            } else {
                BLOCK_TYPES[i] = i + " " + BLOCK_TYPE_NAMES[i];
            }
        }
    }
    
    private static final long serialVersionUID = 1L;
}