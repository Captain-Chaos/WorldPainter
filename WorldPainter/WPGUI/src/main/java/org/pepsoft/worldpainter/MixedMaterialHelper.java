/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.worldpainter;

import org.pepsoft.util.DesktopUtils;
import org.pepsoft.util.FileUtils;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.io.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

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
        });
        if (selectedFile != null) {
            try {
                try (ObjectInputStream in = new ObjectInputStream(new GZIPInputStream(new BufferedInputStream(new FileInputStream(selectedFile))))) {
                    return MixedMaterial.duplicateNewMaterialsWhile(() -> (MixedMaterial) in.readObject());
                }
            } catch (IOException e) {
                throw new RuntimeException("I/O error while reading " + selectedFile, e);
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
        });
        if (selectedFiles != null) {
            return MixedMaterial.duplicateNewMaterialsWhile(() -> {
                MixedMaterial[] materials = new MixedMaterial[selectedFiles.length];
                for (int i = 0; i < selectedFiles.length; i++) {
                    try {
                        try (ObjectInputStream in = new ObjectInputStream(new GZIPInputStream(new BufferedInputStream(new FileInputStream(selectedFiles[i]))))) {
                            materials[i] = (MixedMaterial) in.readObject();
                        }
                    } catch (IOException e) {
                        throw new RuntimeException("I/O error while reading " + selectedFiles[i], e);
                    } catch (ClassNotFoundException e) {
                        throw new RuntimeException("Class not found exception while reading " + selectedFiles[i], e);
                    }
                }
                return materials;
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
        File selectedFile = new File(terrainDirectory, FileUtils.sanitiseName(material.getName()) + ".terrain");
        selectedFile = FileUtils.selectFileForSave(SwingUtilities.getWindowAncestor(parent), "Export as WorldPainter custom terrain file", selectedFile, new FileFilter() {
            @Override
            public boolean accept(File f) {
                return f.isDirectory() || f.getName().toLowerCase().endsWith(".terrain");
            }

            @Override
            public String getDescription() {
                return "WorldPainter Custom Terrains (*.terrain)";
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
            JOptionPane.showMessageDialog(parent, "Custom terrain " + material.getName() + " exported successfully", "Success", JOptionPane.INFORMATION_MESSAGE);
        }
    }
}
