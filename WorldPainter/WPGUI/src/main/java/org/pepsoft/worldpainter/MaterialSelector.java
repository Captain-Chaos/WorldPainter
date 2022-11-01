/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter;

import org.pepsoft.minecraft.Material;
import org.pepsoft.util.DesktopUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

import static org.pepsoft.minecraft.Block.BLOCKS;
import static org.pepsoft.minecraft.Material.MINECRAFT;
import static org.pepsoft.worldpainter.Platform.Capability.NAME_BASED;

/**
 * A GUI control for selecting a Minecraft material. Supports both legacy
 * (pre-1.13) and modern (1.13 and later materials.
 *
 * @author Pepijn
 */
public class MaterialSelector extends javax.swing.JPanel {
    /**
     * Creates new form MaterialEditor
     */
    @SuppressWarnings("UseOfObsoleteCollectionType") // Tell that to Swing
    public MaterialSelector() {
        initComponents();
        
        Vector<String> minecraftNames = new Vector<>(Material.getAllSimpleNamesForNamespace(Material.MINECRAFT));
        Collections.sort(minecraftNames);
        comboBoxMinecraftName.setModel(new DefaultComboBoxModel<>(minecraftNames));
        
        Vector<String> namespaces = new Vector<>(Material.getAllNamespaces());
        Collections.sort(namespaces);
        comboBoxNamespace.setModel(new DefaultComboBoxModel<>(namespaces));

        String[] blockIds = new String[256];
        for (int i = 0; i < blockIds.length; i++) {
            if (BLOCKS[i].name != null) {
                blockIds[i] = i + ": " + BLOCKS[i].name;
            } else {
                blockIds[i] = Integer.toString(i);
            }
        }
        comboBoxBlockType.setModel(new DefaultComboBoxModel<>(blockIds));
    }
    
    public void setMaterial(Material material) {
        programmaticChange = true;
        try {
            this.material = material;
            namespace = material.namespace;
            simpleName = material.simpleName;
            loadActualProperties();
            if (namespace.equals(Material.MINECRAFT) || ((platform != null) && (! platform.capabilities.contains(NAME_BASED)))) {
                radioButtonMinecraft.setSelected(true);
                int blockType = material.blockType;
                if (blockType >= 0) {
                    comboBoxBlockType.setSelectedIndex(blockType);
                    spinnerDataValue.setValue(material.data);
                } else {
                    comboBoxBlockType.setSelectedItem(null);
                    spinnerDataValue.setValue(0);
                }
                updateModernIds();
            } else {
                radioButtonCustom.setSelected(true);
                comboBoxNamespace.setSelectedItem(namespace);
                comboBoxCustomName.setSelectedItem(simpleName);
                comboBoxBlockType.setSelectedItem(null);
                spinnerDataValue.setValue(0);
                comboBoxMinecraftName.setSelectedItem(null);
                updateLegacyIds();
            }
            setControlStates();
        } finally {
            programmaticChange = false;
        }
    }
    
    public Material getMaterial() {
        updateMaterial();
        return material;
    }

    public boolean isExtendedBlockIds() {
        return extendedBlockIds;
    }

    public void setExtendedBlockIds(boolean extendedBlockIds) {
        if (extendedBlockIds != this.extendedBlockIds) {
            this.extendedBlockIds = extendedBlockIds;
            String[] blockIds = new String[extendedBlockIds ? 4096 : 256];
            for (int i = 0; i < blockIds.length; i++) {
                if (BLOCKS[i].name != null) {
                    blockIds[i] = i + ": " + BLOCKS[i].name;
                } else {
                    blockIds[i] = Integer.toString(i);
                }
            }
            comboBoxBlockType.setModel(new DefaultComboBoxModel<>(blockIds));
        }
    }

    public Platform getPlatform() {
        return platform;
    }

    public void setPlatform(Platform platform) {
        if (! Objects.equals(platform, this.platform)) {
            this.platform = platform;
            setControlStates();
        }
    }

    /**
     * Load the properties of the current material into the properties panel.
     */
    private void loadActualProperties() {
        properties = (material.getProperties() != null) ? new HashMap<>(material.getProperties()) : null;
        updateProperties();
    }

