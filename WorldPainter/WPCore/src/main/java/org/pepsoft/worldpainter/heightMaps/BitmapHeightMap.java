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
        bits = raster.getSampleModel().getSampleSize(0);
        final int transferType = raster.getTransferType();
        signed = (transferType == TYPE_SHORT) || (transferType == TYPE_FLOAT) || (transferType == TYPE_DOUBLE);
        minHeight = signed ? -(1 << (bits - 1)) : 0;
        maxHeight = signed ? (1 << (bits - 1)) - 1 : (1 << bits) - 1;
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

    public void setSmoothScaling(boolean smoothScaling) {
        this.smoothScaling = smoothScaling;
    }

    // HeightMap
    
    @Override
    public float getHeight(int x, int y) {
        if (repeat) {
            return getSample(MathUtils.mod(x, width), MathUtils.mod(y, height));
        } else if (extent.contains(x, y)) {
            return getSample(x, y);
        } else {
            return minHeight;
        }
    }

    @Override
    public float getHeight(float x, float y) {
        if (! smoothScaling) {
            return getHeight((int) x, (int) y);
        } else {
            // Bicubic interpolation
            x -= Math.signum(x) / 2;
            y -= Math.signum(y) / 2;
            int xFloor = (int) Math.floor(x), yFloor = (int) Math.floor(y);
            float xDelta = x - xFloor, yDelta = y - yFloor;
            float val1 = cubicInterpolate(getExtHeight(xFloor - 1, yFloor - 1), getExtHeight(xFloor - 1, yFloor), getExtHeight(xFloor - 1, yFloor + 1), getExtHeight(xFloor - 1, yFloor + 2), yDelta);
            float val2 = cubicInterpolate(getExtHeight(xFloor,     yFloor - 1), getExtHeight(xFloor,     yFloor), getExtHeight(xFloor,     yFloor + 1), getExtHeight(xFloor,     yFloor + 2), yDelta);
            float val3 = cubicInterpolate(getExtHeight(xFloor + 1, yFloor - 1), getExtHeight(xFloor + 1, yFloor), getExtHeight(xFloor + 1, yFloor + 1), getExtHeight(xFloor + 1, yFloor + 2), yDelta);
            float val4 = cubicInterpolate(getExtHeight(xFloor + 2, yFloor - 1), getExtHeight(xFloor + 2, yFloor), getExtHeight(xFloor + 2, yFloor + 1), getExtHeight(xFloor + 2, yFloor + 2), yDelta);
            return cubicInterpolate(val1, val2, val3, val4, xDelta);
        }
    }

    /**
     * Private version of {@link #getHeight(int, int)}} which extends the
     * edge pixels of the image if it is non-repeating, to make the bicubic
     * interpolation work correctly around the edges.
     */
    private float getExtHeight(int x, int y) {
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

    @Override
    public float[] getRange() {
        return new float[] { minHeight, maxHeight };
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
    private float cubicInterpolate(float y0, float y1, float y2, float y3, float μ) {
        return y1 + 0.5f * μ * (y2 - y0 + μ * (2.0f * y0 - 5.0f * y1 + 4.0f * y2 - y3 + μ * (3.0f * (y1 - y2) + y3 - y0)));
    }

    private long getSample(int x, int y) {
        return signed
                ? raster.getSample(x, y, channel)
                : raster.getSample(x, y, channel) & 0xffffffffL; // Convert to unsigned integer
    }

    private final BufferedImage image;
    private final int channel, width, height, bits;
    private final Raster raster;
    private final Rectangle extent;
    private final File imageFile;
    private final boolean repeat, signed;
    private final float minHeight, maxHeight;
    private boolean smoothScaling;

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