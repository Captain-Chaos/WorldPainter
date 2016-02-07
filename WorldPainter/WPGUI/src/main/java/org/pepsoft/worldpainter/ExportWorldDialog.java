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

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.DefaultComboBoxModel;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.pepsoft.util.DesktopUtils;
import org.pepsoft.worldpainter.layers.CustomLayer;
import org.pepsoft.worldpainter.layers.Layer;
import org.pepsoft.worldpainter.layers.Populate;
import org.pepsoft.worldpainter.util.MinecraftUtil;

import static org.pepsoft.minecraft.Constants.*;
import static org.pepsoft.worldpainter.Constants.*;
import org.pepsoft.worldpainter.biomeschemes.CustomBiomeManager;

/**
 *
 * @author pepijn
 */
public class ExportWorldDialog extends javax.swing.JDialog {
    /** Creates new form ExportWorldDialog */
    public ExportWorldDialog(java.awt.Frame parent, World2 world, BiomeScheme biomeScheme, ColourScheme colourScheme, CustomBiomeManager customBiomeManager, Collection<Layer> hiddenLayers, boolean contourLines, int contourSeparation, TileRenderer.LightOrigin lightOrigin, WorldPainter view) {
        super(parent, true);
        this.world = world;
        selectedTiles = world.getTilesToExport();
        selectedDimension = (selectedTiles != null) ? world.getDimensionsToExport().iterator().next() : DIM_NORMAL;
        generatorOptions = world.getGeneratorOptions();
        this.biomeScheme = biomeScheme;
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
            jLabel4.setVisible(false);
            comboBoxGenerator.setVisible(false);
            buttonGeneratorOptions.setVisible(false);
            checkBoxMapFeatures.setVisible(false);

            jLabel1.setVisible(false);
            comboBoxMinecraftVersion.setVisible(false);
        }

        if (config.getExportDirectory() != null) {
            fieldDirectory.setText(config.getExportDirectory().getAbsolutePath());
        } else {
            File minecraftDir = MinecraftUtil.findMinecraftDir();
            if (minecraftDir != null) {
                fieldDirectory.setText(new File(minecraftDir, "saves").getAbsolutePath());
            } else {
                fieldDirectory.setText(DesktopUtils.getDocumentsFolder().getAbsolutePath());
            }
        }
        fieldName.setText(world.getName());

