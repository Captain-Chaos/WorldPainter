/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.tools;

import org.pepsoft.minecraft.MinecraftMapTileProvider;
import org.pepsoft.util.FileUtils;
import org.pepsoft.util.swing.TileProvider;
import org.pepsoft.util.swing.TiledImageViewer;
import org.pepsoft.worldpainter.util.MinecraftUtil;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.io.File;
import java.io.IOException;

/**
 *
 * @author pepijn
 */
public class MapViewer {
    public static void main(String[] args) throws IOException {
        File mySavesDir = null;
        File minecraftDir = MinecraftUtil.findMinecraftDir();
        if (minecraftDir != null) {
            mySavesDir = new File(minecraftDir, "saves");
        }
        File levelDatFile = FileUtils.selectFileForOpen(null, "Select Minecraft map level.dat file", mySavesDir, new FileFilter() {
            @Override
            public boolean accept(File f) {
                return f.isDirectory() || f.getName().equalsIgnoreCase("level.dat");
            }

            @Override
            public String getDescription() {
                return "Minecraft levels (level.dat)";
            }
        });
        if (levelDatFile != null) {
            final File worldDir = levelDatFile.getParentFile();
            TileProvider tileProvider = new MinecraftMapTileProvider(worldDir);

            JFrame frame = new JFrame("Map Viewer");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            final TiledImageViewer viewer = new TiledImageViewer(true, true);
            viewer.setTileProvider(tileProvider);
            viewer.addMouseWheelListener(e -> {
                int zoom = viewer.getZoom();
                zoom -= e.getWheelRotation();
//                System.out.println("Setting zoom to " + zoom);
                viewer.setZoom(zoom);
            });
            frame.getContentPane().add(viewer, BorderLayout.CENTER);
            frame.setSize(800, 600);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        }
    }
}