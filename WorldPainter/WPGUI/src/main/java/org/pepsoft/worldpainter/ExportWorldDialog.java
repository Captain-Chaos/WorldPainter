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
import org.pepsoft.worldpainter.biomeschemes.CustomBiomeManager;
import org.pepsoft.worldpainter.layers.CustomLayer;
import org.pepsoft.worldpainter.layers.Layer;
import org.pepsoft.worldpainter.layers.Populate;
import org.pepsoft.worldpainter.plugins.PlatformManager;
import org.pepsoft.worldpainter.util.EnumListCellRenderer;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static java.util.Collections.singleton;
import static org.pepsoft.minecraft.Constants.DIFFICULTY_HARD;
import static org.pepsoft.minecraft.Constants.DIFFICULTY_PEACEFUL;
import static org.pepsoft.worldpainter.Constants.*;
import static org.pepsoft.worldpainter.DefaultPlugin.JAVA_ANVIL_1_18;
import static org.pepsoft.worldpainter.DefaultPlugin.JAVA_MCREGION;
import static org.pepsoft.worldpainter.GameType.*;
import static org.pepsoft.worldpainter.Platform.Capability.NAME_BASED;
import static org.pepsoft.worldpainter.Platform.Capability.POPULATE;
import static org.pepsoft.worldpainter.util.BackupUtils.cleanUpBackups;
import static org.pepsoft.worldpainter.util.MaterialUtils.gatherBlocksWithoutIds;

/**
 *
 * @author pepijn
 */