        surfacePropertiesEditor.setColourScheme(colourScheme);
        surfacePropertiesEditor.setMode(DimensionPropertiesEditor.Mode.EXPORT);
        surfacePropertiesEditor.setDimension(world.getDimension(0));
        if (world.getDimension(DIM_NETHER) != null) {
            netherPropertiesEditor.setColourScheme(colourScheme);
            netherPropertiesEditor.setMode(DimensionPropertiesEditor.Mode.EXPORT);
            netherPropertiesEditor.setDimension(world.getDimension(DIM_NETHER));
        } else {
            jTabbedPane1.setEnabledAt(2, false);
        }
        if (world.getDimension(DIM_END) != null) {
            endPropertiesEditor.setColourScheme(colourScheme);
            endPropertiesEditor.setMode(DimensionPropertiesEditor.Mode.EXPORT);
            endPropertiesEditor.setDimension(world.getDimension(DIM_END));
        } else {
            jTabbedPane1.setEnabledAt(4, false);
        }
        if (world.getDimension(DIM_NORMAL_CEILING) != null) {
            surfaceCeilingPropertiesEditor.setColourScheme(colourScheme);
            surfaceCeilingPropertiesEditor.setMode(DimensionPropertiesEditor.Mode.EXPORT);
            surfaceCeilingPropertiesEditor.setDimension(world.getDimension(DIM_NORMAL_CEILING));
        } else {
            jTabbedPane1.setEnabledAt(1, false);
        }
        if (world.getDimension(DIM_NETHER_CEILING) != null) {
            netherCeilingPropertiesEditor.setColourScheme(colourScheme);
            netherCeilingPropertiesEditor.setMode(DimensionPropertiesEditor.Mode.EXPORT);
            netherCeilingPropertiesEditor.setDimension(world.getDimension(DIM_NETHER_CEILING));
        } else {
            jTabbedPane1.setEnabledAt(3, false);
        }
        if (world.getDimension(DIM_END_CEILING) != null) {
            endCeilingPropertiesEditor.setColourScheme(colourScheme);
            endCeilingPropertiesEditor.setMode(DimensionPropertiesEditor.Mode.EXPORT);
            endCeilingPropertiesEditor.setDimension(world.getDimension(DIM_END_CEILING));
        } else {
            jTabbedPane1.setEnabledAt(5, false);
        }
        checkBoxGoodies.setSelected(world.isCreateGoodiesChest());
        int generator = world.getGenerator().ordinal();
        int gameType = world.getGameType();
        if (world.getMaxHeight() != DEFAULT_MAX_HEIGHT_2) {
            comboBoxGenerator.setModel(new DefaultComboBoxModel(new String[] {"Default", "Superflat"}));
            if (generator > 1) {
                generator = 0;
            }
            comboBoxGameType.setModel(new DefaultComboBoxModel(new String[] {"Survival", "Creative"}));
            if (gameType > World2.GAME_TYPE_CREATIVE) {
                gameType = World2.GAME_TYPE_SURVIVAL;
            }
            comboBoxMinecraftVersion.setSelectedIndex(1);
            comboBoxMinecraftVersion.setEnabled(false);
            comboBoxMinecraftVersion.setToolTipText("Only worlds with a height of 256 blocks can be exported in Anvil format");
        } else if (world.getVersion() == SUPPORTED_VERSION_1) {
            comboBoxGenerator.setModel(new DefaultComboBoxModel(new String[] {"Default", "Superflat"}));
            if (generator > 1) {
                generator = 0;
            }
            comboBoxGameType.setModel(new DefaultComboBoxModel(new String[] {"Survival", "Creative"}));
            if (gameType > World2.GAME_TYPE_CREATIVE) {
                gameType = World2.GAME_TYPE_SURVIVAL;
            }
            comboBoxMinecraftVersion.setSelectedIndex(1);
        } else {
            comboBoxGenerator.setModel(new DefaultComboBoxModel(new String[] {"Default", "Superflat", "Large Biomes"}));
        }
        comboBoxGenerator.setSelectedIndex(generator);
        comboBoxGameType.setSelectedIndex(gameType);
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
dims:   for (Dimension dim: world.getDimensions()) {
            for (CustomLayer customLayer: dim.getCustomLayers()) {
                if (! customLayer.isExport()) {
                    disableDisabledLayersWarning = false;
                    break dims;
                }
            }
        }

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

        rootPane.setDefaultButton(buttonExport);

        setControlStates();
        
