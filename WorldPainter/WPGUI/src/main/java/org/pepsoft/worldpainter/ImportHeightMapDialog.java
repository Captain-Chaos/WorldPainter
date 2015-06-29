/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/*
 * ImportHeightMapDialog.java
 *
 * Created on 22-jan-2012, 19:47:55
 */
package org.pepsoft.worldpainter;

import java.awt.Color;
import java.awt.Window;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.ImageIO;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JSpinner.NumberEditor;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileFilter;

import org.pepsoft.util.ProgressReceiver;
import org.pepsoft.util.ProgressReceiver.OperationCancelled;
import org.pepsoft.util.swing.ProgressDialog;
import org.pepsoft.util.swing.ProgressTask;
import org.pepsoft.worldpainter.heightMaps.HeightMapUtils;

import static org.pepsoft.minecraft.Constants.*;
import org.pepsoft.worldpainter.importing.HeightMapImporter;
import org.pepsoft.worldpainter.themes.SimpleTheme;
import org.pepsoft.worldpainter.themes.SimpleThemeEditor;
import org.pepsoft.worldpainter.themes.Theme;

/**
 *
 * @author pepijn
 */
public class ImportHeightMapDialog extends WorldPainterDialog implements DocumentListener, SimpleThemeEditor.ChangeListener {
    public ImportHeightMapDialog(Window parent, ColourScheme colourScheme) {
        super(parent);
        
        tileFactory = TileFactoryFactory.createNoiseTileFactory(new Random().nextLong(), Terrain.GRASS, DEFAULT_MAX_HEIGHT_2, 58, 62, false, true, 20, 1.0);
        
        initComponents();
        
        heightMapTileFactoryEditor1.setColourScheme(colourScheme);
        heightMapTileFactoryEditor1.setTheme((SimpleTheme) tileFactory.getTheme());
        heightMapTileFactoryEditor1.setChangeListener(this);
        spinnerOffsetX.setEditor(new NumberEditor(spinnerOffsetX, "0"));
        spinnerOffsetY.setEditor(new NumberEditor(spinnerOffsetY, "0"));
        pack();
        labelWarning.setVisible(false);
        
        setLocationRelativeTo(parent);
        
        fieldFilename.getDocument().addDocumentListener(this);
        
        rootPane.setDefaultButton(buttonOk);
        
        loadDefaults();
    }

