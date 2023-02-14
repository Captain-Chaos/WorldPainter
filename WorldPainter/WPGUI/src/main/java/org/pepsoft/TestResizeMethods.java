package org.pepsoft;

import com.twelvemonkeys.image.ResampleOp;

import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.io.File;
import java.io.IOException;

import static com.twelvemonkeys.image.ResampleOp.FILTER_POINT;
import static com.twelvemonkeys.image.ResampleOp.FILTER_QUADRATIC;
import static java.awt.RenderingHints.*;
import static java.awt.image.AffineTransformOp.TYPE_BICUBIC;
import static java.awt.image.AffineTransformOp.TYPE_NEAREST_NEIGHBOR;
import static java.lang.Math.round;

public class TestResizeMethods {
    public static void main(String[] args) throws IOException {
        while (true) {
            for (String arg: args) {
                final BufferedImage image = ImageIO.read(new File(arg));
                System.out.printf("%s (%dx%d; %s)%n", arg, image.getWidth(), image.getHeight(), image);
                doTest(image, 2.0f, 10, 0, false);
                doTest(image, 2.0f, 10, 0, true);
                doTest(image, 2.0f, 10, 1, false);
                doTest(image, 2.0f, 10, 1, true);
                doTest(image, 2.0f, 10, 2, false);
                doTest(image, 2.0f, 10, 2, true);
                doTest(image, 2.0f, 10, 3, false);
                doTest(image, 2.0f, 10, 3, true);
                doTest(image, 2.0f, 10, 4, false);
                doTest(image, 2.0f, 10, 4, true);
                doTest(image, 1.333333f, 10, 0, false);
                doTest(image, 1.333333f, 10, 0, true);
                doTest(image, 1.333333f, 10, 1, false);
                doTest(image, 1.333333f, 10, 1, true);
                doTest(image, 1.333333f, 10, 2, false);
                doTest(image, 1.333333f, 10, 2, true);
                doTest(image, 1.333333f, 10, 3, false);
                doTest(image, 1.333333f, 10, 3, true);
                doTest(image, 1.333333f, 10, 4, false);
                doTest(image, 1.333333f, 10, 4, true);
            }
        }
    }

    public static void main1(String[] args) throws IOException {
        for (String arg: args) {
            final File file = new File(arg);
            final BufferedImage image = ImageIO.read(file);
            final String name = file.getName();
            System.out.printf("%s (%dx%d; %s)%n", arg, image.getWidth(), image.getHeight(), image);
            dump(doTest(image, 2.0f, 1, 0, false), 2.0f, name, 0, false);
            dump(doTest(image, 2.0f, 1, 0, true), 2.0f, name, 0, true);
            dump(doTest(image, 2.0f, 1, 1, false), 2.0f, name, 1, false);
            dump(doTest(image, 2.0f, 1, 1, true), 2.0f, name, 1, true);
            dump(doTest(image, 2.0f, 1, 2, false), 2.0f, name, 2, false);
            dump(doTest(image, 2.0f, 1, 2, true), 2.0f, name, 2, true);
            dump(doTest(image, 2.0f, 1, 3, false), 2.0f, name, 3, false);
            dump(doTest(image, 2.0f, 1, 3, true), 2.0f, name, 3, true);
            dump(doTest(image, 2.0f, 1, 4, false), 2.0f, name, 4, false);
            dump(doTest(image, 2.0f, 1, 4, true), 2.0f, name, 4, true);
            dump(doTest(image, 1.333333f, 1, 0, false), 1.333333f, name, 0, false);
            dump(doTest(image, 1.333333f, 1, 0, true), 1.333333f, name, 0, true);
            dump(doTest(image, 1.333333f, 1, 1, false), 1.333333f, name, 1, false);
            dump(doTest(image, 1.333333f, 1, 1, true), 1.333333f, name, 1, true);
            dump(doTest(image, 1.333333f, 1, 2, false), 1.333333f, name, 2, false);
            dump(doTest(image, 1.333333f, 1, 2, true), 1.333333f, name, 2, true);
            dump(doTest(image, 1.333333f, 1, 3, false), 1.333333f, name, 3, false);
            dump(doTest(image, 1.333333f, 1, 3, true), 1.333333f, name, 3, true);
            dump(doTest(image, 1.333333f, 1, 4, false), 1.333333f, name, 4, false);
            dump(doTest(image, 1.333333f, 1, 4, true), 1.333333f, name, 4, true);
        }
    }