@SuppressWarnings({"unused", "FieldCanBeLocal", "rawtypes", "Convert2Lambda", "Anonymous2MethodRef"}) // Managed by NetBeans
public class ExportWorldDialog extends WorldPainterDialog {
    /** Creates new form ExportWorldDialog */
    public ExportWorldDialog(java.awt.Frame parent, World2 world, ColourScheme colourScheme, CustomBiomeManager customBiomeManager, Set<Layer> hiddenLayers, boolean contourLines, int contourSeparation, TileRenderer.LightOrigin lightOrigin, WorldPainter view) {
        super(parent);
        this.world = world;
        selectedTiles = world.getTilesToExport();
        selectedDimension = (selectedTiles != null) ? world.getDimensionsToExport().iterator().next() : DIM_NORMAL;
        final Dimension dim0 = world.getDimension(0);
        this.colourScheme = colourScheme;
        this.hiddenLayers = hiddenLayers;
        this.contourLines = contourLines;
        this.contourSeparation = contourSeparation;
        this.lightOrigin = lightOrigin;
        this.customBiomeManager = customBiomeManager;
        this.view = view;
        initComponents();

        Configuration config = Configuration.getInstance();
        if (config.isEasyMode()) {
            checkBoxMapFeatures.setVisible(false);
            jLabel1.setVisible(false);
            labelPlatform.setVisible(false);
        }

        Platform platform = world.getPlatform();
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
        fieldName.setText(world.getName());

        surfacePropertiesEditor.setColourScheme(colourScheme);
        surfacePropertiesEditor.setMode(DimensionPropertiesEditor.Mode.EXPORT);
        surfacePropertiesEditor.setDimension(dim0);
        dimensionPropertiesEditors.put(DIM_NORMAL, surfacePropertiesEditor);
        if (world.getDimension(DIM_NETHER) != null) {
            netherPropertiesEditor.setColourScheme(colourScheme);
            netherPropertiesEditor.setMode(DimensionPropertiesEditor.Mode.EXPORT);
            netherPropertiesEditor.setDimension(world.getDimension(DIM_NETHER));
            dimensionPropertiesEditors.put(DIM_NETHER, netherPropertiesEditor);
        } else {
            jTabbedPane1.setEnabledAt(2, false);
        }
        if (world.getDimension(DIM_END) != null) {
            endPropertiesEditor.setColourScheme(colourScheme);
            endPropertiesEditor.setMode(DimensionPropertiesEditor.Mode.EXPORT);
            endPropertiesEditor.setDimension(world.getDimension(DIM_END));
            dimensionPropertiesEditors.put(DIM_END, endPropertiesEditor);
        } else {
            jTabbedPane1.setEnabledAt(4, false);
        }
        if (world.getDimension(DIM_NORMAL_CEILING) != null) {
            surfaceCeilingPropertiesEditor.setColourScheme(colourScheme);
            surfaceCeilingPropertiesEditor.setMode(DimensionPropertiesEditor.Mode.EXPORT);
            surfaceCeilingPropertiesEditor.setDimension(world.getDimension(DIM_NORMAL_CEILING));
            dimensionPropertiesEditors.put(DIM_NORMAL_CEILING, surfaceCeilingPropertiesEditor);
        } else {
            jTabbedPane1.setEnabledAt(1, false);
        }
        if (world.getDimension(DIM_NETHER_CEILING) != null) {
            netherCeilingPropertiesEditor.setColourScheme(colourScheme);
            netherCeilingPropertiesEditor.setMode(DimensionPropertiesEditor.Mode.EXPORT);
            netherCeilingPropertiesEditor.setDimension(world.getDimension(DIM_NETHER_CEILING));
            dimensionPropertiesEditors.put(DIM_NETHER_CEILING, netherCeilingPropertiesEditor);
        } else {
            jTabbedPane1.setEnabledAt(3, false);
        }
        if (world.getDimension(DIM_END_CEILING) != null) {
            endCeilingPropertiesEditor.setColourScheme(colourScheme);
            endCeilingPropertiesEditor.setMode(DimensionPropertiesEditor.Mode.EXPORT);
            endCeilingPropertiesEditor.setDimension(world.getDimension(DIM_END_CEILING));
            dimensionPropertiesEditors.put(DIM_END_CEILING, endCeilingPropertiesEditor);
        } else {
            jTabbedPane1.setEnabledAt(5, false);
        }
        checkBoxGoodies.setSelected(world.isCreateGoodiesChest());
        labelPlatform.setText("<html><u>" + platform.displayName + "</u></html>");
        labelPlatform.setToolTipText("Click to change the map format");
        comboBoxGameType.setModel(new DefaultComboBoxModel<>(platform.supportedGameTypes.toArray(new GameType[platform.supportedGameTypes.size()])));
        comboBoxGameType.setSelectedItem(world.getGameType());
        comboBoxGameType.setEnabled(comboBoxGameType.getItemCount() > 1);
        comboBoxGameType.setRenderer(new EnumListCellRenderer());
        checkBoxAllowCheats.setSelected(world.isAllowCheats());
        if (selectedTiles != null) {
            radioButtonExportSelection.setText("export " + selectedTiles.size() + " selected tiles");
            radioButtonExportSelection.setSelected(true);
        }
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
            DesktopUtils.beep();
            JOptionPane.showMessageDialog(this, sb.toString(), "Map Format Not Compatible", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        if (! platform.isCompatible(world)) {
            DesktopUtils.beep();
            JOptionPane.showMessageDialog(this, String.format(/* language=HTML */ "<html>" +
                    "<p>The world cannot be exported in format %s because it is not compatible, for one of these reasons:" +
                    "<ul><li>The format does not support the world depth of %d blocks" +
                    "<li>The format does not support the world height of %d blocks" +
                    "<li>The format does not support one or more of the dimensions in this world" +
                    "</ul>" +
                    "</html>", platform.displayName, -world.getPlatform().minZ, world.getMaxHeight()), "Map Format Not Compatible", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        return true;
    }

    private void export() {
        // Check for errors
        if (! new File(fieldDirectory.getText().trim()).isDirectory()) {
            fieldDirectory.requestFocusInWindow();
            DesktopUtils.beep();
            JOptionPane.showMessageDialog(this, "The selected output directory does not exist or is not a directory.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (fieldName.getText().trim().isEmpty()) {
            fieldName.requestFocusInWindow();
            DesktopUtils.beep();
            JOptionPane.showMessageDialog(this, "You have not specified a name for the map.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if ((! radioButtonExportEverything.isSelected()) && ((selectedTiles == null) || selectedTiles.isEmpty())) {
            radioButtonExportEverything.requestFocusInWindow();
            DesktopUtils.beep();
            JOptionPane.showMessageDialog(this, "No tiles have been selected for export.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        final Platform platform = world.getPlatform();

        // Check for warnings
        StringBuilder sb = new StringBuilder("<html>Please confirm that you want to export the world<br>notwithstanding the following warnings:<br><ul>");
        boolean showWarning = false;
        Configuration config = Configuration.getInstance();
        if ((platform == JAVA_ANVIL_1_18) && (! config.isMessageDisplayed(BETA_118_WARNING_KEY))) {
            sb.append("<li><strong>Minecraft 1.18 support is still in preview!</strong><br>" +
                    "Be careful and keep backups. If you encounter<br>" +
                    "problems, please report them on GitHub:<br>" +
                    "https://www.worldpainter.net/issues<br>" +
                    "This warning will only be displayed once.");
            showWarning = true;
        }
        for (Dimension dimension: world.getDimensions()) {
            if (dimension.getDim() < 0) {
                // Skip ceilings
                continue;
            }
            final DimensionPropertiesEditor editor = dimensionPropertiesEditors.get(dimension.getDim());
            final Generator generatorType = editor.getSelectedGeneratorType();
            if ((editor.isPopulateSelected() || dimension.getAllLayers(true).contains(Populate.INSTANCE)) && (! platform.capabilities.contains(POPULATE))) {
                sb.append("<li>Population not supported for<br>map format " + platform.displayName + "; it will not have an effect");
                showWarning = true;
            } else if ((! radioButtonExportSelection.isSelected()) || (selectedDimension == dimension.getDim())) {
                // The dimension is going to be exported
                if ((generatorType == Generator.FLAT) && (editor.isPopulateSelected() || dimension.getAllLayers(true).contains(Populate.INSTANCE))) {
                    sb.append("<li>The Superflat world type is selected and Populate is in use.<br>Minecraft will <em>not</em> populate generated chunks for Superflat maps.");
                    showWarning = true;
                }
            }
            if (! platform.supportedGenerators.contains(generatorType)) {
                sb.append("<li>Map format " + platform.displayName + " does not support world type " + generatorType.getDisplayName() + ".<br>The world type will be reset to " + platform.supportedGenerators.get(0).getDisplayName() + ".");
                editor.setSelectedGeneratorType(platform.supportedGenerators.get(0));
                showWarning = true;
            }
        }
        if (radioButtonExportSelection.isSelected()) {
            if (selectedDimension == DIM_NORMAL) {
                boolean spawnInSelection = false;
                Point spawnPoint = world.getSpawnPoint();
                for (Point tile: selectedTiles) {
                    if ((spawnPoint.x >= (tile.x << 7)) && (spawnPoint.x < ((tile.x + 1) << 7)) && (spawnPoint.y >= (tile.y << 7)) && (spawnPoint.y < ((tile.y + 1) << 7))) {
                        spawnInSelection = true;
                        break;
                    }
                }
                if (! spawnInSelection) {
                    sb.append("<li>The spawn point is not inside the selected area.");
                    showWarning = true;
                }
            }
            String dim;
            switch (selectedDimension) {
                case DIM_NORMAL:
                    dim = "Surface";
                    break;
                case DIM_NETHER:
                    dim = "Nether";
                    break;
                case DIM_END:
                    dim = "End";
                    break;
                default:
                    throw new InternalError();
            }
            sb.append("<li>A tile selection is active! Only " + selectedTiles.size() + " tiles of the<br>" + dim + " dimension are going to be exported.");
            showWarning = showWarning || (! disableTileSelectionWarning);
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
        sb.append("</ul>Do you want to continue with the export?</html>");
        if (showWarning) {
            DesktopUtils.beep();
            if (JOptionPane.showConfirmDialog(this, sb.toString(), "Review Warnings", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) != JOptionPane.YES_OPTION) {
                return;
            }
        }

        File baseDir = new File(fieldDirectory.getText().trim());
        String name = fieldName.getText().trim();

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
        if (! checkCompatibility(platform)) {
            return;
        }

        world.setCreateGoodiesChest(checkBoxGoodies.isSelected());
        world.setGameType((GameType) comboBoxGameType.getSelectedItem());
        world.setAllowCheats(checkBoxAllowCheats.isSelected());
        world.setMapFeatures(checkBoxMapFeatures.isSelected());
        if (radioButtonExportEverything.isSelected()) {
            world.setDimensionsToExport(null);
            world.setTilesToExport(null);
        } else {
            world.setDimensionsToExport(singleton(selectedDimension));
            world.setTilesToExport(selectedTiles);
        }
        world.setDifficulty(comboBoxDifficulty.getSelectedIndex());
        
        fieldDirectory.setEnabled(false);
        fieldName.setEnabled(false);
        buttonSelectDirectory.setEnabled(false);
        buttonExport.setEnabled(false);
        buttonCancel.setEnabled(false);
        surfacePropertiesEditor.setEnabled(false);
        netherPropertiesEditor.setEnabled(false);
        endPropertiesEditor.setEnabled(false);
        checkBoxGoodies.setEnabled(false);
        comboBoxGameType.setEnabled(false);
        checkBoxAllowCheats.setEnabled(false);
        radioButtonExportEverything.setEnabled(false);
        radioButtonExportSelection.setEnabled(false);
        labelSelectTiles.setForeground(null);
        labelSelectTiles.setCursor(null);
        checkBoxMapFeatures.setEnabled(false);
        comboBoxDifficulty.setEnabled(false);

        config.setExportDirectory(world.getPlatform(), baseDir);
        if (platform == JAVA_ANVIL_1_18) {
            config.setMessageDisplayed(BETA_118_WARNING_KEY);
        }

        ExportProgressDialog dialog = new ExportProgressDialog(this, world, baseDir, name);
        view.setInhibitUpdates(true);
        try {
            dialog.setVisible(true);
        } finally {
            view.setInhibitUpdates(false);
        }
        ok();
    }

    private boolean saveDimensionSettings() {
        if (! surfacePropertiesEditor.saveSettings()) {
            jTabbedPane1.setSelectedIndex(0);
            return false;
        }
        if (world.getDimension(DIM_NETHER) != null) {
            if (! netherPropertiesEditor.saveSettings()) {
                jTabbedPane1.setSelectedIndex(2);
                return false;
            }
        }
        if (world.getDimension(DIM_END) != null) {
            if (! endPropertiesEditor.saveSettings()) {
                jTabbedPane1.setSelectedIndex(4);
                return false;
            }
        }
        if (world.getDimension(DIM_NORMAL_CEILING) != null) {
            if (! surfaceCeilingPropertiesEditor.saveSettings()) {
                jTabbedPane1.setSelectedIndex(1);
                return false;
            }
        }
        if (world.getDimension(DIM_NETHER_CEILING) != null) {
            if (! netherCeilingPropertiesEditor.saveSettings()) {
                jTabbedPane1.setSelectedIndex(3);
                return false;
            }
        }
        if (world.getDimension(DIM_END_CEILING) != null) {
            if (! endCeilingPropertiesEditor.saveSettings()) {
                jTabbedPane1.setSelectedIndex(5);
                return false;
            }
        }
        return true;
    }

    private void setControlStates() {
        boolean notHardcore = comboBoxGameType.getSelectedItem() != HARDCORE;
        if (radioButtonExportSelection.isSelected()) {
            labelSelectTiles.setForeground(Color.BLUE);
            labelSelectTiles.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        } else {
            labelSelectTiles.setForeground(null);
            labelSelectTiles.setCursor(null);
        }
        checkBoxAllowCheats.setEnabled((world.getPlatform() != JAVA_MCREGION) && notHardcore);
        comboBoxDifficulty.setEnabled(notHardcore);
    }

    private void selectDir() {
        // Can't use FileUtils.selectFileForOpen() since it doesn't support
        // selecting a directory
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setSelectedFile(new File(fieldDirectory.getText().trim()));
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            fieldDirectory.setText(fileChooser.getSelectedFile().getAbsolutePath());
        }
    }
    
    private void selectTiles() {
        if (radioButtonExportSelection.isSelected()) {
            ExportTileSelectionDialog dialog = new ExportTileSelectionDialog(this, world, selectedDimension, selectedTiles, colourScheme, customBiomeManager, hiddenLayers, contourLines, contourSeparation, lightOrigin);
            dialog.setVisible(true);
            selectedDimension = dialog.getSelectedDimension();
            selectedTiles = dialog.getSelectedTiles();
            radioButtonExportSelection.setText("export " + selectedTiles.size() + " selected tiles");
            setControlStates();
            disableTileSelectionWarning = true;
        }
    }

    private void changePlatform() {
        if (! saveDimensionSettings()) {
            return;
        }
        if (App.getInstance().changeWorldHeight(this)) {
            dimensionPropertiesEditors.forEach((dim, editor) -> editor.setDimension(world.getDimension(dim)));
            platformChanged();
        }
    }

    // Change the line adding jTabbedPane1 to the vertical group to:
    //
    //     .addComponent(jTabbedPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 100, Short.MAX_VALUE)
    //
    // in order to fix a bug where the tabbed pane is made much too tall.

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        buttonGroup2 = new javax.swing.ButtonGroup();
        jLabel2 = new javax.swing.JLabel();
        fieldDirectory = new javax.swing.JTextField();
        buttonSelectDirectory = new javax.swing.JButton();
        jLabel3 = new javax.swing.JLabel();
        fieldName = new javax.swing.JTextField();
        buttonCancel = new javax.swing.JButton();
        buttonExport = new javax.swing.JButton();
        jTabbedPane1 = new javax.swing.JTabbedPane();
        surfacePropertiesEditor = new org.pepsoft.worldpainter.DimensionPropertiesEditor();
        surfaceCeilingPropertiesEditor = new org.pepsoft.worldpainter.DimensionPropertiesEditor();
        netherPropertiesEditor = new org.pepsoft.worldpainter.DimensionPropertiesEditor();
        netherCeilingPropertiesEditor = new org.pepsoft.worldpainter.DimensionPropertiesEditor();
        endPropertiesEditor = new org.pepsoft.worldpainter.DimensionPropertiesEditor();
        endCeilingPropertiesEditor = new org.pepsoft.worldpainter.DimensionPropertiesEditor();
        checkBoxGoodies = new javax.swing.JCheckBox();
        jLabel1 = new javax.swing.JLabel();
        radioButtonExportEverything = new javax.swing.JRadioButton();
        radioButtonExportSelection = new javax.swing.JRadioButton();
        labelSelectTiles = new javax.swing.JLabel();
        checkBoxAllowCheats = new javax.swing.JCheckBox();
        jLabel5 = new javax.swing.JLabel();
        comboBoxGameType = new javax.swing.JComboBox<>();
        jLabel6 = new javax.swing.JLabel();
        comboBoxDifficulty = new javax.swing.JComboBox();
        labelPlatform = new javax.swing.JLabel();
        checkBoxMapFeatures = new javax.swing.JCheckBox();
        jLabel4 = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Exporting");

        jLabel2.setText("Directory:");

        fieldDirectory.setText("jTextField1");

        buttonSelectDirectory.setText("...");
        buttonSelectDirectory.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonSelectDirectoryActionPerformed(evt);
            }
        });

        jLabel3.setText("Name:");

        fieldName.setText("jTextField2");

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
        jTabbedPane1.addTab("Surface", surfacePropertiesEditor);
        jTabbedPane1.addTab("Surface Ceiling", surfaceCeilingPropertiesEditor);
        jTabbedPane1.addTab("Nether", netherPropertiesEditor);
        jTabbedPane1.addTab("Nether Ceiling", netherCeilingPropertiesEditor);
        jTabbedPane1.addTab("End", endPropertiesEditor);
        jTabbedPane1.addTab("End Ceiling", endCeilingPropertiesEditor);

        checkBoxGoodies.setSelected(true);
        checkBoxGoodies.setText("Include chest of goodies");
        checkBoxGoodies.setToolTipText("Include a chest with tools and resources near spawn for you as the level designer");

        jLabel1.setText("Map format:");

        buttonGroup2.add(radioButtonExportEverything);
        radioButtonExportEverything.setSelected(true);
        radioButtonExportEverything.setText("export everything");
        radioButtonExportEverything.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                radioButtonExportEverythingActionPerformed(evt);
            }
        });

        buttonGroup2.add(radioButtonExportSelection);
        radioButtonExportSelection.setText("export selected tiles");
        radioButtonExportSelection.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                radioButtonExportSelectionActionPerformed(evt);
            }
        });

        labelSelectTiles.setText("<html><u>select tiles</u></html>");
        labelSelectTiles.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                labelSelectTilesMouseClicked(evt);
            }
        });

        checkBoxAllowCheats.setSelected(true);
        checkBoxAllowCheats.setText("allow cheats:");
        checkBoxAllowCheats.setToolTipText("Whether to allow cheats (single player commands)");
        checkBoxAllowCheats.setHorizontalTextPosition(javax.swing.SwingConstants.LEADING);

        jLabel5.setText("mode:");

        comboBoxGameType.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                comboBoxGameTypeActionPerformed(evt);
            }
        });

        jLabel6.setText("difficulty:");

        comboBoxDifficulty.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Peaceful", "Easy", "Normal", "Hard" }));

        labelPlatform.setForeground(new java.awt.Color(0, 0, 255));
        labelPlatform.setText("<html><u>[EXPERIMENTAL] Minecraft 1.17</u></html>");
        labelPlatform.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        labelPlatform.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                labelPlatformMouseClicked(evt);
            }
        });

        checkBoxMapFeatures.setSelected(true);
        checkBoxMapFeatures.setText("generate structures:");
        checkBoxMapFeatures.setHorizontalTextPosition(javax.swing.SwingConstants.LEADING);

        jLabel4.setText("Minecraft settings:");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(fieldName)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(fieldDirectory)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(buttonSelectDirectory))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel4)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel5)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(comboBoxGameType, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(checkBoxAllowCheats)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel6)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(comboBoxDifficulty, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(checkBoxMapFeatures)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(buttonExport)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(buttonCancel))
                    .addComponent(jTabbedPane1)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel2)
                            .addComponent(jLabel3))
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(checkBoxGoodies)
                        .addGap(18, 18, 18)
                        .addComponent(jLabel1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(labelPlatform, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(radioButtonExportEverything)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(radioButtonExportSelection)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(labelSelectTiles, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(fieldDirectory, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(buttonSelectDirectory))
                .addGap(18, 18, 18)
                .addComponent(jLabel3)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(fieldName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(jTabbedPane1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(checkBoxGoodies)
                    .addComponent(radioButtonExportEverything)
                    .addComponent(radioButtonExportSelection)
                    .addComponent(labelSelectTiles, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel1)
                    .addComponent(labelPlatform, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(buttonCancel)
                    .addComponent(buttonExport)
                    .addComponent(checkBoxAllowCheats)
                    .addComponent(jLabel5)
                    .addComponent(comboBoxGameType, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel6)
                    .addComponent(comboBoxDifficulty, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel4)
                    .addComponent(checkBoxMapFeatures))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void buttonCancelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonCancelActionPerformed
        cancel();
    }//GEN-LAST:event_buttonCancelActionPerformed

    private void buttonExportActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonExportActionPerformed
        export();
    }//GEN-LAST:event_buttonExportActionPerformed

    private void buttonSelectDirectoryActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonSelectDirectoryActionPerformed
        selectDir();
    }//GEN-LAST:event_buttonSelectDirectoryActionPerformed

    private void radioButtonExportEverythingActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_radioButtonExportEverythingActionPerformed
        setControlStates();
    }//GEN-LAST:event_radioButtonExportEverythingActionPerformed

    private void labelSelectTilesMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_labelSelectTilesMouseClicked
        selectTiles();
    }//GEN-LAST:event_labelSelectTilesMouseClicked

    private void radioButtonExportSelectionActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_radioButtonExportSelectionActionPerformed
        if (radioButtonExportSelection.isSelected()) {
            selectTiles();
        } else {
            setControlStates();
        }
    }//GEN-LAST:event_radioButtonExportSelectionActionPerformed

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
        File exportDir = Configuration.getInstance().getExportDirectory(newPlatform);
        if ((exportDir == null) || (! exportDir.isDirectory())) {
            exportDir = PlatformManager.getInstance().getDefaultExportDir(newPlatform);
        }
        if ((exportDir != null) && exportDir.isDirectory()) {
            fieldDirectory.setText(exportDir.getAbsolutePath());
        }

        checkBoxGoodies.setSelected(world.isCreateGoodiesChest());

        dimensionPropertiesEditors.forEach((dim, editor) -> editor.setPlatform(newPlatform));

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

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton buttonCancel;
    private javax.swing.JButton buttonExport;
    private javax.swing.ButtonGroup buttonGroup2;
    private javax.swing.JButton buttonSelectDirectory;
    private javax.swing.JCheckBox checkBoxAllowCheats;
    private javax.swing.JCheckBox checkBoxGoodies;
    private javax.swing.JCheckBox checkBoxMapFeatures;
    private javax.swing.JComboBox comboBoxDifficulty;
    private javax.swing.JComboBox<GameType> comboBoxGameType;
    private org.pepsoft.worldpainter.DimensionPropertiesEditor endCeilingPropertiesEditor;
    private org.pepsoft.worldpainter.DimensionPropertiesEditor endPropertiesEditor;
    private javax.swing.JTextField fieldDirectory;
    private javax.swing.JTextField fieldName;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JLabel labelPlatform;
    private javax.swing.JLabel labelSelectTiles;
    private org.pepsoft.worldpainter.DimensionPropertiesEditor netherCeilingPropertiesEditor;
    private org.pepsoft.worldpainter.DimensionPropertiesEditor netherPropertiesEditor;
    private javax.swing.JRadioButton radioButtonExportEverything;
    private javax.swing.JRadioButton radioButtonExportSelection;
    private org.pepsoft.worldpainter.DimensionPropertiesEditor surfaceCeilingPropertiesEditor;
    private org.pepsoft.worldpainter.DimensionPropertiesEditor surfacePropertiesEditor;
    // End of variables declaration//GEN-END:variables

    private final World2 world;
    private final ColourScheme colourScheme;
    private final Set<Layer> hiddenLayers;
    private final boolean contourLines;
    private final int contourSeparation;
    private final TileRenderer.LightOrigin lightOrigin;
    private final CustomBiomeManager customBiomeManager;
    private final WorldPainter view;
    private final Map<Integer, DimensionPropertiesEditor> dimensionPropertiesEditors = new HashMap<>();
    private int selectedDimension;
    private Set<Point> selectedTiles;
    private boolean disableTileSelectionWarning, disableDisabledLayersWarning;

    private static final String BETA_118_WARNING_KEY = "org.pepsoft.worldpainter.beta118Warning";
    private static final long serialVersionUID = 1L;
}