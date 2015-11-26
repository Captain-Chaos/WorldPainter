/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.util;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import javax.imageio.ImageIO;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import org.jetbrains.annotations.NonNls;

/**
 *
 * @author pepijn
 */
@NonNls
public final class IconUtils {
    private IconUtils() {
        // Prevent instantiation
    }

    public static Icon loadIcon(String path) {
        BufferedImage image = loadImage(path);
        return (image != null) ? new ImageIcon(image) : null;
    }
    
    public static Icon loadIcon(ClassLoader classLoader, String path) {
        BufferedImage image = loadImage(classLoader, path);
        return (image != null) ? new ImageIcon(image) : null;
    }
    
    public static BufferedImage loadImage(String path) {
        try {
            URL url = ClassLoader.getSystemResource(path);
            if (url != null) {
                return ImageIO.read(url);
            } else {
                return null;
            }
        } catch (IOException e) {
            throw new RuntimeException("I/O error loading image " + path, e);
        }
    }
    
    public static BufferedImage loadImage(ClassLoader classLoader, String path) {
        try {
            URL url = classLoader.getResource(path);
            if (url != null) {
                return ImageIO.read(url);
            } else {
                return null;
            }
        } catch (IOException e) {
            throw new RuntimeException("I/O error loading image " + path, e);
        }
    }

    public static Icon createColourIcon(int colour) {
        BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < 16; x++) {
            for (int y = 0; y < 16; y++) {
                image.setRGB(x, y, colour);
            }
        }
        return new ImageIcon(image);
    }
}