    public World2 getImportedWorld() {
        final HeightMapImporter importer = new HeightMapImporter();
        importer.setImage(image);
        String name = selectedFile.getName();
        int p = name.lastIndexOf('.');
        if (p != -1) {
            name = name.substring(0, p);
        }
        importer.setName(name);
        importer.setTileFactory(tileFactory);
        importer.setScale((Integer) spinnerScale.getValue());
        importer.setInvert(checkBoxInvert.isSelected());
        importer.setMaxHeight(Integer.parseInt((String) comboBoxHeight.getSelectedItem()));
        importer.setImageLowLevel((Integer) spinnerImageLow.getValue());
        importer.setImageHighLevel((Integer) spinnerImageHigh.getValue());
        importer.setWorldLowLevel((Integer) spinnerWorldLow.getValue());
        importer.setWorldWaterLevel((Integer) spinnerWorldMiddle.getValue());
        importer.setWorldHighLevel((Integer) spinnerWorldHigh.getValue());
        importer.setVoidBelowLevel(checkBoxVoid.isSelected() ? ((Integer) spinnerVoidBelow.getValue()) : 0);
        importer.setOffsetX((Integer) spinnerOffsetX.getValue());
        importer.setOffsetY((Integer) spinnerOffsetY.getValue());
        World2 world = ProgressDialog.executeTask(this, new ProgressTask<World2>() {
            @Override
            public String getName() {
                return "Importing height map";
            }

            @Override
            public World2 execute(ProgressReceiver progressReceiver) throws OperationCancelled {
                return importer.doImport(progressReceiver);
            }
        }, false);
        Configuration.getInstance().setHeightMapsDirectory(selectedFile.getParentFile());
        return world;
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

    // SimpleThemeEditor.ChangeListener
    
    @Override
    public void settingsModified(SimpleThemeEditor editor) {
        buttonSaveAsDefaults.setEnabled(true);
        buttonLoadDefaults.setEnabled(true);
    }

    private void setControlStates() {
        File file = new File(fieldFilename.getText());
        if ((file.isFile()) && ((selectedFile == null) || (! file.equals(selectedFile)))) {
            selectedFile = file;
            loadImage();
        }
        boolean fileSelected = (selectedFile != null) && selectedFile.isFile();
        buttonOk.setEnabled(fileSelected);
    }

    private void loadImage() {
        try {
            image = null; // Set image to null first to make more memory available for loading the new image
            image = ImageIO.read(selectedFile);
            if (image == null) {
                labelImageDimensions.setForeground(Color.RED);
                labelImageDimensions.setText(String.format("Not an image file, or damaged file!"));
                selectedFile = null;
            } else if ((image.getType() == BufferedImage.TYPE_BYTE_BINARY) || (image.getType() == BufferedImage.TYPE_BYTE_INDEXED)) {
                labelImageDimensions.setForeground(Color.RED);
                labelImageDimensions.setText(String.format("Indexed image not supported! Please convert to non-indexed."));
                selectedFile = null;
            } else if (image.isAlphaPremultiplied()) {
                labelImageDimensions.setForeground(Color.RED);
                labelImageDimensions.setText(String.format("Premultiplied alpha not supported! Please convert to non-premultiplied."));
                selectedFile = null;
            } else {
                if (image.getType() == BufferedImage.TYPE_CUSTOM) {
                    spinnerScale.setValue(100);
                    spinnerScale.setEnabled(false);
                    spinnerScale.setToolTipText("<html>Scaling not supported for grey scale images with an alpha channel!<br>To enable scaling, please remove the alpha channel.</html>");
                } else {
                    spinnerScale.setEnabled(true);
                    spinnerScale.setToolTipText(null);
                }
                labelImageDimensions.setForeground(null);
                int width = image.getWidth(), height = image.getHeight();
                bitDepth = image.getSampleModel().getSampleSize(0);
                WritableRaster raster = image.getRaster();
                imageLowValue = Integer.MAX_VALUE;
                imageHighValue = Integer.MIN_VALUE;
outer:          for (int x = 0; x < width; x++) {
                    for (int y = 0; y < height; y++) {
                        int value = raster.getSample(x, y, 0);
                        if (value < imageLowValue) {
                            imageLowValue = value;
                        }
                        if (value > imageHighValue) {
                            imageHighValue = value;
                        }
                        if ((imageLowValue == 0) && ((bitDepth == 16) ? (imageHighValue == 65535) : (imageHighValue == 255))) {
                            // No point in looking any further!
                            break outer;
                        }
                    }
                }
                ((SpinnerNumberModel) spinnerImageHigh.getModel()).setMaximum((bitDepth == 16) ? 65535 : 255);

                // Determine maxHeight and whether to default to scaled mode
                int maxHeight;
                if (imageHighValue < 256) {
                    maxHeight = 256;
                } else if (imageHighValue < 512) {
                    maxHeight = 512;
                } else if (imageHighValue < 1024) {
                    maxHeight = 1024;
                } else if (imageHighValue < 2048) {
                    maxHeight = 2048;
                } else {
                    maxHeight = 256;
                }
                comboBoxHeight.setSelectedItem(Integer.toString(maxHeight));
                ((SpinnerNumberModel) spinnerWorldLow.getModel()).setMaximum(maxHeight - 1);
                ((SpinnerNumberModel) spinnerWorldMiddle.getModel()).setMaximum(maxHeight - 1);
                ((SpinnerNumberModel) spinnerWorldHigh.getModel()).setMaximum(maxHeight - 1);
                ((SpinnerNumberModel) spinnerVoidBelow.getModel()).setMaximum(maxHeight - 1);
                
                // Set levels to reasonable defaults
                spinnerImageLow.setValue(imageLowValue);
                spinnerImageHigh.setValue(imageHighValue);
                if (imageHighValue >= 2048) {
                    spinnerWorldLow.setValue(imageLowValue / 256);
                    spinnerWorldHigh.setValue(imageHighValue / 256);
                } else {
                    spinnerWorldLow.setValue(imageLowValue);
                    spinnerWorldHigh.setValue(imageHighValue);
                }

                labelImageDimensions.setText(String.format("Image size: %d x %d, %d bits, lowest value: %d, highest value: %d", width, height, bitDepth, imageLowValue, imageHighValue));
                updateWorldDimensions();
                updateImageWaterLevel();
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "I/O error loading image " + selectedFile, e);
            labelImageDimensions.setForeground(Color.RED);
            labelImageDimensions.setText(String.format("I/O error loading image (message: " + e.getMessage() + ")!"));
            selectedFile = null;
        }
    }

    private void updateWorldDimensions() {
        int scale = (Integer) spinnerScale.getValue();
        labelWorldDimensions.setText("Scaled size: " + (image.getWidth() * scale / 100) + " x " + (image.getHeight() * scale / 100) + " blocks");
    }
    
    private void updateImageWaterLevel() {
        int imageLowLevel    = (Integer) spinnerImageLow.getValue();
        int imageHighLevel   = (Integer) spinnerImageHigh.getValue();
        int worldLowLevel    = (Integer) spinnerWorldLow.getValue();
        int worldMiddleLevel = (Integer) spinnerWorldMiddle.getValue();
        int worldHighLevel   = (Integer) spinnerWorldHigh.getValue();
        float levelScale = (float) (worldHighLevel - worldLowLevel) / (imageHighLevel - imageLowLevel);
        int imageMiddleLevel = (int) ((worldMiddleLevel - worldLowLevel) / levelScale + imageLowLevel);
        if (imageMiddleLevel < 0) {
            labelImageWaterLevel.setText("< 0");
        } else if (imageMiddleLevel > ((bitDepth == 16) ? 65535 : 255)) {
            labelImageWaterLevel.setText(((bitDepth == 16) ? "> 65535" : "> 255"));
        } else {
            labelImageWaterLevel.setText(Integer.toString(imageMiddleLevel));
        }
    }

    private void loadDefaults() {
        Theme defaults = Configuration.getInstance().getHeightMapDefaultTheme();
        if (defaults == null) {
            HeightMapTileFactory tmpTileFactory = TileFactoryFactory.createNoiseTileFactory(seed, Terrain.GRASS, DEFAULT_MAX_HEIGHT_2, 58, 62, false, true, 20, 1.0);
            defaults = tmpTileFactory.getTheme();
            buttonResetDefaults.setEnabled(false);
        } else {
            buttonResetDefaults.setEnabled(true);
        }
        tileFactory.setTheme(defaults);
        spinnerWorldMiddle.setValue(defaults.getWaterHeight());
        updateImageWaterLevel();
        heightMapTileFactoryEditor1.setTheme((SimpleTheme) defaults);
        buttonLoadDefaults.setEnabled(false);
        buttonSaveAsDefaults.setEnabled(false);
    }

    private void saveAsDefaults() {
        if (heightMapTileFactoryEditor1.save()) {
            Theme defaults = heightMapTileFactoryEditor1.getTheme();
            Configuration.getInstance().setHeightMapDefaultTheme(defaults);
            buttonResetDefaults.setEnabled(true);
            buttonLoadDefaults.setEnabled(false);
            buttonSaveAsDefaults.setEnabled(false);
        }
    }

    private void resetDefaults() {
        Configuration.getInstance().setHeightMapDefaultTheme(null);
        loadDefaults();
        buttonSaveAsDefaults.setEnabled(false);
        JOptionPane.showMessageDialog(this, "Theme reset to factory defaults.", "Default Theme Reset", JOptionPane.INFORMATION_MESSAGE);
    }
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        jLabel14 = new javax.swing.JLabel();
        jLabel1 = new javax.swing.JLabel();
        fieldFilename = new javax.swing.JTextField();
        buttonSelectFile = new javax.swing.JButton();
        labelImageDimensions = new javax.swing.JLabel();
        buttonCancel = new javax.swing.JButton();
        buttonOk = new javax.swing.JButton();
        checkBoxInvert = new javax.swing.JCheckBox();
        jTabbedPane1 = new javax.swing.JTabbedPane();
        jPanel2 = new javax.swing.JPanel();
        jLabel3 = new javax.swing.JLabel();
        spinnerScale = new javax.swing.JSpinner();
        labelWorldDimensions = new javax.swing.JLabel();
        comboBoxHeight = new javax.swing.JComboBox();
        jLabel4 = new javax.swing.JLabel();
        jPanel1 = new javax.swing.JPanel();
        jLabel5 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        spinnerImageLow = new javax.swing.JSpinner();
        spinnerWorldLow = new javax.swing.JSpinner();
        jLabel8 = new javax.swing.JLabel();
        labelImageWaterLevel = new javax.swing.JLabel();
        spinnerWorldMiddle = new javax.swing.JSpinner();
        jLabel9 = new javax.swing.JLabel();
        spinnerImageHigh = new javax.swing.JSpinner();
        spinnerWorldHigh = new javax.swing.JSpinner();
        jLabel2 = new javax.swing.JLabel();
        jLabel10 = new javax.swing.JLabel();
        checkBoxVoid = new javax.swing.JCheckBox();
        spinnerVoidBelow = new javax.swing.JSpinner();
        labelWarning = new javax.swing.JLabel();
        jLabel11 = new javax.swing.JLabel();
        spinnerOffsetX = new javax.swing.JSpinner();
        jLabel12 = new javax.swing.JLabel();
        spinnerOffsetY = new javax.swing.JSpinner();
        jPanel3 = new javax.swing.JPanel();
        heightMapTileFactoryEditor1 = new org.pepsoft.worldpainter.themes.SimpleThemeEditor();
        buttonLoadDefaults = new javax.swing.JButton();
        buttonSaveAsDefaults = new javax.swing.JButton();
        buttonResetDefaults = new javax.swing.JButton();

        jLabel14.setText("jLabel14");

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Import Height Map");

        jLabel1.setText("Select the image to import as a height map:");

        buttonSelectFile.setText("...");
        buttonSelectFile.addActionListener(this::buttonSelectFileActionPerformed);

        labelImageDimensions.setText("Image size: ? x ?, bit depth: ?, lowest value: ?, highest value: ?");

        buttonCancel.setText("Cancel");
        buttonCancel.addActionListener(this::buttonCancelActionPerformed);

        buttonOk.setText("OK");
        buttonOk.setEnabled(false);
        buttonOk.addActionListener(this::buttonOkActionPerformed);

        checkBoxInvert.setText("Invert (white is low, black is high)");

        jTabbedPane1.addChangeListener(this::jTabbedPane1StateChanged);

        jLabel3.setText("Scale:");

        spinnerScale.setModel(new javax.swing.SpinnerNumberModel(100, 1, 999, 1));
        spinnerScale.addChangeListener(this::spinnerScaleStateChanged);

        labelWorldDimensions.setText("Scaled size: ? x ? blocks");

        comboBoxHeight.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "32", "64", "128", "256", "512", "1024", "2048" }));
        comboBoxHeight.setSelectedIndex(3);
        comboBoxHeight.addActionListener(this::comboBoxHeightActionPerformed);

        jLabel4.setText("%");

        jPanel1.setLayout(new java.awt.GridBagLayout());

        jLabel5.setText("Image:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
        jPanel1.add(jLabel5, gridBagConstraints);

        jLabel6.setText("Minecraft:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
        jPanel1.add(jLabel6, gridBagConstraints);

        jLabel7.setText("Bottom:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
        jPanel1.add(jLabel7, gridBagConstraints);

        spinnerImageLow.setModel(new javax.swing.SpinnerNumberModel(0, 0, 65535, 1));
        spinnerImageLow.addChangeListener(this::spinnerImageLowStateChanged);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
        jPanel1.add(spinnerImageLow, gridBagConstraints);

        spinnerWorldLow.setModel(new javax.swing.SpinnerNumberModel(0, 0, 65535, 1));
        spinnerWorldLow.addChangeListener(this::spinnerWorldLowStateChanged);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
        jPanel1.add(spinnerWorldLow, gridBagConstraints);

        jLabel8.setText("Water level:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
        jPanel1.add(jLabel8, gridBagConstraints);

        labelImageWaterLevel.setText("62");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_END;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 19);
        jPanel1.add(labelImageWaterLevel, gridBagConstraints);

        spinnerWorldMiddle.setModel(new javax.swing.SpinnerNumberModel(62, 0, 65535, 1));
        spinnerWorldMiddle.addChangeListener(this::spinnerWorldMiddleStateChanged);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
        jPanel1.add(spinnerWorldMiddle, gridBagConstraints);

        jLabel9.setText("Top:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
        jPanel1.add(jLabel9, gridBagConstraints);

        spinnerImageHigh.setModel(new javax.swing.SpinnerNumberModel(255, 0, 65535, 1));
        spinnerImageHigh.addChangeListener(this::spinnerImageHighStateChanged);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
        jPanel1.add(spinnerImageHigh, gridBagConstraints);

        spinnerWorldHigh.setModel(new javax.swing.SpinnerNumberModel(255, 0, 65535, 1));
        spinnerWorldHigh.addChangeListener(this::spinnerWorldHighStateChanged);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
        jPanel1.add(spinnerWorldHigh, gridBagConstraints);

        jLabel2.setText("Height:");

        jLabel10.setText("blocks");

        checkBoxVoid.setText("create Void below image value:");
        checkBoxVoid.addActionListener(this::checkBoxVoidActionPerformed);

        spinnerVoidBelow.setModel(new javax.swing.SpinnerNumberModel(1, 1, 255, 1));
        spinnerVoidBelow.setEnabled(false);

        labelWarning.setFont(labelWarning.getFont().deriveFont(labelWarning.getFont().getStyle() | java.awt.Font.BOLD));
        labelWarning.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/pepsoft/worldpainter/icons/error.png"))); // NOI18N
        labelWarning.setText("Only Minecraft 1.1, with mods!");

        jLabel11.setText("Offset:");
        jLabel11.setToolTipText("The origin of the height map will be at these coordinates in the map");

        spinnerOffsetX.setModel(new javax.swing.SpinnerNumberModel(0, -999999, 999999, 1));
        spinnerOffsetX.setToolTipText("The origin of the height map will be at these coordinates in the map");
        spinnerOffsetX.addChangeListener(this::spinnerOffsetXStateChanged);

        jLabel12.setText(",");
        jLabel12.setToolTipText("The origin of the height map will be at these coordinates in the map");

        spinnerOffsetY.setModel(new javax.swing.SpinnerNumberModel(0, -999999, 999999, 1));
        spinnerOffsetY.setToolTipText("The origin of the height map will be at these coordinates in the map");
        spinnerOffsetY.addChangeListener(this::spinnerOffsetYStateChanged);

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(jLabel3)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(spinnerScale, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 0, 0)
                        .addComponent(jLabel4)
                        .addGap(18, 18, 18)
                        .addComponent(jLabel11)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(spinnerOffsetX, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 0, 0)
                        .addComponent(jLabel12)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(spinnerOffsetY, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(labelWorldDimensions)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(jLabel2)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(comboBoxHeight, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 0, 0)
                        .addComponent(jLabel10)
                        .addGap(18, 18, 18)
                        .addComponent(labelWarning))
                    .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(checkBoxVoid)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(spinnerVoidBelow, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3)
                    .addComponent(spinnerScale, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel4)
                    .addComponent(jLabel11)
                    .addComponent(spinnerOffsetX, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel12)
                    .addComponent(spinnerOffsetY, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(labelWorldDimensions)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(comboBoxHeight, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel10)
                    .addComponent(labelWarning))
                .addGap(18, 18, 18)
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(checkBoxVoid)
                    .addComponent(spinnerVoidBelow, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab("Scaling", jPanel2);

        buttonLoadDefaults.setText("Load Defaults");
        buttonLoadDefaults.addActionListener(this::buttonLoadDefaultsActionPerformed);

        buttonSaveAsDefaults.setText("Save As Defaults");
        buttonSaveAsDefaults.addActionListener(this::buttonSaveAsDefaultsActionPerformed);

        buttonResetDefaults.setText("Reset");
        buttonResetDefaults.addActionListener(this::buttonResetDefaultsActionPerformed);

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(heightMapTileFactoryEditor1, javax.swing.GroupLayout.DEFAULT_SIZE, 408, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel3Layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(buttonResetDefaults)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(buttonSaveAsDefaults)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(buttonLoadDefaults)))
                .addContainerGap())
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(heightMapTileFactoryEditor1, javax.swing.GroupLayout.DEFAULT_SIZE, 190, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(buttonLoadDefaults)
                    .addComponent(buttonSaveAsDefaults)
                    .addComponent(buttonResetDefaults))
                .addContainerGap())
        );

        jTabbedPane1.addTab("Terrain", jPanel3);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jTabbedPane1)
                    .addComponent(jLabel1)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(fieldFilename)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(buttonSelectFile))
                    .addComponent(labelImageDimensions)
                    .addComponent(checkBoxInvert)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(buttonOk)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(buttonCancel)))
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
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(checkBoxInvert)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jTabbedPane1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(buttonCancel)
                    .addComponent(buttonOk))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void spinnerImageLowStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_spinnerImageLowStateChanged
        int lowLevel    = (Integer) spinnerImageLow.getValue();
        int highLevel   = (Integer) spinnerImageHigh.getValue();
        if (lowLevel > highLevel) {
            spinnerImageHigh.setValue(lowLevel);
        }
        updateImageWaterLevel();
    }//GEN-LAST:event_spinnerImageLowStateChanged

    private void buttonCancelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonCancelActionPerformed
        cancel();
    }//GEN-LAST:event_buttonCancelActionPerformed

    private void spinnerImageHighStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_spinnerImageHighStateChanged
        int lowLevel    = (Integer) spinnerImageLow.getValue();
        int highLevel   = (Integer) spinnerImageHigh.getValue();
        if (highLevel < lowLevel) {
            spinnerImageLow.setValue(highLevel);
        }
        updateImageWaterLevel();
    }//GEN-LAST:event_spinnerImageHighStateChanged

    private void spinnerWorldLowStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_spinnerWorldLowStateChanged
        int lowLevel    = (Integer) spinnerWorldLow.getValue();
