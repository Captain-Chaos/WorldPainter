/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/*
 * CustomMaterialDialog.java
 *
 * Created on 11-okt-2011, 15:21:20
 */
package org.pepsoft.worldpainter;

import org.pepsoft.minecraft.Material;
import org.pepsoft.worldpainter.MixedMaterial.Row;
import org.pepsoft.worldpainter.themes.JSpinnerTableCellEditor;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Arrays;

import static org.pepsoft.minecraft.Constants.HIGHEST_KNOWN_BLOCK_ID;
import static org.pepsoft.worldpainter.DefaultPlugin.*;
import static org.pepsoft.worldpainter.MixedMaterialTableModel.*;

/**
 *
 * @author pepijn
 */
public class CustomMaterialDialog extends WorldPainterDialog implements PropertyChangeListener  {
    public CustomMaterialDialog(Window parent, Platform platform, MixedMaterial material, boolean extendedBlockIds, ColourScheme colourScheme) {
        super(parent);
        this.platform = platform;
        this.material = material;
        this.extendedBlockIds = extendedBlockIds;
        this.colourScheme = colourScheme;
        
        initComponents();

        materialEditor1.setExtendedBlockIds(extendedBlockIds);
        materialEditor1.setPlatform(platform);
        materialEditor1.addPropertyChangeListener("material", this);
        fieldName.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                if (! programmaticChange) {
                    setControlStates();
                }
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                if (! programmaticChange) {
                    setControlStates();
                }
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                if (! programmaticChange) {
                    setControlStates();
                }
            }
        });
        tableMaterialRows.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
        tableMaterialRows.getSelectionModel().addListSelectionListener(e -> {
            if (! programmaticChange) {
                setControlStates();
            }
        });
        tableMaterialRows.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                Point point = e.getPoint();
                int column = tableMaterialRows.columnAtPoint(point);
                int row = tableMaterialRows.rowAtPoint(point);
                if ((row != -1) && (column == COLUMN_MATERIAL)) {
                    JDialog dialog = new WorldPainterDialog(CustomMaterialDialog.this);
                    dialog.setTitle("Edit Material");
                    MaterialSelector materialSelector = new MaterialSelector();
                    materialSelector.setPlatform(platform);
                    materialSelector.setExtendedBlockIds(extendedBlockIds);
                    materialSelector.setMaterial((Material) tableModel.getValueAt(row, COLUMN_MATERIAL));
                    dialog.getContentPane().add(materialSelector, BorderLayout.CENTER);
                    JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
                    JButton okButton = new JButton("OK");
                    okButton.addActionListener(event -> {
                        Material material = materialSelector.getMaterial();
                        tableModel.setValueAt(material, row, COLUMN_MATERIAL);
                        dialog.dispose();
                    });
                    buttonPanel.add(okButton);
                    JButton cancelButton = new JButton("Cancel");
                    cancelButton.addActionListener(event -> dialog.dispose());
                    buttonPanel.add(cancelButton);
                    dialog.getRootPane().setDefaultButton(okButton);
                    dialog.getContentPane().add(buttonPanel, BorderLayout.SOUTH);
                    dialog.pack();
                    dialog.setLocationRelativeTo(CustomMaterialDialog.this);
                    dialog.setVisible(true);
                }
            }
        });
        tableMaterialRows.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                Point point = e.getPoint();
                int column = tableMaterialRows.columnAtPoint(point);
                int row = tableMaterialRows.rowAtPoint(point);
                if ((row != -1) && (column == COLUMN_MATERIAL)) {
                    if (! cursorSet) {
                        tableMaterialRows.setCursor(HAND_CURSOR);
                        cursorSet = true;
                    }
                } else {
                    if (cursorSet) {
                        tableMaterialRows.setCursor(null);
                        cursorSet = false;
                    }
                }
            }

            private boolean cursorSet;
        });
        init(material);

        rootPane.setDefaultButton(buttonOK);

        scaleToUI();
        setLocationRelativeTo(parent);
    }

    // PropertyChangeListener


    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getPropertyName().equals("material")) {
            updateName();
            schedulePreviewUpdate();
        }
    }

    @Override
    protected void ok() {
        if (tableMaterialRows.isEditing()) {
            tableMaterialRows.getCellEditor().stopCellEditing();
        }
        saveSettings();
        super.ok();
    }

    private void saveSettings() {
        switch (jTabbedPane1.getSelectedIndex()) {
            case 0:
                // Simple
                material.edit(
                    fieldName.getText(),
                    new Row[] {new Row(materialEditor1.getMaterial(), 1000, 1.0f)},
                    biome,
                    MixedMaterial.Mode.SIMPLE,
                    1.0f,
                    checkBoxColour.isSelected() ? selectedColour : null,
                    null,
                    0.0,
                    0.0,
                    false);
                break;
            case 1:
                // Complex
                Row[] rows = tableModel.getRows();
                if (rows.length == 1) {
                    material.edit(
                            fieldName.getText(),
                            rows,
                            biome,
                            MixedMaterial.Mode.SIMPLE,
                            1.0f,
                            checkBoxColour.isSelected() ? selectedColour : null,
                            null,
                            0.0,
                            0.0,
                            false);
                } else if (radioButtonNoise.isSelected()) {
                    material.edit(
                            fieldName.getText(),
                            rows,
                            biome,
                            MixedMaterial.Mode.NOISE,
                            1.0f,
                            checkBoxColour.isSelected() ? selectedColour : null,
                            null,
                            0.0,
                            0.0,
                            false);
                } else if (radioButtonBlobs.isSelected()) {
                    material.edit(
                            fieldName.getText(),
                            rows,
                            biome,
                            MixedMaterial.Mode.BLOBS,
                            ((Integer) spinnerScale.getValue()) / 100.0f,
                            checkBoxColour.isSelected() ? selectedColour : null,
                            null,
                            0.0,
                            0.0,
                            false);
                } else {
                    NoiseSettings variation = noiseSettingsEditorLayeredVariation.getNoiseSettings();
                    boolean repeat = checkBoxLayeredRepeat.isSelected();
                    material.edit(
                            fieldName.getText(),
                            rows,
                            biome,
                            MixedMaterial.Mode.LAYERED,
                            ((Integer) spinnerScale.getValue()) / 100.0f,
                            checkBoxColour.isSelected() ? selectedColour : null,
                            (variation.getRange() != 0) ? variation : null,
                            repeat ? Math.tan((-(Integer) spinnerLayeredXAngle.getValue()) / DEGREES_TO_RADIANS) : 0.0,
                            repeat ? Math.tan((-(Integer) spinnerLayeredYAngle.getValue()) / DEGREES_TO_RADIANS) : 0.0,
                            repeat);
                }
                break;
        }
    }

    private MixedMaterial getNewMaterial() {
        switch (jTabbedPane1.getSelectedIndex()) {
            case 0:
                // Simple
                return new MixedMaterial(
                    fieldName.getText(),
                    new Row(materialEditor1.getMaterial(), 1000, 1.0f),
                    biome,
                    checkBoxColour.isSelected() ? selectedColour : null);
            case 1:
                // Complex
                MixedMaterialTableModel tableModelClone = tableModel.clone();
                Row[] rows = tableModelClone.getRows();
                if (rows.length == 1) {
                    return new MixedMaterial(
                            fieldName.getText(),
                            rows[0],
                            biome,
                            checkBoxColour.isSelected() ? selectedColour : null);
                } else if (radioButtonNoise.isSelected()) {
                    return new MixedMaterial(
                            fieldName.getText(),
                            rows,
                            biome,
                            checkBoxColour.isSelected() ? selectedColour : null);
                } else if (radioButtonBlobs.isSelected()) {
                    return new MixedMaterial(
                            fieldName.getText(),
                            rows,
                            biome,
                            checkBoxColour.isSelected() ? selectedColour : null,
                            ((Integer) spinnerScale.getValue()) / 100.0f);
                } else {
                    NoiseSettings variation = noiseSettingsEditorLayeredVariation.getNoiseSettings().clone();
                    boolean repeat = checkBoxLayeredRepeat.isSelected();
                    return new MixedMaterial(
                            fieldName.getText(),
                            rows,
                            biome,
                            checkBoxColour.isSelected() ? selectedColour : null,
                            (variation.getRange() != 0) ? variation : null,
                            repeat ? Math.tan((-(Integer) spinnerLayeredXAngle.getValue()) / DEGREES_TO_RADIANS) : 0.0,
                            repeat ? Math.tan((-(Integer) spinnerLayeredYAngle.getValue()) / DEGREES_TO_RADIANS) : 0.0,
                            repeat);
                }
            default:
                throw new InternalError();
        }
    }

    private void init(MixedMaterial mixedMaterial) {
        programmaticChange = true;
        try {
            biome = mixedMaterial.getBiome();
            fieldName.setText(mixedMaterial.getName());
            tableModel = new MixedMaterialTableModel(mixedMaterial);
            tableModel.addTableModelListener(e -> {
                if ((!checkBoxColour.isSelected()) && isExtendedBlockIds()) {
                    checkBoxColour.setSelected(true);
                }
                setControlStates();
                updateName();
                schedulePreviewUpdate();
            });
            switch (mixedMaterial.getMode()) {
                case SIMPLE:
                    jTabbedPane1.setSelectedIndex(0);
                    materialEditor1.setMaterial(mixedMaterial.getSingleMaterial());
                    break;
                case NOISE:
                    jTabbedPane1.setSelectedIndex(1);
                    radioButtonNoise.setSelected(true);
                    break;
                case BLOBS:
                    jTabbedPane1.setSelectedIndex(1);
                    radioButtonBlobs.setSelected(true);
                    spinnerScale.setValue(Math.round(mixedMaterial.getScale() * 100));
                    break;
                case LAYERED:
                    jTabbedPane1.setSelectedIndex(1);
                    radioButtonLayered.setSelected(true);
                    checkBoxLayeredRepeat.setSelected(mixedMaterial.isRepeat());
                    if (mixedMaterial.getVariation() != null) {
                        noiseSettingsEditorLayeredVariation.setNoiseSettings(mixedMaterial.getVariation());
                    }
                    spinnerLayeredXAngle.setValue(-(int) Math.round(Math.atan(mixedMaterial.getLayerXSlope()) * DEGREES_TO_RADIANS));
                    spinnerLayeredYAngle.setValue(-(int) Math.round(Math.atan(mixedMaterial.getLayerYSlope()) * DEGREES_TO_RADIANS));
                    break;
            }
            if (materialEditor1.getMaterial() == null) {
                // Set a material on the single material editor in case the user
                // switches back:
                materialEditor1.setMaterial(mixedMaterial.getRows()[0].material);
            }
            tableMaterialRows.setModel(tableModel);
            previousCalculatedName = createName();
            selectedColour = (mixedMaterial.getColour() != null) ? mixedMaterial.getColour() : Color.ORANGE.getRGB();
            checkBoxColour.setSelected(mixedMaterial.getColour() != null);

            setControlStates();
            updatePreview();
            configureTable();
        } finally {
            programmaticChange = false;
        }
    }
    
    private void addMaterial() {
        if (tableMaterialRows.isEditing()) {
            tableMaterialRows.getCellEditor().stopCellEditing();
        }

        tableModel.addMaterial(new Row(Material.DIRT, tableModel.getAverageCount(), 1.0f));
    }
    
    private void removeMaterial() {
        if (tableMaterialRows.isEditing()) {
            tableMaterialRows.getCellEditor().stopCellEditing();
        }
        int[] selectedRows = tableMaterialRows.getSelectedRows();
        for (int i = selectedRows.length - 1; i >= 0; i--) {
            tableModel.removeMaterial(selectedRows[i]);
        }
    }
    
    private void setControlStates() {
        boolean nameSet = ! fieldName.getText().trim().isEmpty();
        switch (jTabbedPane1.getSelectedIndex()) {
            case 0:
                // Simple
                buttonOK.setEnabled(nameSet);
                break;
            case 1:
                // Complex
                boolean countValid = (Integer) tableModel.getValueAt(0, COLUMN_COUNT) >= 0;
                buttonOK.setEnabled(nameSet && countValid);
                buttonRemoveMaterial.setEnabled((tableModel.getRowCount() > 1) && (tableMaterialRows.getSelectedRows().length > 0));
                spinnerScale.setEnabled(radioButtonBlobs.isSelected());
                checkBoxLayeredRepeat.setEnabled(radioButtonLayered.isSelected());
                noiseSettingsEditorLayeredVariation.setEnabled(radioButtonLayered.isSelected());
                spinnerLayeredXAngle.setEnabled(radioButtonLayered.isSelected() && checkBoxLayeredRepeat.isSelected());
                spinnerLayeredYAngle.setEnabled(radioButtonLayered.isSelected() && checkBoxLayeredRepeat.isSelected());
                if (radioButtonNoise.isSelected()) {
                    tableModel.setMode(MixedMaterial.Mode.NOISE);
                } else if (radioButtonBlobs.isSelected()) {
                    tableModel.setMode(MixedMaterial.Mode.BLOBS);
                } else {
                    tableModel.setMode(MixedMaterial.Mode.LAYERED);
                }
                break;
        }
        if (checkBoxColour.isSelected()) {
            setLabelColour();
            buttonSelectColour.setEnabled(true);
        } else {
            labelColour.setBackground(null);
            buttonSelectColour.setEnabled(false);
        }
    }
    
    private String createName() {
        switch (jTabbedPane1.getSelectedIndex()) {
            case 0:
                // Simple
                return createName(materialEditor1.getMaterial());
            case 1:
                // Complex
                Row[] rows = tableModel.getRows();
                rows = Arrays.copyOf(rows, rows.length);
                Arrays.sort(rows, (r1, r2) -> r2.occurrence - r1.occurrence);
                StringBuilder sb = new StringBuilder();
                for (Row row: rows) {
                    if (sb.length() > 0) {
                        sb.append('/');
                    }
                    sb.append(createName(row.material));
                }
                if (sb.length() > 30) {
                    return sb.toString().substring(0, 27) + "...";
                } else {
                    return sb.toString();
                }
            default:
                throw new InternalError();
        }
    }

    private String createName(Material material) {
        return ((platform == JAVA_ANVIL_1_15) || (platform == JAVA_ANVIL_1_17) || (platform == JAVA_ANVIL_1_18)) /* TODO make dynamic */ ? material.toString() : material.toLegacyString();
    }

    private boolean isExtendedBlockIds() {
        for (Row row: tableModel.getRows()) {
            if (row.material.blockType > HIGHEST_KNOWN_BLOCK_ID) {
                return true;
            }
        }
        return false;
    }
    
    private void pickColour() {
        Color pick = JColorChooser.showDialog(this, "Select Colour", new Color(selectedColour));
        if (pick != null) {
            selectedColour = pick.getRGB();
            setLabelColour();
        }
    }

    private void setLabelColour() {
        labelColour.setBackground(new Color(selectedColour));
    }
    
    private void configureTable() {
        if (jTabbedPane1.getSelectedIndex() == 1) {
            TableColumnModel columnModel = tableMaterialRows.getColumnModel();
            TableColumn materialColumn = columnModel.getColumn(COLUMN_MATERIAL);
            materialColumn.setCellRenderer(new MaterialTableCellRenderer(platform));
            SpinnerModel countSpinnerModel = new SpinnerNumberModel(1000, 1, 1000, 1);
            columnModel.getColumn(COLUMN_COUNT).setCellEditor(new JSpinnerTableCellEditor(countSpinnerModel));
            if (tableModel.getMode() == MixedMaterial.Mode.BLOBS) {
                SpinnerModel scaleSpinnerModel = new SpinnerNumberModel(100, 1, 9999, 1);
                columnModel.getColumn(COLUMN_SCALE).setCellEditor(new JSpinnerTableCellEditor(scaleSpinnerModel));
            }
        }
    }
    
    private void schedulePreviewUpdate() {
        if (previewUpdateTimer == null) {
            previewUpdateTimer = new Timer(250, e -> updatePreview());
            previewUpdateTimer.setRepeats(false);
        }
        previewUpdateTimer.restart();
    }
    
    private void updatePreview() {
        if (previewUpdateTimer != null) {
            previewUpdateTimer.stop(); // Superfluous?
            previewUpdateTimer = null;
        }
        int width = labelPreview.getWidth(), height = labelPreview.getHeight();
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        MixedMaterial mixedMaterial = getNewMaterial();
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                image.setRGB(x, y, colourScheme.getColour(mixedMaterial.getMaterial(0L, x, 0, height - y - 1)));
            }
        }
        labelPreview.setIcon(new ImageIcon(image));
    }

    private void saveMaterial() {
        MixedMaterialHelper.save(this, getNewMaterial());
    }

    private void loadMaterial() {
        MixedMaterial material = MixedMaterialHelper.load(this);
        if (material != null) {
            init(material);
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

        buttonGroup1 = new javax.swing.ButtonGroup();
        jLabel8 = new javax.swing.JLabel();
        buttonCancel = new javax.swing.JButton();
        buttonOK = new javax.swing.JButton();
        jLabel2 = new javax.swing.JLabel();
        fieldName = new javax.swing.JTextField();
        jLabel5 = new javax.swing.JLabel();
        labelColour = new javax.swing.JLabel();
        buttonSelectColour = new javax.swing.JButton();
        checkBoxColour = new javax.swing.JCheckBox();
        jPanel1 = new javax.swing.JPanel();
        labelPreview = new javax.swing.JLabel();
        buttonLoad = new javax.swing.JButton();
        buttonSave = new javax.swing.JButton();
        jTabbedPane1 = new javax.swing.JTabbedPane();
        jPanel3 = new javax.swing.JPanel();
        materialEditor1 = new org.pepsoft.worldpainter.MaterialSelector();
        jPanel2 = new javax.swing.JPanel();
        spinnerScale = new javax.swing.JSpinner();
        buttonAddMaterial = new javax.swing.JButton();
        buttonRemoveMaterial = new javax.swing.JButton();
        jLabel6 = new javax.swing.JLabel();
        spinnerLayeredXAngle = new javax.swing.JSpinner();
        jLabel9 = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        tableMaterialRows = new javax.swing.JTable();
        jLabel7 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        noiseSettingsEditorLayeredVariation = new org.pepsoft.worldpainter.NoiseSettingsEditor();
        radioButtonLayered = new javax.swing.JRadioButton();
        radioButtonNoise = new javax.swing.JRadioButton();
        spinnerLayeredYAngle = new javax.swing.JSpinner();
        radioButtonBlobs = new javax.swing.JRadioButton();
        jLabel1 = new javax.swing.JLabel();
        checkBoxLayeredRepeat = new javax.swing.JCheckBox();

        jLabel8.setText("jLabel8");

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Select Custom Material");
        addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentResized(java.awt.event.ComponentEvent evt) {
                formComponentResized(evt);
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

        jLabel2.setText("Name:");

        fieldName.setColumns(20);

        jLabel5.setText("Colour:");

        labelColour.setText("                 ");
        labelColour.setToolTipText("Select to override actual block colours");
        labelColour.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        labelColour.setOpaque(true);

        buttonSelectColour.setText("...");
        buttonSelectColour.setEnabled(false);
        buttonSelectColour.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonSelectColourActionPerformed(evt);
            }
        });

        checkBoxColour.setText(" ");
        checkBoxColour.setToolTipText("Select to override actual block colours");
        checkBoxColour.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                checkBoxColourActionPerformed(evt);
            }
        });

        jPanel1.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.LOWERED));

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(labelPreview, javax.swing.GroupLayout.DEFAULT_SIZE, 124, Short.MAX_VALUE)
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(labelPreview, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        buttonLoad.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/pepsoft/worldpainter/icons/folder_page_white.png"))); // NOI18N
        buttonLoad.setToolTipText("Load this custom material from a file");
        buttonLoad.setMargin(new java.awt.Insets(2, 2, 2, 2));
        buttonLoad.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonLoadActionPerformed(evt);
            }
        });

        buttonSave.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/pepsoft/worldpainter/icons/disk.png"))); // NOI18N
        buttonSave.setToolTipText("Save this custom material to a file");
        buttonSave.setMargin(new java.awt.Insets(2, 2, 2, 2));
        buttonSave.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonSaveActionPerformed(evt);
            }
        });

        jTabbedPane1.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                jTabbedPane1StateChanged(evt);
            }
        });

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(materialEditor1, javax.swing.GroupLayout.PREFERRED_SIZE, 489, Short.MAX_VALUE)
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addComponent(materialEditor1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 222, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab("Simple", jPanel3);

        spinnerScale.setModel(new javax.swing.SpinnerNumberModel(100, 1, 9999, 1));
        spinnerScale.setEnabled(false);
        spinnerScale.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                spinnerScaleStateChanged(evt);
            }
        });

        buttonAddMaterial.setText("Add Material");
        buttonAddMaterial.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonAddMaterialActionPerformed(evt);
            }
        });

        buttonRemoveMaterial.setText("Remove Material");
        buttonRemoveMaterial.setEnabled(false);
        buttonRemoveMaterial.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonRemoveMaterialActionPerformed(evt);
            }
        });

        jLabel6.setText("Variation:");

        spinnerLayeredXAngle.setModel(new javax.swing.SpinnerNumberModel(0, -89, 89, 1));
        spinnerLayeredXAngle.setEnabled(false);
        spinnerLayeredXAngle.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                spinnerLayeredXAngleStateChanged(evt);
            }
        });

        jLabel9.setText("Z-axis angle:");

        tableMaterialRows.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "Title 1", "Title 2", "Title 3", "Title 4"
            }
        ));
        jScrollPane1.setViewportView(tableMaterialRows);

        jLabel7.setText("X-axis angle:");

        jLabel3.setText("Scale:");

        noiseSettingsEditorLayeredVariation.setEnabled(false);
        noiseSettingsEditorLayeredVariation.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                noiseSettingsEditorLayeredVariationStateChanged(evt);
            }
        });

        buttonGroup1.add(radioButtonLayered);
        radioButtonLayered.setText("Layers");
        radioButtonLayered.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                radioButtonLayeredActionPerformed(evt);
            }
        });

        buttonGroup1.add(radioButtonNoise);
        radioButtonNoise.setSelected(true);
        radioButtonNoise.setText("Noise");
        radioButtonNoise.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                radioButtonNoiseActionPerformed(evt);
            }
        });

        spinnerLayeredYAngle.setModel(new javax.swing.SpinnerNumberModel(0, -89, 89, 1));
        spinnerLayeredYAngle.setEnabled(false);

        buttonGroup1.add(radioButtonBlobs);
        radioButtonBlobs.setText("Blobs");
        radioButtonBlobs.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                radioButtonBlobsActionPerformed(evt);
            }
        });

        jLabel1.setText("Select the block ID and data value(s) for your custom material:");

        checkBoxLayeredRepeat.setSelected(true);
        checkBoxLayeredRepeat.setText("Repeat:");
        checkBoxLayeredRepeat.setEnabled(false);
        checkBoxLayeredRepeat.setHorizontalTextPosition(javax.swing.SwingConstants.LEADING);
        checkBoxLayeredRepeat.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                checkBoxLayeredRepeatActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(radioButtonNoise)
                            .addComponent(jLabel1)
                            .addGroup(jPanel2Layout.createSequentialGroup()
                                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(radioButtonLayered)
                                    .addComponent(radioButtonBlobs))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(jPanel2Layout.createSequentialGroup()
                                        .addComponent(jLabel6)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(noiseSettingsEditorLayeredVariation, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                    .addGroup(jPanel2Layout.createSequentialGroup()
                                        .addComponent(jLabel7)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(spinnerLayeredXAngle, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addGap(18, 18, 18)
                                        .addComponent(jLabel9)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(spinnerLayeredYAngle, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                    .addGroup(jPanel2Layout.createSequentialGroup()
                                        .addComponent(jLabel3)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(spinnerScale, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                    .addComponent(checkBoxLayeredRepeat))))
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(buttonRemoveMaterial)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(buttonAddMaterial)))
                .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(radioButtonNoise)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(radioButtonBlobs)
                    .addComponent(jLabel3)
                    .addComponent(spinnerScale, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(6, 6, 6)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(radioButtonLayered)
                    .addComponent(checkBoxLayeredRepeat))
                .addGap(11, 11, 11)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel6)
                    .addComponent(noiseSettingsEditorLayeredVariation, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel7)
                    .addComponent(spinnerLayeredXAngle, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel9)
                    .addComponent(spinnerLayeredYAngle, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 205, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(buttonAddMaterial)
                    .addComponent(buttonRemoveMaterial))
                .addContainerGap())
        );

        jTabbedPane1.addTab("Complex", jPanel2);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(buttonLoad)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(buttonSave)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(buttonOK)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(buttonCancel))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jLabel2)
                                    .addComponent(jLabel5))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(layout.createSequentialGroup()
                                        .addComponent(checkBoxColour)
                                        .addGap(0, 0, 0)
                                        .addComponent(labelColour)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(buttonSelectColour))
                                    .addComponent(fieldName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGap(0, 0, Short.MAX_VALUE))
                            .addComponent(jTabbedPane1))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel2)
                            .addComponent(fieldName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel5)
                            .addComponent(labelColour)
                            .addComponent(buttonSelectColour)
                            .addComponent(checkBoxColour))
                        .addGap(18, 18, 18)
                        .addComponent(jTabbedPane1))
                    .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(buttonCancel)
                    .addComponent(buttonOK)
                    .addComponent(buttonLoad)
                    .addComponent(buttonSave))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void buttonCancelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonCancelActionPerformed
        cancel();
    }//GEN-LAST:event_buttonCancelActionPerformed

    private void buttonOKActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonOKActionPerformed
        ok();
    }//GEN-LAST:event_buttonOKActionPerformed

    private void buttonAddMaterialActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonAddMaterialActionPerformed
        addMaterial();
    }//GEN-LAST:event_buttonAddMaterialActionPerformed

    private void buttonRemoveMaterialActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonRemoveMaterialActionPerformed
        removeMaterial();
    }//GEN-LAST:event_buttonRemoveMaterialActionPerformed

    private void radioButtonNoiseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_radioButtonNoiseActionPerformed
        setControlStates();
        configureTable();
        schedulePreviewUpdate();
    }//GEN-LAST:event_radioButtonNoiseActionPerformed

    private void radioButtonBlobsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_radioButtonBlobsActionPerformed
        setControlStates();
        configureTable();
        schedulePreviewUpdate();
    }//GEN-LAST:event_radioButtonBlobsActionPerformed

    private void checkBoxColourActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_checkBoxColourActionPerformed
        setControlStates();
    }//GEN-LAST:event_checkBoxColourActionPerformed

    private void buttonSelectColourActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonSelectColourActionPerformed
        pickColour();
    }//GEN-LAST:event_buttonSelectColourActionPerformed

    private void radioButtonLayeredActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_radioButtonLayeredActionPerformed
        setControlStates();
        configureTable();
        schedulePreviewUpdate();
    }//GEN-LAST:event_radioButtonLayeredActionPerformed

    private void spinnerScaleStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_spinnerScaleStateChanged
        schedulePreviewUpdate();
    }//GEN-LAST:event_spinnerScaleStateChanged

    private void checkBoxLayeredRepeatActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_checkBoxLayeredRepeatActionPerformed
        setControlStates();
        schedulePreviewUpdate();
    }//GEN-LAST:event_checkBoxLayeredRepeatActionPerformed

    private void noiseSettingsEditorLayeredVariationStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_noiseSettingsEditorLayeredVariationStateChanged
        schedulePreviewUpdate();
    }//GEN-LAST:event_noiseSettingsEditorLayeredVariationStateChanged

    private void spinnerLayeredXAngleStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_spinnerLayeredXAngleStateChanged
        schedulePreviewUpdate();
    }//GEN-LAST:event_spinnerLayeredXAngleStateChanged

    private void formComponentResized(java.awt.event.ComponentEvent evt) {//GEN-FIRST:event_formComponentResized
        schedulePreviewUpdate();
    }//GEN-LAST:event_formComponentResized

    private void buttonSaveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonSaveActionPerformed
        saveMaterial();
    }//GEN-LAST:event_buttonSaveActionPerformed

    private void buttonLoadActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonLoadActionPerformed
        loadMaterial();
    }//GEN-LAST:event_buttonLoadActionPerformed

    private void jTabbedPane1StateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_jTabbedPane1StateChanged
        if (! programmaticChange) {
            setControlStates();
            updateName();
            configureTable();
            schedulePreviewUpdate();
        }
    }//GEN-LAST:event_jTabbedPane1StateChanged

    private void updateName() {
        if (fieldName.getText().equals(previousCalculatedName)) {
            String calculatedName = createName();
            fieldName.setText(createName());
            previousCalculatedName = calculatedName;
        }
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton buttonAddMaterial;
    private javax.swing.JButton buttonCancel;
    private javax.swing.ButtonGroup buttonGroup1;
    private javax.swing.JButton buttonLoad;
    private javax.swing.JButton buttonOK;
    private javax.swing.JButton buttonRemoveMaterial;
    private javax.swing.JButton buttonSave;
    private javax.swing.JButton buttonSelectColour;
    private javax.swing.JCheckBox checkBoxColour;
    private javax.swing.JCheckBox checkBoxLayeredRepeat;
    private javax.swing.JTextField fieldName;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JLabel labelColour;
    private javax.swing.JLabel labelPreview;
    private org.pepsoft.worldpainter.MaterialSelector materialEditor1;
    private org.pepsoft.worldpainter.NoiseSettingsEditor noiseSettingsEditorLayeredVariation;
    private javax.swing.JRadioButton radioButtonBlobs;
    private javax.swing.JRadioButton radioButtonLayered;
    private javax.swing.JRadioButton radioButtonNoise;
    private javax.swing.JSpinner spinnerLayeredXAngle;
    private javax.swing.JSpinner spinnerLayeredYAngle;
    private javax.swing.JSpinner spinnerScale;
    private javax.swing.JTable tableMaterialRows;
    // End of variables declaration//GEN-END:variables

    private final Platform platform;
    private final boolean extendedBlockIds;
    private final ColourScheme colourScheme;
    private MixedMaterial material;
    private MixedMaterialTableModel tableModel;
    private int biome;
    private String previousCalculatedName;
    private int selectedColour = Color.ORANGE.getRGB();
    private Timer previewUpdateTimer;
    private boolean programmaticChange;
    
    private static final double DEGREES_TO_RADIANS = 180 / Math.PI;
    private static final Cursor HAND_CURSOR = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
    private static final long serialVersionUID = 1L;
}