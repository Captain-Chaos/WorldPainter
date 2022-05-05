/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.tools;

import org.pepsoft.minecraft.MinecraftMapTileProvider;
import org.pepsoft.util.swing.TileProvider;
import org.pepsoft.util.swing.TiledImageViewer;
import org.pepsoft.worldpainter.AbstractTool;
import org.pepsoft.worldpainter.plugins.PlatformProvider;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;

import static org.pepsoft.worldpainter.util.MapUtils.selectMap;

/**
 *
 * @author pepijn
 */
public class MapViewer extends AbstractTool {
    public static void main(String[] args) throws IOException {
        initialisePlatform();

        PlatformProvider.MapInfo map = selectMap(null, null);
        if (map != null) {
            TileProvider tileProvider = new MinecraftMapTileProvider(map.dir);

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