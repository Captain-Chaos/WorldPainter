/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/*
 * PreferencesDialog.java
 *
 * Created on Apr 26, 2012, 3:18:17 PM
 */
package org.pepsoft.worldpainter;

import org.pepsoft.minecraft.*;
import org.pepsoft.util.GUIUtils;
import org.pepsoft.worldpainter.TileRenderer.LightOrigin;
import org.pepsoft.worldpainter.exporting.ExportSettings;
import org.pepsoft.worldpainter.exporting.ExportSettingsEditor;
import org.pepsoft.worldpainter.plugins.PlatformManager;
import org.pepsoft.worldpainter.plugins.PlatformProvider;
import org.pepsoft.worldpainter.themes.SimpleTheme;
import org.pepsoft.worldpainter.themes.TerrainListCellRenderer;
import org.pepsoft.worldpainter.util.BackupUtils;
import org.pepsoft.worldpainter.util.EnumListCellRenderer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.SortedMap;

import static java.awt.BorderLayout.CENTER;
import static java.awt.BorderLayout.SOUTH;
import static java.awt.Color.BLUE;
import static java.awt.Color.GRAY;
import static java.awt.Cursor.HAND_CURSOR;
import static java.awt.FlowLayout.RIGHT;
import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;
import static org.pepsoft.minecraft.Constants.DEFAULT_WATER_LEVEL;
import static org.pepsoft.worldpainter.Constants.DIM_NORMAL;
import static org.pepsoft.worldpainter.DefaultPlugin.*;
import static org.pepsoft.worldpainter.Generator.*;
import static org.pepsoft.worldpainter.HeightTransform.IDENTITY;
import static org.pepsoft.worldpainter.Platform.Capability.BLOCK_BASED;
import static org.pepsoft.worldpainter.Platform.Capability.NAME_BASED;
import static org.pepsoft.worldpainter.Terrain.GRASS;
import static org.pepsoft.worldpainter.World2.DEFAULT_OCEAN_SEED;

/**
 *
 * @author pepijn
 */
@SuppressWarnings({"unchecked", "rawtypes", "Convert2Lambda", "Anonymous2MethodRef", "unused", "FieldCanBeLocal"}) // Managed by NetBeans
public class PreferencesDialog extends WorldPainterDialog {
    /** Creates new form PreferencesDialog */
    public PreferencesDialog(java.awt.Frame parent, ColourScheme colourScheme) {
        super(parent);
        this.colourScheme = colourScheme;
        
        initComponents();
        
        comboBoxSurfaceMaterial.setModel(new DefaultComboBoxModel(Terrain.PICK_LIST));
        comboBoxSurfaceMaterial.setRenderer(new TerrainListCellRenderer(colourScheme));
        comboBoxMode.setRenderer(new EnumListCellRenderer());
        comboBoxWorldType.setRenderer(new EnumListCellRenderer());

        List<AccelerationType> accelTypes = AccelerationType.getForThisOS();
        radioButtonAccelDefault.setEnabled(accelTypes.contains(AccelerationType.DEFAULT));
        radioButtonAccelDirect3D.setEnabled(accelTypes.contains(AccelerationType.DIRECT3D));
        radioButtonAccelOpenGL.setEnabled(accelTypes.contains(AccelerationType.OPENGL));
        radioButtonAccelQuartz.setEnabled(accelTypes.contains(AccelerationType.QUARTZ));
        radioButtonAccelUnaccelerated.setEnabled(accelTypes.contains(AccelerationType.UNACCELERATED));
        radioButtonAccelXRender.setEnabled(accelTypes.contains(AccelerationType.XRENDER));

        comboBoxPlatform.setModel(new DefaultComboBoxModel<>(PlatformManager.getInstance().getAllPlatforms().toArray(new Platform[0])));
        comboBoxPlatform.setRenderer(new PlatformListCellRenderer());

        loadSettings();
        
        rootPane.setDefaultButton(buttonOK);
        scaleToUI();
        pack();
        setLocationRelativeTo(parent);
    }
    
    public void ok() {
        saveSettings();
        super.ok();
    }
    
    private void loadSettings() {
        Configuration config = Configuration.getInstance();
        if (Main.privateContext == null) {
            checkBoxPing.setSelected(false);
            checkBoxPing.setEnabled(false);
            pingNotSet = true;
        } else if (config.getPingAllowed() != null) {
            checkBoxPing.setSelected(config.getPingAllowed());
        } else {
            checkBoxPing.setSelected(false);
            pingNotSet = true;
        }
        if ((Main.privateContext == null)
                || "true".equals(System.getProperty("org.pepsoft.worldpainter.devMode"))
                || "true".equals(System.getProperty("org.pepsoft.worldpainter.disableUpdateCheck"))) {
            checkBoxCheckForUpdates.setSelected(false);
            checkBoxCheckForUpdates.setEnabled(false);
        } else {
            checkBoxCheckForUpdates.setSelected(config.isCheckForUpdates());
        }
        if ("true".equals(System.getProperty("org.pepsoft.worldpainter.disableUndo"))) {
            checkBoxUndo.setSelected(false);
            checkBoxUndo.setEnabled(false);
            spinnerUndoLevels.setEnabled(false);
        } else {
            checkBoxUndo.setSelected(config.isUndoEnabled());
            spinnerUndoLevels.setValue(config.getUndoLevels());
        }
        
        checkBoxGrid.setSelected(config.isDefaultGridEnabled());
        spinnerGrid.setValue(config.getDefaultGridSize());
        checkBoxContours.setSelected(config.isDefaultContoursEnabled());
        spinnerContours.setValue(config.getDefaultContourSeparation());
        checkBoxViewDistance.setSelected(config.isDefaultViewDistanceEnabled());
        checkBoxWalkingDistance.setSelected(config.isDefaultWalkingDistanceEnabled());
        comboBoxLightDirection.setSelectedItem(config.getDefaultLightOrigin());
        checkBoxCircular.setSelected(config.isDefaultCircularWorld());
        spinnerBrushSize.setValue(config.getMaximumBrushSize());
        
        spinnerWidth.setValue(config.getDefaultWidth() * 128);
        spinnerHeight.setValue(config.getDefaultHeight() * 128);
        comboBoxPlatform.setSelectedItem(config.getDefaultPlatform());
        comboBoxHeight.setSelectedItem(config.getDefaultMaxHeight());
        if (config.isHilly()) {
            radioButtonHilly.setSelected(true);
        } else {
            radioButtonFlat.setSelected(true);
            spinnerRange.setEnabled(false);
            spinnerScale.setEnabled(false);
        }
        spinnerRange.setValue(Math.round(config.getDefaultRange()));
        spinnerScale.setValue((int) Math.round(config.getDefaultScale() * 100));
        spinnerGroundLevel.setValue(config.getLevel());
        spinnerWaterLevel.setValue(config.getWaterLevel());
        checkBoxLava.setSelected(config.isLava());
        checkBoxBeaches.setSelected(config.isBeaches());
        comboBoxSurfaceMaterial.setSelectedItem(config.getSurface());
        spinnerWorldBackups.setValue(config.getWorldFileBackups());
        checkBoxExtendedBlockIds.setSelected(config.isDefaultExtendedBlockIds());
        
        // Export settings
        checkBoxChestOfGoodies.setSelected(config.isDefaultCreateGoodiesChest());
        comboBoxWorldType.setSelectedItem(config.getDefaultGenerator().getType());
        generatorOptions = (config.getDefaultGenerator() instanceof CustomGenerator) ? ((CustomGenerator) config.getDefaultGenerator()).getName() : null;
        checkBoxStructures.setSelected(config.isDefaultMapFeatures());
        comboBoxMode.setSelectedItem(config.getDefaultGameType());
        checkBoxCheats.setSelected(config.isDefaultAllowCheats());

        previousMaxHeight = config.getDefaultMaxHeight();

        comboBoxLookAndFeel.setSelectedIndex(config.getLookAndFeel() != null ? config.getLookAndFeel().ordinal() : 0);
        if (config.getUiScale() == 0.0f) {
            radioButtonUIScaleAuto.setSelected(true);
            sliderUIScale.setValue((int) (GUIUtils.SYSTEM_UI_SCALE_FLOAT * 100));
        } else {
            radioButtonUIScaleManual.setSelected(true);
            sliderUIScale.setValue((int) (config.getUiScale() * 100));
        }
        updateLabelUIScale();
        
        switch (config.getAccelerationType()) {
            case DEFAULT:
                radioButtonAccelDefault.setSelected(true);
                break;
            case DIRECT3D:
                radioButtonAccelDirect3D.setSelected(true);
                break;
            case OPENGL:
                radioButtonAccelOpenGL.setSelected(true);
                break;
            case QUARTZ:
                radioButtonAccelQuartz.setSelected(true);
                break;
            case UNACCELERATED:
                radioButtonAccelUnaccelerated.setSelected(true);
                break;
            case XRENDER:
                radioButtonAccelXRender.setSelected(true);
                break;
        }
        
        switch (config.getOverlayType()) {
            case OPTIMISE_ON_LOAD:
                radioButtonOverlayOptimiseOnLoad.setSelected(true);
                break;
            case SCALE_ON_LOAD:
                radioButtonOverlayScaleOnLoad.setSelected(true);
                break;
            case SCALE_ON_PAINT:
                radioButtonOverlayScaleOnPaint.setSelected(true);
                break;
        }
        
        checkBoxAutoSave.setSelected(config.isAutosaveEnabled());
        spinnerAutoSaveGuardTime.setValue(config.getAutosaveDelay() / 1000);
        spinnerAutoSaveInterval.setValue(config.getAutosaveInterval() / 1000);
        spinnerFreeSpaceForMaps.setValue(config.getMinimumFreeSpaceForMaps());
        checkBoxAutoDeleteBackups.setSelected(config.isAutoDeleteBackups());

        defaultTerrainAndLayerSettings = config.getDefaultTerrainAndLayerSettings(); // TODO this should be cloned too
        defaultExportSettings = (config.getDefaultExportSettings() != null) ? config.getDefaultExportSettings().clone() : null;

        setControlStates();
    }
    
