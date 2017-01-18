/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.worldpainter.operations;

import org.pepsoft.util.PerlinNoise;
import static org.pepsoft.worldpainter.Constants.MEDIUM_BLOBS;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.MapDragControl;
import org.pepsoft.worldpainter.RadiusControl;
import org.pepsoft.worldpainter.WorldPainter;
import org.pepsoft.worldpainter.brushes.Brush;

import javax.swing.*;

/**
 *
 * @author pepijn
 */
public class RaiseMountain extends RadiusOperation {
    public RaiseMountain(WorldPainter view, RadiusControl radiusControl, MapDragControl mapDragControl) {
        super("Raise Mountain", "Raises a mountain out of the ground", view, radiusControl, mapDragControl, 100, "operation.raiseMountain", "mountain");
        options = new TerrainShapingOptions<>();
        options.setApplyTheme(true); // This has historically been the default for this operation
        optionsPanel = new TerrainShapingOptionsPanel(options);
    }

    @Override
    public JPanel getOptionsPanel() {
        return optionsPanel;
    }

    @Override
    protected void tick(int centreX, int centreY, boolean inverse, boolean first, float dynamicLevel) {
        Dimension dimension = getDimension();
        float adjustment = (float) Math.pow(getLevel() * dynamicLevel * 2, 2.0);
        float peakHeight = dimension.getHeightAt(centreX + peakDX, centreY + peakDY) + (inverse ? -adjustment : adjustment);
        if (peakHeight < 0.0f) {
            peakHeight = 0.0f;
        } else if (peakHeight > (dimension.getMaxHeight() - 1)) {
            peakHeight = dimension.getMaxHeight() - 1;
        }
        dimension.setEventsInhibited(true);
        try {
            int maxZ = dimension.getMaxHeight() - 1;
            int radius = getEffectiveRadius();
            long seed = dimension.getSeed();
            boolean applyTheme = options.isApplyTheme();
            for (int x = centreX - radius; x <= centreX + radius; x++) {
                for (int y = centreY - radius; y <= centreY + radius; y++) {
                    float currentHeight = dimension.getHeightAt(x, y);
                    float targetHeight = getTargetHeight(seed, maxZ, centreX, centreY, x, y, peakHeight, inverse);
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
        final int radius = getEffectiveRadius();
        if (brush == null) {
            return;
        }

        // Some calculations to support brushes where the centre point is not
        // the brightest point and/or where the brightest point is less than 1.0
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
    
    private float getTargetHeight(long seed, int maxZ, int centerX, int centerY, int x, int y, float peakHeight, boolean undo) {
        return undo
            ? Math.max(maxZ - (maxZ - peakHeight) * peakFactor * getNoisyStrength(x, y, getBrush().getFullStrength(x - centerX, y - centerY)), 0)
            : Math.min(peakHeight * peakFactor * getNoisyStrength(x, y, getBrush().getFullStrength(x - centerX, y - centerY)), maxZ);
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