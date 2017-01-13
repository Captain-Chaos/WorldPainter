package org.pepsoft.worldpainter.heightMaps;

import org.pepsoft.util.IconUtils;
import org.pepsoft.util.MathUtils;

import javax.swing.*;

/**
 * Created by pepijn on 28-3-16.
 */
public class BandedHeightMap extends AbstractHeightMap {
    public float getSegment1EndHeight() {
        return segment1EndHeight;
    }

    public void setSegment1EndHeight(float segment1EndHeight) {
        this.segment1EndHeight = segment1EndHeight;
        recalculate();
    }

    public int getSegment1Length() {
        return segment1Length;
    }

    public void setSegment1Length(int segment1Length) {
        this.segment1Length = segment1Length;
        recalculate();
    }

    public int getSegment2Length() {
        return segment2Length;
    }

    public void setSegment2Length(int segment2Length) {
        this.segment2Length = segment2Length;
        recalculate();
    }

    public float getSegment2EndHeight() {
        return segment2EndHeight;
    }

    public void setSegment2EndHeight(float segment2EndHeight) {
        this.segment2EndHeight = segment2EndHeight;
        recalculate();
    }

    public boolean isSmooth() {
        return smooth;
    }

    public void setSmooth(boolean smooth) {
        this.smooth = smooth;
    }

    // HeightMap

    @Override
    public float getHeight(float x0, float y0) {
        final float d = MathUtils.mod(x0, totalLength);
        if (d < segment1Length) {
            if (smooth) {
                return (float) (segment2EndHeight + (0.5 - Math.cos(d * Math.PI / segment1Length) / 2) * segment1EndDelta);
            } else {
                return segment2EndHeight + d / segment1Length * segment1EndDelta;
            }
        } else {
            if (smooth) {
                return (float) (segment1EndHeight + (0.5 - Math.cos((d - segment1Length) * Math.PI / segment2Length) / 2) * segment2EndDelta);
            } else {
                return segment1EndHeight + (d - segment1Length) / segment2Length * segment2EndDelta;
            }
        }
    }

    @Override
    public Icon getIcon() {
        return ICON_BANDED_HEIGHTMAP;
    }

    @Override
    public float[] getRange() {
        return RANGE;
    }

    private void recalculate() {
        totalLength = segment1Length + segment2Length;
        segment1EndDelta = segment1EndHeight - segment2EndHeight;
        segment2EndDelta = segment2EndHeight - segment1EndHeight;
    }

    private int segment1Length = 100, segment2Length = 100, totalLength = 200;
    private float segment1EndHeight = 1f, segment2EndHeight, segment1EndDelta = 1f, segment2EndDelta;
    private boolean smooth;

    private static final long serialVersionUID = 1L;
    private static final Icon ICON_BANDED_HEIGHTMAP = IconUtils.loadScaledIcon("org/pepsoft/worldpainter/icons/sawtooth.png");
    private static final float[] RANGE = {0.0f, 1.0f};
}