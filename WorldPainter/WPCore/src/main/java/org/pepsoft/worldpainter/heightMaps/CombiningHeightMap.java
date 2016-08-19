/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.heightMaps;

import org.pepsoft.worldpainter.HeightMap;

import java.awt.*;

/**
 * An abstract base class for a height map which somehow combines two
 * subordinate height maps.
 * 
 * @author SchmitzP
 */
public abstract class CombiningHeightMap extends DelegatingHeightMap {
    public CombiningHeightMap(HeightMap heightMap1, HeightMap heightMap2) {
        super("heightMap1", "heightMap2");
        setHeightMap(0, heightMap1);
        setHeightMap(1, heightMap2);
    }

    public CombiningHeightMap(String name, HeightMap heightMap1, HeightMap heightMap2) {
        super("heightMap1", "heightMap2");
        setName(name);
        setHeightMap(0, heightMap1);
        setHeightMap(1, heightMap2);
    }

    public final HeightMap getHeightMap1() {
        return children[0];
    }

    public final HeightMap getHeightMap2() {
        return children[1];
    }

    public void setHeightMap1(HeightMap heightMap1) {
        replace(0, heightMap1);
    }

    public void setHeightMap2(HeightMap heightMap2) {
        replace(1, heightMap2);
    }

    // HeightMap

    @Override
    public Rectangle getExtent() {
        Rectangle extent1 = children[0].getExtent();
        Rectangle extent2 = children[1].getExtent();
        return (extent1 != null)
            ? ((extent2 != null) ? extent1.union(extent2) : extent1)
            : extent2;
    }

    @Override
    public abstract CombiningHeightMap clone();

    @Deprecated
    protected HeightMap heightMap1, heightMap2;

    private static final long serialVersionUID = 1L;
}