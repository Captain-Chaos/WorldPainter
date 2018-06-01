/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.util;

import org.pepsoft.util.MathUtils;

/**
 *
 * @author pepijn
 */
public final class GeometryUtil {
    private GeometryUtil() {
        // Prevent instantiation
    }

    /**
     * Visit all the points along the outline of a circle in an integer
     * coordinate space with the centre at 0,0. The order in which the points
     * are visited is not defined. The visitor may abort the process at any
     * point by returning <code>false</code>.
     *
     * @param radius The radius of the circle to visit.
     * @param visitor The visitor to invoke for each point.
     * @return <code>true</code> if the visitor returned true for each point
     *     (and therefore every point was visited). <code>false</code> if the
     *     visitor returned <code>false</code> for some point and the process
     *     was aborted.
     */
    public static boolean visitCircle(int radius, PlaneVisitor visitor) {
        int dx = radius, dy = 0;
        int radiusError = 1 - dx;
        while (dx >= dy) {
            if (! visitor.visit( dx,  dy, radius)) {return false;}
            if (! visitor.visit(-dx,  dy, radius)) {return false;}
            if (! visitor.visit(-dx, -dy, radius)) {return false;}
            if (! visitor.visit( dx, -dy, radius)) {return false;}
            if (dx != dy) {
                if (! visitor.visit( dy,  dx, radius)) {return false;}
                if (! visitor.visit(-dy,  dx, radius)) {return false;}
                if (! visitor.visit(-dy, -dx, radius)) {return false;}
                if (! visitor.visit( dy, -dx, radius)) {return false;}
            }

            dy++;
            if (radiusError < 0) {
                radiusError += 2 * dy + 1;
            } else {
                dx--;
                radiusError += 2 * (dy - dx + 1);
            }
        }
        return true;
    }

    /**
     * Visit all the points on the face of a filled circular disk in an integer
     * coordinate space with the centre at 0,0. The order in which the points
     * are visited is not defined. The visitor may abort the process at any
     * point by returning <code>false</code>.
     *
     * @param radius The radius of the circle to visit.
     * @param visitor The visitor to invoke for each point.
     * @return <code>true</code> if the visitor returned true for each point
     *     (and therefore every point was visited). <code>false</code> if the
     *     visitor returned <code>false</code> for some point and the process
     *     was aborted.
     */
    public static boolean visitFilledCircle(int radius, PlaneVisitor visitor) {
        int dx = radius, dy = 0;
        int radiusError = 1 - dx;
        while (dx >= dy) {
            if (! visitor.visit(0, -dy, dy)) {return false;}
            if ((dy > 0) && (! visitor.visit(0, dy, dy))) {return false;}
            for (int i = 1; i <= dx; i++) {
                final float d = MathUtils.getDistance(i, dy);
                if (! visitor.visit(-i, -dy, d)) {return false;}
                if (! visitor.visit(-i, dy, d)) {return false;}
                if (! visitor.visit(i, -dy, d)) {return false;}
                if (! visitor.visit(i, dy, d)) {return false;}
            }
            if (dx > 0) {
                if (! visitor.visit(0, -dx, dx)) {return false;}
                if (! visitor.visit(0, dx, dx)) {return false;}
            }
            for (int i = 1; i <= dy; i++) {
                final float d = MathUtils.getDistance(i, dx);
                if (! visitor.visit(-i, -dx, d)) {return false;}
                if (! visitor.visit(-i, dx, d)) {return false;}
                if (! visitor.visit(i, -dx, d)) {return false;}
                if (! visitor.visit(i, dx, d)) {return false;}
            }

            dy++;
            if (radiusError < 0) {
                radiusError += 2 * dy + 1;
            } else {
                dx--;
                radiusError += 2 * (dy - dx + 1);
            }
        }
        return true;
    }


