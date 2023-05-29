package org.pepsoft.worldpainter.heightMaps;

import org.pepsoft.util.IconUtils;
import org.pepsoft.util.MathUtils;

import javax.swing.*;

/**
 * Created by pepijn on 28-3-16.
 */
public class BandedHeightMap extends AbstractHeightMap {
    public BandedHeightMap(int segment1Length, double segment1EndHeight, int segment2Length, double segment2EndHeight, boolean smooth) {
        this(null, segment1Length, segment1EndHeight, segment2Length, segment2EndHeight, smooth);
    }

    public BandedHeightMap(String name, int segment1Length, double segment1EndHeight, int segment2Length, double segment2EndHeight, boolean smooth) {
        super(name);
        this.segment1Length = segment1Length;
        this.segment1EndHeight = segment1EndHeight;
        this.segment2Length = segment2Length;
        this.segment2EndHeight = segment2EndHeight;
        this.smooth = smooth;
        totalLength = segment1Length + segment2Length;
        segment1EndDelta = segment1EndHeight - segment2EndHeight;
        segment2EndDelta = segment2EndHeight - segment1EndHeight;
    }

    public double getSegment1EndHeight() {
        return segment1EndHeight;
    }

    public int getSegment1Length() {
        return segment1Length;
    }

    public int getSegment2Length() {
        return segment2Length;
    }

    public double getSegment2EndHeight() {
        return segment2EndHeight;
    }

    public boolean isSmooth() {
        return smooth;
    }

    // HeightMap

    @Override
    public double getHeight(float x0, float y0) {
        final float d = MathUtils.mod(x0, totalLength);
        if (d < segment1Length) {
            if (smooth) {
                return segment2EndHeight + (0.5 - Math.cos(d * Math.PI / segment1Length) / 2) * segment1EndDelta;
            } else {
                return segment2EndHeight + d / segment1Length * segment1EndDelta;
            }
        } else {
            if (smooth) {
                return segment1EndHeight + (0.5 - Math.cos((d - segment1Length) * Math.PI / segment2Length) / 2) * segment2EndDelta;
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
    public double[] getRange() {
        return RANGE;
    }

    private final int segment1Length, segment2Length, totalLength;
    private final double segment1EndHeight, segment2EndHeight, segment1EndDelta, segment2EndDelta;
    private final boolean smooth;

    private static final long serialVersionUID = 1L;
    private static final Icon ICON_BANDED_HEIGHTMAP = IconUtils.loadScaledIcon("org/pepsoft/worldpainter/icons/sawtooth.png");
    private static final double[] RANGE = {0.0, 1.0};
}