    private void saveSettings() {
        Configuration config = Configuration.getInstance();
        if (! pingNotSet) {
            config.setPingAllowed(checkBoxPing.isSelected());
        }
        if ((! "true".equals(System.getProperty("org.pepsoft.worldpainter.devMode")))
                && (! "true".equals(System.getProperty("org.pepsoft.worldpainter.disableUpdateCheck")))) {
            config.setCheckForUpdates(checkBoxCheckForUpdates.isSelected());
        }
        if (! "true".equals(System.getProperty("org.pepsoft.worldpainter.disableUndo"))) {
            config.setUndoEnabled(checkBoxUndo.isSelected());
            config.setUndoLevels(((Number) spinnerUndoLevels.getValue()).intValue());
        }
        config.setDefaultGridEnabled(checkBoxGrid.isSelected());
        config.setDefaultGridSize((Integer) spinnerGrid.getValue());
        config.setDefaultContoursEnabled(checkBoxContours.isSelected());
        config.setDefaultContourSeparation((Integer) spinnerContours.getValue());
        config.setDefaultViewDistanceEnabled(checkBoxViewDistance.isSelected());
        config.setDefaultWalkingDistanceEnabled(checkBoxWalkingDistance.isSelected());
        config.setDefaultLightOrigin((LightOrigin) comboBoxLightDirection.getSelectedItem());
        config.setDefaultWidth(((Integer) spinnerWidth.getValue()) / 128);
        // Set defaultCircularWorld *before* defaultHeight, otherwise defaultHeight might not take
        config.setDefaultCircularWorld(checkBoxCircular.isSelected());
        config.setDefaultHeight(((Integer) spinnerHeight.getValue()) / 128);
        final Platform platform = (Platform) comboBoxPlatform.getSelectedItem();
        config.setDefaultPlatform(platform);
        config.setDefaultMaxHeight((Integer) comboBoxHeight.getSelectedItem());
        config.setHilly(radioButtonHilly.isSelected());
        config.setDefaultRange(((Number) spinnerRange.getValue()).floatValue());
        config.setDefaultScale((Integer) spinnerScale.getValue() / 100.0);
        config.setLevel((Integer) spinnerGroundLevel.getValue());
        config.setWaterLevel((Integer) spinnerWaterLevel.getValue());
        config.setLava(checkBoxLava.isSelected());
        config.setBeaches(checkBoxBeaches.isSelected());
        config.setSurface((Terrain) comboBoxSurfaceMaterial.getSelectedItem());
        config.setWorldFileBackups((Integer) spinnerWorldBackups.getValue());
        config.setMaximumBrushSize((Integer) spinnerBrushSize.getValue());
        config.setDefaultExtendedBlockIds(checkBoxExtendedBlockIds.isSelected());
        
        // Export settings
        config.setDefaultCreateGoodiesChest(checkBoxChestOfGoodies.isSelected());
        final MapGenerator defaultGenerator;
        final Generator generatorType = (Generator) comboBoxWorldType.getSelectedItem();
        switch (generatorType) {
            case DEFAULT:
            case LARGE_BIOMES:
            case AMPLIFIED:
            case NETHER:
            case END:
                defaultGenerator = new SeededGenerator(generatorType, DEFAULT_OCEAN_SEED);
                break;
            case FLAT:
                defaultGenerator = new SuperflatGenerator(SuperflatPreset.defaultPreset(platform));
                break;
            default:
                throw new InternalError("Generator type " + generatorType + " not supported");
        }
        config.setDefaultGenerator(defaultGenerator);
        config.setDefaultMapFeatures(checkBoxStructures.isSelected());
        config.setDefaultGameType((GameType) comboBoxMode.getSelectedItem());
        config.setDefaultAllowCheats(checkBoxCheats.isSelected());

        config.setLookAndFeel(Configuration.LookAndFeel.values()[comboBoxLookAndFeel.getSelectedIndex()]);
        if (radioButtonUIScaleAuto.isSelected()) {
            config.setUiScale(0.0f);
        } else {
            config.setUiScale(sliderUIScale.getValue() / 100.0f);
        }
        
        if (radioButtonAccelDefault.isSelected()) {
            config.setAccelerationType(AccelerationType.DEFAULT);
        } else if (radioButtonAccelDirect3D.isSelected()) {
            config.setAccelerationType(AccelerationType.DIRECT3D);
        } else if (radioButtonAccelOpenGL.isSelected()) {
            config.setAccelerationType(AccelerationType.OPENGL);
        } else if (radioButtonAccelQuartz.isSelected()) {
            config.setAccelerationType(AccelerationType.QUARTZ);
        } else if (radioButtonAccelUnaccelerated.isSelected()) {
            config.setAccelerationType(AccelerationType.UNACCELERATED);
        } else if (radioButtonAccelXRender.isSelected()) {
            config.setAccelerationType(AccelerationType.XRENDER);
        }
        
        if (radioButtonOverlayOptimiseOnLoad.isSelected()) {
            config.setOverlayType(Configuration.OverlayType.OPTIMISE_ON_LOAD);
        } else if (radioButtonOverlayScaleOnLoad.isSelected()) {
            config.setOverlayType(Configuration.OverlayType.SCALE_ON_LOAD);
        } else if (radioButtonOverlayScaleOnPaint.isSelected()) {
            config.setOverlayType(Configuration.OverlayType.SCALE_ON_PAINT);
        }
        
        config.setAutosaveEnabled(checkBoxAutoSave.isSelected());
        config.setAutosaveDelay(((Integer) spinnerAutoSaveGuardTime.getValue()) * 1000);
        config.setAutosaveInterval(((Integer) spinnerAutoSaveInterval.getValue()) * 1000);
        config.setMinimumFreeSpaceForMaps((Integer) spinnerFreeSpaceForMaps.getValue());
        config.setAutoDeleteBackups(checkBoxAutoDeleteBackups.isSelected());
        config.setDefaultExportSettings(defaultExportSettings);

        try {
            config.save();
        } catch (IOException e) {
            ErrorDialog errorDialog = new ErrorDialog(this);
            errorDialog.setException(e);
            errorDialog.setVisible(true);
        }
    }
    
    private void setControlStates() {
        spinnerUndoLevels.setEnabled(checkBoxUndo.isSelected());
        boolean hilly = radioButtonHilly.isSelected();
        spinnerRange.setEnabled(hilly);
        spinnerScale.setEnabled(hilly);
        spinnerHeight.setEnabled(! checkBoxCircular.isSelected());
        buttonModePreset.setEnabled(comboBoxWorldType.getSelectedItem() == FLAT);
        boolean autosaveEnabled = checkBoxAutoSave.isSelected();
        boolean autosaveInhibited = Configuration.getInstance().isAutosaveInhibited();
        checkBoxAutoSave.setEnabled(! autosaveInhibited);
        spinnerAutoSaveGuardTime.setEnabled(autosaveEnabled && (! autosaveInhibited));
        spinnerAutoSaveInterval.setEnabled(autosaveEnabled && (! autosaveInhibited));
        sliderUIScale.setEnabled(radioButtonUIScaleManual.isSelected());
    }

    private void updateLabelUIScale() {
        if (radioButtonUIScaleAuto.isSelected()) {
            labelUIScale.setText((int) (GUIUtils.SYSTEM_UI_SCALE_FLOAT * 100) + "%");
        } else {
            labelUIScale.setText(sliderUIScale.getValue() + "%");
        }
    }
    
