/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.worldpainter.painting;

import org.pepsoft.worldpainter.Dimension;

import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

/**
 *
 * @author SchmitzP
 */
public final class DimensionPainter {
    public void drawPoint(int x, int y) {
        if (undo) {
            paint.remove(dimension, x, y, 1.0f);
        } else {
            paint.apply(dimension, x, y, 1.0f);
        }
    }

    public void drawPoint(int x, int y, float dynamicLevel) {
        if (undo) {
            paint.remove(dimension, x, y, dynamicLevel);
        } else {
            paint.apply(dimension, x, y, dynamicLevel);
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

    public void drawLine(int x1, int y1, int x2, int y2, float dynamicLevel) {
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
                drawPoint((int) (x + 0.5f), y, dynamicLevel);
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
                drawPoint(x, (int) (y + 0.5f), dynamicLevel);
                y += fDy;
            }
        }
    }
    
    public void drawText(int x, int y, String text) {
        dimension.setEventsInhibited(true);
        try {
            String[] lines = text.split("\\n");
            for (String line: lines) {
                int lineHeight = drawTextLine(x, y, line);
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
        } finally {
            dimension.setEventsInhibited(false);
        }
    }

    private int drawTextLine(int x, int y, String text) {
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
        if (undo) {
            for (int xx = 0; xx < textWidth; xx++) {
                for (int yy = 0; yy < textHeight; yy++) {
                    if ((image.getRGB(xx, yy) & 1) != 0) {
                        switch (textAngle) {
                            case 0:
                                paint.removePixel(dimension, x + xx, y + yy);
                                break;
                            case 1:
                                paint.removePixel(dimension, x + yy, y - xx);
                                break;
                            case 2:
                                paint.removePixel(dimension, x - xx, y - yy);
                                break;
                            case 3:
                                paint.removePixel(dimension, x - yy, y + xx);
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
                                paint.applyPixel(dimension, x + xx, y + yy);
                                break;
                            case 1:
                                paint.applyPixel(dimension, x + yy, y - xx);
                                break;
                            case 2:
                                paint.applyPixel(dimension, x - xx, y - yy);
                                break;
                            case 3:
                                paint.applyPixel(dimension, x - yy, y + xx);
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

    public Paint getPaint() {
        return paint;
    }

    public void setPaint(Paint paint) {
        this.paint = paint;
    }

    public void setUndo(boolean undo) {
        this.undo = undo;
    }

    private Paint paint;
    private Dimension dimension;
    private int textAngle;
    private boolean undo;
    private Font font;
}