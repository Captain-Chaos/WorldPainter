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

import org.pepsoft.util.FileUtils;
import org.pepsoft.util.ProgressReceiver;
import org.pepsoft.util.ProgressReceiver.OperationCancelled;
import org.pepsoft.util.swing.ProgressDialog;
import org.pepsoft.util.swing.ProgressTask;
import org.pepsoft.util.swing.TileProvider;
import org.pepsoft.worldpainter.heightMaps.*;
import org.pepsoft.worldpainter.importing.HeightMapImporter;
import org.pepsoft.worldpainter.layers.Void;
import org.pepsoft.worldpainter.plugins.PlatformManager;
import org.pepsoft.worldpainter.themes.SimpleTheme;
import org.pepsoft.worldpainter.themes.Theme;
import org.pepsoft.worldpainter.themes.impl.simple.SimpleThemeEditor;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.JSpinner.NumberEditor;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.*;

import static org.pepsoft.minecraft.Constants.DEFAULT_MAX_HEIGHT_ANVIL;
import static org.pepsoft.minecraft.Constants.DEFAULT_MAX_HEIGHT_MCREGION;
import static org.pepsoft.util.AwtUtils.doLaterOnEventThread;
import static org.pepsoft.util.swing.ProgressDialog.NOT_CANCELABLE;
import static org.pepsoft.util.swing.SpinnerUtils.setMaximum;
import static org.pepsoft.worldpainter.Constants.MAX_HEIGHT;
import static org.pepsoft.worldpainter.DefaultPlugin.JAVA_ANVIL;
import static org.pepsoft.worldpainter.DefaultPlugin.JAVA_MCREGION;
import static org.pepsoft.worldpainter.util.LayoutUtils.setDefaultSizeAndLocation;
import static org.pepsoft.worldpainter.util.MinecraftUtil.blocksToWalkingTime;

/**
 *
 * @author pepijn
 */
public class ImportHeightMapDialog extends WorldPainterDialog implements DocumentListener, SimpleThemeEditor.ChangeListener {
    public ImportHeightMapDialog(Window parent, ColourScheme colourScheme, boolean contourLines, int contourSeparation, TileRenderer.LightOrigin lightOrigin) {
        this(parent, null, colourScheme, contourLines, contourSeparation, lightOrigin);
    }

    public ImportHeightMapDialog(Window parent, Dimension currentDimension, ColourScheme colourScheme, boolean contourLines, int contourSeparation, TileRenderer.LightOrigin lightOrigin) {
        super(parent);
        this.currentDimension = currentDimension;
        this.colourScheme = colourScheme;
        this.contourLines = contourLines;
        this.contourSeparation = contourSeparation;
        this.lightOrigin = lightOrigin;

        initComponents();
        tiledImageViewer2.setZoom(0);
        tiledImageViewerContainer1.setView(tiledImageViewer2);
        
        themeEditor.setColourScheme(colourScheme);
        spinnerOffsetX.setEditor(new NumberEditor(spinnerOffsetX, "0"));
        spinnerOffsetY.setEditor(new NumberEditor(spinnerOffsetY, "0"));
        checkBoxCreateTiles.setSelected(true);
        labelWarning.setVisible(false);
        comboBoxPlatform.setModel(new DefaultComboBoxModel<>(PlatformManager.getInstance().getAllPlatforms().toArray(new Platform[0])));

        fieldFilename.getDocument().addDocumentListener(this);
        
        rootPane.setDefaultButton(buttonOk);
        
        if (currentDimension != null) {
            jTabbedPane1.setEnabledAt(1, false);
            comboBoxPlatform.setSelectedItem(currentDimension.getWorld().getPlatform());
            comboBoxPlatform.setEnabled(false);
            comboBoxHeight.setSelectedItem(currentDimension.getMaxHeight());
            comboBoxHeight.setEnabled(false);
            buttonResetDefaults.setEnabled(false);
            spinnerWorldMiddle.setValue(((HeightMapTileFactory) currentDimension.getTileFactory()).getTheme().clone().getWaterHeight());
            buttonLoadDefaults.setEnabled(true);
            buttonSaveAsDefaults.setEnabled(true);
        } else {
            themeEditor.setTheme((SimpleTheme) TileFactoryFactory.createNoiseTileFactory(new Random().nextLong(), Terrain.GRASS, DEFAULT_MAX_HEIGHT_ANVIL, 58, 62, false, true, 20, 1.0).getTheme());
            themeEditor.setChangeListener(this);
            comboBoxPlatform.setSelectedItem(JAVA_ANVIL);
            labelNoUndo.setText(" ");
            checkBoxCreateTiles.setEnabled(false);
            checkBoxOnlyRaise.setEnabled(false);
            loadDefaults();
        }

        scaleToUI();
        pack();
        setDefaultSizeAndLocation(this, 60);

        setControlStates();
        initialising = false;
    }

