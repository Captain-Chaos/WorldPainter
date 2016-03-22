/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.worldpainter.heightMaps;

import org.pepsoft.worldpainter.HeightMap;

import java.awt.*;

/**
 *
 * @author pepijn
 */
public class DisplacementHeightMap extends AbstractHeightMap {
    public DisplacementHeightMap(HeightMap baseHeightMap, HeightMap angleMap, HeightMap distanceMap) {
        this.baseHeightMap = baseHeightMap;
        this.angleMap = angleMap;
        this.distanceMap = distanceMap;
    }
    
    public DisplacementHeightMap(String name, HeightMap baseHeightMap, HeightMap angleMap, HeightMap distanceMap) {
        super(name);
        this.baseHeightMap = baseHeightMap;
        this.angleMap = angleMap;
        this.distanceMap = distanceMap;
    }

    public HeightMap getBaseHeightMap() {
        return baseHeightMap;
    }

    public HeightMap getAngleMap() {
        return angleMap;
    }

    public HeightMap getDistanceMap() {
        return distanceMap;
    }

    public void setAngleMap(HeightMap angleMap) {
        this.angleMap = angleMap;
    }

    public void setBaseHeightMap(HeightMap baseHeightMap) {
        this.baseHeightMap = baseHeightMap;
    }

    public void setDistanceMap(HeightMap distanceMap) {
        this.distanceMap = distanceMap;
    }

    // HeightMap
    
    @Override
    public float getHeight(float x, float y) {
        float angle = angleMap.getHeight(x, y);
        float distance = distanceMap.getHeight(x, y);
        float actualX = (float) (x + Math.sin(angle) * distance);
        float actualY = (float) (y + Math.cos(angle) * distance);
        return baseHeightMap.getHeight(actualX, actualY);
    }

    @Override
    public float getBaseHeight() {
        return baseHeightMap.getBaseHeight();
    }

    @Override
    public Rectangle getExtent() {
        return baseHeightMap.getExtent();
    }

    @Override
    public int getColour(int x, int y) {
        float angle = angleMap.getHeight(x, y);
        float distance = distanceMap.getHeight(x, y);
        double actualX = x + Math.sin(angle) * distance;
        double actualY = y + Math.cos(angle) * distance;
        return baseHeightMap.getColour((int) (actualX + 0.5), (int) (actualY + 0.5));
    }
    
    // Object
    
    @Override
    public DisplacementHeightMap clone() {
        DisplacementHeightMap clone = new DisplacementHeightMap(name, baseHeightMap.clone(), angleMap.clone(), distanceMap.clone());
        clone.setSeed(getSeed());
        return clone;
    }

    private HeightMap baseHeightMap, angleMap, distanceMap;
}