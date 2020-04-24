package org.pepsoft.util;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;

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
        UI_SCALE = Math.round(uiScale);
    }

    /**
     * Scale an image according to {@link #UI_SCALE}. Nearest neighbour scaling
     * is used, in other words no smoothing or interpolation is applied.
     *
     * @param image The image to scale.
     * @return The original image if {@code UI_SCALE} is 1, or an
     * appropriately scaled copy otherwise.
     */
    public static BufferedImage scaleToUI(BufferedImage image) {
        if (getUIScaleInt() == 1) {
            return image;
        } else {
            BufferedImageOp op = new AffineTransformOp(AffineTransform.getScaleInstance(getUIScaleInt(), getUIScaleInt()), AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
            return op.filter(image, null);
        }
    }

    /**
     * The detected system default UI scale.
     *
     * <p><strong>Note:</strong> for now UI scaling is only activated on <!-- TODO -->
     * Windows, until the current support on Mac and Linux can be investigated. <!-- TODO -->
     */
    public static final float SYSTEM_UI_SCALE_FLOAT = SystemUtils.isWindows() ? (float) Toolkit.getDefaultToolkit().getScreenResolution() / 96 : 1.0f;

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
    public static final int SYSTEM_UI_SCALE = Math.round(SYSTEM_UI_SCALE_FLOAT);

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
     * <p>This is {@link #UI_SCALE_FLOAT} rounded to the nearest integer and is
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
}