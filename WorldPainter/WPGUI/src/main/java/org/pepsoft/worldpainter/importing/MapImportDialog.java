/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.importing;

import org.pepsoft.minecraft.Level;
import org.pepsoft.minecraft.RegionFile;
import org.pepsoft.util.FileUtils;
import org.pepsoft.util.ProgressReceiver;
import org.pepsoft.util.ProgressReceiver.OperationCancelled;
import org.pepsoft.util.swing.ProgressDialog;
import org.pepsoft.util.swing.ProgressTask;
import org.pepsoft.worldpainter.*;
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
import java.text.NumberFormat;
import java.util.*;
import java.util.List;
import java.util.regex.Pattern;

import static org.pepsoft.minecraft.Constants.SUPPORTED_VERSION_1;
import static org.pepsoft.minecraft.Constants.SUPPORTED_VERSION_2;

/**
 *
 * @author SchmitzP
 */
public class MapImportDialog extends javax.swing.JDialog {
    /**
     * Creates new form MapImportDialog
     */
    public MapImportDialog(App app) {
        super(app, ModalityType.APPLICATION_MODAL);
        this.app = app;
        
        initComponents();
        
        resetStats();

        fieldFilename.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                checkSelection();
                setControlStates();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                checkSelection();
                setControlStates();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                checkSelection();
                setControlStates();
            }
        });
        
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
        
        getRootPane().setDefaultButton(buttonOK);
        
        setLocationRelativeTo(app);
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public World2 getImportedWorld() {
        return importedWorld;
    }

    private void checkSelection() {
        String fileStr = fieldFilename.getText();
        if (fileStr.endsWith("level.dat")) {
            File file = new File(fileStr);
            if (file.isFile() && (! file.equals(previouslySelectedFile))) {
                previouslySelectedFile = file;
                analyseMap();
            }
        }
    }
    
    private void analyseMap() {
        mapStatistics = null;
        resetStats();
        
        File levelDatFile = new File(fieldFilename.getText());
        final File worldDir = levelDatFile.getParentFile();

        // Check if it's a valid level.dat file before we commit
        int version;
        try {
            Level testLevel = Level.load(levelDatFile);
            version = testLevel.getVersion();
        } catch (IOException e) {
            logger.error("IOException while analysing map " + levelDatFile, e);
            JOptionPane.showMessageDialog(MapImportDialog.this, strings.getString("selected.file.is.not.a.valid.level.dat.file"), strings.getString("invalid.file"), JOptionPane.ERROR_MESSAGE);
            return;
        } catch (IllegalArgumentException e) {
            logger.error("IllegalArgumentException while analysing map " + levelDatFile, e);
            JOptionPane.showMessageDialog(MapImportDialog.this, strings.getString("selected.file.is.not.a.valid.level.dat.file"), strings.getString("invalid.file"), JOptionPane.ERROR_MESSAGE);
            return;
        } catch (NullPointerException e) {
            logger.error("NullPointerException while analysing map " + levelDatFile, e);
            JOptionPane.showMessageDialog(MapImportDialog.this, strings.getString("selected.file.is.not.a.valid.level.dat.file"), strings.getString("invalid.file"), JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Other sanity checks
        if ((version != SUPPORTED_VERSION_1) && (version != SUPPORTED_VERSION_2)) {
            logger.error("Unsupported Minecraft version while analysing map " + levelDatFile);
            JOptionPane.showMessageDialog(MapImportDialog.this, strings.getString("unsupported.minecraft.version"), strings.getString("unsupported.version"), JOptionPane.ERROR_MESSAGE);
            return;
        }
        File regionDir = new File(worldDir, "region");
        if (! regionDir.isDirectory()) {
            logger.error("Region directory missing while analysing map " + levelDatFile);
            JOptionPane.showMessageDialog(MapImportDialog.this, strings.getString("the.region.folder.is.missing"), strings.getString("region.folder.missing"), JOptionPane.ERROR_MESSAGE);
            return;
        }
        final Pattern regionFilePattern = (version == SUPPORTED_VERSION_1)
            ? Pattern.compile("r\\.-?\\d+\\.-?\\d+\\.mcr")
            : Pattern.compile("r\\.-?\\d+\\.-?\\d+\\.mca");
        final File[] regionFiles = regionDir.listFiles((dir, name) -> {
            return regionFilePattern.matcher(name).matches();
        });
        if ((regionFiles == null) || (regionFiles.length == 0)) {
            logger.error("Region files missing while analysing map " + levelDatFile);
            JOptionPane.showMessageDialog(MapImportDialog.this, strings.getString("the.region.folder.contains.no.region.files"), strings.getString("region.files.missing"), JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        // Check for Nether and End
        boolean netherPresent = false, endPresent = false;
        File netherRegionDir = new File(worldDir, "DIM-1/region");
        if (netherRegionDir.isDirectory()) {
            File[] netherRegionFiles = netherRegionDir.listFiles((dir, name) -> {
                return regionFilePattern.matcher(name).matches();
            });
            if ((netherRegionFiles != null) && (netherRegionFiles.length > 0)) {
                netherPresent = true;
            }
        }
        File endRegionDir = new File(worldDir, "DIM1/region");
        if (endRegionDir.isDirectory()) {
            File[] endRegionFiles = endRegionDir.listFiles((dir, name) -> {
                return regionFilePattern.matcher(name).matches();
            });
            if ((endRegionFiles != null) && (endRegionFiles.length > 0)) {
                endPresent = true;
            }
        }
        checkBoxImportNether.setEnabled(netherPresent);
        checkBoxImportNether.setSelected(netherPresent);
        checkBoxImportEnd.setEnabled(endPresent);
        checkBoxImportEnd.setSelected(endPresent);

        mapStatistics = ProgressDialog.executeTask(this, new ProgressTask<MapStatistics>() {
            @Override
            public String getName() {
                return "Analyzing map...";
            }
            
            @Override
            public MapStatistics execute(ProgressReceiver progressReceiver) throws OperationCancelled {
                MapStatistics stats = new MapStatistics();
                
                int chunkCount = 0;
                List<Integer> xValues = new ArrayList<>(), zValues = new ArrayList<>();
                List<Point> chunks = new ArrayList<>();
                int count = 0;
                for (File file: regionFiles) {
                    String[] nameFrags = file.getName().split("\\.");
                    int regionX = Integer.parseInt(nameFrags[1]);
                    int regionZ = Integer.parseInt(nameFrags[2]);
                    try {
                        RegionFile regionFile = new RegionFile(file);
                        try {
                            for (int x = 0; x < 32; x++) {
                                for (int z = 0; z < 32; z++) {
                                    if (regionFile.containsChunk(x, z)) {
                                        chunkCount++;
                                        int chunkX = regionX * 32 + x, chunkZ = regionZ * 32 + z;
                                        if (chunkX < stats.lowestChunkX) {
                                            stats.lowestChunkX = chunkX;
                                        }
                                        if (chunkX > stats.highestChunkX) {
                                            stats.highestChunkX = chunkX;
                                        }
                                        if (chunkZ < stats.lowestChunkZ) {
                                            stats.lowestChunkZ = chunkZ;
                                        }
                                        if (chunkZ > stats.highestChunkZ) {
                                            stats.highestChunkZ = chunkZ;
                                        }
                                        xValues.add(chunkX);
                                        zValues.add(chunkZ);
                                        chunks.add(new Point(chunkX, chunkZ));
                                    }
                                }
                            }
                        } finally {
                            regionFile.close();
                        }
                    } catch (IOException e) {
                        throw new RuntimeException("I/O error while analyzing map " + worldDir, e);
                    }
                    count++;
                    progressReceiver.setProgress((float) count / (regionFiles.length + 1));
                }
                stats.chunkCount = chunkCount;

                if (chunkCount == 0) {
                    // Completely empty map (wrong region file format)?
                    progressReceiver.setProgress(1.0f);
                    return stats;
                }
                
                Collections.sort(xValues);
                int p1 = xValues.size() / 4;
                float q1 = xValues.get(p1) * 0.75f + xValues.get(p1 + 1) * 0.25f;
                int p2 = xValues.size() / 2;
                float q2 = (xValues.get(p2) + xValues.get(p2 + 1)) / 2f;
                int p3 = xValues.size() * 3 / 4;
                float q3 = xValues.get(p3) * 0.25f + xValues.get(p3 + 1) * 0.75f;
                float iqr = q3 - q1;
                int lowerLimit = (int) (q2 - iqr * 1.5f);
                int upperLimit = (int) (q2 + iqr * 1.5f);
                for (Point chunk: chunks) {
                    if ((chunk.x < lowerLimit) || (chunk.x > upperLimit)) {
                        stats.outlyingChunks.add(chunk);
                    }
                }

                Collections.sort(zValues);
                p1 = zValues.size() / 4;
                q1 = zValues.get(p1) * 0.75f + zValues.get(p1 + 1) * 0.25f;
                p2 = zValues.size() / 2;
                q2 = (zValues.get(p2) + zValues.get(p2 + 1)) / 2f;
                p3 = zValues.size() * 3 / 4;
                q3 = zValues.get(p3) * 0.25f + zValues.get(p3 + 1) * 0.75f;
                iqr = q3 - q1;
                lowerLimit = (int) (q2 - iqr * 1.5f);
                upperLimit = (int) (q2 + iqr * 1.5f);
                for (Point chunk: chunks) {
                    if ((chunk.y < lowerLimit) || (chunk.y > upperLimit)) {
                        stats.outlyingChunks.add(chunk);
                    }
                }
                
                if (! stats.outlyingChunks.isEmpty()) {
                    chunks.stream().filter(chunk -> !stats.outlyingChunks.contains(chunk)).forEach(chunk -> {
                        if (chunk.x < stats.lowestChunkXNoOutliers) {
                            stats.lowestChunkXNoOutliers = chunk.x;
                        }
                        if (chunk.x > stats.highestChunkXNoOutliers) {
                            stats.highestChunkXNoOutliers = chunk.x;
                        }
                        if (chunk.y < stats.lowestChunkZNoOutliers) {
                            stats.lowestChunkZNoOutliers = chunk.y;
                        }
                        if (chunk.y > stats.highestChunkZNoOutliers) {
                            stats.highestChunkZNoOutliers = chunk.y;
                        }
                    });
                } else {
                    stats.lowestChunkXNoOutliers = stats.lowestChunkX;
                    stats.highestChunkXNoOutliers = stats.highestChunkX;
                    stats.lowestChunkZNoOutliers = stats.lowestChunkZ;
                    stats.highestChunkZNoOutliers = stats.highestChunkZ;
                }
                
                progressReceiver.setProgress(1.0f);
                return stats;
            }
        }, true);
        if ((mapStatistics != null) && (mapStatistics.chunkCount > 0)) {
            int width = mapStatistics.highestChunkXNoOutliers - mapStatistics.lowestChunkXNoOutliers + 1;
            int length = mapStatistics.highestChunkZNoOutliers - mapStatistics.lowestChunkZNoOutliers + 1;
            int area = (mapStatistics.chunkCount - mapStatistics.outlyingChunks.size());
            labelWidth.setText(FORMATTER.format(width * 16) + " blocks (from " + FORMATTER.format(mapStatistics.lowestChunkXNoOutliers << 4) + " to " + FORMATTER.format((mapStatistics.highestChunkXNoOutliers << 4) + 15) + "; " + FORMATTER.format(width) + " chunks)");
            labelLength.setText(FORMATTER.format(length * 16) + " blocks (from " + FORMATTER.format(mapStatistics.lowestChunkZNoOutliers << 4) + " to " + FORMATTER.format((mapStatistics.highestChunkZNoOutliers << 4) + 15) + "; " + FORMATTER.format(length) + " chunks)");
            labelArea.setText(FORMATTER.format(area * 256L) + " blocks² (" + FORMATTER.format(area) + " chunks)");
            if (! mapStatistics.outlyingChunks.isEmpty()) {
                // There are outlying chunks
                int widthWithOutliers = mapStatistics.highestChunkX - mapStatistics.lowestChunkX + 1;
                int lengthWithOutliers = mapStatistics.highestChunkZ - mapStatistics.lowestChunkZ + 1;
                int areaOfOutliers = mapStatistics.outlyingChunks.size();
                labelOutliers1.setVisible(true);
                labelOutliers2.setVisible(true);
                labelWidthWithOutliers.setText(FORMATTER.format(widthWithOutliers * 16) + " blocks (" + FORMATTER.format(widthWithOutliers) + " chunks)");
                labelWidthWithOutliers.setVisible(true);
                labelOutliers3.setVisible(true);
                labelLengthWithOutliers.setText(FORMATTER.format(lengthWithOutliers * 16) + " blocks (" + FORMATTER.format(lengthWithOutliers) + " chunks)");
                labelLengthWithOutliers.setVisible(true);
                labelOutliers4.setVisible(true);
                labelAreaOutliers.setText(FORMATTER.format(areaOfOutliers * 256L) + " blocks² (" + FORMATTER.format(areaOfOutliers) + " chunks)");
                labelAreaOutliers.setVisible(true);
                checkBoxImportOutliers.setVisible(true);
                // The dialog may need to become bigger:
                pack();
            }
        }
    }
    
    private void setControlStates() {
        String fileStr = fieldFilename.getText().trim();
        File file = (! fileStr.isEmpty()) ? new File(fileStr) : null;
        if ((mapStatistics == null) || (mapStatistics.chunkCount == 0) || (file == null) || (! file.isFile())) {
            buttonOK.setEnabled(false);
        } else {
            buttonOK.setEnabled(true);
        }
    }
    
    private void resetStats() {
        labelWidth.setText("0 blocks (from ? to ?; 0 chunks)");
        labelLength.setText("0 blocks (from ? to ?; 0 chunks)");
        labelArea.setText("0 blocks² (0 chunks)");

        labelOutliers1.setVisible(false);
        labelOutliers2.setVisible(false);
        labelWidthWithOutliers.setVisible(false);
        labelOutliers3.setVisible(false);
        labelLengthWithOutliers.setVisible(false);
        labelOutliers4.setVisible(false);
        labelAreaOutliers.setVisible(false);
        checkBoxImportOutliers.setSelected(false);
        checkBoxImportOutliers.setVisible(false);
    }
    
    private void selectFile() {
        File mySavesDir = Configuration.getInstance().getSavesDirectory();
        if ((mySavesDir == null) && (MinecraftUtil.findMinecraftDir() != null)) {
            mySavesDir = new File(MinecraftUtil.findMinecraftDir(), "saves");
        }
        File selectedFile = FileUtils.selectFileForOpen(this, "Select Minecraft map level.dat file", mySavesDir, new FileFilter() {
            @Override
            public boolean accept(File f) {
                return f.isDirectory() || f.getName().equalsIgnoreCase("level.dat");
            }

            @Override
            public String getDescription() {
                return strings.getString("minecraft.level.dat.file");
            }
        });
        if (selectedFile != null) {
            fieldFilename.setText(selectedFile.getAbsolutePath());
        }        
    }
    
    private void importWorld() {
        final File levelDatFile = new File(fieldFilename.getText());
        final Set<Point> chunksToSkip = checkBoxImportOutliers.isSelected() ? null : mapStatistics.outlyingChunks;
        final JavaMapImporter.ReadOnlyOption readOnlyOption;
        if (radioButtonReadOnlyAll.isSelected()) {
            readOnlyOption = JavaMapImporter.ReadOnlyOption.ALL;
        } else if (radioButtonReadOnlyManMade.isSelected()) {
            readOnlyOption = JavaMapImporter.ReadOnlyOption.MAN_MADE;
        } else if (radioButtonReadOnlyManMadeAboveGround.isSelected()) {
            readOnlyOption = JavaMapImporter.ReadOnlyOption.MAN_MADE_ABOVE_GROUND;
        } else {
            readOnlyOption = JavaMapImporter.ReadOnlyOption.NONE;
        }
        app.setWorld(null);
        importedWorld = ProgressDialog.executeTask(this, new ProgressTask<World2>() {
            @Override
            public String getName() {
                return strings.getString("importing.world");
            }

            @Override
            public World2 execute(ProgressReceiver progressReceiver) throws OperationCancelled {
                try {
                    Level level = Level.load(levelDatFile);
                    int maxHeight = level.getMaxHeight();
                    int waterLevel;
                    if (level.getVersion() == SUPPORTED_VERSION_1) {
                        waterLevel = maxHeight / 2 - 2;
                    } else {
                        waterLevel = 62;
                    }
                    int terrainLevel = waterLevel - 4;
                    TileFactory tileFactory = TileFactoryFactory.createNoiseTileFactory(0, Terrain.GRASS, maxHeight, terrainLevel, waterLevel, false, true, 20, 1.0);
                    Set<Integer> dimensionsToImport = new HashSet<>(3);
                    dimensionsToImport.add(Constants.DIM_NORMAL);
                    if (checkBoxImportNether.isSelected()) {
                        dimensionsToImport.add(Constants.DIM_NETHER);
                    }
                    if (checkBoxImportEnd.isSelected()) {
                        dimensionsToImport.add(Constants.DIM_END);
                    }
                    final JavaMapImporter importer = new JavaMapImporter(tileFactory, levelDatFile, false, chunksToSkip, readOnlyOption, dimensionsToImport);
                    World2 world = importer.doImport(progressReceiver);
                    if (importer.getWarnings() != null) {
                        try {
                            SwingUtilities.invokeAndWait(() -> {
                                Icon warningIcon = UIManager.getIcon("OptionPane.warningIcon");
                                Toolkit.getDefaultToolkit().beep();
                                int selectedOption = JOptionPane.showOptionDialog(MapImportDialog.this, strings.getString("the.import.process.generated.warnings"), strings.getString("import.warnings"), JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, warningIcon, new Object[] {strings.getString("review.warnings"), strings.getString("ok")}, null);
                                if (selectedOption == 0) {
                                    ImportWarningsDialog warningsDialog = new ImportWarningsDialog(MapImportDialog.this, strings.getString("import.warnings"));
                                    warningsDialog.setWarnings(importer.getWarnings());
                                    warningsDialog.setVisible(true);
                                }
                            });
                        } catch (InterruptedException | InvocationTargetException e) {
                            throw new RuntimeException(e);
                        }
                    }
                    return world;
                } catch (IOException e) {
                    throw new RuntimeException("I/O error while importing world", e);
                }
            }

        }, true);
        if (importedWorld == null) {
            // The import was cancelled
            dispose();
            return;
        }
        
        importedWorld.setDirty(false);
        Configuration config = Configuration.getInstance();
        config.setSavesDirectory(levelDatFile.getParentFile().getParentFile());
        cancelled = false;
        dispose();
    }
    
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        buttonGroup1 = new javax.swing.ButtonGroup();
        jLabel1 = new javax.swing.JLabel();
        fieldFilename = new javax.swing.JTextField();
        buttonSelectFile = new javax.swing.JButton();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        labelWidth = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        labelLength = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        labelArea = new javax.swing.JLabel();
        labelOutliers3 = new javax.swing.JLabel();
        labelOutliers2 = new javax.swing.JLabel();
        labelOutliers1 = new javax.swing.JLabel();
        labelWidthWithOutliers = new javax.swing.JLabel();
        labelLengthWithOutliers = new javax.swing.JLabel();
        labelOutliers4 = new javax.swing.JLabel();
        labelAreaOutliers = new javax.swing.JLabel();
        buttonCancel = new javax.swing.JButton();
        buttonOK = new javax.swing.JButton();
        checkBoxImportOutliers = new javax.swing.JCheckBox();
        jLabel5 = new javax.swing.JLabel();
        radioButtonReadOnlyNone = new javax.swing.JRadioButton();
        radioButtonReadOnlyManMade = new javax.swing.JRadioButton();
        radioButtonReadOnlyAll = new javax.swing.JRadioButton();
        radioButtonReadOnlyManMadeAboveGround = new javax.swing.JRadioButton();
        checkBoxImportSurface = new javax.swing.JCheckBox();
        checkBoxImportNether = new javax.swing.JCheckBox();
        checkBoxImportEnd = new javax.swing.JCheckBox();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Import Existing Minecraft Map");

        jLabel1.setText("Select the level.dat file of an existing Minecraft map:");

        buttonSelectFile.setText("...");
        buttonSelectFile.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonSelectFileActionPerformed(evt);
            }
        });

        jLabel2.setText("Statistics for surface:");

        jLabel3.setText("Width:");

        labelWidth.setText("0");

        jLabel4.setText("Length:");

        labelLength.setText("0");

        jLabel7.setText("Area:");

        labelArea.setText("0");

        labelOutliers3.setText("Length including outliers:");

        labelOutliers2.setText("Width including outliers:");

        labelOutliers1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/pepsoft/worldpainter/icons/error.png"))); // NOI18N
        labelOutliers1.setText("This map has rogue outlying chunks!");

        labelWidthWithOutliers.setText("0");

        labelLengthWithOutliers.setText("0");

        labelOutliers4.setText("Area of outlying chunks:");

        labelAreaOutliers.setText("0");

        buttonCancel.setText("Cancel");
        buttonCancel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonCancelActionPerformed(evt);
            }
        });

        buttonOK.setText("OK");
        buttonOK.setEnabled(false);
        buttonOK.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonOKActionPerformed(evt);
            }
        });

        checkBoxImportOutliers.setText("include outlying chunks in import");

        jLabel5.setText("Options:");

        buttonGroup1.add(radioButtonReadOnlyNone);
        radioButtonReadOnlyNone.setText("do not mark any chunks read-only");

        buttonGroup1.add(radioButtonReadOnlyManMade);
        radioButtonReadOnlyManMade.setText("mark chunks containing man-made blocks read-only");

        buttonGroup1.add(radioButtonReadOnlyAll);
        radioButtonReadOnlyAll.setText("mark all chunks read-only");

        buttonGroup1.add(radioButtonReadOnlyManMadeAboveGround);
        radioButtonReadOnlyManMadeAboveGround.setSelected(true);
        radioButtonReadOnlyManMadeAboveGround.setText("<html>mark chunks containing man-made blocks <i>above ground</i> read-only</html>");

        checkBoxImportSurface.setSelected(true);
        checkBoxImportSurface.setText("Import Surface");
        checkBoxImportSurface.setEnabled(false);

        checkBoxImportNether.setText("Import Nether");
        checkBoxImportNether.setEnabled(false);

        checkBoxImportEnd.setText("Import End");
        checkBoxImportEnd.setEnabled(false);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(fieldFilename)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(buttonSelectFile))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(buttonOK)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(buttonCancel))
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(radioButtonReadOnlyManMadeAboveGround, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(radioButtonReadOnlyAll)
                            .addComponent(radioButtonReadOnlyManMade)
                            .addComponent(radioButtonReadOnlyNone)
                            .addComponent(jLabel1)
                            .addGroup(layout.createSequentialGroup()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(layout.createSequentialGroup()
                                        .addComponent(jLabel4)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(labelLength))
                                    .addGroup(layout.createSequentialGroup()
                                        .addComponent(jLabel3)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(labelWidth))
                                    .addGroup(layout.createSequentialGroup()
                                        .addComponent(jLabel7)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(labelArea)))
                                .addGap(18, 18, 18)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(layout.createSequentialGroup()
                                        .addComponent(labelOutliers4)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(labelAreaOutliers))
                                    .addGroup(layout.createSequentialGroup()
                                        .addComponent(labelOutliers2)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(labelWidthWithOutliers))
                                    .addGroup(layout.createSequentialGroup()
                                        .addComponent(labelOutliers3)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(labelLengthWithOutliers))
                                    .addComponent(checkBoxImportOutliers)))
                            .addComponent(jLabel5)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jLabel2)
                                .addGap(18, 18, 18)
                                .addComponent(labelOutliers1))
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(checkBoxImportSurface)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(checkBoxImportNether)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(checkBoxImportEnd)))
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
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(labelOutliers1))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3)
                    .addComponent(labelWidth)
                    .addComponent(labelOutliers2)
                    .addComponent(labelWidthWithOutliers))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel4)
                    .addComponent(labelLength)
                    .addComponent(labelOutliers3)
                    .addComponent(labelLengthWithOutliers))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel7)
                    .addComponent(labelArea)
                    .addComponent(labelOutliers4)
                    .addComponent(labelAreaOutliers))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(checkBoxImportOutliers)
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(checkBoxImportSurface)
                    .addComponent(checkBoxImportNether)
                    .addComponent(checkBoxImportEnd))
                .addGap(18, 18, 18)
                .addComponent(jLabel5)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(radioButtonReadOnlyNone)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(radioButtonReadOnlyManMadeAboveGround, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(radioButtonReadOnlyManMade)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(radioButtonReadOnlyAll)
                .addGap(18, 18, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(buttonCancel)
                    .addComponent(buttonOK))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void buttonSelectFileActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonSelectFileActionPerformed
        selectFile();
    }//GEN-LAST:event_buttonSelectFileActionPerformed

    private void buttonOKActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonOKActionPerformed
        importWorld();
    }//GEN-LAST:event_buttonOKActionPerformed

    private void buttonCancelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonCancelActionPerformed
        dispose();
    }//GEN-LAST:event_buttonCancelActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton buttonCancel;
    private javax.swing.ButtonGroup buttonGroup1;
    private javax.swing.JButton buttonOK;
    private javax.swing.JButton buttonSelectFile;
    private javax.swing.JCheckBox checkBoxImportEnd;
    private javax.swing.JCheckBox checkBoxImportNether;
    private javax.swing.JCheckBox checkBoxImportOutliers;
    private javax.swing.JCheckBox checkBoxImportSurface;
    private javax.swing.JTextField fieldFilename;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel labelArea;
    private javax.swing.JLabel labelAreaOutliers;
    private javax.swing.JLabel labelLength;
    private javax.swing.JLabel labelLengthWithOutliers;
    private javax.swing.JLabel labelOutliers1;
    private javax.swing.JLabel labelOutliers2;
    private javax.swing.JLabel labelOutliers3;
    private javax.swing.JLabel labelOutliers4;
    private javax.swing.JLabel labelWidth;
    private javax.swing.JLabel labelWidthWithOutliers;
    private javax.swing.JRadioButton radioButtonReadOnlyAll;
    private javax.swing.JRadioButton radioButtonReadOnlyManMade;
    private javax.swing.JRadioButton radioButtonReadOnlyManMadeAboveGround;
    private javax.swing.JRadioButton radioButtonReadOnlyNone;
    // End of variables declaration//GEN-END:variables

    private final App app;
    private File previouslySelectedFile;
    private MapStatistics mapStatistics;
    private boolean cancelled = true;
    private World2 importedWorld;
    
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(MapImportDialog.class);
    private static final ResourceBundle strings = ResourceBundle.getBundle("org.pepsoft.worldpainter.resources.strings"); // NOI18N
    private static final NumberFormat FORMATTER = NumberFormat.getIntegerInstance();
    private static final long serialVersionUID = 1L;
    
    static class MapStatistics {
        int lowestChunkX = Integer.MAX_VALUE, lowestChunkZ = Integer.MAX_VALUE, highestChunkX = Integer.MIN_VALUE, highestChunkZ = Integer.MIN_VALUE;
        int lowestChunkXNoOutliers = Integer.MAX_VALUE, lowestChunkZNoOutliers = Integer.MAX_VALUE, highestChunkXNoOutliers = Integer.MIN_VALUE, highestChunkZNoOutliers = Integer.MIN_VALUE;
        int chunkCount;
        final Set<Point> outlyingChunks = new HashSet<>();
        String errorMessage;
    }
}