package org.pepsoft.worldpainter.util;

import java.awt.*;
import java.awt.image.BufferedImage;

import static java.awt.Font.BOLD;

public class PreviewUtils {
    /**
     * Paint height markers on the right edge of an image, starting with minHeight at the bottom.
     */
    public static void paintHeightMarks(final BufferedImage image, final int minHeight) {
        final int width = image.getWidth(), height = image.getHeight();
        final Graphics2D g2 = image.createGraphics();
        try {
            g2.setColor(Color.GRAY);
            g2.setFont(HEIGHT_MARKER_FONT);
            for (int y = (minHeight / 20) * 20; y < (height + minHeight); y += 20) {
                g2.drawLine(width - 10, height + minHeight - y, width - 1, height + minHeight - y);
                g2.drawString(Integer.toString(y), width - 30, height + minHeight - y + 4);
            }
        } finally {
            g2.dispose();
        }
    }

    /**
     * Paint height markers on the right edge of an image, starting with minHeight at the bottom.
     */
    public static void paintFiftyPercent(final BufferedImage image) {
        final int width = image.getWidth(), height = image.getHeight(), x = width / 2;
        final Graphics2D g2 = image.createGraphics();
        try {
            g2.setColor(Color.GRAY);
            g2.setFont(HEIGHT_MARKER_FONT);
            g2.drawLine(x, height - 10, x, height - 1);
            g2.drawString("50%", x - 10, height - 14);
        } finally {
            g2.dispose();
        }
    }

    private static final Font HEIGHT_MARKER_FONT = new Font("SansSerif", BOLD, 10); // TODO UI scaling (but then also scale the pixels)
}