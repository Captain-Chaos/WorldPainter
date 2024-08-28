/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.util;

import java.util.function.BiFunction;

import static java.lang.Math.PI;
import static org.pepsoft.worldpainter.Constants.TILE_SIZE;

/**
 *
 * @author pepijn
 */
public final class MathUtils {
    private MathUtils() {
        // Prevent instantiation
    }
    
    public static float getSmallestDistanceFromOrigin(int x, int y) {
        return Math.min(
            Math.min(org.pepsoft.util.MathUtils.getDistance(x * TILE_SIZE + 0.5f, y * TILE_SIZE + 0.5f),
                org.pepsoft.util.MathUtils.getDistance(x * TILE_SIZE + TILE_SIZE - 0.5f, y * TILE_SIZE + 0.5f)),
            Math.min(org.pepsoft.util.MathUtils.getDistance(x * TILE_SIZE + 0.5f, y * TILE_SIZE + TILE_SIZE - 0.5f),
                org.pepsoft.util.MathUtils.getDistance(x * TILE_SIZE + TILE_SIZE - 0.5f, y * TILE_SIZE + TILE_SIZE - 0.5f)));
    }

    public static float getLargestDistanceFromOrigin(int x, int y) {
        return Math.max(
            Math.max(org.pepsoft.util.MathUtils.getDistance(x * TILE_SIZE, y * TILE_SIZE),
                org.pepsoft.util.MathUtils.getDistance(x * TILE_SIZE + TILE_SIZE - 1, y * TILE_SIZE)),
            Math.max(org.pepsoft.util.MathUtils.getDistance(x * TILE_SIZE, y * TILE_SIZE + TILE_SIZE - 1),
                org.pepsoft.util.MathUtils.getDistance(x * TILE_SIZE + TILE_SIZE - 1, y * TILE_SIZE + TILE_SIZE - 1)));
    }

    /**
     * Get the clockwise angle in radians from the origin to the indicated coordinates, where 0 is due east.
     *
     * @param dx The X coordinate to which to calculate the angle, where the value increases to the right.
     * @param dy The Y coordinate to which to calculate the angle, where the value increases downwards.
     * @return The clockwise angle in radians from the origin to the specified coordinates.
     */
    public static double getAngle(int dx, int dy) {
        double α = Math.atan((double) dy / dx);
        if (dx < 0) {
            α += PI;
        }
        if (α < 0) {
            α += PI * 2;
        }
        return α;
    }

    /**
     * Get the lowest value in a square area.
     */
    public static int getLowest2D(int size, BiFunction<Integer, Integer, Integer> valueProvider) {
        int lowestValue = Integer.MAX_VALUE;
        for (int dx = 0; dx < size; dx++) {
            for (int dy = 0; dy < size; dy++) {
                final int value = valueProvider.apply(dx, dy);
                if (value < lowestValue) {
                    lowestValue = value;
                }
            }
        }
        return lowestValue;
    }
}