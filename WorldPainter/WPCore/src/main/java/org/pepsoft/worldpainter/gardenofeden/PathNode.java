/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.gardenofeden;

import javax.vecmath.Point3i;

/**
 *
 * @author pepijn
 */
public abstract class PathNode extends Seed {
    public PathNode(Garden garden, long seed, PathNode parent, Point3i location, int germinationTime, int category) {
        super(garden, seed, parent, location, germinationTime, category);
    }
}