    /**
     * Load the default properties (the properties of an arbitrary block with
     * the current namespace and simple name) into the properties panel.
     */
    private void loadDefaultProperties() {
        Material defaultMaterial = Material.getPrototype(namespace + ":" + simpleName);
        properties = (defaultMaterial.getProperties() != null) ? new HashMap<>(defaultMaterial.getProperties()) : null;
        updateProperties();
    }

    /**
     * Update the properties panel to reflect the current properties.
     */
    private void updateProperties() {
        panelProperties.removeAll();
        propertyEditors.clear();
        if (properties != null) {
            for (Map.Entry<String, String> entry: properties.entrySet()) {
                final String name = entry.getKey();
                final String value = entry.getValue();
                final Material.PropertyDescriptor descriptor = (material.propertyDescriptors != null) ? material.propertyDescriptors.get(name) : null;
                if (descriptor != null) {
                    switch (descriptor.type) {
                        case BOOLEAN:
                            addBooleanProperty(name, Boolean.parseBoolean(value), false);
                            break;
                        case INTEGER:
                            addIntProperty(name, descriptor.minValue, Integer.parseInt(value), descriptor.maxValue, false);
                            break;
                        case ENUM:
                            addStringProperty(name, value, descriptor.enumValues, false);
                            break;
                        default:
                            throw new IllegalArgumentException("Unknown property type: " + descriptor.type);
                    }
                } else {
                    // No idea; just fall back to a string
                    addStringProperty(name, value, null, false);
                }
            }
//            GridBagConstraints constraints = new GridBagConstraints();
//            constraints.gridwidth = GridBagConstraints.REMAINDER;
//            constraints.weighty = 1.0;
//            panelProperties.add(Box.createGlue(), constraints);
        }
        Window parentWindow = SwingUtilities.windowForComponent(this);
        if (parentWindow != null) {
            parentWindow.validate();
            repaint();
        }
    }

    /**
     * Update the legacy block ID and data value to match the current material,
     * if appropriate.
     */
    private void updateLegacyIds() {
        if (material.blockType >= 0) {
            comboBoxBlockType.setSelectedIndex(material.blockType);
            spinnerDataValue.setValue(material.data);
        } else {
            comboBoxBlockType.setSelectedItem(null);
            spinnerDataValue.setValue(0);
        }
    }

    /**
     * Update the modern ID fields to match the current material.
     */
    private void updateModernIds() {
        if (namespace.equals(Material.MINECRAFT)) {
            comboBoxMinecraftName.setSelectedItem(simpleName);
            comboBoxNamespace.setSelectedItem(null);
            comboBoxCustomName.setSelectedItem(null);
        } else {
            comboBoxMinecraftName.setSelectedItem(null);
            comboBoxNamespace.setSelectedItem(namespace);
            comboBoxCustomName.setSelectedItem(simpleName);
        }
    }

    /**
     * Add a string-typed property to the properties panel.
     *
     * @param key The key of the property.
     * @param value The initial value of the property.
     * @param values Optionally, the possible values of the property.
     * @param focus Whether the new field should receive the keyboard focus.
     */
    private void addStringProperty(String key, String value, String[] values, boolean focus) {
        if (propertyEditors.containsKey(key)) {
            throw new IllegalStateException("Property " + key + " already present");
        }
        JComponent control;
        if (values != null) {
            control = new JComboBox<>(values);
            ((JComboBox<?>) control).setSelectedItem(value);
        } else {
            control = new JTextField(value, 15);
        }
        control.setEnabled(propertiesEnabled);
        propertyEditors.put(key, control);
        if (control instanceof JComboBox) {
            ((JComboBox<?>) control).addActionListener(e -> {
                if (properties == null) {
                    properties = new HashMap<>();
                }
                properties.put(key, (String) ((JComboBox<?>) control).getSelectedItem());
                updateMaterial();
            });
        } else {
            control.addFocusListener(new FocusAdapter() {
                @Override
                public void focusLost(FocusEvent e) {
                    if (properties == null) {
                        properties = new HashMap<>();
                    }
                    properties.put(key, ((JTextField) control).getText());
                    updateMaterial();
                }
            });
        }
        JLabel label = new JLabel(uppercaseFirst(key) + ':');
        label.setLabelFor(control);
        addControlsRow(label, control);
        if (focus) {
            control.requestFocusInWindow();
        }
    }

