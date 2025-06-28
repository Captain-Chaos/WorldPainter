/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.worldpainter.painting;

import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.Terrain;
import org.pepsoft.worldpainter.brushes.Brush;
import org.pepsoft.worldpainter.brushes.LineBrush;
import org.pepsoft.worldpainter.brushes.SymmetricBrush;
import org.pepsoft.worldpainter.layers.Layer;

import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

import static java.awt.RenderingHints.KEY_TEXT_ANTIALIASING;
import static java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_OFF;
import static org.pepsoft.worldpainter.Constants.TILE_SIZE_BITS;
import static org.pepsoft.worldpainter.brushes.BrushShape.CIRCLE;

/**
 * A utility class for painting basic shapes to dimension using any kind of {@link Paint}.
 *
 * <p><strong>Note:</strong> does <em>not</em> do any event inhibition management. The client should do that by
 * surrounding use of this class with invocations of {@link Dimension#setEventsInhibited(boolean)}:
 *
 * <pre>dimension.setEventsInhibited(true);
 *try {
 *    painter.setDimension(dimension);
 *    // One or more calls to painting methods...
 *} finally {
 *    dimension.setEventsInhibited(false); // Will fire all necessary events
 *}</pre>
 *
 * @author SchmitzP
 */
public final class DimensionPainter {
    /**
     * Paint a single impression of the current brush using the current paint.
     *
     * @param x The x coordinate at which to paint the impression.
     * @param y The y coordinate at which to paint the impression.
     */
    public void drawPoint(Dimension dimension, int x, int y) {
        if (undo) {
            paint.remove(dimension, x, y, 1.0f);
        } else {
            paint.apply(dimension, x, y, 1.0f);
        }
    }

    /**
     * Paint a single impression of the current brush, attenuated by a dynamic level, using the current paint.
     *
     * @param x The x coordinate at which to paint the impression.
     * @param y The y coordinate at which to paint the impression.
     * @param dynamicLevel The dynamic level between {@code 0.0f} and {@code 1.0f} (inclusive) with which to
     *                     multiply the brush.
     */
    public void drawPoint(Dimension dimension, int x, int y, float dynamicLevel) {
        if (undo) {
            paint.remove(dimension, x, y, dynamicLevel);
        } else {
            paint.apply(dimension, x, y, dynamicLevel);
        }
    }
    
    /**
     * Draw a straight line between two points using the current brush and paint, by applying the brush at each point
     * along the line. This version defaults to using slow but accurate algorithms. See
     * {@link #drawLine(Dimension, int, int, int, int, boolean)} for a version which can use fast but possibly less
     * accurate algorithms.
     *
     * @param x1 The x coordinate at which to start the line.
     * @param y1 The y coordinate at which to start the line.
     * @param x2 The x coordinate at which to end the line.
     * @param y2 The y coordinate at which to end the line.
     */
    public void drawLine(Dimension dimension, int x1, int y1, int x2, int y2) {
        drawLine(dimension, x1, y1, x2, y2, 1.0f, false);
    }

    /**
     * Draw a straight line between two points using the current brush and paint, by applying the brush at each point
     * along the line.
     *
     * @param x1 The x coordinate at which to start the line.
     * @param y1 The y coordinate at which to start the line.
     * @param x2 The x coordinate at which to end the line.
     * @param y2 The y coordinate at which to end the line.
     * @param fast Use fast but possibly less accurate algorithms.
     */
    public void drawLine(Dimension dimension, int x1, int y1, int x2, int y2, boolean fast) {
        drawLine(dimension, x1, y1, x2, y2, 1.0f, fast);
    }

