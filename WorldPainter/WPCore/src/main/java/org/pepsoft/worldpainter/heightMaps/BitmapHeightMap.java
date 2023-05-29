/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.worldpainter.heightMaps;

import org.pepsoft.util.IconUtils;
import org.pepsoft.util.MathUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.io.File;

import static java.awt.image.DataBuffer.*;

/**
 *
 * @author SchmitzP
 */
public final class BitmapHeightMap extends AbstractHeightMap {
    private BitmapHeightMap(String name, BufferedImage image, int channel, File imageFile, boolean repeat, boolean smoothScaling) {
        super(name);
        this.image = image;
        this.channel = channel;
        this.imageFile = imageFile;
        this.repeat = repeat;
        this.smoothScaling = smoothScaling;
        raster = image.getRaster();
        width = image.getWidth();
        height = image.getHeight();
        extent = repeat ? null : new Rectangle(0, 0, width, height);
        bitDepth = raster.getSampleModel().getSampleSize(0);
        final int transferType = raster.getTransferType();
        switch (transferType) {
            case TYPE_FLOAT:
                minHeight = -Float.MAX_VALUE;
                maxHeight = Float.MAX_VALUE;
                floatingPoint = true;
                signed = true;
                break;
            case TYPE_DOUBLE:
                minHeight = -Double.MAX_VALUE;
                maxHeight = Double.MAX_VALUE;
                floatingPoint = true;
                signed = true;
                break;
            case TYPE_SHORT:
                minHeight = -(1L << (bitDepth - 1));
                maxHeight = (1L << (bitDepth - 1)) - 1L;
                floatingPoint = false;
                signed = true;
                break;
            default:
                minHeight = 0;
                maxHeight = (1L << bitDepth) - 1L;
                floatingPoint = false;
                signed = false;
                break;
        }
        hasAlpha = image.getColorModel().hasAlpha();
    }

    public BufferedImage getImage() {
        return image;
    }

    public int getChannel() {
        return channel;
    }

    public boolean isRepeat() {
        return repeat;
    }

