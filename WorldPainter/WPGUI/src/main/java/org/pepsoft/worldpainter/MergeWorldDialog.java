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

import org.pepsoft.minecraft.Level;
import org.pepsoft.util.DesktopUtils;
import org.pepsoft.util.FileUtils;
import org.pepsoft.util.ProgressReceiver;
import org.pepsoft.util.ProgressReceiver.OperationCancelled;
import org.pepsoft.util.swing.ProgressComponent.Listener;
import org.pepsoft.util.swing.ProgressTask;
import org.pepsoft.worldpainter.biomeschemes.CustomBiomeManager;
import org.pepsoft.worldpainter.layers.Layer;
import org.pepsoft.worldpainter.merging.WorldMerger;
import org.pepsoft.worldpainter.util.FileInUseException;
import org.pepsoft.worldpainter.util.MinecraftUtil;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.pepsoft.worldpainter.Constants.*;

/**
 *
 * @author pepijn
 */
// TODO: add support for multiple dimensions
public class MergeWorldDialog extends javax.swing.JDialog implements Listener {
    /** Creates new form ExportWorldDialog */
    public MergeWorldDialog(java.awt.Frame parent, World2 world, BiomeScheme biomeScheme, ColourScheme colourScheme, CustomBiomeManager customBiomeManager, Collection<Layer> hiddenLayers, boolean contourLines, int contourSeparation, TileRenderer.LightOrigin lightOrigin) {
        super(parent, true);
        this.world = world;
        this.biomeScheme = biomeScheme;
        this.colourScheme = colourScheme;
        this.hiddenLayers = hiddenLayers;
        this.contourLines = contourLines;
        this.contourSeparation = contourSeparation;
        this.lightOrigin = lightOrigin;
        this.customBiomeManager = customBiomeManager;
        selectedTiles = world.getTilesToExport();
        selectedDimension = (selectedTiles != null) ? world.getDimensionsToExport().iterator().next() : DIM_NORMAL;
        
        initComponents();

        Configuration config = Configuration.getInstance();
        if (world.getMergedWith() != null) {
            fieldLevelDatFile.setText(world.getMergedWith().getAbsolutePath());
        } else if (world.getImportedFrom() != null) {
            fieldLevelDatFile.setText(world.getImportedFrom().getAbsolutePath());
        } else if ((config != null) && (config.getSavesDirectory() != null)) {
            fieldLevelDatFile.setText(config.getSavesDirectory().getAbsolutePath());
        } else {
            File minecraftDir = MinecraftUtil.findMinecraftDir();
            if (minecraftDir != null) {
                fieldLevelDatFile.setText(new File(minecraftDir, "saves").getAbsolutePath());
            } else {
                fieldLevelDatFile.setText(DesktopUtils.getDocumentsFolder().getAbsolutePath());
            }
        }
        ((SpinnerNumberModel) spinnerSurfaceThickness.getModel()).setMaximum(world.getMaxHeight());
        if (selectedTiles != null) {
            radioButtonExportSelection.setText("merge " + selectedTiles.size() + " selected tiles");
            radioButtonExportSelection.setSelected(true);
            checkBoxSurface.setSelected(selectedDimension == DIM_NORMAL);
            checkBoxNether.setSelected(selectedDimension == DIM_NETHER);
            checkBoxEnd.setSelected(selectedDimension == DIM_END);
        } else if (world.getDimensionsToExport() != null) {
            checkBoxSurface.setSelected(world.getDimensionsToExport().contains(DIM_NORMAL));
            checkBoxNether.setSelected(world.getDimensionsToExport().contains(DIM_NETHER));
            checkBoxEnd.setSelected(world.getDimensionsToExport().contains(DIM_END));
        } else {
            checkBoxSurface.setSelected(world.getDimension(DIM_NORMAL) != null);
            checkBoxNether.setSelected(world.getDimension(DIM_NETHER) != null);
            checkBoxEnd.setSelected(world.getDimension(DIM_END) != null);
        }
        
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
        fieldLevelDatFile.getDocument().addDocumentListener(documentListener);

        setLocationRelativeTo(parent);

        ActionMap actionMap = rootPane.getActionMap();
        actionMap.put("cancel", new AbstractAction("cancel") {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
            
            private static final long serialVersionUID = 1L;
        });

        InputMap inputMap = rootPane.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "cancel");

