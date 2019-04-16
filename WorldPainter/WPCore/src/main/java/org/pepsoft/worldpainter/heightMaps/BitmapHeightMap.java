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

/**
 *
 * @author SchmitzP
 */
public final class BitmapHeightMap extends AbstractHeightMap {
    public BitmapHeightMap(String name, BufferedImage image, int channel, File imageFile, boolean repeat, boolean smoothScaling) {
        super(name);
        this.image = image;
        raster = image.getRaster();
        width = image.getWidth();
        height = image.getHeight();
        extent = repeat ? null : new Rectangle(0, 0, width, height);
        bits = raster.getSampleModel().getSampleSize(0);
        this.channel = channel;
        this.imageFile = imageFile;
        this.repeat = repeat;
        this.smoothScaling = smoothScaling;
    }

    public BufferedImage getImage() {
        return image;
    }

    public void setImage(BufferedImage image) {
        this.image = image;
        raster = image.getRaster();
        width = image.getWidth();
        height = image.getHeight();
        extent = repeat ? null : new Rectangle(0, 0, width, height);
        bits = raster.getSampleModel().getSampleSize(0);
    }

    public int getChannel() {
        return channel;
    }

    public void setChannel(int channel) {
        this.channel = channel;
    }

    public void setImageFile(File imageFile) {
        this.imageFile = imageFile;
    }

    public boolean isRepeat() {
        return repeat;
    }

    public void setRepeat(boolean repeat) {
        this.repeat = repeat;
        extent = repeat ? null : new Rectangle(0, 0, width, height);
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
            return raster.getSample(MathUtils.mod(x, width), MathUtils.mod(y, height), channel);
        } else if (extent.contains(x, y)) {
            return raster.getSample(x, y, channel);
        } else {
            return 0f;
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
            return raster.getSample(MathUtils.mod(x, width), MathUtils.mod(y, height), channel);
        } else if (extent.contains(x, y)) {
            return raster.getSample(x, y, channel);
        } else if (x < 0) {
            // West of the extent
            if (y < 0) {
                // Northwest of the extent
                return raster.getSample(0, 0, channel);
            } else if (y < height) {
                // Due west of the extent
                return raster.getSample(0, y, channel);
            } else {
                // Southwest of the extent
                return raster.getSample(0, height - 1, channel);
            }
        } else if (x < width) {
            // North or south of the extent
            if (y < 0) {
                // Due north of the extent
                return raster.getSample(x, 0, channel);
            } else {
                // Due south of the extent
                return raster.getSample(x, height - 1, channel);
            }
        } else {
            // East of the extent
            if (y < 0) {
                // Northeast of the extent
                return raster.getSample(width - 1, 0, channel);
            } else if (y < height) {
                // Due east of the extent
                return raster.getSample(width - 1, y, channel);
            } else {
                // Southeast of the extent
                return raster.getSample(width - 1, height - 1, channel);
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
        switch (bits) {
            case 8:
                return RANGE_8BIT;
            case 16:
                return RANGE_16BIT;
            default:
                throw new UnsupportedOperationException();
        }
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

    private BufferedImage image;
    private int channel, width, height, bits;
    private Raster raster;
    private Rectangle extent;
    private File imageFile;
    private boolean repeat, smoothScaling;

    private static final long serialVersionUID = 1L;
    private static final Icon ICON_BITMAP_HEIGHTMAP = IconUtils.loadScaledIcon("org/pepsoft/worldpainter/icons/height_map.png");
    private static final float[] RANGE_8BIT = {0.0f, 255.0f}, RANGE_16BIT = {0.0f, 65535.0f};

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