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
        JFileChooser fileChooser = new JFileChooser(terrainDirectory);
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.setFileFilter(new FileFilter() {
            @Override
            public boolean accept(File f) {
                return f.isDirectory() || f.getName().toLowerCase().endsWith(".terrain");
            }

            @Override
            public String getDescription() {
                return "WorldPainter Custom Terrains (*.terrain)";
            }
        });
        if (fileChooser.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            try {
                ObjectInputStream in = new ObjectInputStream(new GZIPInputStream(new BufferedInputStream(new FileInputStream(selectedFile))));
                try {
                    return (MixedMaterial) in.readObject();
                } finally {
                    in.close();
                }
            } catch (IOException e) {
                throw new RuntimeException("I/O error while reading " + selectedFile, e);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException("Class not found exception while reading " + selectedFile, e);
            }
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
        JFileChooser fileChooser = new JFileChooser(terrainDirectory);
        fileChooser.setSelectedFile(selectedFile);
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.setFileFilter(new FileFilter() {
            @Override
            public boolean accept(File f) {
                return f.isDirectory() || f.getName().toLowerCase().endsWith(".terrain");
            }

            @Override
            public String getDescription() {
                return "WorldPainter Custom Terrains (*.terrain)";
            }
        });
        if (fileChooser.showSaveDialog(parent) == JFileChooser.APPROVE_OPTION) {
            selectedFile = fileChooser.getSelectedFile();
            if (! selectedFile.getName().toLowerCase().endsWith(".terrain")) {
                selectedFile = new File(selectedFile.getPath() + ".terrain");
            }
            if (selectedFile.isFile() && (JOptionPane.showConfirmDialog(parent, "The file " + selectedFile.getName() + " already exists.\nDo you want to overwrite it?", "Overwrite File", JOptionPane.YES_NO_OPTION) == JOptionPane.NO_OPTION)) {
                return;
            }
            try {
                ObjectOutputStream out = new ObjectOutputStream(new GZIPOutputStream(new BufferedOutputStream(new FileOutputStream(selectedFile))));
                try {
                    out.writeObject(material);
                } finally {
                    out.close();
                }
            } catch (IOException e) {
                throw new RuntimeException("I/O error while trying to write " + selectedFile, e);
            }
            config.setTerrainDirectory(selectedFile.getParentFile());
            JOptionPane.showMessageDialog(parent, "Custom terrain " + material.getName() + " exported successfully", "Success", JOptionPane.INFORMATION_MESSAGE);
        }
    }
}
