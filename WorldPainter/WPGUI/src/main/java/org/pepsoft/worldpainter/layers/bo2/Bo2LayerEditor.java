/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.worldpainter.layers.bo2;

import org.pepsoft.minecraft.Constants;
import org.pepsoft.util.DesktopUtils;
import org.pepsoft.worldpainter.ColourScheme;
import org.pepsoft.worldpainter.Configuration;
import org.pepsoft.worldpainter.layers.AbstractLayerEditor;
import org.pepsoft.worldpainter.layers.Bo2Layer;
import org.pepsoft.worldpainter.layers.exporters.ExporterSettings;
import org.pepsoft.worldpainter.objects.WPObject;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileFilter;
import javax.vecmath.Point3i;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.NumberFormat;
import java.util.*;
import java.util.List;

import static org.pepsoft.worldpainter.objects.WPObject.*;

/**
 *
 * @author Pepijn Schmitz
 */
public class Bo2LayerEditor extends AbstractLayerEditor<Bo2Layer> implements ListSelectionListener, DocumentListener {
    /**
     * Creates new form Bo2LayerEditor
     */
    public Bo2LayerEditor() {
        initComponents();
        
        listModel = new DefaultListModel();
        listObjects.setModel(listModel);
        listObjects.setCellRenderer(new WPObjectListCellRenderer());
        
        listObjects.getSelectionModel().addListSelectionListener(this);
        fieldName.getDocument().addDocumentListener(this);

        updateBlocksPerAttempt();
    }

    // LayerEditor
    
    @Override
    public Bo2Layer createLayer() {
        return new Bo2Layer(new Bo2ObjectTube("My Custom Objects", Collections.EMPTY_LIST), Color.ORANGE.getRGB());
    }

    @Override
    public void setLayer(Bo2Layer layer) {
        super.setLayer(layer);
        reset();
    }

    @Override
    public void commit() {
        if (! isCommitAvailable()) {
            throw new IllegalStateException("Settings invalid or incomplete");
        }
        saveSettings(layer);
    }
    