    private static BufferedImage doTest(BufferedImage image, float scale, int count, int method, boolean smooth) {
        try {
            final long start = System.currentTimeMillis();
            BufferedImage result = null;
            for (int i = 0; i < count; i++) {
                switch (method) {
                    case 0:
                        result = resizeWithGraphics1(image, scale, smooth);
                        break;
                    case 1:
                        result = resizeWithGraphics2(image, scale, smooth);
                        break;
                    case 2:
                        result = resizeWithGraphics3(image, scale, smooth);
                        break;
                    case 3:
                        result = resizeWithAffineTransform(image, scale, smooth);
                        break;
                    case 4:
                        result = resizeWithResample(image, scale, smooth);
                        break;
                    default:
                        throw new InternalError();
                }
                // Check results
                if (result.getType() != image.getType()) {
                    int type = result.getType();
                    int type1 = image.getType();
                    System.err.println("Type changed from " + TYPE_NAMES[type1] + " to " + TYPE_NAMES[type]);
                }
                if (result.getTransparency() != image.getTransparency()) {
                    int transparency = result.getTransparency();
                    int transparency1 = image.getTransparency();
                    System.err.println("Transparency changed from " + TRANSPARENCY_NAMES[transparency1] + " to " + TRANSPARENCY_NAMES[transparency]);
                }
                if (round(result.getWidth() / scale) != image.getWidth()) {
                    System.err.println("Scaled width " + result.getWidth() + " is not " + scale + " times " + image.getWidth());
                }
                if (round(result.getHeight() / scale) != image.getHeight()) {
                    System.err.println("Scaled height " + result.getHeight() + " is not " + scale + " times " + image.getHeight());
                }
                if (result.getSampleModel().getNumBands() != image.getSampleModel().getNumBands()) {
                    System.err.println("Number of bands changed from " + image.getSampleModel().getNumBands() + " to " + result.getSampleModel().getNumBands());
                }
                for (int j = 0; j < image.getSampleModel().getNumBands(); j++) {
                    if (result.getSampleModel().getSampleSize(j) != image.getSampleModel().getSampleSize(j)) {
                        System.err.println("Sample size of band " + j + " changed from " + image.getSampleModel().getSampleSize(j) + " to " + result.getSampleModel().getSampleSize(j));
                    }
                }
            }
            final long duration = System.currentTimeMillis() - start;
            System.out.printf("Scaling %d images %f times with method %d (smooth: %b) took %d ms (%d ms per image)%n", count, scale, method, smooth, duration, duration / count);
            System.out.printf("Adjusted for scale: %f ms per image%n", (float) duration / count / (scale * scale));
            return result;
        } catch (RuntimeException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static BufferedImage resizeWithGraphics1(BufferedImage image, float scale, boolean smooth) {
        final int scaledWidth = round(image.getWidth() * scale), scaledHeight = round(image.getHeight() * scale);
        final BufferedImage scaledImage = ImageTypeSpecifier.createFromRenderedImage(image).createBufferedImage(scaledWidth, scaledHeight);
        final Graphics2D g2 = scaledImage.createGraphics();
        try {
            g2.setRenderingHint(KEY_INTERPOLATION, smooth ? VALUE_INTERPOLATION_BICUBIC : VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            g2.drawImage(image, 0, 0, scaledWidth, scaledHeight, null);
        } finally {
            g2.dispose();
        }
        return scaledImage;
    }

    private static BufferedImage resizeWithGraphics2(BufferedImage image, float scale, boolean smooth) {
        final int scaledWidth = round(image.getWidth() * scale), scaledHeight = round(image.getHeight() * scale);
        final BufferedImageOp op = new AffineTransformOp(AffineTransform.getScaleInstance(scale, scale), smooth ? TYPE_BICUBIC : TYPE_NEAREST_NEIGHBOR);
        final BufferedImage scaledImage = ImageTypeSpecifier.createFromRenderedImage(image).createBufferedImage(scaledWidth, scaledHeight);
        final Graphics2D g2 = scaledImage.createGraphics();
        try {
            g2.drawImage(image, op, 0, 0);
        } finally {
            g2.dispose();
        }
        return scaledImage;
    }

    private static BufferedImage resizeWithGraphics3(BufferedImage image, float scale, boolean smooth) {
        final int scaledWidth = round(image.getWidth() * scale), scaledHeight = round(image.getHeight() * scale);
        final AffineTransform transform = AffineTransform.getScaleInstance(scale, scale);
        final BufferedImage scaledImage = ImageTypeSpecifier.createFromRenderedImage(image).createBufferedImage(scaledWidth, scaledHeight);
        final Graphics2D g2 = scaledImage.createGraphics();
        try {
            g2.setRenderingHint(KEY_INTERPOLATION, smooth ? VALUE_INTERPOLATION_BICUBIC : VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            g2.drawImage(image, transform, null);
        } finally {
            g2.dispose();
        }
        return scaledImage;
    }

    private static BufferedImage resizeWithAffineTransform(BufferedImage image, float scale, boolean smooth) {
        return new AffineTransformOp(AffineTransform.getScaleInstance(scale, scale), smooth ? TYPE_BICUBIC : TYPE_NEAREST_NEIGHBOR).filter(image, null);
    }

    private static BufferedImage resizeWithResample(BufferedImage image, float scale, boolean smooth) {
        return new ResampleOp(round(image.getWidth() * scale), round(image.getHeight() * scale), smooth ? FILTER_QUADRATIC : FILTER_POINT).filter(image, null);
    }

    private static void dump(BufferedImage image, float scale, String name, int method, boolean smooth) throws IOException {
        if (image == null) {
            return;
        }
        ImageIO.write(image, "PNG", new File(name + "-" + method + "-" + scale + (smooth ? "-smooth" : "") + ".png"));
    }

    private static final String[] TYPE_NAMES = {"CUSTOM", "INT_RGB", "INT_ARGB", "INT_ARGB_PRE", "INT_BGR", "3BYTE_BGR", "4BYTE_ABGR", "4BYTE_ABGR_PRE", "USHORT_565_RGB", "USHORT_555_RGB", "BYTE_GRAY", "USHORT_GRAY", "BYTE_BINARY", "BYTE_INDEXED"};
    private static final String[] TRANSPARENCY_NAMES = {null, "OPAQUE", "BITMASK", "TRANSLUCENT"};
}