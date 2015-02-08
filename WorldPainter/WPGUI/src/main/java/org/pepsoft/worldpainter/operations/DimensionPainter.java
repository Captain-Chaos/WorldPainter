/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.worldpainter.operations;

import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.font.FontRenderContext;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.layers.Layer;

/**
 *
 * @author SchmitzP
 */
public final class DimensionPainter {
    public void drawPoint(int x, int y) {
        if (effectiveRadius == 0) {
            if (layerIsBinary) {
                dimension.setBitLayerValueAt(layer, x, y, binValue);
            } else {
                dimension.setLayerValueAt(layer, x, y, numValue);
            }
        } else {
            if (layerIsBinary) {
                for (int dx = -effectiveRadius; dx <= effectiveRadius; dx++) {
                    for (int dy = -effectiveRadius; dy <= effectiveRadius; dy++) {
                        if (brush.getFullStrength(dx, dy) >= 0.5f) {
                            dimension.setBitLayerValueAt(layer, x + dx, y + dy, binValue);
                        }
                    }
                }
            } else {
                for (int dx = -effectiveRadius; dx <= effectiveRadius; dx++) {
                    for (int dy = -effectiveRadius; dy <= effectiveRadius; dy++) {
                        if (brush.getFullStrength(dx, dy) >= 0.5f) {
                            dimension.setLayerValueAt(layer, x + dx, y + dy, numValue);
                        }
                    }
                }
            }
        }
    }
    
    public void drawLine(int x1, int y1, int x2, int y2) {
        final int dx = Math.abs(x2 - x1);
        final int dy = Math.abs(y2 - y1);
        if (dx < dy) {
            // Mostly vertical; go from top to bottom
            // Normalise the endpoints
            if (y2 < y1) {
                int tmp = y1;
                y1 = y2;
                y2 = tmp;
                tmp = x1;
                x1 = x2;
                x2 = tmp;
            }
            float x = x1 - 0.5f;
            final float fDx = (float) (x2 - x1) / dy;
            for (int y = y1; y <= y2; y++) {
                drawPoint((int) (x + 0.5f), y);
                x += fDx;
            }
        } else {
            // Mostly horizontal; go from left to right
            // Normalise the endpoints
            if (x2 < x1) {
                int tmp = y1;
                y1 = y2;
                y2 = tmp;
                tmp = x1;
                x1 = x2;
                x2 = tmp;
            }
            float y = y1 - 0.5f;
            final float fDy = (float) (y2 - y1) / dx;
            for (int x = x1; x <= x2; x++) {
                drawPoint(x, (int) (y + 0.5f));
                y += fDy;
            }
        }
    }
    
    public void drawText(int x, int y, String text) {
        String[] lines = text.split("\\n");
        for (String line: lines) {
            int lineHeight = drawLine(x, y, line);
            switch (textAngle) {
                case 0:
                    y += lineHeight;
                    break;
                case 1:
                    x += lineHeight;
                    break;
                case 2:
                    y -= lineHeight;
                    break;
                case 3:
                    x -= lineHeight;
                    break;
            }
        }
    }
    
    public int drawLine(int x, int y, String text) {
        BufferedImage image = new BufferedImage(1000, 100, BufferedImage.TYPE_BYTE_BINARY);
        Rectangle2D bounds;
        Graphics2D g2 = image.createGraphics();
        final int textWidth, textHeight;
        try {
            g2.setFont(font);
            FontRenderContext frc = g2.getFontRenderContext();
            bounds = font.getStringBounds(text, frc);
            textWidth = (int) Math.ceil(bounds.getWidth());
            textHeight = (int) Math.ceil(bounds.getHeight());
            if ((textWidth > 1000) || (textHeight > 100)) {
                g2.dispose();
                image = new BufferedImage(textWidth, textHeight, BufferedImage.TYPE_BYTE_BINARY);
                g2 = image.createGraphics();
                g2.setFont(font);
            }
            g2.drawString(text, (int) (-bounds.getX()), (int) (-bounds.getY()));
        } finally {
            g2.dispose();
        }
        if (layerIsBinary) {
            for (int xx = 0; xx < textWidth; xx++) {
                for (int yy = 0; yy < textHeight; yy++) {
                    if ((image.getRGB(xx, yy) & 1) != 0) {
                        switch (textAngle) {
                            case 0:
                                dimension.setBitLayerValueAt(layer, x + xx, y + yy, binValue);
                                break;
                            case 1:
                                dimension.setBitLayerValueAt(layer, x + yy, y - xx, binValue);
                                break;
                            case 2:
                                dimension.setBitLayerValueAt(layer, x - xx, y - yy, binValue);
                                break;
                            case 3:
                                dimension.setBitLayerValueAt(layer, x - yy, y + xx, binValue);
                                break;
                        }
                    }
                }
            }
        } else {
            for (int xx = 0; xx < textWidth; xx++) {
                for (int yy = 0; yy < textHeight; yy++) {
                    if ((image.getRGB(xx, yy) & 1) != 0) {
                        switch (textAngle) {
                            case 0:
                                dimension.setLayerValueAt(layer, x + xx, y + yy, numValue);
                                break;
                            case 1:
                                dimension.setLayerValueAt(layer, x + yy, y - xx, numValue);
                                break;
                            case 2:
                                dimension.setLayerValueAt(layer, x - xx, y - yy, numValue);
                                break;
                            case 3:
                                dimension.setLayerValueAt(layer, x - yy, y + xx, numValue);
                                break;
                        }
                    }
                }
            }
        }
        return (int) bounds.getHeight();
    }

    public Dimension getDimension() {
        return dimension;
    }

    public void setDimension(Dimension dimension) {
        this.dimension = dimension;
    }

    public Layer getLayer() {
        return layer;
    }

    public void setLayer(Layer layer) {
        this.layer = layer;
        layerIsBinary = layer.getDataSize() == Layer.DataSize.BIT || layer.getDataSize() == Layer.DataSize.BIT_PER_CHUNK;
    }

    public int getNumValue() {
        return numValue;
    }

    public void setNumValue(int numValue) {
        this.numValue = numValue;
    }

    public boolean isBinValue() {
        return binValue;
    }

    public void setBinValue(boolean binValue) {
        this.binValue = binValue;
    }

    public Brush getBrush() {
        return brush;
    }

    public void setBrush(Brush brush) {
        this.brush = brush;
        effectiveRadius = (brush instanceof RotatedBrush) ? ((RotatedBrush) brush).getEffectiveRadius() : brush.getRadius();
    }

    public Font getFont() {
        return font;
    }

    public void setFont(Font font) {
        this.font = font;
    }

    public int getTextAngle() {
        return textAngle;
    }

    public void setTextAngle(int textAngle) {
        this.textAngle = textAngle;
    }
    
    private Dimension dimension;
    private Layer layer;
    private int numValue, textAngle, effectiveRadius;
    private boolean binValue, layerIsBinary;
    private Brush brush;
    private Font font;
}