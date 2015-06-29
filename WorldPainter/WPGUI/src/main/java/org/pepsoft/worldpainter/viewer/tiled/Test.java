/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.viewer.tiled;

import org.pepsoft.worldpainter.*;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.biomeschemes.AutoBiomeScheme;
import org.pepsoft.worldpainter.biomeschemes.CustomBiomeManager;
import org.pepsoft.worldpainter.colourschemes.DynMapColourScheme;
import org.pepsoft.worldpainter.viewer.Layer;
import org.pepsoft.worldpainter.viewer.LayerViewer;

import javax.swing.*;
import java.awt.*;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.zip.GZIPInputStream;

/**
 *
 * @author pepijn
 */
public class Test {
    public static void main(String[] args) throws IOException, ClassNotFoundException {
        final World2 world;
        try (ObjectInputStream in = new ObjectInputStream(new GZIPInputStream(new FileInputStream(args[0])))) {
            world = (World2) in.readObject();
        }
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("TiledLayerView Test");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            LayerViewer layerViewer = new LayerViewer();
            frame.getContentPane().add(layerViewer, BorderLayout.CENTER);

            final Dimension dimension = world.getDimension(Constants.DIM_NORMAL);
            Layer layer = new Layer() {
                @Override
                public String getName() {
                    return "Tiles";
                }

                @Override
                public String getDescription() {
                    return "WorldPainter tiles from world";
                }

                @Override
                public Rectangle getBounds() {
                    return new Rectangle(dimension.getLowestX() << Constants.TILE_SIZE_BITS,
                            dimension.getLowestY() << Constants.TILE_SIZE_BITS,
                            dimension.getWidth() << Constants.TILE_SIZE_BITS,
                            dimension.getHeight() << Constants.TILE_SIZE_BITS);
                }
            };
            final ColourScheme colourScheme = new DynMapColourScheme("default", true);
            final BiomeScheme biomeScheme = new AutoBiomeScheme(dimension);
            final CustomBiomeManager customBiomeManager = new CustomBiomeManager();
            TileRendererFactory tileRendererFactory = () -> new TileRenderer(dimension, colourScheme, biomeScheme, customBiomeManager, 0);
            TiledLayerView layerView = new TiledLayerView(layer, dimension, tileRendererFactory);
            layerViewer.addLayer(layerView, new Point(0, 0));

            frame.setSize(1024, 768);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            layerViewer.start();
        });
    }
}
