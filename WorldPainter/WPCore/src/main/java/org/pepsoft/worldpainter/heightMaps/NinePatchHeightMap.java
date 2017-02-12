/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.heightMaps;

import org.pepsoft.util.IconUtils;
import org.pepsoft.util.MathUtils;

import javax.swing.*;

/**
 * A heightmap creating a square with rounded corners, with a smoothly descending border on the outside.
 *
 * <p>The <code>innerSize</code> is the inner size of the square (from the centre, so half the actual width) without the rounded corners.<br>
 * The <code>borderSize</code> is the radius of the rounded corners.<br>
 * The <code>coastSize</code> is the distance over which the height decreases to zero outside the border.<br>
 * The <code>height</code> is the height of the part described by <code>innerSize + borderSize</code>. From there the height sinuously decreases to zero over <code>coastSize</code> pixels.
 *
 * <p><img src="doc-files/ninepatch.png"/>
 *
 * <p>By setting the <code>innerSize</code> to zero you can create a completely circular shape. The two constructors that take only two <code>int</code>s are convenience constructors to do that.
 *
 * <p>To have entirely sharp, perpendicular corners, set the <code>borderSize</code> to zero.
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
        this.innerSizeX = innerSize;
        this.innerSizeY = innerSize;
        this.borderSize = borderSize;
        this.coastSize = coastSize;
        this.height = height;
        sizesChanged();
    }

    public int getInnerSizeX() {
        return innerSizeX;
    }

    public int getInnerSizeY() {
        return innerSizeY;
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

    public void setBorderSize(int borderSize) {
        this.borderSize = borderSize;
        sizesChanged();
    }

    public void setCoastSize(int coastSize) {
        this.coastSize = coastSize;
        sizesChanged();
    }

    public void setHeight(float height) {
        this.height = height;
        sizesChanged();
    }

    public void setInnerSizeX(int innerSizeX) {
        this.innerSizeX = innerSizeX;
        sizesChanged();
    }

    public void setInnerSizeY(int innerSizeY) {
        this.innerSizeY = innerSizeY;
        sizesChanged();
    }

    public void setInnerSize(int innerSize) {
        this.innerSizeX = innerSize;
        this.innerSizeY = innerSize;
        sizesChanged();
    }

    // HeightMap

    @Override
    public float getHeight(float x, float y) {
        x = Math.abs(x);
        y = Math.abs(y);
        if (x < innerSizeX) {
            if (y < innerSizeY) {
                // On the continent
                return height;
            } else if (y < borderTotalY) {
                // Border
                return height;
            } else if (y < coastTotalY) {
                // Coast
                return (float) (Math.cos((y - borderTotalY) / coastSize * Math.PI) * halfHeight) + halfHeight;
            } else {
                // Outside the continent
                return 0;
            }
        } else if (x < borderTotalX) {
            if (y < innerSizeY) {
                // Border
                return height;
            } else if (y < coastTotalY) {
                // Corner
                float distanceFromCorner = MathUtils.getDistance(x - innerSizeX, y - innerSizeY);
                if (distanceFromCorner < borderSize) {
                    // Border
                    return height;
                } else if (distanceFromCorner - borderSize < coastSize) {
                    return (float) (Math.cos((distanceFromCorner - borderSize) / coastSize * Math.PI) * halfHeight) + halfHeight;
                    // Coast
                } else {
                    // Outside the continent
                    return 0;
                }
            } else {
                // Outside the continent
                return 0;
            }
        } else if (x < coastTotalX) {
            if (y < innerSizeY) {
                // Coast
                return (float) (Math.cos((x - borderTotalX) / coastSize * Math.PI) * halfHeight) + halfHeight;
            } else if (y < coastTotalY) {
                // Corner
                float distanceFromCorner = MathUtils.getDistance(x - innerSizeX, y - innerSizeY);
                if (distanceFromCorner < borderSize) {
                    // Border
                    return height;
                } else if (distanceFromCorner - borderSize < coastSize) {
                    return (float) (Math.cos((distanceFromCorner - borderSize) / coastSize * Math.PI) * halfHeight) + halfHeight;
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
    public Icon getIcon() {
        return ICON_NINE_PATCH_HEIGHTMAP;
    }

    @Override
    public float[] getRange() {
        return new float[] {0.0f, height};
    }

    private void sizesChanged() {
        halfHeight = height / 2;
        borderTotalX = innerSizeX + borderSize;
        borderTotalY = innerSizeY + borderSize;
        coastTotalX = borderTotalX + coastSize;
        coastTotalY = borderTotalY + coastSize;
    }

    private int innerSizeX, innerSizeY, borderSize, coastSize;
    private int borderTotalX, borderTotalY, coastTotalX, coastTotalY;
    private float height, halfHeight;
    
    private static final long serialVersionUID = 1L;
    private static final Icon ICON_NINE_PATCH_HEIGHTMAP = IconUtils.loadScaledIcon("org/pepsoft/worldpainter/icons/nine_patch.png");
}