    public World2 getImportedWorld() {
        if (currentDimension != null) {
            throw new IllegalStateException();
        }
        final HeightMapImporter importer = createImporter();
        World2 world = ProgressDialog.executeTask(this, new ProgressTask<World2>() {
            @Override
            public String getName() {
                return "Importing height map";
            }

            @Override
            public World2 execute(ProgressReceiver progressReceiver) throws OperationCancelled {
                return importer.importToNewWorld(progressReceiver);
            }
        }, NOT_CANCELABLE);
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
        if (currentDimension != null) {
            buttonResetDefaults.setEnabled(true);
        }
        buttonSaveAsDefaults.setEnabled(true);
        buttonLoadDefaults.setEnabled(true);
        updatePreview();
    }

    private HeightMapImporter createImporter() {
        HeightMap heightMap = new BitmapHeightMap(selectedFile.getName(), image, 0, selectedFile, false, false);
        int scale = (Integer) spinnerScale.getValue();
        int offsetX = (Integer) spinnerOffsetX.getValue();
        int offsetY = (Integer) spinnerOffsetY.getValue();
        if ((scale != 100) || (offsetX != 0) || (offsetY != 0)) {
            if (scale != 100) {
                ((BitmapHeightMap) heightMap).setSmoothScaling(true);
            }
            heightMap = new TransformingHeightMap(heightMap.getName() + " transformed", heightMap, scale, scale, offsetX, offsetY, 0);
        }
        if (checkBoxInvert.isSelected()) {
            if (image.getSampleModel().getSampleSize(0) == 16) {
                heightMap = new DifferenceHeightMap(new ConstantHeightMap(65535f), heightMap);
            } else {
                heightMap = new DifferenceHeightMap(new ConstantHeightMap(255f), heightMap);
            }
        }

        String name = selectedFile.getName();
        int p = name.lastIndexOf('.');
        if (p != -1) {
            name = name.substring(0, p);
        }

        int waterLevel = (int) spinnerWorldMiddle.getValue();
        int maxHeight = (int) comboBoxHeight.getSelectedItem();

        HeightMapImporter importer = new HeightMapImporter();
        importer.setPlatform((Platform) comboBoxPlatform.getSelectedItem());
        importer.setHeightMap(heightMap);
        importer.setImageFile(selectedFile);
        importer.setName(name);
        if (currentDimension != null) {
            importer.setTileFactory(currentDimension.getTileFactory());
        } else {
            themeEditor.save();
            SimpleTheme theme = themeEditor.getTheme();
            theme.setMaxHeight(maxHeight);
            importer.setTileFactory(new HeightMapTileFactory(seed, new SumHeightMap(new ConstantHeightMap(waterLevel - 4), new NoiseHeightMap((float) 20, 1.0, 1, 0)), maxHeight, false, theme));
        }
        importer.setMaxHeight(maxHeight);
        importer.setImageLowLevel((Integer) spinnerImageLow.getValue());
        importer.setImageHighLevel((Integer) spinnerImageHigh.getValue());
        importer.setWorldLowLevel((Integer) spinnerWorldLow.getValue());
        importer.setWorldWaterLevel(waterLevel);
        importer.setWorldHighLevel((Integer) spinnerWorldHigh.getValue());
        importer.setVoidBelowLevel(checkBoxVoid.isSelected() ? ((Integer) spinnerVoidBelow.getValue()) : 0);
        return importer;
    }