    @Override
    public void reset() {
        List<WPObject> objects = new ArrayList<>();
        fieldName.setText(layer.getName());
        selectedColour = layer.getColour();
        List<File> files = layer.getFiles();
        if (files != null) {
            if (files.isEmpty()) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Existing layer contains new style objects");
                }
                // New layer; files stored in object attributes
                objects.addAll(layer.getObjectProvider().getAllObjects());
            } else {
                // Old layer; files stored separately
                int missingFiles = 0;
                if ((files.size() == 1) && files.get(0).isDirectory()) {
                    logger.info("Existing custom object layer contains old style directory; migrating to new style");
                    File[] filesInDir = files.get(0).listFiles((dir, name) -> name.toLowerCase().endsWith(".bo2") || name.toLowerCase().endsWith(".schematic"));
                    for (File file: filesInDir) {
                        try {
                            WPObject object;
                            if (file.getName().toLowerCase().endsWith(".bo2")) {
                                object = Bo2Object.load(file);
                            } else {
                                object = Schematic.load(file);
                            }
                            objects.add(object);
                        } catch (IOException e) {
                            logger.error("I/O error while trying to load custom object " + file, e);
                            missingFiles++;
                        }
                    }
                } else {
                    logger.info("Existing custom object layer contains old style file list; migrating to new style");
                    for (File file: files) {
                        if (file.exists()) {
                            try {
                                WPObject object;
                                if (file.getName().toLowerCase().endsWith(".bo2")) {
                                    object = Bo2Object.load(file);
                                } else {
                                    object = Schematic.load(file);
                                }
                                objects.add(object);
                            } catch (IOException e) {
                                logger.error("I/O error while trying to load custom object " + file, e);
                                missingFiles++;
                            }
                        } else {
                            missingFiles++;
                        }
                    }
                }
                if (missingFiles > 0) {
                    JOptionPane.showMessageDialog(this, "This is an old custom object layer and " + missingFiles + " objects\ncould NOT be restored because they were missing or\nreading them resulted in an I/O error.\n\nYou will have to re-add these objects before\nsaving the settings, otherwise the existing object\ndata will be gone. You may also cancel the dialog\nwithout affecting the object data.", "Missing Files", JOptionPane.WARNING_MESSAGE);
                }
            }
        } else {
            logger.info("Existing custom object layer contains very old style objects with no file information; migrating to new style");
            // Very old layer; no file information at all
            objects.addAll(layer.getObjectProvider().getAllObjects());
        }
        listModel.clear();
        for (WPObject object: objects) {
            listModel.addElement(object.clone());
        }
        spinnerBlocksPerAttempt.setValue(layer.getDensity());
        
        setLabelColour();
        
        refreshLeafDecaySettings();
        
        settingsChanged();
    }

    @Override
    public ExporterSettings<Bo2Layer> getSettings() {
        if (! isCommitAvailable()) {
            throw new IllegalStateException("Settings invalid or incomplete");
        }
        final Bo2Layer previewLayer = saveSettings(null);
        return new ExporterSettings<Bo2Layer>() {
            @Override
            public boolean isApplyEverywhere() {
                return false;
            }

            @Override
            public Bo2Layer getLayer() {
                return previewLayer;
            }

            @Override
            public ExporterSettings<Bo2Layer> clone() {
                throw new UnsupportedOperationException("Not supported");
            }
        };
    }

    @Override
    public boolean isCommitAvailable() {
        boolean filesSelected = listModel.getSize() > 0;
        boolean nameSpecified = fieldName.getText().trim().length() > 0;
        return filesSelected && nameSpecified;
    }

    @Override
    public void setContext(LayerEditorContext context) {
        super.setContext(context);
        colourScheme = context.getColourScheme();
    }
        
    // ListSelectionListener
    
    @Override
    public void valueChanged(ListSelectionEvent e) {
        settingsChanged();
    }

    // DocumentListener
    
    @Override
    public void insertUpdate(DocumentEvent e) {
        settingsChanged();
    }

    @Override
    public void removeUpdate(DocumentEvent e) {
        settingsChanged();
    }

    @Override
    public void changedUpdate(DocumentEvent e) {
        settingsChanged();
    }

    private Bo2Layer saveSettings(Bo2Layer layer) {
        String name = fieldName.getText();
        List<WPObject> objects = new ArrayList<>(listModel.getSize());
        for (int i = 0; i < listModel.getSize(); i++) {
            objects.add((WPObject) listModel.getElementAt(i));
        }
        Bo2ObjectProvider objectProvider = new Bo2ObjectTube(name, objects);
        if (layer == null) {
            layer = new Bo2Layer(objectProvider, selectedColour);
        } else {
            layer.setObjectProvider(objectProvider);
            layer.setColour(selectedColour);
        }
        layer.setDensity((Integer) spinnerBlocksPerAttempt.getValue());
        return layer;
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
    
    private void settingsChanged() {
        setControlStates();
        context.settingsChanged();
    }
    
    private void setControlStates() {
        boolean filesSelected = listModel.getSize() > 0;
        boolean objectsSelected = listObjects.getSelectedIndex() != -1;
        buttonRemoveFile.setEnabled(objectsSelected);
        buttonReloadAll.setEnabled(filesSelected);
        buttonEdit.setEnabled(objectsSelected);
    }
    
    private void addFilesOrDirectory() {
        // Can't use FileUtils.selectFilesForOpen() because it doesn't support
        // selecting directories, or adding custom components to the dialog
        JFileChooser fileChooser = new JFileChooser();
        Configuration config = Configuration.getInstance();
        if ((config.getCustomObjectsDirectory() != null) && config.getCustomObjectsDirectory().isDirectory()) {
            fileChooser.setCurrentDirectory(config.getCustomObjectsDirectory());
        }
        fileChooser.setDialogTitle("Select File(s) or Directory");
        fileChooser.setMultiSelectionEnabled(true);
        fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        fileChooser.setFileFilter(new FileFilter() {
            @Override
            public boolean accept(File f) {
                return f.isDirectory() || f.getName().toLowerCase().endsWith(".bo2") || f.getName().toLowerCase().endsWith(".bo3") || f.getName().toLowerCase().endsWith(".schematic");
            }

            @Override
            public String getDescription() {
                return "Custom Object Files (*.bo2, *.bo3, *.schematic)";
            }
        });
        WPObjectPreviewer previewer = new WPObjectPreviewer();
        fileChooser.addPropertyChangeListener(JFileChooser.SELECTED_FILE_CHANGED_PROPERTY, previewer);
        fileChooser.setAccessory(previewer);
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File[] selectedFiles = fileChooser.getSelectedFiles();
            if (selectedFiles.length > 0) {
                config.setCustomObjectsDirectory(selectedFiles[0].getParentFile());
                for (File selectedFile: selectedFiles) {
                    if (selectedFile.isDirectory()) {
                        if (fieldName.getText().isEmpty()) {
                            String name = selectedFiles[0].getName();
                            if (name.length() > 12) {
                                name = "..." + name.substring(name.length() - 10);
                            }
                            fieldName.setText(name);
                        }
                        File[] files = selectedFile.listFiles((dir, name) -> name.toLowerCase().endsWith(".bo2") || name.toLowerCase().endsWith(".bo3") || name.toLowerCase().endsWith(".schematic"));
                        if (files.length == 0) {
                            JOptionPane.showMessageDialog(this, "Directory " + selectedFile.getName() + " does not contain any .bo2 or .schematic files.", "No Custom Object Files", JOptionPane.ERROR_MESSAGE);
                        } else {
                            for (File file: files) {
                                try {
                                    WPObject object;
                                    if (file.getName().toLowerCase().endsWith(".bo2")) {
                                        object = Bo2Object.load(file);
                                    } else if (file.getName().toLowerCase().endsWith(".bo3")) {
                                        object = Bo3Object.load(file);
                                    } else {
                                        object = Schematic.load(file);
                                    }
                                    listModel.addElement(object);
                                } catch (IOException e) {
                                    logger.error("I/O error while trying to load custom object " + file, e);
                                    JOptionPane.showMessageDialog(this, "I/O error while loading " + file.getName() + "; it was not added", "I/O Error", JOptionPane.ERROR_MESSAGE);
                                }
                            }
                        }
                    } else {
                        if (fieldName.getText().isEmpty()) {
                            String name = selectedFile.getName();
                            int p = name.lastIndexOf('.');
                            if (p != -1) {
                                name = name.substring(0, p);
                            }
                            if (name.length() > 12) {
                                name = "..." + name.substring(name.length() - 10);
                            }
                            fieldName.setText(name);
                        }
                        try {
                            WPObject object;
                            if (selectedFile.getName().toLowerCase().endsWith(".bo2")) {
                                object = Bo2Object.load(selectedFile);
                            } else if (selectedFile.getName().toLowerCase().endsWith(".bo3")) {
                                object = Bo3Object.load(selectedFile);
                            } else {
                                object = Schematic.load(selectedFile);
                            }
                            listModel.addElement(object);
                        } catch (IOException e) {
                            logger.error("I/O error while trying to load custom object " + selectedFile, e);
                            JOptionPane.showMessageDialog(this, "I/O error while loading " + selectedFile.getName() + "; it was not added", "I/O Error", JOptionPane.ERROR_MESSAGE);
                        }
                    }
                }
                settingsChanged();
                refreshLeafDecaySettings();
            }
        }
    }
    
    private void removeFiles() {
        int[] selectedIndices = listObjects.getSelectedIndices();
        for (int i = selectedIndices.length - 1; i >= 0; i--) {
            listModel.removeElementAt(selectedIndices[i]);
        }
        settingsChanged();
        refreshLeafDecaySettings();
    }

    private void reloadObjects() {
        StringBuilder noFiles = new StringBuilder();
        StringBuilder notFound = new StringBuilder();
        StringBuilder errors = new StringBuilder();
        int[] indices;
        if (listObjects.getSelectedIndex() != -1) {
            indices = listObjects.getSelectedIndices();
        } else {
            indices = new int[listModel.getSize()];
            for (int i = 0; i < indices.length; i++) {
                indices[i] = i;
            }
        }
        for (int indice : indices) {
            WPObject object = (WPObject) listModel.getElementAt(indice);
            File file = object.getAttribute(ATTRIBUTE_FILE, (File) null);
            if (file != null) {
                if (file.isFile() && file.canRead()) {
                    try {
                        Map<String, Serializable> existingAttributes = object.getAttributes();
                        if (file.getName().toLowerCase().endsWith(".bo2")) {
                            object = Bo2Object.load(file);
                        } else if (file.getName().toLowerCase().endsWith(".bo3")) {
                            object = Bo3Object.load(file);
                        } else {
                            object = Schematic.load(file);
                        }
                        if (existingAttributes != null) {
                            Map<String, Serializable> attributes = object.getAttributes();
                            if (attributes == null) {
                                attributes = new HashMap<>();
                            }
                            attributes.putAll(existingAttributes);
                            object.setAttributes(attributes);
                        }
                        listModel.setElementAt(object, indice);
                    } catch (IOException e) {
                        logger.error("I/O error while reloading " + file, e);
                        errors.append(file.getPath()).append('\n');
                    }
                } else {
                    notFound.append(file.getPath()).append('\n');
                }
            } else {
                noFiles.append(object.getName()).append('\n');
            }
        }
        if ((noFiles.length() > 0) || (notFound.length() > 0)) {
            StringBuilder message = new StringBuilder();
            message.append("Not all files could be reloaded!\n");
            if (noFiles.length() > 0) {
                message.append("\nThe following objects came from an old layer and have no filename stored:\n");
                message.append(noFiles);
            }
            if (notFound.length() > 0) {
                message.append("\nThe following files were missing or not accessible:\n");
                message.append(notFound);
            }
            JOptionPane.showMessageDialog(this, message, "Not All Files Reloaded", JOptionPane.ERROR_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this, indices.length + " objects successfully reloaded", "Success", JOptionPane.INFORMATION_MESSAGE);
        }
        refreshLeafDecaySettings();
    }
    
    private void editObjects() {
        List<WPObject> selectedObjects = new ArrayList<>(listObjects.getSelectedIndices().length);
        int[] selectedIndices = listObjects.getSelectedIndices();
        for (int i = selectedIndices.length - 1; i >= 0; i--) {
            selectedObjects.add((WPObject) listModel.getElementAt(selectedIndices[i]));
        }
        EditObjectAttributes dialog = new EditObjectAttributes(SwingUtilities.getWindowAncestor(this), selectedObjects, colourScheme);
        dialog.setVisible(true);
        if (! dialog.isCancelled()) {
            refreshLeafDecaySettings();
        }
    }

    private void refreshLeafDecaySettings() {
        if (listModel.isEmpty()) {
            labelLeafDecayTitle.setEnabled(false);
            labelEffectiveLeafDecaySetting.setEnabled(false);
            labelEffectiveLeafDecaySetting.setText("N/A");
            buttonSetDecay.setEnabled(false);
            buttonSetNoDecay.setEnabled(false);
            buttonReset.setEnabled(false);
            return;
        }
        boolean decayingLeavesFound = false;
        boolean nonDecayingLeavesFound = false;
outer:  for (Enumeration<WPObject> e = (Enumeration<WPObject>) listModel.elements(); e.hasMoreElements(); ) {
            WPObject object = e.nextElement();
            int leafDecayMode = object.getAttribute(ATTRIBUTE_LEAF_DECAY_MODE, LEAF_DECAY_NO_CHANGE);
            switch (leafDecayMode) {
                case LEAF_DECAY_NO_CHANGE:
                    // Leaf decay attribute not set (or set to "no change");
                    // examine actual blocks
                    Point3i dim = object.getDimensions();
                    for (int x = 0; x < dim.x; x++) {
                        for (int y = 0; y < dim.y; y++) {
                            for (int z = 0; z < dim.z; z++) {
                                if ((object.getMask(x, y, z))
                                        && ((object.getMaterial(x, y, z).blockType == Constants.BLK_LEAVES)
                                            || (object.getMaterial(x, y, z).blockType == Constants.BLK_LEAVES2))) {
                                    if ((object.getMaterial(x, y, z).data & 0x4) == 0x4) {
                                        // Non decaying leaf block
                                        nonDecayingLeavesFound = true;
                                        if (decayingLeavesFound) {
                                            // We have enough information; no
                                            // reason to continue the
                                            // examination
                                            break outer;
                                        }
                                    } else {
                                        // Decaying leaf block
                                        decayingLeavesFound = true;
                                        if (nonDecayingLeavesFound) {
                                            // We have enough information; no
                                            // reason to continue the
                                            // examination
                                            break outer;
                                        }
                                    }
                                }
                            }
                        }
                    }
                    break;
                case LEAF_DECAY_OFF:
                    // Leaf decay attribute set to "off"; don't examine blocks
                    // for performance (even though this could lead to
                    // misleading information if the object doesn't contain any
                    // leaf blocks)
                    nonDecayingLeavesFound = true;
                    if (decayingLeavesFound) {
                        // We have enough information; no reason to continue the
                        // examination
                        break outer;
                    }
                    break;
                case LEAF_DECAY_ON:
                    // Leaf decay attribute set to "off"; don't examine blocks
                    // for performance (even though this could lead to
                    // misleading information if the object doesn't contain any
                    // leaf blocks)
                    decayingLeavesFound = true;
                    if (nonDecayingLeavesFound) {
                        // We have enough information; no reason to continue the
                        // examination
                        break outer;
                    }
                    break;
                default:
                    throw new InternalError();
            }
        }

        if (decayingLeavesFound) {
            if (nonDecayingLeavesFound) {
                // Both decaying and non decaying leaves found
                labelLeafDecayTitle.setEnabled(true);
                labelEffectiveLeafDecaySetting.setEnabled(true);
                labelEffectiveLeafDecaySetting.setText("<html>Decaying <i>and</i> non decaying leaves.</html>");
                buttonSetDecay.setEnabled(true);
                buttonSetNoDecay.setEnabled(true);
                buttonReset.setEnabled(true);
            } else {
                // Only decaying leaves found
                labelLeafDecayTitle.setEnabled(true);
                labelEffectiveLeafDecaySetting.setEnabled(true);
                labelEffectiveLeafDecaySetting.setText("<html>Leaves <b>do</b> decay.</html>");
                buttonSetDecay.setEnabled(false);
                buttonSetNoDecay.setEnabled(true);
                buttonReset.setEnabled(true);
            }
        } else {
            if (nonDecayingLeavesFound) {
                // Only non decaying leaves found
                labelLeafDecayTitle.setEnabled(true);
                labelEffectiveLeafDecaySetting.setEnabled(true);
                labelEffectiveLeafDecaySetting.setText("<html>Leaves do <b>not</b> decay.</html>");
                buttonSetDecay.setEnabled(true);
                buttonSetNoDecay.setEnabled(false);
                buttonReset.setEnabled(true);
            } else {
                // No leaf blocks encountered at all, so N/A
                labelLeafDecayTitle.setEnabled(false);
                labelEffectiveLeafDecaySetting.setEnabled(false);
                labelEffectiveLeafDecaySetting.setText("N/A");
                buttonSetDecay.setEnabled(false);
                buttonSetNoDecay.setEnabled(false);
                buttonReset.setEnabled(false);
            }
        }
    }

    private void setLeavesDecay() {
        for (Enumeration<WPObject> e = (Enumeration<WPObject>) listModel.elements(); e.hasMoreElements(); ) {
            WPObject object = e.nextElement();
            object.setAttribute(ATTRIBUTE_LEAF_DECAY_MODE, LEAF_DECAY_ON);
        }
        refreshLeafDecaySettings();
    }

    private void setLeavesNoDecay() {
        for (Enumeration<WPObject> e = (Enumeration<WPObject>) listModel.elements(); e.hasMoreElements(); ) {
            WPObject object = e.nextElement();
            object.setAttribute(ATTRIBUTE_LEAF_DECAY_MODE, LEAF_DECAY_OFF);
        }
        refreshLeafDecaySettings();
    }

    private void resetLeafDecay() {
        for (Enumeration<WPObject> e = (Enumeration<WPObject>) listModel.elements(); e.hasMoreElements(); ) {
            WPObject object = e.nextElement();
            object.getAttributes().remove(ATTRIBUTE_LEAF_DECAY_MODE);
        }
        refreshLeafDecaySettings();
    }

    private void updateBlocksPerAttempt() {
        int blocksAt50 = (Integer) spinnerBlocksPerAttempt.getValue();
        int blocksAt1 = blocksAt50 * 64, blocksAt100 = (int) (blocksAt50 / 3.515625f + 0.5f);
        StringBuilder sb = new StringBuilder();
        sb.append("one per ").append(numberFormat.format(blocksAt1)).append(" blocks at 1%");
        if (blocksAt100 <= 1) {
            sb.append("; one every block at 100%)");
        } else {
            sb.append("; one per ").append(numberFormat.format(blocksAt100)).append(" blocks at 100%)");
        }
        labelBlocksPerAttempt.setText(sb.toString());
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        buttonReloadAll = new javax.swing.JButton();
        jSeparator2 = new javax.swing.JSeparator();
        buttonEdit = new javax.swing.JButton();
        jPanel3 = new javax.swing.JPanel();
        labelLeafDecayTitle = new javax.swing.JLabel();
        labelEffectiveLeafDecaySetting = new javax.swing.JLabel();
        buttonSetDecay = new javax.swing.JButton();
        buttonSetNoDecay = new javax.swing.JButton();
        buttonReset = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        listObjects = new javax.swing.JList();
        jLabel6 = new javax.swing.JLabel();
        jLabel1 = new javax.swing.JLabel();
        jPanel2 = new javax.swing.JPanel();
        jLabel3 = new javax.swing.JLabel();
        fieldName = new javax.swing.JTextField();
        jLabel4 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        buttonPickColour = new javax.swing.JButton();
        jLabel2 = new javax.swing.JLabel();
        buttonAddFile = new javax.swing.JButton();
        buttonRemoveFile = new javax.swing.JButton();
        jLabel7 = new javax.swing.JLabel();
        spinnerBlocksPerAttempt = new javax.swing.JSpinner();
        jLabel9 = new javax.swing.JLabel();
        labelBlocksPerAttempt = new javax.swing.JLabel();
        jLabel10 = new javax.swing.JLabel();

        buttonReloadAll.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/pepsoft/worldpainter/icons/arrow_rotate_clockwise.png"))); // NOI18N
        buttonReloadAll.setToolTipText("Reload all or selected objects from disk");
        buttonReloadAll.setEnabled(false);
        buttonReloadAll.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonReloadAllActionPerformed(evt);
            }
        });

        jSeparator2.setOrientation(javax.swing.SwingConstants.VERTICAL);

        buttonEdit.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/pepsoft/worldpainter/icons/brick_edit.png"))); // NOI18N
        buttonEdit.setToolTipText("Edit selected object(s) options");
        buttonEdit.setEnabled(false);
        buttonEdit.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonEditActionPerformed(evt);
            }
        });

        labelLeafDecayTitle.setText("Leaf decay settings for these objects:");

        labelEffectiveLeafDecaySetting.setText("<html>Leaves do <b>not</b> decay.</html>");
        labelEffectiveLeafDecaySetting.setEnabled(false);

        buttonSetDecay.setText("Set all to decay");
        buttonSetDecay.setToolTipText("Set all objects to decaying leaves");
        buttonSetDecay.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonSetDecayActionPerformed(evt);
            }
        });

        buttonSetNoDecay.setText("<html>Set all to <b>not</b> decay</html>");
        buttonSetNoDecay.setToolTipText("Set all objects to non decaying leaves");
        buttonSetNoDecay.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonSetNoDecayActionPerformed(evt);
            }
        });

        buttonReset.setText("Reset");
        buttonReset.setToolTipText("Reset leaf decay to object defaults");
        buttonReset.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonResetActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(labelEffectiveLeafDecaySetting, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(labelLeafDecayTitle)
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addComponent(buttonSetDecay)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(buttonSetNoDecay, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(buttonReset)))
                .addGap(0, 0, Short.MAX_VALUE))
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addComponent(labelLeafDecayTitle)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(labelEffectiveLeafDecaySetting, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(buttonSetDecay)
                    .addComponent(buttonSetNoDecay, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(buttonReset)))
        );

        listObjects.setModel(new javax.swing.AbstractListModel() {
            String[] strings = { "Item 1", "Item 2", "Item 3", "Item 4", "Item 5" };
            public int getSize() { return strings.length; }
            public Object getElementAt(int i) { return strings[i]; }
        });
        listObjects.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                listObjectsMouseClicked(evt);
            }
        });
        jScrollPane1.setViewportView(listObjects);

        jLabel6.setForeground(new java.awt.Color(0, 0, 255));
        jLabel6.setText("<html><u>Get custom objects</u></html>");
        jLabel6.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        jLabel6.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jLabel6MouseClicked(evt);
            }
        });

        jLabel1.setText("Define your custom object layer on this screen.");

        jLabel3.setText("Name:");

        fieldName.setColumns(10);

        jLabel4.setText("Colour:");

        jLabel5.setBackground(java.awt.Color.orange);
        jLabel5.setText("                 ");
        jLabel5.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        jLabel5.setOpaque(true);

        buttonPickColour.setText("...");
        buttonPickColour.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonPickColourActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(jLabel3)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(fieldName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(jLabel4)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel5)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(buttonPickColour)))
                .addGap(0, 0, Short.MAX_VALUE))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3)
                    .addComponent(fieldName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, Short.MAX_VALUE)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel4)
                    .addComponent(jLabel5)
                    .addComponent(buttonPickColour)))
        );

        jLabel2.setText("Object(s):");

        buttonAddFile.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/pepsoft/worldpainter/icons/brick_add.png"))); // NOI18N
        buttonAddFile.setToolTipText("Add one or more objects");
        buttonAddFile.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonAddFileActionPerformed(evt);
            }
        });

        buttonRemoveFile.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/pepsoft/worldpainter/icons/brick_delete.png"))); // NOI18N
        buttonRemoveFile.setToolTipText("Remove selected object(s)");
        buttonRemoveFile.setEnabled(false);
        buttonRemoveFile.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonRemoveFileActionPerformed(evt);
            }
        });

        jLabel7.setText("Sparseness:");

        spinnerBlocksPerAttempt.setModel(new javax.swing.SpinnerNumberModel(20, 1, 100000, 1));
        spinnerBlocksPerAttempt.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                spinnerBlocksPerAttemptStateChanged(evt);
            }
        });

        jLabel9.setText(" blocks (at 50% intensity;");

        labelBlocksPerAttempt.setText("one per x blocks at 1%; one per y blocks at 100%)");

        jLabel10.setText("one object per ");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jScrollPane1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(buttonAddFile, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(buttonRemoveFile, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(buttonEdit, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(buttonReloadAll, javax.swing.GroupLayout.Alignment.TRAILING)))
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel2)
                    .addComponent(jLabel1)
                    .addComponent(jLabel6, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jSeparator2, javax.swing.GroupLayout.PREFERRED_SIZE, 2, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel7)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(labelBlocksPerAttempt)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jLabel10)
                                .addGap(0, 0, 0)
                                .addComponent(spinnerBlocksPerAttempt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(0, 0, 0)
                                .addComponent(jLabel9)))))
                .addGap(0, 0, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel6, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(jLabel2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(buttonAddFile)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(buttonRemoveFile)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(buttonEdit)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(buttonReloadAll)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE))
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel7)
                    .addComponent(spinnerBlocksPerAttempt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel9)
                    .addComponent(jLabel10))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(labelBlocksPerAttempt)
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jSeparator2)
                    .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void buttonReloadAllActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonReloadAllActionPerformed
        reloadObjects();
    }//GEN-LAST:event_buttonReloadAllActionPerformed

    private void buttonEditActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonEditActionPerformed
        editObjects();
    }//GEN-LAST:event_buttonEditActionPerformed

    private void buttonSetDecayActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonSetDecayActionPerformed
        setLeavesDecay();
    }//GEN-LAST:event_buttonSetDecayActionPerformed

    private void buttonSetNoDecayActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonSetNoDecayActionPerformed
        setLeavesNoDecay();
    }//GEN-LAST:event_buttonSetNoDecayActionPerformed

    private void buttonResetActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonResetActionPerformed
        resetLeafDecay();
    }//GEN-LAST:event_buttonResetActionPerformed

    private void listObjectsMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_listObjectsMouseClicked
        if (evt.getClickCount() == 2) {
            int row = listObjects.getSelectedIndex();
            if (row != -1) {
                WPObject object = (WPObject) listModel.getElementAt(row);
                EditObjectAttributes dialog = new EditObjectAttributes(SwingUtilities.getWindowAncestor(this), object, colourScheme);
                dialog.setVisible(true);
                if (! dialog.isCancelled()) {
                    refreshLeafDecaySettings();
                }
            }
        }
    }//GEN-LAST:event_listObjectsMouseClicked

    private void jLabel6MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jLabel6MouseClicked
        try {
            DesktopUtils.open(new URL("http://www.worldpainter.net/trac/wiki/CustomObjects"));
        } catch (MalformedURLException e) {
            throw new RuntimeException("Malformed URL exception while trying to open http://www.worldpainter.net/trac/wiki/CustomObjects", e);
        }
    }//GEN-LAST:event_jLabel6MouseClicked

    private void buttonPickColourActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonPickColourActionPerformed
        pickColour();
    }//GEN-LAST:event_buttonPickColourActionPerformed

    private void buttonAddFileActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonAddFileActionPerformed
        addFilesOrDirectory();
    }//GEN-LAST:event_buttonAddFileActionPerformed

    private void buttonRemoveFileActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonRemoveFileActionPerformed
        removeFiles();
    }//GEN-LAST:event_buttonRemoveFileActionPerformed

    private void spinnerBlocksPerAttemptStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_spinnerBlocksPerAttemptStateChanged
        updateBlocksPerAttempt();
        settingsChanged();
    }//GEN-LAST:event_spinnerBlocksPerAttemptStateChanged


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton buttonAddFile;
    private javax.swing.JButton buttonEdit;
    private javax.swing.JButton buttonPickColour;
    private javax.swing.JButton buttonReloadAll;
    private javax.swing.JButton buttonRemoveFile;
    private javax.swing.JButton buttonReset;
    private javax.swing.JButton buttonSetDecay;
    private javax.swing.JButton buttonSetNoDecay;
    private javax.swing.JTextField fieldName;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JSeparator jSeparator2;
    private javax.swing.JLabel labelBlocksPerAttempt;
    private javax.swing.JLabel labelEffectiveLeafDecaySetting;
    private javax.swing.JLabel labelLeafDecayTitle;
    private javax.swing.JList listObjects;
    private javax.swing.JSpinner spinnerBlocksPerAttempt;
    // End of variables declaration//GEN-END:variables

    private final DefaultListModel listModel;
    private final NumberFormat numberFormat = NumberFormat.getInstance();
    private ColourScheme colourScheme;
    private int selectedColour = Color.ORANGE.getRGB();
    
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Bo2LayerEditor.class);
    private static final long serialVersionUID = 1L;
}