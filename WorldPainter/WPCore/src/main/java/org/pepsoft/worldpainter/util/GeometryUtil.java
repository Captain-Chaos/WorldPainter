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
    public static boolean visitCircle(int radius, GeometryVisitor visitor) {
        final float d = radius;
        int dx = radius, dy = 0;
        int radiusError = 1 - dx;
        while (dx >= dy) {
            if (! visitor.visit( dx,  dy, d)) {return false;}
            if (! visitor.visit( dy,  dx, d)) {return false;}
            if (! visitor.visit(-dx,  dy, d)) {return false;}
            if (! visitor.visit(-dy,  dx, d)) {return false;}
            if (! visitor.visit(-dx, -dy, d)) {return false;}
            if (! visitor.visit(-dy, -dx, d)) {return false;}
            if (! visitor.visit( dx, -dy, d)) {return false;}
            if (! visitor.visit( dy, -dx, d)) {return false;}

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
    public static boolean visitFilledCircle(int radius, GeometryVisitor visitor) {
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

    @FunctionalInterface
    public interface GeometryVisitor {
        /**
         * Visit the specified location relative to the origin of the geometric
         * shape.
         * 
         * @param dx The x coordinate to visit relative to the origin of the
         *     geometric shape.
         * @param dy The y coordinate to visit relative to the origin of the
         *     geometric shape.
         * @param d The distance from the origin.
         * @return <code>true</code> if the process should continue;
         * <code>false</code> if no more points should be visited on the shape.
         */
        boolean visit(int dx, int dy, float d);
    }
}