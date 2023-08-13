package org.pepsoft.worldpainter.layers.renderers;

import lombok.Getter;
import org.pepsoft.util.ColourUtils;
import org.pepsoft.worldpainter.layers.CustomLayer;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;

import static java.util.Objects.requireNonNull;
import static org.pepsoft.util.MathUtils.mod;

/**
 * A {@link LayerRenderer} that can render the types of paint returned by {@link CustomLayer#getPaint()}.
 */
@Getter
public class PaintRenderer implements NibbleLayerRenderer, BitLayerRenderer{
    public PaintRenderer(Object paint, float opacity) {
        requireNonNull(paint, "paint");
        if ((opacity < 0.0f) || (opacity > 1.0f)) {
            throw new IllegalArgumentException("opacity " + opacity);
        }
        if (paint instanceof BufferedImage) {
            final BufferedImage image = (BufferedImage) paint;
            w = image.getWidth();
            h = image.getHeight();
            red = new int[w * h];
            green = new int[w * h];
            blue = new int[w * h];
            alpha = new float[w * h];
            final WritableRaster alphaRaster = image.getAlphaRaster();
            for (int x = 0; x < w; x++) {
                for (int y = 0; y < h; y++) {
                    final int rgb = image.getRGB(x, y);
                    final int index = x * w + y;
                    red[index] = (rgb & 0x00ff0000) >> 16;
                    green[index] = (rgb & 0x0000ff00) >> 8;
                    blue[index] = rgb & 0x000000ff;
                    alpha[index] = (alphaRaster != null) ? (alphaRaster.getSample(x, y, 0) / 255.0f) : 1.0f;
                }
            }
            colour = -1;
        } else if (paint instanceof Color) {
            colour = ((Color) paint).getRGB();
            w = h = -1;
            red = green = blue = null;
            alpha = null;
        } else {
            throw new IllegalArgumentException("Paint type " + paint.getClass() + " not supported");
        }
        this.opacity = opacity;
    }

    @Override
    public int getPixelColour(int globalX, int globalY, int underlyingColour, boolean value) {
        return getPixelColour(globalX, globalY, underlyingColour, value ? 15 : 0);
    }

    @Override
    public int getPixelColour(int globalX, int globalY, int underlyingColour, int value) {
        if (value > 0) {
            if (red != null) {
                final int index = mod(globalX, w) * w + mod(globalY, h);
                final float alpha = this.alpha[index] * opacity * value / 15;
                return (Math.round(alpha * this.red[index] + (1 - alpha) * ((underlyingColour & 0x00ff0000) >> 16)) << 16)
                        | (Math.round(alpha * this.green[index] + (1 - alpha) * ((underlyingColour & 0x0000ff00) >> 8)) << 8)
                        | Math.round(alpha * this.blue[index] + (1 - alpha) * (underlyingColour & 0x000000ff));
            } else if (opacity < 1.0f) {
                return ColourUtils.mix(colour, underlyingColour, Math.round(value * opacity * 255 / 15));
            } else {
                return ColourUtils.mix(colour, underlyingColour, value * 255 / 15);
            }
        } else {
            return underlyingColour;
        }
    }

    private final int w, h, colour;
    private final int[] red, green, blue;
    private final float[] alpha;
    private final float opacity;
}