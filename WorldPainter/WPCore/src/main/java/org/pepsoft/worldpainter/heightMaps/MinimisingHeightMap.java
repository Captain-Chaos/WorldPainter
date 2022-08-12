/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.heightMaps;

import org.pepsoft.util.IconUtils;
import org.pepsoft.worldpainter.HeightMap;

import javax.swing.*;

/**
 * A height map which returns the lowest of two subordinate height maps.
 *
 * @author SchmitzP
 */
public class MinimisingHeightMap extends CombiningHeightMap {
    public MinimisingHeightMap(HeightMap heightMap1, HeightMap heightMap2) {
        super(heightMap1, heightMap2);
    }

    public MinimisingHeightMap(String name, HeightMap heightMap1, HeightMap heightMap2) {
        super(name, heightMap1, heightMap2);
    }

    @Override
    protected float doGetHeight(int x, int y) {
        return Math.min(children[0].getHeight(x, y), children[1].getHeight(x, y));
    }

    @Override
    protected float doGetHeight(float x, float y) {
        return Math.min(children[0].getHeight(x, y), children[1].getHeight(x, y));
    }

    @Override
    public MinimisingHeightMap clone() {
        MinimisingHeightMap clone = new MinimisingHeightMap(name, children[0].clone(), children[1].clone());
        clone.setSeed(getSeed());
        return clone;
    }

    @Override
    public Icon getIcon() {
        return ICON_MINIMISING_HEIGHTMAP;
    }

    @Override
    public float[] getRange() {
        float[] range0 = children[0].getRange();
        float[] range1 = children[1].getRange();
        return new float[]{Math.min(range0[0], range1[0]), Math.min(range0[1], range1[1])};
    }

    private static final long serialVersionUID = 1L;
    private static final Icon ICON_MINIMISING_HEIGHTMAP = IconUtils.loadScaledIcon("org/pepsoft/worldpainter/icons/min.png"); // TODO
}