    /**
     * Add a integer-typed property to the properties panel.
     *
     * @param key The key of the property.
     * @param minValue The minimum value of the property.
     * @param value The initial value of the property.
     * @param maxValue The maximum value of the property.
     * @param focus Whether the new field should receive the keyboard focus.
     */
    private void addIntProperty(String key, int minValue, int value, int maxValue, boolean focus) {
        if (propertyEditors.containsKey(key)) {
            throw new IllegalStateException("Property " + key + " already present");
        }
        JSpinner control = new JSpinner(new SpinnerNumberModel(value, minValue, maxValue, 1));
        control.setEnabled(propertiesEnabled);
        propertyEditors.put(key, control);
        control.addChangeListener(e -> {
            if (properties == null) {
                properties = new HashMap<>();
            }
            properties.put(key, Integer.toString((Integer) control.getValue()));
            updateMaterial();
        });
        JLabel label = new JLabel(uppercaseFirst(key) + ':');
        label.setLabelFor(control);
        addControlsRow(label, control); // TODO: determine actual bounds, if any
        if (focus) {
            control.requestFocusInWindow();
        }
    }

    /**
     * Add a boolean-typed property to the properties panel.
     *
     * @param key The key of the property.
     * @param value The initial value of the property.
     * @param focus Whether the new field should receive the keyboard focus.
     */
    private void addBooleanProperty(String key, boolean value, boolean focus) {
        if (propertyEditors.containsKey(key)) {
            throw new IllegalStateException("Property " + key + " already present");
        }
        // Use a zero width space as the text so that the checkbox aligns to the
        // base line of the label
        JCheckBox control = new JCheckBox("\u200b", value);
        control.setEnabled(propertiesEnabled);
        propertyEditors.put(key, control);
        control.addActionListener(e -> {
            if (properties == null) {
                properties = new HashMap<>();
            }
            properties.put(key, Boolean.toString(control.isSelected()));
            updateMaterial();
        });
        JLabel label = new JLabel(uppercaseFirst(key) + ':');
        label.setLabelFor(control);
        addControlsRow(label, control);
        if (focus) {
            control.requestFocusInWindow();
        }
    }

    /**
     * Change the first letter of a string to uppercase.
     *
     * @param str The string to change.
     * @return The string with its first letter changed to uppercase.
     */
    private String uppercaseFirst(String str) {
        return str.isEmpty() ? str : Character.toUpperCase(str.charAt(0)) + str.substring(1);
    }

    /**
     * Add a left-adjusted row of AWT contols to the properties panel.
     *
     * @param controls The controls to add.
     */
    private void addControlsRow(Component... controls) {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.anchor = GridBagConstraints.BASELINE_LEADING;
        constraints.insets = new Insets(2, 2, 0, 0);
        for (Component control: controls) {
            panelProperties.add(control, constraints);
        }
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.weightx = 1.0;
        panelProperties.add(Box.createGlue(), constraints);
    }

    /**
     * Interactively add a property to {@link #properties} and the properties
     * panel.
     */
    private void addProperty() {
        final String name = JOptionPane.showInputDialog(this, "Enter the name of the property to add:", "Add Property", JOptionPane.QUESTION_MESSAGE);
        if ((name != null) && (! name.trim().isEmpty())) {
            addStringProperty(name.trim(), "", null, true);
            SwingUtilities.windowForComponent(this).validate();
            repaint();
        }
    }

