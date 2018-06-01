/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.util;

import javax.vecmath.Vector3d;

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
     * @param a The operand.
     * @param b The modulus.
     * @return x modulo y
     */
    public static float mod(float a, float b) {
        if (a < 0) {
            return a + (float) Math.ceil(-a / b) * b;
        } else if (a >= b) {
            return a - (float) Math.floor(a / b) * b;
        } else {
            return a;
        }
    }

    /**
     * Calculates x modulo y. This is different than the Java remainder operator
     * (%) in that it always returns a positive value.
     *
     * @param a The operand.
     * @param b The modulus.
     * @return x modulo y
     */
    public static double mod(double a, double b) {
        if (a < 0) {
            return a + Math.ceil(-a / b) * b;
        } else if (a >= b) {
            return a - Math.floor(a / b) * b;
        } else {
            return a;
        }
    }

    /**
     * Calculates x modulo y. This is different than the Java remainder operator
     * (%) in that it always returns a positive value.
     *
     * @param a The operand.
     * @param b The modulus.
     * @return x modulo y
     */
    public static int mod(int a, int b) {
        if (a < 0) {
            return (a % b + b) % b;
        } else {
            return a % b;
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
     * Calculates the distance between two points in 2D space.
     *
     * @param dx The difference along the x axis between the two points.
     * @param dy The difference along the y axis between the two points.
     * @return The distance between the two points.
     */
    public static float getDistance(float dx, float dy) {
        return (float) Math.sqrt(dx * dx + dy * dy);
    }
    
    /**
     * Calculates the distance between two points in 3D space.
     *
     * @param dx The difference along the x axis between the two points.
     * @param dy The difference along the y axis between the two points.
     * @param dz The difference along the z axis between the two points.
     * @return The distance between the two points.
     */
    public static float getDistance(float dx, float dy, float dz) {
        final double d = Math.sqrt(dx * dx + dy * dy);
        return (float) Math.sqrt(d * d + dz * dz);
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

    public static double clamp(double min, double value, double max) {
        return (value < min) ? min : ((value > max) ? max : value);
    }

    /**
     * Rotate a vector counterclockwise around an axis.
     *
     * @param vector The vector to rotate.
     * @param axis The axis around which to rotate it.
     * @param angle How many radians to rotate it counterclockwise.
     * @return The rotated vector.
     */
    public static Vector3d rotateVectorCC(Vector3d vector, Vector3d axis, double angle){
        double x = vector.x, y = vector.y, z = vector.z;
        double u = axis.x, v = axis.y, w = axis.z;
        double cosAngle = Math.cos(angle);
        double sinAngle = Math.sin(angle);
        double product = u * x + v * y + w * z;
        double xPrime = u * product * (1d - cosAngle) + x * cosAngle + (-w * y + v * z) * sinAngle;
        double yPrime = v * product * (1d - cosAngle) + y * cosAngle + (w * x - u * z) * sinAngle;
        double zPrime = w * product * (1d - cosAngle) + z * cosAngle + (-v * x + u * y) * sinAngle;
        return new Vector3d(xPrime, yPrime, zPrime);
    }

    /**
     * Calculates the shortest distance of a point to a line segment.
     *
     * @param px The X coordinate of the point.
     * @param py The Y coordinate of the point.
     * @param vx The X coordinate of the start of the line segment.
     * @param vy The Y coordinate of the start of the line segment.
     * @param wx The X coordinate of the end of the line segment.
     * @param wy The Y coordinate of the end of the line segment.
     * @return The shortest distance of the specified point to the specified
     * line segment.
     */
    public static double distanceToLineSegment(int px, int py, int vx, int vy, int wx, int wy) {
        return Math.sqrt(distToSegmentSquared(px, py, vx, vy, wx, wy));
    }

    private static double dist2(double x1, double y1, double x2, double y2) {
        return (x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2);
    }

    private static double distToSegmentSquared(long vx, long vy, long wx, long wy, long px, long py) {
        double l2 = dist2(vx, vy, wx, wy);
        if (l2 == 0) {
            return dist2(px, py, vx, vy);
        }
        double t = ((px - vx) * (wx - vx) + (py - vy) * (wy - vy)) / l2;
        t = Math.max(0, Math.min(1, t));
        return dist2(px, py, vx + t * (wx - vx), vy + t * (wy - vy));
    }

    public static final double TWO_PI = Math.PI * 2;

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