        pack();
        setLocationRelativeTo(parent);
    }

    private void export() {
        StringBuilder sb = new StringBuilder("<html>Please confirm that you want to export the world<br>notwithstanding the following warnings:<br><ul>");
        boolean showWarning = false;
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
                    sb.append("<li>The spawn point is not inside the selected area!");
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
        Generator generator = Generator.values()[comboBoxGenerator.getSelectedIndex()];
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
        if (showWarning && (JOptionPane.showConfirmDialog(this, sb.toString(), "Review Warnings", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) != JOptionPane.YES_OPTION)) {
            return;
        }

        File baseDir = new File(fieldDirectory.getText().trim());
        String name = fieldName.getText().trim();

        if (! surfacePropertiesEditor.saveSettings()) {
            jTabbedPane1.setSelectedIndex(0);
            return;
        }
        
        if (world.getDimension(DIM_NETHER) != null) {
            if (! netherPropertiesEditor.saveSettings()) {
                jTabbedPane1.setSelectedIndex(2);
                return;
            }
        }
        if (world.getDimension(DIM_END) != null) {
            if (! endPropertiesEditor.saveSettings()) {
                jTabbedPane1.setSelectedIndex(4);
                return;
            }
        }
        if (world.getDimension(DIM_NORMAL_CEILING) != null) {
            if (! surfaceCeilingPropertiesEditor.saveSettings()) {
                jTabbedPane1.setSelectedIndex(1);
                return;
            }
        }
        if (world.getDimension(DIM_NETHER_CEILING) != null) {
            if (! netherCeilingPropertiesEditor.saveSettings()) {
                jTabbedPane1.setSelectedIndex(3);
                return;
            }
        }
        if (world.getDimension(DIM_END_CEILING) != null) {
            if (! endCeilingPropertiesEditor.saveSettings()) {
                jTabbedPane1.setSelectedIndex(5);
                return;
            }
        }
        world.setCreateGoodiesChest(checkBoxGoodies.isSelected());
        world.setGameType(comboBoxGameType.getSelectedIndex());
        world.setAllowCheats(checkBoxAllowCheats.isSelected());
        world.setGenerator(generator);
        if ((generatorOptions != null) && (! generatorOptions.trim().isEmpty())) {
            world.setGeneratorOptions(generatorOptions.trim());
        } else {
            world.setGeneratorOptions(null);
        }
        world.setVersion((comboBoxMinecraftVersion.getSelectedIndex() == 0) ? SUPPORTED_VERSION_2 : SUPPORTED_VERSION_1);
        if (radioButtonExportEverything.isSelected()) {
            world.setDimensionsToExport(null);
            world.setTilesToExport(null);
        } else {
            world.setDimensionsToExport(Collections.singleton(selectedDimension));
            world.setTilesToExport(selectedTiles);
        }
        world.setMapFeatures(checkBoxMapFeatures.isSelected());
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
        comboBoxGenerator.setEnabled(false);
        comboBoxMinecraftVersion.setEnabled(false);
        radioButtonExportEverything.setEnabled(false);
        radioButtonExportSelection.setEnabled(false);
        labelSelectTiles.setForeground(null);
        labelSelectTiles.setCursor(null);
        checkBoxMapFeatures.setEnabled(false);
        comboBoxDifficulty.setEnabled(false);

        Configuration config = Configuration.getInstance();
        if (config != null) {
            config.setExportDirectory(baseDir);
        }

        ExportProgressDialog dialog = new ExportProgressDialog(this, world, baseDir, name);
        view.setInhibitUpdates(true);
        try {
            dialog.setVisible(true);
        } finally {
            view.setInhibitUpdates(false);
        }
        close();
    }

    private void close() {
        dispose();
    }

    private void cancel() {
        dispose();
    }

    private void setControlStates() {
        boolean dirExists = new File(fieldDirectory.getText().trim()).isDirectory();
        boolean nameEntered = fieldName.getText().trim().length() > 0;
        boolean tilesSelected = radioButtonExportEverything.isSelected() || ((selectedTiles != null) && (! selectedTiles.isEmpty()));
        buttonExport.setEnabled(dirExists && nameEntered && tilesSelected);
        if (radioButtonExportSelection.isSelected()) {
            labelSelectTiles.setForeground(Color.BLUE);
            labelSelectTiles.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        } else {
            labelSelectTiles.setForeground(null);
            labelSelectTiles.setCursor(null);
        }
        checkBoxAllowCheats.setEnabled((comboBoxMinecraftVersion.getSelectedIndex() == 0) && (comboBoxGameType.getSelectedIndex() != World2.GAME_TYPE_HARDCORE));
        buttonGeneratorOptions.setEnabled(comboBoxGenerator.getSelectedIndex() == 1);
        comboBoxDifficulty.setEnabled(comboBoxGameType.getSelectedIndex() != World2.GAME_TYPE_HARDCORE);
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
            ExportTileSelectionDialog dialog = new ExportTileSelectionDialog(this, world, selectedDimension, selectedTiles, colourScheme, biomeScheme, customBiomeManager, hiddenLayers, contourLines, contourSeparation, lightOrigin);
            dialog.setVisible(true);
            selectedDimension = dialog.getSelectedDimension();
            selectedTiles = dialog.getSelectedTiles();
            radioButtonExportSelection.setText("export " + selectedTiles.size() + " selected tiles");
            setControlStates();
            disableTileSelectionWarning = true;
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
        comboBoxMinecraftVersion = new javax.swing.JComboBox();
        jLabel1 = new javax.swing.JLabel();
        radioButtonExportEverything = new javax.swing.JRadioButton();
        radioButtonExportSelection = new javax.swing.JRadioButton();
        labelSelectTiles = new javax.swing.JLabel();
        checkBoxAllowCheats = new javax.swing.JCheckBox();
        jLabel4 = new javax.swing.JLabel();
        comboBoxGenerator = new javax.swing.JComboBox();
        jLabel5 = new javax.swing.JLabel();
        comboBoxGameType = new javax.swing.JComboBox();
        buttonGeneratorOptions = new javax.swing.JButton();
        checkBoxMapFeatures = new javax.swing.JCheckBox();
        jLabel6 = new javax.swing.JLabel();
        comboBoxDifficulty = new javax.swing.JComboBox();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Exporting");

        jLabel2.setText("Directory:");

        fieldDirectory.setText("jTextField1");

        buttonSelectDirectory.setText("...");
        buttonSelectDirectory.addActionListener(this::buttonSelectDirectoryActionPerformed);

        jLabel3.setText("Name:");

        fieldName.setText("jTextField2");

        buttonCancel.setText("Cancel");
        buttonCancel.addActionListener(this::buttonCancelActionPerformed);

        buttonExport.setText("Export");
        buttonExport.addActionListener(this::buttonExportActionPerformed);

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

        comboBoxMinecraftVersion.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Minecraft 1.8 (and 1.2 - 1.7; \"Anvil\")", "Minecraft 1.1 (and earlier; \"MCRegion\")" }));
        comboBoxMinecraftVersion.addActionListener(this::comboBoxMinecraftVersionActionPerformed);

        jLabel1.setText("Map format:");

        buttonGroup2.add(radioButtonExportEverything);
        radioButtonExportEverything.setSelected(true);
        radioButtonExportEverything.setText("export everything");
        radioButtonExportEverything.addActionListener(this::radioButtonExportEverythingActionPerformed);

        buttonGroup2.add(radioButtonExportSelection);
        radioButtonExportSelection.setText("export selected tiles");
        radioButtonExportSelection.addActionListener(this::radioButtonExportSelectionActionPerformed);

        labelSelectTiles.setText("<html><u>select tiles</u></html>");
        labelSelectTiles.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                labelSelectTilesMouseClicked(evt);
            }
        });

        checkBoxAllowCheats.setSelected(true);
        checkBoxAllowCheats.setText("Allow cheats");
        checkBoxAllowCheats.setToolTipText("Whether to allow cheats (single player commands)");

        jLabel4.setText("World type:");

        comboBoxGenerator.setToolTipText("<html>The world generator type to use for new land <em>outside</em> the WorldPainter-generated part</html>");
        comboBoxGenerator.addActionListener(this::comboBoxGeneratorActionPerformed);

        jLabel5.setText("Mode:");

        comboBoxGameType.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Survival", "Creative", "Adventure", "Hardcore" }));
        comboBoxGameType.addActionListener(this::comboBoxGameTypeActionPerformed);

        buttonGeneratorOptions.setText("...");
        buttonGeneratorOptions.setToolTipText("Edit the Superflat mode preset");
        buttonGeneratorOptions.addActionListener(this::buttonGeneratorOptionsActionPerformed);

        checkBoxMapFeatures.setSelected(true);
        checkBoxMapFeatures.setText("Structures");
        checkBoxMapFeatures.setToolTipText("<html>Whether Minecraft should generate NPC villages,<br>\nabandoned mines , strongholds, jungle temples and<br>\ndesert temples (only applies to areas with Populate)</html>");

        jLabel6.setText("Difficulty:");

        comboBoxDifficulty.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Peaceful", "Easy", "Normal", "Hard" }));

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
                        .addComponent(jLabel5)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(comboBoxGameType, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(checkBoxAllowCheats)
                        .addGap(18, 18, 18)
                        .addComponent(jLabel6)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(comboBoxDifficulty, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(jLabel1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(comboBoxMinecraftVersion, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, Short.MAX_VALUE)
                        .addComponent(buttonExport)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(buttonCancel))
                    .addComponent(jTabbedPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel2)
                            .addComponent(jLabel3))
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(checkBoxGoodies)
                        .addGap(18, 18, 18)
                        .addComponent(jLabel4)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(comboBoxGenerator, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(buttonGeneratorOptions)
                        .addGap(18, 18, 18)
                        .addComponent(checkBoxMapFeatures)
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
                    .addComponent(jLabel4)
                    .addComponent(comboBoxGenerator, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(buttonGeneratorOptions)
                    .addComponent(checkBoxMapFeatures))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(buttonCancel)
                    .addComponent(buttonExport)
                    .addComponent(comboBoxMinecraftVersion, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel1)
                    .addComponent(checkBoxAllowCheats)
                    .addComponent(jLabel5)
                    .addComponent(comboBoxGameType, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel6)
                    .addComponent(comboBoxDifficulty, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
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

    private void comboBoxMinecraftVersionActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_comboBoxMinecraftVersionActionPerformed
        int generator = comboBoxGenerator.getSelectedIndex();
        int gameType = comboBoxGameType.getSelectedIndex();
        switch (comboBoxMinecraftVersion.getSelectedIndex()) {
            case 0:
                comboBoxGenerator.setModel(new DefaultComboBoxModel(new String[] {"Default", "Superflat", "Large Biomes"}));
                comboBoxGameType.setModel(new DefaultComboBoxModel(new String[] {"Survival", "Creative", "Adventure", "Hardcore"}));
                checkBoxAllowCheats.setSelected(comboBoxGameType.getSelectedIndex() == World2.GAME_TYPE_CREATIVE);
                break;
            case 1:
                comboBoxGenerator.setModel(new DefaultComboBoxModel(new String[] {"Default", "Superflat"}));
                if (generator > 1) {
                    generator = 0;
                }
                comboBoxGameType.setModel(new DefaultComboBoxModel(new String[] {"Survival", "Creative"}));
                if (gameType > World2.GAME_TYPE_CREATIVE) {
                    gameType = World2.GAME_TYPE_SURVIVAL;
                }
                checkBoxAllowCheats.setSelected(false);
                break;
        }
        comboBoxGenerator.setSelectedIndex(generator);
        comboBoxGameType.setSelectedIndex(gameType);
        setControlStates();
    }//GEN-LAST:event_comboBoxMinecraftVersionActionPerformed

    private void comboBoxGameTypeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_comboBoxGameTypeActionPerformed
        if ((comboBoxMinecraftVersion.getSelectedIndex() == 0) && (comboBoxGameType.getSelectedIndex() == World2.GAME_TYPE_CREATIVE)) {
            checkBoxAllowCheats.setSelected(true);
            comboBoxDifficulty.setSelectedIndex(DIFFICULTY_PEACEFUL);
        } else if (comboBoxGameType.getSelectedIndex() == World2.GAME_TYPE_HARDCORE) {
            checkBoxAllowCheats.setSelected(false);
            comboBoxDifficulty.setSelectedIndex(DIFFICULTY_HARD);
        }
        setControlStates();
    }//GEN-LAST:event_comboBoxGameTypeActionPerformed

    private void buttonGeneratorOptionsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonGeneratorOptionsActionPerformed
        String editedGeneratorOptions = JOptionPane.showInputDialog(this, "Edit the Superflat mode preset:", generatorOptions);
        if (editedGeneratorOptions != null) {
            generatorOptions = editedGeneratorOptions;
        }
    }//GEN-LAST:event_buttonGeneratorOptionsActionPerformed

    private void comboBoxGeneratorActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_comboBoxGeneratorActionPerformed
        setControlStates();
    }//GEN-LAST:event_comboBoxGeneratorActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton buttonCancel;
    private javax.swing.JButton buttonExport;
    private javax.swing.JButton buttonGeneratorOptions;
    private javax.swing.ButtonGroup buttonGroup2;
    private javax.swing.JButton buttonSelectDirectory;
    private javax.swing.JCheckBox checkBoxAllowCheats;
    private javax.swing.JCheckBox checkBoxGoodies;
    private javax.swing.JCheckBox checkBoxMapFeatures;
    private javax.swing.JComboBox comboBoxDifficulty;
    private javax.swing.JComboBox comboBoxGameType;
    private javax.swing.JComboBox comboBoxGenerator;
    private javax.swing.JComboBox comboBoxMinecraftVersion;
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
    private javax.swing.JLabel labelSelectTiles;
    private org.pepsoft.worldpainter.DimensionPropertiesEditor netherCeilingPropertiesEditor;
    private org.pepsoft.worldpainter.DimensionPropertiesEditor netherPropertiesEditor;
    private javax.swing.JRadioButton radioButtonExportEverything;
    private javax.swing.JRadioButton radioButtonExportSelection;
    private org.pepsoft.worldpainter.DimensionPropertiesEditor surfaceCeilingPropertiesEditor;
    private org.pepsoft.worldpainter.DimensionPropertiesEditor surfacePropertiesEditor;
    // End of variables declaration//GEN-END:variables

    private final World2 world;
    private final BiomeScheme biomeScheme;
    private final ColourScheme colourScheme;
    private final Collection<Layer> hiddenLayers;
    private final boolean contourLines;
    private final int contourSeparation;
    private final TileRenderer.LightOrigin lightOrigin;
    private final CustomBiomeManager customBiomeManager;
    private final WorldPainter view;
    private int selectedDimension;
    private Set<Point> selectedTiles;
    private boolean disableTileSelectionWarning, disableDisabledLayersWarning;
    private String generatorOptions;
    
    private static final long serialVersionUID = 1L;
}