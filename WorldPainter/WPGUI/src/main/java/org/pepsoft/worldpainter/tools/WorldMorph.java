/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.worldpainter.tools;

import java.awt.BorderLayout;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import org.pepsoft.minecraft.Constants;
import org.pepsoft.worldpainter.*;
import org.pepsoft.worldpainter.colourschemes.DynMapColourScheme;

/**
 *
 * @author pepijn
 */
public class WorldMorph {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            final WorldPainter view = new WorldPainter(createNewWorld().getDimension(0), new DynMapColourScheme("default", true), null, null);
            JFrame frame = new JFrame("WorldMorph");
            frame.getContentPane().add(view, BorderLayout.CENTER);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
            Timer timer = new Timer(2000, e -> view.setDimension(createNewWorld().getDimension(0)));
            timer.start();
        });
    }

    private static World2 createNewWorld() {
        long seed = System.currentTimeMillis();
        TileFactory tileFactory = TileFactoryFactory.createNoiseTileFactory(seed, Terrain.GRASS, Constants.DEFAULT_MAX_HEIGHT_MCREGION, 16, 24, false, true, 20, 1.0);
        World2 world = new World2(DefaultPlugin.JAVA_MCREGION, seed, tileFactory, World2.DEFAULT_MAX_HEIGHT);
        Dimension dim0 = world.getDimension(0);
        for (int x = -2; x <= 2; x++) {
            for (int y = -2; y <= 2; y++) {
                dim0.addTile(tileFactory.createTile(x, y));
            }
        }
        return world;
    }
}
