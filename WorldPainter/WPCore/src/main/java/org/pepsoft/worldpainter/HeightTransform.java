/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter;

import org.pepsoft.worldpainter.heightMaps.ConstantHeightMap;
import org.pepsoft.worldpainter.heightMaps.ProductHeightMap;
import org.pepsoft.worldpainter.heightMaps.SumHeightMap;

/**
 *
 * @author pepijn
 */
public class HeightTransform {
    private HeightTransform(int scaleAmount, int translateAmount) {
        scalingFactor = scaleAmount / 100f;
        this.translateAmount = translateAmount;
    }
    
    public boolean isIdentity() {
        return false;
    }
    
    public float transformHeight(float height) {
        return height * scalingFactor + translateAmount;
    }
    
    public int transformHeight(int height) {
        return Math.round(height * scalingFactor + translateAmount);
    }

    public HeightMap transformHeightMap(HeightMap heightMap) {
        if (scalingFactor != 1.0f) {
            heightMap = new ProductHeightMap(heightMap.getName(), heightMap, new ConstantHeightMap("scale", scalingFactor));
        }
        if (translateAmount != 0) {
            heightMap = new SumHeightMap(heightMap.getName(), heightMap, new ConstantHeightMap("translate", translateAmount));
        }
        return heightMap;
    }

    public static HeightTransform get(int scaleAmount, int translateAmount) {
        if ((scaleAmount == 100) && (translateAmount == 0)) {
            return IDENTITY;
        } else {
            return new HeightTransform(scaleAmount, translateAmount);
        }
    }
    
    private final float scalingFactor;
    private final int translateAmount;
    
    public static final HeightTransform IDENTITY = new HeightTransform(100, 0) {
        @Override
        public boolean isIdentity() {
            return true;
        }

        @Override
        public float transformHeight(float height) {
            return height;
        }

        @Override
        public int transformHeight(int height) {
            return height;
        }

        @Override
        public HeightMap transformHeightMap(HeightMap heightMap) {
            return heightMap;
        }
    };
}