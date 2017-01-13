package org.pepsoft.util;

import java.awt.*;
import java.awt.image.BufferedImage;

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
            BufferedImage scaledImage = new BufferedImage(image.getWidth() * UI_SCALE, image.getHeight() * UI_SCALE, (image.getTransparency() != OPAQUE) ? TYPE_INT_ARGB : TYPE_INT_RGB);
            Graphics2D g2 = scaledImage.createGraphics();
            try {
                g2.setRenderingHint(KEY_INTERPOLATION, VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                g2.drawImage(image, 0, 0, image.getWidth() * UI_SCALE, image.getHeight() * UI_SCALE, null);
            } finally {
                g2.dispose();
            }
            return scaledImage;
        }
    }

    /**
     * How many times to scale pixel sizes to display at approximately the
     * originally intended size for assets which were designed for 92-96 dpi
     * screens.
     *
     * <p>Currently this is set to 2 for reported dpis higher than 120, 1
     * otherwise.
     */
    public static final int UI_SCALE = Toolkit.getDefaultToolkit().getScreenResolution() > 120 ? 2 : 1;
}
