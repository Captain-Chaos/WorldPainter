/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.brushes;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.RescaleOp;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.Reference;
import java.lang.ref.SoftReference;

/**
 * A {@link Brush} based on a bitmap image. If it is constructed with a file to load the image from, then the image is
 * stored as a weak reference so that it can be garbage collected to make space on the heap; it will be reloaded as
 * needed from the original file.
 *
 * @author pepijn
 */
public final class BitmapBrush extends AbstractBrush {
    public BitmapBrush(InputStream in, String name) {
        super(name);
        imageFile = null;
        image = loadImage(in);
        imageReference = null;
    }
    
    public BitmapBrush(File imageFile) {
        super(imageFile.getName());
        if (! imageFile.isFile()) {
            throw new IllegalArgumentException(imageFile + " is not a file or does not exist");
        } else if (! imageFile.canRead()) {
            throw new IllegalArgumentException(imageFile + " is not readable");
        }
        this.imageFile = imageFile;
        image = null;
    }

    @Override
    public float getStrength(int dx, int dy) {
        return getData().buffer.getElem(0, dx + radius + (dy + radius) * diameter) / 255f;
    }

    @Override
    public float getFullStrength(int dx, int dy) {
        return getData().fullStrengthBuffer.getElem(0, dx + radius + (dy + radius) * diameter) / 255f;
    }
    
    @Override
    public int getRadius() {
        return radius;
    }

    @Override
    public void setRadius(int radius) {
        if (radius != this.radius) {
            logger.debug("{{}} radius going from {} to {}", getName(), this.radius, radius);
            this.radius = radius;
            diameter = radius * 2 + 1;
            dataReference = null;
        }
    }

    @Override
    public float getLevel() {
        return level;
    }

    @Override
    public void setLevel(float level) {
        if (level != this.level) {
            logger.debug("{{}} level going from {} to {}", getName(), this.level, level);
            this.level = level;
            rescaleOp = new RescaleOp(level, 0, null);
            dataReference = null;
        }
    }

    @Override
    public BrushShape getBrushShape() {
        return BrushShape.BITMAP;
    }

    private Data getData() {
        Data data = (dataReference != null) ? dataReference.get() : null;
        if (data == null) {
            logger.debug("{{}} data not present; (re)creating data", getName());
            data = createData();
            dataReference = new SoftReference<>(data);
        }
        return data;
    }

    private Data createData() {
        BufferedImage image = this.image;
        if (image == null) {
            image = (imageReference != null) ? imageReference.get() : null;
            if (image == null) {
                logger.debug("{{}} image not present; (re)loading image {}", getName(), imageFile);
                image = loadImage(imageFile);
                imageReference = new SoftReference<>(image);
            }
        }
        BufferedImage fullStrengthMask = new BufferedImage(diameter, diameter, BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g2 = fullStrengthMask.createGraphics();
        try {
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g2.drawImage(image, 0, 0, diameter, diameter, null);
        } finally {
            g2.dispose();
        }
        Data data = new Data();
        data.fullStrengthBuffer = (DataBufferByte) fullStrengthMask.getRaster().getDataBuffer();
        if (level < 1.0f) {
            BufferedImage mask = rescaleOp.filter(fullStrengthMask, null);
            data.buffer = (DataBufferByte) mask.getRaster().getDataBuffer();
        } else {
            data.buffer = data.fullStrengthBuffer;
        }
        return data;
    }

    private BufferedImage loadImage(File imageFile) {
        try {
            return ImageIO.read(imageFile);
        } catch (IOException e) {
            throw new RuntimeException("I/O error reading image file " + imageFile, e);
        }
    }
    
    private BufferedImage loadImage(InputStream in) {
        try {
            return ImageIO.read(in);
        } catch (IOException e) {
            throw new RuntimeException("I/O error reading image from classpath", e);
        }
    }

    private final File imageFile;
    private final BufferedImage image; // Used when the image is loaded from a stream and cannot be recreated
    private Reference<BufferedImage> imageReference; // Used when the image is loaded from a file and can be reloaded
    private Reference<Data> dataReference;
    private int radius, diameter = 1;
    private float level = 1.0f;
    private RescaleOp rescaleOp = new RescaleOp(1.0f, 0, null);

    private static final Logger logger = LoggerFactory.getLogger(BitmapBrush.class);

    static class Data {
        DataBufferByte fullStrengthBuffer, buffer;
    }
}