    public boolean isSmoothScaling() {
        return smoothScaling;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    /**
     * Get the theoretical minimum value as determined by the image format, regardless of the actual minimum value.
     */
    public double getMinHeight() {
        return minHeight;
    }

    /**
     * Get the theoretical maximum value as determined by the image format, regardless of the actual maximum value.
     */
    public double getMaxHeight() {
        return maxHeight;
    }

    public boolean isFloatingPoint() {
        return floatingPoint;
    }

    public int getBitDepth() {
        return bitDepth;
    }

    public boolean hasAlpha() {
        return hasAlpha;
    }

    public boolean isSigned() {
        return signed;
    }

    // HeightMap
    
    @Override
    public double getHeight(int x, int y) {
        if (repeat) {
            return getSample(MathUtils.mod(x, width), MathUtils.mod(y, height));
        } else if (extent.contains(x, y)) {
            return getSample(x, y);
        } else {
            return minHeight;
        }
    }

    @Override
    public double getHeight(float x, float y) {
        if (! smoothScaling) {
            return getHeight((int) x, (int) y);
        } else {
            // Bicubic interpolation
            x -= Math.signum(x) / 2;
            y -= Math.signum(y) / 2;
            int xFloor = (int) Math.floor(x), yFloor = (int) Math.floor(y);
            float xDelta = x - xFloor, yDelta = y - yFloor;
            double val1 = cubicInterpolate(getExtHeight(xFloor - 1, yFloor - 1), getExtHeight(xFloor - 1, yFloor), getExtHeight(xFloor - 1, yFloor + 1), getExtHeight(xFloor - 1, yFloor + 2), yDelta);
            double val2 = cubicInterpolate(getExtHeight(xFloor,     yFloor - 1), getExtHeight(xFloor,     yFloor), getExtHeight(xFloor,     yFloor + 1), getExtHeight(xFloor,     yFloor + 2), yDelta);
            double val3 = cubicInterpolate(getExtHeight(xFloor + 1, yFloor - 1), getExtHeight(xFloor + 1, yFloor), getExtHeight(xFloor + 1, yFloor + 1), getExtHeight(xFloor + 1, yFloor + 2), yDelta);
            double val4 = cubicInterpolate(getExtHeight(xFloor + 2, yFloor - 1), getExtHeight(xFloor + 2, yFloor), getExtHeight(xFloor + 2, yFloor + 1), getExtHeight(xFloor + 2, yFloor + 2), yDelta);
            return cubicInterpolate(val1, val2, val3, val4, xDelta);
        }
    }

    /**
     * Private version of {@link #getHeight(int, int)}} which extends the
     * edge pixels of the image if it is non-repeating, to make the bicubic
     * interpolation work correctly around the edges.
     */
    private double getExtHeight(int x, int y) {
        if (repeat) {
            return getSample(MathUtils.mod(x, width), MathUtils.mod(y, height));
        } else if (extent.contains(x, y)) {
            return getSample(x, y);
        } else if (x < 0) {
            // West of the extent
            if (y < 0) {
                // Northwest of the extent
                return getSample(0, 0);
            } else if (y < height) {
                // Due west of the extent
                return getSample(0, y);
            } else {
                // Southwest of the extent
                return getSample(0, height - 1);
            }
        } else if (x < width) {
            // North or south of the extent
            if (y < 0) {
                // Due north of the extent
                return getSample(x, 0);
            } else {
                // Due south of the extent
                return getSample(x, height - 1);
            }
        } else {
            // East of the extent
            if (y < 0) {
                // Northeast of the extent
                return getSample(width - 1, 0);
            } else if (y < height) {
                // Due east of the extent
                return getSample(width - 1, y);
            } else {
                // Southeast of the extent
                return getSample(width - 1, height - 1);
            }
        }
    }

    @Override
    public Rectangle getExtent() {
        return extent;
    }

    @Override
    public int getColour(int x, int y) {
        if (repeat) {
            return image.getRGB(MathUtils.mod(x, width), MathUtils.mod(y, height));
        } else if (extent.contains(x, y)) {
            return image.getRGB(x, y);
        } else {
            return 0;
        }
    }

    @Override
    public Icon getIcon() {
        return ICON_BITMAP_HEIGHTMAP;
    }

    /**
     * Get the <em>actual</em>> minimum and maximum values contained in the image data.
     */
    @Override
    public double[] getRange() {
        if (range == null) {
            double imageLowValue = Double.MAX_VALUE, imageHighValue = -Double.MAX_VALUE;
            outer:
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    final double value = getSample(x, y);
                    if (value < imageLowValue) {
                        imageLowValue = value;
                    }
                    if (value > imageHighValue) {
                        imageHighValue = value;
                    }
                    if ((imageLowValue <= minHeight) && (imageHighValue >= maxHeight)) {
                        // No point in looking any further!
                        break outer;
                    }
                }
            }
            range = new double[] { imageLowValue, imageHighValue };
        }
        return range;
    }

    public File getImageFile() {
        return imageFile;
    }

    public static BitmapHeightMapBuilder build() {
        return new BitmapHeightMapBuilder();
    }

    /**
     * Cubic interpolation using Catmull-Rom splines.
     */
    private double cubicInterpolate(double y0, double y1, double y2, double y3, float μ) {
        return y1 + 0.5 * μ * (y2 - y0 + μ * (2.0 * y0 - 5.0 * y1 + 4.0 * y2 - y3 + μ * (3.0 * (y1 - y2) + y3 - y0)));
    }

    private double getSample(int x, int y) {
        if (floatingPoint) {
            return raster.getSampleDouble(x, y, channel);
        } else {
            return signed ? raster.getSample(x, y, channel) : (raster.getSample(x, y, channel) & 0xffffffffL);
        }
    }

    private final BufferedImage image;
    private final int channel, width, height, bitDepth;
    private final Raster raster;
    private final Rectangle extent;
    private final File imageFile;
    private final boolean repeat, smoothScaling, floatingPoint, hasAlpha, signed;
    private final double minHeight, maxHeight;
    private double[] range;

    private static final long serialVersionUID = 1L;
    private static final Icon ICON_BITMAP_HEIGHTMAP = IconUtils.loadScaledIcon("org/pepsoft/worldpainter/icons/height_map.png");

    public static class BitmapHeightMapBuilder {
        public BitmapHeightMapBuilder withName(String name) {
            this.name = name;
            return this;
        }

        public BitmapHeightMapBuilder withChannel(int channel) {
            this.channel = channel;
            return this;
        }

        public BitmapHeightMapBuilder withImage(BufferedImage image) {
            this.image = image;
            return this;
        }

        public BitmapHeightMapBuilder withFile(File file) {
            this.file = file;
            return this;
        }

        public BitmapHeightMapBuilder withRepeat(boolean repeat) {
            this.repeat = repeat;
            return this;
        }

        public BitmapHeightMapBuilder withSmoothScaling(boolean smoothScaling) {
            this.smoothScaling = smoothScaling;
            return this;
        }

        public BitmapHeightMap now() {
            return new BitmapHeightMap(name, image, channel, file, repeat, smoothScaling);
        }

        private String name;
        private BufferedImage image;
        private int channel;
        private File file;
        private boolean repeat, smoothScaling;
    }
}