    /**
     * Make sure all controls are correctly enabled or disabled according to
     * their current settings.
     */
    private void setControlStates() {
        boolean minecraft = radioButtonMinecraft.isSelected();
        boolean platformHasNames = (platform == null) || platform.capabilities.contains(NAME_BASED);
        radioButtonMinecraft.setEnabled(platformHasNames);
        radioButtonCustom.setEnabled(platformHasNames);
        comboBoxBlockType.setEnabled(minecraft);
        spinnerDataValue.setEnabled(minecraft);
        comboBoxMinecraftName.setEnabled(minecraft && platformHasNames);
        comboBoxNamespace.setEnabled(! minecraft);
        comboBoxCustomName.setEnabled(! minecraft);
        buttonAddProperty.setEnabled(! minecraft);
        propertiesEnabled = platformHasNames;
        for (Component control: propertyEditors.values()) {
            control.setEnabled(propertiesEnabled);
        }
    }

    /**
     * Update the current material to a legacy material based on the currently
     * selected block ID and data value.
     */
    private void blockIdOrDataChanged() {
        int blockType = comboBoxBlockType.getSelectedIndex();
        int dataValue = (Integer) spinnerDataValue.getValue();
        if ((blockType < 0) || (blockType > 4095) || (dataValue < 0) || (dataValue > 15)) {
            // No idea why this happens, but it has been observed in the wild TODO find out why and fix the underlying
            //  cause
            logger.error("blockIdOrDataChanged(): blockType = {}, dataValue = {}, comboBoxMinecraftName.selectedItem = {}, comboBoxNamespace.selectedItem = {}, comboBoxCustomName.selectedItem = {}", blockType, dataValue, comboBoxMinecraftName.getSelectedItem(), comboBoxNamespace.getSelectedItem(), comboBoxCustomName.getSelectedItem());
            return;
        }
        material = Material.get(blockType, dataValue);
        namespace = material.namespace;
        simpleName = material.simpleName;
        loadActualProperties();
        updateModernIds();
        firePropertyChange("material", null, getMaterial());
    }

    /**
     * Update the current material to a modern material based on the Minecraft
     * namespace currently
     * selected namespace and simple name.
     */
    private void minecraftNameChanged() {
        namespace = Material.MINECRAFT;
        simpleName = (String) comboBoxMinecraftName.getSelectedItem();
        material = Material.getPrototype(namespace + ':' + simpleName);
        loadDefaultProperties();
        updateLegacyIds();
        firePropertyChange("material", null, getMaterial());
    }
    
