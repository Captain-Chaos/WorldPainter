/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.brushes;

import org.pepsoft.util.MathUtils;

/**
 * A brush where the strength is purely a function of the angle to and distance
 * from the middle of the brush. This class automatically supports square
 * brushes by mapping the distance appropriately.
 *
 * @author pepijn
 */
public abstract class RadialBrush extends SymmetricBrush {
    public RadialBrush(String name, BrushShape brushShape, boolean rotationallySymmetric) {
        super(name, brushShape, rotationallySymmetric);
    }
    
    @Override
    protected final float calcStrength(int dx, int dy) {
        int radius = getRadius();
        float distance = MathUtils.getDistance(dx, dy);
        float dr = distance / radius;
        switch (brushShape) {
            case CIRCLE:
                if (distance <= radius) {
                    return calcStrength(dr);
                } else {
                    return 0f;
                }
            case SQUARE:
                if ((dx != 0) && (dy != 0)) {
                    // Extend the line from the center through (x, y) to the
                    // edge of the square, to calculate how much the distance
                    // should be increased to fill a square
                    int u, v;
                    if (dx < dy) {
                        u = radius * dx / dy;
                        v = radius;
                    } else {
                        u = radius;
                        v = radius * dy / dx;
                    }
                    float d = MathUtils.getDistance(u, v);
                    float factor = d / radius;
                    dr = dr / factor;
                }
                return calcStrength(dr);
            default:
                throw new InternalError();
        }
    }
    
    /**
     * Calculate the strength of this falloff as a function of the normalised
     * distance from the middle of the brush.
     * 
     * @param dr The distance from the middle, normalised so that dr = 1.0 at
     *     the edge of the brush.
     * @return  The strength at the specified normalised distance from the
     *     middle.
     */
    protected abstract float calcStrength(float dr);
}