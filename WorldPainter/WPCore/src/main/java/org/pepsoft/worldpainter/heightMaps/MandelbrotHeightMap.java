package org.pepsoft.worldpainter.heightMaps;

import javax.swing.*;

/**
 * Created by Pepijn Schmitz on 02-09-16.
 */
public class MandelbrotHeightMap extends AbstractHeightMap {
    @Override
    public float getHeight(float x0, float y0) {
        float x = 0.0f, y = 0.0f;
        int iteration = 0;
        while ((x * x + y * y < 4) && (iteration < MAX_ITERATIONS)) {
            float xtemp = x * x - y * y + x0;
            y = 2 * x * y + y0;
            x = xtemp;
            iteration++;
        }
        return iteration;
    }

    @Override
    public Icon getIcon() {
        return null;
    }

    @Override
    public float[] getRange() {
        return RANGE;
    }

    private static final int MAX_ITERATIONS = 255;
    private static final float[] RANGE = {0.0f, MAX_ITERATIONS};
}