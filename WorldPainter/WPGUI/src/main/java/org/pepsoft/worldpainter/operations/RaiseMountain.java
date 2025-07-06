/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.worldpainter.operations;

import org.pepsoft.util.PerlinNoise;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.WorldPainter;
import org.pepsoft.worldpainter.brushes.Brush;

import javax.swing.*;

import static org.pepsoft.worldpainter.Constants.MEDIUM_BLOBS;

/**
 *
 * @author pepijn
 */
public class RaiseMountain extends AbstractBrushOperation {
    public RaiseMountain(WorldPainter view) {
        super("Raise Mountain", "Raises a mountain out of the ground", view, 100, "operation.raiseMountain", "mountain");
        options = new TerrainShapingOptions<>();
        options.setApplyTheme(true); // This has historically been the default for this operation
        optionsPanel = new TerrainShapingOptionsPanel("Mountain", "<ul><li>Left-click to raise a mountain in the shape of the brush and its base at bedrock<li>Right-click to dig a hole in the shape of the brush and its base at build height</ul>", options);
    }

    @Override
    public JPanel getOptionsPanel() {
        return optionsPanel;
    }

    @Override
    protected void tick(int centreX, int centreY, boolean inverse, boolean first, float dynamicLevel) {
        final Dimension dimension = getDimension();
        if (dimension == null) {
            // Probably some kind of race condition
            return;
        }
        final float adjustment = (float) Math.pow(getLevel() * dynamicLevel * 2, 2.0);
        final int minZ = dimension.getMinHeight(), maxRange = dimension.getMaxHeight() - 1 - minZ;
        float peakHeight = dimension.getHeightAt(centreX + peakDX, centreY + peakDY) - minZ + (inverse ? -adjustment : adjustment);
        if (peakHeight < 0) {
            peakHeight = 0;
        } else if (peakHeight > maxRange) {
            peakHeight = maxRange;
        }
        dimension.setEventsInhibited(true);
        try {
            final int radius = getEffectiveRadius();
            final boolean applyTheme = options.isApplyTheme();
            for (int x = centreX - radius; x <= centreX + radius; x++) {
                for (int y = centreY - radius; y <= centreY + radius; y++) {
                    final float currentHeight = dimension.getHeightAt(x, y);
                    final float targetHeight = getTargetHeight(minZ, maxRange, centreX, centreY, x, y, peakHeight, inverse);
                    if (inverse ? (targetHeight < currentHeight) : (targetHeight > currentHeight)) {
//                        float strength = calcStrength(centerX, centerY, x, y);
//                        float newHeight = strength * targetHeight  + (1f - strength) * currentHeight;
                        dimension.setHeightAt(x, y, targetHeight);
                        if (applyTheme) {
                            dimension.applyTheme(x, y);
                        }
                    }
                }
            }
        } finally {
            dimension.setEventsInhibited(false);
        }
    }
    
    @Override
    protected final void brushChanged(Brush brush) {
        super.brushChanged(brush);
        if (brush == null) {
            return;
        }

        // Some calculations to support brushes where the centre point is not
        // the brightest point and/or where the brightest point is less than 1.0
        final int radius = getEffectiveRadius();
        float strength = brush.getFullStrength(0, 0);
        if (strength == 1.0f) {
            peakDX = 0;
            peakDY = 0;
            peakFactor = 1.0f;
//            System.out.println("Peak: 1.0 @ " + peakDX + ", " + peakDY);
            return;
        }
        float highestStrength = 0.0f;
        for (int r = 1; r <= radius; r++) {
            for (int i = -r + 1; i <= r; i++) {
                strength = brush.getFullStrength(i, -r);
                if (strength > highestStrength) {
                    peakDX = i;
                    peakDY = -r;
                    peakFactor = 1.0f / strength;
                    highestStrength = strength;
                    if (strength == 1.0f) {
//                        System.out.println("Peak: 1.0 @ " + peakDX + ", " + peakDY);
                        return;
                    }
                }
                strength = brush.getFullStrength(r, i);
                if (strength > highestStrength) {
                    peakDX = r;
                    peakDY = i;
                    peakFactor = 1.0f / strength;
                    highestStrength = strength;
                    if (strength == 1.0f) {
//                        System.out.println("Peak: 1.0 @ " + peakDX + ", " + peakDY);
                        return;
                    }
                }
                strength = brush.getFullStrength(-i, r);
                if (strength > highestStrength) {
                    peakDX = -i;
                    peakDY = r;
                    peakFactor = 1.0f / strength;
                    highestStrength = strength;
                    if (strength == 1.0f) {
//                        System.out.println("Peak: 1.0 @ " + peakDX + ", " + peakDY);
                        return;
                    }
                }
                strength = brush.getFullStrength(-r, -i);
                if (strength > highestStrength) {
                    peakDX = -r;
                    peakDY = -i;
                    peakFactor = 1.0f / strength;
                    highestStrength = strength;
                    if (strength == 1.0f) {
//                        System.out.println("Peak: 1.0 @ " + peakDX + ", " + peakDY);
                        return;
                    }
                }
            }
        }
//        System.out.println("Peak: " + highestStrength + " @ " + peakDX + ", " + peakDY);
    }

    /**
     * Calculate the target height for the mountain at a particular location. Note that {@code peakHeight} is the
     * absolute height above bedrock (not above z == 0) of the peak.
     */
    private float getTargetHeight(int minZ, int maxRange, int centerX, int centerY, int x, int y, float peakHeight, boolean undo) {
        return (undo
            ? Math.max(maxRange - (maxRange - peakHeight) * peakFactor * getNoisyStrength(x, y, getBrush().getFullStrength(x - centerX, y - centerY)), 0)
            : Math.min(peakHeight * peakFactor * getNoisyStrength(x, y, getBrush().getFullStrength(x - centerX, y - centerY)), maxRange)) + minZ;
    }
    
    private float getNoisyStrength(int x, int y, float strength) {
        float allowableNoiseRange = (0.5f - Math.abs(strength - 0.5f)) / 5;
        float noise = perlinNoise.getPerlinNoise(x / MEDIUM_BLOBS, y / MEDIUM_BLOBS);
        strength = strength + noise * allowableNoiseRange * strength;
        if (strength < 0.0) {
            return 0.0f;
        } else if (strength > 1.0) {
            return 1.0f;
        } else {
            return strength;
        }
    }
    
    private final PerlinNoise perlinNoise = new PerlinNoise(67);
    private final TerrainShapingOptions<RaiseMountain> options;
    private final TerrainShapingOptionsPanel optionsPanel;
    private int peakDX, peakDY;
    private float peakFactor;
}