    /**
     * Draw a straight line between two points using the current brush, attenuated by a dynamic level, and paint, by
     * applying the brush at each point along the line. This version defaults to using slow but accurate algorithms. See
     * {@link #drawLine(Dimension, int, int, int, int, float, boolean)} for a version which can use fast but possibly
     * less accurate algorithms.
     *
     * @param x1 The x coordinate at which to start the line.
     * @param y1 The y coordinate at which to start the line.
     * @param x2 The x coordinate at which to end the line.
     * @param y2 The y coordinate at which to end the line.
     * @param dynamicLevel The dynamic level between {@code 0.0f} and {@code 1.0f} (inclusive) with which to
     *                     multiply the brush.
     */
    public void drawLine(Dimension dimension, int x1, int y1, int x2, int y2, float dynamicLevel) {
        drawLine(dimension, x1, y1, x2, y2, dynamicLevel, false);
    }

    /**
     * Draw a straight line between two points using the current brush, attenuated by a dynamic level, and paint, by
     * applying the brush at each point along the line.
     *
     * @param x1 The x coordinate at which to start the line.
     * @param y1 The y coordinate at which to start the line.
     * @param x2 The x coordinate at which to end the line.
     * @param y2 The y coordinate at which to end the line.
     * @param dynamicLevel The dynamic level between {@code 0.0f} and {@code 1.0f} (inclusive) with which to
     *                     multiply the brush.
     * @param fast Use fast but possibly less accurate algorithms.
     */
    public void drawLine(Dimension dimension, int x1, int y1, int x2, int y2, float dynamicLevel, boolean fast) {
        final Brush currentBrush = paint.getBrush();
        if (currentBrush == null) {
            // This happens when the null paint is selected because the user has not yet made a paint choice
            return;
        }
        final int dx = Math.abs(x2 - x1);
        final int dy = Math.abs(y2 - y1);
        if (fast && (paint.getBrush().getRadius() == 0)) {
            // Special case: if the radius is 0, assume that the user wants to paint complete pixels instead of trying
            // to apply the brush. The LineBrush is not good at that, and also not faster than slow mode in this case,
            // so fall back to slow mode
            fast = false;
        }
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
            if (! fast) {
                float x = x1 - 0.5f;
                final float fDx = (float) (x2 - x1) / dy;
                for (int y = y1; y <= y2; y++) {
                    drawPoint(dimension, Math.round(x), y, dynamicLevel);
                    x += fDx;
                }
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
            if (! fast) {
                float y = y1 - 0.5f;
                final float fDy = (float) (y2 - y1) / dx;
                for (int x = x1; x <= x2; x++) {
                    drawPoint(dimension, x, Math.round(y), dynamicLevel);
                    y += fDy;
                }
            }
        }
        if (fast) {
            final Brush lineBrush;
            if (currentBrush.getBrushShape() == CIRCLE) {
                lineBrush = LineBrush.of(currentBrush, (x2 - x1), (y2 - y1));
            } else {
                MY_CONSTANT_CIRCLE.setRadius(currentBrush.getRadius());
                MY_CONSTANT_CIRCLE.setLevel(currentBrush.getLevel());
                lineBrush = LineBrush.of(MY_CONSTANT_CIRCLE, (x2 - x1), (y2 - y1));
            }
            paint.setBrush(lineBrush);
            try {
                if (undo) {
                    paint.remove(dimension, x1 + ((x2 - x1) / 2), y1 + ((y2 - y1) / 2), dynamicLevel);
                } else {
                    paint.apply(dimension, x1 + ((x2 - x1) / 2), y1 + ((y2 - y1) / 2), dynamicLevel);
                }
            } finally {
                paint.setBrush(currentBrush);
            }
        }
    }

