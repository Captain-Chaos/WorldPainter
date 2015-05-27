/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.worldpainter.operations;

import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.MapDragControl;
import org.pepsoft.worldpainter.RadiusControl;
import org.pepsoft.worldpainter.WorldPainter;
import org.pepsoft.worldpainter.brushes.Brush;

/**
 *
 * @author pepijn
 */
public class RaiseMountain extends RadiusOperation {
    public RaiseMountain(WorldPainter view, RadiusControl radiusControl, MapDragControl mapDragControl) {
        super("Raise Mountain", "Raises a mountain out of the ground", view, radiusControl, mapDragControl, 100, "operation.raiseMountain");
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
        int maxZ = dimension.getMaxHeight() - 1;
        try {
            int radius = getEffectiveRadius();
            long seed = dimension.getSeed();
            for (int x = centreX - radius; x <= centreX + radius; x++) {
                for (int y = centreY - radius; y <= centreY + radius; y++) {
                    float currentHeight = dimension.getHeightAt(x, y);
                    float targetHeight = getTargetHeight(seed, maxZ, centreX, centreY, x, y, peakHeight, inverse);
                    if (inverse ? (targetHeight < currentHeight) : (targetHeight > currentHeight)) {
//                        float strength = calcStrength(centerX, centerY, x, y);
//                        float newHeight = strength * targetHeight  + (1f - strength) * currentHeight;
                        dimension.setHeightAt(x, y, targetHeight);
                        dimension.applyTheme(x, y);
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
            ? Math.max(maxZ - (maxZ - peakHeight) * peakFactor * getBrush().getNoisyFullStrength(seed, centerX, centerY, x, y), 0)
            : Math.min(peakHeight * peakFactor * getBrush().getNoisyFullStrength(seed, centerX, centerY, x, y), maxZ);
    }
    
    private int peakDX, peakDY;
    private float peakFactor;
}