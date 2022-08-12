package org.pepsoft.worldpainter.heightMaps;

import org.pepsoft.util.IconUtils;
import org.pepsoft.util.MathUtils;
import org.pepsoft.worldpainter.HeightMap;

import javax.swing.*;
import java.awt.*;

import static org.pepsoft.util.MathUtils.clamp;

/**
 * A height map that performs bicubic interpolation on an underlying height map.
 */
public class BicubicHeightMap extends DelegatingHeightMap {
    public BicubicHeightMap(HeightMap baseHeightMap) {
        this(baseHeightMap, false);
    }

    public BicubicHeightMap(HeightMap baseHeightMap, boolean repeat) {
        super("baseHeightMap");
        this.repeat = repeat;
        extent = baseHeightMap.getExtent();
        if ((extent != null) && ((extent.x != 0) || (extent.y != 0))) {
            // TODO lift this restriction
            throw new IllegalArgumentException("Only extents with the origin at zero are currently supported");
        }
        width = (extent != null) ? extent.width : -1;
        height = (extent != null) ? extent.height : -1;
        if (repeat && (extent == null)) {
            throw new IllegalArgumentException("Base height map must have an extent if it is to be repeated");
        }
        setHeightMap(0, baseHeightMap);
    }

    public boolean isRepeat() {
        return repeat;
    }

    // HeightMap

    @Override
    protected float doGetHeight(int x, int y) {
        if (repeat) {
            return children[0].getHeight(MathUtils.mod(x, width), MathUtils.mod(y, height));
        } else {
            return children[0].getHeight(x, y);
        }
    }

    @SuppressWarnings("SuspiciousNameCombination") // Don't worry about it
    @Override
    protected float doGetHeight(float x, float y) {
        x -= Math.signum(x) / 2;
        y -= Math.signum(y) / 2;
        final int xFloor = (int) Math.floor(x), yFloor = (int) Math.floor(y);
        final float xDelta = x - xFloor, yDelta = y - yFloor;
        final float upperLeftValue = getExtHeight(xFloor, yFloor);
        final float upperRightValue = getExtHeight(xFloor, yFloor + 1);
        final float lowerLeftValue = getExtHeight(xFloor + 1, yFloor);
        final float lowerRightValue = getExtHeight(xFloor + 1, yFloor + 1);
        final float min = Math.min(Math.min(upperLeftValue, upperRightValue), Math.min(lowerLeftValue, lowerRightValue));
        final float max = Math.max(Math.max(upperLeftValue, upperRightValue), Math.max(lowerLeftValue, lowerRightValue));
        final float val1 = cubicInterpolate(getExtHeight(xFloor - 1, yFloor - 1), getExtHeight(xFloor - 1, yFloor), getExtHeight(xFloor - 1, yFloor + 1), getExtHeight(xFloor - 1, yFloor + 2), yDelta);
        final float val2 = cubicInterpolate(getExtHeight(xFloor,     yFloor - 1), upperLeftValue,                   upperRightValue,                      getExtHeight(xFloor,     yFloor + 2), yDelta);
        final float val3 = cubicInterpolate(getExtHeight(xFloor + 1, yFloor - 1), lowerLeftValue,                   lowerRightValue,                      getExtHeight(xFloor + 1, yFloor + 2), yDelta);
        final float val4 = cubicInterpolate(getExtHeight(xFloor + 2, yFloor - 1), getExtHeight(xFloor + 2, yFloor), getExtHeight(xFloor + 2, yFloor + 1), getExtHeight(xFloor + 2, yFloor + 2), yDelta);
        // Constrain the value between the heights of the four corners, to try and mitigate haloing/ringing
        return clamp(min, cubicInterpolate(val1, val2, val3, val4, xDelta), max);
    }

    /**
     * Private version of {@link #getHeight(int, int)}} which extends the
     * edge pixels of the image if it is non-repeating, to make the bicubic
     * interpolation work correctly around the edges.
     */
    private float getExtHeight(float x, float y) {
        if (repeat) {
            return children[0].getHeight(MathUtils.mod(x, width), MathUtils.mod(y, height));
        } else if ((extent == null) || extent.contains(x, y)) {
            return children[0].getHeight(x, y);
        } else if (x < 0) {
            // West of the extent
            if (y < 0) {
                // Northwest of the extent
                return children[0].getHeight(0f, 0f);
            } else if (y < height) {
                // Due west of the extent
                return children[0].getHeight(0f, y);
            } else {
                // Southwest of the extent
                return children[0].getHeight(0f, height - 1f);
            }
        } else if (x < width) {
            // North or south of the extent
            if (y < 0) {
                // Due north of the extent
                return children[0].getHeight(x, 0f);
            } else {
                // Due south of the extent
                return children[0].getHeight(x, height - 1f);
            }
        } else {
            // East of the extent
            if (y < 0) {
                // Northeast of the extent
                return children[0].getHeight(width - 1f, 0f);
            } else if (y < height) {
                // Due east of the extent
                return children[0].getHeight(width - 1f, y);
            } else {
                // Southeast of the extent
                return children[0].getHeight(width - 1f, height - 1f);
            }
        }
    }

    @Override
    public Rectangle getExtent() {
        return extent;
    }

    @Override
    public Icon getIcon() {
        // TODO separate icon
        return ICON_BITMAP_HEIGHTMAP;
    }

    @Override
    public float[] getRange() {
        return children[0].getRange();
    }

    /**
     * Cubic interpolation using Catmull-Rom splines.
     */
    private float cubicInterpolate(float y0, float y1, float y2, float y3, float μ) {
        return y1 + 0.5f * μ * (y2 - y0 + μ * (2.0f * y0 - 5.0f * y1 + 4.0f * y2 - y3 + μ * (3.0f * (y1 - y2) + y3 - y0)));
    }

    private final int width, height;
    private final Rectangle extent;
    private final boolean repeat;

    private static final long serialVersionUID = 1L;
    private static final Icon ICON_BITMAP_HEIGHTMAP = IconUtils.loadScaledIcon("org/pepsoft/worldpainter/icons/height_map.png");
}