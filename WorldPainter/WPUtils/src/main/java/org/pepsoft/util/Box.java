/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.util;

import java.awt.*;

/**
 * A rectangular three dimensional volume, defined by two corners, the corner
 * with the lowest x, y and z coordinates being inclusive and the opposite
 * corner being exclusive.
 *
 * @author pepijn
 */
public final class Box implements Cloneable {
    public Box() {
        // Do nothing
    }

    public Box(int x1, int x2, int y1, int y2, int z1, int z2) {
        this.x1 = x1;
        this.y1 = y1;
        this.z1 = z1;
        this.x2 = x2;
        this.y2 = y2;
        this.z2 = z2;
        normalise();
    }

    public int getX1() {
        return x1;
    }

    public void setX1(int x1) {
        this.x1 = x1;
    }

    public int getX2() {
        return x2;
    }

    public void setX2(int x2) {
        this.x2 = x2;
    }

    public int getY1() {
        return y1;
    }

    public void setY1(int y1) {
        this.y1 = y1;
    }

    public int getY2() {
        return y2;
    }

    public void setY2(int y2) {
        this.y2 = y2;
    }

    public int getZ1() {
        return z1;
    }

    public void setZ1(int z1) {
        this.z1 = z1;
    }

    public int getZ2() {
        return z2;
    }

    public void setZ2(int z2) {
        this.z2 = z2;
    }

    /**
     * Get the size of the box along the X axis.
     *
     * @return The size of the box along the X axis.
     */
    public int getWidth() {
        return Math.abs(x2 - x1);
    }

    /**
     * Get the size of the box along the Y axis.
     *
     * @return The size of the box along the Y axis.
     */
    public int getLength() {
        return Math.abs(y2 - y1);
    }

    /**
     * Get the size of the box along the Z axis.
     *
     * @return The size of the box along the Z axis.
     */
    public int getHeight() {
        return Math.abs(z2 - z1);
    }

    /**
     * Get the volume of the box.
     *
     * @return The volume of the box.
     */
    public int getVolume() {
        return getWidth() * getLength() * getHeight();
    }

    /**
     * Get the total surface area of the box.
     *
     * @return The total surface area of the box.
     */
    public int getSurface() {
        int width = getWidth(), length = getLength(), height = getHeight();
        return width * height * 2
            + length * height * 2
            + width * length * 2;
    }

    public void translate(int dx, int dy, int dz) {
        x1 += dx;
        x2 += dx;
        y1 += dy;
        y2 += dy;
        z1 += dz;
        x2 += dz;
    }

    /**
     * Extends this box so that it encompasses the other box
     *
     * @param box The box to encompass.
     */
    public void encompass(Box box) {
        normalise();
        box.normalise();
        if (box.x1 < x1) {
            x1 = box.x1;
        }
        if (box.x2 > x2) {
            x2 = box.x2;
        }
        if (box.y1 < y1) {
            y1 = box.y1;
        }
        if (box.y2 > y2) {
            y2 = box.y2;
        }
        if (box.z1 < z1) {
            z1 = box.z1;
        }
        if (box.z2 > z2) {
            z2 = box.z2;
        }
    }

    /**
     * Sets this box to the intersection of it and the specified box; in other
     * words to the volume encompassed by both boxes.
     *
     * <p>If the boxes don't intersect, the x2, y2 and z2 coordinates of this
     * box will be made equal to the x1, y1 and z1 coordinates so that it
     * becomes empty.
     *
     * @param box The box to intersect this box with.
     */
    public void intersect(Box box) {
        normalise();
        box.normalise();
        if ((box.x1 >= x2) || (box.x2 <= x1) || (box.y1 >= y2) || (box.y2 <= y1) || (box.z1 >= z2) || (box.z2 <= z1)) {
            // The boxes don't intersect
            x2 = x1;
            y2 = y1;
            z2 = z1;
        } else {
            if (box.x1 > x1) {
                x1 = box.x1;
            }
            if (box.x2 > x2) {
                x2 = box.x2;
            }
            if (box.y1 > y1) {
                y1 = box.y1;
            }
            if (box.y2 > y2) {
                y2 = box.y2;
            }
            if (box.z1 > z1) {
                z1 = box.z1;
            }
            if (box.z2 > z2) {
                z2 = box.z2;
            }
        }
    }

    public boolean isEmpty() {
        return (x1 == x2) && (y1 == y2) && (z1 == z2);
    }

    public boolean contains(int x, int y, int z) {
        return (x >= x1) && (x < x2) && (y >= y1) && (y < y2) && (z >= z1) && (z < z2);
    }

    public boolean containsXY(int x, int y) {
        return (x >= x1) && (x < x2) && (y >= y1) && (y < y2);
    }

    public Rectangle getFootPrint() {
        normalise();
        return new Rectangle(x1, y1, x2 - x1, y2 - y1);
    }

    /**
     * Expand the box by a particular amount in every direction.
     *
     * @param delta The delta by which to move every face of the box outwards.
     */
    public void expand(int delta) {
        normalise();
        x1 -= delta;
        x2 += delta;
        y1 -= delta;
        y2 += delta;
        z1 -= delta;
        z2 += delta;
    }

    @Override
    public String toString() {
        return "[" + x1 + "," + y1 + "," + z1 + " -> " + x2 + "," + y2 + "," + z2 + "]";
    }

    @Override
    public Box clone() {
        try {
            return (Box) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new InternalError();
        }
    }

    /**
     * Normalises the coordinates such that coordinate 1 is always lower than
     * coordinate 2.
     */
    private void normalise() {
        int tmp;
        if (x1 > x2) {
            tmp = x1;
            x1 = x2;
            x2 = tmp;
        }
        if (y1 > y2) {
            tmp = y1;
            y1 = y2;
            y2 = tmp;
        }
        if (z1 > z2) {
            tmp = z1;
            z1 = z2;
            z2 = tmp;
        }
    }

    private int x1, y1, z1, x2, y2, z2;
}