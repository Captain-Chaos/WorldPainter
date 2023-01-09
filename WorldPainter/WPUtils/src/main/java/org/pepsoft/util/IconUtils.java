/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.util;

import org.jetbrains.annotations.NonNls;
import org.pepsoft.util.mdc.MDCCapturingRuntimeException;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.io.IOException;
import java.net.URL;

import static java.awt.RenderingHints.KEY_ANTIALIASING;
import static java.awt.RenderingHints.VALUE_ANTIALIAS_ON;
import static java.awt.Transparency.TRANSLUCENT;
import static java.awt.geom.AffineTransform.getRotateInstance;
import static java.awt.image.AffineTransformOp.TYPE_BICUBIC;
import static java.lang.Math.toRadians;
import static org.pepsoft.util.GUIUtils.getUIScaleInt;
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

    public static String getTheme() {
        return theme;
    }

    public static void setTheme(String theme) {
        IconUtils.theme = theme;
    }

    /**
     * Load an icon from the classpath using the system class loader.
     *
     * <p>The icon will automatically be scaled up for HiDPI displays.
     *
     * @param path The path of the image to load.
     * @return The specified icon, or {@code null} if the specified path
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
     * @return The specified icon, or {@code null} if the specified path
     *     did not contain a resource.
     */
    public static ImageIcon loadScaledIcon(ClassLoader classLoader, String path) {
        BufferedImage image = loadScaledImage(classLoader, path);
        return (image != null) ? new ImageIcon(image) : null;
    }

    /**
     * Load an image from the classpath using the system class loader. If {@code theme} is set it will first look for a
     * themed version of the image by appending {@code _<theme>} to the filename.
     *
     * <p>The image will be returned at its original resolution and not be rescaled.
     *
     * @param path The path of the image to load.
     * @return The specified image, or {@code null} if the specified path did not contain a resource.
     * @see #setTheme(String)
     */
    public static BufferedImage loadUnscaledImage(String path) {
        try {
            path = path.startsWith("/") ? path.substring(1) : path;
            if (theme != null) {
                final String themedPath = path.substring(0, path.lastIndexOf('.')) + '_' + theme + path.substring(path.lastIndexOf('.'));
                final URL url = ClassLoader.getSystemResource(themedPath);
                if (url != null) {
                    return ImageIO.read(url);
                }
            }
            final URL url = ClassLoader.getSystemResource(path);
            if (url != null) {
                return ImageIO.read(url);
            } else {
                return null;
            }
        } catch (IOException e) {
            throw new MDCCapturingRuntimeException("I/O error loading image " + path, e);
        }
    }

    /**
     * Load an image from the classpath using the system class loader.
     *
     * <p>The image will automatically be scaled up for HiDPI displays.
     *
     * @param path The path of the image to load.
     * @return The specified image, or {@code null} if the specified path
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
     * Load an image from the classpath using a specific class loader. If {@code theme} is set it will first look for a
     * themed version of the image by appending {@code _<theme>} to the filename.
     *
     * <p>The image will automatically be scaled up for HiDPI displays.
     *
     * @param classLoader The class loader to use to load the image.
     * @param path The path of the image to load.
     * @return The specified image, or {@code null} if the specified path did not contain a resource.
     * @see #setTheme(String)
     */
    public static BufferedImage loadScaledImage(ClassLoader classLoader, String path) {
        try {
            if (theme != null) {
                final String themedPath = path.substring(0, path.lastIndexOf('.')) + '_' + theme + path.substring(path.lastIndexOf('.'));
                final URL url = classLoader.getResource(themedPath);
                if (url != null) {
                    return scaleToUI(ImageIO.read(url));
                }
            }
            final URL url = classLoader.getResource(path);
            if (url != null) {
                return scaleToUI(ImageIO.read(url));
            } else {
                return null;
            }
        } catch (IOException e) {
            throw new MDCCapturingRuntimeException("I/O error loading image " + path, e);
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
        final int size = 16 * getUIScaleInt();
        final BufferedImage image = newBufferedImage(size);
        for (int x = 1; x < size - 1; x++) {
            for (int y = 1; y < size - 1; y++) {
                image.setRGB(x, y, colour);
            }
        }
        return new ImageIcon(image);
    }

    public static Icon createScaledLetterIcon(char letter, Color colour) {
        final int size = 16 * getUIScaleInt();
        final BufferedImage image = newBufferedImage(size);
        final Graphics2D g2 = image.createGraphics();
        try {
            g2.setFont(Font.decode("SansSerif-BOLD").deriveFont(16.0f * getUIScaleInt()));
            g2.setColor(colour);
            g2.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);
            g2.drawString(String.valueOf(letter), 1 + 2 * getUIScaleInt(), size - 1 - 2 * getUIScaleInt());
        } finally {
            g2.dispose();
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
        return (icon != null) ? new ImageIcon(scaleIcon(icon.getImage(), size)) : null;
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
        final int scaledSize = size * getUIScaleInt();
        if ((iconImage instanceof BufferedImage) && (((BufferedImage) iconImage).getWidth() == scaledSize)) {
            return (BufferedImage) iconImage;
        }
        BufferedImage newImage = newBufferedImage(scaledSize);
        Graphics2D g2 = newImage.createGraphics();
        try {
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g2.drawImage(iconImage, 0, 0, scaledSize, scaledSize, null);
        } finally {
            g2.dispose();
        }
        return newImage;
    }

    /**
     * Rotate a square icon clockwise by the specified number of degrees.
     *
     * @param icon    The icon to rotate.
     * @param degrees The number of degrees to rotate the icon.
     * @return The rotated icon.
     */
    public static Icon rotateIcon(Icon icon, int degrees) {
        if (degrees == 0) {
            return icon;
        }
        final BufferedImage bufferedImage;
        if (icon instanceof ImageIcon) {
            final Image image = ((ImageIcon) icon).getImage();
            if (image instanceof BufferedImage) {
                bufferedImage = (BufferedImage) image;
            } else {
                bufferedImage = newBufferedImage(image.getWidth(null));
                final Graphics2D g2 = bufferedImage.createGraphics();
                try {
                    g2.drawImage(image, 0, 0, null);
                } finally {
                    g2.dispose();
                }
            }
        } else {
            bufferedImage = newBufferedImage(icon.getIconWidth());
            final Graphics2D g2 = bufferedImage.createGraphics();
            try {
                icon.paintIcon(null, g2, 0, 0);
            } finally {
                g2.dispose();
            }
        }
        final BufferedImageOp op = new AffineTransformOp(getRotateInstance(toRadians(degrees), icon.getIconWidth() / 2.0, icon.getIconHeight() / 2.0), TYPE_BICUBIC);
        final BufferedImage targetImage = newBufferedImage(icon.getIconWidth());
        return new ImageIcon(op.filter(bufferedImage, targetImage));
    }

    private static BufferedImage newBufferedImage(int size) {
        return GraphicsEnvironment.isHeadless()
                ? new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB)
                : GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration().createCompatibleImage(size, size, TRANSLUCENT);
    }

    private static volatile String theme;
}