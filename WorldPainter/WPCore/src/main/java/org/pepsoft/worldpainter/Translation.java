/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.worldpainter;

import java.awt.Point;
import javax.vecmath.Point3i;
import org.pepsoft.minecraft.Direction;

/**
 *
 * @author Pepijn Schmitz
 */
public final class Translation extends CoordinateTransform {
    public Translation(int dx, int dy) {
        if ((dx == 0) && (dy == 0)) {
            throw new IllegalArgumentException("dx and dy may not both be zero");
        }
        this.dx = dx;
        this.dy = dy;
    }

    @Override
    public void transformInPlace(Point coords) {
        coords.x += dx;
        coords.y += dy;
    }

    @Override
    public void transformInPlace(Point3i coords) {
        coords.x += dx;
        coords.y += dy;
    }

    @Override
    public Direction transform(Direction direction) {
        return direction;
    }

    @Override
    public Direction inverseTransform(Direction direction) {
        return direction;
    }

    @Override
    public float transform(float angle) {
        return angle;
    }
    
    private final int dx, dy;
}