    private void editTerrainAndLayerSettings() {
        Configuration config = Configuration.getInstance();
        Dimension defaultSettings = config.getDefaultTerrainAndLayerSettings();
        DimensionPropertiesDialog dialog = new DimensionPropertiesDialog(this, defaultSettings, colourScheme, true);
        dialog.setVisible(true);
        TileFactory tileFactory = defaultSettings.getTileFactory();
        if ((tileFactory instanceof HeightMapTileFactory)
                && (((HeightMapTileFactory) tileFactory).getTheme() instanceof SimpleTheme)) {
            HeightMapTileFactory heightMapTileFactory = (HeightMapTileFactory) tileFactory;
            SimpleTheme theme = (SimpleTheme) ((HeightMapTileFactory) tileFactory).getTheme();
            checkBoxBeaches.setSelected(theme.isBeaches());
            int waterLevel = heightMapTileFactory.getWaterHeight();
            spinnerWaterLevel.setValue(waterLevel);
            defaultSettings.setBorderLevel(heightMapTileFactory.getWaterHeight());
            SortedMap<Integer, Terrain> terrainRanges = theme.getTerrainRanges();
            comboBoxSurfaceMaterial.setSelectedItem(terrainRanges.get(terrainRanges.headMap(waterLevel + 3).lastKey()));
        }
    }
    
    private void cleanUpBackupsNow() {
        Configuration config = Configuration.getInstance();
        int oldMinimumFreeSpaceForMaps = config.getMinimumFreeSpaceForMaps();
        config.setMinimumFreeSpaceForMaps((Integer) spinnerFreeSpaceForMaps.getValue());
        try {
            BackupUtils.cleanUpBackups(null, this);
        } catch (IOException e) {
            throw new RuntimeException("I/O error while cleaning backups", e);
        } finally {
            config.setMinimumFreeSpaceForMaps(oldMinimumFreeSpaceForMaps);
        }
    }

    private void platformSelected() {
        Platform platform = (Platform) comboBoxPlatform.getSelectedItem();
        final Generator currentGenerator = (Generator) comboBoxWorldType.getSelectedItem();
        final List<Generator> supportedGenerators = new ArrayList<>(platform.supportedGenerators);
        supportedGenerators.retainAll(asList(DEFAULT, LARGE_BIOMES, AMPLIFIED, NETHER, END, FLAT));
        comboBoxWorldType.setModel(new DefaultComboBoxModel<>(supportedGenerators.toArray(new Generator[0])));
        if ((currentGenerator != null) && supportedGenerators.contains(currentGenerator)) {
            comboBoxWorldType.setSelectedItem(currentGenerator);
        }
        final Integer currentMaxHeight = (Integer) comboBoxHeight.getSelectedItem();
        final List<Integer> supportedMaxHeights = stream(platform.maxHeights).boxed().collect(toList());
        comboBoxHeight.setModel(new DefaultComboBoxModel<>(supportedMaxHeights.toArray(new Integer[0])));
        final int newMaxHeight;
        if ((currentMaxHeight != null) && supportedMaxHeights.contains(currentMaxHeight)) {
            newMaxHeight = currentMaxHeight;
        } else {
            newMaxHeight = platform.standardMaxHeight;
        }
        comboBoxHeight.setSelectedItem(newMaxHeight);
        final GameType currentGameType = (GameType) comboBoxMode.getSelectedItem();
        final List<GameType> supportedGameTypes = platform.supportedGameTypes;
        comboBoxMode.setModel(new DefaultComboBoxModel<>(supportedGameTypes.toArray(new GameType[0])));
        if ((currentGameType != null) && supportedGameTypes.contains(currentGameType)) {
            comboBoxMode.setSelectedItem(currentGameType);
        }
        checkBoxChestOfGoodies.setEnabled((platform != JAVA_ANVIL_1_15) && (platform != JAVA_ANVIL_1_17));
        checkBoxExtendedBlockIds.setEnabled(platform.capabilities.contains(BLOCK_BASED) && (! platform.capabilities.contains(NAME_BASED)) && (platform != JAVA_MCREGION));
        final Dimension defaultTerrainAndLayerSettings = Configuration.getInstance().getDefaultTerrainAndLayerSettings();
        defaultTerrainAndLayerSettings.setMinHeight(platform.minZ);
        defaultTerrainAndLayerSettings.setMaxHeight(newMaxHeight);
        defaultTerrainAndLayerSettings.getTileFactory().setMinMaxHeight(platform.minZ, newMaxHeight, IDENTITY);

        // Check whether this platform supports the current default export settings (or any export settings)
        final PlatformProvider platformProvider = PlatformManager.getInstance().getPlatformProvider(platform);
        final ExportSettings platformDefaultExportSettings = platformProvider.getDefaultExportSettings();
        if (platformDefaultExportSettings != null) {
            labelEditExportSettingsLink.setForeground(BLUE);
            labelEditExportSettingsLink.setCursor(new Cursor(HAND_CURSOR));
            if ((defaultExportSettings != null) && (platformDefaultExportSettings.getClass() != defaultExportSettings.getClass())) {
                defaultExportSettings = null;
            }
        } else {
            defaultExportSettings = null;
            labelEditExportSettingsLink.setForeground(GRAY);
            labelEditExportSettingsLink.setCursor(null);
        }
    }