    private void setControlStates() {
        File file = new File(fieldFilename.getText());
        if (file.isFile() && (!file.equals(selectedFile))) {
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
                labelImageDimensions.setText("Not an image file, or damaged file!");
                selectedFile = null;
            } else if ((image.getType() == BufferedImage.TYPE_BYTE_BINARY) || (image.getType() == BufferedImage.TYPE_BYTE_INDEXED)) {
                labelImageDimensions.setForeground(Color.RED);
                labelImageDimensions.setText("Indexed image not supported! Please convert to non-indexed.");
                selectedFile = null;
            } else if (image.isAlphaPremultiplied()) {
                labelImageDimensions.setForeground(Color.RED);
                labelImageDimensions.setText("Premultiplied alpha not supported! Please convert to non-premultiplied.");
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
                int imageLowValue = Integer.MAX_VALUE;
                imageHighValue = Integer.MIN_VALUE;
                int imageMaxHeight = (int) Math.pow(2, bitDepth) - 1;
                boolean invert = checkBoxInvert.isSelected();
outer:          for (int x = 0; x < width; x++) {
                    for (int y = 0; y < height; y++) {
                        int value = invert ? (imageMaxHeight - raster.getSample(x, y, 0)) : raster.getSample(x, y, 0);
                        if (value < imageLowValue) {
                            imageLowValue = value;
                        }
                        if (value > imageHighValue) {
                            imageHighValue = value;
                        }
                        if ((imageLowValue == 0) && (imageHighValue == imageMaxHeight)) {
                            // No point in looking any further!
                            break outer;
                        }
                    }
                }
                setMaximum(spinnerImageLow, imageMaxHeight);
                setMaximum(spinnerImageHigh, imageMaxHeight);

                // Set levels to reasonable defaults
                selectDefaultVerticalScaling();

                labelImageDimensions.setText(String.format("Image size: %d x %d, %d bits, lowest value: %d, highest value: %d", width, height, bitDepth, imageLowValue, imageHighValue));
                updateWorldDimensions();
                updatePreview();
            }
        } catch (IOException e) {
            logger.error("I/O error loading image " + selectedFile, e);
            labelImageDimensions.setForeground(Color.RED);
            labelImageDimensions.setText(String.format("I/O error loading image (message: %s)!", e.getMessage()));
            selectedFile = null;
        }
    }

    private void selectDefaultVerticalScaling() {
        int imageMaxHeight = (int) Math.pow(2, bitDepth) - 1;
        int platformMaxHeight = (int) comboBoxHeight.getSelectedItem();
        spinnerImageLow.setValue(0);
        if (imageHighValue < platformMaxHeight) {
            // The image fits beneath the maximum platform height
            // unscaled; set default vertical scale to 1:1
            spinnerImageHigh.setValue(Math.min(imageMaxHeight, platformMaxHeight - 1));
        } else if ((bitDepth == 16) && (imageHighValue < (platformMaxHeight << 8))) {
            // The image is 16-bit and fits beneath the maximum platform
            // height when divided by 256; set default vertical scale
            // to 256:1
            spinnerImageHigh.setValue(Math.min(imageMaxHeight, (platformMaxHeight << 8) - 1));
        } else {
            // The image does not fit beneath the maximum platform
            // height so just set the high mark to the maximum
            spinnerImageHigh.setValue(imageMaxHeight);
        }
        spinnerWorldLow.setValue(0);
        spinnerWorldHigh.setValue(Math.min(imageMaxHeight, platformMaxHeight - 1));
        updateImageWaterLevel();
    }

    private void updateWorldDimensions() {
        int scale = (Integer) spinnerScale.getValue();
        int scaledWidth = image.getWidth() * scale / 100;
        int scaledHeight = image.getHeight() * scale / 100;
        labelWorldDimensions.setText("Scaled size: " + scaledWidth + " x " + scaledHeight + " blocks");
        String westEastTime = blocksToWalkingTime(scaledWidth);
        String northSouthTime = blocksToWalkingTime(scaledHeight);
        if (westEastTime.equals(northSouthTime)) {
            labelWalkingTime.setText(westEastTime);
        } else {
            labelWalkingTime.setText("West to east: " + westEastTime + ", north to south: " + northSouthTime);
        }
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
        Theme defaultTheme = Configuration.getInstance().getHeightMapDefaultTheme();
        if (defaultTheme == null) {
            HeightMapTileFactory tmpTileFactory = TileFactoryFactory.createNoiseTileFactory(seed, Terrain.GRASS, DEFAULT_MAX_HEIGHT_ANVIL, 58, 62, false, true, 20, 1.0);
            defaultTheme = tmpTileFactory.getTheme();
            if (currentDimension == null) {
                buttonResetDefaults.setEnabled(false);
            }
        } else {
            buttonResetDefaults.setEnabled(true);
        }
        spinnerWorldMiddle.setValue(defaultTheme.getWaterHeight());
        updateImageWaterLevel();
        updatePreview();
        themeEditor.setTheme((SimpleTheme) defaultTheme);
        buttonLoadDefaults.setEnabled(false);
        buttonSaveAsDefaults.setEnabled(false);
    }

    private void saveAsDefaults() {
        if (themeEditor.save()) {
            Theme defaults = themeEditor.getTheme();
            Configuration.getInstance().setHeightMapDefaultTheme(defaults);
            buttonResetDefaults.setEnabled(true);
            buttonLoadDefaults.setEnabled(false);
            buttonSaveAsDefaults.setEnabled(false);
        }
    }

    private void resetDefaults() {
        if (currentDimension != null) {
            Theme theme = ((HeightMapTileFactory) currentDimension.getTileFactory()).getTheme();
            buttonResetDefaults.setEnabled(false);
            spinnerWorldMiddle.setValue(theme.getWaterHeight());
            updateImageWaterLevel();
            updatePreview();
            themeEditor.setTheme((SimpleTheme) theme);
            buttonLoadDefaults.setEnabled(true);
            buttonSaveAsDefaults.setEnabled(true);
        } else {
            Configuration.getInstance().setHeightMapDefaultTheme(null);
            loadDefaults();
            buttonSaveAsDefaults.setEnabled(false);
            JOptionPane.showMessageDialog(this, "Theme reset to factory defaults.", "Default Theme Reset", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void exportToDimension() {
        if (currentDimension == null) {
            throw new IllegalStateException();
        }
        final HeightMapImporter importer = createImporter();
        importer.setOnlyRaise(checkBoxOnlyRaise.isSelected());
        ProgressDialog.executeTask(this, new ProgressTask<Void>() {
            @Override
            public String getName() {
                return "Importing height map";
            }

            @Override
            public Void execute(ProgressReceiver progressReceiver) throws OperationCancelled {
                importer.importToDimension(currentDimension, checkBoxCreateTiles.isSelected(), progressReceiver);
                return null;
            }
        }, NOT_CANCELABLE);
        Configuration.getInstance().setHeightMapsDirectory(selectedFile.getParentFile());
        currentDimension.clearUndo();
        currentDimension.armSavePoint();
    }

    @Override
    protected void ok() {
        if (currentDimension != null) {
            exportToDimension();
        }
        super.ok();
    }

    private void platformChanged() {
        Platform platform = (Platform) comboBoxPlatform.getSelectedItem();
        int maxMaxHeight = Math.min(platform.maxMaxHeight, MAX_HEIGHT);
        int maxHeight;
        if ((platform.minMaxHeight == maxMaxHeight) || (maxMaxHeight == MAX_HEIGHT)) {
            maxHeight = maxMaxHeight;
            comboBoxHeight.setModel(new DefaultComboBoxModel<>(new Integer[] {maxMaxHeight}));
            comboBoxHeight.setEnabled(false);
        } else {
            maxHeight = platform.standardMaxHeight;
            List<Integer> maxHeights = new ArrayList<>();
            for (int height = platform.minMaxHeight; height <= platform.maxMaxHeight; height *= 2) {
                maxHeights.add(height);
            }
            comboBoxHeight.setModel(new DefaultComboBoxModel<>(maxHeights.toArray(new Integer[maxHeights.size()])));
            comboBoxHeight.setEnabled(true);
        }
        comboBoxHeight.setSelectedItem(maxHeight);
        maxHeightChanged();
        spinnerWorldHigh.setValue(Math.min(maxHeight - 1, (int) Math.pow(2, bitDepth) - 1));
    }

    private void maxHeightChanged() {
        int platformMaxHeight = (int) comboBoxHeight.getSelectedItem();
        setMaximum(spinnerWorldLow, platformMaxHeight - 1);
        setMaximum(spinnerWorldMiddle, platformMaxHeight - 1);
        setMaximum(spinnerWorldHigh, platformMaxHeight - 1);

        // Set levels to reasonable defaults
        selectDefaultVerticalScaling();
        updatePreview();

        labelWarning.setVisible((comboBoxPlatform.getSelectedItem() == JAVA_MCREGION) && (platformMaxHeight != DEFAULT_MAX_HEIGHT_MCREGION));
    }

    private void updatePreview() {
        doLaterOnEventThread(UPDATE_HEIGHT_MAP_PREVIEW, 250, () -> {
            if (image != null) {
                TileProvider previewProvider = createImporter().getPreviewProvider(colourScheme, contourLines, contourSeparation, lightOrigin);
                if (previewProvider != null) {
                    if (tiledImageViewer2.getTileProviderCount() == 0) {
                        tiledImageViewer2.addTileProvider(previewProvider);
                    } else {
                        tiledImageViewer2.replaceTileProvider(0, previewProvider);
                    }
                }
            }
        });
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

        buttonGroup1 = new javax.swing.ButtonGroup();
        tiledImageViewer2 = new org.pepsoft.util.swing.TiledImageViewer();
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
        comboBoxHeight = new javax.swing.JComboBox<>();
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
        jLabel14 = new javax.swing.JLabel();
        labelWalkingTime = new javax.swing.JLabel();
        jPanel3 = new javax.swing.JPanel();
        themeEditor = new org.pepsoft.worldpainter.themes.impl.simple.SimpleThemeEditor();
        buttonLoadDefaults = new javax.swing.JButton();
        buttonSaveAsDefaults = new javax.swing.JButton();
        buttonResetDefaults = new javax.swing.JButton();
        checkBoxCreateTiles = new javax.swing.JCheckBox();
        labelNoUndo = new javax.swing.JLabel();
        checkBoxOnlyRaise = new javax.swing.JCheckBox();
        jLabel13 = new javax.swing.JLabel();
        comboBoxPlatform = new javax.swing.JComboBox<>();
        tiledImageViewerContainer1 = new org.pepsoft.util.swing.TiledImageViewerContainer();

        tiledImageViewer2.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Import Height Map");

        jLabel1.setText("Select the image to import as a height map:");

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

        checkBoxInvert.setText("Invert (white is low, black is high)");
        checkBoxInvert.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                checkBoxInvertActionPerformed(evt);
            }
        });

        jTabbedPane1.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                jTabbedPane1StateChanged(evt);
            }
        });

        jLabel3.setText("Scale:");

        spinnerScale.setModel(new javax.swing.SpinnerNumberModel(100, 1, 999, 1));
        spinnerScale.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                spinnerScaleStateChanged(evt);
            }
        });

        labelWorldDimensions.setText("Scaled size: ? x ? blocks");

        comboBoxHeight.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                comboBoxHeightActionPerformed(evt);
            }
        });

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
        spinnerImageLow.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                spinnerImageLowStateChanged(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
        jPanel1.add(spinnerImageLow, gridBagConstraints);

        spinnerWorldLow.setModel(new javax.swing.SpinnerNumberModel(0, 0, 65535, 1));
        spinnerWorldLow.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                spinnerWorldLowStateChanged(evt);
            }
        });
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
        spinnerWorldMiddle.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                spinnerWorldMiddleStateChanged(evt);
            }
        });
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
        spinnerImageHigh.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                spinnerImageHighStateChanged(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
        jPanel1.add(spinnerImageHigh, gridBagConstraints);

        spinnerWorldHigh.setModel(new javax.swing.SpinnerNumberModel(255, 0, 65535, 1));
        spinnerWorldHigh.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                spinnerWorldHighStateChanged(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
        jPanel1.add(spinnerWorldHigh, gridBagConstraints);

        jLabel2.setText("Height:");

        jLabel10.setText("blocks");

        checkBoxVoid.setText("create Void below image value:");
        checkBoxVoid.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                checkBoxVoidActionPerformed(evt);
            }
        });

        spinnerVoidBelow.setModel(new javax.swing.SpinnerNumberModel(1, 1, 255, 1));
        spinnerVoidBelow.setEnabled(false);

        labelWarning.setFont(labelWarning.getFont().deriveFont(labelWarning.getFont().getStyle() | java.awt.Font.BOLD));
        labelWarning.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/pepsoft/worldpainter/icons/error.png"))); // NOI18N
        labelWarning.setText("Only with mods!");

        jLabel11.setText("Offset:");
        jLabel11.setToolTipText("The origin of the height map will be at these coordinates in the map");

        spinnerOffsetX.setModel(new javax.swing.SpinnerNumberModel(0, -999999, 999999, 1));
        spinnerOffsetX.setToolTipText("The origin of the height map will be at these coordinates in the map");
        spinnerOffsetX.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                spinnerOffsetXStateChanged(evt);
            }
        });

        jLabel12.setText(",");
        jLabel12.setToolTipText("The origin of the height map will be at these coordinates in the map");

        spinnerOffsetY.setModel(new javax.swing.SpinnerNumberModel(0, -999999, 999999, 1));
        spinnerOffsetY.setToolTipText("The origin of the height map will be at these coordinates in the map");
        spinnerOffsetY.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                spinnerOffsetYStateChanged(evt);
            }
        });

        jLabel14.setText("Edge to edge walking time:");

        labelWalkingTime.setText("...");

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
                        .addComponent(spinnerVoidBelow, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(jLabel14)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(labelWalkingTime)))
                .addContainerGap(71, Short.MAX_VALUE))
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
                    .addComponent(jLabel14)
                    .addComponent(labelWalkingTime))
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
        buttonLoadDefaults.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonLoadDefaultsActionPerformed(evt);
            }
        });

        buttonSaveAsDefaults.setText("Save As Defaults");
        buttonSaveAsDefaults.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonSaveAsDefaultsActionPerformed(evt);
            }
        });

        buttonResetDefaults.setText("Reset");
        buttonResetDefaults.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonResetDefaultsActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(themeEditor, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
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
                .addComponent(themeEditor, javax.swing.GroupLayout.DEFAULT_SIZE, 206, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(buttonLoadDefaults)
                    .addComponent(buttonSaveAsDefaults)
                    .addComponent(buttonResetDefaults))
                .addContainerGap())
        );

        jTabbedPane1.addTab("Theme", jPanel3);

        checkBoxCreateTiles.setText("Create new tiles");

        labelNoUndo.setText("<html><b>Note:</b> this cannot be undone!</html>");

        checkBoxOnlyRaise.setText("Only where higher");
        checkBoxOnlyRaise.setToolTipText("When selected, the height map will only be applied where it is higher than the existing terrain");

        jLabel13.setText("Map format:");

        comboBoxPlatform.setRenderer(new PlatformListCellRenderer());
        comboBoxPlatform.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                comboBoxPlatformActionPerformed(evt);
            }
        });

        tiledImageViewerContainer1.setMinimumSize(new java.awt.Dimension(384, 22));

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(buttonOk)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(buttonCancel))
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                .addComponent(fieldFilename)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(buttonSelectFile))
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jLabel13)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(comboBoxPlatform, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addComponent(jLabel1)
                            .addComponent(labelImageDimensions)
                            .addComponent(checkBoxInvert)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(checkBoxCreateTiles)
                                .addGap(18, 18, 18)
                                .addComponent(checkBoxOnlyRaise)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(labelNoUndo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addComponent(jTabbedPane1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(18, 18, 18)
                        .addComponent(tiledImageViewerContainer1, javax.swing.GroupLayout.DEFAULT_SIZE, 384, Short.MAX_VALUE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(fieldFilename, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(buttonSelectFile))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(labelImageDimensions)
                        .addGap(4, 4, 4)
                        .addComponent(checkBoxInvert)
                        .addGap(18, 18, 18)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel13)
                            .addComponent(comboBoxPlatform, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(18, 18, 18)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(checkBoxCreateTiles)
                            .addComponent(labelNoUndo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(checkBoxOnlyRaise))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jTabbedPane1))
                    .addComponent(tiledImageViewerContainer1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGap(18, 18, 18)
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
        updatePreview();
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
        updatePreview();
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
        updatePreview();
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
        updatePreview();
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
        updatePreview();
    }//GEN-LAST:event_spinnerWorldHighStateChanged

    private void buttonSelectFileActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonSelectFileActionPerformed
        File myHeightMapDir = heightMapDir;
        if (myHeightMapDir == null) {
            myHeightMapDir = Configuration.getInstance().getHeightMapsDirectory();
        }
        if (myHeightMapDir == null) {
            myHeightMapDir = Configuration.getInstance().getMasksDirectory();
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
        File file = FileUtils.selectFileForOpen(this, "Select a height map image file", myHeightMapDir, new FileFilter() {
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
            heightMapDir = file.getParentFile();
            fieldFilename.setText(file.getAbsolutePath());
        }
    }//GEN-LAST:event_buttonSelectFileActionPerformed

    private void spinnerScaleStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_spinnerScaleStateChanged
        if (image != null) {
            updateWorldDimensions();
            updatePreview();
        }
    }//GEN-LAST:event_spinnerScaleStateChanged

    private void buttonOkActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonOkActionPerformed
        if (jTabbedPane1.getSelectedIndex() == 0) {
            themeEditor.setWaterHeight((int) spinnerWorldMiddle.getValue());
            if (themeEditor.save()) {
                ok();
            } else {
                jTabbedPane1.setSelectedIndex(1);
            }
        } else if (themeEditor.save()) {
            spinnerWorldMiddle.setValue(themeEditor.getTheme().getWaterHeight());
            ok();
        }
    }//GEN-LAST:event_buttonOkActionPerformed

    private void comboBoxHeightActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_comboBoxHeightActionPerformed
        maxHeightChanged();
    }//GEN-LAST:event_comboBoxHeightActionPerformed

    private void jTabbedPane1StateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_jTabbedPane1StateChanged
        if (! initialising) {
            switch (jTabbedPane1.getSelectedIndex()) {
                case 0:
                    themeEditor.save();
                    spinnerWorldMiddle.setValue(themeEditor.getTheme().getWaterHeight());
                    break;
                case 1:
                    themeEditor.setWaterHeight((int) spinnerWorldMiddle.getValue());
                    break;
            }
        }
    }//GEN-LAST:event_jTabbedPane1StateChanged

    private void checkBoxVoidActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_checkBoxVoidActionPerformed
        spinnerVoidBelow.setEnabled(checkBoxVoid.isSelected());
    }//GEN-LAST:event_checkBoxVoidActionPerformed

    private void spinnerOffsetXStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_spinnerOffsetXStateChanged
        if (image != null) {
            updateWorldDimensions();
            updatePreview();
        }
    }//GEN-LAST:event_spinnerOffsetXStateChanged

    private void spinnerOffsetYStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_spinnerOffsetYStateChanged
        if (image != null) {
            updateWorldDimensions();
            updatePreview();
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

    private void comboBoxPlatformActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_comboBoxPlatformActionPerformed
        platformChanged();
    }//GEN-LAST:event_comboBoxPlatformActionPerformed

    private void checkBoxInvertActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_checkBoxInvertActionPerformed
        if (image != null) {
            loadImage();
        }
    }//GEN-LAST:event_checkBoxInvertActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton buttonCancel;
    private javax.swing.ButtonGroup buttonGroup1;
    private javax.swing.JButton buttonLoadDefaults;
    private javax.swing.JButton buttonOk;
    private javax.swing.JButton buttonResetDefaults;
    private javax.swing.JButton buttonSaveAsDefaults;
    private javax.swing.JButton buttonSelectFile;
    private javax.swing.JCheckBox checkBoxCreateTiles;
    private javax.swing.JCheckBox checkBoxInvert;
    private javax.swing.JCheckBox checkBoxOnlyRaise;
    private javax.swing.JCheckBox checkBoxVoid;
    private javax.swing.JComboBox<Integer> comboBoxHeight;
    private javax.swing.JComboBox<Platform> comboBoxPlatform;
    private javax.swing.JTextField fieldFilename;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel13;
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
    private javax.swing.JLabel labelNoUndo;
    private javax.swing.JLabel labelWalkingTime;
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
    private org.pepsoft.worldpainter.themes.impl.simple.SimpleThemeEditor themeEditor;
    private org.pepsoft.util.swing.TiledImageViewer tiledImageViewer2;
    private org.pepsoft.util.swing.TiledImageViewerContainer tiledImageViewerContainer1;
    // End of variables declaration//GEN-END:variables

    private final long seed = new Random().nextLong();
    private final Dimension currentDimension;
    private final ColourScheme colourScheme;
    private final boolean contourLines;
    private final int contourSeparation;
    private final TileRenderer.LightOrigin lightOrigin;
    private File selectedFile, heightMapDir;
    private volatile BufferedImage image;
    private int bitDepth = 8, imageHighValue = 255;
    private boolean initialising = true;

    private static final String UPDATE_HEIGHT_MAP_PREVIEW = ImportHeightMapDialog.class.getName() + ".updateHeightMap";
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ImportHeightMapDialog.class);
    private static final long serialVersionUID = 1L;
}