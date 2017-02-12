/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.worldpainter.heightMaps;

import org.pepsoft.util.IconUtils;
import org.pepsoft.worldpainter.HeightMap;

import javax.swing.*;
import java.awt.*;

/**
 *
 * @author pepijn
 */
public class DisplacementHeightMap extends DelegatingHeightMap {
    public DisplacementHeightMap(HeightMap baseHeightMap, HeightMap angleMap, HeightMap distanceMap) {
        super("baseHeightMap", "angleMap", "distanceMap");
        setHeightMap(0, baseHeightMap);
        setHeightMap(1, angleMap);
        setHeightMap(2, distanceMap);
    }
    
    public DisplacementHeightMap(String name, HeightMap baseHeightMap, HeightMap angleMap, HeightMap distanceMap) {
        super("baseHeightMap", "angleMap", "distanceMap");
        setName(name);
        setHeightMap(0, baseHeightMap);
        setHeightMap(1, angleMap);
        setHeightMap(2, distanceMap);
    }

    public HeightMap getBaseHeightMap() {
        return children[0];
    }

    public HeightMap getAngleMap() {
        return children[1];
    }

    public HeightMap getDistanceMap() {
        return children[2];
    }

    public void setAngleMap(HeightMap angleMap) {
        replace(1, angleMap);
    }

    public void setBaseHeightMap(HeightMap baseHeightMap) {
        replace(0, baseHeightMap);
    }

    public void setDistanceMap(HeightMap distanceMap) {
        replace(2, distanceMap);
    }

    // HeightMap
    
    @Override
    protected float doGetHeight(float x, float y) {
        float angle = children[1].getHeight(x, y);
        float distance = children[2].getHeight(x, y);
        float actualX = (float) (x + Math.sin(angle) * distance);
        float actualY = (float) (y + Math.cos(angle) * distance);
        return children[0].getHeight(actualX, actualY);
    }

    @Override
    public Rectangle getExtent() {
        return children[0].getExtent();
    }

    @Override
    protected int doGetColour(int x, int y) {
        float angle = children[1].getHeight(x, y);
        float distance = children[2].getHeight(x, y);
        double actualX = x + Math.sin(angle) * distance;
        double actualY = y + Math.cos(angle) * distance;
        return children[0].getColour((int) (actualX + 0.5), (int) (actualY + 0.5));
    }

    @Override
    public Icon getIcon() {
        return ICON_DISPLACEMENT_HEIGHTMAP;
    }

    @Override
    public float[] getRange() {
        return children[0].getRange();
    }

    // Object
    
    @Override
    public DisplacementHeightMap clone() {
        DisplacementHeightMap clone = new DisplacementHeightMap(name, children[0].clone(), children[1].clone(), children[2].clone());
        clone.setSeed(getSeed());
        return clone;
    }

    private static final long serialVersionUID = 1L;
    private static final Icon ICON_DISPLACEMENT_HEIGHTMAP = IconUtils.loadScaledIcon("org/pepsoft/worldpainter/icons/displacement.png");
}