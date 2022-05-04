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
import org.pepsoft.worldpainter.heightMaps.gui.ImportPresetListCellRenderer;
import org.pepsoft.worldpainter.importing.HeightMapImporter;
import org.pepsoft.worldpainter.layers.Void;
import org.pepsoft.worldpainter.plugins.PlatformManager;
import org.pepsoft.worldpainter.themes.SimpleTheme;
import org.pepsoft.worldpainter.themes.TerrainListCellRenderer;
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
import java.text.NumberFormat;
import java.util.List;
import java.util.*;

import static com.google.common.primitives.Ints.asList;
import static java.awt.image.DataBuffer.TYPE_DOUBLE;
import static java.awt.image.DataBuffer.TYPE_FLOAT;
import static org.pepsoft.minecraft.Constants.DEFAULT_MAX_HEIGHT_MCREGION;
import static org.pepsoft.minecraft.Constants.DEFAULT_WATER_LEVEL;
import static org.pepsoft.util.AwtUtils.doLaterOnEventThread;
import static org.pepsoft.util.swing.ProgressDialog.NOT_CANCELABLE;
import static org.pepsoft.util.swing.SpinnerUtils.setMaximum;
import static org.pepsoft.util.swing.SpinnerUtils.setMinimum;
import static org.pepsoft.worldpainter.Constants.MAX_HEIGHT;
import static org.pepsoft.worldpainter.DefaultPlugin.JAVA_MCREGION;
import static org.pepsoft.worldpainter.HeightTransform.IDENTITY;
import static org.pepsoft.worldpainter.Terrain.GRASS;
import static org.pepsoft.worldpainter.Terrain.PICK_LIST;
import static org.pepsoft.worldpainter.util.LayoutUtils.setDefaultSizeAndLocation;
import static org.pepsoft.worldpainter.util.MinecraftUtil.blocksToWalkingTime;

/**
 *
 * @author pepijn
 */
