package org.pepsoft.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.plaf.FontUIResource;
import javax.swing.plaf.InsetsUIResource;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.util.Map;

import static java.awt.Image.SCALE_SMOOTH;
import static java.awt.RenderingHints.*;
import static java.awt.image.AffineTransformOp.TYPE_BICUBIC;
import static java.awt.image.AffineTransformOp.TYPE_NEAREST_NEIGHBOR;
import static java.awt.image.BufferedImage.TYPE_INT_ARGB;
import static java.lang.Math.round;

/**
 * Created by pepijn on 13-Jan-17.
 */
public class GUIUtils {
    /**
     * Override the detected system default UI scale.
     *
     * @param uiScale The UI scale to use instead of the detected system
     *                default.
     */
    public static void setUIScale(float uiScale) {
        UI_SCALE_FLOAT = uiScale;
        UI_SCALE = round(uiScale);
    }

    /**
     * Scale an image according to {@link #UI_SCALE}. Nearest neighbour scaling
     * is used, in other words no smoothing or interpolation is applied.
     *
     * @param image The image to scale.
     * @return The original image if {@code UI_SCALE} is 1, or an
     * appropriately scaled copy otherwise.
     */
    public static BufferedImage scaleToUI(Image image) {
        return scaleToUI(image, false);
    }

