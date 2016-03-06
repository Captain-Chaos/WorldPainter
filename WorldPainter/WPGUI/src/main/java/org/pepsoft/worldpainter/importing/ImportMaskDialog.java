/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/*
 * ImportHeightMapDialog.java
 *
 * Created on 22-jan-2012, 19:47:55
 */
package org.pepsoft.worldpainter.importing;

import org.pepsoft.util.FileUtils;
import org.pepsoft.util.ProgressReceiver;
import org.pepsoft.util.swing.ProgressDialog;
import org.pepsoft.util.swing.ProgressTask;
import org.pepsoft.worldpainter.*;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.layers.Layer;
import org.pepsoft.worldpainter.layers.Void;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.pepsoft.worldpainter.importing.MaskImporter.InputType.*;
import static org.pepsoft.worldpainter.importing.MaskImporter.Mapping.*;


/**
 *
 * @author pepijn
 */
public class ImportMaskDialog extends WorldPainterDialog implements DocumentListener {
    public ImportMaskDialog(Window parent, Dimension dimension, ColourScheme colourScheme, List<Layer> allLayers) {
        super(parent);
        this.dimension = dimension;
        this.allLayers = allLayers;

        initComponents();
        
        fieldFilename.getDocument().addDocumentListener(this);
        comboBoxLayer.setModel(new DefaultComboBoxModel<>(allLayers.toArray(new Layer[allLayers.size()])));
        comboBoxLayer.setRenderer(new LayerListCellRenderer());
        
        rootPane.setDefaultButton(buttonOk);
        
        loadDefaults();

        pack();
        setLocationRelativeTo(parent);

        setControlStates();
    }

    // DocumentListener
    
    @Override
    public void insertUpdate(DocumentEvent e) {
        setControlStates();
    }

    @Override
    public void removeUpdate(DocumentEvent e) {
        setControlStates();
    }

    @Override
    public void changedUpdate(DocumentEvent e) {
        setControlStates();
    }

    private void setControlStates() {
        File file = new File(fieldFilename.getText());
        if (file.isFile() && ((selectedFile == null) || (! file.equals(selectedFile)))) {
            selectedFile = file;
            loadImage();
        }
        boolean fileSelected = (selectedFile != null) && selectedFile.isFile();
        radioButtonLayer.setEnabled(fileSelected);
        if (! fileSelected) {
            // loadImage() will enable it when necessary
            radioButtonTerrain.setEnabled(false);
        }
        boolean targetSelected = (radioButtonLayer.isSelected() && (comboBoxLayer.getSelectedItem() != null)) || radioButtonTerrain.isSelected();
        if (radioButtonTerrain.isEnabled() && radioButtonTerrain.isSelected()) {
            maskImporter.setApplyToTerrain(true);
        } else if (radioButtonLayer.isEnabled() && radioButtonLayer.isSelected() && comboBoxLayer.getSelectedItem() != null) {
            maskImporter.setApplyToLayer((Layer) comboBoxLayer.getSelectedItem());
        }
        radioButtonOneToOne.setEnabled(fileSelected && targetSelected && maskImporter.getPossibleMappings().contains(ONE_TO_ONE));
        radioButtonThreshold.setEnabled(fileSelected && targetSelected && maskImporter.getPossibleMappings().contains(THRESHOLD));
        radioButtonDither.setEnabled(fileSelected && targetSelected && maskImporter.getPossibleMappings().contains(DITHERING));
        radioButtonFullRange.setEnabled(fileSelected && targetSelected && maskImporter.getPossibleMappings().contains(FULL_RANGE));
        spinnerThreshold.setEnabled(fileSelected && radioButtonThreshold.isSelected());
        comboBoxLayer.setEnabled(fileSelected && radioButtonLayer.isSelected());
        checkBoxRemoveExisting.setEnabled(fileSelected && radioButtonLayer.isSelected());
        boolean mappingSelected = (radioButtonOneToOne.isEnabled() && radioButtonOneToOne.isSelected())
            || (radioButtonThreshold.isEnabled() && radioButtonThreshold.isSelected())
            || (radioButtonDither.isEnabled() && radioButtonDither.isSelected())
            || (radioButtonFullRange.isEnabled() && radioButtonFullRange.isSelected());
        buttonOk.setEnabled(fileSelected && targetSelected && mappingSelected);
    }

