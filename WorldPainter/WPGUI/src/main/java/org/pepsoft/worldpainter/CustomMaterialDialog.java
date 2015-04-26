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
import org.pepsoft.util.DesktopUtils;
import org.pepsoft.worldpainter.MixedMaterial.Row;
import org.pepsoft.worldpainter.themes.JSpinnerTableCellEditor;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Comparator;

import static org.pepsoft.minecraft.Block.BLOCK_TYPE_NAMES;
import static org.pepsoft.minecraft.Constants.HIGHEST_KNOWN_BLOCK_ID;
import static org.pepsoft.worldpainter.MixedMaterialTableModel.*;

/**
 *
 * @author pepijn
 */
public class CustomMaterialDialog extends WorldPainterDialog {
    public CustomMaterialDialog(Window parent, MixedMaterial material, boolean extendedBlockIds, ColourScheme colourScheme) {
        super(parent);
        this.material = material;
        this.extendedBlockIds = extendedBlockIds;
        this.colourScheme = colourScheme;
        
        initComponents();

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
        tableMaterialRows.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (! programmaticChange) {
                    setControlStates();
                }
            }
        });
        init(material);

        rootPane.setDefaultButton(buttonOK);
        
        setLocationRelativeTo(parent);
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
            material.edit(
                    fieldName.getText(),
                    rows,
                    biome,
                    MixedMaterial.Mode.LAYERED,
                    ((Integer) spinnerScale.getValue()) / 100.0f,
                    checkBoxColour.isSelected() ? selectedColour : null,
                    (variation.getRange() != 0) ? variation : null,
                    Math.tan((-(Integer) spinnerLayeredXAngle.getValue()) / DEGREES_TO_RADIANS),
                    Math.tan((-(Integer) spinnerLayeredYAngle.getValue()) / DEGREES_TO_RADIANS),
                    checkBoxLayeredRepeat.isSelected());
        }
    }

    private MixedMaterial getNewMaterial() {
        Row[] rows = tableModel.getRows().clone();
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
            return new MixedMaterial(
                    fieldName.getText(),
                    rows,
                    biome,
                    checkBoxColour.isSelected() ? selectedColour : null,
                    (variation.getRange() != 0) ? variation : null,
                    Math.tan((-(Integer) spinnerLayeredXAngle.getValue()) / DEGREES_TO_RADIANS),
                    Math.tan((-(Integer) spinnerLayeredYAngle.getValue()) / DEGREES_TO_RADIANS),
                    checkBoxLayeredRepeat.isSelected());
        }
    }

    private void init(MixedMaterial mixedMaterial) {
        programmaticChange = true;
        try {
            biome = mixedMaterial.getBiome();
            fieldName.setText(mixedMaterial.getName());
            tableModel = new MixedMaterialTableModel(mixedMaterial);
            tableModel.addTableModelListener(new TableModelListener() {
                @Override public void tableChanged(TableModelEvent e) {
                    if ((!checkBoxColour.isSelected()) && isExtendedBlockIds()) {
                        checkBoxColour.setSelected(true);
                    }
                    setControlStates();
                    if (fieldName.getText().equals(previousCalculatedName)) {
                        String calculatedName = createName();
                        fieldName.setText(createName());
                        previousCalculatedName = calculatedName;
                    }
                    schedulePreviewUpdate();
                }
            });
            switch (mixedMaterial.getMode()) {
            case SIMPLE:
            case NOISE:
                radioButtonNoise.setSelected(true);
                break;
            case BLOBS:
                radioButtonBlobs.setSelected(true);
                spinnerScale.setValue((int) (mixedMaterial.getScale() * 100 + 0.5f));
                break;
            case LAYERED:
                radioButtonLayered.setSelected(true);
                checkBoxLayeredRepeat.setSelected(mixedMaterial.isRepeat());
                if (mixedMaterial.getVariation() != null) {
                    noiseSettingsEditorLayeredVariation.setNoiseSettings(mixedMaterial.getVariation());
                }
                spinnerLayeredXAngle.setValue((int) (Math.atan(mixedMaterial.getLayerXSlope()) * DEGREES_TO_RADIANS + 0.5));
                spinnerLayeredYAngle.setValue((int) (Math.atan(mixedMaterial.getLayerYSlope()) * DEGREES_TO_RADIANS + 0.5));
                break;
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
        tableModel.addMaterial(new Row(Material.DIRT, radioButtonLayered.isSelected() ? 3 : (1000 / (tableModel.getRowCount() + 1)), 1.0f));
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
        boolean occurrenceValid = (Integer) tableModel.getValueAt(0, COLUMN_OCCURRENCE) >= 0;
        buttonOK.setEnabled(nameSet && occurrenceValid);
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
        if (checkBoxColour.isSelected()) {
            setLabelColour();
            buttonSelectColour.setEnabled(true);
        } else {
            labelColour.setBackground(null);
            buttonSelectColour.setEnabled(false);
        }
    }
    
    private String createName() {
        Row[] rows = tableModel.getRows();
        rows = Arrays.copyOf(rows, rows.length);
        Arrays.sort(rows, new Comparator<Row>() {
            @Override
            public int compare(Row r1, Row r2) {
                return r2.occurrence - r1.occurrence;
            }
        });
        StringBuilder sb = new StringBuilder();
        for (Row row: rows) {
            if (sb.length() > 0) {
                sb.append('/');
            }
            int blockId = row.material.getBlockType();
            if ((blockId < BLOCK_TYPE_NAMES.length) && (BLOCK_TYPE_NAMES[blockId] != null)) {
                sb.append(BLOCK_TYPE_NAMES[blockId]);
            } else {
                sb.append(blockId);
            }
            int data = row.material.getData();
            if (data != 0) {
                sb.append(" (");
                sb.append(data);
                sb.append(')');
            }
        }
        return sb.toString();
    }
    
    private boolean isExtendedBlockIds() {
        for (Row row: tableModel.getRows()) {
            if (row.material.getBlockType() > HIGHEST_KNOWN_BLOCK_ID) {
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
        TableColumnModel columnModel = tableMaterialRows.getColumnModel();
        TableColumn blockIDColumn = columnModel.getColumn(COLUMN_BLOCK_ID);
        blockIDColumn.setCellEditor(new BlockIDTableCellEditor(extendedBlockIds));
        blockIDColumn.setCellRenderer(new BlockIDTableCellRenderer());
        SpinnerModel dataValueSpinnerModel = new SpinnerNumberModel(0, 0, 15, 1);
        columnModel.getColumn(COLUMN_DATA_VALUE).setCellEditor(new JSpinnerTableCellEditor(dataValueSpinnerModel));
        SpinnerModel occurrenceSpinnerModel = new SpinnerNumberModel(1000, 1, 1000, 1);
        columnModel.getColumn(COLUMN_OCCURRENCE).setCellEditor(new JSpinnerTableCellEditor(occurrenceSpinnerModel));
        if (tableModel.getMode() == MixedMaterial.Mode.BLOBS) {
            SpinnerModel scaleSpinnerModel = new SpinnerNumberModel(100, 1, 9999, 1);
            columnModel.getColumn(COLUMN_SCALE).setCellEditor(new JSpinnerTableCellEditor(scaleSpinnerModel));
        }
    }
    
    private void schedulePreviewUpdate() {
        if (previewUpdateTimer == null) {
            previewUpdateTimer = new Timer(250, new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    updatePreview();
                }
            });
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
        jLabel1 = new javax.swing.JLabel();
        buttonCancel = new javax.swing.JButton();
        buttonOK = new javax.swing.JButton();
        jLabel4 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        fieldName = new javax.swing.JTextField();
        buttonAddMaterial = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        tableMaterialRows = new javax.swing.JTable();
        buttonRemoveMaterial = new javax.swing.JButton();
        radioButtonNoise = new javax.swing.JRadioButton();
        radioButtonBlobs = new javax.swing.JRadioButton();
        jLabel3 = new javax.swing.JLabel();
        spinnerScale = new javax.swing.JSpinner();
        jLabel5 = new javax.swing.JLabel();
        labelColour = new javax.swing.JLabel();
        buttonSelectColour = new javax.swing.JButton();
        checkBoxColour = new javax.swing.JCheckBox();
        radioButtonLayered = new javax.swing.JRadioButton();
        checkBoxLayeredRepeat = new javax.swing.JCheckBox();
        jLabel6 = new javax.swing.JLabel();
        noiseSettingsEditorLayeredVariation = new org.pepsoft.worldpainter.NoiseSettingsEditor();
        jLabel7 = new javax.swing.JLabel();
        spinnerLayeredXAngle = new javax.swing.JSpinner();
        jLabel9 = new javax.swing.JLabel();
        spinnerLayeredYAngle = new javax.swing.JSpinner();
        jPanel1 = new javax.swing.JPanel();
        labelPreview = new javax.swing.JLabel();
        buttonLoad = new javax.swing.JButton();
        buttonSave = new javax.swing.JButton();

        jLabel8.setText("jLabel8");

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Select Custom Material");
        addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentResized(java.awt.event.ComponentEvent evt) {
                formComponentResized(evt);
            }
        });

        jLabel1.setText("Select the block ID and data value(s) for your custom material:");

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

        jLabel4.setForeground(new java.awt.Color(0, 0, 255));
        jLabel4.setText("<html><u>Look up block ID's and data values</u></html>");
        jLabel4.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        jLabel4.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jLabel4MouseClicked(evt);
            }
        });

        jLabel2.setText("Name:");

        fieldName.setColumns(20);

        buttonAddMaterial.setText("Add Material");
        buttonAddMaterial.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonAddMaterialActionPerformed(evt);
            }
        });

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

        buttonRemoveMaterial.setText("Remove Material");
        buttonRemoveMaterial.setEnabled(false);
        buttonRemoveMaterial.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonRemoveMaterialActionPerformed(evt);
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

        buttonGroup1.add(radioButtonBlobs);
        radioButtonBlobs.setText("Blobs");
        radioButtonBlobs.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                radioButtonBlobsActionPerformed(evt);
            }
        });

        jLabel3.setText("Scale:");

        spinnerScale.setModel(new javax.swing.SpinnerNumberModel(100, 1, 9999, 1));
        spinnerScale.setEnabled(false);
        spinnerScale.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                spinnerScaleStateChanged(evt);
            }
        });

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

        buttonGroup1.add(radioButtonLayered);
        radioButtonLayered.setText("Layers");
        radioButtonLayered.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                radioButtonLayeredActionPerformed(evt);
            }
        });

        checkBoxLayeredRepeat.setSelected(true);
        checkBoxLayeredRepeat.setText("Repeat:");
        checkBoxLayeredRepeat.setEnabled(false);
        checkBoxLayeredRepeat.setHorizontalTextPosition(javax.swing.SwingConstants.LEADING);
        checkBoxLayeredRepeat.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                checkBoxLayeredRepeatActionPerformed(evt);
            }
        });

        jLabel6.setText("Variation:");

        noiseSettingsEditorLayeredVariation.setEnabled(false);
        noiseSettingsEditorLayeredVariation.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                noiseSettingsEditorLayeredVariationStateChanged(evt);
            }
        });

        jLabel7.setText("X-axis angle:");

        spinnerLayeredXAngle.setModel(new javax.swing.SpinnerNumberModel(0, -89, 89, 1));
        spinnerLayeredXAngle.setEnabled(false);
        spinnerLayeredXAngle.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                spinnerLayeredXAngleStateChanged(evt);
            }
        });

        jLabel9.setText("Z-axis angle:");

        spinnerLayeredYAngle.setModel(new javax.swing.SpinnerNumberModel(0, -89, 89, 1));
        spinnerLayeredYAngle.setEnabled(false);

        jPanel1.setBorder(javax.swing.BorderFactory.createBevelBorder(1));

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(labelPreview, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(labelPreview, javax.swing.GroupLayout.DEFAULT_SIZE, 127, Short.MAX_VALUE)
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

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(buttonRemoveMaterial)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(buttonAddMaterial))
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.Alignment.TRAILING)
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
                                    .addComponent(fieldName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                            .addComponent(radioButtonNoise))
                        .addGap(18, 18, 18)
                        .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel1)
                            .addGroup(layout.createSequentialGroup()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(radioButtonLayered)
                                    .addComponent(radioButtonBlobs))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(layout.createSequentialGroup()
                                        .addComponent(jLabel6)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(noiseSettingsEditorLayeredVariation, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                    .addGroup(layout.createSequentialGroup()
                                        .addComponent(jLabel7)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(spinnerLayeredXAngle, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addGap(18, 18, 18)
                                        .addComponent(jLabel9)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(spinnerLayeredYAngle, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                    .addGroup(layout.createSequentialGroup()
                                        .addComponent(jLabel3)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(spinnerScale, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                    .addComponent(checkBoxLayeredRepeat))))
                        .addGap(0, 0, Short.MAX_VALUE)))
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
                        .addComponent(radioButtonNoise)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(radioButtonBlobs)
                            .addComponent(jLabel3)
                            .addComponent(spinnerScale, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(6, 6, 6)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(radioButtonLayered)
                            .addComponent(checkBoxLayeredRepeat)))
                    .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(11, 11, 11)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel6)
                    .addComponent(noiseSettingsEditorLayeredVariation, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel7)
                    .addComponent(spinnerLayeredXAngle, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel9)
                    .addComponent(spinnerLayeredYAngle, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 136, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(buttonAddMaterial)
                    .addComponent(buttonRemoveMaterial))
                .addGap(25, 25, 25)
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

    private void jLabel4MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jLabel4MouseClicked
        try {
            DesktopUtils.open(new URL("http://www.minecraftwiki.net/wiki/Data_values"));
        } catch (MalformedURLException e) {
            throw new RuntimeException("Malformed URL exception while trying to open http://www.minecraftwiki.net/wiki/Data_values", e);
        }
    }//GEN-LAST:event_jLabel4MouseClicked

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
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JLabel labelColour;
    private javax.swing.JLabel labelPreview;
    private org.pepsoft.worldpainter.NoiseSettingsEditor noiseSettingsEditorLayeredVariation;
    private javax.swing.JRadioButton radioButtonBlobs;
    private javax.swing.JRadioButton radioButtonLayered;
    private javax.swing.JRadioButton radioButtonNoise;
    private javax.swing.JSpinner spinnerLayeredXAngle;
    private javax.swing.JSpinner spinnerLayeredYAngle;
    private javax.swing.JSpinner spinnerScale;
    private javax.swing.JTable tableMaterialRows;
    // End of variables declaration//GEN-END:variables

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
    private static final long serialVersionUID = 1L;
}
