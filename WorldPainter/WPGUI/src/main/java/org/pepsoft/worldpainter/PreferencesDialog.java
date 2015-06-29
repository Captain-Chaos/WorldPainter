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

import java.util.SortedMap;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import org.pepsoft.worldpainter.themes.TerrainListCellRenderer;
import java.io.IOException;
import java.util.List;
import java.util.Random;
import javax.swing.*;
import static org.pepsoft.worldpainter.Terrain.*;
import static org.pepsoft.minecraft.Constants.*;
import static org.pepsoft.worldpainter.Constants.*;
import org.pepsoft.worldpainter.TileRenderer.LightOrigin;
import org.pepsoft.worldpainter.themes.SimpleTheme;

/**
 *
 * @author pepijn
 */
public class PreferencesDialog extends javax.swing.JDialog {
    /** Creates new form PreferencesDialog */
    public PreferencesDialog(java.awt.Frame parent, ColourScheme colourScheme) {
        super(parent, true);
        this.colourScheme = colourScheme;
        
        initComponents();
        
        comboBoxSurfaceMaterial.setModel(new DefaultComboBoxModel(Terrain.PICK_LIST));
        comboBoxSurfaceMaterial.setRenderer(new TerrainListCellRenderer(colourScheme));
        if (! JAVA_7) {
            comboBoxLookAndFeel.setModel(new DefaultComboBoxModel(new Object[] {"System", "Metal", "Nimbus", "Dark Metal"}));
        }
        
        List<AccelerationType> accelTypes = AccelerationType.getForThisOS();
        radioButtonAccelDefault.setEnabled(accelTypes.contains(AccelerationType.DEFAULT));
        radioButtonAccelDirect3D.setEnabled(accelTypes.contains(AccelerationType.DIRECT3D));
        radioButtonAccelOpenGL.setEnabled(accelTypes.contains(AccelerationType.OPENGL));
        radioButtonAccelQuartz.setEnabled(accelTypes.contains(AccelerationType.QUARTZ));
        radioButtonAccelUnaccelerated.setEnabled(accelTypes.contains(AccelerationType.UNACCELERATED));
        radioButtonAccelXRender.setEnabled(accelTypes.contains(AccelerationType.XRENDER));
        
        loadSettings();
        
        ActionMap actionMap = rootPane.getActionMap();
        actionMap.put("cancel", new AbstractAction("cancel") {
            @Override
            public void actionPerformed(ActionEvent e) {
                cancel();
            }
            
            private static final long serialVersionUID = 1L;
        });

        InputMap inputMap = rootPane.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "cancel");

