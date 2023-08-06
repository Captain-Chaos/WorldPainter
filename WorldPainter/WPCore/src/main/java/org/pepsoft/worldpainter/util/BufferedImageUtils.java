package org.pepsoft.worldpainter.util;

import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;

import static java.awt.image.BufferedImage.TYPE_INT_ARGB;

public class BufferedImageUtils {
    public static BufferedImage createColourSquare(int size, int colour) {
        final BufferedImage image = new BufferedImage(size, size, TYPE_INT_ARGB);
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                image.setRGB(x, y, 0xff000000 | colour);
            }
        }
        return image;
    }

    public static BufferedImage clone(BufferedImage image) {
        final ColorModel cm = image.getColorModel();
        return new BufferedImage(cm, image.copyData(image.getRaster().createCompatibleWritableRaster()), cm.isAlphaPremultiplied(), null);
    }
}