    private void editDefaultExportSettings() {
        final Platform platform = (Platform) comboBoxPlatform.getSelectedItem();
        final PlatformProvider platformProvider = PlatformManager.getInstance().getPlatformProvider(platform);
        final ExportSettings platformDefaultExportSettings = platformProvider.getDefaultExportSettings();
        if (platformDefaultExportSettings != null) {
            final ExportSettingsEditor editor = platformProvider.getExportSettingsEditor();
            if ((defaultExportSettings != null) && (defaultExportSettings.getClass() == platformDefaultExportSettings.getClass())) {
                editor.setExportSettings(defaultExportSettings);
            } else {
                editor.setExportSettings(platformDefaultExportSettings);
            }
            final WorldPainterDialog dialog = new WorldPainterDialog(this);
            dialog.setTitle("Configure Default Post Processing Settings");
            dialog.getContentPane().add(editor, CENTER);
            final JPanel panel = new JPanel(new FlowLayout(RIGHT));
            final boolean[] reset = { false };
            panel.add(new JButton(new AbstractAction("Reset") {
                @Override
                public void actionPerformed(ActionEvent e) {
                    reset[0] = true;
                    dialog.ok();
                }
            }));
            final JButton okButton = new JButton(new AbstractAction("OK") {
                @Override
                public void actionPerformed(ActionEvent e) {
                    dialog.ok();
                }
            });
            panel.add(okButton);
            panel.add(new JButton(new AbstractAction("Cancel") {
                @Override
                public void actionPerformed(ActionEvent e) {
                    dialog.cancel();
                }
            }));
            dialog.getContentPane().add(panel, SOUTH);
            dialog.getRootPane().setDefaultButton(okButton);
            dialog.pack();
            dialog.setLocationRelativeTo(this);
            dialog.setVisible(true);
            if (! dialog.isCancelled()) {
                if (reset[0]) {
                    defaultExportSettings = null;
                    JOptionPane.showMessageDialog(this, "Default post processing settings reset to default values.", "Default Post Processing Settings Reset", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    defaultExportSettings = editor.getExportSettings();
                }
            }
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
        buttonGroup2 = new javax.swing.ButtonGroup();
        buttonGroup3 = new javax.swing.ButtonGroup();
        buttonGroup4 = new javax.swing.ButtonGroup();
        jTabbedPane1 = new javax.swing.JTabbedPane();
        jPanel1 = new javax.swing.JPanel();
        checkBoxPing = new javax.swing.JCheckBox();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        checkBoxCheckForUpdates = new javax.swing.JCheckBox();
        jLabel20 = new javax.swing.JLabel();
        spinnerWorldBackups = new javax.swing.JSpinner();
        jLabel30 = new javax.swing.JLabel();
        comboBoxLookAndFeel = new javax.swing.JComboBox();
        jLabel32 = new javax.swing.JLabel();
        checkBoxAutoSave = new javax.swing.JCheckBox();
        jLabel45 = new javax.swing.JLabel();
        spinnerAutoSaveGuardTime = new javax.swing.JSpinner();
        jLabel46 = new javax.swing.JLabel();
        spinnerAutoSaveInterval = new javax.swing.JSpinner();
        jLabel47 = new javax.swing.JLabel();
        jLabel48 = new javax.swing.JLabel();
        checkBoxUndo = new javax.swing.JCheckBox();
        jLabel5 = new javax.swing.JLabel();
        spinnerUndoLevels = new javax.swing.JSpinner();
        jLabel2 = new javax.swing.JLabel();
        sliderUIScale = new javax.swing.JSlider();
        labelUIScale = new javax.swing.JLabel();
        radioButtonUIScaleAuto = new javax.swing.JRadioButton();
        radioButtonUIScaleManual = new javax.swing.JRadioButton();
        jSeparator1 = new javax.swing.JSeparator();
        jLabel49 = new javax.swing.JLabel();
        jLabel50 = new javax.swing.JLabel();
        spinnerFreeSpaceForMaps = new javax.swing.JSpinner();
        jLabel51 = new javax.swing.JLabel();
        checkBoxAutoDeleteBackups = new javax.swing.JCheckBox();
        buttonCleanUpBackupsNow = new javax.swing.JButton();
        jPanel5 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jLabel18 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        jSeparator2 = new javax.swing.JSeparator();
        checkBoxGrid = new javax.swing.JCheckBox();
        checkBoxContours = new javax.swing.JCheckBox();
        checkBoxViewDistance = new javax.swing.JCheckBox();
        jLabel22 = new javax.swing.JLabel();
        comboBoxLightDirection = new javax.swing.JComboBox();
        jLabel7 = new javax.swing.JLabel();
        spinnerGrid = new javax.swing.JSpinner();
        jLabel8 = new javax.swing.JLabel();
        spinnerContours = new javax.swing.JSpinner();
        checkBoxWalkingDistance = new javax.swing.JCheckBox();
        jLabel26 = new javax.swing.JLabel();
        spinnerBrushSize = new javax.swing.JSpinner();
        jLabel21 = new javax.swing.JLabel();
        jLabel27 = new javax.swing.JLabel();
        jLabel9 = new javax.swing.JLabel();
        jSeparator3 = new javax.swing.JSeparator();
        jLabel10 = new javax.swing.JLabel();
        spinnerWidth = new javax.swing.JSpinner();
        jLabel11 = new javax.swing.JLabel();
        spinnerHeight = new javax.swing.JSpinner();
        jLabel19 = new javax.swing.JLabel();
        jLabel12 = new javax.swing.JLabel();
        comboBoxHeight = new javax.swing.JComboBox<>();
        jLabel13 = new javax.swing.JLabel();
        radioButtonHilly = new javax.swing.JRadioButton();
        jLabel23 = new javax.swing.JLabel();
        spinnerRange = new javax.swing.JSpinner();
        jLabel24 = new javax.swing.JLabel();
        spinnerScale = new javax.swing.JSpinner();
        jLabel25 = new javax.swing.JLabel();
        radioButtonFlat = new javax.swing.JRadioButton();
        checkBoxCircular = new javax.swing.JCheckBox();
        jLabel14 = new javax.swing.JLabel();
        spinnerGroundLevel = new javax.swing.JSpinner();
        jLabel15 = new javax.swing.JLabel();
        spinnerWaterLevel = new javax.swing.JSpinner();
        checkBoxLava = new javax.swing.JCheckBox();
        checkBoxBeaches = new javax.swing.JCheckBox();
        jLabel16 = new javax.swing.JLabel();
        comboBoxSurfaceMaterial = new javax.swing.JComboBox();
        checkBoxExtendedBlockIds = new javax.swing.JCheckBox();
        buttonReset = new javax.swing.JButton();
        labelTerrainAndLayerSettings = new javax.swing.JLabel();
        jLabel17 = new javax.swing.JLabel();
        jSeparator5 = new javax.swing.JSeparator();
        checkBoxChestOfGoodies = new javax.swing.JCheckBox();
        jLabel28 = new javax.swing.JLabel();
        comboBoxWorldType = new javax.swing.JComboBox<>();
        buttonModePreset = new javax.swing.JButton();
        checkBoxStructures = new javax.swing.JCheckBox();
        jLabel29 = new javax.swing.JLabel();
        comboBoxMode = new javax.swing.JComboBox<>();
        checkBoxCheats = new javax.swing.JCheckBox();
        jLabel52 = new javax.swing.JLabel();
        comboBoxPlatform = new javax.swing.JComboBox<>();
        labelEditExportSettingsLink = new javax.swing.JLabel();
        jPanel2 = new javax.swing.JPanel();
        jPanel3 = new javax.swing.JPanel();
        jLabel31 = new javax.swing.JLabel();
        radioButtonAccelDefault = new javax.swing.JRadioButton();
        radioButtonAccelDirect3D = new javax.swing.JRadioButton();
        radioButtonAccelOpenGL = new javax.swing.JRadioButton();
        radioButtonAccelQuartz = new javax.swing.JRadioButton();
        radioButtonAccelXRender = new javax.swing.JRadioButton();
        radioButtonAccelUnaccelerated = new javax.swing.JRadioButton();
        jLabel33 = new javax.swing.JLabel();
        jLabel34 = new javax.swing.JLabel();
        jLabel36 = new javax.swing.JLabel();
        jLabel37 = new javax.swing.JLabel();
        jLabel38 = new javax.swing.JLabel();
        jLabel39 = new javax.swing.JLabel();
        jLabel35 = new javax.swing.JLabel();
        jPanel4 = new javax.swing.JPanel();
        jLabel40 = new javax.swing.JLabel();
        jLabel41 = new javax.swing.JLabel();
        radioButtonOverlayScaleOnLoad = new javax.swing.JRadioButton();
        radioButtonOverlayOptimiseOnLoad = new javax.swing.JRadioButton();
        radioButtonOverlayScaleOnPaint = new javax.swing.JRadioButton();
        jLabel42 = new javax.swing.JLabel();
        jLabel43 = new javax.swing.JLabel();
        jLabel44 = new javax.swing.JLabel();
        buttonCancel = new javax.swing.JButton();
        buttonOK = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Preferences");

        checkBoxPing.setSelected(true);
        checkBoxPing.setText("Send usage information to the developer");
        checkBoxPing.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                checkBoxPingActionPerformed(evt);
            }
        });

        jLabel3.setFont(jLabel3.getFont().deriveFont((jLabel3.getFont().getStyle() | java.awt.Font.ITALIC)));
        jLabel3.setText("Note that the information does not include personally identifiable ");

        jLabel4.setFont(jLabel4.getFont().deriveFont((jLabel4.getFont().getStyle() | java.awt.Font.ITALIC)));
        jLabel4.setText("information, and will never be sold or given to third parties. ");

        checkBoxCheckForUpdates.setSelected(true);
        checkBoxCheckForUpdates.setText("Check for updates on startup");

        jLabel20.setText("No. of backups of .world files to keep:");

        spinnerWorldBackups.setModel(new javax.swing.SpinnerNumberModel(3, 0, null, 1));

        jLabel30.setText("Visual theme:");