//        int middleLevel = (Integer) spinnerWorldMiddle.getValue();
        int highLevel   = (Integer) spinnerWorldHigh.getValue();
//        if (lowLevel > middleLevel) {
//            spinnerWorldMiddle.setValue(lowLevel);
//        }
        if (lowLevel > highLevel) {
            spinnerWorldHigh.setValue(lowLevel);
        }
        updateImageWaterLevel();
    }//GEN-LAST:event_spinnerWorldLowStateChanged

    private void spinnerWorldMiddleStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_spinnerWorldMiddleStateChanged
//        int lowLevel    = (Integer) spinnerWorldLow.getValue();
//        int middleLevel = (Integer) spinnerWorldMiddle.getValue();
//        int highLevel   = (Integer) spinnerWorldHigh.getValue();
//        if (middleLevel < lowLevel) {
//            spinnerWorldLow.setValue(middleLevel);
//        }
//        if (middleLevel > highLevel) {
//            spinnerWorldHigh.setValue(middleLevel);
//        }
        updateImageWaterLevel();
        int waterLevel = ((Number) spinnerWorldMiddle.getValue()).intValue();
        tileFactory.setWaterHeight(waterLevel);
        float baseHeight = tileFactory.getBaseHeight();
        float transposeAmount = (waterLevel - 4) - baseHeight;
        tileFactory.setHeightMap(HeightMapUtils.transposeHeightMap(tileFactory.getHeightMap(), transposeAmount));
        heightMapTileFactoryEditor1.setTheme((SimpleTheme) tileFactory.getTheme());
    }//GEN-LAST:event_spinnerWorldMiddleStateChanged

    private void spinnerWorldHighStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_spinnerWorldHighStateChanged
        int lowLevel    = (Integer) spinnerWorldLow.getValue();