    private void loadImage() {
        try {
            image = null; // Set image to null first to make more memory available for loading the new image
            image = ImageIO.read(selectedFile);
            if (image == null) {
                labelImageDimensions.setForeground(Color.RED);
                labelImageDimensions.setText("Not an image file, or damaged file!");
                selectedFile = null;
            } else if (image.isAlphaPremultiplied()) {
                labelImageDimensions.setForeground(Color.RED);
                labelImageDimensions.setText("Premultiplied alpha not supported! Please convert to non-premultiplied.");
                selectedFile = null;
            } else {
                labelImageDimensions.setForeground(null);
                int width = image.getWidth(), height = image.getHeight();
                maskImporter = new MaskImporter(dimension, image, allLayers);
                MaskImporter.InputType inputType = maskImporter.getInputType();
                if (inputType == EIGHT_BIT_GREY_SCALE || inputType == SIXTEEN_BIT_GREY_SCALE) {
                    SpinnerNumberModel thresholdModel = (SpinnerNumberModel) spinnerThreshold.getModel();
                    thresholdModel.setMinimum(maskImporter.getImageLowValue());
                    thresholdModel.setMaximum(maskImporter.getImageHighValue());
                    thresholdModel.setValue((maskImporter.getImageHighValue() - maskImporter.getImageLowValue()) / 2);
                }

                radioButtonTerrain.setEnabled(maskImporter.isTerrainPossible());
                if (! maskImporter.isTerrainPossible()) {
                    radioButtonLayer.setSelected(true);
                }
                List<Layer> possibleLayers = maskImporter.getPossibleLayers();
                comboBoxLayer.setModel(new DefaultComboBoxModel<>(possibleLayers.toArray(new Layer[possibleLayers.size()])));

                if (inputType == COLOUR) {
                    labelImageDimensions.setText(String.format("Image size: %d x %d, indexed colour, %d bits", width, height, image.getSampleModel().getSampleSize(0)));
                } else if (inputType == ONE_BIT_GRAY_SCALE) {
                    labelImageDimensions.setText(String.format("Image size: %d x %d, black and white", width, height));
                } else {
                    labelImageDimensions.setText(String.format("Image size: %d x %d, grey scale, %d bits, lowest value: %d, highest value: %d", width, height, image.getSampleModel().getSampleSize(0), maskImporter.getImageLowValue(), maskImporter.getImageHighValue()));
                }
                updateWorldDimensions();
            }
        } catch (IOException e) {
            logger.error("I/O error loading image " + selectedFile, e);
            labelImageDimensions.setForeground(Color.RED);
            labelImageDimensions.setText(String.format("I/O error loading image (message: %s)!", e.getMessage()));
            selectedFile = null;
        }
    }

    private void updateWorldDimensions() {
        int scale = (Integer) spinnerScale.getValue();
        labelWorldDimensions.setText("Scaled size: " + (image.getWidth() * scale / 100) + " x " + (image.getHeight() * scale / 100) + " blocks");
    }

    private void loadDefaults() {
        // TODO
    }

