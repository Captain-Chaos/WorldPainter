package org.pepsoft.util;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;

import static java.awt.RenderingHints.*;
import static java.awt.Transparency.*;
import static java.awt.image.BufferedImage.*;

/**
 * Created by pepijn on 13-Jan-17.
 */
public class GUIUtils {
    /**
     * Scale an image according to {@link #UI_SCALE}. Nearest neighbour scaling
     * is used, in other words no smoothing or interpolation is applied.
     *
     * @param image The image to scale.
     * @return The original image if <code>UI_SCALE</code> is 1, or an
     * appropriately scaled copy otherwise.
     */
    public static BufferedImage scaleToUI(BufferedImage image) {
        if (UI_SCALE == 1) {
            return image;
        } else {
            BufferedImageOp op = new AffineTransformOp(AffineTransform.getScaleInstance(UI_SCALE, UI_SCALE), AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
            return op.filter(image, null);
        }
    }

    /**
     * How many times to scale pixel sizes to display at approximately the
     * originally intended size for assets which were designed for 92-96 dpi
     * screens.
     *
     * <p>Currently this is set to 2 for reported dpis higher than 120, 1
     * otherwise.
     *
     * <p><strong>Note:</strong> for now UI scaling is only activated on
     * Windows, until the current support on Mac and Linux can be investigated.
     */
    public static final int UI_SCALE = (SystemUtils.isWindows() && Toolkit.getDefaultToolkit().getScreenResolution() > 120) ? 2 : 1;
}