        rootPane.setDefaultButton(buttonOK);
        pack();
        setLocationRelativeTo(parent);
    }
    
    public void ok() {
        saveSettings();
        cancelled = false;
        dispose();
    }
    
    public void cancel() {
        dispose();
    }
    
    public boolean isCancelled() {
        return cancelled;
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
        comboBoxHeight.setSelectedItem(Integer.toString(config.getDefaultMaxHeight()));
        if (config.isHilly()) {
            radioButtonHilly.setSelected(true);
        } else {
            radioButtonFlat.setSelected(true);
            spinnerRange.setEnabled(false);
            spinnerScale.setEnabled(false);
        }
        spinnerRange.setValue((int) (config.getDefaultRange() + 0.5f));
        spinnerScale.setValue((int) (config.getDefaultScale() * 100 + 0.5f));
        spinnerGroundLevel.setValue(config.getLevel());
        spinnerWaterLevel.setValue(config.getWaterLevel());
        checkBoxLava.setSelected(config.isLava());
        checkBoxBeaches.setSelected(config.isBeaches());
        comboBoxSurfaceMaterial.setSelectedItem(config.getSurface());
        spinnerWorldBackups.setValue(config.getWorldFileBackups());
        checkBoxExtendedBlockIds.setSelected(config.isDefaultExtendedBlockIds());
        
        // Export settings
        checkBoxChestOfGoodies.setSelected(config.isDefaultCreateGoodiesChest());
        comboBoxWorldType.setSelectedIndex(config.getDefaultGenerator().ordinal());
        generatorOptions = config.getDefaultGeneratorOptions();
        checkBoxStructures.setSelected(config.isDefaultMapFeatures());
        comboBoxMode.setSelectedIndex(config.getDefaultGameType());
        checkBoxCheats.setSelected(config.isDefaultAllowCheats());

        previousExp = (int) Math.round(Math.log(config.getDefaultMaxHeight()) / Math.log(2.0));
        
        comboBoxLookAndFeel.setSelectedIndex(config.getLookAndFeel() != null ? config.getLookAndFeel().ordinal() : 0);
        
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
        config.setDefaultMaxHeight(Integer.parseInt((String) comboBoxHeight.getSelectedItem()));
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
        config.setDefaultGenerator(Generator.values()[comboBoxWorldType.getSelectedIndex()]);
        config.setDefaultGeneratorOptions(generatorOptions);
        config.setDefaultMapFeatures(checkBoxStructures.isSelected());
        config.setDefaultGameType(comboBoxMode.getSelectedIndex());
        config.setDefaultAllowCheats(checkBoxCheats.isSelected());
        
        config.setLookAndFeel(Configuration.LookAndFeel.values()[comboBoxLookAndFeel.getSelectedIndex()]);
        
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
        buttonModePreset.setEnabled(comboBoxWorldType.getSelectedIndex() == 1);
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
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        buttonGroup1 = new javax.swing.ButtonGroup();
        jRadioButton3 = new javax.swing.JRadioButton();
        buttonGroup2 = new javax.swing.ButtonGroup();
        buttonGroup3 = new javax.swing.ButtonGroup();
        jTabbedPane1 = new javax.swing.JTabbedPane();
        jPanel1 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jSeparator1 = new javax.swing.JSeparator();
        checkBoxPing = new javax.swing.JCheckBox();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        checkBoxCheckForUpdates = new javax.swing.JCheckBox();
        jLabel6 = new javax.swing.JLabel();
        jSeparator2 = new javax.swing.JSeparator();
        checkBoxGrid = new javax.swing.JCheckBox();
        jLabel7 = new javax.swing.JLabel();
        spinnerGrid = new javax.swing.JSpinner();
        jLabel8 = new javax.swing.JLabel();
        checkBoxContours = new javax.swing.JCheckBox();
        spinnerContours = new javax.swing.JSpinner();
        checkBoxViewDistance = new javax.swing.JCheckBox();
        checkBoxWalkingDistance = new javax.swing.JCheckBox();
        jLabel9 = new javax.swing.JLabel();
        jSeparator3 = new javax.swing.JSeparator();
        jLabel10 = new javax.swing.JLabel();
        spinnerWidth = new javax.swing.JSpinner();
        jLabel11 = new javax.swing.JLabel();
        spinnerHeight = new javax.swing.JSpinner();
        jLabel12 = new javax.swing.JLabel();
        comboBoxHeight = new javax.swing.JComboBox();
        jLabel13 = new javax.swing.JLabel();
        radioButtonHilly = new javax.swing.JRadioButton();
        radioButtonFlat = new javax.swing.JRadioButton();
        jLabel14 = new javax.swing.JLabel();
        spinnerGroundLevel = new javax.swing.JSpinner();
        jLabel15 = new javax.swing.JLabel();
        spinnerWaterLevel = new javax.swing.JSpinner();
        checkBoxLava = new javax.swing.JCheckBox();
        checkBoxBeaches = new javax.swing.JCheckBox();
        jLabel16 = new javax.swing.JLabel();
        comboBoxSurfaceMaterial = new javax.swing.JComboBox();
        labelTerrainAndLayerSettings = new javax.swing.JLabel();
        jLabel19 = new javax.swing.JLabel();
        jLabel18 = new javax.swing.JLabel();
        buttonReset = new javax.swing.JButton();
        jSeparator4 = new javax.swing.JSeparator();
        jLabel20 = new javax.swing.JLabel();
        spinnerWorldBackups = new javax.swing.JSpinner();
        jLabel21 = new javax.swing.JLabel();
        jLabel22 = new javax.swing.JLabel();
        comboBoxLightDirection = new javax.swing.JComboBox();
        jLabel23 = new javax.swing.JLabel();
        spinnerRange = new javax.swing.JSpinner();
        jLabel24 = new javax.swing.JLabel();
        spinnerScale = new javax.swing.JSpinner();
        jLabel25 = new javax.swing.JLabel();
        jLabel26 = new javax.swing.JLabel();
        spinnerBrushSize = new javax.swing.JSpinner();
        jLabel27 = new javax.swing.JLabel();
        checkBoxCircular = new javax.swing.JCheckBox();
        checkBoxExtendedBlockIds = new javax.swing.JCheckBox();
        jLabel17 = new javax.swing.JLabel();
        jSeparator5 = new javax.swing.JSeparator();
        checkBoxChestOfGoodies = new javax.swing.JCheckBox();
        jLabel28 = new javax.swing.JLabel();
        comboBoxWorldType = new javax.swing.JComboBox();
        buttonModePreset = new javax.swing.JButton();
        checkBoxStructures = new javax.swing.JCheckBox();
        jLabel29 = new javax.swing.JLabel();
        comboBoxMode = new javax.swing.JComboBox();
        checkBoxCheats = new javax.swing.JCheckBox();
        jLabel30 = new javax.swing.JLabel();
        comboBoxLookAndFeel = new javax.swing.JComboBox();
        jLabel32 = new javax.swing.JLabel();
        jPanel2 = new javax.swing.JPanel();
        checkBoxUndo = new javax.swing.JCheckBox();
        jLabel5 = new javax.swing.JLabel();
        spinnerUndoLevels = new javax.swing.JSpinner();
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

        jRadioButton3.setText("jRadioButton3");

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Preferences");

        jLabel1.setText("Configure your preferences and default settings on this screen:");

        jLabel2.setFont(jLabel2.getFont().deriveFont((jLabel2.getFont().getStyle() | java.awt.Font.ITALIC)));
        jLabel2.setText("General ");

        checkBoxPing.setSelected(true);
        checkBoxPing.setText("Send usage information to the developer");
        checkBoxPing.addActionListener(this::checkBoxPingActionPerformed);

        jLabel3.setFont(jLabel3.getFont().deriveFont((jLabel3.getFont().getStyle() | java.awt.Font.ITALIC)));
        jLabel3.setText("Note that the information does not include personally identifiable ");

        jLabel4.setFont(jLabel4.getFont().deriveFont((jLabel4.getFont().getStyle() | java.awt.Font.ITALIC)));
        jLabel4.setText("information, and will never be sold or given to third parties. ");

        checkBoxCheckForUpdates.setSelected(true);
        checkBoxCheckForUpdates.setText("Check for updates on startup");

        jLabel6.setFont(jLabel6.getFont().deriveFont((jLabel6.getFont().getStyle() | java.awt.Font.ITALIC)));
        jLabel6.setText("Default view settings ");

        checkBoxGrid.setText("Grid enabled");

        jLabel7.setLabelFor(spinnerGrid);
        jLabel7.setText("Grid size:");

        spinnerGrid.setModel(new javax.swing.SpinnerNumberModel(128, 2, 999, 1));

        jLabel8.setText("Separation:");

        checkBoxContours.setSelected(true);
        checkBoxContours.setText("Contour lines enabled");

        spinnerContours.setModel(new javax.swing.SpinnerNumberModel(10, 2, 999, 1));

        checkBoxViewDistance.setText("View distance enabled");

        checkBoxWalkingDistance.setText("Walking distance enabled");

        jLabel9.setFont(jLabel9.getFont().deriveFont((jLabel9.getFont().getStyle() | java.awt.Font.ITALIC)));
        jLabel9.setText("Default world settings ");

        jLabel10.setLabelFor(spinnerWidth);
        jLabel10.setText("Dimensions:");

        spinnerWidth.setModel(new javax.swing.SpinnerNumberModel(Integer.valueOf(640), Integer.valueOf(128), null, Integer.valueOf(128)));
        spinnerWidth.addChangeListener(this::spinnerWidthStateChanged);

        jLabel11.setText("x");

        spinnerHeight.setModel(new javax.swing.SpinnerNumberModel(Integer.valueOf(640), Integer.valueOf(128), null, Integer.valueOf(128)));
        spinnerHeight.addChangeListener(this::spinnerHeightStateChanged);

        jLabel12.setLabelFor(comboBoxHeight);
        jLabel12.setText("Height:");

        comboBoxHeight.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "32", "64", "128", "256", "512", "1024", "2048" }));
        comboBoxHeight.setSelectedIndex(3);
        comboBoxHeight.addActionListener(this::comboBoxHeightActionPerformed);

        jLabel13.setText("Topography:");

        buttonGroup1.add(radioButtonHilly);
        radioButtonHilly.setSelected(true);
        radioButtonHilly.setText("Hilly");
        radioButtonHilly.addActionListener(this::radioButtonHillyActionPerformed);

        buttonGroup1.add(radioButtonFlat);
        radioButtonFlat.setText("Flat");
        radioButtonFlat.addActionListener(this::radioButtonFlatActionPerformed);

        jLabel14.setLabelFor(spinnerGroundLevel);
        jLabel14.setText("Level:");

        spinnerGroundLevel.setModel(new javax.swing.SpinnerNumberModel(58, 1, 255, 1));

        jLabel15.setText("Water level:");

        spinnerWaterLevel.setModel(new javax.swing.SpinnerNumberModel(62, 0, 255, 1));
        spinnerWaterLevel.addChangeListener(this::spinnerWaterLevelStateChanged);

        checkBoxLava.setText("Lava instead of water");

        checkBoxBeaches.setSelected(true);
        checkBoxBeaches.setText("Beaches");
        checkBoxBeaches.addActionListener(this::checkBoxBeachesActionPerformed);

        jLabel16.setText("Surface material:");

        comboBoxSurfaceMaterial.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        comboBoxSurfaceMaterial.addActionListener(this::comboBoxSurfaceMaterialActionPerformed);

        labelTerrainAndLayerSettings.setForeground(java.awt.Color.blue);
        labelTerrainAndLayerSettings.setText("<html><u>Configure default border, terrain and layer settings</u></html>");
        labelTerrainAndLayerSettings.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        labelTerrainAndLayerSettings.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                labelTerrainAndLayerSettingsMouseClicked(evt);
            }
        });

        jLabel19.setText("blocks");

        jLabel18.setFont(jLabel18.getFont().deriveFont((jLabel18.getFont().getStyle() | java.awt.Font.ITALIC)));
        jLabel18.setText("(Note that changes to these settings will only take effect for the next world you load or create.) ");

        buttonReset.setText("Reset...");
        buttonReset.addActionListener(this::buttonResetActionPerformed);

        jSeparator4.setOrientation(javax.swing.SwingConstants.VERTICAL);

        jLabel20.setText("No. of backups of .world files to keep:");

        spinnerWorldBackups.setModel(new javax.swing.SpinnerNumberModel(Integer.valueOf(3), Integer.valueOf(0), null, Integer.valueOf(1)));

        jLabel21.setText(" ");

        jLabel22.setText("Light direction:");

        comboBoxLightDirection.setModel(new DefaultComboBoxModel(LightOrigin.values()));

        jLabel23.setText("(height:");

        spinnerRange.setModel(new javax.swing.SpinnerNumberModel(20, 1, 255, 1));

        jLabel24.setText("scale:");

        spinnerScale.setModel(new javax.swing.SpinnerNumberModel(100, 1, 999, 1));

        jLabel25.setText("%)");

        jLabel26.setText("Maximum brush size:");

        spinnerBrushSize.setModel(new javax.swing.SpinnerNumberModel(Integer.valueOf(300), Integer.valueOf(100), null, Integer.valueOf(10)));

        jLabel27.setFont(jLabel27.getFont().deriveFont((jLabel27.getFont().getStyle() | java.awt.Font.ITALIC)));
        jLabel27.setText("Warning: large brush sizes could slow your computer to a crawl! ");

        checkBoxCircular.setText("Circular world");
        checkBoxCircular.addActionListener(this::checkBoxCircularActionPerformed);

        checkBoxExtendedBlockIds.setText("Extended block ID's");

        jLabel17.setFont(jLabel17.getFont().deriveFont((jLabel17.getFont().getStyle() | java.awt.Font.ITALIC)));
        jLabel17.setText("Default export settings");

        checkBoxChestOfGoodies.setText("Include chest of goodies");

        jLabel28.setText("World type:");

        comboBoxWorldType.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Default", "Superflat", "Large Biomes" }));
        comboBoxWorldType.addActionListener(this::comboBoxWorldTypeActionPerformed);

        buttonModePreset.setText("...");
        buttonModePreset.addActionListener(this::buttonModePresetActionPerformed);

        checkBoxStructures.setText("Structures");

        jLabel29.setText("Mode:");

        comboBoxMode.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Survival", "Creative", "Adventure" }));
        comboBoxMode.addActionListener(this::comboBoxModeActionPerformed);

        checkBoxCheats.setText("Allow Cheats");

        jLabel30.setText("Visual theme:");

        comboBoxLookAndFeel.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "System", "Metal", "Nimbus", "Dark Metal", "Dark Nimbus" }));

        jLabel32.setText("<html><em>Effective after restart  </em></html>");

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel1)
                            .addComponent(jLabel18))
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jSeparator3)
                            .addComponent(jSeparator2)
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addComponent(labelTerrainAndLayerSettings, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(buttonReset))
                            .addComponent(jSeparator1)
                            .addComponent(jSeparator5)
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addComponent(checkBoxChestOfGoodies)
                                .addGap(18, 18, 18)
                                .addComponent(jLabel28)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(comboBoxWorldType, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(buttonModePreset)
                                .addGap(18, 18, 18)
                                .addComponent(checkBoxStructures)
                                .addGap(18, 18, 18)
                                .addComponent(jLabel29)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(comboBoxMode, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(checkBoxCheats))
                            .addGroup(jPanel1Layout.createSequentialGroup()
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
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(jPanel1Layout.createSequentialGroup()
                                        .addGap(12, 12, 12)
                                        .addComponent(jLabel7)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(spinnerGrid, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                    .addComponent(checkBoxGrid))
                                .addGap(18, 18, 18)
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(checkBoxContours)
                                    .addGroup(jPanel1Layout.createSequentialGroup()
                                        .addGap(12, 12, 12)
                                        .addComponent(jLabel8)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(spinnerContours, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                                .addGap(18, 18, 18)
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(checkBoxWalkingDistance)
                                    .addComponent(checkBoxViewDistance))
                                .addGap(18, 18, 18)
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(jPanel1Layout.createSequentialGroup()
                                        .addComponent(jLabel26)
                                        .addGap(6, 6, 6)
                                        .addComponent(spinnerBrushSize, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(jLabel21))
                                    .addGroup(jPanel1Layout.createSequentialGroup()
                                        .addComponent(jLabel22)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(comboBoxLightDirection, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                    .addComponent(jLabel27)))
                            .addComponent(jLabel9)
                            .addGroup(jPanel1Layout.createSequentialGroup()
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
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addComponent(jLabel16)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(comboBoxSurfaceMaterial, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(18, 18, 18)
                                .addComponent(checkBoxExtendedBlockIds))
                            .addComponent(jLabel2)
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(checkBoxPing)
                                    .addComponent(checkBoxCheckForUpdates)
                                    .addGroup(jPanel1Layout.createSequentialGroup()
                                        .addGap(12, 12, 12)
                                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                            .addComponent(jLabel4)
                                            .addComponent(jLabel3))))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jSeparator4, javax.swing.GroupLayout.PREFERRED_SIZE, 2, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(jPanel1Layout.createSequentialGroup()
                                        .addComponent(jLabel20)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(spinnerWorldBackups, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                    .addGroup(jPanel1Layout.createSequentialGroup()
                                        .addComponent(jLabel30)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                            .addComponent(jLabel32, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                            .addComponent(comboBoxLookAndFeel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))))
                            .addComponent(jLabel17))
                        .addContainerGap())))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel18)
                .addGap(18, 18, 18)
                .addComponent(jLabel6)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSeparator2, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(checkBoxGrid)
                    .addComponent(checkBoxContours)
                    .addComponent(checkBoxViewDistance)
                    .addComponent(jLabel22)
                    .addComponent(comboBoxLightDirection, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
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
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
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
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel14)
                    .addComponent(spinnerGroundLevel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel15)
                    .addComponent(spinnerWaterLevel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(checkBoxLava)
                    .addComponent(checkBoxBeaches)
                    .addComponent(checkBoxCircular))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel16)
                    .addComponent(comboBoxSurfaceMaterial, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(checkBoxExtendedBlockIds))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(labelTerrainAndLayerSettings, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(buttonReset))
                .addGap(18, 18, 18)
                .addComponent(jLabel17)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSeparator5, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(checkBoxChestOfGoodies)
                    .addComponent(jLabel28)
                    .addComponent(comboBoxWorldType, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(buttonModePreset)
                    .addComponent(checkBoxStructures)
                    .addComponent(jLabel29)
                    .addComponent(comboBoxMode, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(checkBoxCheats))
                .addGap(18, 18, 18)
                .addComponent(jLabel2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                        .addGroup(jPanel1Layout.createSequentialGroup()
                            .addComponent(checkBoxPing)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(jLabel3)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(jLabel4)
                            .addGap(18, 18, 18)
                            .addComponent(checkBoxCheckForUpdates))
                        .addComponent(jSeparator4))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel20)
                            .addComponent(spinnerWorldBackups, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(18, 18, 18)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel30)
                            .addComponent(comboBoxLookAndFeel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel32, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab("General", jPanel1);

        checkBoxUndo.setSelected(true);
        checkBoxUndo.setText("Enable undo");
        checkBoxUndo.addActionListener(this::checkBoxUndoActionPerformed);

        jLabel5.setLabelFor(spinnerUndoLevels);
        jLabel5.setText("Undo levels:");

        spinnerUndoLevels.setModel(new javax.swing.SpinnerNumberModel(25, 1, 999, 1));

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
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(checkBoxUndo)
                            .addGroup(jPanel2Layout.createSequentialGroup()
                                .addGap(12, 12, 12)
                                .addComponent(jLabel5)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(spinnerUndoLevels, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addComponent(jPanel4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(checkBoxUndo)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel5)
                    .addComponent(spinnerUndoLevels, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab("Performance", jPanel2);

        buttonCancel.setText("Cancel");
        buttonCancel.addActionListener(this::buttonCancelActionPerformed);

        buttonOK.setText("OK");
        buttonOK.addActionListener(this::buttonOKActionPerformed);

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
                .addGap(18, 18, 18)
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

    private void checkBoxPingActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_checkBoxPingActionPerformed
        pingNotSet = false;
    }//GEN-LAST:event_checkBoxPingActionPerformed

    private void comboBoxHeightActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_comboBoxHeightActionPerformed
        int maxHeight = Integer.parseInt((String) comboBoxHeight.getSelectedItem());
        int exp = (int) (Math.log(maxHeight) / Math.log(2));
        if (exp != previousExp) {
            previousExp = exp;
            
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
            spinnerWidth.setValue(640);
            spinnerHeight.setValue(640);
            comboBoxHeight.setSelectedIndex(3);
            radioButtonHilly.setSelected(true);
            spinnerGroundLevel.setValue(58);
            spinnerWaterLevel.setValue(62);
            checkBoxLava.setSelected(false);
            checkBoxBeaches.setSelected(true);
            comboBoxSurfaceMaterial.setSelectedItem(GRASS);
            Configuration.getInstance().setDefaultTerrainAndLayerSettings(new Dimension(World2.DEFAULT_OCEAN_SEED, TileFactoryFactory.createNoiseTileFactory(new Random().nextLong(), GRASS, DEFAULT_MAX_HEIGHT_2, 58, 62, false, true, 20, 1.0), DIM_NORMAL, DEFAULT_MAX_HEIGHT_2));
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
        if (comboBoxMode.getSelectedIndex() > 0) {
            checkBoxCheats.setSelected(true);
        }
    }//GEN-LAST:event_comboBoxModeActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton buttonCancel;
    private javax.swing.ButtonGroup buttonGroup1;
    private javax.swing.ButtonGroup buttonGroup2;
    private javax.swing.ButtonGroup buttonGroup3;
    private javax.swing.JButton buttonModePreset;
    private javax.swing.JButton buttonOK;
    private javax.swing.JButton buttonReset;
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
    private javax.swing.JComboBox comboBoxHeight;
    private javax.swing.JComboBox comboBoxLightDirection;
    private javax.swing.JComboBox comboBoxLookAndFeel;
    private javax.swing.JComboBox comboBoxMode;
    private javax.swing.JComboBox comboBoxSurfaceMaterial;
    private javax.swing.JComboBox comboBoxWorldType;
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
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JRadioButton jRadioButton3;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JSeparator jSeparator2;
    private javax.swing.JSeparator jSeparator3;
    private javax.swing.JSeparator jSeparator4;
    private javax.swing.JSeparator jSeparator5;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JLabel labelTerrainAndLayerSettings;
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
    private javax.swing.JSpinner spinnerBrushSize;
    private javax.swing.JSpinner spinnerContours;
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
    private boolean pingNotSet, cancelled = true;
    private int previousExp;
    private String generatorOptions;
    
    private static final long serialVersionUID = 1L;
}