        rootPane.setDefaultButton(buttonMerge);

        setControlStates();
        pack();
    }

    // ProgressComponent.Listener

    @Override
    public void exceptionThrown(final Throwable exception) {
        Throwable cause = exception;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        if (cause instanceof FileInUseException) {
            JOptionPane.showMessageDialog(MergeWorldDialog.this, "Could not merge the world because the existing map directory is in use.\nPlease close Minecraft and all other windows and try again.", "Map In Use", JOptionPane.ERROR_MESSAGE);
        } else if (cause instanceof MissingCustomTerrainException) {
            JOptionPane.showMessageDialog(MergeWorldDialog.this,
                "Custom Terrain " + ((MissingCustomTerrainException) exception).getIndex() + " not configured!\n" +
                "Please configure it on the Custom Terrain panel.\n" +
                "\n" +
                "The partially exported map is now probably corrupted.\n" +
                "You should delete it, or export the map again.", "Unconfigured Custom Terrain", JOptionPane.ERROR_MESSAGE);
        } else {
            ErrorDialog dialog = new ErrorDialog(MergeWorldDialog.this);
            dialog.setException(exception);
            dialog.setVisible(true);
        }
        close();
    }

    @Override
    public void done(Object result) {
        long end = System.currentTimeMillis();
        StringBuilder sb = new StringBuilder();
        sb.append("World merged with ").append(levelDatFile);
        long duration = (end - start) / 1000;
        int hours = (int) (duration / 3600);
        duration = duration - hours * 3600;
        int minutes = (int) (duration / 60);
        int seconds = (int) (duration - minutes * 60);
        sb.append("\nMerge took ").append(hours).append(":").append((minutes < 10) ? "0" : "").append(minutes).append(":").append((seconds < 10) ? "0" : "").append(seconds);
        sb.append("\n\nBackup of existing map created in:\n").append(backupDir);
        JOptionPane.showMessageDialog(MergeWorldDialog.this, sb.toString(), "Success", JOptionPane.INFORMATION_MESSAGE);
        close();
    }

    @Override
    public void cancelled() {
        JOptionPane.showMessageDialog(MergeWorldDialog.this, "Export cancelled by user.\n\nThe partially merged map is now probably corrupted!\nYou should delete it, and restore it from the backup at:\n" + backupDir, "Merge Cancelled", JOptionPane.WARNING_MESSAGE);
        close();
    }

    private void merge() {
        StringBuilder sb = new StringBuilder("<html>Please confirm that you want to merge the world<br>notwithstanding the following warnings:<br><ul>");
        boolean showWarning = false;
        if ((radioButtonExportSelection.isSelected()) && (! disableWarning)) {
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
            sb.append("<li>A tile selection is active! Only " + selectedTiles.size() + " tiles of the<br>" + dim + " dimension are going to be merged.");
            showWarning = showWarning || (! disableWarning);
        }
        sb.append("</ul>Do you want to continue with the merge?</html>");
        if (showWarning && (JOptionPane.showConfirmDialog(this, sb.toString(), "Review Warnings", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) != JOptionPane.YES_OPTION)) {
            return;
        }

        final boolean biomesOnly = radioButtonBiomes.isSelected();
        final boolean replaceChunks = radioButtonReplaceChunks.isSelected();

        fieldLevelDatFile.setEnabled(false);
        buttonSelectDirectory.setEnabled(false);
        buttonMerge.setEnabled(false);
        radioButtonAll.setEnabled(false);
        radioButtonBiomes.setEnabled(false);
        radioButtonReplaceChunks.setEnabled(false);
        radioButtonExportEverything.setEnabled(false);
        radioButtonExportSelection.setEnabled(false);
        checkBoxFillCaves.setEnabled(false);
        checkBoxRemoveManMadeAboveGround.setEnabled(false);
        checkBoxRemoveManMadeBelowGround.setEnabled(false);
        checkBoxRemoveResources.setEnabled(false);
        checkBoxRemoveTrees.setEnabled(false);
        checkBoxRemoveVegetation.setEnabled(false);
        spinnerSurfaceThickness.setEnabled(false);
        labelSelectTiles.setForeground(null);
        labelSelectTiles.setCursor(null);
        checkBoxSurface.setEnabled(false);
        checkBoxNether.setEnabled(false);
        checkBoxEnd.setEnabled(false);

        Configuration config = Configuration.getInstance();
        config.setSavesDirectory(levelDatFile.getParentFile().getParentFile());
        config.setMergeWarningDisplayed(true);
        world.setImportedFrom(levelDatFile);
        if (radioButtonExportEverything.isSelected()) {
            Set<Integer> dimensionsToExport = new HashSet<>();
            if (checkBoxSurface.isSelected()) {
                dimensionsToExport.add(DIM_NORMAL);
            }
            if (checkBoxNether.isSelected()) {
                dimensionsToExport.add(DIM_NETHER);
            }
            if (checkBoxEnd.isSelected()) {
                dimensionsToExport.add(DIM_END);
            }
            boolean allDimensionsSelected = true;
            for (Dimension dimension: world.getDimensions()) {
                if (! dimensionsToExport.contains(dimension.getDim())) {
                    allDimensionsSelected = false;
                    break;
                }
            }
            world.setDimensionsToExport(allDimensionsSelected ? null : dimensionsToExport);
            world.setTilesToExport(null);
        } else {
            world.setDimensionsToExport(Collections.singleton(selectedDimension));
            world.setTilesToExport(selectedTiles);
        }

        setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);

        start = System.currentTimeMillis();
        progressComponent1.setTask(new ProgressTask<Void>() {
            @Override
            public String getName() {
                return "Please wait";
            }

            @Override
            public Void execute(ProgressReceiver progressReceiver) throws OperationCancelled {
                final WorldMerger merger = new WorldMerger(world, levelDatFile);
                try {
                    backupDir = merger.selectBackupDir(levelDatFile.getParentFile());
                    if (biomesOnly) {
                        merger.mergeBiomes(backupDir, progressReceiver);
                    } else {
                        if (replaceChunks) {
                            merger.setReplaceChunks(true);
                        } else {
                            merger.setClearManMadeAboveGround(checkBoxRemoveManMadeAboveGround.isSelected());
                            merger.setClearManMadeBelowGround(checkBoxRemoveManMadeBelowGround.isSelected());
                            merger.setClearResources(checkBoxRemoveResources.isSelected());
                            merger.setClearTrees(checkBoxRemoveTrees.isSelected());
                            merger.setClearVegetation(checkBoxRemoveVegetation.isSelected());
                            merger.setFillCaves(checkBoxFillCaves.isSelected());
                            merger.setSurfaceMergeDepth((Integer) spinnerSurfaceThickness.getValue());
                        }
                        merger.merge(backupDir, progressReceiver);
                    }
                    if (merger.getWarnings() != null) {
                        try {
                            SwingUtilities.invokeAndWait(() -> {
                                Icon warningIcon = UIManager.getIcon("OptionPane.warningIcon");
                                Toolkit.getDefaultToolkit().beep();
                                int selectedOption = JOptionPane.showOptionDialog(MergeWorldDialog.this, "The merge process generated warnings! The existing map may have had pre-\nexisting damage or corruption. Not all chunks may have been merged correctly.", "Merge Warnings", JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, warningIcon, new Object[] {"Review warnings", "OK"}, null);
                                if (selectedOption == 0) {
                                    ImportWarningsDialog warningsDialog = new ImportWarningsDialog(MergeWorldDialog.this, "Merge Warnings");
                                    warningsDialog.setWarnings(merger.getWarnings());
                                    warningsDialog.setVisible(true);
                                }
                            });
                        } catch (InterruptedException | InvocationTargetException e) {
                            throw new RuntimeException(e);
                        }
                    }
                } catch (IOException e) {
                    throw new RuntimeException("I/O error while merging world", e);
                }
                return null;
            }
        });
        progressComponent1.setListener(this);
        progressComponent1.start();
    }

    private void close() {
        dispose();
    }

    private void setControlStates() {
        File file = new File(fieldLevelDatFile.getText().trim());
        boolean levelDatSelected = file.isFile() && (file.getName().equalsIgnoreCase("level.dat"));
        if (levelDatSelected) {
            levelDatFile = file;
            try {
                Level level = Level.load(levelDatFile);
                if (level.getVersion() != org.pepsoft.minecraft.Constants.SUPPORTED_VERSION_2) {
                    if (radioButtonBiomes.isSelected()) {
                        radioButtonAll.setSelected(true);
                    }
                    radioButtonBiomes.setEnabled(false);
                } else {
                    radioButtonBiomes.setEnabled(true);
                }
            } catch (IOException e) {
                throw new RuntimeException("I/O error while loading level.dat", e);
            }
        }
        boolean mergeAll = radioButtonAll.isSelected();
        boolean mergeBiomesOnly = radioButtonBiomes.isSelected();
        boolean mergeEverything = radioButtonExportEverything.isSelected();
        boolean surfacePresent = world.getDimension(DIM_NORMAL) != null;
        boolean netherPresent = world.getDimension(DIM_NETHER) != null;
        boolean endPresent = world.getDimension(DIM_END) != null;
        boolean oneDimensionPresent = world.getDimensions().length == 1;
        radioButtonExportEverything.setEnabled(! mergeBiomesOnly);
        radioButtonExportSelection.setEnabled(! mergeBiomesOnly);
        checkBoxFillCaves.setEnabled(mergeAll);
        checkBoxRemoveManMadeAboveGround.setEnabled(mergeAll);
        checkBoxRemoveManMadeBelowGround.setEnabled(mergeAll);
        checkBoxRemoveResources.setEnabled(mergeAll);
        checkBoxRemoveTrees.setEnabled(mergeAll);
        checkBoxRemoveVegetation.setEnabled(mergeAll);
        spinnerSurfaceThickness.setEnabled(mergeAll);
        checkBoxSurface.setEnabled(mergeEverything && (! mergeBiomesOnly) && surfacePresent && (! oneDimensionPresent));
        checkBoxNether.setEnabled(mergeEverything && (! mergeBiomesOnly) && netherPresent && (! oneDimensionPresent));
        checkBoxEnd.setEnabled(mergeEverything && (! mergeBiomesOnly) && endPresent && (! oneDimensionPresent));
        buttonMerge.setEnabled(levelDatSelected && (checkBoxSurface.isSelected() || checkBoxNether.isSelected() || checkBoxEnd.isSelected()));
        if (radioButtonExportSelection.isSelected()) {
            labelSelectTiles.setForeground(Color.BLUE);
            labelSelectTiles.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        } else {
            labelSelectTiles.setForeground(null);
            labelSelectTiles.setCursor(null);
        }
    }

    private void selectLevelDatFile() {
        File file = new File(fieldLevelDatFile.getText().trim());
        file = FileUtils.selectFileForOpen(this, "Select Minecraft map level.dat file", file.exists() ? file : null, new FileFilter() {
            @Override
            public boolean accept(File f) {
                return f.isDirectory() || f.getName().equalsIgnoreCase("level.dat");
            }

            @Override
            public String getDescription() {
                return "Minecraft level.dat files";
            }
        });
        if (file != null) {
            fieldLevelDatFile.setText(file.getAbsolutePath());
        }
    }

    private void selectTiles() {
        if (radioButtonExportSelection.isSelected() && (! radioButtonBiomes.isSelected())) {
            ExportTileSelectionDialog dialog = new ExportTileSelectionDialog(this, world, selectedDimension, selectedTiles, colourScheme, biomeScheme, customBiomeManager, hiddenLayers, contourLines, contourSeparation, lightOrigin);
            dialog.setVisible(true);
            selectedDimension = dialog.getSelectedDimension();
            checkBoxSurface.setSelected(selectedDimension == DIM_NORMAL);
            checkBoxNether.setSelected(selectedDimension == DIM_NETHER);
            checkBoxEnd.setSelected(selectedDimension == DIM_END);
            selectedTiles = dialog.getSelectedTiles();
            radioButtonExportSelection.setText("merge " + selectedTiles.size() + " selected tiles");
            pack();
            setControlStates();
            disableWarning = true;
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
        jLabel2 = new javax.swing.JLabel();
        fieldLevelDatFile = new javax.swing.JTextField();
        buttonSelectDirectory = new javax.swing.JButton();
        buttonMerge = new javax.swing.JButton();
        radioButtonAll = new javax.swing.JRadioButton();
        radioButtonBiomes = new javax.swing.JRadioButton();
        radioButtonReplaceChunks = new javax.swing.JRadioButton();
        progressComponent1 = new org.pepsoft.util.swing.ProgressComponent();
        radioButtonExportEverything = new javax.swing.JRadioButton();
        radioButtonExportSelection = new javax.swing.JRadioButton();
        labelSelectTiles = new javax.swing.JLabel();
        jLabel1 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        checkBoxRemoveTrees = new javax.swing.JCheckBox();
        checkBoxRemoveVegetation = new javax.swing.JCheckBox();
        checkBoxRemoveManMadeAboveGround = new javax.swing.JCheckBox();
        checkBoxRemoveResources = new javax.swing.JCheckBox();
        checkBoxFillCaves = new javax.swing.JCheckBox();
        checkBoxRemoveManMadeBelowGround = new javax.swing.JCheckBox();
        jLabel7 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jLabel8 = new javax.swing.JLabel();
        spinnerSurfaceThickness = new javax.swing.JSpinner();
        jLabel9 = new javax.swing.JLabel();
        checkBoxSurface = new javax.swing.JCheckBox();
        checkBoxNether = new javax.swing.JCheckBox();
        checkBoxEnd = new javax.swing.JCheckBox();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Merging");

        jLabel2.setText("Existing map to merge with:");

        fieldLevelDatFile.setText("jTextField1");

        buttonSelectDirectory.setText("...");
        buttonSelectDirectory.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonSelectDirectoryActionPerformed(evt);
            }
        });

        buttonMerge.setText("Merge");
        buttonMerge.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonMergeActionPerformed(evt);
            }
        });

        buttonGroup1.add(radioButtonAll);
        radioButtonAll.setSelected(true);
        radioButtonAll.setText("Merge old and new chunks");
        radioButtonAll.setToolTipText("Will merge everything (terrain type and height changes, new layers, biome changes, etc.). Takes a very long time.");
        radioButtonAll.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                radioButtonAllActionPerformed(evt);
            }
        });

        buttonGroup1.add(radioButtonBiomes);
        radioButtonBiomes.setText("Only change the biomes (for the entire Surface dimension)");
        radioButtonBiomes.setToolTipText("<html>Will merge <i>only</i> biome changes. Ignores the read-only layer. Much quicker than merging everything, and with no side effects.</html>");
        radioButtonBiomes.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                radioButtonBiomesActionPerformed(evt);
            }
        });

        buttonGroup1.add(radioButtonReplaceChunks);
        radioButtonReplaceChunks.setText("Completely replace chunks with new chunks");
        radioButtonReplaceChunks.setToolTipText("<html><i>This will </i>replace<i> all non-read-only chunks,<br>destroying everything that's there in the existing map! </i></html>");
        radioButtonReplaceChunks.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                radioButtonReplaceChunksActionPerformed(evt);
            }
        });

        buttonGroup2.add(radioButtonExportEverything);
        radioButtonExportEverything.setSelected(true);
        radioButtonExportEverything.setText("Merge everything");
        radioButtonExportEverything.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                radioButtonExportEverythingActionPerformed(evt);
            }
        });

        buttonGroup2.add(radioButtonExportSelection);
        radioButtonExportSelection.setText("merge selected tiles");
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

        jLabel1.setText("Choose which part of the map to merge:");

        jLabel4.setText("Choose what kind of merge to perform:");

        jLabel5.setText("<html>Options for the existing map (<b>non-read-only</b> chunks in <b>selected tiles</b> only):</html>");

        jLabel6.setText("<html><b>Above</b> ground:</html>");

        checkBoxRemoveTrees.setText("Remove all trees and huge mushrooms");
        checkBoxRemoveTrees.setToolTipText("Removes all wood and leaf blocks, as wells as cocoa plants, vines and saplings.");

        checkBoxRemoveVegetation.setText("Remove all other vegetation and crops");
        checkBoxRemoveVegetation.setToolTipText("Removes all tall grass, flowers, mushrooms, nether wart, pumpkins and melons, carrots and potatoes, wheat, etc.");

        checkBoxRemoveManMadeAboveGround.setText("Remove all man-made structures");
        checkBoxRemoveManMadeAboveGround.setToolTipText("Removes any block which cannot occur naturally, above ground.");

        checkBoxRemoveResources.setText("Remove all resources/ores");
        checkBoxRemoveResources.setToolTipText("Replaces all resource/ore blocks with stone (or netherrack in the case of quartz).");

        checkBoxFillCaves.setText("Fill in all caves and other hollow spaces");
        checkBoxFillCaves.setToolTipText("<html>Replaces all air, water, lava and other insubstantial blocks with stone.<br>\nTo replace man-made blocks as well, use \"remove all man-made structures\" also.</html>");

        checkBoxRemoveManMadeBelowGround.setText("Remove all man-made structures");
        checkBoxRemoveManMadeBelowGround.setToolTipText("Replaces any block which cannot occur naturally with stone or air, below ground.");

        jLabel7.setText("<html><b>Below</b> ground:</html>");

        jLabel3.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/pepsoft/worldpainter/icons/error.png"))); // NOI18N
        jLabel3.setText(" ");
        jLabel3.setToolTipText("<html>This removes <em>all</em> wood and leaf blocks, including man-made ones!<br>\nWorldPainter can't tell the difference between natural and man-made wood blocks.<br>\nBe sure to protect your builds with the Read-Only layer.</html>");

        jLabel8.setText("Thickness of surface layer to replace:");

        spinnerSurfaceThickness.setModel(new javax.swing.SpinnerNumberModel(3, 1, 256, 1));

        jLabel9.setText("blocks");

        checkBoxSurface.setText("Surface");
        checkBoxSurface.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                checkBoxSurfaceActionPerformed(evt);
            }
        });

        checkBoxNether.setText("Nether");
        checkBoxNether.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                checkBoxNetherActionPerformed(evt);
            }
        });

        checkBoxEnd.setText("End");
        checkBoxEnd.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                checkBoxEndActionPerformed(evt);
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
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                .addComponent(fieldLevelDatFile)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(buttonSelectDirectory))
                            .addComponent(progressComponent1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(buttonMerge, javax.swing.GroupLayout.Alignment.TRAILING)
                            .addGroup(layout.createSequentialGroup()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jLabel2)
                                    .addComponent(jLabel4)
                                    .addComponent(jLabel5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addGroup(layout.createSequentialGroup()
                                        .addGap(12, 12, 12)
                                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                            .addComponent(radioButtonBiomes)
                                            .addComponent(radioButtonAll)
                                            .addComponent(radioButtonReplaceChunks))))
                                .addGap(0, 0, Short.MAX_VALUE)))
                        .addContainerGap())
                    .addGroup(layout.createSequentialGroup()
                        .addGap(12, 12, 12)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(checkBoxRemoveTrees)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabel3))
                            .addComponent(checkBoxRemoveVegetation)
                            .addComponent(checkBoxRemoveManMadeAboveGround)
                            .addComponent(jLabel6, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(0, 0, 0)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel7, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(checkBoxRemoveManMadeBelowGround)
                            .addComponent(checkBoxFillCaves)
                            .addComponent(checkBoxRemoveResources))
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jLabel8)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(spinnerSurfaceThickness, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(0, 0, 0)
                                .addComponent(jLabel9))
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(radioButtonExportEverything)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(radioButtonExportSelection)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(labelSelectTiles, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addComponent(jLabel1)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(checkBoxSurface)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(checkBoxNether)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(checkBoxEnd)))
                        .addGap(0, 0, Short.MAX_VALUE))))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(fieldLevelDatFile, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(buttonSelectDirectory))
                .addGap(18, 18, 18)
                .addComponent(jLabel4)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(radioButtonAll)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(radioButtonReplaceChunks)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(radioButtonBiomes)
                .addGap(18, 18, 18)
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(radioButtonExportEverything)
                    .addComponent(radioButtonExportSelection)
                    .addComponent(labelSelectTiles, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(checkBoxSurface)
                    .addComponent(checkBoxNether)
                    .addComponent(checkBoxEnd))
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel8)
                    .addComponent(spinnerSurfaceThickness, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel9))
                .addGap(18, 18, 18)
                .addComponent(jLabel5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel6, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel7, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(checkBoxRemoveTrees)
                    .addComponent(checkBoxRemoveResources)
                    .addComponent(jLabel3))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(checkBoxRemoveVegetation)
                    .addComponent(checkBoxFillCaves))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(checkBoxRemoveManMadeAboveGround)
                    .addComponent(checkBoxRemoveManMadeBelowGround))
                .addGap(18, 18, 18)
                .addComponent(progressComponent1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(buttonMerge)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void buttonMergeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonMergeActionPerformed
        merge();
    }//GEN-LAST:event_buttonMergeActionPerformed

    private void buttonSelectDirectoryActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonSelectDirectoryActionPerformed
        selectLevelDatFile();
    }//GEN-LAST:event_buttonSelectDirectoryActionPerformed

    private void radioButtonExportEverythingActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_radioButtonExportEverythingActionPerformed
        setControlStates();
    }//GEN-LAST:event_radioButtonExportEverythingActionPerformed

    private void radioButtonExportSelectionActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_radioButtonExportSelectionActionPerformed
        if (radioButtonExportSelection.isSelected()) {
            selectTiles();
        } else {
            setControlStates();
        }
    }//GEN-LAST:event_radioButtonExportSelectionActionPerformed

    private void labelSelectTilesMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_labelSelectTilesMouseClicked
        selectTiles();
    }//GEN-LAST:event_labelSelectTilesMouseClicked

    private void radioButtonAllActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_radioButtonAllActionPerformed
        setControlStates();
    }//GEN-LAST:event_radioButtonAllActionPerformed

    private void radioButtonBiomesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_radioButtonBiomesActionPerformed
        setControlStates();
    }//GEN-LAST:event_radioButtonBiomesActionPerformed

    private void radioButtonReplaceChunksActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_radioButtonReplaceChunksActionPerformed
        setControlStates();
    }//GEN-LAST:event_radioButtonReplaceChunksActionPerformed

    private void checkBoxSurfaceActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_checkBoxSurfaceActionPerformed
        setControlStates();
    }//GEN-LAST:event_checkBoxSurfaceActionPerformed

    private void checkBoxNetherActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_checkBoxNetherActionPerformed
        setControlStates();
    }//GEN-LAST:event_checkBoxNetherActionPerformed

    private void checkBoxEndActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_checkBoxEndActionPerformed
        setControlStates();
    }//GEN-LAST:event_checkBoxEndActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.ButtonGroup buttonGroup1;
    private javax.swing.ButtonGroup buttonGroup2;
    private javax.swing.JButton buttonMerge;
    private javax.swing.JButton buttonSelectDirectory;
    private javax.swing.JCheckBox checkBoxEnd;
    private javax.swing.JCheckBox checkBoxFillCaves;
    private javax.swing.JCheckBox checkBoxNether;
    private javax.swing.JCheckBox checkBoxRemoveManMadeAboveGround;
    private javax.swing.JCheckBox checkBoxRemoveManMadeBelowGround;
    private javax.swing.JCheckBox checkBoxRemoveResources;
    private javax.swing.JCheckBox checkBoxRemoveTrees;
    private javax.swing.JCheckBox checkBoxRemoveVegetation;
    private javax.swing.JCheckBox checkBoxSurface;
    private javax.swing.JTextField fieldLevelDatFile;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JLabel labelSelectTiles;
    private org.pepsoft.util.swing.ProgressComponent progressComponent1;
    private javax.swing.JRadioButton radioButtonAll;
    private javax.swing.JRadioButton radioButtonBiomes;
    private javax.swing.JRadioButton radioButtonExportEverything;
    private javax.swing.JRadioButton radioButtonExportSelection;
    private javax.swing.JRadioButton radioButtonReplaceChunks;
    private javax.swing.JSpinner spinnerSurfaceThickness;
    // End of variables declaration//GEN-END:variables

    private final World2 world;
    private final BiomeScheme biomeScheme;
    private final ColourScheme colourScheme;
    private final Collection<Layer> hiddenLayers;
    private final boolean contourLines;
    private final int contourSeparation;
    private final TileRenderer.LightOrigin lightOrigin;
    private final CustomBiomeManager customBiomeManager;
    private File levelDatFile;
    private volatile File backupDir;
    private long start;
    private int selectedDimension;
    private Set<Point> selectedTiles;
    private boolean disableWarning;
    
    private static final long serialVersionUID = 1L;
}