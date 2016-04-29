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
            float val1 = cubicInterpolate(getHeight(xFloor - 1, yFloor - 1), getHeight(xFloor - 1, yFloor), getHeight(xFloor - 1, yFloor + 1), getHeight(xFloor - 1, yFloor + 2), yDelta);
            float val2 = cubicInterpolate(getHeight(xFloor,     yFloor - 1), getHeight(xFloor,     yFloor), getHeight(xFloor,     yFloor + 1), getHeight(xFloor,     yFloor + 2), yDelta);
            float val3 = cubicInterpolate(getHeight(xFloor + 1, yFloor - 1), getHeight(xFloor + 1, yFloor), getHeight(xFloor + 1, yFloor + 1), getHeight(xFloor + 1, yFloor + 2), yDelta);
            float val4 = cubicInterpolate(getHeight(xFloor + 2, yFloor - 1), getHeight(xFloor + 2, yFloor), getHeight(xFloor + 2, yFloor + 1), getHeight(xFloor + 2, yFloor + 2), yDelta);
            return cubicInterpolate(val1, val2, val3, val4, xDelta);
        }
    }

    @Override
    public float getBaseHeight() {
        return 0;
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

    public File getImageFile() {
        return imageFile;
    }

    public static BitmapHeightMapBuilder build() {
        return new BitmapHeightMapBuilder();
    }

    /**
     * Cubic interpolation using Catmull-Rom splines.
     */
    private float cubicInterpolate(float y0, float y1, float y2, float y3, float mu) {
//        float a0 = -0.5f * y0 + 1.5f * y1 - 1.5f * y2 + 0.5f * y3;
//        float a1 = y0 - 2.5f * y1 + 2 * y2 - 0.5f * y3;
//        float a2 = -0.5f * y0 + 0.5f * y2;
//        float mu2 = mu * mu;
//        return a0 * mu * mu2 + a1 * mu2 + a2 * mu + y1;
        return y1 + 0.5f * mu * (y2 - y0 + mu * (2.0f * y0 - 5.0f * y1 + 4.0f * y2 - y3 + mu * (3.0f * (y1 - y2) + y3 - y0)));
    }

    private BufferedImage image;
    private int channel, width, height;
    private Raster raster;
    private Rectangle extent;
    private File imageFile;
    private boolean repeat, smoothScaling;

    private static final long serialVersionUID = 1L;
    private static final Icon ICON_BITMAP_HEIGHTMAP = IconUtils.loadIcon("org/pepsoft/worldpainter/icons/height_map.png");

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