    /**
     * Scale an image according to {@link #UI_SCALE}.
     *
     * @param image The image to scale.
     * @param smooth Whether to do smooth scaling. When this is {@code true},
     *               the image is scaled to the floating point UI scale using
     *               bicubic scaling. When it is {@code false}, it is scaled to
     *               the UI scale rounded to the nearest integer, using nearest
     *               neighbour scaling.
     * @return The original image if {@code UI_SCALE} is 1, or an
     * appropriately scaled copy otherwise.
     */
    public static BufferedImage scaleToUI(Image image, boolean smooth) {
        if (((UI_SCALE_FLOAT == 1.0f) || ((! smooth) && (UI_SCALE == 1))) && (image instanceof BufferedImage)) {
            return (BufferedImage) image;
        } else if (image instanceof BufferedImage) {
            BufferedImageOp op;
            if (smooth) {
                op = new AffineTransformOp(AffineTransform.getScaleInstance(getUIScale(), getUIScale()), TYPE_BICUBIC);
            } else {
                op = new AffineTransformOp(AffineTransform.getScaleInstance(getUIScaleInt(), getUIScaleInt()), TYPE_NEAREST_NEIGHBOR);
            }
            return op.filter((BufferedImage) image, null);
        } else {
            BufferedImage scaledImage;
            if (smooth) {
                scaledImage = new BufferedImage(round(image.getWidth(null) * getUIScale()), round(image.getHeight(null) * getUIScale()), TYPE_INT_ARGB);
                Graphics2D g2 = scaledImage.createGraphics();
                try {
                    g2.setRenderingHint(KEY_INTERPOLATION, VALUE_INTERPOLATION_BICUBIC);
                    g2.drawImage(image, AffineTransform.getScaleInstance(getUIScale(), getUIScale()), null);
                } finally {
                    g2.dispose();
                }
            } else {
                scaledImage = new BufferedImage(image.getWidth(null) * getUIScaleInt(), image.getHeight(null) * getUIScaleInt(), TYPE_INT_ARGB);
                Graphics2D g2 = scaledImage.createGraphics();
                try {
                    g2.setRenderingHint(KEY_INTERPOLATION, VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                    g2.drawImage(image, AffineTransform.getScaleInstance(getUIScaleInt(), getUIScaleInt()), null);
                } finally {
                    g2.dispose();
                }
            }
            return scaledImage;
        }
    }

    public static void scaleToUI(Container container) {
        if (UI_SCALE_FLOAT == 1.0f) {
            return;
        }
        if (container instanceof Window) {
            Window window = (Window) container;
            window.setSize((int) (window.getWidth() * UI_SCALE_FLOAT + 0.5f), (int) (window.getHeight() * UI_SCALE_FLOAT + 0.5f));
        }
        for (Component component: container.getComponents()) {
            if (component instanceof JTable) {
                JTable table = (JTable) component;
                table.setRowHeight((int) (table.getRowHeight() * UI_SCALE_FLOAT + 0.5f));
                table.setRowMargin((int) (table.getRowMargin() * UI_SCALE_FLOAT + 0.5f));
            } else if (component instanceof JTextArea) {
                component.setFont(UIManager.getFont("TextField.font"));
            } else if (component instanceof Container) {
                scaleToUI((Container) component);
            }
        }
    }

    /**
     * Adjusts the {@link UIManager} defaults to show the Java 2D UI at the
     * specified scale.
     */
    public static void scaleLookAndFeel(float scale) {
        if (scale == 1.0f) {
            return;
        }
        for (Map.Entry<Object, Object> entry: UIManager.getDefaults().entrySet()) {
            Object key = entry.getKey();
            Object value = UIManager.get(key);
            if (value instanceof FontUIResource) {
                FontUIResource previousResource = (FontUIResource) value;
                FontUIResource newResource = new FontUIResource(previousResource.getFamily(), previousResource.getStyle(), (int) (previousResource.getSize() * scale + 0.5f));
                UIManager.put(key, newResource);
                logger.debug("Scaled FontUIResource {}", key);
            } else if (value instanceof InsetsUIResource) {
                InsetsUIResource oldResource = (InsetsUIResource) value;
                InsetsUIResource newResource = new InsetsUIResource((int) (oldResource.top * scale + 0.5f),
                        (int) (oldResource.left * scale + 0.5f),
                        (int) (oldResource.bottom * scale + 0.5f),
                        (int) (oldResource.right * scale + 0.5f));
                UIManager.put(key, newResource);
                logger.debug("Scaled InsetsUIResource {}", key);
            } else if (value instanceof Insets) {
                Insets oldResource = (Insets) value;
                Insets newResource = new Insets((int) (oldResource.top * scale + 0.5f),
                        (int) (oldResource.left * scale + 0.5f),
                        (int) (oldResource.bottom * scale + 0.5f),
                        (int) (oldResource.right * scale + 0.5f));
                UIManager.put(key, newResource);
                logger.debug("Scaled Insets {}", key);
            } else if ((value instanceof Integer) && (key instanceof String) &&
                    (((String) key).toLowerCase().contains("margin")
                            || ((String) key).toLowerCase().contains("thickness")
                            || ((String) key).toLowerCase().contains("gap")
                            || ((String) key).toLowerCase().contains("width")
                            || ((String) key).toLowerCase().contains("height")
                            || ((String) key).toLowerCase().contains("spacing")
                            || ((String) key).toLowerCase().contains("size")
                            || ((String) key).toLowerCase().contains("length")
                            || ((String) key).toLowerCase().contains("offset")
                            || ((String) key).toLowerCase().contains("shift")
                            || ((String) key).toLowerCase().contains("indent")
                            || ((String) key).toLowerCase().contains("padding"))) {
                int oldValue = (Integer) value;
                int newValue = (int) (oldValue * scale + 0.5f);
                UIManager.put(key, newValue);
                logger.debug("Scaled integer {}", key);
            } else if (value instanceof ImageIcon) {
                ImageIcon icon = (ImageIcon) value;
                Image scaledImage = icon.getImage().getScaledInstance((int) (icon.getIconWidth() * scale + 0.5f + 0.5f), -1, SCALE_SMOOTH);
                UIManager.put(key, new ImageIcon(scaledImage));
                logger.debug("Scaled ImageIcon {}", key);
            } else if (value instanceof Icon) {
                Icon icon = (Icon) value;
                BufferedImage image = new BufferedImage(icon.getIconWidth(), icon.getIconHeight(), TYPE_INT_ARGB);
                Graphics2D g2 = image.createGraphics();
                try {
                    icon.paintIcon(null, g2, 0, 0);
                    Image scaledImage = image.getScaledInstance((int) (icon.getIconWidth() * scale + 0.5f + 0.5f), -1, SCALE_SMOOTH);
                    UIManager.put(key, new ImageIcon(scaledImage));
                    logger.debug("Scaled Icon {}", key);
                } catch (NullPointerException e) {
                    logger.debug("Did NOT scale Icon {} due to NullPointerException", key);
                }
            } else if ((value instanceof Color) || (value instanceof Boolean)) {
                // Ignore silently
            } else {
                logger.debug("Did NOT scale {}: {}} ({}})", key, value, (value != null) ? value.getClass().getSimpleName() : "null");
            }
        }
    }

    /**
     * The detected system default UI scale.
     *
     * <p><strong>Note:</strong> for now UI scaling is only activated on <!-- TODO -->
     * Windows, until the current support on Mac and Linux can be investigated. <!-- TODO -->
     */
    public static final float SYSTEM_UI_SCALE_FLOAT = SystemUtils.isWindows() ? MathUtils.clamp(1.0f, (float) Toolkit.getDefaultToolkit().getScreenResolution() / 96, 2.0f) : 1.0f;

    /**
     * How many times to scale pixel sizes to display at approximately the
     * originally intended size for assets which were designed for 92-96 dpi
     * screens.
     *
     * <p>This is {@link #UI_SCALE_FLOAT} rounded to the nearest integer and is
     * intended for small images that don't scale well to non integer factors
     * such as icons.
     *
     * <p><strong>Note:</strong> for now UI scaling is only activated on <!-- TODO -->
     * Windows, until the current support on Mac and Linux can be investigated. <!-- TODO -->
     */
    public static final int SYSTEM_UI_SCALE = round(SYSTEM_UI_SCALE_FLOAT);

    /**
     * How many times to scale pixel sizes to display at approximately the
     * originally intended size for assets which were designed for 96 dpi
     * screens.
     *
     * <p><strong>Note:</strong> for now UI scaling is only activated on <!-- TODO -->
     * Windows, until the current support on Mac and Linux can be investigated. <!-- TODO -->
     */
    public static float getUIScale() {
        return UI_SCALE_FLOAT;
    }

    /**
     * How many times to scale pixel sizes to display at approximately the
     * originally intended size for assets which were designed for 92-96 dpi
     * screens.
     *
     * <p>This is {@link #getUIScale()} rounded to the nearest integer and is
     * intended for small images that don't scale well to non integer factors
     * such as icons.
     *
     * <p><strong>Note:</strong> for now UI scaling is only activated on <!-- TODO -->
     * Windows, until the current support on Mac and Linux can be investigated. <!-- TODO -->
     */
    public static int getUIScaleInt() {
        return UI_SCALE;
    }

    private static float UI_SCALE_FLOAT = SYSTEM_UI_SCALE_FLOAT;
    private static int UI_SCALE = SYSTEM_UI_SCALE;

    private static final Logger logger = LoggerFactory.getLogger(GUIUtils.class);
}