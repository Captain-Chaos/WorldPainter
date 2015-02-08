/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.minecraft.mapexplorer;

import java.awt.BorderLayout;
import java.io.File;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileFilter;
import javax.swing.tree.TreeModel;
import org.pepsoft.worldpainter.util.MinecraftUtil;

/**
 *
 * @author pepijn
 */
public class MapExplorer {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setFileFilter(new FileFilter() {
                    @Override
                    public boolean accept(File f) {
                        return f.isDirectory() || f.getName().equals("level.dat") || f.getName().toLowerCase().endsWith(".schematic");
                    }

                    @Override
                    public String getDescription() {
                        return "Minecraft level.dat files or MCEdit .schematic files";
                    }
                });
                fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
                fileChooser.setCurrentDirectory(new File(MinecraftUtil.findMinecraftDir(), "saves"));
                if (fileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                    File file = fileChooser.getSelectedFile();
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
            }
        });
    }
}