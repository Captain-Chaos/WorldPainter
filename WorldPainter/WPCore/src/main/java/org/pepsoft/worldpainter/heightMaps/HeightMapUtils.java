/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.heightMaps;

import org.pepsoft.worldpainter.HeightMap;

/**
 *
 * @author pepijn
 */
public final class HeightMapUtils {
    private HeightMapUtils() {
        // Prevent instantiation
    }
    
    public static HeightMap transposeHeightMap(HeightMap heightMap, float amount) {
        if (heightMap instanceof ConstantHeightMap) {
            return new ConstantHeightMap(heightMap.getName(), heightMap.getBaseHeight() + amount);
        } else if (heightMap instanceof SumHeightMap) {
            SumHeightMap sumHeightMap = (SumHeightMap) heightMap;
            if (sumHeightMap.getHeightMap1() instanceof ConstantHeightMap) {
                return new SumHeightMap(sumHeightMap.getName(), transposeHeightMap(sumHeightMap.getHeightMap1(), amount), sumHeightMap.getHeightMap2());
            } else if (sumHeightMap.getHeightMap2() instanceof ConstantHeightMap) {
                return new SumHeightMap(sumHeightMap.getName(), sumHeightMap.getHeightMap1(), transposeHeightMap(sumHeightMap.getHeightMap2(), amount));
            }
        }
        return new SumHeightMap(new ConstantHeightMap(amount), heightMap);
    }
}