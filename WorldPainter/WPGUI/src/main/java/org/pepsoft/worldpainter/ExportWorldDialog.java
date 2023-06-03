/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/*
 * ExportWorldDialog.java
 *
 * Created on Mar 29, 2011, 5:09:50 PM
 */

package org.pepsoft.worldpainter;

import org.pepsoft.util.DesktopUtils;
import org.pepsoft.worldpainter.Dimension.Anchor;
import org.pepsoft.worldpainter.World2.BorderSettings;
import org.pepsoft.worldpainter.biomeschemes.CustomBiomeManager;
import org.pepsoft.worldpainter.exporting.WorldExportSettings;
import org.pepsoft.worldpainter.layers.CustomLayer;
import org.pepsoft.worldpainter.layers.Layer;
import org.pepsoft.worldpainter.layers.Populate;
import org.pepsoft.worldpainter.plugins.PlatformManager;
import org.pepsoft.worldpainter.util.EnumListCellRenderer;
import org.pepsoft.worldpainter.util.FileFilter;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.function.Function.identity;
import static javax.swing.JOptionPane.YES_NO_OPTION;
import static javax.swing.JOptionPane.YES_OPTION;
import static org.pepsoft.minecraft.Constants.DIFFICULTY_HARD;
import static org.pepsoft.minecraft.Constants.DIFFICULTY_PEACEFUL;
import static org.pepsoft.minecraft.datapack.DataPack.isDataPackFile;
import static org.pepsoft.util.swing.MessageUtils.beepAndShowError;
import static org.pepsoft.worldpainter.Constants.DIM_NORMAL;
import static org.pepsoft.worldpainter.DefaultPlugin.JAVA_MCREGION;
import static org.pepsoft.worldpainter.Dimension.Anchor.NORMAL_DETAIL;
import static org.pepsoft.worldpainter.ExceptionHandler.doWithoutExceptionReporting;
import static org.pepsoft.worldpainter.GameType.*;
import static org.pepsoft.worldpainter.Platform.Capability.*;
import static org.pepsoft.worldpainter.exporting.WorldExportSettings.EXPORT_EVERYTHING;
import static org.pepsoft.worldpainter.util.BackupUtils.cleanUpBackups;
import static org.pepsoft.worldpainter.util.FileUtils.selectFileForOpen;
import static org.pepsoft.worldpainter.util.MaterialUtils.gatherBlocksWithoutIds;

/**
 *
 * @author pepijn
 */
@SuppressWarnings({"unused", "FieldCanBeLocal", "rawtypes", "Convert2Lambda", "Anonymous2MethodRef", "ConstantConditions"}) // Managed by NetBeans
public class ExportWorldDialog extends WPDialogWithPaintSelection {
    /** Creates new form ExportWorldDialog */
    public ExportWorldDialog(Window parent, World2 world, ColourScheme colourScheme, CustomBiomeManager customBiomeManager, Set<Layer> hiddenLayers, boolean contourLines, int contourSeparation, TileRenderer.LightOrigin lightOrigin, WorldPainter view) {
        super(parent);
        this.world = world;
        final Dimension dim0 = world.getDimension(NORMAL_DETAIL);
        this.colourScheme = colourScheme;
        this.hiddenLayers = hiddenLayers;
        this.contourLines = contourLines;
        this.contourSeparation = contourSeparation;
        this.lightOrigin = lightOrigin;
        this.customBiomeManager = customBiomeManager;
        this.view = view;
        initComponents();

        final Configuration config = Configuration.getInstance();
        if (config.isEasyMode()) {
            checkBoxMapFeatures.setVisible(false);
            jLabel1.setVisible(false);
            labelPlatform.setVisible(false);
        }

        supportedPlatforms.addAll(PlatformManager.getInstance().getAllPlatforms());
        final Platform platform = world.getPlatform();
        if (supportedPlatforms.contains(platform)) {
            labelPlatformWarning.setVisible(false);
            if (config.getExportDirectory(platform) != null) {
                fieldDirectory.setText(config.getExportDirectory(platform).getAbsolutePath());
            } else {
                File exportDir = PlatformManager.getInstance().getDefaultExportDir(platform);
                if (exportDir != null) {
                    fieldDirectory.setText(exportDir.getAbsolutePath());
                } else {
                    fieldDirectory.setText(DesktopUtils.getDocumentsFolder().getAbsolutePath());
                }
            }
        } else {
            fieldDirectory.setText(null);
        }
        fieldName.setText(world.getName());

        createDimensionPropertiesEditors();
        checkBoxGoodies.setSelected(world.isCreateGoodiesChest());
        labelPlatform.setText("<html><u>" + platform.displayName + "</u></html>");
        labelPlatform.setToolTipText("Click to change the map format");
        comboBoxGameType.setModel(new DefaultComboBoxModel<>(platform.supportedGameTypes.toArray(new GameType[platform.supportedGameTypes.size()])));
        comboBoxGameType.setSelectedItem(world.getGameType());
        comboBoxGameType.setEnabled(comboBoxGameType.getItemCount() > 1);
        comboBoxGameType.setRenderer(new EnumListCellRenderer());
        checkBoxAllowCheats.setSelected(world.isAllowCheats());
        if (world.getDataPacks() != null) {
            for (File dataPackFile: world.getDataPacks()) {
                dataPacksListModel.addElement(dataPackFile);
            }
        }
        listDataPacks.setModel(dataPacksListModel);
        listDataPacks.setEnabled(platform.capabilities.contains(DATA_PACKS));
        checkBoxMapFeatures.setSelected(world.isMapFeatures());
        comboBoxDifficulty.setSelectedIndex(world.getDifficulty());

        DocumentListener documentListener = new DocumentListener() {
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
        };
        fieldDirectory.getDocument().addDocumentListener(documentListener);
        fieldName.getDocument().addDocumentListener(documentListener);

        // Minecraft world border
        World2.BorderSettings borderSettings = world.getBorderSettings();
        spinnerMcBorderCentreX.setValue(borderSettings.getCentreX());
        spinnerMcBorderCentreY.setValue(borderSettings.getCentreY());
        spinnerMcBorderSize.setValue(borderSettings.getSize());
//        spinnerMcBorderBuffer.setValue(borderSettings.getSafeZone());
//        spinnerMcBorderDamage.setValue(borderSettings.getDamagePerBlock());
//        spinnerMcBorderWarningTime.setValue(borderSettings.getWarningTime());
//        spinnerMcBorderWarningDistance.setValue(borderSettings.getWarningBlocks());

        disableDisabledLayersWarning = true;
        dims:
        for (Dimension dim: world.getDimensions()) {
            for (CustomLayer customLayer: dim.getCustomLayers()) {
                if (! customLayer.isExport()) {
                    disableDisabledLayersWarning = false;
                    break dims;
                }
            }
        }

        rootPane.setDefaultButton(buttonExport);

        setControlStates();

        scaleToUI();
        pack();
        setLocationRelativeTo(parent);
    }

