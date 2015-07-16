/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.minecraft.mapexplorer;

import org.pepsoft.util.FileUtils;
import org.pepsoft.worldpainter.util.MinecraftUtil;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.tree.TreeModel;
import java.awt.*;
import java.io.File;

/**
 *
 * @author pepijn
 */
public class MapExplorer {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            File file = FileUtils.selectFileForOpen(null, "Select Minecraft map level.dat file", new File(MinecraftUtil.findMinecraftDir(), "saves"), new FileFilter() {
                @Override
                public boolean accept(File f) {
                    return f.isDirectory() || f.getName().equals("level.dat") || f.getName().toLowerCase().endsWith(".schematic");
                }

                @Override
                public String getDescription() {
                    return "Minecraft level.dat files or MCEdit .schematic files";
                }
            });
            if (file != null) {
                JFrame frame = new JFrame("Minecraft Map Explorer");
                frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                TreeModel treeModel = new MapTreeModel(file);
                JTree tree = new JTree(treeModel);
                tree.setCellRenderer(new MapTreeCellRenderer());
                JScrollPane scrollPane = new JScrollPane(tree);
                frame.getContentPane().add(scrollPane, BorderLayout.CENTER);
                frame.setSize(1024, 768);
                frame.setLocationRelativeTo(null);
                frame.setVisible(true);
            }
        });
    }
}