@SuppressWarnings({"ConstantConditions", // Guaranteed by code
        "FieldCanBeLocal", "unused", "Convert2Lambda", "Anonymous2MethodRef"}) // Managed by NetBeans
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
        labelWarningCutOffBelow.setVisible(false);
        labelWarningCutOffAbove.setVisible(false);
        comboBoxSingleTerrain.setRenderer(new TerrainListCellRenderer(colourScheme));
        comboBoxSingleTerrain.setSelectedItem(GRASS);
        comboBoxPreset.setRenderer(new ImportPresetListCellRenderer());

        fieldFilename.getDocument().addDocumentListener(this);
        
        rootPane.setDefaultButton(buttonOk);
        
        if (currentDimension != null) {
            platform = currentDimension.getWorld().getPlatform();
            jTabbedPane1.setEnabledAt(1, false);
            comboBoxPlatform.setSelectedItem(platform);
            comboBoxPlatform.setEnabled(false);
            buttonResetDefaults.setEnabled(false);
            spinnerWorldMiddle.setValue(((HeightMapTileFactory) currentDimension.getTileFactory()).getTheme().clone().getWaterHeight());
            buttonLoadDefaults.setEnabled(true);
            buttonSaveAsDefaults.setEnabled(true);
            checkBoxOnlyRaise.setSelected(true);
            comboBoxSingleTerrain.setModel(new DefaultComboBoxModel<>(Terrain.getConfiguredValues()));
        } else {
            platform = Configuration.getInstance().getDefaultPlatform();
            themeEditor.setTheme(SimpleTheme.createDefault(GRASS, platform.minZ, platform.standardMaxHeight, DEFAULT_WATER_LEVEL, true, true));
            themeEditor.setChangeListener(this);
            comboBoxPlatform.setSelectedItem(platform);
            labelNoUndo.setText(" ");
            checkBoxCreateTiles.setEnabled(false);
            checkBoxOnlyRaise.setEnabled(false);
            comboBoxSingleTerrain.setModel(new DefaultComboBoxModel<>(PICK_LIST));
            loadDefaults();
        }

        scaleToUI();
        pack();
        setDefaultSizeAndLocation(this, 60);

        programmaticChange = false;
        platformChanged();
        updateImageLevelLabels();
        setControlStates();
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
        HeightMap heightMap = BitmapHeightMap.build().withName(selectedFile.getName()).withImage(image).withFile(selectedFile).now();
        final int scale = (Integer) spinnerScale.getValue();
        final int offsetX = (Integer) spinnerOffsetX.getValue();
        final int offsetY = (Integer) spinnerOffsetY.getValue();
        if ((scale != 100) || (offsetX != 0) || (offsetY != 0)) {
            if (scale != 100) {
                ((BitmapHeightMap) heightMap).setSmoothScaling(true);
            }
            heightMap = new TransformingHeightMap(heightMap.getName() + " transformed", heightMap, scale, scale, offsetX, offsetY, 0);
        }
        if (checkBoxInvert.isSelected()) {
            heightMap = new DifferenceHeightMap(new ConstantHeightMap((float) (Math.pow(2, bitDepth) - 1)), heightMap);
        }

        String name = selectedFile.getName();
        final int p = name.lastIndexOf('.');
        if (p != -1) {
            name = name.substring(0, p);
        }

        final int waterLevel = (int) spinnerWorldMiddle.getValue();
        final int maxHeight = (int) comboBoxHeight.getSelectedItem();

        final HeightMapImporter importer = new HeightMapImporter();
        final Platform platform = (Platform) comboBoxPlatform.getSelectedItem();
        importer.setPlatform(platform);
        importer.setHeightMap(heightMap);
        importer.setImageFile(selectedFile);
        importer.setName(name);
        if (currentDimension != null) {
            importer.setTileFactory(currentDimension.getTileFactory());
            if (radioButtonSingleTerrain.isSelected()) {
                final SimpleTheme theme = SimpleTheme.createSingleTerrain((Terrain) comboBoxSingleTerrain.getSelectedItem(), platform.minZ, maxHeight, waterLevel);
                theme.setSeed(seed);
                importer.setTheme(theme);
            }
        } else {
            themeEditor.save();
            final SimpleTheme theme;
            if (radioButtonSingleTerrain.isSelected()) {
                theme = SimpleTheme.createSingleTerrain((Terrain) comboBoxSingleTerrain.getSelectedItem(), platform.minZ, maxHeight, waterLevel);
            } else {
                theme = themeEditor.getTheme();
                theme.setMinMaxHeight(platform.minZ, maxHeight, IDENTITY); // TODO add support for adjusting minHeight
            }
            theme.setSeed(seed);
            importer.setTileFactory(new HeightMapTileFactory(seed, new SumHeightMap(new ConstantHeightMap(waterLevel - 4), new NoiseHeightMap((float) 20, 1.0, 1, 0)), platform.minZ, maxHeight, false, theme));
        }
        importer.setMaxHeight(maxHeight);
        importer.setImageLowLevel((Long) spinnerImageLow.getValue());
        importer.setImageHighLevel((Long) spinnerImageHigh.getValue());
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
        comboBoxPreset.setEnabled(fileSelected);
        buttonOk.setEnabled(fileSelected);
        spinnerImageLow.setEnabled(fileSelected);
        spinnerImageHigh.setEnabled(fileSelected);
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
            } else if ((image.getSampleModel().getTransferType() == TYPE_FLOAT) || (image.getSampleModel().getTransferType() == TYPE_DOUBLE)) {
                labelImageDimensions.setForeground(Color.RED);
                labelImageDimensions.setText("Floating point height maps not yet supported! Please convert to 32-bit integer.");
                selectedFile = null;
            } else {
                final int width = image.getWidth(), height = image.getHeight();
                programmaticChange = true;
                try {
                    if (image.getColorModel().hasAlpha()) {
                        spinnerScale.setValue(100);
                        spinnerScale.setEnabled(false);
                        spinnerScale.setToolTipText("<html>Scaling not supported for grey scale images with an alpha channel!<br>To enable scaling, please remove the alpha channel.</html>");
                    } else {
                        spinnerScale.setEnabled(true);
                        spinnerScale.setToolTipText(null);
                    }
                    labelImageDimensions.setForeground(null);
                    bitDepth = image.getSampleModel().getSampleSize(0);
                    final WritableRaster raster = image.getRaster();
                    imageLowValue = Long.MAX_VALUE;
                    imageHighValue = Long.MIN_VALUE;
                    final long imageMaxHeight = (long) Math.pow(2, bitDepth) - 1;
                    final boolean invert = checkBoxInvert.isSelected();
                    outer:
                    for (int x = 0; x < width; x++) {
                        for (int y = 0; y < height; y++) {
                            final long value = invert ? (imageMaxHeight - raster.getSample(x, y, 0) & 0xffffffffL) : raster.getSample(x, y, 0) & 0xffffffffL; // Convert to unsigned integers
                            if (value < imageLowValue) {
                                imageLowValue = value;
                            }
                            if (value > imageHighValue) {
                                imageHighValue = value;
                            }
                            if ((imageLowValue == 0L) && (imageHighValue == imageMaxHeight)) {
                                // No point in looking any further!
                                break outer;
                            }
                        }
                    }
                    setMaximum(spinnerImageLow, imageMaxHeight);
                    setMaximum(spinnerImageHigh, imageMaxHeight);
                } finally {
                    programmaticChange = false;
                }
                labelImageLowestLevel.setText(NUMBER_FORMAT.format(imageLowValue));
                labelImageHighestLevel.setText(NUMBER_FORMAT.format(imageHighValue));

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
        if (image == null) {
            return;
        }
        final ImportPreset currentPreset = (ImportPreset) comboBoxPreset.getSelectedItem();
        final Vector<ImportPreset> presets = new Vector<>(ImportPreset.PRESETS.length + 1);
        presets.add(null);
        for (ImportPreset preset: ImportPreset.PRESETS) {
            if (preset.isValid(bitDepth, imageLowValue, imageHighValue, platform, (Integer) comboBoxHeight.getSelectedItem())) {
                presets.add(preset);
            }
        }
        final DefaultComboBoxModel<ImportPreset> presetsModel = new DefaultComboBoxModel<>(presets);
        comboBoxPreset.setModel(presetsModel);
        comboBoxPreset.setEnabled(presets.size() > 1);
        if ((currentPreset != null) && presets.contains(currentPreset)) {
            comboBoxPreset.setSelectedItem(currentPreset);
        } else {
            comboBoxPreset.setSelectedItem(presets.lastElement());
        }
        applyPreset((ImportPreset) comboBoxPreset.getSelectedItem());
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
    
    private void updateImageLevelLabels() {
        if (programmaticChange) {
            return;
        }
        final int maxHeight        = (Integer) comboBoxHeight.getSelectedItem() - 1;
        final long imageLowLevel   = (Long) spinnerImageLow.getValue();
        final long imageHighLevel  = (Long) spinnerImageHigh.getValue();
        final int worldLowLevel    = (Integer) spinnerWorldLow.getValue();
        final int worldMiddleLevel = (Integer) spinnerWorldMiddle.getValue();
        final int worldHighLevel   = (Integer) spinnerWorldHigh.getValue();
        final float levelScale = (float) (worldHighLevel - worldLowLevel) / (imageHighLevel - imageLowLevel);
        final long imageMiddleLevel = (long) ((worldMiddleLevel - worldLowLevel) / levelScale + imageLowLevel);
        final int worldLowestLevel = (int) ((imageLowValue - imageLowLevel) * levelScale + worldLowLevel);
        final int worldHighestLevel = (int) ((imageHighValue - imageLowLevel) * levelScale + worldLowLevel);
        if (imageMiddleLevel < imageLowValue) {
            labelImageWaterLevel.setText("-");
        } else if (imageMiddleLevel > imageHighValue) {
            labelImageWaterLevel.setText("> " + imageHighValue);
        } else {
            labelImageWaterLevel.setText(NUMBER_FORMAT.format(imageMiddleLevel));
        }
        if (worldLowestLevel < platform.minZ) {
            labelWorldLowestLevel.setText("<html><b>&lt; " + platform.minZ + "</b></html>");
            labelWarningCutOffBelow.setVisible(true);
        } else if (worldLowestLevel > maxHeight) {
            labelWorldLowestLevel.setText("<html><b>&gt; " + NUMBER_FORMAT.format(maxHeight) + "</b></html>");
            labelWarningCutOffBelow.setVisible(false);
        } else {
            labelWorldLowestLevel.setText(NUMBER_FORMAT.format(worldLowestLevel));
            labelWarningCutOffBelow.setVisible(false);
        }
        if (worldHighestLevel < platform.minZ) {
            labelWorldHighestLevel.setText("<html><b>&lt; " + platform.minZ + "</b></html>");
            labelWarningCutOffAbove.setVisible(false);
        } else if (worldHighestLevel > maxHeight) {
            labelWorldHighestLevel.setText("<html><b>&gt; " + NUMBER_FORMAT.format(maxHeight) + "</b></html>");
            labelWarningCutOffAbove.setVisible(true);
        } else {
            labelWorldHighestLevel.setText(NUMBER_FORMAT.format(worldHighestLevel));
            labelWarningCutOffAbove.setVisible(false);
        }
    }

    private void loadDefaults() {
        loadDefaultTheme();
        updateImageLevelLabels();
        updatePreview();
    }

    private void loadDefaultTheme() {
        final int maxHeight = comboBoxHeight.getSelectedItem() != null ? (Integer) comboBoxHeight.getSelectedItem() : platform.standardMaxHeight;
        Theme defaultTheme = Configuration.getInstance().getHeightMapDefaultTheme();
        if (defaultTheme == null) {
            defaultTheme = SimpleTheme.createDefault(GRASS, platform.minZ, maxHeight, DEFAULT_WATER_LEVEL, true, true);
            if (currentDimension == null) {
                buttonResetDefaults.setEnabled(false);
            }
        } else {
            buttonResetDefaults.setEnabled(true);
        }
        defaultTheme.setMinMaxHeight(platform.minZ, maxHeight, IDENTITY);
        spinnerWorldMiddle.setValue(defaultTheme.getWaterHeight());
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
            updateImageLevelLabels();
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
        if (programmaticChange) {
            return;
        }
        platform = (Platform) comboBoxPlatform.getSelectedItem();
        final int maxHeight;
        programmaticChange = true;
        try {
            if (currentDimension != null) {
                maxHeight = currentDimension.getMaxHeight();
                comboBoxHeight.setModel(new DefaultComboBoxModel<>(new Integer[] { maxHeight }));
                comboBoxHeight.setSelectedItem(maxHeight);
                comboBoxHeight.setEnabled(false);
            } else {
                maxHeight = platform.standardMaxHeight;
                final List<Integer> maxHeights = new ArrayList<>(asList(platform.maxHeights));
                maxHeights.removeIf(height -> height > MAX_HEIGHT);
                if (maxHeights.isEmpty()) {
                    maxHeights.add(MAX_HEIGHT);
                }
                comboBoxHeight.setModel(new DefaultComboBoxModel<>(maxHeights.toArray(new Integer[maxHeights.size()])));
                comboBoxHeight.setSelectedItem(Math.min(platform.standardMaxHeight, MAX_HEIGHT));
                comboBoxHeight.setEnabled(maxHeights.size() > 1);
            }
            setMinimum(spinnerWorldLow, platform.minZ);
            setMinimum(spinnerWorldMiddle, platform.minZ);
            setMinimum(spinnerWorldHigh, platform.minZ);
            labelMinHeight.setText(String.valueOf(platform.minZ));
            maxHeightChanged();
            spinnerWorldHigh.setValue((int) Math.min(maxHeight - 1, (long) Math.pow(2, bitDepth) - 1)); // TODO overflow
            loadDefaultTheme();
        } finally {
            programmaticChange = false;
        }
    }

    private void maxHeightChanged() {
        int platformMaxHeight = (int) comboBoxHeight.getSelectedItem();
        programmaticChange = true;
        try {
            setMaximum(spinnerWorldLow, platformMaxHeight - 1);
            setMaximum(spinnerWorldMiddle, platformMaxHeight - 1);
            setMaximum(spinnerWorldHigh, platformMaxHeight - 1);
            labelMaxHeight.setText(NUMBER_FORMAT.format(platformMaxHeight - 1));
        } finally {
            programmaticChange = false;
        }

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

    private void applyPreset(ImportPreset preset) {
        if (preset != null) {
            programmaticChange = true;
            try {
                final long[][] mappings = preset.getMapping(bitDepth, imageLowValue, imageHighValue, platform, (Integer) comboBoxHeight.getSelectedItem());
                spinnerImageLow.setValue(mappings[0][0]);
                spinnerWorldLow.setValue((int) mappings[1][0]);
                spinnerImageHigh.setValue(mappings[0][1]);
                spinnerWorldHigh.setValue((int) mappings[1][1]);
            } finally {
                programmaticChange = false;
            }
        }
        updateImageLevelLabels();
    }

    private void resetPreset() {
        if (programmaticChange) {
            return;
        }
        programmaticChange = true;
        try {
            comboBoxPreset.setSelectedItem(null);
        } finally {
            programmaticChange = false;
        }
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        tiledImageViewer2 = new org.pepsoft.util.swing.TiledImageViewer();
        themeButtonGroup = new javax.swing.ButtonGroup();
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
        jLabel15 = new javax.swing.JLabel();
        labelImageLowestLevel = new javax.swing.JLabel();
        labelWorldLowestLevel = new javax.swing.JLabel();
        jLabel18 = new javax.swing.JLabel();
        labelImageHighestLevel = new javax.swing.JLabel();
        labelWorldHighestLevel = new javax.swing.JLabel();
        labelWarningCutOffAbove = new javax.swing.JLabel();
        labelWarningCutOffBelow = new javax.swing.JLabel();
        jLabel16 = new javax.swing.JLabel();
        jLabel17 = new javax.swing.JLabel();
        labelMinHeight = new javax.swing.JLabel();
        labelMaxHeight = new javax.swing.JLabel();
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
        radioButtonApplyTheme = new javax.swing.JRadioButton();
        radioButtonSingleTerrain = new javax.swing.JRadioButton();
        comboBoxSingleTerrain = new javax.swing.JComboBox<>();
        jLabel19 = new javax.swing.JLabel();
        comboBoxPreset = new javax.swing.JComboBox<>();
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

        jLabel5.setText("From image:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
        jPanel1.add(jLabel5, gridBagConstraints);

        jLabel6.setText("To Minecraft:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
        jPanel1.add(jLabel6, gridBagConstraints);

        jLabel7.setText("Low mapping:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
        jPanel1.add(jLabel7, gridBagConstraints);

        spinnerImageLow.setModel(new javax.swing.SpinnerNumberModel(Long.valueOf(0L), Long.valueOf(0L), Long.valueOf(4294967295L), Long.valueOf(1L)));
        spinnerImageLow.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                spinnerImageLowStateChanged(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
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
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.RELATIVE;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
        jPanel1.add(spinnerWorldLow, gridBagConstraints);

        jLabel8.setText("Water level:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
        jPanel1.add(jLabel8, gridBagConstraints);

        labelImageWaterLevel.setText("62");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
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
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
        jPanel1.add(spinnerWorldMiddle, gridBagConstraints);

        jLabel9.setText("High mapping:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
        jPanel1.add(jLabel9, gridBagConstraints);

        spinnerImageHigh.setModel(new javax.swing.SpinnerNumberModel(Long.valueOf(255L), Long.valueOf(0L), Long.valueOf(4294967295L), Long.valueOf(1L)));
        spinnerImageHigh.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                spinnerImageHighStateChanged(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 6;
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
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
        jPanel1.add(spinnerWorldHigh, gridBagConstraints);

        jLabel15.setText("Lowest value:");
        jLabel15.setToolTipText("The lowest value that actually occurs on the height map.");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
        jPanel1.add(jLabel15, gridBagConstraints);

        labelImageLowestLevel.setText("0");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_END;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 19);
        jPanel1.add(labelImageLowestLevel, gridBagConstraints);

        labelWorldLowestLevel.setText("0");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.RELATIVE;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_END;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 19);
        jPanel1.add(labelWorldLowestLevel, gridBagConstraints);

        jLabel18.setText("Highest value:");
        jLabel18.setToolTipText("The highest value that actually occurs on the height map.");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
        jPanel1.add(jLabel18, gridBagConstraints);

        labelImageHighestLevel.setText("255");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_END;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 19);
        jPanel1.add(labelImageHighestLevel, gridBagConstraints);

        labelWorldHighestLevel.setText("255");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_END;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 19);
        jPanel1.add(labelWorldHighestLevel, gridBagConstraints);

        labelWarningCutOffAbove.setFont(labelWarningCutOffAbove.getFont().deriveFont(labelWarningCutOffAbove.getFont().getStyle() | java.awt.Font.BOLD));
        labelWarningCutOffAbove.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/pepsoft/worldpainter/icons/error.png"))); // NOI18N
        labelWarningCutOffAbove.setText("Cut off above!");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
        jPanel1.add(labelWarningCutOffAbove, gridBagConstraints);

        labelWarningCutOffBelow.setFont(labelWarningCutOffBelow.getFont().deriveFont(labelWarningCutOffBelow.getFont().getStyle() | java.awt.Font.BOLD));
        labelWarningCutOffBelow.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/pepsoft/worldpainter/icons/error.png"))); // NOI18N
        labelWarningCutOffBelow.setText("Cut off below!");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
        jPanel1.add(labelWarningCutOffBelow, gridBagConstraints);

        jLabel16.setText("Build limit:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
        jPanel1.add(jLabel16, gridBagConstraints);

        jLabel17.setText("Build limit:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
        jPanel1.add(jLabel17, gridBagConstraints);

        labelMinHeight.setText("-64");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_END;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 19);
        jPanel1.add(labelMinHeight, gridBagConstraints);

        labelMaxHeight.setText("319");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_END;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 19);
        jPanel1.add(labelMaxHeight, gridBagConstraints);

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

        themeButtonGroup.add(radioButtonApplyTheme);
        radioButtonApplyTheme.setSelected(true);
        radioButtonApplyTheme.setText("apply theme");
        radioButtonApplyTheme.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                radioButtonApplyThemeActionPerformed(evt);
            }
        });

        themeButtonGroup.add(radioButtonSingleTerrain);
        radioButtonSingleTerrain.setText("single terrain:");
        radioButtonSingleTerrain.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                radioButtonSingleTerrainActionPerformed(evt);
            }
        });

        comboBoxSingleTerrain.setEnabled(false);
        comboBoxSingleTerrain.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                comboBoxSingleTerrainActionPerformed(evt);
            }
        });

        jLabel19.setText("Preset:");

        comboBoxPreset.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                comboBoxPresetActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(radioButtonApplyTheme)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(radioButtonSingleTerrain)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(comboBoxSingleTerrain, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
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
                        .addComponent(labelWalkingTime))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(jLabel19)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(comboBoxPreset, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
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
                    .addComponent(jLabel19)
                    .addComponent(comboBoxPreset, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(checkBoxVoid)
                    .addComponent(spinnerVoidBelow, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(radioButtonApplyTheme)
                    .addComponent(radioButtonSingleTerrain)
                    .addComponent(comboBoxSingleTerrain, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
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
                .addComponent(themeEditor, javax.swing.GroupLayout.DEFAULT_SIZE, 390, Short.MAX_VALUE)
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
                        .addComponent(jTabbedPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 478, Short.MAX_VALUE))
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
        long lowLevel  = (Long) spinnerImageLow.getValue();
        long highLevel = (Long) spinnerImageHigh.getValue();
        if (lowLevel > highLevel) {
            spinnerImageHigh.setValue(lowLevel);
        }
        resetPreset();
        updateImageLevelLabels();
        updatePreview();
        setControlStates();
    }//GEN-LAST:event_spinnerImageLowStateChanged

    private void buttonCancelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonCancelActionPerformed
        cancel();
    }//GEN-LAST:event_buttonCancelActionPerformed

    private void spinnerImageHighStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_spinnerImageHighStateChanged
        long lowLevel  = (Long) spinnerImageLow.getValue();
        long highLevel = (Long) spinnerImageHigh.getValue();
        if (highLevel < lowLevel) {
            spinnerImageLow.setValue(highLevel);
        }
        resetPreset();
        updateImageLevelLabels();
        updatePreview();
        setControlStates();
    }//GEN-LAST:event_spinnerImageHighStateChanged

    private void spinnerWorldLowStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_spinnerWorldLowStateChanged
        int lowLevel    = (Integer) spinnerWorldLow.getValue();
        int highLevel   = (Integer) spinnerWorldHigh.getValue();
        if (lowLevel > highLevel) {
            spinnerWorldHigh.setValue(lowLevel);
        }
        resetPreset();
        updateImageLevelLabels();
        updatePreview();
        setControlStates();
    }//GEN-LAST:event_spinnerWorldLowStateChanged

    private void spinnerWorldMiddleStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_spinnerWorldMiddleStateChanged
        updateImageLevelLabels();
        updatePreview();
    }//GEN-LAST:event_spinnerWorldMiddleStateChanged

    private void spinnerWorldHighStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_spinnerWorldHighStateChanged
        int lowLevel    = (Integer) spinnerWorldLow.getValue();
        int highLevel   = (Integer) spinnerWorldHigh.getValue();
        if (highLevel < lowLevel) {
            spinnerWorldLow.setValue(highLevel);
        }
        resetPreset();
        updateImageLevelLabels();
        updatePreview();
        setControlStates();
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
        if (!programmaticChange) {
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

    private void radioButtonApplyThemeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_radioButtonApplyThemeActionPerformed
        comboBoxSingleTerrain.setEnabled(false);
        jTabbedPane1.setEnabledAt(1, currentDimension == null);
        updatePreview();
    }//GEN-LAST:event_radioButtonApplyThemeActionPerformed

    private void radioButtonSingleTerrainActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_radioButtonSingleTerrainActionPerformed
        comboBoxSingleTerrain.setEnabled(true);
        jTabbedPane1.setEnabledAt(1, false);
        updatePreview();
    }//GEN-LAST:event_radioButtonSingleTerrainActionPerformed

    private void comboBoxSingleTerrainActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_comboBoxSingleTerrainActionPerformed
        updatePreview();
    }//GEN-LAST:event_comboBoxSingleTerrainActionPerformed

    private void comboBoxPresetActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_comboBoxPresetActionPerformed
        applyPreset((ImportPreset) comboBoxPreset.getSelectedItem());
    }//GEN-LAST:event_comboBoxPresetActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton buttonCancel;
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
    private javax.swing.JComboBox<ImportPreset> comboBoxPreset;
    private javax.swing.JComboBox<Terrain> comboBoxSingleTerrain;
    private javax.swing.JTextField fieldFilename;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel14;
    private javax.swing.JLabel jLabel15;
    private javax.swing.JLabel jLabel16;
    private javax.swing.JLabel jLabel17;
    private javax.swing.JLabel jLabel18;
    private javax.swing.JLabel jLabel19;
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
    private javax.swing.JLabel labelImageHighestLevel;
    private javax.swing.JLabel labelImageLowestLevel;
    private javax.swing.JLabel labelImageWaterLevel;
    private javax.swing.JLabel labelMaxHeight;
    private javax.swing.JLabel labelMinHeight;
    private javax.swing.JLabel labelNoUndo;
    private javax.swing.JLabel labelWalkingTime;
    private javax.swing.JLabel labelWarning;
    private javax.swing.JLabel labelWarningCutOffAbove;
    private javax.swing.JLabel labelWarningCutOffBelow;
    private javax.swing.JLabel labelWorldDimensions;
    private javax.swing.JLabel labelWorldHighestLevel;
    private javax.swing.JLabel labelWorldLowestLevel;
    private javax.swing.JRadioButton radioButtonApplyTheme;
    private javax.swing.JRadioButton radioButtonSingleTerrain;
    private javax.swing.JSpinner spinnerImageHigh;
    private javax.swing.JSpinner spinnerImageLow;
    private javax.swing.JSpinner spinnerOffsetX;
    private javax.swing.JSpinner spinnerOffsetY;
    private javax.swing.JSpinner spinnerScale;
    private javax.swing.JSpinner spinnerVoidBelow;
    private javax.swing.JSpinner spinnerWorldHigh;
    private javax.swing.JSpinner spinnerWorldLow;
    private javax.swing.JSpinner spinnerWorldMiddle;
    private javax.swing.ButtonGroup themeButtonGroup;
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
    private int bitDepth = 8;
    private long imageLowValue, imageHighValue = 255L;
    private boolean programmaticChange = true;
    private Platform platform;

    private static final String UPDATE_HEIGHT_MAP_PREVIEW = ImportHeightMapDialog.class.getName() + ".updateHeightMap";
    private static final NumberFormat NUMBER_FORMAT = NumberFormat.getIntegerInstance();
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ImportHeightMapDialog.class);
    private static final long serialVersionUID = 1L;
}