    private void createDimensionPropertiesEditors() {
        final SortedMap<Anchor, Dimension> dimensions = world.getDimensions().stream().collect(Collectors.toMap(
                Dimension::getAnchor,
                identity(),
                (u,v) -> { throw new IllegalStateException(String.format("Duplicate key %s", u)); },
                TreeMap::new));
        for (Dimension dimension: dimensions.values()) {
            final DimensionPropertiesEditor editor = new DimensionPropertiesEditor();
            editor.setColourScheme(colourScheme);
            editor.setDimension(dimension);
            editor.setMode(DimensionPropertiesEditor.Mode.EXPORT);
            jTabbedPane1.addTab(dimension.getName(), editor);
            dimensionPropertiesEditors.put(dimension.getAnchor(), editor);
        }
    }

    /**
     * Check whether a platform is compatible with the loaded world. If not,
     * reports the reason to the user with a popup and returns {@code false},
     * otherwise returns {@code true}.
     *
     * @param platform The platform to check for compatibility.
     * @return {@code true} is the platform is compatible with the loaded world.
     */
    @SuppressWarnings("HtmlRequiredLangAttribute") // Not real HTML
    private boolean checkCompatibility(Platform platform) {
        final Map<String, Set<String>> nameOnlyMaterials = gatherBlocksWithoutIds(world, platform);
        if ((! nameOnlyMaterials.isEmpty()) && (! platform.capabilities.contains(NAME_BASED))) {
            final StringBuilder sb = new StringBuilder();
            sb.append("<html>");
            sb.append("<p>The world cannot be exported in format ").append(platform.displayName).append(" because it contains the following incompatible block types:");
            sb.append("<table><tr><th align='left'>Block Type</th><th align='left'>Source</th></tr>");
            nameOnlyMaterials.forEach((name, sources) ->
                    sb.append("<tr><td>").append(name).append("</td><td>").append(String.join(",", sources)).append("</td></tr>"));
            sb.append("</table>");
            beepAndShowError(this, sb.toString(), "Map Format Not Compatible");
            return false;
        }
        final String incompatibilityReason = PlatformManager.getInstance().getPlatformProvider(platform).isCompatible(platform, world);
        if (incompatibilityReason != null) {
            beepAndShowError(this, String.format(/* language=HTML */ "<html>" +
                    "<p>The world cannot be exported in format %s because it is not compatible:</p>" +
                    "<p>%s</p>" +
                    "</html>", platform.displayName, incompatibilityReason), "Map Format Not Compatible");
            return false;
        }
        return true;
    }

