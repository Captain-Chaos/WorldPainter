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
 * A height map which returns the highest of two subordinate height maps.
 *
 * @author SchmitzP
 */
public class MaximisingHeightMap extends CombiningHeightMap {
    public MaximisingHeightMap(HeightMap heightMap1, HeightMap heightMap2) {
        super(heightMap1, heightMap2);
    }
    
    public MaximisingHeightMap(String name, HeightMap heightMap1, HeightMap heightMap2) {
        super(name, heightMap1, heightMap2);
    }

    @Override
    public float getHeight(int x, int y) {
        return Math.max(children[0].getHeight(x, y), children[1].getHeight(x, y));
    }

    @Override
    public float getHeight(float x, float y) {
        return Math.max(children[0].getHeight(x, y), children[1].getHeight(x, y));
    }

    @Override
    public float getBaseHeight() {
        return Math.max(children[0].getBaseHeight(), children[1].getBaseHeight());
    }

    @Override
    public MaximisingHeightMap clone() {
        MaximisingHeightMap clone = new MaximisingHeightMap(name, children[0].clone(), children[1].clone());
        clone.setSeed(getSeed());
        return clone;
    }

    @Override
    public Icon getIcon() {
        return ICON_MAXIMISING_HEIGHTMAP;
    }

    private static final long serialVersionUID = 1L;
    private static final Icon ICON_MAXIMISING_HEIGHTMAP = IconUtils.loadIcon("org/pepsoft/worldpainter/icons/max.png");
}