    @Override
    protected void ok() {
        if (radioButtonOneToOne.isSelected()) {
            maskImporter.setMapping(ONE_TO_ONE);
        } else if (radioButtonFullRange.isSelected()) {
            maskImporter.setMapping(FULL_RANGE);
        } else if (radioButtonThreshold.isSelected()) {
            maskImporter.setMapping(THRESHOLD);
            maskImporter.setThreshold((Integer) spinnerThreshold.getValue());
        } else {
            maskImporter.setMapping(DITHERING);
        }
        if (radioButtonLayer.isSelected()) {
            maskImporter.setRemoveExistingLayer(checkBoxRemoveExisting.isSelected());
        }
        maskImporter.setScale((Integer) spinnerScale.getValue());
        maskImporter.setxOffset((Integer) spinnerOffsetX.getValue());
        maskImporter.setyOffset((Integer) spinnerOffsetY.getValue());
        ProgressDialog.executeTask(this, new ProgressTask<Void>() {
            @Override
            public String getName() {
                return "Importing mask";
            }

            @Override
            public Void execute(ProgressReceiver progressReceiver) throws ProgressReceiver.OperationCancelled {
                maskImporter.doImport(progressReceiver);
                return null;
            }
        });
        Configuration.getInstance().setMasksDirectory(selectedFile.getParentFile());
        dimension.armSavePoint();
        super.ok();
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLabel14 = new javax.swing.JLabel();
        buttonGroup1 = new javax.swing.ButtonGroup();
        buttonGroup2 = new javax.swing.ButtonGroup();
        jLabel1 = new javax.swing.JLabel();
        fieldFilename = new javax.swing.JTextField();
        buttonSelectFile = new javax.swing.JButton();
        labelImageDimensions = new javax.swing.JLabel();
        buttonCancel = new javax.swing.JButton();
        buttonOk = new javax.swing.JButton();
        jLabel2 = new javax.swing.JLabel();
        radioButtonTerrain = new javax.swing.JRadioButton();
        radioButtonLayer = new javax.swing.JRadioButton();
        comboBoxLayer = new javax.swing.JComboBox<>();
        jLabel3 = new javax.swing.JLabel();
        radioButtonOneToOne = new javax.swing.JRadioButton();
        radioButtonThreshold = new javax.swing.JRadioButton();
        radioButtonDither = new javax.swing.JRadioButton();
        spinnerThreshold = new javax.swing.JSpinner();
        jLabel4 = new javax.swing.JLabel();
        spinnerScale = new javax.swing.JSpinner();
        jLabel5 = new javax.swing.JLabel();
        jLabel11 = new javax.swing.JLabel();
        spinnerOffsetX = new javax.swing.JSpinner();
        spinnerOffsetY = new javax.swing.JSpinner();
        jLabel12 = new javax.swing.JLabel();
        labelWorldDimensions = new javax.swing.JLabel();
        radioButtonFullRange = new javax.swing.JRadioButton();
        checkBoxRemoveExisting = new javax.swing.JCheckBox();
        jLabel6 = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        jLabel8 = new javax.swing.JLabel();
        jLabel9 = new javax.swing.JLabel();

        jLabel14.setText("jLabel14");

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Import Height Map");

        jLabel1.setText("Select the image to import as a mask:");

        buttonSelectFile.setText("...");
        buttonSelectFile.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonSelectFileActionPerformed(evt);
            }
        });

        labelImageDimensions.setText("Image size: ? x ?, bit depth: ?, lowest value: ?, highest value: ?");

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

        jLabel2.setText("Apply to:");

        buttonGroup1.add(radioButtonTerrain);
        radioButtonTerrain.setText("terrain");
        radioButtonTerrain.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                radioButtonTerrainActionPerformed(evt);
            }
        });

        buttonGroup1.add(radioButtonLayer);
        radioButtonLayer.setText("layer");
        radioButtonLayer.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                radioButtonLayerActionPerformed(evt);
            }
        });

        comboBoxLayer.setEnabled(false);
        comboBoxLayer.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                comboBoxLayerActionPerformed(evt);
            }
        });

        jLabel3.setText("Mapping:");

        buttonGroup2.add(radioButtonOneToOne);
        radioButtonOneToOne.setText("one to one");
        radioButtonOneToOne.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                radioButtonOneToOneActionPerformed(evt);
            }
        });

        buttonGroup2.add(radioButtonThreshold);
        radioButtonThreshold.setText("threshold");
        radioButtonThreshold.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                radioButtonThresholdActionPerformed(evt);
            }
        });

        buttonGroup2.add(radioButtonDither);
        radioButtonDither.setText("dither");
        radioButtonDither.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                radioButtonDitherActionPerformed(evt);
            }
        });

        spinnerThreshold.setModel(new javax.swing.SpinnerNumberModel(0, 0, 255, 1));
        spinnerThreshold.setEnabled(false);

        jLabel4.setText("Scale:");

        spinnerScale.setModel(new javax.swing.SpinnerNumberModel(100, 1, 999, 1));
        spinnerScale.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                spinnerScaleStateChanged(evt);
            }
        });

        jLabel5.setText("%");

        jLabel11.setText("Offset:");
        jLabel11.setToolTipText("The origin of the height map will be at these coordinates in the map");

        spinnerOffsetX.setModel(new javax.swing.SpinnerNumberModel(0, -999999, 999999, 1));
        spinnerOffsetX.setToolTipText("The origin of the height map will be at these coordinates in the map");
        spinnerOffsetX.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                spinnerOffsetXStateChanged(evt);
            }
        });

        spinnerOffsetY.setModel(new javax.swing.SpinnerNumberModel(0, -999999, 999999, 1));
        spinnerOffsetY.setToolTipText("The origin of the height map will be at these coordinates in the map");
        spinnerOffsetY.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                spinnerOffsetYStateChanged(evt);
            }
        });

        jLabel12.setText(",");
        jLabel12.setToolTipText("The origin of the height map will be at these coordinates in the map");

        labelWorldDimensions.setText("Scaled size: ? x ? blocks");

        buttonGroup2.add(radioButtonFullRange);
        radioButtonFullRange.setText("full range");
        radioButtonFullRange.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                radioButtonFullRangeActionPerformed(evt);
            }
        });

        checkBoxRemoveExisting.setText("remove existing layer");
        checkBoxRemoveExisting.setEnabled(false);

        jLabel6.setText("Place layer where grey scale value is higher than threshold");

        jLabel7.setText("Map full range of grey scale to full range of layer");

        jLabel8.setText("Set the layer value to the exact grey scale value or colour");

        jLabel9.setText("Use the full grey scale range to dither the layer");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(fieldFilename)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(buttonSelectFile))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(buttonOk)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(buttonCancel))
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel1)
                            .addComponent(labelImageDimensions)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jLabel4)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(spinnerScale, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(0, 0, 0)
                                .addComponent(jLabel5)
                                .addGap(18, 18, 18)
                                .addComponent(jLabel11)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(spinnerOffsetX, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(0, 0, 0)
                                .addComponent(jLabel12)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(spinnerOffsetY, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addComponent(labelWorldDimensions)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jLabel2)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(radioButtonTerrain)
                                    .addGroup(layout.createSequentialGroup()
                                        .addComponent(radioButtonLayer)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(comboBoxLayer, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addGap(18, 18, 18)
                                        .addComponent(checkBoxRemoveExisting))))
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jLabel3)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(layout.createSequentialGroup()
                                        .addComponent(radioButtonThreshold)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(spinnerThreshold, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                    .addComponent(radioButtonFullRange)
                                    .addComponent(radioButtonOneToOne)
                                    .addComponent(radioButtonDither))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jLabel9)
                                    .addComponent(jLabel8)
                                    .addComponent(jLabel7)
                                    .addComponent(jLabel6))))
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(fieldFilename, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(buttonSelectFile))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(labelImageDimensions)
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel4)
                    .addComponent(spinnerScale, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel5)
                    .addComponent(jLabel11)
                    .addComponent(spinnerOffsetX, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel12)
                    .addComponent(spinnerOffsetY, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(labelWorldDimensions)
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(radioButtonTerrain))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(radioButtonLayer)
                    .addComponent(comboBoxLayer, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(checkBoxRemoveExisting))
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3)
                    .addComponent(radioButtonOneToOne)
                    .addComponent(jLabel8))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(radioButtonFullRange)
                    .addComponent(jLabel7))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(radioButtonThreshold)
                    .addComponent(spinnerThreshold, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel6))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(radioButtonDither)
                    .addComponent(jLabel9))
                .addGap(18, 18, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(buttonCancel)
                    .addComponent(buttonOk))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void buttonCancelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonCancelActionPerformed
        cancel();
    }//GEN-LAST:event_buttonCancelActionPerformed

    private void buttonSelectFileActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonSelectFileActionPerformed
        File myHeightMapDir = masksDir;
        if (myHeightMapDir == null) {
            myHeightMapDir = Configuration.getInstance().getMasksDirectory();
        }
        if (myHeightMapDir == null) {
            myHeightMapDir = Configuration.getInstance().getHeightMapsDirectory();
        }
        final Set<String> extensions = new HashSet<>(Arrays.asList(ImageIO.getReaderFileSuffixes()));
        StringBuilder sb = new StringBuilder("Supported image formats (");
        boolean first = true;
        for (String extension: extensions) {
            if (first) {
                first = false;
            } else {
                sb.append(", ");
            }
            sb.append("*.");
            sb.append(extension);
        }
        sb.append(')');
        final String description = sb.toString();
        File file = FileUtils.selectFileForOpen(this, "Select a mask image file", myHeightMapDir, new FileFilter() {
            @Override
            public boolean accept(File f) {
                if (f.isDirectory()) {
                    return true;
                }
                String filename = f.getName();
                int p = filename.lastIndexOf('.');
                if (p != -1) {
                    String extension = filename.substring(p + 1).toLowerCase();
                    return extensions.contains(extension);
                } else {
                    return false;
                }
            }

            @Override
            public String getDescription() {
                return description;
            }
        });
        if (file != null) {
            masksDir = file.getParentFile();
            fieldFilename.setText(file.getAbsolutePath());
        }
    }//GEN-LAST:event_buttonSelectFileActionPerformed

    private void buttonOkActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonOkActionPerformed
        ok();
    }//GEN-LAST:event_buttonOkActionPerformed

    private void spinnerScaleStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_spinnerScaleStateChanged
        if (image != null) {
            updateWorldDimensions();
        }
    }//GEN-LAST:event_spinnerScaleStateChanged

    private void spinnerOffsetXStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_spinnerOffsetXStateChanged
        if (image != null) {
            updateWorldDimensions();
        }
    }//GEN-LAST:event_spinnerOffsetXStateChanged

    private void spinnerOffsetYStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_spinnerOffsetYStateChanged
        if (image != null) {
            updateWorldDimensions();
        }
    }//GEN-LAST:event_spinnerOffsetYStateChanged

    private void radioButtonTerrainActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_radioButtonTerrainActionPerformed
        setControlStates();
    }//GEN-LAST:event_radioButtonTerrainActionPerformed

    private void radioButtonLayerActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_radioButtonLayerActionPerformed
        setControlStates();
    }//GEN-LAST:event_radioButtonLayerActionPerformed

    private void radioButtonOneToOneActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_radioButtonOneToOneActionPerformed
        setControlStates();
    }//GEN-LAST:event_radioButtonOneToOneActionPerformed

    private void radioButtonFullRangeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_radioButtonFullRangeActionPerformed
        setControlStates();
    }//GEN-LAST:event_radioButtonFullRangeActionPerformed

    private void radioButtonThresholdActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_radioButtonThresholdActionPerformed
        setControlStates();
    }//GEN-LAST:event_radioButtonThresholdActionPerformed

    private void radioButtonDitherActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_radioButtonDitherActionPerformed
        setControlStates();
    }//GEN-LAST:event_radioButtonDitherActionPerformed

    private void comboBoxLayerActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_comboBoxLayerActionPerformed
        setControlStates();
    }//GEN-LAST:event_comboBoxLayerActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton buttonCancel;
    private javax.swing.ButtonGroup buttonGroup1;
    private javax.swing.ButtonGroup buttonGroup2;
    private javax.swing.JButton buttonOk;
    private javax.swing.JButton buttonSelectFile;
    private javax.swing.JCheckBox checkBoxRemoveExisting;
    private javax.swing.JComboBox<Layer> comboBoxLayer;
    private javax.swing.JTextField fieldFilename;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel14;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JLabel labelImageDimensions;
    private javax.swing.JLabel labelWorldDimensions;
    private javax.swing.JRadioButton radioButtonDither;
    private javax.swing.JRadioButton radioButtonFullRange;
    private javax.swing.JRadioButton radioButtonLayer;
    private javax.swing.JRadioButton radioButtonOneToOne;
    private javax.swing.JRadioButton radioButtonTerrain;
    private javax.swing.JRadioButton radioButtonThreshold;
    private javax.swing.JSpinner spinnerOffsetX;
    private javax.swing.JSpinner spinnerOffsetY;
    private javax.swing.JSpinner spinnerScale;
    private javax.swing.JSpinner spinnerThreshold;
    // End of variables declaration//GEN-END:variables

    private final Dimension dimension;
    private final List<Layer> allLayers;
    private File selectedFile, masksDir;
    private volatile BufferedImage image;
    private MaskImporter maskImporter;

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ImportMaskDialog.class);
    private static final long serialVersionUID = 1L;
}