    protected final void export(WorldExportSettings exportSettings) {
        exportSettings = (exportSettings != null)
                ? exportSettings
                : ((world.getExportSettings() != null) ? world.getExportSettings() : EXPORT_EVERYTHING);
        final boolean exportAllDimensions = exportSettings.getDimensionsToExport() == null;
        final Set<Point> selectedTiles = exportAllDimensions ? null : exportSettings.getTilesToExport();
        final int selectedDimension = exportAllDimensions ? DIM_NORMAL : exportSettings.getDimensionsToExport().iterator().next();

        // Check for errors
        if (! new File(fieldDirectory.getText().trim()).isDirectory()) {
            fieldDirectory.requestFocusInWindow();
            beepAndShowError(this, "The selected output directory does not exist or is not a directory.", "Error");
            return;
        }
        if (fieldName.getText().trim().isEmpty()) {
            fieldName.requestFocusInWindow();
            beepAndShowError(this, "You have not specified a name for the map.", "Error");
            return;
        }

        // Check for warnings
        final Platform platform = world.getPlatform();
        final StringBuilder sb = new StringBuilder("<html>Please confirm that you want to export the world<br>notwithstanding the following warnings:<br><ul>");
        boolean showWarning = false;
        for (DimensionPropertiesEditor editor: dimensionPropertiesEditors.values()) {
            final Generator generatorType = editor.getSelectedGeneratorType();
            final Dimension dimension = editor.getDimension();
            if ((editor.isPopulateSelected() || dimension.getAllLayers(true).contains(Populate.INSTANCE)) && (! platform.capabilities.contains(POPULATE))) {
                sb.append("<li>Population not supported for<br>map format " + platform.displayName + "; it will not have an effect");
                showWarning = true;
            } else if (exportAllDimensions || (selectedDimension == dimension.getAnchor().dim)) {
                // The dimension is going to be exported
                if ((generatorType == Generator.FLAT) && (editor.isPopulateSelected() || dimension.getAllLayers(true).contains(Populate.INSTANCE))) {
                    sb.append("<li>The Superflat world type is selected and Populate is in use.<br>Minecraft will <em>not</em> populate generated chunks for Superflat maps.");
                    showWarning = true;
                }
            }
            if ((generatorType != null) && (! platform.supportedGenerators.contains(generatorType))) {
                sb.append("<li>Map format " + platform.displayName + " does not support world type " + generatorType.getDisplayName() + ".<br>The world type will be reset to " + platform.supportedGenerators.get(0).getDisplayName() + ".");
                editor.setSelectedGeneratorType(platform.supportedGenerators.get(0));
                showWarning = true;
            }
        }
        if ((selectedTiles != null) && (selectedDimension == DIM_NORMAL)) {
            boolean spawnInSelection = false;
            Point spawnPoint = world.getSpawnPoint();
            for (Point tile: selectedTiles) {
                if ((spawnPoint.x >= (tile.x << 7)) && (spawnPoint.x < ((tile.x + 1) << 7)) && (spawnPoint.y >= (tile.y << 7)) && (spawnPoint.y < ((tile.y + 1) << 7))) {
                    spawnInSelection = true;
                    break;
                }
            }
            if (! spawnInSelection) {
                sb.append("<li>The spawn point is not inside the selected area.<br>It will temporarily be moved to the middle of the selected area.");
                showWarning = true;
            }
        }
        int disabledLayerCount = 0;
        for (Dimension dimension: world.getDimensions()) {
            for (CustomLayer customLayer: dimension.getCustomLayers()) {
                if (! customLayer.isExport()) {
                    disabledLayerCount++;
                }
            }
        }
        if (disabledLayerCount > 0) {
            if (disabledLayerCount == 1) {
                sb.append("<li>There are disabled custom layers!<br>One layer is not going to be exported.");
            } else {
                sb.append("<li>There are disabled custom layers!<br>" + disabledLayerCount + " layers are not going to be exported.");
            }
            showWarning = showWarning || (! disableDisabledLayersWarning);
        }
        for (int i = 0; i < dataPacksListModel.size(); i++) {
            final File dataPackFile = dataPacksListModel.getElementAt(i);
            if (! dataPackFile.exists()) {
                sb.append("<li>Data pack file " + dataPackFile.getName() + " cannot be found.<br>It will not be installed.");
                showWarning = true;
            } else if (! dataPackFile.isFile()) {
                sb.append("<li>Data pack file " + dataPackFile.getName() + " is not a regular file.<br>It will not be installed.");
                showWarning = true;
            } else if (! dataPackFile.canRead()) {
                sb.append("<li>Data pack file " + dataPackFile.getName() + " is not accessible.<br>It will not be installed.");
                showWarning = true;
            }
        }
        sb.append("</ul>Do you want to continue with the export?</html>");
        if (showWarning) {
            DesktopUtils.beep();
            if (JOptionPane.showConfirmDialog(this, sb.toString(), "Review Warnings", YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) != JOptionPane.YES_OPTION) {
                return;
            }
        }

        final File baseDir = new File(fieldDirectory.getText().trim());
        final String name = fieldName.getText().trim();

        // Make sure the minimum free disk space is met
        try {
            if (! cleanUpBackups(baseDir, null)) {
                return;
            }
        } catch (IOException e) {
            throw new RuntimeException("I/O error while cleaning backups", e);
        }

        if (! saveDimensionSettings()) {
            return;
        }
        if (! checkCompatibility(world.getPlatform())) {
            return;
        }

        world.setCreateGoodiesChest(checkBoxGoodies.isSelected());
        world.setGameType((GameType) comboBoxGameType.getSelectedItem());
        world.setAllowCheats(checkBoxAllowCheats.isSelected());
        world.setMapFeatures(checkBoxMapFeatures.isSelected());
        world.setDifficulty(comboBoxDifficulty.getSelectedIndex());
        world.setDataPacks(dataPacksListModel.isEmpty() ? null : Collections.list(dataPacksListModel.elements()));

        // Minecraft world border
        BorderSettings borderSettings = world.getBorderSettings();
        borderSettings.setCentreX((Integer) spinnerMcBorderCentreX.getValue());
        borderSettings.setCentreY((Integer) spinnerMcBorderCentreY.getValue());
        borderSettings.setSize((Integer) spinnerMcBorderSize.getValue());
//        borderSettings.setSafeZone((Integer) spinnerMcBorderBuffer.getValue());
//        borderSettings.setDamagePerBlock((Float) spinnerMcBorderDamage.getValue());
//        borderSettings.setWarningTime((Integer) spinnerMcBorderWarningTime.getValue());
//        borderSettings.setWarningBlocks((Integer) spinnerMcBorderWarningDistance.getValue());

        fieldDirectory.setEnabled(false);
        fieldName.setEnabled(false);
        buttonSelectDirectory.setEnabled(false);
        buttonExport.setEnabled(false);
        buttonTestExport.setEnabled(false);
        buttonCancel.setEnabled(false);
        for (DimensionPropertiesEditor editor: dimensionPropertiesEditors.values()) {
            editor.setEnabled(false);
        }
        checkBoxGoodies.setEnabled(false);
        comboBoxGameType.setEnabled(false);
        checkBoxAllowCheats.setEnabled(false);
        checkBoxMapFeatures.setEnabled(false);
        comboBoxDifficulty.setEnabled(false);
        listDataPacks.setEnabled(false);
        buttonAddDataPack.setEnabled(false);
        buttonRemoveDataPack.setEnabled(false);

        final Configuration config = Configuration.getInstance();
        config.setExportDirectory(world.getPlatform(), baseDir);

        final ExportProgressDialog dialog = new ExportProgressDialog(this, world, exportSettings, baseDir, name);
        view.setInhibitUpdates(true);
        try {
            dialog.setVisible(true);
        } finally {
            view.setInhibitUpdates(false);
        }
        if (! dialog.isAllowRetry()) {
            ok();
        } else {
            fieldName.setEnabled(true);
            buttonCancel.setEnabled(true);
            for (DimensionPropertiesEditor editor: dimensionPropertiesEditors.values()) {
                editor.setEnabled(true);
            }
            checkBoxGoodies.setEnabled(true);
            comboBoxGameType.setEnabled(true);
            checkBoxMapFeatures.setEnabled(true);
            listDataPacks.setEnabled(platform.capabilities.contains(DATA_PACKS));
            setControlStates();
        }
    }

