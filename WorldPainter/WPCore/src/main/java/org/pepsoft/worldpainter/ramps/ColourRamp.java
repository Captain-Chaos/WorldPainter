package org.pepsoft.worldpainter.ramps;

import java.awt.*;
import java.awt.image.BufferedImage;

import static java.awt.image.BufferedImage.TYPE_INT_RGB;

public interface ColourRamp {
    /**
     * Get the colour to use for a value {@code n}.
     *
     * @param n The value for which to get the colour.
     * @return The colour to use for the specified value.
     */
    int getColour(float n);

    /**
     * Get a preview image of this colour ramp for the range [0.0, 1.0].
     *
     * @param width  The width of the preview image.
     * @param height The height of the preview image.
     * @return The requested preview image.
     */
    default BufferedImage getPreview(int width, int height) {
        return getPreview(width, height, 0.0f, 1.0f);
    }

    /**
     * Get a preview image of this colour ramp.
     *
     * @param width  The width of the preview image.
     * @param height The height of the preview image.
     * @param min    The start of the range of values to plot.
     * @param max    The end of the range of values to plot.
     * @return The requested preview image.
     */
    default BufferedImage getPreview(int width, int height, float min, float max) {
        final BufferedImage image = new BufferedImage(width, height, TYPE_INT_RGB);
        final Graphics2D g2 = image.createGraphics();
        try {
            if (width > height) {
                for (int x = 0; x < width; x++) {
                    final int colour = getColour((float) x / (width - 1) * (max - min) + min);
                    g2.setColor(new Color(colour));
                    g2.drawLine(x, 0, x, height - 1);
                }
            } else {
                for (int y = 0; y < height; y++) {
                    final int colour = getColour((float) (height - y - 1) / (width - 1) * (max - min) + min);
                    g2.setColor(new Color(colour));
                    g2.drawLine(0, y, width - 1, y);
                }
            }
        } finally {
            g2.dispose();
        }
        return image;
    }
}