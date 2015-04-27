/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.heightMaps;

import org.pepsoft.worldpainter.HeightMap;

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
        return Math.max(heightMap1.getHeight(x, y), heightMap2.getHeight(x, y));
    }

    @Override
    public float getBaseHeight() {
        return Math.max(heightMap1.getBaseHeight(), heightMap2.getBaseHeight());
    }

    @Override
    public MaximisingHeightMap clone() {
        MaximisingHeightMap clone = new MaximisingHeightMap(name, heightMap1.clone(), heightMap2.clone());
        clone.setSeed(getSeed());
        return clone;
    }

    private static final long serialVersionUID = 1L;    
}