    private boolean saveDimensionSettings() {
        for (DimensionPropertiesEditor editor: dimensionPropertiesEditors.values()) {
            if (! editor.saveSettings()) {
                jTabbedPane1.setSelectedComponent(editor);
                return false;
            }
        }
        return true;
    }
    
    private void testExport() {
        final TestExportDialog dialog = new TestExportDialog(this, world, colourScheme, customBiomeManager, hiddenLayers, contourLines, contourSeparation, lightOrigin);
        dialog.setVisible(true);
        if (! dialog.isCancelled()) {
            export(world.getExportSettings());
        }
    }

    private void setControlStates() {
        boolean notHardcore = comboBoxGameType.getSelectedItem() != HARDCORE;
        final boolean platformSupported = supportedPlatforms.contains(world.getPlatform());
        checkBoxAllowCheats.setEnabled((world.getPlatform() != JAVA_MCREGION) && notHardcore);
        comboBoxDifficulty.setEnabled(notHardcore);
        fieldDirectory.setEnabled(platformSupported);
        buttonSelectDirectory.setEnabled(platformSupported);
        buttonExport.setEnabled(platformSupported);
        buttonTestExport.setEnabled(platformSupported);
        final boolean dataPacksEnabled = listDataPacks.isEnabled();
        buttonAddDataPack.setEnabled(dataPacksEnabled);
        buttonRemoveDataPack.setEnabled(dataPacksEnabled && (listDataPacks.getSelectedIndex() != -1));
        if (! platformSupported) {
            buttonExport.setToolTipText(labelPlatformWarning.getToolTipText());
            buttonTestExport.setToolTipText(labelPlatformWarning.getToolTipText());
        } else {
            buttonExport.setToolTipText(null);
            buttonTestExport.setToolTipText(null);
        }
    }

