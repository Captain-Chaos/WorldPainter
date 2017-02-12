/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.heightMaps;

import org.pepsoft.util.IconUtils;
import org.pepsoft.worldpainter.HeightMap;

import javax.swing.*;
import java.io.ObjectStreamException;

/**
 * A height map which is the sum of two other height maps.
 * 
 * @author pepijn
 */
public final class SumHeightMap extends CombiningHeightMap {
    public SumHeightMap(HeightMap heightMap1, HeightMap heightMap2) {
        super(heightMap1, heightMap2);
    }

    public SumHeightMap(String name, HeightMap heightMap1, HeightMap heightMap2) {
        super(name, heightMap1, heightMap2);
    }

    @Override
    protected float doGetHeight(int x, int y) {
        return children[0].getHeight(x, y) + children[1].getHeight(x, y);
    }

    @Override
    protected float doGetHeight(float x, float y) {
        return children[0].getHeight(x, y) + children[1].getHeight(x, y);
    }

    @Override
    public SumHeightMap clone() {
        SumHeightMap clone = new SumHeightMap(name, children[0].clone(), children[1].clone());
        clone.setSeed(getSeed());
        return clone;
    }

    @Override
    public Icon getIcon() {
        return ICON_SUM_HEIGHTMAP;
    }

    private Object readResolve() throws ObjectStreamException {
        if (heightMap1 != null) {
            return new SumHeightMap(heightMap1, heightMap2);
        } else {
            return this;
        }
    }

    @Override
    public float[] getRange() {
        float[] range0 = children[0].getRange();
        float[] range1 = children[1].getRange();
        return new float[] {range0[0] + range1[0], range0[1] + range1[1]};
    }

    private static final long serialVersionUID = 1L;
    private static final Icon ICON_SUM_HEIGHTMAP = IconUtils.loadScaledIcon("org/pepsoft/worldpainter/icons/plus.png");
}