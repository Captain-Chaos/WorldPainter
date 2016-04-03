/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.heightMaps;

import java.io.ObjectStreamException;

import org.pepsoft.util.IconUtils;
import org.pepsoft.worldpainter.HeightMap;

import javax.swing.*;

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
    public float getHeight(int x, int y) {
        return children[0].getHeight(x, y) + children[1].getHeight(x, y);
    }

    @Override
    public float getHeight(float x, float y) {
        return children[0].getHeight(x, y) + children[1].getHeight(x, y);
    }

    @Override
    public float getBaseHeight() {
        return children[0].getBaseHeight() + children[1].getBaseHeight();
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
        // There are worlds in the wild where heightMap1 and/or heightMap2 are
        // null. No idea how that could happen, but it will cause errors, so
        // fix it as best we can
        if (children[0] == null) {
            if (children[1] == null) {
                return new SumHeightMap(name, new ConstantHeightMap(62), new ConstantHeightMap(0));
            } else {
                return new SumHeightMap(name, new ConstantHeightMap(58 - children[1].getBaseHeight()), children[1]);
            }
        } else if (children[1] == null) {
            return new SumHeightMap(children[0], new ConstantHeightMap(58 - children[0].getBaseHeight()));
        }
        return this;
    }
    
    private static final long serialVersionUID = 1L;
    private static final Icon ICON_SUM_HEIGHTMAP = IconUtils.loadIcon("org/pepsoft/worldpainter/icons/plus.png");
}