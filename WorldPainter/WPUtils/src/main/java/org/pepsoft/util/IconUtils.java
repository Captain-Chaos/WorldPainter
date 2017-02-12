/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.util;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import javax.imageio.ImageIO;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import org.jetbrains.annotations.NonNls;

import static org.pepsoft.util.GUIUtils.UI_SCALE;
import static org.pepsoft.util.GUIUtils.scaleToUI;

/**
 * Utility methods for loading and scaling images and icons, with automatic
 * support for HiDPI displays.
 *
 * @author pepijn
 */
@NonNls
public final class IconUtils {
    private IconUtils() {
        // Prevent instantiation
    }

    /**
     * Load an icon from the classpath using the system class loader.
     *
     * <p>The icon will automatically be scaled up for HiDPI displays.
     *
     * @param path The path of the image to load.
     * @return The specified icon, or <code>null</code> if the specified path
     *     did not contain a resource.
     */
    public static ImageIcon loadScaledIcon(String path) {
        BufferedImage image = loadScaledImage(path);
        return (image != null) ? new ImageIcon(image) : null;
    }
    
    /**
     * Load an icon from the classpath using a specific class loader.
     *
     * <p>The icon will automatically be scaled up for HiDPI displays.
     *
     * @param classLoader The class loader to use to load the image.
     * @param path The path of the image to load.
     * @return The specified icon, or <code>null</code> if the specified path
     *     did not contain a resource.
     */
    public static ImageIcon loadScaledIcon(ClassLoader classLoader, String path) {
        BufferedImage image = loadScaledImage(classLoader, path);
        return (image != null) ? new ImageIcon(image) : null;
    }

    /**
     * Load an image from the classpath using the system class loader.
     *
     * <p>The image will be returned at its original resolution and not be
     * rescaled.
     *
     * @param path The path of the image to load.
     * @return The specified image, or <code>null</code> if the specified path
     *     did not contain a resource.
     */
    public static BufferedImage loadUnscaledImage(String path) {
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

    /**
     * Load an image from the classpath using the system class loader.
     *
     * <p>The image will automatically be scaled up for HiDPI displays.
     *
     * @param path The path of the image to load.
     * @return The specified image, or <code>null</code> if the specified path
     *     did not contain a resource.
     */
    public static BufferedImage loadScaledImage(String path) {
        BufferedImage image = loadUnscaledImage(path);
        if (image != null) {
            return scaleToUI(image);
        } else {
            return null;
        }
    }
    
    /**
     * Load an image from the classpath using a specific class loader.
     *
     * <p>The image will automatically be scaled up for HiDPI displays.
     *
     * @param classLoader The class loader to use to load the image.
     * @param path The path of the image to load.
     * @return The specified image, or <code>null</code> if the specified path
     *     did not contain a resource.
     */
    public static BufferedImage loadScaledImage(ClassLoader classLoader, String path) {
        try {
            URL url = classLoader.getResource(path);
            if (url != null) {
                return scaleToUI(ImageIO.read(url));
            } else {
                return null;
            }
        } catch (IOException e) {
            throw new RuntimeException("I/O error loading image " + path, e);
        }
    }

    /**
     * Create a 16x16 pixel icon of a solid colour.
     *
     * <p>The icon will automatically be scaled up for HiDPI displays.
     *
     * @param colour The colour as a combined rgb value.
     * @return A 16x16 icon of the specified colour.
     */
    public static Icon createScaledColourIcon(int colour) {
        BufferedImage image = new BufferedImage(16 * UI_SCALE, 16 * UI_SCALE, BufferedImage.TYPE_INT_RGB);
        for (int x = 1; x < 16 * UI_SCALE - 1; x++) {
            for (int y = 1; y < 16 * UI_SCALE - 1; y++) {
                image.setRGB(x, y, colour);
            }
        }
        return new ImageIcon(image);
    }

    /**
     * Scale a square icon using bicubic scaling.
     *
     * <p>The icon will automatically be scaled up for HiDPI displays.
     *
     * @param icon The icon to scale.
     * @param size The size (edge to edge) of the scaled icon.
     * @return The scaled icon.
     */
    public static ImageIcon scaleIcon(ImageIcon icon, int size) {
        return new ImageIcon(scaleIcon(icon.getImage(), size));
    }

    /**
     * Scale a square icon using bicubic scaling.
     *
     * <p>The icon will automatically be scaled up for HiDPI displays.
     *
     * @param iconImage The icon to scale.
     * @param size The size (edge to edge) of the scaled icon.
     * @return The scaled icon.
     */
    public static BufferedImage scaleIcon(Image iconImage, int size) {
        BufferedImage newImage = new BufferedImage(size * UI_SCALE, size * UI_SCALE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = newImage.createGraphics();
        try {
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g2.drawImage(iconImage, 0, 0, size * UI_SCALE, size * UI_SCALE, null);
        } finally {
            g2.dispose();
        }
        return newImage;
    }
}