    private void selectDir() {
        // Can't use FileUtils.selectFileForOpen() since it doesn't support
        // selecting a directory
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setSelectedFile(new File(fieldDirectory.getText().trim()));
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (doWithoutExceptionReporting(() -> fileChooser.showOpenDialog(this)) == JFileChooser.APPROVE_OPTION) {
            fieldDirectory.setText(fileChooser.getSelectedFile().getAbsolutePath());
        }
    }
    
    private void changePlatform() {
        if (! saveDimensionSettings()) {
            return;
        }
        if (App.getInstance().changeWorldHeight(this)) {
            while (jTabbedPane1.getTabCount() > 1) {
                jTabbedPane1.removeTabAt(jTabbedPane1.getTabCount() - 1);
            }
            createDimensionPropertiesEditors();
            platformChanged();
        }
    }

    private void addDataPack() {
        final File dataPackFile = selectFileForOpen(this, "Select a data pack", null, new FileFilter() {
            @Override
            public String getExtensions() {
                return "*.zip";
            }

            @Override
            public boolean accept(File f) {
                return f.isDirectory() || f.getName().toLowerCase().endsWith(".zip");
            }

            @Override
            public String getDescription() {
                return "Data packs (*.zip)";
            }
        });
        if (dataPackFile != null) {
            if (! isDataPackFile(dataPackFile)) {
                beepAndShowError(this, "The selected file \"" + dataPackFile.getName() + "\" is not a Minecraft data pack.", "Not A Data Pack");
                return;
            } else if (dataPackFile.getName().equalsIgnoreCase("worldpainter.zip")) {
                beepAndShowError(this, "The WorldPainter data pack is managed by WorldPainter.", "Cannot Add WorldPainter Data Pack");
                return;
            } else {
                for (Enumeration<File> e = dataPacksListModel.elements(); e.hasMoreElements(); ) {
                    final File existingDataPack = e.nextElement();
                    if (existingDataPack.getName().equalsIgnoreCase(dataPackFile.getName())) {
                        if (JOptionPane.showConfirmDialog(this, "There is already a data pack selected with the mame \"" + dataPackFile.getName() + "\"\nDo you want to replaced it?", "Data Pack Name Already Present", YES_NO_OPTION) == YES_OPTION) {
                            dataPacksListModel.removeElement(existingDataPack);
                            break;
                        } else {
                            return;
                        }
                    }
                }
            }
            dataPacksListModel.addElement(dataPackFile);
        }
    }

