/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.heightMaps;

import org.pepsoft.util.MathUtils;

/**
 *
 * @author pepijn
 */
public final class NinePatchHeightMap extends AbstractHeightMap {
    public NinePatchHeightMap(int innerRadius, int coastSize, float height) {
        this(null, 0, innerRadius, coastSize, height);
    }
    
    public NinePatchHeightMap(int innerSize, int borderSize, int coastSize, float height) {
        this(null, innerSize, borderSize, coastSize, height);
    }
    
    public NinePatchHeightMap(String name, int innerRadius, int coastSize, float height) {
        this(name, 0, innerRadius, coastSize, height);
    }
    
    public NinePatchHeightMap(String name, int innerSize, int borderSize, int coastSize, float height) {
        super(name);
        if ((innerSize < 0) || (borderSize < 0) || (coastSize < 0) || (height <= 0.0f)) {
            throw new IllegalArgumentException();
        }
        if ((innerSize == 0) && (borderSize == 0) && (coastSize == 0)) {
            throw new IllegalArgumentException();
        }
        this.innerSize = innerSize;
        this.borderSize = borderSize;
        this.coastSize = coastSize;
        this.height = height;
        halfHeight = height / 2;
        borderTotal = innerSize + borderSize;
        coastTotal = borderTotal + coastSize;
    }

    public int getInnerSize() {
        return innerSize;
    }

    public int getBorderSize() {
        return borderSize;
    }

    public int getCoastSize() {
        return coastSize;
    }

    public float getHeight() {
        return height;
    }

    // HeightMap
    
    @Override
    public float getHeight(int x, int y) {
        x = Math.abs(x);
        y = Math.abs(y);
        if (x < innerSize) {
            if (y < innerSize) {
                // On the continent
                return height;
            } else if (y < borderTotal) {
                // Border
                return height;
            } else if (y < coastTotal) {
                // Coast
                return (float) (Math.cos((double) (y - borderTotal) / coastSize * Math.PI) * halfHeight) + halfHeight;
            } else {
                // Outside the continent
                return 0;
            }
        } else if (x < borderTotal) {
            if (y < innerSize) {
                // Border
                return height;
            } else if (y < coastTotal) {
                // Corner
                float distanceFromCorner = MathUtils.getDistance(x - innerSize, y - innerSize);
                if (distanceFromCorner < borderSize) {
                    // Border
                    return height;
                } else if (distanceFromCorner - borderSize < coastSize) {
                    return (float) (Math.cos((double) (distanceFromCorner - borderSize) / coastSize * Math.PI) * halfHeight) + halfHeight;
                    // Coast
                } else {
                    // Outside the continent
                    return 0;
                }
            } else {
                // Outside the continent
                return 0;
            }
        } else if (x < coastTotal) {
            if (y < innerSize) {
                // Coast
                return (float) (Math.cos((double) (x - borderTotal) / coastSize * Math.PI) * halfHeight) + halfHeight;
            } else if (y < coastTotal) {
                // Corner
                float distanceFromCorner = MathUtils.getDistance(x - innerSize, y - innerSize);
                if (distanceFromCorner < borderSize) {
                    // Border
                    return height;
                } else if (distanceFromCorner - borderSize < coastSize) {
                    return (float) (Math.cos((double) (distanceFromCorner - borderSize) / coastSize * Math.PI) * halfHeight) + halfHeight;
                    // Coast
                } else {
                    // Outside the continent
                    return 0;
                }
            } else {
                // Outside the continent
                return 0;
            }
        } else {
            // Outside the continent
            return 0;
        }
    }

    @Override
    public float getBaseHeight() {
        return 0.0f;
    }
    
    private final int innerSize, borderSize, coastSize;
    private final int borderTotal, coastTotal;
    private final float height, halfHeight;
    
    private static final long serialVersionUID = 1L;
}