        comboBoxLookAndFeel.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "System", "Metal", "Nimbus", "Dark Metal", "Dark Nimbus" }));
        comboBoxLookAndFeel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                comboBoxLookAndFeelActionPerformed(evt);
            }
        });

        jLabel32.setText("<html><em>Effective after restart  </em></html>");

        checkBoxAutoSave.setSelected(true);
        checkBoxAutoSave.setText("Enable autosave");
        checkBoxAutoSave.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                checkBoxAutoSaveActionPerformed(evt);
            }
        });

        jLabel45.setText("Guard time:");

        spinnerAutoSaveGuardTime.setModel(new javax.swing.SpinnerNumberModel(10, 1, 999, 1));

        jLabel46.setText("Autosave interval:");

        spinnerAutoSaveInterval.setModel(new javax.swing.SpinnerNumberModel(300, 1, 9999, 1));

        jLabel47.setText("seconds");

        jLabel48.setText("seconds");

        checkBoxUndo.setSelected(true);
        checkBoxUndo.setText("Enable undo");
        checkBoxUndo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                checkBoxUndoActionPerformed(evt);
            }
        });

        jLabel5.setLabelFor(spinnerUndoLevels);
        jLabel5.setText("Undo levels:");

        spinnerUndoLevels.setModel(new javax.swing.SpinnerNumberModel(25, 1, 999, 1));

        jLabel2.setLabelFor(sliderUIScale);
        jLabel2.setText("UI scale:");

        sliderUIScale.setMajorTickSpacing(25);
        sliderUIScale.setMaximum(400);
        sliderUIScale.setMinimum(25);
        sliderUIScale.setSnapToTicks(true);
        sliderUIScale.setValue(100);
        sliderUIScale.setEnabled(false);
        sliderUIScale.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                sliderUIScaleStateChanged(evt);
            }
        });

        labelUIScale.setText("100 %");

        buttonGroup4.add(radioButtonUIScaleAuto);
        radioButtonUIScaleAuto.setText("auto:");
        radioButtonUIScaleAuto.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                radioButtonUIScaleAutoActionPerformed(evt);
            }
        });

        buttonGroup4.add(radioButtonUIScaleManual);
        radioButtonUIScaleManual.setText("manual:");
        radioButtonUIScaleManual.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                radioButtonUIScaleManualActionPerformed(evt);
            }
        });

        jSeparator1.setOrientation(javax.swing.SwingConstants.VERTICAL);

        jLabel49.setText("Backup settings");

        jLabel50.setText("Minimum free space on drive:");

        spinnerFreeSpaceForMaps.setModel(new javax.swing.SpinnerNumberModel(2, 1, 999, 1));

        jLabel51.setText("GB");

        checkBoxAutoDeleteBackups.setSelected(true);
        checkBoxAutoDeleteBackups.setText("Offer to delete old map backups on Export and Merge as necessary");
        checkBoxAutoDeleteBackups.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                checkBoxAutoDeleteBackupsActionPerformed(evt);
            }
        });

        buttonCleanUpBackupsNow.setText("Clean Up Backups Now");
        buttonCleanUpBackupsNow.setToolTipText("Delete backups, oldest first, until there is at least the indicated amount of space free");
        buttonCleanUpBackupsNow.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonCleanUpBackupsNowActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(checkBoxPing)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(checkBoxCheckForUpdates)
                            .addComponent(checkBoxUndo)
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jLabel30)
                                    .addComponent(jLabel2))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(comboBoxLookAndFeel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(jLabel32, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addGroup(jPanel1Layout.createSequentialGroup()
                                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                            .addComponent(radioButtonUIScaleManual)
                                            .addComponent(radioButtonUIScaleAuto))
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                            .addComponent(labelUIScale)
                                            .addComponent(sliderUIScale, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))))
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addGap(12, 12, 12)
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jLabel4)
                                    .addComponent(jLabel3)
                                    .addGroup(jPanel1Layout.createSequentialGroup()
                                        .addComponent(jLabel5)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(spinnerUndoLevels, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))))
                        .addGap(22, 22, 22)
                        .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGap(18, 18, 18)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel49)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jLabel20)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(spinnerWorldBackups, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(checkBoxAutoSave)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(19, 19, 19)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addComponent(jLabel45)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(spinnerAutoSaveGuardTime, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(0, 0, 0)
                                .addComponent(jLabel47))
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addComponent(jLabel46)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(spinnerAutoSaveInterval, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(0, 0, 0)
                                .addComponent(jLabel48))))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jLabel50)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(spinnerFreeSpaceForMaps, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel51))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(10, 10, 10)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(buttonCleanUpBackupsNow)
                            .addComponent(checkBoxAutoDeleteBackups))))
                .addContainerGap(19, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jSeparator1)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addComponent(checkBoxPing)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabel3)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabel4)
                                .addGap(18, 18, 18)
                                .addComponent(checkBoxCheckForUpdates)
                                .addGap(18, 18, 18)
                                .addComponent(checkBoxUndo)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(jLabel5)
                                    .addComponent(spinnerUndoLevels, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGap(18, 18, 18)
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(jLabel30)
                                    .addComponent(comboBoxLookAndFeel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(radioButtonUIScaleAuto)
                                    .addComponent(jLabel2)
                                    .addComponent(labelUIScale))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(jPanel1Layout.createSequentialGroup()
                                        .addComponent(radioButtonUIScaleManual)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(jLabel32, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                    .addComponent(sliderUIScale, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addComponent(jLabel49)
                                .addGap(18, 18, 18)
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(jLabel20)
                                    .addComponent(spinnerWorldBackups, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGap(18, 18, 18)
                                .addComponent(checkBoxAutoSave)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(jLabel45)
                                    .addComponent(spinnerAutoSaveGuardTime, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(jLabel47))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(jLabel46)
                                    .addComponent(spinnerAutoSaveInterval, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(jLabel48))
                                .addGap(18, 18, 18)
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(jLabel50)
                                    .addComponent(spinnerFreeSpaceForMaps, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(jLabel51))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(checkBoxAutoDeleteBackups)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(buttonCleanUpBackupsNow)))
                        .addGap(0, 166, Short.MAX_VALUE)))
                .addContainerGap())
        );

        jTabbedPane1.addTab("General", jPanel1);

        jLabel1.setText("Configure your default settings on this screen:");

        jLabel18.setFont(jLabel18.getFont().deriveFont((jLabel18.getFont().getStyle() | java.awt.Font.ITALIC)));
        jLabel18.setText("(Note that changes to these settings will only take effect for the next world you load or create.) ");

        jLabel6.setFont(jLabel6.getFont().deriveFont((jLabel6.getFont().getStyle() | java.awt.Font.ITALIC)));
        jLabel6.setText("Default view settings ");

        checkBoxGrid.setText("Grid enabled");

        checkBoxContours.setSelected(true);
        checkBoxContours.setText("Contour lines enabled");

        checkBoxViewDistance.setText("View distance enabled");

        jLabel22.setText("Light direction:");

        comboBoxLightDirection.setModel(new DefaultComboBoxModel(LightOrigin.values()));
        comboBoxLightDirection.setRenderer(new EnumListCellRenderer());

        jLabel7.setLabelFor(spinnerGrid);
        jLabel7.setText("Grid size:");

        spinnerGrid.setModel(new javax.swing.SpinnerNumberModel(128, 2, 999, 1));

        jLabel8.setText("Separation:");

        spinnerContours.setModel(new javax.swing.SpinnerNumberModel(10, 2, 999, 1));

        checkBoxWalkingDistance.setText("Walking distance enabled");

        jLabel26.setText("Maximum brush size:");

        spinnerBrushSize.setModel(new javax.swing.SpinnerNumberModel(300, 100, null, 10));

        jLabel21.setText(" ");

        jLabel27.setFont(jLabel27.getFont().deriveFont((jLabel27.getFont().getStyle() | java.awt.Font.ITALIC)));
        jLabel27.setText("Warning: large brush sizes could slow your computer to a crawl! ");

        jLabel9.setFont(jLabel9.getFont().deriveFont((jLabel9.getFont().getStyle() | java.awt.Font.ITALIC)));
        jLabel9.setText("Default world settings ");

        jLabel10.setLabelFor(spinnerWidth);
        jLabel10.setText("Dimensions:");

        spinnerWidth.setModel(new javax.swing.SpinnerNumberModel(640, 128, null, 128));
        spinnerWidth.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                spinnerWidthStateChanged(evt);
            }
        });

        jLabel11.setText("x");

        spinnerHeight.setModel(new javax.swing.SpinnerNumberModel(640, 128, null, 128));
        spinnerHeight.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                spinnerHeightStateChanged(evt);
            }
        });

        jLabel19.setText("blocks");

        jLabel12.setLabelFor(comboBoxHeight);
        jLabel12.setText("Height:");

        comboBoxHeight.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                comboBoxHeightActionPerformed(evt);
            }
        });

        jLabel13.setText("Topography:");

        buttonGroup1.add(radioButtonHilly);
        radioButtonHilly.setSelected(true);
        radioButtonHilly.setText("Hilly");
        radioButtonHilly.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                radioButtonHillyActionPerformed(evt);
            }
        });

        jLabel23.setText("(height:");

        spinnerRange.setModel(new javax.swing.SpinnerNumberModel(20, 1, 255, 1));

        jLabel24.setText("scale:");

        spinnerScale.setModel(new javax.swing.SpinnerNumberModel(100, 1, 999, 1));

        jLabel25.setText("%)");

        buttonGroup1.add(radioButtonFlat);
        radioButtonFlat.setText("Flat");
        radioButtonFlat.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                radioButtonFlatActionPerformed(evt);
            }
        });

        checkBoxCircular.setText("Circular world");
        checkBoxCircular.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                checkBoxCircularActionPerformed(evt);
            }
        });

        jLabel14.setLabelFor(spinnerGroundLevel);
        jLabel14.setText("Level:");

        spinnerGroundLevel.setModel(new javax.swing.SpinnerNumberModel(58, 1, 255, 1));

        jLabel15.setText("Water level:");

        spinnerWaterLevel.setModel(new javax.swing.SpinnerNumberModel(62, 0, 255, 1));
        spinnerWaterLevel.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                spinnerWaterLevelStateChanged(evt);
            }
        });

        checkBoxLava.setText("Lava instead of water");

        checkBoxBeaches.setSelected(true);
        checkBoxBeaches.setText("Beaches");
        checkBoxBeaches.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                checkBoxBeachesActionPerformed(evt);
            }
        });

        jLabel16.setText("Surface material:");

        comboBoxSurfaceMaterial.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        comboBoxSurfaceMaterial.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                comboBoxSurfaceMaterialActionPerformed(evt);
            }
        });

        checkBoxExtendedBlockIds.setText("Extended block ID's");

        buttonReset.setText("Reset...");
        buttonReset.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonResetActionPerformed(evt);
            }
        });

        labelTerrainAndLayerSettings.setForeground(java.awt.Color.blue);
        labelTerrainAndLayerSettings.setText("<html><u>Configure default border, terrain and layer settings</u></html>");
        labelTerrainAndLayerSettings.setCursor(new java.awt.Cursor(HAND_CURSOR));
        labelTerrainAndLayerSettings.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                labelTerrainAndLayerSettingsMouseClicked(evt);
            }
        });

        jLabel17.setFont(jLabel17.getFont().deriveFont((jLabel17.getFont().getStyle() | java.awt.Font.ITALIC)));
        jLabel17.setText("Default export settings");

        checkBoxChestOfGoodies.setText("Include chest of goodies");

        jLabel28.setText("World type:");

        comboBoxWorldType.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                comboBoxWorldTypeActionPerformed(evt);
            }
        });

        buttonModePreset.setText("...");
        buttonModePreset.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonModePresetActionPerformed(evt);
            }
        });

        checkBoxStructures.setText("Structures");

        jLabel29.setText("Mode:");

        comboBoxMode.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                comboBoxModeActionPerformed(evt);
            }
        });

        checkBoxCheats.setText("Allow Cheats");

        jLabel52.setText("Map format:");

        comboBoxPlatform.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                comboBoxPlatformActionPerformed(evt);
            }
        });

        labelEditExportSettingsLink.setForeground(new java.awt.Color(0, 0, 255));
        labelEditExportSettingsLink.setText("<html><u>Configure default post processing settings</u></html>");
        labelEditExportSettingsLink.setCursor(new java.awt.Cursor(HAND_CURSOR));
        labelEditExportSettingsLink.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                labelEditExportSettingsLinkMouseClicked(evt);
            }
        });

        javax.swing.GroupLayout jPanel5Layout = new javax.swing.GroupLayout(jPanel5);
        jPanel5.setLayout(jPanel5Layout);
        jPanel5Layout.setHorizontalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jSeparator3)
                    .addComponent(jSeparator2)
                    .addGroup(jPanel5Layout.createSequentialGroup()
                        .addComponent(labelTerrainAndLayerSettings, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(buttonReset))
                    .addComponent(jSeparator5)
                    .addGroup(jPanel5Layout.createSequentialGroup()
                        .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel1)
                            .addComponent(jLabel18)
                            .addGroup(jPanel5Layout.createSequentialGroup()
                                .addComponent(checkBoxChestOfGoodies)
                                .addGap(18, 18, 18)
                                .addComponent(checkBoxStructures)
                                .addGap(18, 18, 18)
                                .addComponent(jLabel29)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(comboBoxMode, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(checkBoxCheats)
                                .addGap(18, 18, 18)
                                .addComponent(labelEditExportSettingsLink, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(jPanel5Layout.createSequentialGroup()
                                .addComponent(jLabel10)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(spinnerWidth, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabel11)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(spinnerHeight, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(0, 0, 0)
                                .addComponent(jLabel19)
                                .addGap(18, 18, 18)
                                .addComponent(jLabel12)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(comboBoxHeight, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(18, 18, 18)
                                .addComponent(jLabel13)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(radioButtonHilly)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabel23)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(spinnerRange, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabel24)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(spinnerScale, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(0, 0, 0)
                                .addComponent(jLabel25)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(radioButtonFlat))
                            .addComponent(jLabel6)
                            .addGroup(jPanel5Layout.createSequentialGroup()
                                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(jPanel5Layout.createSequentialGroup()
                                        .addGap(12, 12, 12)
                                        .addComponent(jLabel7)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(spinnerGrid, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                    .addComponent(checkBoxGrid))
                                .addGap(18, 18, 18)
                                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(checkBoxContours)
                                    .addGroup(jPanel5Layout.createSequentialGroup()
                                        .addGap(12, 12, 12)
                                        .addComponent(jLabel8)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(spinnerContours, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                                .addGap(18, 18, 18)
                                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(checkBoxWalkingDistance)
                                    .addComponent(checkBoxViewDistance))
                                .addGap(18, 18, 18)
                                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(jPanel5Layout.createSequentialGroup()
                                        .addComponent(jLabel26)
                                        .addGap(6, 6, 6)
                                        .addComponent(spinnerBrushSize, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(jLabel21))
                                    .addGroup(jPanel5Layout.createSequentialGroup()
                                        .addComponent(jLabel22)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(comboBoxLightDirection, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                    .addComponent(jLabel27)))
                            .addComponent(jLabel9)
                            .addGroup(jPanel5Layout.createSequentialGroup()
                                .addComponent(checkBoxCircular)
                                .addGap(18, 18, 18)
                                .addComponent(jLabel14)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(spinnerGroundLevel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(18, 18, 18)
                                .addComponent(jLabel15)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(spinnerWaterLevel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(18, 18, 18)
                                .addComponent(checkBoxLava)
                                .addGap(18, 18, 18)
                                .addComponent(checkBoxBeaches))
                            .addGroup(jPanel5Layout.createSequentialGroup()
                                .addComponent(jLabel16)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(comboBoxSurfaceMaterial, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(18, 18, 18)
                                .addComponent(checkBoxExtendedBlockIds))
                            .addComponent(jLabel17)
                            .addGroup(jPanel5Layout.createSequentialGroup()
                                .addComponent(jLabel52)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(comboBoxPlatform, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(18, 18, 18)
                                .addComponent(jLabel28)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(comboBoxWorldType, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(buttonModePreset)))
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        jPanel5Layout.setVerticalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel18)
                .addGap(18, 18, 18)
                .addComponent(jLabel6)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSeparator2, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(checkBoxGrid)
                    .addComponent(checkBoxContours)
                    .addComponent(checkBoxViewDistance)
                    .addComponent(jLabel22)
                    .addComponent(comboBoxLightDirection, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel7)
                    .addComponent(spinnerGrid, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel8)
                    .addComponent(spinnerContours, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(checkBoxWalkingDistance)
                    .addComponent(jLabel21)
                    .addComponent(jLabel26)
                    .addComponent(spinnerBrushSize, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel27)
                .addGap(18, 18, 18)
                .addComponent(jLabel9)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSeparator3, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jLabel28)
                        .addComponent(comboBoxWorldType, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(buttonModePreset))
                    .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jLabel52)
                        .addComponent(comboBoxPlatform, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel10)
                    .addComponent(spinnerWidth, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel11)
                    .addComponent(spinnerHeight, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel12)
                    .addComponent(comboBoxHeight, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel13)
                    .addComponent(radioButtonHilly)
                    .addComponent(radioButtonFlat)
                    .addComponent(jLabel19)
                    .addComponent(jLabel23)
                    .addComponent(spinnerRange, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel24)
                    .addComponent(spinnerScale, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel25))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel14)
                    .addComponent(spinnerGroundLevel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel15)
                    .addComponent(spinnerWaterLevel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(checkBoxLava)
                    .addComponent(checkBoxBeaches)
                    .addComponent(checkBoxCircular))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel16)
                    .addComponent(comboBoxSurfaceMaterial, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(checkBoxExtendedBlockIds))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(labelTerrainAndLayerSettings, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(buttonReset))
                .addGap(18, 18, 18)
                .addComponent(jLabel17)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSeparator5, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(checkBoxChestOfGoodies)
                    .addComponent(checkBoxStructures)
                    .addComponent(jLabel29)
                    .addComponent(comboBoxMode, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(checkBoxCheats)
                    .addComponent(labelEditExportSettingsLink, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab("Defaults", jPanel5);

        jPanel3.setBorder(javax.swing.BorderFactory.createTitledBorder("Hardware Acceleration"));

        jLabel31.setText("Select a method of hardware accelerated painting. Experiment with this to improve the general editor performance:");

        buttonGroup2.add(radioButtonAccelDefault);
        radioButtonAccelDefault.setText("Default");

        buttonGroup2.add(radioButtonAccelDirect3D);
        radioButtonAccelDirect3D.setText("Direct3D");

        buttonGroup2.add(radioButtonAccelOpenGL);
        radioButtonAccelOpenGL.setText("OpenGL");

        buttonGroup2.add(radioButtonAccelQuartz);
        radioButtonAccelQuartz.setText("Quartz");

        buttonGroup2.add(radioButtonAccelXRender);
        radioButtonAccelXRender.setText("XRender");

        buttonGroup2.add(radioButtonAccelUnaccelerated);
        radioButtonAccelUnaccelerated.setText("Unaccelerated");

        jLabel33.setText("Disable all hardware acceleration");

        jLabel34.setText("Uses the XRender X11 extension on Linux");

        jLabel36.setText("<html><em>Effective after restart  </em></html>");

        jLabel37.setText("Uses the OpenGL rendering system ");

        jLabel38.setText("Uses the Direct3D rendering system on Windows");

        jLabel39.setText("Uses Java's default rendering settings");

        jLabel35.setText("Uses Apple's Quartz rendering system on Mac OS X");

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel31)
                    .addComponent(jLabel36, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addComponent(radioButtonAccelUnaccelerated)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel34)
                            .addComponent(jLabel33)
                            .addComponent(jLabel35)
                            .addComponent(jLabel37)
                            .addComponent(jLabel38)
                            .addComponent(jLabel39)))
                    .addComponent(radioButtonAccelDirect3D)
                    .addComponent(radioButtonAccelDefault)
                    .addComponent(radioButtonAccelXRender)
                    .addComponent(radioButtonAccelQuartz)
                    .addComponent(radioButtonAccelOpenGL))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel31)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel36, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(radioButtonAccelDefault)
                    .addComponent(jLabel39))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(radioButtonAccelDirect3D)
                    .addComponent(jLabel38))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(radioButtonAccelOpenGL)
                    .addComponent(jLabel37))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(radioButtonAccelQuartz)
                    .addComponent(jLabel35))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(radioButtonAccelXRender)
                    .addComponent(jLabel34))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(radioButtonAccelUnaccelerated)
                    .addComponent(jLabel33))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel4.setBorder(javax.swing.BorderFactory.createTitledBorder("Overlay Scaling and Painting"));

        jLabel40.setText("Select a method of overlay image scaling and painting. Experiment with this to improve overlay image performance:");

        jLabel41.setText("<html><em>Effective after reload  </em></html>");

        buttonGroup3.add(radioButtonOverlayScaleOnLoad);
        radioButtonOverlayScaleOnLoad.setText("Scale on load");

        buttonGroup3.add(radioButtonOverlayOptimiseOnLoad);
        radioButtonOverlayOptimiseOnLoad.setText("Optimise on load, scale on paint");

        buttonGroup3.add(radioButtonOverlayScaleOnPaint);
        radioButtonOverlayScaleOnPaint.setText("Scale on paint");

        jLabel42.setText("Optimises the image when it is first loaded, but scales it when painting. Uses less memory.");

        jLabel43.setText("Scales and optimises the image in memory when it is first loaded. Uses a lot of memory.");

        jLabel44.setText("Does not optimise the image at all and scales it when painting. Uses least memory.");

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel40)
                    .addComponent(jLabel41, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(radioButtonOverlayOptimiseOnLoad)
                            .addComponent(radioButtonOverlayScaleOnLoad)
                            .addComponent(radioButtonOverlayScaleOnPaint))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel44)
                            .addComponent(jLabel43)
                            .addComponent(jLabel42))))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel40)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel41, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(radioButtonOverlayScaleOnLoad)
                    .addComponent(jLabel43))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(radioButtonOverlayOptimiseOnLoad)
                    .addComponent(jLabel42))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(radioButtonOverlayScaleOnPaint)
                    .addComponent(jLabel44))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab("Performance", jPanel2);

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

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jTabbedPane1)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(buttonOK)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(buttonCancel)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jTabbedPane1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(buttonCancel)
                    .addComponent(buttonOK))
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

    private void checkBoxUndoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_checkBoxUndoActionPerformed
        setControlStates();
    }//GEN-LAST:event_checkBoxUndoActionPerformed

    private void labelTerrainAndLayerSettingsMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_labelTerrainAndLayerSettingsMouseClicked
        editTerrainAndLayerSettings();
    }//GEN-LAST:event_labelTerrainAndLayerSettingsMouseClicked

    private void comboBoxHeightActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_comboBoxHeightActionPerformed
        int maxHeight = (Integer) comboBoxHeight.getSelectedItem();
        if (maxHeight != previousMaxHeight) {
            previousMaxHeight = maxHeight;
            
            int terrainLevel = (Integer) spinnerGroundLevel.getValue();
            int waterLevel = (Integer) spinnerWaterLevel.getValue();
            if (terrainLevel >= maxHeight) {
                spinnerGroundLevel.setValue(maxHeight - 1);
            }
            if (waterLevel >= maxHeight) {
                spinnerWaterLevel.setValue(maxHeight - 1);
            }
            ((SpinnerNumberModel) spinnerGroundLevel.getModel()).setMaximum(maxHeight - 1);
            ((SpinnerNumberModel) spinnerWaterLevel.getModel()).setMaximum(maxHeight - 1);
            
            int range = (Integer) spinnerRange.getValue();
            if (range >= maxHeight) {
                spinnerRange.setValue(maxHeight - 1);
            }
            ((SpinnerNumberModel) spinnerRange.getModel()).setMaximum(maxHeight - 1);
                        
            setControlStates();
        }
    }//GEN-LAST:event_comboBoxHeightActionPerformed

    private void spinnerWaterLevelStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_spinnerWaterLevelStateChanged
        int waterLevel = ((Number) spinnerWaterLevel.getValue()).intValue();
        Dimension defaults = Configuration.getInstance().getDefaultTerrainAndLayerSettings();
        defaults.setBorderLevel(waterLevel);
        TileFactory tileFactory = defaults.getTileFactory();
        if (tileFactory instanceof HeightMapTileFactory) {
            ((HeightMapTileFactory) tileFactory).setWaterHeight(waterLevel);
        }
    }//GEN-LAST:event_spinnerWaterLevelStateChanged

    private void checkBoxBeachesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_checkBoxBeachesActionPerformed
        TileFactory tileFactory = Configuration.getInstance().getDefaultTerrainAndLayerSettings().getTileFactory();
        if (tileFactory instanceof HeightMapTileFactory) {
            ((HeightMapTileFactory) tileFactory).setBeaches(checkBoxBeaches.isSelected());
        }
    }//GEN-LAST:event_checkBoxBeachesActionPerformed

    private void comboBoxSurfaceMaterialActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_comboBoxSurfaceMaterialActionPerformed
        // Update the terrain ranges map to conform to the surface material
        // setting
        Configuration config = Configuration.getInstance();
        Dimension defaultSettings = config.getDefaultTerrainAndLayerSettings();
        TileFactory tileFactory = defaultSettings.getTileFactory();
        if ((tileFactory instanceof HeightMapTileFactory)
                && (((HeightMapTileFactory) tileFactory).getTheme() instanceof SimpleTheme)) {
            SortedMap<Integer, Terrain> defaultTerrainRanges = ((SimpleTheme) ((HeightMapTileFactory) tileFactory).getTheme()).getTerrainRanges();
            // Find what is probably meant to be the surface material. With the
            // default settings this should be -1, but if someone configured a
            // default underwater material, try not to change that
            int waterLevel = (Integer) spinnerWaterLevel.getValue();
            int surfaceLevel = defaultTerrainRanges.headMap(waterLevel + 3).lastKey();
            defaultTerrainRanges.put(surfaceLevel, (Terrain) comboBoxSurfaceMaterial.getSelectedItem());
        }
    }//GEN-LAST:event_comboBoxSurfaceMaterialActionPerformed

    private void buttonResetActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonResetActionPerformed
        if (JOptionPane.showConfirmDialog(this, "Are you sure you want to reset all default world settings,\nincluding the border, terrain and layer settings, to the defaults?", "Confirm Reset", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            final Configuration config = Configuration.getInstance();
            spinnerWidth.setValue(640);
            spinnerHeight.setValue(640);
            final Platform defaultPlatform = config.getDefaultPlatform();
            comboBoxPlatform.setSelectedItem(defaultPlatform);
            radioButtonHilly.setSelected(true);
            spinnerGroundLevel.setValue(58);
            spinnerWaterLevel.setValue(DEFAULT_WATER_LEVEL);
            checkBoxLava.setSelected(false);
            checkBoxBeaches.setSelected(true);
            comboBoxSurfaceMaterial.setSelectedItem(GRASS);
            config.setDefaultTerrainAndLayerSettings(new World2(defaultPlatform, World2.DEFAULT_OCEAN_SEED, TileFactoryFactory.createNoiseTileFactory(new Random().nextLong(), GRASS, defaultPlatform.minZ, defaultPlatform.standardMaxHeight, 58, DEFAULT_WATER_LEVEL, false, true, 20, 1.0), defaultPlatform.standardMaxHeight).getDimension(DIM_NORMAL));
        }
    }//GEN-LAST:event_buttonResetActionPerformed

    private void spinnerWidthStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_spinnerWidthStateChanged
        int value = (Integer) spinnerWidth.getValue();
        value = Math.round(value / 128f) * 128;
        if (value < 128) {
            value = 128;
        }
        spinnerWidth.setValue(value);
    }//GEN-LAST:event_spinnerWidthStateChanged

    private void spinnerHeightStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_spinnerHeightStateChanged
        int value = (Integer) spinnerHeight.getValue();
        value = Math.round(value / 128f) * 128;
        if (value < 128) {
            value = 128;
        }
        spinnerHeight.setValue(value);
    }//GEN-LAST:event_spinnerHeightStateChanged

    private void radioButtonHillyActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_radioButtonHillyActionPerformed
        setControlStates();
    }//GEN-LAST:event_radioButtonHillyActionPerformed

    private void radioButtonFlatActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_radioButtonFlatActionPerformed
        setControlStates();
    }//GEN-LAST:event_radioButtonFlatActionPerformed

    private void checkBoxCircularActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_checkBoxCircularActionPerformed
        setControlStates();
    }//GEN-LAST:event_checkBoxCircularActionPerformed

    private void buttonModePresetActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonModePresetActionPerformed
        String editedGeneratorOptions = JOptionPane.showInputDialog(this, "Edit the Superflat mode preset:", generatorOptions);
        if (editedGeneratorOptions != null) {
            generatorOptions = editedGeneratorOptions;
        }
    }//GEN-LAST:event_buttonModePresetActionPerformed

    private void comboBoxWorldTypeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_comboBoxWorldTypeActionPerformed
        setControlStates();
    }//GEN-LAST:event_comboBoxWorldTypeActionPerformed

    private void comboBoxModeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_comboBoxModeActionPerformed
        if (comboBoxMode.getSelectedItem() == GameType.CREATIVE) {
            checkBoxCheats.setSelected(true);
        }
    }//GEN-LAST:event_comboBoxModeActionPerformed

    private void checkBoxPingActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_checkBoxPingActionPerformed
        pingNotSet = false;
    }//GEN-LAST:event_checkBoxPingActionPerformed

    private void checkBoxAutoSaveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_checkBoxAutoSaveActionPerformed
        setControlStates();
    }//GEN-LAST:event_checkBoxAutoSaveActionPerformed

    private void radioButtonUIScaleAutoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_radioButtonUIScaleAutoActionPerformed
        setControlStates();
        sliderUIScale.setValue((int) (GUIUtils.SYSTEM_UI_SCALE_FLOAT * 100));
        updateLabelUIScale();
    }//GEN-LAST:event_radioButtonUIScaleAutoActionPerformed

    private void radioButtonUIScaleManualActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_radioButtonUIScaleManualActionPerformed
        setControlStates();
        updateLabelUIScale();
    }//GEN-LAST:event_radioButtonUIScaleManualActionPerformed

    private void sliderUIScaleStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_sliderUIScaleStateChanged
        updateLabelUIScale();
    }//GEN-LAST:event_sliderUIScaleStateChanged

    private void comboBoxLookAndFeelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_comboBoxLookAndFeelActionPerformed
        // Do nothing
    }//GEN-LAST:event_comboBoxLookAndFeelActionPerformed

    private void checkBoxAutoDeleteBackupsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_checkBoxAutoDeleteBackupsActionPerformed
        setControlStates();
    }//GEN-LAST:event_checkBoxAutoDeleteBackupsActionPerformed

    private void buttonCleanUpBackupsNowActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonCleanUpBackupsNowActionPerformed
        cleanUpBackupsNow();
    }//GEN-LAST:event_buttonCleanUpBackupsNowActionPerformed

    private void comboBoxPlatformActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_comboBoxPlatformActionPerformed
        platformSelected();
    }//GEN-LAST:event_comboBoxPlatformActionPerformed

    private void labelEditExportSettingsLinkMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_labelEditExportSettingsLinkMouseClicked
        editDefaultExportSettings();
    }//GEN-LAST:event_labelEditExportSettingsLinkMouseClicked

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton buttonCancel;
    private javax.swing.JButton buttonCleanUpBackupsNow;
    private javax.swing.ButtonGroup buttonGroup1;
    private javax.swing.ButtonGroup buttonGroup2;
    private javax.swing.ButtonGroup buttonGroup3;
    private javax.swing.ButtonGroup buttonGroup4;
    private javax.swing.JButton buttonModePreset;
    private javax.swing.JButton buttonOK;
    private javax.swing.JButton buttonReset;
    private javax.swing.JCheckBox checkBoxAutoDeleteBackups;
    private javax.swing.JCheckBox checkBoxAutoSave;
    private javax.swing.JCheckBox checkBoxBeaches;
    private javax.swing.JCheckBox checkBoxCheats;
    private javax.swing.JCheckBox checkBoxCheckForUpdates;
    private javax.swing.JCheckBox checkBoxChestOfGoodies;
    private javax.swing.JCheckBox checkBoxCircular;
    private javax.swing.JCheckBox checkBoxContours;
    private javax.swing.JCheckBox checkBoxExtendedBlockIds;
    private javax.swing.JCheckBox checkBoxGrid;
    private javax.swing.JCheckBox checkBoxLava;
    private javax.swing.JCheckBox checkBoxPing;
    private javax.swing.JCheckBox checkBoxStructures;
    private javax.swing.JCheckBox checkBoxUndo;
    private javax.swing.JCheckBox checkBoxViewDistance;
    private javax.swing.JCheckBox checkBoxWalkingDistance;
    private javax.swing.JComboBox<Integer> comboBoxHeight;
    private javax.swing.JComboBox comboBoxLightDirection;
    private javax.swing.JComboBox comboBoxLookAndFeel;
    private javax.swing.JComboBox<GameType> comboBoxMode;
    private javax.swing.JComboBox<Platform> comboBoxPlatform;
    private javax.swing.JComboBox comboBoxSurfaceMaterial;
    private javax.swing.JComboBox<Generator> comboBoxWorldType;
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
    private javax.swing.JLabel jLabel20;
    private javax.swing.JLabel jLabel21;
    private javax.swing.JLabel jLabel22;
    private javax.swing.JLabel jLabel23;
    private javax.swing.JLabel jLabel24;
    private javax.swing.JLabel jLabel25;
    private javax.swing.JLabel jLabel26;
    private javax.swing.JLabel jLabel27;
    private javax.swing.JLabel jLabel28;
    private javax.swing.JLabel jLabel29;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel30;
    private javax.swing.JLabel jLabel31;
    private javax.swing.JLabel jLabel32;
    private javax.swing.JLabel jLabel33;
    private javax.swing.JLabel jLabel34;
    private javax.swing.JLabel jLabel35;
    private javax.swing.JLabel jLabel36;
    private javax.swing.JLabel jLabel37;
    private javax.swing.JLabel jLabel38;
    private javax.swing.JLabel jLabel39;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel40;
    private javax.swing.JLabel jLabel41;
    private javax.swing.JLabel jLabel42;
    private javax.swing.JLabel jLabel43;
    private javax.swing.JLabel jLabel44;
    private javax.swing.JLabel jLabel45;
    private javax.swing.JLabel jLabel46;
    private javax.swing.JLabel jLabel47;
    private javax.swing.JLabel jLabel48;
    private javax.swing.JLabel jLabel49;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel50;
    private javax.swing.JLabel jLabel51;
    private javax.swing.JLabel jLabel52;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JSeparator jSeparator2;
    private javax.swing.JSeparator jSeparator3;
    private javax.swing.JSeparator jSeparator5;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JLabel labelEditExportSettingsLink;
    private javax.swing.JLabel labelTerrainAndLayerSettings;
    private javax.swing.JLabel labelUIScale;
    private javax.swing.JRadioButton radioButtonAccelDefault;
    private javax.swing.JRadioButton radioButtonAccelDirect3D;
    private javax.swing.JRadioButton radioButtonAccelOpenGL;
    private javax.swing.JRadioButton radioButtonAccelQuartz;
    private javax.swing.JRadioButton radioButtonAccelUnaccelerated;
    private javax.swing.JRadioButton radioButtonAccelXRender;
    private javax.swing.JRadioButton radioButtonFlat;
    private javax.swing.JRadioButton radioButtonHilly;
    private javax.swing.JRadioButton radioButtonOverlayOptimiseOnLoad;
    private javax.swing.JRadioButton radioButtonOverlayScaleOnLoad;
    private javax.swing.JRadioButton radioButtonOverlayScaleOnPaint;
    private javax.swing.JRadioButton radioButtonUIScaleAuto;
    private javax.swing.JRadioButton radioButtonUIScaleManual;
    private javax.swing.JSlider sliderUIScale;
    private javax.swing.JSpinner spinnerAutoSaveGuardTime;
    private javax.swing.JSpinner spinnerAutoSaveInterval;
    private javax.swing.JSpinner spinnerBrushSize;
    private javax.swing.JSpinner spinnerContours;
    private javax.swing.JSpinner spinnerFreeSpaceForMaps;
    private javax.swing.JSpinner spinnerGrid;
    private javax.swing.JSpinner spinnerGroundLevel;
    private javax.swing.JSpinner spinnerHeight;
    private javax.swing.JSpinner spinnerRange;
    private javax.swing.JSpinner spinnerScale;
    private javax.swing.JSpinner spinnerUndoLevels;
    private javax.swing.JSpinner spinnerWaterLevel;
    private javax.swing.JSpinner spinnerWidth;
    private javax.swing.JSpinner spinnerWorldBackups;
    // End of variables declaration//GEN-END:variables

    private final ColourScheme colourScheme;
    private boolean pingNotSet;
    private int previousMaxHeight;
    private String generatorOptions; // TODOMC118 this needs to become a SuperflatPreset
    private Dimension defaultTerrainAndLayerSettings;
    private ExportSettings defaultExportSettings;
    
    private static final long serialVersionUID = 1L;
}