/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.objects;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.vecmath.Point3i;
import org.pepsoft.minecraft.Constants;
import org.pepsoft.minecraft.Material;
import org.pepsoft.worldpainter.ColourScheme;
import org.pepsoft.worldpainter.colourschemes.DynMapColourScheme;
import org.pepsoft.worldpainter.layers.bo2.Bo2Object;

/**
 *
 * @author pepijn
 */
public class WPObjectRenderer {
    public WPObjectRenderer(WPObject object, ColourScheme colourScheme, int blockSize) {
        this.object = object;
        dim = object.getDimensions();
        this.colourScheme = colourScheme;
        this.blockSize = blockSize;
        width = getImageCoordinates(dim.x - 1, 0, 0).x + 2 * blockSize;
        height = -getImageCoordinates(dim.x - 1, dim.y - 1, dim.z - 1).y;
    }
    
    public BufferedImage render() {
        final BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        final Graphics2D g2 = image.createGraphics();
        try {
            for (int z = 0; z < dim.z; z++) {
                for (int y = dim.y - 1; y >= 0; y--) {
                    for (int x = dim.x - 1; x >= 0; x--) {
                        if (object.getMask(x, dim.y - y - 1, z)) {
                            final Material material = object.getMaterial(x, dim.y - y - 1, z);
                            if (material != Material.AIR) {
                                final Point coords = getImageCoordinates(x, y, z);
                                final int blockType = material.blockType;
                                final int alpha = ((blockType == Constants.BLK_LEAVES) || (blockType == Constants.BLK_LEAVES2)) ? 192 : 255;
                                paintBlock(g2,  coords.x, coords.y, material, alpha);
                            }
                        }
                    }
                }
            }
        } finally {
            g2.dispose();
        }
        return image;
    }

    private void paintBlock(Graphics2D g2, int x, int y, Material material, int alpha) {
        Color colour;
        if (alpha < 255) {
            colour = new Color(colourScheme.getColour(material) | (alpha << 24), true);
            g2.setColor(new Color(colour.brighter().getRGB() | (alpha << 24), true));
        } else {
            colour = new Color(colourScheme.getColour(material));
            g2.setColor(colour.brighter());
        }
        for (int dx = 0; dx < blockSize; dx++) {
            g2.drawLine(x + dx, y + blockSize + dx, x + dx, y + blockSize * 2 - 1 + dx);
        }
        if (alpha < 255) {
            g2.setColor(new Color(colour.darker().getRGB() | (alpha << 24), true));
        } else {
            g2.setColor(colour.darker());
        }
        for (int dx = 0; dx < blockSize; dx++) {
            g2.drawLine(x + blockSize * 2 - 1 - dx, y + blockSize + dx, x + blockSize * 2 - 1 - dx, y + blockSize * 2 - 1 + dx);
        }
        g2.setColor(colour);
        for (int i = 0; i < blockSize; i++) {
            g2.drawLine(x + blockSize - 1 - i, y + i, x + blockSize + i, y + i);
            if (i < (blockSize - 1)) {
                g2.drawLine(x + blockSize - 1 - i, y + (blockSize - 1) * 2 - i, x + blockSize + i, y + (blockSize - 1) * 2 - i);
            }
        }
    }
    
    private Point getImageCoordinates(int x, int y, int z) {
        return new Point(
            (x + dim.y - 1 - y) * blockSize,
            height - 1 - ((y + x + z + 3) * blockSize - 2));
    }
    
    public static void main(String[] args) throws IOException {
        WPObject object = Bo2Object.load(new File("/home/pepijn/NetBeansProjects/Minecraft/BOBPlugins/wessex_tallredwood.bo2"));
//        WPObject object = new WPObject() {
//            public Point3i getDimensions() {
//                return new Point3i(3, 3, 1);
//            }
//
//            public Point3i getOrigin() {
//                return new Point3i(1, 1, 0);
//            }
//
//            public int getBlockID(int x, int y, int z) {
////                if (x == 2 - y) {
//                    return Constants.BLK_STONE;
////                } else {
////                    return Constants.BLK_AIR;
////                }
//            }
//
//            public int getData(int x, int y, int z) {
//                return 0;
//            }
//
//            public boolean getMask(int x, int y, int z) {
////                return (x == 2 - y);
//                return true;
//            }
//        };
        ColourScheme colourScheme = new DynMapColourScheme("default", true);
        WPObjectRenderer renderer = new WPObjectRenderer(object, colourScheme, 10);
        BufferedImage image = renderer.render();
        ImageIcon icon = new ImageIcon(image);
        JLabel label = new JLabel(icon);
        JFrame frame = new JFrame("WPObjectRenderer Test");
        frame.getContentPane().add(label, BorderLayout.CENTER);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
    
    private final WPObject object;
    private final Point3i dim;
    private final int width, height;
    private final ColourScheme colourScheme;
    private final int blockSize;
}
