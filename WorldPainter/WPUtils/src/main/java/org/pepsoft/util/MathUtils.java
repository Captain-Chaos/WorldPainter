/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.util;

/**
 *
 * @author pepijn
 */
public final class MathUtils {
    private MathUtils() {
        // Prevent instantiation
    }
    
    /**
     * Positive integer powers.
     */
    public static int pow(int x, int y) {
        if ((x >= 0) && (x < 50) && (y < 50)) {
            return INTEGER_POWERS[x][y];
        } else {
            switch (y) {
                case 0:
                    return 1;
                case 1:
                    return x;
                case 2:
                    return x * x;
                case 3:
                    return x * x * x;
                default:
                    if (y < 0) {
                        throw new IllegalArgumentException("y " + y + " < 0");
                    } else {
                        for (int i = 1; i < y; i++) {
                            x *= x;
                        }
                        return x;
                    }
            }
        }
    }

    /**
     * Calculates x modulo y. This is different than the Java remainder operator
     * (%) in that it always returns a positive value.
     *
     * <p>Hint, if y is a power of two, it is much faster to do a binary AND
     * with (y - 1)
     *
     * @param x The operand.
     * @param y The modulus.
     * @return x modulo y
     * @deprecated Use {@link Math#floorMod(int, int)}
     */
    @Deprecated
    public static int mod(int x, int y) {
        if (x >= 0) {
            return x % y;
        } else {
            int mod = x % y;
            if (mod == 0) {
                return 0;
            } else {
                return y + mod;
            }
        }
    }

    /**
     * Calculates x modulo y. This is different than the Java remainder operator
     * (%) in that it always returns a positive value.
     *
     * @param x The operand.
     * @param y The modulus.
     * @return x modulo y
     */
    public static double mod(double x, double y) {
        if (x < 0) {
            return x + Math.ceil(-x / y) * y;
        } else if (x >= y) {
            return x - Math.floor(x / y) * y;
        } else {
            return x;
        }
    }

    /**
     * Calculates the distance between two points in two dimensional space. Uses
     * a lookup table for distances below 300 for speed.
     *
     * @param x1 The X coordinate of the first point.
     * @param y1 The Y coordinate of the first point.
     * @param x2 The X coordinate of the second point.
     * @param y2 The Y coordinate of the second point.
     * @return The distance between the two points.
     */
    public static float getDistance(int x1, int y1, int x2, int y2) {
        return getDistance(x2 - x1, y2 - y1);
    }

    /**
     * Calculates the distance between two points in three dimensional space.
     * Uses a lookup table for distances below 50 for speed.
     *
     * @param x1 The X coordinate of the first point.
     * @param y1 The Y coordinate of the first point.
     * @param z1 The Z coordinate of the first point.
     * @param x2 The X coordinate of the second point.
     * @param y2 The Y coordinate of the second point.
     * @param z2 The Z coordinate of the second point.
     * @return The distance between the two points.
     */
    public static float getDistance(int x1, int y1, int z1, int x2, int y2, int z2) {
        return getDistance(x2 - x1, y2 - y1, z2 - z1);
    }

    /**
     * Calculates the distance between two points. Uses a lookup table for
     * distances below 300 for speed.
     *
     * @param dx The difference along the x axis between the two points.
     * @param dy The difference along the y axis between the two points.
     * @return The distance between the two points.
     */
    public static float getDistance(int dx, int dy) {
        dx = Math.abs(dx);
        dy = Math.abs(dy);
        if ((dx <= 300) && (dy <= 300)) {
            return DISTANCES_2D[dx][dy];
        } else {
            return (float) Math.sqrt(dx * dx + dy * dy);
        }
    }

    /**
     * Calculates the distance between two points.
     *
     * @param dx The difference along the x axis between the two points.
     * @param dy The difference along the y axis between the two points.
     * @return The distance between the two points.
     */
    public static float getDistance(float dx, float dy) {
        return (float) Math.sqrt(dx * dx + dy * dy);
    }
    
    public static float getDistance(int dx, int dy, int dz) {
        dx = Math.abs(dx);
        dy = Math.abs(dy);
        dz = Math.abs(dz);
        if ((dx <= 50) && (dy <= 50) && (dz <= 50)) {
            return DISTANCES_3D[dx][dy][dz];
        } else {
            return (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        }
    }
    
    public static int clamp(int min, int value, int max) {
        return (value < min) ? min : ((value > max) ? max : value);
    }

    public static float clamp(float min, float value, float max) {
        return (value < min) ? min : ((value > max) ? max : value);
    }
    
    private static final float[][] DISTANCES_2D = new float[301][301];
    private static final float[][][] DISTANCES_3D = new float[51][51][51];
    private static final int[][] INTEGER_POWERS = new int[50][50];

    static {
        for (int dx = 0; dx <= 300; dx++) {
            for (int dy = 0; dy <= 300; dy++) {
                DISTANCES_2D[dx][dy] = (float) Math.sqrt(dx * dx + dy * dy);
            }
        }

        for (int dx = 0; dx <= 50; dx++) {
            for (int dy = 0; dy <= 50; dy++) {
                for (int dz = 0; dz <= 50; dz++) {
                    DISTANCES_3D[dx][dy][dz] = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
                }
            }
        }
        
        for (int x = 0; x < 50; x++) {
            for (int y = 0; y < 50; y++) {
                long n = 1;
                for (int i = 0; i < y; i++) {
                    n *= x;
                    if (n > Integer.MAX_VALUE) {
                        n = Integer.MAX_VALUE;
                        break;
                    }
                }
                INTEGER_POWERS[x][y] = (int) n;
            }
        }
    }
}