//        int middleLevel = (Integer) spinnerWorldMiddle.getValue();
        int highLevel   = (Integer) spinnerWorldHigh.getValue();
        if (highLevel < lowLevel) {
            spinnerWorldLow.setValue(highLevel);
        }
//        if (highLevel < middleLevel) {
//            spinnerWorldMiddle.setValue(highLevel);
//        }
        updateImageWaterLevel();
    }//GEN-LAST:event_spinnerWorldHighStateChanged

    private void buttonSelectFileActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonSelectFileActionPerformed
        JFileChooser fileChooser = new JFileChooser();
        Configuration config = Configuration.getInstance();
        if (heightMapDir != null) {
            fileChooser.setCurrentDirectory(heightMapDir);
        } else if (config.getHeightMapsDirectory() != null) {
            fileChooser.setCurrentDirectory(config.getHeightMapsDirectory());
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
        fileChooser.setFileFilter(new FileFilter() {
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
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            heightMapDir = file.getParentFile();
            fieldFilename.setText(file.getAbsolutePath());
        }
    }//GEN-LAST:event_buttonSelectFileActionPerformed

    private void spinnerScaleStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_spinnerScaleStateChanged
        if (image != null) {
            updateWorldDimensions();
        }
    }//GEN-LAST:event_spinnerScaleStateChanged

    private void buttonOkActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonOkActionPerformed
        if ((jTabbedPane1.getSelectedIndex() == 0) || heightMapTileFactoryEditor1.save()) {
            spinnerWorldMiddle.setValue(tileFactory.getWaterHeight());
            ok();
        }
    }//GEN-LAST:event_buttonOkActionPerformed

    private void comboBoxHeightActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_comboBoxHeightActionPerformed
        int maxHeight = Integer.parseInt((String) comboBoxHeight.getSelectedItem());
        if (maxHeight != previousMaxHeight) {
            if ((Integer) spinnerWorldLow.getValue() >= maxHeight) {
                spinnerWorldLow.setValue(maxHeight - 1);
            }
            if ((Integer) spinnerWorldMiddle.getValue() >= maxHeight) {
                spinnerWorldMiddle.setValue(maxHeight - 1);
            }
            if ((Integer) spinnerWorldHigh.getValue() >= maxHeight) {
                spinnerWorldHigh.setValue(maxHeight - 1);
            }
            if ((Integer) spinnerVoidBelow.getValue() >= maxHeight) {
                spinnerVoidBelow.setValue(maxHeight - 1);
            }
            ((SpinnerNumberModel) spinnerWorldLow.getModel()).setMaximum(maxHeight - 1);
            ((SpinnerNumberModel) spinnerWorldMiddle.getModel()).setMaximum(maxHeight - 1);
            ((SpinnerNumberModel) spinnerWorldHigh.getModel()).setMaximum(maxHeight - 1);
            ((SpinnerNumberModel) spinnerVoidBelow.getModel()).setMaximum(maxHeight - 1);
            
            HeightTransform transform = HeightTransform.get(maxHeight * 100 / previousMaxHeight, 0);
            tileFactory.setMaxHeight(maxHeight, transform);
            int waterLevel = ((Number) spinnerWorldMiddle.getValue()).intValue();
            tileFactory.setWaterHeight(waterLevel);
            float baseHeight = tileFactory.getBaseHeight();
            float transposeAmount = Math.max(waterLevel - 4, 0) - baseHeight;
            tileFactory.setHeightMap(HeightMapUtils.transposeHeightMap(tileFactory.getHeightMap(), transposeAmount));
            heightMapTileFactoryEditor1.setTheme((SimpleTheme) tileFactory.getTheme());
            if (maxHeight == DEFAULT_MAX_HEIGHT_1) {
                labelWarning.setText("Only Minecraft 1.1!");
            } else {
                labelWarning.setText("Only Minecraft 1.1, with mods!");
            }
            labelWarning.setVisible(maxHeight != DEFAULT_MAX_HEIGHT_2);
            
            previousMaxHeight = maxHeight;
        }
    }//GEN-LAST:event_comboBoxHeightActionPerformed

    private void jTabbedPane1StateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_jTabbedPane1StateChanged
        switch (jTabbedPane1.getSelectedIndex()) {
            case 0:
                heightMapTileFactoryEditor1.save();
                spinnerWorldMiddle.setValue(tileFactory.getWaterHeight());
                break;
        }
    }//GEN-LAST:event_jTabbedPane1StateChanged

    private void checkBoxVoidActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_checkBoxVoidActionPerformed
        spinnerVoidBelow.setEnabled(checkBoxVoid.isSelected());
    }//GEN-LAST:event_checkBoxVoidActionPerformed

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

    private void buttonLoadDefaultsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonLoadDefaultsActionPerformed
        loadDefaults();
    }//GEN-LAST:event_buttonLoadDefaultsActionPerformed

    private void buttonSaveAsDefaultsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonSaveAsDefaultsActionPerformed
        saveAsDefaults();
    }//GEN-LAST:event_buttonSaveAsDefaultsActionPerformed

    private void buttonResetDefaultsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonResetDefaultsActionPerformed
        resetDefaults();
    }//GEN-LAST:event_buttonResetDefaultsActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton buttonCancel;
    private javax.swing.JButton buttonLoadDefaults;
    private javax.swing.JButton buttonOk;
    private javax.swing.JButton buttonResetDefaults;
    private javax.swing.JButton buttonSaveAsDefaults;
    private javax.swing.JButton buttonSelectFile;
    private javax.swing.JCheckBox checkBoxInvert;
    private javax.swing.JCheckBox checkBoxVoid;
    private javax.swing.JComboBox comboBoxHeight;
    private javax.swing.JTextField fieldFilename;
    private org.pepsoft.worldpainter.themes.SimpleThemeEditor heightMapTileFactoryEditor1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
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
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JLabel labelImageDimensions;
    private javax.swing.JLabel labelImageWaterLevel;
    private javax.swing.JLabel labelWarning;
    private javax.swing.JLabel labelWorldDimensions;
    private javax.swing.JSpinner spinnerImageHigh;
    private javax.swing.JSpinner spinnerImageLow;
    private javax.swing.JSpinner spinnerOffsetX;
    private javax.swing.JSpinner spinnerOffsetY;
    private javax.swing.JSpinner spinnerScale;
    private javax.swing.JSpinner spinnerVoidBelow;
    private javax.swing.JSpinner spinnerWorldHigh;
    private javax.swing.JSpinner spinnerWorldLow;
    private javax.swing.JSpinner spinnerWorldMiddle;
    // End of variables declaration//GEN-END:variables

    private final HeightMapTileFactory tileFactory;
    private final long seed = new Random().nextLong();
    private File selectedFile, heightMapDir;
    private volatile BufferedImage image;
    private int previousMaxHeight = DEFAULT_MAX_HEIGHT_2, bitDepth = 8, imageLowValue = 32, imageHighValue = 224;
    
    private static final Logger logger = Logger.getLogger(ImportHeightMapDialog.class.getName());
    private static final long serialVersionUID = 1L;
}