    /**
     * Visit all the points inside a spherical volume in an integer coordinate
     * space with the centre at 0,0,0. The order in which the points are visited
     * is not defined. The visitor may abort the process at any point by
     * returning <code>false</code>.
     *
     * @param radius The radius of the sphere to visit.
     * @param visitor The visitor to invoke for each point.
     * @return <code>true</code> if the visitor returned true for each point
     *     (and therefore every point was visited). <code>false</code> if the
     *     visitor returned <code>false</code> for some point and the process
     *     was aborted.
     */
    public static boolean visitFilledSphere(int radius, VolumeVisitor visitor) {
        for (int dz = 0; dz <= radius; dz++) {
            int r = (int) (Math.sqrt(radius * radius - dz * dz) + 0.5);
            final int finalDz = dz;
            if (! visitFilledCircle(r, ((dx, dy, d) -> visitor.visit(dx, dy, finalDz, MathUtils.getDistance(dx, dy, finalDz))))) {
                return false;
            }
            if ((dz > 0) && (! visitFilledCircle(r, ((dx, dy, d) -> visitor.visit(dx, dy, -finalDz, MathUtils.getDistance(dx, dy, -finalDz)))))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Visit all the points along a straight line on a 2D plane. The visitor may
     * abort the process at any point by returning <code>false</code>.
     *
     * @param x1 The X coordinate of the start of the line.
     * @param y1 The Y coordinate of the start of the line.
     * @param x2 The X coordinate of the end of the line.
     * @param y2 The Y coordinate of the end of the line.
     * @param visitor The visitor to invoke for each point.
     * @return <code>true</code> if the visitor returned true for each point
     *     (and therefore every point was visited). <code>false</code> if the
     *     visitor returned <code>false</code> for some point and the process
     *     was aborted.
     */
    public static boolean visitLine(int x1, int y1, int x2, int y2, LineVisitor visitor) {
        if ((x1 == x2) && (y1 == y2)) {
            return visitor.visit(x1, y1, 0);
        }
        int dx = x2 - x1;
        int dy = y2 - y1;
        if (Math.abs(dx) > Math.abs(dy)) {
            float y = y1, offset = (float) dy / Math.abs(dx);
            float distance = 0, stepLength = (float) Math.sqrt(1 + ((1 + offset) * (1 + offset)));
            dx = (dx < 0) ? -1 : 1;
            for (int x = x1; x != x2; x += dx) {
                if (! visitor.visit(x, (int) y, distance)) {
                    return false;
                }
                y += offset;
                distance += stepLength;
            }
        } else {
            float x = x1, offset = (float) dx / Math.abs(dy);
            float distance = 0, stepLength = (float) Math.sqrt(1 + ((1 + offset) * (1 + offset)));
            dy = (dy < 0) ? -1 : 1;
            for (int y = y1; y != y2; y += dy) {
                if (! visitor.visit((int) x, y, distance)) {
                    return false;
                }
                x += offset;
                distance += stepLength;
            }
        }
        return true;
    }

    /**
     * Visit all the points along a straight line on a 2D plane. The visitor may
     * abort the process at any point by returning <code>false</code>. Points on
     * the line can optionally be skipped by specifying the <code>every</code>
     * parameter larger than one.
     *
     * @param x1 The X coordinate of the start of the line.
     * @param y1 The Y coordinate of the start of the line.
     * @param x2 The X coordinate of the end of the line.
     * @param y2 The Y coordinate of the end of the line.
     * @param every The interval between performances of the specified task.
     * @param visitor The visitor to invoke for each point.
     * @return <code>true</code> if the visitor returned true for each point
     *     (and therefore every point was visited). <code>false</code> if the
     *     visitor returned <code>false</code> for some point and the process
     *     was aborted.
     */
    public static boolean visitLine(int x1, int y1, int x2, int y2, int every, LineVisitor visitor) {
        if ((x1 == x2) && (y1 == y2)) {
            return visitor.visit(x1, y1, 0);
        }
        int dx = x2 - x1;
        int dy = y2 - y1;
        int count = every / 2;
        if (Math.abs(dx) > Math.abs(dy)) {
            float y = y1, offset = (float) dy / Math.abs(dx);
            float distance = 0, stepLength = (float) Math.sqrt(1 + ((1 + offset) * (1 + offset)));
            dx = (dx < 0) ? -1 : 1;
            for (int x = x1; x != x2; x += dx) {
                if (((count % every) == 0) && (! visitor.visit(x, (int) y, distance))) {
                    return false;
                }
                y += offset;
                distance += stepLength;
                count++;
            }
        } else {
            float x = x1, offset = (float) dx / Math.abs(dy);
            float distance = 0, stepLength = (float) Math.sqrt(1 + ((1 + offset) * (1 + offset)));
            dy = (dy < 0) ? -1 : 1;
            for (int y = y1; y != y2; y += dy) {
                if (((count % every) == 0) && (! visitor.visit((int) x, y, distance))) {
                    return false;
                }
                x += offset;
                distance += stepLength;
                count++;
            }
        }
        return true;
    }

    @FunctionalInterface
    public interface LineVisitor {
        /**
         * Visit the specified absolute location along a straight line on a 2D
         * plane.
         *
         * @param x The absolute X coordinate to visit.
         * @param y The absolute Y coordinate to visit.
         * @param d The distance from the start of the line.
         * @return <code>true</code> if the process should continue;
         * <code>false</code> if no more points should be visited on the line.
         */
        boolean visit(int x, int y, float d);
    }

    @FunctionalInterface
    public interface PlaneVisitor {
        /**
         * Visit the specified location relative to the origin of the plane.
         * 
         * @param dx The x coordinate to visit relative to the origin of the
         *     plane.
         * @param dy The y coordinate to visit relative to the origin of the
         *     plane.
         * @param d The distance from the origin.
         * @return <code>true</code> if the process should continue;
         * <code>false</code> if no more points should be visited on the plane.
         */
        boolean visit(int dx, int dy, float d);
    }

    @FunctionalInterface
    public interface VolumeVisitor {
        /**
         * Visit the specified location relative to the origin of the volume.
         *
         * @param dx The x coordinate to visit relative to the origin of the
         *     volume.
         * @param dy The y coordinate to visit relative to the origin of the
         *     volume.
         * @param dz The z coordinate to visit relative to the origin of the
         *     volume.
         * @param d The distance from the origin.
         * @return <code>true</code> if the process should continue;
         * <code>false</code> if no more points should be visited in the volume.
         */
        boolean visit(int dx, int dy, int dz, float d);
    }
}