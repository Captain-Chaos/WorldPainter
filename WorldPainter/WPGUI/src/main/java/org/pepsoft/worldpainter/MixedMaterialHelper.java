/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.worldpainter;

import org.pepsoft.util.DesktopUtils;
import org.pepsoft.worldpainter.util.FileFilter;
import org.pepsoft.worldpainter.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import static org.pepsoft.util.swing.MessageUtils.beepAndShowError;
import static org.pepsoft.util.swing.MessageUtils.showInfo;

/**
 *
 * @author Pepijn Schmitz
 */
public class MixedMaterialHelper {
    private MixedMaterialHelper() {
        // Prevent instantiation
    }
    
    public static MixedMaterial load(Component parent) {
        Configuration config = Configuration.getInstance();
        File terrainDirectory = config.getTerrainDirectory();
        if ((terrainDirectory == null) || (! terrainDirectory.isDirectory())) {
            terrainDirectory = DesktopUtils.getDocumentsFolder();
        }
        File selectedFile = FileUtils.selectFileForOpen(SwingUtilities.getWindowAncestor(parent), "Select WorldPainter custom terrain file", terrainDirectory, new FileFilter() {
            @Override
            public boolean accept(File f) {
                return f.isDirectory() || f.getName().toLowerCase().endsWith(".terrain");
            }

            @Override
            public String getDescription() {
                return "WorldPainter Custom Terrains (*.terrain)";
            }

            @Override
            public String getExtensions() {
                return "*.terrain";
            }
        });
        if (selectedFile != null) {
            try {
                try (ObjectInputStream in = new ObjectInputStream(new GZIPInputStream(new BufferedInputStream(new FileInputStream(selectedFile))))) {
                    return MixedMaterial.duplicateNewMaterialsWhile(() -> (MixedMaterial) in.readObject());
                }
            } catch (IOException e) {
                logger.error("{} while reading {}", e.getClass().getSimpleName(), selectedFile, e);
                beepAndShowError(parent, "An input error occurred while reading the file (message: " + e.getMessage() + ")", "Input Error");
            } catch (ClassCastException e) {
                logger.error("{} while reading {}", e.getClass().getSimpleName(), selectedFile, e);
                beepAndShowError(parent, "The selected file is not a valid WorldPainter custom terrain file", "Invalid File");
            }
        }
        return null;
    }

    public static MixedMaterial[] loadMultiple(Component parent) {
        Configuration config = Configuration.getInstance();
        File terrainDirectory = config.getTerrainDirectory();
        if ((terrainDirectory == null) || (! terrainDirectory.isDirectory())) {
            terrainDirectory = DesktopUtils.getDocumentsFolder();
        }
        File[] selectedFiles = FileUtils.selectFilesForOpen(SwingUtilities.getWindowAncestor(parent), "Select WorldPainter custom terrain file(s)", terrainDirectory, new FileFilter() {
            @Override
            public boolean accept(File f) {
                return f.isDirectory() || f.getName().toLowerCase().endsWith(".terrain");
            }

            @Override
            public String getDescription() {
                return "WorldPainter Custom Terrains (*.terrain)";
            }

            @Override
            public String getExtensions() {
                return "*.terrain";
            }
        });
        if (selectedFiles != null) {
            return MixedMaterial.duplicateNewMaterialsWhile(() -> {
                final List<MixedMaterial> materials = new ArrayList<>(selectedFiles.length);
                for (File selectedFile: selectedFiles) {
                    try {
                        try (ObjectInputStream in = new ObjectInputStream(new GZIPInputStream(new BufferedInputStream(new FileInputStream(selectedFile))))) {
                            materials.add((MixedMaterial) in.readObject());
                        }
                    } catch (IOException e) {
                        logger.error("{} while reading {}", e.getClass().getSimpleName(), selectedFile, e);
                        beepAndShowError(parent, "An input error occurred while reading " + selectedFile + " (message: " + e.getMessage() + ")", "Input Error");
                    } catch (ClassCastException e) {
                        logger.error("{} while reading {}", e.getClass().getSimpleName(), selectedFile, e);
                        beepAndShowError(parent, selectedFile + " is not a valid WorldPainter custom terrain file", "Invalid File");
                    }
                }
                return (! materials.isEmpty()) ? materials.toArray(new MixedMaterial[materials.size()]) : null;
            });
        }
        return null;
    }
    
    public static void save(Component parent, MixedMaterial material) {
        Configuration config = Configuration.getInstance();
        File terrainDirectory = config.getTerrainDirectory();
        if ((terrainDirectory == null) || (! terrainDirectory.isDirectory())) {
            terrainDirectory = DesktopUtils.getDocumentsFolder();
        }
        File selectedFile = new File(terrainDirectory, org.pepsoft.util.FileUtils.sanitiseName(material.getName()) + ".terrain");
        selectedFile = FileUtils.selectFileForSave(SwingUtilities.getWindowAncestor(parent), "Export as WorldPainter custom terrain file", selectedFile, new FileFilter() {
            @Override
            public boolean accept(File f) {
                return f.isDirectory() || f.getName().toLowerCase().endsWith(".terrain");
            }

            @Override
            public String getDescription() {
                return "WorldPainter Custom Terrains (*.terrain)";
            }

            @Override
            public String getExtensions() {
                return "*.terrain";
            }
        });
        if (selectedFile != null) {
            if (! selectedFile.getName().toLowerCase().endsWith(".terrain")) {
                selectedFile = new File(selectedFile.getPath() + ".terrain");
            }
            if (selectedFile.isFile() && (JOptionPane.showConfirmDialog(parent, "The file " + selectedFile.getName() + " already exists.\nDo you want to overwrite it?", "Overwrite File", JOptionPane.YES_NO_OPTION) == JOptionPane.NO_OPTION)) {
                return;
            }
            try {
                try (ObjectOutputStream out = new ObjectOutputStream(new GZIPOutputStream(new BufferedOutputStream(new FileOutputStream(selectedFile))))) {
                    out.writeObject(material);
                }
            } catch (IOException e) {
                throw new RuntimeException("I/O error while trying to write " + selectedFile, e);
            }
            config.setTerrainDirectory(selectedFile.getParentFile());
            showInfo(parent, "Custom terrain " + material.getName() + " exported successfully", "Success");
        }
    }

    private static final Logger logger = LoggerFactory.getLogger(MixedMaterialHelper.class);
}