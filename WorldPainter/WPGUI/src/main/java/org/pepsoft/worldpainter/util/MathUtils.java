/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.util;

import static org.pepsoft.worldpainter.Constants.*;

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
}