    private void updateMaterial() {
        final Material oldMaterial = material;
        programmaticChange = true;
        try {
            if (radioButtonCustom.isSelected()) {
                // Make sure to finish editing the custom name, even if the field still has the keyboard focus
                simpleName = (String) comboBoxCustomName.getSelectedItem();
            }
            if ((simpleName != null) && (! simpleName.trim().isEmpty())) {
                if ((namespace == null) || namespace.trim().isEmpty()) {
                    namespace = Material.MINECRAFT;
                }
                material = Material.get(namespace.trim() + ':' + simpleName.trim(), properties);
                if (material != oldMaterial) {
                    if (namespace.equals(Material.MINECRAFT)) {
                        updateLegacyIds();
                    }
                    firePropertyChange("material", null, material);
                }
            }
        } finally {
            programmaticChange = false;
        }
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
        radioButtonCustom = new javax.swing.JRadioButton();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        comboBoxMinecraftName = new javax.swing.JComboBox<>();
        buttonAddProperty = new javax.swing.JButton();
        comboBoxNamespace = new javax.swing.JComboBox<>();
        radioButtonMinecraft = new javax.swing.JRadioButton();
        jLabel3 = new javax.swing.JLabel();
        comboBoxBlockType = new javax.swing.JComboBox<>();
        jLabel4 = new javax.swing.JLabel();
        spinnerDataValue = new javax.swing.JSpinner();
        jLabel5 = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        jLabel9 = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        panelProperties = new javax.swing.JPanel();
        comboBoxCustomName = new javax.swing.JComboBox<>();

        buttonGroup1.add(radioButtonCustom);
        radioButtonCustom.setText("<html><em>Custom:</em></html>");
        radioButtonCustom.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                radioButtonCustomActionPerformed(evt);
            }
        });

        jLabel1.setText(":");

        jLabel2.setText("minecraft:");

        comboBoxMinecraftName.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        comboBoxMinecraftName.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                comboBoxMinecraftNameActionPerformed(evt);
            }
        });

        buttonAddProperty.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/pepsoft/worldpainter/icons/brick_add.png"))); // NOI18N
        buttonAddProperty.setToolTipText("Add a property");
        buttonAddProperty.setEnabled(false);
        buttonAddProperty.setMargin(new java.awt.Insets(2, 2, 2, 2));
        buttonAddProperty.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonAddPropertyActionPerformed(evt);
            }
        });

        comboBoxNamespace.setEditable(true);
        comboBoxNamespace.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        comboBoxNamespace.setEnabled(false);
        comboBoxNamespace.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                comboBoxNamespaceActionPerformed(evt);
            }
        });

        buttonGroup1.add(radioButtonMinecraft);
        radioButtonMinecraft.setSelected(true);
        radioButtonMinecraft.setText("<html><em>Minecraft:</em></html>");
        radioButtonMinecraft.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                radioButtonMinecraftActionPerformed(evt);
            }
        });

        jLabel3.setText("block ID:");

        comboBoxBlockType.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                comboBoxBlockTypeActionPerformed(evt);
            }
        });

        jLabel4.setText("data value:");

        spinnerDataValue.setModel(new javax.swing.SpinnerNumberModel(0, 0, 15, 1));
        spinnerDataValue.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                spinnerDataValueStateChanged(evt);
            }
        });

        jLabel5.setForeground(new java.awt.Color(0, 0, 255));
        jLabel5.setText("<html><u>look up legacy block IDs and data values</u></html>");
        jLabel5.setVerticalAlignment(javax.swing.SwingConstants.TOP);
        jLabel5.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        jLabel5.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jLabel5MouseClicked(evt);
            }
        });

        jLabel7.setText("Legacy:");

        jLabel9.setText("Modern:");

        jScrollPane1.setBorder(javax.swing.BorderFactory.createTitledBorder("Properties"));

        panelProperties.setLayout(new java.awt.GridBagLayout());
        jScrollPane1.setViewportView(panelProperties);

        comboBoxCustomName.setEditable(true);
        comboBoxCustomName.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                comboBoxCustomNameFocusLost(evt);
            }
        });
        comboBoxCustomName.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                comboBoxCustomNameActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jScrollPane1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(buttonAddProperty))
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(radioButtonCustom, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(radioButtonMinecraft, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGroup(layout.createSequentialGroup()
                                .addGap(21, 21, 21)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(layout.createSequentialGroup()
                                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                            .addComponent(jLabel7)
                                            .addComponent(jLabel9))
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                            .addGroup(layout.createSequentialGroup()
                                                .addComponent(jLabel2)
                                                .addGap(0, 0, 0)
                                                .addComponent(comboBoxMinecraftName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                            .addComponent(jLabel5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                            .addGroup(layout.createSequentialGroup()
                                                .addComponent(jLabel3)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(comboBoxBlockType, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addGap(18, 18, 18)
                                                .addComponent(jLabel4)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(spinnerDataValue, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))))
                                    .addGroup(layout.createSequentialGroup()
                                        .addComponent(comboBoxNamespace, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(jLabel1)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(comboBoxCustomName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))))
                        .addGap(0, 46, Short.MAX_VALUE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(radioButtonMinecraft, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(comboBoxMinecraftName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel9))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel7)
                    .addComponent(jLabel3)
                    .addComponent(comboBoxBlockType, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel4)
                    .addComponent(spinnerDataValue, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(radioButtonCustom, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(comboBoxNamespace, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(comboBoxCustomName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(buttonAddProperty)
                        .addGap(0, 22, Short.MAX_VALUE))
                    .addComponent(jScrollPane1))
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

    private void radioButtonCustomActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_radioButtonCustomActionPerformed
        namespace = (String) comboBoxNamespace.getSelectedItem();
        simpleName = (String) comboBoxCustomName.getSelectedItem();
        setControlStates();
        properties = null;
        updateProperties();
        updateMaterial();
    }//GEN-LAST:event_radioButtonCustomActionPerformed

    private void buttonAddPropertyActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonAddPropertyActionPerformed
        addProperty();
    }//GEN-LAST:event_buttonAddPropertyActionPerformed

    private void comboBoxNamespaceActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_comboBoxNamespaceActionPerformed
        if (! programmaticChange) {
            programmaticChange = true;
            try {
                namespace = (String) comboBoxNamespace.getSelectedItem();
                final Vector<String> simpleNames;
                if (MINECRAFT.equals(namespace)) {
                    simpleNames = new Vector<>();
                } else {
                    simpleNames = new Vector<>(Material.getAllSimpleNamesForNamespace(namespace));
                    Collections.sort(simpleNames);
                }
                comboBoxCustomName.setModel(new DefaultComboBoxModel<>(simpleNames));
                comboBoxCustomName.setSelectedItem(simpleName);
                updateMaterial();
            } finally {
                programmaticChange = false;
            }
        }
    }//GEN-LAST:event_comboBoxNamespaceActionPerformed

    private void comboBoxBlockTypeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_comboBoxBlockTypeActionPerformed
        if (! programmaticChange) {
            programmaticChange = true;
            try {
                spinnerDataValue.setValue(0);
                blockIdOrDataChanged();
            } finally {
                programmaticChange = false;
            }
        }
    }//GEN-LAST:event_comboBoxBlockTypeActionPerformed

    private void spinnerDataValueStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_spinnerDataValueStateChanged
        if (! programmaticChange) {
            programmaticChange = true;
            try {
                blockIdOrDataChanged();
            } finally {
                programmaticChange = false;
            }
        }
    }//GEN-LAST:event_spinnerDataValueStateChanged

    private void radioButtonMinecraftActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_radioButtonMinecraftActionPerformed
        namespace = Material.MINECRAFT;
        setControlStates();
        loadDefaultProperties();
        firePropertyChange("material", null, getMaterial());
    }//GEN-LAST:event_radioButtonMinecraftActionPerformed

    private void jLabel5MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jLabel5MouseClicked
        try {
            DesktopUtils.open(new URL("https://www.worldpainter.net/links/dataValues"));
        } catch (MalformedURLException e) {
            throw new RuntimeException("Malformed URL exception while trying to open https://www.worldpainter.net/links/dataValues", e);
        }
    }//GEN-LAST:event_jLabel5MouseClicked

    private void comboBoxMinecraftNameActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_comboBoxMinecraftNameActionPerformed
        if (! programmaticChange) {
            programmaticChange = true;
            try {
                minecraftNameChanged();
            } finally {
                programmaticChange = false;
            }
        }
    }//GEN-LAST:event_comboBoxMinecraftNameActionPerformed

    private void comboBoxCustomNameFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_comboBoxCustomNameFocusLost
        simpleName = (String) comboBoxCustomName.getSelectedItem();
        programmaticChange = true;
        try {
            updateMaterial();
        } finally {
            programmaticChange = false;
        }
    }//GEN-LAST:event_comboBoxCustomNameFocusLost

    private void comboBoxCustomNameActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_comboBoxCustomNameActionPerformed
        if (! programmaticChange) {
            simpleName = (String) comboBoxCustomName.getSelectedItem();
            programmaticChange = true;
            try {
                updateMaterial();
            } finally {
                programmaticChange = false;
            }
        }
    }//GEN-LAST:event_comboBoxCustomNameActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton buttonAddProperty;
    private javax.swing.ButtonGroup buttonGroup1;
    private javax.swing.JComboBox<String> comboBoxBlockType;
    private javax.swing.JComboBox<String> comboBoxCustomName;
    private javax.swing.JComboBox<String> comboBoxMinecraftName;
    private javax.swing.JComboBox<String> comboBoxNamespace;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JPanel panelProperties;
    private javax.swing.JRadioButton radioButtonCustom;
    private javax.swing.JRadioButton radioButtonMinecraft;
    private javax.swing.JSpinner spinnerDataValue;
    // End of variables declaration//GEN-END:variables

    private final Map<String, Component> propertyEditors = new HashMap<>();
    private boolean extendedBlockIds, programmaticChange, propertiesEnabled;
    private Platform platform;
    private Material material;
    private String namespace, simpleName;
    private Map<String, String> properties;

    private static final Logger logger = LoggerFactory.getLogger(MaterialSelector.class);
}