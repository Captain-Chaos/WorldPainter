/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.brushes;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.RescaleOp;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import javax.imageio.ImageIO;

/**
 *
 * @author pepijn
 */
public final class BitmapBrush extends AbstractBrush {
    public BitmapBrush(InputStream in, String name) {
        super(name);
        image = loadImage(in);
        setLevel(1.0f);
    }
    
    public BitmapBrush(File imageFile) {
        super(imageFile.getName());
        image = loadImage(imageFile);
        setLevel(1.0f);
    }

    @Override
    public float getStrength(int dx, int dy) {
        return buffer.getElem(0, dx + radius + (dy + radius) * diameter) / 255f;
    }

    @Override
    public float getFullStrength(int dx, int dy) {
        return fullStrengthBuffer.getElem(0, dx + radius + (dy + radius) * diameter) / 255f;
    }
    
    @Override
    public int getRadius() {
        return radius;
    }

    @Override
    public void setRadius(int radius) {
        if (radius != this.radius) {
            this.radius = radius;
            diameter = radius * 2 + 1;
            createMask();
        }
    }

    @Override
    public float getLevel() {
        return level;
    }

    @Override
    public void setLevel(float level) {
        if (level != this.level) {
            this.level = level;
            rescaleOp = new RescaleOp(level, 0, null);
            createMask();
        }
    }

    @Override
    public BrushShape getBrushShape() {
        return BrushShape.BITMAP;
    }
    
    private void createMask() {
        BufferedImage fullStrengthMask = new BufferedImage(diameter, diameter, BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g2 = fullStrengthMask.createGraphics();
        try {
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g2.drawImage(image, 0, 0, diameter, diameter, null);
        } finally {
            g2.dispose();
        }
        fullStrengthBuffer = (DataBufferByte) fullStrengthMask.getRaster().getDataBuffer();
        if (level < 1.0f) {
            BufferedImage mask = rescaleOp.filter(fullStrengthMask, null);
            buffer = (DataBufferByte) mask.getRaster().getDataBuffer();
        } else {
            buffer = fullStrengthBuffer;
        }
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
    
    private final BufferedImage image;
    private DataBufferByte fullStrengthBuffer, buffer;
    private int radius, diameter = 1;
    private float level;
    private RescaleOp rescaleOp;
}