/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.brushes;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.*;

/**
 * A brush which can rotate another brush an arbitrary angle around its center.
 * 
 * @author SchmitzP
 */
public final class RotatedBrush extends AbstractBrush {
    private RotatedBrush(Brush brush, int degrees) {
        super(brush.getName());
        this.brush = brush.clone();
        this.degrees = degrees;
        radius = brush.getRadius();
        level = brush.getLevel();
        cacheAllStrengths();
    }

    public Brush getBrush() {
        return brush;
    }

    public int getDegrees() {
        return degrees;
    }
    
    public void setDegrees(int degrees) {
        if (degrees != this.degrees) {
            this.degrees = degrees;
            cacheAllStrengths();
        }
    }
    
    @Override
    public int getRadius() {
        return radius;
    }

    @Override
    public void setRadius(int radius) {
        if (radius != this.radius) {
            this.radius = radius;
            brush.setRadius(radius);
            cacheAllStrengths();
        }
    }

    @Override
    public int getEffectiveRadius() {
        return effectiveRadius;
    }

    @Override
    public float getLevel() {
        return level;
    }

    @Override
    public void setLevel(float level) {
        if (level != this.level) {
            this.level = level;
            brush.setLevel(level);
            if (fullStrengthCache == null) {
                cacheAllStrengths();
            }
        }
    }

    @Override
    public BrushShape getBrushShape() {
        return brush.getBrushShape();
    }

    @Override
    public float getStrength(int dx, int dy) {
        final int x = dx + effectiveRadius, y = dy + effectiveRadius;
        if ((x < 0) || (x >= effectiveWidth) || (y < 0) || (y >= effectiveHeight)) {
            return 0.0f;
        } else if (level == 1.0f) {
            return fullStrengthCache.getSample(x, y, 0) / 255f;
        } else {
            return fullStrengthCache.getSample(x, y, 0) / 255f * level;
        }
    }

    @Override
    public float getFullStrength(int dx, int dy) {
        final int x = dx + effectiveRadius, y = dy + effectiveRadius;
        if ((x < 0) || (x >= effectiveWidth) || (y < 0) || (y >= effectiveHeight)) {
            return 0.0f;
        } else {
            return fullStrengthCache.getSample(x, y, 0) / 255f;
        }
    }

    @Override
    public RotatedBrush clone() {
        RotatedBrush clone = (RotatedBrush) super.clone();
        clone.brush = brush.clone();
        return clone;
    }
    
    private void cacheAllStrengths() {
        final int d = radius * 2 + 1;
        
        // Create image from original brush
        final byte[] rasterData = new byte[d * d];
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                rasterData[dx + radius + (dy + radius) * d] = (byte) (brush.getFullStrength(dx, dy) * 255);
            }
        }
        final DataBuffer imageDataBuffer = new DataBufferByte(rasterData, d * d);
        final Raster raster = Raster.createRaster(new SinglePixelPackedSampleModel(DataBuffer.TYPE_BYTE, d, d, new int[] {255}), imageDataBuffer, new Point(0, 0));
        
        // Rotate image
        final double a = degrees / 180d * Math.PI;
        AffineTransformOp transformOp = new AffineTransformOp(AffineTransform.getRotateInstance(a), AffineTransformOp.TYPE_BICUBIC);
        Rectangle2D transformedBounds = transformOp.getBounds2D(raster);
        AffineTransform transform = AffineTransform.getTranslateInstance(-transformedBounds.getX(), -transformedBounds.getY());
        transform.rotate(a);
        transformOp = new AffineTransformOp(transform, AffineTransformOp.TYPE_BICUBIC);
        fullStrengthCache = transformOp.filter(raster, null);

        // Calculate effective dimensions
        effectiveWidth = fullStrengthCache.getWidth();
        effectiveHeight = fullStrengthCache.getHeight();
        effectiveRadius = effectiveWidth / 2;
    }

    /**
     * Rotate a brush in the most efficient way possible. If no rotation is
     * necessary or applicable, this method returns the original brush. If the
     * specified brush is already a rotated brush, a new rotated brush based on
     * the original non-rotated brush is returned, to avoid unnecessary double
     * rotations.
     * 
     * @param brush The brush to rotate.
     * @param degrees The number of degrees to rotate the brush clockwise.
     * @return The specified brush, rotated as specified.
     */
    public static Brush rotate(Brush brush, int degrees) {
        degrees = Math.floorMod(degrees, 360);
        if (degrees == 0) {
            return brush;
        } else if (brush instanceof RotatedBrush) {
            int adjustedDegrees = Math.floorMod(((RotatedBrush) brush).degrees + degrees, 360);
            if (adjustedDegrees == 0) {
                return ((RotatedBrush) brush).brush;
            } else {
                return new RotatedBrush(((RotatedBrush) brush).brush, adjustedDegrees);
            }
        } else if ((brush instanceof SymmetricBrush) && ((SymmetricBrush) brush).isRotationallySymmetric()) {
            return brush;
        } else {
            return new RotatedBrush(brush, degrees);
        }
    }
    
    private Brush brush;
    private int radius, effectiveRadius, degrees, effectiveWidth, effectiveHeight;
    private float level;
    private Raster fullStrengthCache;
}