    private void removeSelectedDataPacks() {
        final int[] selectedIndices = listDataPacks.getSelectedIndices();
        for (int i = selectedIndices.length - 1; i >= 0; i--) {
            dataPacksListModel.remove(selectedIndices[i]);
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

        buttonGroup2 = new javax.swing.ButtonGroup();
        buttonCancel = new javax.swing.JButton();
        buttonExport = new javax.swing.JButton();
        jTabbedPane1 = new javax.swing.JTabbedPane();
        jPanel1 = new javax.swing.JPanel();
        checkBoxGoodies = new javax.swing.JCheckBox();
        jLabel1 = new javax.swing.JLabel();
        labelPlatform = new javax.swing.JLabel();
        labelPlatformWarning = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        comboBoxGameType = new javax.swing.JComboBox<>();
        checkBoxAllowCheats = new javax.swing.JCheckBox();
        jLabel6 = new javax.swing.JLabel();
        comboBoxDifficulty = new javax.swing.JComboBox();
        checkBoxMapFeatures = new javax.swing.JCheckBox();
        jLabel7 = new javax.swing.JLabel();
        jSeparator1 = new javax.swing.JSeparator();
        jLabel8 = new javax.swing.JLabel();
        jLabel9 = new javax.swing.JLabel();
        jLabel10 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        fieldDirectory = new javax.swing.JTextField();
        fieldName = new javax.swing.JTextField();
        buttonSelectDirectory = new javax.swing.JButton();
        jLabel11 = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        listDataPacks = new javax.swing.JList<>();
        buttonAddDataPack = new javax.swing.JButton();
        buttonRemoveDataPack = new javax.swing.JButton();
        jLabel12 = new javax.swing.JLabel();
        panelMinecraftWorldBorder = new javax.swing.JPanel();
        jLabel79 = new javax.swing.JLabel();
        spinnerMcBorderCentreX = new javax.swing.JSpinner();
        jLabel80 = new javax.swing.JLabel();
        spinnerMcBorderCentreY = new javax.swing.JSpinner();
        jLabel81 = new javax.swing.JLabel();
        spinnerMcBorderSize = new javax.swing.JSpinner();
        jLabel85 = new javax.swing.JLabel();
        jLabel86 = new javax.swing.JLabel();
        buttonTestExport = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Exporting");

        buttonCancel.setText("Cancel");
        buttonCancel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonCancelActionPerformed(evt);
            }
        });

        buttonExport.setText("Export");
        buttonExport.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonExportActionPerformed(evt);
            }
        });

        jTabbedPane1.setTabPlacement(javax.swing.JTabbedPane.RIGHT);

        checkBoxGoodies.setSelected(true);
        checkBoxGoodies.setText(" ");
        checkBoxGoodies.setToolTipText("Include a chest with tools and resources near spawn for you as the level designer");

        jLabel1.setText("Map format:");

        labelPlatform.setForeground(new java.awt.Color(0, 0, 255));
        labelPlatform.setText("<html><u>[EXPERIMENTAL] Minecraft 1.17</u></html>");
        labelPlatform.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        labelPlatform.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                labelPlatformMouseClicked(evt);
            }
        });

        labelPlatformWarning.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/pepsoft/worldpainter/icons/error.png"))); // NOI18N
        labelPlatformWarning.setText("<html><b>unknown format</b></html>");
        labelPlatformWarning.setToolTipText("<html>This map format is unknown and cannot be Exported. Most likely it<br>\nis supported by a plugin that is not installed or cannot be loaded.</html>");

        jLabel4.setText("Game settings");

        jLabel5.setText("Game mode:");

        comboBoxGameType.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                comboBoxGameTypeActionPerformed(evt);
            }
        });

        checkBoxAllowCheats.setSelected(true);
        checkBoxAllowCheats.setText(" ");
        checkBoxAllowCheats.setToolTipText("Whether to allow cheats (single player commands)");

        jLabel6.setText("Difficulty:");

        comboBoxDifficulty.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Peaceful", "Easy", "Normal", "Hard" }));

        checkBoxMapFeatures.setSelected(true);
        checkBoxMapFeatures.setText(" ");

        jLabel7.setText("General settings");

        jLabel8.setLabelFor(checkBoxAllowCheats);
        jLabel8.setText("Allow cheats:");

        jLabel9.setLabelFor(checkBoxMapFeatures);
        jLabel9.setText("Generate structures:");

        jLabel10.setLabelFor(checkBoxGoodies);
        jLabel10.setText("Include chest of goodies:");

        jLabel2.setText("Directory:");

        jLabel3.setText("Name:");

        fieldDirectory.setText("jTextField1");

        fieldName.setText("jTextField2");

        buttonSelectDirectory.setText("...");
        buttonSelectDirectory.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonSelectDirectoryActionPerformed(evt);
            }
        });

        jLabel11.setText("Data packs:");

        listDataPacks.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
                listDataPacksValueChanged(evt);
            }
        });
        jScrollPane1.setViewportView(listDataPacks);

        buttonAddDataPack.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/pepsoft/worldpainter/icons/brick_add.png"))); // NOI18N
        buttonAddDataPack.setToolTipText("Add a data pack");
        buttonAddDataPack.setMargin(new java.awt.Insets(2, 2, 2, 2));
        buttonAddDataPack.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonAddDataPackActionPerformed(evt);
            }
        });

        buttonRemoveDataPack.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/pepsoft/worldpainter/icons/brick_delete.png"))); // NOI18N
        buttonRemoveDataPack.setToolTipText("Remove the selected data pack(s)");
        buttonRemoveDataPack.setEnabled(false);
        buttonRemoveDataPack.setMargin(new java.awt.Insets(2, 2, 2, 2));
        buttonRemoveDataPack.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonRemoveDataPackActionPerformed(evt);
            }
        });

        jLabel12.setText("<html><i>Use at own risk. WorldPainter does</i> not <i>check the validity or version! <i></html>");

        panelMinecraftWorldBorder.setBorder(javax.swing.BorderFactory.createTitledBorder("World Border"));

        jLabel79.setText("Centre:");

        spinnerMcBorderCentreX.setModel(new javax.swing.SpinnerNumberModel(0, -99999, 99999, 1));

        jLabel80.setText(", ");

        spinnerMcBorderCentreY.setModel(new javax.swing.SpinnerNumberModel(0, -99999, 99999, 1));

        jLabel81.setText("Size:");

        spinnerMcBorderSize.setModel(new javax.swing.SpinnerNumberModel(0, 0, 60000000, 1));

        jLabel85.setText(" blocks");

        jLabel86.setText(" blocks");

        javax.swing.GroupLayout panelMinecraftWorldBorderLayout = new javax.swing.GroupLayout(panelMinecraftWorldBorder);
        panelMinecraftWorldBorder.setLayout(panelMinecraftWorldBorderLayout);
        panelMinecraftWorldBorderLayout.setHorizontalGroup(
            panelMinecraftWorldBorderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelMinecraftWorldBorderLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(panelMinecraftWorldBorderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(panelMinecraftWorldBorderLayout.createSequentialGroup()
                        .addComponent(jLabel79)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(spinnerMcBorderCentreX, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 0, 0)
                        .addComponent(jLabel80)
                        .addGap(0, 0, 0)
                        .addComponent(spinnerMcBorderCentreY, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 0, 0)
                        .addComponent(jLabel85))
                    .addGroup(panelMinecraftWorldBorderLayout.createSequentialGroup()
                        .addComponent(jLabel81)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(spinnerMcBorderSize, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 0, 0)
                        .addComponent(jLabel86)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        panelMinecraftWorldBorderLayout.setVerticalGroup(
            panelMinecraftWorldBorderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelMinecraftWorldBorderLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(panelMinecraftWorldBorderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel79)
                    .addComponent(spinnerMcBorderCentreX, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel80)
                    .addComponent(spinnerMcBorderCentreY, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel85))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(panelMinecraftWorldBorderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel81)
                    .addComponent(spinnerMcBorderSize, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel86))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jSeparator1)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jLabel2)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(fieldDirectory)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(buttonSelectDirectory))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jLabel3)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(fieldName))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jScrollPane1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(buttonAddDataPack, javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(buttonRemoveDataPack, javax.swing.GroupLayout.Alignment.TRAILING)))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel7)
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addComponent(jLabel10)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(checkBoxGoodies))
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addComponent(jLabel1)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(labelPlatform, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(labelPlatformWarning, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addComponent(jLabel12, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel4)
                            .addComponent(jLabel11))
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addComponent(jLabel8)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(checkBoxAllowCheats))
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addComponent(jLabel6)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(comboBoxDifficulty, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addComponent(jLabel5)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(comboBoxGameType, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addComponent(jLabel9)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(checkBoxMapFeatures)))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 18, Short.MAX_VALUE)
                        .addComponent(panelMinecraftWorldBorder, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel7)
                .addGap(18, 18, 18)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(fieldDirectory, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(buttonSelectDirectory))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3)
                    .addComponent(fieldName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(labelPlatform, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(labelPlatformWarning, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(checkBoxGoodies)
                    .addComponent(jLabel10))
                .addGap(18, 18, 18)
                .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel4)
                .addGap(18, 18, 18)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel5)
                            .addComponent(comboBoxGameType, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel6)
                            .addComponent(comboBoxDifficulty, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(checkBoxAllowCheats)
                            .addComponent(jLabel8))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel9)
                            .addComponent(checkBoxMapFeatures))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel11))
                    .addComponent(panelMinecraftWorldBorder, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel12, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(buttonAddDataPack)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(buttonRemoveDataPack)))
                .addContainerGap())
        );

        jTabbedPane1.addTab("General", jPanel1);

        buttonTestExport.setText("Test Export...");
        buttonTestExport.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonTestExportActionPerformed(evt);
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
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(buttonTestExport)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(buttonExport)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(buttonCancel))
                    .addComponent(jTabbedPane1))
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
                    .addComponent(buttonExport)
                    .addComponent(buttonTestExport))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void buttonCancelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonCancelActionPerformed
        cancel();
    }//GEN-LAST:event_buttonCancelActionPerformed

    private void buttonExportActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonExportActionPerformed
        export(EXPORT_EVERYTHING);
    }//GEN-LAST:event_buttonExportActionPerformed

    private void buttonSelectDirectoryActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonSelectDirectoryActionPerformed
        selectDir();
    }//GEN-LAST:event_buttonSelectDirectoryActionPerformed

    private void platformChanged() {
        Platform newPlatform = world.getPlatform();
        labelPlatform.setText("<html><u>" + newPlatform.displayName + "</u></html>");
        GameType gameType = (GameType) comboBoxGameType.getSelectedItem();
        comboBoxGameType.setModel(new DefaultComboBoxModel<>(newPlatform.supportedGameTypes.toArray(new GameType[newPlatform.supportedGameTypes.size()])));
        if (newPlatform.supportedGameTypes.contains(gameType)) {
            comboBoxGameType.setSelectedItem(gameType);
        } else {
            comboBoxGameType.setSelectedItem(SURVIVAL);
        }
        comboBoxGameType.setEnabled(newPlatform.supportedGameTypes.size() > 1);
        if (newPlatform != JAVA_MCREGION) {
            checkBoxAllowCheats.setSelected(gameType == CREATIVE);
        } else {
            checkBoxAllowCheats.setSelected(false);
        }
        listDataPacks.setEnabled(newPlatform.capabilities.contains(DATA_PACKS));

        if (supportedPlatforms.contains(newPlatform)) {
            labelPlatformWarning.setVisible(false);
            File exportDir = Configuration.getInstance().getExportDirectory(newPlatform);
            if ((exportDir == null) || (!exportDir.isDirectory())) {
                exportDir = PlatformManager.getInstance().getDefaultExportDir(newPlatform);
            }
            if ((exportDir != null) && exportDir.isDirectory()) {
                fieldDirectory.setText(exportDir.getAbsolutePath());
            }
        } else {
            labelPlatformWarning.setVisible(true);
        }

        checkBoxGoodies.setSelected(world.isCreateGoodiesChest());

        pack();
        setControlStates();
    }

    private void comboBoxGameTypeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_comboBoxGameTypeActionPerformed
        if ((world.getPlatform() != JAVA_MCREGION) && (comboBoxGameType.getSelectedItem() == CREATIVE)) {
            checkBoxAllowCheats.setSelected(true);
            comboBoxDifficulty.setSelectedIndex(DIFFICULTY_PEACEFUL);
        } else if (comboBoxGameType.getSelectedItem() == HARDCORE) {
            checkBoxAllowCheats.setSelected(false);
            comboBoxDifficulty.setSelectedIndex(DIFFICULTY_HARD);
        }
        setControlStates();
    }//GEN-LAST:event_comboBoxGameTypeActionPerformed

    private void labelPlatformMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_labelPlatformMouseClicked
        changePlatform();
    }//GEN-LAST:event_labelPlatformMouseClicked

    private void buttonTestExportActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonTestExportActionPerformed
        testExport();
    }//GEN-LAST:event_buttonTestExportActionPerformed

    private void buttonAddDataPackActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonAddDataPackActionPerformed
        addDataPack();
    }//GEN-LAST:event_buttonAddDataPackActionPerformed

    private void buttonRemoveDataPackActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonRemoveDataPackActionPerformed
        removeSelectedDataPacks();
    }//GEN-LAST:event_buttonRemoveDataPackActionPerformed

    private void listDataPacksValueChanged(javax.swing.event.ListSelectionEvent evt) {//GEN-FIRST:event_listDataPacksValueChanged
        setControlStates();
    }//GEN-LAST:event_listDataPacksValueChanged

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton buttonAddDataPack;
    private javax.swing.JButton buttonCancel;
    private javax.swing.JButton buttonExport;
    private javax.swing.ButtonGroup buttonGroup2;
    private javax.swing.JButton buttonRemoveDataPack;
    private javax.swing.JButton buttonSelectDirectory;
    private javax.swing.JButton buttonTestExport;
    private javax.swing.JCheckBox checkBoxAllowCheats;
    private javax.swing.JCheckBox checkBoxGoodies;
    private javax.swing.JCheckBox checkBoxMapFeatures;
    private javax.swing.JComboBox comboBoxDifficulty;
    private javax.swing.JComboBox<GameType> comboBoxGameType;
    private javax.swing.JTextField fieldDirectory;
    private javax.swing.JTextField fieldName;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel79;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel80;
    private javax.swing.JLabel jLabel81;
    private javax.swing.JLabel jLabel85;
    private javax.swing.JLabel jLabel86;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JLabel labelPlatform;
    private javax.swing.JLabel labelPlatformWarning;
    private javax.swing.JList<File> listDataPacks;
    private javax.swing.JPanel panelMinecraftWorldBorder;
    private javax.swing.JSpinner spinnerMcBorderCentreX;
    private javax.swing.JSpinner spinnerMcBorderCentreY;
    private javax.swing.JSpinner spinnerMcBorderSize;
    // End of variables declaration//GEN-END:variables

    private final World2 world;
    private final ColourScheme colourScheme;
    private final Set<Layer> hiddenLayers;
    private final boolean contourLines;
    private final int contourSeparation;
    private final TileRenderer.LightOrigin lightOrigin;
    private final CustomBiomeManager customBiomeManager;
    private final WorldPainter view;
    private final Map<Anchor, DimensionPropertiesEditor> dimensionPropertiesEditors = new HashMap<>();
    private final List<Platform> supportedPlatforms = new ArrayList<>();
    private final DefaultListModel<File> dataPacksListModel = new DefaultListModel<>();
    private boolean disableDisabledLayersWarning;

    private static final long serialVersionUID = 1L;
}