    /**
     * Draw one or more lines of text using the current paint, font and rotation. The current brush is ignored. The text
     * may be rotated at 90 degree angles using {@link #setTextAngle(int)}.
     *
     * @param x The x coordinate of the start/top corner of the block of text.
     * @param y The y coordinate of the start/top corner of the block of text.
     * @param text The text to paint. May have multiple lines separated by line feed ('\n') characters.
     */
    @SuppressWarnings("SuspiciousNameCombination")
    public void drawText(Dimension dimension, int x, int y, String text) {
        final String[] lines = text.split("\\n");
        for (String line: lines) {
            final int lineHeight = drawTextLine(dimension, x, y, line);
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

    /**
     * Flood fill an area of the dimension with the current paint. The area to be flooded is the area where the type of
     * paint has the same value as where the fill is started. If the value is already the same as the configured paint,
     * nothing happens. If the operation takes more than two seconds a modal dialog is shown to the user with an
     * indeterminate progress bar.
     *
     * <p>The total filled area cannot exceed a square around the given coordinates with a surface area of
     * {@link Integer#MAX_VALUE}, due to Java limitations. If the boundary of this area has been hit, the method will
     * return {@code false} and the entire matching area may not have been filled.
     *
     * @param x The X coordinate to start the flood fill.
     * @param y The Y coordinate to start the flood fill.
     * @param parent The window to use as parent for the modal dialog shown if the operation takes more than two
     *               seconds.
     * @return {@code true} if the fill operation was completed, or {@code false} if the filled area touched the
     * maximum bounds and the operation may not have filled the entire matching area.
     */
    public boolean fill(Dimension dimension, final int x, final int y, Window parent) {
        AbstractDimensionPaintFillMethod fillMethod;
        if (paint instanceof LayerPaint) {
            final Layer layer = ((LayerPaint) paint).getLayer();
            switch (layer.getDataSize()) {
                case BIT:
                case BIT_PER_CHUNK:
                    if (undo) {
                        fillMethod = new UndoDimensionPaintFillMethod("Removing " + layer, dimension, paint) {
                            @Override
                            public boolean isBoundary(int x, int y) {
                                return ! dimension.getBitLayerValueAt(layer, x, y);
                            }
                        };
                    } else {
                        fillMethod = new DimensionPaintFillMethod("Applying " + layer, dimension, paint) {
                            @Override
                            public boolean isBoundary(int x, int y) {
                                return dimension.getBitLayerValueAt(layer, x, y);
                            }
                        };
                    }
                    break;
                case NIBBLE:
                case BYTE:
                    if (paint instanceof DiscreteLayerPaint) {
                        final int fillValue = dimension.getLayerValueAt(layer, x, y);
                        if (undo) {
                            fillMethod = new UndoDimensionPaintFillMethod("Removing " + layer, dimension, paint) {
                                @Override
                                public boolean isBoundary(int x, int y) {
                                    return dimension.getLayerValueAt(layer, x, y) != fillValue;
                                }

                                @Override
                                boolean isFilled(int x, int y) {
                                    return dimension.getLayerValueAt(layer, x, y) == layer.getDefaultValue();
                                }
                            };
                        } else {
                            fillMethod = new DimensionPaintFillMethod("Applying " + layer, dimension, paint) {
                                @Override
                                public boolean isBoundary(int x, int y) {
                                    return dimension.getLayerValueAt(layer, x, y) != fillValue;
                                }

                                @Override
                                boolean isFilled(int x, int y) {
                                    return dimension.getLayerValueAt(layer, x, y) == ((DiscreteLayerPaint) paint).getValue();
                                }
                            };
                        }
                    } else {
                        if (undo) {
                            fillMethod = new UndoDimensionPaintFillMethod("Removing " + layer, dimension, paint) {
                                @Override
                                public boolean isBoundary(int x, int y) {
                                    return dimension.getLayerValueAt(layer, x, y) == 0;
                                }
                            };
                        } else {
                            fillMethod = new DimensionPaintFillMethod("Applying " + layer, dimension, paint) {
                                @Override
                                public boolean isBoundary(int x, int y) {
                                    return dimension.getLayerValueAt(layer, x, y) >= targetValue;
                                }

                                final int targetValue = 1 + Math.round((layer.getDataSize() == Layer.DataSize.NIBBLE) ? (paint.getBrush().getLevel() * 14) : (paint.getBrush().getLevel() * 254));
                            };
                        }
                    }
                    break;
                default:
                    throw new IllegalArgumentException("Don't know how to fill with layer with data size " + layer.getDataSize());
            }
        } else if (paint instanceof TerrainPaint) {
            final Terrain terrainToFill = dimension.getTerrainAt(x, y);
            if (undo) {
                fillMethod = new UndoDimensionPaintFillMethod("Removing " + terrainToFill, dimension, paint) {
                    @Override
                    public boolean isBoundary(int x, int y) {
                        return dimension.getTerrainAt(x, y) != terrainToFill;
                    }

                    @Override
                    boolean isFilled(int x, int y) {
                        return dimension.getTerrainAt(x, y) == ((TerrainPaint) paint).getTerrain();
                    }
                };
            } else {
                fillMethod = new DimensionPaintFillMethod("Applying " + ((TerrainPaint) paint).getTerrain(), dimension, paint) {
                    @Override
                    public boolean isBoundary(int x, int y) {
                        return dimension.getTerrainAt(x, y) != terrainToFill;
                    }

                    @Override
                    boolean isFilled(int x, int y) {
                        return dimension.getTerrainAt(x, y) == ((TerrainPaint) paint).getTerrain();
                    }
                };
            }
        } else if (paint instanceof PaintFactory.NullPaint) {
            return true;
        } else {
            throw new IllegalArgumentException("Don't know how to fill with paint " + paint);
        }
        if (! fillMethod.isFilled(x, y)) {
            GeneralQueueLinearFloodFiller filler = new GeneralQueueLinearFloodFiller(fillMethod);
            filler.floodFill(x, y, parent);
            return ! filler.isBoundsHit();
        } else {
            return true;
        }
    }

    /**
     * Get the font with which text is painted.
     *
     * @return The font with which text is painted.
     */
    public Font getFont() {
        return font;
    }

    /**
     * Set the font with which text is painted.
     *
     * @param font The font with which text is painted.
     */
    public void setFont(Font font) {
        this.font = font;
    }

    /**
     * Get the angle at which text is painted.
     *
     * @return The angle at which text is painted in terms of one of the constants {@link #ANGLE_0_DEGREES},
     * {@link #ANGLE_90_DEGREES}, {@link #ANGLE_180_DEGREES} or {@link #ANGLE_270_DEGREES}.
     */
    public int getTextAngle() {
        return textAngle;
    }

    /**
     * Set the angle at which text is painted.
     *
     * @param textAngle The angle at which text is painted in terms of one of the constants {@link #ANGLE_0_DEGREES},
     * {@link #ANGLE_90_DEGREES}, {@link #ANGLE_180_DEGREES} or {@link #ANGLE_270_DEGREES}.
     */
    public void setTextAngle(int textAngle) {
        if ((textAngle < ANGLE_0_DEGREES) || (textAngle > ANGLE_270_DEGREES)) {
            throw new IllegalArgumentException();
        }
        this.textAngle = textAngle;
    }

    /**
     * Get the paint which the operations apply to the dimension.
     *
     * @return The paint which the operations apply to the dimension.
     */
    public Paint getPaint() {
        return paint;
    }

    /**
     * Set the paint which the operations apply to the dimension.
     *
     * @param paint The paint which the operations apply to the dimension.
     */
    public void setPaint(Paint paint) {
        this.paint = paint;
    }

    /**
     * Configure whether the paint is <em>applied</em> or <em>removed</em> by the operations. What constitutes
     * "application" or "removal" is defined by the paint.
     *
     * @param undo Whether the paint is <em>applied</em> (when {@code false}) or <em>removed</em> (when
     *             {@code true}) by the operations.
     */
    public void setUndo(boolean undo) {
        this.undo = undo;
    }

    /**
     * Determine whether the paint is <em>applied</em> or <em>removed</em> by the operations. What constitutes
     * "application" or "removal" is defined by the paint.
     *
     * @return Whether the paint is <em>applied</em> (when {@code false}) or <em>removed</em> (when
     * {@code true}) by the operations.
     */
    public boolean isUndo() {
        return undo;
    }

    private int drawTextLine(Dimension dimension, int x, int y, String text) {
        BufferedImage image = new BufferedImage(1000, 100, BufferedImage.TYPE_BYTE_BINARY);
        final Rectangle2D bounds;
        Graphics2D g2 = image.createGraphics();
        g2.setRenderingHint(KEY_TEXT_ANTIALIASING, VALUE_TEXT_ANTIALIAS_OFF);
        final int textWidth, textHeight;
        try {
            g2.setFont(font);
            final FontRenderContext frc = g2.getFontRenderContext();
            bounds = font.getStringBounds(text, frc);
            textWidth = (int) Math.ceil(bounds.getWidth());
            textHeight = (int) Math.ceil(bounds.getHeight());
            if ((textWidth < 1) || (textHeight < 1)) {
                return (int) bounds.getHeight();
            }
            if ((textWidth > 1000) || (textHeight > 100)) {
                g2.dispose();
                image = new BufferedImage(textWidth, textHeight, BufferedImage.TYPE_BYTE_BINARY);
                g2 = image.createGraphics();
                g2.setRenderingHint(KEY_TEXT_ANTIALIASING, VALUE_TEXT_ANTIALIAS_OFF);
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
                            case ANGLE_0_DEGREES:
                                paint.removePixel(dimension, x + xx, y + yy);
                                break;
                            case ANGLE_90_DEGREES:
                                paint.removePixel(dimension, x + yy, y - xx);
                                break;
                            case ANGLE_180_DEGREES:
                                paint.removePixel(dimension, x - xx, y - yy);
                                break;
                            case ANGLE_270_DEGREES:
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
                            case ANGLE_0_DEGREES:
                                paint.applyPixel(dimension, x + xx, y + yy);
                                break;
                            case ANGLE_90_DEGREES:
                                paint.applyPixel(dimension, x + yy, y - xx);
                                break;
                            case ANGLE_180_DEGREES:
                                paint.applyPixel(dimension, x - xx, y - yy);
                                break;
                            case ANGLE_270_DEGREES:
                                paint.applyPixel(dimension, x - yy, y + xx);
                                break;
                        }
                    }
                }
            }
        }
        return (int) bounds.getHeight();
    }

    private Paint paint;
    private int textAngle;
    private boolean undo;
    private Font font;

    public static final int ANGLE_0_DEGREES   = 0;
    public static final int ANGLE_90_DEGREES  = 1;
    public static final int ANGLE_180_DEGREES = 2;
    public static final int ANGLE_270_DEGREES = 3;

    private static final Brush MY_CONSTANT_CIRCLE = SymmetricBrush.CONSTANT_CIRCLE.clone();

    static abstract class AbstractDimensionPaintFillMethod implements GeneralQueueLinearFloodFiller.FillMethod {
        protected AbstractDimensionPaintFillMethod(String description, Dimension dimension, Paint paint) {
            this.description = description;
            this.dimension = dimension;
            this.paint = paint;
            bounds = new Rectangle(dimension.getLowestX() << TILE_SIZE_BITS, dimension.getLowestY() << TILE_SIZE_BITS, dimension.getWidth() << TILE_SIZE_BITS, dimension.getHeight() << TILE_SIZE_BITS);
        }

        @Override
        public final Rectangle getBounds() {
            return bounds;
        }

        @Override
        public final String getDescription() {
            return description;
        }

        boolean isFilled(int x, int y) {
            return isBoundary(x, y);
        }

        private final String description;
        private final Rectangle bounds;
        protected final Dimension dimension;
        protected final Paint paint;
    }

    static abstract class DimensionPaintFillMethod extends AbstractDimensionPaintFillMethod {
        DimensionPaintFillMethod(String description, Dimension dimension, Paint paint) {
            super(description, dimension, paint);
        }

        @Override
        public final void fill(int x, int y) {
            paint.applyPixel(dimension, x, y);
        }
    }

    static abstract class UndoDimensionPaintFillMethod extends AbstractDimensionPaintFillMethod {
        UndoDimensionPaintFillMethod(String description, Dimension dimension, Paint paint) {
            super(description, dimension, paint);
        }

        @Override
        public final void fill(int x, int y) {
            paint.removePixel(dimension, x, y);
        }
    }
}